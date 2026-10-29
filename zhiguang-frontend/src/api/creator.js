import request from '@/utils/request'

export const applyCreator = (data) => {
  return request({
    url: '/api/creator-applications',
    method: 'post',
    data
  })
}

export const getMyCreatorApplication = () => {
  return request({
    url: '/api/creator-applications/my',
    method: 'get'
  })
}

export const listCreatorApplications = (params = {}) => {
  return request({
    url: '/api/creator-applications',
    method: 'get',
    params
  })
}

export const reviewCreatorApplication = (id, data) => {
  return request({
    url: `/api/creator-applications/${id}/review`,
    method: 'put',
    data
  })
}
