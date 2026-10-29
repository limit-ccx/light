import request from '@/utils/request'

const TOKEN_KEY = 'tree-hole-anon-token'
const EXPIRES_KEY = 'tree-hole-anon-expires'
const PROFILE_KEY = 'tree-hole-anon-profile'

let identityPromise = null

/**
 * 确保持有服务端签发的匿名身份 token：
 * - 无 token → 签发新身份；
 * - 7 天内到期 → 携带旧 token 无损续期（anonId 不变，旧内容属主关系保留）。
 * 身份档案（昵称/徽标/颜色）由服务端随 token 下发并缓存。
 */
export function ensureTreeHoleIdentity ({ force = false } = {}) {
  const token = localStorage.getItem(TOKEN_KEY)
  const expires = Number(localStorage.getItem(EXPIRES_KEY) || 0)
  const refreshBefore = Date.now() + 7 * 24 * 3600 * 1000
  if (!force && token && expires > refreshBefore) {
    return Promise.resolve(token)
  }
  if (!identityPromise) {
    identityPromise = request({
      url: '/api/tree-hole/identity',
      method: 'post',
      data: token ? { token } : {},
      skipAuthRefresh: true
    })
      .then((data) => {
        localStorage.setItem(TOKEN_KEY, data.token)
        localStorage.setItem(EXPIRES_KEY, String(data.expiresAt || 0))
        if (data.displayName) {
          localStorage.setItem(PROFILE_KEY, JSON.stringify({
            displayName: data.displayName,
            emoji: data.emoji || '',
            color: data.color || ''
          }))
        }
        return data.token
      })
      .finally(() => {
        identityPromise = null
      })
  }
  return identityPromise
}

/** 读取缓存的匿名身份档案（服务端按身份哈希派生的昵称/颜色） */
export function getTreeHoleProfile () {
  try {
    const raw = localStorage.getItem(PROFILE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/** 写接口：先确保身份；身份无效（4401）时重新签发并重试一次 */
async function withIdentity (fn) {
  await ensureTreeHoleIdentity().catch(() => {})
  try {
    return await fn()
  } catch (err) {
    if (err && err.businessCode === 4401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(EXPIRES_KEY)
      await ensureTreeHoleIdentity({ force: true })
      return await fn()
    }
    throw err
  }
}

/** 读接口：身份缺失不阻塞（服务端降级为临时身份） */
async function withOptionalIdentity (fn) {
  await ensureTreeHoleIdentity().catch(() => {})
  return fn()
}

export const getTreeHolePosts = (params) => {
  return withOptionalIdentity(() => request({
    url: '/api/tree-hole/posts',
    method: 'get',
    params
  }))
}

export const createTreeHolePost = (data) => {
  return withIdentity(() => request({
    url: '/api/tree-hole/posts',
    method: 'post',
    data
  }))
}

export const getTreeHolePostDetail = (id) => {
  return withOptionalIdentity(() => request({
    url: `/api/tree-hole/posts/${id}`,
    method: 'get'
  }))
}

export const deleteTreeHolePost = (id) => {
  return withIdentity(() => request({
    url: `/api/tree-hole/posts/${id}`,
    method: 'delete'
  }))
}

export const createTreeHoleReply = (data) => {
  return withIdentity(() => request({
    url: '/api/tree-hole/replies',
    method: 'post',
    data
  }))
}

export const deleteTreeHoleReply = (id) => {
  return withIdentity(() => request({
    url: `/api/tree-hole/replies/${id}`,
    method: 'delete'
  }))
}

export const toggleTreeHoleReaction = (data) => {
  return withIdentity(() => request({
    url: '/api/tree-hole/reactions',
    method: 'post',
    data
  }))
}

export const reportTreeHoleContent = (data) => {
  return withIdentity(() => request({
    url: '/api/tree-hole/reports',
    method: 'post',
    data
  }))
}

export const recommendTreeHolePosts = (params) => {
  return withOptionalIdentity(() => request({
    url: '/api/tree-hole/recommend',
    method: 'get',
    params
  }))
}

export const getTreeHoleEmotionStats = () => {
  return request({
    url: '/api/tree-hole/emotion-stats',
    method: 'get'
  })
}

export const getEmotionTrends = (days = 7) => {
  return request({
    url: '/api/tree-hole/emotion-trends',
    method: 'get',
    params: { days }
  })
}

export const getHotTopics = (limit = 10) => {
  return request({
    url: '/api/tree-hole/hot-topics',
    method: 'get',
    params: { limit }
  })
}

export const emotionTags = [
  { key: 'all', label: '全部', color: '#94a3b8', icon: '🌈' },
  { key: 'happiness', label: '开心', color: '#f59e0b', icon: '😊' },
  { key: 'sadness', label: '难过', color: '#3b82f6', icon: '😢' },
  { key: 'anxiety', label: '焦虑', color: '#0891b2', icon: '😰' },
  { key: 'excitement', label: '兴奋', color: '#ec4899', icon: '🤩' },
  { key: 'loneliness', label: '孤独', color: '#64748b', icon: '🌙' },
  { key: 'anger', label: '愤怒', color: '#ef4444', icon: '😠' },
  { key: 'gratitude', label: '感恩', color: '#10b981', icon: '🙏' },
  { key: 'other', label: '其他', color: '#6b7280', icon: '💭' }
]
