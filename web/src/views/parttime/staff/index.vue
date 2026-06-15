<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <template #header>
        <div class="staff-header">
          <div>
            <h2>兼职人员</h2>
            <p>查看小程序入驻资料，并完成审核通过或驳回。</p>
          </div>
          <el-button icon="Refresh" :loading="loading" @click="getList">刷新</el-button>
        </div>
      </template>

      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="queryParams.realName" placeholder="请输入姓名" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="onboardingStatus">
          <el-select v-model="queryParams.onboardingStatus" placeholder="全部状态" clearable style="width: 160px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-table v-loading="loading" :data="staffList" border>
        <el-table-column label="姓名" prop="realName" min-width="120">
          <template #default="{ row }">
            <span>{{ row.realName || '未填写' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="手机号" prop="phone" width="140">
          <template #default="{ row }">
            <span>{{ row.phone || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="微信号" prop="wechatId" min-width="130">
          <template #default="{ row }">
            <span>{{ row.wechatId || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="地区" prop="region" min-width="140">
          <template #default="{ row }">
            <span>{{ row.region || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="抖音号" prop="douyinId" min-width="140">
          <template #default="{ row }">
            <span>{{ row.douyinId || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="入驻状态" prop="onboardingStatus" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.onboardingStatus).type">{{ statusMeta(row.onboardingStatus).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="驳回原因" prop="rejectReason" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.rejectReason || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="180">
          <template #default="{ row }">
            <span>{{ proxy?.parseTime(row.createTime) || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-hasPermi="['parttime:staff:approve']"
              link
              type="primary"
              :disabled="row.onboardingStatus !== 'pending'"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-hasPermi="['parttime:staff:reject']"
              link
              type="danger"
              :disabled="row.onboardingStatus !== 'pending'"
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

<script setup name="ParttimeStaff" lang="ts">
import { ElMessageBox } from 'element-plus';
import { approveParttimeStaff, listParttimeStaff, rejectParttimeStaff } from '@/api/parttime/staff';
import type { PtStaffProfile, PtStaffQuery, StaffOnboardingStatus } from '@/api/parttime/staff/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
const staffList = ref<PtStaffProfile[]>([]);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();

const queryParams = reactive<PtStaffQuery>({
  pageNum: 1,
  pageSize: 10,
  realName: '',
  onboardingStatus: ''
});

const statusOptions: Array<{ label: string; value: StaffOnboardingStatus }> = [
  { label: '未完善', value: 'incomplete' },
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
  { label: '已禁用', value: 'disabled' }
];

const statusMeta = (status?: StaffOnboardingStatus | string) => {
  const map: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' }> = {
    incomplete: { label: '未完善', type: 'info' },
    pending: { label: '待审核', type: 'warning' },
    approved: { label: '已通过', type: 'success' },
    rejected: { label: '已驳回', type: 'danger' },
    disabled: { label: '已禁用', type: 'info' }
  };
  return map[status || ''] || { label: status || '未知', type: 'info' };
};

const getList = async () => {
  loading.value = true;
  try {
    const res = await listParttimeStaff(queryParams);
    staffList.value = res.rows || [];
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

const handleApprove = async (row: PtStaffProfile) => {
  await proxy?.$modal.confirm(`确认通过「${row.realName || row.userId}」的兼职入驻申请？`);
  await approveParttimeStaff(row.profileId);
  proxy?.$modal.msgSuccess('审核通过');
  await getList();
};

const handleReject = async (row: PtStaffProfile) => {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回兼职入驻', {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '驳回原因不能为空'
  });
  await rejectParttimeStaff(row.profileId, value);
  proxy?.$modal.msgSuccess('已驳回');
  await getList();
};

onMounted(getList);
</script>

<style scoped>
.staff-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.staff-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.staff-header p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
