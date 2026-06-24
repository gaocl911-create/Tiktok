# Docker 部署

这套 Compose 会启动：

- MySQL 8
- Redis 7
- MinIO
- SnailJob
- Java API
- SnailJob Server
- Nginx 管理端前端

## 1. 准备环境变量

```powershell
Copy-Item deploy\.env.prod.example deploy\.env.prod
```

编辑 `deploy\.env.prod`，至少修改：

- `MYSQL_ROOT_PASSWORD`
- `REDIS_PASSWORD`
- `MINIO_ROOT_PASSWORD`
- `CONTAINER_PREFIX`（同一台机器部署多套时需要不同前缀）
- `WECHAT_MINIAPP_APPID`
- `WECHAT_MINIAPP_APP_SECRET`
- `TIKHUB_API_TOKEN`（需要启用 TikHub 时）

## 2. 构建镜像

```powershell
docker compose --env-file deploy\.env.prod -f deploy\docker-compose.prod.yml build
```

## 3. 启动服务

```powershell
docker compose --env-file deploy\.env.prod -f deploy\docker-compose.prod.yml up -d
```

如果机器上已经有旧的 `tiktok-platform` 基础容器，并且要复用旧 MySQL/Redis/MinIO 数据，只启动应用容器：

```powershell
docker compose -p tiktok-platform --env-file deploy\.env.prod -f deploy\docker-compose.old-data.yml up -d
```

这条命令会和旧的 `mysql` / `redis` / `minio` 显示在同一个 Docker Desktop 分组里。不要加 `--remove-orphans`，否则会移除同组里的旧基础容器。

默认访问地址：

- 管理端：`http://localhost`
- API：`http://localhost:8088`
- SnailJob：`http://localhost:18800/snail-job`
- MinIO 控制台：`http://localhost:19001`

## 4. 查看状态和日志

```powershell
docker compose --env-file deploy\.env.prod -f deploy\docker-compose.prod.yml ps
docker compose --env-file deploy\.env.prod -f deploy\docker-compose.prod.yml logs -f api
docker compose --env-file deploy\.env.prod -f deploy\docker-compose.prod.yml logs -f web
```

## 5. 停止服务

```powershell
docker compose --env-file deploy\.env.prod -f deploy\docker-compose.prod.yml down
```

如果需要清空数据库并重新执行初始化 SQL：

```powershell
docker compose --env-file deploy\.env.prod -f deploy\docker-compose.prod.yml down -v
docker compose --env-file deploy\.env.prod -f deploy\docker-compose.prod.yml up -d
```

注意：MySQL 官方镜像只会在数据卷为空时执行 `/docker-entrypoint-initdb.d` 里的 SQL。已有数据卷时，新增迁移 SQL 需要手动进库执行。

## 小程序

`miniapp` 不作为常驻 Docker 服务部署。构建微信小程序：

```powershell
cd miniapp
npm run build:mp-weixin
```

然后用微信开发者工具上传 `miniapp\dist\build\mp-weixin`。

## MinIO bucket init

The backend OSS config uses bucket `ruoyi`. If image upload stays at 100% or returns an OSS upload error after rebuilding Docker data, create the bucket and allow public download:

```powershell
docker exec tiktok-platform-minio sh -lc "mc alias set local http://127.0.0.1:9000 ruoyi ruoyi123 && mc mb -p local/ruoyi && mc anonymous set download local/ruoyi"
```

When the API runs inside Docker, `sys_oss_config.endpoint` should use the Docker service name:

```sql
update sys_oss_config
set endpoint = 'minio:9000',
    domain = 'localhost:19000',
    is_https = 'N'
where config_key = 'minio';
```
