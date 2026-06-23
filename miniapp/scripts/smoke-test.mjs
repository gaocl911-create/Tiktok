#!/usr/bin/env node
/**
 * Miniapp 端到端冒烟测试脚本
 *
 * 用法：
 *   node scripts/smoke-test.mjs
 *   API_BASE=http://127.0.0.1:8081/prod-api node scripts/smoke-test.mjs
 *   API_BASE=https://your.host/prod-api MOCK_OPENID=dev_test_001 node scripts/smoke-test.mjs
 *
 * 默认走开发模拟登录（mockOpenid），不需要真实微信 code。
 * 用 SKIP_WRITE=1 跳过会写库的步骤（updateProfile/submitForAudit/claim/submitContent）。
 */

const API_BASE = process.env.API_BASE || "http://127.0.0.1:8081/prod-api";
const MOCK_OPENID = process.env.MOCK_OPENID || `dev_smoke_${Date.now()}`;
const CLIENT_ID = "ba9e8a5f68fd1436043780186727e92f";
const SKIP_WRITE = process.env.SKIP_WRITE === "1";

let token = "";
const stats = { pass: 0, fail: 0, skip: 0 };
const failures = [];

const COLOR = {
  reset: "\x1b[0m", red: "\x1b[31m", green: "\x1b[32m",
  yellow: "\x1b[33m", cyan: "\x1b[36m", gray: "\x1b[90m",
};

function log(level, msg) {
  const tag = { info: COLOR.cyan + "INFO " + COLOR.reset,
                pass: COLOR.green + "PASS " + COLOR.reset,
                fail: COLOR.red + "FAIL " + COLOR.reset,
                skip: COLOR.yellow + "SKIP " + COLOR.reset,
                dim:  COLOR.gray + "     " + COLOR.reset }[level];
  console.log(`${tag}${msg}`);
}

async function call(method, path, { body, allowBizCode = null } = {}) {
  // allowBizCode: null = 任意非 200 都报错；数字/数组 = 允许这些业务码原样返回。
  const url = `${API_BASE}${path}`;
  const headers = {
    "Content-Type": "application/json",
    clientid: CLIENT_ID,
  };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const init = { method, headers };
  if (body !== undefined) init.body = JSON.stringify(body);

  let res, text, parsed;
  try {
    res = await fetch(url, init);
    text = await res.text();
  } catch (err) {
    throw new Error(`network error: ${err.message}`);
  }

  try { parsed = text ? JSON.parse(text) : {}; }
  catch { throw new Error(`non-JSON response (${res.status}): ${text.slice(0, 200)}`); }

  const allowed = allowBizCode == null ? [] :
                  Array.isArray(allowBizCode) ? allowBizCode : [allowBizCode];

  if (res.status === 401 && allowed.includes(401)) return parsed;
  if (res.status < 200 || res.status >= 300) {
    throw new Error(`HTTP ${res.status} ${parsed?.msg || text.slice(0, 200)}`);
  }
  if (parsed.code && parsed.code !== 200) {
    if (allowed.includes(parsed.code)) return parsed;
    throw new Error(`biz code ${parsed.code}: ${parsed.msg || "no msg"}`);
  }
  return parsed;
}

async function step(name, fn, { writeOp = false } = {}) {
  if (writeOp && SKIP_WRITE) {
    log("skip", name + "  (SKIP_WRITE=1)");
    stats.skip++; return null;
  }
  try {
    const result = await fn();
    log("pass", name);
    return result;
  } catch (err) {
    stats.fail++;
    failures.push({ name, msg: err.message });
    log("fail", `${name}  →  ${err.message}`);
    return null;
  }
}

function check(label, cond, detail = "") {
  if (cond) { stats.pass++; log("dim", `  ✓ ${label}`); }
  else { stats.fail++; failures.push({ name: label, msg: detail || "assertion failed" });
         log("dim", `  ${COLOR.red}✗ ${label}${detail ? ` — ${detail}` : ""}${COLOR.reset}`); }
}

// ---------------------------------------------------------------- main ----

(async () => {
  console.log(`\n${COLOR.cyan}Miniapp smoke test${COLOR.reset}`);
  console.log(`  API_BASE    : ${API_BASE}`);
  console.log(`  MOCK_OPENID : ${MOCK_OPENID}`);
  console.log(`  SKIP_WRITE  : ${SKIP_WRITE}\n`);

  // 0. 未登录访问受保护接口应返回 401（后端用 HTTP 200 + body.code=401 表达）
  await step("0. 未登录访问 /miniapp/user/profile 应返回 401", async () => {
    const r = await call("GET", "/miniapp/user/profile", { allowBizCode: 401 });
    check("业务码为 401", r.code === 401, `got code=${r.code} msg=${r.msg}`);
  });

  // 1. 模拟登录
  const login = await step("1. 模拟登录 POST /miniapp/auth/login", async () => {
    const r = await call("POST", "/miniapp/auth/login", {
      body: { mockOpenid: MOCK_OPENID },
      // 生产环境未启用 mock 时返回 500 + "请先配置微信小程序 AppID 和 AppSecret"，属预期
      allowBizCode: 500,
    });
    if (r.code === 500) {
      check("拒绝文案为配置缺失（仅运行检查，不影响其他覆盖）",
            /AppID|AppSecret|mock|配置/.test(r.msg || ""),
            `msg=${r.msg}`);
      return null;
    }
    check("data.access_token 存在", typeof r.data?.access_token === "string", `data=${JSON.stringify(r.data)}`);
    check("data.openid 存在",       typeof r.data?.openid === "string");
    check("data.client_id 匹配",     r.data?.client_id === CLIENT_ID, `got ${r.data?.client_id}`);
    return r.data;
  });
  if (!login) {
    log("dim", `  └── 登录不可用，以下 2-8 步跳过 (令牌为空)`);
    stats.skip += 6;
    summary();
    return;
  }
  token = login.access_token;

  // 2. 查询当前 profile
  const profile = await step("2. GET /miniapp/user/profile", async () => {
    const r = await call("GET", "/miniapp/user/profile");
    check("data 是对象", r.data && typeof r.data === "object");
    check("含 onboardingStatus 字段", "onboardingStatus" in (r.data || {}));
    return r.data;
  });

  // 3. 更新 profile
  await step("3. PUT /miniapp/user/profile", async () => {
    const r = await call("PUT", "/miniapp/user/profile", {
      body: {
        realName: "冒烟测试",
        phone: "13800000000",
        wechatId: "smoke_wx",
        region: "测试 测试",
        douyinId: "smoke_dy",
        remark: "smoke-test",
      },
    });
    check("data.realName 写入成功", r.data?.realName === "冒烟测试", `got ${r.data?.realName}`);
  }, { writeOp: true });

  // 4. 提交审核（只在 incomplete/rejected 状态下做）
  const status = profile?.onboardingStatus;
  if (status === "incomplete" || status === "rejected") {
    await step("4. POST /miniapp/user/profile/submit", async () => {
      const r = await call("POST", "/miniapp/user/profile/submit");
      check("status 变为 pending/approved", ["pending", "approved"].includes(r.data?.onboardingStatus),
            `got ${r.data?.onboardingStatus}`);
    }, { writeOp: true });
  } else {
    log("skip", `4. POST /miniapp/user/profile/submit  (current status=${status})`);
    stats.skip++;
  }

  // 5. 任务列表
  const taskPage = await step("5. GET /miniapp/task/list", async () => {
    const r = await call("GET", "/miniapp/task/list?pageNum=1&pageSize=10");
    // TableDataInfo 形态： { code, msg, rows, total }
    check("rows 是数组", Array.isArray(r.rows), `typeof rows=${typeof r.rows}`);
    check("total 是数字", typeof r.total === "number", `typeof total=${typeof r.total}`);
    return r;
  });

  // 6. 我的任务
  const myPage = await step("6. GET /miniapp/task/my", async () => {
    const r = await call("GET", "/miniapp/task/my?pageNum=1&pageSize=10");
    check("rows 是数组", Array.isArray(r.rows));
    return r;
  });

  // 7. 领取任务（任务广场有任务，且未领取过）
  let claim = null;
  if (taskPage?.rows?.length > 0) {
    const candidate = taskPage.rows.find(t =>
      !(myPage?.rows || []).some(c => String(c.taskId) === String(t.taskId))
    );
    if (candidate) {
      claim = await step(`7. POST /miniapp/task/${candidate.taskId}/claim`, async () => {
        // 未通过审核时后端会返回业务码 500 + "兼职入驻审核通过后才能领取任务"，属于预期。
        const r = await call("POST", `/miniapp/task/${candidate.taskId}/claim`,
          { allowBizCode: 500 });
        if (r.code === 500) {
          check("拒领文案为入驻审核相关", /审核/.test(r.msg || ""),
                `msg=${r.msg}（未审核通过时应被拒绝，符合预期）`);
          return null;
        }
        check("data.claimId 存在", r.data?.claimId != null);
        check("data.claimStatus = claimed", r.data?.claimStatus === "claimed",
              `got ${r.data?.claimStatus}`);
        return r.data;
      }, { writeOp: true });
    } else {
      log("skip", "7. claim  (所有任务都已领取过)");
      stats.skip++;
    }
  } else {
    log("skip", "7. claim  (任务广场无可领取任务)");
    stats.skip++;
  }

  // 8. 提交作品
  const submittable = claim || (myPage?.rows || []).find(
    c => c.claimStatus === "claimed" || c.claimStatus === "rejected");
  if (submittable) {
    await step(`8. POST /miniapp/task/claim/${submittable.claimId}/submit-content`, async () => {
      const r = await call("POST", `/miniapp/task/claim/${submittable.claimId}/submit-content`, {
        body: {
          contentUrl: `https://v.douyin.com/smoke-${Date.now()}`,
          contentDesc: "冒烟测试自动提交",
          screenshotUrl: "",
        },
      });
      check("data.submissionStatus 是 pending/approved",
            ["pending", "approved"].includes(r.data?.submissionStatus),
            `got ${r.data?.submissionStatus}`);
    }, { writeOp: true });
  } else {
    log("skip", "8. submit-content  (没有可提交的 claim)");
    stats.skip++;
  }

  summary();
})().catch(err => {
  console.error(`\n${COLOR.red}脚本异常退出${COLOR.reset}: ${err.message}`);
  process.exit(2);
});

function summary() {
  console.log(`\n${COLOR.cyan}=========== 结果 ===========${COLOR.reset}`);
  console.log(`  通过 : ${COLOR.green}${stats.pass}${COLOR.reset}`);
  console.log(`  失败 : ${stats.fail > 0 ? COLOR.red : COLOR.gray}${stats.fail}${COLOR.reset}`);
  console.log(`  跳过 : ${COLOR.yellow}${stats.skip}${COLOR.reset}`);
  if (failures.length) {
    console.log(`\n${COLOR.red}失败明细：${COLOR.reset}`);
    failures.forEach((f, i) => console.log(`  ${i + 1}. ${f.name}\n     ${f.msg}`));
  }
  process.exit(stats.fail > 0 ? 1 : 0);
}
