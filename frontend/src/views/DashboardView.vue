<script setup>
import {computed, onMounted, onBeforeUnmount, reactive, ref, nextTick, watch} from "vue";
import {announcementApi, progressApi, projectApi, userApi} from "../api/modules";
import StatCard from "../components/StatCard.vue";
import {useAuthStore} from "../stores/auth";
import {useRouter} from "vue-router";
import * as echarts from 'echarts';
import {
  PROJECT_STATUS_OPTIONS,
  PROGRESS_STATUS_OPTIONS,
  getLabel
} from "../utils/constants";
import {
  Promotion, List, Bell, User, DataLine, EditPen, Reading, ChatLineRound, TrendCharts, Document
} from "@element-plus/icons-vue";

const authStore = useAuthStore();
const router = useRouter();

// 感知全局暗色主题
const isDark = computed(() => document.documentElement.classList.contains('dark'));

// 监听 html class 变化（用 MutationObserver 实现响应式）
const darkMode = ref(document.documentElement.classList.contains('dark'));
let observer = null;

function watchDarkMode() {
  observer = new MutationObserver(() => {
    darkMode.value = document.documentElement.classList.contains('dark');
  });
  observer.observe(document.documentElement, {attributes: true, attributeFilter: ['class']});
}

// echarts 主题色
const chartTextColor = computed(() => darkMode.value ? '#9b8f84' : '#334155');
const chartBgColor = computed(() => darkMode.value ? '#1e1e22' : '#ffffff');
const chartBorderColor = computed(() => darkMode.value ? '#2a2a2e' : '#ffffff');

const isStudent = computed(() => authStore.role === 0);
const isTeacher = computed(() => authStore.role === 1);
const isAdmin = computed(() => authStore.role === 2);

const greeting = computed(() => {
  const h = new Date().getHours();
  if (h < 6) return '深夜好';
  if (h < 11) return '早上好';
  if (h < 14) return '中午好';
  if (h < 18) return '下午好';
  return '晚上好';
});

const state = reactive({
  users: {total: 0, list: []},
  projects: {total: 0, list: []},
  progress: {total: 0, list: []},
  announcements: {total: 0, list: []},
  // 教师专用
  myStudentProjects: {total: 0, list: []},
  // 管理员专用
  teacherCount: 0,
});

// ===== 图表引用 =====
const pieRef = ref(null);
const barRef = ref(null);
let pieChart = null;
let barChart = null;
let lineChart = null;

// ===== 图表初始化 =====
function initStudentCharts() {
  if (pieRef.value) {
    pieChart?.dispose();
    pieChart = echarts.init(pieRef.value);
    const statusStats = PROJECT_STATUS_OPTIONS.map(opt => ({
      name: opt.label,
      value: state.projects.list.filter(p => p.status === opt.value).length
    }));
    pieChart.setOption({
      backgroundColor: chartBgColor.value,
      title: {text: '我的课题状态', left: 'center', top: 8, textStyle: {fontSize: 13, color: chartTextColor.value}},
      tooltip: {trigger: 'item', formatter: '{b}: {c} ({d}%)'},
      legend: {bottom: 4, textStyle: {fontSize: 11, color: chartTextColor.value}},
      color: ['#f59e0b', '#22c55e', '#ef4444', '#6366f1'],
      series: [{
        type: 'pie', radius: ['38%', '65%'],
        avoidLabelOverlap: false,
        itemStyle: {borderRadius: 8, borderColor: chartBorderColor.value, borderWidth: 2},
        label: {show: false},
        data: statusStats
      }]
    });
  }
}

function initTeacherCharts() {
  if (barRef.value) {
    barChart?.dispose();
    barChart = echarts.init(barRef.value);
    const statusData = PROGRESS_STATUS_OPTIONS.map(opt => ({
      name: opt.label,
      value: state.progress.list.filter(p => p.status === opt.value).length
    }));
    barChart.setOption({
      backgroundColor: chartBgColor.value,
      title: {text: '学生进度状态分布', left: 'center', top: 8, textStyle: {fontSize: 13, color: chartTextColor.value}},
      tooltip: {trigger: 'axis'},
      color: ['#6366f1', '#22c55e', '#ef4444'],
      xAxis: {type: 'category', data: statusData.map(s => s.name), axisLabel: {fontSize: 11, color: chartTextColor.value}, axisLine: {lineStyle: {color: chartTextColor.value}}},
      yAxis: {type: 'value', minInterval: 1, axisLabel: {color: chartTextColor.value}, splitLine: {lineStyle: {color: darkMode.value ? 'rgba(255,255,255,0.08)' : '#e8ecf0'}}},
      grid: {left: '5%', right: '5%', bottom: '15%', top: '20%', containLabel: true},
      series: [{
        type: 'bar', barWidth: '45%',
        data: statusData.map((s, i) => ({value: s.value, itemStyle: {color: ['#f59e0b', '#22c55e', '#ef4444'][i]}})),
        showBackground: true,
        backgroundStyle: {color: darkMode.value ? 'rgba(255,255,255,0.04)' : 'rgba(220,220,220,0.2)'},
        itemStyle: {borderRadius: [4, 4, 0, 0]}
      }]
    });
  }
  if (pieRef.value) {
    pieChart?.dispose();
    pieChart = echarts.init(pieRef.value);
    const statusStats = PROJECT_STATUS_OPTIONS.map(opt => ({
      name: opt.label,
      value: state.myStudentProjects.list.filter(p => p.status === opt.value).length
    }));
    pieChart.setOption({
      backgroundColor: chartBgColor.value,
      title: {text: '我的学生课题审核状态', left: 'center', top: 8, textStyle: {fontSize: 13, color: chartTextColor.value}},
      tooltip: {trigger: 'item', formatter: '{b}: {c} ({d}%)'},
      legend: {bottom: 4, textStyle: {fontSize: 11, color: chartTextColor.value}},
      color: ['#f59e0b', '#22c55e', '#ef4444', '#6366f1'],
      series: [{
        type: 'pie', radius: ['38%', '65%'],
        avoidLabelOverlap: false,
        itemStyle: {borderRadius: 8, borderColor: chartBorderColor.value, borderWidth: 2},
        label: {show: false},
        data: statusStats
      }]
    });
  }
}

function initAdminCharts() {
  if (pieRef.value) {
    pieChart?.dispose();
    pieChart = echarts.init(pieRef.value);
    const statusStats = PROJECT_STATUS_OPTIONS.map(opt => ({
      name: opt.label,
      value: state.projects.list.filter(p => p.status === opt.value).length
    }));
    pieChart.setOption({
      backgroundColor: chartBgColor.value,
      title: {text: '全平台课题审核分布', left: 'center', top: 8, textStyle: {fontSize: 13, color: chartTextColor.value}},
      tooltip: {trigger: 'item', formatter: '{b}: {c} ({d}%)'},
      legend: {bottom: 4, textStyle: {fontSize: 11, color: chartTextColor.value}},
      color: ['#f59e0b', '#22c55e', '#ef4444', '#6366f1'],
      series: [{
        type: 'pie', radius: ['38%', '65%'],
        avoidLabelOverlap: false,
        itemStyle: {borderRadius: 8, borderColor: chartBorderColor.value, borderWidth: 2},
        label: {show: false},
        data: statusStats
      }]
    });
  }
  if (barRef.value) {
    barChart?.dispose();
    barChart = echarts.init(barRef.value);
    barChart.setOption({
      backgroundColor: chartBgColor.value,
      title: {text: '平台用户角色分布', left: 'center', top: 8, textStyle: {fontSize: 13, color: chartTextColor.value}},
      tooltip: {trigger: 'axis'},
      xAxis: {type: 'category', data: ['学生', '指导教师', '管理员'], axisLabel: {fontSize: 11, color: chartTextColor.value}, axisLine: {lineStyle: {color: chartTextColor.value}}},
      yAxis: {type: 'value', minInterval: 1, axisLabel: {color: chartTextColor.value}, splitLine: {lineStyle: {color: darkMode.value ? 'rgba(255,255,255,0.08)' : '#e8ecf0'}}},
      grid: {left: '5%', right: '5%', bottom: '15%', top: '20%', containLabel: true},
      series: [{
        type: 'bar', barWidth: '40%',
        data: [
          {value: state.users.total, itemStyle: {color: '#6366f1', borderRadius: [4, 4, 0, 0]}},
          {value: state.teacherCount, itemStyle: {color: '#22c55e', borderRadius: [4, 4, 0, 0]}},
          {value: 1, itemStyle: {color: '#f59e0b', borderRadius: [4, 4, 0, 0]}}
        ],
        showBackground: true,
        backgroundStyle: {color: darkMode.value ? 'rgba(255,255,255,0.04)' : 'rgba(220,220,220,0.2)'}
      }]
    });
  }
}

const handleResize = () => {
  pieChart?.resize();
  barChart?.resize();
  lineChart?.resize();
};

// ===== 数据加载 =====
async function loadStats() {
  const base = {pageNo: 1, pageSize: 100};
  if (isStudent.value) {
    // 学生：加载自己的课题 + 公告
    const [projects, announcements] = await Promise.all([
      projectApi.list({...base, sid: authStore.userId}),
      announcementApi.list({...base, status: 1})
    ]);
    state.projects = projects;
    state.announcements = announcements;
    nextTick(initStudentCharts);

  } else if (isTeacher.value) {
    // 教师：加载指导的课题 + 全部进度 + 公告
    const [myProjects, allProgress, announcements] = await Promise.all([
      projectApi.list({...base, tid: authStore.userId}),
      progressApi.list(base),
      announcementApi.list({...base, status: 1})
    ]);
    state.myStudentProjects = myProjects;
    state.progress = allProgress;
    state.announcements = announcements;
    // 同时加载待审核课题（全部）
    const pending = await projectApi.list({...base, status: 0});
    state.projects = pending;
    nextTick(initTeacherCharts);

  } else if (isAdmin.value) {
    // 管理员：加载全部数据
    const [projects, students, teachers, announcements, progress] = await Promise.all([
      projectApi.list(base),
      userApi.list({...base, role: 0}),
      userApi.list({...base, role: 1}),
      announcementApi.list(base),
      progressApi.list(base)
    ]);
    state.projects = projects;
    state.users = students;
    state.teacherCount = teachers.total ?? 0;
    state.announcements = announcements;
    state.progress = progress;
    nextTick(initAdminCharts);
  }
}

onMounted(() => {
  loadStats();
  watchDarkMode();
  window.addEventListener('resize', handleResize);
});
onBeforeUnmount(() => {
  observer?.disconnect();
  window.removeEventListener('resize', handleResize);
  pieChart?.dispose();
  barChart?.dispose();
  lineChart?.dispose();
});

// 主题切换时重绘图表
watch(darkMode, () => {
  nextTick(() => {
    if (isStudent.value) initStudentCharts();
    else if (isTeacher.value) initTeacherCharts();
    else if (isAdmin.value) initAdminCharts();
  });
});
</script>

<template>
  <div class="dash-wrap">

    <!-- ===================== 学生首页 ===================== -->
    <template v-if="isStudent">
      <!-- 欢迎横幅 -->
      <div class="hero student-hero">
        <div class="hero-left">
          <div class="hero-greeting">{{ greeting }}，{{ authStore.userInfo?.user?.nickname || '同学' }} !</div>
          <div class="hero-actions">
            <el-button type="primary" @click="router.push('/projects')">
              <el-icon>
                <EditPen/>
              </el-icon>
              去申报课题
            </el-button>
            <el-button plain @click="router.push('/progress')">
              <el-icon>
                <List/>
              </el-icon>
              查看我的进度
            </el-button>
          </div>
        </div>
      </div>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-row">
        <el-col :xs="12" :sm="6">
          <div class="stat-mini">
            <div class="stat-mini-val">{{ state.projects.total }}</div>
            <div class="stat-mini-label">我的申报</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-mini">
            <div class="stat-mini-val">{{
                state.projects.list.filter(i => i.status === 1).length
              }}
            </div>
            <div class="stat-mini-label">已通过</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-mini">
            <div class="stat-mini-val">{{
                state.projects.list.filter(i => i.status === 0).length
              }}
            </div>
            <div class="stat-mini-label">待审核</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-mini">
            <div class="stat-mini-val">
              {{ state.projects.list.filter(i => i.status === 2 || i.status === 3).length }}
            </div>
            <div class="stat-mini-label">需处理</div>
          </div>
        </el-col>
      </el-row>

      <!-- 主内容区 -->
      <el-row :gutter="20" class="content-row">
        <el-col :md="10" :sm="24">
          <el-card shadow="hover" class="chart-card">
            <div ref="pieRef" style="height:260px"></div>
          </el-card>
        </el-col>
        <el-col :md="14" :sm="24">
          <el-card shadow="hover" class="list-card">
            <template #header>
              <div class="card-head">
                <span>我的课题申报动态</span>
                <el-button link type="primary" @click="router.push('/projects')">全部</el-button>
              </div>
            </template>
            <div v-if="!state.projects.list.length" class="empty-wrap">
              <el-empty description="还没有申报过课题" :image-size="64">
                <el-button type="primary" size="small" @click="router.push('/projects')">立即申报</el-button>
              </el-empty>
            </div>
            <div v-else>
              <div v-for="item in state.projects.list.slice(0,5)" :key="item.id" class="list-row">
                <div class="list-row-main">
                  <div class="list-row-title">{{ item.title }}</div>
                  <div class="list-row-meta">{{ item.type || '未分类' }}{{
                      item.opinion ? ' · ' + item.opinion : ''
                    }}
                  </div>
                </div>
                <el-tag size="small"
                        :type="item.status===1?'success':item.status===0?'warning':item.status===3?'':item.status===2?'danger':''">
                  {{ getLabel(PROJECT_STATUS_OPTIONS, item.status) }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 最新公告 -->
      <el-card shadow="hover" class="ann-card">
        <template #header>
          <div class="card-head">
            <span><el-icon><Bell/></el-icon> 最新公告</span>
            <el-button link type="primary" @click="router.push('/announcements')">查看全部</el-button>
          </div>
        </template>
        <el-empty v-if="!state.announcements.list.length" description="暂无公告" :image-size="48"/>
        <div v-else class="ann-grid">
          <div v-for="ann in state.announcements.list.slice(0,4)" :key="ann.id" class="ann-item"
               @click="router.push('/announcements')">
            <el-tag size="small" :type="ann.priority===2?'danger':ann.priority===1?'warning':'info'"
                    style="margin-right:8px;flex-shrink:0">
              {{ ann.priority === 2 ? '置顶' : ann.priority === 1 ? '紧急' : '普通' }}
            </el-tag>
            <span class="ann-title">{{ ann.title }}</span>
            <span class="ann-time">{{ ann.createTime ? ann.createTime.slice(0, 10) : '' }}</span>
          </div>
        </div>
        <div class="remind-tip">
          <el-icon>
            <Bell/>
          </el-icon>
          记得按时提交周进展报告，保持与导师的沟通！
        </div>
      </el-card>
    </template>

    <!-- ===================== 教师首页 ===================== -->
    <template v-else-if="isTeacher">
      <!-- 欢迎横幅 -->
      <div class="hero teacher-hero">
        <div class="hero-left">
          <div class="hero-greeting">{{ greeting }}，{{ authStore.userInfo?.user?.nickname || '老师' }}</div>
          <div class="hero-actions">
            <el-button type="primary" @click="router.push('/projects')">
              <el-icon>
                <Promotion/>
              </el-icon>
              审核课题申请
            </el-button>
            <el-button plain @click="router.push('/progress')">
              <el-icon>
                <DataLine/>
              </el-icon>
              查阅学生进度
            </el-button>
            <el-button plain @click="router.push('/chat')">
              <el-icon>
                <ChatLineRound/>
              </el-icon>
              与学生沟通
            </el-button>
          </div>
        </div>
      </div>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-row">
        <el-col :xs="12" :sm="6">
          <div class="stat-mini">
            <div class="stat-mini-val">{{
                state.projects.list.filter(i => i.status === 0).length
              }}
            </div>
            <div class="stat-mini-label">待审课题</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-mini">
            <div class="stat-mini-val">{{ state.myStudentProjects.total }}</div>
            <div class="stat-mini-label">我的学生</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-mini">
            <div class="stat-mini-val">{{
                state.progress.list.filter(i => i.status === 0).length
              }}
            </div>
            <div class="stat-mini-label">待阅进度</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-mini">
            <div class="stat-mini-val">
              {{ state.myStudentProjects.list.filter(i => i.status === 1).length }}
            </div>
            <div class="stat-mini-label">通过课题</div>
          </div>
        </el-col>
      </el-row>

      <!-- 主内容区 -->
      <el-row :gutter="20" class="content-row">
        <el-col :md="10" :sm="24">
          <el-card shadow="hover" class="chart-card">
            <div ref="pieRef" style="height:240px"></div>
          </el-card>
          <el-card shadow="hover" class="chart-card" style="margin-top:16px">
            <div ref="barRef" style="height:220px"></div>
          </el-card>
        </el-col>
        <el-col :md="14" :sm="24">

          <el-card shadow="hover" class="list-card">
            <template #header>
              <div class="card-head">
                <span>待审课题列表</span>
                <el-button link type="primary" @click="router.push('/projects')">全部</el-button>
              </div>
            </template>
            <div v-if="!state.projects.list.filter(i=>i.status===0).length" class="empty-wrap">
              <el-empty description="暂无待审课题" :image-size="64"/>
            </div>
            <div v-else>
              <div v-for="item in state.projects.list.filter(i=>i.status===0).slice(0,6)" :key="item.id"
                   class="list-row pending-row" @click="router.push('/projects')">
                <div class="list-row-main">
                  <div class="list-row-title">{{ item.title }}</div>
                  <div class="list-row-meta">学生 ID：{{ item.sid }}{{ item.type ? ' · ' + item.type : '' }}</div>
                </div>
                <el-tag size="small" type="warning">待审核</el-tag>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="list-card" style="margin-top:16px">
            <template #header>
              <div class="card-head">
                <span>最近学生进度</span>
                <el-button link type="primary" @click="router.push('/progress')">全部</el-button>
              </div>
            </template>
            <div v-if="!state.progress.list.filter(i=>i.status===0).length" class="empty-wrap">
              <el-empty description="暂无待阅进度" :image-size="48"/>
            </div>
            <div v-else>
              <div v-for="item in state.progress.list.filter(i=>i.status===0).slice(0,4)" :key="item.id"
                   class="list-row" @click="router.push('/progress')">
                <div class="list-row-main">
                  <div class="list-row-title">{{ item.title || '进度报告' }}</div>
                  <div class="list-row-meta">{{ item.createTime ? item.createTime.slice(0, 10) : '' }}</div>
                </div>
                <el-tag size="small" type="warning">待阅</el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 公告 -->
      <el-card shadow="hover" class="ann-card">
        <template #header>
          <div class="card-head">
            <span><el-icon><Bell/></el-icon> 平台公告</span>
            <el-button link type="primary" @click="router.push('/announcements')">查看全部</el-button>
          </div>
        </template>
        <el-empty v-if="!state.announcements.list.length" description="暂无公告" :image-size="48"/>
        <div v-else class="ann-grid">
          <div v-for="ann in state.announcements.list.slice(0,4)" :key="ann.id" class="ann-item"
               @click="router.push('/announcements')">
            <el-tag size="small" :type="ann.priority===2?'danger':ann.priority===1?'warning':'info'"
                    style="margin-right:8px;flex-shrink:0">
              {{ ann.priority === 2 ? '置顶' : ann.priority === 1 ? '紧急' : '普通' }}
            </el-tag>
            <span class="ann-title">{{ ann.title }}</span>
            <span class="ann-time">{{ ann.createTime ? ann.createTime.slice(0, 10) : '' }}</span>
          </div>
        </div>
      </el-card>
    </template>

    <!-- ===================== 管理员首页 ===================== -->
    <template v-else-if="isAdmin">
      <!-- 欢迎横幅 -->
      <div class="hero admin-hero">
        <div class="hero-left">
          <div class="hero-greeting">{{ greeting }}，{{ authStore.userInfo?.user?.nickname || '管理员' }} !</div>
          <div class="hero-actions">
            <el-button type="primary" @click="router.push('/projects')">
              <el-icon>
                <Document/>
              </el-icon>
              课题审核管理
            </el-button>
            <el-button plain @click="router.push('/users')">
              <el-icon>
                <User/>
              </el-icon>
              用户档案管理
            </el-button>
            <el-button plain @click="router.push('/announcements')">
              <el-icon>
                <Bell/>
              </el-icon>
              发布公告
            </el-button>
          </div>
        </div>
      </div>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stat-row">
        <el-col :xs="12" :sm="6">
          <div class="stat-mini">
            <div class="stat-mini-val">{{ state.users.total }}</div>
            <div class="stat-mini-label">在校学生</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-mini">
            <div class="stat-mini-val">{{ state.teacherCount }}</div>
            <div class="stat-mini-label">指导教师</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-mini">
            <div class="stat-mini-val">{{
                state.projects.list.filter(i => i.status === 0).length
              }}
            </div>
            <div class="stat-mini-label">待审课题</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-mini">
            <div class="stat-mini-val">{{
                state.progress.list.filter(i => i.status === 0).length
              }}
            </div>
            <div class="stat-mini-label">待阅进度</div>
          </div>
        </el-col>
      </el-row>

      <!-- 图表 -->
      <el-row :gutter="20" class="content-row">
        <el-col :md="10" :sm="24">
          <el-card shadow="hover" class="chart-card">
            <div ref="pieRef" style="height:280px"></div>
          </el-card>
        </el-col>
        <el-col :md="14" :sm="24">
          <el-card shadow="hover" class="chart-card" style="height:100%">
            <div ref="barRef" style="height:280px"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 待处理列表 -->
      <el-row :gutter="20" class="content-row">
        <el-col :md="12" :sm="24">
          <el-card shadow="hover" class="list-card">
            <template #header>
              <div class="card-head">
                <span>待审核课题</span>
                <el-button link type="primary" @click="router.push('/projects')">去审核</el-button>
              </div>
            </template>
            <el-empty v-if="!state.projects.list.filter(i=>i.status===0).length" description="暂无待审"
                      :image-size="56"/>
            <div v-else>
              <div v-for="item in state.projects.list.filter(i=>i.status===0).slice(0,5)" :key="item.id"
                   class="list-row pending-row" @click="router.push('/projects')">
                <div class="list-row-main">
                  <div class="list-row-title">{{ item.title }}</div>
                  <div class="list-row-meta">类型：{{ item.type || '未分类' }} · 学生 ID：{{ item.sid }}</div>
                </div>
                <el-tag size="small" type="warning">待审</el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :md="12" :sm="24">
          <el-card shadow="hover" class="list-card">
            <template #header>
              <div class="card-head">
                <span>公告管理</span>
                <el-button link type="primary" @click="router.push('/announcements')">去管理</el-button>
              </div>
            </template>
            <el-empty v-if="!state.announcements.list.length" description="暂无公告" :image-size="56"/>
            <div v-else>
              <div v-for="ann in state.announcements.list.slice(0,5)" :key="ann.id" class="list-row">
                <div class="list-row-main">
                  <div class="list-row-title">{{ ann.title }}</div>
                  <div class="list-row-meta">阅读量：{{ ann.viewCount || 0 }} ·
                    {{ ann.createTime ? ann.createTime.slice(0, 10) : '' }}
                  </div>
                </div>
                <el-tag size="small" :type="ann.status===1?'success':ann.status===0?'info':'danger'">
                  {{ ann.status === 1 ? '已发布' : ann.status === 0 ? '草稿' : '已废弃' }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

  </div>
</template>

<style scoped>
.dash-wrap {
  padding: 24px;
  background: transparent;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ===== 欢迎横幅 ===== */
.hero {
  height: 150px;
  border-radius: var(--r-card);
  padding: 32px 36px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}

.student-hero {
  background: linear-gradient(135deg, #334155 0%, #1e293b 100%);
  color: white;
}

.teacher-hero {
  background: linear-gradient(135deg, #334155 0%, #1e293b 100%);
  color: white;
}

.admin-hero {
  background: linear-gradient(135deg, #334155 0%, #1e293b 100%);
  color: white;
}

.hero-greeting {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 8px;
}

.hero-actions {
  margin-top: 16px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-actions .el-button {
  border-radius: var(--r-btn);
}

/* ===== 统计小卡片 ===== */
.stat-row {
  margin: 0 !important;
}

.stat-mini {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--r-card);
  padding: 20px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s, background 0.3s;
}

.stat-mini:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.stat-mini-val {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--brand);
}

.stat-mini-label {
  font-size: 13px;
  color: var(--muted);
  margin-top: 6px;
}

/* ===== 图表卡片 ===== */
.chart-card {
  border-radius: var(--r-card);
}

.content-row {
  margin: 0 !important;
}

/* ===== 列表卡片 ===== */
.list-card {
  border-radius: var(--r-card);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: var(--text);
  font-size: 14px;
}

.list-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
  gap: 12px;
}

.list-row:last-child {
  border: none;
}

.pending-row {
  cursor: pointer;
}

.pending-row:hover {
  background: var(--panel);
  border-radius: 8px;
  padding-left: 6px;
}

.list-row-main {
  flex: 1;
  min-width: 0;
}

.list-row-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.list-row-meta {
  font-size: 11px;
  color: var(--muted);
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty-wrap {
  padding: 16px 0;
}

/* ===== 公告卡片 ===== */
.ann-card {
  border-radius: var(--r-card);
}

.ann-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ann-item {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 6px;
  border-radius: var(--r-tag);
  transition: background 0.15s;
}

.ann-item:hover {
  background: var(--panel);
}

.ann-title {
  font-size: 13px;
  color: var(--text);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ann-time {
  font-size: 11px;
  color: var(--muted);
  margin-left: 8px;
  flex-shrink: 0;
}

.remind-tip {
  margin-top: 14px;
  font-size: 12px;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
  padding: 10px 14px;
  border-radius: var(--r-tag);
  display: flex;
  align-items: center;
  gap: 6px;
}

:deep(.el-card__header) {
  border-bottom: 1px solid var(--line);
  padding: 12px 16px;
}

:deep(.el-card) {
  background: var(--panel) !important;
  border-color: var(--line) !important;
}

:deep(.el-card__body) {
  background: transparent;
}
</style>
