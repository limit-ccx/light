<template>
  <div class="user-profile-page">
    <Header />
    <main class="main-content">
      <div class="container">
        <div class="profile-header">
          <div class="profile-avatar">
            <img :src="userInfo.avatar || defaultAvatar" alt="头像" />
          </div>
          <div class="profile-info">
            <h1 class="profile-name">{{ userInfo.username }}</h1>
            <div v-if="userInfo.creatorBadges?.length" class="profile-badges">
              <span
                v-for="badge in userInfo.creatorBadges"
                :key="badge"
                class="creator-badge"
              >
                {{ badge }}
              </span>
            </div>
            <p class="profile-bio">{{ userInfo.bio || '这个人很懒，什么都没有写' }}</p>
            <div class="profile-stats">
              <div class="stat-item">
                <span class="stat-value">{{ userInfo.articleCount || 0 }}</span>
                <span class="stat-label">文章</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ userInfo.followCount || 0 }}</span>
                <span class="stat-label">关注</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ userInfo.fansCount || 0 }}</span>
                <span class="stat-label">粉丝</span>
              </div>
            </div>
          </div>
          <div class="profile-actions">
            <button
              v-if="!isCurrentUser"
              class="btn-primary btn-sm"
              :class="{ 'btn-secondary': isFollowing }"
              @click="toggleFollow"
            >
              {{ isFollowing ? '取消关注' : '关注' }}
            </button>
            <router-link v-if="isCurrentUser" to="/profile" class="btn-secondary btn-sm">
              编辑资料
            </router-link>
          </div>
        </div>

        <div class="profile-tabs">
          <span
            class="tab-item"
            :class="{ active: activeTab === 'articles' }"
            @click="activeTab = 'articles'"
          >
            文章
          </span>
        </div>

        <div class="profile-content">
          <div class="tab-panel">
            <div class="article-list" v-loading="loading">
              <ArticleCard
                v-for="article in userArticles"
                :key="article.id"
                :article="article"
              />
              <div v-if="!loading && userArticles.length === 0" class="empty-state">
                <p>还没有发布文章</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
    <Footer />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import ArticleCard from '@/components/ArticleCard.vue'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const defaultAvatar = 'https://www.w3schools.com/w3images/avatar2.png'
const activeTab = ref('articles')
const userArticles = ref([])
const isFollowing = ref(false)

const userInfo = reactive({
  username: '',
  bio: '',
  avatar: '',
  creatorBadges: [],
  articleCount: 0,
  followCount: 0,
  fansCount: 0
})

const userId = computed(() => route.params.id)
const isCurrentUser = computed(() => authStore.user?.id === Number(userId.value))

onMounted(() => {
  loadUserProfile()
  loadUserArticles()
  if (!isCurrentUser.value && authStore.isLoggedIn) {
    loadFollowStatus()
  }
})

const loadUserProfile = async () => {
  loading.value = true
  try {
    const data = await request({
      url: `/api/users/${userId.value}`,
      method: 'get'
    })
    const user = data?.user || {}
    Object.assign(userInfo, {
      username: user.username || '',
      bio: user.bio || '',
      avatar: user.avatar || '',
      creatorBadges: user.creatorBadges || [],
      articleCount: data?.articleCount || 0,
      followCount: data?.followCount || 0,
      fansCount: data?.fansCount || 0
    })
  } catch (error) {
    console.error('加载用户信息失败:', error)
    ElMessage.error('用户不存在')
    router.push('/home')
  } finally {
    loading.value = false
  }
}

const loadUserArticles = async () => {
  try {
    const data = await request({
      url: `/api/articles/user/${userId.value}`,
      method: 'get',
      params: { page: 0, size: 20 }
    })
    userArticles.value = data?.list || []
  } catch (error) {
    console.error('加载用户文章失败:', error)
    userArticles.value = []
  }
}

const loadFollowStatus = async () => {
  try {
    const data = await request({
      url: `/api/follows/status/${userId.value}`,
      method: 'get'
    })
    isFollowing.value = data?.isFollowing || false
  } catch {
    isFollowing.value = false
  }
}

const toggleFollow = async () => {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    const data = await request({
      url: `/api/follows/${userId.value}`,
      method: 'post'
    })
    isFollowing.value = data?.isFollowing || false
    userInfo.fansCount = data?.followerCount || userInfo.fansCount
    ElMessage.success(isFollowing.value ? '关注成功' : '已取消关注')
  } catch {
    ElMessage.error('操作失败')
  }
}
</script>

<style lang="scss" scoped>
.user-profile-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(60% 36% at 88% 0%, var(--content-glow-b), transparent 62%),
    radial-gradient(50% 34% at 8% 10%, var(--content-glow-a), transparent 62%),
    var(--color-bg);
}

.main-content {
  flex: 1;
  padding: calc(var(--header-height) + 36px) 0 56px;
}

.profile-header {
  display: flex;
  gap: 32px;
  align-items: flex-start;
  margin-bottom: 24px;
  padding: 32px;
  border-radius: 28px;
  border: 1px solid var(--content-hero-line);
  background: var(--content-hero-bg);
  box-shadow: var(--content-hero-shadow);
}

.profile-avatar {
  width: 140px;
  height: 140px;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
    border: 4px solid var(--content-surface);
    box-shadow: 0 12px 28px rgba(32, 60, 120, 0.16);
  }
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-name {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--content-ink);
  line-height: 1.2;
}

.profile-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.creator-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  background: var(--content-chip);
  border: 1px solid var(--content-chip-line);
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
}

.profile-bio {
  font-size: 16px;
  color: var(--content-ink-2);
  margin-bottom: 24px;
  line-height: 1.6;
}

.profile-stats {
  display: flex;
  gap: 40px;
}

.stat-item {
  text-align: center;
  padding: 8px 0;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: var(--content-ink);
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 14px;
  color: var(--content-ink-3);
  margin-top: 4px;
}

.profile-actions {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
}

.profile-tabs {
  display: inline-flex;
  gap: 4px;
  margin-bottom: 24px;
  padding: 5px;
  border-radius: 999px;
  border: 1px solid var(--content-line);
  background: var(--content-tabs);
}

.tab-item {
  font-size: 14px;
  color: var(--content-ink-2);
  cursor: pointer;
  padding: 8px 20px;
  border-radius: 999px;
  transition: all var(--transition-fast);

  &:hover {
    color: var(--color-primary);
  }

  &.active {
    color: var(--color-primary);
    background: var(--content-surface);
    box-shadow: var(--content-shadow);
    font-weight: 500;
  }
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  border-radius: 24px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);

  p {
    font-size: 16px;
    color: var(--content-ink-2);
    margin-bottom: 16px;
  }
}

.btn-sm {
  padding: 8px 16px;
  font-size: 13px;
}

@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 24px;
    gap: 24px;
  }

  .profile-avatar {
    width: 120px;
    height: 120px;
  }

  .profile-name {
    font-size: 24px;
  }

  .profile-stats {
    justify-content: center;
    gap: 24px;
  }

  .profile-actions {
    width: 100%;
    justify-content: center;
  }
}
</style>