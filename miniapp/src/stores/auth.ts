import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { miniappLogin } from "@/api/auth";

const TOKEN_KEY = "creator-miniapp-token";
const OPENID_KEY = "creator-miniapp-openid";

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
    const mockOpenid = `dev_${Date.now()}`;
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
