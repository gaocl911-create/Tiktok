# TikTok Platform

基于 RuoYi-Vue-Plus 重构的创作者作品监测与兼职任务管理平台。

## Project Structure

```text
server/   RuoYi-Vue-Plus Java backend
web/      plus-ui management frontend
miniapp/  UniApp WeChat mini program
docs/     PRD and implementation plan
deploy/   Docker and Nginx deployment files
sql/      Business database migrations
```

## Baseline Versions

- RuoYi-Vue-Plus: `v5.6.1`
- plus-ui: `v5.6.1-v2.6.1`
- Java: 17
- Database: MySQL 8
- Cache: Redis
- Scheduler: SnailJob

## Documents

- [Product Requirements](docs/JAVA_RUOYI_REBUILD_PRD.md)
- [Implementation Plan](docs/JAVA_RUOYI_IMPLEMENTATION_PLAN.md)

## Local Development

Start Docker infrastructure, SnailJob, the Java API, and the Vue frontend:

```powershell
.\scripts\start-dev.ps1 -SkipBuild
```

Stop all local development services:

```powershell
.\scripts\stop-dev.ps1
```

Local addresses:

- Web: `http://localhost:5180`
- API: `http://localhost:8088`
- SnailJob: `http://localhost:18800/snail-job`
- MinIO Console: `http://localhost:19001`
