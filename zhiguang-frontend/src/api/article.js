
import request from '@/utils/request'

// ===== V2.0 文章历史版本 =====

export const getArticleVersions = (articleId) => {
  return request({ url: `/api/articles/${articleId}/versions`, method: "get" })
}

export const getArticleVersion = (articleId, versionId) => {
  return request({ url: `/api/articles/${articleId}/versions/${versionId}`, method: "get" })
}

export const restoreArticleVersion = (articleId, versionId) => {
  return request({ url: `/api/articles/${articleId}/versions/${versionId}/restore`, method: "post" })
}

// ===== 用户收藏 =====

export const getMyFavorites = (page = 0, size = 20) => {
  return request({ url: '/api/users/favorites', method: 'get', params: { page, size } })
}

// ===== 标签云 =====

export const getTagCloud = () => {
  return request({ url: '/api/articles/tags', method: 'get' })
}

// ===== 文章列表与详情 =====

export const getArticles = (params = {}) => {
  return request({ url: '/api/articles', method: 'get', params })
}

export const getHotArticles = (limit = 5) => {
  return request({ url: '/api/articles/hot', method: 'get', params: { limit } })
}

export const getPopularArticles = (limit = 5) => {
  return request({ url: '/api/articles/popular', method: 'get', params: { limit } })
}

export const getArticle = (id) => {
  return request({ url: `/api/articles/${id}`, method: 'get' })
}

export const likeArticle = (id) => {
  return request({ url: `/api/articles/${id}/like`, method: 'post' })
}

export const favoriteArticle = (id) => {
  return request({ url: `/api/articles/${id}/favorite`, method: 'post' })
}

export const createArticle = (data) => {
  return request({ url: '/api/articles', method: 'post', data })
}

export const updateArticle = (id, data) => {
  return request({ url: `/api/articles/${id}`, method: 'put', data })
}

export const deleteArticle = (id) => {
  return request({ url: `/api/articles/${id}`, method: 'delete' })
}
