<template>
  <div class="profile-page">
    <Header />

    <main class="main-content">
      <div class="container">
        <div class="profile-header">
          <div class="profile-avatar">
            <img :src="currentAvatar" alt="头像" />
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
              <div class="stat-item">
                <span class="stat-value">{{ userInfo.knowledgeCount || 0 }}</span>
                <span class="stat-label">知识库</span>
              </div>
            </div>
          </div>
          <div class="profile-actions">
            <router-link to="/creator/apply" class="btn-secondary btn-sm">
              申请认证
            </router-link>
          </div>
        </div>

        <div class="profile-tabs">
          <span
            class="tab-item"
            :class="{ active: activeTab === 'articles' }"
            @click="activeTab = 'articles'"
          >
            我的文章
          </span>
          <span
            class="tab-item"
            :class="{ active: activeTab === 'favorites' }"
            @click="activeTab = 'favorites'"
          >
            我的收藏
          </span>
          <span
            class="tab-item"
            :class="{ active: activeTab === 'following' }"
            @click="activeTab = 'following'"
          >
            关注 {{ followStats.followingCount > 0 ? followStats.followingCount : '' }}
          </span>
          <span
            class="tab-item"
            :class="{ active: activeTab === 'followers' }"
            @click="activeTab = 'followers'"
          >
            粉丝 {{ followStats.followerCount > 0 ? followStats.followerCount : '' }}
          </span>
          <span
            class="tab-item"
            :class="{ active: activeTab === 'knowledge' }"
            @click="goToKnowledge"
          >
            我的知识库
          </span>
          <span
            class="tab-item"
            :class="{ active: activeTab === 'settings' }"
            @click="activeTab = 'settings'"
          >
            编辑资料
          </span>
        </div>

        <div class="profile-content">
          <div v-if="activeTab === 'articles'" class="tab-panel">
            <div class="article-list" v-loading="loading">
              <ArticleCard
                v-for="article in myArticles"
                :key="article.id"
                :article="article"
              />
              <div v-if="!loading && myArticles.length === 0" class="empty-state">
                <p>还没有发布文章</p>
                <router-link to="/create" class="btn-primary">写文章</router-link>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'favorites'" class="tab-panel">
            <div class="article-list" v-loading="loading">
              <ArticleCard
                v-for="article in myFavorites"
                :key="article.id"
                :article="article"
              />
              <div v-if="!loading && myFavorites.length === 0" class="empty-state">
                <p>还没有收藏文章</p>
                <router-link to="/articles" class="btn-primary">浏览文章</router-link>
              </div>
              <div v-if="favoritesHasMore" class="load-more">
                <button class="btn-secondary" :disabled="loadingMoreFavorites" @click="loadMoreFavorites">
                  {{ loadingMoreFavorites ? '加载中...' : '加载更多收藏' }}
                </button>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'following'" class="tab-panel">
            <div class="user-list" v-loading="loading">
              <div v-for="item in followingList" :key="item.id" class="user-list-item">
                <img :src="item.user?.avatar || defaultAvatar" class="user-list-avatar" />
                <div class="user-list-info">
                  <div class="user-list-name">{{ item.user?.username }}</div>
                  <div class="user-list-bio">{{ item.user?.bio || '这个人很懒，什么都没有写' }}</div>
                </div>
                <button class="btn-secondary btn-sm" @click="goToUserProfile(item.user?.id)">查看主页</button>
              </div>
              <div v-if="!loading && followingList.length === 0" class="empty-state">
                <p>还没有关注任何人</p>
                <router-link to="/articles" class="btn-primary">去发现作者</router-link>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'followers'" class="tab-panel">
            <div class="user-list" v-loading="loading">
              <div v-for="item in followerList" :key="item.id" class="user-list-item">
                <img :src="item.user?.avatar || defaultAvatar" class="user-list-avatar" />
                <div class="user-list-info">
                  <div class="user-list-name">{{ item.user?.username }}</div>
                  <div class="user-list-bio">{{ item.user?.bio || '这个人很懒，什么都没有写' }}</div>
                </div>
                <button class="btn-secondary btn-sm" @click="goToUserProfile(item.user?.id)">查看主页</button>
              </div>
              <div v-if="!loading && followerList.length === 0" class="empty-state">
                <p>还没有粉丝</p>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'settings'" class="tab-panel">
            <div class="settings-form">
              <div class="form-group avatar-panel">
                <div class="avatar-panel-head">
                  <div>
                    <label class="form-label">头像</label>
                    <p class="avatar-panel-sub">选择已拥有的头像，或上传自己的图片</p>
                  </div>
                  <div class="avatar-panel-actions">
                    <span class="avatar-points">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="9"></circle>
                        <path d="M12 7v10M9.5 9.5h5M9.5 14.5h5"></path>
                      </svg>
                      {{ avatarShop.points }} 积分
                    </span>
                    <router-link to="/avatar-shop" class="btn-secondary btn-sm">头像商城</router-link>
                  </div>
                </div>

                <div class="avatar-current">
                  <div class="avatar-current-img">
                    <img :src="editForm.avatar || currentAvatar" alt="当前头像" />
                    <span class="current-badge">当前</span>
                  </div>
                  <div class="avatar-current-copy">
                    <strong>{{ currentAvatarName }}</strong>
                    <p>{{ currentAvatarDesc }}</p>
                    <div class="avatar-current-actions">
                      <button type="button" class="btn-upload" @click="triggerFileInput" :disabled="uploading">
                        {{ uploading ? '上传中...' : '上传本地图片' }}
                      </button>
                      <span class="upload-tip">JPG / PNG，最大 10MB</span>
                    </div>
                  </div>
                </div>

                <div class="owned-head">
                  <span>我的头像</span>
                  <span v-if="ownedAvatars.length" class="owned-count">{{ ownedAvatars.length }} 个商城头像</span>
                </div>

                <div class="owned-grid">
                  <button
                    v-if="customAvatarTile"
                    type="button"
                    class="owned-avatar"
                    :class="{ selected: editForm.avatar === customAvatarTile.imageUrl }"
                    @click="selectAvatar(customAvatarTile.imageUrl)"
                  >
                    <img :src="customAvatarTile.imageUrl" alt="自定义头像" />
                    <span class="owned-name">自定义</span>
                    <span v-if="editForm.avatar === customAvatarTile.imageUrl" class="owned-check">✓</span>
                  </button>

                  <button
                    v-for="avatar in ownedAvatars"
                    :key="avatar.id"
                    type="button"
                    class="owned-avatar"
                    :class="{ selected: editForm.avatar === avatar.imageUrl }"
                    @click="selectAvatar(avatar.imageUrl)"
                  >
                    <img :src="avatar.imageUrl" :alt="avatar.name" loading="lazy" />
                    <span class="owned-name">{{ avatar.name }}</span>
                    <span v-if="avatar.equipped" class="owned-label">使用中</span>
                    <span v-if="editForm.avatar === avatar.imageUrl" class="owned-check">✓</span>
                  </button>

                  <router-link to="/avatar-shop" class="owned-avatar shop-tile">
                    <span class="shop-plus">+</span>
                    <span class="owned-name">头像商城</span>
                  </router-link>
                </div>

                <p v-if="!ownedAvatars.length" class="avatar-empty-hint">
                  还没有商城头像，去商城用 {{ avatarShop.price }} 积分兑换一个吧。
                </p>
                <p class="avatar-select-hint">选择头像后，点击页面底部“保存修改”生效。</p>

                <input
                  type="file"
                  ref="fileInput"
                  accept="image/*"
                  style="display: none"
                  @change="handleFileChange"
                />
                <div class="avatar-preview" v-if="previewUrl">
                  <img :src="previewUrl" alt="预览" />
                  <span class="preview-label">本地预览，保存后生效</span>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">用户名</label>
                <input v-model="editForm.username" type="text" class="input-field" />
              </div>
              <div class="form-group">
                <label class="form-label">邮箱</label>
                <input v-model="editForm.email" type="email" class="input-field" disabled />
              </div>
              <div class="form-group">
                <label class="form-label">个人简介</label>
                <textarea v-model="editForm.bio" class="textarea-field" rows="4" placeholder="介绍一下自己..."></textarea>
              </div>

              <div class="inner-voice-entry">
                <span class="inner-voice-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 11.5a8.4 8.4 0 0 1-9 8.4 8.5 8.5 0 0 1-3.4-.7L3 21l1.8-4.6A8.4 8.4 0 0 1 3.5 11 8.5 8.5 0 0 1 12 3a8.4 8.4 0 0 1 9 8.5z"></path>
                    <path d="M8.5 10.5h7M8.5 13.5h4.5"></path>
                  </svg>
                </span>
                <div class="inner-voice-copy">
                  <strong>心声</strong>
                  <p>从推荐里挑，也可以写下自己的心声，最多选 {{ MAX_INNER_VOICES }} 条轮播</p>
                </div>
                <button type="button" class="btn-secondary btn-sm" @click="innerVoiceStore.openManager()">
                  管理心声
                </button>
              </div>

              <div class="form-actions">
                <button class="btn-primary" @click="saveProfile" :disabled="saving">
                  {{ saving ? '保存中...' : '保存修改' }}
                </button>
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import ArticleCard from '@/components/ArticleCard.vue'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getMyFavorites } from '@/api/article'
import { getKnowledgeCount } from '@/api/knowledge'
import { getAvatarShop } from '@/api/avatarShop'
import { MAX_INNER_VOICES, useInnerVoiceStore } from '@/store/innerVoice'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const innerVoiceStore = useInnerVoiceStore()
const router = useRouter()

const loading = ref(false)
const activeTab = ref('articles')
const defaultAvatar = 'https://www.w3schools.com/w3images/avatar2.png'
const saving = ref(false)
const uploading = ref(false)
const followingList = ref([])
const followerList = ref([])
const followStats = ref({ followingCount: 0, followerCount: 0 })
const previewUrl = ref('')
const fileInput = ref(null)
const avatarShop = ref({
  points: 0,
  price: 1000,
  ownedCount: 0,
  avatars: []
})

const ownedAvatars = computed(() =>
  (avatarShop.value.avatars || []).filter((avatar) => avatar.owned)
)

const currentShopAvatar = computed(() =>
  (avatarShop.value.avatars || []).find((avatar) => avatar.imageUrl === editForm.avatar)
)

const currentAvatarName = computed(() => {
  if (currentShopAvatar.value) return currentShopAvatar.value.name
  if (editForm.avatar && editForm.avatar !== defaultAvatar) return '自定义头像'
  return '默认头像'
})

const currentAvatarDesc = computed(() => {
  if (currentShopAvatar.value) return `商城头像 · ${currentShopAvatar.value.tagline}`
  if (editForm.avatar && editForm.avatar !== defaultAvatar) return '来自本地上传或外链图片'
  return '还没有设置头像'
})

const customAvatarTile = computed(() => {
  const avatarUrl = userInfo.avatar
  if (!avatarUrl || avatarUrl === defaultAvatar) return null
  if (ownedAvatars.value.some((avatar) => avatar.imageUrl === avatarUrl)) return null
  return { imageUrl: avatarUrl }
})

const selectAvatar = (avatar) => {
  editForm.avatar = avatar
  previewUrl.value = '' // 清除本地预览
}

// 触发文件选择
const triggerFileInput = () => {
  fileInput.value.click()
}

// 处理文件选择
const handleFileChange = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 校验文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }

  // 校验文件大小 (10MB)
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    return
  }

  // 显示本地预览
  const reader = new FileReader()
  reader.onload = (e) => {
    previewUrl.value = e.target.result
  }
  reader.readAsDataURL(file)

  // 上传文件
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)

    const avatarUrl = await request({
      url: '/api/upload/avatar',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    // request 拦截器已解包，avatarUrl 直接就是 URL 字符串
    if (avatarUrl && typeof avatarUrl === 'string') {
      editForm.avatar = avatarUrl
      userInfo.avatar = avatarUrl // 立即更新页面头像显示
      ElMessage.success('头像上传成功，请点击"保存修改"生效')
    } else {
      ElMessage.error('上传返回数据异常')
      previewUrl.value = ''
    }
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('头像上传失败，请重试')
    previewUrl.value = ''
  } finally {
    uploading.value = false
    // 清空文件输入，允许重复选择同一文件
    event.target.value = ''
  }
}

const userInfo = reactive({
  username: '',
  email: '',
  bio: '',
  avatar: '',
  creatorBadges: [],
  articleCount: 0,
  followCount: 0,
  fansCount: 0,
  knowledgeCount: 0
})

const editForm = reactive({
  username: '',
  email: '',
  bio: '',
  avatar: ''
})

const myArticles = ref([])
const myFavorites = ref([])
const favoritesPage = ref(0)
const favoritesHasMore = ref(false)
const loadingMoreFavorites = ref(false)

const currentAvatar = computed(() => userInfo.avatar || defaultAvatar)

onMounted(() => {
  if (!authStore.isLoggedIn) {
    return
  }
  loadProfile()
  loadMyArticles()
  loadFollowStats()
  loadAvatarShop()
})

watch(activeTab, (tab) => {
  if (tab === 'articles') {
    loadMyArticles()
  } else if (tab === 'favorites') {
    loadMyFavorites()
  } else if (tab === 'following') {
    loadFollowing()
  } else if (tab === 'followers') {
    loadFollowers()
  }
})

const loadProfile = async () => {
  try {
    const response = await request({
      url: '/api/users/profile',
      method: 'get'
    })

    const data = response

    // 加载知识库数量
    let knowledgeCount = 0
    try {
      const kc = await getKnowledgeCount()
      knowledgeCount = kc.count || 0
    } catch (e) {
      // 忽略错误
    }

    Object.assign(userInfo, {
      username: data.user?.username || '',
      email: data.user?.email || '',
      bio: data.user?.bio || '',
      avatar: data.user?.avatar || '',
      creatorBadges: data.user?.creatorBadges || [],
      articleCount: data.articleCount || 0,
      followCount: data.followCount || 0,
      fansCount: data.fansCount || 0,
      knowledgeCount: knowledgeCount
    })

    Object.assign(editForm, {
      username: userInfo.username,
      email: userInfo.email,
      bio: userInfo.bio,
      avatar: userInfo.avatar
    })
  } catch (error) {
    console.error('加载个人信息失败:', error)
  }
}

const goToKnowledge = () => {
  router.push('/knowledge')
}

const loadMyArticles = async () => {
  if (!authStore.isLoggedIn) return

  loading.value = true
  try {
    const response = await request({
      url: '/api/users/profile',
      method: 'get'
    })
    
    userInfo.articleCount = response.articleCount || 0
    userInfo.followCount = response.followCount || 0
    userInfo.fansCount = response.fansCount || 0
    
    const userId = response.user?.id || authStore.user?.id
    if (!userId) {
      throw new Error('无法获取用户信息')
    }
    const page = await request({
      url: `/api/articles/user/${userId}`,
      method: 'get',
      params: { page: 0, size: 20 }
    })
    myArticles.value = Array.isArray(page?.list) ? page.list : []
  } catch (error) {
    console.error('加载我的文章失败:', error)
    myArticles.value = []
  } finally {
    loading.value = false
  }
}

const loadMyFavorites = async (reset = true) => {
  if (!authStore.isLoggedIn) return

  if (reset) {
    favoritesPage.value = 0
    myFavorites.value = []
    favoritesHasMore.value = false
  }

  loading.value = true
  try {
    const pageData = await getMyFavorites(favoritesPage.value, 20)
    const list = Array.isArray(pageData?.list) ? pageData.list : []
    myFavorites.value = reset ? list : myFavorites.value.concat(list)
    favoritesHasMore.value = favoritesPage.value + 1 < (pageData?.totalPages || 0)
  } catch (error) {
    console.error('加载收藏失败:', error)
    if (reset) myFavorites.value = []
  } finally {
    loading.value = false
  }
}

const loadMoreFavorites = async () => {
  if (!favoritesHasMore.value || loadingMoreFavorites.value) return
  loadingMoreFavorites.value = true
  favoritesPage.value += 1
  try {
    await loadMyFavorites(false)
  } finally {
    loadingMoreFavorites.value = false
  }
}

const loadFollowing = async () => {
  if (!authStore.isLoggedIn) return
  loading.value = true
  try {
    const res = await request({ url: '/api/follows/following', method: 'get', params: { page: 0, size: 100 } })
    followingList.value = res?.list || []
  } catch {
    followingList.value = []
  } finally {
    loading.value = false
  }
}

const loadFollowers = async () => {
  if (!authStore.isLoggedIn) return
  loading.value = true
  try {
    const res = await request({ url: '/api/follows/followers', method: 'get', params: { page: 0, size: 100 } })
    followerList.value = res?.list || []
  } catch {
    followerList.value = []
  } finally {
    loading.value = false
  }
}

const loadFollowStats = async () => {
  if (!authStore.isLoggedIn) return
  try {
    const res = await request({ url: `/api/follows/status/${authStore.user?.id}`, method: 'get' })
    followStats.value = {
      followingCount: res?.followingCount || 0,
      followerCount: res?.followerCount || 0
    }
  } catch {
    followStats.value = { followingCount: 0, followerCount: 0 }
  }
}

const goToUserProfile = (userId) => {
  router.push(`/user/${userId}`)
}

watch(activeTab, (tab) => {
  if (tab === 'following') loadFollowing()
  if (tab === 'followers') loadFollowers()
  if (tab === 'settings') loadAvatarShop()
})

const loadAvatarShop = async () => {
  if (!authStore.isLoggedIn) return
  try {
    const data = await getAvatarShop()
    avatarShop.value = {
      points: data.points || 0,
      price: data.price || 1000,
      ownedCount: data.ownedCount || 0,
      avatars: Array.isArray(data.avatars) ? data.avatars : []
    }
  } catch {
    // 商城加载失败时不影响资料编辑
  }
}

const saveProfile = async () => {
  saving.value = true
  try {
    await request({
      url: '/api/users/profile',
      method: 'put',
      data: {
        username: editForm.username,
        bio: editForm.bio,
        avatar: editForm.avatar
      }
    })

    userInfo.username = editForm.username
    userInfo.bio = editForm.bio
    userInfo.avatar = editForm.avatar

    authStore.updateUser({
      username: editForm.username,
      bio: editForm.bio,
      avatar: editForm.avatar
    })

    await loadAvatarShop()
    ElMessage.success('个人资料更新成功')
  } catch (error) {
    ElMessage.error('更新失败')
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
@use "@/styles/tech-button-mixins.scss" as *;

.profile-page {
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
  position: relative;
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

  .avatar-edit-btn {
    position: absolute;
    bottom: 0;
    right: 0;
    width: 36px;
    height: 36px;
    background: var(--color-primary);
    color: white;
    border-radius: 50%;
    @include flex-center;
    transition: background var(--transition-fast);

    &:hover {
      background: var(--color-primary-dark);
    }
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
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 24px;
  padding: 5px;
  border-radius: 999px;
  border: 1px solid var(--content-line);
  background: var(--content-tabs);
  width: fit-content;
  max-width: 100%;
}

.tab-item {
  font-size: 14px;
  color: var(--content-ink-2);
  cursor: pointer;
  padding: 8px 18px;
  border-radius: 999px;
  white-space: nowrap;
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

.tab-panel {
  min-height: 300px;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-list-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-radius: 20px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
  transition: transform var(--transition-fast), border-color var(--transition-fast);

  &:hover {
    transform: translateX(3px);
    border-color: var(--content-line-strong);
  }
}

.user-list-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.user-list-info {
  flex: 1;
  min-width: 0;
}

.user-list-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--content-ink);
}

.user-list-bio {
  font-size: 13px;
  color: var(--content-ink-2);
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.btn-sm {
  padding: 8px 16px;
  font-size: 13px;
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

.settings-form {
  max-width: 560px;
  padding: clamp(22px, 3vw, 32px);
  border-radius: 24px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
}

.inner-voice-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);
}

.inner-voice-icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(135deg, #1a73e8, #8b5cf6);
  box-shadow: 0 10px 20px rgba(70, 90, 220, 0.22);
}

.inner-voice-copy {
  flex: 1;
  min-width: 0;

  strong {
    display: block;
    margin-bottom: 3px;
    color: var(--content-ink);
    font-size: 14.5px;
    font-weight: 600;
  }

  p {
    margin: 0;
    color: var(--content-ink-3);
    font-size: 12.5px;
    line-height: 1.5;
  }
}

.form-group {
  margin-bottom: 24px;
}

.avatar-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  .form-label {
    margin-bottom: 3px;
  }
}

.avatar-panel-sub {
  margin: 0;
  color: var(--content-ink-3);
  font-size: 12px;
}

.avatar-panel-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.avatar-points {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 9px;
  border-radius: 999px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--content-ink-2);
  font-size: 11.5px;
  white-space: nowrap;

  svg {
    color: #d97706;
  }
}

.avatar-current {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--content-line);
  background:
    radial-gradient(120% 100% at 100% 0%, var(--content-glow-b), transparent 58%),
    var(--content-surface-tint);
}

.avatar-current-img {
  position: relative;
  flex-shrink: 0;
  width: 82px;
  height: 82px;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
    border: 3px solid var(--content-surface);
    box-shadow: 0 10px 22px rgba(32, 60, 120, 0.16);
  }
}

.current-badge {
  position: absolute;
  left: 50%;
  bottom: -2px;
  transform: translateX(-50%);
  padding: 2px 8px;
  border-radius: 999px;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  white-space: nowrap;
  background: linear-gradient(120deg, #1a73e8, #6f5cf6);
  box-shadow: 0 4px 10px rgba(47, 99, 235, 0.28);
}

.avatar-current-copy {
  flex: 1;
  min-width: 0;

  strong {
    display: block;
    margin-bottom: 4px;
    color: var(--content-ink);
    font-size: 16px;
    font-weight: 600;
  }

  p {
    margin: 0 0 10px;
    color: var(--content-ink-2);
    font-size: 12.5px;
    line-height: 1.5;
  }
}

.avatar-current-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.btn-upload {
  height: 34px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--content-ink-2);
  font-family: inherit;
  font-size: 12.5px;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover:not(:disabled) {
    border-color: var(--content-line-strong);
    color: var(--color-primary);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.upload-tip {
  color: var(--content-ink-3);
  font-size: 11.5px;
}

.owned-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 20px 0 10px;
  color: var(--content-ink);
  font-size: 13px;
  font-weight: 600;
}

.owned-count {
  color: var(--content-ink-3);
  font-size: 11.5px;
  font-weight: 400;
}

.owned-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.owned-avatar {
  position: relative;
  overflow: hidden;
  padding: 0;
  border-radius: 14px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);
  font-family: inherit;
  cursor: pointer;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast);

  &:hover {
    transform: translateY(-1px);
    border-color: var(--content-line-strong);
  }

  &.selected {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 2px var(--content-glow-a);
  }

  img {
    width: 100%;
    aspect-ratio: 1;
    display: block;
    object-fit: cover;
  }

  .owned-name {
    display: block;
    padding: 6px 4px;
    overflow: hidden;
    color: var(--content-ink-2);
    font-size: 11px;
    text-align: center;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .owned-label {
    position: absolute;
    top: 5px;
    left: 5px;
    padding: 2px 6px;
    border-radius: 999px;
    color: #fff;
    font-size: 9.5px;
    font-weight: 600;
    background: rgba(16, 185, 129, 0.92);
  }

  .owned-check {
    position: absolute;
    top: 5px;
    right: 5px;
    display: grid;
    place-items: center;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    color: #fff;
    font-size: 11px;
    font-weight: 700;
    background: linear-gradient(135deg, #1a73e8, #8b5cf6);
    box-shadow: 0 4px 10px rgba(47, 99, 235, 0.28);
  }
}

.shop-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  border-style: dashed;
  border-color: var(--content-line-strong);
  color: var(--color-primary);
  text-decoration: none;

  .shop-plus {
    margin-top: 6px;
    font-size: 24px;
    font-weight: 300;
    line-height: 1;
  }

  .owned-name {
    color: var(--color-primary);
  }
}

.avatar-empty-hint,
.avatar-select-hint {
  margin: 10px 0 0;
  color: var(--content-ink-3);
  font-size: 11.5px;
  line-height: 1.5;
}

.avatar-select-hint {
  margin-top: 6px;
}

.avatar-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;

  img {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    object-fit: cover;
    border: 1px solid var(--content-line);
  }

  .preview-label {
    color: var(--content-ink-3);
    font-size: 11.5px;
  }
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--content-ink);
}

.input-field {
  width: 100%;
  height: 44px;
  padding: 0 16px;
  border: 1px solid var(--content-chip-line);
  border-radius: 12px;
  background: var(--content-surface-tint);
  color: var(--content-ink);
  font-family: inherit;
  font-size: 14px;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);

  &:focus {
    outline: none;
    border-color: var(--content-line-strong);
    box-shadow: 0 0 0 3px var(--content-glow-a);
  }

  &:disabled {
    background: var(--color-bg-soft);
    color: var(--content-ink-3);
  }
}

.textarea-field {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--content-chip-line);
  border-radius: 12px;
  background: var(--content-surface-tint);
  color: var(--content-ink);
  font-family: inherit;
  font-size: 14px;
  resize: vertical;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);

  &:focus {
    outline: none;
    border-color: var(--content-line-strong);
    box-shadow: 0 0 0 3px var(--content-glow-a);
  }
}

.form-actions {
  padding-top: 16px;
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

  .profile-tabs {
    flex-wrap: nowrap;
    gap: 4px;
    width: 100%;
    max-width: 100%;
    padding: 8px;
    overflow-x: auto;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  .tab-item {
    flex: 0 0 auto;
    font-size: 13px;
    padding: 8px 12px;
  }

  .avatar-panel-head {
    flex-direction: column;
  }

  .avatar-panel-actions {
    width: 100%;
    justify-content: space-between;
  }

  .avatar-current {
    padding: 14px;
    gap: 12px;
  }

  .avatar-current-img {
    width: 72px;
    height: 72px;
  }

  .owned-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
.load-more {
  margin: 20px 0;
  display: flex;
  justify-content: center;
}
</style>