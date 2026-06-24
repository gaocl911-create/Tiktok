<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <template #header>
        <div class="page-header">
          <div>
            <h2>素材分类</h2>
            <p>管理文案库和图片库的分类，任务发布时会按分类绑定素材。</p>
          </div>
          <div class="header-actions">
            <el-button :icon="Refresh" :loading="loading" @click="getList">刷新</el-button>
            <el-button v-hasPermi="['parttime:material:category:add']" type="primary" :icon="Plus" @click="handleAdd">新增分类</el-button>
          </div>
        </div>
      </template>

      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="queryParams.categoryName" placeholder="请输入分类名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="类型" prop="categoryType">
          <el-select v-model="queryParams.categoryType" placeholder="全部类型" clearable style="width: 140px">
            <el-option label="文案" value="text" />
            <el-option label="图片" value="image" />
          </el-select>
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
      <el-table v-loading="loading" :data="categoryList" border>
        <el-table-column label="分类名称" prop="categoryName" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" prop="categoryType" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.categoryType === 'text' ? 'success' : 'warning'">{{ typeLabel(row.categoryType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="sort" width="90" align="center" />
        <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
        <el-table-column label="创建时间" prop="createTime" width="180">
          <template #default="{ row }">
            <span>{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPermi="['parttime:material:category:edit']" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-hasPermi="['parttime:material:category:remove']" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.categoryId ? '编辑素材分类' : '新增素材分类'" width="520px" append-to-body>
      <el-form ref="categoryFormRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="分类类型" prop="categoryType">
          <el-radio-group v-model="form.categoryType">
            <el-radio-button label="text">文案</el-radio-button>
            <el-radio-button label="image">图片</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="例如：冠心病推广文案" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :precision="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ParttimeMaterialCategory" lang="ts">
import { Plus, Refresh, Search } from '@element-plus/icons-vue';
import {
  addMaterialCategory,
  deleteMaterialCategory,
  listMaterialCategories,
  updateMaterialCategory
} from '@/api/parttime/material/category';
import type { PtMaterialCategory, PtMaterialCategoryForm, PtMaterialCategoryQuery } from '@/api/parttime/material/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const categoryList = ref<PtMaterialCategory[]>([]);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const categoryFormRef = ref<ElFormInstance>();

const queryParams = reactive<PtMaterialCategoryQuery>({
  pageNum: 1,
  pageSize: 10,
  categoryType: '',
  categoryName: '',
  status: ''
});

const defaultForm = (): PtMaterialCategoryForm => ({
  categoryType: 'text',
  categoryName: '',
  sort: 0,
  status: '0',
  remark: ''
});

const form = reactive<PtMaterialCategoryForm>(defaultForm());

const rules: ElFormRules = {
  categoryType: [{ required: true, message: '请选择分类类型', trigger: 'change' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
};

const typeLabel = (type?: string) => (type === 'text' ? '文案' : type === 'image' ? '图片' : type || '--');
const statusLabel = (status?: string) => (status === '0' ? '启用' : '停用');
const formatTime = (value?: string) => (value ? proxy?.parseTime(value) || value : '--');

const getList = async () => {
  loading.value = true;
  try {
    const res = await listMaterialCategories(queryParams);
    categoryList.value = res.rows || [];
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
  categoryFormRef.value?.clearValidate();
};

const handleAdd = () => {
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row: PtMaterialCategory) => {
  resetForm();
  Object.assign(form, {
    categoryId: row.categoryId,
    categoryType: row.categoryType,
    categoryName: row.categoryName,
    sort: row.sort || 0,
    status: row.status || '0',
    remark: row.remark || ''
  });
  dialogVisible.value = true;
};

const submitForm = async () => {
  await categoryFormRef.value?.validate();
  submitting.value = true;
  try {
    if (form.categoryId) {
      await updateMaterialCategory(form);
      proxy?.$modal.msgSuccess('分类已更新');
    } else {
      await addMaterialCategory(form);
      proxy?.$modal.msgSuccess('分类已创建');
    }
    dialogVisible.value = false;
    await getList();
  } finally {
    submitting.value = false;
  }
};

const handleDelete = async (row: PtMaterialCategory) => {
  await proxy?.$modal.confirm(`确认删除分类「${row.categoryName}」？`);
  await deleteMaterialCategory(row.categoryId);
  proxy?.$modal.msgSuccess('已删除');
  await getList();
};

onMounted(getList);
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

.page-header p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
