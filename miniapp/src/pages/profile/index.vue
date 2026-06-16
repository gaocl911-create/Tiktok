<template>
  <view class="page-shell">
    <view class="surface profile-card">
      <view class="avatar">{{ isLoggedIn ? "微" : "未" }}</view>
      <view class="profile-copy">
        <view class="profile-name">{{ profileName }}</view>
        <text class="muted">
          {{ isLoggedIn ? `openid：${shortOpenid}` : "登录后才能领取任务、提交作品和查看审核状态" }}
        </text>
      </view>
      <wd-tag :type="loginTagType" plain>{{ loginTagText }}</wd-tag>
    </view>

    <view v-if="isLoggedIn" class="surface onboarding-card" @click="openProfileEdit">
      <view>
        <view class="onboarding-title">兼职资料</view>
        <text class="muted">{{ onboardingHint }}</text>
      </view>
      <view class="onboarding-side">
        <wd-tag :type="onboardingTagType" plain>{{ onboardingText }}</wd-tag>
        <text class="arrow">›</text>
      </view>
    </view>

    <view v-if="!isLoggedIn" class="surface login-card">
      <view class="login-title">先完成小程序登录</view>
      <text class="muted">
        微信小程序正式流程是：前端调用 wx.login 获取 code，后端用 code 换 openid，再签发系统 token。
      </text>
      <wd-button block :loading="loggingIn === 'wechat'" @click="handleWechatLogin">
        微信登录
      </wd-button>
      <wd-button block plain :loading="loggingIn === 'mock'" @click="handleMockLogin">
        开发模拟登录
      </wd-button>
      <text class="tips">本地开发先用模拟登录即可；上线前再切回真实微信登录。</text>
    </view>

    <view class="section-title">账户与业务</view>
    <view class="surface menu-list">
      <view
        v-for="item in menuItems"
        :key="item.label"
        class="menu-item"
        @click="handleMenu(item)"
      >
        <text>{{ item.label }}</text>
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
  action: "profile" | "tasks" | "works" | "todo";
}

const authStore = useAuthStore();
const { isLoggedIn, openid } = storeToRefs(authStore);
const loggingIn = ref<"" | "wechat" | "mock">("");
const profile = ref<StaffProfile>({});

const statusMap: Record<
  OnboardingStatus,
  { text: string; hint: string; tag: "primary" | "warning" | "success" | "danger" }
> = {
  incomplete: {
    text: "未完善",
    hint: "补齐手机号、微信号和抖音号后提交审核。",
    tag: "warning",
  },
  pending: {
    text: "待审核",
    hint: "资料已提交，等待后台管理员审核。",
    tag: "primary",
  },
  approved: {
    text: "已通过",
    hint: "资料已审核通过，可以领取任务。",
    tag: "success",
  },
  rejected: {
    text: "已驳回",
    hint: "请查看原因并重新提交资料。",
    tag: "danger",
  },
};

const menuItems: MenuItem[] = [
  { label: "完善兼职资料", action: "profile" },
  { label: "我的任务", action: "works" },
  { label: "佣金明细", action: "todo" },
  { label: "结算记录", action: "todo" },
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
const loginTagType = computed(() => (isLoggedIn.value ? "success" : "warning"));
const loginTagText = computed(() => (isLoggedIn.value ? "已登录" : "待登录"));
const onboardingText = computed(() => statusMap[currentStatus.value].text);
const onboardingHint = computed(() => statusMap[currentStatus.value].hint);
const onboardingTagType = computed(() => statusMap[currentStatus.value].tag);

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
.profile-card,
.onboarding-card {
  display: flex;
  align-items: center;
  gap: 22rpx;
  padding: 30rpx;
}

.avatar {
  width: 96rpx;
  height: 96rpx;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #172033);
  border-radius: 50%;
  font-size: 34rpx;
  font-weight: 700;
}

.profile-copy {
  flex: 1;
  min-width: 0;
}

.profile-name {
  margin-bottom: 8rpx;
  font-size: 32rpx;
  font-weight: 700;
}

.profile-card .muted,
.login-card .muted,
.onboarding-card .muted {
  font-size: 24rpx;
  line-height: 1.6;
}

.onboarding-card {
  justify-content: space-between;
  margin-top: 22rpx;
}

.onboarding-title {
  margin-bottom: 8rpx;
  font-size: 30rpx;
  font-weight: 700;
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
  margin-top: 22rpx;
  padding: 30rpx;
}

.login-title {
  font-size: 32rpx;
  font-weight: 800;
}

.tips {
  color: #a4adba;
  font-size: 23rpx;
  line-height: 1.6;
}

.menu-list {
  overflow: hidden;
}

.menu-item {
  min-height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28rpx;
  border-bottom: 1rpx solid #edf1f5;
  font-size: 28rpx;
}

.menu-item:last-child {
  border-bottom: 0;
}

.arrow {
  color: #a4adba;
  font-size: 40rpx;
}

:deep(.logout-btn) {
  margin-top: 28rpx;
}

.version {
  margin-top: 44rpx;
  color: #a4adba;
  font-size: 23rpx;
  text-align: center;
}
</style>
