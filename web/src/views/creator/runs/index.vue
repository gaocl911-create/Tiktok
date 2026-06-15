<template>
  <div class="monitor-page">
    <header class="page-heading">
      <div>
        <h1>采集运行</h1>
        <p>查看监控目标的运行记录、API调用、耗时和失败原因。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadTargets">刷新</el-button>
    </header>

    <section class="surface filter-bar">
      <el-select v-model="selectedTargetId" filterable placeholder="选择一个监控目标" @change="handleTargetChange">
        <el-option v-for="item in targets" :key="item.targetId" :label="item.targetName" :value="item.targetId" />
      </el-select>
      <span class="filter-hint">先选择监控目标，再查看最近运行记录</span>
    </section>

    <section class="surface table-surface">
      <el-table v-loading="loading" :data="runs">
        <el-table-column label="开始时间" width="180">
          <template #default="{ row }">{{ formatTime(row.startedAt) }}</template>
        </el-table-column>
        <el-table-column label="任务类型" prop="runType" min-width="180" />
        <el-table-column label="触发方式" prop="triggerSource" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }"><status-badge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="耗时" width="110" align="right">
          <template #default="{ row }">{{ formatDuration(row.durationMs) }}</template>
        </el-table-column>
        <el-table-column label="API调用" prop="apiCallCount" width="110" align="right" />
        <el-table-column label="估算费用" width="120" align="right">
          <template #default="{ row }">¥{{ Number(row.estimatedCostCny || 0).toFixed(4) }}</template>
        </el-table-column>
        <el-table-column label="结果" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.errorMessage || `成功采集 ${row.collectedCount || 0} 项` }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && runs.length === 0" description="暂无运行记录" />
      <pagination
        v-show="total > 0"
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="loadRuns"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import { listMonitorTargets, listTargetRuns } from '@/api/creator';
import type { CollectionRun, MonitorTarget } from '@/api/creator/types';
import StatusBadge from '../components/StatusBadge.vue';

const loading = ref(false);
const targets = ref<MonitorTarget[]>([]);
const runs = ref<CollectionRun[]>([]);
const selectedTargetId = ref('');
const total = ref(0);
const query = reactive({
  pageNum: 1,
  pageSize: 20
});

const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '--';
const formatDuration = (value?: number) => value == null ? '--' : value < 1000 ? `${value} ms` : `${(value / 1000).toFixed(1)} s`;

const loadTargets = async () => {
  loading.value = true;
  try {
    const res = await listMonitorTargets({ pageNum: 1, pageSize: 100, platform: 'douyin' });
    targets.value = res.rows || [];
    if (!selectedTargetId.value && targets.value.length) {
      selectedTargetId.value = targets.value[0].targetId;
    }
    await loadRuns();
  } finally {
    loading.value = false;
  }
};

const loadRuns = async () => {
  if (!selectedTargetId.value) {
    runs.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const res = await listTargetRuns(selectedTargetId.value, query);
    runs.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const handleTargetChange = () => {
  query.pageNum = 1;
  loadRuns();
};

onMounted(loadTargets);
</script>

<style scoped>
@import '../monitor-page.css';
</style>
