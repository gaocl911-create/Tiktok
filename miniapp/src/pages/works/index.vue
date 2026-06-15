<template>
  <view class="page-shell">
    <view class="page-heading">
      <text class="title">我的作品</text>
      <text class="muted">查看提交记录和监测数据</text>
    </view>

    <view class="surface empty-state">
      <view class="empty-mark">作</view>
      <text class="empty-title">还没有提交作品</text>
      <text class="muted">领取任务并发布抖音作品后，可以在这里查看审核和监测状态。</text>
      <wd-button @click="openTasks">去任务广场</wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
const openTasks = () => {
  uni.switchTab({ url: "/pages/tasks/index" });
};
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

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 88rpx 44rpx;
  text-align: center;
}

.empty-mark {
  width: 104rpx;
  height: 104rpx;
  display: grid;
  place-items: center;
  color: #2563eb;
  background: #eef4ff;
  border-radius: 28rpx;
  font-size: 38rpx;
  font-weight: 800;
}

.empty-title {
  margin-top: 28rpx;
  font-size: 32rpx;
  font-weight: 700;
}

.empty-state .muted {
  margin: 16rpx 0 30rpx;
  font-size: 25rpx;
  line-height: 1.7;
}
</style>
