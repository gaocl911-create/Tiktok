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
  --cm-bg: #f6f5f1;
  --cm-surface: #ffffff;
  --cm-ink: #0b0b0b;
  --cm-muted: #77736b;
  --cm-soft: #efeee8;
  --cm-line: #e4e1d8;
  --cm-success: #147a4b;
  --cm-warning: #a05a00;
  --cm-danger: #c02f2f;
  --wot-button-primary-bg: #0b0b0b;
  --wot-button-primary-bg-active: #2a2926;
  --wot-button-primary-color: #0b0b0b;
  --wot-button-primary-color-active: #2a2926;
  --wot-button-primary-plain-border: #0b0b0b;
  --wot-button-primary-plain-border-active: #2a2926;
  --wot-button-primary-plain-bg-active: #efeee8;
  --wot-tag-primary-color: #0b0b0b;
  --wot-tag-primary-bg: #efeee8;
  --wot-tag-primary-border-color: #d8d5ca;
  min-height: 100%;
  color: var(--cm-ink);
  background: var(--cm-bg);
  font-family:
    -apple-system, BlinkMacSystemFont, "SF Pro Display", "PingFang SC",
    "Microsoft YaHei", sans-serif;
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
  padding: 34rpx 28rpx calc(56rpx + env(safe-area-inset-bottom));
}

.page-heading {
  margin: 4rpx 2rpx 30rpx;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 42rpx;
  padding: 0 18rpx;
  color: var(--cm-ink);
  background: var(--cm-soft);
  border: 1rpx solid var(--cm-line);
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.title {
  display: block;
  margin-top: 18rpx;
  color: var(--cm-ink);
  font-size: 52rpx;
  font-weight: 900;
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.subtitle {
  display: block;
  margin-top: 16rpx;
  color: var(--cm-muted);
  font-size: 26rpx;
  line-height: 1.65;
}

.surface {
  background: var(--cm-surface);
  border: 1rpx solid var(--cm-line);
  border-radius: 34rpx;
  box-shadow: 0 16rpx 42rpx rgba(11, 11, 11, 0.035);
}

.surface-dark {
  color: #fff;
  background: var(--cm-ink);
  border-radius: 38rpx;
  box-shadow: 0 22rpx 54rpx rgba(11, 11, 11, 0.16);
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 42rpx 4rpx 20rpx;
  color: var(--cm-ink);
  font-size: 34rpx;
  font-weight: 900;
  letter-spacing: -0.02em;
}

.section-link {
  color: var(--cm-muted);
  font-size: 24rpx;
  font-weight: 700;
}

.muted {
  color: var(--cm-muted);
}

.state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 22rpx;
  padding: 78rpx 38rpx;
  text-align: center;
}

.state-card.compact {
  padding: 44rpx 32rpx;
}

.empty-mark {
  width: 96rpx;
  height: 96rpx;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--cm-ink);
  border-radius: 30rpx;
  font-size: 34rpx;
  font-weight: 900;
}

.empty-title {
  color: var(--cm-ink);
  font-size: 32rpx;
  font-weight: 900;
}

.pill {
  display: inline-flex;
  align-items: center;
  min-height: 44rpx;
  padding: 0 18rpx;
  color: var(--cm-ink);
  background: var(--cm-soft);
  border: 1rpx solid var(--cm-line);
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 800;
}

.pill.success {
  color: var(--cm-success);
  background: #edf7f1;
  border-color: #c9e7d5;
}

.pill.warning {
  color: var(--cm-warning);
  background: #fff5e5;
  border-color: #f1d5a8;
}

.pill.danger {
  color: var(--cm-danger);
  background: #fff0ef;
  border-color: #efc1bd;
}

.form-input,
.form-textarea {
  width: 100%;
  color: var(--cm-ink);
  background: #f8f7f3;
  border: 1rpx solid var(--cm-line);
  border-radius: 24rpx;
  font-size: 28rpx;
}

.form-input {
  height: 94rpx;
  padding: 0 26rpx;
}

.form-textarea {
  min-height: 190rpx;
  padding: 24rpx 26rpx;
  line-height: 1.65;
}

.placeholder {
  color: #aaa69b;
}
</style>
