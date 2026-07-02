param(
  [string]$Domain,
  [string]$ServerHost,
  [string]$ServerUser = "root",
  [string]$RemotePath = "/opt/TikTok_Platform/deploy/.env.prod",
  [switch]$Upload
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $repoRoot "deploy\.env.prod"
$examplePath = Join-Path $repoRoot "deploy\.env.prod.example"
$secretsPath = Join-Path $repoRoot "SECRETS.local.md"

function New-RandomSecret([int]$Bytes = 32) {
  $buffer = [byte[]]::new($Bytes)
  $rng = [System.Security.Cryptography.RNGCryptoServiceProvider]::new()
  try {
    $rng.GetBytes($buffer)
    return [Convert]::ToBase64String($buffer)
  } finally {
    $rng.Dispose()
  }
}

function New-RsaKeyPair {
  $openssl = Get-Command openssl -ErrorAction SilentlyContinue
  if (-not $openssl) {
    $opensslCandidates = @(
      "C:\Program Files\Git\usr\bin\openssl.exe",
      "C:\Program Files\Git\mingw64\bin\openssl.exe",
      "D:\soft\Git\usr\bin\openssl.exe",
      "D:\soft\Git\mingw64\bin\openssl.exe",
      "D:\soft\BianCheng\Git\usr\bin\openssl.exe",
      "D:\soft\BianCheng\Git\mingw64\bin\openssl.exe"
    )
    foreach ($candidate in $opensslCandidates) {
      if (Test-Path $candidate) {
        $openssl = @{ Source = $candidate }
        break
      }
    }
  }
  if (-not $openssl) {
    throw "openssl is required to generate RSA keys. Install OpenSSL or fill API_DECRYPT_REQUEST_PRIVATE_KEY and WEB_RSA_PUBLIC_KEY manually."
  }

  $tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("tiktok-rsa-" + [guid]::NewGuid().ToString("N"))
  New-Item -ItemType Directory -Path $tempDir | Out-Null
  $privatePem = Join-Path $tempDir "request-private.pem"
  $publicPem = Join-Path $tempDir "request-public.pem"
  try {
    & $openssl.Source genrsa -out $privatePem 2048 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "openssl genrsa failed" }
    & $openssl.Source rsa -in $privatePem -pubout -out $publicPem | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "openssl rsa -pubout failed" }

    $privateBody = (Get-Content -Encoding ASCII $privatePem | Where-Object { $_ -notmatch '^-----' }) -join ''
    $publicBody = (Get-Content -Encoding ASCII $publicPem | Where-Object { $_ -notmatch '^-----' }) -join ''

    return @{
      Private = $privateBody
      Public = $publicBody
    }
  } finally {
    Remove-Item -Recurse -Force $tempDir -ErrorAction SilentlyContinue
  }
}

function Read-EnvFile([string]$Path) {
  $map = [ordered]@{}
  if (-not (Test-Path $Path)) {
    return $map
  }
  Get-Content -Encoding UTF8 $Path | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z0-9_]+)\s*=(.*)$') {
      $map[$Matches[1]] = $Matches[2].Trim()
    }
  }
  return $map
}

function Read-SecretFromMarkdown([string]$Key) {
  if (-not (Test-Path $secretsPath)) {
    return $null
  }
  $content = Get-Content -Encoding UTF8 $secretsPath -Raw

  $patterns = @(
    "(?m)^\s*(?:[-*]\s*)?`?$([regex]::Escape($Key))`?\s*[:=]\s*`?([^`\r\n]+)`?\s*$",
    "(?m)^\s*\|\s*`?$([regex]::Escape($Key))`?\s*\|\s*`?([^|`\r\n]+)`?\s*\|"
  )

  foreach ($pattern in $patterns) {
    $match = [regex]::Match($content, $pattern)
    if ($match.Success) {
      $value = $match.Groups[1].Value.Trim()
      if ($value -and $value -notmatch '^\*+$' -and $value -ne '--') {
        return $value
      }
    }
  }
  return $null
}

function Get-CurrentValue([string]$Key, [hashtable]$EnvValues) {
  if ($EnvValues.Contains($Key) -and -not [string]::IsNullOrWhiteSpace($EnvValues[$Key])) {
    return $EnvValues[$Key]
  }
  return Read-SecretFromMarkdown $Key
}

function Normalize-Domain([string]$Value) {
  if ([string]::IsNullOrWhiteSpace($Value)) {
    return $null
  }
  $normalized = $Value.Trim().TrimEnd("/")
  if ($normalized -notmatch '^https?://') {
    $normalized = "https://$normalized"
  }
  if ($normalized -notmatch '^https://') {
    throw "Domain must use HTTPS: $normalized"
  }
  return $normalized
}

if (-not (Test-Path $examplePath)) {
  throw "Missing template: $examplePath"
}

$envValues = Read-EnvFile $envPath
$result = [ordered]@{}

Get-Content -Encoding UTF8 $examplePath | ForEach-Object {
  if ($_ -match '^\s*([A-Za-z0-9_]+)\s*=') {
    $key = $Matches[1]
    $value = Get-CurrentValue $key $envValues
    if ($null -ne $value) {
      $result[$key] = $value
    } else {
      $result[$key] = ""
    }
  }
}

$defaults = @{
  CONTAINER_PREFIX = "tiktok-platform"
  MYSQL_DATABASE = "ry-vue"
  MYSQL_USERNAME = "ruoyi"
  MINIO_ROOT_USER = "minioadmin"
  TIKHUB_ENABLED = "false"
  WECHAT_MINIAPP_MOCK_ENABLED = "false"
  WEB_HTTP_PORT = "80"
  WEB_HTTPS_PORT = "443"
  JAVA_OPTS = "-Xms512m -Xmx1024m"
  WEB_APP_TITLE = "Creator Monitoring"
  WEB_APP_LOGO_TITLE = "Creator Monitoring"
  WEB_CONTEXT_PATH = "/"
  WEB_MONITOR_ADMIN = "/admin/applications"
  WEB_SNAILJOB_ADMIN = "/snail-job"
  WEB_BASE_API = "/prod-api"
  WEB_BUILD_COMPRESS = "gzip"
  WEB_CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e"
  WEB_WEBSOCKET = "false"
  WEB_SSE = "true"
}

foreach ($key in $defaults.Keys) {
  if ([string]::IsNullOrWhiteSpace($result[$key])) {
    $result[$key] = $defaults[$key]
  }
}

foreach ($key in @(
  "MYSQL_ROOT_PASSWORD",
  "MYSQL_PASSWORD",
  "REDIS_PASSWORD",
  "MINIO_ROOT_PASSWORD",
  "SNAIL_JOB_TOKEN",
  "SA_TOKEN_JWT_SECRET"
)) {
  if ([string]::IsNullOrWhiteSpace($result[$key])) {
    $result[$key] = New-RandomSecret 32
  }
}

if ([string]::IsNullOrWhiteSpace($result["API_DECRYPT_REQUEST_PRIVATE_KEY"]) -or
    [string]::IsNullOrWhiteSpace($result["WEB_RSA_PUBLIC_KEY"]) -or
    [string]::IsNullOrWhiteSpace($result["API_DECRYPT_RESPONSE_PUBLIC_KEY"])) {
  $rsa = New-RsaKeyPair
  if ([string]::IsNullOrWhiteSpace($result["API_DECRYPT_REQUEST_PRIVATE_KEY"])) {
    $result["API_DECRYPT_REQUEST_PRIVATE_KEY"] = $rsa.Private
  }
  if ([string]::IsNullOrWhiteSpace($result["WEB_RSA_PUBLIC_KEY"])) {
    $result["WEB_RSA_PUBLIC_KEY"] = $rsa.Public
  }
  if ([string]::IsNullOrWhiteSpace($result["API_DECRYPT_RESPONSE_PUBLIC_KEY"])) {
    $result["API_DECRYPT_RESPONSE_PUBLIC_KEY"] = $rsa.Public
  }
}

$normalizedDomain = Normalize-Domain $Domain
if ($normalizedDomain) {
  $result["CORS_ALLOWED_ORIGINS"] = $normalizedDomain
}

$missing = @()
foreach ($key in @(
  "MYSQL_ROOT_PASSWORD",
  "MYSQL_USERNAME",
  "MYSQL_PASSWORD",
  "REDIS_PASSWORD",
  "MINIO_ROOT_USER",
  "MINIO_ROOT_PASSWORD",
  "SNAIL_JOB_TOKEN",
  "SA_TOKEN_JWT_SECRET",
  "API_DECRYPT_REQUEST_PRIVATE_KEY",
  "API_DECRYPT_RESPONSE_PUBLIC_KEY",
  "WEB_RSA_PUBLIC_KEY",
  "WEB_CLIENT_ID",
  "CORS_ALLOWED_ORIGINS",
  "WECHAT_MINIAPP_APPID",
  "WECHAT_MINIAPP_APP_SECRET"
)) {
  if ([string]::IsNullOrWhiteSpace($result[$key])) {
    $missing += $key
  }
}

if ($missing.Count -gt 0) {
  Write-Host "Still missing required values:" -ForegroundColor Yellow
  $missing | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
  Write-Host ""
  Write-Host "Pass -Domain your-domain.com, or fill the missing values in deploy/.env.prod / SECRETS.local.md." -ForegroundColor Yellow
  exit 1
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Generated by scripts/prepare-prod-env.ps1")
$lines.Add("# Do not commit this file.")
foreach ($key in $result.Keys) {
  $lines.Add("$key=$($result[$key])")
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines($envPath, $lines, $utf8NoBom)
Write-Host "Wrote production env: $envPath" -ForegroundColor Green

if ($Upload) {
  if ([string]::IsNullOrWhiteSpace($ServerHost)) {
    throw "-ServerHost is required when using -Upload"
  }
  $target = "${ServerUser}@${ServerHost}:${RemotePath}"
  Write-Host "Uploading to $target" -ForegroundColor Cyan
  scp $envPath $target
  if ($LASTEXITCODE -ne 0) {
    throw "scp upload failed"
  }
  Write-Host "Upload complete. Run this on the server:" -ForegroundColor Green
  Write-Host "docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml config --quiet"
}
