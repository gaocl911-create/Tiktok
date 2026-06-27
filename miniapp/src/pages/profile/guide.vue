<template>
  <view class="page-shell">
    <view class="page-heading">
      <text class="eyebrow">GUIDE</text>
      <text class="title">使用操作说明</text>
      <text class="subtitle">按照下面流程完成资料审核、领取任务、发布作品和提交链接。</text>
    </view>

    <view class="surface-dark guide-hero">
      <text class="hero-kicker">完整流程</text>
      <view class="hero-title">先领取，再发布，最后提交链接</view>
      <text class="hero-copy">任务广场只负责领取任务；作品页负责提交作品链接和查看审核状态。</text>
    </view>

    <view class="section-title">新手流程</view>
    <view class="surface steps-card">
      <view v-for="(step, index) in steps" :key="step.title" class="step-item">
        <view class="step-index">{{ index + 1 }}</view>
        <view class="step-copy">
          <view class="step-title">{{ step.title }}</view>
          <text class="step-desc">{{ step.desc }}</text>
        </view>
      </view>
    </view>

    <view class="section-title">常见问题</view>
    <view class="surface faq-card">
      <view v-for="item in faqs" :key="item.question" class="faq-item">
        <view class="faq-question">{{ item.question }}</view>
        <text class="faq-answer">{{ item.answer }}</text>
      </view>
    </view>

    <view class="guide-actions">
      <wd-button block @click="openTasks">去任务广场</wd-button>
      <wd-button block plain @click="openWorks">查看我的作品</wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
const steps = [
  {
    title: "登录并完善兼职资料",
    desc: "进入个人中心登录，填写真实姓名、手机号、微信号和抖音号，提交后等待后台审核。",
  },
  {
    title: "在任务广场领取任务",
    desc: "资料审核通过后，可以进入任务广场领取任务。部分任务支持继续领取多次。",
  },
  {
    title: "复制文案并保存素材图片",
    desc: "领取后到我的作品打开待提交卡片，复制任务文案、保存分配图片，然后按要求发布作品。",
  },
  {
    title: "提交作品链接",
    desc: "作品发布后，把作品链接粘贴到提交页，提交给后台审核。",
  },
  {
    title: "查看审核状态",
    desc: "我的作品默认展示待提交内容，也可以切换到已完成查看待审核和已通过作品。",
  },
];

const faqs = [
  {
    question: "为什么任务广场没有提交按钮？",
    answer: "任务广场只负责领取任务。领取后的提交入口统一在我的作品里，避免领取和提交流程混在一起。",
  },
  {
    question: "同一个任务领取多次会怎样显示？",
    answer: "每次领取都会生成独立作品卡片，并展示第几次领取和素材编号，方便分别提交不同作品。",
  },
  {
    question: "作品被驳回后怎么办？",
    answer: "被驳回的作品会回到待提交分类，你可以按驳回原因修改后重新提交链接。",
  },
];

const openTasks = () => {
  uni.switchTab({ url: "/pages/tasks/index" });
};

const openWorks = () => {
  uni.switchTab({ url: "/pages/works/index" });
};
</script>

<style scoped lang="scss">
.guide-hero {
  padding: 34rpx;
}

.hero-kicker {
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

.hero-title {
  margin-top: 38rpx;
  color: #fff;
  font-size: 44rpx;
  font-weight: 900;
  line-height: 1.2;
}

.hero-copy {
  display: block;
  margin-top: 18rpx;
  color: rgba(255, 255, 255, 0.66);
  font-size: 26rpx;
  line-height: 1.65;
}

.steps-card,
.faq-card {
  padding: 4rpx 30rpx;
}

.step-item {
  display: flex;
  gap: 22rpx;
  padding: 30rpx 0;
  border-bottom: 1rpx solid var(--cm-line);
}

.step-item:last-child,
.faq-item:last-child {
  border-bottom: 0;
}

.step-index {
  width: 56rpx;
  height: 56rpx;
  display: grid;
  flex: 0 0 56rpx;
  place-items: center;
  color: #fff;
  background: var(--cm-ink);
  border-radius: 18rpx;
  font-size: 24rpx;
  font-weight: 900;
}

.step-copy {
  flex: 1;
  min-width: 0;
}

.step-title,
.faq-question {
  color: var(--cm-ink);
  font-size: 30rpx;
  font-weight: 900;
  line-height: 1.35;
}

.step-desc,
.faq-answer {
  display: block;
  margin-top: 12rpx;
  color: var(--cm-muted);
  font-size: 25rpx;
  line-height: 1.7;
}

.faq-item {
  padding: 30rpx 0;
  border-bottom: 1rpx solid var(--cm-line);
}

.guide-actions {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 30rpx;
}
</style>
