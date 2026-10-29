import { ElMessageBox } from 'element-plus'

/**
 * 后台统一确认框：显式带上浅色弹窗样式，
 * 避免站点处于暗色主题时，确认框与浅色后台形成黑白硬对比。
 */
export const adminConfirm = (message, title, options = {}) => {
  return ElMessageBox.confirm(message, title, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    ...options,
    customClass: ['admin-dialog', 'admin-messagebox', options.customClass]
      .filter(Boolean)
      .join(' ')
  })
}
