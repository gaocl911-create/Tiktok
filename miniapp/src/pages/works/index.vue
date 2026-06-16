<template>
  <view class="page-shell">
    <view class="page-heading">
      <text class="title">我的作品</text>
      <text class="muted">这里显示已领取任务、提交状态和审核结果。</text>
    </view>

    <view v-if="needLogin" class="surface state-card">
      <text class="empty-title">请先登录</text>
      <text class="muted">登录后才能查看已领取任务和作品审核状态。</text>
      <wd-button @click="openProfile">去登录</wd-button>
    </view>

    <view v-else-if="loading" class="surface state-card">
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
        <view>
          <wd-tag :type="statusType(claim.claimStatus)" plain>
            {{ statusText(claim.claimStatus) }}
          </wd-tag>
          <view class="work-title">{{ claim.taskTitle || "未命名任务" }}</view>
        </view>
        <text class="platform">{{ formatPlatform(claim.platform) }}</text>
      </view>

      <view class="info-row">
        <text>领取时间</text>
        <text>{{ formatTime(claim.claimTime) }}</text>
      </view>
      <view v-if="claim.submitTime" class="info-row">
        <text>提交时间</text>
        <text>{{ formatTime(claim.submitTime) }}</text>
      </view>

      <wd-button
        v-if="canSubmit(claim.claimStatus)"
        block
        @click="openSubmit(claim)"
      >
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

const statusType = (status: ClaimStatus) => {
  const map: Record<ClaimStatus, "primary" | "warning" | "success" | "danger"> = {
    claimed: "primary",
    submitted: "warning",
    approved: "success",
    rejected: "danger",
  };
  return map[status] || "primary";
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
    if (reset) {
      pageNum.value = 1;
    }
    const page = await listMyTasks({
      pageNum: pageNum.value,
      pageSize,
    });
    total.value = page.total || 0;
    claims.value = reset ? page.rows || [] : [...claims.value, ...(page.rows || [])];
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
    url: `/pages/works/submit?claimId=${claim.claimId}&taskTitle=${encodeURIComponent(
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
.page-heading {
  margin: 8rpx 4rpx 28rpx;
}

.title,
.page-heading .muted {
  display: block;
}

.title {
  font-size: 44rpx;
  font-weight: 800;
}

.page-heading .muted {
  margin-top: 10rpx;
  font-size: 26rpx;
  line-height: 1.6;
}

.state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;
  padding: 80rpx 40rpx;
  text-align: center;
}

.empty-mark {
  width: 104rpx;
  height: 104rpx;
  display: grid;
  place-items: center;
  color: #2563eb;
  background: #eef4ff;
  border-radius: 28rpx;
  font-size: 38rpx;
  font-weight: 800;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 700;
}

.work-card {
  padding: 28rpx;
  margin-bottom: 22rpx;
}

.work-head {
  display: flex;
  justify-content: space-between;
  gap: 22rpx;
}

.work-title {
  margin-top: 16rpx;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1.45;
}

.platform {
  color: #778196;
  font-size: 24rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin: 22rpx 0;
  color: #5f6b7c;
  font-size: 25rpx;
}
</style>
