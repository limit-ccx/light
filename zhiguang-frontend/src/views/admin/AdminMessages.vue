<template>
  <div class="admin-messages">
    <div class="page-head">
      <div>
        <p class="admin-eyebrow">运营中心 · 消息</p>
        <h2 class="admin-page-title">消息提示</h2>
        <p class="admin-page-desc">后台催办与回执，与前台私信完全分开</p>
      </div>
      <div class="admin-toolbar">
        <el-button v-if="authStore.isSuperAdmin" type="primary" @click="openNew">
          发起催办
        </el-button>
        <el-button :loading="loading" @click="refreshAll">刷新</el-button>
      </div>
    </div>

    <div class="stat-row">
      <div v-for="s in statCards" :key="s.label" class="stat-card admin-panel">
        <span class="stat-label">{{ s.label }}</span>
        <strong class="stat-value" :class="s.tone">{{ s.value }}</strong>
      </div>
    </div>

    <div class="admin-panel list-panel">
      <div class="msg-tabs" role="tablist">
        <button
          class="msg-tab"
          :class="{ 'is-active': activeTab === 'received' }"
          role="tab"
          :aria-selected="activeTab === 'received'"
          @click="switchTab('received')"
        >
          我收到的
          <span v-if="stats.unread > 0" class="tab-badge">{{ stats.unread }}</span>
        </button>
        <button
          class="msg-tab"
          :class="{ 'is-active': activeTab === 'sent' }"
          role="tab"
          :aria-selected="activeTab === 'sent'"
          @click="switchTab('sent')"
        >
          我发出的
        </button>
      </div>

      <div v-if="loading" class="msg-skeleton">
        <span v-for="n in 3" :key="n" class="sk-block"></span>
      </div>

      <div v-else-if="current.list.length === 0" class="msg-empty">
        <span class="admin-dot admin-dot--idle"></span>
        {{ activeTab === 'received' ? '暂无收到的催办' : '还没有发出过催办' }}
      </div>

      <ul v-else class="msg-list">
        <li
          v-for="item in current.list"
          :key="item.id"
          class="msg-card"
          :class="{ 'is-unread': activeTab === 'received' && !item.readAt }"
        >
          <div class="msg-main">
            <div class="msg-top">
              <span class="msg-type">催办</span>
              <strong class="msg-title">{{ item.title }}</strong>
              <span class="status-pill" :class="item.status === 'DONE' ? 'is-done' : 'is-pending'">
                {{ item.status === 'DONE' ? '已完成' : '待完成' }}
              </span>
            </div>

            <p v-if="item.content" class="msg-content">{{ item.content }}</p>

            <div class="msg-meta">
              <span v-if="activeTab === 'received'">来自 {{ item.fromUsername }}</span>
              <span v-else>发给 {{ item.toUsername }}</span>
              <span>·</span>
              <span>{{ formatTime(item.createdAt) }}</span>
            </div>

            <div v-if="item.reply" class="msg-reply">
              <span class="reply-label">回执</span>
              <p>{{ item.reply }}</p>
              <em>{{ formatTime(item.completedAt) }}</em>
            </div>
          </div>

          <div class="msg-actions">
            <el-button
              v-if="activeTab === 'received' && item.status === 'PENDING'"
              type="primary"
              size="small"
              @click="openComplete(item)"
            >标记完成</el-button>

            <el-button
              v-if="activeTab === 'sent' && item.status === 'DONE' && !item.acknowledgedAt"
              type="primary"
              size="small"
              @click="handleAck(item)"
            >确认收到</el-button>

            <span v-if="activeTab === 'sent' && item.status === 'PENDING'" class="wait-tip">
              等待对方处理
            </span>
            <span v-else-if="item.acknowledgedAt" class="wait-tip">已确认回执</span>
          </div>
        </li>
      </ul>

      <div v-if="current.total > current.size" class="pagination-bar">
        <el-pagination
          v-model:current-page="current.page"
          :page-size="current.size"
          :total="current.total"
          layout="total, prev, pager, next"
          @current-change="loadList()"
        />
      </div>
    </div>

    <!-- 发起催办 -->
    <el-dialog v-model="newDialog" class="admin-dialog" title="发起催办" width="480px">
      <div class="dialog-form">
        <label class="field-label">催办对象</label>
        <el-select v-model="form.toUserId" popper-class="admin-select-popper" placeholder="选择运营管理" style="width: 100%">
          <el-option
            v-for="u in recipients"
            :key="u.id"
            :label="`${u.username}（${u.email}）`"
            :value="u.id"
          />
        </el-select>

        <label class="field-label">催办标题</label>
        <el-input v-model="form.title" maxlength="120" placeholder="例如：今日内容审核队列清空" />

        <label class="field-label">催办说明（可选）</label>
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          maxlength="500"
          placeholder="补充要求或截止时间"
        />
      </div>

      <template #footer>
        <el-button @click="newDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitNew">发送催办</el-button>
      </template>
    </el-dialog>

    <!-- 完成回执 -->
    <el-dialog v-model="completeDialog" class="admin-dialog" title="标记完成" width="460px">
      <p class="dialog-tip">{{ completeTarget?.title }}</p>
      <el-input
        v-model="replyText"
        type="textarea"
        :rows="4"
        maxlength="500"
        placeholder="填写完成情况（可选，会同步给超级管理员）"
      />
      <template #footer>
        <el-button @click="completeDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitComplete">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { adminMessage as ElMessage } from '@/utils/adminMessage'
import { useAuthStore } from '@/store/auth'
import {
  getReceivedReminders,
  getSentReminders,
  getReminderStats,
  createReminder,
  completeReminder,
  acknowledgeReminder,
  markRemindersRead,
  getOperationsAdmins
} from '@/api/admin'

const authStore = useAuthStore()

const activeTab = ref('received')
const loading = ref(false)
const saving = ref(false)
const stats = ref({ unread: 0, receivedPending: 0, sentCompleted: 0 })

const received = reactive({ list: [], page: 1, size: 8, total: 0 })
const sent = reactive({ list: [], page: 1, size: 8, total: 0 })
const current = computed(() => (activeTab.value === 'received' ? received : sent))

const newDialog = ref(false)
const form = reactive({ toUserId: null, title: '', content: '' })
const recipients = ref([])

const completeDialog = ref(false)
const completeTarget = ref(null)
const replyText = ref('')

let readMarked = false

const statCards = computed(() => [
  { label: '未读消息', value: stats.value.unread || 0, tone: 'is-warn' },
  { label: '待我完成', value: stats.value.receivedPending || 0, tone: 'is-danger' },
  {
    label: authStore.isSuperAdmin ? '已回执待确认' : '已回执',
    value: stats.value.sentCompleted || 0,
    tone: 'is-ok'
  }
])

const formatTime = (v) => (v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '—')

const loadStats = async () => {
  try {
    stats.value = (await getReminderStats()) || stats.value
  } catch {
    // 统计失败不阻断列表
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const state = current.value
    const fetcher = activeTab.value === 'received' ? getReceivedReminders : getSentReminders
    const data = await fetcher({ page: state.page - 1, size: state.size })
    state.list = data?.list || []
    state.total = data?.total || 0
  } catch {
    // 全局请求拦截器已提示，这里不重复弹出
  } finally {
    loading.value = false
  }
}

const markRead = async () => {
  if (readMarked || activeTab.value !== 'received') return
  readMarked = true
  if (!stats.value.unread) return
  try {
    await markRemindersRead()
    await loadStats()
    window.dispatchEvent(new CustomEvent('light-note:admin-messages-read'))
  } catch {
    // 忽略已读失败
  }
}

const switchTab = async (tab) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  await loadList()
  if (tab === 'received') markRead()
}

const refreshAll = async () => {
  await loadStats()
  await loadList()
  if (activeTab.value === 'received') markRead()
}

const loadRecipients = async () => {
  try {
    const data = await getOperationsAdmins({ position: 'OPERATION', page: 0, size: 50 })
    recipients.value = data?.list || []
  } catch {
    recipients.value = []
  }
}

const openNew = async () => {
  form.toUserId = null
  form.title = ''
  form.content = ''
  newDialog.value = true
  if (recipients.value.length === 0) {
    await loadRecipients()
  }
}

const submitNew = async () => {
  if (!form.toUserId) {
    ElMessage.warning('请选择催办对象')
    return
  }
  if (!form.title.trim()) {
    ElMessage.warning('请填写催办标题')
    return
  }
  saving.value = true
  try {
    await createReminder({
      toUserId: form.toUserId,
      title: form.title.trim(),
      content: form.content.trim()
    })
    ElMessage.success('催办已发送')
    newDialog.value = false
    activeTab.value = 'sent'
    sent.page = 1
    await refreshAll()
  } catch {
    // 全局请求拦截器已提示
  } finally {
    saving.value = false
  }
}

const openComplete = (item) => {
  completeTarget.value = item
  replyText.value = ''
  completeDialog.value = true
}

const submitComplete = async () => {
  if (!completeTarget.value) return
  saving.value = true
  try {
    await completeReminder(completeTarget.value.id, replyText.value.trim())
    ElMessage.success('已回执完成，超级管理员会收到结果')
    completeDialog.value = false
    await refreshAll()
  } catch {
    // 全局请求拦截器已提示
  } finally {
    saving.value = false
  }
}

const handleAck = async (item) => {
  try {
    await acknowledgeReminder(item.id)
    ElMessage.success('已确认')
    await refreshAll()
    window.dispatchEvent(new CustomEvent('light-note:admin-messages-read'))
  } catch {
    // 全局请求拦截器已提示
  }
}

onMounted(async () => {
  await loadStats()
  await loadList()
  markRead()
})
</script>

<style lang="scss" scoped>
@use "@/styles/admin-mixins.scss" as *;

.admin-messages {
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
  &.is-danger { color: var(--admin-error-text); }
  &.is-ok { color: var(--admin-success-text); }
}

.list-panel {
  padding: var(--admin-space-4) var(--admin-space-5) var(--admin-space-4);
}

.msg-tabs {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.05);
  margin-bottom: var(--admin-space-4);
}

.msg-tab {
  height: 30px;
  padding: 0 14px;
  border-radius: 999px;
  border: none;
  background: transparent;
  color: var(--admin-text-secondary);
  font-family: inherit;
  font-size: var(--admin-text-sm);
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;

  &:hover {
    color: var(--admin-primary-700);
    background: rgba(255, 255, 255, 0.75);
  }

  &.is-active {
    background: var(--admin-grad-primary);
    color: #fff;
    box-shadow: 0 6px 16px rgba(89, 139, 253, 0.28);
  }
}

.tab-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--admin-error);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.msg-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--admin-space-3);
}

.msg-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--admin-space-4);
  padding: var(--admin-space-4);
  border-radius: var(--admin-radius-lg);
  border: 1px solid var(--admin-border);
  background: rgba(255, 255, 255, 0.6);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;

  &:hover {
    border-color: var(--admin-primary-200);
    box-shadow: var(--admin-shadow-sm);
    transform: translateY(-1px);
  }

  &.is-unread {
    border-color: rgba(89, 139, 253, 0.35);
    background: linear-gradient(120deg, rgba(239, 246, 255, 0.9), rgba(255, 255, 255, 0.7));
  }
}

.msg-main {
  flex: 1;
  min-width: 0;
}

.msg-top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.msg-type {
  height: 20px;
  padding: 0 8px;
  border-radius: 6px;
  background: var(--admin-primary-50);
  color: var(--admin-primary-700);
  font-size: 11px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
}

.msg-title {
  font-size: var(--admin-text-base);
  font-weight: var(--admin-font-semibold);
  color: var(--admin-text);
}

.status-pill {
  height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;

  &.is-pending { background: var(--admin-warning-light); color: var(--admin-warning-text); }
  &.is-done { background: var(--admin-success-light); color: var(--admin-success-text); }
}

.msg-content {
  margin: 8px 0 0;
  font-size: var(--admin-text-sm);
  line-height: var(--admin-leading-relaxed);
  color: var(--admin-text-secondary);
}

.msg-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
  font-size: var(--admin-text-xs);
  color: var(--admin-text-muted);
}

.msg-reply {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: var(--admin-radius-md);
  background: rgba(236, 253, 245, 0.7);
  border: 1px solid rgba(16, 185, 129, 0.18);

  .reply-label {
    font-size: 11px;
    font-weight: 700;
    color: var(--admin-success-text);
    margin-right: 6px;
  }

  p {
    margin: 4px 0 0;
    font-size: var(--admin-text-sm);
    color: var(--admin-text-secondary);
    line-height: var(--admin-leading-normal);
    white-space: pre-wrap;
  }

  em {
    display: block;
    margin-top: 4px;
    font-style: normal;
    font-size: 11px;
    color: var(--admin-text-muted);
  }
}

.msg-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.wait-tip {
  font-size: var(--admin-text-xs);
  color: var(--admin-text-muted);
  white-space: nowrap;
}

.msg-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: var(--admin-space-8) 0;
  justify-content: center;
  font-size: var(--admin-text-sm);
  color: var(--admin-text-muted);
}

.msg-skeleton {
  display: flex;
  flex-direction: column;
  gap: var(--admin-space-3);
}

.sk-block {
  height: 88px;
  border-radius: var(--admin-radius-lg);
  background: rgba(226, 232, 240, 0.45);
  animation: skPulse 1.4s ease-in-out infinite;
}

@keyframes skPulse {
  0%, 100% { opacity: 0.55; }
  50% { opacity: 1; }
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--admin-space-3);
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  margin-top: 6px;
  font-size: var(--admin-text-sm);
  font-weight: var(--admin-font-medium);
  color: var(--admin-text-secondary);
}

.dialog-tip {
  margin: 0 0 var(--admin-space-3);
  font-size: var(--admin-text-sm);
  color: var(--admin-text-secondary);
}

@media (max-width: 768px) {
  .stat-row { grid-template-columns: minmax(0, 1fr); }
  .msg-card { flex-direction: column; }
  .msg-actions { width: 100%; justify-content: flex-end; }
}
</style>
