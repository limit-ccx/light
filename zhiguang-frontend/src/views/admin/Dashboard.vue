<template>
  <div class="ai-dashboard">
    <!-- ===== 左主列 ===== -->
    <div class="dash-main">
      <!-- 渐变玻璃主卡 -->
      <section class="hero-card">
        <span class="hero-blob hero-blob--ice"></span>
        <span class="hero-blob hero-blob--violet"></span>
        <span class="hero-grid"></span>

        <header class="hero-head">
          <div class="hero-identity">
            <span class="hero-avatar">
              <span class="hero-avatar-inner">
                <img
                  v-if="authStore.user?.avatar && !avatarBroken"
                  :src="authStore.user.avatar"
                  alt="运营助手头像"
                  @error="avatarBroken = true"
                />
                <span v-else class="hero-initial">{{ avatarInitial }}</span>
              </span>
              <span class="hero-status"></span>
            </span>
            <div class="hero-copy">
              <p class="hero-eyebrow">
                <span class="hero-spark"></span> AI 运营助手
              </p>
              <h1 class="hero-title">{{ greeting }}，{{ authStore.user?.username || '管理员' }}</h1>
              <p class="hero-desc">
                今天还有
                <strong>{{ totalBacklog }}</strong>
                项待处理，其中待审文章
                <strong>{{ dashboard.pendingReviewArticles || 0 }}</strong>
                篇
              </p>
            </div>
          </div>

          <div class="hero-actions">
            <button class="hero-btn hero-btn--glass" @click="router.push('/admin/content')">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/>
              </svg>
              审核待办
            </button>
            <button class="hero-btn hero-btn--solid" @click="router.push('/admin/config')">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
                <path d="M12 5v14M5 12h14"/>
              </svg>
              发布公告
            </button>
          </div>
        </header>

        <div class="hero-body">
          <!-- 任务进度列表 -->
          <div class="hero-tasks">
            <div class="task-head">
              <span>队列</span>
              <span>待处理</span>
              <span>今日已处理</span>
              <span>清理率</span>
            </div>

            <template v-if="queueLoading">
              <div v-for="n in 4" :key="n" class="task-row task-row--sk">
                <span class="sk-bar"></span>
              </div>
            </template>

            <div v-for="q in queueProgress" v-else :key="q.label" class="task-row">
              <span class="task-name">
                <span class="task-dot" :class="`dot--${q.tone}`"></span>
                {{ q.label }}
              </span>
              <span class="task-pending">{{ q.pending }}</span>
              <span class="task-done">{{ q.done }}</span>
              <span class="task-rate">
                <span class="task-rate-track">
                  <span class="task-rate-fill" :class="`fill--${q.tone}`" :style="{ width: q.percent + '%' }"></span>
                </span>
                <em>{{ q.percent }}%</em>
              </span>
            </div>
          </div>

          <!-- 半透明信息面板 -->
          <aside class="hero-panel">
            <p class="hero-panel-title">今日重点</p>
            <ul class="hero-panel-list">
              <li>
                <span>新增用户</span>
                <strong>{{ dashboard.todayNewUsers || 0 }}</strong>
              </li>
              <li>
                <span>新增文章</span>
                <strong>{{ dashboard.todayNewArticles || 0 }}</strong>
              </li>
              <li>
                <span>新增评论</span>
                <strong>{{ dashboard.todayNewComments || 0 }}</strong>
              </li>
              <li>
                <span>AI 调用</span>
                <strong>{{ dashboard.aiCallCount || 0 }}</strong>
              </li>
            </ul>
            <div class="hero-panel-foot">
              <span class="admin-dot admin-dot--ok"></span>
              {{ healthLabel }}
            </div>
          </aside>
        </div>

        <footer class="hero-foot">
          <button class="hero-round" @click="router.push('/admin/statistics')" aria-label="数据统计">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 20V10M12 20V4M6 20v-6"/></svg>
          </button>
          <button class="hero-btn hero-btn--glass hero-btn--sm" @click="router.push('/admin/logs')">
            查看治理记录
          </button>
          <button class="hero-round" @click="refreshAll" aria-label="刷新">
            <svg
              width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
              :class="{ 'is-spinning': refreshing }"
            >
              <path d="M21 12a9 9 0 1 1-3-6.7"/><path d="M21 4v5h-5"/>
            </svg>
          </button>
          <span class="hero-foot-note">数据每 60 秒自动更新</span>
        </footer>
      </section>

      <!-- 底部：团队表格 + 任务节点图 -->
      <div class="bottom-grid">
        <section class="glass-card team-card">
          <header class="card-head">
            <div>
              <h3 class="card-title">运营团队</h3>
              <p class="card-sub">共 {{ userOverview.adminCount || 0 }} 位管理员</p>
            </div>
            <div class="card-tools">
              <el-input
                v-model="memberKeyword"
                placeholder="搜索成员"
                clearable
                class="mini-search"
              >
                <template #prefix>
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.2-3.2"/></svg>
                </template>
              </el-input>
              <router-link to="/admin/users" class="card-link">管理</router-link>
            </div>
          </header>

          <div v-if="!canViewTeam" class="card-empty">
            <span class="admin-dot admin-dot--idle"></span>
            当前职位无成员明细权限，共 {{ userOverview.adminCount || 0 }} 位管理员
          </div>

          <div v-else-if="teamLoading" class="skeleton-list">
            <span v-for="n in 4" :key="n" class="sk-row"></span>
          </div>

          <div v-else-if="filteredMembers.length === 0" class="card-empty">
            <span class="admin-dot admin-dot--idle"></span>
            {{ memberKeyword ? '没有匹配的成员' : '暂无管理员账号' }}
          </div>

          <ul v-else class="member-list">
            <li v-for="m in filteredMembers" :key="m.id" class="member-item">
              <span class="member-avatar">
                <img
                  v-if="m.avatar && !brokenAvatars.has(m.id)"
                  :src="m.avatar"
                  :alt="m.username"
                  @error="brokenAvatars.add(m.id)"
                />
                <span v-else class="member-initial">{{ initial(m.username) }}</span>
              </span>
              <span class="member-main">
                <span class="member-name">{{ m.username }}</span>
                <span class="member-mail">{{ m.email }}</span>
              </span>
              <span class="member-role" :class="roleClass(m.role)">{{ roleLabel(m.role) }}</span>
              <span class="member-stat">
                <em>{{ m.articleCount }}</em> 文章
              </span>
              <span class="member-state">
                <span class="admin-dot" :class="m.status === 'BANNED' ? 'admin-dot--err' : 'admin-dot--ok'"></span>
                {{ m.status === 'BANNED' ? '已封禁' : '正常' }}
              </span>
            </li>
          </ul>
        </section>

        <section class="glass-card node-card">
          <header class="card-head">
            <div>
              <h3 class="card-title">治理任务节点</h3>
              <p class="card-sub">按工作流归并近 {{ trendRange }} 天的操作分布</p>
            </div>
            <el-radio-group v-model="trendRange" size="small" class="mini-segment">
              <el-radio-button :value="7">7 天</el-radio-button>
              <el-radio-button :value="30">30 天</el-radio-button>
            </el-radio-group>
          </header>

          <div v-if="ganttLoading" class="skeleton-list">
            <span v-for="n in 5" :key="n" class="sk-row"></span>
          </div>

          <div v-else-if="!hasWorkstreamData" class="card-empty">
            <span class="admin-dot admin-dot--idle"></span>
            统计窗口内暂无后台治理动作
          </div>

          <div v-else class="node-chart">
            <div class="node-axis">
              <span></span>
              <div class="node-axis-cells">
                <span v-for="d in axisLabels" :key="d.key">{{ d.label }}</span>
              </div>
            </div>

            <div v-for="ws in workstreams" :key="ws.key" class="node-row">
              <span class="node-label">
                <span class="admin-dot" :class="`dot--${ws.tone}`"></span>
                {{ ws.label }}
              </span>
              <div class="node-track">
                <span
                  v-if="ws.count > 0"
                  class="node-link"
                  :style="{ left: ws.leftPct + '%', width: ws.widthPct + '%' }"
                ></span>
                <span
                  v-for="p in ws.points"
                  :key="p.key"
                  class="node-point"
                  :class="[`point--${ws.tone}`, { 'is-peak': p.isPeak }]"
                  :style="{ left: p.leftPct + '%' }"
                  :title="`${ws.label} · ${p.label}：${p.count} 次`"
                >
                  <em>{{ p.count }}</em>
                </span>
              </div>
            </div>

            <footer class="node-legend">
              <span class="legend-item"><i class="legend-line"></i>动作跨度</span>
              <span class="legend-item"><i class="legend-point"></i>当日处理量</span>
            </footer>
          </div>
        </section>
      </div>
    </div>

    <!-- ===== 右侧数据卡 ===== -->
    <aside class="dash-side">
      <!-- 环形完成率 -->
      <section class="glass-card ring-card">
        <header class="card-head">
          <div>
            <h3 class="card-title">今日治理完成率</h3>
            <p class="card-sub">已处理 ÷（已处理 + 积压）</p>
          </div>
          <span class="pill-badge">{{ trendRange }}d</span>
        </header>

        <div v-if="queueLoading" class="ring-skeleton"></div>
        <div v-else class="ring-wrap">
          <div class="ring" :style="{ '--pct': completionRate }">
            <div class="ring-center">
              <strong>{{ completionRate }}%</strong>
              <span>完成率</span>
            </div>
          </div>
          <ul class="ring-legend">
            <li v-for="q in queueProgress" :key="q.label">
              <i :class="`dot--${q.tone}`"></i>
              <span>{{ q.label }}</span>
              <em>{{ q.pending }}</em>
            </li>
          </ul>
        </div>
      </section>

      <!-- 折线趋势 -->
      <section class="glass-card trend-card">
        <header class="card-head">
          <div>
            <h3 class="card-title">用户增长</h3>
            <p class="card-sub">近 {{ trendRange }} 天新增</p>
          </div>
          <span class="trend-delta" :class="deltaClass(dashboard.newUserTrend)">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><path d="M12 19V5M5 12l7-7 7 7"/></svg>
            {{ Math.abs(dashboard.newUserTrend || 0).toFixed(1) }}%
          </span>
        </header>

        <div v-if="trendLoading" class="spark-skeleton"></div>
        <template v-else>
          <div class="trend-big">
            <strong>{{ trendSummary ? trendSummary.total : 0 }}</strong>
            <span>区间新增</span>
          </div>
          <svg class="spark" viewBox="0 0 300 86" preserveAspectRatio="none" role="img" aria-label="新增用户趋势">
            <defs>
              <linearGradient id="sparkFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#598BFD" stop-opacity="0.28" />
                <stop offset="100%" stop-color="#598BFD" stop-opacity="0" />
              </linearGradient>
              <linearGradient id="sparkLine" x1="0" y1="0" x2="1" y2="0">
                <stop offset="0%" stop-color="#7A9DFE" />
                <stop offset="60%" stop-color="#598BFD" />
                <stop offset="100%" stop-color="#35B3C9" />
              </linearGradient>
            </defs>
            <path v-if="spark.area" :d="spark.area" fill="url(#sparkFill)" />
            <path v-if="spark.line" :d="spark.line" fill="none" stroke="url(#sparkLine)" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" vector-effect="non-scaling-stroke" />
            <circle v-if="spark.last" :cx="spark.last.x" :cy="spark.last.y" r="3.6" fill="#fff" stroke="#598BFD" stroke-width="2.4" vector-effect="non-scaling-stroke" />
          </svg>
          <div class="spark-axis">
            <span v-for="d in axisLabels" :key="d.key">{{ d.label }}</span>
          </div>
          <div class="trend-foot">
            <span>日均 <strong>{{ trendSummary ? trendSummary.avg : 0 }}</strong> 人</span>
            <span>峰值 <strong>{{ trendSummary ? trendSummary.peakLabel : '—' }}</strong></span>
          </div>
        </template>
      </section>

      <!-- 队列构成 -->
      <section class="glass-card load-card">
        <header class="card-head">
          <div>
            <h3 class="card-title">待办构成</h3>
            <p class="card-sub">积压 {{ totalBacklog }} 项</p>
          </div>
          <span class="glass-select">全部</span>
        </header>

        <div v-if="queueLoading" class="skeleton-list">
          <span v-for="n in 4" :key="n" class="sk-row"></span>
        </div>

        <ul v-else class="load-list">
          <li v-for="q in queueProgress" :key="q.label">
            <span class="admin-dot" :class="`dot--${q.tone}`"></span>
            <span class="load-label">{{ q.label }}</span>
            <span class="load-track">
              <span class="load-fill" :class="`fill--${q.tone}`" :style="{ width: loadPercent(q.pending) + '%' }"></span>
            </span>
            <span class="load-value">{{ q.pending }}</span>
          </li>
        </ul>

        <div class="load-foot">
          <span class="admin-dot" :class="healthDot"></span>
          <span>{{ healthLabel }}</span>
        </div>
      </section>
    </aside>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { adminMessage as ElMessage } from '@/utils/adminMessage'
import { getDashboard, getUserOverview, getUserGrowth, getAdminLogs, getReports, getUsers, getFeedbackStats, getOperationsAdmins } from '@/api/admin'
import { getTreeHoleAdminPosts } from '@/api/treeHoleAdmin'

const router = useRouter()
const authStore = useAuthStore()

const trendRange = ref(7)
const refreshing = ref(false)
const queueLoading = ref(true)
const trendLoading = ref(true)
const ganttLoading = ref(true)
const teamLoading = ref(true)

const dashboard = ref({})
const userOverview = ref({})
const trendRaw = ref({ dates: [], dailyNewUsers: [] })
const logs = ref([])
const backlog = ref({ reports: 0, treeHole: 0, feedback: 0, feedbackHandled: 0 })
const teamMembers = ref([])
const brokenAvatars = ref(new Set())
const avatarBroken = ref(false)
const memberKeyword = ref('')

let refreshTimer = null

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const avatarInitial = computed(() =>
  (authStore.user?.username || 'A').slice(0, 1).toUpperCase()
)

/* 动作新旧命名并存，两侧都要收 */
const WORKSTREAMS = [
  { key: 'review', label: '内容审核', tone: 'primary', actions: ['APPROVE_ARTICLE', 'TAKEDOWN_ARTICLE', 'TAKE_DOWN_ARTICLE', 'REJECT_ARTICLE', 'APPROVE_COMMENT', 'DELETE_COMMENT'] },
  { key: 'user', label: '用户治理', tone: 'violet', actions: ['BAN_USER', 'UNBAN_USER', 'UPDATE_ROLE', 'SET_ROLE', 'RESET_PASSWORD'] },
  { key: 'treeHole', label: '树洞治理', tone: 'cyan', actions: ['TREEHOLE_APPROVE_POST', 'TREEHOLE_REJECT_POST', 'TREEHOLE_DELETE_POST', 'TREEHOLE_DELETE_REPLY', 'TREEHOLE_RESOLVE_REPORT', 'TREEHOLE_DISMISS_REPORT'] },
  { key: 'report', label: '举报处理', tone: 'warning', actions: ['HANDLE_REPORT'] },
  { key: 'launch', label: '配置投放', tone: 'rose', actions: ['UPDATE_CONFIG', 'BATCH_UPDATE_CONFIG', 'CREATE_ANNOUNCEMENT', 'PUBLISH_ANNOUNCEMENT', 'UPDATE_ANNOUNCEMENT', 'DELETE_ANNOUNCEMENT', 'CREATE_BANNER', 'ADD_BANNER', 'UPDATE_BANNER', 'UPDATE_BANNER_STATUS', 'DELETE_BANNER', 'ADD_FEATURED', 'REMOVE_FEATURED'] }
]

const windowStart = computed(() => {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  d.setDate(d.getDate() - (trendRange.value - 1))
  return d
})

const ganttDays = computed(() => {
  const days = []
  const start = windowStart.value
  for (let i = 0; i < trendRange.value; i++) {
    const d = new Date(start)
    d.setDate(start.getDate() + i)
    days.push({
      key: d.toISOString().slice(0, 10),
      label: `${d.getMonth() + 1}/${d.getDate()}`,
      isToday: d.toDateString() === new Date().toDateString()
    })
  }
  return days
})

const axisLabels = computed(() => {
  const days = ganttDays.value
  if (days.length <= 7) return days
  const step = Math.ceil(days.length / 6)
  return days.filter((_, i) => i % step === 0 || i === days.length - 1).map((d) => ({ ...d, label: d.label }))
})

const dayIndexOf = (dateStr) => {
  const d = new Date(dateStr)
  d.setHours(0, 0, 0, 0)
  return Math.floor((d.getTime() - windowStart.value.getTime()) / 86400000)
}

const workstreams = computed(() => {
  const total = ganttDays.value.length
  const rows = WORKSTREAMS.map((ws) => {
    const hits = logs.value.filter((l) => ws.actions.includes(l.action) && dayIndexOf(l.createdAt) >= 0)
    const perDay = {}
    hits.forEach((l) => {
      const idx = Math.min(Math.max(dayIndexOf(l.createdAt), 0), total - 1)
      perDay[idx] = (perDay[idx] || 0) + 1
    })
    const idxs = Object.keys(perDay).map(Number).sort((a, b) => a - b)
    const first = idxs.length ? idxs[0] : 0
    const last = idxs.length ? idxs[idxs.length - 1] : 0
    const peak = Math.max(...Object.values(perDay), 0)
    return {
      ...ws,
      count: hits.length,
      leftPct: (first / total) * 100,
      widthPct: ((last - first + 1) / total) * 100,
      points: idxs.map((i) => ({
        key: i,
        leftPct: ((i + 0.5) / total) * 100,
        count: perDay[i],
        isPeak: perDay[i] === peak,
        label: ganttDays.value[i]?.label
      }))
    }
  })
  return rows
})

const hasWorkstreamData = computed(() => workstreams.value.some((w) => w.count > 0))

/* ===== 待办 ===== */
const todayCount = (actions) => {
  const today = new Date().toDateString()
  return logs.value.filter(
    (l) => actions.includes(l.action) && new Date(l.createdAt).toDateString() === today
  ).length
}

const queueProgress = computed(() => {
  const d = dashboard.value
  const items = [
    { label: '文章审核', module: 'content', done: todayCount(['APPROVE_ARTICLE', 'TAKEDOWN_ARTICLE', 'TAKE_DOWN_ARTICLE', 'REJECT_ARTICLE']), pending: d.pendingReviewArticles || 0, tone: 'primary' },
    { label: '评论审核', module: 'content', done: todayCount(['APPROVE_COMMENT', 'DELETE_COMMENT']), pending: d.pendingReviewComments || 0, tone: 'violet' },
    { label: '举报处理', module: 'content', done: todayCount(['HANDLE_REPORT']), pending: backlog.value.reports, tone: 'warning' },
    { label: '树洞审核', module: 'community', done: todayCount(['TREEHOLE_APPROVE_POST', 'TREEHOLE_REJECT_POST', 'TREEHOLE_DELETE_POST']), pending: backlog.value.treeHole, tone: 'cyan' },
    { label: '建议反馈', module: 'feedback', done: backlog.value.feedbackHandled, pending: backlog.value.feedback, tone: 'rose' }
  ]
  return items
    .filter((i) => authStore.canAccessModule(i.module))
    .map((i) => {
      const sum = i.done + i.pending
      return { ...i, percent: sum === 0 ? 100 : Math.round((i.done / sum) * 100) }
    })
})

const totalBacklog = computed(() => queueProgress.value.reduce((a, q) => a + q.pending, 0))

const completionRate = computed(() => {
  const done = queueProgress.value.reduce((a, q) => a + q.done, 0)
  const sum = done + totalBacklog.value
  return sum === 0 ? 100 : Math.round((done / sum) * 100)
})

const maxPending = computed(() => Math.max(...queueProgress.value.map((q) => q.pending), 1))
const loadPercent = (v) => Math.round((v / maxPending.value) * 100)

const healthLabel = computed(() => {
  if (totalBacklog.value === 0) return '队列已清空'
  if (totalBacklog.value <= 10) return '队列轻量，运转正常'
  if (totalBacklog.value <= 40) return '积压需要关注'
  return '积压偏多，建议增派人手'
})

const healthDot = computed(() => {
  if (totalBacklog.value <= 10) return 'admin-dot--ok'
  if (totalBacklog.value <= 40) return 'admin-dot--warn'
  return 'admin-dot--err'
})

/* ===== 趋势 ===== */
const trendSummary = computed(() => {
  const users = trendRaw.value.dailyNewUsers || []
  if (users.length === 0) return null
  const total = users.reduce((a, b) => a + (b || 0), 0)
  const peakVal = Math.max(...users)
  const dates = trendRaw.value.dates || []
  return {
    total,
    avg: Math.round((total / users.length) * 10) / 10,
    peakLabel: peakVal === 0 ? '—' : dates[users.indexOf(peakVal)]?.slice(5) || '—'
  }
})

const spark = computed(() => {
  const vals = trendRaw.value.dailyNewUsers || []
  if (vals.length === 0) return { line: '', area: '', last: null }
  const W = 300
  const H = 86
  const pad = 6
  const max = Math.max(...vals, 1)
  const step = vals.length > 1 ? W / (vals.length - 1) : W
  const pts = vals.map((v, i) => [i * step, H - pad - (v / max) * (H - pad * 2)])

  let d = `M ${pts[0][0].toFixed(1)} ${pts[0][1].toFixed(1)}`
  for (let i = 1; i < pts.length; i++) {
    const [px, py] = pts[i - 1]
    const [cx, cy] = pts[i]
    const mx = (px + cx) / 2
    d += ` C ${mx.toFixed(1)} ${py.toFixed(1)}, ${mx.toFixed(1)} ${cy.toFixed(1)}, ${cx.toFixed(1)} ${cy.toFixed(1)}`
  }
  const area = `${d} L ${W} ${H} L 0 ${H} Z`
  const last = { x: pts[pts.length - 1][0].toFixed(1), y: pts[pts.length - 1][1].toFixed(1) }
  return { line: d, area, last }
})

/* ===== 团队 ===== */
const filteredMembers = computed(() => {
  const kw = memberKeyword.value.trim().toLowerCase()
  if (!kw) return teamMembers.value
  return teamMembers.value.filter(
    (m) => m.username?.toLowerCase().includes(kw) || m.email?.toLowerCase().includes(kw)
  )
})

const initial = (name) => (name || 'A').slice(0, 1).toUpperCase()
const roleLabel = (role) => ({ SUPER_ADMIN: '超级管理员', ADMIN: '运营管理员', CREATOR: '创作者' }[role] || role)
const roleClass = (role) => (role === 'SUPER_ADMIN' ? 'is-super' : role === 'ADMIN' ? 'is-admin' : 'is-creator')
const deltaClass = (t) => (t > 0 ? 'delta--up' : t < 0 ? 'delta--down' : 'delta--flat')

/* ===== 加载 ===== */
const loadKpi = async () => {
  try {
    const [d, o] = await Promise.all([getDashboard(), getUserOverview()])
    dashboard.value = d || {}
    userOverview.value = o || {}
  } catch {
    ElMessage.error('获取仪表盘数据失败')
  }
}

const loadTrend = async () => {
  trendLoading.value = true
  try {
    const data = await getUserGrowth(trendRange.value)
    trendRaw.value = { dates: data?.dates || [], dailyNewUsers: data?.dailyNewUsers || [] }
  } catch {
    ElMessage.error('获取趋势数据失败')
  } finally {
    trendLoading.value = false
  }
}

const loadLogs = async () => {
  ganttLoading.value = true
  try {
    const data = await getAdminLogs({ page: 0, size: 120 })
    logs.value = data?.list || []
  } catch {
    logs.value = []
  } finally {
    ganttLoading.value = false
  }
}

const loadBacklog = async () => {
  queueLoading.value = true
  const next = { reports: 0, treeHole: 0, feedback: 0, feedbackHandled: 0 }
  try {
    const tasks = [
      // 反馈是通用模块，所有职位都能看到
      getFeedbackStats().then((s) => {
        next.feedback = s?.pending || 0
        next.feedbackHandled = s?.handled || 0
      })
    ]
    if (authStore.canAccessModule('content')) {
      tasks.push(getReports({ status: 'PENDING', page: 0, size: 1 }).then((r) => {
        next.reports = r?.total || 0
      }))
    }
    if (authStore.canAccessModule('community')) {
      tasks.push(getTreeHoleAdminPosts({ status: 0, page: 0, size: 1 }).then((r) => {
        next.treeHole = r?.total || 0
      }))
    }
    await Promise.all(tasks)
    backlog.value = next
  } catch {
    backlog.value = next
  } finally {
    queueLoading.value = false
  }
}

const canViewTeam = computed(() =>
  authStore.canAccessModule('user') || authStore.canAccessModule('operations')
)

const loadTeam = async () => {
  if (!canViewTeam.value) {
    teamMembers.value = []
    teamLoading.value = false
    return
  }
  teamLoading.value = true
  try {
    let members = []
    if (authStore.canAccessModule('user')) {
      const [admins, supers] = await Promise.all([
        getUsers({ role: 'ADMIN', page: 0, size: 6 }),
        getUsers({ role: 'SUPER_ADMIN', page: 0, size: 3 })
      ])
      members = [...(supers?.list || []), ...(admins?.list || [])]
    } else {
      // 运营管理：走管理员专属接口
      const data = await getOperationsAdmins({ page: 0, size: 6 })
      members = data?.list || []
    }
    const seen = new Set()
    teamMembers.value = members.filter((u) => {
      if (seen.has(u.id)) return false
      seen.add(u.id)
      return true
    }).slice(0, 6)
  } catch {
    teamMembers.value = []
  } finally {
    teamLoading.value = false
  }
}

const refreshAll = async () => {
  refreshing.value = true
  await Promise.all([loadKpi(), loadTrend(), loadLogs(), loadBacklog(), loadTeam()])
  refreshing.value = false
}

watch(trendRange, () => {
  loadTrend()
  loadLogs()
})

onMounted(() => {
  refreshAll()
  refreshTimer = setInterval(refreshAll, 60000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style lang="scss" scoped>
@use "@/styles/admin-mixins.scss" as *;

.ai-dashboard {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 348px;
  gap: var(--admin-space-4);
  align-items: start;
}

.dash-main {
  display: flex;
  flex-direction: column;
  gap: var(--admin-space-4);
  min-width: 0;
}

.dash-side {
  display: flex;
  flex-direction: column;
  gap: var(--admin-space-4);
}

/* ============================================================
   渐变玻璃主卡
   ============================================================ */
.hero-card {
  @include admin-neon-edge(var(--admin-radius-hero), 0.55);
  position: relative;
  overflow: hidden;
  border-radius: var(--admin-radius-hero);
  padding: 26px 28px 20px;
  background: var(--admin-grad-hero);
  box-shadow: var(--admin-shadow-hero);
  color: #fff;
  isolation: isolate;
}

/* 内部光斑 */
.hero-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(46px);
  pointer-events: none;
  z-index: -1;
}

.hero-blob--ice {
  width: 420px;
  height: 320px;
  left: -80px;
  top: -120px;
  background: radial-gradient(circle, rgba(186, 230, 253, 0.75), transparent 70%);
}

.hero-blob--violet {
  width: 460px;
  height: 360px;
  right: -120px;
  bottom: -160px;
  background: radial-gradient(circle, rgba(233, 213, 255, 0.6), transparent 70%);
}

.hero-grid {
  position: absolute;
  inset: 0;
  z-index: -1;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.08) 1px, transparent 1px);
  background-size: 34px 34px;
  mask-image: linear-gradient(120deg, rgba(0, 0, 0, 0.9), transparent 72%);
  -webkit-mask-image: linear-gradient(120deg, rgba(0, 0, 0, 0.9), transparent 72%);
  pointer-events: none;
}

.hero-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--admin-space-4);
  flex-wrap: wrap;
}

.hero-identity {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.hero-avatar {
  position: relative;
  width: 76px;
  height: 76px;
  flex-shrink: 0;
  border-radius: 50%;
  padding: 3px;
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.95), rgba(191, 211, 255, 0.45));
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.35), 0 12px 30px rgba(13, 30, 77, 0.35);
}

.hero-avatar-inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;

  img { width: 100%; height: 100%; object-fit: cover; }
}

.hero-initial {
  font-size: 26px;
  font-weight: var(--admin-font-bold);
  background: var(--admin-grad-text);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-status {
  position: absolute;
  right: 5px;
  bottom: 6px;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: #4ADE80;
  border: 3px solid #2E64E8;
  box-shadow: 0 0 10px rgba(74, 222, 128, 0.9);
}

.hero-copy {
  min-width: 0;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin: 0 0 6px;
  font-size: var(--admin-text-xs);
  font-weight: var(--admin-font-semibold);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.78);
}

.hero-spark {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #5FC6D6;
  box-shadow: 0 0 10px 2px rgba(121, 213, 223, 0.9);
  animation: sparkPulse 2.4s ease-in-out infinite;
}

@keyframes sparkPulse {
  0%, 100% { opacity: 0.55; transform: scale(0.9); }
  50% { opacity: 1; transform: scale(1.15); }
}

.hero-title {
  margin: 0;
  font-size: var(--admin-text-2xl);
  font-weight: var(--admin-font-bold);
  letter-spacing: -0.02em;
  color: #fff;
  line-height: 1.25;
}

.hero-desc {
  margin: 8px 0 0;
  font-size: var(--admin-text-base);
  color: rgba(255, 255, 255, 0.78);
  line-height: var(--admin-leading-normal);

  strong {
    font-weight: var(--admin-font-bold);
    color: #fff;
    font-variant-numeric: tabular-nums;
  }
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 18px;
  border-radius: var(--admin-radius-full);
  font-family: inherit;
  font-size: var(--admin-text-base);
  font-weight: var(--admin-font-semibold);
  cursor: pointer;
  transition: all var(--admin-transition-base);
  white-space: nowrap;

  &:hover { transform: translateY(-1px); }
}

.hero-btn--glass {
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(12px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);

  &:hover {
    background: rgba(255, 255, 255, 0.28);
    box-shadow: 0 10px 22px rgba(11, 26, 70, 0.3);
  }
}

.hero-btn--solid {
  color: var(--admin-primary-700);
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid transparent;
  box-shadow: 0 10px 24px rgba(9, 24, 66, 0.28);

  &:hover { background: #fff; }
}

.hero-btn--sm {
  height: 34px;
  padding: 0 14px;
  font-size: var(--admin-text-sm);
}

/* 任务列表 */
.hero-body {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
  gap: 18px;
  margin-top: 22px;
}

.hero-tasks {
  background: rgba(255, 255, 255, 0.13);
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: var(--admin-radius-xl);
  padding: 14px 16px 8px;
  backdrop-filter: blur(14px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.22);
}

.task-head,
.task-row {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) 70px 86px 118px;
  align-items: center;
  gap: 8px;
}

.task-head {
  padding: 0 0 10px;
  font-size: var(--admin-text-xs);
  font-weight: var(--admin-font-semibold);
  letter-spacing: 0.04em;
  color: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.16);

  span:not(:first-child) { text-align: right; }
  span:last-child { text-align: right; }
}

.task-row {
  padding: 9px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);

  &:last-child { border-bottom: none; }
}

.task-row--sk {
  grid-template-columns: 1fr;
}

.task-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--admin-text-base);
  font-weight: var(--admin-font-medium);
  color: rgba(255, 255, 255, 0.94);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 8px currentColor;
}

.task-pending,
.task-done {
  text-align: right;
  font-size: var(--admin-text-base);
  font-weight: var(--admin-font-semibold);
  font-variant-numeric: tabular-nums;
}

.task-pending { color: #FDE68A; }
.task-done { color: rgba(255, 255, 255, 0.86); }

.task-rate {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-end;

  em {
    font-style: normal;
    font-size: var(--admin-text-xs);
    font-weight: var(--admin-font-semibold);
    color: rgba(255, 255, 255, 0.86);
    font-variant-numeric: tabular-nums;
    min-width: 30px;
    text-align: right;
  }
}

.task-rate-track {
  position: relative;
  flex: 1;
  height: 5px;
  border-radius: var(--admin-radius-full);
  background: rgba(255, 255, 255, 0.22);
  overflow: hidden;
}

.task-rate-fill {
  display: block;
  height: 100%;
  border-radius: var(--admin-radius-full);
  background: linear-gradient(90deg, #BAE6FD, #fff);
  transition: width var(--admin-transition-slow);
}

/* 半透明信息面板 */
.hero-panel {
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.3), rgba(255, 255, 255, 0.12));
  border: 1px solid rgba(255, 255, 255, 0.36);
  border-radius: var(--admin-radius-xl);
  padding: 16px 18px;
  backdrop-filter: blur(22px) saturate(170%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.34);
  display: flex;
  flex-direction: column;
}

.hero-panel-title {
  margin: 0 0 10px;
  font-size: var(--admin-text-sm);
  font-weight: var(--admin-font-semibold);
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 0.04em;
}

.hero-panel-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 9px;

  li {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 10px;
    font-size: var(--admin-text-sm);
    color: rgba(255, 255, 255, 0.8);

    strong {
      font-size: var(--admin-text-lg);
      font-weight: var(--admin-font-bold);
      color: #fff;
      font-variant-numeric: tabular-nums;
    }
  }
}

.hero-panel-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.18);
  font-size: var(--admin-text-xs);
  font-weight: var(--admin-font-medium);
  color: rgba(255, 255, 255, 0.86);
}

/* 底部操作 */
.hero-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.18);
}

.hero-round {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--admin-radius-md);
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.36);
  cursor: pointer;
  transition: all var(--admin-transition-base);

  &:hover {
    background: rgba(255, 255, 255, 0.3);
    transform: translateY(-1px);
  }
}

.hero-foot-note {
  margin-left: auto;
  font-size: var(--admin-text-xs);
  color: rgba(255, 255, 255, 0.6);
}

.is-spinning { animation: spin 0.9s linear infinite; }

@keyframes spin { to { transform: rotate(360deg); } }

/* ============================================================
   通用玻璃卡
   ============================================================ */
.glass-card {
  @include admin-glass(0.72, 22px);
  border-radius: var(--admin-radius-xl);
  padding: var(--admin-space-4) var(--admin-space-5) var(--admin-space-5);
  display: flex;
  flex-direction: column;
  gap: var(--admin-space-4);
  transition: transform var(--admin-transition-base), box-shadow var(--admin-transition-base);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--admin-shadow-lg), var(--admin-inset-highlight);
  }
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--admin-space-3);
}

.card-title {
  margin: 0;
  font-size: var(--admin-text-lg);
  font-weight: var(--admin-font-semibold);
  color: var(--admin-text);
  letter-spacing: -0.01em;
}

.card-sub {
  margin: 4px 0 0;
  font-size: var(--admin-text-xs);
  color: var(--admin-text-muted);
}

.card-tools {
  display: flex;
  align-items: center;
  gap: var(--admin-space-2);
}

.card-link {
  font-size: var(--admin-text-sm);
  font-weight: var(--admin-font-semibold);
  color: var(--admin-primary-600);
  text-decoration: none;
  white-space: nowrap;

  &:hover { color: var(--admin-primary-700); }
}

.mini-search {
  width: 150px;

  :deep(.el-input__wrapper) {
    height: 32px;
    border-radius: var(--admin-radius-full);
    background: rgba(241, 245, 249, 0.85);
    box-shadow: 0 0 0 1px rgba(226, 232, 240, 0.9) inset;
  }

  :deep(.el-input__inner) { font-size: var(--admin-text-sm); }
}

.mini-segment :deep(.el-radio-button__inner) {
  padding: 6px 12px;
}

.card-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: var(--admin-space-6) 0;
  font-size: var(--admin-text-sm);
  color: var(--admin-text-muted);
}

/* 底部两栏 */
.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  gap: var(--admin-space-4);
}

/* ============================================================
   团队成员
   ============================================================ */
.member-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 8px;
  border-radius: var(--admin-radius-md);
  border-bottom: 1px solid var(--admin-divider);
  transition: background-color var(--admin-transition-base), transform var(--admin-transition-base);

  &:last-child { border-bottom: none; }

  &:hover {
    background: rgba(239, 246, 255, 0.9);
    transform: translateX(2px);
  }
}

.member-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(140deg, #9BB8FE, #35B3C9);
  box-shadow: 0 4px 12px rgba(53, 179, 201, 0.28);

  img { width: 100%; height: 100%; object-fit: cover; }
}

.member-initial {
  font-size: var(--admin-text-base);
  font-weight: var(--admin-font-bold);
  color: #fff;
}

.member-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.member-name {
  font-size: var(--admin-text-base);
  font-weight: var(--admin-font-semibold);
  color: var(--admin-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-mail {
  font-size: var(--admin-text-xs);
  color: var(--admin-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-role {
  font-size: var(--admin-text-xs);
  font-weight: var(--admin-font-semibold);
  padding: 3px 9px;
  border-radius: var(--admin-radius-full);
  white-space: nowrap;

  &.is-super { background: var(--admin-accent-100); color: var(--admin-accent-700); }
  &.is-admin { background: var(--admin-primary-50); color: var(--admin-primary-700); }
  &.is-creator { background: var(--admin-slate-100); color: var(--admin-slate-600); }
}

.member-stat {
  font-size: var(--admin-text-xs);
  color: var(--admin-text-muted);
  white-space: nowrap;

  em {
    font-style: normal;
    font-weight: var(--admin-font-semibold);
    color: var(--admin-text);
    font-variant-numeric: tabular-nums;
  }
}

.member-state {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--admin-text-xs);
  color: var(--admin-text-secondary);
  white-space: nowrap;
}

/* ============================================================
   任务节点图
   ============================================================ */
.node-chart {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.node-axis,
.node-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}

.node-axis {
  padding-bottom: 6px;
  border-bottom: 1px solid var(--admin-divider);
  margin-bottom: 4px;
}

.node-axis-cells {
  display: flex;
  justify-content: space-between;
  font-size: var(--admin-text-xs);
  color: var(--admin-text-muted);
  font-variant-numeric: tabular-nums;
}

.node-row {
  height: 40px;
}

.node-label {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: var(--admin-text-sm);
  font-weight: var(--admin-font-medium);
  color: var(--admin-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-track {
  position: relative;
  height: 40px;
  border-radius: var(--admin-radius-sm);
  background-image: linear-gradient(90deg, rgba(226, 232, 240, 0.7) 1px, transparent 1px);
  background-size: 20% 100%;
}

.node-link {
  position: absolute;
  top: 50%;
  height: 2px;
  transform: translateY(-50%);
  border-radius: var(--admin-radius-full);
  background: linear-gradient(90deg, rgba(122, 157, 254, 0.35), rgba(53, 179, 201, 0.45));
}

.node-point {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--admin-primary-400);
  box-shadow: 0 3px 10px rgba(89, 139, 253, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: default;
  transition: transform var(--admin-transition-base);

  em {
    position: absolute;
    bottom: 18px;
    font-style: normal;
    font-size: 10px;
    font-weight: var(--admin-font-bold);
    color: var(--admin-text-secondary);
    opacity: 0;
    transition: opacity var(--admin-transition-base);
    white-space: nowrap;
  }

  &:hover {
    transform: translate(-50%, -50%) scale(1.18);
    em { opacity: 1; }
  }

  &.is-peak {
    width: 18px;
    height: 18px;
    border-width: 3px;
    box-shadow: 0 0 0 5px rgba(89, 139, 253, 0.14), 0 6px 14px rgba(89, 139, 253, 0.35);
  }
}

.point--primary { border-color: var(--admin-primary-500); }
.point--violet { border-color: var(--admin-accent-500); }
.point--cyan { border-color: #35B3C9; }
.point--warning { border-color: var(--admin-warning); }
.point--rose { border-color: var(--admin-error); }

.node-legend {
  display: flex;
  gap: var(--admin-space-4);
  padding-top: 10px;
  margin-top: 4px;
  border-top: 1px solid var(--admin-divider);
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: var(--admin-text-xs);
  color: var(--admin-text-muted);
}

.legend-line {
  width: 16px;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, #9BB8FE, #79D5DF);
}

.legend-point {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--admin-primary-400);
}

/* ============================================================
   右侧数据卡
   ============================================================ */
.pill-badge,
.glass-select {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: var(--admin-radius-full);
  background: var(--admin-primary-50);
  color: var(--admin-primary-600);
  font-size: var(--admin-text-xs);
  font-weight: var(--admin-font-semibold);
}

/* 环形 */
.ring-wrap {
  display: flex;
  align-items: center;
  gap: 18px;
}

.ring {
  --pct: 0;
  position: relative;
  width: 132px;
  height: 132px;
  flex-shrink: 0;
  border-radius: 50%;
  background: conic-gradient(
    from 180deg,
    #7A9DFE 0%,
    #598BFD calc(var(--pct) * 0.55%),
    #35B3C9 calc(var(--pct) * 1%),
    rgba(226, 232, 240, 0.85) calc(var(--pct) * 1%),
    rgba(226, 232, 240, 0.85) 100%
  );
  filter: drop-shadow(0 8px 20px rgba(89, 139, 253, 0.28));
  animation: ringIn 0.6s cubic-bezier(0.4, 0, 0.2, 1);

  &::before {
    content: '';
    position: absolute;
    inset: 13px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.96);
    box-shadow: inset 0 2px 8px rgba(47, 91, 212, 0.06);
  }
}

@keyframes ringIn {
  from { transform: rotate(-16deg) scale(0.92); opacity: 0.4; }
  to { transform: rotate(0) scale(1); opacity: 1; }
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;

  strong {
    font-size: var(--admin-text-2xl);
    font-weight: var(--admin-font-bold);
    color: var(--admin-text);
    font-variant-numeric: tabular-nums;
    letter-spacing: -0.02em;
  }

  span {
    font-size: var(--admin-text-xs);
    color: var(--admin-text-muted);
  }
}

.ring-legend {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 9px;

  li {
    display: flex;
    align-items: center;
    gap: 7px;
    font-size: var(--admin-text-xs);
    color: var(--admin-text-secondary);
  }

  i {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  span {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  em {
    font-style: normal;
    font-weight: var(--admin-font-semibold);
    color: var(--admin-text);
    font-variant-numeric: tabular-nums;
  }
}

/* 趋势 */
.trend-delta {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  height: 24px;
  padding: 0 8px;
  border-radius: var(--admin-radius-full);
  font-size: var(--admin-text-xs);
  font-weight: var(--admin-font-semibold);
}

.delta--up { background: var(--admin-success-light); color: var(--admin-success-text); }
.delta--down { background: var(--admin-error-light); color: var(--admin-error-text); }
.delta--flat { background: var(--admin-slate-100); color: var(--admin-text-muted); }

.trend-big {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: -4px;

  strong {
    font-size: var(--admin-text-3xl);
    font-weight: var(--admin-font-bold);
    letter-spacing: -0.03em;
    color: var(--admin-text);
    font-variant-numeric: tabular-nums;
  }

  span {
    font-size: var(--admin-text-xs);
    color: var(--admin-text-muted);
  }
}

.spark {
  width: 100%;
  height: 86px;
  display: block;
  overflow: visible;
}

.spark-axis {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: var(--admin-text-muted);
  font-variant-numeric: tabular-nums;
  margin-top: 2px;
}

.trend-foot {
  display: flex;
  gap: var(--admin-space-4);
  padding-top: 10px;
  border-top: 1px solid var(--admin-divider);
  font-size: var(--admin-text-xs);
  color: var(--admin-text-muted);

  strong {
    font-weight: var(--admin-font-semibold);
    color: var(--admin-text-secondary);
    font-variant-numeric: tabular-nums;
  }
}

/* 待办构成 */
.load-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;

  li {
    display: grid;
    grid-template-columns: 8px 66px minmax(0, 1fr) 34px;
    align-items: center;
    gap: 8px;
  }
}

.load-label {
  font-size: var(--admin-text-sm);
  color: var(--admin-text-secondary);
  white-space: nowrap;
}

.load-track {
  height: 6px;
  border-radius: var(--admin-radius-full);
  background: rgba(226, 232, 240, 0.8);
  overflow: hidden;
}

.load-fill {
  display: block;
  height: 100%;
  border-radius: var(--admin-radius-full);
  transition: width var(--admin-transition-slow);
}

.load-value {
  font-size: var(--admin-text-sm);
  font-weight: var(--admin-font-semibold);
  color: var(--admin-text);
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.load-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--admin-divider);
  font-size: var(--admin-text-xs);
  color: var(--admin-text-secondary);
  font-weight: var(--admin-font-medium);
}

/* 色点 / 填充 */
.dot--primary { background: var(--admin-primary-500); box-shadow: 0 0 0 3px var(--admin-primary-100); }
.dot--violet { background: var(--admin-accent-500); box-shadow: 0 0 0 3px var(--admin-accent-100); }
.dot--cyan { background: #35B3C9; box-shadow: 0 0 0 3px rgba(34, 211, 238, 0.18); }
.dot--warning { background: var(--admin-warning); box-shadow: 0 0 0 3px var(--admin-warning-light); }
.dot--rose { background: var(--admin-error); box-shadow: 0 0 0 3px var(--admin-error-light); }

.fill--primary { background: linear-gradient(90deg, #7A9DFE, #3F72F0); }
.fill--violet { background: linear-gradient(90deg, #79D5DF, #2A93A8); }
.fill--cyan { background: linear-gradient(90deg, #67E8F9, #06B6D4); }
.fill--warning { background: linear-gradient(90deg, #FCD34D, #F59E0B); }
.fill--rose { background: linear-gradient(90deg, #FDA4AF, #F43F5E); }

/* 骨架 */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sk-row {
  display: block;
  height: 40px;
  border-radius: var(--admin-radius-md);
  background: linear-gradient(100deg, rgba(226, 232, 240, 0.5) 30%, rgba(241, 245, 249, 0.9) 50%, rgba(226, 232, 240, 0.5) 70%);
  background-size: 220% 100%;
  animation: skPulse 1.4s ease-in-out infinite;
}

.sk-bar {
  display: block;
  height: 18px;
  border-radius: var(--admin-radius-sm);
  background: rgba(255, 255, 255, 0.28);
}

.ring-skeleton,
.spark-skeleton {
  height: 148px;
  border-radius: var(--admin-radius-lg);
  background: linear-gradient(100deg, rgba(226, 232, 240, 0.5) 30%, rgba(241, 245, 249, 0.9) 50%, rgba(226, 232, 240, 0.5) 70%);
  background-size: 220% 100%;
  animation: skPulse 1.4s ease-in-out infinite;
}

.spark-skeleton { height: 130px; }

@keyframes skPulse {
  0%, 100% { opacity: 0.55; }
  50% { opacity: 1; }
}

/* ============================================================
   响应式
   ============================================================ */
@media (max-width: 1500px) {
  .ai-dashboard { grid-template-columns: minmax(0, 1fr) 318px; }
}

@media (max-width: 1320px) {
  .ai-dashboard { grid-template-columns: minmax(0, 1fr); }
  .dash-side { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); align-items: stretch; }
}

@media (max-width: 1080px) {
  .hero-body { grid-template-columns: minmax(0, 1fr); }
  .bottom-grid { grid-template-columns: minmax(0, 1fr); }
  .dash-side { grid-template-columns: minmax(0, 1fr); }
}

@media (max-width: 720px) {
  .hero-card { padding: 20px 18px 16px; border-radius: var(--admin-radius-xl); }
  .hero-title { font-size: var(--admin-text-xl); }
  .hero-avatar { width: 60px; height: 60px; }
  .task-head, .task-row { grid-template-columns: minmax(0, 1.2fr) 52px 60px 86px; }
  .hero-actions { width: 100%; }
  .hero-foot-note { display: none; }
  .member-stat { display: none; }
  .node-axis, .node-row { grid-template-columns: 78px minmax(0, 1fr); }
}
</style>
