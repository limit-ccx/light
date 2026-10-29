<template>
  <div class="admin-statistics">
    <section class="dash-head">
      <div>
        <p class="admin-eyebrow">运营中心 · 数据</p>
        <h2 class="admin-page-title">数据统计</h2>
        <p class="admin-page-desc">用户增长曲线与内容活跃度分布</p>
      </div>
      <div class="admin-toolbar">
        <el-radio-group v-model="growthDays" size="small" @change="fetchUserGrowth">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
          <el-radio-button :value="90">近 90 天</el-radio-button>
        </el-radio-group>
      </div>
    </section>

    <!-- KPI -->
    <section class="kpi-grid">
      <template v-if="kpiLoading">
        <div v-for="n in 6" :key="n" class="kpi-card admin-panel sk-card"></div>
      </template>
      <article
        v-for="(item, idx) in kpiCards"
        v-else
        :key="item.label"
        class="kpi-card admin-panel"
        :style="{ animationDelay: `${idx * 45}ms` }"
      >
        <div class="kpi-head">
          <span class="admin-dot" :class="`dot--${item.tone}`"></span>
          <span class="kpi-label">{{ item.label }}</span>
        </div>
        <div class="admin-stat-value" :class="`value--${item.tone}`">{{ formatNumber(item.value) }}</div>
      </article>
    </section>

    <!-- 增长趋势 -->
    <article class="admin-panel chart-panel">
      <header class="panel-head">
        <div>
          <h3 class="panel-title">用户增长趋势</h3>
          <p class="panel-sub">每日新增注册用户数</p>
        </div>
        <span v-if="growthSummary" class="panel-metric">
          区间合计 <strong>{{ growthSummary.total }}</strong> 人
        </span>
      </header>

      <div class="chart-wrap">
        <div v-if="growthLoading" class="sk-chart"></div>
        <div v-show="!growthLoading" ref="growthChart" class="chart"></div>
      </div>
    </article>

    <!-- 内容活跃度 -->
    <article class="admin-panel chart-panel">
      <header class="panel-head">
        <div>
          <h3 class="panel-title">内容活跃度统计</h3>
          <p class="panel-sub">文章与评论在各状态下的存量分布</p>
        </div>
      </header>

      <div class="chart-wrap">
        <div v-if="contentLoading" class="sk-chart"></div>
        <div v-show="!contentLoading" ref="contentChart" class="chart"></div>
      </div>
    </article>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { adminMessage as ElMessage } from '@/utils/adminMessage'
import * as echarts from 'echarts'
import { getUserGrowth, getContentStats, getDashboard } from '@/api/admin'

const growthChart = ref(null)
const contentChart = ref(null)
let growthInstance = null
let contentInstance = null

const kpiLoading = ref(true)
const growthLoading = ref(true)
const contentLoading = ref(true)
const growthDays = ref(30)
const growthSummary = ref(null)

const kpiCards = ref([
  { label: '总用户数', value: 0, tone: 'primary' },
  { label: '今日新增', value: 0, tone: 'accent' },
  { label: '已发布文章', value: 0, tone: 'info' },
  { label: '总评论数', value: 0, tone: 'warning' },
  { label: '总点赞数', value: 0, tone: 'rose' },
  { label: '待审核内容', value: 0, tone: 'warning' }
])

const formatNumber = (n) => (n == null ? '0' : Number(n).toLocaleString('zh-CN'))

/* 图表配色全部取自设计令牌，避免与主题脱节 */
const token = (name, fallback) => {
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || fallback
}

const palette = () => ({
  primary: token('--admin-primary-600', '#3F72F0'),
  primarySoft: token('--admin-primary-400', '#7A9DFE'),
  accent: token('--admin-accent-500', '#35B3C9'),
  warning: token('--admin-warning', '#F59E0B'),
  error: token('--admin-error', '#F43F5E'),
  text: token('--admin-text-secondary', '#475569'),
  muted: token('--admin-text-muted', '#94A3B8'),
  divider: token('--admin-divider', '#F0F2F5'),
  border: token('--admin-border', '#E4E7EC')
})

const axisBase = (c) => ({
  axisLine: { lineStyle: { color: c.border } },
  axisTick: { show: false },
  axisLabel: { color: c.muted, fontSize: 12 },
  splitLine: { lineStyle: { color: c.divider, type: 'dashed' } }
})

/* 图表容器在骨架屏期间是隐藏的，尺寸要等可见后按容器实测值重算 */
const resizeCharts = async () => {
  await nextTick()
  const g = growthChart.value
  const c = contentChart.value
  if (g?.clientWidth && g?.clientHeight) {
    growthInstance?.resize({ width: g.clientWidth, height: g.clientHeight })
  }
  if (c?.clientWidth && c?.clientHeight) {
    contentInstance?.resize({ width: c.clientWidth, height: c.clientHeight })
  }
}

const fetchDashboardKpi = async () => {
  kpiLoading.value = true
  try {
    const data = await getDashboard()
    kpiCards.value = [
      { label: '总用户数', value: data?.totalUsers || 0, tone: 'primary' },
      { label: '今日新增', value: data?.todayNewUsers || 0, tone: 'accent' },
      { label: '已发布文章', value: data?.articleCount || 0, tone: 'info' },
      { label: '总评论数', value: data?.commentCount || 0, tone: 'warning' },
      { label: '总点赞数', value: data?.likeCount || 0, tone: 'rose' },
      {
        label: '待审核内容',
        value: (data?.pendingReviewArticles || 0) + (data?.pendingReviewComments || 0),
        tone: 'warning'
      }
    ]
  } catch {
    // KPI 拉取失败保留上一次数值，不打断页面
  } finally {
    kpiLoading.value = false
  }
}

const fetchUserGrowth = async () => {
  growthLoading.value = true
  try {
    const data = await getUserGrowth(growthDays.value)
    const dates = data?.dates || []
    const dailyNewUsers = data?.dailyNewUsers || []
    const total = dailyNewUsers.reduce((a, b) => a + (b || 0), 0)
    growthSummary.value = { total }

    const c = palette()
    await nextTick()
    growthInstance?.setOption({
      color: [c.primary],
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255,255,255,0.94)',
        borderColor: c.border,
        borderWidth: 1,
        textStyle: { color: c.text, fontSize: 12 },
        extraCssText: 'border-radius:8px;box-shadow:0 10px 18px rgba(43,48,56,0.07);'
      },
      legend: {
        data: ['每日新增用户'],
        right: 0,
        top: 0,
        textStyle: { color: c.text, fontSize: 12 },
        icon: 'roundRect',
        itemWidth: 10,
        itemHeight: 10
      },
      grid: { left: '2%', right: '3%', bottom: '3%', top: '14%', containLabel: true },
      xAxis: {
        type: 'category',
        data: dates.map((d) => d.substring(5)),
        boundaryGap: false,
        ...axisBase(c)
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        boundaryGap: ['15%', '10%'],
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: c.muted, fontSize: 12 },
        splitLine: { lineStyle: { color: c.divider } }
      },
      series: [
        {
          name: '每日新增用户',
          type: 'line',
          data: dailyNewUsers,
          smooth: true,
          showSymbol: dates.length <= 31,
          symbolSize: 6,
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(89, 139, 253, 0.26)' },
              { offset: 1, color: 'rgba(53, 179, 201, 0.02)' }
            ])
          },
          lineStyle: { color: c.primary, width: 2 },
          itemStyle: { color: c.primary, borderColor: '#fff', borderWidth: 2 }
        }
      ]
    })
  } catch {
    ElMessage.error('获取用户增长数据失败')
  } finally {
    growthLoading.value = false
    await resizeCharts()
  }
}

const fetchContentStats = async () => {
  contentLoading.value = true
  try {
    const data = await getContentStats()
    const categories = Object.keys(data || {})
    const values = Object.values(data || {})
    const c = palette()
    const barColors = [c.primary, c.primarySoft, c.accent, c.warning, c.error, c.muted]

    await nextTick()
    contentInstance?.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        backgroundColor: 'rgba(255,255,255,0.94)',
        borderColor: c.border,
        borderWidth: 1,
        textStyle: { color: c.text, fontSize: 12 },
        extraCssText: 'border-radius:8px;box-shadow:0 10px 18px rgba(43,48,56,0.07);'
      },
      grid: { left: '2%', right: '3%', bottom: '4%', top: '8%', containLabel: true },
      xAxis: {
        type: 'category',
        data: categories,
        axisLine: { lineStyle: { color: c.border } },
        axisTick: { show: false },
        axisLabel: {
          color: c.muted,
          fontSize: 12,
          rotate: categories.length > 5 ? 22 : 0,
          interval: 0
        }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: c.muted, fontSize: 12 },
        splitLine: { lineStyle: { color: c.divider } }
      },
      series: [
        {
          type: 'bar',
          data: values.map((v, i) => ({
            value: v,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: barColors[i % barColors.length] },
                { offset: 1, color: `${barColors[i % barColors.length]}55` }
              ])
            }
          })),
          barWidth: '46%',
          itemStyle: { borderRadius: [6, 6, 0, 0] }
        }
      ]
    })
  } catch {
    ElMessage.error('获取内容统计数据失败')
  } finally {
    contentLoading.value = false
    await resizeCharts()
  }
}

const handleResize = () => {
  resizeCharts()
}

onMounted(async () => {
  await nextTick()
  // 首帧图表容器被骨架屏遮住（display:none），显式给尺寸避免 ECharts 量到 0；
  // 数据返回后 resizeCharts() 会按真实容器尺寸重算
  const hiddenSize = { width: 560, height: 320 }
  if (growthChart.value) growthInstance = echarts.init(growthChart.value, null, hiddenSize)
  if (contentChart.value) contentInstance = echarts.init(contentChart.value, null, hiddenSize)
  fetchDashboardKpi()
  fetchUserGrowth()
  fetchContentStats()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  growthInstance?.dispose()
  contentInstance?.dispose()
})
</script>

<style lang="scss" scoped>
.admin-statistics {
  display: flex;
  flex-direction: column;
  gap: var(--admin-space-4);
}

.dash-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--admin-space-4);
  flex-wrap: wrap;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: var(--admin-space-4);
}

.kpi-card {
  padding: var(--admin-space-4);
  display: flex;
  flex-direction: column;
  gap: var(--admin-space-2);
  min-height: 96px;
  animation: kpiIn 0.4s cubic-bezier(0.4, 0, 0.2, 1) both;
}

@keyframes kpiIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.sk-card {
  background: rgba(228, 231, 236, 0.4);
  animation: skPulse 1.4s ease-in-out infinite;
}

@keyframes skPulse {
  0%, 100% { opacity: 0.55; }
  50% { opacity: 1; }
}

.kpi-head {
  display: flex;
  align-items: center;
  gap: 7px;
}

.kpi-label {
  font-size: var(--admin-text-sm);
  font-weight: var(--admin-font-medium);
  color: var(--admin-text-secondary);
}

.value--primary { color: var(--admin-primary-700); }
.value--accent { color: var(--admin-accent-700); }
.value--info { color: var(--admin-primary-500); }
.value--warning { color: var(--admin-warning-text); }
.value--rose { color: var(--admin-error-text); }

.dot--primary { background: var(--admin-primary-500); box-shadow: 0 0 0 3px var(--admin-primary-100); }
.dot--accent { background: var(--admin-accent-400); box-shadow: 0 0 0 3px var(--admin-accent-100); }
.dot--info { background: var(--admin-primary-300); box-shadow: 0 0 0 3px var(--admin-primary-50); }
.dot--warning { background: var(--admin-warning); box-shadow: 0 0 0 3px var(--admin-warning-light); }
.dot--rose { background: var(--admin-error); box-shadow: 0 0 0 3px var(--admin-error-light); }

.chart-panel {
  padding: var(--admin-space-5);
  display: flex;
  flex-direction: column;
  gap: var(--admin-space-4);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--admin-space-4);
}

.panel-title {
  margin: 0;
  font-size: var(--admin-text-lg);
  font-weight: var(--admin-font-semibold);
  color: var(--admin-text);
}

.panel-sub {
  margin: 4px 0 0;
  font-size: var(--admin-text-sm);
  color: var(--admin-text-muted);
}

.panel-metric {
  font-size: var(--admin-text-sm);
  color: var(--admin-text-secondary);
  white-space: nowrap;

  strong {
    font-weight: var(--admin-font-bold);
    color: var(--admin-primary-700);
    font-variant-numeric: tabular-nums;
  }
}

.chart-wrap {
  position: relative;
  min-height: 320px;
}

.chart {
  width: 100%;
  height: 320px;
}

.sk-chart {
  width: 100%;
  height: 320px;
  border-radius: var(--admin-radius-lg);
  background: rgba(228, 231, 236, 0.4);
  animation: skPulse 1.4s ease-in-out infinite;
}

@media (max-width: 1600px) {
  .kpi-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 768px) {
  .kpi-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .chart, .chart-wrap, .sk-chart { height: 260px; min-height: 260px; }
}
</style>
