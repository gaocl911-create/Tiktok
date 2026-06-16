<template>
  <view class="page-shell">
    <view class="surface submit-card">
      <wd-tag type="primary" plain>{{ formatPlatform(platform) }}</wd-tag>
      <view class="task-title">{{ taskTitle || "提交作品" }}</view>
      <text class="muted">请粘贴你已发布的作品链接，后台审核通过后会自动进入作品监测。</text>
    </view>

    <view class="surface form-card">
      <view class="field">
        <text class="label">作品链接</text>
        <input
          v-model="form.contentUrl"
          class="input"
          placeholder="例如：https://v.douyin.com/..."
          placeholder-class="placeholder"
        />
      </view>

      <view class="field">
        <text class="label">作品文案</text>
        <textarea
          v-model="form.contentDesc"
          class="textarea"
          placeholder="可填写作品标题、文案或备注，方便后台审核"
          placeholder-class="placeholder"
          maxlength="500"
        />
      </view>

      <view class="field">
        <text class="label">截图地址（可选）</text>
        <input
          v-model="form.screenshotUrl"
          class="input"
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

const claimId = ref(0);
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
  claimId.value = Number(query?.claimId || 0);
  taskTitle.value = decodeURIComponent(String(query?.taskTitle || ""));
  platform.value = decodeURIComponent(String(query?.platform || ""));
});
</script>

<style scoped lang="scss">
.submit-card,
.form-card {
  padding: 30rpx;
}

.submit-card {
  margin-bottom: 22rpx;
}

.task-title {
  margin: 18rpx 0 12rpx;
  font-size: 34rpx;
  font-weight: 800;
  line-height: 1.45;
}

.submit-card .muted {
  font-size: 25rpx;
  line-height: 1.7;
}

.field {
  margin-bottom: 30rpx;
}

.label {
  display: block;
  margin-bottom: 14rpx;
  color: #172033;
  font-size: 27rpx;
  font-weight: 700;
}

.input,
.textarea {
  width: 100%;
  color: #172033;
  background: #f8fafc;
  border: 1rpx solid #e8edf3;
  border-radius: 18rpx;
  font-size: 26rpx;
}

.input {
  height: 88rpx;
  padding: 0 24rpx;
}

.textarea {
  min-height: 180rpx;
  padding: 22rpx 24rpx;
  line-height: 1.6;
}

.placeholder {
  color: #a4adba;
}
</style>
