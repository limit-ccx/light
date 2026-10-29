<template>
  <div class="admin-operation-log">
    <h2 class="page-title">操作日志</h2>

    <el-card shadow="never">
      <div class="filter-bar">
        <el-select v-model="filters.action" popper-class="admin-select-popper" placeholder="操作类型" clearable style="width: 160px">
          <el-option-group label="用户操作">
            <el-option label="封禁用户" value="BAN_USER" />
            <el-option label="解封用户" value="UNBAN_USER" />
            <el-option label="重置密码" value="RESET_PASSWORD" />
            <el-option label="调整角色" value="UPDATE_ROLE" />
          </el-option-group>
          <el-option-group label="内容操作">
            <el-option label="通过文章" value="APPROVE_ARTICLE" />
            <el-option label="下架/驳回文章" value="TAKEDOWN_ARTICLE" />
            <el-option label="通过评论" value="APPROVE_COMMENT" />
            <el-option label="删除评论" value="DELETE_COMMENT" />
            <el-option label="处理举报" value="HANDLE_REPORT" />
          </el-option-group>
          <el-option-group label="树洞操作">
            <el-option label="通过树洞内容" value="TREEHOLE_APPROVE_POST" />
            <el-option label="驳回树洞内容" value="TREEHOLE_REJECT_POST" />
            <el-option label="删除树洞内容" value="TREEHOLE_DELETE_POST" />
            <el-option label="处理树洞举报" value="TREEHOLE_RESOLVE_REPORT" />
            <el-option label="忽略树洞举报" value="TREEHOLE_DISMISS_REPORT" />
          </el-option-group>
          <el-option-group label="配置操作">
            <el-option label="修改配置" value="UPDATE_CONFIG" />
            <el-option label="批量配置" value="BATCH_UPDATE_CONFIG" />
            <el-option label="新建公告" value="CREATE_ANNOUNCEMENT" />
            <el-option label="更新公告" value="UPDATE_ANNOUNCEMENT" />
            <el-option label="删除公告" value="DELETE_ANNOUNCEMENT" />
            <el-option label="新增Banner" value="CREATE_BANNER" />
            <el-option label="更新Banner" value="UPDATE_BANNER" />
            <el-option label="Banner状态" value="UPDATE_BANNER_STATUS" />
            <el-option label="删除Banner" value="DELETE_BANNER" />
          </el-option-group>
          <el-option-group label="推荐操作">
            <el-option label="添加推荐" value="ADD_FEATURED" />
            <el-option label="移除推荐" value="REMOVE_FEATURED" />
          </el-option-group>
          <el-option-group label="敏感词">
            <el-option label="添加敏感词" value="ADD_SENSITIVE_WORD" />
            <el-option label="删除敏感词" value="DELETE_SENSITIVE_WORD" />
          </el-option-group>
          <!-- 历史日志沿用了旧的动作命名，保留可筛选项 -->
          <el-option-group label="历史命名">
            <el-option label="设置角色（旧）" value="SET_ROLE" />
            <el-option label="驳回文章（旧）" value="REJECT_ARTICLE" />
            <el-option label="下架文章（旧）" value="TAKE_DOWN_ARTICLE" />
            <el-option label="发布公告（旧）" value="PUBLISH_ANNOUNCEMENT" />
            <el-option label="添加Banner（旧）" value="ADD_BANNER" />
          </el-option-group>
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 280px"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <span class="log-count" v-if="pagination.total > 0">共 {{ pagination.total }} 条记录</span>
      </div>

      <el-table :data="logList" v-loading="loading" empty-text="暂无日志数据" :row-class-name="getRowClassName">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="action" label="操作类型" width="150">
          <template #default="{ row }">
            <el-tag :type="getActionTagType(row.action)" size="small">{{ actionText(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="操作对象类型" width="140" />
        <el-table-column prop="targetId" label="对象ID" width="100" />
        <el-table-column prop="detail" label="详情" min-width="280" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP 地址" width="140">
          <template #default="{ row }">
            {{ row.ip || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="操作时间" min-width="170" />
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchLogs"
          @current-change="fetchLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminMessage as ElMessage } from '@/utils/adminMessage'
import { getAdminLogs } from '@/api/admin'

const loading = ref(false)
const logList = ref([])
const dateRange = ref(null)

const filters = reactive({
  action: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const actionText = (action) => {
  const map = {
    BAN_USER: '封禁用户',
    UNBAN_USER: '解封用户',
    RESET_PASSWORD: '重置密码',
    SET_ROLE: '修改角色',
    UPDATE_ROLE: '调整角色',
    APPROVE_ARTICLE: '通过文章',
    REJECT_ARTICLE: '驳回文章',
    TAKE_DOWN_ARTICLE: '下架文章',
    TAKEDOWN_ARTICLE: '下架/驳回文章',
    DELETE_COMMENT: '删除评论',
    APPROVE_COMMENT: '通过评论',
    HANDLE_REPORT: '处理举报',
    UPDATE_CONFIG: '修改配置',
    BATCH_UPDATE_CONFIG: '批量配置',
    CREATE_ANNOUNCEMENT: '发布公告',
    PUBLISH_ANNOUNCEMENT: '发布公告',
    UPDATE_ANNOUNCEMENT: '更新公告',
    DELETE_ANNOUNCEMENT: '删除公告',
    CREATE_BANNER: '创建Banner',
    ADD_BANNER: '添加Banner',
    UPDATE_BANNER: '更新Banner',
    UPDATE_BANNER_STATUS: 'Banner状态',
    DELETE_BANNER: '删除Banner',
    ADD_FEATURED: '添加推荐',
    REMOVE_FEATURED: '移除推荐',
    ADD_SENSITIVE_WORD: '添加敏感词',
    DELETE_SENSITIVE_WORD: '删除敏感词',
    TREEHOLE_APPROVE_POST: '通过树洞内容',
    TREEHOLE_REJECT_POST: '驳回树洞内容',
    TREEHOLE_DELETE_POST: '删除树洞内容',
    TREEHOLE_DELETE_REPLY: '删除树洞回复',
    TREEHOLE_RESOLVE_REPORT: '处理树洞举报',
    TREEHOLE_DISMISS_REPORT: '忽略树洞举报',
    LOGIN: '登录',
    LOGOUT: '登出'
  }
  return map[action] || action
}

const getActionTagType = (action) => {
  if (action?.includes('BAN') || action?.includes('DELETE') || action?.includes('TAKEDOWN')) return 'danger'
  if (action?.includes('APPROVE') || action?.includes('CREATE') || action?.includes('ADD')) return 'success'
  if (action?.includes('UPDATE') || action?.includes('HANDLE')) return 'warning'
  return 'info'
}

const getRowClassName = ({ row }) => {
  if (row.action?.includes('BAN') || row.action?.includes('DELETE')) return 'danger-row'
  return ''
}

const fetchLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
      action: filters.action || undefined
    }
    // 如果有日期范围，传递日期参数
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const data = await getAdminLogs(params)
    logList.value = data.list || []
    pagination.total = data.total || 0
  } catch (error) {
    ElMessage.error('获取操作日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchLogs()
}

const resetFilters = () => {
  filters.action = ''
  dateRange.value = null
  handleSearch()
}

onMounted(fetchLogs)
</script>

<style lang="scss" scoped>
.page-title {
  margin: 0 0 var(--admin-space-6);
  font-size: var(--admin-text-xl);
  font-weight: var(--admin-font-semibold);
  color: var(--admin-text);
  letter-spacing: -0.01em;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
}

.log-count {
  margin-left: auto;
  font-size: 13px;
  color: var(--admin-text-secondary);
  font-weight: 500;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

:deep(.danger-row) {
  background: rgba(239, 68, 68, 0.04) !important;
}
</style>
