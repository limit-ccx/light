import request from '@/utils/request'

async function createStreamUrl (path) {
  const { ticket } = await request({
    url: '/api/auth/sse-ticket',
    method: 'post'
  })
  const base = request.defaults.baseURL || ''
  return `${base}${path}?ticket=${encodeURIComponent(ticket)}`
}

export const getNotifications = () => {
  return request({
    url: '/api/notifications',
    method: 'get'
  })
}

export const getUnreadCount = () => {
  return request({
    url: '/api/notifications/unread-count',
    method: 'get'
  })
}

export const markAllAsRead = () => {
  return request({
    url: '/api/notifications/read-all',
    method: 'post'
  })
}

export const markAsRead = (id) => {
  return request({
    url: `/api/notifications/${id}/read`,
    method: 'post'
  })
}

export const deleteNotification = (id) => {
  return request({
    url: `/api/notifications/${id}`,
    method: 'delete'
  })
}

export const subscribeNotificationStream = async () => {
  return new EventSource(await createStreamUrl('/api/notifications/stream'))
}
