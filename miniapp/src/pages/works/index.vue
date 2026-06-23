<template>
  <view class="page-shell">
    <view class="page-heading">
      <text class="eyebrow">WORKS</text>
      <text class="title">我的作品</text>
      <text class="subtitle">跟踪已领取任务、作品提交状态和后台审核结果。</text>
    </view>

    <view v-if="needLogin" class="surface state-card">
      <view class="empty-mark">登</view>
      <text class="empty-title">请先登录</text>
      <text class="muted">登录后才能查看已领取任务和作品审核状态。</text>
      <wd-button @click="openProfile">去登录</wd-button>
    </view>

    <view v-else-if="loading" class="surface state-card compact">
      <text class="muted">正在加载我的任务...</text>
    </view>

    <view v-else-if="claims.length === 0" class="surface state-card">
      <view class="empty-mark">作</view>
      <text class="empty-title">还没有领取任务</text>
      <text class="muted">先去任务广场领取任务，发布作品后再回来提交链接。</text>
      <wd-button @click="openTasks">去任务广场</wd-button>
    </view>

    <view v-for="claim in claims" v-else :key="claim.claimId" class="surface work-card">
      <view class="work-head">
        <view class="work-main">
          <text :class="['pill', statusTone(claim.claimStatus)]">{{ statusText(claim.claimStatus) }}</text>
          <view class="work-title">{{ claim.taskTitle || "未命名任务" }}</view>
        </view>
        <text class="platform">{{ formatPlatform(claim.platform) }}</text>
      </view>

      <view class="info-list">
        <view class="info-row">
          <text>领取时间</text>
          <text>{{ formatTime(claim.claimTime) }}</text>
        </view>
        <view v-if="claim.submitTime" class="info-row">
          <text>提交时间</text>
          <text>{{ formatTime(claim.submitTime) }}</text>
        </view>
      </view>

      <wd-button v-if="canSubmit(claim.claimStatus)" block @click="openSubmit(claim)">
        提交作品链接
      </wd-button>
      <wd-button v-else block plain @click="openTasks">继续领取任务</wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { onPullDownRefresh, onReachBottom, onShow } from "@dcloudio/uni-app";
import { listMyTasks, type ClaimStatus, type TaskClaim } from "@/api/task";
import { hasToken } from "@/utils/request";

const claims = ref<TaskClaim[]>([]);
const loading = ref(false);
const needLogin = ref(!hasToken());
const pageNum = ref(1);
const pageSize = 10;
const total = ref(0);

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

const loadClaims = async (reset = false) => {
  needLogin.value = !hasToken();
  if (needLogin.value) {
    uni.stopPullDownRefresh();
    return;
  }
  if (loading.value) return;
  loading.value = true;
  try {
    if (reset) pageNum.value = 1;
    const page = await listMyTasks({ pageNum: pageNum.value, pageSize });
    total.value = page.total || 0;
    claims.value = reset ? page.rows || [] : [...claims.value, ...(page.rows || [])];
  } catch {
    // 接口失败时回退到登录态判定，避免把"token 过期"展示成"空作品列表"
    needLogin.value = !hasToken();
    if (needLogin.value) {
      if (reset) claims.value = [];
      return;
    }
    uni.showToast({ title: "作品列表加载失败，请稍后重试", icon: "none" });
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
};

const openTasks = () => {
  uni.switchTab({ url: "/pages/tasks/index" });
};

const openProfile = () => {
  uni.switchTab({ url: "/pages/profile/index" });
};

const openSubmit = (claim: TaskClaim) => {
  uni.navigateTo({
    url: `/pages/works/submit?claimId=${encodeURIComponent(String(claim.claimId))}&taskTitle=${encodeURIComponent(
      claim.taskTitle || "",
    )}&platform=${encodeURIComponent(claim.platform || "")}`,
  });
};

onMounted(() => loadClaims(true));
onShow(() => loadClaims(true));
onPullDownRefresh(() => loadClaims(true));
onReachBottom(() => {
  if (claims.value.length >= total.value) return;
  pageNum.value += 1;
  loadClaims();
});
</script>

<style scoped lang="scss">
.work-card {
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.work-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 22rpx;
}

.work-main {
  flex: 1;
  min-width: 0;
}

.work-title {
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

.info-list {
  margin: 26rpx 0 28rpx;
  padding: 22rpx 24rpx;
  background: #f8f7f3;
  border: 1rpx solid var(--cm-line);
  border-radius: 26rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 22rpx;
  color: var(--cm-muted);
  font-size: 24rpx;
  line-height: 1.7;
}

.info-row + .info-row {
  margin-top: 10rpx;
}
</style>
