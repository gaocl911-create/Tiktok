<template>
  <div class="monitor-page content-monitor-page">
    <header class="page-heading">
      <div>
        <h1>内容监测</h1>
        <p>查看作品最新互动指标，并对单个作品执行即时采集。</p>
      </div>
      <div class="heading-actions">
        <el-button v-hasPermi="['creator:target:collect']" :icon="Refresh" :loading="batchLoading" @click="batchCollect">批量刷新</el-button>
        <el-button v-hasPermi="['creator:content:add']" type="primary" :icon="Plus" @click="dialogVisible = true">添加作品</el-button>
      </div>
    </header>

    <section class="surface filter-bar">
      <el-input v-model="query.title" clearable placeholder="搜索作品标题或文案" :prefix-icon="Search" @keyup.enter="loadData" />
      <el-select v-model="query.metricsStatus" clearable placeholder="全部状态">
        <el-option label="数据完整" value="success" />
        <el-option label="部分数据" value="partial" />
        <el-option label="采集失败" value="failed" />
        <el-option label="等待采集" value="waiting_collect" />
      </el-select>
      <el-button :icon="Search" @click="loadData">查询</el-button>
    </section>

    <section v-loading="loading" class="surface content-card-surface">
      <div v-if="rows.length" class="content-grid">
        <article v-for="row in rows" :key="row.contentId" class="content-card">
          <div class="card-main">
            <div class="content-cover">
              <img v-if="row.coverUrl" :src="row.coverUrl" :alt="titleText(row)" loading="lazy" />
              <el-icon v-else><VideoCamera /></el-icon>
            </div>

            <div class="content-info">
              <button
                v-if="row.creatorId"
                class="creator-link"
                type="button"
                :aria-label="`查看作者 ${creatorName(row.creatorId)} 的详情`"
                @click="openCreator(row.creatorId)"
              >
                <el-avatar :size="22" :src="creatorMap[row.creatorId]?.avatarUrl">
                  {{ creatorName(row.creatorId).slice(0, 1) }}
                </el-avatar>
                <span>{{ creatorName(row.creatorId) }}</span>
              </button>
              <div class="content-title-row">
                <strong :title="titleText(row)">{{ titleText(row) }}</strong>
                <status-badge :status="row.metricsStatus" />
              </div>
              <p v-if="descriptionText(row)" class="content-desc" :title="descriptionText(row)">{{ descriptionText(row) }}</p>
              <div class="content-meta">
                <span>发布于 {{ formatTime(row.publishTime) }}</span>
                <span v-if="row.platformContentId">ID {{ row.platformContentId }}</span>
              </div>
            </div>
          </div>

          <div class="metric-grid">
            <div class="metric-item">
              <span>点赞</span>
              <strong>{{ formatNumber(row.latestLikeCount) }}</strong>
            </div>
            <div class="metric-item">
              <span>评论</span>
              <strong>{{ formatNumber(row.latestCommentCount) }}</strong>
            </div>
            <div class="metric-item">
              <span>收藏</span>
              <strong>{{ formatNumber(row.latestCollectCount) }}</strong>
            </div>
            <div class="metric-item">
              <span>分享</span>
              <strong>{{ formatNumber(row.latestShareCount) }}</strong>
            </div>
          </div>

          <div class="card-footer">
            <span>最近采集 {{ formatTime(row.lastMetricsCollectAt) }}</span>
            <div class="card-actions">
              <el-button
                v-hasPermi="['creator:target:collect']"
                link
                type="primary"
                :loading="collectingId === row.contentId"
                @click="collectContent(row)"
              >
                刷新
              </el-button>
              <el-button link type="primary" @click="goContentDetail(row.contentId)">监控详情</el-button>
              <el-button v-if="row.contentUrl || row.shareUrl" link @click="openContent(row)">打开作品</el-button>
              <el-button
                v-hasPermi="['creator:content:remove']"
                link
                type="danger"
                :loading="deletingId === row.contentId"
                @click="removeContent(row)"
              >
                取消监控
              </el-button>
            </div>
          </div>
        </article>
      </div>

      <el-empty v-else description="暂无作品数据" />

      <pagination
        v-show="total > 0"
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="loadData"
      />
    </section>

    <el-dialog v-model="dialogVisible" title="根据链接添加作品" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="作品链接 / 分享文案" prop="contentInput">
          <el-input v-model="form.contentInput" type="textarea" :rows="5" placeholder="粘贴抖音作品链接、短链接或完整分享文案" />
        </el-form-item>
        <el-form-item label="作者备注">
          <el-input v-model="form.remark" placeholder="例如：兼职投放、员工账号" />
        </el-form-item>
        <el-form-item label="监控间隔">
          <el-select v-model="form.contentCollectIntervalMin" style="width: 100%">
            <el-option label="每 30 分钟" :value="30" />
            <el-option label="每 1 小时" :value="60" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">添加监控</el-button>
      </template>
    </el-dialog>

    <creator-detail-drawer
      v-model="creatorDrawerVisible"
      :creator-id="selectedCreatorId"
      @refreshed="handleCreatorRefreshed"
    />
  </div>
</template>

<script setup lang="ts">
import { Plus, Refresh, Search, VideoCamera } from '@element-plus/icons-vue';
import {
  addContentLink,
  collectDueTargets,
  collectTarget,
  deleteContentMonitors,
  getContentTarget,
  getCreatorAccount,
  listContentPosts
} from '@/api/creator';
import type { ContentLinkForm, ContentPost, CreatorAccount, MonitorTarget } from '@/api/creator/types';
import CreatorDetailDrawer from '../components/CreatorDetailDrawer.vue';
import StatusBadge from '../components/StatusBadge.vue';

const loading = ref(false);
const submitting = ref(false);
const batchLoading = ref(false);
const collectingId = ref('');
const deletingId = ref('');
const dialogVisible = ref(false);
const creatorDrawerVisible = ref(false);
const selectedCreatorId = ref('');
const rows = ref<ContentPost[]>([]);
const creatorMap = reactive<Record<string, CreatorAccount>>({});
const total = ref(0);
const formRef = ref<ElFormInstance>();
const query = reactive({ pageNum: 1, pageSize: 12, platform: 'douyin', title: '', metricsStatus: '' });
const form = reactive<ContentLinkForm>({ platform: 'douyin', contentInput: '', remark: '', contentCollectIntervalMin: 30 });
const rules: ElFormRules = {
  contentInput: [{ required: true, message: '请粘贴作品链接或分享文案', trigger: 'blur' }]
};

const formatNumber = (value?: number) => (value == null ? '--' : new Intl.NumberFormat('zh-CN').format(value));
const formatTime = (value?: string) => (value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '尚未采集');
const titleText = (row: ContentPost) => {
  if (/^Douyin content \d+$/.test(row.title || '')) {
    return '暂无作品文案';
  }
  return row.title || row.description?.split('\n')[0] || '暂无作品文案';
};
const descriptionText = (row: ContentPost) => {
  const value = row.description || '';
  if (!value || value === row.title) {
    return '';
  }
  return value.replace(/\s+/g, ' ').trim();
};
const creatorName = (creatorId: string) => creatorMap[creatorId]?.nickname || '查看作者';

const loadCreators = async (contents: ContentPost[]) => {
  const creatorIds = [...new Set(contents.map((item) => item.creatorId).filter((id): id is string => Boolean(id)))];
  await Promise.all(
    creatorIds.map(async (creatorId) => {
      if (creatorMap[creatorId]) return;
      const res = await getCreatorAccount(creatorId);
      if (res.data) creatorMap[creatorId] = res.data;
    })
  );
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await listContentPosts(query);
    rows.value = res.rows || [];
    total.value = res.total || 0;
    await loadCreators(rows.value);
  } finally {
    loading.value = false;
  }
};

const openCreator = (creatorId: string) => {
  selectedCreatorId.value = creatorId;
  creatorDrawerVisible.value = true;
};

const handleCreatorRefreshed = async () => {
  if (!selectedCreatorId.value) return;
  const res = await getCreatorAccount(selectedCreatorId.value);
  if (res.data) creatorMap[selectedCreatorId.value] = res.data;
};

const findTarget = async (contentId: string): Promise<MonitorTarget | undefined> => {
  const res = await getContentTarget(contentId);
  return res.data;
};

const collectContent = async (row: ContentPost) => {
  collectingId.value = row.contentId;
  try {
    const target = await findTarget(row.contentId);
    if (!target) {
      ElMessage.warning('该作品尚未绑定有效监控目标');
      return;
    }
    await collectTarget(target.targetId);
    ElMessage.success('作品指标已刷新');
    await loadData();
  } finally {
    collectingId.value = '';
  }
};

const batchCollect = async () => {
  batchLoading.value = true;
  try {
    const res = await collectDueTargets(100);
    ElMessage.success(`已处理 ${res.data || 0} 个到期监控目标`);
    await loadData();
  } finally {
    batchLoading.value = false;
  }
};

const removeContent = async (row: ContentPost) => {
  await ElMessageBox.confirm(
    `确定取消对“${titleText(row)}”的作品监控吗？只会解除当前监控关系，作品数据和历史快照都会保留。`,
    '取消作品监控关系',
    { type: 'warning', confirmButtonText: '解除关系', cancelButtonText: '返回' }
  );
  deletingId.value = row.contentId;
  try {
    await deleteContentMonitors([row.contentId]);
    ElMessage.success('已取消作品监控关系');
    await loadData();
  } finally {
    deletingId.value = '';
  }
};

const openContent = (row: ContentPost) => window.open(row.contentUrl || row.shareUrl, '_blank', 'noopener');
const router = useRouter();
const goContentDetail = (contentId: string) => router.push(`/douyin/content/detail/${contentId}`);

const submit = async () => {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    await addContentLink(form);
    ElMessage.success('作品已添加并开始监控');
    dialogVisible.value = false;
    form.contentInput = '';
    form.remark = '';
    await loadData();
  } finally {
    submitting.value = false;
  }
};

onMounted(loadData);
</script>

<style scoped>
@import '../monitor-page.css';

.content-card-surface {
  padding: 14px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.content-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 228px;
  padding: 12px;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  background: #fff;
}

.card-main {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 12px;
  min-width: 0;
}

.content-cover {
  width: 76px;
  height: 96px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid #ededed;
  border-radius: 4px;
  color: #737373;
  background: #f5f5f4;
}

.content-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.content-info {
  min-width: 0;
}

.creator-link {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 28px;
  margin: -3px 0 4px;
  padding: 2px 4px 2px 2px;
  border: 0;
  border-radius: 4px;
  color: #525252;
  font: inherit;
  background: transparent;
  cursor: pointer;
}

.creator-link:hover,
.creator-link:focus-visible {
  color: #171717;
  background: #f5f5f4;
  outline: none;
}

.creator-link span {
  margin-left: 6px;
  overflow: hidden;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content-title-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.content-title-row strong {
  flex: 1;
  min-width: 0;
  color: #171717;
  font-size: 14px;
  line-height: 20px;
  font-weight: 650;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.content-desc {
  margin: 7px 0 0;
  color: #525252;
  font-size: 12px;
  line-height: 18px;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.content-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  margin-top: 8px;
  color: #737373;
  font-size: 12px;
  line-height: 18px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 12px;
  border: 1px solid #ededed;
  border-radius: 4px;
  overflow: hidden;
}

.metric-item {
  min-width: 0;
  padding: 8px 8px 7px;
  background: #fafafa;
  border-right: 1px solid #ededed;
}

.metric-item:last-child {
  border-right: 0;
}

.metric-item span,
.metric-item strong {
  display: block;
}

.metric-item span {
  color: #737373;
  font-size: 12px;
  line-height: 16px;
}

.metric-item strong {
  margin-top: 2px;
  color: #171717;
  font-size: 15px;
  line-height: 20px;
  font-variant-numeric: tabular-nums;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 12px;
  color: #737373;
  font-size: 12px;
}

.card-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 2px;
}

.content-card-surface :deep(.pagination-container) {
  margin-top: 16px;
  padding: 0;
  background: transparent;
}

@media (max-width: 1500px) {
  .content-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
