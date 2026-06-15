<script setup lang="ts">
import { onHide, onLaunch, onShow } from "@dcloudio/uni-app";

onLaunch(() => {
  console.info("Creator task miniapp launched");
});
onShow(() => undefined);
onHide(() => undefined);
</script>

<style lang="scss">
@use "@wot-ui/ui/styles/theme/index.scss";

page {
  min-height: 100%;
  color: #172033;
  background: #f5f7fa;
  font-family:
    -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei",
    sans-serif;
}

view,
text,
button,
input,
textarea {
  box-sizing: border-box;
}

button::after {
  border: 0;
}

.page-shell {
  min-height: 100vh;
  padding: 32rpx 28rpx calc(48rpx + env(safe-area-inset-bottom));
}

.surface {
  background: #fff;
  border: 1rpx solid #e8edf3;
  border-radius: 24rpx;
  box-shadow: 0 12rpx 36rpx rgba(23, 32, 51, 0.05);
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 36rpx 0 20rpx;
  font-size: 32rpx;
  font-weight: 700;
}

.muted {
  color: #778196;
}
</style>
