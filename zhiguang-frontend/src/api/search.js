import request from '@/utils/request'

/** 普通/语义搜索 */
export const search = (q, page = 0, size = 20) =>
  request({ url: '/api/search', method: 'get', params: { q, page, size } })

/** 向量相似度搜索 */
export const vectorSearch = (q, page = 0, size = 20) =>
  request({ url: '/api/search/vector', method: 'get', params: { q, page, size } })

/** RAG 问答 */
export const ragAsk = (question) =>
  request({ url: '/api/search/rag', method: 'post', data: { question } })

/** 相关文章推荐 */
export const getRelated = (articleId, limit = 5) =>
  request({ url: `/api/search/related/${articleId}`, method: 'get', params: { limit } })
