import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getArticles as apiGetArticles, getArticle as apiGetArticle, createArticle as apiCreateArticle, updateArticle as apiUpdateArticle, deleteArticle as apiDeleteArticle } from '@/api/article'
import { likeArticle as apiLikeArticle, favoriteArticle as apiFavoriteArticle } from '@/api/article'

export const useArticleStore = defineStore('article', () => {
  const articles = ref([])
  const currentArticle = ref(null)
  const loading = ref(false)
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(10)

  const getArticles = async (params = {}) => {
    loading.value = true
    try {
      const response = await apiGetArticles({
        page: currentPage.value,
        size: pageSize.value,
        ...params
      })
      articles.value = response.list || response
      total.value = response.total || articles.value.length
      return response
    } finally {
      loading.value = false
    }
  }

  const getArticle = async (id) => {
    loading.value = true
    try {
      const response = await apiGetArticle(id)
      currentArticle.value = response
      return response
    } finally {
      loading.value = false
    }
  }

  const createArticle = async (data) => {
    const response = await apiCreateArticle(data)
    return response
  }

  const updateArticle = async (id, data) => {
    const response = await apiUpdateArticle(id, data)
    return response
  }

  const deleteArticle = async (id) => {
    await apiDeleteArticle(id)
    articles.value = articles.value.filter(a => a.id !== id)
  }

  const likeArticle = async (id) => {
    const response = await apiLikeArticle(id)
    const article = articles.value.find(a => a.id === id)
    if (article) {
      article.isLiked = response.isLiked
      article.likeCount = response.likeCount
    }
    return response
  }

  const favoriteArticle = async (id) => {
    const response = await apiFavoriteArticle(id)
    const article = articles.value.find(a => a.id === id)
    if (article) {
      article.isFavorited = response.isFavorited
      article.favoriteCount = response.favoriteCount
    }
    return response
  }

  return {
    articles,
    currentArticle,
    loading,
    total,
    currentPage,
    pageSize,
    getArticles,
    getArticle,
    createArticle,
    updateArticle,
    deleteArticle,
    likeArticle,
    favoriteArticle
  }
})