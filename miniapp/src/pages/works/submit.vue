<template>
  <view class="page-shell">
    <view class="surface-dark submit-hero">
      <text class="hero-pill">{{ formatPlatform(platform) }}</text>
      <view class="task-title">{{ taskTitle || "提交作品" }}</view>
      <text class="hero-copy">粘贴你已经发布的作品链接，后台审核通过后会进入作品监测。</text>
    </view>

    <view class="surface ai-card">
      <view>
        <view class="ai-title">AI 文案助手</view>
        <text class="ai-copy">后续可以在这里接入库存图片识别与文案生成。</text>
      </view>
      <text class="pill">规划中</text>
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

      <view class="field">
        <text class="label">作品文案</text>
        <textarea
          v-model="form.contentDesc"
          class="form-textarea"
          placeholder="可填写作品标题、文案或备注，方便后台审核"
          placeholder-class="placeholder"
          maxlength="500"
        />
      </view>

      <view class="field">
        <text class="label">截图地址（可选）</text>
        <input
          v-model="form.screenshotUrl"
          class="form-input"
          placeholder="暂时填写图片链接，后续再接上传"
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
import { submitTaskContent } from "@/api/task";

const claimId = ref("");
const taskTitle = ref("");
const platform = ref("");
const submitting = ref(false);

const form = reactive({
  contentUrl: "",
  contentDesc: "",
  screenshotUrl: "",
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
      contentDesc: form.contentDesc.trim(),
      screenshotUrl: form.screenshotUrl.trim(),
    });
    uni.showToast({ title: "提交成功", icon: "success" });
    setTimeout(() => {
      uni.navigateBack();
    }, 600);
  } finally {
    submitting.value = false;
  }
};

onLoad((query) => {
  claimId.value = String(query?.claimId || "");
  taskTitle.value = decodeURIComponent(String(query?.taskTitle || ""));
  platform.value = decodeURIComponent(String(query?.platform || ""));
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

.ai-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.ai-title {
  color: var(--cm-ink);
  font-size: 30rpx;
  font-weight: 900;
}

.ai-copy {
  display: block;
  margin-top: 8rpx;
  color: var(--cm-muted);
  font-size: 24rpx;
  line-height: 1.55;
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
