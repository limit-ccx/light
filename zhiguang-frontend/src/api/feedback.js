import request from '@/utils/request'

/** 提交用户建议反馈（需登录） */
export const submitFeedback = (data) => {
  return request({ url: '/api/feedback', method: 'post', data })
}
