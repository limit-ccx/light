import request from '@/utils/request'

/** 创建专栏 */
export const createColumn = (data) => request({ url: '/api/columns', method: 'post', data })

/** 编辑专栏 */
export const updateColumn = (id, data) => request({ url: `/api/columns/${id}`, method: 'put', data })

/** 获取专栏详情 */
export const getColumn = (id) => request({ url: `/api/columns/${id}`, method: 'get' })

/** 获取用户专栏列表 */
export const getUserColumns = (userId, page = 0, size = 20) =>
  request({ url: `/api/columns/user/${userId}`, method: 'get', params: { page, size } })

/** 专栏内文章 */
export const getColumnArticles = (columnId, page = 0, size = 20) =>
  request({ url: `/api/columns/${columnId}/articles`, method: 'get', params: { page, size } })

/** 添加文章到专栏 */
export const addArticleToColumn = (columnId, articleId) =>
  request({ url: `/api/columns/${columnId}/articles/${articleId}`, method: 'post' })

/** 移除文章 */
export const removeArticleFromColumn = (columnId, articleId) =>
  request({ url: `/api/columns/${columnId}/articles/${articleId}`, method: 'delete' })

/** 订阅专栏 */
export const subscribeColumn = (columnId) =>
  request({ url: `/api/columns/${columnId}/subscribe`, method: 'post' })

/** 取消订阅 */
export const unsubscribeColumn = (columnId) =>
  request({ url: `/api/columns/${columnId}/subscribe`, method: 'delete' })

/** 删除专栏 */
export const deleteColumn = (id) => request({ url: `/api/columns/${id}`, method: 'delete' })

/** 公开：全部专栏（按订阅数排序） */
export const getColumns = (page = 0, size = 20) =>
  request({ url: '/api/columns', method: 'get', params: { page, size } })
