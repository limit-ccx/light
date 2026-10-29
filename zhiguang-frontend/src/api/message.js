import request from '@/utils/request'

async function createStreamUrl (path) {
  const { ticket } = await request({
    url: '/api/auth/sse-ticket',
    method: 'post'
  })
  const base = request.defaults.baseURL || ''
  return `${base}${path}?ticket=${encodeURIComponent(ticket)}`
}

export const sendMessage = (receiverId, content, clientMessageId) => {
  return request({
    url: `/api/messages/${receiverId}`,
    method: 'post',
    data: { content, clientMessageId }
  })
}

export const getConversation = (userId, params) => {
  return request({
    url: `/api/messages/conversation/${userId}`,
    method: 'get',
    params
  })
}

export const getMessagesBefore = (partnerId, before, limit) => {
  return request({
    url: `/api/messages/conversation/${partnerId}/before`,
    method: 'get',
    params: { before, limit }
  })
}

export const markConversationAsRead = (userId) => {
  return request({
    url: `/api/messages/conversation/${userId}/read`,
    method: 'post'
  })
}

export const getUnreadMessageCount = () => {
  return request({
    url: '/api/messages/unread-count',
    method: 'get'
  })
}

export const getConversationList = () => {
  return request({
    url: '/api/messages/conversations',
    method: 'get'
  })
}

/** 好友列表（互相关注的用户，发起新对话默认展示） */
export const getFriendList = () => {
  return request({
    url: '/api/messages/friends',
    method: 'get'
  })
}

/** 与某用户的聊天资格（未互关只能发一条打招呼消息） */
export const getChatStatus = (partnerId) => {
  return request({
    url: `/api/messages/chat-status/${partnerId}`,
    method: 'get'
  })
}

export const searchUsers = (keyword) => {
  return request({
    url: '/api/messages/users/search',
    method: 'get',
    params: { keyword }
  })
}

export const searchMessages = (keyword, page = 0, size = 20) => {
  return request({
    url: '/api/messages/search',
    method: 'get',
    params: { keyword, page, size }
  })
}

export const searchConversationMessages = (partnerId, keyword, page = 0, size = 20) => {
  return request({
    url: `/api/messages/search/conversation/${partnerId}`,
    method: 'get',
    params: { keyword, page, size }
  })
}

export const deleteMessage = (messageId) => {
  return request({
    url: `/api/messages/${messageId}`,
    method: 'delete'
  })
}

export const subscribeMessageStream = async () => {
  return new EventSource(await createStreamUrl('/api/messages/stream'))
}
