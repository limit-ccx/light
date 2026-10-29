import request from '@/utils/request'

// ========== 7.1 用户管理 ==========

export const getUsers = (params = {}) => {
  return request({ url: '/api/admin/users', method: 'get', params })
}

export const getUserOverview = () => {
  return request({ url: '/api/admin/users/overview', method: 'get' })
}

export const getUserDetail = (userId) => {
  return request({ url: `/api/admin/users/${userId}`, method: 'get' })
}

export const banUser = (userId) => {
  return request({ url: `/api/admin/users/${userId}/ban`, method: 'post' })
}

export const unbanUser = (userId) => {
  return request({ url: `/api/admin/users/${userId}/unban`, method: 'post' })
}

export const resetUserPassword = (userId) => {
  return request({ url: `/api/admin/users/${userId}/reset-password`, method: 'post' })
}

export const updateUserRole = (userId, role) => {
  return request({ url: `/api/admin/users/${userId}/role`, method: 'put', data: { role } })
}

// ========== 7.2 内容审核 ==========

export const getAdminArticles = (params = {}) => {
  return request({ url: '/api/admin/content/articles', method: 'get', params })
}

export const reviewArticle = (articleId, data) => {
  return request({ url: `/api/admin/content/articles/${articleId}/review`, method: 'post', data })
}

export const getAdminComments = (params = {}) => {
  return request({ url: '/api/admin/content/comments', method: 'get', params })
}

export const reviewComment = (commentId, data) => {
  return request({ url: `/api/admin/content/comments/${commentId}/review`, method: 'post', data })
}

export const getReports = (params = {}) => {
  return request({ url: '/api/admin/content/reports', method: 'get', params })
}

export const handleReport = (reportId, data) => {
  return request({ url: `/api/admin/content/reports/${reportId}/handle`, method: 'post', data })
}

export const getSensitiveWords = (params = {}) => {
  return request({ url: '/api/admin/content/sensitive-words', method: 'get', params })
}

export const addSensitiveWord = (data) => {
  return request({ url: '/api/admin/content/sensitive-words', method: 'post', data })
}

export const deleteSensitiveWord = (wordId) => {
  return request({ url: `/api/admin/content/sensitive-words/${wordId}`, method: 'delete' })
}

export const checkSensitiveWord = (text) => {
  return request({ url: '/api/admin/content/check-sensitive', method: 'get', params: { text } })
}

// ========== 7.3 数据统计 ==========

export const getDashboard = () => {
  return request({ url: '/api/admin/statistics/dashboard', method: 'get' })
}

export const getUserGrowth = (days = 30) => {
  return request({ url: '/api/admin/statistics/user-growth', method: 'get', params: { days } })
}

export const getContentStats = () => {
  return request({ url: '/api/admin/statistics/content', method: 'get' })
}

// ========== 7.4 内容推荐 ==========

export const getFeaturedContent = (params = {}) => {
  return request({ url: '/api/admin/recommend', method: 'get', params })
}

export const addFeaturedContent = (data) => {
  return request({ url: '/api/admin/recommend', method: 'post', data })
}

export const removeFeaturedContent = (id) => {
  return request({ url: `/api/admin/recommend/${id}`, method: 'delete' })
}

// ========== 7.5 系统配置与公告 ==========

export const getAllConfigs = () => {
  return request({ url: '/api/admin/config/system', method: 'get' })
}

export const updateConfig = (key, value) => {
  return request({ url: `/api/admin/config/system/${key}`, method: 'put', data: { value } })
}

export const batchUpdateConfigs = (configs) => {
  return request({ url: '/api/admin/config/system/batch', method: 'put', data: configs })
}

export const getAnnouncements = (params = {}) => {
  return request({ url: '/api/admin/config/announcements', method: 'get', params })
}

export const createAnnouncement = (data) => {
  return request({ url: '/api/admin/config/announcements', method: 'post', data })
}

export const updateAnnouncement = (id, data) => {
  return request({ url: `/api/admin/config/announcements/${id}`, method: 'put', data })
}

export const deleteAnnouncement = (id) => {
  return request({ url: `/api/admin/config/announcements/${id}`, method: 'delete' })
}

export const getBanners = (params = {}) => {
  return request({ url: '/api/admin/config/banners', method: 'get', params })
}

export const createBanner = (data) => {
  return request({ url: '/api/admin/config/banners', method: 'post', data })
}

export const updateBanner = (id, data) => {
  return request({ url: `/api/admin/config/banners/${id}`, method: 'put', data })
}

export const updateBannerStatus = (id, status) => {
  return request({ url: `/api/admin/config/banners/${id}/status`, method: 'put', data: { status } })
}

export const deleteBanner = (id) => {
  return request({ url: `/api/admin/config/banners/${id}`, method: 'delete' })
}

// ========== 7.6 审计日志 ==========

export const getAdminLogs = (params = {}) => {
  return request({ url: '/api/admin/logs', method: 'get', params })
}

// ========== 7.7 用户建议反馈 ==========

export const getFeedbackList = (params = {}) => {
  return request({ url: '/api/admin/feedback', method: 'get', params })
}

export const getFeedbackStats = () => {
  return request({ url: '/api/admin/feedback/stats', method: 'get' })
}

export const handleFeedback = (id, data) => {
  return request({ url: `/api/admin/feedback/${id}`, method: 'put', data })
}

// ========== 7.8 后台职位分配（仅超级管理员） ==========

export const updateUserPosition = (userId, position) => {
  return request({ url: `/api/admin/users/${userId}/position`, method: 'put', data: { position } })
}

// ========== 7.9 运营管理（管理员账号，仅 SUPER / OPERATION） ==========

export const getOperationsAdmins = (params = {}) => {
  return request({ url: '/api/admin/operations/admins', method: 'get', params })
}

export const updateOperationPosition = (userId, position) => {
  return request({
    url: `/api/admin/operations/admins/${userId}/position`,
    method: 'put',
    data: { position }
  })
}

export const resetOperationPassword = (userId) => {
  return request({
    url: `/api/admin/operations/admins/${userId}/reset-password`,
    method: 'post'
  })
}

export const updateOperationRole = (userId, role) => {
  return request({
    url: `/api/admin/operations/admins/${userId}/role`,
    method: 'put',
    data: { role }
  })
}

// ========== 7.10 后台消息提示（催办，与前台私信隔离） ==========

export const getReceivedReminders = (params = {}) => {
  return request({ url: '/api/admin/reminders/received', method: 'get', params })
}

export const getSentReminders = (params = {}) => {
  return request({ url: '/api/admin/reminders/sent', method: 'get', params })
}

export const getReminderStats = () => {
  return request({ url: '/api/admin/reminders/stats', method: 'get' })
}

export const createReminder = (data) => {
  return request({ url: '/api/admin/reminders', method: 'post', data })
}

export const completeReminder = (id, reply) => {
  return request({ url: `/api/admin/reminders/${id}/complete`, method: 'put', data: { reply } })
}

export const acknowledgeReminder = (id) => {
  return request({ url: `/api/admin/reminders/${id}/ack`, method: 'put' })
}

export const markRemindersRead = () => {
  return request({ url: '/api/admin/reminders/read-all', method: 'put' })
}
