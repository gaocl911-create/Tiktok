<template>
  <el-drawer
    :model-value="modelValue"
    class="creator-detail-drawer"
    direction="rtl"
    size="min(520px, 100vw)"
    destroy-on-close
    @close="emit('update:modelValue', false)"
    @open="loadDetail"
  >
    <template #header>
      <div>
        <strong class="drawer-title">作者详情</strong>
        <p class="drawer-subtitle">作者资料来自系统最近一次采集</p>
      </div>
    </template>

    <div v-loading="loading" class="drawer-content">
      <template v-if="creator">
        <section class="creator-profile">
          <el-avatar :size="64" :src="creator.avatarUrl">{{ creator.nickname?.slice(0, 1) }}</el-avatar>
          <div class="creator-identity">
            <div class="creator-name-row">
              <h2>{{ creator.nickname || '未知作者' }}</h2>
              <status-badge :status="creator.profileStatus" />
            </div>
            <p>{{ displayAccountId(creator) }}</p>
            <span v-if="creator.ipLocation">{{ creator.ipLocation }}</span>
          </div>
        </section>

        <p v-if="creator.signature" class="creator-signature">{{ creator.signature }}</p>

        <section class="stat-grid">
          <div>
            <span>粉丝</span>
            <strong>{{ formatNumber(creator.followerCount) }}</strong>
          </div>
          <div>
            <span>关注</span>
            <strong>{{ formatNumber(creator.followingCount) }}</strong>
          </div>
          <div>
            <span>累计获赞</span>
            <strong>{{ formatNumber(creator.totalFavoritedCount) }}</strong>
          </div>
          <div>
            <span>作品</span>
            <strong>{{ formatNumber(creator.contentCount) }}</strong>
          </div>
        </section>

        <section class="detail-section">
          <div class="section-heading">
            <h3>监控信息</h3>
          </div>
          <dl class="detail-list">
            <div>
              <dt>监控类型</dt>
              <dd>{{ monitorTypeText }}</dd>
            </div>
            <div>
              <dt>作者备注</dt>
              <dd>{{ target?.remark || '暂无备注' }}</dd>
            </div>
            <div>
              <dt>标签</dt>
              <dd>{{ target?.tags || '暂无标签' }}</dd>
            </div>
            <div>
              <dt>主页刷新</dt>
              <dd>{{ intervalText(target?.profileCollectIntervalMin) }}</dd>
            </div>
            <div>
              <dt>最近采集</dt>
              <dd>{{ formatTime(creator.lastProfileCollectAt) }}</dd>
            </div>
            <div>
              <dt>系统监控作品</dt>
              <dd>{{ monitoredContentTotal }} 条</dd>
            </div>
          </dl>
        </section>

        <section class="detail-section">
          <div class="section-heading">
            <h3>最近监控作品</h3>
            <el-button link type="primary" @click="goFullDetail">查看全部</el-button>
          </div>
          <div v-if="recentContents.length" class="recent-content-list">
            <button v-for="item in recentContents" :key="item.contentId" type="button" @click="openContent(item)">
              <span class="recent-cover">
                <img v-if="item.coverUrl" :src="item.coverUrl" :alt="contentTitle(item)" loading="lazy" />
                <el-icon v-else><VideoCamera /></el-icon>
              </span>
              <span class="recent-info">
                <strong>{{ contentTitle(item) }}</strong>
                <small>{{ formatTime(item.publishTime) }}</small>
              </span>
            </button>
          </div>
          <el-empty v-else :image-size="72" description="暂无监控作品" />
        </section>
      </template>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="emit('update:modelValue', false)">关闭</el-button>
        <el-button @click="goFullDetail">完整详情</el-button>
        <el-button
          v-hasPermi="['creator:target:collect']"
          type="primary"
          :icon="Refresh"
          :loading="refreshing"
          @click="refreshProfile"
        >
          刷新作者数据
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { Refresh, VideoCamera } from '@element-plus/icons-vue';
import { collectCreatorProfile, getCreatorAccount, listContentPosts, listMonitorTargets } from '@/api/creator';
import type { ContentPost, CreatorAccount, MonitorTarget } from '@/api/creator/types';
import StatusBadge from './StatusBadge.vue';

const props = defineProps<{
  modelValue: boolean;
  creatorId?: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  refreshed: [];
}>();

const router = useRouter();
const loading = ref(false);
const refreshing = ref(false);
const creator = ref<CreatorAccount>();
const target = ref<MonitorTarget>();
const recentContents = ref<ContentPost[]>([]);
const monitoredContentTotal = ref(0);

const formatNumber = (value?: number) => (value == null ? '--' : new Intl.NumberFormat('zh-CN').format(value));
const formatTime = (value?: string) => (value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '尚未采集');
const displayAccountId = (row: CreatorAccount) =>
  [row.platformDisplayId, row.platformUserId, row.platformCreatorId].find((value) => value && value !== '0') || '--';
const intervalText = (minutes?: number) => {
  if (!minutes) return '未设置';
  if (minutes % 1440 === 0) return `每 ${minutes / 1440} 天`;
  if (minutes % 60 === 0) return `每 ${minutes / 60} 小时`;
  return `每 ${minutes} 分钟`;
};
const contentTitle = (row: ContentPost) => {
  if (/^Douyin content \d+$/.test(row.title || '')) return '暂无作品文案';
  return row.title || row.description?.split('\n')[0] || '暂无作品文案';
};
const monitorTypeText = computed(() => {
  if (!target.value) return '仅关联作者';
  return target.value.targetType === 'creator_collection' ? '作者作品集监控' : '单作品监控';
});

const loadDetail = async () => {
  if (!props.creatorId) return;
  loading.value = true;
  try {
    const [creatorRes, targetRes, contentRes] = await Promise.all([
      getCreatorAccount(props.creatorId),
      listMonitorTargets({ pageNum: 1, pageSize: 10, platform: 'douyin', creatorId: props.creatorId }),
      listContentPosts({ pageNum: 1, pageSize: 5, platform: 'douyin', creatorId: props.creatorId })
    ]);
    creator.value = creatorRes.data;
    target.value =
      targetRes.rows?.find((item) => item.targetType === 'creator_collection') ||
      targetRes.rows?.find((item) => item.targetType === 'single_content') ||
      targetRes.rows?.[0];
    recentContents.value = contentRes.rows || [];
    monitoredContentTotal.value = contentRes.total || 0;
  } finally {
    loading.value = false;
  }
};

const refreshProfile = async () => {
  if (!props.creatorId) return;
  refreshing.value = true;
  try {
    await collectCreatorProfile(props.creatorId);
    await loadDetail();
    emit('refreshed');
    ElMessage.success('作者主页数据已刷新');
  } finally {
    refreshing.value = false;
  }
};

const openContent = (row: ContentPost) => {
  const url = row.contentUrl || row.shareUrl;
  if (url) window.open(url, '_blank', 'noopener');
};

const goFullDetail = () => {
  if (!props.creatorId) return;
  emit('update:modelValue', false);
  router.push(`/douyin/account/detail/${props.creatorId}`);
};
</script>

<style scoped>
.drawer-title {
  color: #171717;
  font-size: 18px;
}

.drawer-subtitle {
  margin: 4px 0 0;
  color: #737373;
  font-size: 12px;
}

.drawer-content {
  min-height: 420px;
}

.creator-profile {
  display: flex;
  align-items: center;
  gap: 14px;
}

.creator-identity {
  min-width: 0;
}

.creator-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.creator-name-row h2 {
  margin: 0;
  overflow: hidden;
  color: #171717;
  font-size: 20px;
  line-height: 28px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.creator-identity p,
.creator-identity span {
  margin: 3px 0 0;
  color: #737373;
  font-size: 12px;
}

.creator-signature {
  margin: 16px 0 0;
  color: #404040;
  font-size: 13px;
  line-height: 20px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 20px;
  overflow: hidden;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
}

.stat-grid div {
  min-width: 0;
  padding: 12px 10px;
  border-right: 1px solid #e5e5e5;
  background: #fafafa;
}

.stat-grid div:last-child {
  border-right: 0;
}

.stat-grid span,
.stat-grid strong {
  display: block;
}

.stat-grid span {
  color: #737373;
  font-size: 12px;
}

.stat-grid strong {
  margin-top: 4px;
  color: #171717;
  font-size: 16px;
  font-variant-numeric: tabular-nums;
}

.detail-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #ededed;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-heading h3 {
  margin: 0;
  color: #171717;
  font-size: 15px;
}

.detail-list {
  margin: 0;
}

.detail-list div {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  gap: 12px;
  padding: 8px 0;
}

.detail-list dt {
  color: #737373;
}

.detail-list dd {
  margin: 0;
  color: #262626;
  text-align: right;
  overflow-wrap: anywhere;
}

.recent-content-list {
  display: grid;
  gap: 8px;
}

.recent-content-list button {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  width: 100%;
  min-height: 56px;
  padding: 6px;
  border: 1px solid transparent;
  border-radius: 4px;
  color: inherit;
  text-align: left;
  background: transparent;
  cursor: pointer;
}

.recent-content-list button:hover,
.recent-content-list button:focus-visible {
  border-color: #dedede;
  background: #fafafa;
  outline: none;
}

.recent-cover {
  width: 44px;
  height: 52px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 3px;
  color: #737373;
  background: #f0f0ef;
}

.recent-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recent-info {
  min-width: 0;
}

.recent-info strong,
.recent-info small {
  display: block;
}

.recent-info strong {
  overflow: hidden;
  color: #262626;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-info small {
  margin-top: 5px;
  color: #737373;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 600px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stat-grid div:nth-child(2) {
    border-right: 0;
  }

  .stat-grid div:nth-child(-n + 2) {
    border-bottom: 1px solid #e5e5e5;
  }
}
</style>
