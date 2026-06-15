<template>
  <view class="page-shell">
    <view class="page-heading">
      <text class="title">任务广场</text>
      <text class="muted">领取适合你的推广任务</text>
    </view>

    <view class="surface filter-row">
      <text class="filter active">全部</text>
      <text class="filter">进行中</text>
      <text class="filter">即将结束</text>
    </view>

    <view v-for="task in tasks" :key="task.title" class="surface task-card">
      <view class="task-head">
        <wd-tag type="primary" plain>{{ task.platform }}</wd-tag>
        <text class="price">{{ task.price }}</text>
      </view>
      <view class="task-title">{{ task.title }}</view>
      <text class="task-desc">{{ task.description }}</text>
      <view class="meta-row">
        <text>剩余 {{ task.slots }} 个名额</text>
        <text>{{ task.deadline }}</text>
      </view>
      <wd-button block plain>查看任务</wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
const tasks = [
  {
    title: "某奶茶新品推广",
    platform: "抖音短视频",
    price: "¥20/条",
    slots: 24,
    deadline: "2天后截止",
    description: "发布不少于15秒的视频，使用指定素材并带上活动话题。",
  },
  {
    title: "本地门店探店",
    platform: "抖音图文",
    price: "¥35/条",
    slots: 8,
    deadline: "6月28日截止",
    description: "到店拍摄并发布真实体验，作品需保留30天。",
  },
];
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
}

.filter-row {
  display: flex;
  gap: 14rpx;
  padding: 14rpx;
  margin-bottom: 22rpx;
}

.filter {
  min-height: 72rpx;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  color: #778196;
  border-radius: 18rpx;
  font-size: 25rpx;
}

.filter.active {
  color: #2563eb;
  background: #eef4ff;
  font-weight: 600;
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
}

.task-desc {
  display: block;
  margin: 16rpx 0 22rpx;
  color: #5f6b7c;
  font-size: 26rpx;
  line-height: 1.65;
}

.meta-row {
  margin-bottom: 26rpx;
  color: #778196;
  font-size: 23rpx;
}
</style>
