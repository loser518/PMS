<script setup>
import {onMounted, reactive, ref} from "vue";
import {projectTypeApi} from "../api/modules";
import PaginationBar from "../components/PaginationBar.vue";
import {ElMessage, ElMessageBox} from "element-plus";
import {Plus} from "@element-plus/icons-vue";

// ---- 查询条件 ----
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  name: "",
  status: ""
});

// ---- 页面状态 ----
const state = reactive({
  rows: [],
  total: 0,
  loading: false,
  dialogVisible: false,
  submitting: false,
  form: {id: null, name: "", description: "", sort: 0, status: 1}
});

const isEdit = ref(false);

// ---- 数据加载 ----
async function loadData() {
  state.loading = true;
  try {
    const params = {
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      name: query.name || undefined,
      status: query.status !== "" ? query.status : undefined
    };
    const res = await projectTypeApi.list(params);
    state.rows = res.list || [];
    state.total = res.total || 0;
  } catch {
    ElMessage.error("加载课题类型列表失败");
  } finally {
    state.loading = false;
  }
}

function handleSearch() {
  query.pageNo = 1;
  loadData();
}

function handleReset() {
  query.name = "";
  query.status = "";
  query.pageNo = 1;
  loadData();
}

// ---- 弹窗操作 ----
function openAdd() {
  isEdit.value = false;
  state.form = {id: null, name: "", description: "", sort: 0, status: 1};
  state.dialogVisible = true;
}

function openEdit(row) {
  isEdit.value = true;
  state.form = {...row};
  state.dialogVisible = true;
}

async function submitForm() {
  if (!state.form.name?.trim()) {
    return ElMessage.warning("类型名称不能为空");
  }
  state.submitting = true;
  try {
    if (isEdit.value) {
      await projectTypeApi.update(state.form);
      ElMessage.success("更新成功");
    } else {
      await projectTypeApi.create(state.form);
      ElMessage.success("添加成功");
    }
    state.dialogVisible = false;
    loadData();
  } finally {
    state.submitting = false;
  }
}

// ---- 删除 ----
function handleRemove(id) {
  ElMessageBox.confirm("确定删除该课题类型吗？删除后无法恢复。", "提示", {type: "warning"}).then(async () => {
    await projectTypeApi.remove([id]);
    ElMessage.success("删除成功");
    loadData();
  });
}

// ---- 切换启用/禁用 ----
async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1;
  await projectTypeApi.update({id: row.id, status: newStatus});
  row.status = newStatus;
  ElMessage.success(newStatus === 1 ? "已启用" : "已禁用");
}

onMounted(loadData);
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="flex-between">
          <div class="header-info">
            <span class="header-title">课题类型管理</span>
            <span class="header-tag">{{ state.total }} 条</span>
          </div>
          <el-button type="primary" :icon="Plus" @click="openAdd">新增类型</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="filter-section">
        <el-form :inline="true" size="default">
          <el-form-item>
            <el-input v-model="query.name" placeholder="按名称搜索..." prefix-icon="Search" clearable style="width:200px"/>
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.status" placeholder="全部状态" clearable style="width:130px">
              <el-option label="启用" :value="1"/>
              <el-option label="禁用" :value="0"/>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 表格 -->
      <el-table :data="state.rows" v-loading="state.loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center"/>
        <el-table-column prop="name" label="类型名称" min-width="150"/>
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{'text-muted': !row.description}">{{ row.description || '暂无描述' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序号" width="90" align="center"/>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="dark" round>
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="175" align="center"/>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button :type="row.status === 1 ? 'warning' : 'success'" link @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" link @click="handleRemove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <PaginationBar :page-no="query.pageNo" :page-size="query.pageSize" :total="state.total"
                       @change="(p) => { query.pageNo = p; loadData(); }"
                       @size-change="(s) => { query.pageSize = s; query.pageNo = 1; loadData(); }"/>
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="state.dialogVisible" :title="isEdit ? '编辑课题类型' : '新增课题类型'" width="500px">
      <el-form :model="state.form" label-width="90px" :disabled="state.submitting">
        <el-form-item label="类型名称" required>
          <el-input v-model="state.form.name" placeholder="如：纵向课题、横向课题、毕业论文" maxlength="50" show-word-limit/>
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="state.form.description" :rows="3" placeholder="简要说明该类型的用途或范围..." maxlength="500" show-word-limit/>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="state.form.sort" :min="0" :max="9999" style="width:140px"/>
          <span class="form-tip">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="state.form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="state.dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="state.submitting">确认提交</el-button>
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

.filter-section {
  margin-bottom: 18px;
  padding: 10px;
  background: var(--panel-strong);
  border-radius: var(--r-tag);
}

.text-muted {
  color: #c0c4cc;
  font-style: italic;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}
</style>
