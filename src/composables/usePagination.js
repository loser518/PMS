import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';

/**
 * 通用分页 Hook
 * 用于统一处理分页、加载状态和数据获取逻辑
 * 
 * @param {Function} fetchFn - 数据获取函数，接收 query 参数
 * @param {Object} initialQuery - 初始查询参数
 * @returns {Object} 分页相关的响应式状态和方法
 * 
 * @example
 * const { query, page, loading, loadData, changePage, changeSize, resetQuery } = usePagination(
 *   (params) => userApi.list(params),
 *   { username: '', status: '' }
 * );
 */
export function usePagination(fetchFn, initialQuery = {}) {
  // 查询参数
  const query = reactive({
    pageNo: 1,
    pageSize: 10,
    sortBy: 'id',
    isAsc: false,
    ...initialQuery
  });

  // 分页数据
  const page = ref({
    total: 0,
    pageNo: 1,
    pageSize: 10,
    list: []
  });

  // 加载状态
  const loading = ref(false);

  /**
   * 加载数据
   * 过滤空参数并调用 fetchFn
   */
  const loadData = async () => {
    loading.value = true;
    
    try {
      // 过滤空字符串参数
      const params = { ...query };
      Object.keys(params).forEach(key => {
        if (params[key] === '' || params[key] === null || params[key] === undefined) {
          delete params[key];
        }
      });

      const result = await fetchFn(params);
      page.value = result;
    } catch (error) {
      console.error('加载数据失败:', error);
      ElMessage.error('加载数据失败，请稍后重试');
    } finally {
      loading.value = false;
    }
  };

  /**
   * 切换页码
   * @param {number} newPage - 新页码
   */
  const changePage = (newPage) => {
    query.pageNo = newPage;
    loadData();
  };

  /**
   * 切换每页条数
   * @param {number} newSize - 新的每页条数
   */
  const changeSize = (newSize) => {
    query.pageSize = newSize;
    query.pageNo = 1; // 重置到第一页
    loadData();
  };

  /**
   * 重置查询条件
   * 保留分页相关参数，重置其他查询条件
   */
  const resetQuery = () => {
    const paginationKeys = ['pageNo', 'pageSize', 'sortBy', 'isAsc'];
    Object.keys(query).forEach(key => {
      if (!paginationKeys.includes(key)) {
        if (typeof initialQuery[key] !== 'undefined') {
          query[key] = initialQuery[key];
        } else {
          query[key] = '';
        }
      }
    });
    query.pageNo = 1;
    loadData();
  };

  /**
   * 搜索（重置到第一页并加载）
   */
  const search = () => {
    query.pageNo = 1;
    loadData();
  };

  return {
    query,
    page,
    loading,
    loadData,
    changePage,
    changeSize,
    resetQuery,
    search
  };
}
