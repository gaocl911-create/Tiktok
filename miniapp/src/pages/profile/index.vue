<template>
  <view class="page-shell">
    <view class="surface profile-card">
      <view class="avatar">兼</view>
      <view class="profile-copy">
        <view class="profile-name">兼职用户</view>
        <text class="muted">资料尚未完善</text>
      </view>
      <wd-tag type="warning" plain>待入驻</wd-tag>
    </view>

    <view class="section-title">账户与业务</view>
    <view class="surface menu-list">
      <view v-for="item in menuItems" :key="item" class="menu-item">
        <text>{{ item }}</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="version">创作者兼职任务 V0.1.0</view>
  </view>
</template>

<script setup lang="ts">
const menuItems = ["完善兼职资料", "我的任务", "佣金明细", "结算记录", "用户协议与隐私政策"];
</script>

<style scoped lang="scss">
.profile-card {
  display: flex;
  align-items: center;
  gap: 22rpx;
  padding: 30rpx;
}

.avatar {
  width: 96rpx;
  height: 96rpx;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #172033);
  border-radius: 50%;
  font-size: 34rpx;
  font-weight: 700;
}

.profile-copy {
  flex: 1;
}

.profile-name {
  margin-bottom: 8rpx;
  font-size: 32rpx;
  font-weight: 700;
}

.profile-card .muted {
  font-size: 24rpx;
}

.menu-list {
  overflow: hidden;
}

.menu-item {
  min-height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28rpx;
  border-bottom: 1rpx solid #edf1f5;
  font-size: 28rpx;
}

.menu-item:last-child {
  border-bottom: 0;
}

.arrow {
  color: #a4adba;
  font-size: 40rpx;
}

.version {
  margin-top: 44rpx;
  color: #a4adba;
  font-size: 23rpx;
  text-align: center;
}
</style>
