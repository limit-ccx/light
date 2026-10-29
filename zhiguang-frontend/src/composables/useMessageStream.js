import { onMounted, onUnmounted } from 'vue'
import { subscribeMessageStream } from '@/api/message'

const getToken = () => sessionStorage.getItem('token') || localStorage.getItem('token')

export function useMessageStream(options = {}) {
  const {
    onEvent,
    onConnected,
    onDisconnected,
    maxReconnect = Infinity,
    reconnectDelay = 3000
  } = options

  let eventSource = null
  let reconnectCount = 0
  let intentionalClose = false
  let reconnectTimer = null

  const parseData = (raw) => {
    if (!raw) return null
    try {
      return JSON.parse(raw)
    } catch {
      return raw
    }
  }

  const shouldConnect = () => !!getToken()

  const cleanupEventSource = () => {
    if (eventSource) {
      try {
        eventSource.close()
      } catch {}
      eventSource = null
    }
  }

  // 指数退避：3s 起、每次 ×2、上限 30s；连接成功归零。
  // 此前固定 3s 且最多 5 次后永久静默失联，长驻页面会悄悄失去实时推送。
  const scheduleReconnect = () => {
    if (intentionalClose || !shouldConnect()) return
    if (reconnectCount >= maxReconnect) return
    const delay = Math.min(reconnectDelay * Math.pow(2, reconnectCount), 30000)
    reconnectCount += 1
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
    }, delay)
  }

  const bind = (source) => {
    source.addEventListener('open', () => {
      reconnectCount = 0
      onConnected?.()
      onEvent?.('open', {})
    })

    source.addEventListener('message', (e) => {
      const data = parseData(e.data)
      if (data && typeof data === 'object' && data.type) {
        onEvent?.(data.type, data.payload ?? data)
      } else {
        onEvent?.('message', data)
      }
    })

    source.addEventListener('connect', (e) => {
      onEvent?.('connect', parseData(e.data) ?? e.data)
    })

    source.addEventListener('heartbeat', (e) => {
      onEvent?.('heartbeat', parseData(e.data) ?? e.data)
    })

    source.addEventListener('error', (e) => {
      onEvent?.('error', e)
      onDisconnected?.()
      cleanupEventSource()
      scheduleReconnect()
    })
  }

  const connect = async () => {
    if (!shouldConnect()) return

    cleanupEventSource()
    try {
      eventSource = await subscribeMessageStream()
      bind(eventSource)
    } catch (err) {
      onEvent?.('error', err)
      scheduleReconnect()
    }
  }

  const close = () => {
    intentionalClose = true
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    cleanupEventSource()
  }

  onMounted(() => {
    connect()
  })

  onUnmounted(() => {
    close()
  })

  return { close }
}
