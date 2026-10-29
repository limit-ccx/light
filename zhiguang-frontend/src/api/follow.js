import request from '@/utils/request'

export const toggleFollow = (userId) => {
  return request({
    url: `/api/follows/${userId}`,
    method: 'post'
  })
}

export const getFollowStatus = (userId) => {
  return request({
    url: `/api/follows/status/${userId}`,
    method: 'get'
  })
}

export const getFollowingList = (params) => {
  return request({
    url: '/api/follows/following',
    method: 'get',
    params
  })
}

export const getFollowerList = (params) => {
  return request({
    url: '/api/follows/followers',
    method: 'get',
    params
  })
}
