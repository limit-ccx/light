import axios from 'axios'
import { ElMessage } from 'element-plus'

export function resolveApiBaseURL () {
  const raw = import.meta.env.VITE_API_BASE_URL
  if (raw != null && String(raw).trim() !== '') {
    return String(raw).trim().replace(/\/+$/, '')
  }
  // 默认使用相对路径，让开发和生产都走当前域名的代理或同域部署
  return ''
}

function resolveTimeoutMs () {
  const n = Number(import.meta.env.VITE_API_TIMEOUT)
  if (Number.isFinite(n) && n > 0) return n
  return 30000
}

const request = axios.create({
  baseURL: resolveApiBaseURL(),
  timeout: resolveTimeoutMs(),
  withCredentials: true
})

// 树洞匿名身份：服务端签发的签名 token（见 api/treeHole.js 的 ensureTreeHoleIdentity），
// 本地不再自行生成 UUID——旧机制等于明文口令，可被轮换/冒充
function getTreeHoleToken () {
  return localStorage.getItem('tree-hole-anon-token') || ''
}

export function getAccessToken () {
  return sessionStorage.getItem('token') || localStorage.getItem('token') || ''
}

function isAuthEndpoint (url = '') {
  return ['/api/auth/login', '/api/auth/register', '/api/auth/refresh', '/api/auth/logout'].some(endpoint => url.includes(endpoint))
}

let refreshPromise = null

async function refreshAccessToken () {
  if (!refreshPromise) {
    refreshPromise = request({
      url: '/api/auth/refresh',
      method: 'post',
      skipAuthRefresh: true
    }).finally(() => {
      refreshPromise = null
    })
  }

  const data = await refreshPromise
  const { useAuthStore } = await import('@/store/auth')
  const authStore = useAuthStore()
  authStore.setSession(data.token, data.user, authStore.remembered)
  return data
}

function clearLocalSession () {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('user')
  sessionStorage.removeItem('auth-remember')
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('auth-remember')
}

request.interceptors.request.use(
  (config) => {
    const token = getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    if (config.url && config.url.includes('/api/tree-hole')) {
      const anonToken = getTreeHoleToken()
      if (anonToken) {
        config.headers['X-Anonymous-Id'] = anonToken
      }
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (
      payload &&
      typeof payload === 'object' &&
      typeof payload.code === 'number'
    ) {
      if (payload.code !== 200) {
        const err = new Error(payload.message || '请求失败')
        err.businessCode = payload.code
        err.response = response
        return Promise.reject(err)
      }
      return payload.data
    }
    return payload
  },
  async (error) => {
    const status = error.response?.status
    const config = error.config || {}
    const raw = error.response?.data
    const statusFallback = {
      400: '请求参数有误',
      401: '账号或密码错误',
      403: '请求被拒绝（403），请检查登录状态或服务配置',
      404: '请求的资源不存在',
      429: '操作过于频繁，请稍后再试',
      500: '服务器开小差了，请稍后再试',
      502: '后端服务暂不可用，请稍后重试',
      504: '后端服务响应超时，请稍后重试'
    }
    const message =
      (typeof raw === 'object' && raw?.message) ||
      (status
        ? statusFallback[status] || error.message || '请求失败'
        : error.code === 'ECONNABORTED'
          ? '请求超时，请检查后端服务'
          : '无法连接后端服务，请确认后端已启动')

    if (
      status === 401 &&
      !config.skipAuthRefresh &&
      !config._retry &&
      !isAuthEndpoint(config.url)
    ) {
      config._retry = true
      try {
        await refreshAccessToken()
        config.headers = config.headers || {}
        config.headers.Authorization = `Bearer ${getAccessToken()}`
        return request(config)
      } catch {
        clearLocalSession()
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
        return Promise.reject(error)
      }
    }

    if (status === 401) {
      if (!isAuthEndpoint(config.url)) {
        clearLocalSession()
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
      } else {
        ElMessage.error(message)
      }
      return Promise.reject(error)
    }

    if (status === 403) {
      ElMessage.warning(message || '没有权限访问该资源')
      return Promise.reject(error)
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
