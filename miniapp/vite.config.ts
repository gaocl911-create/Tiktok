import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";

// https://vitejs.dev/config/
export default defineConfig(({ command }) => ({
  plugins: [uni()],
  // 生产构建时移除 console / debugger，避免日志泄漏请求 URL / openid / errMsg 到微信控制台。
  esbuild: command === "build" ? { drop: ["console", "debugger"] } : undefined,
}));
