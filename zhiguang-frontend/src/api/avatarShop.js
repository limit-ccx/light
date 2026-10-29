import request from '@/utils/request'

export const getAvatarShop = () => {
  return request({
    url: '/api/avatar-shop',
    method: 'get'
  })
}

export const purchaseAvatar = (avatarId) => {
  return request({
    url: `/api/avatar-shop/${avatarId}/purchase`,
    method: 'post'
  })
}

export const equipAvatar = (avatarId) => {
  return request({
    url: `/api/avatar-shop/${avatarId}/equip`,
    method: 'post'
  })
}
