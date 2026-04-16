<script setup>
import {computed, reactive, ref} from "vue";
import {authApi, userApi} from "../api/modules";
import {useAuthStore} from "../stores/auth";
import {
  GENDER_OPTIONS,
  ROLE_OPTIONS,
  STUDENT_PROFILE_FIELDS,
  TEACHER_PROFILE_FIELDS,
  getLabel
} from "../utils/constants";
import {ElMessage, ElNotification} from "element-plus";

// ---- 常量 ----
/** 头像对象存储基础路径 */
const AVATAR_BASE_URL = (import.meta.env.VITE_MINIO_BASE_URL || "http://127.0.0.1:9000/pms-bucket") + "/";
/** 头像最大文件大小 2MB */
const MAX_AVATAR_SIZE = 2 * 1024 * 1024;
/** 允许上传的头像格式 */
const VALID_AVATAR_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
/** 预设封面背景图片列表 */
const PRESET_BACKGROUNDS = [
  "https://images.unsplash.com/photo-1557683316-973673baf926?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1477346611705-65d1883cee1e?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=80"
];
/** 学生档案数字类型字段 */
const NUMERIC_PROFILE_KEYS = ['enrollmentYear', 'maxStudentCount', 'currentStudentCount'];

const authStore = useAuthStore();
const activeTab = ref('basic');

// ---- 头像裁剪相关 ----
const showCropper = ref(false);
const cropperRef = ref(null);
const cropOptions = reactive({
  img: '',
  autoCrop: true,
  autoCropWidth: 200,
  autoCropHeight: 200,
  fixedBox: true,
  fixed: true,
  fixedNumber: [1, 1],
  centerBox: true,
  infoTrue: true,
  full: false,
  canMove: true,
  canMoveBox: true,
  original: false,
  maxImgSize: 3000,
  enlarge: 1,
  mode: 'contain'
});
const previewUrl = ref('');
const croppedFile = ref(null);

// ---- 图片预览相关 ----
const showPreview = ref(false);
const previewImage = ref('');

// ---- 背景选择相关 ----
const showBgPicker = ref(false);
const tempBg = ref("");

function openBgPicker() {
  tempBg.value = state.profileForm.user.background;
  showBgPicker.value = true;
}

function confirmBg() {
  state.profileForm.user.background = tempBg.value;
  showBgPicker.value = false;
  saveProfile();
}

// ---- 表单状态 ----
const state = reactive({
  profileForm: JSON.parse(JSON.stringify(authStore.userInfo || {user: {}, profile: {}})),
  passwordForm: {pwd: "", npwd: ""}
});

const isStudent = computed(() => authStore.role === 0);
const isTeacher = computed(() => authStore.role === 1);
const profileFields = computed(() => (isStudent.value ? STUDENT_PROFILE_FIELDS : isTeacher.value ? TEACHER_PROFILE_FIELDS : []));

/** 获取头像完整 URL */
const avatarFullUrl = computed(() => {
  const avatar = state.profileForm.user?.avatar;
  if (!avatar) return '';
  return avatar.startsWith('http') ? avatar : `${AVATAR_BASE_URL}${avatar}`;
});

/**
 * 初始化 profile 字段默认值，防止 v-model 绑定空对象时报错
 */
function normalizeProfile() {
  if (isStudent.value) {
    state.profileForm.profile = {
      grade: "", major: "", college: "", className: "",
      enrollmentYear: null,
      ...(state.profileForm.profile || {})
    };
  } else if (isTeacher.value) {
    state.profileForm.profile = {
      title: "", department: "", college: "", researchField: "",
      maxStudentCount: null, currentStudentCount: null,
      ...(state.profileForm.profile || {})
    };
  }
}

normalizeProfile();

/** 保存基础/专业资料 */
async function saveProfile() {
  try {
    await userApi.update(state.profileForm);
    ElNotification({title: '成功', message: '资料更新成功', type: 'success'});
    await authStore.refreshProfile();
  } catch {
    ElNotification({title: '错误', message: '更新失败', type: 'error'});
  }
}

// ---- 头像裁剪功能 ----

/** 选择头像文件，校验后进入裁剪流程 */
function selectAvatarFile(e) {
  const file = e.target.files?.[0];
  if (!file) return;

  if (!VALID_AVATAR_TYPES.includes(file.type)) {
    ElMessage.error('请选择正确的图片格式 (JPG/PNG/GIF/JPEG/WEBP)');
    return;
  }
  if (file.size > MAX_AVATAR_SIZE) {
    ElMessage.error('图片大小不能超过2MB');
    return;
  }

  const reader = new FileReader();
  reader.onload = (ev) => {
    cropOptions.img = ev.target.result;
    croppedFile.value = null;
    previewUrl.value = '';
    showCropper.value = true;
  };
  reader.readAsDataURL(file);
  e.target.value = '';
}

/** 实时更新裁剪预览 */
function updatePreview() {
  if (!cropperRef.value) return;
  const cropData = cropperRef.value.getCropData();
  if (cropData) previewUrl.value = cropData;
}

/** 确认裁剪（仅裁剪，不上传） */
function confirmCrop() {
  if (!cropperRef.value) {
    ElMessage.error('裁剪器初始化失败');
    return;
  }

  const loading = ElMessage.info('正在处理图片...');
  try {
    cropperRef.value.getCropBlob(async (blob) => {
      if (!blob) {
        loading.close();
        ElMessage.error('裁剪失败，请重试');
        return;
      }
      if (previewUrl.value) URL.revokeObjectURL(previewUrl.value);
      previewUrl.value = URL.createObjectURL(blob);
      croppedFile.value = new File([blob], 'avatar.jpg', {type: 'image/jpeg'});
      loading.close();
      ElMessage.success('裁剪完成，点击"上传头像"保存');
    });
  } catch {
    loading.close();
    ElMessage.error('图片处理失败');
  }
}

/** 上传裁剪后的头像 */
async function uploadCroppedAvatar() {
  if (!croppedFile.value) {
    ElMessage.warning('请先裁剪图片');
    return;
  }

  const loading = ElMessage.info('头像上传中...');
  try {
    const res = await userApi.uploadAvatar(croppedFile.value);
    loading.close();

    if (res?.data) {
      if (typeof res.data === 'string') {
        state.profileForm.user.avatar = res.data;
      } else if (res.data.objectName) {
        state.profileForm.user.avatar = res.data.objectName;
      } else if (res.data.fileUrl) {
        state.profileForm.user.avatar = res.data.fileUrl;
      }
    }

    await authStore.refreshProfile();
    ElNotification({title: '成功', message: '头像更新成功', type: 'success'});
    showCropper.value = false;
    croppedFile.value = null;
    if (previewUrl.value) {
      URL.revokeObjectURL(previewUrl.value);
      previewUrl.value = '';
    }
  } catch (err) {
    loading.close();
    ElNotification({
      title: '错误',
      message: '头像上传失败: ' + (err.message || '未知错误'),
      type: 'error'
    });
  }
}

/** 左旋转 */
function rotateLeft() {
  if (cropperRef.value) {
    cropperRef.value.rotateLeft();
    setTimeout(updatePreview, 50);
  }
}

/** 右旋转 */
function rotateRight() {
  if (cropperRef.value) {
    cropperRef.value.rotateRight();
    setTimeout(updatePreview, 50);
  }
}

/** 取消裁剪，清理状态 */
function cancelCrop() {
  showCropper.value = false;
  cropOptions.img = '';
  croppedFile.value = null;
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value);
    previewUrl.value = '';
  }
}

/** 预览头像大图 */
function previewAvatar() {
  if (avatarFullUrl.value) {
    previewImage.value = avatarFullUrl.value;
    showPreview.value = true;
  }
}

// ---- 密码修改 ----

/**
 * 校验密码表单
 * @returns {boolean}
 */
function validatePasswordForm() {
  const {pwd, npwd} = state.passwordForm;
  if (!pwd) {
    ElMessage.warning('请输入旧密码');
    return false;
  }
  if (!npwd) {
    ElMessage.warning('请输入新密码');
    return false;
  }
  if (npwd.length < 6) {
    ElMessage.warning('新密码长度不能小于6位');
    return false;
  }
  if (pwd === npwd) {
    ElMessage.warning('新密码不能与旧密码相同');
    return false;
  }
  return true;
}

/** 修改密码 */
async function updatePassword() {
  if (!validatePasswordForm()) return;

  const loading = ElMessage.info('正在修改密码...');
  try {
    const res = await authApi.updatePassword(state.passwordForm);
    loading.close();

    if (res.status === "success") {
      ElNotification({title: '成功', message: '密码修改成功，请重新登录', type: 'success', duration: 3000});
      state.passwordForm.pwd = "";
      state.passwordForm.npwd = "";
      setTimeout(async () => {
        await authStore.logout();
        window.location.href = "/login";
      }, 1500);
    } else {
      ElNotification({title: '错误', message: res.msg, type: 'error', duration: 3000});
    }
  } catch (err) {
    loading.close();
    const errorMsg = err.response?.data?.message || err.message || '密码修改失败';
    ElNotification({title: '错误', message: errorMsg, type: 'error', duration: 3000});
  }
}
</script>

<template>
  <div class="settings-container">
    <header class="settings-hero"
            :style="{ backgroundImage: `linear-gradient(rgba(0,0,0,0.1), rgba(0,0,0,0.5)), url(${state.profileForm.user.background || PRESET_BACKGROUNDS[0]})` }">
      <div class="hero-overlay">
        <div class="user-meta">
          <div class="avatar-wrapper">
            <img
                :src="avatarFullUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                class="user-avatar"
                @error="(e) => e.target.src = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                @click="previewAvatar"
            />
            <label class="avatar-edit-badge">
              <input type="file" @change="selectAvatarFile" accept="image/*" hidden/>
              <span>更换头像</span>
            </label>
          </div>
          <div class="user-info">
            <h1>{{ state.profileForm.user.nickname || state.profileForm.user.username }}</h1>
            <p>{{ getLabel(ROLE_OPTIONS, authStore.role) }}</p>
            <p>{{ state.profileForm.user.description || '保持热爱，奔赴山海' }}</p>
          </div>
        </div>
        <button class="btn-change-bg" @click="openBgPicker">更换封面</button>
      </div>
    </header>

    <div class="settings-content">
      <aside class="settings-sidebar">
        <nav>
          <button :class="{ active: activeTab === 'basic' }" @click="activeTab = 'basic'">基础资料</button>
          <button v-if="profileFields.length" :class="{ active: activeTab === 'profile' }"
                  @click="activeTab = 'profile'">
            {{ isStudent ? '学籍信息' : '教研档案' }}
          </button>
          <button :class="{ active: activeTab === 'security' }" @click="activeTab = 'security'">账号安全</button>
        </nav>
      </aside>

      <main class="settings-main">
        <transition name="fade" mode="out-in">
          <section v-if="activeTab === 'basic'" class="card" key="basic">
            <h3 class="card-title">基础资料</h3>
            <div class="form-group">
              <label>用户名</label>
              <input v-model="state.profileForm.user.username" placeholder="输入用户名"/>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>昵称</label>
                <input v-model="state.profileForm.user.nickname"/>
              </div>
              <div class="form-group">
                <label>性别</label>
                <select v-model.number="state.profileForm.user.gender">
                  <option v-for="opt in GENDER_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>手机号</label>
                <input v-model="state.profileForm.user.phone"/>
              </div>
              <div class="form-group">
                <label>邮箱</label>
                <input v-model="state.profileForm.user.email"/>
              </div>
            </div>
            <div class="form-group">
              <label>个人简介</label>
              <textarea v-model="state.profileForm.user.description" rows="3"
                        placeholder="介绍一下你自己..."></textarea>
            </div>
            <div class="form-footer">
              <button class="btn-primary" @click="saveProfile">保存修改</button>
            </div>
          </section>

          <section v-else-if="activeTab === 'profile'" class="card" key="profile">
            <h3 class="card-title">{{ isStudent ? '学籍信息' : '教研档案' }}</h3>
            <div class="form-grid-2">
              <template v-for="field in profileFields.filter(f => f.key !== 'currentStudentCount')" :key="field.key">
                <!-- 其他字段：普通 input -->
                <div class="form-group">
                  <label>{{ field.label }}</label>
                  <input v-model="state.profileForm.profile[field.key]"
                         :type="NUMERIC_PROFILE_KEYS.includes(field.key) ? 'number' : 'text'"/>
                </div>
              </template>
              <!-- 教师特有字段，当前指导人数只读 -->
              <template v-if="isTeacher">
                <div class="form-group" v-for="field in profileFields.filter(f=>f.key==='currentStudentCount')" :key="field.key+'_ro'">
                  <label>{{ field.label }}</label>
                  <input :value="state.profileForm.profile[field.key]" disabled style="background:#f8fafc;color:#94a3b8"/>
                </div>
              </template>
            </div>
            <div class="form-footer">
              <button class="btn-primary" @click="saveProfile">更新档案</button>
            </div>
          </section>

          <section v-else-if="activeTab === 'security'" class="card" key="security">
            <h3 class="card-title">修改密码</h3>
            <div class="form-group">
              <label>旧密码 <span class="required-mark">*</span></label>
              <el-input
                  v-model="state.passwordForm.pwd"
                  type="password"
                  placeholder="请输入旧密码"
                  @keyup.enter="updatePassword"
                  show-password
              />
            </div>
            <div class="form-group">
              <label>新密码 <span class="required-mark">*</span></label>
              <el-input
                  v-model="state.passwordForm.npwd"
                  type="password"
                  placeholder="请输入新密码（至少6位）"
                  @keyup.enter="updatePassword"
                  show-password
              />
              <span class="input-hint">密码长度至少6位</span>
              <span class="input-hint">至少包含一个数字和一个字母</span>
            </div>
            <div class="form-footer">
              <button class="btn-danger" @click="updatePassword">确认修改密码</button>
            </div>
          </section>
        </transition>
      </main>
    </div>

    <!-- 封面选择弹窗 -->
    <div v-if="showBgPicker" class="picker-mask" @click.self="showBgPicker = false">
      <div class="picker-dialog">
        <div class="picker-header">
          <h3>选择封面背景</h3>
          <button class="btn-close" @click="showBgPicker = false">×</button>
        </div>
        <div class="picker-body">
          <div class="bg-grid">
            <div
                v-for="url in PRESET_BACKGROUNDS"
                :key="url"
                class="bg-item"
                :class="{ selected: tempBg === url }"
                :style="{ backgroundImage: `url(${url})` }"
                @click="tempBg = url"
            >
              <div v-if="tempBg === url" class="selected-icon">✓</div>
            </div>
          </div>
          <div class="custom-url">
            <label>自定义 URL</label>
            <input v-model="tempBg" placeholder="粘贴外部图片地址..."/>
          </div>
        </div>
        <div class="picker-footer">
          <button class="btn-cancel" @click="showBgPicker = false">取消</button>
          <button class="btn-primary" @click="confirmBg">保存更换</button>
        </div>
      </div>
    </div>

    <!-- 头像裁剪弹窗 - 左右结构 -->
    <div v-if="showCropper" class="cropper-mask" @click.self="cancelCrop">
      <div class="cropper-dialog cropper-dialog-split">
        <!-- 左侧：裁剪区域 -->
        <div class="cropper-left">
          <div class="cropper-header">
            <h3>裁剪头像</h3>
            <button class="btn-close" @click="cancelCrop">×</button>
          </div>
          <div class="cropper-container">
            <VueCropper
                ref="cropperRef"
                :img="cropOptions.img"
                :autoCrop="cropOptions.autoCrop"
                :autoCropWidth="cropOptions.autoCropWidth"
                :autoCropHeight="cropOptions.autoCropHeight"
                :fixedBox="cropOptions.fixedBox"
                :fixed="cropOptions.fixed"
                :fixedNumber="cropOptions.fixedNumber"
                :centerBox="cropOptions.centerBox"
                :infoTrue="cropOptions.infoTrue"
                :full="cropOptions.full"
                :canMove="cropOptions.canMove"
                :canMoveBox="cropOptions.canMoveBox"
                :original="cropOptions.original"
                :maxImgSize="cropOptions.maxImgSize"
                :enlarge="cropOptions.enlarge"
                :mode="cropOptions.mode"
                @imgLoad="updatePreview"
                @crop="updatePreview"
            />
          </div>
          <div class="cropper-tools">
            <button class="tool-btn" @click="rotateLeft" title="左旋转">
              <span class="tool-icon">↺</span>
            </button>
            <button class="tool-btn" @click="rotateRight" title="右旋转">
              <span class="tool-icon">↻</span>
            </button>
          </div>
          <span class="size-limit">图片大小不能超过2MB</span>
          <span class="size-limit">支持的图片格式 (JPG/PNG/GIF/JPEG/WEBP)</span>
        </div>

        <!-- 右侧：预览和操作 -->
        <div class="cropper-right">
          <div class="preview-section">
            <h4>预览效果</h4>
            <div class="preview-box-large">
              <img :src="previewUrl || cropOptions.img" class="preview-img-large"/>
            </div>

            <div class="preview-info">
              <div class="info-item">
                <span class="info-label">状态：</span>
                <span class="info-value">
                  <el-tag :type="croppedFile ? 'success' : 'info'" size="small">
                    {{ croppedFile ? '已裁剪' : '未裁剪' }}
                  </el-tag>
                </span>
              </div>
              <div class="info-item" v-if="croppedFile">
                <span class="info-label">大小：</span>
                <span class="info-value">{{ (croppedFile.size / 1024).toFixed(2) }}KB</span>
              </div>
            </div>
          </div>

          <div class="action-section">
            <button class="action-btn action-btn-cancel" @click="cancelCrop">
              <span class="btn-icon">✕</span> 取消
            </button>
            <button class="action-btn action-btn-crop" @click="confirmCrop">
              <span class="btn-icon">✓</span> 确认裁剪
            </button>
            <button
                class="action-btn action-btn-upload"
                :class="{ 'action-btn-disabled': !croppedFile }"
                :disabled="!croppedFile"
                @click="uploadCroppedAvatar">
              <span class="btn-icon">↑</span> 上传头像
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 头像大图预览弹窗 -->
    <div v-if="showPreview" class="preview-mask" @click.self="showPreview = false">
      <div class="preview-dialog">
        <div class="preview-header">
          <h3>头像预览</h3>
          <button class="btn-close" @click="showPreview = false">×</button>
        </div>
        <div class="preview-body">
          <img :src="previewImage" class="preview-large-img"/>
        </div>
        <div class="preview-footer">
          <button class="btn-primary" @click="showPreview = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 原有的样式保持不变 */
.settings-container {
  max-width: 1100px;
  margin: 2rem auto;
  background: var(--panel);
  border-radius: var(--r-card);
  overflow: hidden;
  box-shadow: var(--shadow);
  min-height: 85vh;
}

.settings-hero {
  height: 300px;
  background-size: cover;
  background-position: center;
  position: relative;
  transition: 0.5s ease;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 40px;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 24px;
  color: white;
}

.avatar-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
  cursor: pointer;
}

.user-avatar {
  width: 100%;
  height: 100%;
  border-radius: var(--r-card);
  border: 4px solid rgba(255, 255, 255, 0.8);
  object-fit: cover;
  transition: transform 0.3s ease;
}

.user-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
}

.avatar-edit-badge {
  position: absolute;
  bottom: -5px;
  right: -5px;
  background: var(--panel);
  color: var(--text);
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  cursor: pointer;
  font-weight: 600;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
  transition: background 0.2s, color 0.2s, transform 0.2s;
  z-index: 2;
}

.avatar-edit-badge:hover {
  background: #3b82f6;
  color: white;
}

.user-info h1 {
  margin: 0;
  font-size: 32px;
}

.btn-change-bg {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  padding: 10px 18px;
  border-radius: var(--r-btn);
  cursor: pointer;
  transition: 0.3s;
}

.btn-change-bg:hover {
  background: rgba(255, 255, 255, 0.4);
}

.settings-content {
  display: grid;
  grid-template-columns: 240px 1fr;
  padding: 40px;
  gap: 40px;
}

.settings-sidebar nav {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.settings-sidebar button {
  text-align: left;
  padding: 14px 20px;
  border-radius: var(--r-btn);
  border: none;
  background: var(--panel-strong);
  color: var(--muted);
  font-weight: 500;
  cursor: pointer;
  transition: 0.3s;
}

.settings-sidebar button.active {
  background: #3b82f6;
  color: white;
  box-shadow: 0 10px 15px rgba(59, 130, 246, 0.2);
}

.card {
  animation: slideIn 0.4s ease-out;
}

.card-title {
  margin: 0 0 25px;
  font-size: 1.4rem;
  color: var(--text);
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: var(--text);
  font-size: 14px;
}

.form-group input, .form-group select, .form-group textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 16px;
  border: 1px solid var(--line);
  border-radius: var(--r-input);
  background: var(--panel-strong);
  color: var(--text);
  transition: 0.2s;
}

.form-group input:focus {
  border-color: #3b82f6;
  background: var(--panel);
  outline: none;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-row, .form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-footer {
  margin-top: 30px;
}

.btn-primary {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 12px 28px;
  border-radius: var(--r-btn);
  cursor: pointer;
  font-weight: 600;
  transition: background 0.2s, transform 0.2s, box-shadow 0.2s;
}

.btn-primary:hover {
  background: #2563eb;
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.3);
}

.btn-danger {
  background: #fee2e2;
  color: #ef4444;
  border: none;
  padding: 12px 28px;
  border-radius: var(--r-btn);
  cursor: pointer;
  font-weight: 600;
  transition: background 0.2s, transform 0.2s;
}

.btn-danger:hover {
  background: #fecaca;
  transform: translateY(-2px);
}

/* 遮罩层通用样式 */
.picker-mask,
.cropper-mask,
.preview-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.picker-dialog {
  background: var(--panel);
  width: 600px;
  border-radius: var(--r-dialog);
  padding: 30px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.bg-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

.bg-item {
  aspect-ratio: 16/9;
  border-radius: 12px;
  background-size: cover;
  cursor: pointer;
  position: relative;
  border: 3px solid transparent;
  transition: 0.2s;
}

.bg-item.selected {
  border-color: #3b82f6;
}

.selected-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: #3b82f6;
  color: white;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.custom-url input {
  width: 100%;
  margin-top: 8px;
}

.picker-footer {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 25px;
}

.btn-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: var(--muted);
}

.btn-close:hover {
  color: var(--text);
}

.btn-cancel {
  background: var(--panel-strong);
  border: none;
  padding: 10px 20px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  color: var(--text);
}

.btn-cancel:hover {
  background: var(--line);
}

/* 裁剪弹窗 - 左右结构 */
.cropper-dialog-split {
  display: flex;
  width: 1100px;
  max-width: 90vw;
  background: var(--panel);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

/* 左侧 */
.cropper-left {
  flex: 2;
  padding: 24px;
  border-right: 1px solid var(--line);
}

.cropper-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.cropper-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: var(--text);
}

.cropper-container {
  height: 400px;
  background: var(--panel-strong);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
}

.cropper-tools {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.tool-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: var(--panel);
  color: var(--text);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tool-btn:hover {
  background: var(--panel-strong);
  border-color: var(--muted);
}

.tool-icon {
  font-size: 18px;
  font-weight: bold;
}

.size-limit {
  display: block;
  text-align: center;
  margin-top: 12px;
  font-size: 12px;
  color: #ef4444;
}

/* 右侧 */
.cropper-right {
  flex: 1;
  padding: 24px;
  background: var(--panel-strong);
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.preview-section h4 {
  margin: 0 0 16px;
  font-size: 1rem;
  color: var(--muted);
}

.preview-box-large {
  width: 100%;
  aspect-ratio: 1/1;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid #fff;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
  margin-bottom: 16px;
}

.preview-img-large {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-info {
  background: var(--panel);
  border-radius: 12px;
  padding: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  color: var(--muted);
  font-size: 14px;
}

.info-value {
  font-weight: 600;
  color: var(--text);
  font-size: 14px;
}

.action-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-btn {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.action-btn-cancel {
  background: var(--panel-strong);
  color: var(--muted);
}

.action-btn-cancel:hover {
  background: var(--line);
  transform: translateY(-2px);
}

.action-btn-crop {
  background: #3b82f6;
  color: white;
}

.action-btn-crop:hover {
  background: #2563eb;
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.3);
}

.action-btn-upload {
  background: #67c23a;
  color: white;
}

.action-btn-upload:hover:not(:disabled) {
  background: #529b2e;
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(103, 194, 58, 0.3);
}

.action-btn-disabled {
  background: #e4e7ed;
  color: #c0c4cc;
  cursor: not-allowed;
}

.action-btn-disabled:hover {
  transform: none;
  box-shadow: none;
}

.btn-icon {
  font-size: 18px;
}

/* 头像预览弹窗 */
.preview-dialog {
  background: var(--panel);
  width: 600px;
  max-width: 90vw;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--line);
}

.preview-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: var(--text);
}

.preview-body {
  padding: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--panel-strong);
}

.preview-large-img {
  max-width: 100%;
  max-height: 400px;
  border-radius: 30px;
  object-fit: contain;
}

.preview-footer {
  padding: 20px 24px;
  border-top: 1px solid var(--line);
  display: flex;
  justify-content: flex-end;
}

.required-mark {
  color: #ef4444;
  margin-left: 4px;
}

/* 输入提示 */
.input-hint {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

:deep(.el-input__wrapper) {
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: var(--panel-strong);
  box-shadow: none;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #ef4444;
  box-shadow: 0 0 0 1px #ef4444;
}

:deep(.el-input__inner) {
  height: 44px;
  color: var(--text);
  background: transparent;
}

/* 修改密码表单组样式 */
.card .form-group:last-of-type {
  margin-bottom: 0;
}

/* 按钮容器 */
.form-footer {
  margin-top: 30px;
}

/* 危险按钮样式优化 */
.btn-danger {
  background: #fee2e2;
  color: #ef4444;
  border: none;
  padding: 12px 28px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.2s, transform 0.2s;
  width: 100%;
  font-size: 16px;
}

.btn-danger:hover:not(:disabled) {
  background: #fecaca;
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(239, 68, 68, 0.2);
}

.btn-danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 输入框焦点状态优化 */
.form-group input:focus {
  border-color: #ef4444;
  background: var(--panel);
  outline: none;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
