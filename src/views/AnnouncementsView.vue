<script setup>
import {computed, onMounted, reactive, ref, shallowRef, onBeforeUnmount} from "vue";
import {announcementApi, categoryApi} from "../api/modules";
import {useAuthStore} from "../stores/auth";
import {usePagination} from "../composables";
import PaginationBar from "../components/PaginationBar.vue";
import {ElNotification, ElMessageBox} from "element-plus";
// 1. 引入富文本组件与样式
import {Editor, Toolbar} from "@wangeditor/editor-for-vue";
import "@wangeditor/editor/dist/css/style.css";
import {
  ANNOUNCEMENT_PRIORITY_OPTIONS,
  ANNOUNCEMENT_STATUS_OPTIONS,
  TARGET_ROLE_OPTIONS,
  getLabel,
} from "../utils/constants";
import {Refresh} from "@element-plus/icons-vue";

const authStore = useAuthStore();

// 使用通用分页 Hook
const {query, page, loading, loadData, changePage, changeSize, search, resetQuery} = usePagination(
    (params) => announcementApi.list(params),
    {title: '', categoryId: '', status: '', targetRole: '', pageSize: 8}
);

// 分类数据
const categories = ref([]);

// --- wangEditor 配置 ---
const editorRef = shallowRef(); // 必须使用 shallowRef
const toolbarConfig = {};
const editorConfig = {placeholder: "请输入公告正文内容..."};
const onEditorCreated = (editor) => {
  editorRef.value = editor;
};

// 页面销毁前销毁编辑器，防止内存泄露
onBeforeUnmount(() => {
  const editor = editorRef.value;
  if (editor == null) return;
  editor.destroy();
});

// --- 状态定义 ---
const form = reactive({
  id: null, title: "", content: "", categoryId: null, priority: 0, targetRole: "ALL", status: 1
});
const modal = reactive({edit: false, detail: false});
const selectedItem = ref(null);

// --- 权限与映射 ---
const isAdmin = computed(() => authStore.role === 2);
const isTeacher = computed(() => authStore.role === 1);
const isStudent = computed(() => authStore.role === 0);
const categoryMap = computed(() => Object.fromEntries(categories.value.map((item) => [item.id, item])));

// --- 数据列表逻辑 ---
const filteredList = computed(() => {
  let list = [...page.value.list];
  if (!isAdmin.value) list = list.filter(item => item.status === 1);

  // 根据用户角色过滤公告
  if (!isAdmin.value) {
    list = list.filter(item => {
      const targetRole = item.targetRole;
      // ALL 全部可见
      if (targetRole === 'ALL') return true;
      // STUDENT 仅学生可见
      if (targetRole === 'STUDENT' && isStudent.value) return true;
      // TEACHER 仅教师可见
      if (targetRole === 'TEACHER' && isTeacher.value) return true;
      return false;
    });
  }

  list.sort((a, b) => {
    if ((a.priority === 2) !== (b.priority === 2)) return a.priority === 2 ? -1 : 1;
    const sortField = query.sortBy === 'view_count' ? 'viewCount' : 'id';
    return query.isAsc ? a[sortField] - b[sortField] : b[sortField] - a[sortField];
  });
  return list;
});

// --- 业务逻辑 ---
async function loadCategories() {
  const res = await categoryApi.list({pageNo: 1, pageSize: 500});
  categories.value = res.list || [];
}

function openEdit(item = null) {
  if (item) {
    Object.assign(form, {...item});
  } else {
    Object.assign(form, {
      id: null, title: "", content: "", categoryId: null, priority: 0, targetRole: "ALL", status: 1
    });
  }
  modal.edit = true;
}

function viewDetail(item) {
  selectedItem.value = item;
  modal.detail = true;
  // 调用后端接口增加阅读量，同时在前端乐观更新
  announcementApi.view(item.id).catch(() => {
  });
  item.viewCount = (item.viewCount || 0) + 1;
}

async function submitForm() {
  // 富文本空内容判断（wangEditor 默认空内容为 <p><br></p>）
  if (!form.title || !form.content || form.content === '<p><br></p>') {
    return ElNotification.warning("请填写完整的标题和内容");
  }
  try {
    const api = form.id ? announcementApi.update : announcementApi.create;
    await api(form);
    ElNotification.success("操作成功");
    modal.edit = false;
    loadData();
  } catch (err) {
    ElNotification.error("保存失败");
  }
}

function confirmDelete(id) {
  ElMessageBox.confirm("确定要永久删除这条公告吗？", "警告", {
    confirmButtonText: "确定删除",
    cancelButtonText: "取消",
    type: "warning"
  }).then(async () => {
    await announcementApi.remove([id]);
    ElNotification.success("删除成功");
    loadData();
  });
}

onMounted(() => {
  loadCategories();
  loadData();
});
</script>

<template>
  <div class="announcement-manage">

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="公告标题">
          <el-input v-model="query.title" placeholder="关键字搜索" clearable @keyup.enter="search"/>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 150px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"/>
          </el-select>
        </el-form-item>
        <el-form-item v-if="isAdmin" label="发布状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option v-for="s in ANNOUNCEMENT_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value"/>
          </el-select>
        </el-form-item>
        <el-form-item v-if="isAdmin" label="目标角色">
          <el-select v-model="query.targetRole" placeholder="全部角色" clearable style="width: 120px">
            <el-option v-for="r in TARGET_ROLE_OPTIONS" :key="r.value" :label="r.label" :value="r.value"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询数据</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
        <el-form-item>
          <el-button v-if="isAdmin" type="primary" @click="openEdit()">发布新公告</el-button>
          <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-loading="loading" class="content-body">
      <el-row :gutter="20" v-if="filteredList.length > 0">
        <el-col v-for="item in filteredList" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6">
          <el-card class="announcement-item" shadow="hover" :class="{ 'top-priority': item.priority === 2 }">
            <template #header>
              <div class="card-header">
                <el-tag size="small" :type="item.priority === 2 ? 'danger' : item.priority === 1 ? 'warning' : 'info'">
                  {{
                    item.priority === 2 ? '📌 置顶' : item.priority === 1 ? '⚡ 紧急' : categoryMap[item.categoryId]?.name || '通知'
                  }}
                </el-tag>
                <div class="ann-meta-row">
                  <span class="view-count">{{ item.createTime ? item.createTime.slice(0, 10) : '' }}</span>
                  <span>👁 {{ item.viewCount || 0 }} 次阅读</span>
                </div>
              </div>
            </template>

            <div class="card-body" @click="viewDetail(item)">
              <h3 class="title">{{ item.title }}</h3>
              <div class="excerpt" v-html="item.content"></div>
            </div>

            <div v-if="isAdmin" class="card-footer">
              <el-tag size="small" :type="item.status === 1 ? 'success' : 'warning'">
                {{ getLabel(ANNOUNCEMENT_STATUS_OPTIONS, item.status) }}
              </el-tag>
              <div class="ops">
                <el-button link type="primary" @click="openEdit(item)">编辑</el-button>
                <el-button link type="danger" @click="confirmDelete(item.id)">删除</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-else description="暂无相关公告"/>
    </div>

    <div class="pagination-wrapper">
      <PaginationBar
          :page-no="page.pageNo || query.pageNo"
          :page-size="page.pageSize || query.pageSize"
          :total="page.total"
          :page-size-options="[8, 12, 24, 48]"
          @change="changePage"
          @size-change="changeSize"
      />
    </div>

    <el-dialog
        v-model="modal.edit"
        :title="form.id ? '编辑公告内容' : '发布新公告信息'"
        width="800px"
        destroy-on-close
    >
      <el-form :model="form" label-position="top">
        <el-form-item label="公告标题" required>
          <el-input v-model="form.title" placeholder="请输入标题"/>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="公告分类">
              <el-select v-model="form.categoryId" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="展示优先级">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option v-for="p in ANNOUNCEMENT_PRIORITY_OPTIONS" :key="p.value" :label="p.label" :value="p.value"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="目标角色">
              <el-select v-model="form.targetRole" style="width: 100%">
                <el-option v-for="r in TARGET_ROLE_OPTIONS" :key="r.value" :label="r.label" :value="r.value"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="公告详情内容" required>
          <div class="editor-wrap">
            <Toolbar
                class="editor-toolbar"
                :editor="editorRef"
                :defaultConfig="toolbarConfig"
                mode="default"
            />
            <Editor
                style="height: 350px; overflow-y: hidden;"
                v-model="form.content"
                :defaultConfig="editorConfig"
                mode="default"
                @onCreated="onEditorCreated"
            />
          </div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发布状态">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">立即发布</el-radio>
                <el-radio :label="0">保存草稿</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="modal.edit = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认提交</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="modal.detail" title="公告正文详情" size="50%">
      <div v-if="selectedItem" class="announcement-detail">
        <h2 class="detail-title">{{ selectedItem.title }}</h2>
        <div class="detail-info">
          <el-tag size="small">{{ categoryMap[selectedItem.categoryId]?.name }}</el-tag>
          <span class="time">发布时间：{{ selectedItem.createTime || '近期' }}</span>
          <span class="views">阅读量：{{ selectedItem.viewCount }}</span>
        </div>
        <el-divider/>
        <div class="detail-content rich-content" v-html="selectedItem.content"></div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.announcement-manage {
  padding: 20px;
  background-color: transparent;
  min-height: 100vh;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: var(--r-card);
}

.content-body {
  margin-bottom: 20px;
}

.announcement-item {
  margin-bottom: 20px;
  border-radius: var(--r-card);
  transition: box-shadow 0.2s, transform 0.2s;
  border: 1px solid var(--line);
}

.top-priority {
  border-left: 4px solid var(--el-color-danger);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.view-count {
  padding-right: 20px;
  font-size: 12px;
  color: #909399;
}

.card-body {
  cursor: pointer;
  height: 130px;
}

.ann-meta-row {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 8px;
}

.title {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 7. 预览样式优化 */
.excerpt {
  font-size: 14px;
  color: var(--muted);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.excerpt :deep(img) {
  display: none;
}

/* 预览时隐藏富文本里的图片 */
.excerpt :deep(p) {
  margin: 0;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid var(--line);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding-bottom: 20px;
}

.announcement-detail .detail-title {
  margin-top: 0;
  color: var(--text);
  text-align: center;
}

.announcement-detail .detail-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
  font-size: 13px;
  color: var(--muted);
}

/* 详情页富文本样式 */
.detail-content {
  line-height: 1.8;
  color: var(--text);
  font-size: 15px;
  padding: 10px;
}

/* wangEditor 容器 */
.editor-wrap {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: var(--r-input);
  overflow: hidden;
}

.editor-toolbar {
  border-bottom: 1px solid var(--line);
}

/* 暗色模式：富文本预览内容重置白底 */
:deep(.rich-content),
:deep(.rich-content *) {
  background: transparent !important;
  color: inherit;
}

/* 暗色模式：wangEditor 编辑器区域适配 */
:deep(.w-e-text-container) {
  background-color: transparent !important;
  color: var(--text) !important;
}

:deep(.w-e-toolbar) {
  background-color: transparent !important;
  border-color: var(--line) !important;
}

/* 暗色模式下标题颜色 */
.detail-title,
.title {
  color: var(--text);
}
</style>
