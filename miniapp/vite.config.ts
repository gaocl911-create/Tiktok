import { defineConfig, loadEnv } from "vite";
import uni from "@dcloudio/vite-plugin-uni";

// https://vitejs.dev/config/
export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), "");

  if (command === "build") {
    const apiBaseUrl = (process.env.VITE_API_BASE_URL || env.VITE_API_BASE_URL)?.trim();
    if (!apiBaseUrl) {
      throw new Error("VITE_API_BASE_URL is required before building the WeChat mini program.");
    }
    if (!apiBaseUrl.startsWith("https://")) {
      throw new Error("VITE_API_BASE_URL must be an HTTPS URL for production mini program builds.");
    }
    if (/localhost|127\.0\.0\.1|0\.0\.0\.0/i.test(apiBaseUrl)) {
      throw new Error("VITE_API_BASE_URL must not point to a local development address.");
    }
  }

  return {
    plugins: [uni()],
    // Remove console/debugger from production builds to avoid leaking request URLs,
    // openid, errMsg or other runtime details into the WeChat console.
    esbuild: command === "build" ? { drop: ["console", "debugger"] } : undefined,
  };
});
