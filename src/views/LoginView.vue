<script setup>
import {onMounted, reactive, ref} from "vue";
import {useRouter} from "vue-router";
import {authApi} from "../api/modules";
import {useAuthStore} from "../stores/auth";
import {ROLE_OPTIONS} from "../utils/constants";
import {ElNotification} from "element-plus";

const router = useRouter();
const authStore = useAuthStore();

// 状态管理
const mode = ref("login");
const loading = ref(false);
const loadingCaptcha = ref(false);

// 控制通知显示的标志
let notificationClosed = true;
let lastNotificationTime = 0;
const NOTIFICATION_COOLDOWN = 1000; // 冷却时间1秒

// 表单数据
const loginForm = reactive({
  username: "",
  password: "",
  checkCode: ""
});

const registerForm = reactive({
  username: "",
  password: "",
  role: "",
  checkCode: ""
});

// 验证码
const captcha = reactive({
  checkCode: "",
  checkCodeKey: ""
});

// 统一的Warning函数（带冷却时间）
function showNotification(title, message, type = 'error', duration = 2000) {
  const now = Date.now();

  // 如果通知还在显示中，或者距离上次通知时间太短，则不显示新通知
  if (!notificationClosed || now - lastNotificationTime < NOTIFICATION_COOLDOWN) {
    return;
  }

  notificationClosed = false;
  lastNotificationTime = now;

  ElNotification({
    title: title,
    message: message,
    type: type,
    duration: duration,
    onClose: () => {
      notificationClosed = true;
    }
  });
}

// 登录表单验证
function validateLoginForm() {
  if (!loginForm.username.trim()) {
    showNotification('Warning', '请输入用户名', 'warning');
    return false;
  }
  if (!loginForm.password) {
    showNotification('Warning', '请输入密码', 'warning');
    return false;
  }
  if (!loginForm.checkCode) {
    showNotification('Warning', '请输入验证码', 'warning');
    return false;
  }
  return true;
}

// 注册表单验证
function validateRegisterForm() {
  if (!registerForm.username.trim()) {
    showNotification('Warning', '请输入用户名', 'warning');
    return false;
  }
  if (!registerForm.password) {
    showNotification('Warning', '请输入密码', 'warning');
    return false;
  }
  if (registerForm.password.length < 6 || registerForm.password.length > 16) {
    showNotification('Warning', '密码长度必须在6-16位之间', 'warning');
    return false;
  }
  if (!/(?=.*[A-Za-z])(?=.*\d)/.test(registerForm.password)) {
    showNotification('Warning', '密码必须包含字母和数字', 'warning');
    return false;
  }
  if (registerForm.role === '' || registerForm.role === null || registerForm.role === undefined) {
    showNotification('Warning', '请选择注册角色', 'warning');
    return false;
  }
  if (!registerForm.checkCode) {
    showNotification('Warning', '请输入验证码', 'warning');
    return false;
  }
  return true;
}

// 加载验证码
async function loadCaptcha() {
  if (loadingCaptcha.value) return;

  loadingCaptcha.value = true;
  try {
    const res = await authApi.getCaptcha();
    captcha.checkCode = res.data.checkCode;
    captcha.checkCodeKey = res.data.checkCodeKey;
  } catch (error) {
    showNotification('Error', '验证码加载失败，请稍后重试', 'error', 3000);
  } finally {
    loadingCaptcha.value = false;
  }
}

// 登录处理
async function handleLogin() {
  // 防重复点击
  if (loading.value) return;

  // 表单验证
  if (!validateLoginForm()) return;

  loading.value = true;

  try {
    await authStore.login({
      ...loginForm,
      checkCodeKey: captcha.checkCodeKey
    });

    showNotification('Success', '登录成功！', 'success', 1500);

    // 清空密码和验证码
    loginForm.username = "";
    loginForm.password = "";
    loginForm.checkCode = "";

    await router.push("/dashboard");


  } catch (err) {
    showNotification('Error', err.message || '登录失败，请重试', 'error', 3000);

    // 重新加载验证码
    await loadCaptcha();
    loginForm.checkCode = "";

  } finally {
    loading.value = false;
  }
}

// 注册处理
async function handleRegister() {
  // 防重复点击
  if (loading.value) return;

  // 表单验证
  if (!validateRegisterForm()) return;

  loading.value = true;

  try {
    const res = await authApi.register({
      ...registerForm,
      checkCodeKey: captcha.checkCodeKey
    });

    if (res.code !== 200 && res.status !== "success") {
      throw new Error(res.message || res.msg || "注册失败");
    }

    showNotification('Success', '注册成功！请登录', 'success', 1500);

    // 注册成功后清空表单并切换到登录页
    mode.value = "login";
    registerForm.username = "";
    registerForm.password = "";
    registerForm.checkCode = "";
    registerForm.role = "";

    // 重新加载验证码
    await loadCaptcha();

  } catch (err) {
    showNotification('Error', err.message || '注册失败，请重试', 'error', 3000);

    // 重新加载验证码
    await loadCaptcha();
    registerForm.checkCode = "";

  } finally {
    loading.value = false;
  }
}

// 切换模式
function handleModeChange(newMode) {
  if (newMode === mode.value || loading.value) return;

  mode.value = newMode;
  loginForm.username = "";
  loginForm.password = "";
  loginForm.checkCode = "";

  registerForm.username = "";
  registerForm.password = "";
  registerForm.role = "";
  registerForm.checkCode = "";

  // 关闭所有通知
  ElNotification.closeAll();
  notificationClosed = true;

  // 重新加载验证码
  loadCaptcha();
}

// 页面挂载时加载验证码
onMounted(() => {
  loadCaptcha();
});
</script>

<template>
  <div class="auth-wrapper">
    <div class="blob blob-1"></div>
    <div class="blob blob-2"></div>

    <div class="auth-container">

      <section class="auth__card">
        <div class="auth__tabs">
          <div class="tabs__slider" :class="mode"></div>
          <button
              class="tab-btn"
              :class="{ 'is-active': mode === 'login' }"
              @click="handleModeChange('login')"
              :disabled="loading"
          >
            登录
          </button>
          <button
              class="tab-btn"
              :class="{ 'is-active': mode === 'register' }"
              @click="handleModeChange('register')"
              :disabled="loading"
          >
            注册
          </button>
        </div>

        <transition name="form-fade" mode="out-in">
          <form v-if="mode === 'login'" class="auth__form" @submit.prevent="handleLogin" key="login">
            <div class="form-item">
              <label>用户名</label>
              <el-input
                  v-model.trim="loginForm.username"
                  placeholder="请输入用户名"
                  :disabled="loading"
                  class="custom-input"
                  clearable
              />
            </div>

            <div class="form-item">
              <label>密码</label>
              <el-input
                  v-model.trim="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  :disabled="loading"
                  show-password
                  class="custom-input"
              />
            </div>

            <div class="form-item">
              <label>验证码</label>
              <div class="captcha-group">
                <el-input
                    v-model.trim="loginForm.checkCode"
                    placeholder="验证码"
                    :disabled="loading"
                    class="custom-input"
                />
                <div class="captcha-wrapper" @click="loadCaptcha">
                  <img :src="captcha.checkCode" alt="captcha" :class="{ 'is-loading': loadingCaptcha }"/>
                  <div v-if="loadingCaptcha" class="captcha-mask">...</div>
                </div>
              </div>
            </div>

            <button class="submit-btn" :disabled="loading" :class="{ 'is-loading': loading }">
              <span v-if="loading" class="spinner"></span>
              {{ loading ? "正在进入..." : "登录" }}
            </button>
          </form>

          <form v-else class="auth__form" @submit.prevent="handleRegister" key="register">
            <div class="form-item">
              <label>用户名</label>
              <el-input
                  v-model.trim="registerForm.username"
                  placeholder="设置您的用户名"
                  class="custom-input"
              />
            </div>

            <div class="form-item">
              <label>初始密码</label>
              <el-input
                  v-model.trim="registerForm.password"
                  type="password"
                  placeholder="6-16位字母+数字"
                  show-password
                  class="custom-input"
              />
            </div>

            <div class="form-item">
              <label>注册角色</label>
              <el-select v-model="registerForm.role" placeholder="请选择您的身份" class="custom-select">
                <el-option
                    v-for="item in ROLE_OPTIONS.filter(i => i.value !== 2)"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
              </el-select>
            </div>

            <div class="form-item">
              <label>验证码</label>
              <div class="captcha-group">
                <el-input v-model.trim="registerForm.checkCode" placeholder="验证码" class="custom-input"/>
                <div class="captcha-wrapper" @click="loadCaptcha">
                  <img :src="captcha.checkCode" alt="captcha" :class="{ 'is-loading': loadingCaptcha }"/>
                </div>
              </div>
            </div>

            <button class="submit-btn" :disabled="loading" :class="{ 'is-loading': loading }">
              {{ loading ? "创建账户中..." : "注册" }}
            </button>
          </form>
        </transition>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* 核心变量定义 */
.auth-wrapper {
  --brand: #ff8a3d;
  --brand-light: #ffba82;
  --text-main: #583f27;
  --text-muted: #a89485;
  --bg-soft: #fdf6f0;

  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-soft);
  position: relative;
  overflow: hidden;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

/* 装饰背景 */
.blob {
  position: absolute;
  filter: blur(60px);
  z-index: 0;
  border-radius: 50%;
  opacity: 0.4;
}

.auth-container {
  overflow-x: hidden;
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 440px;
  padding: 20px;
}

/* Hero 文字区域 */
.auth__hero {
  text-align: center;
  margin-bottom: 32px;
}

.brand__eyebrow {
  color: var(--brand);
  text-transform: uppercase;
  letter-spacing: 2px;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
}

.brand__title {
  font-size: 64px;
  font-weight: 900;
  color: var(--text-main);
  margin: 0;
  line-height: 1;
}

.brand__subtitle {
  color: var(--text-muted);
  margin-top: 12px;
  font-size: 15px;
}

/* 卡片主体 */
.auth__card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  padding: 40px;
  border-radius: 32px;
  box-shadow: 0 25px 50px -12px rgba(88, 63, 39, 0.12);
}

/* 选项卡设计 */
.auth__tabs {
  display: flex;
  position: relative;
  background: rgba(88, 63, 39, 0.05);
  padding: 4px;
  border-radius: 16px;
  margin-bottom: 32px;
}

.tabs__slider {
  position: absolute;
  top: 4px;
  left: 4px;
  bottom: 4px;
  width: calc(50% - 4px);
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tabs__slider.register {
  transform: translateX(100%);
}

.tab-btn {
  flex: 1;
  padding: 12px;
  border: none;
  background: transparent;
  position: relative;
  z-index: 1;
  cursor: pointer;
  font-weight: 600;
  color: var(--text-muted);
  transition: color 0.3s;
}

.tab-btn.is-active {
  color: var(--brand);
}

/* 表单细节 */
.auth__form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 8px;
  padding-left: 4px;
}

/* 验证码组 */
.captcha-group {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 12px;
}

.captcha-wrapper {
  height: 48px;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid rgba(88, 63, 39, 0.1);
  transition: transform 0.2s;
}

.captcha-wrapper:hover {
  transform: scale(1.02);
}

.captcha-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-wrapper img.is-loading {
  opacity: 0.5;
}

/* 提交按钮 */
.submit-btn {
  margin-top: 10px;
  height: 52px;
  border-radius: 16px;
  background: var(--brand);
  color: white;
  border: none;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s, box-shadow 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.submit-btn:hover:not(:disabled) {
  background: #f77a25;
  transform: translateY(-2px);
  box-shadow: 0 10px 20px -5px rgba(255, 138, 61, 0.4);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 深度选择器修改 Element 样式 */
:deep(.custom-input .el-input__wrapper),
:deep(.custom-select .el-input__wrapper) {
  background: white !important;
  border-radius: 14px !important;
  height: 48px !important;
  box-shadow: 0 0 0 1px rgba(88, 63, 39, 0.1) inset !important;
  transition: border-color 0.2s, box-shadow 0.2s;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--brand) inset, 0 0 0 4px rgba(255, 138, 61, 0.1) !important;
}

/* 修复输入框内部高度 */
:deep(.custom-input .el-input__inner),
:deep(.custom-select .el-input__inner) {
  height: 48px !important;
  line-height: 48px !important;
}

/* 修复验证码输入框高度与图片对齐 */
:deep(.captcha-group .el-input__wrapper) {
  height: 48px !important;
}

:deep(.captcha-group .el-input__inner) {
  height: 48px !important;
  line-height: 48px !important;
}

/* 动画 */
.form-fade-enter-active, .form-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.form-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.form-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
