<script setup>
import {onMounted, reactive, ref} from "vue";
import {authApi, userApi} from "../api/modules";
import PaginationBar from "../components/PaginationBar.vue";
import {usePagination} from "../composables";
import {ElMessage, ElMessageBox, ElNotification} from "element-plus";
import {Search, Plus, Refresh, Edit, Delete, Lock, User, Download, Upload} from '@element-plus/icons-vue';
import {
  GENDER_OPTIONS, STUDENT_PROFILE_FIELDS, USER_STATUS_OPTIONS,
  getLabel, getStatusTone, summarizeProfile
} from "../utils/constants";

// 使用通用分页 Hook
const {query, page, loading, loadData, changePage, changeSize, resetQuery} = usePagination(
    (params) => userApi.list({...params, role: 0}),
    {username: '', nickname: '', status: ''}
);

// 表单相关状态
const captcha = reactive({checkCode: "", checkCodeKey: ""});
const registerForm = reactive({username: "", password: "", checkCode: ""});
const editForm = reactive({
  user: {id: null, username: "", nickname: "", avatar: "", gender: 2, status: 0, role: 0, phone: "", email: ""},
  profile: {grade: "", major: "", college: "", className: "", enrollmentYear: null, advisorId: null}
});
const modal = reactive({edit: false, create: false});
const submitting = ref(false);

// ==================== Excel 导入导出 ====================
const excelLoading = ref(false);
const importFileRef = ref(null);

/** 导出当前筛选条件下的用户列表 */
async function handleExport() {
  excelLoading.value = true;
  try {
    // 传入当前筛选参数（role=0 固定为学生）
    await userApi.exportExcel({...query, role: 0});
    ElMessage.success("导出成功");
  } catch {
    ElMessage.error("导出失败");
  } finally {
    excelLoading.value = false;
  }
}

/** 触发文件选择框 */
function triggerImport() {
  importFileRef.value?.click();
}

/** 处理 Excel 文件上传 */
async function handleImportFile(e) {
  const file = e.target.files?.[0];
  if (!file) return;
  // 重置 input，方便重复上传同名文件
  e.target.value = "";
  excelLoading.value = true;
  try {
    const res = await userApi.importExcel(file);
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

// 加载验证码
async function loadCaptcha() {
  const res = await authApi.getCaptcha();
  captcha.checkCode = res.data.checkCode;
  captcha.checkCodeKey = res.data.checkCodeKey;
}

// 创建学生
async function createStudent() {
  submitting.value = true;
  try {
    await authApi.register({...registerForm, role: 0, checkCodeKey: captcha.checkCodeKey});
    ElNotification({title: '成功', message: '录入账号成功', type: 'success'});
    modal.create = false;
    Object.assign(registerForm, {username: "", password: "", checkCode: ""});
    await loadData();
  } catch (e) {
    await loadCaptcha();
  } finally {
    submitting.value = false;
  }
}

// 打开编辑弹窗
function openEdit(item) {
  Object.assign(editForm, JSON.parse(JSON.stringify(item)));
  editForm.profile = {
    grade: "",
    major: "",
    college: "",
    className: "",
    enrollmentYear: null,
    advisorId: null,
    ...(item.profile || {})
  };
  modal.edit = true;
}

// 保存用户
async function saveUser() {
  submitting.value = true;
  try {
    await userApi.update(editForm);
    ElNotification({title: '成功', message: '更新信息成功', type: 'success'});
    modal.edit = false;
    await loadData();
  } finally {
    submitting.value = false;
  }
}

// 删除用户
const handleRemove = (id) => {
  ElMessageBox.confirm('此操作将永久删除该学生档案，是否继续？', '安全警告', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning',
    buttonSize: 'default'
  }).then(async () => {
    await userApi.remove([id]);
    ElMessage.success("已删除");
    await loadData();
  }).catch(() => {
  });
};

// 初始化
onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="app-container">

    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="flex-between">
          <div class="header-info">
            <span class="header-title">学生档案管理</span>
            <span class="header-tag">{{ page.total }} 条</span>
          </div>
          <div class="header-actions">
            <el-button type="success" :icon="Plus" @click="modal.create = true; loadCaptcha()">手动录入学生</el-button>
            <el-button :icon="Refresh" @click="loadData">刷新</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="学号">
          <el-input v-model="query.username" placeholder="请输入学号" clearable @keyup.enter="loadData"/>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.nickname" placeholder="请输入姓名" clearable @keyup.enter="loadData"/>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option v-for="opt in USER_STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 工具栏 -->
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-button type="warning" :icon="Upload" :loading="excelLoading" @click="triggerImport">批量导入</el-button>
          <el-button type="primary" :icon="Download" :loading="excelLoading" @click="handleExport">导出 Excel</el-button>
        </div>
        <!-- 隐藏的文件选择框，只接受 .xlsx .xls -->
        <input ref="importFileRef" type="file" accept=".xlsx,.xls" style="display:none" @change="handleImportFile"/>
      </div>

      <el-table v-loading="loading" :data="page.list" border stripe>
        <el-table-column prop="user.id" label="ID" width="80" align="center"/>
        <el-table-column label="学号/账号" width="140">
          <template #default="{ row }">
            <code class="username-code">{{ row.user.username }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="user.nickname" label="姓名" width="100"/>
        <el-table-column label="学籍档案摘要" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-secondary">{{ summarizeProfile(row.profile, STUDENT_PROFILE_FIELDS) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTone('user', row.user.status)" size="small">
              {{ getLabel(USER_STATUS_OPTIONS, row.user.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleRemove(row.user.id)">删除</el-button>
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

    <el-dialog v-model="modal.create" title="系统录入新学生" width="400px">
      <el-form label-position="top">
        <el-form-item label="登录账号 (建议使用学号)">
          <el-input v-model="registerForm.username" :prefix-icon="User"/>
        </el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="registerForm.password" type="password" show-password :prefix-icon="Lock"/>
        </el-form-item>
        <el-form-item label="安全验证">
          <div class="captcha-wrapper">
            <el-input v-model="registerForm.checkCode" placeholder="计算结果"/>
            <img :src="captcha.checkCode" @click="loadCaptcha" class="captcha-img"/>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modal.create = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="createStudent">确认录入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="modal.edit" title="学生档案综合管理" width="700px">
      <el-form :model="editForm" label-width="80px">
        <el-divider content-position="left">账号基础资料</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="editForm.user.nickname"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="editForm.user.gender">
                <el-radio v-for="g in GENDER_OPTIONS" :key="g.value" :label="g.value">{{ g.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账户状态">
              <el-select v-model="editForm.user.status" class="w-full">
                <el-option v-for="s in USER_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">学籍详细档案</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="学院">
              <el-input v-model="editForm.profile.college"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="专业">
              <el-input v-model="editForm.profile.major"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年级">
              <el-input v-model="editForm.profile.grade"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="班级">
              <el-input v-model="editForm.profile.className"/>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="modal.edit = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveUser">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.app-container {
  padding: 24px;
  background-color: transparent;
  min-height: 100vh;
}

.main-card {
  border-radius: var(--r-card);
}

.flex-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-info {
  display: flex;
  align-items: center;
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

.header-actions {
  display: flex;
  gap: 8px;
}

.filter-form {
  margin-bottom: 16px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
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

.username-code {
  background: var(--panel-strong);
  padding: 2px 6px;
  border-radius: 4px;
  color: #409eff;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.text-secondary {
  color: #606266;
  font-size: 13px;
}

.captcha-wrapper {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-img {
  height: 32px;
  cursor: pointer;
  border-radius: 4px;
  transition: opacity 0.2s;
}

.captcha-img:hover {
  opacity: 0.8;
}

.pagination-footer {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.w-full {
  width: 100%;
}

:deep(.el-divider__text) {
  font-weight: 600;
  color: #409eff;
  background-color: var(--panel);
}
</style>
