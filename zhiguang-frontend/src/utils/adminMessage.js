import { ElMessage } from 'element-plus'

/**
 * 后台统一消息提示：显式带上浅色弹窗样式（customClass: admin-msg），
 * 避免站点处于暗色主题时，提示框与浅色后台形成黑白硬对比。
 * 用法与 ElMessage 完全一致：adminMessage.success('保存成功')
 */
const withAdminClass = (options = {}) => ({
  ...options,
  customClass: ['admin-msg', options.customClass].filter(Boolean).join(' ')
})

const wrap = (fn) => (message, options) => {
  const base = typeof message === 'string' || typeof message === 'function' ? { message } : { ...message }
  return fn(withAdminClass({ ...base, ...options }))
}

export const adminMessage = {
  success: wrap((o) => ElMessage.success(o)),
  warning: wrap((o) => ElMessage.warning(o)),
  info: wrap((o) => ElMessage.info(o)),
  error: wrap((o) => ElMessage.error(o))
}
