import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, register as apiRegister, getProfile as apiGetProfile } from '@/api/auth'
import request from '@/utils/request'

const ACCESS_TOKEN_KEY = 'token'
const USER_KEY = 'user'
const REMEMBER_KEY = 'auth-remember'

// 出于 XSS 防护：token 一律只存 sessionStorage（标签页级），
// 历史 localStorage 残留在读取时一次性迁移并清除。
// "记住我"不再影响存储位置，长会话由后端 refresh cookie（HttpOnly）维持。
function readToken() {
  const session = sessionStorage.getItem(ACCESS_TOKEN_KEY)
  if (session) return session
  const legacy = localStorage.getItem(ACCESS_TOKEN_KEY)
  if (legacy) {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, legacy)
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    const legacyUser = localStorage.getItem(USER_KEY)
    if (legacyUser && !sessionStorage.getItem(USER_KEY)) {
      sessionStorage.setItem(USER_KEY, legacyUser)
      localStorage.removeItem(USER_KEY)
    }
    return legacy
  }
  return ''
}

function readUser() {
  const raw = sessionStorage.getItem(USER_KEY)
  try {
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function clearStorage() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
  sessionStorage.removeItem(REMEMBER_KEY)
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(REMEMBER_KEY)
}

function persist(token, user, remember) {
  clearStorage()
  if (token) sessionStorage.setItem(ACCESS_TOKEN_KEY, token)
  if (user) sessionStorage.setItem(USER_KEY, JSON.stringify(user))
  sessionStorage.setItem(REMEMBER_KEY, String(!!remember))
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref(readUser())
  const token = ref(readToken())

  const isLoggedIn = computed(() => !!token.value)
  const remembered = computed(() => {
    const value = sessionStorage.getItem(REMEMBER_KEY)
    return value === 'true'
  })

  // 注意：isAdmin 仅用于前端 UX 引导（如显示/隐藏管理菜单）
  // 后端 /api/admin/** 由 hasRole("ADMIN") 强制保护，前端绕过不会获得实际权限
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  const isBanned = computed(() => user.value?.status === 'BANNED')

  /* ===== 后台职位与模块权限 ===== */
  const ADMIN_POSITION_MODULES = {
    SUPER: ['content', 'community', 'user', 'operations', 'messages', 'system'],
    OPERATION: ['operations', 'messages'],
    CONTENT: ['content'],
    COMMUNITY: ['community'],
    USER: ['user'],
    FEEDBACK: [],
    SYSTEM: ['system'],
    DATA: []
  }
  // 所有职位都可见的通用模块
  const COMMON_MODULES = ['dashboard', 'statistics', 'feedback']

  const adminPosition = computed(() => user.value?.adminPosition || null)
  const isSuperAdmin = computed(() => adminPosition.value === 'SUPER')

  const canAccessModule = (module) => {
    if (!isAdmin.value) return false
    if (!module || COMMON_MODULES.includes(module)) return true
    if (isSuperAdmin.value) return true
    return (ADMIN_POSITION_MODULES[adminPosition.value] || []).includes(module)
  }

  const setSession = (nextToken, nextUser = null, remember = remembered.value) => {
    token.value = nextToken || ''
    user.value = nextUser
    if (nextToken) {
      persist(nextToken, nextUser, remember)
    } else {
      clearStorage()
    }
  }

  const updateUser = (patch) => {
    if (!user.value) return
    user.value = { ...user.value, ...patch }
    if (token.value) {
      persist(token.value, user.value, remembered.value)
    }
  }

  const login = async (identifier, password, remember = true) => {
    const data = await apiLogin({ identifier, password, remember })
    setSession(data.token, data.user, remember)
    return data
  }

  const register = async (data) => {
    const response = await apiRegister(data)
    setSession(response.token, response.user, true)
    return response
  }

  const refreshSession = async () => {
    const data = await request({
      url: '/api/auth/refresh',
      method: 'post',
      skipAuthRefresh: true
    })
    setSession(data.token, data.user, remembered.value)
    return data
  }

  const logout = async () => {
    try {
      await request.post('/api/auth/logout')
    } catch (e) {
      // 服务端登出失败时仍需清除本地状态
    }
    setSession('', null, false)
    // 清理 PWA 缓存的 /api 响应（INFRA-P2-2）：共用设备上登出后，
    // Cache Storage 里仍残留本账号可见的文章/搜索结果
    try {
      if (window.caches) {
        await caches.delete('api-cache')
      }
    } catch (e) {
      // 缓存清理失败不影响登出
    }
  }

  const fetchProfile = async () => {
    try {
      const profile = await apiGetProfile()
      const nextUser = profile.user || profile
      if (profile.user) {
        setSession(token.value, profile.user, remembered.value)
        return profile
      }

      setSession(token.value, nextUser, remembered.value)
      return nextUser
    } catch (error) {
      setSession('', null, false)
      throw error
    }
  }

  return {
    user,
    token,
    isLoggedIn,
    remembered,
    isAdmin,
    isBanned,
    adminPosition,
    isSuperAdmin,
    canAccessModule,
    setSession,
    updateUser,
    login,
    register,
    refreshSession,
    logout,
    fetchProfile
  }
})
