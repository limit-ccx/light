import request from '@/utils/request'

export const getUserSettings = () => {
  return request({
    url: '/api/users/settings',
    method: 'get'
  })
}

export const updateUserSettings = (data) => {
  return request({
    url: '/api/users/settings',
    method: 'post',
    data
  })
}

export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/api/upload/image',
    method: 'post',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    data: formData
  })
}
