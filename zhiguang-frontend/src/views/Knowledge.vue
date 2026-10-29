<template>
  <InfoLayout
    eyebrow="Knowledge Base"
    title="我的知识库"
    description="把读过的文章沉淀成可检索、可问答、可连接的知识网络。"
    :stats="heroStats"
    :particles="false"
  >
    <!-- 快捷操作 -->
    <div class="quick-actions">
      <button type="button" class="quick-action" @click="showRagDialog = true">
        <span class="qa-icon tone-violet"><el-icon><ChatDotRound /></el-icon></span>
        <span class="qa-copy">
          <strong>AI 问答</strong>
          <small>基于知识库智能问答</small>
        </span>
        <span class="qa-arrow">→</span>
      </button>

      <button type="button" class="quick-action" @click="showImportDialog = true">
        <span class="qa-icon tone-green"><el-icon><Upload /></el-icon></span>
        <span class="qa-copy">
          <strong>导入文档</strong>
          <small>上传本地文件到知识库</small>
        </span>
        <span class="qa-arrow">→</span>
      </button>

      <button type="button" class="quick-action" @click="goToKnowledgeGraph">
        <span class="qa-icon tone-cyan"><el-icon><Share /></el-icon></span>
        <span class="qa-copy">
          <strong>知识图谱</strong>
          <small>可视化知识关联</small>
        </span>
        <span class="qa-arrow">→</span>
      </button>

      <button type="button" class="quick-action" @click="showCategoryDialog = true">
        <span class="qa-icon tone-amber"><el-icon><FolderAdd /></el-icon></span>
        <span class="qa-copy">
          <strong>新建分类</strong>
          <small>创建知识库分类</small>
        </span>
        <span class="qa-arrow">→</span>
      </button>
    </div>

    <div class="knowledge-layout">
          <!-- 左侧分类树 -->
          <aside class="kb-sidebar">
            <div class="sidebar-header">
              <div>
                <span class="sidebar-eyebrow">Library</span>
                <h2 class="sidebar-title">知识目录</h2>
              </div>
              <span class="sidebar-count">{{ totalCount }} 篇</span>
            </div>

            <div class="category-section">
              <h3 class="side-label">分类</h3>
              <div class="category-list">
                <div
                  class="category-item"
                  :class="{ active: selectedCategory === null }"
                  @click="selectedCategory = null"
                >
                  <el-icon><Folder /></el-icon>
                  <span class="category-name">全部文章</span>
                  <span class="count">{{ totalCount }}</span>
                </div>
                <div
                  v-for="category in categories"
                  :key="category.id"
                  class="category-item"
                  :class="{ active: selectedCategory === category.id }"
                  @click="selectedCategory = category.id"
                >
                  <el-icon><FolderOpened /></el-icon>
                  <span class="category-name">{{ category.name }}</span>
                  <span
                    class="delete-btn"
                    role="button"
                    tabindex="0"
                    aria-label="删除分类"
                    @click.stop="handleDeleteCategory(category.id)"
                  >
                    <el-icon><Delete /></el-icon>
                  </span>
                </div>
              </div>
            </div>

            <div class="tag-section">
              <h3 class="side-label">标签</h3>
              <div v-if="allTags.length" class="tag-list">
                <button
                  v-for="tag in allTags"
                  :key="tag"
                  type="button"
                  class="filter-tag"
                  :class="{ active: selectedTags.includes(tag) }"
                  @click="toggleTag(tag)"
                >
                  # {{ tag }}
                </button>
              </div>
              <p v-else class="side-empty">还没有自定义标签</p>
            </div>
          </aside>

          <!-- 右侧内容区 -->
          <div class="content-area">
            <!-- 搜索栏 -->
            <div class="kb-toolbar">
              <div class="toolbar-main">
                <el-input
                  v-model="searchKeyword"
                  placeholder="搜索标题、摘要或笔记..."
                  prefix-icon="Search"
                  clearable
                  @input="handleSearch"
                  class="search-input"
                />
                <el-select v-model="filterStatus" placeholder="阅读状态" clearable @change="loadItems" class="status-select">
                  <el-option label="未读" value="UNREAD" />
                  <el-option label="在读" value="READING" />
                  <el-option label="已读" value="READ" />
                </el-select>
              </div>
              <div class="toolbar-meta">
                <span class="result-summary">
                  {{ currentScopeLabel }}
                  <i></i>
                  {{ filteredItems.length }} 条
                </span>
                <button v-if="hasActiveFilters" type="button" class="clear-filters" @click="clearFilters">
                  清除筛选
                </button>
              </div>
            </div>

            <!-- 知识条目列表 -->
            <div class="knowledge-list" v-loading="loading">
              <article
                v-for="item in filteredItems"
                :key="item.id"
                class="knowledge-card"
              >
                <header class="card-head">
                  <div class="card-title-row">
                    <span class="status-pill" :class="getStatusClass(item.readStatus)">
                      {{ getStatusLabel(item.readStatus) }}
                    </span>
                    <h3 class="card-title" @click="goToDetail(item)">
                      {{ item.cachedTitle }}
                      <span class="title-arrow">→</span>
                    </h3>
                  </div>
                  <el-dropdown trigger="click">
                    <button type="button" class="card-menu" aria-label="条目操作">
                      <el-icon><More /></el-icon>
                    </button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click="handleUpdateStatus(item)">
                          标记为{{ getNextStatus(item.readStatus) }}
                        </el-dropdown-item>
                        <el-dropdown-item @click="handleEdit(item)">编辑</el-dropdown-item>
                        <el-dropdown-item divided @click="handleDelete(item)" class="danger">
                          删除
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </header>

                <p class="card-summary" v-if="item.cachedSummary">{{ item.cachedSummary }}</p>

                <div class="card-notes" v-if="item.notes">
                  <el-icon><EditPen /></el-icon>
                  <span>{{ item.notes }}</span>
                </div>

                <div class="card-tags" v-if="item.customTags?.length || item.originalDeleted">
                  <span v-for="tag in item.customTags" :key="tag" class="custom-tag"># {{ tag }}</span>
                  <span v-if="item.originalDeleted" class="deleted-tag">原文已删除</span>
                </div>

                <footer class="card-meta">
                  <div class="meta-left">
                    <img :src="item.cachedAuthorAvatar || defaultAvatar" class="author-avatar" />
                    <span class="author-name">{{ item.cachedAuthorName }}</span>
                  </div>
                  <div class="meta-right">
                    <span>{{ formatDate(item.createdAt) }} 收藏</span>
                  </div>
                </footer>
              </article>

              <div v-if="!loading && filteredItems.length === 0" class="kb-empty">
                <span class="empty-orb">
                  <el-icon><FolderOpened /></el-icon>
                </span>
                <h3>{{ hasActiveFilters ? '没有匹配的知识条目' : '知识库还是空的' }}</h3>
                <p>
                  {{ hasActiveFilters
                    ? '换一个关键词或清除筛选条件，再看看其他内容。'
                    : '导入文章或文档，把读过的内容沉淀成自己的知识网络。' }}
                </p>
                <div class="empty-actions">
                  <el-button v-if="!hasActiveFilters" type="primary" @click="showImportDialog = true">
                    导入第一篇文章
                  </el-button>
                  <el-button v-else @click="clearFilters">清除筛选</el-button>
                </div>
              </div>
            </div>

            <!-- 分页 -->
            <el-pagination
              v-if="totalPages > 1"
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="totalElements"
              layout="prev, pager, next"
              @current-change="loadItems"
              class="pagination"
            />
          </div>
        </div>

    <!-- 新建分类对话框 -->
    <el-dialog v-model="showCategoryDialog" title="新建分类" width="400px">
      <el-form :model="categoryForm" label-width="80px">
        <el-form-item label="分类名称">
          <el-input v-model="categoryForm.name" placeholder="输入分类名称" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="categoryForm.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCategoryDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateCategory" :loading="categoryLoading">
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑知识条目对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑知识条目" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="editForm.categoryId" clearable placeholder="选择分类">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="editForm.customTags"
            multiple
            filterable
            allow-create
            placeholder="选择或创建标签"
          >
            <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="笔记">
          <el-input
            v-model="editForm.notes"
            type="textarea"
            :rows="3"
            placeholder="添加个人笔记"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateItem" :loading="editLoading">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 导入文档对话框 -->
    <ImportDocumentDialog
      v-model="showImportDialog"
      @success="onImportSuccess"
    />

    <!-- RAG问答对话框 -->
    <el-dialog v-model="showRagDialog" title="知识库AI问答" width="800px" class="rag-dialog">
      <div class="rag-container">
        <!-- 文章选择区域 -->
        <div class="article-select-area" v-if="!ragMessages.length">
          <div class="select-header">
            <h4>选择参考文章</h4>
            <div class="select-actions">
              <el-button text size="small" @click="selectAllArticles">全选</el-button>
              <el-button text size="small" @click="clearSelectedArticles">清空</el-button>
            </div>
          </div>
          <div class="article-list" v-loading="loading">
            <el-checkbox-group v-model="selectedArticleIds">
              <div
                v-for="item in items"
                :key="item.id"
                class="article-select-item"
              >
                <el-checkbox :label="item.id">
                  <div class="article-info">
                    <span class="article-title">{{ item.cachedTitle }}</span>
                    <span class="article-meta" v-if="item.cachedSummary">{{ item.cachedSummary.substring(0, 60) }}...</span>
                  </div>
                </el-checkbox>
              </div>
            </el-checkbox-group>
            <el-empty v-if="!loading && items.length === 0" description="知识库为空，请先添加文章" />
          </div>
          <div class="selected-count" v-if="selectedArticleIds.length > 0">
            已选择 {{ selectedArticleIds.length }} 篇文章
          </div>
        </div>

        <!-- 问答历史 -->
        <div class="rag-history" ref="ragHistoryRef" v-else>
          <div v-for="(msg, index) in ragMessages" :key="index" class="rag-message" :class="msg.role">
            <div class="message-avatar">
              <el-icon v-if="msg.role === 'user'"><User /></el-icon>
              <el-icon v-else><ChatDotRound /></el-icon>
            </div>
            <div class="message-content">
              <p>{{ msg.content }}</p>
              <div v-if="msg.sources?.length" class="message-sources">
                <div class="sources-title">参考来源：</div>
                <div v-for="source in msg.sources" :key="source.id" class="source-item">
                  <el-icon><Document /></el-icon>
                  <span>{{ source.cachedTitle }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-if="ragLoading" class="rag-message assistant">
            <div class="message-avatar">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="message-content">
              <el-skeleton :rows="2" animated />
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="rag-input">
          <el-input
            v-model="ragQuestion"
            :placeholder="selectedArticleIds.length > 0 ? '输入问题，AI将基于选中的文章回答...' : '请先选择参考文章'"
            @keyup.enter="handleAsk"
            :disabled="selectedArticleIds.length === 0"
          >
            <template #append>
              <el-button @click="handleAsk" :loading="ragLoading" :disabled="selectedArticleIds.length === 0">
                <el-icon><Promotion /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </el-dialog>
  </InfoLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import InfoLayout from '@/components/content/InfoLayout.vue'
import { useAuthStore } from '@/store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Folder, FolderOpened, FolderAdd, Delete, More,
  ChatDotRound, EditPen, User, Document, Promotion, Upload, Share
} from '@element-plus/icons-vue'
import {
  getKnowledgeItems, searchKnowledgeItems, deleteKnowledgeItem,
  updateKnowledgeItem, updateReadStatus, getCategories, createCategory,
  deleteCategory, getAllTags, getKnowledgeCount, askKnowledge
} from '@/api/knowledge'
import ImportDocumentDialog from '@/components/ImportDocumentDialog.vue'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const items = ref([])
const categories = ref([])
const allTags = ref([])
const selectedCategory = ref(null)
const selectedTags = ref([])
const searchKeyword = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)
const totalCount = ref(0)

const showCategoryDialog = ref(false)
const categoryLoading = ref(false)
const categoryForm = reactive({
  name: '',
  color: '#409EFF'
})

const showEditDialog = ref(false)
const editLoading = ref(false)
const editForm = reactive({
  id: null,
  categoryId: null,
  notes: '',
  customTags: []
})

const showRagDialog = ref(false)
const ragLoading = ref(false)
const ragQuestion = ref('')
const ragMessages = ref([])
const ragHistoryRef = ref(null)
const selectedArticleIds = ref([])

const showImportDialog = ref(false)

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const heroStats = computed(() => [
  { value: totalCount.value, label: '收藏文章' },
  { value: categories.value.length, label: '分类' },
  { value: allTags.value.length, label: '标签' }
])

const currentScopeLabel = computed(() => {
  if (selectedCategory.value == null) return '全部文章'
  return categories.value.find((cat) => cat.id === selectedCategory.value)?.name || '当前分类'
})

const hasActiveFilters = computed(() =>
  Boolean(searchKeyword.value.trim() || filterStatus.value || selectedTags.value.length)
)

const clearFilters = () => {
  searchKeyword.value = ''
  filterStatus.value = ''
  selectedTags.value = []
  selectedCategory.value = null
  currentPage.value = 1
  loadItems()
}

const getStatusClass = (status) => {
  const map = { UNREAD: 'is-unread', READING: 'is-reading', READ: 'is-read' }
  return map[status] || 'is-unread'
}

// 过滤后的条目
const filteredItems = computed(() => {
  let result = items.value

  // 按标签筛选
  if (selectedTags.value.length > 0) {
    result = result.filter(item =>
      item.customTags?.some(tag => selectedTags.value.includes(tag))
    )
  }

  // 按阅读状态筛选
  if (filterStatus.value) {
    result = result.filter(item => item.readStatus === filterStatus.value)
  }

  return result
})

onMounted(() => {
  loadItems()
  loadCategories()
  loadTags()
  loadCount()
})

const loadItems = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    if (selectedCategory.value) {
      params.categoryId = selectedCategory.value
    }

    const response = await getKnowledgeItems(params)
    items.value = response.list || []
    totalElements.value = response.total
    totalPages.value = response.pages
  } catch (error) {
    console.error('加载知识库失败:', error)
    ElMessage.error('加载知识库失败')
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    categories.value = await getCategories()
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadTags = async () => {
  try {
    allTags.value = await getAllTags()
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

const loadCount = async () => {
  try {
    const result = await getKnowledgeCount()
    totalCount.value = result.count || 0
  } catch (error) {
    console.error('加载统计失败:', error)
  }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    loadItems()
    return
  }

  loading.value = true
  try {
    items.value = await searchKnowledgeItems(searchKeyword.value)
    totalElements.value = items.value.length
    totalPages.value = 1
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const handleCreateCategory = async () => {
  if (!categoryForm.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }

  categoryLoading.value = true
  try {
    await createCategory({
      name: categoryForm.name,
      color: categoryForm.color
    })
    ElMessage.success('分类创建成功')
    showCategoryDialog.value = false
    categoryForm.name = ''
    categoryForm.color = '#409EFF'
    loadCategories()
  } catch (error) {
    const message = error.response?.data?.message || '创建失败'
    ElMessage.error(message)
  } finally {
    categoryLoading.value = false
  }
}

const handleDeleteCategory = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个分类吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCategory(id)
    ElMessage.success('分类已删除')
    loadCategories()
    if (selectedCategory.value === id) {
      selectedCategory.value = null
      loadItems()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleEdit = (item) => {
  editForm.id = item.id
  editForm.categoryId = item.categoryId
  editForm.notes = item.notes || ''
  editForm.customTags = item.customTags || []
  showEditDialog.value = true
}

const handleUpdateItem = async () => {
  editLoading.value = true
  try {
    await updateKnowledgeItem(editForm.id, {
      categoryId: editForm.categoryId,
      notes: editForm.notes,
      customTags: editForm.customTags
    })
    ElMessage.success('更新成功')
    showEditDialog.value = false
    loadItems()
  } catch (error) {
    ElMessage.error('更新失败')
  } finally {
    editLoading.value = false
  }
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm('确定要从知识库中移除这篇文章吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteKnowledgeItem(item.id)
    ElMessage.success('已移除')
    loadItems()
    loadCount()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleUpdateStatus = async (item) => {
  const nextStatus = getNextStatus(item.readStatus)
  try {
    await updateReadStatus(item.id, nextStatus)
    item.readStatus = nextStatus
    ElMessage.success('状态已更新')
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const toggleTag = (tag) => {
  const index = selectedTags.value.indexOf(tag)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
  } else {
    selectedTags.value.push(tag)
  }
}

const goToDetail = (item) => {
  if (item.originalArticleId && !item.originalDeleted) {
    router.push(`/articles/${item.originalArticleId}`)
  }
}

const goToKnowledgeGraph = () => {
  router.push('/knowledge-graph')
}

const getStatusLabel = (status) => {
  const map = { UNREAD: '未读', READING: '在读', READ: '已读' }
  return map[status] || status
}

const getNextStatus = (status) => {
  const map = { UNREAD: 'READING', READING: 'READ', READ: 'UNREAD' }
  return map[status] || 'UNREAD'
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}

// RAG问答
const handleAsk = async () => {
  if (!ragQuestion.value.trim()) return
  if (selectedArticleIds.value.length === 0) {
    ElMessage.warning('请先选择参考文章')
    return
  }

  const question = ragQuestion.value.trim()
  ragMessages.value.push({ role: 'user', content: question })
  ragQuestion.value = ''
  ragLoading.value = true

  await nextTick()
  scrollToBottom()

  try {
    const response = await askKnowledge(question, selectedArticleIds.value)
    ragMessages.value.push({
      role: 'assistant',
      content: response.answer,
      sources: response.sources
    })
  } catch (error) {
    ragMessages.value.push({
      role: 'assistant',
      content: '抱歉，回答生成失败，请稍后重试。'
    })
  } finally {
    ragLoading.value = false
    await nextTick()
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  if (ragHistoryRef.value) {
    ragHistoryRef.value.scrollTop = ragHistoryRef.value.scrollHeight
  }
}

const selectAllArticles = () => {
  selectedArticleIds.value = items.value.map(item => item.id)
}

const clearSelectedArticles = () => {
  selectedArticleIds.value = []
}

const onImportSuccess = () => {
  loadItems()
  loadCount()
}
</script>

<style lang="scss" scoped>
/* ===== 快捷操作 ===== */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.quick-action {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
  color: inherit;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: transform var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);

  &:hover {
    transform: translateY(-3px);
    border-color: var(--content-line-strong);
    box-shadow: var(--content-shadow-lg);

    .qa-arrow {
      opacity: 1;
      transform: translateX(0);
    }
  }
}

.qa-icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  font-size: 20px;

  &.tone-violet {
    color: #8b5cf6;
    background: rgba(139, 92, 246, 0.12);
  }

  &.tone-green {
    color: #10b981;
    background: rgba(16, 185, 129, 0.12);
  }

  &.tone-cyan {
    color: #06b6d4;
    background: rgba(6, 182, 212, 0.12);
  }

  &.tone-amber {
    color: #f59e0b;
    background: rgba(245, 158, 11, 0.14);
  }
}

.qa-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;

  strong {
    color: var(--content-ink);
    font-size: 15px;
    font-weight: 600;
  }

  small {
    color: var(--content-ink-3);
    font-size: 12.5px;
    line-height: 1.45;
  }
}

.qa-arrow {
  margin-left: auto;
  color: var(--color-primary);
  font-size: 15px;
  opacity: 0;
  transform: translateX(-4px);
  transition: all var(--transition-fast);
}

/* ===== 主布局 ===== */
.knowledge-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.kb-sidebar {
  position: sticky;
  top: calc(var(--header-height) + 20px);
  padding: 20px;
  border-radius: 24px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
}

.sidebar-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--content-line);
}

.sidebar-eyebrow {
  display: block;
  margin-bottom: 5px;
  color: var(--content-ink-3);
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.sidebar-title {
  margin: 0;
  color: var(--content-ink);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.sidebar-count {
  flex-shrink: 0;
  padding: 4px 11px;
  border-radius: 999px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.category-section {
  margin-bottom: 24px;
}

.side-label {
  margin: 0 0 10px;
  color: var(--content-ink-3);
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  color: var(--content-ink-2);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast);

  .el-icon {
    flex-shrink: 0;
    color: var(--content-ink-3);
    font-size: 17px;
  }

  &:hover {
    background: var(--content-surface-tint);
    color: var(--content-ink);
  }

  &.active {
    color: #fff;
    background: linear-gradient(120deg, #1a73e8, #6f5cf6);
    box-shadow: 0 10px 22px rgba(47, 99, 235, 0.24);

    .el-icon {
      color: rgba(255, 255, 255, 0.9);
    }

    .count {
      color: #fff;
      background: rgba(255, 255, 255, 0.2);
    }

    .delete-btn {
      color: rgba(255, 255, 255, 0.8);
    }
  }

  .category-name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .count {
    flex-shrink: 0;
    padding: 2px 8px;
    border-radius: 999px;
    background: var(--content-chip);
    color: var(--content-ink-3);
    font-size: 11.5px;
    font-variant-numeric: tabular-nums;
  }

  .delete-btn {
    display: grid;
    place-items: center;
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    border-radius: 8px;
    color: var(--content-ink-3);
    opacity: 0;
    transition: all var(--transition-fast);

    &:hover {
      color: var(--color-error);
      background: rgba(234, 67, 53, 0.12);
    }
  }

  &:hover .delete-btn {
    opacity: 1;
  }
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.filter-tag {
  height: 28px;
  padding: 0 11px;
  border-radius: 999px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--content-ink-2);
  font-family: inherit;
  font-size: 12.5px;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    border-color: var(--content-line-strong);
    color: var(--color-primary);
  }

  &.active {
    color: #fff;
    border-color: transparent;
    background: linear-gradient(120deg, #1a73e8, #6f5cf6);
    box-shadow: 0 8px 16px rgba(47, 99, 235, 0.22);
  }
}

.side-empty {
  margin: 0;
  color: var(--content-ink-3);
  font-size: 12.5px;
}

/* ===== 内容区 ===== */
.content-area {
  min-width: 0;
}

.kb-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-soft);
  box-shadow: var(--content-shadow);
  backdrop-filter: blur(12px);
}

.toolbar-main {
  display: flex;
  flex: 1;
  min-width: 260px;
  align-items: center;
  gap: 10px;

  :deep(.search-input) {
    flex: 1;
    min-width: 0;
  }

  :deep(.status-select) {
    flex: 0 0 140px;
    width: 140px;
  }
}

.toolbar-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-summary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--content-ink-3);
  font-size: 12.5px;
  white-space: nowrap;

  i {
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--content-ink-3);
  }
}

.clear-filters {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--color-primary);
  font-family: inherit;
  font-size: 12.5px;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.knowledge-card {
  padding: 20px 22px;
  border-radius: 22px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
  transition: transform var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);

  &:hover {
    transform: translateY(-2px);
    border-color: var(--content-line-strong);
    box-shadow: var(--content-shadow-lg);
  }
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.status-pill {
  flex-shrink: 0;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.02em;

  &.is-unread {
    color: var(--content-ink-3);
    background: var(--content-chip);
    border: 1px solid var(--content-chip-line);
  }

  &.is-reading {
    color: #b45309;
    background: rgba(245, 158, 11, 0.14);
  }

  &.is-read {
    color: #15803d;
    background: rgba(52, 168, 83, 0.13);
  }
}

:global([data-theme='dark']) .status-pill {
  &.is-reading {
    color: #fcd34d;
  }

  &.is-read {
    color: #86efac;
  }
}

.card-title {
  min-width: 0;
  margin: 0;
  color: var(--content-ink);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  transition: color var(--transition-fast);

  .title-arrow {
    margin-left: 4px;
    color: var(--color-primary);
    font-size: 14px;
    opacity: 0;
    transition: opacity var(--transition-fast);
  }

  &:hover {
    color: var(--color-primary);

    .title-arrow {
      opacity: 1;
    }
  }
}

.card-menu {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--content-ink-3);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    color: var(--content-ink);
    background: var(--content-surface-tint);
    border-color: var(--content-chip-line);
  }
}

.card-summary {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0 0 12px;
  color: var(--content-ink-2);
  font-size: 14px;
  line-height: 1.7;
}

.card-notes {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border-left: 3px solid var(--content-line-strong);
  border-radius: 4px 12px 12px 4px;
  background: var(--content-surface-tint);
  color: var(--content-ink-2);
  font-size: 13px;
  line-height: 1.6;

  .el-icon {
    flex-shrink: 0;
    margin-top: 3px;
    color: var(--color-primary);
  }
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.custom-tag {
  padding: 3px 9px;
  border-radius: 999px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--color-primary);
  font-size: 11.5px;
}

.deleted-tag {
  padding: 3px 9px;
  border-radius: 999px;
  background: rgba(234, 67, 53, 0.1);
  color: var(--color-error);
  font-size: 11.5px;
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--content-line);
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.author-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--content-line);
}

.author-name {
  overflow: hidden;
  color: var(--content-ink-3);
  font-size: 12.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-right {
  flex-shrink: 0;
  color: var(--content-ink-3);
  font-size: 12px;
}

/* ===== 空状态 ===== */
.kb-empty {
  padding: 64px 24px;
  border-radius: 24px;
  border: 1px dashed var(--content-line-strong);
  background: var(--content-surface-soft);
  text-align: center;

  .empty-orb {
    display: grid;
    place-items: center;
    width: 58px;
    height: 58px;
    margin: 0 auto 16px;
    border-radius: 18px;
    color: #fff;
    font-size: 26px;
    background: linear-gradient(135deg, #1a73e8, #8b5cf6);
    box-shadow: 0 16px 32px rgba(70, 90, 220, 0.25);
  }

  h3 {
    margin: 0 0 8px;
    color: var(--content-ink);
    font-size: 18px;
    font-weight: 600;
  }

  p {
    max-width: 380px;
    margin: 0 auto;
    color: var(--content-ink-2);
    font-size: 14px;
    line-height: 1.7;
  }

  .empty-actions {
    margin-top: 20px;
  }
}

.pagination {
  margin-top: 24px;
  justify-content: center;
}

/* ===== RAG 问答 ===== */
.rag-container {
  display: flex;
  flex-direction: column;
  height: min(560px, 68vh);
  gap: 14px;
}

.article-select-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .select-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--content-line);

    h4 {
      margin: 0;
      color: var(--content-ink);
      font-size: 15px;
      font-weight: 600;
    }

    .select-actions {
      display: flex;
      gap: 8px;
    }
  }

  .article-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
    border-radius: 16px;
    border: 1px solid var(--content-line);
    background: var(--content-surface-tint);

    .article-select-item {
      padding: 8px;
      border-radius: 12px;
      transition: background var(--transition-fast);

      &:hover {
        background: var(--content-surface);
      }

      .el-checkbox {
        width: 100%;
        height: auto;
        align-items: flex-start;

        .el-checkbox__label {
          flex: 1;
          padding-left: 8px;
        }
      }

      .article-info {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .article-title {
          color: var(--content-ink);
          font-size: 14px;
          font-weight: 500;
        }

        .article-meta {
          display: -webkit-box;
          -webkit-line-clamp: 1;
          -webkit-box-orient: vertical;
          overflow: hidden;
          color: var(--content-ink-3);
          font-size: 12px;
        }
      }
    }
  }

  .selected-count {
    margin-top: 12px;
    padding: 8px 12px;
    border-radius: 12px;
    background: var(--content-chip);
    color: var(--color-primary);
    font-size: 12.5px;
    text-align: center;
  }
}

.rag-history {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);
}

.rag-message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;

  &.user {
    flex-direction: row-reverse;

    .message-content {
      color: #fff;
      background: linear-gradient(120deg, #1a73e8, #6f5cf6);
      box-shadow: 0 10px 24px rgba(47, 99, 235, 0.2);
    }

    .message-avatar {
      color: #fff;
      background: linear-gradient(135deg, #1a73e8, #8b5cf6);
    }
  }

  &.assistant {
    .message-content {
      border: 1px solid var(--content-line);
      background: var(--content-surface);
    }

    .message-avatar {
      color: var(--color-primary);
      background: var(--content-chip);
      border: 1px solid var(--content-chip-line);
    }
  }
}

.message-avatar {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 12px;
}

.message-content {
  max-width: 82%;
  padding: 12px 15px;
  border-radius: 14px;
  color: var(--content-ink);
  font-size: 14px;
  line-height: 1.7;

  p {
    margin: 0;
  }
}

.message-sources {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--content-line);

  .sources-title {
    margin-bottom: 6px;
    color: var(--content-ink-3);
    font-size: 11.5px;
    font-weight: 600;
  }

  .source-item {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 4px;
    color: var(--content-ink-2);
    font-size: 12px;
  }
}

.rag-input {
  padding-top: 12px;
  border-top: 1px solid var(--content-line);

  :deep(.el-input__wrapper) {
    border-radius: 12px 0 0 12px;
  }

  :deep(.el-input-group__append) {
    border-radius: 0 12px 12px 0;
    overflow: hidden;
  }
}

.danger {
  color: var(--color-error);
}

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .quick-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .knowledge-layout {
    grid-template-columns: 230px minmax(0, 1fr);
  }
}

@media (max-width: 1024px) {
  .knowledge-layout {
    grid-template-columns: 1fr;
  }

  .kb-sidebar {
    position: static;
  }

  .category-section {
    margin-bottom: 18px;
  }

  .category-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }
}

@media (max-width: 768px) {
  .quick-action {
    padding: 15px;
  }

  .qa-icon {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }

  .qa-arrow {
    display: none;
  }

  .kb-toolbar {
    padding: 12px;
  }

  .toolbar-main {
    min-width: 0;
    flex-wrap: wrap;

    :deep(.search-input) {
      flex-basis: 100%;
    }

    :deep(.status-select) {
      flex: 1;
      width: 100%;
    }
  }

  .knowledge-card {
    padding: 18px;
  }

  .card-title {
    font-size: 16.5px;
    white-space: normal;
  }

  .card-title-row {
    align-items: flex-start;
  }

  .status-pill {
    margin-top: 3px;
  }
}

@media (max-width: 520px) {
  .quick-actions {
    grid-template-columns: 1fr;
  }

  .qa-copy small {
    font-size: 12px;
  }
}
</style>
