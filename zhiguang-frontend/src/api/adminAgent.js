import request from '@/utils/request'
import { resolveApiBaseURL, getAccessToken } from '@/utils/request'

/**
 * 管理员智能体（运营助手）对话（SSE 流式）。
 * 事件：status / action{name,status} / observation{name,summary} / delta{text} /
 * action-draft{operationId,...} / done{pendingActions,quotaLeft} / error{code,message}
 */
export const adminAgentChatStream = (message, { provider, effort } = {}, handlers = {}) => {
  const { onStatus, onAction, onObservation, onDelta, onDraft, onAsk, onDone, onError } = handlers
  const controller = new AbortController()
  ;(async () => {
    try {
      const base = resolveApiBaseURL()
      const token = getAccessToken()
      const resp = await fetch(`${base}/api/admin/agent/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: JSON.stringify({ message, provider, effort }),
        signal: controller.signal
      })
      if (!resp.ok || !resp.body) {
        let msg = '助手暂时不可用'
        try { msg = (await resp.json()).message || msg } catch { /* keep */ }
        onError && onError({ code: 'HTTP_' + resp.status, message: msg })
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
          if (event === 'status') onStatus && onStatus(payload)
          else if (event === 'action') onAction && onAction(payload)
          else if (event === 'observation') onObservation && onObservation(payload)
          else if (event === 'delta') onDelta && onDelta(payload.text || '')
          else if (event === 'action-draft') onDraft && onDraft(payload)
          else if (event === 'ask') onAsk && onAsk(payload)
          else if (event === 'done') { onDone && onDone(payload); return }
          else if (event === 'error') { onError && onError(payload); return }
        }
      }
      onDone && onDone({})
    } catch (err) {
      if (err && err.name === 'AbortError') return
      onError && onError({ code: 'NETWORK', message: err?.message || '连接失败' })
    }
  })()
  return () => controller.abort()
}

/** 可用模型平台（含密钥配置状态） */
/** 停止当前生成 */
export const stopAgentChat = () =>
  request({ url: '/api/admin/agent/stop', method: 'post' })

/** 插入对话（生成进行中追加问题，本轮结束后处理） */
export const insertAgentQuestion = (message) =>
  request({ url: '/api/admin/agent/insert', method: 'post', data: { message } })

export const getAgentModels = () =>
  request({ url: '/api/admin/agent/models', method: 'get' })

/** 持久化对话历史（分页） */
export const getChatHistory = (page = 0, size = 30) =>
  request({ url: '/api/admin/agent/history', method: 'get', params: { page, size } })

export const getAdminAgentHistory = () =>
  request({ url: '/api/admin/agent/conversations/current', method: 'get' })

export const clearAdminAgentHistory = () =>
  request({ url: '/api/admin/agent/conversations/current', method: 'delete' })

export const getPendingActions = () =>
  request({ url: '/api/admin/agent/actions/pending', method: 'get' })

export const confirmPendingAction = (operationId, approve) =>
  request({ url: `/api/admin/agent/actions/${operationId}/confirm`, method: 'post', data: { approve } })
