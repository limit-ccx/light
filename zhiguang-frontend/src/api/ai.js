import request from '@/utils/request'
import { resolveApiBaseURL, getAccessToken } from '@/utils/request'

/** AI 润色（同步全量返回；前端推荐使用 polishContentStream 流式） */
export const polishContent = (data) => {
  return request({ url: '/api/ai/polish', method: 'post', data, timeout: 90000 })
}

/**
 * AI 润色（SSE 流式）。
 * 回调：onStatus/onDelta(增量文本)/onDone({model,length,truncated})/onError(message)
 * 返回 abort 函数，用于用户关闭弹窗时中断生成。
 */
export const polishContentStream = (data, { onStatus, onDelta, onDone, onError } = {}) => {
  const controller = new AbortController()
  ;(async () => {
    try {
      const base = resolveApiBaseURL()
      const token = getAccessToken()
      const resp = await fetch(`${base}/api/ai/polish/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: JSON.stringify(data),
        signal: controller.signal
      })

      if (!resp.ok || !resp.body) {
        let message = 'AI 服务暂时不可用，请稍后重试'
        try {
          const err = await resp.json()
          message = err.message || message
        } catch { /* 保留默认提示 */ }
        onError && onError(message)
        return
      }

      const reader = resp.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const blocks = buffer.split('\n\n')
        buffer = blocks.pop() || ''
        for (const block of blocks) {
          let event = 'message'
          let dataStr = ''
          for (const line of block.split('\n')) {
            if (line.startsWith('event:')) event = line.slice(6).trim()
            else if (line.startsWith('data:')) dataStr += line.slice(5).trim()
          }
          if (!dataStr) continue
          let payload
          try { payload = JSON.parse(dataStr) } catch { continue }
          if (event === 'status') { onStatus && onStatus(payload) }
          else if (event === 'delta') { onDelta && onDelta(payload.text || '') }
          else if (event === 'done') { onDone && onDone(payload); return }
          else if (event === 'error') { onError && onError(payload.message || 'AI 生成失败，请稍后重试'); return }
        }
      }
      onDone && onDone({})
    } catch (err) {
      if (err && err.name === 'AbortError') return
      onError && onError(err?.message || 'AI 生成失败，请稍后重试')
    }
  })()
  return () => controller.abort()
}

/** AI 一键生成：标题 + 摘要 + 标签 一次返回 */
export const aiAssist = (data) => {
  return request({ url: '/api/ai/write/assist', method: 'post', data, timeout: 90000 })
}

/** AI 总结 */
export const summarizeArticle = (articleId, force = false) => {
  return request({ url: `/api/ai/summarize/${articleId}`, method: 'post', params: { force }, timeout: 90000 })
}

// ===== V2.0 AI 写作助手 =====

/** 生成标题 */
export const generateTitles = (data) => {
  return request({ url: '/api/ai/write/titles', method: 'post', data, timeout: 60000 })
}

/** 生成摘要 */
export const generateSummary = (data) => {
  return request({ url: '/api/ai/write/summary', method: 'post', data, timeout: 60000 })
}

/** 生成标签 */
export const generateTags = (data) => {
  return request({ url: '/api/ai/write/tags', method: 'post', data, timeout: 60000 })
}

/** 结构优化 */
export const optimizeStructure = (data) => {
  return request({ url: '/api/ai/write/structure', method: 'post', data, timeout: 90000 })
}

/** 自动纠错 */
export const autoCorrect = (data) => {
  return request({ url: '/api/ai/write/correct', method: 'post', data, timeout: 90000 })
}

/** 关键观点 */
export const extractKeyPoints = (data) => {
  return request({ url: '/api/ai/write/key-points', method: 'post', data, timeout: 60000 })
}

/** 思维导图 */
export const generateMindMap = (data) => {
  return request({ url: '/api/ai/write/mind-map', method: 'post', data, timeout: 60000 })
}

// ===== V2.0 AI 评论助手 =====

/** 生成评论回复 */
export const generateCommentReply = (data) => {
  return request({ url: '/api/ai/comment/reply', method: 'post', data, timeout: 60000 })
}

// ===== V2.0 知识图谱 =====

/** 提取知识实体 */
export const extractKnowledge = (data) => {
  return request({ url: '/api/ai/knowledge/extract', method: 'post', data, timeout: 90000 })
}
