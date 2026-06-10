param(
  [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$serverRoot = Join-Path $projectRoot 'server'
$webRoot = Join-Path $projectRoot 'web'
$logRoot = Join-Path $projectRoot 'logs'
$composeFile = Join-Path $projectRoot 'deploy\docker-compose.dev.yml'
$mavenSettings = Join-Path $projectRoot 'deploy\maven-settings.xml'
$jdkHome = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$javaExe = Join-Path $jdkHome 'bin\java.exe'
$snailJobJar = Join-Path $serverRoot 'ruoyi-extend\ruoyi-snailjob-server\target\ruoyi-snailjob-server.jar'
$adminJar = Join-Path $serverRoot 'ruoyi-admin\target\ruoyi-admin.jar'

if (-not (Test-Path $javaExe)) {
  throw "JDK 17 not found: $javaExe"
}

New-Item -ItemType Directory -Path $logRoot -Force | Out-Null

docker compose -p tiktok-platform -f $composeFile up -d mysql redis minio
if ($LASTEXITCODE -ne 0) {
  throw 'Failed to start Docker infrastructure.'
}

$env:JAVA_HOME = $jdkHome
$env:Path = "$jdkHome\bin;$env:Path"

if (-not $SkipBuild) {
  Push-Location $serverRoot
  try {
    mvn -s $mavenSettings -T 1C -DskipTests package
    if ($LASTEXITCODE -ne 0) {
      throw 'Backend build failed.'
    }
  } finally {
    Pop-Location
  }

  Push-Location $webRoot
  try {
    if (-not (Test-Path (Join-Path $webRoot 'node_modules'))) {
      npm install
      if ($LASTEXITCODE -ne 0) {
        throw 'Frontend dependency installation failed.'
      }
    }
  } finally {
    Pop-Location
  }
}

function Test-PortListening {
  param([int]$Port)
  return $null -ne (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

if (-not (Test-PortListening 18800)) {
  Start-Process `
    -FilePath $javaExe `
    -ArgumentList @(
      '-Dserver.port=18800',
      '-Dsnail-job.server-host=127.0.0.1',
      '-Dsnail-job.server-port=17889',
      '-Dspring.datasource.url=jdbc:mysql://localhost:13307/ry-vue?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true',
      '-Dspring.datasource.username=root',
      '-Dspring.datasource.password=root',
      '-Dspring.boot.admin.client.enabled=false',
      '-jar',
      $snailJobJar
    ) `
    -WorkingDirectory $projectRoot `
    -RedirectStandardOutput (Join-Path $logRoot 'snailjob.out.log') `
    -RedirectStandardError (Join-Path $logRoot 'snailjob.err.log') `
    -WindowStyle Hidden
}

if (-not (Test-PortListening 8088)) {
  Start-Process `
    -FilePath $javaExe `
    -ArgumentList @(
      '-Dserver.port=8088',
      '-Dspring.boot.admin.client.enabled=false',
      '-jar',
      $adminJar
    ) `
    -WorkingDirectory $projectRoot `
    -RedirectStandardOutput (Join-Path $logRoot 'server.out.log') `
    -RedirectStandardError (Join-Path $logRoot 'server.err.log') `
    -WindowStyle Hidden
}

if (-not (Test-PortListening 5180)) {
  Start-Process `
    -FilePath 'npm.cmd' `
    -ArgumentList @('run', 'dev') `
    -WorkingDirectory $webRoot `
    -RedirectStandardOutput (Join-Path $logRoot 'web.out.log') `
    -RedirectStandardError (Join-Path $logRoot 'web.err.log') `
    -WindowStyle Hidden
}

Write-Host 'Development services are starting:'
Write-Host '  Web:      http://localhost:5180'
Write-Host '  API:      http://localhost:8088'
Write-Host '  SnailJob: http://localhost:18800/snail-job'
Write-Host '  MinIO:    http://localhost:19001'
Write-Host "Logs: $logRoot"
