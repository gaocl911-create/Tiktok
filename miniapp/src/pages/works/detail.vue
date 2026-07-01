<template>
  <view class="page-shell">
    <view v-if="loading" class="surface state-card compact">
      <text class="muted">正在加载作品详情...</text>
    </view>

    <view v-else-if="!claim" class="surface state-card">
      <view class="empty-mark">?</view>
      <text class="empty-title">领取记录不存在</text>
      <text class="muted">请返回我的作品重新进入。</text>
      <wd-button @click="backToWorks">返回我的作品</wd-button>
    </view>

    <template v-else>
      <view class="surface-dark detail-hero">
        <view class="hero-top">
          <text class="hero-pill">{{ formatPlatform(claim.platform) }}</text>
          <text :class="['status-pill', statusTone(claim.claimStatus)]">{{ statusText(claim.claimStatus) }}</text>
        </view>
        <view class="task-title">{{ claim.taskTitle || "未命名任务" }}</view>
        <text class="hero-copy">这是你本次领取任务的素材、提交记录和后台审核结果。</text>
      </view>

      <view class="surface info-card">
        <view class="section-title">领取信息</view>
        <view class="info-list">
          <view class="info-row">
            <text>领取次数</text>
            <text>{{ claim.claimRound ? `第 ${claim.claimRound} 次领取` : "-" }}</text>
          </view>
          <view class="info-row">
            <text>素材序号</text>
            <text>{{ claim.assignIndex ? `#${claim.assignIndex}` : "-" }}</text>
          </view>
          <view class="info-row">
            <text>领取时间</text>
            <text>{{ formatTime(claim.claimTime) }}</text>
          </view>
          <view class="info-row">
            <text>提交时间</text>
            <text>{{ formatTime(claim.submitTime) }}</text>
          </view>
          <view v-if="claim.auditTime" class="info-row">
            <text>审核时间</text>
            <text>{{ formatTime(claim.auditTime) }}</text>
          </view>
        </view>
      </view>

      <view class="surface material-card">
        <view class="section-head">
          <view>
            <view class="section-title">分配素材</view>
            <text class="section-copy">按任务素材库顺序自动分配。可以在这里再次复制文案或保存图片。</text>
          </view>
        </view>

        <view v-if="!claim.assignedText && !claim.assignedImageUrl" class="empty-inline">
          暂无分配素材。
        </view>

        <view v-if="claim.assignedText" class="copy-box">
          <text class="copy-label">任务文案</text>
          <text class="copy-content">{{ claim.assignedText }}</text>
          <wd-button plain block @click="copyAssignedText">复制文案</wd-button>
        </view>

        <view v-if="claim.assignedImageUrl" class="image-box">
          <image class="assigned-image" :src="claim.assignedImageUrl" mode="aspectFit" @click="previewAssignedImage" />
          <view class="image-actions">
            <wd-button plain size="small" @click="previewAssignedImage">预览图片</wd-button>
            <wd-button size="small" @click="saveAssignedImage">保存图片</wd-button>
          </view>
        </view>
      </view>

      <view class="surface info-card">
        <view class="section-title">提交与审核</view>
        <view v-if="!claim.contentUrl" class="empty-inline">
          当前还没有提交作品链接。
        </view>
        <view v-else class="submit-box">
          <view class="submit-url" @click="copyContentUrl">{{ claim.contentUrl }}</view>
          <view v-if="claim.contentDesc" class="submit-desc">{{ claim.contentDesc }}</view>
          <view v-if="claim.rejectReason" class="reject-box">
            <text class="reject-label">驳回原因</text>
            <text class="reject-text">{{ claim.rejectReason }}</text>
          </view>
        </view>

        <view class="action-stack">
          <wd-button v-if="canSubmit(claim.claimStatus)" block @click="openSubmit">提交/重新提交作品</wd-button>
          <wd-button v-if="claim.contentUrl" block plain @click="copyContentUrl">复制作品链接</wd-button>
          <wd-button v-if="claim.contentUrl" block plain @click="openContentUrl">打开作品</wd-button>
          <wd-button block plain @click="backToWorks">返回我的作品</wd-button>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad, onPullDownRefresh } from "@dcloudio/uni-app";
import { getMyTaskClaim, type ClaimStatus, type TaskClaim } from "@/api/task";

const claimId = ref("");
const claim = ref<TaskClaim | null>(null);
const loading = ref(false);

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

const loadDetail = async () => {
  if (!claimId.value) {
    claim.value = null;
    return;
  }
  loading.value = true;
  try {
    claim.value = await getMyTaskClaim(claimId.value);
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
};

const copyAssignedText = () => {
  const text = claim.value?.assignedText || "";
  if (!text) {
    uni.showToast({ title: "暂无可复制文案", icon: "none" });
    return;
  }
  uni.setClipboardData({
    data: text,
    success: () => uni.showToast({ title: "文案已复制", icon: "success" }),
  });
};

const copyContentUrl = () => {
  const url = claim.value?.contentUrl || "";
  if (!url) {
    uni.showToast({ title: "暂无作品链接", icon: "none" });
    return;
  }
  uni.setClipboardData({
    data: url,
    success: () => uni.showToast({ title: "作品链接已复制", icon: "success" }),
  });
};

const previewAssignedImage = () => {
  const url = claim.value?.assignedImageUrl;
  if (!url) return;
  uni.previewImage({
    urls: [url],
    current: url,
  });
};

const saveAssignedImage = () => {
  const url = claim.value?.assignedImageUrl;
  if (!url) {
    uni.showToast({ title: "暂无可保存图片", icon: "none" });
    return;
  }
  uni.showLoading({ title: "保存中" });
  uni.downloadFile({
    url,
    success: (res) => {
      if (res.statusCode !== 200 || !res.tempFilePath) {
        uni.showToast({ title: "图片下载失败", icon: "none" });
        return;
      }
      uni.saveImageToPhotosAlbum({
        filePath: res.tempFilePath,
        success: () => uni.showToast({ title: "图片已保存", icon: "success" }),
        fail: () => uni.showToast({ title: "保存失败，请检查相册权限", icon: "none" }),
      });
    },
    fail: () => uni.showToast({ title: "图片下载失败", icon: "none" }),
    complete: () => uni.hideLoading(),
  });
};

const openContentUrl = () => {
  const url = claim.value?.contentUrl;
  if (!url) return;
  // 微信小程序不能任意打开外部网页，这里先复制链接，方便用户到抖音查看。
  uni.setClipboardData({
    data: url,
    success: () => uni.showToast({ title: "已复制链接，请到抖音打开", icon: "none" }),
  });
};

const openSubmit = () => {
  if (!claim.value) return;
  uni.navigateTo({
    url: `/pages/works/submit?claimId=${encodeURIComponent(String(claim.value.claimId))}&taskTitle=${encodeURIComponent(
      claim.value.taskTitle || "",
    )}&platform=${encodeURIComponent(claim.value.platform || "")}`,
  });
};

const backToWorks = () => {
  uni.switchTab({ url: "/pages/works/index" });
};

onLoad((query) => {
  claimId.value = String(query?.claimId || "");
  loadDetail();
});

onPullDownRefresh(loadDetail);
</script>

<style scoped lang="scss">
.detail-hero {
  padding: 36rpx;
  margin-bottom: 24rpx;
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.hero-pill,
.status-pill {
  display: inline-flex;
  min-height: 42rpx;
  align-items: center;
  padding: 0 18rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 900;
}

.hero-pill {
  color: #0b0b0b;
  background: #fff;
}

.status-pill {
  color: #fff;
  border: 1rpx solid rgba(255, 255, 255, 0.24);
}

.status-pill.warning {
  color: #8a5b00;
  background: #fff5d8;
}

.status-pill.success {
  color: #0f7a42;
  background: #dcf8e8;
}

.status-pill.danger {
  color: #b3261e;
  background: #ffe8e6;
}

.task-title {
  margin: 34rpx 0 14rpx;
  color: #fff;
  font-size: 44rpx;
  font-weight: 900;
  line-height: 1.25;
  letter-spacing: -0.04em;
}

.hero-copy,
.section-copy,
.empty-inline,
.submit-desc {
  color: var(--cm-muted);
  font-size: 24rpx;
  line-height: 1.65;
}

.hero-copy {
  color: rgba(255, 255, 255, 0.64);
}

.info-card,
.material-card {
  padding: 30rpx;
  margin-bottom: 24rpx;
}

.section-title {
  color: var(--cm-ink);
  font-size: 31rpx;
  font-weight: 900;
}

.section-copy {
  display: block;
  margin-top: 8rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 22rpx;
}

.info-list {
  margin-top: 22rpx;
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

.info-row text:last-child {
  color: var(--cm-ink);
  font-weight: 800;
  text-align: right;
}

.empty-inline {
  margin-top: 22rpx;
}

.copy-box,
.submit-box {
  margin-top: 24rpx;
  padding: 24rpx;
  background: #f8f7f3;
  border: 1rpx solid var(--cm-line);
  border-radius: 26rpx;
}

.copy-label {
  display: block;
  margin-bottom: 14rpx;
  color: var(--cm-ink);
  font-size: 25rpx;
  font-weight: 900;
}

.copy-content {
  display: block;
  margin-bottom: 22rpx;
  color: #34322d;
  font-size: 25rpx;
  line-height: 1.7;
  white-space: pre-wrap;
}

.image-box {
  margin-top: 22rpx;
}

.assigned-image {
  display: block;
  width: 88%;
  height: 320rpx;
  margin: 0 auto;
  background: #f8f7f3;
  border: 1rpx solid var(--cm-line);
  border-radius: 28rpx;
}

.image-actions {
  display: flex;
  gap: 16rpx;
  width: 88%;
  margin: 16rpx auto 0;
}

.submit-url {
  color: var(--cm-ink);
  font-size: 25rpx;
  font-weight: 800;
  line-height: 1.6;
  word-break: break-all;
}

.submit-desc {
  margin-top: 14rpx;
}

.reject-box {
  margin-top: 22rpx;
  padding: 20rpx;
  background: #fff3f2;
  border: 1rpx solid #ffd3cf;
  border-radius: 20rpx;
}

.reject-label,
.reject-text {
  display: block;
}

.reject-label {
  color: #b3261e;
  font-size: 23rpx;
  font-weight: 900;
}

.reject-text {
  margin-top: 8rpx;
  color: #7a211c;
  font-size: 24rpx;
  line-height: 1.6;
}

.action-stack {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 26rpx;
}
</style>
