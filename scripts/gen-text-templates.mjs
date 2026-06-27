#!/usr/bin/env node
/**
 * 生成测试用的 Excel 模板文件，用于在后台「文案库 → 批量导入」按钮里上传。
 *
 * 用法：
 *   node scripts/gen-text-templates.mjs
 *
 * 会在 d:/workspace/TikTok_Platform/scripts/test-templates/ 下生成几个 .xlsx：
 *   1. text-template-basic.xlsx        — 5 条普通文案
 *   2. text-template-large.xlsx        — 30 条文案（压测）
 *   3. text-template-mixed.xlsx        — 含 emoji / 长文 / 启停混合
 *   4. text-template-edge.xlsx         — 边缘情况（空行、特殊字符）
 */

import { mkdirSync, writeFileSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const OUT_DIR = join(__dirname, "test-templates");
mkdirSync(OUT_DIR, { recursive: true });

// ---- 内嵌最小 xlsx 生成器（与 test-features.mjs 相同实现，无外部依赖） ----

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
    uniqueStrings.map((s) => `<si><t xml:space="preserve">${xmlEsc(s)}</t></si>`).join("") +
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
    { name: "[Content_Types].xml",        data: Buffer.from(contentTypes, "utf8") },
    { name: "_rels/.rels",                data: Buffer.from(rootRels, "utf8") },
    { name: "xl/workbook.xml",            data: Buffer.from(workbook, "utf8") },
    { name: "xl/_rels/workbook.xml.rels", data: Buffer.from(workbookRels, "utf8") },
    { name: "xl/sharedStrings.xml",       data: Buffer.from(sharedStrings, "utf8") },
    { name: "xl/worksheets/sheet1.xml",   data: Buffer.from(sheetData, "utf8") },
  ];

  const local = []; const central = []; let offset = 0;
  for (const f of files) {
    const nameBuf = Buffer.from(f.name, "utf8");
    const crc = crc32(f.data);
    const size = f.data.length;
    const lh = Buffer.alloc(30);
    lh.writeUInt32LE(0x04034b50, 0); lh.writeUInt16LE(20, 4); lh.writeUInt16LE(0, 6);
    lh.writeUInt16LE(0, 8); lh.writeUInt16LE(0, 10); lh.writeUInt16LE(0x21, 12);
    lh.writeUInt32LE(crc, 14); lh.writeUInt32LE(size, 18); lh.writeUInt32LE(size, 22);
    lh.writeUInt16LE(nameBuf.length, 26); lh.writeUInt16LE(0, 28);
    local.push(Buffer.concat([lh, nameBuf, f.data]));
    const ch = Buffer.alloc(46);
    ch.writeUInt32LE(0x02014b50, 0); ch.writeUInt16LE(20, 4); ch.writeUInt16LE(20, 6);
    ch.writeUInt16LE(0, 8); ch.writeUInt16LE(0, 10); ch.writeUInt16LE(0, 12); ch.writeUInt16LE(0x21, 14);
    ch.writeUInt32LE(crc, 16); ch.writeUInt32LE(size, 20); ch.writeUInt32LE(size, 24);
    ch.writeUInt16LE(nameBuf.length, 28); ch.writeUInt16LE(0, 30); ch.writeUInt16LE(0, 32);
    ch.writeUInt16LE(0, 34); ch.writeUInt16LE(0, 36); ch.writeUInt32LE(0, 38);
    ch.writeUInt32LE(offset, 42);
    central.push(Buffer.concat([ch, nameBuf]));
    offset += local[local.length - 1].length;
  }
  const localBuf = Buffer.concat(local);
  const centralBuf = Buffer.concat(central);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0); end.writeUInt16LE(0, 4); end.writeUInt16LE(0, 6);
  end.writeUInt16LE(files.length, 8); end.writeUInt16LE(files.length, 10);
  end.writeUInt32LE(centralBuf.length, 12); end.writeUInt32LE(localBuf.length, 16);
  end.writeUInt16LE(0, 20);
  return Buffer.concat([localBuf, centralBuf, end]);
}

// ----------------------------------------------- 模板数据 ----

// 后端 ExcelProperty 注解定义的列头（顺序必须严格一致）
const HEADER = ["文案内容", "排序", "状态（启用/停用）", "备注"];

// ============================ 1. 基础模板 ============================
const basic = [
  ["每天一杯柠檬水，皮肤状态明显改善 🍋 真实分享，姐妹们冲！", 1, "启用", "美妆类"],
  ["熬夜党救星！这款眼霜让我黑眼圈淡了一半 👀", 2, "启用", "美妆类"],
  ["新手化妆教程，5 分钟搞定通勤妆，姐妹们码住", 3, "启用", "美妆类"],
  ["平价好物分享，学生党也能负担得起的护肤品", 4, "启用", "美妆类"],
  ["618 大促必囤清单，亲测有效不踩雷", 5, "启用", "美妆类"],
];

// ============================ 2. 大批量模板（30 条）============================
const large = [];
const themes = [
  "宝藏小店挖到了", "原来这才是正确用法", "亲测有效不踩雷", "强烈推荐这款",
  "超实用小技巧", "新手必看教程", "省钱必备神器", "颜值党狂喜",
  "懒人福音来啦", "用过都说好",
];
const tails = ["快冲！", "码住！", "姐妹们冲鸭", "码住不亏", "真的爱了", "再不知道就晚了"];
for (let i = 0; i < 30; i++) {
  const theme = themes[i % themes.length];
  const tail = tails[i % tails.length];
  large.push([
    `${theme} ${tail} 这个 #推荐好物 测试条目 ${String(i + 1).padStart(2, "0")}`,
    i + 1,
    i === 29 ? "停用" : "启用",   // 最后一条停用，测一下状态字段
    i % 5 === 0 ? "重点款" : "",
  ]);
}

// ============================ 3. 混合内容（emoji / 长文 / 启停）============================
const mixed = [
  ["💄 短文案：粉底液真的绝了 ✨", 1, "启用", "短"],
  [
    "📝 长文案：作为一名敏感肌患者，我用过无数品牌的护肤品都没能解决我的红血丝问题。" +
    "直到上个月朋友推荐了这款修复精华，刚开始我是抱着试试看的心态，没想到用了两周后明显感觉皮肤稳定多了，" +
    "现在已经用了一个月，红血丝淡了非常多，皮肤状态肉眼可见的变好。强烈推荐给同样敏感肌的姐妹们！",
    2, "启用", "长文",
  ],
  ["含特殊字符 & 引号 \" 测试 < 转义 > 是否正常", 3, "启用", "测试转义"],
  ["纯英文 content: This is a sample English text for batch import test.", 4, "启用", "英文"],
  ["这是一条停用的文案，不应该参与分配", 5, "停用", "停用测试"],
  ["数字混搭：2026 年最热门的 5 款单品，第 3 款最让人惊喜", 6, "启用", ""],
];

// ============================ 4. 边缘场景 ============================
const edge = [
  ["第一条正常文案", 1, "启用", "正常"],
  ["", 2, "启用", "这条内容为空，应该被后端忽略"],
  ["第三条正常文案", 3, "启用", "应当被导入"],
  ["   前后都有空格的文案，导入时应该被 trim   ", 4, "启用", "trim 测试"],
  ["不填排序也可以", null, "启用", "排序为空"],
  ["不填状态默认为启用", 6, "", "状态空值"],
  ["最后一条文案：边缘测试完毕", 7, "启用", "结尾"],
];

// ----------------------------------------------- 生成 ----

function write(name, rows) {
  const buf = buildXlsx(HEADER, rows.map((r) => r.map((v) => v == null ? "" : v)));
  const path = join(OUT_DIR, name);
  writeFileSync(path, buf);
  return { path, size: buf.length, rowCount: rows.length };
}

const results = [
  { label: "1. 基础模板（5 条）",      ...write("text-template-basic.xlsx", basic) },
  { label: "2. 大批量模板（30 条）",   ...write("text-template-large.xlsx", large) },
  { label: "3. 混合内容（6 条）",      ...write("text-template-mixed.xlsx", mixed) },
  { label: "4. 边缘场景（7 条，含空）",...write("text-template-edge.xlsx",  edge) },
];

console.log(`\n✅ 已生成 ${results.length} 个模板到：${OUT_DIR}\n`);
results.forEach((r) => {
  console.log(`  ${r.label}`);
  console.log(`    文件：${r.path}`);
  console.log(`    大小：${r.size} 字节  ·  数据行：${r.rowCount}\n`);
});

console.log(`使用方式：
  1. 后台登录 → 兼职任务管理 → 文案库
  2. 先确保有一个「启用」状态的文案分类
  3. 选好分类后，点击「批量导入」按钮（如果有的话）或调用 importData 接口
  4. 选上面任意一个 .xlsx 文件上传
`);
