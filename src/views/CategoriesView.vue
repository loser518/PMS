<script setup>
import {onMounted, reactive, ref} from "vue";
import {categoryApi} from "../api/modules";
import PaginationBar from "../components/PaginationBar.vue";
import {usePagination} from "../composables";
import {COLOR_TYPE_OPTIONS} from "../utils/constants";
import {ElMessage, ElMessageBox} from 'element-plus';

// 使用通用分页 Hook
const {query, page, loading, loadData, changePage, changeSize, search, resetQuery} = usePagination(
    (params) => categoryApi.list(params),
    {name: '', isActive: ''}
);

// 表单相关状态
const form = reactive({id: null, name: "", colorType: "info", isActive: 1});
const originalName = ref("");
const dialogVisible = ref(false);

/**
 * 打开编辑弹窗
 */
function openEdit(item = null) {
  if (item) {
    Object.assign(form, {...item, isActive: Number(item.isActive)});
    originalName.value = item.name;
  } else {
    Object.assign(form, {id: null, name: "", colorType: "info", isActive: 1});
    originalName.value = "";
  }
  dialogVisible.value = true;
}

/**
 * 提交表单
 */
async function submitForm() {
  if (!form.name) return ElMessage.warning("请输入分类名称");

  try {
    let res;
    // 兼容同名更新逻辑
    if (form.id && form.name === originalName.value) {
      const tempName = `${form.name}_tmp_${Date.now()}`;
      await categoryApi.update({...form, name: tempName});
      res = await categoryApi.update({...form, name: originalName.value});
    } else if (form.id) {
      res = await categoryApi.update({...form});
    } else {
      res = await categoryApi.create({...form, isActive: String(form.isActive)});
    }

    ElMessage.success(res.msg || "保存成功");
    dialogVisible.value = false;
    await loadData();
  } catch (err) {
    ElMessage.error("操作失败");
  }
}

/**
 * 删除分类
 */
function handleRemove(id) {
  ElMessageBox.confirm('确定要删除吗？', '提示', {type: 'warning'}).then(async () => {
    await categoryApi.remove([id]);
    ElMessage.success("删除成功");
    await loadData();
  }).catch(() => {
  });
}

// 初始化
onMounted(loadData);
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="flex-between">
          <div class="header-info">
            <span class="header-title">公告分类管理</span>
            <span class="header-tag">{{ page.total }} 条</span>
          </div>
          <div class="actions">
            <el-button @click="loadData">刷新</el-button>
            <el-button type="primary" @click="openEdit()">+ 新增分类</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" size="default" class="filter-form">
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="搜索..." clearable/>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.isActive" placeholder="全部" style="width: 100px">
            <el-option label="全部" value=""/>
            <el-option label="启用" :value="1"/>
            <el-option label="停用" :value="0"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="page.list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center"/>
        <el-table-column prop="name" label="分类名称"/>
        <el-table-column label="颜色标识" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.colorType" effect="dark">{{ row.colorType }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="启用状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch
                :model-value="Number(row.isActive) === 1"
                active-color="#13ce66"
                inactive-color="#ff4949"
                disabled
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" align="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleRemove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <PaginationBar
            :page-no="page.pageNo || query.pageNo"
            :page-size="page.pageSize || query.pageSize"
            :total="page.total"
            @change="changePage"
            @size-change="changeSize"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑' : '新增'" width="400px">
      <el-form label-position="top">
        <el-form-item label="名称">
          <el-input v-model="form.name"/>
        </el-form-item>
        <el-form-item label="颜色">
          <el-select v-model="form.colorType" style="width: 100%">
            <el-option v-for="item in COLOR_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="状态控制">
          <el-switch
              v-model="form.isActive"
              :active-value="1"
              :inactive-value="0"
              active-text="启用"
              inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: 20px;
  background: transparent;
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

.filter-form {
  margin-bottom: 16px;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-form-item) {
  margin-bottom: 12px;
}
</style>
