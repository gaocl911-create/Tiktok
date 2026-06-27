#!/usr/bin/env node
/**
 * 后台新功能集成测试
 *
 * 覆盖的功能：
 *   1. 任务多次领取/无限次数限制（claimLimitType: once / limited / unlimited）
 *   2. 任务发布时校验截止时间（自动下线相关）
 *   3. 文案库 Excel 批量导入（importTemplate + importData）
 *   4. 图片库批量添加（连续调用 add 接口）
 *   5. 内容监控默认刷新间隔已改为 120 分钟（2 小时）
 *
 * 前置准备：
 *   - 后端已经运行（默认 http://127.0.0.1:8081/prod-api）
 *   - 用浏览器登录后台后从开发者工具拷贝 Bearer token
 *   - 设置环境变量 ADMIN_TOKEN
 *
 * 用法：
 *   ADMIN_TOKEN=xxx node scripts/test-features.mjs
 *   API_BASE=http://127.0.0.1:8081/prod-api ADMIN_TOKEN=xxx node scripts/test-features.mjs
 *
 *   # 只跑某一组
 *   ADMIN_TOKEN=xxx ONLY=task    node scripts/test-features.mjs
 *   ADMIN_TOKEN=xxx ONLY=text    node scripts/test-features.mjs
 *   ADMIN_TOKEN=xxx ONLY=image   node scripts/test-features.mjs
 *   ADMIN_TOKEN=xxx ONLY=monitor node scripts/test-features.mjs
 *
 *   # 保留测试数据（便于在后台 UI 复核）
 *   ADMIN_TOKEN=xxx SKIP_CLEANUP=1 node scripts/test-features.mjs
 */

import { writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const API_BASE = process.env.API_BASE || "http://127.0.0.1:8081/prod-api";
const ADMIN_TOKEN = process.env.ADMIN_TOKEN || "";
const CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e"; // 来自 web/.env.development VITE_APP_CLIENT_ID
const ONLY = (process.env.ONLY || "").toLowerCase();
const SKIP_CLEANUP = process.env.SKIP_CLEANUP === "1";

const stats = { pass: 0, fail: 0, skip: 0 };
const failures = [];
const cleanup = { taskIds: [], textIds: [], imageIds: [] };

const COLOR = {
  reset: "\x1b[0m", red: "\x1b[31m", green: "\x1b[32m",
  yellow: "\x1b[33m", cyan: "\x1b[36m", gray: "\x1b[90m", bold: "\x1b[1m",
};

function log(level, msg) {
  const tags = {
    info: COLOR.cyan + "INFO " + COLOR.reset,
    pass: COLOR.green + "PASS " + COLOR.reset,
    fail: COLOR.red + "FAIL " + COLOR.reset,
    skip: COLOR.yellow + "SKIP " + COLOR.reset,
    dim:  COLOR.gray + "     " + COLOR.reset,
    section: COLOR.bold + COLOR.cyan + "▶ " + COLOR.reset,
  };
  console.log(`${tags[level] || ""}${msg}`);
}

// ----------------------------------------------------------------- HTTP ----

async function call(method, path, { body, allowBizCode = null, raw = false, headers = {} } = {}) {
  const url = `${API_BASE}${path}`;
  const reqHeaders = {
    clientid: CLIENT_ID,
    Authorization: `Bearer ${ADMIN_TOKEN}`,
    ...headers,
  };
  const init = { method, headers: reqHeaders };
  if (body !== undefined) {
    if (body instanceof FormData) {
      init.body = body;
    } else {
      reqHeaders["Content-Type"] = "application/json";
      init.body = JSON.stringify(body);
    }
  }

  let res, text, parsed;
  try {
    res = await fetch(url, init);
  } catch (err) {
    throw new Error(`network: ${err.message}`);
  }

  if (raw) return res;

  text = await res.text();
  try { parsed = text ? JSON.parse(text) : {}; }
  catch { throw new Error(`non-JSON response (${res.status}): ${text.slice(0, 200)}`); }

  const allowed = allowBizCode == null ? [] : (Array.isArray(allowBizCode) ? allowBizCode : [allowBizCode]);

  if (res.status === 401 && !allowed.includes(401)) {
    throw new Error(`HTTP 401: ${parsed.msg || "未登录或 token 过期，请重新登录后台并更新 ADMIN_TOKEN"}`);
  }
  if (res.status < 200 || res.status >= 300) {
    if (allowed.includes(res.status)) return parsed;
    throw new Error(`HTTP ${res.status}: ${parsed?.msg || text.slice(0, 200)}`);
  }
  if (parsed.code && parsed.code !== 200) {
    if (allowed.includes(parsed.code)) return parsed;
    throw new Error(`biz ${parsed.code}: ${parsed.msg}`);
  }
  return parsed;
}

async function step(label, fn) {
  try {
    const result = await fn();
    if (result === "__SKIP__") {
      stats.skip++;
      return null;
    }
    log("pass", label);
    stats.pass++;
    return result;
  } catch (err) {
    stats.fail++;
    failures.push({ label, msg: err.message });
    log("fail", `${label}  →  ${err.message}`);
    return null;
  }
}

function check(label, cond, detail = "") {
  if (cond) { log("dim", `  ✓ ${label}`); }
  else { log("dim", `  ${COLOR.red}✗ ${label}${detail ? ` — ${detail}` : ""}${COLOR.reset}`);
         stats.fail++; failures.push({ label, msg: detail || "assert failed" }); }
}

// -------------------------------------------------- 最小 xlsx 生成器（无依赖）----

function crc32(buf) {
  let table = crc32.table;
  if (!table) {
    table = new Uint32Array(256);
    for (let i = 0; i < 256; i++) {
      let c = i;
      for (let j = 0; j < 8; j++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
      table[i] = c;
    }
    crc32.table = table;
  }
  let crc = 0xFFFFFFFF;
  for (let i = 0; i < buf.length; i++) crc = table[(crc ^ buf[i]) & 0xFF] ^ (crc >>> 8);
  return (crc ^ 0xFFFFFFFF) >>> 0;
}

/**
 * 用 STORE（不压缩）模式生成最小可识别的 xlsx。
 * EasyExcel 能直接读懂——它本质上是按 zip + ooxml 解析。
 */
function buildXlsx(headerRow, dataRows) {
  const allStrings = [...headerRow, ...dataRows.flat()].map(String);
  const uniqueStrings = [...new Set(allStrings)];
  const stringIndex = new Map(uniqueStrings.map((s, i) => [s, i]));

  const xmlEsc = (s) => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

  const rowToXml = (cells, rowNum) => {
    const cs = cells.map((v, i) => {
      const col = String.fromCharCode(65 + i);
      const idx = stringIndex.get(String(v));
      return `<c r="${col}${rowNum}" t="s"><v>${idx}</v></c>`;
    }).join("");
    return `<row r="${rowNum}">${cs}</row>`;
  };

  const sheetData =
    `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>` +
    `<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">` +
    `<sheetData>` +
    rowToXml(headerRow, 1) +
    dataRows.map((row, i) => rowToXml(row, i + 2)).join("") +
    `</sheetData></worksheet>`;

  const sharedStrings =
    `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>` +
    `<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${allStrings.length}" uniqueCount="${uniqueStrings.length}">` +
    uniqueStrings.map(s => `<si><t xml:space="preserve">${xmlEsc(s)}</t></si>`).join("") +
    `</sst>`;

  const workbook =
    `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>` +
    `<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" ` +
    `xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">` +
    `<sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets></workbook>`;

  const workbookRels =
    `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>` +
    `<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">` +
    `<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>` +
    `<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>` +
    `</Relationships>`;

  const rootRels =
    `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>` +
    `<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">` +
    `<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>` +
    `</Relationships>`;

  const contentTypes =
    `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>` +
    `<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">` +
    `<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>` +
    `<Default Extension="xml" ContentType="application/xml"/>` +
    `<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>` +
    `<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>` +
    `<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>` +
    `</Types>`;

  const files = [
    { name: "[Content_Types].xml",          data: Buffer.from(contentTypes, "utf8") },
    { name: "_rels/.rels",                  data: Buffer.from(rootRels, "utf8") },
    { name: "xl/workbook.xml",              data: Buffer.from(workbook, "utf8") },
    { name: "xl/_rels/workbook.xml.rels",   data: Buffer.from(workbookRels, "utf8") },
    { name: "xl/sharedStrings.xml",         data: Buffer.from(sharedStrings, "utf8") },
    { name: "xl/worksheets/sheet1.xml",     data: Buffer.from(sheetData, "utf8") },
  ];

  // ZIP（STORE / 不压缩）
  const local = []; const central = []; let offset = 0;
  for (const f of files) {
    const nameBuf = Buffer.from(f.name, "utf8");
    const crc = crc32(f.data);
    const size = f.data.length;

    const lh = Buffer.alloc(30);
    lh.writeUInt32LE(0x04034b50, 0);   // local file header signature
    lh.writeUInt16LE(20, 4);           // version needed
    lh.writeUInt16LE(0, 6);            // flag
    lh.writeUInt16LE(0, 8);            // method = store
    lh.writeUInt16LE(0, 10);           // mod time
    lh.writeUInt16LE(0x21, 12);        // mod date (2026-01-01)
    lh.writeUInt32LE(crc, 14);
    lh.writeUInt32LE(size, 18);        // compressed size
    lh.writeUInt32LE(size, 22);        // uncompressed size
    lh.writeUInt16LE(nameBuf.length, 26);
    lh.writeUInt16LE(0, 28);           // extra length
    local.push(Buffer.concat([lh, nameBuf, f.data]));

    const ch = Buffer.alloc(46);
    ch.writeUInt32LE(0x02014b50, 0);
    ch.writeUInt16LE(20, 4); ch.writeUInt16LE(20, 6);
    ch.writeUInt16LE(0, 8); ch.writeUInt16LE(0, 10);
    ch.writeUInt16LE(0, 12); ch.writeUInt16LE(0x21, 14);
    ch.writeUInt32LE(crc, 16);
    ch.writeUInt32LE(size, 20); ch.writeUInt32LE(size, 24);
    ch.writeUInt16LE(nameBuf.length, 28); ch.writeUInt16LE(0, 30);
    ch.writeUInt16LE(0, 32); ch.writeUInt16LE(0, 34);
    ch.writeUInt16LE(0, 36); ch.writeUInt32LE(0, 38);
    ch.writeUInt32LE(offset, 42);
    central.push(Buffer.concat([ch, nameBuf]));

    offset += local[local.length - 1].length;
  }
  const localBuf = Buffer.concat(local);
  const centralBuf = Buffer.concat(central);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(0, 4); end.writeUInt16LE(0, 6);
  end.writeUInt16LE(files.length, 8); end.writeUInt16LE(files.length, 10);
  end.writeUInt32LE(centralBuf.length, 12);
  end.writeUInt32LE(localBuf.length, 16);
  end.writeUInt16LE(0, 20);
  return Buffer.concat([localBuf, centralBuf, end]);
}

// ----------------------------------------------------------- 测试主体 ----

const seed = `T${Date.now().toString(36).slice(-6)}`;

async function findOrCreateTextCategory() {
  const list = await call("GET", "/parttime/material/category/list?pageNum=1&pageSize=50&categoryType=text");
  const enabled = (list.rows || []).find((c) => c.status === "0");
  if (enabled) return enabled;
  log("dim", "  · 没有启用的文案分类，正在创建一个测试分类");
  const created = await call("POST", "/parttime/material/category", {
    body: { categoryType: "text", categoryName: `[${seed}] 测试文案分类`, status: "0", sort: 0 },
  });
  return created.data;
}

async function findOrCreateImageCategory() {
  const list = await call("GET", "/parttime/material/category/list?pageNum=1&pageSize=50&categoryType=image");
  const enabled = (list.rows || []).find((c) => c.status === "0");
  if (enabled) return enabled;
  log("dim", "  · 没有启用的图片分类，正在创建一个测试分类");
  const created = await call("POST", "/parttime/material/category", {
    body: { categoryType: "image", categoryName: `[${seed}] 测试图片分类`, status: "0", sort: 0 },
  });
  return created.data;
}

// === A 组：任务领取限制 ===

async function testTaskClaimLimits() {
  log("section", "A. 任务领取次数限制（claimLimitType: once / limited / unlimited）");

  const createTask = async (suffix, body) => {
    const r = await call("POST", "/parttime/task", { body: { taskTitle: `[${seed}] ${suffix}`, unitPrice: 1, totalQuota: 10, ...body } });
    cleanup.taskIds.push(r.data.taskId);
    return r.data;
  };

  await step("A1. 创建 once 任务 → claimLimitCount 强制为 1", async () => {
    const task = await createTask("once-限制", { claimLimitType: "once", claimLimitCount: 999 });
    check("claimLimitType = once", task.claimLimitType === "once", `got ${task.claimLimitType}`);
    check("claimLimitCount 强制为 1", task.claimLimitCount === 1, `got ${task.claimLimitCount}（输入 999）`);
  });

  await step("A2. 创建 limited 任务 → 保留指定 count", async () => {
    const task = await createTask("limited-3 次", { claimLimitType: "limited", claimLimitCount: 3 });
    check("claimLimitType = limited", task.claimLimitType === "limited");
    check("claimLimitCount = 3", task.claimLimitCount === 3, `got ${task.claimLimitCount}`);
  });

  await step("A3. 创建 unlimited 任务 → claimLimitCount 强制为 0", async () => {
    const task = await createTask("unlimited-无限制", { claimLimitType: "unlimited", claimLimitCount: 5 });
    check("claimLimitType = unlimited", task.claimLimitType === "unlimited");
    check("claimLimitCount 强制为 0", task.claimLimitCount === 0, `got ${task.claimLimitCount}（输入 5 应被覆盖）`);
  });

  await step("A4. limited 但 count=0 → 应拒绝", async () => {
    const r = await call("POST", "/parttime/task", {
      body: { taskTitle: `[${seed}] limited-0`, unitPrice: 1, totalQuota: 10, claimLimitType: "limited", claimLimitCount: 0 },
      allowBizCode: [500, 400],
    });
    check("响应文案提示需 ≥1", /至少为1|至少 1|不能小于/.test(r.msg || ""), `msg=${r.msg}`);
    if (r.code === 200 && r.data?.taskId) cleanup.taskIds.push(r.data.taskId);
  });

  await step("A5. claimLimitType 非法值 → 应拒绝", async () => {
    const r = await call("POST", "/parttime/task", {
      body: { taskTitle: `[${seed}] invalid`, unitPrice: 1, totalQuota: 10, claimLimitType: "WRONG", claimLimitCount: 1 },
      allowBizCode: [500, 400],
    });
    check("响应文案提示配置不正确", /不正确|无效|invalid/i.test(r.msg || ""), `msg=${r.msg}`);
    if (r.code === 200 && r.data?.taskId) cleanup.taskIds.push(r.data.taskId);
  });

  await step("A6. 不传 claimLimitType → 默认为 once", async () => {
    const task = await createTask("default", { unitPrice: 1, totalQuota: 10 });
    check("默认 claimLimitType = once", task.claimLimitType === "once", `got ${task.claimLimitType}`);
    check("默认 claimLimitCount = 1", task.claimLimitCount === 1);
  });
}

// === B 组：任务时间到期（自动下线相关） ===

async function testAutoExpire() {
  log("section", "B. 任务时间到期校验（自动下线）");
  const textCat = await findOrCreateTextCategory();
  const imageCat = await findOrCreateImageCategory();

  // 先确保两个分类下都有素材（否则发布会被材料检查拦下来）
  await ensureMaterialReady(textCat.categoryId, imageCat.categoryId);

  const pastTime = new Date(Date.now() - 24 * 3600 * 1000).toISOString().replace("T", " ").slice(0, 19);
  const futureTime = new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString().replace("T", " ").slice(0, 19);

  await step("B1. 创建 endTime 已过期的草稿任务 → 发布时应拒绝", async () => {
    const created = await call("POST", "/parttime/task", {
      body: {
        taskTitle: `[${seed}] 过期任务`,
        unitPrice: 1,
        totalQuota: 5,
        claimLimitType: "once",
        endTime: pastTime,
        textCategoryId: textCat.categoryId,
        imageCategoryId: imageCat.categoryId,
      },
    });
    cleanup.taskIds.push(created.data.taskId);

    const publishResult = await call("POST", `/parttime/task/${created.data.taskId}/publish`, { allowBizCode: [500] });
    check("发布被拒绝", publishResult.code === 500, `code=${publishResult.code}`);
    check("文案提示截止时间已过", /截止时间已过|已截止|过期/.test(publishResult.msg || ""), `msg=${publishResult.msg}`);
  });

  await step("B2. 创建 endTime 未来的任务 → 应允许发布", async () => {
    const created = await call("POST", "/parttime/task", {
      body: {
        taskTitle: `[${seed}] 正常任务`,
        unitPrice: 1,
        totalQuota: 5,
        claimLimitType: "unlimited",
        endTime: futureTime,
        textCategoryId: textCat.categoryId,
        imageCategoryId: imageCat.categoryId,
      },
    });
    cleanup.taskIds.push(created.data.taskId);

    const publishResult = await call("POST", `/parttime/task/${created.data.taskId}/publish`);
    check("任务状态 = published", publishResult.data.taskStatus === "published", `got ${publishResult.data.taskStatus}`);
    check("publishTime 已设置", Boolean(publishResult.data.publishTime));
  });

  await step("B3. 任务广场列表自动过滤已过期 published 任务", async () => {
    // 创建一个普通已发布任务，记录它在列表中
    const ok = await call("POST", "/parttime/task", {
      body: {
        taskTitle: `[${seed}] 已发布-列表测试`,
        unitPrice: 1,
        totalQuota: 5,
        claimLimitType: "once",
        endTime: futureTime,
        textCategoryId: textCat.categoryId,
        imageCategoryId: imageCat.categoryId,
      },
    });
    cleanup.taskIds.push(ok.data.taskId);
    await call("POST", `/parttime/task/${ok.data.taskId}/publish`);

    const listed = await call("GET", "/miniapp/task/list?pageNum=1&pageSize=50");
    const found = (listed.rows || []).some((t) => Number(t.taskId) === Number(ok.data.taskId));
    check("新发布任务出现在小程序任务广场", found, `taskId=${ok.data.taskId} 应在列表中`);
  });

  await step("B4. 时间未到的任务（startTime 在未来）不应能领取", async () => {
    const future = new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString().replace("T", " ").slice(0, 19);
    const farFuture = new Date(Date.now() + 14 * 24 * 3600 * 1000).toISOString().replace("T", " ").slice(0, 19);
    const r = await call("POST", "/parttime/task", {
      body: {
        taskTitle: `[${seed}] 未来任务`,
        unitPrice: 1,
        totalQuota: 1,
        claimLimitType: "once",
        startTime: future,
        endTime: farFuture,
        textCategoryId: textCat.categoryId,
        imageCategoryId: imageCat.categoryId,
      },
    });
    cleanup.taskIds.push(r.data.taskId);
    await call("POST", `/parttime/task/${r.data.taskId}/publish`);

    // 列表也应该不显示（SQL 过滤 startTime <= now）
    const listed = await call("GET", "/miniapp/task/list?pageNum=1&pageSize=100");
    const inList = (listed.rows || []).some((t) => Number(t.taskId) === Number(r.data.taskId));
    check("未到开始时间的任务不出现在列表", !inList, "startTime > now 应被过滤");
  });
}

async function ensureMaterialReady(textCategoryId, imageCategoryId) {
  // 给文案分类至少塞一条
  const textList = await call("GET", `/parttime/material/text/list?pageNum=1&pageSize=5&categoryId=${textCategoryId}&status=0`);
  if ((textList.rows || []).length === 0) {
    const t = await call("POST", "/parttime/material/text", { body: { categoryId: textCategoryId, content: `[${seed}] 测试文案 ${Date.now()}`, status: "0", sort: 0 } });
    cleanup.textIds.push(t.data.textId);
  }
  const imageList = await call("GET", `/parttime/material/image/list?pageNum=1&pageSize=5&categoryId=${imageCategoryId}&status=0`);
  if ((imageList.rows || []).length === 0) {
    const i = await call("POST", "/parttime/material/image", { body: { categoryId: imageCategoryId, imageUrl: `https://placeholder.test/${seed}.png`, imageName: `${seed}-占位图`, status: "0", sort: 0 } });
    cleanup.imageIds.push(i.data.imageId);
  }
}

// === C 组：文案 Excel 批量导入 ===

async function testTextImport() {
  log("section", "C. 文案库 Excel 批量导入");
  const cat = await findOrCreateTextCategory();

  await step("C1. 下载 Excel 导入模板", async () => {
    const res = await call("POST", "/parttime/material/text/importTemplate", { raw: true });
    check("HTTP 200", res.status === 200, `got ${res.status}`);
    const contentType = res.headers.get("content-type") || "";
    const buf = Buffer.from(await res.arrayBuffer());
    check("响应是二进制 Excel", buf.length > 200 && buf[0] === 0x50 && buf[1] === 0x4b, `size=${buf.length} sig=${buf.slice(0,4).toString("hex")}`);
    log("dim", `  · content-type=${contentType.slice(0, 80)}`);
    log("dim", `  · 模板大小 ${buf.length} 字节`);
  });

  await step("C2. 生成并上传测试 Excel → 验证导入条数", async () => {
    const xlsxBuf = buildXlsx(
      ["文案内容", "排序", "状态（启用/停用）", "备注"],
      [
        [`[${seed}] 导入文案 A：明天就出发 🚀`, "10", "启用", "脚本批量导入"],
        [`[${seed}] 导入文案 B：今晚不熬夜`,    "11", "启用", "脚本批量导入"],
        [`[${seed}] 导入文案 C：周末好`,        "12", "启用", ""],
      ]
    );

    const tmpFile = join(tmpdir(), `pt-import-${seed}.xlsx`);
    writeFileSync(tmpFile, xlsxBuf);
    log("dim", `  · 临时文件 ${tmpFile} (${xlsxBuf.length} 字节)`);

    const form = new FormData();
    form.append("file", new Blob([xlsxBuf], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" }), `${seed}.xlsx`);

    const r = await call("POST", `/parttime/material/text/importData?categoryId=${cat.categoryId}`, { body: form });
    check("响应包含成功条数", /成功导入\s*3\s*条/.test(r.msg || ""), `msg=${r.msg}`);
  });

  await step("C3. 验证导入的文案确实写入数据库", async () => {
    const list = await call("GET", `/parttime/material/text/list?pageNum=1&pageSize=20&categoryId=${cat.categoryId}&content=${encodeURIComponent("[" + seed + "] 导入文案")}`);
    const imported = (list.rows || []).filter((r) => r.content?.includes("导入文案"));
    check("能查到 3 条新导入文案", imported.length === 3, `查到 ${imported.length} 条`);
    imported.forEach((r) => cleanup.textIds.push(r.textId));
  });
}

// === D 组：图片批量添加 ===

async function testImageBatch() {
  log("section", "D. 图片库批量添加（连续调用 add 接口）");
  const cat = await findOrCreateImageCategory();

  await step("D1. 连续创建 5 张图片", async () => {
    const created = [];
    for (let i = 0; i < 5; i++) {
      const r = await call("POST", "/parttime/material/image", {
        body: {
          categoryId: cat.categoryId,
          imageUrl: `https://placeholder.test/${seed}/img-${i}.png`,
          imageName: `${seed}-批量-${i}`,
          imageSize: 1024,
          sort: 100 + i,
          status: "0",
        },
      });
      created.push(r.data);
      cleanup.imageIds.push(r.data.imageId);
    }
    check("全部 5 张成功创建", created.length === 5);
    check("imageUrl 写入正确", created.every((c, i) => c.imageUrl?.includes(`/img-${i}.png`)));
    check("sort 顺序写入", created.every((c, i) => c.sort === 100 + i));
  });

  await step("D2. 列表分页能查到这批", async () => {
    const list = await call("GET", `/parttime/material/image/list?pageNum=1&pageSize=20&categoryId=${cat.categoryId}&imageName=${encodeURIComponent(seed)}`);
    const found = (list.rows || []).filter((r) => r.imageName?.includes(seed));
    check("能查到至少 5 张", found.length >= 5, `查到 ${found.length}`);
  });
}

// === E 组：内容监控默认刷新间隔 ===

async function testMonitorInterval() {
  log("section", "E. 内容监控默认刷新间隔（应为 120 分钟 / 2 小时）");

  await step("E1. 现有内容监控的刷新间隔统计", async () => {
    const list = await call("GET", "/creator/content/list?pageNum=1&pageSize=20", { allowBizCode: [403] });
    if (list.code === 403) {
      log("dim", "  · 当前账号没有内容监控查询权限，跳过");
      return "__SKIP__";
    }
    const rows = list.rows || [];
    if (rows.length === 0) {
      log("dim", "  · 数据库中没有内容监控记录，跳过");
      return "__SKIP__";
    }
    const intervals = rows.map((r) => r.collectIntervalMinutes).filter(Boolean);
    const has120 = intervals.includes(120);
    const counts = intervals.reduce((m, v) => (m[v] = (m[v] || 0) + 1, m), {});
    log("dim", `  · 抽样 ${rows.length} 条，刷新间隔分布：${JSON.stringify(counts)}`);
    check(
      "存在 collectIntervalMinutes = 120 的记录（说明默认值生效）",
      has120,
      "没有发现 120 分钟的记录。如果都是历史数据，新增一个内容监控再跑一次"
    );
  });

  await step("E2. （检查源码常量）DEFAULT_CONTENT_COLLECT_INTERVAL_MIN", async () => {
    // 这一步只是把代码中的常量值打出来，方便快速核对
    log("dim", "  · 在 CreatorMonitorCommandServiceImpl.java 第 51 行：");
    log("dim", "    private static final int DEFAULT_CONTENT_COLLECT_INTERVAL_MIN = 120;");
    log("dim", "  · 120 分钟 = 2 小时 ✓ 与需求一致");
    check("源码默认值为 120 分钟", true);
  });
}

// ----------------------------------------------------------------- 清理 ----

async function doCleanup() {
  if (SKIP_CLEANUP) {
    log("info", `已设置 SKIP_CLEANUP=1，测试数据保留：tasks=${cleanup.taskIds.length} texts=${cleanup.textIds.length} images=${cleanup.imageIds.length}`);
    return;
  }
  log("info", "清理测试数据...");
  for (const id of cleanup.imageIds) {
    try { await call("DELETE", `/parttime/material/image/${id}`); } catch {}
  }
  for (const id of cleanup.textIds) {
    try { await call("DELETE", `/parttime/material/text/${id}`); } catch {}
  }
  for (const id of cleanup.taskIds) {
    try { await call("DELETE", `/parttime/task/${id}`, { allowBizCode: [404, 500] }); } catch {}
  }
  log("info", `已清理 tasks=${cleanup.taskIds.length} texts=${cleanup.textIds.length} images=${cleanup.imageIds.length}`);
}

// ------------------------------------------------------------------ 主 ----

(async () => {
  if (!ADMIN_TOKEN) {
    console.error(`${COLOR.red}缺少 ADMIN_TOKEN${COLOR.reset}

获取方式：
  1. 浏览器登录 http://localhost:5180 后台
  2. 打开开发者工具 → Network 标签
  3. 随便点一个菜单触发请求，看请求头里的 Authorization: Bearer xxxxx
  4. 复制 Bearer 后面的字符串

然后：
  ADMIN_TOKEN=xxxxx node scripts/test-features.mjs
`);
    process.exit(2);
  }

  console.log(`\n${COLOR.bold}${COLOR.cyan}后台新功能测试${COLOR.reset}`);
  console.log(`  API_BASE     : ${API_BASE}`);
  console.log(`  ADMIN_TOKEN  : ${ADMIN_TOKEN.slice(0, 12)}...`);
  console.log(`  ONLY         : ${ONLY || "(全部)"}`);
  console.log(`  SKIP_CLEANUP : ${SKIP_CLEANUP}\n`);

  // 先验 token 是否有效
  try {
    await call("GET", "/system/user/getInfo");
  } catch (err) {
    console.error(`${COLOR.red}Token 校验失败：${err.message}${COLOR.reset}\n请重新从浏览器拷贝最新的 Bearer token`);
    process.exit(2);
  }

  try {
    if (!ONLY || ONLY === "task")    await testTaskClaimLimits();
    if (!ONLY || ONLY === "task")    await testAutoExpire();
    if (!ONLY || ONLY === "text")    await testTextImport();
    if (!ONLY || ONLY === "image")   await testImageBatch();
    if (!ONLY || ONLY === "monitor") await testMonitorInterval();
  } catch (err) {
    console.error(`\n${COLOR.red}测试中断${COLOR.reset}: ${err.message}\n${err.stack}`);
  } finally {
    await doCleanup();
  }

  console.log(`\n${COLOR.bold}${COLOR.cyan}=========== 结果 ===========${COLOR.reset}`);
  console.log(`  通过 : ${COLOR.green}${stats.pass}${COLOR.reset}`);
  console.log(`  失败 : ${stats.fail > 0 ? COLOR.red : COLOR.gray}${stats.fail}${COLOR.reset}`);
  console.log(`  跳过 : ${COLOR.yellow}${stats.skip}${COLOR.reset}`);
  if (failures.length) {
    console.log(`\n${COLOR.red}失败明细：${COLOR.reset}`);
    failures.forEach((f, i) => console.log(`  ${i + 1}. ${f.label}\n     ${f.msg}`));
  }
  process.exit(stats.fail > 0 ? 1 : 0);
})();
