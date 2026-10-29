<template>
  <div class="home-page">
    <Header />
    
    <main class="main-content">
      <section class="hero-section">
        <ParticleNetwork />
        <div class="container hero-container-layout">
          <div class="hero-content">
            <h1 class="hero-title">Write to Think</h1>
            <p class="hero-desc">一个为创作者而写的空间。没有算法绑架，没有信息噪音，只有深度思考与真诚表达。</p>
            <div class="hero-actions">
              <router-link v-if="authStore.isLoggedIn" to="/create" class="btn-primary">提笔写作</router-link>
              <router-link v-else to="/register" class="btn-primary">提笔写作</router-link>
              <router-link to="/articles" class="btn-secondary">浏览精选</router-link>
              <router-link to="/" class="btn-primary hero-back">回溯</router-link>
            </div>
          </div>
          
          <!-- Google-style UI Mockup -->
          <div class="hero-mockup">
            <div class="mockup-header">
              <span class="dot red"></span>
              <span class="dot yellow"></span>
              <span class="dot green"></span>
              <div class="mockup-search">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20V10M10 20V4M16 20v-7M22 20V7"/></svg>
                榜单
              </div>
            </div>
            <div class="mockup-body">
              <div class="rank-panel" @mouseenter="rankHover = true" @mouseleave="rankHover = false">
                <div class="rank-tabs" role="tablist">
                  <button
                    v-for="tab in rankTabs"
                    :key="tab.key"
                    class="rank-tab"
                    :class="[`is-${tab.key}`, { 'is-active': activeRank === tab.key }]"
                    role="tab"
                    :aria-selected="activeRank === tab.key"
                    @click="selectRank(tab.key)"
                  >
                    <span class="rank-tab-icon" v-html="tab.icon"></span>
                    <span class="rank-tab-label">{{ tab.label }}</span>
                  </button>
                </div>

                <div class="rank-list">
                  <div v-if="rankLoading" class="rank-skeleton-wrap">
                    <span v-for="n in 3" :key="n" class="rank-skeleton"></span>
                  </div>

                  <transition-group v-else name="rank-swap" tag="div" class="rank-items">
                    <button
                      v-for="(article, index) in activeArticles"
                      :key="article.id"
                      class="rank-item"
                      @click="goArticle(article.id)"
                    >
                      <span class="rank-no" :class="`is-${index + 1}`">{{ index + 1 }}</span>
                      <span class="rank-main">
                        <span class="rank-title">{{ article.title }}</span>
                        <span class="rank-meta">
                          <span class="rank-author">{{ article.author?.username || '匿名作者' }}</span>
                          <span class="rank-stat rank-stat--views">
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="2.6"/></svg>
                            {{ formatCount(article.viewCount) }}
                          </span>
                          <span class="rank-stat rank-stat--likes">
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.7 7.8l1 1L12 21.2l7.8-7.8 1-1a5.5 5.5 0 0 0 0-7.8z"/></svg>
                            {{ formatCount(article.likeCount) }}
                          </span>
                          <span class="rank-stat rank-stat--favorites">
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 3h12a2 2 0 0 1 2 2v16l-8-4.5L4 21V5a2 2 0 0 1 2-2z"/></svg>
                            {{ formatCount(article.favoriteCount) }}
                          </span>
                        </span>
                      </span>
                      <span class="rank-arrow">→</span>
                    </button>
                  </transition-group>

                  <p v-if="!rankLoading && activeArticles.length === 0" class="rank-empty">
                    榜单还在生成中
                  </p>
                </div>

                <div class="rank-foot">
                  <span class="rank-live"></span>
                  <span>{{ activeRankLabel }} · 5 秒自动轮播</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="content-section">
        <div class="container">
          <div class="content-layout">
            <aside class="sidebar-left">
              <div class="sidebar-card">
                <h3 class="sidebar-title">热门标签</h3>
                <div class="tag-cloud">
                  <span v-for="tag in popularTags" :key="tag.name" class="tag-item" @click="handleTagClick(tag.name)">
                    {{ tag.name }}
                  </span>
                </div>
              </div>
            </aside>

            <div class="main-area">
              <div class="section-header">
                <div class="feed-tabs">
                  <button
                    class="feed-tab"
                    :class="{ active: activeFeed === 'recommend' }"
                    @click="switchFeed('recommend')"
                  >推荐</button>
                  <button
                    v-if="authStore.isLoggedIn"
                    class="feed-tab"
                    :class="{ active: activeFeed === 'following' }"
                    @click="switchFeed('following')"
                  >关注</button>
                </div>
                <router-link to="/articles" class="section-more">查看更多 →</router-link>
              </div>

              <div class="article-list" v-loading="loading">
                <ArticleCard
                  v-for="article in articles"
                  :key="article.id"
                  :article="article"
                  @like="handleLike"
                  @favorite="handleFavorite"
                />

                <div v-if="!loading && articles.length === 0" class="empty-state">
                  <div class="empty-icon">📝</div>
                  <p>{{ activeFeed === 'following' ? '关注作者的文章会显示在这里' : '静候第一位创作者' }}</p>
                  <router-link v-if="activeFeed === 'recommend'" to="/create" class="btn-primary">提笔写作</router-link>
                  <router-link v-else to="/articles" class="btn-primary">去发现作者</router-link>
                </div>
              </div>
            </div>

            <aside class="sidebar-right">
              <div v-if="authStore.isLoggedIn && currentUser" class="sidebar-card author-card">
                <div class="author-avatar">
                  <img :src="currentUser?.avatar || defaultAvatar" alt="用户头像" />
                </div>
                <h3 class="author-name">{{ currentUser?.username }}</h3>
                <p class="author-bio">{{ currentUser?.bio || '这个人很懒，什么都没有写' }}</p>
                <div class="author-stats">
                  <div class="stat">
                    <span class="stat-value">{{ userStats.articleCount }}</span>
                    <span class="stat-label">文章</span>
                  </div>
                  <div class="stat">
                    <span class="stat-value">{{ userStats.followCount }}</span>
                    <span class="stat-label">关注</span>
                  </div>
                  <div class="stat">
                    <span class="stat-value">{{ userStats.fansCount }}</span>
                    <span class="stat-label">粉丝</span>
                  </div>
                </div>
                <router-link to="/create" class="btn-primary btn-full">提笔写作</router-link>
              </div>

              <div class="sidebar-card">
                <h3 class="sidebar-title">热门文章</h3>
                <ul class="hot-articles">
                  <li v-for="(item, index) in hotArticles" :key="item.id" class="hot-item">
                    <span class="hot-number" :class="'hot-number-' + (index + 1)">{{ index + 1 }}</span>
                    <router-link :to="`/articles/${item.id}`" class="hot-title">{{ item.title }}</router-link>
                  </li>
                </ul>
              </div>
            </aside>
          </div>
        </div>
      </section>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import ArticleCard from '@/components/ArticleCard.vue'
import ParticleNetwork from '@/components/ParticleNetwork.vue'
import { useAuthStore } from '@/store/auth'
import { getHotArticles, getPopularArticles, getArticles } from '@/api/article'
import request from '@/utils/request'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const articles = ref([])
const hotArticles = ref([])
const currentUser = computed(() => authStore.user)
const defaultAvatar = 'https://www.w3schools.com/w3images/avatar2.png'
const activeFeed = ref('recommend')

const userStats = ref({
  articleCount: 0,
  followCount: 0,
  fansCount: 0
})

const popularTags = ref([
  { name: '前端' },
  { name: '后端' },
  { name: 'JavaScript' },
  { name: 'Vue' },
  { name: 'React' },
  { name: 'Python' },
  { name: '架构' },
  { name: 'DevOps' }
])

/* ===== 首页榜单轮播 ===== */
const rankTabs = [
  {
    key: 'views',
    label: '播放量第一',
    icon: '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="2.6"/></svg>'
  },
  {
    key: 'likes',
    label: '点赞第一',
    icon: '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.7 7.8l1 1L12 21.2l7.8-7.8 1-1a5.5 5.5 0 0 0 0-7.8z"/></svg>'
  },
  {
    key: 'favorites',
    label: '收藏第一',
    icon: '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 3h12a2 2 0 0 1 2 2v16l-8-4.5L4 21V5a2 2 0 0 1 2-2z"/></svg>'
  }
]
const activeRank = ref('views')
const rankLoading = ref(true)
const rankHover = ref(false)
const rankings = ref({ views: [], likes: [], favorites: [] })
const activeArticles = computed(() => rankings.value[activeRank.value] || [])
const activeRankLabel = computed(
  () => rankTabs.find((t) => t.key === activeRank.value)?.label || ''
)
let rankTimer = null

const formatCount = (n) => {
  const v = Number(n) || 0
  if (v >= 10000) return `${(v / 10000).toFixed(1).replace(/\.0$/, '')}w`
  if (v >= 1000) return `${(v / 1000).toFixed(1).replace(/\.0$/, '')}k`
  return String(v)
}

const goArticle = (id) => router.push(`/articles/${id}`)

const selectRank = (key) => {
  activeRank.value = key
  restartRankTimer()
}

const loadRankings = async () => {
  rankLoading.value = true
  try {
    const [hot, popular, favPage] = await Promise.all([
      getHotArticles(3),
      getPopularArticles(3),
      getArticles({ page: 0, size: 3, sortBy: 'favoriteCount' })
    ])
    rankings.value = {
      views: (Array.isArray(hot) ? hot : []).slice(0, 3),
      likes: (Array.isArray(popular) ? popular : []).slice(0, 3),
      favorites: (Array.isArray(favPage?.list) ? favPage.list : []).slice(0, 3)
    }
  } catch {
    // 接口异常时保留占位文案
  } finally {
    rankLoading.value = false
  }
}

function restartRankTimer() {
  clearInterval(rankTimer)
  rankTimer = setInterval(() => {
    if (rankHover.value || rankLoading.value) return
    const idx = rankTabs.findIndex((t) => t.key === activeRank.value)
    activeRank.value = rankTabs[(idx + 1) % rankTabs.length].key
  }, 5000)
}

onMounted(() => {
  loadArticles()
  loadHotArticles()
  loadRankings()
  restartRankTimer()
  if (authStore.isLoggedIn) {
    loadUserProfile()
  }
})

onUnmounted(() => {
  clearInterval(rankTimer)
})

const loadArticles = async () => {
  loading.value = true
  try {
    const url = activeFeed.value === 'following' ? '/api/articles/feed' : '/api/articles'
    const page = await request({
      url,
      method: 'get',
      params: { page: 0, size: 10 }
    })
    articles.value = Array.isArray(page?.list) ? page.list : []
  } catch (error) {
    console.error('加载文章失败:', error)
    articles.value = []
  } finally {
    loading.value = false
  }
}

const switchFeed = (feed) => {
  activeFeed.value = feed
  loadArticles()
}

const loadHotArticles = async () => {
  try {
    const list = await request({
      url: '/api/articles/hot',
      method: 'get',
      params: { limit: 5 }
    })
    hotArticles.value = Array.isArray(list) ? list : []
  } catch (error) {
    console.error('加载热门文章失败:', error)
    hotArticles.value = []
  }
}

const loadUserProfile = async () => {
  try {
    if (!authStore.user) {
      await authStore.fetchProfile()
    }
    const response = await request({
      url: '/api/users/profile',
      method: 'get'
    })
    userStats.value = {
      articleCount: response.articleCount || 0,
      followCount: response.followCount || 0,
      fansCount: response.fansCount || 0
    }
    if (response.user) {
      authStore.user = { ...authStore.user, ...response.user }
    }
  } catch (error) {
    // 401 表示未登录或 token 过期，request.js 会自动跳转登录页，无需报错
    if (error.businessCode !== 401 && error.response?.status !== 401) {
      console.error('加载用户信息失败:', error)
    }
  }
}

const handleTagClick = (tag) => {
  router.push({ path: '/articles', query: { tag } })
}

const handleLike = async (id) => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const data = await request({
      url: `/api/articles/${id}/like`,
      method: 'post'
    })
    const article = articles.value.find(a => a.id === id)
    if (article) {
      article.isLiked = data.isLiked
      article.likeCount = data.likeCount
    }
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

const handleFavorite = async (id) => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const data = await request({
      url: `/api/articles/${id}/favorite`,
      method: 'post'
    })
    const article = articles.value.find(a => a.id === id)
    if (article) {
      article.isFavorited = data.isFavorited
      article.favoriteCount = data.favoriteCount
    }
  } catch (error) {
    console.error('收藏失败:', error)
  }
}
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
}

.hero-section {
  background:
    radial-gradient(circle at 10% 20%, var(--content-glow-a) 0%, transparent 42%),
    radial-gradient(circle at 90% 80%, var(--content-glow-b) 0%, transparent 44%),
    var(--color-bg);
  padding: calc(var(--header-height) + 40px) 0 80px;
  border-bottom: 1px solid var(--content-line);
  position: relative;
  overflow: hidden;
  min-height: 600px;

  &::before {
    content: '';
    position: absolute;
    top: -20%;
    left: -20%;
    width: 140%;
    height: 140%;
    background: radial-gradient(circle at 30% 30%, rgba(26, 115, 232, 0.04) 0%, rgba(168, 85, 247, 0.04) 30%, rgba(236, 72, 153, 0.03) 60%, transparent 100%);
    filter: blur(80px);
    animation: floatGrad 20s ease-in-out infinite;
    z-index: 0;
  }
}

@keyframes floatGrad {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  50% {
    transform: translate(-3%, 3%) rotate(180deg);
  }
}

.hero-container-layout {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.hero-content {
  max-width: 780px;
  margin: 0 auto 48px;
  position: relative;
  z-index: 1;
}

.hero-title {
  font-size: 64px;
  font-family: var(--font-artistic);
  font-weight: 400;
  color: var(--content-ink);
  margin-bottom: 20px;
  line-height: 1.3;
  letter-spacing: 0.04em;
  background: linear-gradient(135deg, var(--content-ink) 30%, var(--color-primary) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-desc {
  /* 42 个汉字要压在一行：容器放宽到 780px 并锁 nowrap，窄屏再放开折行 */
  font-size: 16.5px;
  color: var(--content-ink-2);
  margin-bottom: 32px;
  line-height: 1.7;
  white-space: nowrap;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.hero-back {
  /* 与提笔写作同款按钮，仅压一点视觉权重，避免抢主 CTA */
  opacity: 0.82;

  &:hover { opacity: 1 }
}

/* Google-style UI Mockup */
.hero-mockup {
  width: 100%;
  max-width: 820px;
  height: 380px;
  @include liquid-glass;
  @include glass-rim(0.66, 0.26);
  position: relative;
  z-index: 1;
  border-radius: var(--radius-lg);
  box-shadow:
    inset 1px 1px var(--color-liquid-glass-shine),
    inset -1px -1px rgba(255, 255, 255, 0.14),
    0 20px 50px rgba(32, 33, 36, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .mockup-header {
    height: 40px;
    background: var(--color-bg-elevated);
    border-bottom: 1px solid var(--color-divider);
    display: flex;
    align-items: center;
    padding: 0 16px;
    gap: 8px;
    position: relative;

    .dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      display: inline-block;

      &.red { background: var(--color-google-red); opacity: 0.8; }
      &.yellow { background: var(--color-google-yellow); opacity: 0.8; }
      &.green { background: var(--color-google-green); opacity: 0.8; }
    }

    .mockup-search {
      position: absolute;
      left: 50%;
      transform: translateX(-50%);
      height: 24px;
      padding: 0 14px;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: rgba(59, 130, 246, 0.1);
      border: 1px solid rgba(59, 130, 246, 0.18);
      border-radius: var(--radius-full);
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.08em;
      color: #2563eb;

      svg {
        opacity: 0.85;
      }
    }
  }

  .mockup-body {
    flex: 1;
    display: flex;
  }

  .mockup-body {
    flex: 1;
    display: flex;
    min-height: 0;
  }

  /* ===== 榜单轮播 ===== */
  .rank-panel {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    padding: 16px 18px 12px;
  }

  .rank-tabs {
    display: inline-flex;
    gap: 4px;
    margin-bottom: 12px;
    padding: 4px;
    border-radius: 999px;
    border: 1px solid var(--content-line);
    background: var(--content-tabs);
    flex-shrink: 0;
    align-self: flex-start;
  }

  .rank-tab {
    position: relative;
    height: 28px;
    padding: 0 13px;
    border-radius: 999px;
    border: 1px solid transparent;
    background: transparent;
    color: var(--content-ink-2);
    font-family: inherit;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.02em;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    overflow: hidden;
    transition: color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease,
      transform 0.2s ease;

    /* 掠过式高光 */
    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: -130%;
      width: 60%;
      height: 100%;
      background: linear-gradient(
        100deg,
        transparent,
        rgba(255, 255, 255, 0.6),
        transparent
      );
      transform: skewX(-18deg);
      transition: left 0.5s ease;
      pointer-events: none;
    }

    &:hover {
      color: var(--color-primary);
      background: var(--content-chip);
      transform: translateY(-1px);

      &::after {
        left: 135%;
      }
    }

    &.is-active {
      color: #fff;
      background: linear-gradient(135deg, #3b82f6 0%, #6366f1 55%, #8b5cf6 100%);
      box-shadow:
        0 6px 16px rgba(59, 130, 246, 0.3),
        inset 0 1px 0 rgba(255, 255, 255, 0.28);

      .rank-tab-icon {
        color: #fff;
      }
    }

    &.is-views .rank-tab-icon {
      color: #3b82f6;
    }

    &.is-likes .rank-tab-icon {
      color: #f43f5e;
    }

    &.is-favorites .rank-tab-icon {
      color: #f59e0b;
    }
  }

  .rank-tab-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition: color 0.2s ease;

    :deep(svg) {
      display: block;
    }
  }

  .rank-list {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  .rank-items {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .rank-item {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    padding: 10px 12px;
    border-radius: 14px;
    border: 1px solid var(--content-line);
    background: var(--content-surface);
    text-align: left;
    font-family: inherit;
    cursor: pointer;
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

    &:hover {
      transform: translateX(3px);
      border-color: rgba(59, 130, 246, 0.32);
      box-shadow: 0 8px 20px rgba(59, 130, 246, 0.12);

      .rank-arrow {
        opacity: 1;
        transform: translateX(0);
      }
    }
  }

  .rank-no {
    width: 26px;
    height: 26px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 9px;
    font-size: 12px;
    font-weight: 800;
    font-variant-numeric: tabular-nums;
    color: #fff;
    background: var(--color-border);

    &.is-1 {
      background: linear-gradient(135deg, #fbbf24, #f59e0b);
      box-shadow: 0 4px 10px rgba(245, 158, 11, 0.3);
    }

    &.is-2 {
      background: linear-gradient(135deg, #cbd5e1, #94a3b8);
    }

    &.is-3 {
      background: linear-gradient(135deg, #fdba74, #fb923c);
    }
  }

  .rank-main {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;
  }

  .rank-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--content-ink);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .rank-meta {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 11px;
    color: var(--content-ink-3);
    overflow: hidden;
    white-space: nowrap;
  }

  .rank-author {
    max-width: 90px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .rank-stat {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    height: 20px;
    padding: 0 7px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    transition: background-color 0.2s ease;

    svg {
      opacity: 0.85;
    }

    &--views {
      background: rgba(59, 130, 246, 0.1);
      color: #2563eb;
    }

    &--likes {
      background: rgba(244, 63, 94, 0.1);
      color: #e11d48;
    }

    &--favorites {
      background: rgba(245, 158, 11, 0.13);
      color: #b45309;
    }
  }

  .rank-arrow {
    font-size: 14px;
    color: var(--color-primary);
    opacity: 0;
    transform: translateX(-4px);
    transition: all 0.2s ease;
  }

  .rank-foot {
    display: flex;
    align-items: center;
    gap: 7px;
    margin-top: 10px;
    font-size: 11px;
    color: var(--content-ink-3);
    flex-shrink: 0;
  }

  .rank-live {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #34d399;
    box-shadow: 0 0 0 3px rgba(52, 211, 153, 0.18);
  }

  .rank-empty {
    margin: 24px 0 0;
    text-align: center;
    font-size: 12px;
    color: var(--content-ink-3);
  }

  .rank-skeleton-wrap {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .rank-skeleton {
    display: block;
    height: 58px;
    border-radius: 14px;
    background: linear-gradient(
      100deg,
      var(--color-bg-soft) 30%,
      var(--color-bg-elevated) 50%,
      var(--color-bg-soft) 70%
    );
    background-size: 220% 100%;
    animation: shimmer 1.4s ease-in-out infinite;
  }

  .rank-swap-enter-active,
  .rank-swap-leave-active {
    transition: opacity 0.26s ease, transform 0.26s ease;
  }

  .rank-swap-enter-from {
    opacity: 0;
    transform: translateY(8px);
  }

  .rank-swap-leave-active {
    position: absolute;
    left: 0;
    right: 0;
    pointer-events: none;
  }

  .rank-swap-leave-to {
    opacity: 0;
    transform: translateY(-6px);
  }
  }

.content-section {
  flex: 1;
  padding: 56px 0;
  background: var(--color-bg);
  position: relative;
  z-index: 1;
}

.content-layout {
  display: grid;
  grid-template-columns: 240px 1fr 300px;
  gap: 32px;
}

.sidebar-card {
  padding: 24px;
  margin-bottom: 24px;
  border-radius: 24px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
}

.sidebar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--content-ink);
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--content-line);
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  display: flex;
  align-items: center;
  padding: 7px 14px;
  background: var(--content-chip);
  border: 1px solid var(--content-chip-line);
  border-radius: var(--radius-full);
  font-size: 13px;
  color: var(--content-ink-2);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    border-color: var(--content-line-strong);
    color: var(--color-primary);
  }
}

.section-header {
  @include flex-between;
  margin-bottom: 24px;
}

.feed-tabs {
  display: inline-flex;
  gap: 3px;
  padding: 4px;
  border-radius: var(--radius-full);
  border: 1px solid var(--content-line);
  background: var(--content-tabs);
}

.feed-tab {
  padding: 7px 18px;
  border-radius: var(--radius-full);
  border: none;
  background: transparent;
  color: var(--content-ink-2);
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    color: var(--color-primary);
  }

  &.active {
    background: var(--content-surface);
    color: var(--color-primary);
    box-shadow: var(--content-shadow);
  }
}

.section-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--content-ink);
  letter-spacing: -0.01em;
}

.section-more {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-primary);

  &:hover {
    color: var(--color-primary-dark);
  }
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.empty-state {
  text-align: center;
  padding: 60px 24px;
  border-radius: 24px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);

  .empty-icon {
    font-size: 48px;
    margin-bottom: 18px;
  }

  p {
    font-size: 16px;
    color: var(--content-ink-2);
    margin-bottom: 24px;
  }
}

.author-card {
  text-align: center;

  .author-avatar {
    width: 80px;
    height: 80px;
    margin: 0 auto 16px;
    border-radius: var(--radius-full);
    overflow: hidden;
    border: 3px solid var(--color-google-blue-light);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .author-name {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 6px;
    color: var(--content-ink);
  }

  .author-bio {
    font-size: 13.5px;
    color: var(--content-ink-3);
    margin-bottom: 20px;
    line-height: 1.5;
  }

  .author-stats {
    display: flex;
    justify-content: center;
    gap: 24px;
    margin-bottom: 24px;
    padding-bottom: 20px;
    border-bottom: 1px solid var(--content-line);

    .stat {
      text-align: center;
    }

    .stat-value {
      display: block;
      font-size: 18px;
      font-weight: 600;
      color: var(--color-primary);
    }

    .stat-label {
      font-size: 12px;
      color: var(--content-ink-3);
    }
  }
}

.btn-full {
  width: 100%;
}

.hot-articles {
  .hot-item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 0;
    border-bottom: 1px solid var(--content-line);

    &:last-child {
      border-bottom: none;
    }
  }

  .hot-number {
    flex-shrink: 0;
    width: 20px;
    height: 20px;
    background: var(--color-google-blue-light);
    color: var(--color-primary);
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    @include flex-center;

    &.hot-number-1 {
      background: var(--color-google-red-light);
      color: var(--color-google-red);
    }
    &.hot-number-2 {
      background: var(--color-google-yellow-light);
      color: #b06000;
    }
    &.hot-number-3 {
      background: var(--color-google-green-light);
      color: var(--color-google-green);
    }
  }

  .hot-title {
    font-size: 14px;
    color: var(--content-ink);
    line-height: 1.45;
    font-weight: 400;
    transition: color var(--transition-fast);

    &:hover {
      color: var(--color-primary);
    }
  }
}

@media (max-width: 1199px) {
  .content-layout {
    grid-template-columns: 200px 1fr;
  }

  .sidebar-right {
    display: none;
  }
}

@media (max-width: 768px) {
  .hero-section {
    min-height: auto;
    padding: calc(var(--header-height) + 24px) 0 48px;
  }

  .hero-title {
    font-size: 42px;
  }

  .hero-desc {
    font-size: 16px;
    white-space: normal;
  }

  .hero-mockup {
    display: none;
  }

  .content-layout {
    grid-template-columns: 1fr;
  }

  .sidebar-left {
    order: 2;
  }

  .main-area {
    order: 1;
  }
}

/* ===== 榜单：深色模式适配 ===== */
:global([data-theme='dark']) {
  .rank-tabs {
    background: rgba(255, 255, 255, 0.06);
  }

  .rank-tab:hover {
    background: rgba(255, 255, 255, 0.1);
  }

  .mockup-search {
    background: rgba(59, 130, 246, 0.2);
    border-color: rgba(147, 197, 253, 0.3);
    color: #93c5fd;
  }

  .rank-stat--views {
    background: rgba(59, 130, 246, 0.22);
    color: #93c5fd;
  }

  .rank-stat--likes {
    background: rgba(244, 63, 94, 0.22);
    color: #fda4af;
  }

  .rank-stat--favorites {
    background: rgba(245, 158, 11, 0.22);
    color: #fcd34d;
  }
}
</style>
