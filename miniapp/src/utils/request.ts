interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}

interface RequestOptions extends UniApp.RequestOptions {
  showError?: boolean;
}

const TOKEN_KEY = "creator-miniapp-token";
const baseUrl = import.meta.env.VITE_API_BASE_URL;

export const request = <T>(options: RequestOptions): Promise<T> =>
  new Promise((resolve, reject) => {
    const token = uni.getStorageSync(TOKEN_KEY);

    uni.request({
      ...options,
      url: `${baseUrl}${options.url}`,
      header: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.header,
      },
      success: (response: UniApp.RequestSuccessCallbackResult) => {
        const body = response.data as ApiResponse<T>;
        if (response.statusCode === 401) {
          uni.removeStorageSync(TOKEN_KEY);
        }
        if (response.statusCode >= 200 && response.statusCode < 300 && body.code === 200) {
          resolve(body.data);
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
