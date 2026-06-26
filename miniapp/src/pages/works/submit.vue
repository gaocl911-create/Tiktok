<template>
  <view class="page-shell">
    <view class="surface-dark submit-hero">
      <text class="hero-pill">{{ formatPlatform(platform) }}</text>
      <view class="task-title">{{ taskTitle || "提交作品" }}</view>
      <text class="hero-copy">粘贴你已经发布的作品链接，后台审核通过后会进入作品监测。</text>
    </view>

    <view class="surface material-card">
      <view class="section-head">
        <view>
          <view class="section-title">分配素材</view>
          <text class="section-copy">按任务素材库顺序自动分配，请复制文案并保存图片后发布。</text>
        </view>
        <text v-if="claimDetail?.assignIndex" class="pill">#{{ claimDetail.assignIndex }}</text>
      </view>

      <view v-if="materialLoading" class="material-empty">正在加载素材...</view>
      <view v-else-if="!claimDetail?.assignedText && !claimDetail?.assignedImageUrl" class="material-empty">
        暂无分配素材，请返回任务广场重新进入。
      </view>
      <view v-else class="material-stack">
        <view v-if="claimDetail.assignedText" class="copy-box">
          <text class="copy-label">任务文案</text>
          <text class="copy-content">{{ claimDetail.assignedText }}</text>
          <wd-button plain block @click="copyAssignedText">复制文案</wd-button>
        </view>

        <view v-if="claimDetail.assignedImageUrl" class="image-box">
          <image class="assigned-image" :src="claimDetail.assignedImageUrl" mode="aspectFill" @click="previewAssignedImage" />
          <view class="image-actions">
            <wd-button plain size="small" @click="previewAssignedImage">预览图片</wd-button>
            <wd-button size="small" @click="saveAssignedImage">保存图片</wd-button>
          </view>
        </view>
      </view>
    </view>

    <view class="surface form-card">
      <view class="field">
        <text class="label required">作品链接</text>
        <input
          v-model="form.contentUrl"
          class="form-input"
          placeholder="例如：https://v.douyin.com/..."
          placeholder-class="placeholder"
        />
      </view>

      <wd-button block :loading="submitting" @click="submit">提交审核</wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getMyTaskClaim, submitTaskContent, type TaskClaim } from "@/api/task";

const claimId = ref("");
const taskTitle = ref("");
const platform = ref("");
const submitting = ref(false);
const materialLoading = ref(false);
const claimDetail = ref<TaskClaim | null>(null);

const form = reactive({
  contentUrl: "",
});

const formatPlatform = (value?: string) => {
  const map: Record<string, string> = {
    douyin: "抖音",
    xiaohongshu: "小红书",
  };
  return value ? map[value] || value : "未指定平台";
};

const submit = async () => {
  if (!claimId.value) {
    uni.showToast({ title: "领取记录不存在，请返回任务广场重新进入", icon: "none" });
    return;
  }

  if (!form.contentUrl.trim()) {
    uni.showToast({ title: "请填写作品链接", icon: "none" });
    return;
  }

  submitting.value = true;
  try {
    await submitTaskContent(claimId.value, {
      contentUrl: form.contentUrl.trim(),
    });
    uni.showToast({ title: "提交成功", icon: "success" });
    setTimeout(() => {
      uni.navigateBack();
    }, 600);
  } finally {
    submitting.value = false;
  }
};

const loadClaimDetail = async () => {
  if (!claimId.value) return;
  materialLoading.value = true;
  try {
    claimDetail.value = await getMyTaskClaim(claimId.value);
    taskTitle.value = claimDetail.value.taskTitle || taskTitle.value;
    platform.value = claimDetail.value.platform || platform.value;
  } finally {
    materialLoading.value = false;
  }
};

const copyAssignedText = () => {
  const text = claimDetail.value?.assignedText || "";
  if (!text) {
    uni.showToast({ title: "暂无可复制文案", icon: "none" });
    return;
  }
  uni.setClipboardData({
    data: text,
    success: () => {
      uni.showToast({ title: "文案已复制", icon: "success" });
    },
  });
};

const previewAssignedImage = () => {
  const url = claimDetail.value?.assignedImageUrl;
  if (!url) return;
  uni.previewImage({
    urls: [url],
    current: url,
  });
};

const saveAssignedImage = () => {
  const url = claimDetail.value?.assignedImageUrl;
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

onLoad((query) => {
  claimId.value = String(query?.claimId || "");
  taskTitle.value = decodeURIComponent(String(query?.taskTitle || ""));
  platform.value = decodeURIComponent(String(query?.platform || ""));
  loadClaimDetail();
});
</script>

<style scoped lang="scss">
.submit-hero {
  padding: 36rpx;
  margin-bottom: 24rpx;
}

.hero-pill {
  display: inline-flex;
  min-height: 42rpx;
  align-items: center;
  padding: 0 18rpx;
  color: #0b0b0b;
  background: #fff;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 900;
}

.task-title {
  margin: 34rpx 0 14rpx;
  color: #fff;
  font-size: 44rpx;
  font-weight: 900;
  line-height: 1.25;
  letter-spacing: -0.04em;
}

.hero-copy {
  color: rgba(255, 255, 255, 0.64);
  font-size: 25rpx;
  line-height: 1.7;
}

.material-card {
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22rpx;
}

.section-title {
  color: var(--cm-ink);
  font-size: 30rpx;
  font-weight: 900;
}

.section-copy {
  display: block;
  margin-top: 8rpx;
  color: var(--cm-muted);
  font-size: 24rpx;
  line-height: 1.55;
}

.material-empty {
  margin-top: 24rpx;
  color: var(--cm-muted);
  font-size: 24rpx;
  line-height: 1.6;
}

.material-stack {
  margin-top: 24rpx;
}

.copy-box {
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
  width: 100%;
  height: 420rpx;
  background: #f8f7f3;
  border: 1rpx solid var(--cm-line);
  border-radius: 28rpx;
}

.image-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 16rpx;
}

.form-card {
  padding: 6rpx 30rpx 30rpx;
}

.field {
  padding: 26rpx 0;
}

.label {
  display: block;
  margin-bottom: 16rpx;
  color: var(--cm-ink);
  font-size: 27rpx;
  font-weight: 900;
}

.required::after {
  content: " *";
  color: var(--cm-danger);
}
</style>
