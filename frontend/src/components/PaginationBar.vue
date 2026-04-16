<!--<script setup>-->
<!--import { computed } from "vue";-->

<!--const props = defineProps({-->
<!--  pageNo: {-->
<!--    type: Number,-->
<!--    default: 1-->
<!--  },-->
<!--  pageSize: {-->
<!--    type: Number,-->
<!--    default: 10-->
<!--  },-->
<!--  total: {-->
<!--    type: Number,-->
<!--    default: 0-->
<!--  },-->
<!--  pageSizeOptions: {-->
<!--    type: Array,-->
<!--    default: () => [5, 10, 20, 50]-->
<!--  }-->
<!--});-->

<!--const emit = defineEmits(["change", "size-change"]);-->

<!--const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));-->
<!--const pages = computed(() => {-->
<!--  const current = props.pageNo;-->
<!--  const start = Math.max(1, current - 2);-->
<!--  const end = Math.min(totalPages.value, start + 4);-->
<!--  const adjustedStart = Math.max(1, end - 4);-->
<!--  return Array.from({ length: end - adjustedStart + 1 }, (_, index) => adjustedStart + index);-->
<!--});-->

<!--function go(page) {-->
<!--  if (page < 1 || page > totalPages.value || page === props.pageNo) {-->
<!--    return;-->
<!--  }-->
<!--  emit("change", page);-->
<!--}-->
<!--</script>-->

<!--<template>-->
<!--  <div class="pagination" v-if="total > 0">-->
<!--    <div class="pagination__info">-->
<!--      <span>共 {{ total }} 条</span>-->
<!--      <label>-->
<!--        每页-->
<!--        <select :value="pageSize" @change="$emit('size-change', Number($event.target.value))">-->
<!--          <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>-->
<!--        </select>-->
<!--        条-->
<!--      </label>-->
<!--    </div>-->

<!--    <div class="pagination__actions">-->
<!--      <button class="ghost-button" :disabled="pageNo <= 1" @click="go(pageNo - 1)">上一页</button>-->
<!--      <button-->
<!--        v-for="page in pages"-->
<!--        :key="page"-->
<!--        class="ghost-button"-->
<!--        :class="{ 'is-current': page === pageNo }"-->
<!--        @click="go(page)"-->
<!--      >-->
<!--        {{ page }}-->
<!--      </button>-->
<!--      <button class="ghost-button" :disabled="pageNo >= totalPages" @click="go(pageNo + 1)">下一页</button>-->
<!--    </div>-->
<!--  </div>-->
<!--</template>-->
<template>
  <div class="pagination-container" v-if="total > 0">
    <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="pageSizeOptions"
        :total="total"
        :layout="layout"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup>
import { computed } from "vue";
import { ElPagination } from "element-plus";

const props = defineProps({
  pageNo: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 10
  },
  total: {
    type: Number,
    default: 0
  },
  pageSizeOptions: {
    type: Array,
    default: () => [5, 10, 20, 50]
  },
  layout: {
    type: String,
    default: "total, sizes, prev, pager, next, jumper"
  }
});

const emit = defineEmits(["change", "size-change", "update:pageNo", "update:pageSize"]);

// 使用计算属性实现双向绑定
const currentPage = computed({
  get: () => props.pageNo,
  set: (value) => {
    emit("update:pageNo", value);
    emit("change", value);
  }
});

const pageSize = computed({
  get: () => props.pageSize,
  set: (value) => {
    emit("update:pageSize", value);
    emit("size-change", value);
  }
});

function handleSizeChange(size) {
  // 当改变每页条数时，通常会自动跳转到第一页
  emit("size-change", size);
}

function handleCurrentChange(page) {
  emit("change", page);
}
</script>

<style scoped>
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-start;
}

/* 可选：自定义分页样式 */
:deep(.el-pagination) {
  --el-pagination-font-size: 14px;
  --el-pagination-border-radius: 4px;
  --el-pagination-button-color: #606266;
  --el-pagination-button-disabled-color: #c0c4cc;
  --el-pagination-button-bg-color: var(--panel, #ffffff);
  --el-pagination-hover-color: #4f46e5;
}

:deep(.el-pagination.is-background .btn-prev),
:deep(.el-pagination.is-background .btn-next),
:deep(.el-pagination.is-background .el-pager li) {
  background-color: var(--panel, #ffffff);
  border: 1px solid var(--line, #e5e7eb);
  color: var(--text, #606266);
  margin: 0 4px;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #4f46e5;
  border-color: #4f46e5;
  color: #ffffff;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled):hover) {
  color: #4f46e5;
}
</style>
