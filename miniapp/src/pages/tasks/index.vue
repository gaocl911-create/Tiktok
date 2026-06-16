<template>
  <view class="page-shell">
    <view class="page-heading">
      <text class="title">任务广场</text>
      <text class="muted">领取适合你的推广任务，发布作品后等待后台审核。</text>
    </view>

    <view class="surface notice">
      <text>当前展示后台已发布、未过期的兼职任务。</text>
    </view>

    <view v-if="!needLogin && profileStatus !== 'approved'" class="surface qualification-card">
      <view>
        <view class="qualification-title">{{ qualificationTitle }}</view>
        <text class="muted">{{ qualificationHint }}</text>
      </view>
      <wd-button size="small" @click="openProfileEdit">{{ qualificationButton }}</wd-button>
    </view>

    <view v-if="needLogin" class="surface state-card">
      <text class="empty-title">请先登录</text>
      <text class="muted">登录后才能查看和领取兼职任务。</text>
      <wd-button @click="openProfile">去登录</wd-button>
    </view>

    <view v-else-if="loading" class="surface state-card">
      <text class="muted">正在加载任务...</text>
    </view>

    <view v-else-if="tasks.length === 0" class="surface state-card">
      <text class="empty-title">暂无可领取任务</text>
      <text class="muted">后台发布兼职任务后，会自动显示在这里。</text>
      <wd-button plain @click="refresh">刷新</wd-button>
    </view>

    <view v-for="task in tasks" v-else :key="task.taskId" class="surface task-card">
      <view class="task-head">
        <wd-tag type="primary" plain>{{ formatPlatform(task.platform) }}</wd-tag>
        <text class="price">￥{{ formatMoney(task.unitPrice) }}/条</text>
      </view>

      <view class="task-title">{{ task.taskTitle }}</view>
      <text v-if="task.taskDesc" class="task-desc">{{ task.taskDesc }}</text>

      <view v-if="task.taskRequirement" class="requirement">
        <text class="requirement-label">任务要求</text>
        <text>{{ task.taskRequirement }}</text>
      </view>

      <view class="meta-row">
        <text>名额 {{ task.claimedCount || 0 }}/{{ task.totalQuota || 0 }}</text>
        <text>{{ formatEndTime(task.endTime) }}</text>
      </view>

      <wd-button
        block
        :plain="profileStatus !== 'approved'"
        :loading="claimingTaskId === task.taskId"
        @click="handleClaim(task)"
      >
        {{ profileStatus === "approved" ? "领取任务" : "完善资料后领取" }}
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { onPullDownRefresh, onReachBottom, onShow } from "@dcloudio/uni-app";
import { getMyProfile, type OnboardingStatus } from "@/api/profile";
import { claimTask, listPublishedTasks, type PromotionTask } from "@/api/task";
import { hasToken } from "@/utils/request";

const tasks = ref<PromotionTask[]>([]);
const loading = ref(false);
const claimingTaskId = ref<number | null>(null);
const needLogin = ref(!hasToken());
const profileStatus = ref<OnboardingStatus>("incomplete");
const pageNum = ref(1);
const pageSize = 10;
const total = ref(0);

const qualificationTitle = computed(() => {
  const map: Record<OnboardingStatus, string> = {
    incomplete: "请先完善兼职资料",
    pending: "资料正在审核中",
    approved: "资料已审核通过",
    rejected: "资料审核被驳回",
  };
  return map[profileStatus.value];
});

const qualificationHint = computed(() => {
  const map: Record<OnboardingStatus, string> = {
    incomplete: "补齐手机号、微信号和抖音号，并提交后台审核后才能领取任务。",
    pending: "后台审核通过后，就可以领取任务并提交作品。",
    approved: "现在可以领取任务。",
    rejected: "请根据驳回原因修改资料后重新提交审核。",
  };
  return map[profileStatus.value];
});

const qualificationButton = computed(() => (profileStatus.value === "pending" ? "查看资料" : "去完善"));

const formatPlatform = (platform?: string) => {
  const map: Record<string, string> = {
    douyin: "抖音",
    xiaohongshu: "小红书",
  };
  return platform ? map[platform] || platform : "未指定平台";
};

const formatMoney = (value?: number | string) => {
  const amount = Number(value || 0);
  return amount.toFixed(2).replace(/\.00$/, "");
};

const formatEndTime = (value?: string) => {
  if (!value) {
    return "长期有效";
  }
  return `截止 ${value.slice(0, 10)}`;
};

const loadTasks = async (reset = false) => {
  needLogin.value = !hasToken();
  if (needLogin.value) {
    uni.stopPullDownRefresh();
    return;
  }
  if (loading.value) return;
  loading.value = true;
  try {
    let profile;
    try {
      profile = await getMyProfile(false);
    } catch {
      needLogin.value = true;
      return;
    }
    profileStatus.value = profile.onboardingStatus || "incomplete";
    if (reset) {
      pageNum.value = 1;
    }
    const page = await listPublishedTasks({
      pageNum: pageNum.value,
      pageSize,
    });
    total.value = page.total || 0;
    tasks.value = reset ? page.rows || [] : [...tasks.value, ...(page.rows || [])];
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
};

const refresh = () => loadTasks(true);

const handleClaim = async (task: PromotionTask) => {
  if (!hasToken()) {
    openProfile();
    return;
  }
  if (profileStatus.value !== "approved") {
    uni.showToast({ title: "请先完成兼职资料审核", icon: "none" });
    setTimeout(openProfileEdit, 600);
    return;
  }
  claimingTaskId.value = task.taskId;
  try {
    await claimTask(task.taskId);
    uni.showToast({ title: "领取成功", icon: "success" });
    setTimeout(() => {
      uni.switchTab({ url: "/pages/works/index" });
    }, 600);
  } finally {
    claimingTaskId.value = null;
  }
};

const openProfile = () => {
  uni.switchTab({ url: "/pages/profile/index" });
};

const openProfileEdit = () => {
  uni.navigateTo({ url: "/pages/profile/edit" });
};

onMounted(refresh);
onShow(refresh);
onPullDownRefresh(refresh);
onReachBottom(() => {
  if (tasks.value.length >= total.value) return;
  pageNum.value += 1;
  loadTasks();
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

.notice {
  margin-bottom: 22rpx;
  padding: 22rpx 24rpx;
  color: #5f6b7c;
  font-size: 25rpx;
}

.qualification-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 22rpx;
  padding: 24rpx;
}

.qualification-title {
  margin-bottom: 8rpx;
  font-size: 29rpx;
  font-weight: 700;
}

.qualification-card .muted {
  display: block;
  font-size: 24rpx;
  line-height: 1.55;
}

.state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;
  padding: 80rpx 40rpx;
  text-align: center;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 700;
}

.task-card {
  padding: 28rpx;
  margin-bottom: 22rpx;
}

.task-head,
.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price {
  color: #d97706;
  font-size: 30rpx;
  font-weight: 700;
}

.task-title {
  margin-top: 22rpx;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1.45;
}

.task-desc {
  display: block;
  margin: 16rpx 0 22rpx;
  color: #5f6b7c;
  font-size: 26rpx;
  line-height: 1.65;
}

.requirement {
  margin: 18rpx 0 22rpx;
  padding: 20rpx;
  color: #3b4658;
  background: #f8fafc;
  border-radius: 18rpx;
  font-size: 25rpx;
  line-height: 1.6;
}

.requirement-label {
  display: block;
  margin-bottom: 8rpx;
  color: #172033;
  font-weight: 700;
}

.meta-row {
  margin-bottom: 26rpx;
  color: #778196;
  font-size: 23rpx;
}
</style>
