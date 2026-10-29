<template>
  <div class="admin-content-review">
    <h2 class="page-title">内容审核</h2>

    <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange">
      <el-tab-pane label="文章审核" name="articles">
        <div class="filter-bar">
          <el-input v-model="articleFilters.keyword" placeholder="搜索标题" clearable style="width: 200px" @keyup.enter="fetchArticles" />
          <el-select v-model="articleFilters.status" popper-class="admin-select-popper" placeholder="状态" clearable style="width: 140px">
            <el-option label="草稿" :value="1" />
            <el-option label="已发布" :value="2" />
            <el-option label="待审核" :value="3" />
            <el-option label="已下架" :value="4" />
          </el-select>
          <el-button type="primary" @click="fetchArticles">查询</el-button>
          <el-button @click="articleFilters.status = null; articleFilters.keyword = ''; fetchArticles()">重置</el-button>
        </div>
        <el-table :data="articles" v-loading="articleLoading" empty-text="暂无文章数据">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="authorName" label="作者" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="articleStatusType(row.status)">{{ articleStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="阅读" width="80" />
          <el-table-column prop="likeCount" label="点赞" width="80" />
          <el-table-column prop="createdAt" label="提交时间" min-width="160" />
          <el-table-column label="操作" class-name="col-actions" width="220" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status !== 2" type="success" link size="small" @click="reviewArticleItem(row, 2)">通过</el-button>
              <el-button v-if="row.status !== 4" type="danger" link size="small" @click="reviewArticleItem(row, 4)">下架</el-button>
              <el-button type="info" link size="small" @click="viewArticleDetail(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="articlePage.page"
            v-model:page-size="articlePage.size"
            :total="articlePage.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchArticles"
            @current-change="fetchArticles"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="评论审核" name="comments">
        <div class="filter-bar">
          <el-input v-model="commentFilters.keyword" placeholder="搜索评论内容" clearable style="width: 200px" @keyup.enter="fetchComments" />
          <el-select v-model="commentFilters.status" popper-class="admin-select-popper" placeholder="状态" clearable style="width: 140px">
            <el-option label="正常" value="NORMAL" />
            <el-option label="待审核" value="PENDING" />
            <el-option label="已删除" value="DELETED" />
          </el-select>
          <el-button type="primary" @click="fetchComments">查询</el-button>
          <el-button @click="commentFilters.status = ''; commentFilters.keyword = ''; fetchComments()">重置</el-button>
        </div>
        <el-table :data="comments" v-loading="commentLoading" empty-text="暂无评论数据">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
          <el-table-column prop="articleTitle" label="所属文章" min-width="140" show-overflow-tooltip />
          <el-table-column prop="authorName" label="作者" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="commentStatusType(row.status)">{{ commentStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="提交时间" min-width="160" />
          <el-table-column label="操作" class-name="col-actions" width="180" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status !== 'NORMAL'" type="success" link size="small" @click="reviewCommentItem(row, 'NORMAL')">通过</el-button>
              <el-button v-if="row.status !== 'DELETED'" type="danger" link size="small" @click="reviewCommentItem(row, 'DELETED')">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="commentPage.page"
            v-model:page-size="commentPage.size"
            :total="commentPage.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchComments"
            @current-change="fetchComments"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="举报管理" name="reports">
        <div class="filter-bar">
          <el-select v-model="reportFilters.status" popper-class="admin-select-popper" placeholder="处理状态" clearable style="width: 140px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已处理" value="HANDLED" />
          </el-select>
          <el-button type="primary" @click="fetchReports">查询</el-button>
          <el-button @click="reportFilters.status = ''; fetchReports()">重置</el-button>
        </div>
        <el-table :data="reports" v-loading="reportLoading" empty-text="暂无举报数据">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="targetType" label="目标类型" width="120" />
          <el-table-column prop="targetTitle" label="目标内容" min-width="160" show-overflow-tooltip />
          <el-table-column prop="reason" label="举报原因" min-width="180" show-overflow-tooltip />
          <el-table-column prop="reporterName" label="举报人" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PENDING' ? 'warning' : 'success'">{{ row.status === 'PENDING' ? '待处理' : '已处理' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="举报时间" min-width="160" />
          <el-table-column label="操作" class-name="col-actions" width="160" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PENDING'" type="primary" link size="small" @click="handleReportItem(row)">处理</el-button>
              <el-tag v-else type="info" size="small">已处理</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="reportPage.page"
            v-model:page-size="reportPage.size"
            :total="reportPage.total"
            layout="total, prev, pager, next"
            @current-change="fetchReports"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="敏感词管理" name="sensitive">
        <div class="filter-bar">
          <el-input v-model="newSensitiveWord" placeholder="输入敏感词" clearable style="width: 240px" @keyup.enter="addWord" />
          <el-button type="primary" @click="addWord">新增敏感词</el-button>
          <el-input v-model="checkText" placeholder="检测文本" clearable style="width: 240px; margin-left: auto" />
          <el-button type="warning" @click="checkWord">检测</el-button>
        </div>
        <div v-if="checkResult !== null" class="check-result" :class="checkResult ? 'danger' : 'success'">
          {{ checkResult ? '⚠️ 包含敏感词' : '✅ 未检测到敏感词' }}
        </div>
        <el-table :data="sensitiveWords" v-loading="wordLoading" empty-text="暂无敏感词">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="word" label="敏感词" min-width="200" />
          <el-table-column prop="category" label="分类" width="120" />
          <el-table-column prop="createdAt" label="添加时间" min-width="160" />
          <el-table-column label="操作" class-name="col-actions" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="danger" link size="small" @click="deleteWord(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="wordPage.page"
            v-model:page-size="wordPage.size"
            :total="wordPage.total"
            layout="total, prev, pager, next"
            @current-change="fetchSensitiveWords"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 文章详情弹窗 -->
    <el-dialog v-model="articleDetailVisible" class="admin-dialog" title="文章详情" width="700px">
      <div v-if="currentArticle" class="article-detail-dialog">
        <h3>{{ currentArticle.title }}</h3>
        <div class="meta">
          <span>作者: {{ currentArticle.authorName }}</span>
          <span>状态: {{ articleStatusText(currentArticle.status) }}</span>
          <span>阅读: {{ currentArticle.viewCount }}</span>
          <span>点赞: {{ currentArticle.likeCount }}</span>
        </div>
        <div class="tags" v-if="currentArticle.tags?.length">
          <el-tag v-for="tag in currentArticle.tags" :key="tag" size="small" type="info">{{ tag }}</el-tag>
        </div>
        <el-divider />
        <div class="summary" v-if="currentArticle.summary">
          <strong>摘要：</strong>{{ currentArticle.summary }}
        </div>
      </div>
      <template #footer>
        <el-button @click="articleDetailVisible = false">关闭</el-button>
        <el-button v-if="currentArticle && currentArticle.status !== 2" type="success" @click="reviewArticleItem(currentArticle, 2); articleDetailVisible = false">通过</el-button>
        <el-button v-if="currentArticle && currentArticle.status !== 4" type="danger" @click="reviewArticleItem(currentArticle, 4); articleDetailVisible = false">下架</el-button>
      </template>
    </el-dialog>

    <!-- 举报处理弹窗 -->
    <el-dialog v-model="reportDialogVisible" class="admin-dialog" title="处理举报" width="500px">
      <el-form :model="reportHandleForm" label-width="80px">
        <el-form-item label="处理结果">
          <el-select v-model="reportHandleForm.status" popper-class="admin-select-popper" style="width: 100%">
            <el-option label="确认违规（下架）" value="HANDLED" />
            <el-option label="误报（忽略）" value="DISMISSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input v-model="reportHandleForm.handleResult" type="textarea" :rows="3" placeholder="填写处理说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReportHandle">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminMessage as ElMessage } from '@/utils/adminMessage'
import { adminConfirm } from '@/utils/adminConfirm'
import {
  getAdminArticles, reviewArticle, getAdminComments, reviewComment,
  getReports, handleReport, getSensitiveWords, addSensitiveWord, deleteSensitiveWord,
  checkSensitiveWord
} from '@/api/admin'

const activeTab = ref('articles')

// ===== 文章审核 =====
const articles = ref([])
const articleLoading = ref(false)
const articleFilters = reactive({ status: null, keyword: '' })
const articlePage = reactive({ page: 1, size: 10, total: 0 })
const articleDetailVisible = ref(false)
const currentArticle = ref(null)

// ===== 评论审核 =====
const comments = ref([])
const commentLoading = ref(false)
const commentFilters = reactive({ status: '', keyword: '' })
const commentPage = reactive({ page: 1, size: 10, total: 0 })

// ===== 举报管理 =====
const reports = ref([])
const reportLoading = ref(false)
const reportFilters = reactive({ status: '' })
const reportPage = reactive({ page: 1, size: 10, total: 0 })
const reportDialogVisible = ref(false)
const currentReport = ref(null)
const reportHandleForm = reactive({ status: 'HANDLED', handleResult: '' })

// ===== 敏感词 =====
const sensitiveWords = ref([])
const wordLoading = ref(false)
const newSensitiveWord = ref('')
const wordPage = reactive({ page: 1, size: 10, total: 0 })
const checkText = ref('')
const checkResult = ref(null)

// ===== 状态映射 =====
const articleStatusText = (status) => {
  const map = { 1: '草稿', 2: '已发布', 3: '待审核', 4: '已下架' }
  return map[status] || `未知(${status})`
}

const articleStatusType = (status) => {
  const map = { 1: 'info', 2: 'success', 3: 'warning', 4: 'danger' }
  return map[status] || ''
}

const commentStatusText = (status) => {
  const map = { NORMAL: '正常', PENDING: '待审核', DELETED: '已删除' }
  return map[status] || status
}

const commentStatusType = (status) => {
  const map = { NORMAL: 'success', PENDING: 'warning', DELETED: 'danger' }
  return map[status] || ''
}

const handleTabChange = (tab) => {
  if (tab === 'articles') fetchArticles()
  else if (tab === 'comments') fetchComments()
  else if (tab === 'reports') fetchReports()
  else if (tab === 'sensitive') fetchSensitiveWords()
}

// ===== 文章 =====
const fetchArticles = async () => {
  articleLoading.value = true
  try {
    const data = await getAdminArticles({
      page: articlePage.page - 1,
      size: articlePage.size,
      status: articleFilters.status,
      keyword: articleFilters.keyword || undefined
    })
    articles.value = data.list || []
    articlePage.total = data.total || 0
  } catch (error) {
    ElMessage.error('获取文章列表失败')
  } finally {
    articleLoading.value = false
  }
}

const reviewArticleItem = async (row, status) => {
  const actionText = status === 2 ? '通过' : '下架'
  try {
    await adminConfirm(`确定对文章《${row.title}》执行"${actionText}"操作吗？`, '提示', { type: 'warning' })
    await reviewArticle(row.id, { status, reason: '' })
    ElMessage.success('操作成功')
    fetchArticles()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

const viewArticleDetail = (row) => {
  currentArticle.value = row
  articleDetailVisible.value = true
}

// ===== 评论 =====
const fetchComments = async () => {
  commentLoading.value = true
  try {
    const data = await getAdminComments({
      page: commentPage.page - 1,
      size: commentPage.size,
      status: commentFilters.status || undefined,
      keyword: commentFilters.keyword || undefined
    })
    comments.value = data.list || []
    commentPage.total = data.total || 0
  } catch (error) {
    ElMessage.error('获取评论列表失败')
  } finally {
    commentLoading.value = false
  }
}

const reviewCommentItem = async (row, status) => {
  const actionText = status === 'NORMAL' ? '通过' : '删除'
  try {
    await adminConfirm(`确定对该评论执行"${actionText}"操作吗？`, '提示', { type: 'warning' })
    await reviewComment(row.id, { status })
    ElMessage.success('操作成功')
    fetchComments()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

// ===== 举报 =====
const fetchReports = async () => {
  reportLoading.value = true
  try {
    const data = await getReports({
      page: reportPage.page - 1,
      size: reportPage.size,
      status: reportFilters.status || undefined
    })
    reports.value = data.list || []
    reportPage.total = data.total || 0
  } catch (error) {
    ElMessage.error('获取举报列表失败')
  } finally {
    reportLoading.value = false
  }
}

const handleReportItem = (row) => {
  currentReport.value = row
  reportHandleForm.status = 'HANDLED'
  reportHandleForm.handleResult = ''
  reportDialogVisible.value = true
}

const submitReportHandle = async () => {
  try {
    await handleReport(currentReport.value.id, {
      status: reportHandleForm.status,
      handleResult: reportHandleForm.handleResult
    })
    ElMessage.success('处理成功')
    reportDialogVisible.value = false
    fetchReports()
  } catch (error) {
    ElMessage.error('处理失败')
  }
}

// ===== 敏感词 =====
const fetchSensitiveWords = async () => {
  wordLoading.value = true
  try {
    const data = await getSensitiveWords({
      page: wordPage.page - 1,
      size: wordPage.size
    })
    sensitiveWords.value = data.list || []
    wordPage.total = data.total || 0
  } catch (error) {
    ElMessage.error('获取敏感词失败')
  } finally {
    wordLoading.value = false
  }
}

const addWord = async () => {
  if (!newSensitiveWord.value.trim()) return ElMessage.warning('请输入敏感词')
  try {
    await addSensitiveWord({ word: newSensitiveWord.value.trim() })
    ElMessage.success('添加成功')
    newSensitiveWord.value = ''
    fetchSensitiveWords()
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

const deleteWord = async (row) => {
  try {
    await adminConfirm(`确定删除敏感词"${row.word}"吗？`, '提示', { type: 'warning' })
    await deleteSensitiveWord(row.id)
    ElMessage.success('删除成功')
    fetchSensitiveWords()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const checkWord = async () => {
  if (!checkText.value.trim()) return ElMessage.warning('请输入要检测的文本')
  try {
    const data = await checkSensitiveWord(checkText.value)
    checkResult.value = data.containsSensitiveWord
  } catch (error) {
    ElMessage.error('检测失败')
  }
}

onMounted(() => {
  fetchArticles()
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

.check-result {
  padding: 10px var(--admin-space-4);
  margin-bottom: var(--admin-space-4);
  border-radius: var(--admin-radius-md);
  font-weight: var(--admin-font-medium);
  font-size: var(--admin-text-base);

  &.danger {
    background: var(--admin-error-light);
    color: var(--admin-error-text);
    border: 1px solid rgba(176, 118, 111, 0.24);
  }

  &.success {
    background: var(--admin-success-light);
    color: var(--admin-success-text);
    border: 1px solid rgba(107, 136, 116, 0.24);
  }
}

.article-detail-dialog {
  h3 {
    margin: 0 0 12px;
    font-size: 18px;
    font-weight: 600;
    color: var(--admin-text);
  }

  .meta {
    display: flex;
    gap: 16px;
    margin-bottom: 12px;
    font-size: 13px;
    color: var(--admin-text-secondary);
  }

  .tags {
    display: flex;
    gap: 6px;
    margin-bottom: 12px;
  }

  .summary {
    font-size: 14px;
    color: var(--admin-text-secondary);
    line-height: 1.6;
  }
}
</style>
