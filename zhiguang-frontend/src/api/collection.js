import request from '@/utils/request'

export const createCollection = (data) => request({ url: '/api/collections', method: 'post', data })
export const updateCollection = (id, data) => request({ url: `/api/collections/${id}`, method: 'put', data })
export const getCollection = (id) => request({ url: `/api/collections/${id}`, method: 'get' })
export const getUserCollections = (userId, page = 0, size = 20) =>
  request({ url: `/api/collections/user/${userId}`, method: 'get', params: { page, size } })
export const getCollectionArticles = (collectionId, page = 0, size = 20) =>
  request({ url: `/api/collections/${collectionId}/articles`, method: 'get', params: { page, size } })
export const addArticleToCollection = (collectionId, articleId) =>
  request({ url: `/api/collections/${collectionId}/articles/${articleId}`, method: 'post' })
export const removeArticleFromCollection = (collectionId, articleId) =>
  request({ url: `/api/collections/${collectionId}/articles/${articleId}`, method: 'delete' })
export const deleteCollection = (id) => request({ url: `/api/collections/${id}`, method: 'delete' })

/** 公开：全部合集 */
export const getCollections = (page = 0, size = 20) =>
  request({ url: '/api/collections', method: 'get', params: { page, size } })
