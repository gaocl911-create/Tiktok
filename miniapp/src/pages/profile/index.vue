<template>
  <view class="page-shell">
    <view class="surface profile-card">
      <view class="profile-main">
        <view class="avatar">{{ isLoggedIn ? "兼" : "未" }}</view>
        <view class="profile-copy">
          <text class="profile-kicker">个人账户</text>
          <view class="profile-name">{{ profileName }}</view>
          <text class="profile-openid">
            {{ isLoggedIn ? `ID：${shortOpenid}` : "登录后才能领取任务、提交作品和查看审核状态。" }}
          </text>
        </view>
        <text :class="['pill', isLoggedIn ? 'success' : 'warning']">{{ loginTagText }}</text>
      </view>

      <view class="profile-divider" />

      <view class="profile-bottom" @click="handleProfileAction">
        <view>
          <text class="bottom-label">资料状态</text>
          <text class="bottom-value">{{ isLoggedIn ? onboardingText : "待登录" }}</text>
        </view>
        <view class="profile-action">
          <text>{{ isLoggedIn ? "查看资料" : "先登录" }}</text>
          <text class="action-arrow">›</text>
        </view>
      </view>
    </view>

    <view v-if="isLoggedIn" class="surface onboarding-card" @click="openProfileEdit">
      <view>
        <view class="onboarding-title">兼职资料</view>
        <text class="onboarding-copy">{{ onboardingHint }}</text>
      </view>
      <view class="onboarding-side">
        <text :class="['pill', onboardingTone]">{{ onboardingText }}</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view v-if="!isLoggedIn" class="surface login-card">
      <view class="login-title">先完成小程序登录</view>
      <text class="login-copy">
        正式流程是前端调用 wx.login 获取 code，后端用 code 换 openid，再签发系统 token。本地开发可以先使用模拟登录。
      </text>
      <wd-button block :loading="loggingIn === 'wechat'" @click="handleWechatLogin">微信登录</wd-button>
      <wd-button block plain :loading="loggingIn === 'mock'" @click="handleMockLogin">开发模拟登录</wd-button>
      <text class="tips">上线前再切回真实微信登录。</text>
    </view>

    <view class="section-title">账户与业务</view>
    <view class="surface menu-list">
      <view
        v-for="item in menuItems"
        :key="item.label"
        class="menu-item"
        @click="handleMenu(item)"
      >
        <view>
          <text class="menu-title">{{ item.label }}</text>
          <text v-if="item.desc" class="menu-desc">{{ item.desc }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <wd-button v-if="isLoggedIn" block plain custom-class="logout-btn" @click="logout">
      退出登录
    </wd-button>

    <view class="version">创作者兼职任务 V0.1.0</view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { storeToRefs } from "pinia";
import { getMyProfile, type OnboardingStatus, type StaffProfile } from "@/api/profile";
import { useAuthStore } from "@/stores/auth";

interface MenuItem {
  label: string;
  desc?: string;
  action: "profile" | "tasks" | "works" | "todo";
}

const authStore = useAuthStore();
const { isLoggedIn, openid } = storeToRefs(authStore);
const loggingIn = ref<"" | "wechat" | "mock">("");
const profile = ref<StaffProfile>({});

const statusMap: Record<
  OnboardingStatus,
  { text: string; hint: string; tone: "" | "warning" | "success" | "danger" }
> = {
  incomplete: {
    text: "未完善",
    hint: "补齐手机号、微信号和抖音号后提交审核。",
    tone: "warning",
  },
  pending: {
    text: "待审核",
    hint: "资料已提交，等待后台管理员审核。",
    tone: "warning",
  },
  approved: {
    text: "已通过",
    hint: "资料已审核通过，可以领取任务。",
    tone: "success",
  },
  rejected: {
    text: "已驳回",
    hint: "请查看原因并重新提交资料。",
    tone: "danger",
  },
};

const menuItems: MenuItem[] = [
  { label: "完善兼职资料", desc: "手机号、微信号、抖音号", action: "profile" },
  { label: "任务广场", desc: "查看可领取任务", action: "tasks" },
  { label: "我的作品", desc: "提交链接与审核进度", action: "works" },
  { label: "佣金明细", desc: "后续接入结算数据", action: "todo" },
  { label: "结算记录", desc: "后续接入提现记录", action: "todo" },
  { label: "用户协议与隐私政策", action: "todo" },
];

const currentStatus = computed<OnboardingStatus>(() => profile.value.onboardingStatus || "incomplete");
const profileName = computed(() => {
  if (!isLoggedIn.value) return "未登录";
  return profile.value.realName || "兼职用户";
});
const shortOpenid = computed(() => {
  if (!openid.value) return "-";
  return openid.value.length > 18 ? `${openid.value.slice(0, 18)}...` : openid.value;
});
const loginTagText = computed(() => (isLoggedIn.value ? "已登录" : "待登录"));
const onboardingText = computed(() => statusMap[currentStatus.value].text);
const onboardingHint = computed(() => statusMap[currentStatus.value].hint);
const onboardingTone = computed(() => statusMap[currentStatus.value].tone);

const loadProfile = async () => {
  if (!isLoggedIn.value) {
    profile.value = {};
    return;
  }
  try {
    profile.value = await getMyProfile(false);
  } catch {
    authStore.clearSession();
    profile.value = {};
  }
};

const openProfileEdit = () => {
  if (!isLoggedIn.value) return;
  uni.navigateTo({ url: "/pages/profile/edit" });
};

const handleProfileAction = () => {
  if (isLoggedIn.value) {
    openProfileEdit();
    return;
  }
  uni.showToast({ title: "请先登录", icon: "none" });
};

const handleWechatLogin = async () => {
  loggingIn.value = "wechat";
  try {
    await authStore.loginByWechat();
    await loadProfile();
    uni.showToast({ title: "登录成功", icon: "success" });
  } finally {
    loggingIn.value = "";
  }
};

const handleMockLogin = async () => {
  loggingIn.value = "mock";
  try {
    await authStore.loginByMock();
    await loadProfile();
    uni.showToast({ title: "模拟登录成功", icon: "success" });
  } finally {
    loggingIn.value = "";
  }
};

const handleMenu = (item: MenuItem) => {
  if (item.action === "profile") {
    openProfileEdit();
    return;
  }
  if (item.action === "works") {
    uni.switchTab({ url: "/pages/works/index" });
    return;
  }
  if (item.action === "tasks") {
    uni.switchTab({ url: "/pages/tasks/index" });
    return;
  }
  uni.showToast({ title: "功能规划中", icon: "none" });
};

const logout = () => {
  authStore.clearSession();
  profile.value = {};
  uni.showToast({ title: "已退出", icon: "none" });
};

onShow(loadProfile);
</script>

<style scoped lang="scss">
.profile-card {
  padding: 30rpx;
}

.profile-main {
  display: flex;
  align-items: center;
  gap: 22rpx;
}

.avatar {
  width: 92rpx;
  height: 92rpx;
  display: grid;
  flex: 0 0 92rpx;
  place-items: center;
  color: #fff;
  background: var(--cm-ink);
  border-radius: 28rpx;
  font-size: 34rpx;
  font-weight: 900;
}

.profile-copy {
  flex: 1;
  min-width: 0;
}

.profile-kicker,
.profile-openid,
.bottom-label,
.bottom-value {
  display: block;
}

.profile-kicker {
  color: var(--cm-muted);
  font-size: 22rpx;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.profile-name {
  margin-top: 6rpx;
  color: var(--cm-ink);
  font-size: 42rpx;
  font-weight: 900;
  line-height: 1.15;
  letter-spacing: -0.04em;
}

.profile-openid {
  margin-top: 8rpx;
  color: var(--cm-muted);
  font-size: 23rpx;
  line-height: 1.45;
  word-break: break-all;
}

.profile-divider {
  height: 1rpx;
  margin: 30rpx 0 24rpx;
  background: var(--cm-line);
}

.profile-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.bottom-label {
  color: var(--cm-muted);
  font-size: 22rpx;
  font-weight: 700;
}

.bottom-value {
  margin-top: 6rpx;
  color: var(--cm-ink);
  font-size: 30rpx;
  font-weight: 900;
}

.profile-action {
  display: flex;
  align-items: center;
  gap: 8rpx;
  min-height: 58rpx;
  padding: 0 20rpx;
  color: #fff;
  background: var(--cm-ink);
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 800;
}

.action-arrow {
  font-size: 30rpx;
  line-height: 1;
}

.onboarding-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22rpx;
  margin-top: 20rpx;
  padding: 28rpx 30rpx;
}

.onboarding-title {
  margin-bottom: 8rpx;
  color: var(--cm-ink);
  font-size: 32rpx;
  font-weight: 900;
}

.onboarding-copy {
  color: var(--cm-muted);
  font-size: 24rpx;
  line-height: 1.55;
}

.onboarding-side {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.login-card {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
  margin-top: 24rpx;
  padding: 32rpx;
}

.login-title {
  color: var(--cm-ink);
  font-size: 34rpx;
  font-weight: 900;
}

.login-copy,
.tips {
  color: var(--cm-muted);
  font-size: 24rpx;
  line-height: 1.65;
}

.tips {
  color: #aaa69b;
}

.menu-list {
  overflow: hidden;
}

.menu-item {
  min-height: 112rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 22rpx 30rpx;
  border-bottom: 1rpx solid var(--cm-line);
}

.menu-item:last-child {
  border-bottom: 0;
}

.menu-title,
.menu-desc {
  display: block;
}

.menu-title {
  color: var(--cm-ink);
  font-size: 29rpx;
  font-weight: 800;
}

.menu-desc {
  margin-top: 8rpx;
  color: var(--cm-muted);
  font-size: 23rpx;
}

.arrow {
  color: #aaa69b;
  font-size: 44rpx;
  line-height: 1;
}

:deep(.logout-btn) {
  margin-top: 30rpx;
}

.version {
  margin-top: 44rpx;
  color: #aaa69b;
  font-size: 23rpx;
  text-align: center;
}
</style>
