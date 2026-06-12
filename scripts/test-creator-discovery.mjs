#!/usr/bin/env node
/**
 * 作者新作品自动发现功能测试
 *
 * 验证：添加作者监控后，作者发布的新作品能否被自动发现并加入到内容监测模块
 *
 * 阶段一：添加作者监控 → 触发采集（baseline 是现在，预期 discovered=0）
 * 阶段二：把 baseline_time 改到 2020-01-01 → 再次触发采集（预期 discovered>0，作品入库）
 *
 * 用法：
 *   # 模式 A：复用已存在的作者监控（推荐 —— 不会创建新作者，不污染数据）
 *   node scripts/test-creator-discovery.mjs --target <targetId>
 *   node scripts/test-creator-discovery.mjs --creator <creatorId>
 *
 *   # 模式 B：添加新作者监控
 *   node scripts/test-creator-discovery.mjs <作者主页URL或sec_user_id>
 *
 * 依赖：Node 18+；docker 中的 tiktok-platform-mysql 可访问；
 *      Redis (localhost:16380 ruoyi123) 用来读验证码。
 */

import crypto from 'node:crypto';
import net from 'node:net';
import { execSync } from 'node:child_process';

// ---------- 配置 ----------
const API_BASE = process.env.API_BASE || 'http://localhost:8088';
const USERNAME = process.env.USERNAME || 'admin';
const PASSWORD = process.env.PASSWORD || 'admin123';
const TENANT_ID = process.env.TENANT || '000000';
const CLIENT_ID = process.env.CLIENT_ID || 'e5cd7e4891bf95d1d19206ce24a7b32e';
const MYSQL_CONTAINER = process.env.MYSQL_CONTAINER || 'tiktok-platform-mysql';
const MYSQL_DB = process.env.MYSQL_DB || 'ry-vue';

// 解析参数
const argv = process.argv.slice(2);
let MODE = 'create';     // create | reuse
let CREATOR_INPUT = null;
let REUSE_TARGET_ID = null;
let REUSE_CREATOR_ID = null;
for (let i = 0; i < argv.length; i++) {
  if (argv[i] === '--target') { MODE = 'reuse'; REUSE_TARGET_ID = argv[++i]; }
  else if (argv[i] === '--creator') { MODE = 'reuse'; REUSE_CREATOR_ID = argv[++i]; }
  else if (!CREATOR_INPUT) CREATOR_INPUT = argv[i];
}
if (MODE === 'create' && !CREATOR_INPUT) {
  console.error('用法:');
  console.error('  node scripts/test-creator-discovery.mjs --target <targetId>');
  console.error('  node scripts/test-creator-discovery.mjs --creator <creatorId>');
  console.error('  node scripts/test-creator-discovery.mjs <作者主页URL或sec_user_id>');
  process.exit(2);
}

const RSA_PUBLIC_KEY =
  'MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKoR8mX0rGKLqzcWmOzbfj64K8ZIgOdHnzkXSOVOZbFu/TJhZ7rFAN+eaGkl3C4buccQd/EjEsj9ir7ijT7h96MCAwEAAQ==';
const pemPub = `-----BEGIN PUBLIC KEY-----\n${RSA_PUBLIC_KEY}\n-----END PUBLIC KEY-----`;

// ---------- 美化 ----------
const c = { reset: '\x1b[0m', green: '\x1b[32m', red: '\x1b[31m', yellow: '\x1b[33m', cyan: '\x1b[36m', gray: '\x1b[90m', bold: '\x1b[1m' };
const log = {
  step: (n, t) => console.log(`\n${c.cyan}${c.bold}▶ [${n}] ${t}${c.reset}`),
  ok:   (m)    => console.log(`  ${c.green}✓${c.reset} ${m}`),
  warn: (m)    => console.log(`  ${c.yellow}!${c.reset} ${m}`),
  err:  (m)    => console.log(`  ${c.red}✗ ${m}${c.reset}`),
  info: (m)    => console.log(`  ${c.gray}· ${m}${c.reset}`),
  kv:   (k,v)  => console.log(`  ${c.gray}·${c.reset} ${k}: ${c.bold}${v}${c.reset}`)
};

// ---------- Redis 拿验证码 ----------
const REDIS = { host: 'localhost', port: 16380, password: 'ruoyi123' };
const encodeResp = (args) => `*${args.length}\r\n${args.map(a => `$${Buffer.byteLength(a)}\r\n${a}\r\n`).join('')}`;
function parseResp(buf) {
  const s = buf.toString('utf8');
  const replies = [];
  let i = 0;
  while (i < s.length) {
    const t = s[i++]; const e = s.indexOf('\r\n', i);
    if (e < 0) break;
    const h = s.slice(i, e); i = e + 2;
    if (t === '+' || t === ':') replies.push(h);
    else if (t === '-') replies.push(new Error(h));
    else if (t === '$') {
      const len = parseInt(h, 10);
      if (len === -1) replies.push(null);
      else { replies.push(s.slice(i, i + len)); i += len + 2; }
    }
  }
  return replies;
}
function redisGet(key) {
  return new Promise((resolve, reject) => {
    const cli = net.connect(REDIS.port, REDIS.host);
    let buf = Buffer.alloc(0);
    cli.on('connect', () => cli.write(encodeResp(['AUTH', REDIS.password]) + encodeResp(['GET', key]) + encodeResp(['QUIT'])));
    cli.on('data', (d) => { buf = Buffer.concat([buf, d]); });
    cli.on('error', reject);
    cli.on('end', () => {
      const r = parseResp(buf);
      const getReply = r[r.length - 2];
      if (getReply instanceof Error) reject(getReply); else resolve(getReply);
    });
  });
}

// ---------- 加解密 ----------
const randomAesKey = () => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let s = ''; for (let i = 0; i < 32; i++) s += chars[crypto.randomInt(0, chars.length)];
  return Buffer.from(s, 'utf8');
};
const aesEncrypt = (text, key) => {
  const c = crypto.createCipheriv('aes-256-ecb', key, null);
  return Buffer.concat([c.update(text, 'utf8'), c.final()]).toString('base64');
};
const rsaEncrypt = (text) =>
  crypto.publicEncrypt({ key: pemPub, padding: crypto.constants.RSA_PKCS1_PADDING },
    Buffer.from(text, 'utf8')).toString('base64');

// ---------- HTTP ----------
let token = null;
async function api(path, { method = 'GET', data, encrypt = false, headers = {} } = {}) {
  const url = API_BASE + path;
  const h = { clientid: CLIENT_ID, 'Content-Language': 'zh_CN', ...headers };
  if (token) h.Authorization = `Bearer ${token}`;
  let body;
  if (data !== undefined) {
    const json = JSON.stringify(data);
    if (encrypt) {
      const k = randomAesKey();
      h['encrypt-key'] = rsaEncrypt(k.toString('base64'));
      h['Content-Type'] = 'text/plain;charset=utf-8';
      body = aesEncrypt(json, k);
    } else {
      h['Content-Type'] = 'application/json;charset=utf-8';
      body = json;
    }
  }
  const res = await fetch(url, { method, headers: h, body });
  const text = await res.text();
  let j;
  try { j = JSON.parse(text); } catch { throw new Error(`非JSON ${res.status} ${path}: ${text.slice(0, 200)}`); }
  if (!res.ok) throw new Error(`HTTP ${res.status} ${path}: ${text.slice(0, 200)}`);
  if (j.code !== undefined && j.code !== 200) throw new Error(`业务错误 ${j.code} ${path}: ${j.msg || text}`);
  return j;
}

// ---------- MySQL via docker exec ----------
function sql(q) {
  const out = execSync(
    `docker exec ${MYSQL_CONTAINER} mysql -uroot -proot -D${MYSQL_DB} -N -B -e "${q.replace(/"/g, '\\"')}"`,
    { stdio: ['ignore', 'pipe', 'pipe'] }
  );
  return out.toString('utf8').trim();
}

// ---------- 登录 ----------
async function login() {
  log.step(1, '登录');
  const codeRes = await api('/auth/code');
  const { uuid, captchaEnabled } = codeRes.data;
  let code = '';
  if (captchaEnabled) {
    const v = await redisGet(`global:captcha_codes:${uuid}`);
    code = v ? JSON.parse(v) : '';
    log.info(`captcha=${code}`);
  }
  const r = await api('/auth/login', {
    method: 'POST', encrypt: true,
    data: { username: USERNAME, password: PASSWORD, tenantId: TENANT_ID, clientId: CLIENT_ID, grantType: 'password', code, uuid }
  });
  token = r.data?.access_token;
  if (!token) throw new Error('登录未拿到 token');
  log.ok(`token=${token.slice(0, 20)}…`);
}

// ---------- 主流程 ----------
let createdTargetId = null;
let createdCreatorId = null;
let creatorPreExisted = false;

async function main() {
  console.log(`${c.cyan}${c.bold}=== 作者新作品自动发现功能测试 ===${c.reset}`);
  log.info(`API:      ${API_BASE}`);
  log.info(`MySQL:    docker:${MYSQL_CONTAINER}/${MYSQL_DB}`);
  log.info(`作者输入: ${CREATOR_INPUT}`);

  await login();

  // ============ 阶段一 ============
  if (MODE === 'reuse') {
    log.step(2, '模式：复用已有作者监控，不创建新对象');
    // resolve target — 注意 ID 是雪花算法的 19 位 long，必须保留字符串避免 JS 精度丢失
    if (REUSE_TARGET_ID) {
      createdTargetId = String(REUSE_TARGET_ID);
    } else {
      const t = sql(`SELECT target_id FROM cm_monitor_target WHERE creator_id=${REUSE_CREATOR_ID} AND target_type='creator_collection' ORDER BY target_id DESC LIMIT 1`);
      if (!t) throw new Error(`creator_id=${REUSE_CREATOR_ID} 没有对应 creator_collection 监控目标`);
      createdTargetId = String(t);
    }
    log.kv('target_id', createdTargetId);
    // 用 SQL 直接读取，避开 HTTP 层的 long → JSON 精度问题
    const row = sql(`SELECT creator_id, baseline_time, discover_new_content, target_type FROM cm_monitor_target WHERE target_id=${createdTargetId}`);
    if (!row) throw new Error(`target_id=${createdTargetId} 在 cm_monitor_target 表中不存在`);
    const [cid, baseline, discover, ttype] = row.split('\t');
    createdCreatorId = String(cid);
    creatorPreExisted = true;
    const nickname = sql(`SELECT nickname FROM cm_creator_account WHERE creator_id=${createdCreatorId}`);
    log.kv('creator_id', createdCreatorId);
    log.kv('作者昵称', nickname);
    log.kv('baselineTime', baseline);
    log.kv('discoverNewContent', discover === '1' ? 'true' : 'false');
    log.kv('targetType', ttype);
    if (discover !== '1') throw new Error('discoverNewContent=false，不能测试发现功能');
  } else {
    log.step(2, '添加作者监控（POST /creator/account/monitor）');
    const addRes = await api('/creator/account/monitor', {
      method: 'POST',
      data: {
        platform: 'douyin',
        profileInput: CREATOR_INPUT,
        targetName: `自动化测试-${Date.now()}`,
        discoverNewContent: true,
        profileCollectIntervalMin: 360,
        contentCollectIntervalMin: 30
      }
    });
    const target0 = addRes.data.target;
    const creator0 = addRes.data.creator;
    createdTargetId = String(target0.targetId);
    createdCreatorId = String(creator0.creatorId);
    creatorPreExisted = !addRes.data.creatorCreated;

    log.ok('监控目标创建成功');
    log.kv('creatorId',       creator0.creatorId);
    log.kv('targetId',        target0.targetId);
    log.kv('作者昵称',         creator0.nickname);
    log.kv('粉丝数',          creator0.followerCount);
    log.kv('作品数(平台)',     creator0.contentCount);
    log.kv('platformCreatorId', creator0.platformCreatorId);
    log.kv('targetType',      target0.targetType);
    log.kv('discoverNewContent', target0.discoverNewContent);
    log.kv('baselineTime',    target0.baselineTime);
    log.kv('creatorCreated',  addRes.data.creatorCreated);
    log.kv('targetCreated',   addRes.data.targetCreated);

    if (target0.targetType !== 'creator_collection') {
      throw new Error(`期望 targetType=creator_collection, 实际=${target0.targetType}`);
    }
    if (!target0.discoverNewContent) {
      log.warn('discoverNewContent=false，发现功能不会启用');
    }
  }

  log.step(3, '记录数据库初始状态');
  const contentCountBefore = Number(sql(`SELECT COUNT(*) FROM cm_content_post WHERE creator_id=${createdCreatorId}`));
  const boundCountBefore = Number(sql(`SELECT COUNT(*) FROM cm_monitor_target_content WHERE target_id=${createdTargetId}`));
  const snapshotCountBefore = Number(sql(`SELECT COUNT(*) FROM cm_content_snapshot s JOIN cm_content_post p ON s.content_id=p.content_id WHERE p.creator_id=${createdCreatorId}`));
  log.kv('cm_content_post (该作者)', contentCountBefore);
  log.kv('cm_monitor_target_content', boundCountBefore);
  log.kv('cm_content_snapshot (该作者)', snapshotCountBefore);

  // ============ 第一次采集 ============
  if (MODE === 'create') {
    log.step(4, '首次采集（baseline=now，预期 discoveredCount=0）');
    const c1 = await api(`/creator/target/${createdTargetId}/collect`, { method: 'POST' });
    const run1 = c1.data.run;
    log.kv('runId',           run1.runId);
    log.kv('status',          run1.status);
    log.kv('discoveredCount', run1.discoveredCount);
    log.kv('collectedCount',  run1.collectedCount);
    log.kv('apiCallCount',    run1.apiCallCount);
    if (run1.status !== 'success') throw new Error(`首次采集 status=${run1.status}: ${run1.errorMessage}`);
    if (run1.discoveredCount === 0) log.ok('符合预期：baseline=now，无新作品');
    else log.warn(`意外发现 ${run1.discoveredCount} 条 —— baseline 过滤可能未生效`);
  } else {
    log.step(4, '复用模式：跳过 baseline=now 的首采（直接进入发现验证）');
  }

  // ============ 阶段二：把 baseline 改早 ============
  log.step(5, '把 baseline_time 改到 2020-01-01，模拟"添加监控后又发了新作品"');
  // 同时重置 next_discovery_at 让发现逻辑不被未到期挡住（代码 line 274-276）
  sql(`UPDATE cm_monitor_target SET baseline_time='2020-01-01 00:00:00', next_discovery_at='2020-01-01 00:00:00' WHERE target_id=${createdTargetId}`);
  const newBaseline = sql(`SELECT baseline_time FROM cm_monitor_target WHERE target_id=${createdTargetId}`);
  log.kv('新 baseline_time', newBaseline);

  log.step(6, '第二次采集（baseline 调早后，应该发现历史作品）');
  const c2 = await api(`/creator/target/${createdTargetId}/collect`, { method: 'POST' });
  const run2 = c2.data.run;
  log.kv('runId',           run2.runId);
  log.kv('status',          run2.status);
  log.kv('discoveredCount', run2.discoveredCount);
  log.kv('collectedCount',  run2.collectedCount);

  const contentCountAfterRun2 = Number(sql(`SELECT COUNT(*) FROM cm_content_post WHERE creator_id=${createdCreatorId}`));
  log.kv('采集后在库内容数', contentCountAfterRun2);

  if (run2.status !== 'success') throw new Error(`第二次采集失败: ${run2.errorMessage}`);

  // ============ 关键断言 ============
  log.step(7, '关键断言：内容确实被自动发现并入库');
  if (run2.discoveredCount > 0) {
    log.ok(`✅ 发现 ${run2.discoveredCount} 条新作品 —— 功能正常`);
  } else {
    log.warn('discoveredCount=0，可能原因：作者真的没有作品，或 TikHub 未返回数据');
  }

  const insertedCount = contentCountAfterRun2 - contentCountBefore;
  if (insertedCount > 0) {
    log.ok(`cm_content_post 表新增 ${insertedCount} 条记录`);
  } else {
    log.warn('cm_content_post 没有新增，发现的作品可能在数据库已存在');
  }

  // 验证 target_content 绑定关系
  const boundCount = Number(sql(`SELECT COUNT(*) FROM cm_monitor_target_content WHERE target_id=${createdTargetId}`));
  log.kv('cm_monitor_target_content 绑定数', boundCount);
  if (boundCount > 0) log.ok('作品已绑定到该监控目标');

  // 验证有快照
  const snapshotCount = Number(sql(`SELECT COUNT(*) FROM cm_content_snapshot s JOIN cm_content_post p ON s.content_id=p.content_id WHERE p.creator_id=${createdCreatorId}`));
  log.kv('cm_content_snapshot 数', snapshotCount);
  if (snapshotCount > 0) log.ok('作品快照已生成（点赞/评论数据已采集）');

  // 通过 HTTP API 再查一次内容列表，模拟前端调用
  log.step(8, '通过 HTTP API 验证内容已出现在「内容监测」模块');
  const listRes = await api(`/creator/content/list?pageNum=1&pageSize=10&creatorId=${createdCreatorId}`);
  log.kv('内容API total', listRes.total);
  if (listRes.rows && listRes.rows.length > 0) {
    log.ok('前 3 条作品预览：');
    for (const row of listRes.rows.slice(0, 3)) {
      console.log(`     ${c.gray}·${c.reset} [${row.contentId}] ${(row.title || '(无标题)').slice(0, 40)} 👍${row.latestLikeCount || 0} 💬${row.latestCommentCount || 0}`);
    }
  }

  // ============ 总结 ============
  console.log(`\n${c.cyan}${c.bold}=== 结论 ===${c.reset}`);
  const newContentCount = contentCountAfterRun2 - contentCountBefore;
  const finalBoundCount = Number(sql(`SELECT COUNT(*) FROM cm_monitor_target_content WHERE target_id=${createdTargetId}`));
  const newBoundCount = finalBoundCount - boundCountBefore;
  const newSnapshotCount = snapshotCount - snapshotCountBefore;
  const features = [
    ['采集接口可调用，第二次采集返回 success',                    run2.status === 'success'],
    ['采集流程发现到作者已有作品 (discoveredCount > 0)',          run2.discoveredCount > 0],
    ['作品被插入 cm_content_post 表（核心：发现 → 入库）',         newContentCount > 0],
    ['作品与监控目标绑定到 cm_monitor_target_content',            newBoundCount > 0],
    ['作品同时生成首次快照 cm_content_snapshot',                  newSnapshotCount > 0],
    ['作品出现在「内容监测」列表 API (/creator/content/list)',    Number(listRes.total) > 0]
  ];
  for (const [name, ok] of features) {
    console.log(`  ${ok ? c.green + '✓' : c.red + '✗'}${c.reset} ${name}`);
  }
  const allOk = features.every(([, ok]) => ok);
  console.log(`\n${allOk ? c.green + c.bold + '🎉 自动发现功能验证通过' : c.red + c.bold + '⚠️  存在异常项，请人工复核'}${c.reset}`);
}

async function cleanup() {
  if (!createdTargetId && !createdCreatorId) return;
  log.step('🧹', '清理测试数据');
  try {
    if (MODE === 'reuse') {
      // 复用模式：只恢复 baseline_time，作者和发现到的作品保留（这本来就是应该被发现的）
      // 把 baseline 恢复成 NOW()，避免下次又把所有作品当新发现
      sql(`UPDATE cm_monitor_target SET baseline_time=NOW() WHERE target_id=${createdTargetId}`);
      log.ok(`已恢复 target=${createdTargetId} 的 baseline_time=NOW()，发现到的作品保留`);
      log.info('如需删除发现到的作品/绑定关系，请手动到 web UI 操作');
      return;
    }
    // create 模式：完全删干净
    if (createdTargetId) {
      sql(`DELETE FROM cm_monitor_target_content WHERE target_id=${createdTargetId}`);
      sql(`DELETE FROM cm_collection_run WHERE target_id=${createdTargetId}`);
      sql(`DELETE FROM cm_monitor_target WHERE target_id=${createdTargetId}`);
      log.ok(`已删除 target=${createdTargetId} 及其绑定关系和采集记录`);
    }
    if (createdCreatorId && !creatorPreExisted) {
      sql(`DELETE FROM cm_content_snapshot WHERE content_id IN (SELECT content_id FROM cm_content_post WHERE creator_id=${createdCreatorId})`);
      sql(`DELETE FROM cm_content_post WHERE creator_id=${createdCreatorId}`);
      sql(`DELETE FROM cm_creator_snapshot WHERE creator_id=${createdCreatorId}`);
      sql(`DELETE FROM cm_creator_account WHERE creator_id=${createdCreatorId}`);
      log.ok(`已删除作者及其作品/快照 creator=${createdCreatorId}`);
    }
  } catch (e) {
    log.warn(`清理失败：${e.message}`);
  }
}

(async () => {
  try {
    await main();
    await cleanup();
    process.exit(0);
  } catch (err) {
    console.error(`\n${c.red}${c.bold}❌ 测试失败：${err.message}${c.reset}`);
    if (err.stack && process.env.DEBUG) console.error(err.stack);
    await cleanup();
    process.exit(1);
  }
})();
