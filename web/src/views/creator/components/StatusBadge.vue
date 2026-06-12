<template>
  <span class="status-badge" :class="tone">
    <span class="status-dot" />
    {{ label }}
  </span>
</template>

<script setup lang="ts">
const props = defineProps<{ status?: string }>();

const statusMap: Record<string, { label: string; tone: string }> = {
  active: { label: '监控中', tone: 'success' },
  full: { label: '数据完整', tone: 'success' },
  success: { label: '成功', tone: 'success' },
  no_new_content: { label: '暂无新作品', tone: 'neutral' },
  pending: { label: '等待采集', tone: 'neutral' },
  running: { label: '采集中', tone: 'neutral' },
  partial: { label: '部分数据', tone: 'warning' },
  paused: { label: '已暂停', tone: 'warning' },
  budget_limited: { label: '预算受限', tone: 'warning' },
  failed: { label: '采集失败', tone: 'danger' },
  stopped: { label: '已停止', tone: 'danger' }
};

const current = computed(() => statusMap[props.status || ''] || { label: props.status || '未知', tone: 'neutral' });
const label = computed(() => current.value.label);
const tone = computed(() => current.value.tone);
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 24px;
  padding: 0 8px;
  border: 1px solid #dedede;
  border-radius: 4px;
  color: #525252;
  background: #fff;
  font-size: 12px;
  white-space: nowrap;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #a3a3a3;
}

.success .status-dot {
  background: #16835d;
}

.warning .status-dot {
  background: #b26a00;
}

.danger {
  color: #b42318;
  border-color: #f2c6c2;
  background: #fffafa;
}

.danger .status-dot {
  background: #c92a2a;
}
</style>
