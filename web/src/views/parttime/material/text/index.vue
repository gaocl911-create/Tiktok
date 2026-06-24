<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <template #header>
        <div class="page-header">
          <div>
            <h2>文案库</h2>
            <p>维护任务领取时自动分配给兼职人员复制使用的推广文案。</p>
          </div>
          <div class="header-actions">
            <el-button :icon="Refresh" :loading="loading" @click="getList">刷新</el-button>
            <el-button v-hasPermi="['parttime:material:text:add']" type="primary" :icon="Plus" @click="handleAdd">新增文案</el-button>
          </div>
        </div>
      </template>

      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="queryParams.categoryId" placeholder="全部分类" clearable filterable style="width: 220px">
            <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="文案" prop="content">
          <el-input v-model="queryParams.content" placeholder="搜索文案内容" clearable style="width: 260px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="启用" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-table v-loading="loading" :data="textList" border>
        <el-table-column label="分类" prop="categoryName" width="180" show-overflow-tooltip />
        <el-table-column label="文案内容" prop="content" min-width="420" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="content-preview">{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="sort" width="90" align="center" />
        <el-table-column label="创建时间" prop="createTime" width="180">
          <template #default="{ row }">
            <span>{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPermi="['parttime:material:text:edit']" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-hasPermi="['parttime:material:text:remove']" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.textId ? '编辑文案' : '新增文案'" width="720px" append-to-body>
      <el-form ref="textFormRef" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="文案分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择文案分类" filterable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="文案内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="填写要分配给兼职人员复制的文案" maxlength="3000" show-word-limit />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :precision="0" />
          <span class="form-tip">数字越小越先分配</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ParttimeMaterialText" lang="ts">
import { Plus, Refresh, Search } from '@element-plus/icons-vue';
import { listMaterialCategoryOptions } from '@/api/parttime/material/category';
import { addMaterialText, deleteMaterialText, listMaterialTexts, updateMaterialText } from '@/api/parttime/material/text';
import type { PtMaterialCategory, PtMaterialText, PtMaterialTextForm, PtMaterialTextQuery } from '@/api/parttime/material/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const textList = ref<PtMaterialText[]>([]);
const categoryOptions = ref<PtMaterialCategory[]>([]);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const textFormRef = ref<ElFormInstance>();

const queryParams = reactive<PtMaterialTextQuery>({
  pageNum: 1,
  pageSize: 10,
  categoryId: undefined,
  content: '',
  status: ''
});

const defaultForm = (): PtMaterialTextForm => ({
  categoryId: undefined,
  content: '',
  sort: 0,
  status: '0',
  remark: ''
});

const form = reactive<PtMaterialTextForm>(defaultForm());

const rules: ElFormRules = {
  categoryId: [{ required: true, message: '请选择文案分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入文案内容', trigger: 'blur' }]
};

const statusLabel = (status?: string) => (status === '0' ? '启用' : '停用');
const formatTime = (value?: string) => (value ? proxy?.parseTime(value) || value : '--');

const loadOptions = async () => {
  const res = await listMaterialCategoryOptions('text');
  categoryOptions.value = res.data || [];
};

const getList = async () => {
  loading.value = true;
  try {
    const res = await listMaterialTexts(queryParams);
    textList.value = res.rows || [];
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

const resetForm = () => {
  Object.assign(form, defaultForm());
  textFormRef.value?.clearValidate();
};

const handleAdd = () => {
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row: PtMaterialText) => {
  resetForm();
  Object.assign(form, {
    textId: row.textId,
    categoryId: row.categoryId,
    content: row.content || '',
    sort: row.sort || 0,
    status: row.status || '0',
    remark: row.remark || ''
  });
  dialogVisible.value = true;
};

const submitForm = async () => {
  await textFormRef.value?.validate();
  submitting.value = true;
  try {
    if (form.textId) {
      await updateMaterialText(form);
      proxy?.$modal.msgSuccess('文案已更新');
    } else {
      await addMaterialText(form);
      proxy?.$modal.msgSuccess('文案已创建');
    }
    dialogVisible.value = false;
    await getList();
  } finally {
    submitting.value = false;
  }
};

const handleDelete = async (row: PtMaterialText) => {
  await proxy?.$modal.confirm('确认删除该文案？已分配历史会继续保留快照。');
  await deleteMaterialText(row.textId);
  proxy?.$modal.msgSuccess('已删除');
  await getList();
};

onMounted(async () => {
  await loadOptions();
  await getList();
});
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.page-header p,
.form-tip {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.page-header p {
  margin: 6px 0 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.content-preview {
  white-space: pre-wrap;
}

.form-tip {
  margin-left: 10px;
}
</style>
