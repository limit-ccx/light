import { resolveApiBaseURL, getAccessToken } from '@/utils/request'
import request from '@/utils/request'

/**
 * Agent 对话（SSE 流式）。
 * 事件：status{stage} / tool{name,status} / delta{text} / done{conversationId,quotaLeft,truncated} / error{code,message}
 * 返回 abort 函数。
 */
export const agentChatStream = (data, { onStatus, onTool, onDelta, onDone, onError } = {}) => {
  const controller = new AbortController()
  ;(async () => {
    try {
      const base = resolveApiBaseURL()
      const token = getAccessToken()
      const resp = await fetch(`${base}/api/agent/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: JSON.stringify(data),
        signal: controller.signal
      })

      if (!resp.ok || !resp.body) {
        let message = '助手暂时不可用，请稍后重试'
        try {
          const err = await resp.json()
          message = err.message || message
        } catch { /* 保留默认提示 */ }
        onError && onError({ code: 'HTTP_' + resp.status, message })
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
          else if (event === 'tool') { onTool && onTool(payload) }
          else if (event === 'delta') { onDelta && onDelta(payload.text || '') }
          else if (event === 'done') { onDone && onDone(payload); return }
          else if (event === 'error') { onError && onError(payload); return }
        }
      }
      onDone && onDone({})
    } catch (err) {
      if (err && err.name === 'AbortError') return
      onError && onError({ code: 'NETWORK', message: err?.message || '连接失败，请稍后重试' })
    }
  })()
  return () => controller.abort()
}

/** 恢复当前会话历史 */
export const getAgentHistory = () => {
  return request({ url: '/api/agent/conversations/current', method: 'get' })
}

/** 清空当前会话（新对话） */
export const clearAgentHistory = () => {
  return request({ url: '/api/agent/conversations/current', method: 'delete' })
}

/** 今日剩余 AI 配额 */
export const getAgentQuota = () => {
  return request({ url: '/api/agent/quota', method: 'get' })
}

/** 回复反馈（LIKE/DISLIKE） */
export const sendAgentFeedback = (data) => {
  return request({ url: '/api/agent/feedback', method: 'post', data })
}
