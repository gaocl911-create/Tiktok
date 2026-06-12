<template>
  <div v-loading="loading" class="monitor-page content-detail-page">
    <header class="page-heading">
      <div>
        <el-button link :icon="ArrowLeft" @click="router.back()">返回内容监测</el-button>
        <h1>作品监控详情</h1>
        <p>查看作品每次采集结果与互动指标增长趋势。</p>
      </div>
      <div class="heading-actions">
        <el-button v-if="content?.contentUrl || content?.shareUrl" :icon="Link" @click="openContent">打开作品</el-button>
        <el-button
          v-hasPermi="['creator:target:collect']"
          type="primary"
          :icon="Refresh"
          :loading="refreshing"
          :disabled="!target"
          @click="refreshMetrics"
        >
          立即刷新
        </el-button>
      </div>
    </header>

    <template v-if="content">
      <section class="surface content-overview">
        <div class="detail-cover">
          <img v-if="content.coverUrl" :src="content.coverUrl" :alt="contentTitle" />
          <el-icon v-else><VideoCamera /></el-icon>
        </div>
        <div class="overview-copy">
          <button v-if="creator" class="author-link" type="button" @click="goCreatorDetail">
            <el-avatar :size="26" :src="creator.avatarUrl">{{ creator.nickname?.slice(0, 1) }}</el-avatar>
            <span>{{ creator.nickname }}</span>
          </button>
          <div class="title-row">
            <h2>{{ contentTitle }}</h2>
            <status-badge :status="content.metricsStatus" />
          </div>
          <p>{{ content.description || '暂无作品文案' }}</p>
          <div class="overview-meta">
            <span>发布于 {{ formatTime(content.publishTime) }}</span>
            <span>作品 ID {{ content.platformContentId }}</span>
            <span>加入监控 {{ formatTime(content.firstSeenAt) }}</span>
          </div>
        </div>
        <dl class="monitor-summary">
          <div><dt>监控方式</dt><dd>{{ targetTypeText }}</dd></div>
          <div><dt>刷新间隔</dt><dd>{{ intervalText }}</dd></div>
          <div><dt>最近采集</dt><dd>{{ formatTime(content.lastMetricsCollectAt) }}</dd></div>
          <div><dt>下次采集</dt><dd>{{ formatTime(target?.nextContentCollectAt) }}</dd></div>
        </dl>
      </section>

      <section class="surface metrics-band">
        <div v-for="metric in currentMetrics" :key="metric.key">
          <span>{{ metric.label }}</span>
          <strong>{{ formatNumber(metric.value) }}</strong>
          <small :class="{ positive: metric.delta > 0 }">最近一次 {{ formatDelta(metric.delta) }}</small>
        </div>
      </section>

      <section class="surface trend-section">
        <div class="section-heading">
          <div>
            <h2>互动趋势</h2>
            <p>曲线从作品加入监控并产生首个快照后开始计算。</p>
          </div>
          <el-radio-group v-model="range" size="small">
            <el-radio-button value="24h">24小时</el-radio-button>
            <el-radio-button value="7d">7天</el-radio-button>
            <el-radio-button value="30d">30天</el-radio-button>
            <el-radio-button value="all">全部</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="filteredSnapshots.length" class="chart-grid">
          <div class="chart-panel">
            <div class="chart-title">
              <strong>累计指标</strong>
              <span>每个时间点的实际互动总量</span>
            </div>
            <div ref="totalChartRef" class="trend-chart" />
          </div>
          <div class="chart-panel">
            <div class="chart-title">
              <strong>单次增长</strong>
              <span>与上一次成功快照相比的增长量</span>
            </div>
            <div ref="deltaChartRef" class="trend-chart" />
          </div>
        </div>
        <el-empty v-else description="当前时间范围暂无快照数据" />
      </section>

      <section class="surface history-section">
        <el-tabs v-model="activeHistoryTab">
          <el-tab-pane label="指标快照" name="snapshots">
            <el-table :data="snapshotPageRows">
              <el-table-column label="采集时间" width="180">
                <template #default="{ row }">{{ formatTime(row.collectedAt) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }"><status-badge :status="row.metricsStatus" /></template>
              </el-table-column>
              <el-table-column label="点赞" align="right">
                <template #default="{ row }">{{ formatNumber(row.likeCount) }}</template>
              </el-table-column>
              <el-table-column label="增长" align="right">
                <template #default="{ row }"><span class="delta-text">{{ formatDelta(row.likeDelta) }}</span></template>
              </el-table-column>
              <el-table-column label="评论" align="right">
                <template #default="{ row }">{{ formatNumber(row.commentCount) }}</template>
              </el-table-column>
              <el-table-column label="增长" align="right">
                <template #default="{ row }"><span class="delta-text">{{ formatDelta(row.commentDelta) }}</span></template>
              </el-table-column>
              <el-table-column label="收藏" align="right">
                <template #default="{ row }">{{ formatNumber(row.collectCount) }}</template>
              </el-table-column>
              <el-table-column label="分享" align="right">
                <template #default="{ row }">{{ formatNumber(row.shareCount) }}</template>
              </el-table-column>
              <el-table-column label="缺失字段" min-width="130">
                <template #default="{ row }">{{ row.missingMetricFields || '--' }}</template>
              </el-table-column>
            </el-table>
            <pagination
              v-show="filteredSnapshots.length > snapshotPageSize"
              v-model:page="snapshotPage"
              v-model:limit="snapshotPageSize"
              :total="filteredSnapshots.length"
            />
          </el-tab-pane>

          <el-tab-pane label="运行状态" name="runs">
            <el-table :data="runs">
              <el-table-column label="开始时间" width="180">
                <template #default="{ row }">{{ formatTime(row.startedAt) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="runTagType(row.status)" effect="plain" size="small">{{ runStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="触发方式" width="130">
                <template #default="{ row }">{{ triggerText(row.triggerSource) }}</template>
              </el-table-column>
              <el-table-column label="耗时" width="110" align="right">
                <template #default="{ row }">{{ durationText(row.durationMs) }}</template>
              </el-table-column>
              <el-table-column label="API调用" width="100" align="right" prop="apiCallCount" />
              <el-table-column label="错误信息" min-width="260">
                <template #default="{ row }">
                  <span :class="{ 'error-text': row.status === 'failed' }">{{ row.errorMessage || '--' }}</span>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!runs.length" description="暂无单作品运行记录，后续刷新会开始记录" />
          </el-tab-pane>
        </el-tabs>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, Link, Refresh, VideoCamera } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import {
  collectTarget,
  getContentPost,
  getContentTarget,
  getCreatorAccount,
  listContentRuns,
  listContentSnapshots
} from '@/api/creator';
import type {
  CollectionRun,
  ContentPost,
  ContentSnapshot,
  CreatorAccount,
  MonitorTarget
} from '@/api/creator/types';
import StatusBadge from '../components/StatusBadge.vue';

const route = useRoute();
const router = useRouter();
const contentId = computed(() => String(route.params.contentId || ''));
const content = ref<ContentPost>();
const creator = ref<CreatorAccount>();
const target = ref<MonitorTarget>();
const snapshots = ref<ContentSnapshot[]>([]);
const runs = ref<CollectionRun[]>([]);
const loading = ref(false);
const refreshing = ref(false);
const range = ref<'24h' | '7d' | '30d' | 'all'>('7d');
const activeHistoryTab = ref('snapshots');
const snapshotPage = ref(1);
const snapshotPageSize = ref(10);
const totalChartRef = ref<HTMLElement>();
const deltaChartRef = ref<HTMLElement>();
let totalChart: echarts.ECharts | undefined;
let deltaChart: echarts.ECharts | undefined;
let resizeObserver: ResizeObserver | undefined;

const contentTitle = computed(() => content.value?.title || content.value?.description?.split('\n')[0] || '未命名作品');
const sortedSnapshots = computed(() =>
  [...snapshots.value].sort((a, b) => new Date(a.collectedAt).getTime() - new Date(b.collectedAt).getTime())
);
const filteredSnapshots = computed(() => {
  if (range.value === 'all') return sortedSnapshots.value;
  const rangeMs = {
    '24h': 24 * 60 * 60 * 1000,
    '7d': 7 * 24 * 60 * 60 * 1000,
    '30d': 30 * 24 * 60 * 60 * 1000
  }[range.value];
  const start = Date.now() - rangeMs;
  return sortedSnapshots.value.filter((item) => new Date(item.collectedAt).getTime() >= start);
});
const latestSnapshot = computed(() => sortedSnapshots.value.at(-1));
const currentMetrics = computed(() => [
  { key: 'like', label: '点赞', value: content.value?.latestLikeCount, delta: latestSnapshot.value?.likeDelta || 0 },
  { key: 'comment', label: '评论', value: content.value?.latestCommentCount, delta: latestSnapshot.value?.commentDelta || 0 },
  { key: 'collect', label: '收藏', value: content.value?.latestCollectCount, delta: latestSnapshot.value?.collectDelta || 0 },
  { key: 'share', label: '分享', value: content.value?.latestShareCount, delta: latestSnapshot.value?.shareDelta || 0 }
]);
const snapshotPageRows = computed(() => {
  const descending = [...filteredSnapshots.value].reverse();
  const start = (snapshotPage.value - 1) * snapshotPageSize.value;
  return descending.slice(start, start + snapshotPageSize.value);
});
const targetTypeText = computed(() => target.value?.targetType === 'creator_collection' ? '作者新作品自动监控' : '单作品链接监控');
const intervalText = computed(() => target.value?.contentCollectIntervalMin ? `每 ${target.value.contentCollectIntervalMin} 分钟` : '--');

const formatNumber = (value?: number) => value == null ? '--' : new Intl.NumberFormat('zh-CN').format(value);
const formatDelta = (value?: number) => value == null ? '--' : `${value > 0 ? '+' : ''}${formatNumber(value)}`;
const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '--';
const durationText = (value?: number) => value == null ? '--' : value < 1000 ? `${value} ms` : `${(value / 1000).toFixed(1)} 秒`;
const runStatusText = (status?: string) => ({
  running: '运行中',
  success: '成功',
  partial: '部分成功',
  failed: '失败',
  skipped: '已跳过',
  budget_limited: '预算限制'
}[status || ''] || status || '--');
const runTagType = (status?: string) => {
  if (status === 'success') return 'success';
  if (status === 'failed') return 'danger';
  if (status === 'running') return 'primary';
  return 'warning';
};
const triggerText = (source?: string) => ({
  manual: '手动刷新',
  manual_batch: '手动批量',
  scheduled: '自动任务',
  snailjob: '自动任务',
  system: '系统任务'
}[source || ''] || source || '--');

const loadData = async () => {
  loading.value = true;
  try {
    const [contentRes, targetRes, snapshotRes, runRes] = await Promise.all([
      getContentPost(contentId.value),
      getContentTarget(contentId.value),
      listContentSnapshots(contentId.value, 1000),
      listContentRuns(contentId.value, 300)
    ]);
    content.value = contentRes.data;
    target.value = targetRes.data;
    snapshots.value = snapshotRes.data || [];
    runs.value = runRes.data || [];
    if (content.value?.creatorId) {
      const creatorRes = await getCreatorAccount(content.value.creatorId);
      creator.value = creatorRes.data;
    }
    await nextTick();
    renderCharts();
  } finally {
    loading.value = false;
  }
};

const renderCharts = () => {
  if (!totalChartRef.value || !deltaChartRef.value) return;
  totalChart ||= echarts.init(totalChartRef.value);
  deltaChart ||= echarts.init(deltaChartRef.value);
  const rows = filteredSnapshots.value;
  const labels = rows.map((item) => new Date(item.collectedAt).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }));
  const seriesConfig = [
    { name: '点赞', total: 'likeCount', delta: 'likeDelta', color: '#171717' },
    { name: '评论', total: 'commentCount', delta: 'commentDelta', color: '#2563eb' },
    { name: '收藏', total: 'collectCount', delta: 'collectDelta', color: '#16a34a' },
    { name: '分享', total: 'shareCount', delta: 'shareDelta', color: '#d97706' }
  ] as const;
  const baseOption = {
    animationDuration: 300,
    color: seriesConfig.map((item) => item.color),
    tooltip: { trigger: 'axis' },
    legend: { top: 0, right: 0, itemWidth: 14, itemHeight: 8 },
    grid: { left: 20, right: 18, top: 42, bottom: 22, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      boundaryGap: false,
      axisLabel: { color: '#737373', hideOverlap: true },
      axisLine: { lineStyle: { color: '#e5e5e5' } }
    },
    yAxis: {
      type: 'value',
      min: 'dataMin',
      axisLabel: { color: '#737373' },
      splitLine: { lineStyle: { color: '#eeeeee' } }
    }
  };
  totalChart.setOption({
    ...baseOption,
    series: seriesConfig.map((item) => ({
      name: item.name,
      type: 'line',
      showSymbol: rows.length < 30,
      symbolSize: 6,
      smooth: 0.2,
      data: rows.map((row) => row[item.total] ?? null)
    }))
  }, true);
  deltaChart.setOption({
    ...baseOption,
    yAxis: {
      ...baseOption.yAxis,
      min: undefined
    },
    series: seriesConfig.map((item) => ({
      name: item.name,
      type: 'line',
      showSymbol: true,
      symbolSize: 5,
      smooth: 0.15,
      data: rows.map((row) => row[item.delta] ?? null)
    }))
  }, true);
};

const refreshMetrics = async () => {
  if (!target.value) return;
  refreshing.value = true;
  try {
    await collectTarget(target.value.targetId);
    await loadData();
    ElMessage.success('作品指标已刷新');
  } finally {
    refreshing.value = false;
  }
};

const openContent = () => window.open(content.value?.contentUrl || content.value?.shareUrl, '_blank', 'noopener');
const goCreatorDetail = () => creator.value && router.push(`/douyin/account/detail/${creator.value.creatorId}`);

watch(range, async () => {
  snapshotPage.value = 1;
  await nextTick();
  renderCharts();
});

onMounted(async () => {
  await loadData();
  resizeObserver = new ResizeObserver(() => {
    totalChart?.resize();
    deltaChart?.resize();
  });
  if (totalChartRef.value) resizeObserver.observe(totalChartRef.value);
  if (deltaChartRef.value) resizeObserver.observe(deltaChartRef.value);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  totalChart?.dispose();
  deltaChart?.dispose();
});
</script>

<style scoped>
@import '../monitor-page.css';

.content-overview {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr) minmax(240px, auto);
  gap: 20px;
  align-items: center;
  padding: 20px;
}

.detail-cover {
  width: 112px;
  aspect-ratio: 3 / 4;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid #e5e5e5;
  border-radius: 5px;
  color: #737373;
  background: #f5f5f4;
}

.detail-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.overview-copy {
  min-width: 0;
}

.author-link {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 3px 7px 3px 3px;
  border: 0;
  border-radius: 4px;
  color: #404040;
  font: inherit;
  background: transparent;
  cursor: pointer;
}

.author-link:hover,
.author-link:focus-visible {
  color: #171717;
  background: #f5f5f4;
  outline: 2px solid transparent;
}

.author-link span {
  margin-left: 7px;
  font-size: 13px;
  font-weight: 600;
}

.title-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 6px;
}

.title-row h2 {
  margin: 0;
  color: #171717;
  font-size: 20px;
  line-height: 28px;
}

.overview-copy > p {
  max-width: 900px;
  margin: 9px 0 0;
  color: #525252;
  font-size: 13px;
  line-height: 20px;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.overview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 18px;
  margin-top: 12px;
  color: #737373;
  font-size: 12px;
}

.monitor-summary {
  display: grid;
  gap: 10px;
  min-width: 240px;
  margin: 0;
}

.monitor-summary div {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 14px;
}

.monitor-summary dt {
  color: #737373;
}

.monitor-summary dd {
  margin: 0;
  color: #262626;
  text-align: right;
}

.metrics-band {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 14px;
  overflow: hidden;
}

.metrics-band > div {
  padding: 18px 20px;
  border-right: 1px solid #e5e5e5;
}

.metrics-band > div:last-child {
  border-right: 0;
}

.metrics-band span,
.metrics-band strong,
.metrics-band small {
  display: block;
}

.metrics-band span {
  color: #737373;
  font-size: 12px;
}

.metrics-band strong {
  margin-top: 5px;
  color: #171717;
  font-size: 24px;
  font-variant-numeric: tabular-nums;
}

.metrics-band small {
  margin-top: 4px;
  color: #737373;
  font-size: 12px;
}

.metrics-band small.positive,
.delta-text {
  color: #15803d;
}

.trend-section,
.history-section {
  margin-top: 14px;
  padding: 18px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.section-heading h2 {
  margin: 0;
  color: #171717;
  font-size: 16px;
}

.section-heading p {
  margin: 4px 0 0;
  color: #737373;
  font-size: 12px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.chart-panel {
  min-width: 0;
  padding: 14px;
  border: 1px solid #e5e5e5;
  border-radius: 5px;
}

.chart-title strong,
.chart-title span {
  display: block;
}

.chart-title strong {
  color: #262626;
  font-size: 14px;
}

.chart-title span {
  margin-top: 3px;
  color: #737373;
  font-size: 11px;
}

.trend-chart {
  width: 100%;
  height: 330px;
  margin-top: 8px;
}

.history-section :deep(.pagination-container) {
  margin-top: 14px;
  padding: 0;
  background: transparent;
}

.error-text {
  color: #b91c1c;
}

@media (max-width: 1200px) {
  .content-overview {
    grid-template-columns: 96px minmax(0, 1fr);
  }

  .detail-cover {
    width: 96px;
  }

  .monitor-summary {
    grid-column: 1 / -1;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .content-overview {
    grid-template-columns: 72px minmax(0, 1fr);
    gap: 12px;
    padding: 14px;
  }

  .detail-cover {
    width: 72px;
  }

  .monitor-summary {
    grid-template-columns: 1fr;
  }

  .metrics-band {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .section-heading {
    flex-direction: column;
  }
}
</style>
