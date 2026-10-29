import request from '@/utils/request'

// 获取用户等级
export function getLevel() {
  return request({ url: '/api/growth/level', method: 'get' })
}

// 获取用户积分
export function getPoints() {
  return request({ url: '/api/growth/points', method: 'get' })
}

// 获取积分日志
export function getPointLogs(params) {
  return request({ url: '/api/growth/points/logs', method: 'get', params })
}

// 获取成就列表
export function getAchievements() {
  return request({ url: '/api/growth/achievements', method: 'get' })
}

// 今日签到
export function signIn() {
  return request({ url: '/api/growth/sign-in', method: 'post' })
}

// 补签
export function supplementSignIn(date) {
  return request({
    url: '/api/growth/sign-in/supplement',
    method: 'post',
    params: { date }
  })
}

// 获取签到日历
export function getSignInCalendar(params) {
  return request({ url: '/api/growth/sign-in/calendar', method: 'get', params })
}

// 获取签到状态
export function getSignInStatus() {
  return request({ url: '/api/growth/sign-in/status', method: 'get' })
}
