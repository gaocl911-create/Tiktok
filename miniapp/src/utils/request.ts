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
export const MINIAPP_CLIENT_ID = "ba9e8a5f68fd1436043780186727e92f";

const baseUrl = import.meta.env.VITE_API_BASE_URL || "";

export const getToken = () => uni.getStorageSync(TOKEN_KEY) as string;

export const hasToken = () => Boolean(getToken());

export const request = <T>(options: RequestOptions): Promise<T> =>
  new Promise((resolve, reject) => {
    const token = getToken();

    uni.request({
      ...options,
      url: `${baseUrl}${options.url}`,
      header: {
        "Content-Type": "application/json",
        clientid: MINIAPP_CLIENT_ID,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.header,
      },
      success: (response: UniApp.RequestSuccessCallbackResult) => {
        const body = response.data as ApiResponse<T>;

        if (response.statusCode === 401) {
          uni.removeStorageSync(TOKEN_KEY);
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
        if (options.showError !== false) {
          uni.showToast({ title: message, icon: "none" });
        }
        reject(new Error(message));
      },
      fail: (error: UniApp.GeneralCallbackResult) => {
        if (options.showError !== false) {
          uni.showToast({ title: "网络连接失败，请稍后重试", icon: "none" });
        }
        reject(error);
      },
    });
  });
