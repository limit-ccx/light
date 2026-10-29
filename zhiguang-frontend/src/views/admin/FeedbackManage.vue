<template>
  <div class="admin-feedback">
    <div class="page-head">
      <div>
        <p class="admin-eyebrow">运营中心 · 反馈</p>
        <h2 class="admin-page-title">建议反馈</h2>
        <p class="admin-page-desc">前台用户提交的建议、Bug 与投诉，按职位同步到运营后台</p>
      </div>
      <div class="admin-toolbar">
        <el-select
          v-model="filters.status"
          popper-class="admin-select-popper"
          clearable
          placeholder="全部状态"
          style="width: 140px"
          @change="handleSearch"
        >
          <el-option label="待处理" value="PENDING" />
          <el-option label="已处理" value="HANDLED" />
        </el-select>
        <el-button :loading="loading" @click="refreshAll">刷新</el-button>
      </div>
    </div>

    <div class="stat-row">
      <div v-for="s in statCards" :key="s.label" class="stat-card admin-panel">
        <span class="stat-label">{{ s.label }}</span>
        <strong class="stat-value" :class="s.tone">{{ s.value }}</strong>
      </div>
    </div>

    <div class="admin-panel table-panel">
      <el-table :data="list" v-loading="loading" empty-text="暂无反馈">
        <el-table-column prop="type" label="类型" width="96">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="170">
          <template #default="{ row }">{{ row.title || '（无标题）' }}</template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
        <el-table-column label="提交人" width="120">
          <template #default="{ row }">
            {{ row.username || (row.userId ? `用户#${row.userId}` : '匿名') }}
          </template>
        </el-table-column>
        <el-table-column prop="contact" label="联系方式" width="150" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="提交时间" width="165">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PENDING' ? 'warning' : 'success'" size="small">
              {{ row.status === 'PENDING' ? '待处理' : '已处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" class-name="col-actions" width="96" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openHandle(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.page"
          :page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" class="admin-dialog" title="处理反馈" width="560px">
      <div v-if="current" class="feedback-detail">
        <div class="detail-head">
          <el-tag :type="typeTag(current.type)" size="small">{{ typeText(current.type) }}</el-tag>
          <span class="detail-time">{{ formatTime(current.createdAt) }}</span>
        </div>
        <h4 class="detail-title">{{ current.title || '（无标题）' }}</h4>
        <p class="detail-content">{{ current.content }}</p>
        <p class="detail-meta">
          提交人：{{ current.username || '匿名' }}
          <span v-if="current.contact"> · 联系方式：{{ current.contact }}</span>
        </p>

        <el-input
          v-model="reply"
          type="textarea"
          :rows="4"
          placeholder="填写处理说明（可选，会记录在反馈中）"
        />
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="current?.status === 'HANDLED'" @click="submitHandle('PENDING')">
          标记待处理
        </el-button>
        <el-button type="primary" :loading="saving" @click="submitHandle('HANDLED')">
          标记已处理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { adminMessage as ElMessage } from '@/utils/adminMessage'
import { getFeedbackList, getFeedbackStats, handleFeedback } from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const stats = ref({ pending: 0, handled: 0, total: 0 })
const filters = reactive({ status: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const current = ref(null)
const reply = ref('')

const statCards = computed(() => [
  { label: '待处理', value: stats.value.pending || 0, tone: 'is-warn' },
  { label: '已处理', value: stats.value.handled || 0, tone: 'is-ok' },
  { label: '累计反馈', value: stats.value.total || 0, tone: '' }
])

const TYPE_TEXT = { bug: 'Bug', feature: '功能建议', content: '内容投诉', other: '其他' }
const typeText = (t) => TYPE_TEXT[t] || t || '其他'
const typeTag = (t) => ({ bug: 'danger', feature: 'primary', content: 'warning', other: 'info' }[t] || 'info')

const formatTime = (v) => (v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '—')

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size
    }
    if (filters.status) params.status = filters.status
    const data = await getFeedbackList(params)
    list.value = data?.list || []
    pagination.total = data?.total || 0
  } catch {
    ElMessage.error('获取反馈列表失败')
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    stats.value = (await getFeedbackStats()) || stats.value
  } catch {
    // 统计失败不阻断列表
  }
}

const refreshAll = () => {
  fetchList()
  fetchStats()
}

const handleSearch = () => {
  pagination.page = 1
  fetchList()
}

const openHandle = (row) => {
  current.value = row
  reply.value = row.reply || ''
  dialogVisible.value = true
}

const submitHandle = async (status) => {
  if (!current.value) return
  saving.value = true
  try {
    await handleFeedback(current.value.id, { status, reply: reply.value })
    ElMessage.success(status === 'HANDLED' ? '已标记为处理完成' : '已标记为待处理')
    dialogVisible.value = false
    refreshAll()
  } catch {
    ElMessage.error('操作失败，请重试')
  } finally {
    saving.value = false
  }
}

onMounted(refreshAll)
</script>

<style lang="scss" scoped>
@use "@/styles/admin-mixins.scss" as *;

.admin-feedback {
  display: flex;
  flex-direction: column;
  gap: var(--admin-space-4);
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--admin-space-4);
  flex-wrap: wrap;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--admin-space-4);
}

.stat-card {
  padding: var(--admin-space-4) var(--admin-space-5);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-label {
  font-size: var(--admin-text-sm);
  color: var(--admin-text-secondary);
}

.stat-value {
  font-size: var(--admin-text-2xl);
  font-weight: var(--admin-font-bold);
  font-variant-numeric: tabular-nums;
  color: var(--admin-text);

  &.is-warn { color: var(--admin-warning-text); }
  &.is-ok { color: var(--admin-success-text); }
}

.table-panel {
  padding: var(--admin-space-4) var(--admin-space-4) var(--admin-space-3);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--admin-space-3);
}

.feedback-detail {
  display: flex;
  flex-direction: column;
  gap: var(--admin-space-3);
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-time {
  font-size: var(--admin-text-xs);
  color: var(--admin-text-muted);
}

.detail-title {
  margin: 0;
  font-size: var(--admin-text-lg);
  font-weight: var(--admin-font-semibold);
  color: var(--admin-text);
}

.detail-content {
  margin: 0;
  padding: var(--admin-space-4);
  border-radius: var(--admin-radius-lg);
  background: rgba(240, 242, 245, 0.7);
  border: 1px solid var(--admin-divider);
  font-size: var(--admin-text-base);
  line-height: var(--admin-leading-relaxed);
  color: var(--admin-text-secondary);
  white-space: pre-wrap;
}

.detail-meta {
  margin: 0;
  font-size: var(--admin-text-xs);
  color: var(--admin-text-muted);
}

@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
