import { onMounted, onUnmounted } from 'vue'
import { subscribeNotificationStream } from '@/api/notification'

const getToken = () => sessionStorage.getItem('token') || localStorage.getItem('token')

export function useNotificationStream(options = {}) {
  const {
    onEvent,
    onConnected,
    onDisconnected,
    maxReconnect = 5,
    reconnectDelay = 3000,
    autoConnect = true
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

  const scheduleReconnect = () => {
    if (intentionalClose || !shouldConnect()) return
    if (reconnectCount >= maxReconnect) return
    reconnectCount += 1
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
    }, reconnectDelay)
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
      cleanupEventSource()
      if (intentionalClose) return
      onDisconnected?.()
      scheduleReconnect()
    })
  }

  const connect = async () => {
    if (!shouldConnect()) return

    cleanupEventSource()

    try {
      eventSource = await subscribeNotificationStream()
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
    if (autoConnect) {
      connect()
    }
  })

  onUnmounted(() => {
    close()
  })

  return { connect, close }
}
