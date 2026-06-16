<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <template #header>
        <div class="submission-header">
          <div>
            <h2>作品审核</h2>
            <p>审核兼职人员提交的抖音作品，通过后自动加入内容监测。</p>
          </div>
          <el-button :icon="Refresh" :loading="loading" @click="getList">刷新</el-button>
        </div>
      </template>

      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="状态" prop="submissionStatus">
          <el-select v-model="queryParams.submissionStatus" placeholder="全部状态" clearable style="width: 150px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="平台" prop="platform">
          <el-select v-model="queryParams.platform" placeholder="全部平台" clearable style="width: 130px">
            <el-option label="抖音" value="douyin" />
          </el-select>
        </el-form-item>
        <el-form-item label="作品链接" prop="contentUrl">
          <el-input v-model="queryParams.contentUrl" placeholder="搜索提交链接" clearable style="width: 260px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-table v-loading="loading" :data="submissionList" border>
        <el-table-column label="任务" prop="taskTitle" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.taskTitle || row.taskId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="兼职人员" min-width="150">
          <template #default="{ row }">
            <div class="staff-cell">
              <strong>{{ row.realName || row.userId }}</strong>
              <span>{{ row.phone || row.douyinId || '--' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="作品链接" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :href="row.contentUrl" target="_blank">{{ row.contentUrl }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="contentDesc" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.contentDesc || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="submissionStatus" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.submissionStatus).type">{{ statusMeta(row.submissionStatus).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" prop="submitTime" width="180">
          <template #default="{ row }">
            <span>{{ formatTime(row.submitTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="监测" width="130" align="center">
          <template #default="{ row }">
            <el-button v-if="row.monitorContentId" link type="primary" @click="goMonitor(row)">查看监测</el-button>
            <span v-else class="muted">未关联</span>
          </template>
        </el-table-column>
        <el-table-column label="驳回原因" prop="rejectReason" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.rejectReason || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-hasPermi="['parttime:submission:approve']"
              link
              type="success"
              :disabled="row.submissionStatus !== 'pending'"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-hasPermi="['parttime:submission:reject']"
              link
              type="danger"
              :disabled="row.submissionStatus !== 'pending'"
              @click="handleReject(row)"
            >
              驳回
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script setup name="ParttimeSubmission" lang="ts">
import { Refresh, Search } from '@element-plus/icons-vue';
import { ElMessageBox } from 'element-plus';
import { approveParttimeSubmission, listParttimeSubmissions, rejectParttimeSubmission } from '@/api/parttime/submission';
import type { PtTaskSubmission, PtTaskSubmissionQuery, SubmissionStatus } from '@/api/parttime/submission/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const router = useRouter();

const loading = ref(false);
const submissionList = ref<PtTaskSubmission[]>([]);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();

const queryParams = reactive<PtTaskSubmissionQuery>({
  pageNum: 1,
  pageSize: 10,
  platform: '',
  contentUrl: '',
  submissionStatus: 'pending'
});

const statusOptions: Array<{ label: string; value: SubmissionStatus }> = [
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' }
];

const statusMeta = (status?: SubmissionStatus | string) => {
  const map: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' }> = {
    pending: { label: '待审核', type: 'warning' },
    approved: { label: '已通过', type: 'success' },
    rejected: { label: '已驳回', type: 'danger' }
  };
  return map[status || ''] || { label: status || '未知', type: 'info' };
};

const formatTime = (value?: string) => (value ? proxy?.parseTime(value) || value : '--');

const getList = async () => {
  loading.value = true;
  try {
    const res = await listParttimeSubmissions(queryParams);
    submissionList.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  queryParams.pageNum = 1;
  getList();
};

const handleApprove = async (row: PtTaskSubmission) => {
  await proxy?.$modal.confirm(`确认通过该作品？通过后会自动加入抖音内容监测。`);
  await approveParttimeSubmission(row.submissionId);
  proxy?.$modal.msgSuccess('审核通过，已加入内容监测');
  await getList();
};

const handleReject = async (row: PtTaskSubmission) => {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回作品提交', {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '驳回原因不能为空'
  });
  await rejectParttimeSubmission(row.submissionId, value);
  proxy?.$modal.msgSuccess('已驳回');
  await getList();
};

const goMonitor = (row: PtTaskSubmission) => {
  router.push(`/douyin/content/detail/${row.monitorContentId}`);
};

onMounted(getList);
</script>

<style scoped>
.submission-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.submission-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.submission-header p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.staff-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.staff-cell span,
.muted {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
