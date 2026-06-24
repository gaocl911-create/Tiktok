<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <template #header>
        <div class="task-header">
          <div>
            <h2>兼职任务</h2>
            <p>创建、编辑并发布给小程序兼职人员领取的推广任务。</p>
          </div>
          <div class="task-header-actions">
            <el-button :icon="Refresh" :loading="loading" @click="getList">刷新</el-button>
            <el-button v-hasPermi="['parttime:task:add']" type="primary" :icon="Plus" @click="handleAdd">新增任务</el-button>
          </div>
        </div>
      </template>

      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="任务标题" prop="taskTitle">
          <el-input v-model="queryParams.taskTitle" placeholder="请输入任务标题" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="平台" prop="platform">
          <el-select v-model="queryParams.platform" placeholder="全部平台" clearable style="width: 140px">
            <el-option label="抖音" value="douyin" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="taskStatus">
          <el-select v-model="queryParams.taskStatus" placeholder="全部状态" clearable style="width: 150px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-table v-loading="loading" :data="taskList" border>
        <el-table-column label="任务标题" prop="taskTitle" min-width="220" show-overflow-tooltip />
        <el-table-column label="平台" prop="platform" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ platformLabel(row.platform) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="单价" prop="unitPrice" width="110" align="right">
          <template #default="{ row }">
            <strong class="money">￥{{ formatMoney(row.unitPrice) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="名额" width="170" align="center">
          <template #default="{ row }">
            <span>{{ row.claimedCount || 0 }} / {{ row.totalQuota }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交/通过" width="140" align="center">
          <template #default="{ row }">
            <span>{{ row.submittedCount || 0 }} / {{ row.approvedCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="素材分类" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="material-cell">
              <span>文案：{{ row.textCategoryName || '未配置' }}</span>
              <span>图片：{{ row.imageCategoryName || '未配置' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="260">
          <template #default="{ row }">
            <span>{{ formatTime(row.startTime) }} 至 {{ formatTime(row.endTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="taskStatus" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.taskStatus).type">{{ statusMeta(row.taskStatus).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="180">
          <template #default="{ row }">
            <span>{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPermi="['parttime:task:edit']" link type="primary" :disabled="!canEdit(row)" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-hasPermi="['parttime:task:publish']"
              link
              type="success"
              :disabled="!canPublish(row)"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button v-hasPermi="['parttime:task:pause']" link type="warning" :disabled="row.taskStatus !== 'published'" @click="handlePause(row)">
              暂停
            </el-button>
            <el-button
              v-hasPermi="['parttime:task:finish']"
              link
              type="danger"
              :disabled="row.taskStatus !== 'published' && row.taskStatus !== 'paused'"
              @click="handleFinish(row)"
            >
              结束
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

    <el-dialog v-model="dialogVisible" :title="form.taskId ? '编辑兼职任务' : '新增兼职任务'" width="680px" append-to-body>
      <el-form ref="taskFormRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="任务标题" prop="taskTitle">
          <el-input v-model="form.taskTitle" placeholder="例如：本地门店探店短视频推广" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="平台" prop="platform">
          <el-select v-model="form.platform" style="width: 100%">
            <el-option label="抖音" value="douyin" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价" prop="unitPrice">
          <el-input-number v-model="form.unitPrice" :precision="2" :min="0" :step="1" style="width: 180px" />
          <span class="form-tip">元 / 审核通过作品</span>
        </el-form-item>
        <el-form-item label="名额" prop="totalQuota">
          <el-input-number v-model="form.totalQuota" :min="1" :step="1" :precision="0" style="width: 180px" />
        </el-form-item>
        <el-form-item label="文案分类" prop="textCategoryId">
          <el-select v-model="form.textCategoryId" placeholder="请选择文案分类" filterable style="width: 100%">
            <el-option v-for="item in textCategoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片分类" prop="imageCategoryId">
          <el-select v-model="form.imageCategoryId" placeholder="请选择图片分类" filterable style="width: 100%">
            <el-option v-for="item in imageCategoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="截止时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="任务说明" prop="taskDesc">
          <el-input v-model="form.taskDesc" type="textarea" :rows="3" placeholder="给后台和小程序展示的任务背景、活动说明" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="发布要求" prop="taskRequirement">
          <el-input
            v-model="form.taskRequirement"
            type="textarea"
            :rows="5"
            placeholder="例如：视频不少于15秒；需带指定话题；作品保留30天；禁止搬运等"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="仅后台可见" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ParttimeTask" lang="ts">
import { Plus, Refresh, Search } from '@element-plus/icons-vue';
import {
  addParttimeTask,
  finishParttimeTask,
  listParttimeTasks,
  pauseParttimeTask,
  publishParttimeTask,
  updateParttimeTask
} from '@/api/parttime/task';
import type { PromotionTaskStatus, PtPromotionTask, PtPromotionTaskForm, PtPromotionTaskQuery } from '@/api/parttime/task/types';
import { listMaterialCategoryOptions } from '@/api/parttime/material/category';
import type { PtMaterialCategory } from '@/api/parttime/material/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const taskList = ref<PtPromotionTask[]>([]);
const textCategoryOptions = ref<PtMaterialCategory[]>([]);
const imageCategoryOptions = ref<PtMaterialCategory[]>([]);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const taskFormRef = ref<ElFormInstance>();
const timeRange = ref<[string, string] | []>([]);

const queryParams = reactive<PtPromotionTaskQuery>({
  pageNum: 1,
  pageSize: 10,
  taskTitle: '',
  platform: '',
  taskStatus: ''
});

const defaultForm = (): PtPromotionTaskForm => ({
  taskTitle: '',
  platform: 'douyin',
  taskDesc: '',
  taskRequirement: '',
  unitPrice: 0,
  totalQuota: 10,
  textCategoryId: undefined,
  imageCategoryId: undefined,
  startTime: '',
  endTime: '',
  remark: ''
});

const form = reactive<PtPromotionTaskForm>(defaultForm());

const rules: ElFormRules = {
  taskTitle: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  platform: [{ required: true, message: '请选择平台', trigger: 'change' }],
  unitPrice: [{ required: true, message: '请输入任务单价', trigger: 'blur' }],
  totalQuota: [{ required: true, message: '请输入任务名额', trigger: 'blur' }],
  textCategoryId: [{ required: true, message: '请选择文案分类', trigger: 'change' }],
  imageCategoryId: [{ required: true, message: '请选择图片分类', trigger: 'change' }]
};

const statusOptions: Array<{ label: string; value: PromotionTaskStatus }> = [
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' },
  { label: '已暂停', value: 'paused' },
  { label: '已结束', value: 'finished' }
];

const statusMeta = (status?: PromotionTaskStatus | string) => {
  const map: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' }> = {
    draft: { label: '草稿', type: 'info' },
    published: { label: '已发布', type: 'success' },
    paused: { label: '已暂停', type: 'warning' },
    finished: { label: '已结束', type: 'danger' }
  };
  return map[status || ''] || { label: status || '未知', type: 'info' };
};

const platformLabel = (platform?: string) => {
  const map: Record<string, string> = { douyin: '抖音' };
  return map[platform || ''] || platform || '--';
};

const formatMoney = (value?: number) => Number(value || 0).toFixed(2);
const formatTime = (value?: string) => (value ? proxy?.parseTime(value) || value : '--');
const canEdit = (row: PtPromotionTask) => row.taskStatus === 'draft' || row.taskStatus === 'paused';
const canPublish = (row: PtPromotionTask) => row.taskStatus === 'draft' || row.taskStatus === 'paused';

const getList = async () => {
  loading.value = true;
  try {
    const res = await listParttimeTasks(queryParams);
    taskList.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const loadMaterialOptions = async () => {
  const [textRes, imageRes] = await Promise.all([listMaterialCategoryOptions('text'), listMaterialCategoryOptions('image')]);
  textCategoryOptions.value = textRes.data || [];
  imageCategoryOptions.value = imageRes.data || [];
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

const resetForm = () => {
  Object.assign(form, defaultForm());
  timeRange.value = [];
  taskFormRef.value?.clearValidate();
};

const handleAdd = () => {
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row: PtPromotionTask) => {
  resetForm();
  Object.assign(form, {
    taskId: row.taskId,
    taskTitle: row.taskTitle,
    platform: row.platform || 'douyin',
    taskDesc: row.taskDesc || '',
    taskRequirement: row.taskRequirement || '',
    unitPrice: Number(row.unitPrice || 0),
    totalQuota: Number(row.totalQuota || 1),
    textCategoryId: row.textCategoryId,
    imageCategoryId: row.imageCategoryId,
    startTime: row.startTime || '',
    endTime: row.endTime || '',
    remark: row.remark || ''
  });
  timeRange.value = row.startTime && row.endTime ? [row.startTime, row.endTime] : [];
  dialogVisible.value = true;
};

const submitForm = async () => {
  await taskFormRef.value?.validate();
  const [startTime, endTime] = timeRange.value;
  form.startTime = startTime || '';
  form.endTime = endTime || '';
  submitting.value = true;
  try {
    if (form.taskId) {
      await updateParttimeTask(form);
      proxy?.$modal.msgSuccess('任务已更新');
    } else {
      await addParttimeTask(form);
      proxy?.$modal.msgSuccess('任务已创建');
    }
    dialogVisible.value = false;
    await getList();
  } finally {
    submitting.value = false;
  }
};

const handlePublish = async (row: PtPromotionTask) => {
  await proxy?.$modal.confirm(`确认发布任务「${row.taskTitle}」？发布后小程序端将可以领取。`);
  await publishParttimeTask(row.taskId);
  proxy?.$modal.msgSuccess('任务已发布');
  await getList();
};

const handlePause = async (row: PtPromotionTask) => {
  await proxy?.$modal.confirm(`确认暂停任务「${row.taskTitle}」？暂停后新用户暂时不能领取。`);
  await pauseParttimeTask(row.taskId);
  proxy?.$modal.msgSuccess('任务已暂停');
  await getList();
};

const handleFinish = async (row: PtPromotionTask) => {
  await proxy?.$modal.confirm(`确认结束任务「${row.taskTitle}」？结束后不能再领取。`);
  await finishParttimeTask(row.taskId);
  proxy?.$modal.msgSuccess('任务已结束');
  await getList();
};

onMounted(async () => {
  await loadMaterialOptions();
  await getList();
});
</script>

<style scoped>
.task-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.task-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.task-header p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.task-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.money {
  color: #d97706;
  font-variant-numeric: tabular-nums;
}

.material-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.form-tip {
  margin-left: 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
