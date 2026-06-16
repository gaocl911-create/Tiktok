<template>
  <view class="page-shell">
    <view class="welcome">
      <view>
        <text class="eyebrow">创作者任务</text>
        <view class="welcome-title">{{ greetingText }}</view>
        <text class="welcome-copy">{{ summaryText }}</text>
      </view>
      <view class="avatar">兼</view>
    </view>

    <view v-if="needLogin" class="surface state-card">
      <view class="empty-mark">登</view>
      <text class="empty-title">登录后查看你的兼职任务</text>
      <text class="muted">完成微信登录和兼职资料审核后，就可以领取任务并提交作品。</text>
      <wd-button @click="openProfile">去登录</wd-button>
    </view>

    <template v-else>
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
          <view>
            <wd-tag :type="statusType(claim.claimStatus)" plain>
              {{ statusText(claim.claimStatus) }}
            </wd-tag>
            <view class="task-title">{{ claim.taskTitle || "未命名任务" }}</view>
          </view>
          <text class="platform">{{ formatPlatform(claim.platform) }}</text>
        </view>

        <view class="task-meta">
          <text>领取时间 {{ formatTime(claim.claimTime) }}</text>
        </view>

        <wd-button
          v-if="canSubmit(claim.claimStatus)"
          block
          @click="openSubmit(claim)"
        >
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
  return needLogin.value ? "欢迎来到兼职工作台" : `${name}，你好`;
});

const summaryText = computed(() => {
  if (needLogin.value) {
    return "登录后查看你的任务、作品和审核进度。";
  }
  if (pendingSubmitCount.value > 0) {
    return `当前有 ${pendingSubmitCount.value} 个任务需要提交作品。`;
  }
  if (claims.value.length > 0) {
    return "你的任务进度会实时显示在这里。";
  }
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
.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8rpx 4rpx 32rpx;
}

.eyebrow {
  color: #2563eb;
  font-size: 24rpx;
  font-weight: 600;
}

.welcome-title {
  margin-top: 8rpx;
  color: #172033;
  font-size: 44rpx;
  font-weight: 800;
  line-height: 1.25;
}

.welcome-copy {
  display: block;
  margin-top: 10rpx;
  color: #778196;
  font-size: 27rpx;
}

.avatar {
  width: 88rpx;
  height: 88rpx;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #172033);
  border-radius: 50%;
  font-size: 30rpx;
  font-weight: 700;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18rpx;
  margin-top: 20rpx;
}

.status-card {
  padding: 28rpx 12rpx;
  text-align: center;
}

.status-card strong,
.status-card text {
  display: block;
}

.status-card strong {
  color: #172033;
  font-size: 36rpx;
}

.status-card text {
  margin-top: 8rpx;
  color: #778196;
  font-size: 24rpx;
}

.section-link {
  color: #2563eb;
  font-size: 25rpx;
  font-weight: 500;
}

.state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;
  padding: 80rpx 40rpx;
  text-align: center;
}

.state-card.compact {
  padding: 44rpx 32rpx;
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
  color: #172033;
  font-size: 32rpx;
  font-weight: 700;
}

.muted {
  color: #778196;
  font-size: 26rpx;
  line-height: 1.6;
}

.task-card {
  padding: 28rpx;
  margin-bottom: 22rpx;
}

.task-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.task-title {
  margin-top: 16rpx;
  color: #172033;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1.45;
}

.platform {
  color: #778196;
  font-size: 24rpx;
  white-space: nowrap;
}

.task-meta {
  margin: 24rpx 0 26rpx;
  color: #778196;
  font-size: 23rpx;
}
</style>
