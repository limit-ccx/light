import request from '@/utils/request'

// 知识库条目管理
export const addToKnowledge = (data) => {
  return request({
    url: '/api/knowledge/items',
    method: 'post',
    data
  })
}

export const getKnowledgeItems = (params) => {
  return request({
    url: '/api/knowledge/items',
    method: 'get',
    params
  })
}

export const searchKnowledgeItems = (keyword) => {
  return request({
    url: '/api/knowledge/items/search',
    method: 'get',
    params: { keyword }
  })
}

export const getKnowledgeItem = (id) => {
  return request({
    url: `/api/knowledge/items/${id}`,
    method: 'get'
  })
}

export const updateKnowledgeItem = (id, data) => {
  return request({
    url: `/api/knowledge/items/${id}`,
    method: 'put',
    data
  })
}

export const deleteKnowledgeItem = (id) => {
  return request({
    url: `/api/knowledge/items/${id}`,
    method: 'delete'
  })
}

export const updateReadStatus = (id, status) => {
  return request({
    url: `/api/knowledge/items/${id}/read-status`,
    method: 'put',
    data: { status }
  })
}

// 分类管理
export const createCategory = (data) => {
  return request({
    url: '/api/knowledge/categories',
    method: 'post',
    data
  })
}

export const getCategories = () => {
  return request({
    url: '/api/knowledge/categories',
    method: 'get'
  })
}

export const deleteCategory = (id) => {
  return request({
    url: `/api/knowledge/categories/${id}`,
    method: 'delete'
  })
}

// 标签管理
export const getAllTags = () => {
  return request({
    url: '/api/knowledge/tags',
    method: 'get'
  })
}

// 统计
export const getKnowledgeCount = () => {
  return request({
    url: '/api/knowledge/count',
    method: 'get'
  })
}

// 导入本地文档（文件上传）
export const importDocument = (formData) => {
  return request({
    url: '/api/knowledge/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// RAG问答（支持指定文章ID列表）
export const askKnowledge = (question, itemIds) => {
  return request({
    url: '/api/knowledge/ask',
    method: 'post',
    data: { question, itemIds },
    timeout: 120000 // AI 生成需要较长时间
  })
}

// ==================== 知识图谱 ====================

export const getKnowledgeGraphData = (limit = 200) => {
  return request({
    url: '/api/knowledge-graph/data',
    method: 'get',
    params: { limit }
  })
}

export const getKnowledgeGraphStats = () => {
  return request({
    url: '/api/knowledge-graph/stats',
    method: 'get'
  })
}

export const searchKnowledgeNodes = (keyword) => {
  return request({
    url: '/api/knowledge-graph/nodes/search',
    method: 'get',
    params: { keyword }
  })
}

export const getKnowledgeNodeDetail = (id) => {
  return request({
    url: `/api/knowledge-graph/nodes/${id}`,
    method: 'get'
  })
}

export const addKnowledgeNode = (data) => {
  return request({
    url: '/api/knowledge-graph/nodes',
    method: 'post',
    data
  })
}

export const addKnowledgeEdge = (data) => {
  return request({
    url: '/api/knowledge-graph/edges',
    method: 'post',
    data
  })
}

export const deleteKnowledgeNode = (id) => {
  return request({
    url: `/api/knowledge-graph/nodes/${id}`,
    method: 'delete'
  })
}

export const deleteKnowledgeEdge = (id) => {
  return request({
    url: `/api/knowledge-graph/edges/${id}`,
    method: 'delete'
  })
}

export const extractKnowledgeFromArticle = (articleId, content) => {
  return request({
    url: `/api/knowledge-graph/extract/${articleId}`,
    method: 'post',
    data: { content },
    timeout: 120000
  })
}
