<script setup>
import {computed, onMounted, reactive, ref} from "vue";
import {useAuthStore} from "../stores/auth";
import {ElMessage, ElMessageBox} from "element-plus";
import {Plus, Search, Refresh, InfoFilled, User} from "@element-plus/icons-vue";
import http from "../api/http";

const authStore = useAuthStore();
const isTeacher = computed(() => authStore.role === 1);
const isStudent = computed(() => authStore.role === 0);
const isAdmin = computed(() => authStore.role === 2);

// API helpers
const topicApi = {
  list: (params) => http.get("/topic", {params}),
  create: (data) => http.post("/topic", data),
  update: (data) => http.post("/topic/update", data),
  remove: (id) => http.delete(`/topic/${id}`),
  select: (id) => http.post(`/topic/select/${id}`)
};

// ---- 查询条件 ----
const query = reactive({
  pageNo: 1,
  pageSize: 9,
  keyword: "",
  type: "",
  onlyMine: false
});

const topics = ref([]);
const total = ref(0);
const loading = ref(false);

// ---- 表单 ----
const formDialog = ref(false);
const formLoading = ref(false);
const form = reactive({
  id: null, title: "", type: "", description: "", requirement: "", status: 1
});

const TOPIC_TYPES = ["学术研究", "工程设计", "应用开发", "创新实践", "调研分析", "其他"];

// ---- 加载数据 ----
async function loadData() {
  loading.value = true;
  try {
    const params = {
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      type: query.type || undefined,
      onlyMine: query.onlyMine
    };
    const res = await topicApi.list(params);
    topics.value = res.list || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNo = 1;
  loadData();
}

function handleReset() {
  query.keyword = "";
  query.type = "";
  query.onlyMine = false;
  query.pageNo = 1;
  loadData();
}

// ---- 发布/编辑课题 ----
function openCreate() {
  Object.assign(form, {id: null, title: "", type: "", description: "", requirement: "", status: 1});
  formDialog.value = true;
}

function openEdit(topic) {
  Object.assign(form, topic);
  formDialog.value = true;
}

async function submitForm() {
  if (!form.title) return ElMessage.warning("请填写课题名称");
  formLoading.value = true;
  try {
    if (form.id) {
      await topicApi.update({...form});
      ElMessage.success("课题更新成功");
    } else {
      await topicApi.create({...form});
      ElMessage.success("课题发布成功");
    }
    formDialog.value = false;
    loadData();
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || "操作失败");
  } finally {
    formLoading.value = false;
  }
}

async function handleDelete(topic) {
  await ElMessageBox.confirm(`确定删除课题「${topic.title}」吗？`, "提示", {type: "warning"});
  await topicApi.remove(topic.id);
  ElMessage.success("删除成功");
  loadData();
}

// ---- 学生选题 ----
async function handleSelect(topic) {
  await ElMessageBox.confirm(
      `确定选择课题「${topic.title}」吗？\n选题后将自动生成课题申请提交给指导老师审核。`,
      "确认选题", {type: "info"}
  );
  try {
    await topicApi.select(topic.id);
    ElMessage.success("选题成功！课题申请已自动提交，请前往「课题申报」查看状态");
    loadData();
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || "选题失败");
  }
}

const statusMap = {0: {label: "草稿", type: "info"}, 1: {label: "开放中", type: "success"}, 2: {label: "已关闭", type: "danger"}};

onMounted(loadData);
</script>

<template>
  <div class="page-container">
    <!-- 头部卡片 -->
    <el-card shadow="never" class="header-card">
      <div class="page-header">
        <div>
          <h2 class="page-title">选题市场</h2>
          <p class="page-desc">浏览教师发布的课题，找到感兴趣的方向，一键选题自动提交申请</p>
        </div>
        <div class="header-actions" v-if="isTeacher">
          <el-button type="primary" :icon="Plus" @click="openCreate">发布课题</el-button>
          <el-switch v-model="query.onlyMine" active-text="只看我发布的" @change="handleSearch" style="margin-left: 12px"/>
        </div>
      </div>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input v-model="query.keyword" placeholder="搜索课题名称..." clearable style="width: 280px" @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search/></el-icon></template>
        </el-input>
        <el-select v-model="query.type" placeholder="所有类型" clearable style="width: 160px">
          <el-option v-for="t in TOPIC_TYPES" :key="t" :label="t" :value="t"/>
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        <span class="result-count">共 {{ total }} 个课题</span>
      </div>
    </el-card>

    <!-- 课题卡片列表 -->
    <div v-loading="loading" class="topic-grid">
      <div v-if="topics.length === 0 && !loading" class="empty-state">
        <p>暂无课题，{{ isTeacher ? '点击「发布课题」添加' : '请等待教师发布' }}</p>
      </div>
      <el-card
          v-for="topic in topics" :key="topic.id"
          class="topic-card"
          shadow="hover"
      >
        <!-- 卡片头部 -->
        <div class="card-header">
          <div class="topic-title-row">
            <h3 class="topic-title">{{ topic.title }}</h3>
            <el-tag :type="statusMap[topic.status]?.type" size="small">
              {{ statusMap[topic.status]?.label }}
            </el-tag>
          </div>
          <div class="topic-meta">
            <el-tag size="small" type="info" effect="plain">{{ topic.type || '未分类' }}</el-tag>
            <span v-if="topic.selectedSid" class="selected-badge">已被选择</span>
          </div>
        </div>

        <!-- 教师信息 -->
        <div class="teacher-info">
          <el-icon style="color: #ae5d30"><User/></el-icon>
          <span>{{ topic.teacherName }}</span>
          <span v-if="topic.teacherTitle" class="teacher-title">{{ topic.teacherTitle }}</span>
          <span v-if="topic.teacherCollege" class="teacher-college">· {{ topic.teacherCollege }}</span>
        </div>

        <!-- 课题描述 -->
        <div class="topic-desc">{{ topic.description || '暂无描述' }}</div>

        <!-- 要求 -->
        <div v-if="topic.requirement" class="topic-req">
          <el-icon><InfoFilled/></el-icon>
          <span>{{ topic.requirement }}</span>
        </div>

        <!-- 操作按钮 -->
        <div class="card-footer">
          <span class="publish-time">{{ topic.createTime?.substring(0, 10) }}</span>
          <div class="card-actions">
            <!-- 教师操作 -->
            <template v-if="isTeacher && topic.tid === authStore.userId">
              <el-button type="primary" link size="small" @click="openEdit(topic)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(topic)">删除</el-button>
            </template>
            <!-- 管理员操作 -->
            <template v-if="isAdmin">
              <el-button type="danger" link size="small" @click="handleDelete(topic)">删除</el-button>
            </template>
            <!-- 学生选题 -->
            <el-button
                v-if="isStudent && topic.status === 1 && !topic.selectedSid"
                type="primary" size="small"
                @click="handleSelect(topic)"
            >立即选题</el-button>
            <el-tag v-if="isStudent && topic.selectedSid" type="info" size="small">已被选</el-tag>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="prev, pager, next, total"
          background
          @current-change="loadData"
      />
    </div>
  </div>

  <!-- 发布/编辑课题弹窗 -->
  <el-dialog v-model="formDialog" :title="form.id ? '编辑课题' : '发布课题'" width="560px">
    <el-form label-position="top" :model="form">
      <el-form-item label="课题名称" required>
        <el-input v-model="form.title" placeholder="请输入课题名称"/>
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="课题类型">
            <el-select v-model="form.type" placeholder="选择类型" style="width:100%">
              <el-option v-for="t in TOPIC_TYPES" :key="t" :label="t" :value="t"/>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="发布状态">
            <el-radio-group v-model="form.status">
              <el-radio :label="0">草稿</el-radio>
              <el-radio :label="1">立即发布</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="课题描述">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="研究背景、目标和主要内容..."/>
      </el-form-item>
      <el-form-item label="对学生的要求">
        <el-input v-model="form.requirement" type="textarea" :rows="2" placeholder="先修课程、技能要求等..."/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formDialog=false">取消</el-button>
      <el-button type="primary" @click="submitForm" :loading="formLoading">
        {{ form.id ? '保存修改' : '发布课题' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.page-container {
  padding: 24px;
  background: transparent;
  min-height: 100vh;
}

.header-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.result-count {
  font-size: 13px;
  color: #909399;
}

/* 课题网格 */
.topic-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 0;
  color: #c0c4cc;
  font-size: 14px;
}

.topic-card {
  border-radius: 12px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.topic-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.card-header {
  margin-bottom: 12px;
}

.topic-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}

.topic-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.4;
  flex: 1;
}

.topic-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.selected-badge {
  font-size: 11px;
  color: var(--muted);
  background: var(--panel-strong);
  padding: 2px 8px;
  border-radius: 10px;
}

.teacher-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
}

.teacher-title {
  color: #ae5d30;
  font-size: 12px;
}

.teacher-college {
  color: #909399;
  font-size: 12px;
}

.topic-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.topic-req {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  font-size: 12px;
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.08);
  padding: 6px 10px;
  border-radius: 6px;
  margin-bottom: 12px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;
}

.publish-time {
  font-size: 11px;
  color: #c0c4cc;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}
</style>
