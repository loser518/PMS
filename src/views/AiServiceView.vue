<script setup>
import {ref, computed, nextTick, onMounted, onBeforeUnmount} from "vue";
import {ElMessageBox} from "element-plus";
import {useAuthStore} from "../stores/auth";
import axios from "axios";
import http from "../api/http";

const authStore = useAuthStore();

// ── 会话 ID（刷新页面保持同一会话） ──
const SESSION_KEY = "pms_ai_session";

function getOrCreateSession() {
  let sid = sessionStorage.getItem(SESSION_KEY);
  if (!sid) {
    sid = "session-" + Date.now() + "-" + Math.random().toString(36).slice(2);
    sessionStorage.setItem(SESSION_KEY, sid);
  }
  return sid;
}

const sessionId = ref(getOrCreateSession());

const WELCOME_MSG = {
  role: "assistant",
  content: "👋 你好！我是 PMS 智能客服助手**小P**。\n\n我可以帮你：\n- 📋 查询你的课题申报状态\n- 📈 查看课题进度记录\n- 📢 获取最新系统公告\n- ❓ 解答系统使用疑问\n\n请问有什么我可以帮到你的？",
};

// ── 消息列表 ──
const messages = ref([{...WELCOME_MSG}]);
const historyLoading = ref(false);

// ── 左侧会话列表 ──
const sessionList = ref([]);
const sidebarCollapsed = ref(false);   // false = 展开，true = 收起
const sessionListLoading = ref(false);
const searchQuery = ref("");

const inputText = ref("");
const isLoading = ref(false);
const chatBodyRef = ref(null);

// ── 当前 SSE 连接 ──
let currentEventSource = null;

// ── 搜索过滤后的会话列表 ──
const filteredSessionList = computed(() => {
  if (!searchQuery.value.trim()) return sessionList.value;
  const q = searchQuery.value.trim().toLowerCase();
  return sessionList.value.filter(
      (s) => (s.title || "").toLowerCase().includes(q)
  );
});

// ── 按时间分组会话 ──
const groupedSessions = computed(() => {
  const list = filteredSessionList.value;
  const now = new Date();
  const groups = [
    {label: "今天", items: []},
    {label: "昨天", items: []},
    {label: "最近 7 天", items: []},
    {label: "更早", items: []},
  ];
  list.forEach((s) => {
    const d = new Date(s.updateTime || s.createTime || 0);
    const diffDays = Math.floor((now - d) / 86400000);
    if (diffDays === 0) groups[0].items.push(s);
    else if (diffDays === 1) groups[1].items.push(s);
    else if (diffDays < 7) groups[2].items.push(s);
    else groups[3].items.push(s);
  });
  return groups.filter((g) => g.items.length > 0);
});

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight;
    }
  });
}

// ── 加载当前会话的历史消息 ──
async function loadHistory(sid) {
  historyLoading.value = true;
  messages.value = [];
  try {
    const res = await http.get("/ai/customer/history", {
      params: {sessionId: sid, limit: 100},
    });
    const history = res || [];
    if (history.length > 0) {
      messages.value = history
          .map((m) => ({
            role: m.role === "user" ? "user" : "assistant",
            content: m.content || "",
          }))
          .filter((m) => m.content.trim() !== "");
    } else {
      messages.value = [{...WELCOME_MSG}];
    }
  } catch (e) {
    console.warn("加载历史记录失败:", e);
    messages.value = [{...WELCOME_MSG}];
  } finally {
    historyLoading.value = false;
    scrollToBottom();
  }
}

// ── 加载会话列表 ──
async function loadSessionList() {
  sessionListLoading.value = true;
  try {
    const res = await http.get("/ai/customer/sessions");
    sessionList.value = res || [];
  } catch (e) {
    console.warn("加载会话列表失败:", e);
  } finally {
    sessionListLoading.value = false;
  }
}

// ── 切换到某个历史会话 ──
async function switchSession(sid) {
  if (sid === sessionId.value) return;
  if (currentEventSource) {
    currentEventSource.close();
    currentEventSource = null;
  }
  isLoading.value = false;
  sessionId.value = sid;
  sessionStorage.setItem(SESSION_KEY, sid);
  await loadHistory(sid);
}

// ── 更改会话名称 ──
async function updateSessionName(sid, title) {
  try {
    await http.post(`/ai/customer/sessions/${encodeURIComponent(sid)}`, null, {params: {title}});
    const session = sessionList.value.find((s) => s.sessionId === sid);
    if (session) {
      session.title = title;
    }
  } catch (e) {
    console.warn("更改会话名称失败:", e);
  }
}

// ── 会话重命名（行内编辑）──
const renamingSid = ref(null);    // 正在重命名的会话 ID
const renameText = ref("");       // 编辑框内容

function startRename(session, event) {
  event.stopPropagation();
  renamingSid.value = session.sessionId;
  renameText.value = session.title || "新对话";
  // 等 DOM 更新后聚焦
  nextTick(() => {
    const el = document.getElementById(`rename-input-${session.sessionId}`);
    if (el) {
      el.focus();
      el.select();
    }
  });
}

async function confirmRename(session) {
  const newTitle = renameText.value.trim();
  if (newTitle && newTitle !== session.title) {
    await updateSessionName(session.sessionId, newTitle);
  }
  renamingSid.value = null;
}

function cancelRename() {
  renamingSid.value = null;
}

// ── 删除某个会话 ──
async function deleteSession(sid, event) {
  event.stopPropagation();
  try {
    await ElMessageBox.confirm('确定要删除这条对话记录吗？删除后无法恢复。', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    });
  } catch {
    return; // 点了取消，直接返回
  }
  try {
    await http.delete(`/ai/customer/sessions/${encodeURIComponent(sid)}`);
    sessionList.value = sessionList.value.filter((s) => s.sessionId !== sid);
    if (sid === sessionId.value) {
      startNewSession();
    }
  } catch (e) {
    console.warn("删除会话失败:", e);
  }
}

// ── 切换侧边栏折叠 ──
function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value;
}

// ── 发送消息 ──
async function sendMessage() {
  const text = inputText.value.trim();
  if (!text || isLoading.value) return;

  messages.value.push({role: "user", content: text});
  inputText.value = "";
  scrollToBottom();

  messages.value.push({role: "assistant", content: ""});
  const aiMsgIndex = messages.value.length - 1;
  isLoading.value = true;

  if (currentEventSource) {
    currentEventSource.close();
    currentEventSource = null;
  }

  const rawToken = authStore.token?.replace(/^Bearer\s+/i, "") || "";
  const url = `/api/ai/customer/chat?sessionId=${encodeURIComponent(sessionId.value)}&message=${encodeURIComponent(text)}&token=${encodeURIComponent(rawToken)}`;

  try {
    const es = new EventSource(url);
    currentEventSource = es;

    es.onmessage = (event) => {
      if (event.data && event.data !== "[DONE]") {
        messages.value[aiMsgIndex].content += event.data;
        scrollToBottom();
      }
    };

    es.addEventListener("complete", () => {
      es.close();
      currentEventSource = null;
      isLoading.value = false;
      scrollToBottom();
      loadSessionList();
    });

    es.onerror = () => {
      const hasContent = messages.value[aiMsgIndex].content.trim().length > 0;
      es.close();
      currentEventSource = null;
      isLoading.value = false;
      if (!hasContent) {
        messages.value[aiMsgIndex].content = "连接失败，请稍后重试。";
      }
      scrollToBottom();
      loadSessionList();
    };
  } catch (e) {
    messages.value[aiMsgIndex].content = "请求失败：" + e.message;
    isLoading.value = false;
    scrollToBottom();
  }
}

function handleKeydown(e) {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
}

// ── 新建会话 ──
function startNewSession() {
  if (currentEventSource) {
    currentEventSource.close();
    currentEventSource = null;
  }
  isLoading.value = false;
  const newSid =
      "session-" + Date.now() + "-" + Math.random().toString(36).slice(2);
  sessionId.value = newSid;
  sessionStorage.setItem(SESSION_KEY, newSid);
  messages.value = [
    {
      role: "assistant",
      content: "新的对话已开启！请问有什么我可以帮到你的？",
    },
  ];
}

// ── 格式化时间 ──
function formatTime(dateStr) {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  const now = new Date();
  const diffMs = now - d;
  const diffDays = Math.floor(diffMs / 86400000);
  if (diffDays === 0) {
    return d.toLocaleTimeString("zh-CN", {hour: "2-digit", minute: "2-digit"});
  } else if (diffDays === 1) {
    return "昨天";
  } else if (diffDays < 7) {
    return diffDays + "天前";
  } else {
    return d.toLocaleDateString("zh-CN", {month: "2-digit", day: "2-digit"});
  }
}

// 简单 Markdown 渲染
function renderMarkdown(text) {
  if (!text) return "";
  text = text.replace(/\\n/g, "\n");
  return text
      .replace(
          /```[\s\S]*?```/g,
          (m) =>
              `<pre class="code-block"><code>${m
                  .slice(3, -3)
                  .replace(/^[a-z]*\n/, "")}</code></pre>`
      )
      .replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
      .replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")
      .replace(/\*(.*?)\*/g, "<em>$1</em>")
      .replace(/^[·•\-]\s(.+)$/gm, "<li>$1</li>")
      .replace(/(<li>.*<\/li>\n?)+/gs, (m) => `<ul>${m}</ul>`)
      .replace(/^\d+\.\s(.+)$/gm, "<li>$1</li>")
      .replace(/\n/g, "<br/>");
}

onMounted(async () => {
  await Promise.all([loadHistory(sessionId.value), loadSessionList()]);
});

onBeforeUnmount(() => {
  if (currentEventSource) {
    currentEventSource.close();
  }
});
</script>

<template>
  <div class="ai-service-wrap">
    <!-- ── 左侧历史对话侧边栏 ── -->
    <div class="cs-sidebar" :class="{ collapsed: sidebarCollapsed }">

      <!-- 折叠态：只显示图标列 -->
      <div v-if="sidebarCollapsed" class="sidebar-mini">
        <button class="mini-btn mini-toggle" @click="toggleSidebar" title="展开历史对话">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 18l6-6-6-6"/>
          </svg>
        </button>
        <button class="mini-btn mini-new" @click="startNewSession" title="新建对话">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
        </button>
        <!-- 当前会话小点列表 -->
        <div class="mini-dots">
          <div
              v-for="s in sessionList.slice(0, 8)"
              :key="s.sessionId"
              class="mini-dot"
              :class="{ active: s.sessionId === sessionId }"
              :title="s.title"
              @click="switchSession(s.sessionId)"
          ></div>
        </div>
      </div>

      <!-- 展开态：完整侧边栏 -->
      <template v-else>
        <!-- 顶部 -->
        <div class="sidebar-header">
          <div class="sidebar-header-left">
            <div class="sidebar-icon-wrap">
              <svg class="sidebar-brand-icon" viewBox="0 0 24 24" fill="none">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"
                      stroke="url(#g1)" stroke-width="2" stroke-linejoin="round"/>
                <defs>
                  <linearGradient id="g1" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stop-color="#667eea"/>
                    <stop offset="100%" stop-color="#764ba2"/>
                  </linearGradient>
                </defs>
              </svg>
            </div>
            <span class="sidebar-title">历史对话</span>
          </div>
          <div class="sidebar-header-actions">
            <button class="icon-btn" title="新建对话" @click="startNewSession">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
            </button>
            <button class="icon-btn" title="收起" @click="toggleSidebar">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M15 18l-6-6 6-6"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- 搜索框 -->
        <div class="sidebar-search">
          <div class="search-wrap">
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input
                v-model="searchQuery"
                class="search-input"
                placeholder="搜索历史对话..."
                type="text"
            />
            <button v-if="searchQuery" class="search-clear" @click="searchQuery = ''">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- 会话列表 -->
        <div class="sidebar-body">
          <!-- 加载状态 -->
          <div v-if="sessionListLoading" class="sidebar-loading">
            <div class="loading-spinner"></div>
            <span>加载中…</span>
          </div>

          <!-- 空状态 -->
          <div v-else-if="sessionList.length === 0" class="sidebar-empty">
            <div class="empty-icon">
              <svg viewBox="0 0 64 64" fill="none">
                <circle cx="32" cy="32" r="28" fill="#f0f2ff"/>
                <path d="M20 28h24M20 34h16" stroke="#667eea" stroke-width="2.5" stroke-linecap="round"/>
                <path d="M42 48l-4-4" stroke="#667eea" stroke-width="2.5" stroke-linecap="round"/>
                <circle cx="44" cy="44" r="6" stroke="#667eea" stroke-width="2" fill="white"/>
                <line x1="41" y1="44" x2="47" y2="44" stroke="#667eea" stroke-width="2" stroke-linecap="round"/>
                <line x1="44" y1="41" x2="44" y2="47" stroke="#667eea" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <p class="empty-text">还没有历史对话</p>
            <p class="empty-sub">开始一次对话后会自动保存</p>
          </div>

          <!-- 搜索无结果 -->
          <div
              v-else-if="filteredSessionList.length === 0"
              class="sidebar-empty"
          >
            <div class="empty-icon">
              <svg viewBox="0 0 64 64" fill="none">
                <circle cx="32" cy="32" r="28" fill="#f0f2ff"/>
                <circle cx="30" cy="29" r="10" stroke="#667eea" stroke-width="2"/>
                <line x1="37" y1="37" x2="46" y2="46" stroke="#667eea" stroke-width="2.5" stroke-linecap="round"/>
                <line x1="27" y1="29" x2="33" y2="29" stroke="#ef4444" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <p class="empty-text">未找到相关对话</p>
            <p class="empty-sub">换个关键词试试</p>
          </div>

          <!-- 分组会话列表 -->
          <template v-else>
            <div
                v-for="group in groupedSessions"
                :key="group.label"
                class="session-group"
            >
              <div class="group-label">{{ group.label }}</div>

              <div
                  v-for="s in group.items"
                  :key="s.sessionId"
                  class="session-item"
                  :class="{ active: s.sessionId === sessionId }"
                  @click="renamingSid !== s.sessionId && switchSession(s.sessionId)"
              >
                <div class="session-dot" :class="{ active: s.sessionId === sessionId }"></div>
                <div class="session-info">
                  <!-- 重命名编辑态 -->
                  <input
                      v-if="renamingSid === s.sessionId"
                      :id="`rename-input-${s.sessionId}`"
                      v-model="renameText"
                      class="rename-input"
                      maxlength="40"
                      @keydown.enter.stop="confirmRename(s)"
                      @keydown.escape.stop="cancelRename"
                      @blur="confirmRename(s)"
                      @click.stop
                  />
                  <!-- 普通显示态 -->
                  <div v-else class="session-title" @dblclick.stop="startRename(s, $event)">
                    {{ s.title || "新对话" }}
                  </div>
                  <div class="session-time">{{ formatTime(s.updateTime) }}</div>
                </div>
                <div class="session-actions" v-if="renamingSid !== s.sessionId">
                  <!-- 重命名按钮 -->
                  <button
                      class="session-action-btn"
                      title="重命名"
                      @click.stop="startRename(s, $event)"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                    </svg>
                  </button>
                  <!-- 删除按钮 -->
                  <button
                      class="session-del-btn"
                      @click="deleteSession(s.sessionId, $event)"
                      title="删除此对话"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6"/>
                      <path d="M19 6l-1 14H6L5 6"/>
                      <path d="M10 11v6M14 11v6"/>
                      <path d="M9 6V4h6v2"/>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- 底部统计 -->
        <div class="sidebar-footer">
          <span class="footer-count">
            {{ sessionList.length }} 条对话
          </span>
        </div>
      </template>
    </div>

    <!-- ── 右侧主区域 ── -->
    <div class="cs-main">
      <!-- 头部 -->
      <div class="cs-header">
        <div class="cs-avatar">
          <span class="avatar-emoji"><el-avatar
              :size="50"
              src="https://p3-pc-sign.douyinpic.com/tos-cn-i-0813c000-ce/ogAiAnIdwAA10dMhAEO6baDGPFw1lX2BjpaPi~tplv-dy-aweme-images:q75.webp?biz_tag=aweme_images&from=327834062&lk3s=138a59ce&s=PackSourceEnum_SEARCH&sc=image&se=false&x-expires=1776286800&x-signature=%2B1sBLX32yx4qb4DApo2Tz%2BvSS3s%3D"
          >
              </el-avatar></span>
          <span class="online-dot"></span>
        </div>
        <div class="cs-header-info">
          <div class="cs-name">小P · 智能客服</div>
          <div class="cs-sub">
            <span v-if="historyLoading">加载历史记录中…</span>
            <span v-else>PMS 项目管理系统 · 全天在线</span>
          </div>
        </div>
        <el-button
            size="small"
            plain
            @click="startNewSession"
            :disabled="isLoading"
            class="new-chat-btn"
        >
          <el-icon>
            <Edit/>
          </el-icon>
          新对话
        </el-button>
      </div>

      <!-- 消息区 -->
      <div class="cs-body" ref="chatBodyRef">
        <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="msg-row"
            :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'"
        >
          <!-- AI消息：头像在左 -->
          <template v-if="msg.role === 'assistant'">
            <div class="msg-avatar ai-avatar">
              <el-avatar
                  :size="36"
                  src="https://p3-pc-sign.douyinpic.com/tos-cn-i-0813c000-ce/ogAiAnIdwAA10dMhAEO6baDGPFw1lX2BjpaPi~tplv-dy-aweme-images:q75.webp?biz_tag=aweme_images&from=327834062&lk3s=138a59ce&s=PackSourceEnum_SEARCH&sc=image&se=false&x-expires=1776286800&x-signature=%2B1sBLX32yx4qb4DApo2Tz%2BvSS3s%3D"
              >
              </el-avatar>
            </div>
            <div class="msg-bubble">
              <div
                  class="bubble-content ai-content"
                  v-html="
                  renderMarkdown(msg.content) ||
                  '<span class=\'typing-cursor\'></span>'
                "
              ></div>
            </div>
          </template>

          <!-- 用户消息：头像在右 -->
          <template v-else>
            <div class="msg-bubble user-bubble">
              <div class="bubble-content user-content">{{ msg.content }}</div>
            </div>
            <div class="msg-avatar user-avatar">
              <el-avatar
                  :size="36"
                  :src="
                  'http://127.0.0.1:9000/pms-bucket/' +
                  authStore.userInfo?.user?.avatar
                "
              >
                <img
                    src="https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png"
                />
              </el-avatar>
            </div>
          </template>
        </div>

        <!-- 加载中动画 -->
        <div
            v-if="isLoading && messages[messages.length - 1]?.content === ''"
            class="typing-row"
        >
          <span class="dot"></span><span class="dot"></span
        ><span class="dot"></span>
        </div>
      </div>

      <!-- 快捷提问 -->
      <div class="quick-btns">
        <el-button
            v-for="q in [
            '查看我的课题状态',
            '最新系统公告',
            '如何提交进度报告',
            '系统功能介绍',
          ]"
            :key="q"
            size="small"
            round
            plain
            :disabled="isLoading"
            @click="
            inputText = q;
            sendMessage();
          "
        >{{ q }}
        </el-button
        >
      </div>

      <!-- 输入区 -->
      <div class="cs-footer">
        <el-input
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入问题，按 Enter 发送，Shift+Enter 换行..."
            :disabled="isLoading"
            @keydown="handleKeydown"
            class="chat-input"
            resize="none"
        />
        <el-button
            type="primary"
            :loading="isLoading"
            :disabled="!inputText.trim()"
            @click="sendMessage"
            class="send-btn"
            circle
        >
          <el-icon v-if="!isLoading">
            <Promotion/>
          </el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-service-wrap {
  display: flex;
  flex-direction: row;
  height: calc(100vh - 180px);
  min-height: 500px;
  background: var(--panel);
  border-radius: var(--r-lg);
  overflow: hidden;
  box-shadow: var(--shadow);
}

/* ════════════════════════════════
   侧边栏
════════════════════════════════ */
.cs-sidebar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--panel-strong);
  border-right: 1px solid var(--line);
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
}

.cs-sidebar.collapsed {
  width: 52px;
}

/* ── 折叠态迷你列 ── */
.sidebar-mini {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0;
  gap: 6px;
  height: 100%;
}

.mini-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: var(--r-tag);
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--muted);
  transition: background 0.15s, color 0.15s;
  flex-shrink: 0;
}

.mini-btn svg {
  width: 16px;
  height: 16px;
}

.mini-btn:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.mini-new {
  margin-bottom: 4px;
}

.mini-dots {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 1;
  overflow: hidden;
  padding: 4px 0;
}

.mini-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d1d5db;
  cursor: pointer;
  transition: background 0.15s, transform 0.15s;
  flex-shrink: 0;
}

.mini-dot:hover {
  background: #667eea;
  transform: scale(1.3);
}

.mini-dot.active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  transform: scale(1.2);
}

/* ── 展开态头部 ── */
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 14px 12px;
  flex-shrink: 0;
}

.sidebar-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar-icon-wrap {
  width: 30px;
  height: 30px;
  background: linear-gradient(135deg, #667eea15, #764ba215);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-brand-icon {
  width: 17px;
  height: 17px;
}

.sidebar-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: 0.02em;
}

.sidebar-header-actions {
  display: flex;
  gap: 2px;
}

.icon-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--muted);
  transition: background 0.15s, color 0.15s;
}

.icon-btn svg {
  width: 15px;
  height: 15px;
}

.icon-btn:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

/* ── 搜索框 ── */
.sidebar-search {
  padding: 0 10px 10px;
  flex-shrink: 0;
}

.search-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  width: 14px;
  height: 14px;
  color: #9ca3af;
  pointer-events: none;
}

.search-input {
  width: 100%;
  height: 32px;
  padding: 0 30px 0 32px;
  border: 1px solid var(--line);
  border-radius: var(--r-input);
  background: var(--panel);
  font-size: 12.5px;
  color: var(--text);
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  box-sizing: border-box;
}

.search-input::placeholder {
  color: var(--muted);
}

.search-input:focus {
  border-color: #a5b4fc;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.search-clear {
  position: absolute;
  right: 8px;
  width: 16px;
  height: 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  padding: 0;
}

.search-clear:hover {
  color: #6b7280;
}

.search-clear svg {
  width: 12px;
  height: 12px;
}

/* ── 会话列表 ── */
.sidebar-body {
  flex: 1;
  overflow-y: auto;
  padding: 2px 8px 8px;
}

.sidebar-body::-webkit-scrollbar {
  width: 3px;
}

.sidebar-body::-webkit-scrollbar-thumb {
  background: var(--line);
  border-radius: 4px;
}

.sidebar-body::-webkit-scrollbar-thumb:hover {
  background: var(--muted);
}

/* 加载状态 */
.sidebar-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 40px 0;
  color: var(--muted);
  font-size: 13px;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--line);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 空状态 */
.sidebar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 36px 0 20px;
  gap: 8px;
}

.empty-icon {
  width: 64px;
  height: 64px;
}

.empty-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.empty-sub {
  font-size: 12px;
  color: var(--muted);
  margin: 0;
  text-align: center;
}

/* 分组标签 */
.session-group {
  margin-bottom: 4px;
}

.group-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--muted);
  padding: 8px 8px 4px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

/* 会话条目 */
.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: var(--r-tag);
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
  margin-bottom: 1px;
}

.session-item:hover {
  background: rgba(102, 126, 234, 0.07);
}

.session-item.active {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.12), rgba(118, 75, 162, 0.08));
}

.session-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #d1d5db;
  flex-shrink: 0;
  transition: background 0.2s;
}

.session-dot.active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
}

.session-item:hover .session-dot:not(.active) {
  background: #a5b4fc;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 13px;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
  font-weight: 500;
}

.session-item.active .session-title {
  color: #4338ca;
  font-weight: 600;
}

.session-time {
  font-size: 11px;
  color: var(--muted);
  margin-top: 1px;
}

.session-del-btn {
  width: 22px;
  height: 22px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #b3bac5;
  flex-shrink: 0;
  padding: 0;
  transition: color 0.15s, background 0.15s;
}

.session-del-btn svg {
  width: 13px;
  height: 13px;
}

.session-del-btn:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.12);
}

/* 操作按钮组（重命名 + 删除） */
.session-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  opacity: 0;
  transform: scale(0.85);
  transition: opacity 0.15s, transform 0.15s;
}

.session-item:hover .session-actions {
  opacity: 1;
  transform: scale(1);
}

.session-action-btn {
  width: 22px;
  height: 22px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #b3bac5;
  flex-shrink: 0;
  padding: 0;
  transition: color 0.15s, background 0.15s;
}

.session-action-btn svg {
  width: 13px;
  height: 13px;
}

.session-action-btn:hover {
  color: #667eea;
  background: rgba(102, 126, 234, 0.12);
}

/* 重命名输入框 */
.rename-input {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--text);
  background: var(--panel);
  border: 1px solid #a5b4fc;
  border-radius: 6px;
  padding: 2px 8px;
  outline: none;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  width: 100%;
  box-sizing: border-box;
  line-height: 1.5;
}

/* ── 底部统计 ── */
.sidebar-footer {
  padding: 10px 14px;
  border-top: 1px solid var(--line);
  flex-shrink: 0;
}

.footer-count {
  font-size: 11.5px;
  color: var(--muted);
}

/* ════════════════════════════════
   主区域
════════════════════════════════ */
.cs-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg);
}

/* 头部 - 优化渐变 */
.cs-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 24px;
  background: var(--brand);
  color: #fff;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}

.cs-header::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 50%);
  animation: rotate 30s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.cs-avatar {
  position: relative;
  width: 46px;
  height: 46px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(8px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.online-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  background: #52c41a;
  border-radius: 50%;
  border: 2px solid #fff;
  animation: pulse 2s infinite;
  box-shadow: 0 2px 6px rgba(82, 196, 26, 0.4);
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(82, 196, 26, 0.4);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(82, 196, 26, 0);
  }
}

.cs-name {
  font-size: 16px;
  font-weight: 700;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.cs-sub {
  font-size: 12px;
  opacity: 0.9;
  margin-top: 2px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.new-chat-btn {
  margin-left: auto;
  color: #fff;
  border-color: rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  border-radius: var(--r-lg);
  padding: 8px 16px;
  font-weight: 500;
  transition: background 0.2s, border-color 0.2s, transform 0.2s;
}

.new-chat-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.6);
  transform: translateY(-2px);
}

/* 消息区 - 优化布局 */
.cs-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  background: var(--bg);
  scroll-behavior: smooth;
}

.cs-body::-webkit-scrollbar {
  width: 6px;
}

.cs-body::-webkit-scrollbar-thumb {
  background: var(--line);
  border-radius: 10px;
}

.cs-body::-webkit-scrollbar-thumb:hover {
  background: var(--muted);
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* AI 消息样式 */
.msg-ai {
  justify-content: flex-start;
}

/* 用户消息样式 - 头像在右 */
.msg-user {
  justify-content: flex-end;
}

.msg-avatar {
  flex-shrink: 0;
  transition: transform 0.2s;
}

.msg-avatar:hover {
  transform: scale(1.05);
}

.ai-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 4px 12px rgba(65, 88, 208, 0.2);
  border: 2px solid #fff;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 2px solid #fff;
  transition: transform 0.2s;
}

.user-avatar:hover {
  transform: scale(1.05);
}

.msg-bubble {
  max-width: 60%;
  position: relative;
}

.user-bubble {
  margin-right: 0;
}

.bubble-content {
  padding: 14px 18px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

/* AI 消息气泡 */
.ai-content {
  background: var(--panel);
  color: var(--text);
  border-radius: var(--r-card) var(--r-card) var(--r-card) 4px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
  border: 1px solid var(--line);
}

/* 用户消息气泡 */
.user-content {
  background: #06c3f1;
  color: #fff;
  border-radius: var(--r-card) var(--r-card) 4px var(--r-card);
  box-shadow: 0 4px 15px rgba(193, 80, 192, 0.15);
}

/* 消息时间戳 - 可选 */
.msg-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
  padding: 0 4px;
}

.msg-user .msg-time {
  text-align: right;
}

/* Markdown 样式优化 */
.bubble-content :deep(strong) {
  font-weight: 700;
  color: #4158D0;
}

.msg-user .bubble-content :deep(strong) {
  color: #fff;
  text-decoration: underline;
  text-decoration-color: rgba(255, 255, 255, 0.3);
}

.bubble-content :deep(em) {
  font-style: italic;
}

.bubble-content :deep(ul) {
  padding-left: 20px;
  margin: 8px 0;
}

.bubble-content :deep(li) {
  margin: 4px 0;
  list-style-type: disc;
}

.bubble-content :deep(.code-block) {
  background: #1e293b;
  border-radius: var(--r-card);
  padding: 14px 16px;
  margin: 10px 0;
  font-size: 13px;
  overflow-x: auto;
  color: #e2e8f0;
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.2);
}

.bubble-content :deep(.inline-code) {
  background: rgba(200, 80, 192, 0.08);
  padding: 2px 8px;
  border-radius: var(--r-tag);
  font-size: 13px;
  font-family: 'Fira Code', monospace;
  color: #C850C0;
  border: 1px solid rgba(200, 80, 192, 0.2);
}

/* 正在输入光标 */
.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 18px;
  background: #4158D0;
  animation: blink 1s infinite;
  vertical-align: middle;
  margin-left: 4px;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* 加载点点 - 优化 */
.typing-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-left: 52px;
  margin-bottom: 16px;
}

.dot {
  width: 8px;
  height: 8px;
  background: linear-gradient(135deg, #4158D0, #C850C0);
  border-radius: 50%;
  animation: bounce 1.4s infinite;
  opacity: 0.6;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-8px);
  }
}

/* 快捷提问 - 优化 */
.quick-btns {
  display: flex;
  gap: 10px;
  padding: 12px 24px;
  background: var(--panel);
  border-top: 1px solid var(--line);
  flex-wrap: wrap;
  flex-shrink: 0;
  box-shadow: 0 -4px 10px rgba(0, 0, 0, 0.02);
}

.quick-btns .el-button {
  font-size: 12px;
  color: var(--text);
  border-color: var(--line);
  background: var(--panel-strong);
  border-radius: 999px;
  padding: 8px 18px;
  font-weight: 500;
  transition: all 0.2s;
}

.quick-btns .el-button:hover {
  color: #4158D0;
  border-color: transparent;
  transform: translateY(-2px);
}

/* 输入区 - 优化 */
.cs-footer {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  padding: 16px 24px;
  background: var(--panel);
  border-top: 1px solid var(--line);
  flex-shrink: 0;
  box-shadow: 0 -4px 10px rgba(0, 0, 0, 0.02);
}

.chat-input {
  flex: 1;
}

:deep(.chat-input .el-textarea__inner) {
  border-radius: var(--r-card);
  padding: 12px 20px;
  background: var(--panel-strong);
  border: 2px solid transparent;
  resize: none;
  font-size: 14px;
  line-height: 1.6;
  transition: background 0.2s, border-color 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  color: var(--text);
}

:deep(.chat-input .el-textarea__inner:hover) {
  background: var(--panel);
  border-color: var(--line);
}

:deep(.chat-input .el-textarea__inner:focus) {
  border-color: #4158D0;
  background: var(--panel);
  box-shadow: 0 4px 15px rgba(65, 88, 208, 0.1);
}

.send-btn {
  width: 46px;
  height: 46px;
  border: none;
  align-items: center;
  flex-shrink: 0;
  transition: transform 0.2s, opacity 0.2s;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 10px 25px rgba(193, 80, 192, 0.4);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.95);
}
</style>
