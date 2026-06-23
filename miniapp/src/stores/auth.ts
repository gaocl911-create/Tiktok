import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { miniappLogin } from "@/api/auth";
import { AUTH_LOGOUT_EVENT, OPENID_KEY, TOKEN_KEY } from "@/utils/request";

const MOCK_OPENID_KEY = "creator-miniapp-mock-openid";
const DEFAULT_MOCK_OPENID = "dev_local_user";

export const useAuthStore = defineStore("auth", () => {
  const token = ref<string>(uni.getStorageSync(TOKEN_KEY) || "");
  const openid = ref<string>(uni.getStorageSync(OPENID_KEY) || "");
  const isLoggedIn = computed(() => Boolean(token.value));

  const setSession = (value: { token: string; openid?: string }) => {
    token.value = value.token;
    openid.value = value.openid || "";
    uni.setStorageSync(TOKEN_KEY, value.token);
    uni.setStorageSync(OPENID_KEY, value.openid || "");
  };

  const loginByWechat = async () => {
    const code = await new Promise<string>((resolve, reject) => {
      uni.login({
        provider: "weixin",
        success: (res) => {
          if (res.code) {
            resolve(res.code);
            return;
          }
          reject(new Error("微信登录没有返回 code"));
        },
        fail: reject,
      });
    });
    const result = await miniappLogin({ code });
    setSession({ token: result.access_token, openid: result.openid });
    return result;
  };

  const loginByMock = async () => {
    let mockOpenid = uni.getStorageSync(MOCK_OPENID_KEY) as string;
    if (!mockOpenid) {
      mockOpenid = DEFAULT_MOCK_OPENID;
      uni.setStorageSync(MOCK_OPENID_KEY, mockOpenid);
    }
    const result = await miniappLogin({ mockOpenid });
    setSession({ token: result.access_token, openid: result.openid });
    return result;
  };

  const clearSession = () => {
    token.value = "";
    openid.value = "";
    uni.removeStorageSync(TOKEN_KEY);
    uni.removeStorageSync(OPENID_KEY);
  };

  // request.ts 在收到 401 时会广播这个事件，store 也要同步把内存里的 ref 清掉，
  // 否则页面通过 storeToRefs 拿到的 isLoggedIn 不会变。
  uni.$on(AUTH_LOGOUT_EVENT, () => {
    token.value = "";
    openid.value = "";
  });

  return {
    token,
    openid,
    isLoggedIn,
    setSession,
    loginByWechat,
    loginByMock,
    clearSession,
  };
});
