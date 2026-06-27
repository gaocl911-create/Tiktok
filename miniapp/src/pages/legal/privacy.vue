<template>
  <view class="page-shell">
    <view class="page-heading">
      <text class="eyebrow">PRIVACY</text>
      <text class="title">隐私政策</text>
      <text class="subtitle">本政策说明平台如何收集、使用、保存和保护你的个人信息。</text>
    </view>

    <view class="surface legal-card">
      <view class="updated">最后更新：2026-06-27</view>
      <view v-for="section in sections" :key="section.title" class="legal-section">
        <view class="legal-title">{{ section.title }}</view>
        <text v-for="item in section.items" :key="item" class="legal-text">{{ item }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
const sections = [
  {
    title: "一、我们收集的信息",
    items: [
      "登录信息：微信登录 code、openid、系统登录 token 等用于识别用户身份的信息。",
      "兼职资料：真实姓名、手机号、微信号、抖音号、所在地区、备注等用户主动提交的信息。",
      "任务与作品信息：任务领取记录、素材分配记录、作品链接、审核状态、审核结果等业务数据。",
    ],
  },
  {
    title: "二、信息使用目的",
    items: [
      "用于完成账号登录、资料审核、任务领取、素材分配、作品提交、后台审核和任务进度展示。",
      "用于管理员联系用户、确认作品归属、处理任务异常、识别重复提交或违规提交行为。",
    ],
  },
  {
    title: "三、信息保存与保护",
    items: [
      "平台会在实现业务目的所必需的范围内保存用户信息，并采取合理措施保护数据安全。",
      "未经用户授权或法律法规要求，平台不会将个人信息用于与兼职任务无关的用途。",
    ],
  },
  {
    title: "四、信息共享",
    items: [
      "平台不会主动向无关第三方出售、出租或公开用户个人信息。",
      "在完成任务审核、结算、合规要求或处理争议时，平台可能在必要范围内向相关管理人员展示必要信息。",
    ],
  },
  {
    title: "五、用户权利",
    items: [
      "用户可以在个人中心查看和修改自己的兼职资料。",
      "如需删除个人信息、注销使用记录或反馈隐私问题，可以联系平台管理员处理。",
    ],
  },
  {
    title: "六、政策更新",
    items: [
      "平台可能根据功能变化更新本隐私政策，更新后的内容会在小程序页面展示。",
      "用户继续使用平台服务，即视为已了解并接受更新后的隐私政策。",
    ],
  },
];
</script>

<style scoped lang="scss">
.legal-card {
  padding: 34rpx 32rpx;
}

.updated {
  color: var(--cm-muted);
  font-size: 24rpx;
  line-height: 1.6;
}

.legal-section {
  padding-top: 34rpx;
}

.legal-title {
  color: var(--cm-ink);
  font-size: 32rpx;
  font-weight: 900;
  line-height: 1.4;
}

.legal-text {
  display: block;
  margin-top: 16rpx;
  color: #34322d;
  font-size: 27rpx;
  line-height: 1.85;
}
</style>
