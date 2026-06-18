<template>
  <view class="page-shell">
    <view class="page-heading">
      <text class="eyebrow">CREATOR TASKS</text>
      <text class="title">{{ greetingText }}</text>
      <text class="subtitle">{{ summaryText }}</text>
    </view>

    <view v-if="needLogin" class="surface-dark hero-card">
      <view class="hero-top">
        <view class="hero-logo">CM</view>
        <text class="hero-status">未登录</text>
      </view>
      <view class="hero-title">登录后开始接任务</view>
      <text class="hero-copy">完成资料审核后，可以领取推广任务、提交作品并查看审核进度。</text>
      <wd-button block custom-class="hero-button" @click="openProfile">去登录</wd-button>
    </view>

    <template v-else>
      <view class="surface-dark hero-card">
        <view class="hero-top">
          <view class="hero-logo">CM</view>
          <text class="hero-status">工作台</text>
        </view>
        <view class="hero-title">{{ pendingSubmitCount }} 个待提交</view>
        <text class="hero-copy">任务进度会在这里汇总，优先处理待提交和被驳回的作品。</text>
      </view>

      <view class="status-grid">
        <view v-for="item in statuses" :key="item.label" class="surface status-card">
          <strong>{{ item.value }}</strong>
          <text>{{ item.label }}</text>
        </view>
      </view>

      <view class="section-title">
        <text>我的任务</text>
        <text class="section-link" @click="openWorks">查看全部</text>
      </view>

      <view v-if="loading" class="surface state-card compact">
        <text class="muted">正在加载任务...</text>
      </view>

      <view v-else-if="displayClaims.length === 0" class="surface state-card">
        <view class="empty-mark">任</view>
        <text class="empty-title">暂无进行中的任务</text>
        <text class="muted">可以先去任务广场看看后台发布的真实兼职任务。</text>
        <wd-button @click="openTasks">去任务广场</wd-button>
      </view>

      <view v-for="claim in displayClaims" v-else :key="claim.claimId" class="surface task-card">
        <view class="task-row">
          <view class="task-main">
            <text :class="['pill', statusTone(claim.claimStatus)]">{{ statusText(claim.claimStatus) }}</text>
            <view class="task-title">{{ claim.taskTitle || "未命名任务" }}</view>
          </view>
          <text class="platform">{{ formatPlatform(claim.platform) }}</text>
        </view>

        <view class="task-meta">领取时间 {{ formatTime(claim.claimTime) }}</view>

        <wd-button v-if="canSubmit(claim.claimStatus)" block @click="openSubmit(claim)">
          提交作品链接
        </wd-button>
        <wd-button v-else block plain @click="openWorks">查看处理进度</wd-button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getMyProfile, type StaffProfile } from "@/api/profile";
import { listMyTasks, type ClaimStatus, type TaskClaim } from "@/api/task";
import { hasToken } from "@/utils/request";

const claims = ref<TaskClaim[]>([]);
const profile = ref<StaffProfile | null>(null);
const loading = ref(false);
const needLogin = ref(!hasToken());

const pendingSubmitCount = computed(
  () => claims.value.filter((item) => item.claimStatus === "claimed" || item.claimStatus === "rejected").length,
);

const statuses = computed(() => [
  { label: "待提交", value: pendingSubmitCount.value },
  { label: "待审核", value: claims.value.filter((item) => item.claimStatus === "submitted").length },
  { label: "已通过", value: claims.value.filter((item) => item.claimStatus === "approved").length },
]);

const displayClaims = computed(() =>
  claims.value
    .filter((item) => item.claimStatus === "claimed" || item.claimStatus === "rejected" || item.claimStatus === "submitted")
    .slice(0, 2),
);

const greetingText = computed(() => {
  const name = profile.value?.realName || "欢迎回来";
  return needLogin.value ? "兼职工作台" : `${name}，你好`;
});

const summaryText = computed(() => {
  if (needLogin.value) return "一个更干净的任务领取与作品提交入口。";
  if (pendingSubmitCount.value > 0) return `当前有 ${pendingSubmitCount.value} 个任务需要提交作品。`;
  if (claims.value.length > 0) return "你的任务进度会实时显示在这里。";
  return "暂无已领取任务，可以去任务广场领取。";
});

const statusText = (status: ClaimStatus) => {
  const map: Record<ClaimStatus, string> = {
    claimed: "待提交",
    submitted: "待审核",
    approved: "已通过",
    rejected: "已驳回",
  };
  return map[status] || status;
};

const statusTone = (status: ClaimStatus) => {
  const map: Record<ClaimStatus, string> = {
    claimed: "",
    submitted: "warning",
    approved: "success",
    rejected: "danger",
  };
  return map[status] || "";
};

const canSubmit = (status: ClaimStatus) => status === "claimed" || status === "rejected";

const formatPlatform = (platform?: string) => {
  const map: Record<string, string> = {
    douyin: "抖音",
    xiaohongshu: "小红书",
  };
  return platform ? map[platform] || platform : "未指定平台";
};

const formatTime = (value?: string) => (value ? value.replace("T", " ").slice(0, 16) : "-");

const loadHome = async () => {
  needLogin.value = !hasToken();
  if (needLogin.value) {
    claims.value = [];
    profile.value = null;
    return;
  }

  if (loading.value) return;
  loading.value = true;
  try {
    const [profileData, taskPage] = await Promise.all([
      getMyProfile(false),
      listMyTasks({ pageNum: 1, pageSize: 100 }),
    ]);
    profile.value = profileData;
    claims.value = taskPage.rows || [];
  } catch {
    needLogin.value = true;
    claims.value = [];
    profile.value = null;
  } finally {
    loading.value = false;
  }
};

const openTasks = () => {
  uni.switchTab({ url: "/pages/tasks/index" });
};

const openWorks = () => {
  uni.switchTab({ url: "/pages/works/index" });
};

const openProfile = () => {
  uni.switchTab({ url: "/pages/profile/index" });
};

const openSubmit = (claim: TaskClaim) => {
  uni.navigateTo({
    url: `/pages/works/submit?claimId=${claim.claimId}&taskTitle=${encodeURIComponent(
      claim.taskTitle || "",
    )}&platform=${encodeURIComponent(claim.platform || "")}`,
  });
};

onShow(loadHome);
</script>

<style scoped lang="scss">
.hero-card {
  padding: 34rpx;
}

.hero-top,
.task-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.hero-logo {
  width: 70rpx;
  height: 70rpx;
  display: grid;
  place-items: center;
  color: #0b0b0b;
  background: #fff;
  border-radius: 22rpx;
  font-size: 24rpx;
  font-weight: 900;
}

.hero-status {
  color: rgba(255, 255, 255, 0.66);
  font-size: 24rpx;
  font-weight: 700;
}

.hero-title {
  margin-top: 44rpx;
  color: #fff;
  font-size: 52rpx;
  font-weight: 900;
  letter-spacing: -0.04em;
}

.hero-copy {
  display: block;
  margin: 16rpx 0 30rpx;
  color: rgba(255, 255, 255, 0.64);
  font-size: 26rpx;
  line-height: 1.65;
}

:deep(.hero-button) {
  background: #fff !important;
  color: #0b0b0b !important;
  border-color: #fff !important;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18rpx;
  margin-top: 20rpx;
}

.status-card {
  padding: 30rpx 12rpx;
  text-align: center;
}

.status-card strong,
.status-card text {
  display: block;
}

.status-card strong {
  color: var(--cm-ink);
  font-size: 40rpx;
  font-weight: 900;
}

.status-card text {
  margin-top: 8rpx;
  color: var(--cm-muted);
  font-size: 24rpx;
}

.task-card {
  padding: 30rpx;
  margin-bottom: 22rpx;
}

.task-main {
  flex: 1;
  min-width: 0;
}

.task-title {
  margin-top: 18rpx;
  color: var(--cm-ink);
  font-size: 34rpx;
  font-weight: 900;
  line-height: 1.35;
}

.platform {
  color: var(--cm-muted);
  font-size: 24rpx;
  font-weight: 700;
  white-space: nowrap;
}

.task-meta {
  margin: 24rpx 0 28rpx;
  color: var(--cm-muted);
  font-size: 24rpx;
}
</style>
