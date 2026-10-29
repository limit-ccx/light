import { ElMessage } from 'element-plus'

/**
 * 全局提示去重：
 * 1. 同一句话在 1.2s 内重复出现 → 只保留第一条；
 * 2. 同一次请求失败常被「全局拦截器 + 页面 catch」各提示一次 → 300ms 内只保留第一条。
 * 只包裹 error / warning，success 等正常提示不受影响。
 */
const DEDUPE_WINDOW = 1200
const PAIR_WINDOW = 300

const lastByText = new Map()
let lastErrorAt = 0

const wrap = (type) => {
  const original = ElMessage[type].bind(ElMessage)
  ElMessage[type] = (options) => {
    const text = typeof options === 'string' ? options : options?.message || ''
    const now = Date.now()

    const key = `${type}|${text}`
    if (now - (lastByText.get(key) || 0) < DEDUPE_WINDOW) {
      return undefined
    }
    lastByText.set(key, now)

    if (type === 'error' || type === 'warning') {
      if (now - lastErrorAt < PAIR_WINDOW) {
        return undefined
      }
      lastErrorAt = now
    }

    return original(options)
  }
}

wrap('error')
wrap('warning')
