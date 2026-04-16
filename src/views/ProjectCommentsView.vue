<script setup>
import {computed, onMounted, reactive, ref, watch} from "vue";
import {useRoute, useRouter} from "vue-router";
import {projectApi, commentApi} from "../api/modules";
import {useAuthStore} from "../stores/auth";
import {usePagination} from "../composables";
import {ElMessage, ElMessageBox} from 'element-plus';
import {Refresh, Search, ChatDotRound, Delete} from '@element-plus/icons-vue';
import {timeUtil} from '../utils/TimeFormatUtil.js';

// ---- 常量 ----
/** 头像对象存储基础路径 */
const AVATAR_BASE_URL = (import.meta.env.VITE_MINIO_BASE_URL || "http://127.0.0.1:9000/pms-bucket") + "/";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// ---- 课题列表分页（使用通用 Hook） ----
const {
  query,
  page,
  loading,
  loadData: loadProjects,
  changePage: changeProjectPage
} = usePagination(
    (params) => {
      const p = {...params};
      // 学生只看自己的课题
      if (authStore.role === 0) p.sid = authStore.userId;
      return projectApi.list(p);
    },
    {title: "", type: "", status: "", sortBy: "id", isAsc: false, pageSize: 10}
);

// ---- 评论区状态（独立管理） ----
const commentState = reactive({
  loading: false,
  selectedProjectId: null,
  comments: [],
  mainContent: ""
});

/** 权限判断：管理员和教师可以删除任何评论 */
const canDeleteAny = computed(() => [1, 2].includes(authStore.role));

/**
 * 加载指定课题的评论树
 */
async function loadComments(tid) {
  if (!tid) return;
  commentState.loading = true;
  try {
    const data = await commentApi.tree(tid);
    commentState.comments = data.map(item => initStatus(item));
  } catch {
    ElMessage.error("加载评论失败");
  } finally {
    commentState.loading = false;
  }
}

/**
 * 递归初始化评论的 UI 状态（使用 replies 字段而非 children）
 */
function initStatus(comment) {
  return {
    ...comment,
    showReply: false,
    draft: "",
    replies: comment.replies ? comment.replies.map(child => initStatus(child)) : []
  };
}

/**
 * 选择课题，更新 URL 并加载评论
 */
function selectProject(id) {
  commentState.selectedProjectId = id;
  router.replace({query: {...route.query, projectId: id}});
  loadComments(id);
}

/**
 * 提交评论或回复
 * @param {object|null} parent - 父评论，null 表示根评论
 */
async function submitComment(parent = null) {
  const content = (parent ? parent.draft : commentState.mainContent)?.trim();
  if (!content) return ElMessage.warning("说点什么吧...");

  try {
    await commentApi.create({
      tid: commentState.selectedProjectId,
      parentId: parent?.id ?? 0,
      rootId: parent?.rootId ?? parent?.id ?? 0,
      toUserId: parent?.user?.id ?? null,
      content
    });

    ElMessage.success("发布成功");
    if (parent) {
      parent.draft = "";
      parent.showReply = false;
    } else {
      commentState.mainContent = "";
    }
    loadComments(commentState.selectedProjectId);
  } catch {
    ElMessage.error("发布失败");
  }
}

/**
 * 删除评论
 */
function handleRemove(id) {
  ElMessageBox.confirm('确定要删除这条言论吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await commentApi.remove(id);
    ElMessage.success("已删除");
    loadComments(commentState.selectedProjectId);
  }).catch(() => {
  });
}

function toggleReply(item) {
  item.showReply = !item.showReply;
}

// 初始化：加载课题列表，同时处理 URL 参数跳转
onMounted(async () => {
  await loadProjects();
  const queryId = Number(route.query.projectId);
  if (queryId) {
    commentState.selectedProjectId = queryId;
    loadComments(queryId);
  } else if (page.value?.list?.length > 0) {
    selectProject(page.value.list[0].id);
  }
});

// 监听 URL 变化，支持外部跳转联动
watch(() => route.query.projectId, (newVal) => {
  if (newVal && Number(newVal) !== commentState.selectedProjectId) {
    commentState.selectedProjectId = Number(newVal);
    loadComments(commentState.selectedProjectId);
  }
});
</script>

<template>
  <div class="comment-page">
    <el-row :gutter="20">
      <el-col :md="8" :lg="6">
        <el-card shadow="never" class="side-card">
          <template #header>
            <div class="card-header">
              <span class="title">课题列表</span>
              <el-button :icon="Refresh" circle size="small" @click="loadProjects"/>
            </div>
          </template>

          <el-input
              v-model="query.title"
              placeholder="搜索课题名称..."
              :prefix-icon="Search"
              clearable
              @change="changeProjectPage(1)"
              class="m-b-15"
          />

          <div class="project-list" v-loading="loading">
            <div
                v-for="item in page.list"
                :key="item.id"
                :class="['project-item', { active: commentState.selectedProjectId === item.id }]"
                @click="selectProject(item.id)"
            >
              <div class="item-title">{{ item.title }}</div>
              <div class="item-meta">#{{ item.id }} · 状态: {{ item.status === 1 ? '进行中' : '未开始' }}</div>
            </div>
          </div>

          <div class="side-footer">
            <el-pagination
                small
                layout="prev, pager, next"
                :total="page.total"
                :page-size="query.pageSize"
                @current-change="changeProjectPage"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :md="16" :lg="18">
        <el-card shadow="never" class="main-card">
          <div v-if="commentState.selectedProjectId" v-loading="commentState.loading">
            <div class="main-reply-area">
              <h3 class="discussion-title">
                课题讨论交流 <small>({{ commentState.comments.length }} 条根评论)</small>
              </h3>
              <div class="reply-box">
                <el-avatar :size="48" class="user-avatar" :src="AVATAR_BASE_URL + authStore.userInfo?.user?.avatar">
                  {{ authStore.userInfo?.user?.nickname?.charAt(0) || '用' }}
                </el-avatar>
                <div class="input-container">
                  <el-input
                      v-model="commentState.mainContent"
                      type="textarea"
                      :rows="3"
                      placeholder="发一条友善的评论吧..."
                      resize="none"
                  />
                  <div class="input-actions">
                    <el-button type="primary" @click="submitComment()">发布</el-button>
                  </div>
                </div>
              </div>
            </div>

            <el-divider/>

            <div class="comment-list">
              <div v-for="comment in commentState.comments" :key="comment.id" class="root-comment-item">
                <div class="comment-avatar">
                  <el-avatar :size="40" :src="AVATAR_BASE_URL + comment.user?.avatar"/>
                </div>
                <div class="comment-content">
                  <div class="c-user">
                    {{ comment.user?.nickname || comment.user?.username || '用户' }}
                    <el-tag v-if="comment.user?.id === authStore.userId" size="small" type="info" class="m-l-5">我
                    </el-tag>
                  </div>
                  <div class="c-text">{{ comment.content }}</div>
                  <div class="c-footer">
                    <span class="c-time">{{ timeUtil.formatDate(comment.createTime, 'YYYY-MM-DD HH:mm:ss') }}</span>
                    <el-button link class="reply-btn" @click="toggleReply(comment)">
                      <el-icon>
                        <ChatDotRound/>
                      </el-icon>
                      回复
                    </el-button>
                    <el-button
                        v-if="canDeleteAny || comment.user?.id === authStore.userId"
                        link type="danger" @click="handleRemove(comment.id)"
                    >
                      <el-icon>
                        <Delete/>
                      </el-icon>
                      删除
                    </el-button>
                  </div>

                  <!-- 回复输入框 - 主评论 -->
                  <div v-if="comment.showReply" class="inline-reply-box">
                    <el-input
                        v-model="comment.draft"
                        size="small"
                        :placeholder="`回复 @${comment.user?.nickname || comment.user?.username}...`"
                    />
                    <el-button type="primary" size="small" @click="submitComment(comment)">发布</el-button>
                  </div>

                  <!-- 回复列表 - 使用 replies 字段 -->
                  <div v-if="comment.replies && comment.replies.length > 0" class="sub-comment-list">
                    <div v-for="reply in comment.replies" :key="reply.id" class="sub-comment-item">
                      <div class="sub-user">
                        <el-avatar :size="24" :src="AVATAR_BASE_URL + reply.user?.avatar" class="sub-avatar"/>
                        <span class="sub-name">{{ reply.user?.nickname || reply.user?.username }}</span>
                        <span v-if="reply.toUser" class="reply-info">
                          回复 <span class="to-user">@{{ reply.toUser.nickname || reply.toUser.username }}</span>
                        </span>
                      </div>
                      <div class="sub-text">{{ reply.content }}</div>
                      <div class="c-footer">
                        <span class="c-time">{{ timeUtil.formatDate(reply.createTime, 'YYYY-MM-DD HH:mm:ss') }}</span>
                        <el-button link size="small" @click="toggleReply(reply)">回复</el-button>
                        <el-button
                            v-if="canDeleteAny || reply.user?.id === authStore.userId"
                            link type="danger" size="small" @click="handleRemove(reply.id)"
                        >删除
                        </el-button>
                      </div>

                      <!-- 回复输入框 - 二级回复 -->
                      <div v-if="reply.showReply" class="inline-reply-box">
                        <el-input
                            v-model="reply.draft"
                            size="small"
                            :placeholder="`回复 @${reply.user?.nickname || reply.user?.username}...`"
                        />
                        <el-button type="primary" size="small" @click="submitComment(reply)">发布</el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="请先在左侧选择一个课题"/>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.comment-page {
  padding: 20px;
  background-color: transparent;
  min-height: 100vh;
}

/* 左侧列表样式 */
.side-card {
  height: calc(100vh - 40px);
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .title {
  font-weight: bold;
  font-size: 16px;
}

.project-list {
  flex: 1;
  overflow-y: auto;
  margin-top: 10px;
}

.project-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: background 0.15s, border-color 0.15s;
  border: 1px solid transparent;
}

.project-item:hover {
  background-color: #e3e5e7;
}

.project-item.active {
  background-color: var(--panel);
  border-color: #00aeec;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.item-title {
  font-size: 14px;
  font-weight: 600;
  color: #18191c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  font-size: 12px;
  color: #9499a0;
  margin-top: 4px;
}

.side-footer {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f1f2f3;
}

/* 右侧主评论区 */
.main-card {
  min-height: calc(100vh - 40px);
}

.discussion-title {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  color: #18191c;
}

.discussion-title small {
  font-weight: normal;
  font-size: 13px;
  color: #9499a0;
  margin-left: 8px;
}

/* 发布框 */
.main-reply-area {
  margin-bottom: 30px;
}

.reply-box {
  display: flex;
  gap: 16px;
}

.user-avatar {
  background-color: #00aeec;
  flex-shrink: 0;
}

.input-container {
  flex: 1;
  position: relative;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

/* 评论树 */
.root-comment-item {
  display: flex;
  gap: 16px;
  padding: 20px 0;
  border-bottom: 1px solid #f1f2f3;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
}

.c-user {
  font-size: 13px;
  font-weight: bold;
  color: #61666d;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.c-text {
  font-size: 15px;
  color: #18191c;
  line-height: 24px;
  white-space: pre-wrap;
  margin-bottom: 8px;
}

.c-footer {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 12px;
}

.c-time {
  color: #9499a0;
}

.reply-btn {
  color: #9499a0;
  cursor: pointer;
}

.reply-btn:hover {
  color: #00aeec;
}

/* 楼中楼列表 */
.sub-comment-list {
  margin-top: 15px;
  background-color: var(--panel-strong);
  border-radius: 6px;
  padding: 12px;
}

.sub-comment-item {
  padding: 8px 0;
  border-bottom: 1px dashed #e5e9f0;
}

.sub-comment-item:last-child {
  border-bottom: none;
}

.sub-user {
  font-size: 13px;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.sub-avatar {
  width: 24px;
  height: 24px;
}

.sub-name {
  color: #61666d;
  font-weight: 500;
}

.reply-info {
  margin: 0 4px;
  color: #18191c;
}

.to-user {
  color: #00aeec;
}

.sub-text {
  font-size: 14px;
  color: #18191c;
  margin-bottom: 4px;
  padding-left: 30px;
}

/* 内联回复框 */
.inline-reply-box {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  max-width: 600px;
}

.m-b-15 {
  margin-bottom: 15px;
}

.m-l-5 {
  margin-left: 5px;
}
</style>
