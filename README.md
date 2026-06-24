# TikTok Platform

基于 **RuoYi-Vue-Plus v5.6.1** 重构的创作者作品监测与兼职任务管理平台。

## 项目定位

系统以"作品监测"为核心，服务于两类场景：

1. **抖音监测** — 添加作者/作品链接 → TikHub 采集指标 → 快照趋势 → 自定义预警 → 监控取消与数据隔离
2. **兼职任务** — 管理员发布推广任务 → 小程序用户领取 → 提交作品 → 后台审核 → 佣金结算

**当前阶段状态**：见 Obsidian 项目笔记（`C:\Users\admin\Documents\Obsidian Vault\抖音监控\TikTok Platform 项目\`）的 `01-当前进度.md`、`06-下一步行动清单.md`。

---

## 快速导航

| 如果你要…… | 目标文件 |
|---|---|
| 了解业务全貌 | [PRD](docs/JAVA_RUOYI_REBUILD_PRD.md) |
| 本地启动开发环境 | [本地开发](#本地开发) |
| 查看小程序端代码 | [miniapp/](miniapp/) |
| 运行冒烟测试 | `node miniapp/scripts/smoke-test.mjs` |
| 查看实施路线图 | [实施计划](docs/JAVA_RUOYI_IMPLEMENTATION_PLAN.md) |
| 了解素材库方案 | [素材库PRD](docs/PART_TIME_MATERIAL_LIBRARY_PRD.md) |
| 查看实时项目进度 | Obsidian 项目笔记（项目状态、决策记录、行动清单） |

---

## 架构概览

```text
┌──────────────────────────────────────────────────────┐
│  管理后台 (plus-ui / Vue 3 / Element Plus)           │
│  port 5180                                           │
└─────────────────┬────────────────────────────────────┘
                  │ /prod-api/
┌─────────────────▼────────────────────────────────────┐
│  API Server (Java 17 / Spring Boot 3 / Sa-Token)     │
│  port 8088 / docker port 8088                        │
├──────────────────────────────────────────────────────┤
│  ┌─ creator 模块 ─────────────────────────────────┐  │
│  │ 作者监测 · 作品监测 · 指标采集 · 预警 · 兼职   │  │
│  └────────────────────────────────────────────────┘  │
│  ┌─ system 模块 ──────────────────────────────────┐  │
│  │ 用户 · 角色 · 部门 · 租户 · 微信小程序登录    │  │
│  └────────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────┤
│  SnailJob (分布式调度)     MySQL 8     Redis 7       │
│  port 18800 / gRPC 28088   port 13307   port 16380   │
└──────────────────────────────────────────────────────┘
        ▲                            ▲
        │                            │
┌───────▼──────┐         ┌───────────▼──────────┐
│  微信小程序   │         │  TikHub API          │
│  UniApp / Vue │         │  抖音真实公开数据     │
└───────────────┘         └──────────────────────┘
```

---

## 项目结构

```text
├─ server/                    RuoYi-Vue-Plus Java 后端
│  └─ ruoyi-modules/ruoyi-creator    创作者监测 + 兼职任务模块
├─ web/                       plus-ui 管理后台（Vue 3）
├─ miniapp/                   微信小程序（UniApp / Vue 3 / Wot UI）
│  ├─ src/pages/             登录、首页、任务广场、作品、个人中心
│  ├─ src/api/               REST 接口封装
│  ├─ src/stores/auth.ts     Pinia 登录态管理
│  ├─ src/utils/request.ts   请求封装（401 自动处理 + 事件广播）
│  └─ scripts/smoke-test.mjs 端到端冒烟测试
├─ deploy/                    Docker Compose + Nginx + Dockerfile
│  ├─ docker-compose.prod.yml 生产集群
│  ├─ docker-compose.dev.yml  开发集群
│  └─ nginx.conf              Web 反代配置
├─ docs/                      产品与设计文档
│  ├─ JAVA_RUOYI_REBUILD_PRD.md                       产品需求
│  ├─ JAVA_RUOYI_IMPLEMENTATION_PLAN.md               实施计划
│  ├─ PART_TIME_TASK_PHASE_PLAN.md                    兼职阶段方案
│  ├─ PART_TIME_MATERIAL_LIBRARY_PRD.md               素材库 PRD
│  ├─ PART_TIME_MATERIAL_LIBRARY_IMPLEMENTATION_PLAN.md 素材库实施
│  └─ DATABASE_DESIGN.md                              数据模型
├─ sql/                       业务表迁移 SQL
├─ scripts/                   本地开发脚本
│  ├─ start-dev.ps1           一键启动开发环境
│  └─ stop-dev.ps1            停止开发环境
└─ backups/                   数据备份
```

---

## 技术栈

| 层级 | 技术 |
|---|---|
| Java 后端 | Java 17, Spring Boot 3, RuoYi-Vue-Plus 5.6.1 |
| 数据访问 | MyBatis-Plus, MySQL 8 |
| 认证授权 | Sa-Token |
| 缓存/锁 | Redis 7, Redisson |
| 调度引擎 | SnailJob（分布式） |
| 管理后台 | Vue 3, TypeScript, Element Plus, ECharts, Pinia |
| 微信小程序 | UniApp 3, Vue 3, TypeScript, Wot UI 2, Pinia |
| 外部数据 | TikHub |
| 文件存储 | MinIO |
| 容器化 | Docker Compose, Nginx |
| 测试工具 | Vue-tsc (类型检查), 冒烟测试脚本 |

---

## 核心业务规则

| 规则 | 说明 |
|---|---|
| 作者监控 | 添加时记录基线时间，只发现基线后发布的新作品，不导入历史作品 |
| 单作品监控 | 通过链接添加的作品只监控该作品，绝不自动扩展为作者监控 |
| 取消监控 | 只解除当前用户的监控关系，保留共享作者/作品/快照/预警历史 |
| 数据权限 | 本人 → 递归下级 → 部门 → 管理员，四层隔离 |
| 数据来源 | TikHub 唯一通道，TikOmni 已退出 |
| 兼职素材分配 | 后台预上传，用户领取时按 `assign_index % 容量` 顺序循环分配，结果快照固定 |

---

## 本地开发

### 前置条件

- Java 17+
- Node.js 18+
- Docker Desktop (MySQL / Redis / MinIO)
- Maven 3.6+
- 微信开发者工具（调试小程序）

### 启动后端

```powershell
# 1. 启动基础设施（MySQL + Redis + MinIO + SnailJob）
.\deploy\docker-compose.dev.yml up -d

# 2. 打包后端
cd server
mvn -pl ruoyi-admin -am clean package -DskipTests

# 3. 运行后端（默认 dev profile，mock 模式可用）
java -jar ruoyi-admin/target/ruoyi-admin.jar

# 或用脚本一键启动
.\scripts\start-dev.ps1 -SkipBuild
```

### 启动管理后台

```powershell
cd web
npm install
npm run dev
# → http://localhost:5180
```

### 启动微信小程序

```powershell
cd miniapp
npm install
npm run type-check          # 类型检查
npm run dev:mp-weixin       # 启动开发，用微信开发者工具打开 dist/dev/mp-weixin
npm run build:mp-weixin     # 生产构建，用开发者工具上传
```

**注意**：微信开发者工具详情 → 本地设置 → 勾选**"不校验合法域名"**（开发阶段）。

### 端到端冒烟测试

```bash
cd miniapp
node scripts/smoke-test.mjs
# 默认连 http://127.0.0.1:8081/prod-api
# 可指定环境：API_BASE=https://your.host/prod-api node scripts/smoke-test.mjs
# 跳过写库步骤：SKIP_WRITE=1 node scripts/smoke-test.mjs
```

---

## Docker 生产部署

```powershell
cd deploy
docker-compose -f docker-compose.prod.yml up -d
```

| 服务 | 端口 | 说明 |
|---|---|---|
| MySQL | 13307 | 主数据库 |
| Redis | 16380 | 缓存与锁 |
| MinIO | 19000 / 19001 | 文件存储 / Console |
| SnailJob | 18800 / 17889 | 调度控制台 / Server |
| API | 8088 | Java 后端 |
| Web | 80 | 管理后台 + `/prod-api/` 反代 |

**环境变量**：生产部署需要设置以下变量（建议通过 `.env` 文件）：

```env
WECHAT_MINIAPP_APPID=wx66e41e6f46cf2d23
WECHAT_MINIAPP_APP_SECRET=your-secret
WECHAT_MINIAPP_MOCK_ENABLED=true
MYSQL_ROOT_PASSWORD=your-password
REDIS_PASSWORD=ruoyi123
TIKHUB_API_KEY=your-key
```

---

## 微信小程序要点

| 事项 | 说明 |
|---|---|
| AppID | `wx66e41e6f46cf2d23` |
| AppSecret | 🔴 未配置，阻塞真实微信登录 |
| 登录模式 | mock 模拟（开发） / 真实微信（生产） |
| 生产环境 | 必须 HTTPS + 服务器域名白名单登记 |
| 环境配置 | [miniapp/.env.production](miniapp/.env.production) → 填入 API 域名后构建 |
| 冒烟测试 | `miniapp/scripts/smoke-test.mjs` |

当前 401 处理机制：`request.ts` 收到 HTTP 401 或业务码 401 时，清除 storage 并广播 `AUTH_LOGOUT_EVENT`，`auth store` 同步清理内存 ref，页面自动切换到登录提示。

---

## 文档总览

| 文档 | 内容 | 适合阅读对象 |
|---|---|---|
| [PRD](docs/JAVA_RUOYI_REBUILD_PRD.md) | 一期产品需求、规则、验收标准 | 产品/开发/测试 |
| [实施计划](docs/JAVA_RUOYI_IMPLEMENTATION_PLAN.md) | 阶段路线图、阶段内容、风险 | 项目管理/开发 |
| [兼职阶段方案](docs/PART_TIME_TASK_PHASE_PLAN.md) | 兼职业务流程、角色、菜单 | 产品/开发 |
| [素材库 PRD](docs/PART_TIME_MATERIAL_LIBRARY_PRD.md) | 素材库需求、分配规则、数据模型 | 产品/开发 |
| [素材库实施](docs/PART_TIME_MATERIAL_LIBRARY_IMPLEMENTATION_PLAN.md) | 素材库 M1-M7 实施计划 | 开发 |
| [数据模型](docs/DATABASE_DESIGN.md) | 表结构、字段、索引 | 开发 |
| [上游基线](docs/UPSTREAM_BASELINE.md) | RuoYi-Vue-Plus 上游对应版本 | 开发 |

> [!tip] 项目状态实时维护在 Obsidian 笔记中
> `C:\Users\admin\Documents\Obsidian Vault\抖音监控\TikTok Platform 项目\` — 包含项目首页、当前进度、行动清单、关键决策共 13 个文件。代码仓库内 `docs/` 只保留产品与技术参考文档。

---

## 版本里程碑

| 版本 | 阶段 | 状态 |
|---|---|---|
| v0.1.0 | 工程与权限 | ✅ |
| v0.2.0 | 单作品监控 | ✅ |
| v0.3.0 | 自动采集 | ✅ |
| v0.4.0 | 作者新作品发现 | ✅ |
| v0.5.0 | 兼职任务小程序 | ✅ 已完成但依赖 AppSecret |
| v0.6.0 | 兼职素材库 | 📋 开发中 |
| v0.7.0 | 佣金结算 | 🔴 未启动 |
| v1.0.0 | 一期正式上线 | ⬜ |