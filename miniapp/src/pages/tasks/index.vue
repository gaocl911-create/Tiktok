<template>
  <view class="page-shell">
    <view class="page-heading">
      <text class="eyebrow">TASK MARKET</text>
      <text class="title">任务广场</text>
      <text class="subtitle">领取适合你的推广任务，发布作品后等待后台审核。</text>
    </view>

    <view v-if="!needLogin && profileStatus !== 'approved'" class="surface qualification-card">
      <view>
        <view class="qualification-title">{{ qualificationTitle }}</view>
        <text class="qualification-copy">{{ qualificationHint }}</text>
      </view>
      <wd-button size="small" plain @click="openProfileEdit">{{ qualificationButton }}</wd-button>
    </view>

    <view v-if="needLogin" class="surface state-card">
      <view class="empty-mark">登</view>
      <text class="empty-title">请先登录</text>
      <text class="muted">登录后才能查看和领取兼职任务。</text>
      <wd-button @click="openProfile">去登录</wd-button>
    </view>

    <view v-else-if="loading" class="surface state-card compact">
      <text class="muted">正在加载任务...</text>
    </view>

    <view v-else-if="tasks.length === 0" class="surface state-card">
      <view class="empty-mark">空</view>
      <text class="empty-title">暂无可领取任务</text>
      <text class="muted">后台发布兼职任务后，会自动显示在这里。</text>
      <wd-button plain @click="refresh">刷新</wd-button>
    </view>

    <view v-for="task in tasks" v-else :key="task.taskId" class="surface task-card">
      <view class="task-head">
        <view class="tag-row">
          <text class="pill">{{ formatPlatform(task.platform) }}</text>
          <text v-if="getClaim(task)" :class="['pill', claimTone(getClaim(task)?.claimStatus)]">
            {{ claimLabel(getClaim(task)?.claimStatus) }}
          </text>
        </view>
        <view class="price">¥{{ formatMoney(task.unitPrice) }}<text>/条</text></view>
      </view>

      <view class="task-title">{{ task.taskTitle }}</view>
      <text v-if="task.taskDesc" class="task-desc">{{ task.taskDesc }}</text>

      <view v-if="task.taskRequirement" class="requirement">
        <text class="requirement-label">任务要求</text>
        <text>{{ task.taskRequirement }}</text>
      </view>

      <view class="meta-row">
        <text>{{ quotaText(task) }}</text>
        <text>{{ claimLimitText(task) }}</text>
        <text>{{ formatEndTime(task.endTime) }}</text>
      </view>

      <wd-button
        block
        :plain="taskButtonPlain(task)"
        :disabled="taskButtonDisabled(task)"
        :loading="claimingTaskId === idKey(task.taskId)"
        @click="handleTaskAction(task)"
      >
        {{ taskButtonText(task) }}
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { onPullDownRefresh, onReachBottom, onShow } from "@dcloudio/uni-app";
import { getMyProfile, type OnboardingStatus } from "@/api/profile";
import {
  claimTask,
  listMyTasks,
  listPublishedTasks,
  type ClaimStatus,
  type PromotionTask,
  type TaskClaim,
} from "@/api/task";
import { hasToken } from "@/utils/request";

const tasks = ref<PromotionTask[]>([]);
const myClaims = ref<TaskClaim[]>([]);
const loading = ref(false);
const claimingTaskId = ref<string | null>(null);
const needLogin = ref(!hasToken());
const profileStatus = ref<OnboardingStatus>("incomplete");
const pageNum = ref(1);
const pageSize = 10;
const total = ref(0);

const idKey = (value?: number | string) => String(value ?? "");

const activeClaimStatuses: ClaimStatus[] = ["claimed", "submitted", "rejected"];

const activeClaimByTaskId = computed(() => {
  const map = new Map<string, TaskClaim>();
  myClaims.value.forEach((claim) => {
    const taskId = idKey(claim.taskId);
    if (!map.has(taskId) && activeClaimStatuses.includes(claim.claimStatus)) {
      map.set(taskId, claim);
    }
  });
  return map;
});

const latestClaimByTaskId = computed(() => {
  const map = new Map<string, TaskClaim>();
  myClaims.value.forEach((claim) => {
    const taskId = idKey(claim.taskId);
    if (!map.has(taskId)) {
      map.set(taskId, claim);
    }
  });
  return map;
});

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

const getClaim = (task: PromotionTask) => activeClaimByTaskId.value.get(idKey(task.taskId)) || latestClaimByTaskId.value.get(idKey(task.taskId));
const claimCountForTask = (task: PromotionTask) => myClaims.value.filter((claim) => idKey(claim.taskId) === idKey(task.taskId)).length;

const canClaimAgain = (task: PromotionTask) => {
  if (task.claimLimitType === "unlimited") return true;
  const claimedTimes = claimCountForTask(task);
  if (task.claimLimitType === "limited") {
    return claimedTimes < (task.claimLimitCount || 1);
  }
  return claimedTimes < 1;
};

const hasClaimedTask = (task: PromotionTask) => claimCountForTask(task) > 0;

const claimLabel = (status?: ClaimStatus) => {
  const map: Record<ClaimStatus, string> = {
    claimed: "已领取",
    submitted: "已领取",
    approved: "已领取",
    rejected: "已领取",
  };
  return status ? map[status] || status : "";
};

const claimTone = (status?: ClaimStatus) => {
  const map: Record<ClaimStatus, string> = {
    claimed: "",
    submitted: "",
    approved: "",
    rejected: "",
  };
  return status ? map[status] || "" : "";
};

const taskButtonText = (task: PromotionTask) => {
  if (profileStatus.value !== "approved") return "完善资料后领取";
  if (hasClaimedTask(task)) {
    return canClaimAgain(task) ? "继续领取" : "已领取";
  }
  return "领取任务";
};

const taskButtonPlain = (task: PromotionTask) => {
  if (profileStatus.value !== "approved") return true;
  return hasClaimedTask(task) && !canClaimAgain(task);
};

const taskButtonDisabled = (task: PromotionTask) => {
  if (profileStatus.value !== "approved") return false;
  return hasClaimedTask(task) && !canClaimAgain(task);
};

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
  if (!value) return "长期有效";
  return `截止 ${value.slice(0, 10)}`;
};

const quotaText = (task: PromotionTask) => {
  const approved = task.approvedCount || 0;
  const total = task.totalQuota || 0;
  return total <= 0 ? `通过 ${approved}/不限` : `通过 ${approved}/${total}`;
};

const claimLimitText = (task: PromotionTask) => {
  if (task.claimLimitType === "unlimited") return "每人不限次";
  if (task.claimLimitType === "limited") return `每人最多 ${task.claimLimitCount || 1} 次`;
  return "每人一次";
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
      needLogin.value = !hasToken();
      if (needLogin.value) {
        tasks.value = [];
        myClaims.value = [];
        return;
      }
      uni.showToast({ title: "资料加载失败，请稍后重试", icon: "none" });
      return;
    }
    profileStatus.value = profile.onboardingStatus || "incomplete";
    if (reset) pageNum.value = 1;

    const [taskPage, claimPage] = await Promise.all([
      listPublishedTasks({ pageNum: pageNum.value, pageSize }),
      listMyTasks({ pageNum: 1, pageSize: 500 }),
    ]);

    total.value = taskPage.total || 0;
    tasks.value = reset ? taskPage.rows || [] : [...tasks.value, ...(taskPage.rows || [])];
    myClaims.value = claimPage.rows || [];
  } catch {
    needLogin.value = !hasToken();
    if (needLogin.value) {
      tasks.value = [];
      myClaims.value = [];
      return;
    }
    uni.showToast({ title: "任务加载失败，请稍后重试", icon: "none" });
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
};

const refresh = () => loadTasks(true);

const handleTaskAction = async (task: PromotionTask) => {
  if (!hasToken()) {
    openProfile();
    return;
  }
  if (profileStatus.value !== "approved") {
    uni.showToast({ title: "请先完成兼职资料审核", icon: "none" });
    setTimeout(openProfileEdit, 600);
    return;
  }

  if (hasClaimedTask(task) && !canClaimAgain(task)) {
    uni.showToast({ title: "该任务已领取，请到作品页处理", icon: "none" });
    return;
  }

  claimingTaskId.value = idKey(task.taskId);
  try {
    const claim = await claimTask(task.taskId);
    myClaims.value = [claim, ...myClaims.value.filter((item) => idKey(item.claimId) !== idKey(claim.claimId))];
    uni.showToast({ title: "领取成功，请到作品页提交", icon: "none" });
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
.qualification-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22rpx;
  margin-bottom: 24rpx;
  padding: 28rpx;
}

.qualification-title {
  margin-bottom: 10rpx;
  color: var(--cm-ink);
  font-size: 30rpx;
  font-weight: 900;
}

.qualification-copy {
  color: var(--cm-muted);
  font-size: 24rpx;
  line-height: 1.55;
}

.task-card {
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.task-head,
.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.price {
  color: var(--cm-ink);
  font-size: 38rpx;
  font-weight: 900;
  letter-spacing: -0.04em;
}

.price text {
  margin-left: 4rpx;
  color: var(--cm-muted);
  font-size: 22rpx;
  font-weight: 700;
}

.task-title {
  margin-top: 26rpx;
  color: var(--cm-ink);
  font-size: 36rpx;
  font-weight: 900;
  line-height: 1.35;
}

.task-desc {
  display: block;
  margin: 16rpx 0 22rpx;
  color: var(--cm-muted);
  font-size: 26rpx;
  line-height: 1.65;
}

.requirement {
  margin: 20rpx 0 24rpx;
  padding: 24rpx;
  color: #34322d;
  background: #f8f7f3;
  border: 1rpx solid var(--cm-line);
  border-radius: 26rpx;
  font-size: 25rpx;
  line-height: 1.65;
}

.requirement-label {
  display: block;
  margin-bottom: 8rpx;
  color: var(--cm-ink);
  font-weight: 900;
}

.meta-row {
  margin-bottom: 28rpx;
  color: var(--cm-muted);
  font-size: 24rpx;
}
</style>
