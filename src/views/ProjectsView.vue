<script setup>
import {computed, onMounted, reactive, ref} from "vue";
import {useRouter} from "vue-router";
import {projectApi, teacherApi, aiApi, projectTypeApi} from "../api/modules";
import PaginationBar from "../components/PaginationBar.vue";
import {useAuthStore} from "../stores/auth";
import {usePagination} from "../composables";
import {ElMessage, ElMessageBox, ElNotification} from "element-plus";
import {Search, Plus, Refresh, Edit, Delete, Promotion, MagicStick, EditPen, Key, Download, Upload} from '@element-plus/icons-vue';
import {PROJECT_STATUS_OPTIONS, getLabel, getStatusTone} from "../utils/constants";

const router = useRouter();
const authStore = useAuthStore();

// 使用通用分页 Hook
const {query, page, loading, loadData, changePage, changeSize, resetQuery} = usePagination(
    (params) => {
      // 学生只能看自己的课题
      if (authStore.role === 0) {
        params.sid = authStore.userId;
      }
      return projectApi.list(params);
    },
    {title: '', status: ''}
);

// -------- 教师列表 --------
const teacherOptions = ref([]);
const teacherLoading = ref(false);

async function loadTeacherOptions() {
  teacherLoading.value = true;
  try {
    const res = await teacherApi.options();
    teacherOptions.value = res.data ?? [];
  } catch {
    teacherOptions.value = [];
  } finally {
    teacherLoading.value = false;
  }
}

// -------- 课题类型列表 --------
const typeOptions = ref([]);

async function loadTypeOptions() {
  try {
    const res = await projectTypeApi.enabled();
    typeOptions.value = res.data ?? [];
  } catch {
    typeOptions.value = [];
  }
}

// 根据 tid 获取教师选项对象
const getTeacherOption = (tid) => teacherOptions.value.find(t => t.tid === tid);

// 表单相关状态
const form = reactive({
  id: null,
  title: "",
  typeId: null,
  description: "",
  sid: null,
  tid: null,
  status: 0,
  opinion: ""
});
const modal = reactive({edit: false});
const submitting = ref(false);

// -------- AI 辅助写作 --------
const aiLoading = reactive({generate: false, polish: false, expand: false});
const aiKeywords = ref('');
const showKeywordsInput = ref(false);

async function aiGenerate() {
  if (!form.title) {
    return ElMessage.warning("请先填写课题标题，AI 将根据标题生成描述");
  }
  aiLoading.generate = true;
  try {
    const typeName = typeOptions.value.find(t => t.id === form.typeId)?.name || '';
    const res = await aiApi.generateDescription(form.title, typeName);
    form.description = res.data || res;
    ElMessage.success("AI 已生成课题描述");
  } catch {
    ElMessage.error("AI 服务暂时不可用");
  } finally {
    aiLoading.generate = false;
  }
}

async function aiPolish() {
  if (!form.description || !form.description.trim()) {
    return ElMessage.warning("请先填写课题描述，再使用润色功能");
  }
  aiLoading.polish = true;
  try {
    const res = await aiApi.polish(form.description);
    form.description = res.data || res;
    ElMessage.success("AI 润色完成");
  } catch {
    ElMessage.error("AI 服务暂时不可用");
  } finally {
    aiLoading.polish = false;
  }
}

async function aiExpand() {
  if (!aiKeywords.value.trim()) {
    return ElMessage.warning("请输入研究关键词（如：机器学习,图像识别）");
  }
  aiLoading.expand = true;
  try {
    const res = await aiApi.expand(aiKeywords.value);
    form.description = res.data || res;
    showKeywordsInput.value = false;
    aiKeywords.value = '';
    ElMessage.success("AI 已根据关键词生成描述");
  } catch {
    ElMessage.error("AI 服务暂时不可用");
  } finally {
    aiLoading.expand = false;
  }
}

// 权限计算
const isStudent = computed(() => authStore.role === 0);
const isAuditRole = computed(() => [1, 2].includes(authStore.role));

// 课题是否允许学生编辑
const canStudentEdit = (item) => {
  if (!item.id) return true;
  return [0, 2, 3].includes(item.status);
};

// 待办统计
const pendingCount = computed(() => page.value.list.filter(i => i.status === 0).length);

// 创建空表单
function createEmptyForm() {
  return {
    id: null,
    title: "",
    typeId: null,
    description: "",
    sid: authStore.userId,
    tid: null,
    status: 0,
    opinion: ""
  };
}

// 打开编辑弹窗
function openEdit(item = null) {
  if (item) {
    Object.assign(form, JSON.parse(JSON.stringify(item)));
  } else {
    Object.assign(form, createEmptyForm());
  }
  modal.edit = true;
}

// 提交表单
async function submitForm() {
  if (!form.title || !form.typeId) {
    return ElMessage.error("请完善课题基本信息（标题和类型必填）");
  }

  submitting.value = true;
  try {
    // 审核角色：只允许修改审核结果和意见，不修改基本信息
    if (isAuditRole.value && form.id) {
      await projectApi.update({
        id: form.id,
        status: form.status,
        opinion: form.opinion
      });
      ElMessage.success("审核意见已保存");
      modal.edit = false;
      await loadData();
      return;
    }

    const isEdit = !!form.id;
    const api = isEdit ? projectApi.update : projectApi.create;

    const payload = {...form};
    if (isStudent.value) {
      payload.sid = authStore.userId;
      if (payload.status !== 1) payload.status = 0;
    }

    await api(payload);
    ElMessage.success(isEdit ? "更新成功" : "申报已提交，请等待审核");
    modal.edit = false;
    await loadData();
  } finally {
    submitting.value = false;
  }
}

// 删除课题
const handleRemove = (id) => {
  ElMessageBox.confirm('确认撤回并删除该课题申请吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消'
  }).then(async () => {
    await projectApi.remove([id]);
    ElMessage.success("删除成功");
    await loadData();
  });
};

// ==================== Excel 导入导出（仅管理/教师角色显示）====================
const excelLoading = ref(false);
const importProjectFileRef = ref(null);

/** 导出课题Excel */
async function handleExportProject() {
  excelLoading.value = true;
  try {
    // 传入当前筛选参数
    const params = {...query};
    if (authStore.role === 0) params.sid = authStore.userId;
    await projectApi.exportExcel(params);
    ElMessage.success("导出成功");
  } catch {
    ElMessage.error("导出失败");
  } finally {
    excelLoading.value = false;
  }
}

/** 触发文件选择框 */
function triggerImportProject() {
  importProjectFileRef.value?.click();
}

/** 处理课题Excel导入 */
async function handleImportProjectFile(e) {
  const file = e.target.files?.[0];
  if (!file) return;
  e.target.value = "";
  excelLoading.value = true;
  try {
    const res = await projectApi.importExcel(file);
    ElNotification({
      title: "导入结果",
      message: res.msg || "导入完成",
      type: res.code === 200 ? "success" : "warning",
      duration: 5000
    });
    await loadData();
  } catch {
    ElMessage.error("导入失败，请检查文件格式");
  } finally {
    excelLoading.value = false;
  }
}

// 初始化
onMounted(() => {
  loadData();
  loadTeacherOptions();
  loadTypeOptions();
});
</script>

<template>
  <div class="app-container">
    <el-row :gutter="20" v-if="isAuditRole" class="dashboard">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card warning">
          <template #header>
            <div class="card-header"><span>待审核课题</span>
              <el-icon>
                <Promotion/>
              </el-icon>
            </div>
          </template>
          <div class="num">{{ pendingCount }}</div>
          <div class="tip">请尽快处理学生提交的申报</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="课题标题">
          <el-input v-model="query.title" placeholder="关键字" clearable/>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option v-for="opt in PROJECT_STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button v-if="isStudent" type="success" :icon="Plus" @click="openEdit()">申报新课题</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <!-- 导入导出工具栏（仅管理/教师可见） -->
      <div v-if="isAuditRole" class="table-toolbar">
        <div class="toolbar-left">
          <el-button type="warning" :icon="Upload" :loading="excelLoading" @click="triggerImportProject">批量导入</el-button>
          <el-button type="primary" :icon="Download" :loading="excelLoading" @click="handleExportProject">导出 Excel</el-button>
        </div>
        <span class="table-tips">当前共 {{ page.total }} 条课题记录</span>
        <!-- 隐藏文件选择框 -->
        <input ref="importProjectFileRef" type="file" accept=".xlsx,.xls" style="display:none" @change="handleImportProjectFile"/>
      </div>

      <el-table :data="page.list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="编号" width="80" align="center"/>
        <el-table-column prop="title" label="课题名称" min-width="200" show-overflow-tooltip/>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            {{ typeOptions.find(t => t.id === row.typeId)?.name || '—' }}
          </template>
        </el-table-column>
        <el-table-column v-if="isAuditRole" label="申报学生" width="120">
          <template #default="{ row }">
            <span>{{ row.studentName || `学生${row.sid}` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="指导教师" width="140">
          <template #default="{ row }">
            <template v-if="row.tid">
              <span>{{ getTeacherOption(row.tid)?.nickname ?? `ID:${row.tid}` }}</span>
            </template>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTone('project', row.status)">
              {{ getLabel(PROJECT_STATUS_OPTIONS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="opinion" label="审核意见" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.status === 2 }">{{ row.opinion || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">
              {{ isAuditRole && row.status === 0 ? '审核' : '详情' }}
            </el-button>
<!--            <el-button link type="info" @click="router.push({path: '/project-comments', query: {projectId: row.id}})">-->
<!--              评论-->
<!--            </el-button>-->
            <!-- 学生：已通过课题不可删除；审核角色：可删除 -->
            <el-button
                v-if="isAuditRole || (isStudent && row.status !== 1)"
                link type="danger"
                @click="handleRemove(row.id)"
            >删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-footer">
        <PaginationBar
            :page-no="query.pageNo"
            :page-size="query.pageSize"
            :total="page.total"
            @change="changePage"
            @size-change="changeSize"
        />
      </div>
    </el-card>

    <el-dialog
        v-model="modal.edit"
        :title="isStudent ? (form.id ? '课题申报详情' : '填写申报信息') : '课题审核'"
        width="650px"
    >
      <el-form label-position="left" label-width="100px" :disabled="submitting">
        <el-divider content-position="left">课题基本信息</el-divider>
        <el-form-item label="课题标题" required>
          <el-input v-model="form.title"
                    :disabled="(isStudent && !canStudentEdit(form)) || (isAuditRole && !!form.id)"
                    placeholder="请输入课题全称"/>
        </el-form-item>
        <el-form-item label="课题类型" required>
          <el-select
              v-model="form.typeId"
              :disabled="(isStudent && !canStudentEdit(form)) || (isAuditRole && !!form.id)"
              placeholder="请选择课题类型"
              style="width:100%"
          >
            <el-option
                v-for="t in typeOptions"
                :key="t.id"
                :label="t.name"
                :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="课题描述">
          <div style="width:100%">
            <el-input
                type="textarea"
                v-model="form.description"
                :rows="4"
                :disabled="(isStudent && !canStudentEdit(form)) || (isAuditRole && !!form.id)"
                placeholder="请详细说明课题背景、目标及实施方案..."
            />

            <div v-if="!isAuditRole && (!isStudent || canStudentEdit(form))" class="ai-toolbar">
              <span class="ai-label">✨ AI 辅助：</span>
              <el-button
                  size="small" type="primary" plain
                  :icon="MagicStick"
                  :loading="aiLoading.generate"
                  @click="aiGenerate"
              >生成描述</el-button>
              <el-button
                  size="small" type="success" plain
                  :icon="EditPen"
                  :loading="aiLoading.polish"
                  :disabled="!form.description"
                  @click="aiPolish"
              >润色优化</el-button>
              <el-button
                  size="small" type="warning" plain
                  :icon="Key"
                  @click="showKeywordsInput = !showKeywordsInput"
              >关键词扩展</el-button>
            </div>
            <!-- 关键词输入框（展开后显示） -->
            <div v-if="showKeywordsInput && !isAuditRole && (!isStudent || canStudentEdit(form))" class="ai-keywords-box">
              <el-input
                  v-model="aiKeywords"
                  placeholder="输入研究关键词，用逗号分隔，如：深度学习,图像识别,ResNet"
                  clearable
                  @keyup.enter="aiExpand"
              >
                <template #append>
                  <el-button :loading="aiLoading.expand" @click="aiExpand">生成</el-button>
                </template>
              </el-input>
            </div>
          </div>
        </el-form-item>

        <!-- 指导教师选择（学生填写 / 所有角色可查看） -->
        <el-form-item label="指导教师">
          <el-select
              v-model="form.tid"
              placeholder="请选择指导教师（可不填）"
              clearable
              filterable
              :disabled="(isStudent && !canStudentEdit(form)) || (isAuditRole && !!form.id) || teacherLoading"
              style="width: 100%"
          >
            <el-option
                v-for="t in teacherOptions"
                :key="t.tid"
                :label="`${t.nickname}${t.title ? '（' + t.title + '）' : ''}${t.researchField ? ' · ' + t.researchField : ''}`"
                :value="t.tid"
                :disabled="t.full"
            >
              <div class="teacher-option">
                <span class="teacher-name">
                  {{ t.nickname }}<el-tag v-if="t.title" size="small" type="info" style="margin-left:4px">{{ t.title }}</el-tag>
                </span>
                <span class="teacher-meta">
                  <span v-if="t.researchField">{{ t.researchField }}</span>
                  <el-tag
                      v-if="t.maxStudentCount && t.maxStudentCount > 0"
                      size="small"
                      :type="t.full ? 'danger' : 'success'"
                      style="margin-left:6px"
                  >
                    {{ t.full ? '已满额' : `剩余 ${t.maxStudentCount - (t.currentStudentCount ?? 0)} 名` }}
                  </el-tag>
                  <el-tag v-else size="small" type="success" style="margin-left:6px">名额不限</el-tag>
                </span>
              </div>
            </el-option>
          </el-select>
          <!-- 已选教师的名额提示 -->
          <div v-if="form.tid && getTeacherOption(form.tid)" class="advisor-tip">
            <el-icon color="#67c23a"><i class="el-icon-check"/></el-icon>
            已选：{{ getTeacherOption(form.tid).nickname }}
            <template v-if="getTeacherOption(form.tid).maxStudentCount > 0">
              · 剩余名额
              <b>{{ getTeacherOption(form.tid).maxStudentCount - (getTeacherOption(form.tid).currentStudentCount ?? 0) }}</b>
              /{{ getTeacherOption(form.tid).maxStudentCount }}
            </template>
            <template v-else>· 名额不限</template>
          </div>
        </el-form-item>

        <template v-if="isAuditRole || (isStudent && form.opinion)">
          <el-divider content-position="left">审核环节</el-divider>
          <el-form-item label="审核结果" v-if="isAuditRole">
            <el-radio-group v-model="form.status">
              <el-radio v-for="opt in PROJECT_STATUS_OPTIONS" :key="opt.value" :label="opt.value">{{
                  opt.label
                }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审核意见">
            <el-input
                type="textarea"
                v-model="form.opinion"
                :rows="3"
                :disabled="isStudent"
                :placeholder="isAuditRole ? '若驳回，请填写具体修改建议' : ''"
            />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="modal.edit = false">取消</el-button>
        <el-button
            v-if="!isStudent || canStudentEdit(form)"
            type="primary"
            :loading="submitting"
            @click="submitForm"
        >
          {{ isAuditRole ? '保存审核意见' : (form.id ? '保存修改' : '确认申报') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.app-container {
  padding: 20px;
  background: transparent;
  min-height: 100vh;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  gap: 8px;
  align-items: center;
}

.table-tips {
  font-size: 13px;
  color: #909399;
}

.dashboard {
  margin-bottom: 20px;
}

.stat-card {
  border: none;
  border-radius: var(--r-card);
}

.stat-card.warning {
  background: linear-gradient(135deg, rgba(255, 224, 102, 0.15) 0%, rgba(255, 243, 191, 0.1) 100%);
  border: 1px solid rgba(255, 224, 102, 0.3);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.num {
  font-size: 32px;
  font-weight: bold;
  color: #f08c00;
  margin: 10px 0;
}

.tip {
  font-size: 12px;
  color: #868e96;
}

.search-card {
  margin-bottom: 16px;
  border: none;
}

.pagination-footer {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.text-danger {
  color: #fa5252;
  font-weight: bold;
}

.text-muted {
  color: #adb5bd;
}

/* 教师下拉选项样式 */
.teacher-option {
  display: flex;
  flex-direction: column;
  padding: 4px 0;
  line-height: 1.4;
}

.teacher-name {
  font-weight: 500;
  color: #303133;
  display: flex;
  align-items: center;
}

.teacher-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  margin-top: 2px;
}

/* 已选教师提示 */
.advisor-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 4px;
}

.advisor-tip b {
  color: #67c23a;
  font-size: 14px;
}

:deep(.el-divider__text) {
  background: var(--panel);
  font-weight: bold;
  color: #4dabf7;
}

/* AI 辅助写作工具栏 */
.ai-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.ai-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.ai-keywords-box {
  margin-top: 8px;
}
</style>
