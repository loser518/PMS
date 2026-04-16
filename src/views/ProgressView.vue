<script setup>
import {computed, onMounted, reactive} from "vue";
import {progressApi, projectApi} from "../api/modules";
import PaginationBar from "../components/PaginationBar.vue";
import {useAuthStore} from "../stores/auth";
import {ElMessage, ElMessageBox} from 'element-plus';
import {Bell, Document, Plus, Refresh, UploadFilled} from '@element-plus/icons-vue';
import {PROGRESS_STATUS_OPTIONS, getLabel, getStatusTone} from "../utils/constants";

// ---- 常量 ----
/** 文件上传最大限制 10MB */
const MAX_FILE_SIZE = 10 * 1024 * 1024;
/** 文件大小显示单位 */
const FILE_SIZE_UNITS = ['B', 'KB', 'MB', 'GB'];

const authStore = useAuthStore();

// ---- 查询条件（后端真分页） ----
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  pId: "",
  title: "",
  status: "",
  sortBy: "id",
  isAsc: false
});

// ---- 页面状态 ----
const state = reactive({
  projectOptions: [],
  progressRows: [],
  total: 0,
  form: {id: null, pId: null, title: "", content: "", fileUrl: "", fileName: "", fileSize: 0, status: 0, opinion: ""},
  dialogVisible: false,
  loading: false,
  uploadLoading: false
});

/** 权限与状态计算 */
const isStudent = computed(() => authStore.role === 0);
const isTeacher = computed(() => authStore.role === 1);
const isAdmin = computed(() => authStore.role === 2);
const canAudit = computed(() => isTeacher.value || isAdmin.value);
const projectMetaMap = computed(() => Object.fromEntries(state.projectOptions.map((item) => [item.id, item])));
const isStudentEditingOwnDraft = computed(() => isStudent.value && (!state.form.id || state.form.status === 0 || state.form.status === 2));
const isAuditMode = computed(() => canAudit.value && !!state.form.id);

// 带元数据的当前行数据
const enrichedRows = computed(() => state.progressRows.map((item) => ({
  ...item,
  studentId: projectMetaMap.value[item.pId]?.sid || null,
  projectTitle: projectMetaMap.value[item.pId]?.title || ""
})));

const pendingProgress = computed(() => enrichedRows.value.filter(item => item.status === 0));

/**
 * 格式化文件大小为可读字符串
 */
function formatFileSize(bytes) {
  if (!bytes) return '';
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${parseFloat((bytes / Math.pow(1024, i)).toFixed(2))} ${FILE_SIZE_UNITS[i]}`;
}

/** 加载可用课题列表 */
async function loadProjects() {
  const params = isStudent.value
      ? {pageNo: 1, pageSize: 200, sid: authStore.userId}
      : {pageNo: 1, pageSize: 500};
  const res = await projectApi.list(params);
  state.projectOptions = res.list || [];
}

/** 加载进度列表（后端真分页） */
async function loadData() {
  state.loading = true;
  await loadProjects();
  try {
    const params = {
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      title: query.title || undefined,
      status: query.status !== "" ? query.status : undefined,
      pId: query.pId || undefined,
      isAsc: query.isAsc
    };
    const res = await progressApi.list(params);
    state.progressRows = res.list || [];
    state.total = res.total || 0;
  } catch {
    ElMessage.error("加载进度列表失败");
  } finally {
    state.loading = false;
  }
}

/** 搜索（重置到第1页） */
function handleSearch() {
  query.pageNo = 1;
  loadData();
}

/** 重置筛选 */
function handleReset() {
  query.title = '';
  query.status = '';
  query.pId = '';
  query.pageNo = 1;
  loadData();
}

/** 打开新增/编辑弹窗 */
function openEdit(item = null) {
  state.form = item ? {...item} : {
    id: null, pId: null, title: "", content: "", fileUrl: "", fileName: "", fileSize: 0, status: 0, opinion: ""
  };
  state.dialogVisible = true;
}

/** 处理文件选择/拖拽上传 */
async function handleFileChange(e) {
  const file = e.raw || e;
  if (file.size > MAX_FILE_SIZE) return ElMessage.error(`文件大小不能超过 10MB`);

  state.uploadLoading = true;
  try {
    const res = await progressApi.uploadFile(file);
    if (res?.data) {
      state.form.fileName = file.name;
      // 后端返回 fileUrl（完整URL）、objectName（MinIO存储名）、fileName（原始文件名）
      state.form.fileUrl = res.data.fileUrl || res.data.objectName || '';
      state.form.fileSize = file.size;
      ElMessage.success("文件上传成功");
    }
  } catch {
    ElMessage.error("上传失败");
  } finally {
    state.uploadLoading = false;
  }
}

/** 提交进度表单（学生上报 or 教师审核） */
async function submitForm() {
  if (isAuditMode.value) {
    if (state.form.status === 2 && !state.form.opinion) {
      return ElMessage.warning("退回时必须填写指导意见");
    }
    await progressApi.update({id: state.form.id, status: state.form.status, opinion: state.form.opinion});
  } else {
    if (!state.form.pId || !state.form.title) return ElMessage.warning("请完善课题和标题");
    const api = state.form.id ? progressApi.update : progressApi.create;
    await api({...state.form, status: 0});
  }
  ElMessage.success("提交成功");
  state.dialogVisible = false;
  await loadData();
}

/** 删除进度记录 */
function handleRemove(id) {
  ElMessageBox.confirm('确定删除该记录吗？', '提示', {type: 'warning'}).then(async () => {
    await progressApi.remove([id]);
    ElMessage.success("删除成功");
    await loadData();
  });
}

/** 获取文件完整访问 URL */
function resolveFileUrl(url) {
  if (!url) return '';
  return url;
}

/** 预览附件 */
function handlePreview(item) {
  const url = resolveFileUrl(item.fileUrl);
  if (!url) return ElMessage.warning("无附件可预览");
  window.open(url, '_blank');
}

/** 下载附件，使用后端接口 */
async function handleDownload(item) {
  const fileUrl = resolveFileUrl(item.fileUrl);
  if (!fileUrl) return ElMessage.warning("无附件");
  try {
    await progressApi.download(null, item.fileName, fileUrl);
  } catch (e) {
    ElMessage.error("下载失败");
  }
}

/** 清除已选附件 */
function handleClearFile() {
  state.form.fileUrl = "";
  state.form.fileName = "";
  state.form.fileSize = 0;
}

onMounted(loadData);
</script>

<template>
  <div class="page-container">
    <el-row v-if="canAudit" class="m-b-20">
      <el-col :span="24">
        <el-card shadow="hover" class="audit-card" :class="{ 'is-empty': !pendingProgress.length }">
          <div class="flex-between">
            <div class="audit-info">
              <el-icon class="bell-icon" :class="{ 'pulse': pendingProgress.length }">
                <Bell/>
              </el-icon>
              <span class="m-l-10 font-600">
                {{
                  pendingProgress.length ? `您有 ${pendingProgress.length} 项进度待审核` : '暂无待处理任务，保持得不错！'
                }}
              </span>
            </div>
            <div class="audit-list" v-if="pendingProgress.length">
              <el-avatar-group>
                <el-button type="danger" size="small" round @click="openEdit(pendingProgress[0])">立即处理</el-button>
              </el-avatar-group>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="flex-between">
          <div class="header-info">
            <span class="header-title">课题进度动态</span>
            <span class="header-tag">{{ state.total }} 条</span>
          </div>
          <div class="header-actions">
            <el-button v-if="isStudent" type="primary" :icon="Plus" @click="openEdit()">上报进度</el-button>
            <el-button :icon="Refresh" @click="loadData">刷新</el-button>
          </div>
        </div>
      </template>

        <el-form :inline="true" size="default" class="filter-form">
        <el-form-item>
          <el-input v-model="query.title" placeholder="按阶段标题搜索..." prefix-icon="Search" clearable/>
        </el-form-item>
        <el-form-item v-if="!isStudent">
          <el-select v-model="query.pId" placeholder="按课题筛选" clearable style="width: 180px" filterable>
            <el-option v-for="item in state.projectOptions" :key="item.id" :label="item.title" :value="item.id"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.status" placeholder="所有状态" clearable style="width: 140px">
            <el-option v-for="opt in PROGRESS_STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">筛选</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="enrichedRows" v-loading="state.loading" border stripe>
        <el-table-column label="课题与进展" min-width="240">
          <template #default="{ row }">
            <div class="project-info">
              <div class="p-title">{{ row.projectTitle }}</div>
              <div class="p-stage">
                <el-icon>
                  <Document/>
                </el-icon>
                {{ row.title }}
              </div>
              <div class="p-desc text-ellipsis">{{ row.content || '未填写进展详情' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="材料附件" min-width="200">
          <template #default="{ row }">
            <div v-if="row.fileName" class="file-box">
              <el-icon><Document/></el-icon>
              <span class="f-name">{{ row.fileName }}</span>
              <span class="f-size">{{ formatFileSize(row.fileSize) }}</span>
              <div class="file-ops">
                <el-tooltip content="在线预览" placement="top">
                  <el-button link type="primary" size="small" @click.stop="handlePreview(row)">预览</el-button>
                </el-tooltip>
                <el-tooltip content="下载到本地" placement="top">
                  <el-button link type="success" size="small" @click.stop="handleDownload(row)">下载</el-button>
                </el-tooltip>
              </div>
            </div>
            <span v-else class="text-muted">未上传附件</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTone('progress', row.status)" effect="dark" round>
              {{ getLabel(PROGRESS_STATUS_OPTIONS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="指导意见" min-width="180">
          <template #default="{ row }">
            <div class="opinion-text" :class="{ 'is-empty': !row.opinion }">
              {{ row.opinion || '暂无意见' }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">{{ canAudit ? '去审核' : '详情' }}</el-button>
            <el-button v-if="isAdmin || (isStudent && row.status !== 1)" type="danger" link
                       @click="handleRemove(row.id)">删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <PaginationBar :page-no="query.pageNo" :page-size="query.pageSize" :total="state.total"
                       @change="(p) => { query.pageNo = p; loadData(); }"
                       @size-change="(s) => { query.pageSize = s; query.pageNo = 1; loadData(); }"/>
      </div>
    </el-card>

    <el-dialog v-model="state.dialogVisible" :title="isAuditMode ? '进度评定' : '进度详情'" width="640px">
      <el-form label-position="top">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联课题" required>
              <el-select v-model="state.form.pId" :disabled="!isStudentEditingOwnDraft" style="width: 100%">
                <el-option v-for="item in state.projectOptions" :key="item.id" :label="item.title" :value="item.id"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="进展阶段" required>
              <el-input v-model="state.form.title" :disabled="!isStudentEditingOwnDraft" placeholder="例如：中期汇报"/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="详细内容描述">
          <el-input type="textarea" v-model="state.form.content" rows="3" :disabled="!isStudentEditingOwnDraft"/>
        </el-form-item>

        <el-form-item label="附件材料">
          <el-upload
              v-if="isStudentEditingOwnDraft"
              drag action="#" :auto-upload="false" :on-change="handleFileChange" :show-file-list="false"
              class="full-width-upload"
          >
            <el-icon class="el-icon--upload">
              <UploadFilled/>
            </el-icon>
            <div class="el-upload__text">拖拽文件至此 或 <em>点击上传</em></div>
          </el-upload>

          <div v-if="state.form.fileName" class="file-display-card">
            <div class="f-info">
              <el-icon><Document/></el-icon>
              <span>{{ state.form.fileName }} ({{ formatFileSize(state.form.fileSize) }})</span>
            </div>
            <div class="f-ops">
              <el-button type="primary" link @click="handlePreview(state.form)">在线预览</el-button>
              <el-button type="success" link @click="handleDownload(state.form)">下载</el-button>
              <el-button v-if="isStudentEditingOwnDraft" type="danger" link @click="handleClearFile">移除</el-button>
            </div>
          </div>
        </el-form-item>

        <template v-if="canAudit">
          <el-divider content-position="left">审核评定</el-divider>
          <el-row :gutter="20">
            <el-col :span="10">
              <el-form-item label="审核结果">
                <el-radio-group v-model="state.form.status">
                  <el-radio :label="1">通过</el-radio>
                  <el-radio :label="2">退回修改</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="14">
              <el-form-item label="指导意见" :required="state.form.status === 2">
                <el-input type="textarea" v-model="state.form.opinion" placeholder="请填写评语或修改意见..."/>
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        <el-form-item v-else-if="state.form.opinion" label="指导意见">
          <div class="opinion-box">{{ state.form.opinion }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="state.dialogVisible = false">关闭</el-button>
        <el-button v-if="isStudentEditingOwnDraft || canAudit" type="primary" @click="submitForm"
                   :loading="state.loading">提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: 24px;
  background: transparent;
  min-height: 100vh;
}

.flex-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.m-b-20 {
  margin-bottom: 20px;
}

.font-600 {
  font-weight: 600;
}

/* 审核卡片优化 */
.audit-card {
  border-left: 4px solid #409eff;
  background: var(--panel);
}

.audit-card.is-empty {
  border-left-color: #67c23a;
}

.bell-icon {
  font-size: 20px;
  color: #409eff;
}

.bell-icon.pulse {
  animation: pulse 2s infinite;
  color: #f56c6c;
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

/* 主卡片样式 */
.main-card {
  border-radius: var(--r-card);
}

.header-title {
  font-size: 18px;
  font-weight: bold;
  color: var(--text);
}

.header-tag {
  margin-left: 12px;
  font-size: 12px;
  color: var(--muted);
  background: var(--panel-strong);
  padding: 2px 8px;
  border-radius: var(--r-tag);
}

.filter-form {
  margin-bottom: 16px;
}

/* 表格内项目信息 */
.project-info .p-title {
  font-weight: bold;
  color: #409eff;
}

.project-info .p-stage {
  font-size: 13px;
  margin: 4px 0;
  color: #606266;
}

.project-info .p-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.text-ellipsis {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 附件盒子 */
.file-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: rgba(64, 158, 255, 0.08);
  border: 1px solid rgba(64, 158, 255, 0.2);
  border-radius: var(--r-tag);
  transition: background 0.2s, border-color 0.2s;
}

.file-box:hover {
  background: rgba(64, 158, 255, 0.15);
}

.file-box .f-name {
  font-size: 12px;
  color: #409eff;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-box .f-size {
  font-size: 10px;
  color: #909399;
}

.file-box .file-ops {
  display: flex;
  gap: 4px;
  margin-left: auto;
  flex-shrink: 0;
}

/* 指导意见样式 */
.opinion-text {
  font-size: 13px;
  color: #e6a23c;
}

.opinion-text.is-empty {
  color: #c0c4cc;
  font-style: italic;
}

.opinion-box {
  padding: 12px;
  background: rgba(212, 136, 6, 0.08);
  border: 1px solid rgba(212, 136, 6, 0.2);
  border-radius: var(--r-tag);
  color: var(--warning);
  font-size: 13px;
}

/* 弹窗上传样式 */
.full-width-upload :deep(.el-upload), .full-width-upload :deep(.el-upload-dragger) {
  width: 100%;
}

.file-display-card {
  margin-top: 12px;
  padding: 12px;
  background: var(--panel-strong);
  border-radius: var(--r-tag);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px dashed var(--line);
}

.file-display-card .f-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}
</style>
