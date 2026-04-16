<script setup>
import {computed, nextTick, onMounted, onBeforeUnmount, ref} from "vue";
import {useAuthStore} from "../stores/auth";
import {friendApi, chatApi, chatDetailedApi, userApi} from "../api/modules";
import {ElMessage, ElMessageBox} from "element-plus";
import {ChatLineRound, Search, User, UserFilled, More, Delete, ArrowDown, Check, Close} from "@element-plus/icons-vue";

// ========================= 表情包 =========================
const EMOJI_LIST = [
  "😀","😁","😂","🤣","😃","😄","😅","😆","😉","😊",
  "😋","😎","😍","🥰","😘","😗","😙","😚","🙂","🤗",
  "🤩","🤔","🤨","😐","😑","😶","🙄","😏","😣","😥",
  "😮","🤐","😯","😪","😫","🥱","😴","😌","😛","😜",
  "😝","🤤","😒","😓","😔","😕","🙃","🤑","😲","🙁",
  "😖","😞","😟","😤","😢","😭","😦","😧","😨","😩",
  "🤯","😬","😰","😱","🥵","🥶","😳","🤪","😵","🥴",
  "😷","🤒","🤕","🤢","🤮","🤧","🥳","🥺","👍","👎",
  "👏","🙌","🤝","🤜","🤛","✊","👊","🫶","❤️","🔥",
  "✨","🎉","🎊","💯","😺","🐶","🐱","🐭","🐹","🐰",
];
const showEmojiPanel = ref(false);
const inputRef = ref(null);

const authStore = useAuthStore();

// ========================= 状态 =========================
const friends = ref([]);                // 好友列表（左侧"好友"tab 用）
const pendingRequests = ref([]);        // 好友申请列表
const chatList = ref([]);              // 最近聊天会话列表（左侧"消息"tab 用）
const chatListHasMore = ref(false);    // 会话列表是否还有更多
const chatListOffset = ref(0);

const activeFriend = ref(null);         // 当前打开的会话对象 { friendId, nickname, avatar, ... }
const messages = ref([]);              // 当前会话的消息列表
const messagesLoading = ref(false);
const friendsLoading = ref(false);
const chatListLoading = ref(false);
const inputText = ref("");
const searchKeyword = ref("");
const tab = ref("chats");             // chats | friends | requests
const addFriendDialog = ref(false);
const addFriendMessage = ref("");
const addFriendLoading = ref(false);
const msgHasMore = ref(false);        // 是否还有更多历史消息
const msgOffset = ref(0);             // 历史消息加载偏移
const loadMoreLoading = ref(false);

// 搜索用户相关
const searchUserKeyword = ref("");
const searchUserResults = ref([]);

// 待发送文件/图片预览
const pendingFile = ref(null);        // { file, type: 'image'|'file', previewUrl, name }
const pendingFileInputRef = ref(null);
const searchUserLoading = ref(false);
const selectedUser = ref(null);

// ========================= WebSocket =========================
let ws = null;
const wsConnected = ref(false);
let wsManualClose = false;
// 在线用户ID集合（online_list 初始化 + online/offline 增量更新）
const onlineUserIds = ref(new Set());

// ========================= 工具函数 =========================
const avatarBase = (import.meta.env.VITE_MINIO_BASE_URL || "http://127.0.0.1:9000/pms-bucket") + "/";

function getAvatar(avatar) {
  if (!avatar) return "https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png";
  if (avatar.startsWith("http")) return avatar;
  return avatarBase + avatar;
}

function formatTime(timeStr) {
  if (!timeStr) return "";
  const d = new Date(timeStr);
  if (isNaN(d.getTime())) return timeStr;
  const now = new Date();
  const isToday = d.toDateString() === now.toDateString();
  const isThisYear = d.getFullYear() === now.getFullYear();
  if (isToday) {
    return d.toLocaleTimeString("zh-CN", {hour: "2-digit", minute: "2-digit"});
  }
  if (isThisYear) {
    return `${d.getMonth() + 1}/${d.getDate()} ${d.toLocaleTimeString("zh-CN", {hour: "2-digit", minute: "2-digit"})}`;
  }
  return d.toLocaleDateString("zh-CN");
}

// ========================= 好友列表 =========================
const filteredFriends = computed(() => {
  if (!searchKeyword.value.trim()) return friends.value;
  return friends.value.filter(f => f.nickname?.includes(searchKeyword.value));
});

async function loadFriends() {
  friendsLoading.value = true;
  try {
    const res = await friendApi.list();
    friends.value = res.data ?? [];
  } finally {
    friendsLoading.value = false;
  }
}

async function loadPendingRequests() {
  try {
    const res = await friendApi.requests();
    pendingRequests.value = res.data ?? [];
  } catch {
  }
}

// ========================= 会话列表 =========================
async function loadChatList(reset = true) {
  if (reset) {
    chatListOffset.value = 0;
    chatList.value = [];
  }
  chatListLoading.value = true;
  try {
    const res = await chatApi.recentList(chatListOffset.value);
    const data = res.data ?? {};
    const newItems = data.list ?? [];
    chatList.value = reset ? newItems : [...chatList.value, ...newItems];
    chatListHasMore.value = !!data.more;
    chatListOffset.value += newItems.length;
  } finally {
    chatListLoading.value = false;
  }
}

async function loadMoreChatList() {
  await loadChatList(false);
}

// 找到会话列表中对应的 chat 项
function findChatItem(friendId) {
  return chatList.value.find(item => {
    const uid = item.user?.user?.id ?? item.user?.id;
    return uid === friendId;
  });
}

// 更新会话列表中某条 chat 的数据（收到新消息时）
function updateChatItemInList(friendId, detail, chatData) {
  const idx = chatList.value.findIndex(item => {
    const uid = item.user?.user?.id ?? item.user?.id;
    return uid === friendId;
  });
  if (idx !== -1) {
    const old = chatList.value[idx];
    // 更新最近消息和未读
    chatList.value.splice(idx, 1, {
      ...old,
      chat: chatData ?? old.chat,
      detail: detail ?? old.detail
    });
    // 把它移到最顶部
    const updated = chatList.value.splice(idx, 1)[0];
    chatList.value.unshift(updated);
  }
}

// ========================= 选择会话 =========================
async function selectChat(chatItem) {
  const user = chatItem.user?.user ?? chatItem.user;
  activeFriend.value = {
    friendId: user.id,
    nickname: user.nickname,
    avatar: user.avatar,
    role: user.role,
  };

  // 通知后端进入窗口
  try {
    await chatApi.online(user.id);
  } catch {
  }

  // 清零该会话的未读（前端 chat 对象里的 unread 字段）
  const idx = chatList.value.findIndex(item => {
    const uid = item.user?.user?.id ?? item.user?.id;
    return uid === user.id;
  });
  if (idx !== -1 && chatList.value[idx].chat) {
    chatList.value[idx] = {
      ...chatList.value[idx],
      chat: {...chatList.value[idx].chat, unread: 0}
    };
  }

  // 加载历史消息
  messages.value = [];
  msgOffset.value = 0;
  msgHasMore.value = false;
  messagesLoading.value = true;
  try {
    const res = await chatDetailedApi.getMore(user.id, 0);
    const data = res.data ?? {};
    const list = data.list ?? [];
    // 后端返回 more 字段表示是否还有更多
    messages.value = [...list].reverse();
    msgOffset.value = list.length;
    msgHasMore.value = !!data.more;
  } finally {
    messagesLoading.value = false;
  }
  await nextTick();
  scrollToBottom();
}

// 也允许从好友列表点击发起聊天
async function startChatFromFriend(friend) {
  // 先创建/恢复会话
  try {
    await chatApi.createChat(friend.friendId);
    // 刷新会话列表
    await loadChatList(true);
    // 找到对应的会话项并打开
    const item = findChatItem(friend.friendId);
    if (item) {
      tab.value = "chats";
      await selectChat(item);
    }
  } catch (e) {
    ElMessage.error("创建会话失败");
  }
}

// ========================= 消息区域滚动 =========================
const messagesContainer = ref(null);

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
}

// 加载更多历史消息（上滑加载）
async function loadMoreMessages() {
  if (!activeFriend.value || loadMoreLoading.value || !msgHasMore.value) return;
  loadMoreLoading.value = true;
  const container = messagesContainer.value;
  const prevScrollHeight = container?.scrollHeight ?? 0;
  try {
    const res = await chatDetailedApi.getMore(activeFriend.value.friendId, msgOffset.value);
    const data = res.data ?? {};
    const list = data.list ?? [];
    // 前置到头部（后端倒序，翻转后 prepend）
    messages.value = [...list.reverse(), ...messages.value];
    msgOffset.value += list.length;
    // 使用后端返回的 more 字段
    msgHasMore.value = !!data.more;
    // 保持滚动位置
    await nextTick();
    if (container) {
      container.scrollTop = container.scrollHeight - prevScrollHeight;
    }
  } finally {
    loadMoreLoading.value = false;
  }
}

// 监听消息区滚动，滚到顶部时自动加载更早消息
function onMessagesScroll() {
  const container = messagesContainer.value;
  if (!container) return;
  if (container.scrollTop < 60 && msgHasMore.value && !loadMoreLoading.value) {
    loadMoreMessages();
  }
}

// ========================= 发送消息 =========================
let pendingMsgKey = 0;

function sendMessage() {
  if (!inputText.value.trim()) return;
  if (!activeFriend.value) return ElMessage.warning("请先选择聊天对象");
  if (!wsConnected.value) return ElMessage.error("连接已断开，请稍后重试");

  const text = inputText.value.trim();
  const tmpKey = ++pendingMsgKey;

  ws.send(JSON.stringify({
    code: 101,
    content: JSON.stringify({
      anotherId: activeFriend.value.friendId,
      content: text
    })
  }));

  // 乐观更新：立即追加气泡
  messages.value.push({
    id: null,
    _tmpKey: tmpKey,
    userId: authStore.userId,
    anotherId: activeFriend.value.friendId,
    content: text,
    withdraw: 0,
    userDel: 0,
    anotherDel: 0,
    time: new Date().toISOString()
  });
  inputText.value = "";
  nextTick(scrollToBottom);
}

// ========================= 表情插入 =========================
function insertEmoji(emoji) {
  inputText.value += emoji;
  showEmojiPanel.value = false;
  // 聚焦回输入框
  nextTick(() => {
    const el = inputRef.value?.$el?.querySelector("textarea");
    if (el) el.focus();
  });
}

// ========================= 图片/文件上传发送 =========================
const fileInputRef = ref(null);
const imageInputRef = ref(null);
const uploadingFile = ref(false);

function triggerImagePicker() {
  imageInputRef.value?.click();
}
function triggerFilePicker() {
  fileInputRef.value?.click();
}

// 文件选择后先预览，不立即发送
function onImageSelected(e) {
  const file = e.target.files?.[0];
  if (!file) return;
  if (!file.type.startsWith("image/")) return ElMessage.warning("请选择图片文件");
  if (file.size > 10 * 1024 * 1024) return ElMessage.warning("图片不能超过 10MB");
  
  // 创建本地预览URL
  const previewUrl = URL.createObjectURL(file);
  pendingFile.value = { file, type: 'image', previewUrl, name: file.name };
  e.target.value = "";
}

function onFileSelected(e) {
  const file = e.target.files?.[0];
  if (!file) return;
  if (file.size > 50 * 1024 * 1024) return ElMessage.warning("文件不能超过 50MB");
  
  pendingFile.value = { file, type: 'file', previewUrl: null, name: file.name };
  e.target.value = "";
}

// 取消待发送文件
function cancelPendingFile() {
  if (pendingFile.value?.previewUrl) {
    URL.revokeObjectURL(pendingFile.value.previewUrl);
  }
  pendingFile.value = null;
}

// 确认发送待发送文件
async function confirmSendPendingFile() {
  if (!pendingFile.value) return;
  if (!activeFriend.value) return ElMessage.warning("请先选择聊天对象");
  if (!wsConnected.value) return ElMessage.error("连接已断开，请稍后重试");
  
  const { file, type } = pendingFile.value;
  await uploadAndSend(file, type);
  cancelPendingFile();
}

async function uploadAndSend(file, type) {
  if (!activeFriend.value) return ElMessage.warning("请先选择聊天对象");
  if (!wsConnected.value) return ElMessage.error("连接已断开，请稍后重试");
  uploadingFile.value = true;
  try {
    const res = await chatDetailedApi.uploadFile(file);
    const url = res?.data?.url ?? res?.data ?? res;
    if (!url) throw new Error("上传失败，未获取到文件地址");
    // 组装特殊内容标记
    const content = type === "image"
      ? `[image:${url}]`
      : `[file:${url}|${file.name}]`;
    const tmpKey = ++pendingMsgKey;
    ws.send(JSON.stringify({
      code: 101,
      content: JSON.stringify({ anotherId: activeFriend.value.friendId, content })
    }));
    messages.value.push({
      id: null, _tmpKey: tmpKey,
      userId: authStore.userId,
      anotherId: activeFriend.value.friendId,
      content,
      withdraw: 0, userDel: 0, anotherDel: 0,
      time: new Date().toISOString()
    });
    nextTick(scrollToBottom);
  } catch (e) {
    ElMessage.error(e?.message || "上传失败");
  } finally {
    uploadingFile.value = false;
  }
}

// ========================= 消息内容解析 =========================
function getMsgType(content) {
  if (!content) return "text";
  if (content.startsWith("[image:") && content.endsWith("]")) return "image";
  if (content.startsWith("[file:") && content.endsWith("]")) return "file";
  return "text";
}
function parseImageUrl(content) {
  return content.slice(7, -1); // [image:URL]
}
function parseFileInfo(content) {
  const inner = content.slice(6, -1); // [file:URL|name]
  const sepIdx = inner.indexOf("|");
  if (sepIdx === -1) return { url: inner, name: "文件" };
  return { url: inner.slice(0, sepIdx), name: inner.slice(sepIdx + 1) };
}
// 预览大图
const previewImageUrl = ref("");
const showImagePreview = ref(false);
function openImagePreview(url) {
  previewImageUrl.value = url;
  showImagePreview.value = true;
}

// ========================= 撤回消息 =========================
function withdrawMessage(msg) {
  if (!msg.id) return;
  ElMessageBox.confirm("确定撤回该消息吗？", "提示", {type: "warning"}).then(() => {
    ws.send(JSON.stringify({
      code: 102,
      content: JSON.stringify({id: msg.id})
    }));
    // 注意：不乐观更新，等后端广播"撤回"事件后再更新
  });
}

// ========================= 删除单条消息 =========================
async function deleteMessage(msg) {
  if (!msg.id) return;
  try {
    await chatDetailedApi.deleteMsg(msg.id);
    const idx = messages.value.findIndex(m => m.id === msg.id);
    if (idx !== -1) messages.value.splice(idx, 1);
    ElMessage.success("消息已删除");
  } catch (e) {
    ElMessage.error("删除失败");
  }
}

// ========================= 删除会话 =========================
async function deleteChat(chatItem, event) {
  event?.stopPropagation();
  const user = chatItem.user?.user ?? chatItem.user;
  await ElMessageBox.confirm(`确定删除与「${user.nickname}」的聊天记录吗？`, "提示", {type: "warning"});
  try {
    await chatApi.deleteChat(user.id);
    ElMessage.success("已删除");
    // 若当前打开的正是该会话，关闭它
    if (activeFriend.value?.friendId === user.id) {
      activeFriend.value = null;
      messages.value = [];
      try {
        await chatApi.outline(user.id, authStore.userId);
      } catch {
      }
    }
    await loadChatList(true);
  } catch {
    ElMessage.error("删除失败");
  }
}

// ========================= WebSocket =========================
function connectWs() {
  // 新协议：token 放在第一帧 content 里，不再放 URL 参数
  const wsUrl = import.meta.env.VITE_WS_URL ? `${import.meta.env.VITE_WS_URL}/im` : "ws://127.0.0.1:9091/im";
  ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    wsConnected.value = true;
    // 第一帧：token 验证
    ws.send(JSON.stringify({
      code: 100,
      content: `Bearer ${authStore.token}`
    }));
  };

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      handleWsMessage(data);
    } catch (e) {
      console.error("ws message parse error", e);
    }
  };

  ws.onclose = () => {
    wsConnected.value = false;
    if (!wsManualClose) {
      setTimeout(connectWs, 3000);
    }
  };

  ws.onerror = () => {
    wsConnected.value = false;
  };
}

function handleWsMessage(data) {
  const type = data.type;

  if (type === "whisper") {
    // 后端推送的聊天消息事件，data.data 包含完整的 { type, online, detail, chat, user }
    handleWhisperEvent(data.data);

  } else if (type === "online_list") {
    // 初始在线好友列表
    onlineUserIds.value = new Set(data.data);

  } else if (type === "online") {
    onlineUserIds.value = new Set([...onlineUserIds.value, data.data]);

  } else if (type === "offline") {
    const next = new Set(onlineUserIds.value);
    next.delete(data.data);
    onlineUserIds.value = next;

  } else if (type === "error") {
    ElMessage.error(data.data || "服务器错误");
  }
}

function handleWhisperEvent(payload) {
  if (!payload) return;
  const {type, detail, chat, user, id, sendId, acceptId} = payload;

  if (type === "接收") {
    // 发送方 uid = detail.userId
    const fromId = detail?.userId;
    const toId = detail?.anotherId;

    // 判断这条消息属于哪方
    const myId = authStore.userId;
    const isMine = fromId === myId;  // 是自己发的（多端同步）
    const friendId = isMine ? toId : fromId;

    if (activeFriend.value?.friendId === friendId) {
      // 当前会话正在看这个人
      if (isMine) {
        // 多端同步：找到等待 ACK 的乐观消息回填真实 ID
        for (let i = messages.value.length - 1; i >= 0; i--) {
          const m = messages.value[i];
          if (m.id === null && m._tmpKey !== undefined) {
            const updated = {...m, id: detail.id, time: detail.time};
            delete updated._tmpKey;
            messages.value.splice(i, 1, updated);
            break;
          }
        }
      } else {
        // 对方发来的新消息
        messages.value.push(detail);
        nextTick(scrollToBottom);
      }
    } else if (isMine) {
      // 自己在另一个终端发的，当前窗口没在这个会话，乐观消息无效，刷新会话列表即可
    }

    // 更新会话列表（不管在不在当前窗口，都要刷新最近消息）
    updateChatItemInList(friendId, detail, chat);

  } else if (type === "撤回") {
    // 有人撤回了消息
    const msgId = id;
    const idx = messages.value.findIndex(m => m.id === msgId);
    if (idx !== -1) {
      messages.value.splice(idx, 1, {...messages.value[idx], withdraw: 1});
    }

  } else if (type === "移除") {
    // 对方删除了会话（可选：刷新会话列表）
    loadChatList(true);
  }
}

// ========================= 好友管理 =========================
async function searchUsers() {
  if (!searchUserKeyword.value.trim()) return;
  searchUserLoading.value = true;
  searchUserResults.value = [];
  selectedUser.value = null;
  try {
    const res = await userApi.search(searchUserKeyword.value.trim());
    searchUserResults.value = res.data ?? [];
    if (searchUserResults.value.length === 0) ElMessage.info("未找到相关用户");
  } catch {
    ElMessage.error("搜索失败，请重试");
  } finally {
    searchUserLoading.value = false;
  }
}

function selectSearchUser(u) {
  selectedUser.value = u;
}

async function handleAddFriend() {
  if (!selectedUser.value) return ElMessage.warning("请先搜索并选择要添加的用户");
  addFriendLoading.value = true;
  try {
    await friendApi.apply(selectedUser.value.user.id, addFriendMessage.value);
    ElMessage.success(`已向「${selectedUser.value.user.nickname}」发送好友申请`);
    addFriendDialog.value = false;
    searchUserKeyword.value = "";
    searchUserResults.value = [];
    selectedUser.value = null;
    addFriendMessage.value = "";
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || "申请失败");
  } finally {
    addFriendLoading.value = false;
  }
}

async function handleAccept(request) {
  await friendApi.accept(request.relationId);
  ElMessage.success("已接受好友申请");
  await loadPendingRequests();
  await loadFriends();
}

async function handleReject(request) {
  await friendApi.reject(request.relationId);
  ElMessage.success("已拒绝");
  await loadPendingRequests();
}

async function handleRemoveFriend(friend, event) {
  event?.stopPropagation();
  await ElMessageBox.confirm(`确定删除好友「${friend.nickname}」吗？`, "提示", {type: "warning"});
  await friendApi.remove(friend.friendId);
  ElMessage.success("已删除");
  if (activeFriend.value?.friendId === friend.friendId) activeFriend.value = null;
  await loadFriends();
}

// ========================= 右键菜单命令 =========================
function handleMsgCommand(cmd, msg) {
  if (cmd === "withdraw") withdrawMessage(msg);
  else if (cmd === "delete") deleteMessage(msg);
}

function handleEnter(e) {
  if (e.shiftKey) return;
  e.preventDefault();
  sendMessage();
}

// ========================= 生命周期 =========================
onMounted(() => {
  loadFriends();
  loadPendingRequests();
  loadChatList(true);
  connectWs();
});

onBeforeUnmount(() => {
  wsManualClose = true;
  // 离开当前聊天窗口
  if (activeFriend.value) {
    chatApi.outline(activeFriend.value.friendId, authStore.userId).catch(() => {
    });
  }
  if (ws) ws.close();
});
</script>

<template>
  <div class="chat-container">
    <!-- 左侧侧边栏 -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <div class="sidebar-tabs">
          <span :class="['tab', { active: tab === 'chats' }]" @click="tab = 'chats'">消息</span>
          <span :class="['tab', { active: tab === 'friends' }]" @click="tab = 'friends'">好友</span>
          <span :class="['tab', { active: tab === 'requests' }]" @click="tab = 'requests'">
            申请
            <el-badge v-if="pendingRequests.length" :value="pendingRequests.length" class="req-badge"/>
          </span>
        </div>
        <el-button circle size="small" @click="addFriendDialog = true" title="添加好友">
          <el-icon>
            <User/>
          </el-icon>
        </el-button>
      </div>

      <!-- 搜索栏 -->
      <div v-if="tab !== 'requests'" class="search-bar">
        <el-input v-model="searchKeyword" placeholder="搜索..." prefix-icon="Search" clearable size="small"/>
      </div>

      <!-- ===== 消息列表（tab=chats） ===== -->
      <el-scrollbar v-if="tab === 'chats'" class="friend-list">
        <div v-loading="chatListLoading">
          <div v-if="chatList.length === 0 && !chatListLoading" class="empty-tip">暂无聊天记录</div>
          <div
              v-for="item in chatList"
              :key="item.chat?.id"
              :class="['friend-item', { active: activeFriend?.friendId === (item.user?.user?.id ?? item.user?.id) }]"
              @click="selectChat(item)"
          >
            <div class="avatar-wrap">
              <el-avatar :size="42" :src="getAvatar(item.user?.user?.avatar ?? item.user?.avatar)"/>
              <span
                  :class="['online-dot', onlineUserIds.has(item.user?.user?.id ?? item.user?.id) ? 'is-online' : 'is-offline']"/>
            </div>
            <div class="friend-info">
              <div class="friend-name-row">
                <span class="friend-name">{{ item.user?.user?.nickname ?? item.user?.nickname }}</span>
                <span class="last-time">{{ formatTime(item.detail?.time) }}</span>
              </div>
              <div class="last-msg-row">
                <span class="last-msg">
                  <template v-if="item.detail?.withdraw === 1">消息已撤回</template>
                  <template v-else-if="getMsgType(item.detail?.content) === 'image'">图片</template>
                  <template v-else-if="getMsgType(item.detail?.content) === 'file'">{{ parseFileInfo(item.detail.content).name }}</template>
                  <template v-else>{{ item.detail?.content || "暂无消息" }}</template>
                </span>
                <el-badge v-if="item.chat?.unread > 0 && item.detail?.userId !== authStore.userId" :value="item.chat.unread" :max="99" class="unread-badge"/>
              </div>
            </div>
            <el-button
                type="danger" link size="small" class="delete-chat-btn"
                @click.stop="deleteChat(item, $event)"
                title="删除会话"
            >
              <el-icon>
                <Delete/>
              </el-icon>
            </el-button>
          </div>

          <!-- 加载更多会话 -->
          <div v-if="chatListHasMore" class="load-more-btn" @click="loadMoreChatList">
            <el-button link size="small">加载更多</el-button>
          </div>
        </div>
      </el-scrollbar>

      <!-- ===== 好友列表（tab=friends） ===== -->
      <el-scrollbar v-else-if="tab === 'friends'" class="friend-list">
        <div v-loading="friendsLoading">
          <div v-if="filteredFriends.length === 0" class="empty-tip">暂无好友</div>
          <div
              v-for="friend in filteredFriends"
              :key="friend.friendId"
              :class="['friend-item', { active: activeFriend?.friendId === friend.friendId }]"
              @click="startChatFromFriend(friend)"
          >
            <div class="avatar-wrap">
              <el-avatar :size="42" :src="getAvatar(friend.avatar)"/>
              <span :class="['online-dot', onlineUserIds.has(friend.friendId) ? 'is-online' : 'is-offline']"/>
            </div>
            <div class="friend-info">
              <div class="friend-name-row">
                <span class="friend-name">{{ friend.nickname }}</span>
                <span :class="['online-label', onlineUserIds.has(friend.friendId) ? 'online' : 'offline']">
                  {{ onlineUserIds.has(friend.friendId) ? '在线' : '离线' }}
                </span>
              </div>
              <div class="friend-role">
                {{ friend.role === 0 ? '学生' : friend.role === 1 ? '指导教师' : '管理员' }}
              </div>
            </div>
            <el-button 
              class="delete-friend-btn" 
              circle 
              size="small" 
              @click.stop="handleRemoveFriend(friend, $event)"
              title="删除好友"
            >
              <el-icon><Delete/></el-icon>
            </el-button>
          </div>
        </div>
      </el-scrollbar>

      <!-- ===== 申请列表（tab=requests） ===== -->
      <el-scrollbar v-else class="friend-list">
        <div v-if="pendingRequests.length === 0" class="empty-tip">暂无好友申请</div>
        <div v-for="req in pendingRequests" :key="req.relationId" class="request-item">
          <el-avatar :size="40" :src="getAvatar(req.avatar)"/>
          <div class="req-info">
            <div class="friend-name">{{ req.nickname }}</div>
            <div class="req-msg">{{ req.message || '请求添加你为好友' }}</div>
          </div>
          <div class="req-actions">
            <el-button 
              class="req-btn req-accept" 
              size="small" 
              @click="handleAccept(req)"
              title="接受"
            >
              <el-icon><Check/></el-icon>
            </el-button>
            <el-button 
              class="req-btn req-reject" 
              size="small" 
              @click="handleReject(req)"
              title="拒绝"
            >
              <el-icon><Close/></el-icon>
            </el-button>
          </div>
        </div>
      </el-scrollbar>
    </div>

    <!-- 右侧聊天区 -->
    <div class="chat-main">
      <!-- 未选中会话时的占位 -->
      <div v-if="!activeFriend" class="chat-placeholder">
        <el-icon style="font-size: 64px; color: #c0c4cc">
          <ChatLineRound/>
        </el-icon>
        <p>选择一位好友开始聊天</p>
<!--        <el-tag :type="wsConnected ? 'success' : 'danger'" effect="light">-->
<!--          {{ wsConnected ? '已连接' : '连接中...' }}-->
<!--        </el-tag>-->
      </div>

      <template v-else>
        <!-- 聊天头部 -->
        <div class="chat-header">
          <!-- 手机端返回按钮 -->
          <button class="mobile-back-btn" @click="activeFriend = null">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15 18 9 12 15 6"/>
            </svg>
          </button>
          <div class="avatar-wrap">
            <el-avatar :size="36" :src="getAvatar(activeFriend.avatar)"/>
            <span :class="['online-dot-sm', onlineUserIds.has(activeFriend.friendId) ? 'is-online' : 'is-offline']"/>
          </div>
          <div class="chat-title-wrap">
            <span class="chat-title">{{ activeFriend.nickname }}</span>
            <span :class="['chat-online-label', onlineUserIds.has(activeFriend.friendId) ? 'online' : 'offline']">
              {{ onlineUserIds.has(activeFriend.friendId) ? '在线' : '离线' }}
            </span>
          </div>
        </div>

        <!-- 消息区域 -->
        <div ref="messagesContainer" class="messages-area" v-loading="messagesLoading"
             @scroll="onMessagesScroll">
          <!-- 顶部加载状态提示 -->
          <div v-if="msgHasMore || loadMoreLoading" class="load-more-tip">
            <el-button link size="small" :loading="loadMoreLoading" @click="loadMoreMessages">
              {{ loadMoreLoading ? '加载中...' : '上滑加载更早消息' }}
            </el-button>
          </div>

          <div
              v-for="msg in messages"
              :key="msg.id ?? `tmp_${msg._tmpKey}`"
              class="message-row"
              :class="{ mine: msg.userId === authStore.userId }"
          >
            <el-avatar
                :size="32"
                :src="msg.userId === authStore.userId
                  ? getAvatar(authStore.userInfo?.user?.avatar)
                  : getAvatar(activeFriend.avatar)"
                class="msg-avatar"
            />
            <div class="msg-bubble-wrap">
              <!-- 已撤回 -->
              <div v-if="msg.withdraw === 1" class="msg-withdrawn">消息已撤回</div>
              <!-- 等待发送中（乐观消息） -->
              <template v-else-if="msg.id === null">
                <div v-if="getMsgType(msg.content) === 'image'" class="msg-bubble sending msg-image-wrap">
                  <img :src="parseImageUrl(msg.content)" class="msg-image" alt="图片"/>
                </div>
                <div v-else-if="getMsgType(msg.content) === 'file'" class="msg-bubble sending msg-file-card">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="file-icon">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                  </svg>
                  <span class="file-name">{{ parseFileInfo(msg.content).name }}</span>
                </div>
                <div v-else class="msg-bubble sending" title="发送中...">{{ msg.content }}</div>
              </template>
              <!-- 正常消息 -->
              <el-dropdown
                  v-else
                  trigger="contextmenu"
                  placement="bottom-start"
                  @command="cmd => handleMsgCommand(cmd, msg)"
              >
                <!-- 图片消息 -->
                <div
                  v-if="getMsgType(msg.content) === 'image'"
                  class="msg-bubble msg-image-wrap"
                  @click="openImagePreview(parseImageUrl(msg.content))"
                >
                  <img :src="parseImageUrl(msg.content)" class="msg-image" alt="图片"/>
                </div>
                <!-- 文件消息 -->
                <a
                  v-else-if="getMsgType(msg.content) === 'file'"
                  class="msg-bubble msg-file-card"
                  :href="parseFileInfo(msg.content).url"
                  target="_blank"
                  download
                  @click.stop
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="file-icon">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                  </svg>
                  <div class="file-info">
                    <span class="file-name">{{ parseFileInfo(msg.content).name }}</span>
                    <span class="file-dl-hint">点击下载</span>
                  </div>
                </a>
                <!-- 文字消息 -->
                <div v-else class="msg-bubble">{{ msg.content }}</div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                        v-if="msg.userId === authStore.userId"
                        command="withdraw"
                    >撤回
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" style="color:#f56c6c">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <div class="msg-time">{{ formatTime(msg.time) }}</div>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="input-area" @click.self="showEmojiPanel = false">
          <!-- 工具栏 -->
          <div class="input-toolbar">
            <!-- 表情 -->
            <div class="toolbar-emoji-wrap">
              <button
                class="toolbar-btn"
                title="表情"
                @click.stop="showEmojiPanel = !showEmojiPanel"
                :class="{ active: showEmojiPanel }"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
                  <line x1="9" y1="9" x2="9.01" y2="9"/>
                  <line x1="15" y1="9" x2="15.01" y2="9"/>
                </svg>
              </button>
              <!-- 表情面板 -->
              <transition name="emoji-panel">
                <div v-if="showEmojiPanel" class="emoji-panel" @click.stop>
                  <div class="emoji-grid">
                    <span
                      v-for="em in EMOJI_LIST"
                      :key="em"
                      class="emoji-item"
                      @click="insertEmoji(em)"
                    >{{ em }}</span>
                  </div>
                </div>
              </transition>
            </div>
            <!-- 图片 -->
            <button class="toolbar-btn" title="发送图片" @click="triggerImagePicker" :disabled="uploadingFile">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <circle cx="8.5" cy="8.5" r="1.5"/>
                <polyline points="21 15 16 10 5 21"/>
              </svg>
            </button>
            <!-- 文件 -->
            <button class="toolbar-btn" title="发送文件" @click="triggerFilePicker" :disabled="uploadingFile">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
            </button>
            <!-- 上传中指示 -->
            <span v-if="uploadingFile" class="uploading-tip">上传中...</span>
          </div>

          <!-- 待发送文件预览 -->
          <div v-if="pendingFile" class="pending-file-preview">
            <div class="pending-file-content">
              <!-- 图片预览 -->
              <template v-if="pendingFile.type === 'image'">
                <img :src="pendingFile.previewUrl" class="pending-image" alt="预览"/>
                <span class="pending-file-name">{{ pendingFile.name }}</span>
              </template>
              <!-- 文件预览 -->
              <template v-else>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="pending-file-icon">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                </svg>
                <span class="pending-file-name">{{ pendingFile.name }}</span>
              </template>
            </div>
            <div class="pending-file-actions">
              <el-button type="primary" size="small" @click="confirmSendPendingFile" :loading="uploadingFile">发送</el-button>
              <el-button size="small" @click="cancelPendingFile">取消</el-button>
            </div>
          </div>

          <!-- 文本输入 -->
          <el-input
              ref="inputRef"
              v-model="inputText"
              type="textarea"
              :rows="3"
              placeholder="输入消息，Enter 发送，Shift+Enter 换行..."
              @keydown.enter="handleEnter"
              resize="none"
              @click="showEmojiPanel = false"
          />
          <div class="input-footer">
            <span class="input-hint">右键消息可撤回/删除</span>
            <el-button type="primary" @click="sendMessage" :disabled="!wsConnected">发送</el-button>
          </div>

          <!-- 隐藏的文件选择器 -->
          <input ref="imageInputRef" type="file" accept="image/*" style="display:none" @change="onImageSelected"/>
          <input ref="fileInputRef" type="file" style="display:none" @change="onFileSelected"/>
        </div>
      </template>
    </div>
  </div>

  <!-- 大图预览 -->
  <el-dialog
    v-model="showImagePreview"
    width="auto"
    :show-close="true"
    class="image-preview-dialog"
    align-center
  >
    <img :src="previewImageUrl" style="max-width:80vw;max-height:80vh;display:block;border-radius:8px;" alt="预览"/>
  </el-dialog>

  <!-- 添加好友弹窗 -->
  <el-dialog
      v-model="addFriendDialog"
      title="查找并添加好友"
      width="460px"
      @close="() => { searchUserKeyword = ''; searchUserResults = []; selectedUser = null; addFriendMessage = ''; }"
  >
    <div class="add-friend-wrap">
      <div class="search-row">
        <el-input
            v-model="searchUserKeyword"
            placeholder="输入昵称或用户名搜索..."
            clearable
            @keyup.enter="searchUsers"
        >
          <template #prefix>
            <el-icon>
              <Search/>
            </el-icon>
          </template>
        </el-input>
        <el-button type="primary" :loading="searchUserLoading" @click="searchUsers">搜索</el-button>
      </div>

      <div v-if="searchUserResults.length > 0" class="search-result-list">
        <div
            v-for="u in searchUserResults"
            :key="u.user.id"
            :class="['search-result-item', { selected: selectedUser?.user?.id === u.user.id }]"
            @click="selectSearchUser(u)"
        >
          <el-avatar :size="38" :src="getAvatar(u.user.avatar)"/>
          <div class="sri-info">
            <div class="sri-name">{{ u.user.nickname }}</div>
            <div class="sri-sub">@{{ u.user.username }} ·
              {{ u.user.role === 0 ? '学生' : u.user.role === 1 ? '教师' : '管理员' }}
            </div>
          </div>
          <el-tag v-if="selectedUser?.user?.id === u.user.id" type="success" size="small">已选</el-tag>
        </div>
      </div>
      <el-empty v-else-if="!searchUserLoading && searchUserKeyword" description="未找到相关用户" :image-size="60"/>

      <template v-if="selectedUser">
        <el-divider/>
        <div class="selected-user-hint">
          <el-icon>
            <UserFilled/>
          </el-icon>
          向「<b>{{ selectedUser.user.nickname }}</b>」发送好友申请
        </div>
        <el-input v-model="addFriendMessage" placeholder="申请附言（可选）" maxlength="100" show-word-limit/>
      </template>
    </div>

    <template #footer>
      <el-button @click="addFriendDialog = false">取消</el-button>
      <el-button type="primary" :loading="addFriendLoading" :disabled="!selectedUser" @click="handleAddFriend">
        发送申请
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 140px);
  border-radius: 12px;
  overflow: hidden;
  background: var(--panel);
  box-shadow: var(--shadow);
}

/* ===== 左侧侧边栏 ===== */
.chat-sidebar {
  width: 280px;
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  background: var(--panel-strong);
  flex-shrink: 0;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--line);
}

.sidebar-tabs {
  display: flex;
  gap: 16px;
}

.tab {
  font-size: 14px;
  color: var(--muted);
  cursor: pointer;
  padding-bottom: 2px;
  position: relative;
}

.tab.active {
  color: var(--brand);
  font-weight: 700;
  border-bottom: 2px solid var(--brand);
}

.req-badge {
  position: absolute;
  top: -4px;
  right: -14px;
}

.search-bar {
  padding: 8px 12px;
}

.friend-list {
  flex: 1;
  overflow: hidden;
}

.empty-tip {
  text-align: center;
  padding: 40px 0;
  color: var(--muted);
  font-size: 13px;
}

.load-more-btn {
  text-align: center;
  padding: 8px 0;
}

/* ===== 会话/好友 item ===== */
.friend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid var(--line);
  position: relative;
}

.friend-item:hover {
  background: rgba(174, 93, 48, 0.06);
}

.friend-item.active {
  background: rgba(174, 93, 48, 0.1);
  border-right: 3px solid var(--brand);
}

.friend-item:hover .delete-chat-btn {
  opacity: 1;
}

.delete-chat-btn {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
  padding: 2px 4px;
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.online-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid var(--panel-strong);
}

.online-dot.is-online {
  background: #67c23a;
}

.online-dot.is-offline {
  background: #c0c4cc;
}

.friend-info {
  flex: 1;
  min-width: 0;
}

.friend-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
}

.friend-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.last-time {
  font-size: 11px;
  color: var(--muted);
  flex-shrink: 0;
}

.last-msg-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 2px;
}

.last-msg {
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.unread-badge {
  flex-shrink: 0;
  margin-left: 4px;
}

.friend-role {
  font-size: 11px;
  color: var(--muted);
  margin-top: 2px;
}

.online-label {
  font-size: 11px;
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.online-label.online {
  color: #fff;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  box-shadow: 0 2px 6px rgba(103, 194, 58, 0.3);
}

.online-label.offline {
  color: #909399;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
}

.request-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
}

.req-info {
  flex: 1;
  min-width: 0;
}

.req-msg {
  font-size: 12px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.req-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* 好友申请按钮 */
.req-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  padding: 0;
  border: none;
  transition: all 0.2s;
}

.req-accept {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.35);
}

.req-accept:hover {
  background: linear-gradient(135deg, #5daf34 0%, #77c056 100%);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.45);
}

.req-reject {
  background: #f5f7fa;
  color: #909399;
  border: 1px solid #dcdfe6;
}

.req-reject:hover {
  background: #fef0f0;
  color: #f56c6c;
  border-color: #fde2e2;
  transform: scale(1.05);
}

/* 删除好友按钮 */
.delete-friend-btn {
  opacity: 0;
  transition: all 0.2s;
  color: #c0c4cc;
  background: transparent;
  border: none;
}

.friend-item:hover .delete-friend-btn {
  opacity: 1;
}

.delete-friend-btn:hover {
  color: #f56c6c;
  background: #fef0f0;
  transform: scale(1.1);
}

/* ===== 右侧聊天区 ===== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--muted);
  gap: 12px;
  font-size: 14px;
}

.chat-header {
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--line);
  background: var(--panel);
  flex-shrink: 0;
}

.online-dot-sm {
  position: absolute;
  bottom: 1px;
  right: 1px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  border: 2px solid var(--panel);
}

.online-dot-sm.is-online {
  background: #67c23a;
}

.online-dot-sm.is-offline {
  background: var(--muted);
}

.chat-title-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.chat-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
}

.chat-online-label {
  font-size: 11px;
}

.chat-online-label.online {
  color: #67c23a;
}

.chat-online-label.offline {
  color: #c0c4cc;
}

.ws-status {
  margin-left: auto;
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: var(--bg);
}

.load-more-tip {
  text-align: center;
  padding: 4px 0 8px;
  cursor: pointer;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.message-row.mine {
  flex-direction: row-reverse;
}

.msg-bubble-wrap {
  max-width: 60%;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.message-row.mine .msg-bubble-wrap {
  align-items: flex-end;
}

.msg-bubble {
  background: var(--panel);
  border-radius: 12px 12px 12px 2px;
  padding: 10px 14px;
  font-size: 14px;
  color: var(--text);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
  word-break: break-word;
  line-height: 1.5;
  cursor: pointer;
  user-select: text;
}

.message-row.mine .msg-bubble {
  background: #06c3f1;
  color: #fff;
  border-radius: 12px 12px 2px 12px;
}

.msg-bubble.sending {
  opacity: 0.6;
  cursor: default;
}

.msg-withdrawn {
  font-size: 12px;
  color: var(--muted);
  font-style: italic;
  padding: 6px 10px;
}

.msg-time {
  font-size: 11px;
  color: var(--muted);
  padding: 0 4px;
}

.msg-avatar {
  flex-shrink: 0;
}

/* ===== 输入区 ===== */
.input-area {
  border-top: 1px solid var(--line);
  background: var(--panel);
  flex-shrink: 0;
}

.input-area :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding: 16px 20px 8px;
  resize: none;
  font-size: 14px;
  background: var(--panel);
  color: var(--text);
}

.input-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 8px 16px 12px;
  gap: 12px;
}

.input-hint {
  font-size: 12px;
  color: var(--muted);
  flex: 1;
}

/* ===== 添加好友弹窗 ===== */
.add-friend-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-row {
  display: flex;
  gap: 8px;
}

.search-result-list {
  max-height: 240px;
  overflow-y: auto;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid var(--line);
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item:hover {
  background: rgba(174, 93, 48, 0.05);
}

.search-result-item.selected {
  background: rgba(174, 93, 48, 0.08);
  border-left: 3px solid var(--brand);
}

.sri-info {
  flex: 1;
  min-width: 0;
}

.sri-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.sri-sub {
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
}

.selected-user-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text);
  margin-bottom: 8px;
}

.selected-user-hint b {
  color: var(--brand);
}

/* ===== 输入工具栏 ===== */
.input-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px 0;
  position: relative;
}

.toolbar-btn {
  width: 30px;
  height: 30px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--muted);
  transition: color 0.15s, background 0.15s;
  flex-shrink: 0;
  padding: 0;
}
.toolbar-btn svg { width: 18px; height: 18px; }
.toolbar-btn:hover, .toolbar-btn.active {
  color: var(--brand);
  background: rgba(174, 93, 48, 0.08);
}
.toolbar-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.uploading-tip {
  font-size: 12px;
  color: var(--muted);
  margin-left: 6px;
}

/* ===== 待发送文件预览 ===== */
.pending-file-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #f5f5f5;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}
.pending-file-content {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}
.pending-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--line);
}
.pending-file-icon {
  width: 40px;
  height: 40px;
  color: var(--muted);
}
.pending-file-name {
  font-size: 14px;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pending-file-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* ===== 表情面板 ===== */
.toolbar-emoji-wrap {
  position: relative;
}

.emoji-panel {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 0;
  z-index: 100;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  padding: 10px;
  width: 320px;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 2px;
}

.emoji-item {
  font-size: 20px;
  text-align: center;
  cursor: pointer;
  border-radius: 6px;
  padding: 3px 0;
  line-height: 1.4;
  transition: background 0.1s;
  user-select: none;
}
.emoji-item:hover {
  background: rgba(174, 93, 48, 0.1);
}

/* 表情面板动画 */
.emoji-panel-enter-active, .emoji-panel-leave-active {
  transition: opacity 0.15s, transform 0.15s;
}
.emoji-panel-enter-from, .emoji-panel-leave-to {
  opacity: 0;
  transform: translateY(6px) scale(0.97);
}

/* ===== 图片消息 ===== */
.msg-image-wrap {
  padding: 4px !important;
  cursor: pointer;
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
}
.msg-image {
  max-width: 240px;
  max-height: 200px;
  border-radius: 10px;
  display: block;
  object-fit: cover;
  transition: opacity 0.15s;
}
.msg-image-wrap:hover .msg-image {
  opacity: 0.9;
}

/* ===== 文件消息卡片 ===== */
.msg-file-card {
  display: flex !important;
  align-items: center;
  gap: 10px;
  padding: 12px 16px !important;
  min-width: 180px;
  max-width: 260px;
  text-decoration: none;
  cursor: pointer;
}
.msg-file-card:hover {
  opacity: 0.88;
}
.file-icon {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  color: var(--brand);
}
.message-row.mine .file-icon { color: rgba(255,255,255,0.9); }
.file-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.file-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}
.message-row.mine .file-name { color: #fff; }
.file-dl-hint {
  font-size: 11px;
  color: var(--muted);
}
.message-row.mine .file-dl-hint { color: rgba(255,255,255,0.7); }

/* 手机端返回按钮 - PC端默认隐藏 */
.mobile-back-btn {
  display: none !important;
}

/* ==================== 手机端适配 ==================== */
@media (max-width: 768px) {
  .chat-container {
    height: calc(100vh - 120px);
    border-radius: 12px;
  }

  .chat-sidebar {
    width: 100%;
    border-right: none;
  }

  .chat-main {
    display: none;
  }

  .chat-main.active-chat {
    display: flex;
    width: 100%;
  }

  .chat-sidebar.hidden-on-mobile {
    display: none;
  }

  .sidebar-header {
    padding: 12px;
  }

  .chat-header {
    padding: 10px 14px;
  }

  .messages-area {
    padding: 12px 14px;
  }

  .msg-image {
    max-width: 180px;
    max-height: 150px;
  }

  .input-area :deep(.el-textarea__inner) {
    padding: 12px 14px 6px;
    font-size: 15px;
  }

  .chat-placeholder {
    font-size: 13px;
  }

  .image-preview-overlay {
    padding: 0;
    align-items: center;
  }

  .image-preview-overlay img {
    max-width: 95vw;
    max-height: 80vh;
    border-radius: 8px;
  }

  .image-preview-close {
    top: 12px;
    right: 12px;
  }

  /* 手机端返回按钮 */
  .mobile-back-btn {
    display: flex;
    width: 32px;
    height: 32px;
    border: none;
    border-radius: 50%;
    background: rgba(174, 93, 48, 0.1);
    color: var(--brand);
    cursor: pointer;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    transition: background 0.15s;
  }

  .mobile-back-btn:hover {
    background: rgba(174, 93, 48, 0.2);
  }

  .mobile-back-btn svg {
    width: 18px;
    height: 18px;
  }

  .chat-header {
    gap: 10px;
  }
}
</style>
