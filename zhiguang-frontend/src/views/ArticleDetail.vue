<template>
  <div class="article-detail-page">
    <Header />

    <main class="main-content" v-loading="loading">
      <div class="container" v-if="article">
        <div class="article-layout">
          <article class="article-main">
            <header class="article-header">
              <div class="tags" v-if="article.tags?.length">
                <span v-for="tag in article.tags" :key="tag" class="tag" @click="goToTag(tag)">{{ tag }}</span>
              </div>
              <h1 class="article-title">{{ article.title }}</h1>
              <div class="article-meta">
                <div class="author-info">
                  <img :src="article.author?.avatar || defaultAvatar" alt="头像" class="author-avatar" />
                  <div class="author-details">
                    <span class="author-name">{{ article.author?.username }}</span>
                    <span class="meta-info">
                      {{ formatDate(article.createdAt) }} · {{ article.viewCount }} 阅读
                    </span>
                  </div>
                  <button
                    v-if="article.author?.id !== currentUserId"
                    class="follow-btn"
                    :class="{ following: isFollowing }"
                    @click="toggleFollow"
                  >
                    {{ isFollowing ? '已关注' : '+ 关注' }}
                  </button>
                </div>
              </div>
            </header>

            <div class="article-cover" v-if="article.cover">
              <img :src="article.cover" alt="文章封面" />
            </div>

            <!-- AI Summary Card -->
            <div class="ai-summary-card" v-if="article">
              <div class="ai-summary-header">
                <div class="ai-title">
                  <div class="ai-icon-wrapper">
                    <svg class="ai-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M12 2L14.85 9.15L22 12L14.85 14.85L12 22L9.15 14.85L2 12L9.15 9.15L12 2Z" fill="url(#aiGrad)" />
                      <defs>
                        <linearGradient id="aiGrad" x1="2" y1="2" x2="22" y2="22" gradientUnits="userSpaceOnUse">
                          <stop offset="0%" stop-color="#6366f1" />
                          <stop offset="50%" stop-color="#a855f7" />
                          <stop offset="100%" stop-color="#ec4899" />
                        </linearGradient>
                      </defs>
                    </svg>
                  </div>
                  <span>AI 智能总结</span>
                  <span class="ai-badge">AI Generative</span>
                </div>
                <div class="ai-header-actions" v-if="article.aiSummary && !generatingSummary">
                  <button class="ai-action-text-btn" @click="copySummary">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                      <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                    </svg>
                    复制
                  </button>
                  <button class="ai-action-text-btn" @click="handleGenerateSummary(true)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"></path>
                    </svg>
                    重新生成
                  </button>
                </div>
              </div>

              <div class="ai-summary-body">
                <!-- Generating/loading State -->
                <div class="ai-summary-generating" v-if="generatingSummary">
                  <div class="shimmer-lines">
                    <div class="shimmer-line line-1"></div>
                    <div class="shimmer-line line-2"></div>
                    <div class="shimmer-line line-3"></div>
                  </div>
                  <div class="generating-text">
                    <span class="spinner"></span>
                    <span>AI 正在深度解析文章并生成总结中...</span>
                  </div>
                </div>

                <!-- Ready State -->
                <div class="ai-summary-content" v-else-if="article.aiSummary">
                  <p>{{ article.aiSummary }}</p>
                </div>

                <!-- Ungenerated State -->
                <div class="ai-summary-empty" v-else>
                  <p class="empty-tip">本篇文章暂无 AI 总结，点击下方按钮一键提炼核心要点。</p>
                  <button class="btn-ai-generate" @click="handleGenerateSummary(false)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="sparkle-icon">
                      <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                      <polyline points="2 17 12 22 22 17"></polyline>
                      <polyline points="2 12 12 17 22 12"></polyline>
                    </svg>
                    一键生成 AI 智能总结
                  </button>
                </div>
              </div>
            </div>

            <MarkdownPreview class="article-content" :content="article.content" />

            <footer class="article-footer">
              <div class="actions">
                <button class="action-btn" :class="{ active: article.isLiked }" @click="handleLike">
                  <svg width="20" height="20" viewBox="0 0 24 24" :fill="article.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                  </svg>
                  <span>{{ article.likeCount || 0 }} 赞</span>
                </button>
                <button class="action-btn" :class="{ active: article.isFavorited }" @click="handleFavorite">
                  <svg width="20" height="20" viewBox="0 0 24 24" :fill="article.isFavorited ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                    <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
                  </svg>
                  <span>{{ article.favoriteCount || 0 }} 收藏</span>
                </button>
                <button class="action-btn" @click="handleShare">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="18" cy="5" r="3"/>
                    <circle cx="6" cy="12" r="3"/>
                    <circle cx="18" cy="19" r="3"/>
                    <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
                    <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
                  </svg>
                  <span>分享</span>
                </button>
                <button class="action-btn" @click="handleAddToKnowledge" v-if="authStore.isLoggedIn">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                    <path d="M2 17l10 5 10-5"/>
                    <path d="M2 12l10 5 10-5"/>
                  </svg>
                  <span>加入知识库</span>
                </button>
              </div>
            </footer>

            <section class="comments-section">
              <h3 class="section-title">评论 ({{ article.commentCount ?? comments.length }})</h3>

              <div class="comment-form" v-if="authStore.isLoggedIn">
                <img :src="currentUserAvatar" alt="我的头像" class="comment-avatar" />
                <div class="comment-input-wrapper">
                  <div v-if="replyingTo" class="replying-indicator">
                    <span>回复 @{{ replyingTo.user?.username }}</span>
                    <button class="cancel-reply" @click="cancelReply">取消回复</button>
                  </div>
                  <textarea
                    ref="commentInputRef"
                    v-model="commentContent"
                    :placeholder="replyingTo ? `回复 @${replyingTo.user?.username}...` : '分享你的思考...'"
                    class="comment-input"
                    rows="3"
                  ></textarea>
                  <button class="btn-primary" @click="submitComment" :disabled="!commentContent.trim() || submitting">
                    {{ submitting ? '发送中...' : '发送' }}
                  </button>
                </div>
              </div>

              <div v-else class="login-tip">
                <router-link to="/login" class="link">登录</router-link> 后加入讨论
              </div>

              <div class="comments-list" v-if="comments.length > 0">
                <div v-for="comment in comments" :key="comment.id" class="comment-item">
                  <img :src="comment.user?.avatar || defaultAvatar" alt="头像" class="comment-avatar" />
                  <div class="comment-body">
                    <div class="comment-header">
                      <span class="comment-author">{{ comment.user?.username }}</span>
                      <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                    </div>
                    <p class="comment-content">{{ comment.content }}</p>
                    <div class="comment-actions" v-if="authStore.isLoggedIn">
                      <button class="comment-action-btn" @click="handleReply(comment)">回复</button>
                      <button
                        v-if="comment.user?.id === currentUserId || article.author?.id === currentUserId"
                        class="comment-action-btn danger"
                        @click="deleteComment(comment.id)"
                      >
                        删除
                      </button>
                    </div>

                    <div v-if="comment.replies?.length" class="comment-replies">
                      <div v-for="reply in comment.replies" :key="reply.id" class="comment-item reply-item">
                        <img :src="reply.user?.avatar || defaultAvatar" alt="头像" class="comment-avatar" />
                        <div class="comment-body">
                          <div class="comment-header">
                            <span class="comment-author">{{ reply.user?.username }}</span>
                            <span class="comment-time">{{ formatTime(reply.createdAt) }}</span>
                          </div>
                          <p class="comment-content">{{ reply.content }}</p>
                          <div class="comment-actions" v-if="authStore.isLoggedIn">
                            <button class="comment-action-btn" @click="handleReply(reply)">回复</button>
                            <button
                              v-if="reply.user?.id === currentUserId || article.author?.id === currentUserId"
                              class="comment-action-btn danger"
                              @click="deleteComment(reply.id)"
                            >
                              删除
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="commentsHasMore" class="load-more">
                  <button class="btn-secondary" :disabled="loadingMoreComments" @click="loadMoreComments">
                    {{ loadingMoreComments ? '加载中...' : '加载更多评论' }}
                  </button>
                </div>
              </div>

              <div v-else class="no-comments">
                尚无评论，期待你的洞见
              </div>
            </section>
          </article>

          <aside class="article-sidebar">
            <div class="sidebar-card author-card">
              <h4 class="sidebar-title">作者</h4>
              <div class="author-info">
                <img :src="article.author?.avatar || defaultAvatar" alt="头像" class="author-avatar-lg" />
                <div class="author-details">
                  <span class="author-name">{{ article.author?.username }}</span>
                  <p class="author-bio">{{ article.author?.bio || '暂无简介' }}</p>
                </div>
              </div>
              <button
                v-if="article.author?.id !== currentUserId"
                class="btn-secondary btn-full"
                :class="{ following: isFollowing }"
                @click="toggleFollow"
              >
                {{ isFollowing ? '已关注' : '+ 关注' }}
              </button>
            </div>

            <div class="sidebar-card">
              <h4 class="sidebar-title">相关推荐</h4>
              <ul class="related-articles">
                <li v-for="item in relatedArticles" :key="item.id">
                  <router-link :to="`/articles/${item.id}`" class="related-link">
                    {{ item.title }}
                  </router-link>
                </li>
                <li v-if="relatedArticles.length === 0" class="no-related">
                  暂无相关推荐
                </li>
              </ul>
            </div>
          </aside>
        </div>
      </div>

      <div v-if="!loading && !article" class="not-found">
        <h2>文章已下架或从未存在</h2>
        <router-link to="/articles" class="btn-primary">返回浏览</router-link>
      </div>
    </main>

    <Footer />

    <AddToKnowledgeDialog
      v-model="showAddToKnowledgeDialog"
      :article-id="article?.id"
      @success="onAddToKnowledgeSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import AddToKnowledgeDialog from '@/components/AddToKnowledgeDialog.vue'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { summarizeArticle } from '@/api/ai'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const article = ref(null)
const comments = ref([])
const commentsPage = ref(0)
const commentsHasMore = ref(false)
const loadingMoreComments = ref(false)
const replyingTo = ref(null)
const commentInputRef = ref(null)
const commentContent = ref('')
const submitting = ref(false)
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const relatedArticles = ref([])
const isFollowing = ref(false)
const isFavoriting = ref(false)
const generatingSummary = ref(false)
const showAddToKnowledgeDialog = ref(false)

const handleGenerateSummary = async (force = false) => {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后使用 AI 总结功能')
    router.push({
      path: '/login',
      query: { redirect: route.fullPath }
    })
    return
  }

  generatingSummary.value = true
  try {
    const summary = await summarizeArticle(article.value.id, force)
    article.value.aiSummary = summary
    ElMessage.success(force ? '重新生成总结成功' : '生成总结成功')
  } catch (error) {
    console.error('生成总结失败:', error)
  } finally {
    generatingSummary.value = false
  }
}

const copySummary = () => {
  if (!article.value?.aiSummary) return
  navigator.clipboard.writeText(article.value.aiSummary)
  ElMessage.success('总结内容已复制到剪贴板')
}

const currentUserAvatar = computed(() => authStore.user?.avatar || defaultAvatar)
const currentUserId = computed(() => authStore.user?.id)

onMounted(() => {
  loadArticle()
  loadComments()
})

const loadArticle = async () => {
  loading.value = true
  try {
    const data = await request({
      url: `/api/articles/${route.params.id}`,
      method: 'get'
    })
    article.value = data

    if (authStore.isLoggedIn && data.author?.id) {
      try {
        const followStatus = await request({
          url: `/api/follows/status/${data.author.id}`,
          method: 'get'
        })
        isFollowing.value = followStatus.isFollowing
      } catch (e) {
        console.error('获取关注状态失败', e)
      }
    }

    loadRelatedArticles()
  } catch (error) {
    console.error('加载文章失败:', error)
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
}

const loadRelatedArticles = async () => {
  try {
    const list = await request({
      url: '/api/articles/hot',
      method: 'get',
      params: { limit: 5 }
    })
    relatedArticles.value = (Array.isArray(list) ? list : []).filter(a => a.id !== article.value?.id).slice(0, 5)
  } catch (error) {
    console.error('加载相关推荐失败:', error)
  }
}

const loadComments = async (reset = true) => {
  if (reset) {
    commentsPage.value = 0
    comments.value = []
    commentsHasMore.value = false
  }

  try {
    const pageData = await request({
      url: `/api/comments/article/${route.params.id}`,
      method: 'get',
      params: { page: commentsPage.value, size: 20 }
    })
    const list = Array.isArray(pageData?.list) ? pageData.list : []
    comments.value = reset ? list : comments.value.concat(list)
    commentsHasMore.value = commentsPage.value + 1 < (pageData?.totalPages || 0)
  } catch (error) {
    console.error('加载评论失败:', error)
    if (reset) comments.value = []
  }
}

const loadMoreComments = async () => {
  if (!commentsHasMore.value || loadingMoreComments.value) return
  loadingMoreComments.value = true
  commentsPage.value += 1
  try {
    await loadComments(false)
  } finally {
    loadingMoreComments.value = false
  }
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

const isLiking = ref(false)

const handleLike = async () => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (isLiking.value) {
    ElMessage.warning('操作过于频繁，请稍后再试')
    return
  }
  isLiking.value = true
  try {
    const data = await request({
      url: `/api/articles/${article.value.id}/like`,
      method: 'post'
    })
    article.value.isLiked = data.isLiked
    article.value.likeCount = data.likeCount
  } catch (error) {
    const message = error.response?.data?.message || '操作失败'
    ElMessage.warning(message)
  } finally {
    setTimeout(() => {
      isLiking.value = false
    }, 1000)
  }
}

const toggleFollow = async () => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const data = await request({
      url: `/api/follows/${article.value.author?.id}`,
      method: 'post'
    })
    isFollowing.value = data.isFollowing
    ElMessage.success(isFollowing.value ? '关注成功' : '已取消关注')
  } catch (error) {
    const message = error.response?.data?.message || '操作失败'
    ElMessage.warning(message)
  }
}

const handleFavorite = async () => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (isFavoriting.value) {
    ElMessage.warning('操作过于频繁，请稍后再试')
    return
  }
  isFavoriting.value = true
  try {
    const data = await request({
      url: `/api/articles/${article.value.id}/favorite`,
      method: 'post'
    })
    article.value.isFavorited = data.isFavorited
    article.value.favoriteCount = data.favoriteCount
  } catch (error) {
    const message = error.response?.data?.message || '操作失败'
    ElMessage.warning(message)
  } finally {
    setTimeout(() => {
      isFavoriting.value = false
    }, 1000)
  }
}

const handleShare = () => {
  navigator.clipboard.writeText(window.location.href)
  ElMessage.success('链接已复制到剪贴板')
}

const submitComment = async () => {
  if (!commentContent.value.trim()) return

  submitting.value = true
  try {
    await request({
      url: '/api/comments',
      method: 'post',
      data: {
        articleId: article.value.id,
        content: commentContent.value,
        parentId: replyingTo.value?.id || null
      }
    })
    ElMessage.success('评论发布成功')
    commentContent.value = ''
    replyingTo.value = null
    article.value.commentCount = (article.value.commentCount || 0) + 1
    await loadComments(true)
  } catch (error) {
    const message = error.response?.data?.message || '评论发布失败'
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

const handleReply = (comment) => {
  replyingTo.value = comment
  commentContent.value = ''
  if (commentInputRef.value) {
    commentInputRef.value.focus()
  }
}

const cancelReply = () => {
  replyingTo.value = null
}

const deleteComment = async (commentId) => {
  try {
    await request({
      url: `/api/comments/${commentId}`,
      method: 'delete'
    })
    ElMessage.success('评论已删除')
    article.value.commentCount = Math.max(0, (article.value.commentCount || 0) - 1)
    await loadComments(true)
  } catch (error) {
    const message = error.response?.data?.message || '删除失败'
    ElMessage.error(message)
  }
}

const goToTag = (tag) => {
  router.push({ path: '/articles', query: { tag } })
}

const handleAddToKnowledge = () => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  showAddToKnowledgeDialog.value = true
}

const onAddToKnowledgeSuccess = () => {
  // 可以在这里更新UI状态
}
</script>

<style lang="scss" scoped>
.article-detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(60% 36% at 90% 0%, var(--content-glow-b), transparent 62%),
    radial-gradient(50% 34% at 6% 8%, var(--content-glow-a), transparent 62%),
    var(--color-bg);
}

.main-content {
  flex: 1;
  padding: calc(var(--header-height) + 32px) 0 60px;
}

/* AI Summary Card Styles */
.ai-summary-card {
  position: relative;
  @include liquid-glass;
  border-radius: var(--radius-lg);
  padding: 24px 28px;
  margin-bottom: 36px;
  overflow: hidden;
  z-index: 1;
  transition: all var(--transition-slow);

  /* The rotating sweeping gradient border - Google Gemini colors */
  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: conic-gradient(
      from 0deg,
      transparent 20%,
      var(--color-google-blue) 35%,
      var(--color-ai-purple) 50%,
      var(--color-ai-pink) 65%,
      var(--color-google-green) 80%,
      transparent 100%
    );
    animation: rotate-border 6s linear infinite;
    z-index: -2;
  }

  /* Mask cover to leave a 1px border */
  &::after {
    content: '';
    position: absolute;
    inset: 1px;
    background: linear-gradient(135deg, rgba(26, 115, 232, 0.02) 0%, rgba(168, 85, 247, 0.02) 100%);
    background-color: var(--color-card);
    border-radius: calc(var(--radius-lg) - 1px);
    z-index: -1;
  }

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 16px 40px rgba(26, 115, 232, 0.08);
    
    &::before {
      animation-duration: 3s;
    }
  }
}

@keyframes rotate-border {
  100% {
    transform: rotate(360deg);
  }
}

.ai-summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.ai-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--color-google-blue) 0%, var(--color-ai-purple) 50%, var(--color-ai-pink) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.01em;
}

.ai-badge {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  padding: 2px 8px;
  background: var(--color-google-blue-light);
  color: var(--color-primary);
  border-radius: var(--radius-full);
  border: 1px solid rgba(26, 115, 232, 0.15);
  margin-left: 8px;
  -webkit-text-fill-color: initial;
}

.ai-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-google-blue-light);
  padding: 6px;
  border-radius: 8px;
  
  .ai-icon {
    animation: pulse 2.5s infinite ease-in-out;
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.9;
  }
  50% {
    transform: scale(1.15);
    opacity: 1;
    filter: drop-shadow(0 0 4px rgba(26, 115, 232, 0.4));
  }
}

.ai-header-actions {
  display: flex;
  gap: 12px;
}

.ai-action-text-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);

  &:hover {
    color: var(--color-primary);
    background: var(--color-google-blue-light);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }
}

.ai-summary-body {
  position: relative;
}

.ai-summary-content {
  font-size: 14.5px;
  line-height: 1.8;
  color: var(--color-text-secondary);
  text-align: justify;
}

.ai-summary-empty {
  text-align: center;
  padding: 24px 0;

  .empty-tip {
    font-size: 14px;
    color: var(--color-text-muted);
    margin-bottom: 20px;
  }
}

.btn-ai-generate {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 42px;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  border: 1.5px solid transparent;
  border-radius: 999px;
  cursor: pointer;
  background:
    linear-gradient(180deg, rgba(24, 33, 54, 0.82) 0%, rgba(11, 18, 33, 0.88) 100%) padding-box,
    linear-gradient(100deg, #22d3ee 0%, #3b82f6 34%, #8b5cf6 68%, #e879f9 100%) border-box;
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.14),
    0 8px 22px rgba(37, 99, 235, 0.24),
    0 0 18px rgba(139, 92, 246, 0.18);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1),
    box-shadow 0.2s cubic-bezier(0.4, 0, 0.2, 1), filter 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    filter: brightness(1.2);
    transform: translateY(-2px);
    box-shadow:
      0 0 0 1px rgba(96, 165, 250, 0.26),
      0 14px 32px rgba(37, 99, 235, 0.36),
      0 0 28px rgba(168, 85, 247, 0.32);
  }

  &:active {
    transform: translateY(0);
    filter: brightness(1.05);
  }

  .sparkle-icon {
    animation: spin-slow 8s linear infinite;
  }
}

@keyframes spin-slow {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Shimmer Loading Animations */
.shimmer-lines {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.shimmer-line {
  height: 12px;
  background: linear-gradient(90deg, rgba(26, 115, 232, 0.03) 25%, rgba(26, 115, 232, 0.1) 37%, rgba(26, 115, 232, 0.03) 63%);
  background-size: 400% 100%;
  border-radius: 4px;
  animation: shimmer 1.4s ease infinite;
  
  &.line-1 { width: 90%; }
  &.line-2 { width: 100%; }
  &.line-3 { width: 75%; }
}

@keyframes shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.generating-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  color: var(--color-text-muted);
  margin-top: 12px;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(26, 115, 232, 0.15);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.article-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 40px;
}

.article-main {
  background: var(--content-surface);
  border: 1px solid var(--content-line);
  border-radius: 28px;
  padding: 40px;
  box-shadow: var(--content-shadow);
}

.article-header {
  margin-bottom: 32px;
}

.tags {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.tag {
  padding: 5px 14px;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--color-primary);
  background: var(--content-chip);
  border: 1px solid var(--content-chip-line);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    border-color: var(--content-line-strong);
    background: var(--content-surface-tint);
  }
}

.article-title {
  font-size: 34px;
  font-weight: 600;
  color: var(--content-ink);
  line-height: 1.35;
  margin-bottom: 24px;
  letter-spacing: -0.02em;
}

.article-meta {
  border-bottom: 1px solid var(--content-line);
  padding-bottom: 24px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-full);
  object-fit: cover;
  border: 1px solid var(--content-line);
}

.author-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-name {
  font-size: 14.5px;
  font-weight: 500;
  color: var(--content-ink);
}

.meta-info {
  font-size: 12.5px;
  color: var(--content-ink-3);
}

.article-cover {
  margin: 32px 0;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid var(--content-line);
  box-shadow: var(--content-shadow);

  img {
    width: 100%;
    height: auto;
    display: block;
  }
}

.article-content {
  margin-bottom: 24px;
}

.article-footer {
  margin-top: 48px;
  padding-top: 24px;
  border-top: 1px solid var(--content-line);
}

.actions {
  display: flex;
  gap: 16px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 22px;
  font-size: 14px;
  font-weight: 500;
  color: var(--content-ink-2);
  background: var(--content-chip);
  border: 1px solid var(--content-chip-line);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-normal);

  &:hover {
    border-color: var(--color-primary-light);
    color: var(--color-primary);
    background: var(--color-google-blue-light);
    transform: translateY(-2px);
  }

  &:active {
    transform: translateY(0);
  }

  &.active {
    color: white;
    background: var(--color-primary);
    border-color: var(--color-primary);
    box-shadow: 0 4px 12px rgba(26, 115, 232, 0.2);

    &:nth-child(1) {
      background: var(--color-google-red);
      border-color: var(--color-google-red);
      box-shadow: 0 4px 12px rgba(234, 67, 53, 0.2);
    }

    &:nth-child(2) {
      background: var(--color-google-yellow);
      border-color: var(--color-google-yellow);
      box-shadow: 0 4px 12px rgba(251, 188, 5, 0.2);
      color: var(--color-text-inverse);
    }
  }
}

.comments-section {
  margin-top: 56px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--content-ink);
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--content-line);
}

.comment-form {
  display: flex;
  gap: 16px;
  margin-bottom: 36px;
}

.login-tip {
  text-align: center;
  padding: 28px;
  background: var(--content-surface-tint);
  border: 1px solid var(--content-line);
  border-radius: 16px;
  margin-bottom: 24px;
  color: var(--content-ink-2);

  .link {
    color: var(--color-primary);
    font-weight: 600;
  }
}

.comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
  border: 1px solid var(--color-border);
}

.comment-input-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-end;
}

.comment-input {
  width: 100%;
  padding: 16px 20px;
  border: 1px solid var(--content-chip-line);
  border-radius: 14px;
  font-size: 14px;
  resize: vertical;
  background: var(--content-surface-tint);
  color: var(--content-ink);
  transition: all var(--transition-normal);

  &:focus {
    border-color: var(--content-line-strong);
    box-shadow: 0 0 0 3px var(--content-glow-a);
    outline: none;
  }

  &::placeholder {
    color: var(--content-ink-3);
  }
}

.comment-item {
  display: flex;
  gap: 16px;
  padding: 24px 0;
  border-bottom: 1px solid var(--content-line);

  &:last-child {
    border-bottom: none;
  }
}

.comment-body {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.comment-author {
  font-size: 14px;
  font-weight: 500;
  color: var(--content-ink);
}

.comment-time {
  font-size: 12px;
  color: var(--content-ink-3);
}

.comment-content {
  font-size: 14.5px;
  line-height: 1.65;
  color: var(--content-ink-2);
  margin-bottom: 8px;
}

.comment-actions {
  display: flex;
  gap: 16px;
}

.replying-indicator {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  background: var(--color-bg-elevated);
  color: var(--color-text-secondary);
  font-size: 13px;
}

.cancel-reply {
  color: var(--color-text-muted);
  font-size: 13px;

  &:hover {
    color: var(--color-primary);
  }
}

.comment-replies {
  margin-top: 12px;
  margin-left: 36px;
  padding-left: 16px;
  border-left: 1px solid var(--color-card-border);
}

.reply-item .comment-avatar {
  width: 32px;
  height: 32px;
}

.load-more {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.comment-action-btn {
  font-size: 13px;
  color: var(--color-text-muted);
  font-weight: 500;

  &:hover {
    color: var(--color-primary);
  }

  &.danger:hover {
    color: var(--color-google-red);
  }
}

.no-comments, .no-related {
  text-align: center;
  padding: 32px;
  color: var(--color-text-muted);
  font-size: 14px;
}

.article-sidebar {
  position: sticky;
  top: calc(var(--header-height) + 24px);
  height: fit-content;
}

.sidebar-card {
  background: var(--content-surface);
  border: 1px solid var(--content-line);
  border-radius: 24px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: var(--content-shadow);
  transition: all var(--transition-normal);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--content-shadow-lg);
    border-color: var(--content-line-strong);
  }
}

.sidebar-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--content-line);
  color: var(--content-ink);
}

.author-card {
  .author-info {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    margin-bottom: 20px;
  }

  .author-avatar-lg {
    width: 86px;
    height: 86px;
    border-radius: var(--radius-full);
    margin-bottom: 16px;
    object-fit: cover;
    border: 3px solid transparent;
    background-image: linear-gradient(var(--color-card), var(--color-card)), 
                      linear-gradient(135deg, var(--color-google-blue) 0%, var(--color-ai-purple) 50%, var(--color-ai-pink) 100%);
    background-origin: border-box;
    background-clip: content-box, border-box;
    padding: 3px;
    box-shadow: 0 4px 16px rgba(26, 115, 232, 0.1);
    transition: transform var(--transition-normal);

    &:hover {
      transform: scale(1.05);
    }
  }

  .author-bio {
    font-size: 13.5px;
    color: var(--content-ink-2);
    margin-top: 6px;
    line-height: 1.5;
  }
}

.btn-full {
  width: 100%;
}

.related-articles {
  li {
    padding: 12px 0;
    border-bottom: 1px solid var(--content-line);

    &:last-child {
      border-bottom: none;
    }
  }
}

.related-link {
  font-size: 14px;
  color: var(--content-ink);
  line-height: 1.5;
  transition: color var(--transition-fast);

  &:hover {
    color: var(--color-primary);
  }
}

.not-found {
  text-align: center;
  padding: 80px 20px;

  h2 {
    font-size: 24px;
    color: var(--color-text);
    margin-bottom: 24px;
  }
}

@media (max-width: 1024px) {
  .article-layout {
    grid-template-columns: 1fr;
  }

  .article-sidebar {
    position: static;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
  }
}

@media (max-width: 768px) {
  .article-main {
    padding: 24px;
  }

  .article-title {
    font-size: 24px;
  }

  .article-sidebar {
    grid-template-columns: 1fr;
  }
}
</style>