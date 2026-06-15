# 创作者兼职任务小程序

技术栈：

- UniApp
- Vue 3
- TypeScript
- Pinia
- Wot UI (`@wot-ui/ui`)

## 本地开发

```bash
npm install
npm run dev:mp-weixin
```

将生成的`dist/dev/mp-weixin`目录导入微信开发者工具。

## 构建检查

```bash
npm run type-check
npm run build:mp-weixin
```

开发环境接口地址位于`.env.development`。真机调试时不能使用`localhost`，需要改为局域网或已备案的HTTPS域名。
