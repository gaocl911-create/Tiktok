export interface ApiResponse<T> {
  code: number;
  msg?: string;
  data?: T;
  rows?: unknown[];
  total?: number;
}

export interface PageResult<T> {
  rows: T[];
  total: number;
}

interface RequestOptions extends UniApp.RequestOptions {
  showError?: boolean;
}

export const TOKEN_KEY = "creator-miniapp-token";
export const OPENID_KEY = "creator-miniapp-openid";
export const MINIAPP_CLIENT_ID = "ba9e8a5f68fd1436043780186727e92f";
export const AUTH_LOGOUT_EVENT = "miniapp:auth-logout";

const baseUrl = import.meta.env.VITE_API_BASE_URL || "";

if (!baseUrl) {
  // 生产构建时若 .env.production 没填会得到空串 —— 抛出明显错误，
  // 防止请求悄悄打到相对路径或本机。
  console.error(
    "[miniapp] VITE_API_BASE_URL is empty. Set it in .env.production before building.",
  );
}

export const getToken = () => uni.getStorageSync(TOKEN_KEY) as string;

export const hasToken = () => Boolean(getToken());

export const request = <T>(options: RequestOptions): Promise<T> =>
  new Promise((resolve, reject) => {
    const token = getToken();
    const requestUrl = `${baseUrl}${options.url}`;

    uni.request({
      ...options,
      url: requestUrl,
      header: {
        "Content-Type": "application/json",
        clientid: MINIAPP_CLIENT_ID,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.header,
      },
      success: (response: UniApp.RequestSuccessCallbackResult) => {
        const body = response.data as ApiResponse<T>;

        // 后端实际同时使用 HTTP 401 和 body.code = 401 两种形态表达未登录
        const unauthorized = response.statusCode === 401 || body?.code === 401;
        if (unauthorized) {
          uni.removeStorageSync(TOKEN_KEY);
          uni.removeStorageSync(OPENID_KEY);
          // 广播给 auth store，让 reactive 的 token ref 同步清掉
          uni.$emit(AUTH_LOGOUT_EVENT);
        }

        if (response.statusCode >= 200 && response.statusCode < 300 && body?.code === 200) {
          if (body.data !== undefined) {
            resolve(body.data);
            return;
          }
          resolve(body as T);
          return;
        }

        const message = body?.msg || `请求失败（${response.statusCode}）`;
        if (options.showError !== false && !unauthorized) {
          // 401 由调用方/页面统一引导去登录，这里不再 toast 框架的英文提示
          uni.showToast({ title: message, icon: "none" });
        }
        reject(new Error(message));
      },
      fail: (error: UniApp.GeneralCallbackResult) => {
        // Local mini program debugging often fails silently when domain verification is enabled.
        console.error("[miniapp request failed]", requestUrl, error.errMsg || error);
        if (options.showError !== false) {
          uni.showToast({ title: "网络连接失败，请稍后重试", icon: "none" });
        }
        reject(error);
      },
    });
  });
