<template>
  <div class="dashboard-page">
    <header class="dashboard-header">
      <div>
        <h1>监测总览</h1>
        <p>查看权限范围内的账号、作品和采集运行情况</p>
      </div>
      <div class="header-actions">
        <el-tooltip content="刷新总览" placement="bottom">
          <el-button :icon="Refresh" :loading="loading" aria-label="刷新总览" @click="loadDashboard" />
        </el-tooltip>
        <el-button :icon="User" @click="router.push('/douyin/account')">添加账号</el-button>
        <el-button type="primary" :icon="Plus" @click="router.push('/douyin/content')">添加作品</el-button>
      </div>
    </header>

    <main class="dashboard-content">
      <section class="metric-strip">
        <article v-for="item in metrics" :key="item.label" class="metric-item" :class="{ alert: item.alert }">
          <div class="metric-label">
            <span>{{ item.label }}</span>
            <el-icon v-if="item.alert"><Warning /></el-icon>
          </div>
          <strong>{{ item.value }}</strong>
          <span class="metric-note">{{ item.note }}</span>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="panel activity-panel">
          <div class="panel-heading">
            <div>
              <h2>监控结构</h2>
              <p>当前已建立的作者与单作品监控目标</p>
            </div>
            <el-button link @click="router.push('/douyin/runs')">查看采集运行</el-button>
          </div>
          <div class="target-summary">
            <div class="summary-total">
              <span>全部监控目标</span>
              <strong>{{ formatNumber(targetTotal) }}</strong>
            </div>
            <div class="summary-bars">
              <div>
                <div class="bar-label"><span>作者作品集</span><b>{{ creatorTargetCount }}</b></div>
                <div class="bar-track"><i :style="{ width: targetPercent(creatorTargetCount) }" /></div>
              </div>
              <div>
                <div class="bar-label"><span>单作品监控</span><b>{{ contentTargetCount }}</b></div>
                <div class="bar-track"><i :style="{ width: targetPercent(contentTargetCount) }" /></div>
              </div>
              <div>
                <div class="bar-label"><span>异常或暂停</span><b>{{ abnormalTargetCount }}</b></div>
                <div class="bar-track warning"><i :style="{ width: targetPercent(abnormalTargetCount) }" /></div>
              </div>
            </div>
          </div>
          <div class="trend-placeholder">
            <div class="trend-lines" aria-hidden="true"><i v-for="index in 7" :key="index" /></div>
            <div>
              <el-icon><DataLine /></el-icon>
              <strong>互动增长趋势待接入</strong>
              <span>完成首页聚合接口后，将展示点赞、评论、收藏和分享增长曲线。</span>
            </div>
          </div>
        </article>

        <article class="panel run-panel">
          <div class="panel-heading">
            <div>
              <h2>采集状态</h2>
              <p>监控目标当前数据状态</p>
            </div>
          </div>
          <div class="status-list">
            <div v-for="item in targetStatuses" :key="item.label">
              <span><i :class="item.tone" />{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
          <div class="run-footer">
            <span>监控服务</span>
            <b><i />运行中</b>
          </div>
        </article>
      </section>

      <section class="panel latest-panel">
        <div class="panel-heading">
          <div>
            <h2>最新作品</h2>
            <p>按平台发布时间倒序显示最近发现的作品</p>
          </div>
          <el-button link @click="router.push('/douyin/content')">查看全部</el-button>
        </div>
        <el-table v-loading="loading" :data="latestContents">
          <el-table-column label="作品" min-width="360">
            <template #default="{ row }">
              <div class="content-cell">
                <div class="content-cover">
                  <img v-if="row.coverUrl" :src="row.coverUrl" alt="" />
                  <el-icon v-else><VideoCamera /></el-icon>
                </div>
                <div>
                  <strong>{{ row.title || row.description || '未命名作品' }}</strong>
                  <span>{{ formatTime(row.publishTime) }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="点赞" width="120" align="right">
            <template #default="{ row }">{{ metricValue(row.latestLikeCount) }}</template>
          </el-table-column>
          <el-table-column label="评论" width="110" align="right">
            <template #default="{ row }">{{ metricValue(row.latestCommentCount) }}</template>
          </el-table-column>
          <el-table-column label="收藏" width="110" align="right">
            <template #default="{ row }">{{ metricValue(row.latestCollectCount) }}</template>
          </el-table-column>
          <el-table-column label="分享" width="110" align="right">
            <template #default="{ row }">{{ metricValue(row.latestShareCount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }"><status-badge :status="row.metricsStatus" /></template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && latestContents.length === 0" description="尚未添加监控作品" />
      </section>
    </main>
  </div>
</template>

<script setup name="Index" lang="ts">
import { DataLine, Plus, Refresh, User, VideoCamera, Warning } from '@element-plus/icons-vue';
import { listContentPosts, listCreatorAccounts, listMonitorTargets } from '@/api/creator';
import type { ContentPost, MonitorTarget } from '@/api/creator/types';
import StatusBadge from '@/views/creator/components/StatusBadge.vue';

const router = useRouter();
const loading = ref(false);
const accountTotal = ref(0);
const contentTotal = ref(0);
const targetTotal = ref(0);
const latestContents = ref<ContentPost[]>([]);
const targets = ref<MonitorTarget[]>([]);

const creatorTargetCount = computed(() => targets.value.filter((item) => item.targetType === 'creator_collection').length);
const contentTargetCount = computed(() => targets.value.filter((item) => item.targetType === 'single_content').length);
const abnormalTargetCount = computed(() => targets.value.filter((item) => ['failed', 'paused', 'stopped'].includes(item.status || '')).length);
const todayNewCount = computed(() => {
  const today = new Date().toDateString();
  return latestContents.value.filter((item) => item.publishTime && new Date(item.publishTime).toDateString() === today).length;
});
const targetStatuses = computed(() => [
  { label: '正常监控', value: targets.value.filter((item) => item.status === 'active').length, tone: 'success' },
  { label: '等待或暂无新作品', value: targets.value.filter((item) => ['pending', 'no_new_content'].includes(item.dataStatus || '')).length, tone: 'neutral' },
  { label: '部分数据', value: targets.value.filter((item) => item.dataStatus === 'partial').length, tone: 'warning' },
  { label: '采集失败', value: targets.value.filter((item) => item.dataStatus === 'failed').length, tone: 'danger' }
]);
const metrics = computed(() => [
  { label: '监控账号', value: formatNumber(accountTotal.value), note: '已录入作者主页' },
  { label: '监控作品', value: formatNumber(contentTotal.value), note: '已进入内容库' },
  { label: '今日新增作品', value: formatNumber(todayNewCount.value), note: '按当前返回数据统计' },
  { label: '今日点赞增长', value: '--', note: '等待聚合接口' },
  { label: '待处理预警', value: '--', note: '预警模块待接入', alert: true },
  { label: '监控目标', value: formatNumber(targetTotal.value), note: '作者与单作品合计' }
]);

const formatNumber = (value?: number) => new Intl.NumberFormat('zh-CN').format(value || 0);
const metricValue = (value?: number) => value == null ? '--' : formatNumber(value);
const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '发布时间未知';
const targetPercent = (value: number) => `${targetTotal.value ? Math.max(4, Math.round((value / targetTotal.value) * 100)) : 0}%`;

const loadDashboard = async () => {
  loading.value = true;
  try {
    const [accountRes, contentRes, targetRes] = await Promise.all([
      listCreatorAccounts({ pageNum: 1, pageSize: 1, platform: 'douyin' }),
      listContentPosts({ pageNum: 1, pageSize: 8, platform: 'douyin' }),
      listMonitorTargets({ pageNum: 1, pageSize: 100, platform: 'douyin' })
    ]);
    accountTotal.value = accountRes.total || 0;
    contentTotal.value = contentRes.total || 0;
    latestContents.value = contentRes.rows || [];
    targetTotal.value = targetRes.total || 0;
    targets.value = targetRes.rows || [];
  } finally {
    loading.value = false;
  }
};

onMounted(loadDashboard);
</script>

<style scoped>
.dashboard-page { min-height: 100%; color: #171717; background: #f5f5f4; }
.dashboard-header { min-height: 96px; display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; padding: 18px 24px; border-bottom: 1px solid #dedede; background: #fff; }
.breadcrumb, .dashboard-header p, .panel-heading p { margin: 0; color: #737373; font-size: 13px; }
.dashboard-header h1 { margin: 4px 0 2px; font-size: 24px; line-height: 32px; font-weight: 650; }
.header-actions { display: flex; gap: 8px; }
.dashboard-content { max-width: 1600px; margin: 0 auto; padding: 24px; }
.metric-strip, .panel { border: 1px solid #e2e2e2; border-radius: 6px; background: #fff; }
.metric-strip { display: grid; grid-template-columns: repeat(6, minmax(150px, 1fr)); overflow: hidden; }
.metric-item { min-height: 116px; padding: 16px; border-right: 1px solid #e5e5e5; }
.metric-item:last-child { border-right: 0; }
.metric-item.alert { background: #fffafa; }
.metric-label { display: flex; align-items: center; justify-content: space-between; color: #737373; font-size: 13px; }
.metric-item strong { display: block; margin-top: 10px; font-size: 28px; line-height: 36px; font-weight: 650; font-variant-numeric: tabular-nums; }
.metric-item.alert strong, .metric-item.alert .metric-label { color: #ba1a1a; }
.metric-note { display: block; margin-top: 7px; color: #8a8a8a; font-size: 12px; }
.dashboard-grid { display: grid; grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr); gap: 16px; margin-top: 16px; }
.panel { padding: 18px; }
.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.panel-heading h2 { margin: 0 0 3px; font-size: 16px; font-weight: 650; }
.target-summary { display: grid; grid-template-columns: 180px 1fr; gap: 32px; align-items: center; }
.summary-total span, .summary-total strong { display: block; }
.summary-total span { color: #737373; font-size: 13px; }
.summary-total strong { margin-top: 8px; font-size: 38px; font-variant-numeric: tabular-nums; }
.summary-bars { display: grid; gap: 14px; }
.bar-label { display: flex; justify-content: space-between; margin-bottom: 6px; color: #525252; font-size: 13px; }
.bar-track { height: 6px; overflow: hidden; background: #ededec; }
.bar-track i { display: block; height: 100%; background: #171717; }
.bar-track.warning i { background: #ba1a1a; }
.trend-placeholder { position: relative; min-height: 160px; display: grid; place-items: center; margin-top: 22px; overflow: hidden; border-top: 1px solid #ececec; color: #737373; text-align: center; }
.trend-placeholder > div:last-child { position: relative; max-width: 430px; }
.trend-placeholder .el-icon { display: block; margin: 0 auto 8px; font-size: 24px; }
.trend-placeholder strong, .trend-placeholder span { display: block; }
.trend-placeholder strong { color: #404040; font-size: 14px; }
.trend-placeholder span { margin-top: 5px; font-size: 12px; }
.trend-lines { position: absolute; inset: 20px 0; display: flex; flex-direction: column; justify-content: space-between; }
.trend-lines i { border-top: 1px dashed #ededed; }
.status-list { display: grid; }
.status-list > div { min-height: 48px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #ededed; }
.status-list span { display: flex; align-items: center; gap: 8px; color: #525252; font-size: 13px; }
.status-list i, .run-footer i { width: 7px; height: 7px; border-radius: 50%; background: #a3a3a3; }
.status-list i.success, .run-footer i { background: #16835d; }
.status-list i.warning { background: #b26a00; }
.status-list i.danger { background: #ba1a1a; }
.run-footer { display: flex; justify-content: space-between; margin-top: 16px; padding-top: 16px; border-top: 1px solid #dedede; color: #737373; font-size: 13px; }
.run-footer b { display: flex; align-items: center; gap: 7px; color: #404040; font-weight: 500; }
.latest-panel { margin-top: 16px; padding-bottom: 12px; }
.content-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }
.content-cover { width: 44px; height: 44px; flex: 0 0 44px; display: grid; place-items: center; overflow: hidden; border-radius: 4px; color: #737373; background: #efefee; }
.content-cover img { width: 100%; height: 100%; object-fit: cover; }
.content-cell > div:last-child { min-width: 0; }
.content-cell strong, .content-cell span { display: block; }
.content-cell strong { overflow: hidden; color: #171717; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.content-cell span { margin-top: 4px; color: #737373; font-size: 12px; }
@media (max-width: 1200px) {
  .metric-strip { grid-template-columns: repeat(3, 1fr); }
  .metric-item:nth-child(3) { border-right: 0; }
  .metric-item:nth-child(-n + 3) { border-bottom: 1px solid #e5e5e5; }
}
@media (max-width: 900px) {
  .dashboard-header { align-items: stretch; flex-direction: column; }
  .dashboard-grid { grid-template-columns: 1fr; }
  .dashboard-content { padding: 16px; }
  .metric-strip { grid-template-columns: repeat(2, 1fr); }
  .metric-item { border-bottom: 1px solid #e5e5e5; }
  .target-summary { grid-template-columns: 1fr; }
}
</style>
