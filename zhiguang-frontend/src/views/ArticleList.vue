<template>
  <div class="article-list-page content-page">
    <Header />

    <main class="page-main">
      <div class="container">
        <ContentHero
          eyebrow="READ · 深度阅读"
          title="精选文章"
          description="把零散的想法，读成完整的知识。这里收录社区里值得慢慢读完的长文与笔记。"
          active="articles"
          :stats="heroStats"
        />

        <section class="toolbar">
          <div class="tag-row">
            <button
              type="button"
              class="tag-chip"
              :class="{ 'is-active': !currentTag }"
              @click="handleTagChange(null)"
            >
              全部
            </button>
            <button
              v-for="tag in tags"
              :key="tag"
              type="button"
              class="tag-chip"
              :class="{ 'is-active': currentTag === tag }"
              @click="handleTagChange(tag)"
            >
              {{ tag }}
            </button>
          </div>

          <div class="toolbar-side">
            <div class="sort-seg" role="group" aria-label="排序">
              <button
                v-for="option in sortOptions"
                :key="option.value"
                type="button"
                :class="{ 'is-active': sortBy === option.value }"
                @click="changeSort(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
            <span class="result-count">{{ loading ? '更新中…' : `${total} 篇` }}</span>
          </div>
        </section>

        <div v-if="initialLoading" class="page-skeleton">
          <div class="sk-hero">
            <span class="sk sk-block"></span>
            <div class="sk-hero-lines">
              <span class="sk sk-line" style="width: 30%"></span>
              <span class="sk sk-line" style="width: 70%; height: 22px"></span>
              <span class="sk sk-line" style="width: 90%"></span>
              <span class="sk sk-line" style="width: 60%"></span>
            </div>
          </div>
          <div class="sk-grid">
            <div v-for="i in 4" :key="i" class="sk-card">
              <span class="sk sk-line" style="width: 38%"></span>
              <span class="sk sk-line" style="width: 78%; height: 20px"></span>
              <span class="sk sk-line" style="width: 100%"></span>
              <span class="sk sk-line" style="width: 86%"></span>
            </div>
          </div>
        </div>

        <template v-else>
          <div class="list-body" :class="{ 'is-refreshing': loading }">
          <article v-if="featured" class="feature-card" @click="goDetail(featured)">
            <div class="feature-visual" :class="{ 'has-cover': !!featured.cover }">
              <img :src="featured.cover" alt="" @error="hideBrokenImage" />
              <span class="feature-badge">本周精选</span>
              <span class="feature-mark">EDITOR'S PICK</span>
            </div>

            <div class="feature-body">
              <div class="feature-tags">
                <span v-for="tag in (featured.tags || []).slice(0, 3)" :key="tag" class="mini-tag">{{ tag }}</span>
              </div>
              <h2 class="feature-title">{{ featured.title }}</h2>
              <p class="feature-summary">{{ featured.summary || '作者还没有写摘要，点开读正文吧。' }}</p>

              <div class="feature-foot">
                <div class="feature-author">
                  <img :src="featured.author?.avatar || defaultAvatar" alt="" />
                  <div>
                    <b>{{ featured.author?.username || '匿名作者' }}</b>
                    <time>{{ formatDate(featured.createdAt) }}</time>
                  </div>
                </div>

                <div class="feature-stats">
                  <span>{{ featured.viewCount || 0 }} 阅读</span>
                  <span>{{ featured.likeCount || 0 }} 喜欢</span>
                </div>

                <span class="feature-cta">阅读全文 →</span>
              </div>
            </div>
          </article>

          <div v-if="restArticles.length" class="article-grid">
            <ArticleCard
              v-for="article in restArticles"
              :key="article.id"
              :article="article"
              @like="handleLike"
              @favorite="handleFavorite"
            />
          </div>

          <div v-if="articles.length === 0" class="empty-state">
            <div class="empty-art">
              <span></span><span></span><span></span>
            </div>
            <h3>此处留白，静候佳作</h3>
            <p>换个标签看看，或去写下你的第一篇文章。</p>
            <router-link to="/create" class="empty-action">开始写作 →</router-link>
          </div>

          <div v-if="totalPages > 1" class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              layout="prev, pager, next"
              background
              @current-change="handlePageChange"
            />
          </div>
          </div>
        </template>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import ArticleCard from '@/components/ArticleCard.vue'
import ContentHero from '@/components/content/ContentHero.vue'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const tags = ['前端', '后端', 'JavaScript', 'Vue', 'React', 'Python', '架构', 'DevOps']
const sortOptions = [
  { value: 'createdAt', label: '最新' },
  { value: 'viewCount', label: '浏览' },
  { value: 'likeCount', label: '喜欢' }
]

const loading = ref(false)
const hasLoadedOnce = ref(false)
const articles = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const currentTag = ref(null)
const sortBy = ref('createdAt')
const activeRequests = ref(new Set())

const initialLoading = computed(() => loading.value && !hasLoadedOnce.value)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const showFeatured = computed(
  () => currentPage.value === 1 && !currentTag.value && !route.query.search && articles.value.length > 0
)
const featured = computed(() => (showFeatured.value ? articles.value[0] : null))
const restArticles = computed(() => (showFeatured.value ? articles.value.slice(1) : articles.value))

const heroStats = computed(() => [
  { value: total.value || articles.value.length, label: '篇文章' },
  { value: tags.length, label: '个主题' },
  { value: '∞', label: '持续更新' }
])

const formatDate = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

const hideBrokenImage = (event) => {
  event.target.style.display = 'none'
}

const goDetail = (article) => {
  router.push(`/articles/${article.id}`)
}

onMounted(() => {
  if (route.query.tag) currentTag.value = route.query.tag
  loadArticles()
})

watch(
  () => route.query,
  (query) => {
    if (query.tag) {
      currentTag.value = query.tag
    } else {
      currentTag.value = null
    }
    currentPage.value = 1
    loadArticles()
  },
  { deep: true }
)

let requestSeq = 0

const loadArticles = async () => {
  const seq = ++requestSeq
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      sortBy: sortBy.value
    }
    if (route.query.search) params.search = route.query.search
    else if (currentTag.value) params.tag = currentTag.value

    const response = await request({ url: '/api/articles', method: 'get', params })
    if (seq !== requestSeq) return
    articles.value = Array.isArray(response?.list) ? response.list : []
    total.value = typeof response?.total === 'number' ? response.total : articles.value.length
  } catch (error) {
    if (seq !== requestSeq) return
    if (!hasLoadedOnce.value) {
      articles.value = []
      total.value = 0
    }
  } finally {
    if (seq === requestSeq) {
      loading.value = false
      hasLoadedOnce.value = true
    }
  }
}

const changeSort = (value) => {
  if (sortBy.value === value) return
  sortBy.value = value
  currentPage.value = 1
  loadArticles()
}

const handleTagChange = (tag) => {
  if (currentTag.value === tag) return
  currentTag.value = tag
  currentPage.value = 1
  router.push({ query: tag ? { tag } : {} })
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleLike = async (id) => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (activeRequests.value.has(`like-${id}`)) return
  activeRequests.value.add(`like-${id}`)
  try {
    const data = await request({ url: `/api/articles/${id}/like`, method: 'post' })
    const article = articles.value.find((a) => a.id === id)
    if (article) {
      article.isLiked = data.isLiked
      article.likeCount = data.likeCount
    }
  } catch (error) {
    // 拦截器已提示
  } finally {
    setTimeout(() => activeRequests.value.delete(`like-${id}`), 300)
  }
}

const handleFavorite = async (id) => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (activeRequests.value.has(`fav-${id}`)) return
  activeRequests.value.add(`fav-${id}`)
  try {
    const data = await request({ url: `/api/articles/${id}/favorite`, method: 'post' })
    const article = articles.value.find((a) => a.id === id)
    if (article) {
      article.isFavorited = data.isFavorited
      article.favoriteCount = data.favoriteCount
    }
  } catch (error) {
    // 拦截器已提示
  } finally {
    setTimeout(() => activeRequests.value.delete(`fav-${id}`), 300)
  }
}
</script>

<style lang="scss" scoped>
.content-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(70% 40% at 85% 0%, var(--content-glow-b), transparent 60%),
    radial-gradient(60% 40% at 10% 12%, var(--content-glow-a), transparent 60%),
    var(--color-bg);
}

.page-main {
  flex: 1;
  padding: calc(var(--header-height) + 28px) 0 56px;
}

/* ===== 筛选栏 ===== */
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin: 22px 0 20px;
  padding: 12px 16px;
  border-radius: 18px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-soft);
  backdrop-filter: blur(12px);
  box-shadow: var(--content-shadow);
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.tag-chip {
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--content-ink-2);
  font-family: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    color: var(--color-primary);
    border-color: var(--content-line-strong);
  }

  &.is-active {
    color: #fff;
    border-color: transparent;
    background: linear-gradient(120deg, #1a73e8, #6f5cf6);
    box-shadow: 0 8px 18px rgba(47, 99, 235, 0.24);
  }
}

.toolbar-side {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sort-seg {
  display: inline-flex;
  padding: 3px;
  border-radius: 999px;
  background: var(--content-surface-tint);

  button {
    height: 28px;
    padding: 0 14px;
    border: none;
    border-radius: 999px;
    background: transparent;
    color: var(--content-ink-2);
    font-family: inherit;
    font-size: 12.5px;
    cursor: pointer;
    transition: all var(--transition-fast);

    &.is-active {
      color: var(--color-primary);
      background: var(--content-surface);
      box-shadow: var(--content-shadow);
    }
  }
}

.result-count {
  color: var(--content-ink-3);
  font-size: 12.5px;
  white-space: nowrap;
}

/* ===== 精选大卡 ===== */
.feature-card {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(0, 1fr);
  overflow: hidden;
  margin-bottom: 20px;
  border-radius: 26px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;

  &:hover {
    transform: translateY(-3px);
    border-color: var(--content-line-strong);
    box-shadow: var(--content-shadow-lg);

    .feature-cta {
      gap: 8px;
      color: var(--color-primary);
    }
  }
}

.feature-visual {
  position: relative;
  min-height: 300px;
  background:
    radial-gradient(120% 100% at 20% 0%, rgba(66, 133, 244, 0.5), transparent 60%),
    radial-gradient(120% 100% at 100% 100%, rgba(168, 85, 247, 0.55), transparent 58%),
    linear-gradient(135deg, #1b2a52 0%, #2c1f5e 55%, #3a1d44 100%);

  img {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, rgba(10, 16, 32, 0.05) 0%, rgba(10, 16, 32, 0.55) 100%);
  }
}

.feature-badge {
  position: absolute;
  top: 18px;
  left: 18px;
  z-index: 1;
  padding: 6px 13px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #1a73e8;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  box-shadow: 0 6px 16px rgba(6, 12, 30, 0.18);
}

.feature-mark {
  position: absolute;
  left: 20px;
  bottom: 18px;
  z-index: 1;
  color: rgba(255, 255, 255, 0.8);
  font-size: 10px;
  letter-spacing: 0.32em;
}

.feature-body {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
  padding: clamp(22px, 3vw, 34px);
}

.feature-tags {
  display: flex;
  gap: 7px;
}

.mini-tag {
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--color-google-blue-light);
  color: var(--color-primary);
  font-size: 11.5px;
}

.feature-title {
  margin: 0;
  font-size: clamp(21px, 2.4vw, 28px);
  font-weight: 700;
  line-height: 1.35;
  color: var(--content-ink);
  letter-spacing: -0.01em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.feature-summary {
  margin: 0;
  color: var(--content-ink-2);
  font-size: 14px;
  line-height: 1.8;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.feature-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  margin-top: 6px;
  padding-top: 16px;
  border-top: 1px solid var(--color-divider);
}

.feature-author {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-right: auto;

  img {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    object-fit: cover;
    border: 1px solid var(--color-divider);
  }

  b {
    display: block;
    font-size: 13px;
    font-weight: 500;
    color: var(--content-ink);
  }

  time {
    display: block;
    margin-top: 1px;
    color: var(--content-ink-3);
    font-size: 11.5px;
  }
}

.feature-stats {
  display: flex;
  gap: 10px;
  color: var(--content-ink-3);
  font-size: 12px;
}

.feature-cta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--content-ink-2);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
}

/* ===== 列表 ===== */
.list-body {
  transition: opacity 0.2s ease;
}

.list-body.is-refreshing {
  opacity: 0.45;
  pointer-events: none;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 34px;

  :deep(.el-pagination.is-background .btn-next),
  :deep(.el-pagination.is-background .btn-prev),
  :deep(.el-pagination.is-background .el-pager li) {
    border-radius: 10px;
    background: var(--content-surface);
    color: var(--content-ink-2);
    box-shadow: var(--content-shadow);
  }

  :deep(.el-pagination.is-background .el-pager li.is-active) {
    background: linear-gradient(120deg, #1a73e8, #6f5cf6);
    color: #fff;
    box-shadow: 0 8px 18px rgba(47, 99, 235, 0.28);
  }
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 70px 24px;
  border-radius: 24px;
  border: 1px dashed var(--content-line-strong);
  background: var(--content-veil);
  text-align: center;

  h3 {
    margin: 10px 0 0;
    font-size: 18px;
    color: var(--content-ink);
  }

  p {
    margin: 0;
    color: var(--content-ink-3);
    font-size: 13.5px;
  }
}

.empty-art {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 54px;

  span {
    width: 16px;
    border-radius: 6px;
    background: linear-gradient(180deg, rgba(26, 115, 232, 0.24), rgba(168, 85, 247, 0.18));

    &:nth-child(1) { height: 30px; }
    &:nth-child(2) { height: 50px; }
    &:nth-child(3) { height: 40px; }
  }
}

.empty-action {
  margin-top: 8px;
  color: var(--color-primary);
  font-size: 13.5px;
  font-weight: 500;
}

/* ===== 骨架 ===== */
.page-skeleton {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sk {
  display: block;
  border-radius: 8px;
  background: linear-gradient(90deg, var(--content-sk-a) 0%, var(--content-sk-b) 50%, var(--content-sk-a) 100%);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}

.sk-line { height: 13px; }
.sk-block { height: 100%; min-height: 280px; border-radius: 24px; }

.sk-hero {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 22px;
  padding: 20px;
  border-radius: 26px;
  background: var(--content-surface);
  border: 1px solid var(--content-line);
}

.sk-hero-lines {
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
}

.sk-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.sk-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 24px;
  border-radius: 20px;
  background: var(--content-surface);
  border: 1px solid var(--content-line);
}

@keyframes shimmer {
  from { background-position: 120% 0; }
  to { background-position: -20% 0; }
}

@media (max-width: 900px) {
  .feature-card {
    grid-template-columns: 1fr;
  }

  .feature-visual {
    min-height: 210px;
  }

  .article-grid {
    grid-template-columns: 1fr;
  }

  .sk-hero {
    grid-template-columns: 1fr;
  }

  .sk-block {
    min-height: 200px;
  }

  .sk-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .toolbar {
    align-items: flex-start;
  }

  .toolbar-side {
    width: 100%;
    justify-content: space-between;
  }

  .tag-row {
    width: 100%;
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 4px;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  .tag-chip {
    flex-shrink: 0;
  }

  .feature-body {
    padding: 20px;
  }

  .feature-foot {
    gap: 10px;
  }

  .feature-stats {
    display: none;
  }
}
</style>
