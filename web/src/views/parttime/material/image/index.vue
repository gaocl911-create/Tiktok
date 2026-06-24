<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <template #header>
        <div class="page-header">
          <div>
            <h2>图片库</h2>
            <p>维护任务领取时自动分配给兼职人员保存使用的推广图片。</p>
          </div>
          <div class="header-actions">
            <el-button :icon="Refresh" :loading="loading" @click="getList">刷新</el-button>
            <el-button v-hasPermi="['parttime:material:image:add']" type="primary" :icon="Plus" @click="handleAdd">新增图片</el-button>
          </div>
        </div>
      </template>

      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="queryParams.categoryId" placeholder="全部分类" clearable filterable style="width: 220px">
            <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片名称" prop="imageName">
          <el-input v-model="queryParams.imageName" placeholder="搜索图片名称" clearable style="width: 220px" @keyup.enter="handleQuery" />
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
      <el-table v-loading="loading" :data="imageList" border>
        <el-table-column label="图片" width="120" align="center">
          <template #default="{ row }">
            <el-image class="thumb" :src="row.imageUrl" :preview-src-list="[row.imageUrl]" preview-teleported fit="cover" />
          </template>
        </el-table-column>
        <el-table-column label="分类" prop="categoryName" width="180" show-overflow-tooltip />
        <el-table-column label="图片名称" prop="imageName" min-width="220" show-overflow-tooltip />
        <el-table-column label="图片地址" prop="imageUrl" min-width="280" show-overflow-tooltip />
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
            <el-button v-hasPermi="['parttime:material:image:edit']" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-hasPermi="['parttime:material:image:remove']" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.imageId ? '编辑图片' : '新增图片'" width="720px" append-to-body>
      <el-form ref="imageFormRef" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="图片分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择图片分类" filterable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="上传图片">
          <imageUpload v-model="imageOssId" :limit="1" :file-size="10" :is-show-tip="true" />
          <span class="form-tip">上传成功后会自动回填图片地址</span>
        </el-form-item>
        <el-form-item label="图片地址" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="上传图片后自动回填，也可以直接粘贴图片地址" />
        </el-form-item>
        <el-form-item v-if="form.imageUrl" label="当前预览">
          <el-image class="preview" :src="form.imageUrl" :preview-src-list="[form.imageUrl]" preview-teleported fit="cover" />
        </el-form-item>
        <el-form-item label="图片名称">
          <el-input v-model="form.imageName" placeholder="例如：冠心病推广图-01" maxlength="255" show-word-limit />
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

<script setup name="ParttimeMaterialImage" lang="ts">
import { Plus, Refresh, Search } from '@element-plus/icons-vue';
import { listMaterialCategoryOptions } from '@/api/parttime/material/category';
import { addMaterialImage, deleteMaterialImage, listMaterialImages, updateMaterialImage } from '@/api/parttime/material/image';
import { listByIds } from '@/api/system/oss';
import type { PtMaterialCategory, PtMaterialImage, PtMaterialImageForm, PtMaterialImageQuery } from '@/api/parttime/material/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const imageList = ref<PtMaterialImage[]>([]);
const categoryOptions = ref<PtMaterialCategory[]>([]);
const total = ref(0);
const imageOssId = ref('');
const queryFormRef = ref<ElFormInstance>();
const imageFormRef = ref<ElFormInstance>();

const queryParams = reactive<PtMaterialImageQuery>({
  pageNum: 1,
  pageSize: 10,
  categoryId: undefined,
  imageName: '',
  status: ''
});

const defaultForm = (): PtMaterialImageForm => ({
  categoryId: undefined,
  imageUrl: '',
  imageName: '',
  imageSize: undefined,
  sort: 0,
  status: '0',
  remark: ''
});

const form = reactive<PtMaterialImageForm>(defaultForm());

const rules: ElFormRules = {
  categoryId: [{ required: true, message: '请选择图片分类', trigger: 'change' }],
  imageUrl: [{ required: true, message: '请上传或填写图片地址', trigger: 'blur' }]
};

watch(imageOssId, async (ossId) => {
  if (!ossId) return;
  const res = await listByIds(ossId);
  const item = res.data?.[0];
  if (!item) return;
  form.imageUrl = item.url;
  form.imageName = form.imageName || item.originalName || item.fileName;
});

const statusLabel = (status?: string) => (status === '0' ? '启用' : '停用');
const formatTime = (value?: string) => (value ? proxy?.parseTime(value) || value : '--');

const loadOptions = async () => {
  const res = await listMaterialCategoryOptions('image');
  categoryOptions.value = res.data || [];
};

const getList = async () => {
  loading.value = true;
  try {
    const res = await listMaterialImages(queryParams);
    imageList.value = res.rows || [];
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
  imageOssId.value = '';
  imageFormRef.value?.clearValidate();
};

const handleAdd = () => {
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row: PtMaterialImage) => {
  resetForm();
  Object.assign(form, {
    imageId: row.imageId,
    categoryId: row.categoryId,
    imageUrl: row.imageUrl || '',
    imageName: row.imageName || '',
    imageSize: row.imageSize,
    sort: row.sort || 0,
    status: row.status || '0',
    remark: row.remark || ''
  });
  dialogVisible.value = true;
};

const submitForm = async () => {
  await imageFormRef.value?.validate();
  submitting.value = true;
  try {
    if (form.imageId) {
      await updateMaterialImage(form);
      proxy?.$modal.msgSuccess('图片已更新');
    } else {
      await addMaterialImage(form);
      proxy?.$modal.msgSuccess('图片已创建');
    }
    dialogVisible.value = false;
    await getList();
  } finally {
    submitting.value = false;
  }
};

const handleDelete = async (row: PtMaterialImage) => {
  await proxy?.$modal.confirm('确认删除该图片？已分配历史会继续保留快照。');
  await deleteMaterialImage(row.imageId);
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

.thumb {
  width: 72px;
  height: 72px;
  border-radius: 8px;
}

.preview {
  width: 180px;
  height: 180px;
  border-radius: 10px;
}

.form-tip {
  display: block;
  margin-top: 8px;
}
</style>
