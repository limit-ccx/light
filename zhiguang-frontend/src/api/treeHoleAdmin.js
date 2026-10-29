import request from '@/utils/request'

export const getTreeHoleAdminPosts = (params) => {
  return request({
    url: '/api/admin/tree-hole/posts',
    method: 'get',
    params
  })
}

export const approveTreeHolePost = (id) => {
  return request({
    url: `/api/admin/tree-hole/posts/${id}/approve`,
    method: 'put'
  })
}

export const rejectTreeHolePost = (id, reason) => {
  return request({
    url: `/api/admin/tree-hole/posts/${id}/reject`,
    method: 'put',
    data: { reason }
  })
}

export const deleteTreeHolePost = (id) => {
  return request({
    url: `/api/admin/tree-hole/posts/${id}`,
    method: 'delete'
  })
}

export const deleteTreeHoleReply = (id) => {
  return request({
    url: `/api/admin/tree-hole/replies/${id}`,
    method: 'delete'
  })
}

export const getTreeHoleReports = (params) => {
  return request({
    url: '/api/admin/tree-hole/reports',
    method: 'get',
    params
  })
}

export const resolveReport = (id) => {
  return request({
    url: `/api/admin/tree-hole/reports/${id}/resolve`,
    method: 'put'
  })
}

export const dismissReport = (id) => {
  return request({
    url: `/api/admin/tree-hole/reports/${id}/dismiss`,
    method: 'put'
  })
}

export const getTreeHoleStats = () => {
  return request({
    url: '/api/admin/tree-hole/stats',
    method: 'get'
  })
}
