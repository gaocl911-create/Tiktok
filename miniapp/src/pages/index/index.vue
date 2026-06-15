<template>
  <view class="page-shell">
    <view class="welcome">
      <view>
        <text class="eyebrow">创作者任务</text>
        <view class="welcome-title">下午好，欢迎回来</view>
        <text class="welcome-copy">今天还有 2 个任务需要完成</text>
      </view>
      <view class="avatar">兼</view>
    </view>

    <view class="surface wallet-card">
      <view class="wallet-item">
        <text>待结算</text>
        <strong>¥125.00</strong>
      </view>
      <view class="wallet-divider" />
      <view class="wallet-item">
        <text>累计已结算</text>
        <strong>¥680.00</strong>
      </view>
    </view>

    <view class="status-grid">
      <view v-for="item in statuses" :key="item.label" class="surface status-card">
        <strong>{{ item.value }}</strong>
        <text>{{ item.label }}</text>
      </view>
    </view>

    <view class="section-title">
      <text>进行中的任务</text>
      <text class="section-link" @click="openTasks">查看全部</text>
    </view>

    <view class="surface task-card">
      <view class="task-row">
        <view>
          <wd-tag type="primary" plain>抖音短视频</wd-tag>
          <view class="task-title">某奶茶新品推广</view>
        </view>
        <text class="task-price">¥20/条</text>
      </view>
      <text class="task-desc">发布不少于15秒的视频，并带上指定话题。</text>
      <view class="task-meta">
        <text>剩余 2 天</text>
        <text>提交截止 6月30日</text>
      </view>
      <wd-button block @click="openTasks">继续完成任务</wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
const statuses = [
  { label: "进行中", value: 2 },
  { label: "待审核", value: 1 },
  { label: "已通过", value: 8 },
];

const openTasks = () => {
  uni.switchTab({ url: "/pages/tasks/index" });
};
</script>

<style scoped lang="scss">
.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8rpx 4rpx 32rpx;
}

.eyebrow {
  color: #2563eb;
  font-size: 24rpx;
  font-weight: 600;
}

.welcome-title {
  margin-top: 8rpx;
  font-size: 44rpx;
  font-weight: 800;
  line-height: 1.25;
}

.welcome-copy {
  display: block;
  margin-top: 10rpx;
  color: #778196;
  font-size: 27rpx;
}

.avatar {
  width: 88rpx;
  height: 88rpx;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #172033);
  border-radius: 50%;
  font-size: 30rpx;
  font-weight: 700;
}

.wallet-card {
  display: flex;
  padding: 34rpx 28rpx;
}

.wallet-item {
  flex: 1;
}

.wallet-item text {
  display: block;
  color: #778196;
  font-size: 25rpx;
}

.wallet-item strong {
  display: block;
  margin-top: 12rpx;
  color: #172033;
  font-size: 40rpx;
  font-variant-numeric: tabular-nums;
}

.wallet-divider {
  width: 1rpx;
  margin: 0 28rpx;
  background: #e8edf3;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18rpx;
  margin-top: 20rpx;
}

.status-card {
  padding: 28rpx 12rpx;
  text-align: center;
}

.status-card strong,
.status-card text {
  display: block;
}

.status-card strong {
  font-size: 36rpx;
}

.status-card text {
  margin-top: 8rpx;
  color: #778196;
  font-size: 24rpx;
}

.section-link {
  color: #2563eb;
  font-size: 25rpx;
  font-weight: 500;
}

.task-card {
  padding: 28rpx;
}

.task-row,
.task-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.task-title {
  margin-top: 16rpx;
  font-size: 32rpx;
  font-weight: 700;
}

.task-price {
  color: #d97706;
  font-size: 30rpx;
  font-weight: 700;
}

.task-desc {
  display: block;
  margin: 22rpx 0;
  color: #5f6b7c;
  font-size: 26rpx;
  line-height: 1.65;
}

.task-meta {
  margin-bottom: 26rpx;
  color: #778196;
  font-size: 23rpx;
}
</style>
