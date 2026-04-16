import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';

/**
 * 通用表单弹窗 Hook
 * 用于统一处理表单弹窗的新增、编辑、提交逻辑
 * 
 * @param {Function} createFn - 创建函数
 * @param {Function} updateFn - 更新函数
 * @param {Function} createEmptyForm - 创建空表单的函数
 * @param {Function} onSuccess - 成功回调
 * @returns {Object} 表单弹窗相关的响应式状态和方法
 * 
 * @example
 * const { form, modal, submitting, openEdit, submitForm, closeForm } = useFormModal(
 *   (data) => projectApi.create(data),
 *   (data) => projectApi.update(data),
 *   () => ({ title: '', type: '' }),
 *   () => loadData()
 * );
 */
export function useFormModal(createFn, updateFn, createEmptyForm, onSuccess = null) {
  // 表单数据
  const form = reactive(createEmptyForm());

  // 弹窗状态
  const modal = reactive({
    edit: false,
    create: false
  });

  // 提交状态
  const submitting = ref(false);

  /**
   * 打开编辑弹窗
   * @param {Object|null} item - 编辑对象，null 表示新增
   */
  const openEdit = (item = null) => {
    if (item) {
      // 深拷贝避免直接修改原对象
      Object.assign(form, JSON.parse(JSON.stringify(item)));
      modal.edit = true;
    } else {
      // 重置为空表单
      Object.assign(form, createEmptyForm());
      modal.create = true;
    }
  };

  /**
   * 关闭弹窗
   */
  const closeForm = () => {
    modal.edit = false;
    modal.create = false;
    // 重置表单
    Object.assign(form, createEmptyForm());
  };

  /**
   * 提交表单
   */
  const submitForm = async () => {
    submitting.value = true;
    
    try {
      const isEdit = !!(form.id || modal.edit);
      const apiFn = isEdit ? updateFn : createFn;

      // 构造提交数据（移除响应式）
      const data = JSON.parse(JSON.stringify(form));
      
      await apiFn(data);
      
      ElMessage.success(isEdit ? '更新成功' : '创建成功');
      closeForm();
      
      if (onSuccess) {
        await onSuccess();
      }
    } catch (error) {
      console.error('提交失败:', error);
      ElMessage.error('操作失败，请稍后重试');
    } finally {
      submitting.value = false;
    }
  };

  /**
   * 打开新增弹窗
   */
  const openCreate = () => {
    openEdit(null);
  };

  return {
    form,
    modal,
    submitting,
    openEdit,
    openCreate,
    submitForm,
    closeForm
  };
}
