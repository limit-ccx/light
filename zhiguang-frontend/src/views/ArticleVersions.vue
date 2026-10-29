<template>
  <div class="versions-page">
    <Header />

    <main class="main-content" v-loading="loading">
      <div class="container">
        <div class="page-header">
          <div class="page-header-main">
            <h1 class="page-title">历史版本</h1>
            <p class="page-subtitle">查看、对比并恢复文章的早期版本</p>
          </div>
          <router-link :to="`/articles/${articleId}`" class="btn-secondary btn-sm">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"/>
              <polyline points="12 19 5 12 12 5"/>
            </svg>
            返回文章
          </router-link>
        </div>

        <div v-if="!loading && versions.length === 0" class="versions-card empty-state">
          <p>暂无历史版本</p>
          <router-link :to="`/articles/${articleId}`" class="btn-primary">返回文章详情</router-link>
        </div>

        <div v-else class="versions-layout">
          <section class="versions-list-column">
            <div class="versions-card list-header">
              <div class="list-header-main">
                <h3 class="section-title">版本列表</h3>
                <span v-if="selectedIds.length > 0" class="selection-hint">
                  已选择 {{ selectedIds.length }}/2 个版本
                </span>
              </div>
              <button
                v-if="selectedIds.length === 2"
                class="btn-primary btn-sm"
                @click="compareSelected"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="17 1 21 5 17 9"/>
                  <path d="M3 11V9a4 4 0 0 1 4-4h14"/>
                  <polyline points="7 23 3 19 7 15"/>
                  <path d="M21 13v2a4 4 0 0 1-4 4H3"/>
                </svg>
                对比选中版本
              </button>
              <button
                v-else-if="selectedIds.length > 0"
                class="btn-text btn-sm"
                @click="clearSelection"
              >
                清除选择
              </button>
            </div>

            <div class="version-list">
              <div
                v-for="v in versions"
                :key="v.id"
                class="version-item"
                :class="{ active: selectedIds.includes(v.id) }"
              >
                <label class="version-check">
                  <input
                    type="checkbox"
                    :checked="selectedIds.includes(v.id)"
                    :disabled="!selectedIds.includes(v.id) && selectedIds.length >= 2"
                    @change="toggleSelection(v.id)"
                  />
                </label>

                <div class="version-badge">v{{ v.version }}</div>

                <div class="version-meta" @click="viewVersion(v)">
                  <div class="version-title">{{ v.title || '无标题' }}</div>
                  <div class="version-sub">{{ formatDateTime(v.createdAt) }}</div>
                </div>

                <div class="version-actions">
                  <button class="btn-text btn-sm" @click="viewVersion(v)">查看</button>
                  <button class="btn-danger btn-sm" :disabled="restoringId === v.id" @click="handleRestore(v)">
                    <svg v-if="restoringId === v.id" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
                      <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                    </svg>
                    <span v-else>恢复</span>
                  </button>
                </div>
              </div>
            </div>
          </section>

          <aside class="versions-detail-column">
            <div v-if="mode === 'empty'" class="versions-card detail-empty">
              <div class="empty-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 16 14"/>
                </svg>
              </div>
              <p>点击左侧「查看」浏览版本详情</p>
              <p class="empty-tip">选择两个版本后可进行 Diff 对比</p>
            </div>

            <div v-else-if="mode === 'detail' && selectedVersion" class="versions-card detail-panel">
              <div class="detail-header">
                <div>
                  <span class="detail-badge">v{{ selectedVersion.version }}</span>
                  <span class="detail-time">{{ formatDateTime(selectedVersion.createdAt) }}</span>
                </div>
                <button class="btn-text btn-sm" @click="mode = 'empty'">关闭</button>
              </div>
              <h2 class="detail-title">{{ selectedVersion.title || '无标题' }}</h2>
              <div class="detail-content" v-html="sanitizedVersionContent"></div>
            </div>

            <div v-else-if="mode === 'diff' && diffVersions.length === 2" class="versions-card diff-panel">
              <div class="diff-header">
                <div class="diff-title">
                  <span class="diff-badge del">v{{ diffVersions[0].version }}</span>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="5" y1="12" x2="19" y2="12"/>
                    <polyline points="12 5 19 12 12 19"/>
                  </svg>
                  <span class="diff-badge add">v{{ diffVersions[1].version }}</span>
                </div>
                <button class="btn-text btn-sm" @click="mode = 'empty'">关闭对比</button>
              </div>

              <div class="diff-section">
                <div class="diff-section-title">标题</div>
                <div class="diff-body">
                  <template v-if="titleDiff.length">
                    <span
                      v-for="(seg, idx) in titleDiff"
                      :key="idx"
                      :class="['diff-segment', `diff-${seg.type}`]"
                      v-html="seg.text"
                    />
                  </template>
                  <span v-else class="diff-same">标题无变化</span>
                </div>
              </div>

              <div class="diff-section">
                <div class="diff-section-title">正文</div>
                <div class="diff-body">
                  <template v-if="contentDiff.length">
                    <span
                      v-for="(seg, idx) in contentDiff"
                      :key="idx"
                      :class="['diff-segment', `diff-${seg.type}`]"
                      v-html="seg.text"
                    />
                  </template>
                  <span v-else class="diff-same">正文无变化</span>
                </div>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DOMPurify from 'dompurify'
import { useAuthStore } from '@/store/auth'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { getArticleVersions, getArticleVersion, restoreArticleVersion } from '@/api/article'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const articleId = route.params.id
const article = ref(null)
const versions = ref([])
const loading = ref(true)
const restoringId = ref(null)

const selectedVersion = ref(null)
const selectedIds = ref([])
const mode = ref('empty') // 'empty' | 'detail' | 'diff'

const diffVersions = ref([])

// 版本正文来自用户内容，v-html 渲染前必须净化
const sanitizedVersionContent = computed(() =>
  DOMPurify.sanitize(selectedVersion.value?.content || '<p>无内容</p>')
)

onMounted(async () => {
  if (!authStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }

  try {
    const data = await request({
      url: `/api/articles/${articleId}`,
      method: 'get'
    })
    article.value = data

    const currentUserId = String(authStore.user?.id)
    const authorId = String(data.author?.id)
    if (currentUserId !== authorId) {
      ElMessage.warning('只有文章作者才能查看历史版本')
      router.push(`/articles/${articleId}`)
      return
    }
  } catch (error) {
    ElMessage.error('加载文章信息失败')
    router.push(`/articles/${articleId}`)
    return
  }

  await loadVersions()
  loading.value = false
})

const loadVersions = async () => {
  try {
    const list = await getArticleVersions(articleId)
    versions.value = Array.isArray(list) ? list : []
  } catch (error) {
    ElMessage.error('加载版本列表失败')
  }
}

const formatDateTime = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const toggleSelection = (id) => {
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) {
    selectedIds.value.splice(idx, 1)
  } else if (selectedIds.value.length < 2) {
    selectedIds.value.push(id)
  }
}

const clearSelection = () => {
  selectedIds.value = []
}

const viewVersion = async (v) => {
  try {
    const detail = await getArticleVersion(articleId, v.id)
    selectedVersion.value = detail
    mode.value = 'detail'
  } catch (error) {
    ElMessage.error('加载版本详情失败')
  }
}

const compareSelected = () => {
  const selected = versions.value.filter(v => selectedIds.value.includes(v.id))
  if (selected.length !== 2) return

  // 按版本号升序，旧版本在前
  selected.sort((a, b) => a.version - b.version)
  diffVersions.value = selected
  mode.value = 'diff'
}

const handleRestore = async (v) => {
  try {
    await ElMessageBox.confirm(
      `确定要恢复到 v${v.version} 吗？当前内容会先保存为一个新版本。`,
      '恢复版本',
      {
        confirmButtonText: '恢复',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  restoringId.value = v.id
  try {
    await restoreArticleVersion(articleId, v.id)
    ElMessage.success(`已恢复到 v${v.version}`)
    await loadVersions()
    // 刷新文章信息以反映最新内容
    const data = await request({ url: `/api/articles/${articleId}`, method: 'get' })
    article.value = data
    mode.value = 'empty'
    selectedVersion.value = null
  } catch (error) {
    ElMessage.error('恢复版本失败')
  } finally {
    restoringId.value = null
  }
}

// ===== 纯 JS Diff =====

const escapeHtml = (str) => {
  if (str == null) return ''
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

const tokenize = (text) => {
  return String(text || '')
    .split(/(\s+|[\p{P}\p{S}])/u)
    .filter(t => t.length > 0)
}

const computeDiff = (oldText, newText) => {
  const a = tokenize(oldText)
  const b = tokenize(newText)
  const m = a.length
  const n = b.length

  if (m === 0 && n === 0) return []
  if (m === 0) return [{ type: 'add', text: escapeHtml(b.join('')) }]
  if (n === 0) return [{ type: 'del', text: escapeHtml(a.join('')) }]

  const dp = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0))
  for (let i = m - 1; i >= 0; i--) {
    for (let j = n - 1; j >= 0; j--) {
      if (a[i] === b[j]) {
        dp[i][j] = dp[i + 1][j + 1] + 1
      } else {
        dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1])
      }
    }
  }

  const segments = []
  let i = 0
  let j = 0

  const pushSegment = (type, tokens) => {
    if (!tokens.length) return
    const text = escapeHtml(tokens.join(''))
    if (segments.length && segments[segments.length - 1].type === type) {
      segments[segments.length - 1].text += text
    } else {
      segments.push({ type, text })
    }
  }

  while (i < m || j < n) {
    if (i < m && j < n && a[i] === b[j]) {
      const same = []
      while (i < m && j < n && a[i] === b[j]) {
        same.push(a[i])
        i++
        j++
      }
      pushSegment('same', same)
    } else if (j < n && (i >= m || dp[i][j + 1] >= dp[i + 1][j])) {
      const added = []
      while (j < n && (i >= m || dp[i][j + 1] >= dp[i + 1][j]) && !(i < m && j < n && a[i] === b[j])) {
        added.push(b[j])
        j++
      }
      pushSegment('add', added)
    } else if (i < m) {
      const removed = []
      while (i < m && (j >= n || dp[i + 1][j] > dp[i][j + 1]) && !(i < m && j < n && a[i] === b[j])) {
        removed.push(a[i])
        i++
      }
      pushSegment('del', removed)
    } else {
      break
    }
  }

  return segments
}

const titleDiff = computed(() => {
  if (diffVersions.value.length !== 2) return []
  return computeDiff(diffVersions.value[0].title, diffVersions.value[1].title)
})

const contentDiff = computed(() => {
  if (diffVersions.value.length !== 2) return []
  return computeDiff(diffVersions.value[0].content, diffVersions.value[1].content)
})
</script>

<style lang="scss" scoped>
.versions-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
}

.main-content {
  flex: 1;
  padding: calc(var(--header-height) + 32px) 0 60px;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 32px;
  animation: fadeInUp 0.5s ease;
}

.page-title {
  font-size: 28px;
  font-weight: 500;
  color: var(--color-text);
  letter-spacing: -0.02em;
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-muted);
  font-weight: 400;
}

.versions-layout {
  display: grid;
  grid-template-columns: 1fr 420px;
  gap: 32px;
  align-items: start;
}

.versions-card {
  @include card-static;
  padding: 24px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-card-border);
  animation: fadeInUp 0.6s ease 0.1s both;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 18px 20px;
}

.list-header-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text);
}

.selection-hint {
  font-size: 12px;
  color: var(--color-text-muted);
  padding: 3px 10px;
  background: var(--color-bg);
  border-radius: var(--radius-full);
}

.version-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.version-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  transition: all var(--transition-fast);
  cursor: pointer;

  &:hover {
    border-color: var(--color-border-hover);
    box-shadow: var(--shadow-sm);
  }

  &.active {
    border-color: var(--color-primary);
    background: var(--color-google-blue-light);
  }
}

.version-check {
  display: flex;
  align-items: center;
  justify-content: center;

  input[type="checkbox"] {
    width: 18px;
    height: 18px;
    accent-color: var(--color-primary);
    cursor: pointer;
  }
}

.version-badge {
  width: 44px;
  height: 44px;
  @include flex-center;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  background: var(--color-google-blue-light);
  color: var(--color-primary);
  font-weight: 600;
  font-size: 13px;
}

.version-meta {
  flex: 1;
  min-width: 0;
}

.version-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text);
  line-height: 1.4;
  margin-bottom: 4px;
  @include text-ellipsis(1);
}

.version-sub {
  font-size: 12.5px;
  color: var(--color-text-muted);
}

.version-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.versions-detail-column {
  position: sticky;
  top: calc(var(--header-height) + 28px);
}

.detail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 64px 24px;
  color: var(--color-text-muted);
  text-align: center;

  .empty-icon {
    width: 72px;
    height: 72px;
    @include flex-center;
    border-radius: var(--radius-full);
    background: var(--color-bg);
    color: var(--color-text-muted);
  }

  p {
    font-size: 14px;
  }

  .empty-tip {
    font-size: 12.5px;
    opacity: 0.8;
  }
}

.detail-panel,
.diff-panel {
  animation: fadeInUp 0.5s ease;
}

.detail-header,
.diff-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-divider);
}

.detail-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  background: var(--color-google-blue-light);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  margin-right: 10px;
}

.detail-time {
  font-size: 13px;
  color: var(--color-text-muted);
}

.detail-title {
  font-size: 22px;
  font-weight: 500;
  color: var(--color-text);
  line-height: 1.4;
  margin-bottom: 20px;
}

.detail-content {
  font-size: 15px;
  line-height: 1.85;
  color: var(--color-text-secondary);
  word-break: break-word;

  :deep(p) {
    margin-bottom: 16px;
  }
}

.diff-title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-text-secondary);
}

.diff-badge {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;

  &.add {
    background: var(--color-google-green-light);
    color: var(--color-google-green);
  }

  &.del {
    background: var(--color-google-red-light);
    color: var(--color-google-red);
  }
}

.diff-section {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
}

.diff-section-title {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: 10px;
}

.diff-body {
  padding: 16px;
  border-radius: var(--radius-md);
  background: var(--color-bg);
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.diff-same {
  color: var(--color-text-muted);
  font-style: italic;
}

.diff-segment {
  border-radius: 3px;
  padding: 1px 2px;

  &.diff-add {
    background: var(--color-google-green-light);
    color: var(--color-google-green);
  }

  &.diff-del {
    background: var(--color-google-red-light);
    color: var(--color-google-red);
    text-decoration: line-through;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 64px 24px;
  color: var(--color-text-muted);
  text-align: center;
}

.btn-sm {
  height: 32px;
  padding: 0 14px;
  font-size: 13px;
}

.btn-text.btn-sm {
  height: 32px;
  padding: 0 10px;
}

.btn-danger.btn-sm {
  height: 32px;
  padding: 0 14px;
  font-size: 13px;
}

.spin {
  animation: spin 0.8s linear infinite;
}

@media (max-width: 1024px) {
  .versions-layout {
    grid-template-columns: 1fr;
  }

  .versions-detail-column {
    position: static;
  }
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .version-item {
    flex-wrap: wrap;
  }

  .version-actions {
    width: 100%;
    justify-content: flex-end;
    margin-top: 8px;
  }
}
</style>
