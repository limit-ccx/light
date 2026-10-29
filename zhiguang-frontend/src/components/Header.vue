<template>
  <header class="header" :class="{ 'header-scrolled': isScrolled }">
    <div class="header-container container">
      <router-link to="/home" class="logo">
        <span class="logo-icon">
          <img :src="logoSrc" alt="" aria-hidden="true" draggable="false" />
        </span>
        <span class="logo-text">纸域空间</span>
      </router-link>

      <nav class="nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
        >
          {{ item.label }}
        </router-link>
      </nav>

      <div class="header-actions">
        <div class="search-box">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索文章..."
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <button class="search-btn" @click="handleSearch" aria-label="搜索">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.35-4.35"/>
            </svg>
          </button>
        </div>

        <template v-if="authStore.isLoggedIn">
          <router-link to="/create" class="write-btn btn-primary">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14"/>
            </svg>
            <span>写文章</span>
          </router-link>

          <!-- 私信 -->
          <router-link to="/messages" class="message-btn" :class="{ 'has-unread': unreadMessageCount > 0 }">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            <span v-if="unreadMessageCount > 0" class="notification-badge">{{ unreadMessageCount > 99 ? '99+' : unreadMessageCount }}</span>
          </router-link>

          <!-- 通知 -->
          <el-dropdown trigger="click" class="notification-dropdown" @visible-change="onNotificationVisible">
            <div class="notification-btn" :class="{ 'has-unread': unreadCount > 0 }">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
              </svg>
              <span v-if="unreadCount > 0" class="notification-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="notification-menu">
                <div class="notification-header">
                  <span>消息通知</span>
                  <el-button v-if="notifications.length > 0" link size="small" @click="markAllRead">全部已读</el-button>
                </div>
                <div v-if="notifications.length === 0" class="notification-empty">暂无消息</div>
                <div
                  v-for="n in notifications.slice(0, 8)"
                  :key="n.id"
                  class="notification-item-wrapper"
                  :class="{ unread: !n.isRead }"
                >
                  <div class="notification-item" @click="goToArticle(n.articleId)">
                    <div class="notification-icon">
                      <svg v-if="n.type === 'LIKE'" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                      </svg>
                      <svg v-else-if="n.type === 'COMMENT'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                      </svg>
                      <svg v-else-if="n.type === 'FOLLOW'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                        <circle cx="8.5" cy="7" r="4"/>
                        <line x1="20" y1="8" x2="20" y2="14"/>
                        <line x1="23" y1="11" x2="17" y2="11"/>
                      </svg>
                      <svg v-else-if="n.type === 'MESSAGE'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                        <polyline points="22,6 12,13 2,6"/>
                      </svg>
                      <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"/>
                        <line x1="12" y1="16" x2="12" y2="12"/>
                        <line x1="12" y1="8" x2="12.01" y2="8"/>
                      </svg>
                    </div>
                    <div class="notification-content-wrapper">
                      <div class="notification-content">{{ n.content }}</div>
                      <div class="notification-time">{{ formatTime(n.createdAt) }}</div>
                    </div>
                    <el-button
                      class="notification-delete-btn"
                      type="danger"
                      size="small"
                      text
                      @click.stop="handleDeleteNotification(n.id)"
                    >
                      删除
                    </el-button>
                  </div>
                </div>
                <div v-if="notifications.length > 0" class="notification-footer">
                  <router-link to="/profile">查看全部</router-link>
                </div>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-avatar" :class="{ 'avatar-letter': !userAvatar }">
              <img v-if="userAvatar" :src="userAvatar" alt="用户头像" />
              <span v-else class="avatar-fallback">{{ userInitial }}</span>
              <span class="avatar-status"></span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">{{ authStore.user?.username || '用户' }}</el-dropdown-item>
                <el-dropdown-item command="articles">我的文章</el-dropdown-item>
                <el-dropdown-item command="knowledge">我的知识库</el-dropdown-item>
                <el-dropdown-item command="growth">成长中心</el-dropdown-item>
                <el-dropdown-item command="sign-in">每日签到</el-dropdown-item>
                <el-dropdown-item command="avatar-shop">头像商城</el-dropdown-item>
                <el-dropdown-item v-if="authStore.isAdmin" command="admin" divided>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 4px;"><path d="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z"/></svg>
                  运营后台
                </el-dropdown-item>
                <el-dropdown-item command="settings">设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login" class="btn-text">登录</router-link>
          <router-link to="/register" class="btn-primary">注册</router-link>
        </template>

        <button class="theme-toggle-btn" @click="themeStore.toggleTheme()" :aria-label="themeStore.isDark() ? '切换到亮色模式' : '切换到暗色模式'">
          <!-- 太阳图标 (暗色模式下显示，点击切换到亮色) -->
          <svg v-if="themeStore.isDark()" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="5"/>
            <line x1="12" y1="1" x2="12" y2="3"/>
            <line x1="12" y1="21" x2="12" y2="23"/>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
            <line x1="1" y1="12" x2="3" y2="12"/>
            <line x1="21" y1="12" x2="23" y2="12"/>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
          </svg>
          <!-- 月亮图标 (亮色模式下显示，点击切换到暗色) -->
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useThemeStore } from '@/store/theme'
import { ElMessage } from 'element-plus'
import { getNotifications, getUnreadCount, markAllAsRead, deleteNotification } from '@/api/notification'
import { getUnreadMessageCount } from '@/api/message'
import { useNotificationStream } from '@/composables/useNotificationStream'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const logoSrc = computed(() =>
  themeStore.isDark() ? '/brand/logo-dark.png' : '/brand/logo-light.png'
)

const searchQuery = ref('')
const isScrolled = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
const unreadMessageCount = ref(0)
let notificationTimer = null
const sseConnected = ref(false)
let notificationStreamControls = null

const navItems = [
  { path: '/home', label: '首页' },
  { path: '/articles', label: '文章' },
  { path: '/columns', label: '专栏' },
  { path: '/collections', label: '合集' },
  { path: '/knowledge-graph', label: '知识图谱' },
  { path: '/tree-hole', label: '树洞' },
  { path: '/about', label: '关于' }
]

const userAvatar = computed(() => {
  const avatar = authStore.user?.avatar
  if (!avatar) return ''
  return avatar
})

const userInitial = computed(() => {
  const username = authStore.user?.username || 'U'
  return username.charAt(0).toUpperCase()
})

// 先定义函数，再在 watch 中调用（解决 immediate 执行时函数未定义的问题）
const fetchUnreadCount = async () => {
  if (!authStore.isLoggedIn) return
  try {
    const data = await getUnreadCount()
    unreadCount.value = data?.count || 0
  } catch {
    unreadCount.value = 0
  }
}

const fetchUnreadMessageCount = async () => {
  if (!authStore.isLoggedIn) return
  try {
    const data = await getUnreadMessageCount()
    unreadMessageCount.value = data?.count || 0
  } catch {
    unreadMessageCount.value = 0
  }
}

const startPolling = () => {
  if (notificationTimer) return
  notificationTimer = setInterval(() => {
    fetchUnreadCount()
    fetchUnreadMessageCount()
  }, 30000)
}

const stopPolling = () => {
  if (notificationTimer) {
    clearInterval(notificationTimer)
    notificationTimer = null
  }
}

const startNotificationStream = () => {
  if (notificationStreamControls) return
  notificationStreamControls = useNotificationStream({
    autoConnect: false,
    onEvent: (type) => {
      if (type === 'NEW_NOTIFICATION' || type === 'NEW_MESSAGE') {
        fetchUnreadCount()
        fetchUnreadMessageCount()
      }
    },
    onConnected: () => {
      sseConnected.value = true
      stopPolling()
      fetchUnreadCount()
      fetchUnreadMessageCount()
    },
    onDisconnected: () => {
      sseConnected.value = false
      startPolling()
    }
  })
  notificationStreamControls.connect()
}

const stopNotificationStream = () => {
  if (notificationStreamControls) {
    notificationStreamControls.close()
    notificationStreamControls = null
  }
  sseConnected.value = false
}

const handleUnreadRefresh = () => {
  fetchUnreadCount()
  fetchUnreadMessageCount()
}

watch(() => authStore.isLoggedIn, async (isLoggedIn) => {
  if (isLoggedIn && !authStore.user) {
    try {
      await authStore.fetchProfile()
    } catch {
      // 失败时由 store 清理登录态
    }
  }
  if (isLoggedIn) {
    fetchUnreadCount()
    fetchUnreadMessageCount()
    startPolling()
    startNotificationStream()
  } else {
    stopPolling()
    stopNotificationStream()
    unreadCount.value = 0
    unreadMessageCount.value = 0
    notifications.value = []
  }
}, { immediate: true })

const fetchNotifications = async () => {
  try {
    const data = await getNotifications()
    notifications.value = data?.list || []
  } catch {
    notifications.value = []
  }
}

const onNotificationVisible = (visible) => {
  if (visible) {
    fetchNotifications()
  }
}

const markAllRead = async () => {
  try {
    await markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
    unreadCount.value = 0
    ElMessage.success('全部标记为已读')
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleDeleteNotification = async (id) => {
  try {
    await deleteNotification(id)
    notifications.value = notifications.value.filter(n => n.id !== id)
    ElMessage.success('通知已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

const goToArticle = (articleId) => {
  if (articleId) {
    router.push(`/articles/${articleId}`)
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = (now - date) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 604800) return `${Math.floor(diff / 86400)}天前`
  return date.toLocaleDateString('zh-CN')
}

const handleScroll = () => {
  isScrolled.value = window.scrollY > 0
}

const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    if (authStore.isLoggedIn) {
      fetchUnreadCount()
      fetchUnreadMessageCount()
      if (!sseConnected.value) {
        startPolling()
      }
    }
  } else {
    stopPolling()
  }
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('unread-refresh', handleUnreadRefresh)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('unread-refresh', handleUnreadRefresh)
  stopPolling()
  stopNotificationStream()
})

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { q: searchQuery.value } })
  }
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
    case 'articles':
    case 'settings':
      router.push('/profile')
      break
    case 'knowledge':
      router.push('/knowledge')
      break
    case 'growth':
      router.push('/growth-center')
      break
    case 'sign-in':
      router.push('/sign-in')
      break
    case 'avatar-shop':
      router.push('/avatar-shop')
      break
    case 'admin':
      router.push('/admin')
      break;
    case 'logout':
      authStore.logout()
      ElMessage.success('已退出登录')
      router.push('/home')
      break
  }
}
</script>

<style lang="scss" scoped>
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  height: var(--header-height);
  background: transparent;
  transition: all var(--transition-normal);
  border-bottom: 1px solid transparent;

  &.header-scrolled {
    @include liquid-glass;
    height: calc(var(--header-height) - 16px);
  }
}

.header-container {
  height: 100%;
  @include flex-between;
  flex-wrap: nowrap;
  min-width: 0;
  overflow: visible;
  padding: 0 16px;
}

.logo {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 500; /* 谷歌风格标题较细较柔和 */
  color: var(--color-text);
  letter-spacing: -0.01em;

  .logo-icon {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    box-shadow: 0 4px 14px rgba(59, 130, 246, 0.22);
    transition: transform var(--transition-normal), box-shadow var(--transition-normal);

    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
      transition: transform var(--transition-normal);
    }
  }

  &:hover .logo-icon {
    transform: translateY(-1px);
    box-shadow: 0 8px 20px rgba(59, 130, 246, 0.32);

    img {
      transform: scale(1.06);
    }
  }

  .logo-text {
    font-family: var(--font-serif);
    font-size: 22px;
    font-weight: 600;
    letter-spacing: 0.1em;
    background: linear-gradient(135deg, var(--color-text), var(--color-primary));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.nav {
  display: flex;
  gap: 4px;
  flex-shrink: 1;
  flex-wrap: nowrap;
  min-width: 0;
  overflow: visible;

  .nav-item {
    padding: 8px 14px;
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text-secondary);
    border: 1px solid transparent;
    border-radius: var(--radius-full); /* 胶囊型导航项 */
    transition: all var(--transition-fast);
    position: relative;
    white-space: nowrap;
    flex-shrink: 0;

    &:hover {
      color: var(--color-text);
      background: var(--color-google-blue-light);
      border-color: rgba(147, 197, 253, 0.4);
    }

    /* 选中态：深色玻璃 + 电光描边 */
    &.router-link-active {
      color: #fff;
      font-weight: 600;
      background:
        linear-gradient(180deg, rgba(24, 33, 54, 0.8) 0%, rgba(11, 18, 33, 0.86) 100%) padding-box,
        linear-gradient(100deg, #22d3ee 0%, #3b82f6 34%, #8b5cf6 68%, #e879f9 100%) border-box;
      border-color: transparent;
      box-shadow:
        0 0 0 1px rgba(59, 130, 246, 0.16),
        0 6px 18px rgba(37, 99, 235, 0.26),
        0 0 16px rgba(139, 92, 246, 0.18);

      &:hover {
        filter: brightness(1.2);
        box-shadow:
          0 0 0 1px rgba(96, 165, 250, 0.28),
          0 10px 24px rgba(37, 99, 235, 0.32),
          0 0 22px rgba(168, 85, 247, 0.28);
      }
    }
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  flex-wrap: nowrap;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;

  .search-input {
    width: 180px;
    height: 40px;
    padding: 0 16px 0 40px;
    font-size: 14px;
    background: var(--color-bg-elevated);
    border: 1px solid transparent;
    border-radius: var(--radius-full); /* Pill shape */
    color: var(--color-text);
    transition: all var(--transition-normal);

    &:hover {
      background: var(--color-card-hover);
    }

    &:focus {
      background: var(--color-card);
      border-color: transparent;
      box-shadow: 0 1px 3px rgba(32, 33, 36, 0.15), 0 1px 8px rgba(32, 33, 36, 0.08);
      outline: none;
    }
  }

  .search-btn {
    position: absolute;
    left: 12px; /* 移到左侧更符合谷歌风格 */
    width: 24px;
    height: 24px;
    @include flex-center;
    color: var(--color-text-muted);
    border-radius: var(--radius-full);
    transition: all var(--transition-fast);
    cursor: pointer;

    &:hover {
      color: var(--color-primary);
    }
  }
}

.write-btn {
  height: 38px;
  padding: 0 18px;
  font-size: 13px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 6px;

  /* 导航栏 CTA 太窄，去掉科技主按钮的指示点与右侧图标区 */
  &::before,
  &::after {
    display: none;
  }

  svg {
    transition: transform var(--transition-fast);
  }

  &:hover svg {
    transform: rotate(90deg);
  }
}

.user-avatar {
  position: relative;
  width: 38px;
  height: 38px;
  border-radius: var(--radius-full);
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    border-color: var(--color-primary-light);
    box-shadow: var(--shadow-sm);
  }

  &.avatar-letter {
    background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
    color: #fff;
    font-weight: 600;
    font-size: 16px;
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .avatar-fallback {
    line-height: 1;
  }

  .avatar-status {
    position: absolute;
    bottom: 0;
    right: 0;
    width: 10px;
    height: 10px;
    background: var(--color-success);
    border: 2px solid var(--color-card);
    border-radius: var(--radius-full);
  }
}

.theme-toggle-btn {
  width: 38px;
  height: 38px;
  @include flex-center;
  color: var(--color-text-muted);
  border-radius: var(--radius-full);
  transition: all var(--transition-normal);
  cursor: pointer;
  background: transparent;
  border: 1px solid var(--color-border);

  svg {
    transition: transform var(--transition-normal);
  }

  &:hover {
    color: var(--color-primary);
    background: var(--color-google-blue-light);
    border-color: var(--color-primary-light);
    
    svg {
      transform: rotate(360deg);
    }
  }

  &:active {
    transform: scale(0.92);
  }
}

@media (max-width: 1024px) {
  .nav .nav-item {
    padding: 8px 10px;
    font-size: 13px;
  }

  .search-input {
    width: 160px;
  }
}

@media (max-width: 900px) {
  .nav {
    flex-shrink: 1;
    min-width: 0;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }

    .nav-item {
      padding: 8px 12px;
      border-radius: var(--radius-full);
      white-space: nowrap;
    }
  }

  .search-box {
    display: none;
  }

  .write-btn span {
    display: none;
  }
}

@media (max-width: 768px) {
  .header-container {
    padding: 0 12px;
    gap: 8px;
  }

  .logo {
    gap: 0;

    .logo-text {
      display: none;
    }
  }

  .nav {
    display: none;
  }

  .header-actions {
    gap: 4px;
    margin-left: auto;
  }

  .write-btn {
    width: 38px;
    padding: 0;
    justify-content: center;
  }

  .theme-toggle-btn {
    width: 34px;
    height: 34px;
  }
}

.message-btn, .notification-btn {
  position: relative;
  cursor: pointer;
  padding: 6px;
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  text-decoration: none;
  display: flex;
  align-items: center;

  &:hover {
    background: var(--color-google-blue-light);
    color: var(--color-primary);
  }
}

.notification-btn.has-unread svg {
  animation: bell-shake 2s infinite;
}

@keyframes bell-shake {
  0%, 100% { transform: rotate(0); }
  10%, 30%, 50%, 70%, 90% { transform: rotate(-8deg); }
  20%, 40%, 60%, 80% { transform: rotate(8deg); }
}

.notification-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: #e53935;
  color: white;
  font-size: 10px;
  font-weight: 600;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.notification-menu {
  width: 360px;
  max-height: 420px;
  overflow-y: auto;
  padding: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  font-weight: 600;
  font-size: 15px;
  background: var(--bg-primary);
}

.notification-empty {
  padding: 32px;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.notification-item-wrapper {
  padding: 0 12px;

  &.unread {
    background: var(--color-google-blue-light);
  }
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 8px;
  cursor: pointer;
  transition: background var(--transition-fast);
  border-radius: var(--radius-md);

  &:hover {
    background: var(--bg-primary);
  }
}

.notification-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-google-blue-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  flex-shrink: 0;
}

.notification-content-wrapper {
  flex: 1;
  min-width: 0;
}

.notification-content {
  font-size: 14px;
  line-height: 1.5;
  color: var(--color-text);
  white-space: normal;
  word-break: break-all;
}

.notification-time {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.notification-delete-btn {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.notification-item:hover .notification-delete-btn {
  opacity: 1;
}

.notification-footer {
  padding: 12px 20px;
  text-align: center;
  border-top: 1px solid var(--border-color);
  font-size: 14px;
  background: var(--bg-primary);

  a {
    color: var(--color-primary);
    text-decoration: none;
    font-weight: 500;

    &:hover {
      text-decoration: underline;
    }
  }
}

:deep(.el-dropdown__item.unread) {
  background: var(--color-google-blue-light);
}
</style>
