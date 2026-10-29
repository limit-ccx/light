<template>
  <div class="admin-operations">
    <div class="page-head">
      <div>
        <p class="admin-eyebrow">运营中心 · 组织</p>
        <h2 class="admin-page-title">运营管理</h2>
        <p class="admin-page-desc">管理员账号与职位分配；只处理管理员，普通用户治理请前往用户管理</p>
      </div>
      <div class="admin-toolbar">
        <el-input
          v-model="filters.keyword"
          placeholder="用户名 / 邮箱"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="filters.position"
          popper-class="admin-select-popper"
          clearable
          placeholder="全部职位"
          style="width: 150px"
          @change="handleSearch"
        >
          <el-option v-for="p in positionOptions" :key="p.value" :label="p.label" :value="p.value" />
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
      <el-table :data="list" v-loading="loading" empty-text="暂无管理员">
        <el-table-column label="管理员" min-width="190">
          <template #default="{ row }">
            <div class="admin-cell">
              <span class="admin-avatar">
                <img v-if="row.avatar" :src="row.avatar" alt="" />
                <span v-else class="admin-initial">{{ initial(row.username) }}</span>
              </span>
              <span class="admin-main">
                <strong>{{ row.username }}</strong>
                <em>{{ row.email }}</em>
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="职位" width="120">
          <template #default="{ row }">
            <el-tag :type="positionTagType(row.adminPosition)" size="small" effect="light">
              {{ positionText(row.adminPosition) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="status-pill" :class="row.status === 'BANNED' ? 'is-banned' : 'is-ok'">
              {{ row.status === 'BANNED' ? '已封禁' : '正常' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="articleCount" label="文章" width="80" />
        <el-table-column prop="createdAt" label="注册时间" width="165">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column label="操作" class-name="col-actions" width="250" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canAssign(row)" type="primary" link size="small" @click="openPosition(row)">
              分配职位
            </el-button>
            <el-button v-if="canReset(row)" type="primary" link size="small" @click="handleReset(row)">
              重置密码
            </el-button>
            <el-button v-if="canRevoke(row)" type="danger" link size="small" @click="handleRevoke(row)">
              撤销管理员
            </el-button>
            <span v-if="isSelf(row)" class="self-tip">当前账号</span>
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

    <!-- 分配职位 -->
    <el-dialog v-model="positionDialogVisible" class="admin-dialog" title="分配后台职位" width="440px">
      <p class="dialog-tip">
        为 <strong>{{ positionTarget?.username }}</strong> 分配职位；保存后对方需重新登录生效。
      </p>
      <el-select v-model="positionValue" popper-class="admin-select-popper" style="width: 100%" placeholder="选择职位">
        <el-option
          v-for="p in assignablePositions"
          :key="p.value"
          :label="p.label"
          :value="p.value"
        >
          <span class="position-option">
            <strong>{{ p.label }}</strong>
            <em>{{ p.desc }}</em>
          </span>
        </el-option>
      </el-select>
      <template #footer>
        <el-button @click="positionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPosition">保存</el-button>
      </template>
    </el-dialog>

    <!-- 临时密码 -->
    <el-dialog v-model="passwordDialogVisible" class="admin-dialog" title="密码已重置" width="420px">
      <p class="dialog-tip">请把临时密码线下发给该管理员，并提示其登录后立即修改。</p>
      <div class="temp-password">{{ tempPassword }}</div>
      <template #footer>
        <el-button type="primary" @click="passwordDialogVisible = false">我已记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { adminMessage as ElMessage } from '@/utils/adminMessage'
import { adminConfirm } from '@/utils/adminConfirm'
import { useAuthStore } from '@/store/auth'
import {
  getOperationsAdmins,
  updateOperationPosition,
  resetOperationPassword,
  updateOperationRole
} from '@/api/admin'

const authStore = useAuthStore()

const POSITION_OPTIONS = [
  { value: 'SUPER', label: '超级管理员', desc: '全部模块' },
  { value: 'OPERATION', label: '运营管理', desc: '管理员账号' },
  { value: 'CONTENT', label: '内容运营', desc: '内容审核 / 推荐' },
  { value: 'COMMUNITY', label: '社区运营', desc: '树洞管理' },
  { value: 'USER', label: '用户运营', desc: '用户管理' },
  { value: 'FEEDBACK', label: '反馈运营', desc: '建议反馈' },
  { value: 'SYSTEM', label: '系统运维', desc: '系统配置 / 操作日志' },
  { value: 'DATA', label: '数据分析', desc: '通用数据看板' }
]

// 运营管理不能把他人指派为超级管理员
const positionOptions = computed(() =>
  authStore.isSuperAdmin ? POSITION_OPTIONS : POSITION_OPTIONS.filter((p) => p.value !== 'SUPER')
)
const assignablePositions = positionOptions

const positionText = (p) => POSITION_OPTIONS.find((o) => o.value === p)?.label || '未分配'
const positionTagType = (p) => ({
  SUPER: 'danger',
  OPERATION: 'warning',
  CONTENT: 'primary',
  COMMUNITY: 'success',
  USER: 'info',
  FEEDBACK: 'info',
  SYSTEM: 'warning',
  DATA: 'info'
}[p] || 'info')

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const allAdmins = ref([])
const filters = reactive({ keyword: '', position: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

const positionDialogVisible = ref(false)
const positionTarget = ref(null)
const positionValue = ref('')

const passwordDialogVisible = ref(false)
const tempPassword = ref('')

const statCards = computed(() => {
  const rows = allAdmins.value
  return [
    { label: '管理员总数', value: rows.length, tone: '' },
    { label: '超级管理员', value: rows.filter((r) => r.adminPosition === 'SUPER').length, tone: 'is-danger' },
    { label: '运营管理', value: rows.filter((r) => r.adminPosition === 'OPERATION').length, tone: 'is-warn' },
    { label: '其他运营', value: rows.filter((r) => r.adminPosition && !['SUPER', 'OPERATION'].includes(r.adminPosition)).length, tone: 'is-ok' }
  ]
})

const initial = (name) => (name || 'A').slice(0, 1).toUpperCase()
const formatTime = (v) => (v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '—')

const isSelf = (row) => row.id === authStore.user?.id
const canAssign = (row) => !isSelf(row) && (authStore.isSuperAdmin || row.adminPosition !== 'SUPER')
const canReset = (row) => !isSelf(row) && (authStore.isSuperAdmin || row.adminPosition !== 'SUPER')
const canRevoke = (row) =>
  authStore.isSuperAdmin && !isSelf(row) && row.adminPosition !== 'SUPER'

const fetchList = async () => {
  loading.value = true
  try {
    const params = { page: pagination.page - 1, size: pagination.size }
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
    if (filters.position) params.position = filters.position
    const data = await getOperationsAdmins(params)
    list.value = data?.list || []
    pagination.total = data?.total || 0
  } catch {
    ElMessage.error('获取管理员列表失败')
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const data = await getOperationsAdmins({ page: 0, size: 100 })
    allAdmins.value = data?.list || []
  } catch {
    allAdmins.value = []
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

const openPosition = (row) => {
  positionTarget.value = row
  positionValue.value = row.adminPosition || 'CONTENT'
  positionDialogVisible.value = true
}

const submitPosition = async () => {
  if (!positionTarget.value || !positionValue.value) return
  saving.value = true
  try {
    await updateOperationPosition(positionTarget.value.id, positionValue.value)
    ElMessage.success('职位已更新，对方需重新登录生效')
    positionDialogVisible.value = false
    refreshAll()
  } catch {
    ElMessage.error('职位更新失败')
  } finally {
    saving.value = false
  }
}

const handleReset = async (row) => {
  try {
    await adminConfirm(
      `确定重置管理员 ${row.username} 的密码吗？其当前会话会立即失效。`,
      '重置密码',
      { type: 'warning' }
    )
    const data = await resetOperationPassword(row.id)
    tempPassword.value = data?.tempPassword || '（未返回临时密码）'
    passwordDialogVisible.value = true
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('重置密码失败')
  }
}

const handleRevoke = async (row) => {
  try {
    await adminConfirm(
      `确定撤销 ${row.username} 的管理员身份吗？撤销后将变为普通用户。`,
      '撤销管理员',
      { type: 'warning', confirmButtonText: '确定撤销' }
    )
    await updateOperationRole(row.id, 'USER')
    ElMessage.success('已撤销管理员身份')
    refreshAll()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('撤销失败')
  }
}

onMounted(refreshAll)
</script>

<style lang="scss" scoped>
@use "@/styles/admin-mixins.scss" as *;

.admin-operations {
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

  &.is-danger { color: var(--admin-error-text); }
  &.is-warn { color: var(--admin-warning-text); }
  &.is-ok { color: var(--admin-success-text); }
}

.table-panel {
  padding: var(--admin-space-4) var(--admin-space-4) var(--admin-space-3);
}

.admin-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(140deg, #93c5fd, #8b5cf6);

  img { width: 100%; height: 100%; object-fit: cover; }
}

.admin-initial {
  font-size: var(--admin-text-sm);
  font-weight: var(--admin-font-bold);
  color: #fff;
}

.admin-main {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
  min-width: 0;

  strong {
    font-size: var(--admin-text-base);
    font-weight: var(--admin-font-semibold);
    color: var(--admin-text);
    overflow: hidden;
    text-overflow: ellipsis;
  }

  em {
    font-style: normal;
    font-size: var(--admin-text-xs);
    color: var(--admin-text-muted);
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border-radius: var(--admin-radius-full);
  font-size: var(--admin-text-xs);
  font-weight: var(--admin-font-medium);

  &.is-ok { background: var(--admin-success-light); color: var(--admin-success-text); }
  &.is-banned { background: var(--admin-error-light); color: var(--admin-error-text); }
}

.self-tip {
  font-size: var(--admin-text-xs);
  color: var(--admin-text-muted);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--admin-space-3);
}

.dialog-tip {
  margin: 0 0 var(--admin-space-3);
  font-size: var(--admin-text-sm);
  color: var(--admin-text-secondary);
  line-height: var(--admin-leading-normal);

  strong { color: var(--admin-text); }
}

.position-option {
  display: flex;
  align-items: baseline;
  gap: 8px;

  strong { font-weight: var(--admin-font-semibold); }

  em {
    font-style: normal;
    font-size: var(--admin-text-xs);
    color: var(--admin-text-muted);
  }
}

.temp-password {
  padding: var(--admin-space-4);
  border-radius: var(--admin-radius-lg);
  background: rgba(15, 23, 42, 0.05);
  border: 1px dashed var(--admin-border-strong);
  font-family: var(--admin-font-mono);
  font-size: var(--admin-text-lg);
  font-weight: var(--admin-font-bold);
  letter-spacing: 0.06em;
  text-align: center;
  color: var(--admin-primary-700);
  user-select: all;
}

@media (max-width: 992px) {
  .stat-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
