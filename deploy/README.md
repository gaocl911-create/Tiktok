# TikTok Platform 部署手册

本文档用于把后台管理端、后端 API、SnailJob、MySQL、Redis、MinIO 部署到服务器。小程序端不作为 Docker 服务运行，需要单独构建后用微信开发者工具上传。

## 0. 当前上线结论

代码构建检查已通过，但正式上线前必须先补齐这些外部条件：

- 域名已备案，并解析到服务器公网 IP。
- 服务器已开放 `80`、`443` 端口。
- 已申请 HTTPS 证书，并放到 `deploy/ssl/fullchain.pem` 和 `deploy/ssl/privkey.pem`。
- 微信小程序后台已配置合法服务器域名，例如 `https://your-domain.com`。
- `deploy/.env.prod` 已按 `.env.prod.example` 填完整。
- `miniapp/.env.production` 已填写正式 HTTPS API 地址。

## 1. 服务器准备

推荐环境：

- Linux 服务器，2 核 4G 起步，生产建议 4 核 8G。
- Docker 24+。
- Docker Compose v2。
- Git。

安装后检查：

```bash
docker --version
docker compose version
git --version
```

## 2. 拉取代码

```bash
mkdir -p /opt
cd /opt
git clone https://github.com/gaocl911-create/Tiktok.git TikTok_Platform
cd /opt/TikTok_Platform
git checkout main
git pull origin main
```

后续更新：

```bash
cd /opt/TikTok_Platform
git pull origin main
```

## 3. 配置生产环境变量

```bash
cd /opt/TikTok_Platform
cp deploy/.env.prod.example deploy/.env.prod
chmod 600 deploy/.env.prod
```

编辑：

```bash
nano deploy/.env.prod
```

必须填写：

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `REDIS_PASSWORD`
- `MINIO_ROOT_USER`
- `MINIO_ROOT_PASSWORD`
- `SNAIL_JOB_TOKEN`
- `SA_TOKEN_JWT_SECRET`
- `API_DECRYPT_REQUEST_PRIVATE_KEY`
- `API_DECRYPT_RESPONSE_PUBLIC_KEY`
- `WEB_RSA_PUBLIC_KEY`
- `WEB_CLIENT_ID`
- `CORS_ALLOWED_ORIGINS`
- `WECHAT_MINIAPP_APPID`
- `WECHAT_MINIAPP_APP_SECRET`

建议全部用强随机值，尤其是 MySQL、Redis、MinIO、SnailJob、Sa-Token。

## 4. 生成 RSA Keypair

前端只放公钥，后端只放私钥。

```bash
mkdir -p deploy/keys
openssl genrsa -out deploy/keys/request-private.pem 2048
openssl rsa -in deploy/keys/request-private.pem -pubout -out deploy/keys/request-public.pem
```

复制私钥正文到：

```text
API_DECRYPT_REQUEST_PRIVATE_KEY=
```

复制公钥正文到：

```text
WEB_RSA_PUBLIC_KEY=
```

注意去掉：

```text
-----BEGIN ...-----
-----END ...-----
```

并去掉换行。

`API_DECRYPT_RESPONSE_PUBLIC_KEY` 如果暂时没有启用响应加密，也要按后端要求填一个有效公钥占位，避免 prod fail-fast。

## 5. 配置 HTTPS 证书

创建目录：

```bash
mkdir -p deploy/ssl
```

放入：

```text
deploy/ssl/fullchain.pem
deploy/ssl/privkey.pem
```

修改 `deploy/nginx.conf`：

```nginx
server_name your-domain.com;
```

两个 `server` 块里的 `server_name _;` 都建议替换成真实域名。

## 6. 配置小程序生产 API

在本地或构建机：

```bash
cd miniapp
cp .env.production.example .env.production
```

填写：

```text
VITE_API_BASE_URL=https://your-domain.com/prod-api
```

这个域名必须已经在微信公众平台的小程序后台加入合法 request 域名。

## 7. 构建并启动 Docker 服务

先校验配置：

```bash
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml config --quiet
```

构建镜像：

```bash
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml build
```

启动：

```bash
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml up -d
```

查看状态：

```bash
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml ps
```

查看日志：

```bash
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml logs -f api
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml logs -f web
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml logs -f snailjob
```

## 8. 初始化或检查 MinIO

如果图片上传卡在 100% 或 OSS 报错，检查 bucket。

进入 MinIO 容器：

```bash
docker exec -it tiktok-platform-minio sh
```

在容器里执行：

```bash
mc alias set local http://127.0.0.1:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"
mc mb -p local/ruoyi
mc anonymous set download local/ruoyi
```

如果数据库已有旧配置，需要确认 `sys_oss_config` 指向 Docker 内网服务名：

```sql
update sys_oss_config
set endpoint = 'minio:9000',
    is_https = 'N'
where config_key = 'minio';
```

公网访问域名按你的实际图片访问策略再设置。

## 9. 后台管理端验收

浏览器打开：

```text
https://your-domain.com
```

检查：

- 登录页能打开。
- 能正常登录后台。
- 内容监测列表能加载。
- 兼职任务、文案库、图片库页面能打开。
- 上传图片能成功。
- SnailJob 定时任务能执行。

## 10. 小程序构建与上传

```bash
cd miniapp
npm install
npm run type-check
npm run build:mp-weixin
```

用微信开发者工具打开：

```text
miniapp/dist/build/mp-weixin
```

然后：

1. 真机预览。
2. 测试微信登录。
3. 测试完善兼职资料。
4. 测试领取任务。
5. 测试提交作品。
6. 测试后台审核。
7. 确认接口都走 HTTPS 正式域名。
8. 上传审核。

## 11. 常用运维命令

停止：

```bash
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml down
```

重启：

```bash
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml restart
```

更新代码后重建：

```bash
git pull origin main
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml build
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml up -d
```

清空全部 Docker 数据重新初始化，谨慎使用：

```bash
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml down -v
docker compose --env-file deploy/.env.prod -f deploy/docker-compose.prod.yml up -d
```

## 12. 上线前最终 Checklist

- [ ] `deploy/.env.prod` 所有必填项已填。
- [ ] 生产密码和 token 都不是默认值。
- [ ] 小程序 AppSecret 已重新生成并填入生产环境。
- [ ] `deploy/ssl/fullchain.pem` 存在。
- [ ] `deploy/ssl/privkey.pem` 存在。
- [ ] `deploy/nginx.conf` 的 `server_name` 已替换为真实域名。
- [ ] `miniapp/.env.production` 已填写 HTTPS API 地址。
- [ ] 微信小程序后台已配置合法 request 域名。
- [ ] 后台 Web 能访问。
- [ ] API 健康检查正常。
- [ ] 图片上传正常。
- [ ] SnailJob 定时任务正常。
- [ ] 小程序真机登录、领取、提交、审核闭环通过。
