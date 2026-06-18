<template>
  <view class="page-shell">
    <view class="page-heading">
      <text class="eyebrow">ONBOARDING</text>
      <text class="title">兼职资料</text>
      <text class="subtitle">请填写真实联系方式，审核通过后才能领取任务。</text>
    </view>

    <view class="surface status-card">
      <view>
        <view class="status-title">当前状态</view>
        <text class="status-copy">{{ statusHint }}</text>
      </view>
      <text :class="['pill', statusTone]">{{ statusText }}</text>
    </view>

    <view v-if="profile.onboardingStatus === 'rejected' && profile.rejectReason" class="surface reject-card">
      <text class="reject-title">驳回原因</text>
      <text class="reject-reason">{{ profile.rejectReason }}</text>
    </view>

    <view class="surface form-card">
      <view class="field">
        <text class="label required">真实姓名</text>
        <input
          v-model.trim="form.realName"
          class="form-input"
          placeholder="请输入真实姓名"
          placeholder-class="placeholder"
        />
      </view>
      <view class="field">
        <text class="label required">手机号</text>
        <input
          v-model.trim="form.phone"
          class="form-input"
          maxlength="11"
          placeholder="请输入手机号"
          placeholder-class="placeholder"
          type="number"
        />
      </view>
      <view class="field">
        <text class="label required">微信号</text>
        <input
          v-model.trim="form.wechatId"
          class="form-input"
          placeholder="方便管理员联系你"
          placeholder-class="placeholder"
        />
      </view>
      <view class="field">
        <text class="label required">抖音号</text>
        <input
          v-model.trim="form.douyinId"
          class="form-input"
          placeholder="请输入你的抖音号"
          placeholder-class="placeholder"
        />
      </view>
      <view class="field">
        <text class="label">所在地区</text>
        <input
          v-model.trim="form.region"
          class="form-input"
          placeholder="例如：广东 深圳"
          placeholder-class="placeholder"
        />
      </view>
      <view class="field">
        <text class="label">备注</text>
        <textarea
          v-model.trim="form.remark"
          class="form-textarea"
          maxlength="120"
          placeholder="可填写擅长内容类型、可接任务时间等"
          placeholder-class="placeholder"
        />
      </view>
    </view>

    <view class="actions">
      <wd-button block plain :disabled="!canEdit" :loading="saving" @click="handleSave">
        保存资料
      </wd-button>
      <wd-button block :disabled="!canEdit" :loading="submitting" @click="handleSubmit">
        保存并提交审核
      </wd-button>
    </view>

    <text v-if="!canEdit" class="lock-tip">
      当前状态暂不可修改资料；如需变更，请联系后台管理员。
    </text>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import {
  getMyProfile,
  submitMyProfile,
  updateMyProfile,
  type OnboardingStatus,
  type StaffProfile,
} from "@/api/profile";

const profile = ref<StaffProfile>({});
const saving = ref(false);
const submitting = ref(false);
const form = reactive({
  realName: "",
  phone: "",
  wechatId: "",
  region: "",
  douyinId: "",
  remark: "",
});

const statusMap: Record<OnboardingStatus, { text: string; hint: string; tone: "" | "warning" | "success" | "danger" }> = {
  incomplete: {
    text: "未完善",
    hint: "请补齐资料后提交审核。",
    tone: "warning",
  },
  pending: {
    text: "待审核",
    hint: "资料已提交，请等待后台审核。",
    tone: "warning",
  },
  approved: {
    text: "已通过",
    hint: "你已可以领取任务并提交作品。",
    tone: "success",
  },
  rejected: {
    text: "已驳回",
    hint: "请根据驳回原因修改后重新提交。",
    tone: "danger",
  },
};

const currentStatus = computed<OnboardingStatus>(() => profile.value.onboardingStatus || "incomplete");
const statusText = computed(() => statusMap[currentStatus.value].text);
const statusHint = computed(() => statusMap[currentStatus.value].hint);
const statusTone = computed(() => statusMap[currentStatus.value].tone);
const canEdit = computed(() => currentStatus.value === "incomplete" || currentStatus.value === "rejected");

const fillForm = (value: StaffProfile) => {
  form.realName = value.realName || "";
  form.phone = value.phone || "";
  form.wechatId = value.wechatId || "";
  form.region = value.region || "";
  form.douyinId = value.douyinId || "";
  form.remark = value.remark || "";
};

const loadProfile = async () => {
  try {
    const data = await getMyProfile(false);
    profile.value = data || {};
    fillForm(profile.value);
  } catch {
    uni.showToast({ title: "请先登录", icon: "none" });
    setTimeout(() => {
      uni.switchTab({ url: "/pages/profile/index" });
    }, 500);
  }
};

const validateForm = () => {
  if (!form.realName) return "请填写真实姓名";
  if (!/^1\d{10}$/.test(form.phone)) return "请填写正确的手机号";
  if (!form.wechatId) return "请填写微信号";
  if (!form.douyinId) return "请填写抖音号";
  return "";
};

const handleSave = async () => {
  if (!canEdit.value) return;
  saving.value = true;
  try {
    profile.value = await updateMyProfile(form);
    uni.showToast({ title: "已保存", icon: "success" });
  } finally {
    saving.value = false;
  }
};

const handleSubmit = async () => {
  if (!canEdit.value) return;
  const error = validateForm();
  if (error) {
    uni.showToast({ title: error, icon: "none" });
    return;
  }
  submitting.value = true;
  try {
    await updateMyProfile(form);
    profile.value = await submitMyProfile();
    uni.showToast({ title: "已提交审核", icon: "success" });
  } finally {
    submitting.value = false;
  }
};

onShow(loadProfile);
</script>

<style scoped lang="scss">
.status-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
}

.status-title {
  margin-bottom: 8rpx;
  color: var(--cm-ink);
  font-size: 30rpx;
  font-weight: 900;
}

.status-copy {
  color: var(--cm-muted);
  font-size: 24rpx;
  line-height: 1.55;
}

.reject-card {
  padding: 26rpx 30rpx;
  margin-bottom: 24rpx;
  background: #fff0ef;
  border-color: #efc1bd;
}

.reject-title,
.reject-reason {
  display: block;
}

.reject-title {
  color: var(--cm-danger);
  font-size: 28rpx;
  font-weight: 900;
}

.reject-reason {
  margin-top: 10rpx;
  color: #7f1d1d;
  font-size: 25rpx;
  line-height: 1.6;
}

.form-card {
  padding: 6rpx 30rpx;
}

.field {
  padding: 26rpx 0;
  border-bottom: 1rpx solid var(--cm-line);
}

.field:last-child {
  border-bottom: 0;
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

.actions {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 30rpx;
}

.lock-tip {
  display: block;
  margin-top: 20rpx;
  color: var(--cm-muted);
  font-size: 24rpx;
  line-height: 1.6;
  text-align: center;
}
</style>
