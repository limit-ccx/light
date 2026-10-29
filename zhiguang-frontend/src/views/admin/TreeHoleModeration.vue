<template>
  <div class="tree-hole-moderation">
    <div class="page-header">
      <h2>树洞管理</h2>
      <p class="page-desc">审核匿名树洞内容，处理举报，查看数据统计</p>
    </div>

    <el-tabs v-model="activeTab" class="admin-tabs">
      <!-- Tab 1: 待审核 -->
      <el-tab-pane label="内容审核" name="moderation">
        <div class="tab-toolbar">
          <el-radio-group v-model="postStatusFilter" size="default" @change="loadPosts">
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="1">已通过</el-radio-button>
            <el-radio-button :value="2">已驳回</el-radio-button>
            <el-radio-button :value="3">已删除</el-radio-button>
          </el-radio-group>
        </div>

        <el-table :data="posts" v-loading="postsLoading" stripe class="admin-table">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="内容" min-width="280">
            <template #default="{ row }">
              <div class="content-cell">
                <span class="emotion-dot" :style="{ background: getEmotionColor(row.emotionTag) }"></span>
                <span class="content-text" @click="showContentDetail(row)">{{ row.content }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="emotionTag" label="情绪" width="90">
            <template #default="{ row }">
              <el-tag size="small" :color="`${getEmotionColor(row.emotionTag)}15`" :style="{ color: getEmotionColor(row.emotionTag), borderColor: `${getEmotionColor(row.emotionTag)}30` }">
                {{ getEmotionLabel(row.emotionTag) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="互动" width="150">
            <template #default="{ row }">
              <span class="stat-chips">
                ❤️{{ row.likeCount }} 🫂{{ row.empathyCount }} 💬{{ row.replyCount }} 👁{{ row.viewCount }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="reportCount" label="举报" width="70" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" class-name="col-actions" width="210" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 0" type="success" size="small" text @click="approvePost(row)">
                通过
              </el-button>
              <el-button v-if="row.status === 0" type="danger" size="small" text @click="rejectPost(row)">
                驳回
              </el-button>
              <el-button v-if="row.status !== 3" type="danger" size="small" text @click="deletePost(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="postPage"
            :page-size="20"
            :total="postTotal"
            layout="total, prev, pager, next"
            @current-change="loadPosts"
          />
        </div>
      </el-tab-pane>

      <!-- Tab 2: 举报管理 -->
      <el-tab-pane label="举报管理" name="reports">
        <div class="tab-toolbar">
          <el-radio-group v-model="reportStatusFilter" size="default" @change="loadReports">
            <el-radio-button :value="0">待处理</el-radio-button>
            <el-radio-button :value="1">已处理</el-radio-button>
            <el-radio-button :value="2">已忽略</el-radio-button>
          </el-radio-group>
        </div>

        <el-table :data="reports" v-loading="reportsLoading" stripe class="admin-table">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="targetType" label="类型" width="80">
            <template #default="{ row }">
              {{ row.targetType === 'POST' ? '帖子' : '回复' }}
            </template>
          </el-table-column>
          <el-table-column label="被举报内容" min-width="250">
            <template #default="{ row }">
              <span class="content-text">{{ row.targetContent || '内容已删除' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="举报原因" width="160" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="reportStatusType(row.status)" size="small">{{ reportStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="举报时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" class-name="col-actions" width="160" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 0">
                <el-button type="warning" size="small" text @click="resolveReportAction(row)">处理</el-button>
                <el-button size="small" text @click="dismissReport(row)">忽略</el-button>
              </template>
              <span v-else class="text-muted">--</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="reportPage"
            :page-size="20"
            :total="reportTotal"
            layout="total, prev, pager, next"
            @current-change="loadReports"
          />
        </div>
      </el-tab-pane>

      <!-- Tab 3: 统计面板 -->
      <el-tab-pane label="数据统计" name="stats">
        <div v-loading="statsLoading" class="stats-panel">
          <div class="stats-cards">
            <div class="stat-card">
              <div class="stat-value">{{ stats.totalPosts || 0 }}</div>
              <div class="stat-label">总帖数</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ stats.todayPosts || 0 }}</div>
              <div class="stat-label">今日发布</div>
            </div>
            <div class="stat-card stat-warning">
              <div class="stat-value">{{ stats.pendingPosts || 0 }}</div>
              <div class="stat-label">待审核</div>
            </div>
            <div class="stat-card stat-danger">
              <div class="stat-value">{{ stats.unresolvedReports || 0 }}</div>
              <div class="stat-label">未处理举报</div>
            </div>
          </div>

          <div class="chart-section">
            <h3 class="chart-title">情绪分布</h3>
            <div ref="emotionChartRef" class="emotion-chart"></div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 内容详情对话框 -->
    <el-dialog v-model="showDetail" class="admin-dialog" title="内容详情" width="520px">
      <div v-if="detailPost" class="content-detail">
        <div class="detail-meta">
          <el-tag size="small" :style="{ color: getEmotionColor(detailPost.emotionTag) }">
            {{ getEmotionLabel(detailPost.emotionTag) }}
          </el-tag>
          <span class="detail-time">{{ formatTime(detailPost.createdAt) }}</span>
        </div>
        <div class="detail-full-content">{{ detailPost.fullContent || detailPost.content }}</div>
        <div class="detail-stats">
          ❤️ {{ detailPost.likeCount }} · 🫂 {{ detailPost.empathyCount }} · 💬 {{ detailPost.replyCount }} · 👁 {{ detailPost.viewCount }} · ⚠️ {{ detailPost.reportCount }}
        </div>
      </div>
    </el-dialog>

    <!-- 驳回原因对话框 -->
    <el-dialog v-model="showRejectDialog" class="admin-dialog" title="驳回原因" width="420px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入驳回原因（可选）" />
      <template #footer>
        <el-button @click="showRejectDialog = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { adminMessage as ElMessage } from '@/utils/adminMessage'
import { adminConfirm } from '@/utils/adminConfirm'
import * as echarts from 'echarts'
import {
  getTreeHoleAdminPosts,
  approveTreeHolePost,
  rejectTreeHolePost,
  deleteTreeHolePost,
  getTreeHoleReports,
  resolveReport as resolveReportApi,
  dismissReport as dismissReportApi,
  getTreeHoleStats
} from '@/api/treeHoleAdmin'

const activeTab = ref('moderation')

// Post moderation state
const posts = ref([])
const postsLoading = ref(false)
const postStatusFilter = ref(0)
const postPage = ref(1)
const postTotal = ref(0)

// Report state
const reports = ref([])
const reportsLoading = ref(false)
const reportStatusFilter = ref(0)
const reportPage = ref(1)
const reportTotal = ref(0)

// Stats state
const stats = ref({})
const statsLoading = ref(false)
const emotionChartRef = ref(null)
let emotionChart = null

// Detail dialog
const showDetail = ref(false)
const detailPost = ref(null)

// Reject dialog
const showRejectDialog = ref(false)
const rejectReason = ref('')
const rejectTargetId = ref(null)

/* 情绪配色对齐工作台的蓝紫科技色板 */
const emotionColorMap = {
  happiness: '#FBBF24', sadness: '#598BFD', anxiety: '#35B3C9',
  excitement: '#EC4899', loneliness: '#64748B', anger: '#F43F5E',
  gratitude: '#10B981', other: '#94A3B8'
}

const emotionLabelMap = {
  happiness: '开心', sadness: '难过', anxiety: '焦虑', excitement: '兴奋',
  loneliness: '孤独', anger: '愤怒', gratitude: '感恩', other: '其他'
}

const getEmotionColor = (tag) => emotionColorMap[tag] || emotionColorMap.other
const getEmotionLabel = (tag) => emotionLabelMap[tag] || tag

const statusLabel = (s) => ({ 0: '待审核', 1: '已通过', 2: '已驳回', 3: '已删除' }[s] || '未知')
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }[s] || '')
const reportStatusLabel = (s) => ({ 0: '待处理', 1: '已处理', 2: '已忽略' }[s] || '未知')
const reportStatusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' }[s] || '')

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

// Load posts
const loadPosts = async () => {
  postsLoading.value = true
  try {
    const data = await getTreeHoleAdminPosts({
      status: postStatusFilter.value,
      page: postPage.value - 1,
      size: 20
    })
    posts.value = data.posts || []
    postTotal.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载帖子列表失败')
  } finally {
    postsLoading.value = false
  }
}

// Load reports
const loadReports = async () => {
  reportsLoading.value = true
  try {
    const data = await getTreeHoleReports({
      status: reportStatusFilter.value,
      page: reportPage.value - 1,
      size: 20
    })
    reports.value = data.reports || []
    reportTotal.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载举报列表失败')
  } finally {
    reportsLoading.value = false
  }
}

// Load stats
const loadStats = async () => {
  statsLoading.value = true
  try {
    stats.value = await getTreeHoleStats() || {}
    await nextTick()
    renderEmotionChart()
  } catch (e) {
    ElMessage.error('加载统计数据失败')
  } finally {
    statsLoading.value = false
  }
}

const renderEmotionChart = () => {
  if (!emotionChartRef.value || !stats.value.emotionDistribution) return
  if (!emotionChart) {
    emotionChart = echarts.init(emotionChartRef.value)
  }
  const dist = stats.value.emotionDistribution
  const data = Object.entries(dist).map(([key, value]) => ({
    name: emotionLabelMap[key] || key,
    value
  }))
  emotionChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data: data.map(d => ({
        ...d,
        itemStyle: { color: emotionColorMap[Object.keys(emotionLabelMap).find(k => emotionLabelMap[k] === d.name)] || emotionColorMap.other }
      }))
    }]
  }, true)
}

// Actions
const approvePost = async (row) => {
  try {
    await approveTreeHolePost(row.id)
    ElMessage.success('已通过审核')
    loadPosts()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const rejectPost = (row) => {
  rejectTargetId.value = row.id
  rejectReason.value = ''
  showRejectDialog.value = true
}

const confirmReject = async () => {
  try {
    await rejectTreeHolePost(rejectTargetId.value, rejectReason.value)
    ElMessage.success('已驳回')
    showRejectDialog.value = false
    loadPosts()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const deletePost = async (row) => {
  try {
    await adminConfirm('确定要删除这条内容吗？此操作不可撤销。', '确认删除', { type: 'warning' })
    await deleteTreeHolePost(row.id)
    ElMessage.success('已删除')
    loadPosts()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const resolveReportAction = async (row) => {
  try {
    await adminConfirm('处理此举报将隐藏被举报的内容，确认继续？', '处理举报', { type: 'warning' })
    await resolveReportApi(row.id)
    ElMessage.success('已处理')
    loadReports()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const dismissReport = async (row) => {
  try {
    await dismissReportApi(row.id)
    ElMessage.success('已忽略')
    loadReports()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const showContentDetail = (row) => {
  detailPost.value = row
  showDetail.value = true
}

// Watch tab changes
watch(activeTab, (tab) => {
  if (tab === 'moderation') loadPosts()
  else if (tab === 'reports') loadReports()
  else if (tab === 'stats') loadStats()
})

onMounted(() => {
  loadPosts()
})
</script>

<style lang="scss" scoped>
@use "@/styles/admin-mixins.scss" as *;

.tree-hole-moderation {
  max-width: 1400px;
}

.page-header {
  margin-bottom: 24px;

  h2 {
    font-size: 22px;
    font-weight: 700;
    color: var(--admin-text, #1e293b);
    margin: 0 0 6px;
  }
  .page-desc {
    font-size: 14px;
    color: var(--admin-text-secondary, #64748b);
    margin: 0;
  }
}

.admin-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 20px;
  }
}

.tab-toolbar {
  margin-bottom: 16px;
}

.admin-table {
  border-radius: 12px;
  overflow: hidden;
}

.content-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.emotion-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.content-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 260px;
  display: inline-block;
  cursor: pointer;

  &:hover {
    color: var(--el-color-primary);
  }
}

.stat-chips {
  font-size: 12px;
  color: var(--admin-text-secondary, #64748b);
}

.text-muted {
  color: var(--admin-text-muted, #94a3b8);
  font-size: 13px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.stats-panel {
  min-height: 400px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

.stat-card {
  @include admin-glass(0.6, 18px);
  border-radius: var(--admin-radius-xl);
  padding: var(--admin-space-5);
  text-align: center;
  transition: transform var(--admin-transition-base), box-shadow var(--admin-transition-base);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--admin-shadow-lg), var(--admin-inset-highlight);
  }

  .stat-value {
    font-size: var(--admin-text-2xl);
    font-weight: var(--admin-font-bold);
    color: var(--admin-text);
    margin-bottom: 6px;
    font-variant-numeric: tabular-nums;
  }
  .stat-label {
    font-size: var(--admin-text-sm);
    color: var(--admin-text-secondary);
    font-weight: var(--admin-font-medium);
  }

  &.stat-warning .stat-value { color: var(--admin-warning-text); }
  &.stat-danger .stat-value { color: var(--admin-error-text); }
}

.chart-section {
  @include admin-glass(0.66, 18px);
  border-radius: var(--admin-radius-xl);
  padding: var(--admin-space-5);
  box-shadow: var(--admin-shadow-md), var(--admin-inset-highlight);
}

.chart-title {
  font-size: var(--admin-text-lg);
  font-weight: var(--admin-font-semibold);
  color: var(--admin-text);
  margin: 0 0 var(--admin-space-4);
}

.emotion-chart {
  width: 100%;
  height: 320px;
}

.content-detail {
  .detail-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
  }
  .detail-time {
    font-size: var(--admin-text-sm);
    color: var(--admin-text-muted);
  }
  .detail-full-content {
    font-size: var(--admin-text-base);
    line-height: var(--admin-leading-relaxed);
    color: var(--admin-text-secondary);
    white-space: pre-wrap;
    word-break: break-word;
    margin-bottom: var(--admin-space-4);
    padding: var(--admin-space-4);
    background: rgba(240, 242, 245, 0.72);
    border: 1px solid var(--admin-divider);
    border-radius: var(--admin-radius-lg);
  }
  .detail-stats {
    font-size: var(--admin-text-sm);
    color: var(--admin-text-secondary);
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
