<template>
  <div class="monitor-page creator-detail-page">
    <header class="page-heading">
      <div>
        <el-button link :icon="ArrowLeft" @click="router.back()">返回账号监测</el-button>
        <h1>作者详情</h1>
        <p>查看作者主页资料与系统当前监控的作品。</p>
      </div>
      <div class="heading-actions">
        <el-button v-if="creator?.homepageUrl" :icon="Link" @click="openHomepage">打开主页</el-button>
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
    </header>

    <template v-if="creator">
      <section class="surface profile-band">
        <el-avatar :size="72" :src="creator.avatarUrl">{{ creator.nickname?.slice(0, 1) }}</el-avatar>
        <div class="profile-copy">
          <div>
            <h2>{{ creator.nickname }}</h2>
            <status-badge :status="creator.profileStatus" />
          </div>
          <p>{{ displayAccountId(creator) }}</p>
          <span>{{ creator.signature || '暂无作者简介' }}</span>
        </div>
        <dl class="profile-meta">
          <div><dt>地区</dt><dd>{{ creator.ipLocation || '--' }}</dd></div>
          <div><dt>最近采集</dt><dd>{{ formatTime(creator.lastProfileCollectAt) }}</dd></div>
        </dl>
      </section>

      <section class="surface stats-band">
        <div><span>粉丝</span><strong>{{ formatNumber(creator.followerCount) }}</strong></div>
        <div><span>关注</span><strong>{{ formatNumber(creator.followingCount) }}</strong></div>
        <div><span>累计获赞</span><strong>{{ formatNumber(creator.totalFavoritedCount) }}</strong></div>
        <div><span>主页作品</span><strong>{{ formatNumber(creator.contentCount) }}</strong></div>
        <div><span>系统监控作品</span><strong>{{ total }}</strong></div>
      </section>

      <section class="surface monitored-section">
        <div class="section-heading">
          <div>
            <h2>监控作品</h2>
            <p>这里只展示系统已经建立监控关系的作品。</p>
          </div>
        </div>

        <div v-loading="loading" class="detail-content-grid">
          <article v-for="item in contents" :key="item.contentId">
            <span class="detail-cover">
              <img v-if="item.coverUrl" :src="item.coverUrl" :alt="contentTitle(item)" loading="lazy" />
              <el-icon v-else><VideoCamera /></el-icon>
            </span>
            <div class="detail-content-copy">
              <strong>{{ contentTitle(item) }}</strong>
              <span>{{ formatTime(item.publishTime) }}</span>
              <div class="compact-metrics">
                <span>赞 {{ formatNumber(item.latestLikeCount) }}</span>
                <span>评 {{ formatNumber(item.latestCommentCount) }}</span>
                <span>藏 {{ formatNumber(item.latestCollectCount) }}</span>
                <span>享 {{ formatNumber(item.latestShareCount) }}</span>
              </div>
            </div>
            <el-button v-if="item.contentUrl || item.shareUrl" link @click="openContent(item)">打开</el-button>
          </article>
          <el-empty v-if="!loading && !contents.length" description="暂无监控作品" />
        </div>

        <pagination
          v-show="total > 0"
          v-model:page="query.pageNum"
          v-model:limit="query.pageSize"
          :total="total"
          @pagination="loadContents"
        />
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, Link, Refresh, VideoCamera } from '@element-plus/icons-vue';
import { collectCreatorProfile, collectTarget, getCreatorAccount, listContentPosts, listMonitorTargets } from '@/api/creator';
import type { ContentPost, CreatorAccount, MonitorTarget } from '@/api/creator/types';
import StatusBadge from '../components/StatusBadge.vue';

const route = useRoute();
const router = useRouter();
const creatorId = computed(() => String(route.params.creatorId || ''));
const creator = ref<CreatorAccount>();
const contents = ref<ContentPost[]>([]);
const total = ref(0);
const loading = ref(false);
const refreshing = ref(false);
const query = reactive({ pageNum: 1, pageSize: 12, platform: 'douyin' });

const formatNumber = (value?: number) => (value == null ? '--' : new Intl.NumberFormat('zh-CN').format(value));
const formatTime = (value?: string) => (value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '尚未采集');
const displayAccountId = (row: CreatorAccount) =>
  [row.platformDisplayId, row.platformUserId, row.platformCreatorId].find((value) => value && value !== '0') || '--';
const contentTitle = (row: ContentPost) => {
  if (/^Douyin content \d+$/.test(row.title || '')) return '暂无作品文案';
  return row.title || row.description?.split('\n')[0] || '暂无作品文案';
};

const loadCreator = async () => {
  const res = await getCreatorAccount(creatorId.value);
  creator.value = res.data;
};

const loadContents = async () => {
  loading.value = true;
  try {
    const res = await listContentPosts({ ...query, creatorId: creatorId.value });
    contents.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const findCreatorCollectionTarget = async (): Promise<MonitorTarget | undefined> => {
  const res = await listMonitorTargets({ pageNum: 1, pageSize: 10, platform: 'douyin', creatorId: creatorId.value });
  return res.rows?.find((item) => item.targetType === 'creator_collection');
};

const refreshProfile = async () => {
  refreshing.value = true;
  try {
    const target = await findCreatorCollectionTarget();
    if (target?.targetId) {
      await collectTarget(String(target.targetId));
    } else {
      await collectCreatorProfile(creatorId.value);
    }
    await Promise.all([loadCreator(), loadContents()]);
    ElMessage.success(target?.targetId ? '作者及作品数据已刷新' : '作者主页数据已刷新');
  } finally {
    refreshing.value = false;
  }
};

const openHomepage = () => window.open(creator.value?.homepageUrl, '_blank', 'noopener');
const openContent = (row: ContentPost) => window.open(row.contentUrl || row.shareUrl, '_blank', 'noopener');

onMounted(async () => {
  await Promise.all([loadCreator(), loadContents()]);
});
</script>

<style scoped>
@import '../monitor-page.css';

.profile-band {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 20px;
}

.profile-copy {
  min-width: 0;
}

.profile-copy > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-copy h2 {
  margin: 0;
  color: #171717;
  font-size: 20px;
}

.profile-copy p,
.profile-copy span {
  margin: 5px 0 0;
  color: #737373;
  font-size: 13px;
}

.profile-copy span {
  display: block;
  color: #404040;
}

.profile-meta {
  display: grid;
  gap: 10px;
  margin: 0;
}

.profile-meta div {
  display: grid;
  grid-template-columns: 72px auto;
  gap: 12px;
}

.profile-meta dt {
  color: #737373;
}

.profile-meta dd {
  margin: 0;
  color: #262626;
  text-align: right;
}

.stats-band {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  margin-top: 14px;
  overflow: hidden;
}

.stats-band div {
  padding: 18px 20px;
  border-right: 1px solid #e5e5e5;
}

.stats-band div:last-child {
  border-right: 0;
}

.stats-band span,
.stats-band strong {
  display: block;
}

.stats-band span {
  color: #737373;
  font-size: 12px;
}

.stats-band strong {
  margin-top: 5px;
  color: #171717;
  font-size: 22px;
  font-variant-numeric: tabular-nums;
}

.monitored-section {
  margin-top: 14px;
  padding: 18px;
}

.section-heading h2 {
  margin: 0;
  font-size: 16px;
}

.section-heading p {
  margin: 4px 0 0;
  color: #737373;
  font-size: 12px;
}

.detail-content-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.detail-content-grid article {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-width: 0;
  padding: 9px;
  border: 1px solid #e5e5e5;
  border-radius: 5px;
}

.detail-cover {
  width: 54px;
  height: 68px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 3px;
  color: #737373;
  background: #f0f0ef;
}

.detail-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-content-copy {
  min-width: 0;
}

.detail-content-copy > strong,
.detail-content-copy > span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-content-copy > strong {
  color: #262626;
  font-size: 13px;
}

.detail-content-copy > span {
  margin-top: 5px;
  color: #737373;
  font-size: 11px;
}

.compact-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 3px 8px;
  margin-top: 7px;
}

.compact-metrics span {
  color: #525252;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 1400px) {
  .detail-content-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .profile-band {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .profile-meta {
    grid-column: 1 / -1;
  }

  .stats-band {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
