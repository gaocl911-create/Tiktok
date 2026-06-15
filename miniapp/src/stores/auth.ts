import { defineStore } from "pinia";
import { computed, ref } from "vue";

const TOKEN_KEY = "creator-miniapp-token";

export const useAuthStore = defineStore("auth", () => {
  const token = ref<string>(uni.getStorageSync(TOKEN_KEY) || "");
  const isLoggedIn = computed(() => Boolean(token.value));

  const setToken = (value: string) => {
    token.value = value;
    uni.setStorageSync(TOKEN_KEY, value);
  };

  const clearSession = () => {
    token.value = "";
    uni.removeStorageSync(TOKEN_KEY);
  };

  return {
    token,
    isLoggedIn,
    setToken,
    clearSession,
  };
});
