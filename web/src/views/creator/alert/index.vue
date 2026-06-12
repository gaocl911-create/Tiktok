<template>
  <div class="monitor-page alert-page">
    <header class="page-heading">
      <div>
        <h1>预警中心</h1>
        <p>按自定义时间窗口监测点赞与评论增长，并跟踪管理员处理进度。</p>
      </div>
      <el-button
        v-if="activeTab === 'rules'"
        v-hasPermi="['creator:alert:rule:add']"
        type="primary"
        :icon="Plus"
        @click="openRuleDialog()"
      >
        新建规则
      </el-button>
      <el-button v-else :icon="Refresh" :loading="eventLoading" @click="loadEvents">刷新</el-button>
    </header>

    <section class="alert-summary">
      <button class="summary-item" type="button" @click="filterByStatus('pending')">
        <span>待处理</span><strong>{{ summary.pending }}</strong>
      </button>
      <button class="summary-item" type="button" @click="filterByStatus('tracking')">
        <span>跟踪中</span><strong>{{ summary.tracking }}</strong>
      </button>
      <button class="summary-item urgent" type="button" @click="filterBySeverity('urgent')">
        <span>紧急预警</span><strong>{{ summary.urgent }}</strong>
      </button>
      <div class="summary-item">
        <span>已启用规则</span><strong>{{ enabledRuleCount }}</strong>
      </div>
    </section>

    <section class="surface alert-workspace">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="预警事件" name="events">
          <div class="alert-filters">
            <el-select v-model="eventQuery.status" clearable placeholder="全部状态" @change="resetEventPage">
              <el-option label="待处理" value="pending" />
              <el-option label="跟踪中" value="tracking" />
              <el-option label="已处理" value="resolved" />
              <el-option label="已忽略" value="ignored" />
            </el-select>
            <el-select v-model="eventQuery.severity" clearable placeholder="全部等级" @change="resetEventPage">
              <el-option label="普通" value="normal" />
              <el-option label="重要" value="important" />
              <el-option label="紧急" value="urgent" />
            </el-select>
            <el-select v-model="eventQuery.metricType" clearable placeholder="全部指标" @change="resetEventPage">
              <el-option label="点赞" value="like" />
              <el-option label="评论" value="comment" />
            </el-select>
            <el-button :icon="Search" @click="resetEventPage">查询</el-button>
          </div>

          <el-table v-loading="eventLoading" :data="events">
            <el-table-column label="预警内容" min-width="320">
              <template #default="{ row }">
                <button class="event-title" type="button" @click="openContentDetail(row.contentId)">
                  <strong>{{ row.eventTitle }}</strong>
                  <span>{{ row.contentTitle || `作品 ${row.contentId}` }}</span>
                </button>
              </template>
            </el-table-column>
            <el-table-column label="指标" width="90">
              <template #default="{ row }">{{ metricLabel(row.metricType) }}</template>
            </el-table-column>
            <el-table-column label="触发条件" min-width="180">
              <template #default="{ row }">
                {{ conditionText(row) }}
              </template>
            </el-table-column>
            <el-table-column label="实际值" width="120" align="right">
              <template #default="{ row }"><strong class="observed">{{ formatNumber(row.observedValue) }}</strong></template>
            </el-table-column>
            <el-table-column label="等级" width="90">
              <template #default="{ row }"><span :class="['level', row.severity]">{{ severityLabel(row.severity) }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><span :class="['event-status', row.status]">{{ statusLabel(row.status) }}</span></template>
            </el-table-column>
            <el-table-column label="触发时间" width="170">
              <template #default="{ row }">{{ formatTime(row.lastTriggeredAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openContentDetail(row.contentId)">查看作品</el-button>
                <template v-if="row.status === 'pending' || row.status === 'tracking'">
                  <el-button
                    v-if="row.status === 'pending'"
                    v-hasPermi="['creator:alert:event:handle']"
                    link
                    type="primary"
                    @click="changeEventStatus(row, 'tracking')"
                  >
                    开始跟踪
                  </el-button>
                  <el-dropdown v-hasPermi="['creator:alert:event:handle']" @command="(command) => changeEventStatus(row, command)">
                    <el-button link type="primary">处理<el-icon><ArrowDown /></el-icon></el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="resolved">标记已处理</el-dropdown-item>
                        <el-dropdown-item command="ignored">忽略本次</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!eventLoading && events.length === 0" description="暂无符合条件的预警事件" />
          <pagination
            v-show="eventTotal > 0"
            v-model:page="eventQuery.pageNum"
            v-model:limit="eventQuery.pageSize"
            :total="eventTotal"
            @pagination="loadEvents"
          />
        </el-tab-pane>

        <el-tab-pane label="规则配置" name="rules">
          <div class="rules-toolbar">
            <p>采集产生新快照后自动判断。窗口小于采集间隔时，预警精度以实际采集频率为准。</p>
          </div>
          <el-table v-loading="ruleLoading" :data="rules">
            <el-table-column label="规则名称" prop="ruleName" min-width="180" />
            <el-table-column label="指标" width="90">
              <template #default="{ row }">{{ metricLabel(row.metricType) }}</template>
            </el-table-column>
            <el-table-column label="判断方式" min-width="180">
              <template #default="{ row }">{{ ruleConditionText(row) }}</template>
            </el-table-column>
            <el-table-column label="作用范围" min-width="150">
              <template #default="{ row }">{{ scopeLabel(row) }}</template>
            </el-table-column>
            <el-table-column label="等级" width="90">
              <template #default="{ row }"><span :class="['level', row.severity]">{{ severityLabel(row.severity) }}</span></template>
            </el-table-column>
            <el-table-column label="冷却时间" width="110">
              <template #default="{ row }">{{ row.cooldownMinutes }} 分钟</template>
            </el-table-column>
            <el-table-column label="启用" width="80">
              <template #default="{ row }">
                <el-switch
                  v-hasPermi="['creator:alert:rule:edit']"
                  v-model="row.enabled"
                  @change="toggleRule(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button v-hasPermi="['creator:alert:rule:edit']" link type="primary" @click="openRuleDialog(row)">编辑</el-button>
                <el-button v-hasPermi="['creator:alert:rule:remove']" link type="danger" @click="removeRule(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!ruleLoading && rules.length === 0" description="还没有预警规则，请先创建一条" />
          <pagination
            v-show="ruleTotal > 0"
            v-model:page="ruleQuery.pageNum"
            v-model:limit="ruleQuery.pageSize"
            :total="ruleTotal"
            @pagination="loadRules"
          />
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="ruleDialogVisible" :title="ruleForm.ruleId ? '编辑预警规则' : '新建预警规则'" width="620px">
      <el-form ref="ruleFormRef" :model="ruleForm" :rules="formRules" label-position="top">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="ruleForm.ruleName" maxlength="128" placeholder="例如：30分钟点赞增长超过500" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="监测指标" prop="metricType">
            <el-segmented v-model="ruleForm.metricType" :options="metricOptions" />
          </el-form-item>
          <el-form-item label="判断方式" prop="ruleType">
            <el-select v-model="ruleForm.ruleType">
              <el-option label="累计数量达到阈值" value="cumulative" />
              <el-option label="指定时间内增长达到阈值" value="window_growth" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="ruleForm.ruleType === 'window_growth'" label="统计时间段" prop="windowMinutes">
            <el-input-number v-model="ruleForm.windowMinutes" :min="1" :max="10080" controls-position="right" />
            <span class="field-unit">分钟</span>
          </el-form-item>
          <el-form-item label="触发数量" prop="thresholdValue">
            <el-input-number v-model="ruleForm.thresholdValue" :min="1" :max="999999999999" controls-position="right" />
          </el-form-item>
          <el-form-item label="作用范围" prop="scopeType">
            <el-select v-model="ruleForm.scopeType" @change="ruleForm.scopeId = undefined">
              <el-option label="全部作品" value="all" />
              <el-option label="指定作者" value="creator" />
              <el-option label="指定作品" value="content" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="ruleForm.scopeType === 'creator'" label="选择作者" prop="scopeId">
            <el-select v-model="ruleForm.scopeId" filterable placeholder="请选择作者">
              <el-option v-for="item in creators" :key="item.creatorId" :label="item.nickname" :value="item.creatorId" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="ruleForm.scopeType === 'content'" label="选择作品" prop="scopeId">
            <el-select v-model="ruleForm.scopeId" filterable placeholder="请选择作品">
              <el-option v-for="item in contents" :key="item.contentId" :label="item.title || item.platformContentId" :value="item.contentId" />
            </el-select>
          </el-form-item>
          <el-form-item label="预警等级" prop="severity">
            <el-select v-model="ruleForm.severity">
              <el-option label="普通" value="normal" />
              <el-option label="重要" value="important" />
              <el-option label="紧急" value="urgent" />
            </el-select>
          </el-form-item>
          <el-form-item label="重复提醒冷却时间" prop="cooldownMinutes">
            <el-input-number v-model="ruleForm.cooldownMinutes" :min="0" :max="10080" controls-position="right" />
            <span class="field-unit">分钟</span>
          </el-form-item>
        </div>
        <el-form-item>
          <el-checkbox v-model="ruleForm.enabled">创建后立即启用</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRule" @click="submitRule">保存规则</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown, Plus, Refresh, Search } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import {
  addAlertRule,
  deleteAlertRule,
  handleAlertEvent,
  listAlertEvents,
  listAlertRules,
  listContentPosts,
  listCreatorAccounts,
  updateAlertRule
} from '@/api/creator';
import type { AlertEvent, AlertRule, AlertRuleForm, ContentPost, CreatorAccount } from '@/api/creator/types';

const router = useRouter();
const activeTab = ref('events');
const eventLoading = ref(false);
const ruleLoading = ref(false);
const savingRule = ref(false);
const events = ref<AlertEvent[]>([]);
const rules = ref<AlertRule[]>([]);
const creators = ref<CreatorAccount[]>([]);
const contents = ref<ContentPost[]>([]);
const eventTotal = ref(0);
const ruleTotal = ref(0);
const ruleDialogVisible = ref(false);
const ruleFormRef = ref<FormInstance>();
const eventQuery = reactive({ pageNum: 1, pageSize: 10, status: '', severity: '', metricType: '' });
const ruleQuery = reactive({ pageNum: 1, pageSize: 10 });
const summary = reactive({ pending: 0, tracking: 0, urgent: 0 });

const emptyRule = (): AlertRuleForm => ({
  ruleName: '',
  metricType: 'like',
  ruleType: 'window_growth',
  windowMinutes: 30,
  thresholdValue: 100,
  scopeType: 'all',
  severity: 'important',
  cooldownMinutes: 120,
  enabled: true
});
const ruleForm = reactive<AlertRuleForm>(emptyRule());
const metricOptions = [
  { label: '点赞', value: 'like' },
  { label: '评论', value: 'comment' }
];
const formRules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  metricType: [{ required: true, message: '请选择指标', trigger: 'change' }],
  ruleType: [{ required: true, message: '请选择判断方式', trigger: 'change' }],
  windowMinutes: [{
    validator: (_rule, value, callback) => {
      if (ruleForm.ruleType === 'window_growth' && (!value || value < 1)) callback(new Error('请输入统计时间段'));
      else callback();
    },
    trigger: 'change'
  }],
  thresholdValue: [{ required: true, message: '请输入触发数量', trigger: 'change' }],
  scopeId: [{
    validator: (_rule, value, callback) => {
      if (ruleForm.scopeType !== 'all' && !value) callback(new Error('请选择作用对象'));
      else callback();
    },
    trigger: 'change'
  }]
};

const enabledRuleCount = computed(() => rules.value.filter((item) => item.enabled).length);
const formatNumber = (value?: number) => Number(value || 0).toLocaleString('zh-CN');
const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '--';
const metricLabel = (value: string) => value === 'comment' ? '评论' : '点赞';
const severityLabel = (value: string) => ({ normal: '普通', important: '重要', urgent: '紧急' }[value] || value);
const statusLabel = (value: string) => ({ pending: '待处理', tracking: '跟踪中', resolved: '已处理', ignored: '已忽略' }[value] || value);
const conditionText = (row: AlertEvent) => row.ruleType === 'window_growth'
  ? `${row.windowMinutes} 分钟增长 ≥ ${formatNumber(row.thresholdValue)}`
  : `累计数量 ≥ ${formatNumber(row.thresholdValue)}`;
const ruleConditionText = (row: AlertRule) => `${metricLabel(row.metricType)}${row.ruleType === 'window_growth'
  ? `在 ${row.windowMinutes} 分钟内增长 ≥ ${formatNumber(row.thresholdValue)}`
  : `累计达到 ${formatNumber(row.thresholdValue)}`}`;
const scopeLabel = (row: AlertRule) => row.scopeType === 'all'
  ? '全部作品'
  : row.scopeType === 'creator'
    ? `指定作者：${creators.value.find((item) => item.creatorId === row.scopeId)?.nickname || row.scopeId}`
    : `指定作品：${contents.value.find((item) => item.contentId === row.scopeId)?.title || row.scopeId}`;

const loadEvents = async () => {
  eventLoading.value = true;
  try {
    const res = await listAlertEvents(eventQuery);
    events.value = res.rows || [];
    eventTotal.value = res.total || 0;
    const summaryRes = await listAlertEvents({ pageNum: 1, pageSize: 200 });
    const all = summaryRes.rows || [];
    summary.pending = all.filter((item) => item.status === 'pending').length;
    summary.tracking = all.filter((item) => item.status === 'tracking').length;
    summary.urgent = all.filter((item) => item.severity === 'urgent' && ['pending', 'tracking'].includes(item.status)).length;
  } finally {
    eventLoading.value = false;
  }
};

const loadRules = async () => {
  ruleLoading.value = true;
  try {
    const res = await listAlertRules(ruleQuery);
    rules.value = res.rows || [];
    ruleTotal.value = res.total || 0;
  } finally {
    ruleLoading.value = false;
  }
};

const loadOptions = async () => {
  const [creatorRes, contentRes] = await Promise.all([
    listCreatorAccounts({ pageNum: 1, pageSize: 200, platform: 'douyin' }),
    listContentPosts({ pageNum: 1, pageSize: 200, platform: 'douyin' })
  ]);
  creators.value = creatorRes.rows || [];
  contents.value = contentRes.rows || [];
};

const resetEventPage = () => {
  eventQuery.pageNum = 1;
  loadEvents();
};
const filterByStatus = (status: string) => {
  activeTab.value = 'events';
  eventQuery.status = status;
  resetEventPage();
};
const filterBySeverity = (severity: string) => {
  activeTab.value = 'events';
  eventQuery.severity = severity;
  resetEventPage();
};
const handleTabChange = (name: string | number) => {
  if (name === 'rules') loadRules();
  else loadEvents();
};
const openContentDetail = (contentId: string) => router.push(`/douyin/content/detail/${contentId}`);

const openRuleDialog = (row?: AlertRule) => {
  Object.assign(ruleForm, row ? { ...row } : emptyRule());
  ruleDialogVisible.value = true;
  nextTick(() => ruleFormRef.value?.clearValidate());
};

const submitRule = async () => {
  await ruleFormRef.value?.validate();
  savingRule.value = true;
  try {
    const payload = { ...ruleForm };
    if (payload.ruleType === 'cumulative') payload.windowMinutes = undefined;
    if (payload.scopeType === 'all') payload.scopeId = undefined;
    if (payload.ruleId) await updateAlertRule(payload);
    else await addAlertRule(payload);
    ElMessage.success('预警规则已保存');
    ruleDialogVisible.value = false;
    await loadRules();
  } finally {
    savingRule.value = false;
  }
};

const toggleRule = async (row: AlertRule) => {
  await updateAlertRule({ ...row });
  ElMessage.success(row.enabled ? '规则已启用' : '规则已停用');
};

const removeRule = async (row: AlertRule) => {
  await ElMessageBox.confirm(`确认删除规则“${row.ruleName}”吗？`, '删除规则', { type: 'warning' });
  await deleteAlertRule(row.ruleId);
  ElMessage.success('规则已删除');
  await loadRules();
};

const changeEventStatus = async (row: AlertEvent, status: AlertEvent['status']) => {
  let handleNote = '';
  if (status === 'resolved' || status === 'ignored') {
    const result = await ElMessageBox.prompt('可以填写本次处理结果或备注', status === 'resolved' ? '标记已处理' : '忽略预警', {
      inputPlaceholder: '处理备注（选填）',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    });
    handleNote = result.value;
  }
  await handleAlertEvent(row.eventId, { status, handleNote });
  ElMessage.success(status === 'tracking' ? '已进入跟踪状态' : '预警状态已更新');
  await loadEvents();
};

onMounted(async () => {
  await loadOptions();
  await Promise.all([loadEvents(), loadRules()]);
});
</script>

<style scoped>
@import '../monitor-page.css';

.alert-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 14px;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  background: #fff;
}

.summary-item {
  min-height: 92px;
  padding: 18px 20px;
  text-align: left;
  border: 0;
  border-right: 1px solid #e5e5e5;
  background: transparent;
}

button.summary-item {
  cursor: pointer;
}

.summary-item:last-child {
  border-right: 0;
}

.summary-item span,
.summary-item strong {
  display: block;
}

.summary-item span {
  color: #737373;
  font-size: 13px;
}

.summary-item strong {
  margin-top: 8px;
  font-size: 26px;
  font-variant-numeric: tabular-nums;
}

.summary-item.urgent strong {
  color: #dc2626;
}

.alert-workspace {
  padding: 0 16px 16px;
}

.alert-filters {
  display: grid;
  grid-template-columns: 160px 160px 160px auto;
  gap: 10px;
  margin-bottom: 14px;
}

.rules-toolbar {
  margin-bottom: 12px;
  color: #737373;
  font-size: 13px;
}

.rules-toolbar p {
  margin: 0;
}

.event-title {
  display: block;
  width: 100%;
  padding: 0;
  text-align: left;
  border: 0;
  cursor: pointer;
  background: transparent;
}

.event-title strong,
.event-title span {
  display: block;
}

.event-title strong {
  color: #171717;
}

.event-title span {
  margin-top: 4px;
  overflow: hidden;
  color: #737373;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.observed {
  font-variant-numeric: tabular-nums;
}

.level,
.event-status {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border: 1px solid #d4d4d4;
  border-radius: 4px;
  font-size: 12px;
}

.level.important,
.event-status.tracking {
  color: #a16207;
  border-color: #fde68a;
  background: #fffbeb;
}

.level.urgent {
  color: #b91c1c;
  border-color: #fecaca;
  background: #fef2f2;
}

.event-status.pending {
  color: #b91c1c;
  border-color: #fecaca;
  background: #fef2f2;
}

.event-status.resolved {
  color: #047857;
  border-color: #a7f3d0;
  background: #ecfdf5;
}

.event-status.ignored {
  color: #525252;
  background: #f5f5f5;
}

.field-unit {
  margin-left: 8px;
  color: #737373;
}

:deep(.el-input-number),
:deep(.el-select),
:deep(.el-segmented) {
  width: 100%;
}

@media (max-width: 900px) {
  .alert-summary {
    grid-template-columns: 1fr 1fr;
  }

  .summary-item:nth-child(2) {
    border-right: 0;
  }

  .summary-item:nth-child(-n + 2) {
    border-bottom: 1px solid #e5e5e5;
  }

  .alert-filters {
    grid-template-columns: 1fr;
  }
}
</style>
