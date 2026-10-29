<template>
  <div class="admin-user-manage">
    <h2 class="page-title">用户管理</h2>

    <el-row :gutter="16" v-loading="overviewLoading">
      <el-col :xs="12" :sm="8" :md="4" :lg="4" v-for="item in overviewCards" :key="item.label">
        <el-card class="overview-card" shadow="hover">
          <div class="overview-label">{{ item.label }}</div>
          <div class="overview-value">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card" shadow="never">
      <div class="filter-bar">
        <el-input v-model="filters.username" placeholder="用户名" clearable style="width: 160px" />
        <el-input v-model="filters.email" placeholder="邮箱" clearable style="width: 200px" />
        <el-select v-model="filters.role" popper-class="admin-select-popper" placeholder="角色" clearable style="width: 120px">
          <el-option label="用户" value="USER" />
          <el-option label="创作者" value="CREATOR" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
        <el-select v-model="filters.status" popper-class="admin-select-popper" placeholder="状态" clearable style="width: 120px">
          <el-option label="正常" value="NORMAL" />
          <el-option label="封禁" value="BANNED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table :data="userList" v-loading="loading" empty-text="暂无用户数据">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'CREATOR' ? 'success' : ''">
              {{ roleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'BANNED' ? 'danger' : 'success'">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="职位" width="104">
          <template #default="{ row }">
            <el-tag v-if="row.adminPosition" size="small" effect="plain">
              {{ positionText(row.adminPosition) }}
            </el-tag>
            <span v-else class="cell-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="articleCount" label="文章数" width="90" />
        <el-table-column prop="createdAt" label="注册时间" min-width="160" />
        <el-table-column label="操作" class-name="col-actions" width="330" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'BANNED'" type="danger" link size="small" @click="handleBan(row)">封禁</el-button>
            <el-button v-else type="success" link size="small" @click="handleUnban(row)">解封</el-button>
            <el-button type="primary" link size="small" @click="handleResetPassword(row)">重置密码</el-button>
            <template v-if="authStore.isSuperAdmin">
              <el-button v-if="row.role !== 'ADMIN'" type="warning" link size="small" @click="handleSetAdmin(row)">设为管理员</el-button>
              <el-button v-if="row.role === 'USER'" type="success" link size="small" @click="handleSetCreator(row)">设为创作者</el-button>
              <el-button
                v-if="row.role === 'ADMIN' && row.id !== authStore.user?.id"
                type="primary"
                link
                size="small"
                @click="openPosition(row)"
              >分配职位</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchUsers"
          @current-change="fetchUsers"
        />
      </div>
    </el-card>

    <el-dialog v-model="positionDialogVisible" class="admin-dialog" title="分配后台职位" width="440px">
      <p class="position-tip">
        为 <strong>{{ positionTarget?.username }}</strong> 分配职位；保存后该管理员需重新登录，权限才会生效。
      </p>
      <el-select v-model="positionValue" popper-class="admin-select-popper" style="width: 100%" placeholder="选择职位">
        <el-option
          v-for="p in POSITION_OPTIONS"
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
        <el-button type="primary" :loading="positionSaving" @click="submitPosition">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { adminMessage as ElMessage } from '@/utils/adminMessage'
import { adminConfirm } from '@/utils/adminConfirm'
import { getUsers, banUser, unbanUser, resetUserPassword, updateUserRole, getUserOverview, updateUserPosition } from '@/api/admin'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()

const POSITION_OPTIONS = [
  { value: 'SUPER', label: '超级管理员', desc: '全部模块' },
  { value: 'OPERATION', label: '运营管理', desc: '仅管理员账号' },
  { value: 'CONTENT', label: '内容运营', desc: '内容审核 / 推荐' },
  { value: 'COMMUNITY', label: '社区运营', desc: '树洞管理' },
  { value: 'USER', label: '用户运营', desc: '用户管理' },
  { value: 'FEEDBACK', label: '反馈运营', desc: '建议反馈' },
  { value: 'SYSTEM', label: '系统运维', desc: '系统配置 / 操作日志' },
  { value: 'DATA', label: '数据分析', desc: '通用数据看板' }
]

const positionText = (p) => POSITION_OPTIONS.find((o) => o.value === p)?.label || p

const positionDialogVisible = ref(false)
const positionTarget = ref(null)
const positionValue = ref('')
const positionSaving = ref(false)

const openPosition = (row) => {
  positionTarget.value = row
  positionValue.value = row.adminPosition || 'CONTENT'
  positionDialogVisible.value = true
}

const submitPosition = async () => {
  if (!positionTarget.value || !positionValue.value) return
  positionSaving.value = true
  try {
    await updateUserPosition(positionTarget.value.id, positionValue.value)
    ElMessage.success('职位已更新，该管理员需重新登录生效')
    positionDialogVisible.value = false
    fetchUsers()
  } catch {
    ElMessage.error('职位更新失败')
  } finally {
    positionSaving.value = false
  }
}

const loading = ref(false)
const overviewLoading = ref(false)
const userList = ref([])
const userOverview = ref({})

const filters = reactive({
  username: '',
  email: '',
  role: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const overviewCards = computed(() => [
  { label: '总用户数', value: userOverview.value.totalUsers || 0 },
  { label: '今日新增', value: userOverview.value.todayNewUsers || 0 },
  { label: '活跃用户', value: userOverview.value.activeUsers || 0 },
  { label: '创作者', value: userOverview.value.creatorCount || 0 },
  { label: '管理员', value: userOverview.value.adminCount || 0 },
  { label: '封禁用户', value: userOverview.value.bannedUsers || 0 }
])

const roleText = (role) => {
  const map = { USER: '用户', CREATOR: '创作者', ADMIN: '管理员' }
  return map[role] || role
}

const statusText = (status) => {
  const map = { NORMAL: '正常', ACTIVE: '正常', BANNED: '封禁' }
  return map[status] || status
}

const fetchUserOverview = async () => {
  overviewLoading.value = true
  try {
    const data = await getUserOverview()
    userOverview.value = data || {}
  } catch (error) {
    ElMessage.error('获取用户概览失败')
  } finally {
    overviewLoading.value = false
  }
}

const fetchUsers = async () => {
  loading.value = true
  try {
    // 空字符串会被后端当成「按该值精确筛选」，必须剔除，否则默认查询恒为空
    const activeFilters = Object.fromEntries(
      Object.entries(filters).filter(([, v]) => v !== '' && v !== null && v !== undefined)
    )
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
      ...activeFilters
    }
    const data = await getUsers(params)
    userList.value = data.list || []
    pagination.total = data.total || 0
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchUsers()
}

const resetFilters = () => {
  filters.username = ''
  filters.email = ''
  filters.role = ''
  filters.status = ''
  handleSearch()
}

const handleBan = async (row) => {
  try {
    await adminConfirm(`确定封禁用户 ${row.username} 吗？`, '提示', { type: 'warning' })
    await banUser(row.id)
    ElMessage.success('封禁成功')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('封禁失败')
  }
}

const handleUnban = async (row) => {
  try {
    await adminConfirm(`确定解封用户 ${row.username} 吗？`, '提示', { type: 'warning' })
    await unbanUser(row.id)
    ElMessage.success('解封成功')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('解封失败')
  }
}

const handleResetPassword = async (row) => {
  try {
    await adminConfirm(`确定重置用户 ${row.username} 的密码吗？`, '提示', { type: 'warning' })
    const res = await resetUserPassword(row.id)
    ElMessage.success('密码已重置为 123456')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('重置密码失败')
  }
}

const handleSetAdmin = async (row) => {
  try {
    await adminConfirm(`确定将用户 ${row.username} 设为管理员吗？`, '提示', { type: 'warning' })
    await updateUserRole(row.id, 'ADMIN')
    ElMessage.success('设置成功')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('设置管理员失败')
  }
}

const handleSetCreator = async (row) => {
  try {
    await adminConfirm(`确定将用户 ${row.username} 设为创作者吗？`, '提示', { type: 'warning' })
    await updateUserRole(row.id, 'CREATOR')
    ElMessage.success('设置成功')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('设置创作者失败')
  }
}

onMounted(() => {
  fetchUserOverview()
  fetchUsers()
})
</script>

<style lang="scss" scoped>
.page-title {
  margin: 0 0 var(--admin-space-6);
  font-size: var(--admin-text-xl);
  font-weight: var(--admin-font-semibold);
  color: var(--admin-text);
  letter-spacing: -0.01em;
}

/* 概览卡片 */
.overview-card {
  margin-bottom: 20px;
  padding: 18px;
  text-align: center;
  background: var(--admin-surface);
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius-lg);
  box-shadow: var(--admin-shadow-md);
  transition: all var(--admin-transition);
  cursor: default;
}

.overview-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--admin-shadow-lg), var(--admin-inset-highlight);
  border-color: var(--admin-glass-border);
}

.overview-label {
  font-size: var(--admin-text-sm);
  color: var(--admin-text-secondary);
  font-weight: var(--admin-font-medium);
  margin-bottom: 6px;
}

.overview-value {
  font-size: var(--admin-text-2xl);
  font-weight: var(--admin-font-bold);
  color: var(--admin-primary-700);
  font-variant-numeric: tabular-nums;
}

.table-card {
  margin-top: 0;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.cell-muted {
  color: var(--admin-text-muted);
}

.position-tip {
  margin: 0 0 var(--admin-space-3);
  font-size: var(--admin-text-sm);
  color: var(--admin-text-secondary);
  line-height: var(--admin-leading-normal);

  strong {
    color: var(--admin-text);
  }
}

.position-option {
  display: flex;
  align-items: baseline;
  gap: 8px;

  strong {
    font-weight: var(--admin-font-semibold);
  }

  em {
    font-style: normal;
    font-size: var(--admin-text-xs);
    color: var(--admin-text-muted);
  }
}
</style>
