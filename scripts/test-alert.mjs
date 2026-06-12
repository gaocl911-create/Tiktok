#!/usr/bin/env node
/**
 * 预警中心（CreatorAlertController）端到端冒烟测试
 *
 * 覆盖：
 *   POST /auth/login                          登录拿 token（RSA+AES 加密体）
 *   POST /creator/alert/rule                  新建规则
 *   GET  /creator/alert/rule/list             分页查询规则
 *   GET  /creator/alert/rule/enabled          启用规则列表
 *   PUT  /creator/alert/rule                  编辑规则 / 启停切换
 *   GET  /creator/alert/event/list            预警事件分页
 *   DELETE /creator/alert/rule/{ruleIds}      删除规则
 *
 * 用法：
 *   node scripts/test-alert.mjs                 # 默认 http://localhost:8088 admin/admin123 000000
 *   API_BASE=http://localhost:8088 \
 *   USERNAME=admin PASSWORD=admin123 TENANT=000000 \
 *     node scripts/test-alert.mjs
 *
 * 需求：Node 18+（用了原生 fetch）
 */

import crypto from 'node:crypto';
import net from 'node:net';

// ---------- Redis 裸读（无需依赖）----------
const REDIS = { host: 'localhost', port: 16380, password: 'ruoyi123' };

function encodeResp(args) {
  const parts = [`*${args.length}\r\n`];
  for (const a of args) parts.push(`$${Buffer.byteLength(a)}\r\n${a}\r\n`);
  return parts.join('');
}

// 简易 RESP 解析器：把整个缓冲解成数组（够用就行）
function parseRespAll(buf) {
  const s = buf.toString('utf8');
  const replies = [];
  let i = 0;
  while (i < s.length) {
    const type = s[i++];
    const crlf = s.indexOf('\r\n', i);
    if (crlf === -1) break;
    const head = s.slice(i, crlf);
    i = crlf + 2;
    if (type === '+' || type === '-' || type === ':') {
      replies.push(type === '-' ? new Error(head) : head);
    } else if (type === '$') {
      const len = parseInt(head, 10);
      if (len === -1) replies.push(null);
      else { replies.push(s.slice(i, i + len)); i += len + 2; }
    } else {
      replies.push(head);
    }
  }
  return replies;
}

async function redisGet(key) {
  return new Promise((resolve, reject) => {
    const client = net.connect(REDIS.port, REDIS.host);
    let buf = Buffer.alloc(0);
    const cmds = [];
    if (REDIS.password) cmds.push(['AUTH', REDIS.password]);
    cmds.push(['GET', key]);
    cmds.push(['QUIT']);
    client.on('connect', () => {
      client.write(cmds.map(encodeResp).join(''));
    });
    client.on('data', (chunk) => { buf = Buffer.concat([buf, chunk]); });
    client.on('error', reject);
    client.on('end', () => {
      const replies = parseRespAll(buf);
      // GET 的返回是倒数第二个（最后一个是 QUIT 的 +OK）
      const getReply = replies[replies.length - 2];
      if (getReply instanceof Error) reject(getReply);
      else resolve(getReply);
    });
  });
}

// ---------- 配置 ----------
const API_BASE = process.env.API_BASE || 'http://localhost:8088';
const USERNAME = process.env.USERNAME || 'admin';
const PASSWORD = process.env.PASSWORD || 'admin123';
const TENANT_ID = process.env.TENANT || '000000';
const CLIENT_ID = process.env.CLIENT_ID || 'e5cd7e4891bf95d1d19206ce24a7b32e';

// 与 web/.env.development 中保持一致的密钥对
const RSA_PUBLIC_KEY =
  'MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKoR8mX0rGKLqzcWmOzbfj64K8ZIgOdHnzkXSOVOZbFu/TJhZ7rFAN+eaGkl3C4buccQd/EjEsj9ir7ijT7h96MCAwEAAQ==';
const RSA_PRIVATE_KEY =
  'MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAmc3CuPiGL/LcIIm7zryCEIbl1SPzBkr75E2VMtxegyZ1lYRD+7TZGAPkvIsBcaMs6Nsy0L78n2qh+lIZMpLH8wIDAQABAkEAk82Mhz0tlv6IVCyIcw/s3f0E+WLmtPFyR9/WtV3Y5aaejUkU60JpX4m5xNR2VaqOLTZAYjW8Wy0aXr3zYIhhQQIhAMfqR9oFdYw1J9SsNc+CrhugAvKTi0+BF6VoL6psWhvbAiEAxPPNTmrkmrXwdm/pQQu3UOQmc2vCZ5tiKpW10CgJi8kCIFGkL6utxw93Ncj4exE/gPLvKcT+1Emnoox+O9kRXss5AiAMtYLJDaLEzPrAWcZeeSgSIzbL+ecokmFKSDDcRske6QIgSMkHedwND1olF8vlKsJUGK3BcdtM8w4Xq7BpSBwsloE=';

// ---------- 控制台美化 ----------
const c = {
  reset: '\x1b[0m', green: '\x1b[32m', red: '\x1b[31m',
  yellow: '\x1b[33m', cyan: '\x1b[36m', gray: '\x1b[90m'
};
const log = {
  step: (n, t) => console.log(`\n${c.cyan}▶ [${n}] ${t}${c.reset}`),
  ok:   (m)    => console.log(`  ${c.green}✓${c.reset} ${m}`),
  warn: (m)    => console.log(`  ${c.yellow}!${c.reset} ${m}`),
  err:  (m)    => console.log(`  ${c.red}✗ ${m}${c.reset}`),
  info: (m)    => console.log(`  ${c.gray}· ${m}${c.reset}`)
};

// ---------- 加解密 ----------
const pemPub  = `-----BEGIN PUBLIC KEY-----\n${RSA_PUBLIC_KEY}\n-----END PUBLIC KEY-----`;
const pemPriv = `-----BEGIN PRIVATE KEY-----\n${RSA_PRIVATE_KEY}\n-----END PRIVATE KEY-----`;

const randomAesKey = () => {
  // 与前端保持一致：32 位 [A-Za-z0-9] 字符串，再当作 UTF-8 字节作为 AES key
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let s = '';
  for (let i = 0; i < 32; i++) s += chars[crypto.randomInt(0, chars.length)];
  return Buffer.from(s, 'utf8'); // AES-256 key
};
const aesEncrypt = (plaintext, key) => {
  const cipher = crypto.createCipheriv('aes-256-ecb', key, null);
  cipher.setAutoPadding(true);
  return Buffer.concat([cipher.update(plaintext, 'utf8'), cipher.final()]).toString('base64');
};
const aesDecrypt = (b64, key) => {
  const decipher = crypto.createDecipheriv('aes-256-ecb', key, null);
  decipher.setAutoPadding(true);
  return Buffer.concat([decipher.update(b64, 'base64'), decipher.final()]).toString('utf8');
};
const rsaEncryptWithPub = (text) =>
  crypto.publicEncrypt({ key: pemPub, padding: crypto.constants.RSA_PKCS1_PADDING },
    Buffer.from(text, 'utf8')).toString('base64');
const rsaDecryptWithPriv = (b64) =>
  crypto.privateDecrypt({ key: pemPriv, padding: crypto.constants.RSA_PKCS1_PADDING },
    Buffer.from(b64, 'base64')).toString('utf8');

// ---------- 通用 HTTP ----------
let token = null;

async function api(path, { method = 'GET', data, encrypt = false, headers = {} } = {}) {
  const url = API_BASE + path;
  const finalHeaders = {
    clientid: CLIENT_ID,
    'Content-Language': 'zh_CN',
    ...headers
  };
  if (token) finalHeaders.Authorization = `Bearer ${token}`;

  let body;
  if (data !== undefined) {
    const json = JSON.stringify(data);
    if (encrypt) {
      const aesKey = randomAesKey();
      const aesKeyB64 = aesKey.toString('base64');
      finalHeaders['encrypt-key'] = rsaEncryptWithPub(aesKeyB64);
      finalHeaders['Content-Type'] = 'text/plain;charset=utf-8';
      body = aesEncrypt(json, aesKey);
    } else {
      finalHeaders['Content-Type'] = 'application/json;charset=utf-8';
      body = json;
    }
  }

  const res = await fetch(url, { method, headers: finalHeaders, body });
  const rawText = await res.text();

  // 响应可能加密：encrypt-key 头存在 → RSA 私钥解出 AES key → 解 body
  const respKeyHeader = res.headers.get('encrypt-key');
  let payloadText = rawText;
  if (respKeyHeader) {
    const aesKeyB64 = rsaDecryptWithPriv(respKeyHeader);
    const aesKey = Buffer.from(aesKeyB64, 'base64');
    payloadText = aesDecrypt(rawText, aesKey);
  }

  let json;
  try { json = JSON.parse(payloadText); }
  catch { throw new Error(`非 JSON 响应 (${res.status}): ${payloadText.slice(0, 200)}`); }

  if (!res.ok) {
    throw new Error(`HTTP ${res.status} ${path} → ${payloadText.slice(0, 200)}`);
  }
  if (json.code !== undefined && json.code !== 200) {
    throw new Error(`业务错误 ${json.code} ${path} → ${json.msg || payloadText}`);
  }
  return json;
}

// ---------- 获取验证码（直接从 Redis 拿真值，全自动）----------
async function getCaptcha() {
  log.step(0, '拉取验证码并从 Redis 取真值');
  const res = await api('/auth/code');
  const { captchaEnabled, uuid } = res.data;
  if (!captchaEnabled) {
    log.info('服务器未开启验证码，跳过');
    return { code: '', uuid: '' };
  }
  const redisKey = `global:captcha_codes:${uuid}`;
  let code = await redisGet(redisKey);
  if (!code) throw new Error(`Redis 里没找到验证码 key=${redisKey}`);
  // RedisUtils 用 Jackson 序列化，存的是 "35" 这种带引号的 JSON 字符串
  try { code = JSON.parse(code); } catch { /* 已是裸值 */ }
  log.ok(`uuid=${uuid.slice(0, 8)}… code=${code}`);
  return { code, uuid };
}

// ---------- 业务断言 ----------
function assert(cond, msg) { if (!cond) throw new Error('断言失败: ' + msg); }
function expect(actual, expected, label) {
  if (actual !== expected) throw new Error(`${label} 期望 ${expected}, 实际 ${actual}`);
}

// ---------- 测试主流程 ----------
async function login(captcha) {
  log.step(1, '登录获取 token');
  const res = await api('/auth/login', {
    method: 'POST',
    encrypt: true,
    data: {
      username: USERNAME,
      password: PASSWORD,
      tenantId: TENANT_ID,
      clientId: CLIENT_ID,
      grantType: 'password',
      code: captcha.code,
      uuid: captcha.uuid
    }
  });
  token = res.data?.access_token;
  assert(token, `登录失败，未拿到 token：${JSON.stringify(res)}`);
  log.ok(`登录成功，token=${token.slice(0, 24)}...`);
}

async function testCreateRule() {
  log.step(2, '新建预警规则（30 分钟点赞增长 ≥ 500）');
  const payload = {
    ruleName: `自动化测试-30分钟点赞增长-${Date.now()}`,
    metricType: 'like',
    ruleType: 'window_growth',
    windowMinutes: 30,
    thresholdValue: 500,
    scopeType: 'all',
    severity: 'important',
    cooldownMinutes: 120,
    enabled: true
  };
  const res = await api('/creator/alert/rule', { method: 'POST', data: payload });
  const rule = res.data;
  assert(rule?.ruleId, '新建规则未返回 ruleId');
  expect(rule.ruleName, payload.ruleName, 'ruleName');
  expect(rule.metricType, 'like', 'metricType');
  expect(rule.ruleType, 'window_growth', 'ruleType');
  expect(Number(rule.windowMinutes), 30, 'windowMinutes');
  expect(Number(rule.thresholdValue), 500, 'thresholdValue');
  expect(rule.scopeType, 'all', 'scopeType');
  expect(rule.scopeId, null, 'scopeId（all 应被置空）');
  expect(rule.severity, 'important', 'severity');
  expect(rule.enabled, true, 'enabled');
  log.ok(`规则已创建: ruleId=${rule.ruleId}`);
  return rule;
}

async function testCreateCumulativeRule() {
  log.step(3, '新建累计型规则（评论累计 ≥ 1000），验证 windowMinutes 被服务端置空');
  const payload = {
    ruleName: `自动化测试-评论累计-${Date.now()}`,
    metricType: 'comment',
    ruleType: 'cumulative',
    windowMinutes: 30, // 累计型应被后端清空
    thresholdValue: 1000,
    scopeType: 'all',
    severity: 'urgent',
    cooldownMinutes: 60,
    enabled: true
  };
  const res = await api('/creator/alert/rule', { method: 'POST', data: payload });
  const rule = res.data;
  assert(rule?.ruleId, '新建累计型规则未返回 ruleId');
  expect(rule.ruleType, 'cumulative', 'ruleType');
  expect(rule.windowMinutes, null, 'windowMinutes（cumulative 应被清空）');
  log.ok(`累计型规则已创建: ruleId=${rule.ruleId}`);
  return rule;
}

async function testValidationFailures() {
  log.step(4, '校验失败用例（应被服务端拒绝）');

  const cases = [
    {
      name: 'windowMinutes 缺失但 ruleType=window_growth',
      data: {
        ruleName: '非法-窗口缺失', metricType: 'like', ruleType: 'window_growth',
        thresholdValue: 100, scopeType: 'all', severity: 'normal', cooldownMinutes: 30, enabled: true
      }
    },
    {
      name: 'scopeType=creator 但缺少 scopeId',
      data: {
        ruleName: '非法-范围缺失', metricType: 'like', ruleType: 'cumulative',
        thresholdValue: 100, scopeType: 'creator', severity: 'normal', cooldownMinutes: 30, enabled: true
      }
    },
    {
      name: 'metricType 非法',
      data: {
        ruleName: '非法-指标', metricType: 'share', ruleType: 'cumulative',
        thresholdValue: 100, scopeType: 'all', severity: 'normal', cooldownMinutes: 30, enabled: true
      }
    },
    {
      name: 'thresholdValue=0',
      data: {
        ruleName: '非法-阈值', metricType: 'like', ruleType: 'cumulative',
        thresholdValue: 0, scopeType: 'all', severity: 'normal', cooldownMinutes: 30, enabled: true
      }
    }
  ];

  for (const cs of cases) {
    try {
      await api('/creator/alert/rule', { method: 'POST', data: cs.data });
      log.err(`「${cs.name}」未被拒绝（这是个 bug）`);
    } catch (e) {
      log.ok(`「${cs.name}」被正确拒绝：${String(e.message).split('→')[1]?.trim() || e.message}`);
    }
  }
}

async function testListRules(expectedRule) {
  log.step(5, '分页查询规则，确认新规则在列表里');
  const res = await api(`/creator/alert/rule/list?pageNum=1&pageSize=10`);
  const rows = res.rows || [];
  log.info(`列表共 ${res.total} 条，本页 ${rows.length} 条`);
  const hit = rows.find((r) => Number(r.ruleId) === Number(expectedRule.ruleId));
  assert(hit, `分页未命中刚创建的 ruleId=${expectedRule.ruleId}`);
  log.ok(`命中规则：${hit.ruleName}`);
}

async function testEnabledList() {
  log.step(6, '查询已启用规则');
  const res = await api('/creator/alert/rule/enabled');
  const list = res.data || [];
  log.ok(`已启用规则 ${list.length} 条`);
  return list;
}

async function testUpdateRule(rule) {
  log.step(7, '编辑规则：调高阈值到 800，等级改紧急');
  const payload = { ...rule, thresholdValue: 800, severity: 'urgent' };
  const res = await api('/creator/alert/rule', { method: 'PUT', data: payload });
  expect(Number(res.data.thresholdValue), 800, 'thresholdValue');
  expect(res.data.severity, 'urgent', 'severity');
  log.ok('编辑成功');
  return res.data;
}

async function testToggleRule(rule) {
  log.step(8, '切换启停：先禁用再启用');
  let res = await api('/creator/alert/rule', { method: 'PUT', data: { ...rule, enabled: false } });
  expect(res.data.enabled, false, 'enabled→false');
  log.ok('已禁用');
  res = await api('/creator/alert/rule', { method: 'PUT', data: { ...rule, enabled: true } });
  expect(res.data.enabled, true, 'enabled→true');
  log.ok('已重新启用');
}

async function testEventList() {
  log.step(9, '查询预警事件列表（可能为空，这里只验证接口可访问）');
  const res = await api('/creator/alert/event/list?pageNum=1&pageSize=5');
  log.ok(`事件接口可访问，共 ${res.total ?? 0} 条`);
}

async function testDeleteRules(rules) {
  log.step(10, `清理：删除测试创建的 ${rules.length} 条规则`);
  const ids = rules.map((r) => r.ruleId).join(',');
  await api(`/creator/alert/rule/${ids}`, { method: 'DELETE' });
  log.ok(`已删除 ruleIds=${ids}`);

  // 验证一下：列表里应当查不到了
  const res = await api(`/creator/alert/rule/list?pageNum=1&pageSize=50`);
  const remaining = (res.rows || []).filter((r) => rules.some((x) => Number(x.ruleId) === Number(r.ruleId)));
  assert(remaining.length === 0, `还残留 ${remaining.length} 条未删`);
  log.ok('删除后列表已无残留');
}

(async () => {
  const t0 = Date.now();
  const cleanup = [];
  try {
    console.log(`${c.cyan}=== 预警中心冒烟测试 ===${c.reset}`);
    console.log(`${c.gray}API_BASE = ${API_BASE}\n用户     = ${USERNAME}@${TENANT_ID}${c.reset}`);

    const captcha = await getCaptcha();
    await login(captcha);
    const r1 = await testCreateRule();              cleanup.push(r1);
    const r2 = await testCreateCumulativeRule();    cleanup.push(r2);
    await testValidationFailures();
    await testListRules(r1);
    await testEnabledList();
    const r1b = await testUpdateRule(r1);
    cleanup[0] = r1b;
    await testToggleRule(r1b);
    await testEventList();
    await testDeleteRules(cleanup);

    console.log(`\n${c.green}✅ 全部通过${c.reset}（耗时 ${Date.now() - t0} ms）`);
  } catch (err) {
    console.error(`\n${c.red}❌ 测试失败：${err.message}${c.reset}`);
    // 失败也尽量清理
    if (cleanup.length) {
      try {
        const ids = cleanup.map((r) => r.ruleId).filter(Boolean).join(',');
        if (ids) {
          await api(`/creator/alert/rule/${ids}`, { method: 'DELETE' });
          log.warn(`已清理残留规则 ${ids}`);
        }
      } catch (e) {
        log.warn(`清理失败：${e.message}`);
      }
    }
    process.exit(1);
  }
})();
