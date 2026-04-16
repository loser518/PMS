<script setup>
import {computed, nextTick, onBeforeUnmount, onMounted, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import {useAuthStore} from "../stores/auth";
import {getLabel, ROLE_OPTIONS} from "../utils/constants";
import {ElNotification, ElMessage, ElMessageBox} from "element-plus";
import * as Icons from '@element-plus/icons-vue';
import axios from "axios";
import {notificationApi} from "../api/modules";
import http from "../api/http";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const currentTime = ref(new Date());
const weather = ref({temperature: "", text: ""});
const location = ref("");
const citySearchInput = ref("");
const showCitySearch = ref(false);
const searchResults = ref([]);
const searching = ref(false);
const WEATHER_KEY = '9fd76659260d44e5a3f038ab252d7159';

// 通知相关
const unreadCount = ref(0);
const showNotificationPanel = ref(false);
const notifications = ref([]);
const notificationsLoading = ref(false);

async function loadUnreadCount() {
  try {
    const res = await notificationApi.countUnread();
    unreadCount.value = res.data ?? 0;
  } catch {
  }
}

async function loadNotifications() {
  notificationsLoading.value = true;
  try {
    const res = await notificationApi.list(1, 15);
    notifications.value = res.list || [];
  } catch {
    ElMessage.error("加载通知失败");
  } finally {
    notificationsLoading.value = false;
  }
}

async function openNotifications() {
  showNotificationPanel.value = true;
  await loadNotifications();
}

async function handleMarkAllRead() {
  await notificationApi.markAllRead();
  notifications.value.forEach(n => n.isRead = 1);
  unreadCount.value = 0;
  ElMessage.success("全部已读");
}

async function handleClearAll() {
  await ElMessageBox.confirm('确定要清空所有通知吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  });
  await notificationApi.clearAll();
  notifications.value = [];
  unreadCount.value = 0;
  ElMessage.success("已清空全部通知");
}

async function handleMarkRead(notification) {
  if (notification.isRead === 0) {
    await notificationApi.markRead(notification.id);
    notification.isRead = 1;
    if (unreadCount.value > 0) unreadCount.value--;
  }
}

const notificationTypeLabels = {
  1: '课题通过', 2: '课题驳回', 3: '课题需修改',
  4: '进度已阅', 5: '进度退回', 6: '公告发布',
  7: '好友申请', 8: '好友通过', 9: '系统消息'
};
const notificationTypeColors = {
  1: 'success', 2: 'danger', 3: 'warning',
  4: 'success', 5: 'warning', 6: 'info',
  7: 'primary', 8: 'success', 9: 'info'
};

// ========================= 全局 WebSocket（通知推送） =========================
let notifyWs = null;

function connectNotifyWs() {
  if (!authStore.token) return;
  // 注意：WS 地址与后端 IMServer 的 ws.port 一致
  const wsUrl = import.meta.env.VITE_WS_URL ? `${import.meta.env.VITE_WS_URL}/im` : `ws://127.0.0.1:9091/im`;
  notifyWs = new WebSocket(wsUrl);

  notifyWs.onopen = () => {
    // 第一帧发送 token 认证
    notifyWs.send(JSON.stringify({
      code: 100,
      content: `Bearer ${authStore.token}`
    }));
  };

  notifyWs.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      if (data.type === 'notification') {
        handleNotifyWsMessage(data.data);
      }
    } catch (e) {
      // 忽略非 JSON 或不关心的消息
    }
  };

  notifyWs.onclose = () => {
    // 非手动关闭时自动重连（5秒延迟）
    if (notifyWs) {
      setTimeout(connectNotifyWs, 5000);
    }
  };

  notifyWs.onerror = () => {};
}

function handleNotifyWsMessage(payload) {
  if (!payload) return;
  const {type, title, content} = payload;

  // 1. 未读数 +1
  unreadCount.value++;

  // 2. 弹出桌面通知
  ElNotification({
    title: title || '新通知',
    message: content || '您有一条新消息',
    type: notificationTypeColors[type] || 'info',
    duration: 4000,
    position: 'top-right'
  });

  // 3. 如果通知面板已打开，刷新列表
  if (showNotificationPanel.value) {
    loadNotifications();
  }
}

function disconnectNotifyWs() {
  if (notifyWs) {
    notifyWs.close();
    notifyWs = null;
  }
}

const avatarPath = (import.meta.env.VITE_MINIO_BASE_URL || 'http://127.0.0.1:9000/pms-bucket') + '/';
const isCollapse = ref(false);
const screenWidth = ref(window.innerWidth);
// 手机端侧边栏遮罩层
const showMobileOverlay = ref(false);
const isMobile = computed(() => screenWidth.value < 768);

// -------- 明暗主题切换 --------
const isDark = ref(localStorage.getItem('pms-theme') === 'dark');

function applyTheme(dark) {
  if (dark) {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
}

function toggleTheme() {
  isDark.value = !isDark.value;
  // 只在切换瞬间加过渡类，400ms 后移除，避免平时全局 transition 拖累性能
  document.body.classList.add('theme-transitioning');
  applyTheme(isDark.value);
  localStorage.setItem('pms-theme', isDark.value ? 'dark' : 'light');
  setTimeout(() => {
    document.body.classList.remove('theme-transitioning');
  }, 400);
}

// 初始化时恢复主题
applyTheme(isDark.value);

const handleResize = () => {
  screenWidth.value = window.innerWidth;
  // 屏幕小于 1024px 自动折叠
  isCollapse.value = screenWidth.value < 1024;
};

const toggleSidebar = () => {
  if (isMobile.value) {
    // 手机端：切换侧边栏显示/隐藏
    showMobileOverlay.value = !showMobileOverlay.value;
  } else {
    // PC端：切换折叠/展开
    isCollapse.value = !isCollapse.value;
  }
};

const closeMobileSidebar = () => {
  showMobileOverlay.value = false;
};

const menus = computed(() => [
  {label: "系统首页", path: "/dashboard", icon: "Odometer"},
  {label: "学生档案", path: "/users", roles: [1, 2], icon: "User"},
  {label: "课题申报", path: "/projects", icon: "DocumentAdd"},
  // {label: "互动社区", path: "/project-comments", icon: "ChatLineRound"},
  {label: "进度追踪", path: "/progress", icon: "Timer"},
  {label: "课题类型", path: "/project-types", icon: "Collection"},
  {label: "通知公告", path: "/announcements", icon: "Notification"},
  {label: "分类维护", path: "/categories", roles: [2], icon: "Files"},
  {label: "好友聊天", path: "/chat", icon: "ChatDotRound"},
  // {label: "选题市场", path: "/topic-market", icon: "ShoppingCart"},
  {label: "智能客服", path: "/ai-service", icon: "Service"},
  {label: "个人中心", path: "/profile", icon: "Postcard"},
].filter((item) => !item.roles || item.roles.includes(authStore.role)));

const timeDisplay = computed(() => {
  const d = currentTime.value;
  return {
    time: d.toLocaleTimeString('zh-CN', {hour: '2-digit', minute: '2-digit', second: '2-digit'}),
    date: d.toLocaleDateString('zh-CN', {month: 'long', day: 'numeric', weekday: 'short'})
  };
});


const getWeatherIcon = (text) => {
  const map = {
    "晴": "☀️", "多云": "⛅", "阴": "☁️", "小雨": "🌧️", "中雨": "🌧️",
    "大雨": "🌧️", "暴雨": "🌧️", "雷阵雨": "⛈️", "雪": "❄️", "雾": "🌫️",
    "霾": "💨", "雾霾": "🌫️💨"
  }
  return map[text] || '';
};

async function searchCity() {
  if (!citySearchInput.value) return;
  searching.value = true;
  try {
    const res = await axios.get('https://k27p43b5u3.re.qweatherapi.com/geo/v2/city/lookup', {
      params: {location: citySearchInput.value, range: 'cn'},
      headers: {'X-QW-Api-Key': WEATHER_KEY}
    });
    searchResults.value = res.data.location || [];
  } catch (err) {
    ElMessage.error("城市搜索失败");
  } finally {
    searching.value = false;
  }
}

function handleKeydown(event) {
  if (event.key === 'Escape') {
    toggleSidebar();
  }
}

async function selectCity(city) {
  try {
    const res = await axios.get('https://k27p43b5u3.re.qweatherapi.com/v7/weather/now', {
      params: {location: city.id},
      headers: {'X-QW-Api-Key': WEATHER_KEY}
    });
    const now = res.data.now;
    weather.value = {temperature: now.temp, text: now.text};
    location.value = city.name;
    localStorage.setItem('preferred_city', JSON.stringify({id: city.id, name: city.name}));
    showCitySearch.value = false;
    ElNotification({
      title: '天气已更新',
      message: `${location.value} ${weather.value.text} ${weather.value.temperature}°C`,
      type: 'success'
    });
  } catch (err) {
    ElMessage.error("获取天气失败");
  }
}

const handleLogout = async () => {
  await authStore.logout();
  // localStorage.removeItem('preferred_city');
  ElNotification({title: '退出成功', message: '期待您的下次登录', type: 'success'});
  await router.push("/login");
};

let timer = null;
let notifyTimer = null;

// ── 同步AI头像更新 ──
function handleAvatarUpdate(event) {
  const newUrl = event.detail || localStorage.getItem(AI_AVATAR_KEY);
  if (newUrl && newUrl !== aiAvatarUrl.value) {
    aiAvatarUrl.value = newUrl;
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown);
  window.addEventListener('resize', handleResize);
  // 监听AI头像更新事件
  window.addEventListener('ai-avatar-updated', handleAvatarUpdate);
  // 监听 localStorage 变化（多标签页情况）
  window.addEventListener('storage', (e) => {
    if (e.key === AI_AVATAR_KEY) {
      aiAvatarUrl.value = e.newValue || defaultAiAvatar;
    }
  });
  handleResize();
  timer = setInterval(() => {
    currentTime.value = new Date();
  }, 1000);
  // 每 30 秒轮询一次未读通知数
  loadUnreadCount();
  notifyTimer = setInterval(loadUnreadCount, 30000);
  // 建立全局 WebSocket 连接（即时通知推送）
  connectNotifyWs();
  const saved = localStorage.getItem('preferred_city');
  if (saved) selectCity(JSON.parse(saved));
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown);
  window.removeEventListener('resize', handleResize);
  window.removeEventListener('ai-avatar-updated', handleAvatarUpdate);
  clearInterval(timer);
  clearInterval(notifyTimer);
  disconnectNotifyWs();
  // 关闭悬浮客服的 SSE 连接
  if (floatEventSource) {
    floatEventSource.close();
    floatEventSource = null;
  }
});

// ===================== 右下角悬浮智能客服 =====================
const floatOpen = ref(false);
const floatMessages = ref([]);
const floatInput = ref("");
const floatLoading = ref(false);
const floatBodyRef = ref(null);
const floatSessionId = ref("");
let floatEventSource = null;

const FLOAT_SESSION_KEY = "pms_float_ai_session";
const FLOAT_WELCOME = "👋 你好！我是智能客服**小P**，有什么可以帮到你？";

// AI头像URL（与AiServiceView.vue共用）
const AI_AVATAR_KEY = 'pms_ai_avatar_url';
const defaultAiAvatar = 'https://p3-pc-sign.douyinpic.com/tos-cn-i-0813c000-ce/ogAiAnIdwAA10dMhAEO6baDGPFw1lX2BjpaPi~tplv-dy-aweme-images:q75.webp?biz_tag=aweme_images&from=327834062&lk3s=138a59ce&s=PackSourceEnum_SEARCH&sc=image&se=false&x-expires=1776286800&x-signature=%2B1sBLX32yx4qb4DApo2Tz%2BvSS3s%3D';
const aiAvatarUrl = ref(localStorage.getItem(AI_AVATAR_KEY) || defaultAiAvatar);

function getFloatSession() {
  let sid = sessionStorage.getItem(FLOAT_SESSION_KEY);
  if (!sid) {
    sid = "float-" + Date.now() + "-" + Math.random().toString(36).slice(2);
    sessionStorage.setItem(FLOAT_SESSION_KEY, sid);
  }
  return sid;
}

function toggleFloatChat() {
  floatOpen.value = !floatOpen.value;
  if (floatOpen.value && floatMessages.value.length === 0) {
    floatSessionId.value = getFloatSession();
    loadFloatHistory();
  }
}

async function loadFloatHistory() {
  try {
    const res = await http.get("/ai/customer/history", {
      params: {sessionId: floatSessionId.value, limit: 50}
    });
    const history = res || [];
    if (history.length > 0) {
      floatMessages.value = history
          .map(m => ({role: m.role === "user" ? "user" : "assistant", content: m.content || ""}))
          .filter(m => m.content.trim());
    } else {
      floatMessages.value = [{role: "assistant", content: FLOAT_WELCOME}];
    }
  } catch {
    floatMessages.value = [{role: "assistant", content: FLOAT_WELCOME}];
  }
  floatScrollToBottom();
}

function floatScrollToBottom() {
  nextTick(() => {
    if (floatBodyRef.value) floatBodyRef.value.scrollTop = floatBodyRef.value.scrollHeight;
  });
}

// 简单 Markdown 渲染（同 AiServiceView）
function floatRenderMd(text) {
  if (!text) return "";
  return text
      .replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")
      .replace(/\*(.*?)\*/g, "<em>$1</em>")
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/\n/g, "<br/>");
}

async function sendFloatMessage() {
  const text = floatInput.value.trim();
  if (!text || floatLoading.value) return;
  floatMessages.value.push({role: "user", content: text});
  floatInput.value = "";
  floatScrollToBottom();

  floatMessages.value.push({role: "assistant", content: ""});
  const aiIdx = floatMessages.value.length - 1;
  floatLoading.value = true;

  if (floatEventSource) {
    floatEventSource.close();
    floatEventSource = null;
  }

  const rawToken = authStore.token?.replace(/^Bearer\s+/i, "") || "";
  const url = `/api/ai/customer/chat?sessionId=${encodeURIComponent(floatSessionId.value)}&message=${encodeURIComponent(text)}&token=${encodeURIComponent(rawToken)}`;

  try {
    const es = new EventSource(url);
    floatEventSource = es;
    es.onmessage = (ev) => {
      if (ev.data && ev.data !== "[DONE]") {
        floatMessages.value[aiIdx].content += ev.data;
        floatScrollToBottom();
      }
    };
    es.addEventListener("complete", () => {
      es.close();
      floatEventSource = null;
      floatLoading.value = false;
      floatScrollToBottom();
    });
    es.onerror = () => {
      const hasContent = floatMessages.value[aiIdx].content.trim().length > 0;
      es.close();
      floatEventSource = null;
      floatLoading.value = false;
      if (!hasContent) floatMessages.value[aiIdx].content = "⚠️ 连接失败，请稍后重试。";
      floatScrollToBottom();
    };
  } catch (e) {
    floatMessages.value[aiIdx].content = "⚠️ 请求失败：" + e.message;
    floatLoading.value = false;
    floatScrollToBottom();
  }
}

function handleFloatKeydown(e) {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    sendFloatMessage();
  }
}

function goToFullAiService() {
  floatOpen.value = false;
  router.push("/ai-service");
}
</script>

<template>
  <el-container class="app-shell">
    <!-- 手机端遮罩层 -->
    <transition name="fade">
      <div v-if="showMobileOverlay" class="mobile-overlay" @click="closeMobileSidebar"></div>
    </transition>

    <el-aside :width="isCollapse ? '90px' : '260px'" class="glass-aside" :class="{ 'mobile-open': showMobileOverlay }">
<!--      &lt;!&ndash; 手机端关闭按钮 &ndash;&gt;-->
<!--      <button class="mobile-close-btn" @click="closeMobileSidebar">-->
<!--        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">-->
<!--          <line x1="18" y1="6" x2="6" y2="18"/>-->
<!--          <line x1="6" y1="6" x2="18" y2="18"/>-->
<!--        </svg>-->
<!--      </button>-->

      <!-- 顶部品牌区域 - 固定 -->
      <div class="brand-box">
        <div class="brand-logo">
          <div v-if="isCollapse && !isMobile" class="mini-logo">P</div>
          <div v-else-if="isMobile && !showMobileOverlay" class="mini-logo">P</div>
          <span v-else class="logo-text">PMS 课题管理</span>
        </div>
      </div>

      <!-- 中间菜单区域 - 可滚动 -->
      <div class="menu-wrapper">
        <el-scrollbar>
          <el-menu
              :default-active="route.path"
              router
              class="nav-menu"
              :collapse="isCollapse"
              :collapse-transition="false"
          >
            <el-menu-item v-for="item in menus" :key="item.path" :index="item.path" @click="isMobile && closeMobileSidebar()">
              <el-icon class="menu-icon">
                <component :is="Icons[item.icon]"/>
              </el-icon>
              <template #title>
                <span class="menu-label">{{ item.label }}</span>
              </template>
            </el-menu-item>
          </el-menu>
        </el-scrollbar>
      </div>

      <!-- 底部时间区域 - 固定 -->
      <div class="aside-footer" v-if="!isCollapse">
        <div class="current-date-card">
          <div class="d-day">{{ timeDisplay.date }}</div>
          <div class="d-time">{{ timeDisplay.time }}</div>
        </div>
      </div>
      <!-- 折叠时的迷你时间 -->
      <div class="aside-footer-collapsed" v-else>
        <div class="mini-time">{{ timeDisplay.time }}</div>
      </div>
    </el-aside>

    <el-container class="main-body">
      <el-header class="glass-header">
        <div class="header-left">
          <el-button type="text" @click="toggleSidebar" class="toggle-btn">
            <el-icon>
              <component :is="isCollapse ? Icons.Expand : Icons.Fold"/>
            </el-icon>
          </el-button>
          <div class="title-group">
            <h2 class="page-title">{{ route.meta?.title || route.name || '概览' }}</h2>
          </div>
        </div>

        <!-- 天气胶囊 - 始终居中显示 -->
        <div class="header-center">
          <el-popover placement="bottom" :width="300" trigger="click" v-model:visible="showCitySearch">
            <template #reference>
              <div v-if="weather" class="weather-widget">
                <span class="w-icon">{{ getWeatherIcon(weather.text) }}</span>
                <span class="w-info">{{ weather.text }} · {{ weather.temperature }}°C</span>
                <span class="w-loc"><el-icon><LocationInformation/></el-icon> {{ location }}</span>
              </div>
              <div v-else class="weather-widget">
                <span class="w-info">请选择你所在城市</span>
              </div>
            </template>
            <div class="search-panel">
              <el-input v-model="citySearchInput" placeholder="搜索城市..." @keyup.enter="searchCity">
                <template #suffix>
                  <el-icon v-if="searching" class="is-loading">
                    <Loading/>
                  </el-icon>
                  <el-icon v-else @click="searchCity" style="cursor:pointer">
                    <Search/>
                  </el-icon>
                </template>
              </el-input>
              <div class="res-list">
                <div v-for="c in searchResults" :key="c.id" class="res-item" @click="selectCity(c)">
                  {{ c.adm2 }} / {{ c.name }}
                </div>
              </div>
            </div>
          </el-popover>
        </div>

        <div class="header-right">

          <!-- 主题切换 -->
          <el-tooltip :content="isDark ? '浅色模式' : '深色模式'" placement="bottom">
            <el-button circle class="notify-btn" @click="toggleTheme">
              <el-icon>
                <component :is="isDark ? Icons.Sunny : Icons.Moon"/>
              </el-icon>
            </el-button>
          </el-tooltip>

          <!-- 通知铃铛 -->
          <el-tooltip content="消息通知">
            <el-badge :value="unreadCount" :max="99" :hidden="unreadCount === 0" class="notify-badge">
              <el-button circle @click="openNotifications" class="notify-btn">
                <el-icon>
                  <component :is="Icons.Bell"/>
                </el-icon>
              </el-button>
            </el-badge>
          </el-tooltip>

          <el-dropdown trigger="click">
            <div class="user-pill">
              <div class="user-text hidden-mobile">
                <span class="u-name">{{ authStore.userInfo?.user?.nickname }}</span>
                <span class="u-role">{{ getLabel(ROLE_OPTIONS, authStore.role) }}</span>
              </div>
              <el-avatar :size="40" :src="avatarPath + authStore.userInfo?.user?.avatar"
                         class="u-avatar">
                <img src="https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png"/>
              </el-avatar>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :icon="Icons.User" @click="router.push('/profile')">个人档案</el-dropdown-item>
                <el-dropdown-item divided :icon="Icons.SwitchButton" @click="handleLogout" style="color: #f56c6c">
                  退出系统
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="page-transform" mode="out-in">
            <div :key="route.path" class="view-card">
              <component :is="Component"/>
            </div>
          </transition>
        </router-view>
      </el-main>
    </el-container>


    <!-- ========== 右下角悬浮智能客服 ========== -->
    <!-- 悬浮按钮 -->
    <transition name="float-btn">
      <button class="float-ai-btn" @click="toggleFloatChat" :class="{ open: floatOpen }"
              :title="floatOpen ? '收起客服' : '智能客服'">
        <svg v-if="!floatOpen" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18"/>
          <line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
      </button>
    </transition>

    <!-- 弹出聊天窗 -->
    <transition name="float-panel">
      <div v-if="floatOpen" class="float-chat-panel">
        <!-- 窗口头部 -->
        <div class="float-header">
          <div class="float-avatar"><el-avatar
              :size="38"
              :src="aiAvatarUrl"
          >
          </el-avatar></div>
          <div class="float-header-info">
            <div class="float-title">小P · 智能客服</div>
            <div class="float-sub">PMS 系统全天在线</div>
          </div>
          <button class="float-expand-btn" @click="goToFullAiService" title="在完整页面打开">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15 3 21 3 21 9"/>
              <polyline points="9 21 3 21 3 15"/>
              <line x1="21" y1="3" x2="14" y2="10"/>
              <line x1="3" y1="21" x2="10" y2="14"/>
            </svg>
          </button>
          <button class="float-close-btn" @click="floatOpen = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <!-- 消息区 -->
        <div class="float-body" ref="floatBodyRef">
          <div
              v-for="(msg, idx) in floatMessages"
              :key="idx"
              class="float-msg-row"
              :class="msg.role === 'user' ? 'float-msg-user' : 'float-msg-ai'"
          >
            <div v-if="msg.role === 'assistant'" class="float-msg-avatar"><el-avatar
                :size="35"
                :src="aiAvatarUrl"
            >
            </el-avatar></div>
            <div
                class="float-bubble"
                :class="msg.role === 'user' ? 'float-bubble-user' : 'float-bubble-ai'"
                v-html="msg.role === 'assistant' ? (floatRenderMd(msg.content) || '<span class=\'typing-cursor\'></span>') : msg.content"
            ></div>
            <div v-if="msg.role === 'user'" class="float-user-avatar">
              <el-avatar :size="28" :src="avatarPath + authStore.userInfo?.user?.avatar">
                <img src="https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png"/>
              </el-avatar>
            </div>
          </div>
          <!-- 加载中 -->
          <div v-if="floatLoading && floatMessages[floatMessages.length-1]?.content === ''" class="float-typing">
            <span class="fdot"></span><span class="fdot"></span><span class="fdot"></span>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="float-footer">
          <textarea
              v-model="floatInput"
              class="float-input"
              placeholder="输入问题，Enter 发送..."
              rows="2"
              :disabled="floatLoading"
              @keydown="handleFloatKeydown"
          ></textarea>
          <button
              class="float-send-btn"
              :disabled="!floatInput.trim() || floatLoading"
              @click="sendFloatMessage"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"/>
              <polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
          </button>
        </div>
      </div>
    </transition>

    <!-- 通知抽屉 -->
    <el-drawer v-model="showNotificationPanel" title="消息通知" direction="rtl" size="380px">
      <template #header>
        <div class="notify-drawer-header">
          <span class="notify-title">消息通知</span>
          <div>
            <el-button type="danger" link size="small" @click="handleClearAll" :disabled="notifications.length === 0">清空</el-button>
            <el-button type="primary" link size="small" @click="handleMarkAllRead">全部已读</el-button>
          </div>
        </div>
      </template>

      <div v-loading="notificationsLoading">
        <div v-if="notifications.length === 0" class="notify-empty">
          <el-icon style="font-size:48px;color:#c0c4cc">
            <component :is="Icons.Bell"/>
          </el-icon>
          <p>暂无通知</p>
        </div>
        <div v-for="item in notifications" :key="item.id"
             class="notify-item" :class="{ unread: item.isRead === 0 }"
             @click="handleMarkRead(item)">
          <el-tag :type="notificationTypeColors[item.type] || 'info'" size="small" class="notify-type-tag">
            {{ notificationTypeLabels[item.type] || '通知' }}
          </el-tag>
          <div class="notify-body">
            <div class="notify-item-title">{{ item.title }}</div>
            <div class="notify-item-content">{{ item.content }}</div>
            <div class="notify-item-time">{{ item.createTime }}</div>
          </div>
          <div v-if="item.isRead === 0" class="unread-dot"></div>
        </div>
      </div>
    </el-drawer>
  </el-container>
</template>

<style scoped>
/* 核心布局 */
.app-shell {
  scroll-behavior: smooth;
  height: calc(100vh);
  background-color: #f6f8fa;
  padding: 16px;
  gap: 16px;
  overflow: hidden;
}

.glass-aside {
  background: #333333;
  border-radius: var(--r-lg);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.12);
  height: 100%; /* 确保占满父容器高度 */
  overflow: hidden; /* 防止整体溢出 */
}

/* 顶部品牌区域 - 固定 */
.brand-box {
  padding: 32px 20px;
  height: 100px; /* 固定高度 */
  flex-shrink: 0; /* 防止被压缩 */
  display: flex;
  align-items: center;
}

/* 中间菜单区域 - 可滚动 */
.menu-wrapper {
  flex: 1; /* 占据剩余空间 */
  min-height: 0; /* 重要：允许flex子项收缩 */
  padding: 0 12px;
  overflow: hidden; /* 配合el-scrollbar使用 */
}

/* 隐藏侧边导航滚动条 */
.menu-wrapper .el-scrollbar__wrap {
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-width: none;       /* Firefox */
  -ms-overflow-style: none;    /* IE/Edge */
}
.menu-wrapper .el-scrollbar__wrap::-webkit-scrollbar {
  display: none;               /* Chrome/Safari/Opera */
}

/* 底部时间区域 - 固定 */
.aside-footer {
  padding: 24px;
  flex-shrink: 0;
}

/* 折叠时的底部时间 */
.aside-footer-collapsed {
  padding: 16px 0;
  flex-shrink: 0;
  text-align: center;
}

.mini-time {
  font-family: monospace;
  font-weight: 600;
  color: #ae5d30;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.05);
  padding: 8px 0;
  margin: 0 12px;
  border-radius: var(--r-tag);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.mini-logo {
  width: 40px;
  height: 40px;
  background: #ae5d30;
  border-radius: var(--r-tag);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 20px;
  margin: 0 auto;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  white-space: nowrap;
}

/* 菜单样式 */
.nav-menu {
  border-right: none !important;
  background: transparent !important;
}

:deep(.el-menu-item) {
  height: 54px;
  margin-bottom: 4px;
  border-radius: var(--r-tag);
  color: rgba(255, 255, 255, 0.7) !important;
}

:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1) !important;
  color: #fff !important;
}

:deep(.el-menu-item.is-active) {
  background: #ae5d30 !important;
  color: #fff !important;
  box-shadow: 0 10px 20px rgba(174, 93, 48, 0.3);
}

/* 底部时间卡片 */
.current-date-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: var(--r-card);
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.d-day {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 4px;
}

.d-time {
  font-family: monospace;
  font-weight: 600;
  color: #ae5d30;
  font-size: 16px;
}

/* 顶部栏：毛玻璃 */
.glass-header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-radius: var(--r-lg);
  height: 72px !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  margin-bottom: 8px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.toggle-btn {
  font-size: 22px;
  color: #2d1b14;
  padding: 8px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  color: #1a1a1a;
}

/* 天气部件 - 居中显示 */
.header-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  z-index: 10;
}

.weather-widget {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  padding: 8px 20px;
  border-radius: 50px;
  border: 1px solid #eee;
  cursor: pointer;
  transition: border-color 0.2s, transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.weather-widget:hover {
  border-color: #ae5d30;
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(174, 93, 48, 0.12);
}

.w-icon {
  font-size: 20px;
}

.w-info {
  font-size: 14px;
  font-weight: 600;
  color: #2d1b14;
}

.w-loc {
  font-size: 13px;
  color: #ae5d30;
  display: flex;
  align-items: center;
  gap: 4px;
  padding-left: 8px;
  border-left: 1px solid #eee;
}

.header-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.user-pill {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 4px 4px 16px;
  background: #f1f3f5;
  border-radius: 50px;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
}

.user-pill:hover {
  background: #e9ecef;
  transform: translateY(-1px);
}

.user-text {
  text-align: right;
  line-height: 1.2;
}

.u-name {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #2d1b14;
}

.u-role {
  font-size: 11px;
  color: #ae5d30;
  font-weight: 600;
}

.u-avatar {
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

@media (max-width: 1024px) {
  .hidden-tablet {
    display: none;
  }

  .weather-widget {
    padding: 6px 16px;
    gap: 8px;
  }

  .w-info {
    font-size: 13px;
  }

  .w-loc {
    font-size: 12px;
  }
}

@media (max-width: 768px) {
  .app-shell {
    padding: 8px;
    gap: 8px;
  }

  .glass-header {
    padding: 0 12px;
    height: 60px !important;
  }

  .hidden-mobile {
    display: none;
  }

  .glass-aside {
    position: fixed;
    height: 100vh;
    top: 0;
    left: 0;
    z-index: 2001;
    border-radius: 0;
    transform: translateX(-100%);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    padding-top: 0;
  }

  .glass-aside.mobile-open {
    transform: translateX(0);
  }

  /* 手机端遮罩层 */
  .mobile-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 2000;
    backdrop-filter: blur(2px);
  }

  .fade-enter-active, .fade-leave-active {
    transition: opacity 0.25s;
  }
  .fade-enter-from, .fade-leave-to {
    opacity: 0;
  }

  /* 手机端关闭按钮 */
  .mobile-close-btn {
    position: absolute;
    top: 12px;
    right: 12px;
    width: 32px;
    height: 32px;
    border: none;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.7);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 10;
    transition: background 0.15s;
  }

  .mobile-close-btn:hover {
    background: rgba(255, 255, 255, 0.2);
  }

  .mobile-close-btn svg {
    width: 16px;
    height: 16px;
  }

  /* 手机端菜单项增大点击区域 */
  ::deep(.el-menu-item) {
    height: 52px;
    font-size: 15px;
  }

  .weather-widget {
    padding: 6px 12px;
  }

  .w-loc {
    display: none;
  }

  .header-center {
    display: none; /* 手机端隐藏天气，居中靠左 */
  }

  /* 手机端悬浮按钮调整位置 */
  .float-ai-btn {
    right: 16px;
    bottom: 16px;
    width: 48px;
    height: 48px;
  }

  .float-chat-panel {
    right: 8px;
    bottom: 76px;
    width: calc(100vw - 16px);
    max-width: 360px;
    height: calc(100vh - 120px);
    max-height: 520px;
    border-radius: 16px;
  }

  /* 手机端通知抽屉全屏 */
  ::deep(.el-drawer) {
    width: 100% !important;
  }

  .glass-header .toggle-btn {
    display: flex; /* 手机端显示汉堡按钮 */
  }
}

/* 页面切换动画 - 只过渡 opacity 和 transform，GPU 加速，无卡顿 */
.page-transform-enter-active, .page-transform-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
  will-change: opacity, transform;
}

.page-transform-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-transform-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.res-list {
  margin-top: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.res-item {
  padding: 10px;
  cursor: pointer;
  border-radius: var(--r-tag);
  font-size: 13px;
}

.res-item:hover {
  background: #fdf5f2;
  color: #ae5d30;
}

.search-panel {
  padding: 8px;
}

/* 通知铃铛 */
.notify-badge {
  margin-right: 4px;
}

.notify-btn {
  background: #f1f3f5;
  border: none;
  color: #2d1b14;
  font-size: 18px;
  width: 40px;
  height: 40px;
}

.notify-btn:hover {
  background: #e9ecef;
  color: #ae5d30;
}

/* 通知抽屉内容 */
.notify-drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.notify-title {
  font-size: 16px;
  font-weight: 700;
}

.notify-empty {
  text-align: center;
  padding: 60px 0;
  color: #c0c4cc;
}

.notify-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f2f5;
  position: relative;
  transition: background 0.2s;
}

.notify-item:hover {
  background: #fdf5f2;
}

.notify-item.unread {
  background: #fff8f5;
}

.notify-type-tag {
  flex-shrink: 0;
  margin-top: 2px;
}

.notify-body {
  flex: 1;
  min-width: 0;
}

.notify-item-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.notify-item-content {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notify-item-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: #ae5d30;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}

/* ========== 右下角悬浮智能客服 ========== */
.float-ai-btn {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 3000;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: var(--panel);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.float-ai-btn:hover {
  transform: scale(1.08) translateY(-2px);
}

.float-ai-btn svg {
  width: 22px;
  height: 22px;
}

.float-btn-label {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1;
}

/* 弹窗入场动画 */
.float-btn-enter-active, .float-btn-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.float-btn-enter-from, .float-btn-leave-to {
  opacity: 0;
  transform: scale(0.6);
}

.float-panel-enter-active, .float-panel-leave-active {
  transition: opacity 0.22s, transform 0.22s;
}

.float-panel-enter-from, .float-panel-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

/* 聊天弹窗主体 */
.float-chat-panel {
  position: fixed;
  right: 28px;
  bottom: 96px;
  z-index: 2999;
  width: 360px;
  height: 520px;
  border-radius: 20px;
  background: var(--panel);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18), 0 4px 16px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--line);
}

/* 头部 */
.float-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: var(--brand);
  color: #fff;
  flex-shrink: 0;
}

.float-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.float-header-info {
  flex: 1;
  min-width: 0;
}

.float-title {
  font-size: 14px;
  font-weight: 700;
}

.float-sub {
  font-size: 11px;
  opacity: 0.85;
  margin-top: 1px;
}

.float-expand-btn, .float-close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.15s;
}

.float-expand-btn:hover, .float-close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.float-expand-btn svg, .float-close-btn svg {
  width: 14px;
  height: 14px;
}

/* 消息区 */
.float-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px 14px 8px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.float-body::-webkit-scrollbar {
  width: 4px;
}

.float-body::-webkit-scrollbar-thumb {
  background: var(--line);
  border-radius: 4px;
}

.float-msg-row {
  display: flex;
  align-items: flex-end;
  gap: 6px;
}

.float-msg-user {
  justify-content: flex-end;
}

.float-msg-ai {
  justify-content: flex-start;
}

.float-msg-avatar {
  width: 35px;
  height: 35px;
  border-radius: 50%;
  background: rgba(102, 126, 234, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.float-user-avatar {
  flex-shrink: 0;
}

.float-bubble {
  max-width: 76%;
  padding: 9px 13px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.65;
  word-break: break-word;
}

.float-bubble-ai {
  background: var(--panel);
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 14px 14px 14px 3px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.float-bubble-user {
  background: var(--brand);
  color: #fff;
  border-radius: 14px 14px 3px 14px;
}

.float-bubble code {
  background: rgba(0, 0, 0, 0.08);
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 12px;
}

/* 打字动画光标 */
.float-body :deep(.typing-cursor) {
  display: inline-block;
  width: 2px;
  height: 14px;
  background: var(--brand);
  animation: blink 1s infinite;
  vertical-align: middle;
  margin-left: 3px;
}

@keyframes blink {
  0%, 100% {
    opacity: 1
  }
  50% {
    opacity: 0
  }
}

/* 加载点 */
.float-typing {
  display: flex;
  align-items: center;
  gap: 5px;
  padding-left: 36px;
}

.fdot {
  width: 7px;
  height: 7px;
  background: var(--brand);
  border-radius: 50%;
  opacity: 0.5;
  animation: fbounce 1.2s infinite;
}

.fdot:nth-child(2) {
  animation-delay: 0.2s;
}

.fdot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes fbounce {
  0%, 60%, 100% {
    transform: translateY(0)
  }
  30% {
    transform: translateY(-6px)
  }
}

/* 输入区 */
.float-footer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid var(--line);
  background: var(--panel);
  flex-shrink: 0;
}

.float-input {
  flex: 1;
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 8px 12px;
  font-size: 13px;
  resize: none;
  outline: none;
  background: var(--panel-strong);
  color: var(--text);
  line-height: 1.5;
  transition: border-color 0.15s, box-shadow 0.15s;
  font-family: inherit;
}

.float-input:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(174, 93, 48, 0.1);
}

.float-send-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--brand);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.15s, opacity 0.15s;
}

.float-send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.float-send-btn:not(:disabled):hover {
  transform: scale(1.1);
}

.float-send-btn svg {
  width: 16px;
  height: 16px;
}
</style>

/* 暗色主题覆盖（不加 scoped，确保全局生效） */
<style>
html.dark .app-shell {
  background-color: #141414 !important;
}

html.dark .glass-header {
  background: rgba(28, 28, 32, 0.92) !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3) !important;
}

html.dark .glass-aside {
  background: #1e1e24 !important;
}

html.dark .page-title {
  color: #e8e0d8 !important;
}

html.dark .toggle-btn {
  color: #e8e0d8 !important;
}

html.dark .weather-widget {
  background: rgba(40, 40, 44, 0.95) !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  box-shadow: none !important;
}

html.dark .w-info {
  color: #e8e0d8 !important;
}

html.dark .w-loc {
  color: #d4784a !important;
  border-left-color: rgba(255, 255, 255, 0.12) !important;
}

html.dark .notify-btn {
  background: rgba(255, 255, 255, 0.08) !important;
  border-color: transparent !important;
  color: #e8e0d8 !important;
}

html.dark .notify-btn:hover {
  background: rgba(255, 255, 255, 0.14) !important;
  color: #d4784a !important;
}

/* 悬浮客服暗色适配 */
html.dark .float-chat-panel {
  border-color: rgba(255, 255, 255, 0.1) !important;
}

html.dark .float-bubble-ai {
  background: #252528 !important;
  color: #e8e0d8 !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
}

html.dark .float-body {
  background: #1a1a1e !important;
}

html.dark .float-input {
  background: rgba(255, 255, 255, 0.06) !important;
  color: #e8e0d8 !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
}

html.dark .float-input:focus {
  border-color: #d4784a !important;
  box-shadow: 0 0 0 3px rgba(212, 120, 74, 0.12) !important;
}

html.dark .float-bubble code {
  background: rgba(255, 255, 255, 0.1) !important;
}

html.dark .user-pill {
  background: rgba(255, 255, 255, 0.08) !important;
}

html.dark .user-pill:hover {
  background: rgba(255, 255, 255, 0.14) !important;
}

html.dark .u-name {
  color: #e8e0d8 !important;
}

html.dark .u-role {
  color: #d4784a !important;
}

html.dark .view-card {
  background: transparent !important;
}

html.dark .main-content {
  background: transparent !important;
}

html.dark .current-date-card {
  background: rgba(255, 255, 255, 0.04) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
}

html.dark .notify-item {
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

html.dark .notify-item:hover {
  background: rgba(212, 120, 74, 0.12) !important;
}

html.dark .notify-item.unread {
  background: rgba(212, 120, 74, 0.08) !important;
}

html.dark .notify-item-title {
  color: #e8e0d8 !important;
}

html.dark .notify-item-content {
  color: #9b8f84 !important;
}

html.dark .notify-drawer-header .notify-title {
  color: #e8e0d8 !important;
}

/* Element Plus 暗色覆盖 */
html.dark .el-table {
  --el-table-bg-color: #1e1e22;
  --el-table-tr-bg-color: #1e1e22;
  --el-table-header-bg-color: #252528;
  --el-table-border-color: rgba(255, 255, 255, 0.1);
  --el-table-text-color: #e8e0d8;
  --el-table-header-text-color: #b0a89e;
  --el-table-row-hover-bg-color: rgba(212, 120, 74, 0.1);
}

html.dark .el-card {
  --el-card-bg-color: #1e1e22;
  background-color: #1e1e22 !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  color: #e8e0d8 !important;
}

html.dark .el-card .el-card__header {
  border-bottom-color: rgba(255, 255, 255, 0.1) !important;
  color: #e8e0d8 !important;
}

html.dark .el-dialog {
  --el-dialog-bg-color: #1e1e22;
  --el-text-color-primary: #e8e0d8;
  background-color: #1e1e22 !important;
}

html.dark .el-dialog .el-dialog__title {
  color: #e8e0d8 !important;
}

html.dark .el-dialog .el-dialog__body {
  color: #e8e0d8 !important;
}

html.dark .el-drawer {
  background-color: #1e1e22 !important;
  color: #e8e0d8 !important;
}

html.dark .el-drawer__header {
  color: #e8e0d8 !important;
  border-bottom-color: rgba(255, 255, 255, 0.1) !important;
}

html.dark .el-input__wrapper {
  background-color: rgba(255, 255, 255, 0.06) !important;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.14) inset !important;
}

html.dark .el-input__inner {
  color: #e8e0d8 !important;
  background-color: transparent !important;
}

html.dark .el-textarea__inner {
  background-color: rgba(255, 255, 255, 0.06) !important;
  color: #e8e0d8 !important;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.14) inset !important;
}

html.dark .el-select__wrapper {
  background-color: rgba(255, 255, 255, 0.06) !important;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.14) inset !important;
  color: #e8e0d8 !important;
}

html.dark .el-select-dropdown {
  background-color: #252528 !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
}

html.dark .el-select-dropdown__item {
  color: #e8e0d8 !important;
}

html.dark .el-select-dropdown__item:hover,
html.dark .el-select-dropdown__item.is-hovering {
  background-color: rgba(212, 120, 74, 0.14) !important;
}

html.dark .el-select-dropdown__item.is-selected {
  color: #d4784a !important;
}

html.dark .el-form-item__label {
  color: #9b8f84 !important;
}

html.dark .el-button.is-plain,
html.dark .el-button--default {
  background-color: rgba(255, 255, 255, 0.07) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
  color: #e8e0d8 !important;
}

html.dark .el-button--default:hover {
  background-color: rgba(255, 255, 255, 0.12) !important;
  border-color: rgba(212, 120, 74, 0.5) !important;
  color: #d4784a !important;
}

html.dark .el-dropdown-menu {
  background-color: #252528 !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
}

html.dark .el-dropdown-menu__item {
  color: #e8e0d8 !important;
}

html.dark .el-dropdown-menu__item:hover {
  background-color: rgba(212, 120, 74, 0.12) !important;
  color: #d4784a !important;
}

html.dark .el-popover.el-popper {
  background-color: #252528 !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  color: #e8e0d8 !important;
}

html.dark .el-pagination {
  --el-pagination-bg-color: transparent;
  --el-pagination-button-color: #9b8f84;
  --el-text-color-primary: #e8e0d8;
  color: #9b8f84 !important;
}

html.dark .el-pagination button,
html.dark .el-pagination .el-pager li {
  background-color: rgba(255, 255, 255, 0.06) !important;
  color: #9b8f84 !important;
  border-color: transparent !important;
}

html.dark .el-pagination .el-pager li.is-active {
  background-color: #d4784a !important;
  color: #fff !important;
}

html.dark .el-descriptions__cell {
  background-color: #1e1e22 !important;
  color: #e8e0d8 !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
}

html.dark .el-descriptions__label {
  color: #9b8f84 !important;
}

html.dark .el-badge__content {
  border-color: #1a1a1e !important;
}

html.dark body,
html.dark {
  background: linear-gradient(145deg, #141414 0%, #1a1a1e 100%) !important;
  color: #e8e0d8;
}
</style>
