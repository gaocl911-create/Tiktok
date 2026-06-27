<template>
  <view class="page-shell">
    <view class="page-heading">
      <text class="eyebrow">TERMS</text>
      <text class="title">用户协议</text>
      <text class="subtitle">请在登录、完善资料和参与任务前仔细阅读本协议。</text>
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
    title: "一、服务内容",
    items: [
      "本平台为创作者兼职任务工具，提供任务展示、任务领取、素材分配、作品提交、审核进度查看等服务。",
      "平台会根据后台发布的任务规则、用户资料状态和任务名额情况，决定用户是否可以领取或继续领取任务。",
    ],
  },
  {
    title: "二、账号与资料",
    items: [
      "用户应使用本人真实信息完善兼职资料，包括姓名、手机号、微信号、抖音号等必要信息。",
      "用户应保证提交资料真实、准确、完整。如资料不真实或无法联系，平台有权拒绝审核或暂停相关任务权限。",
    ],
  },
  {
    title: "三、任务领取与作品提交",
    items: [
      "用户应按照任务要求领取任务、使用分配素材、发布作品并提交真实有效的作品链接。",
      "同一个任务是否允许多次领取，以任务发布时设置的领取规则为准。",
      "用户提交的作品链接应与本人发布作品一致，不得提交无关链接、重复链接、失效链接或他人作品链接。",
    ],
  },
  {
    title: "四、审核与违规处理",
    items: [
      "后台会根据任务要求对提交作品进行审核，审核结果可能为待审核、已通过或已驳回。",
      "如发现刷量、搬运、虚假提交、恶意重复提交、冒用他人账号等行为，平台有权驳回作品、限制领取任务或停止账号使用。",
    ],
  },
  {
    title: "五、协议变更",
    items: [
      "平台可根据业务调整更新本协议内容。协议更新后，会通过小程序页面展示最新版本。",
      "用户继续使用平台服务，即视为已阅读并接受更新后的协议。",
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
