<template>
  <div class="monitor-page">
    <header class="page-heading">
      <div>
        <h1>账号监测</h1>
        <p>管理抖音作者主页；默认只刷新作者资料，是否自动发现作品由你手动控制。</p>
      </div>
      <el-button v-hasPermi="['creator:account:add']" type="primary" :icon="Plus" @click="dialogVisible = true">添加账号</el-button>
    </header>

    <section class="surface filter-bar">
      <el-input v-model="query.nickname" clearable placeholder="搜索作者昵称" :prefix-icon="Search" @keyup.enter="handleQuery" />
      <el-select v-model="query.ownerUserId" clearable placeholder="全部添加人" @change="handleQuery">
        <el-option
          v-for="item in addedByOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-select v-model="query.profileStatus" clearable placeholder="全部状态" @change="handleQuery">
        <el-option label="数据完整" value="success" />
        <el-option label="部分数据" value="partial" />
        <el-option label="采集失败" value="failed" />
        <el-option label="等待采集" value="waiting_collect" />
      </el-select>
      <el-button :icon="Search" @click="handleQuery">查询</el-button>
    </section>

    <section class="surface table-surface">
      <el-table v-loading="loading" :data="rows">
        <el-table-column label="账号" min-width="260">
          <template #default="{ row }">
            <div class="account-cell">
              <el-tooltip content="进入作者详情" placement="top">
                <button
                  class="avatar-link"
                  type="button"
                  :aria-label="`查看${row.nickname || '作者'}详情`"
                  @click="goCreatorDetail(row.creatorId)"
                >
                  <el-avatar :size="38" :src="row.avatarUrl">{{ row.nickname?.slice(0, 1) }}</el-avatar>
                </button>
              </el-tooltip>
              <div>
                <strong>{{ row.nickname }}</strong>
                <span>{{ displayAccountId(row) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="添加人" prop="addedByName" width="150">
          <template #default="{ row }">{{ row.addedByName || '--' }}</template>
        </el-table-column>
        <el-table-column label="微信号" prop="contactWechat" width="220">
          <template #default="{ row }">
            <span>{{ row.contactWechat || '--' }}</span>
            <el-button
              v-hasPermi="['creator:account:edit']"
              link
              type="primary"
              :icon="Edit"
              @click="openWechatDialog(row)"
            >
              修改
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="粉丝" prop="followerCount" width="130" align="right">
          <template #default="{ row }">{{ formatNumber(row.followerCount) }}</template>
        </el-table-column>
        <el-table-column label="累计获赞" prop="totalFavoritedCount" width="140" align="right">
          <template #default="{ row }">{{ formatNumber(row.totalFavoritedCount) }}</template>
        </el-table-column>
        <el-table-column label="作品" prop="contentCount" width="100" align="right" />
        <el-table-column label="数据状态" width="130">
          <template #default="{ row }"><status-badge :status="row.profileStatus" /></template>
        </el-table-column>
        <el-table-column label="最近采集" width="180">
          <template #default="{ row }">{{ formatTime(row.lastProfileCollectAt) }}</template>
        </el-table-column>
        <el-table-column label="自动发现作品" width="150" align="center">
          <template #default="{ row }">
            <el-tooltip content="开启后，定时任务会从作者主页发现并加入新作品；关闭后只刷新已监控作品的数据。">
              <el-switch
                :model-value="isDiscoverEnabled(row)"
                :loading="switchingId === row.creatorId"
                @change="(value) => changeDiscover(row, Boolean(value))"
              />
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreator(row.creatorId)">查看</el-button>
            <el-button
              v-hasPermi="['creator:target:collect']"
              link
              type="primary"
              :icon="Refresh"
              :loading="syncingId === row.creatorId"
              :disabled="!row.targetId"
              @click="syncCreatorContents(row)"
            >
              同步作品
            </el-button>
            <el-button
              v-hasPermi="['creator:account:remove']"
              link
              type="danger"
              :loading="deletingId === row.creatorId"
              @click="removeCreator(row)"
            >
              取消监控
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="loadData"
      />
    </section>

    <el-dialog v-model="dialogVisible" title="添加监控账号" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="抖音主页链接 / 分享文案" prop="profileInput">
          <el-input v-model="form.profileInput" type="textarea" :rows="4" placeholder="粘贴抖音作者主页链接或分享文案" />
        </el-form-item>
        <el-form-item label="作者备注">
          <el-input v-model="form.remark" placeholder="例如：正式员工、品牌账号" />
        </el-form-item>
        <el-form-item label="添加人微信号">
          <el-input v-model="form.contactWechat" clearable placeholder="填写添加这个账号的微信号" />
        </el-form-item>
        <el-form-item label="自动发现作者新作品">
          <el-switch
            v-model="form.discoverNewContent"
            active-text="开启"
            inactive-text="关闭"
          />
          <p class="form-tip">建议默认关闭。开启后，定时任务会从作者主页自动加入新作品。</p>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="主页刷新频率">
            <el-select v-model="form.profileCollectIntervalMin">
              <el-option label="每6小时" :value="360" />
              <el-option label="每12小时" :value="720" />
              <el-option label="每天" :value="1440" />
            </el-select>
          </el-form-item>
          <el-form-item label="作品指标刷新频率">
            <el-select v-model="form.contentCollectIntervalMin">
              <el-option label="每30分钟" :value="30" />
              <el-option label="每1小时" :value="60" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">添加并识别</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="wechatDialogVisible" title="修改微信号" width="420px">
      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input :model-value="wechatForm.nickname" disabled />
        </el-form-item>
        <el-form-item label="添加人微信号">
          <el-input v-model="wechatForm.contactWechat" clearable placeholder="填写添加这个账号的微信号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="wechatDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="wechatSubmitting" @click="submitWechat">保存</el-button>
      </template>
    </el-dialog>

    <creator-detail-drawer
      v-model="creatorDrawerVisible"
      :creator-id="selectedCreatorId"
      @refreshed="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { Edit, Plus, Refresh, Search } from '@element-plus/icons-vue';
import {
  addCreatorMonitor,
  collectTarget,
  deleteCreatorMonitors,
  listCreatorAccounts,
  updateCreatorContactWechat,
  updateCreatorDiscoverNewContent
} from '@/api/creator';
import type { CreatorAccount, CreatorMonitorForm } from '@/api/creator/types';
import CreatorDetailDrawer from '../components/CreatorDetailDrawer.vue';
import StatusBadge from '../components/StatusBadge.vue';

const router = useRouter();
const loading = ref(false);
const submitting = ref(false);
const wechatSubmitting = ref(false);
const deletingId = ref('');
const syncingId = ref('');
const switchingId = ref('');
const dialogVisible = ref(false);
const wechatDialogVisible = ref(false);
const creatorDrawerVisible = ref(false);
const selectedCreatorId = ref('');
const rows = ref<CreatorAccount[]>([]);
const total = ref(0);
const formRef = ref<ElFormInstance>();
const addedByOptions = ref<{ label: string; value: string }[]>([]);
const query = reactive({ pageNum: 1, pageSize: 10, platform: 'douyin', nickname: '', ownerUserId: '', profileStatus: '' });
const form = reactive<CreatorMonitorForm>({
  platform: 'douyin',
  profileInput: '',
  remark: '',
  contactWechat: '',
  discoverNewContent: false,
  profileCollectIntervalMin: 360,
  contentCollectIntervalMin: 30
});
const wechatForm = reactive({
  creatorId: '',
  nickname: '',
  contactWechat: ''
});
const rules: ElFormRules = {
  profileInput: [{ required: true, message: '请粘贴抖音主页链接或分享文案', trigger: 'blur' }]
};

const formatNumber = (value?: number) => new Intl.NumberFormat('zh-CN').format(value || 0);
const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '尚未采集';
const isDiscoverEnabled = (row: CreatorAccount) => row.discoverNewContent === true;
const displayAccountId = (row: CreatorAccount) => {
  const candidates = [row.platformDisplayId, row.platformUserId, row.platformCreatorId];
  return candidates.find((value) => value && value !== '0') || '--';
};

const openCreator = (creatorId: string) => {
  selectedCreatorId.value = creatorId;
  creatorDrawerVisible.value = true;
};

const goCreatorDetail = (creatorId: string) => {
  router.push(`/douyin/account/detail/${creatorId}`);
};

const openWechatDialog = (row: CreatorAccount) => {
  wechatForm.creatorId = row.creatorId;
  wechatForm.nickname = row.nickname || displayAccountId(row);
  wechatForm.contactWechat = row.contactWechat || '';
  wechatDialogVisible.value = true;
};

const queryParams = () => ({
  ...query,
  ownerUserId: query.ownerUserId || undefined
});

const loadAddedByOptions = async () => {
  const res = await listCreatorAccounts({ pageNum: 1, pageSize: 1000, platform: 'douyin' });
  const optionMap = new Map<string, string>();
  for (const row of res.rows || []) {
    if (row.ownerUserId && row.addedByName) {
      optionMap.set(String(row.ownerUserId), row.addedByName);
    }
  }
  addedByOptions.value = Array.from(optionMap, ([value, label]) => ({ value, label }));
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await listCreatorAccounts(queryParams());
    rows.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const handleQuery = () => {
  query.pageNum = 1;
  loadData();
};

const changeDiscover = async (row: CreatorAccount, enabled: boolean) => {
  const previous = row.discoverNewContent;
  row.discoverNewContent = enabled;
  switchingId.value = row.creatorId;
  try {
    await updateCreatorDiscoverNewContent(row.creatorId, enabled);
    ElMessage.success(enabled ? '已开启自动发现作品' : '已关闭自动发现作品');
  } catch (error) {
    row.discoverNewContent = previous;
    throw error;
  } finally {
    switchingId.value = '';
  }
};

const syncCreatorContents = async (row: CreatorAccount) => {
  if (!row.targetId) {
    ElMessage.warning('没有找到该账号的监控目标，无法同步作品');
    return;
  }
  await ElMessageBox.confirm(
    `将立即从“${row.nickname || '该作者'}”主页同步一次作品，可能会消耗接口额度。是否继续？`,
    '同步作者作品',
    { type: 'warning', confirmButtonText: '同步作品', cancelButtonText: '取消' }
  );
  syncingId.value = row.creatorId;
  try {
    await collectTarget(String(row.targetId));
    ElMessage.success('作者作品已同步');
    await loadData();
  } finally {
    syncingId.value = '';
  }
};

const removeCreator = async (row: CreatorAccount) => {
  await ElMessageBox.confirm(
    `确定取消对“${row.nickname || '该作者'}”的账号监控吗？已采集的作者、作品及历史数据都会保留。`,
    '取消账号监控',
    { type: 'warning', confirmButtonText: '取消监控', cancelButtonText: '返回' }
  );
  deletingId.value = row.creatorId;
  try {
    await deleteCreatorMonitors([row.creatorId]);
    ElMessage.success('已取消账号监控');
    await loadAddedByOptions();
    await loadData();
  } finally {
    deletingId.value = '';
  }
};

const submitWechat = async () => {
  if (!wechatForm.creatorId) return;
  wechatSubmitting.value = true;
  try {
    await updateCreatorContactWechat(wechatForm.creatorId, wechatForm.contactWechat);
    ElMessage.success('微信号已更新');
    wechatDialogVisible.value = false;
    await loadData();
  } finally {
    wechatSubmitting.value = false;
  }
};

const submit = async () => {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    await addCreatorMonitor(form);
    ElMessage.success('账号已添加，主页信息采集成功');
    dialogVisible.value = false;
    form.profileInput = '';
    form.remark = '';
    form.contactWechat = '';
    form.discoverNewContent = false;
    await loadAddedByOptions();
    await loadData();
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  loadAddedByOptions();
  loadData();
});
</script>

<style scoped>
@import '../monitor-page.css';

.avatar-link {
  display: inline-flex;
  width: 44px;
  height: 44px;
  padding: 3px;
  align-items: center;
  justify-content: center;
  flex: 0 0 44px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  transition: background-color 160ms ease, box-shadow 160ms ease;
}

.avatar-link:hover {
  background: #f2f4f7;
}

.avatar-link:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.form-tip {
  margin: 6px 0 0;
  color: #737373;
  font-size: 12px;
  line-height: 18px;
}
</style>
