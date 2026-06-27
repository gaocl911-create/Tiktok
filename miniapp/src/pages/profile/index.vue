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

    <view v-if="!isLoggedIn" class="surface login-card">
      <view class="login-title">先完成小程序登录</view>
      <text class="login-copy">
        登录后可以完善兼职资料、领取任务、提交作品链接并查看后台审核状态。
      </text>
      <view class="agreement-row" @click="toggleAgreement">
        <view :class="['check-box', { checked: agreementChecked }]">
          <text v-if="agreementChecked">✓</text>
        </view>
        <view class="agreement-copy">
          <text>我已阅读并同意</text>
          <text class="legal-link" @click.stop="openAgreement">《用户协议》</text>
          <text>和</text>
          <text class="legal-link" @click.stop="openPrivacy">《隐私政策》</text>
        </view>
      </view>
      <wd-button block :loading="loggingIn === 'wechat'" @click="handleWechatLogin">微信登录</wd-button>
      <wd-button block plain :loading="loggingIn === 'mock'" @click="handleMockLogin">开发模拟登录</wd-button>
      <text class="tips">首次登录后会弹出使用操作说明，帮助你快速熟悉流程。</text>
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

    <view v-if="guidePopupVisible" class="guide-mask" @click="closeGuidePopup">
      <view class="guide-dialog" @click.stop>
        <view class="guide-head">
          <view>
            <text class="guide-kicker">使用说明</text>
            <view class="guide-title">先看一遍流程，后面接任务会顺很多</view>
          </view>
          <view class="guide-close" @click="closeGuidePopup">×</view>
        </view>

        <view class="guide-steps">
          <view v-for="(step, index) in guideSteps" :key="step.title" class="guide-step">
            <view class="guide-step-index">{{ index + 1 }}</view>
            <view class="guide-step-copy">
              <view class="guide-step-title">{{ step.title }}</view>
              <text class="guide-step-desc">{{ step.desc }}</text>
            </view>
          </view>
        </view>

        <view class="guide-checkbox" @click="toggleDontShowGuide">
          <view :class="['check-box', { checked: dontShowGuideAgain }]">
            <text v-if="dontShowGuideAgain">✓</text>
          </view>
          <text>下次登录不再自动提示</text>
        </view>

        <view class="guide-actions">
          <wd-button block plain @click="openGuideFromPopup">查看完整说明</wd-button>
          <wd-button block @click="closeGuidePopup">我知道了</wd-button>
        </view>
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
import { hasToken } from "@/utils/request";

interface MenuItem {
  label: string;
  desc?: string;
  action: "profile" | "tasks" | "works" | "guide" | "agreement" | "privacy" | "todo";
}

const AGREEMENT_ACCEPTED_KEY = "creator-miniapp-agreement-accepted";
const GUIDE_DISMISSED_KEY = "creator-miniapp-guide-dismissed";

const authStore = useAuthStore();
const { isLoggedIn, openid } = storeToRefs(authStore);
const loggingIn = ref<"" | "wechat" | "mock">("");
const profile = ref<StaffProfile>({});
const agreementChecked = ref(Boolean(uni.getStorageSync(AGREEMENT_ACCEPTED_KEY)));
const guidePopupVisible = ref(false);
const dontShowGuideAgain = ref(false);
const guidePromptedInSession = ref(false);

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
  { label: "使用操作说明", desc: "领取、发布、提交全流程", action: "guide" },
  { label: "完善兼职资料", desc: "手机号、微信号、抖音号", action: "profile" },
  { label: "任务广场", desc: "查看可领取任务", action: "tasks" },
  { label: "我的作品", desc: "提交链接与审核进度", action: "works" },
  { label: "佣金明细", desc: "后续接入结算数据", action: "todo" },
  { label: "结算记录", desc: "后续接入提现记录", action: "todo" },
  { label: "用户协议", desc: "平台服务与任务规则", action: "agreement" },
  { label: "隐私政策", desc: "个人信息收集与使用说明", action: "privacy" },
];

const guideSteps = [
  { title: "完善资料", desc: "登录后补齐手机号、微信号和抖音号，提交后台审核。" },
  { title: "领取任务", desc: "审核通过后进入任务广场，按任务规则领取或继续领取。" },
  { title: "发布作品", desc: "到我的作品复制文案、保存图片，按要求发布作品。" },
  { title: "提交链接", desc: "作品发布后回到我的作品，粘贴作品链接等待审核。" },
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
const guideStorageKey = computed(() => `${GUIDE_DISMISSED_KEY}:${openid.value || "default"}`);

const loadProfile = async (showGuide = true) => {
  if (!isLoggedIn.value) {
    profile.value = {};
    return;
  }
  try {
    profile.value = await getMyProfile(false);
    if (showGuide) showGuidePopupIfNeeded();
  } catch {
    if (!hasToken()) {
      profile.value = {};
      return;
    }
    uni.showToast({ title: "资料加载失败，请稍后重试", icon: "none" });
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

const toggleAgreement = () => {
  agreementChecked.value = !agreementChecked.value;
};

const ensureAgreementAccepted = () => {
  if (agreementChecked.value) {
    uni.setStorageSync(AGREEMENT_ACCEPTED_KEY, "1");
    return true;
  }
  uni.showToast({ title: "请先阅读并同意用户协议与隐私政策", icon: "none" });
  return false;
};

const showGuidePopupIfNeeded = () => {
  if (!isLoggedIn.value || guidePromptedInSession.value || uni.getStorageSync(guideStorageKey.value)) return;
  guidePromptedInSession.value = true;
  dontShowGuideAgain.value = false;
  guidePopupVisible.value = true;
};

const closeGuidePopup = () => {
  if (dontShowGuideAgain.value) {
    uni.setStorageSync(guideStorageKey.value, "1");
  }
  guidePopupVisible.value = false;
};

const toggleDontShowGuide = () => {
  dontShowGuideAgain.value = !dontShowGuideAgain.value;
};

const openGuide = () => {
  uni.navigateTo({ url: "/pages/profile/guide" });
};

const openAgreement = () => {
  uni.navigateTo({ url: "/pages/legal/agreement" });
};

const openPrivacy = () => {
  uni.navigateTo({ url: "/pages/legal/privacy" });
};

const openGuideFromPopup = () => {
  closeGuidePopup();
  openGuide();
};

const handleWechatLogin = async () => {
  if (!ensureAgreementAccepted()) return;
  loggingIn.value = "wechat";
  try {
    await authStore.loginByWechat();
    await loadProfile(false);
    uni.showToast({ title: "登录成功", icon: "success" });
    setTimeout(showGuidePopupIfNeeded, 500);
  } finally {
    loggingIn.value = "";
  }
};

const handleMockLogin = async () => {
  if (!ensureAgreementAccepted()) return;
  loggingIn.value = "mock";
  try {
    await authStore.loginByMock();
    await loadProfile(false);
    uni.showToast({ title: "模拟登录成功", icon: "success" });
    setTimeout(showGuidePopupIfNeeded, 500);
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
  if (item.action === "guide") {
    openGuide();
    return;
  }
  if (item.action === "agreement") {
    openAgreement();
    return;
  }
  if (item.action === "privacy") {
    openPrivacy();
    return;
  }
  uni.showToast({ title: "功能规划中", icon: "none" });
};

const logout = () => {
  authStore.clearSession();
  profile.value = {};
  guidePopupVisible.value = false;
  guidePromptedInSession.value = false;
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

.agreement-row,
.guide-checkbox {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
  color: var(--cm-muted);
  font-size: 24rpx;
  line-height: 1.55;
}

.agreement-row {
  padding: 4rpx 0;
}

.check-box {
  width: 38rpx;
  height: 38rpx;
  display: grid;
  flex: 0 0 38rpx;
  place-items: center;
  margin-top: 2rpx;
  color: #fff;
  border: 2rpx solid var(--cm-line);
  border-radius: 10rpx;
  font-size: 24rpx;
  font-weight: 900;
}

.check-box.checked {
  background: var(--cm-ink);
  border-color: var(--cm-ink);
}

.agreement-copy {
  flex: 1;
  min-width: 0;
}

.legal-link {
  color: var(--cm-ink);
  font-weight: 900;
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

.guide-mask {
  position: fixed;
  z-index: 20;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48rpx 32rpx;
  background: rgba(0, 0, 0, 0.52);
}

.guide-dialog {
  width: 100%;
  max-height: 82vh;
  padding: 32rpx;
  overflow-y: auto;
  background: #fff;
  border-radius: 34rpx;
}

.guide-head {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
}

.guide-kicker {
  display: inline-flex;
  min-height: 40rpx;
  align-items: center;
  padding: 0 16rpx;
  color: var(--cm-ink);
  background: var(--cm-soft);
  border: 1rpx solid var(--cm-line);
  border-radius: 999rpx;
  font-size: 21rpx;
  font-weight: 900;
}

.guide-title {
  margin-top: 18rpx;
  color: var(--cm-ink);
  font-size: 36rpx;
  font-weight: 900;
  line-height: 1.25;
}

.guide-close {
  width: 64rpx;
  height: 64rpx;
  display: grid;
  flex: 0 0 64rpx;
  place-items: center;
  color: var(--cm-muted);
  background: #f8f7f3;
  border-radius: 22rpx;
  font-size: 42rpx;
  line-height: 1;
}

.guide-steps {
  margin: 26rpx 0 22rpx;
}

.guide-step {
  display: flex;
  gap: 18rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid var(--cm-line);
}

.guide-step:last-child {
  border-bottom: 0;
}

.guide-step-index {
  width: 48rpx;
  height: 48rpx;
  display: grid;
  flex: 0 0 48rpx;
  place-items: center;
  color: #fff;
  background: var(--cm-ink);
  border-radius: 16rpx;
  font-size: 22rpx;
  font-weight: 900;
}

.guide-step-copy {
  flex: 1;
  min-width: 0;
}

.guide-step-title {
  color: var(--cm-ink);
  font-size: 28rpx;
  font-weight: 900;
}

.guide-step-desc {
  display: block;
  margin-top: 8rpx;
  color: var(--cm-muted);
  font-size: 24rpx;
  line-height: 1.6;
}

.guide-actions {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 26rpx;
}

.version {
  margin-top: 44rpx;
  color: #aaa69b;
  font-size: 23rpx;
  text-align: center;
}
</style>
