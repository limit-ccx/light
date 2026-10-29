import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { useAuthStore } from '@/store/auth'
import { getUserSettings, updateUserSettings } from '@/api/userSettings'
import { recommendedVoiceTexts } from '@/data/recommendedVoices'

export const MAX_INNER_VOICES = 10
export const MAX_INNER_VOICE_TEXT = 80
const MAX_INNER_VOICE_LIBRARY = 100
const GUEST_KEY = 'light-note:inner-voice-guest-v1'
const INTERVALS = [5000, 8000, 15000, 30000]

const createId = () =>
  `v_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`

const hashText = (text) => {
  let hash = 5381
  for (let i = 0; i < text.length; i++) {
    hash = ((hash << 5) + hash + text.charCodeAt(i)) >>> 0
  }
  return hash.toString(36)
}

export const recommendedVoices = recommendedVoiceTexts.map((text, index) => ({
  id: `rec_${index}_${hashText(text)}`,
  text,
  recommended: true,
  createdAt: null
}))

const recommendedIdSet = new Set(recommendedVoices.map((voice) => voice.id))

const normalize = (raw) => {
  const source = raw && typeof raw === 'object' ? raw : {}
  const voices = Array.isArray(source.voices)
    ? source.voices
        .filter((voice) => voice && typeof voice.text === 'string' && voice.text.trim())
        .slice(0, MAX_INNER_VOICE_LIBRARY)
        .map((voice) => ({
          id: String(voice.id || createId()),
          text: voice.text.trim().slice(0, MAX_INNER_VOICE_TEXT),
          createdAt: voice.createdAt || new Date().toISOString()
        }))
    : []

  const ids = new Set([...recommendedIdSet, ...voices.map((voice) => voice.id)])
  const selectedIds = Array.isArray(source.selectedIds)
    ? source.selectedIds
        .map(String)
        .filter((id) => ids.has(id))
        .slice(0, MAX_INNER_VOICES)
    : []

  const interval = INTERVALS.includes(Number(source.interval))
    ? Number(source.interval)
    : 8000

  return {
    voices,
    selectedIds: [...new Set(selectedIds)],
    enabled: source.enabled !== false,
    interval
  }
}

export const useInnerVoiceStore = defineStore('innerVoice', () => {
  const authStore = useAuthStore()

  const voices = ref([])
  const selectedIds = ref([])
  const enabled = ref(true)
  const interval = ref(8000)
  const managerOpen = ref(false)

  let loadedFor = ''
  let saveTimer = null

  const storageKey = computed(() =>
    authStore.isLoggedIn && authStore.user?.id
      ? `light-note:inner-voice-user-${authStore.user.id}`
      : GUEST_KEY
  )

  const allVoices = computed(() => [...recommendedVoices, ...voices.value])
  const voiceMap = computed(() => {
    const map = new Map()
    allVoices.value.forEach((voice) => map.set(voice.id, voice))
    return map
  })

  const selectedVoices = computed(() =>
    selectedIds.value.map((id) => voiceMap.value.get(id)).filter(Boolean)
  )

  const selectedCount = computed(() => selectedIds.value.length)
  const isSelectionFull = computed(() => selectedCount.value >= MAX_INNER_VOICES)
  const customCount = computed(() => voices.value.length)

  const readLocal = (key) => {
    try {
      const raw = localStorage.getItem(key)
      if (!raw) return null
      return normalize(JSON.parse(raw))
    } catch {
      return null
    }
  }

  const serialize = () => ({
    voices: voices.value,
    selectedIds: selectedIds.value,
    enabled: enabled.value,
    interval: interval.value
  })

  const writeLocal = () => {
    try {
      localStorage.setItem(storageKey.value, JSON.stringify(serialize()))
    } catch {
      // 隐私模式或空间不足时忽略
    }
  }

  const apply = (config) => {
    voices.value = config.voices
    selectedIds.value = config.selectedIds
    enabled.value = config.enabled
    interval.value = config.interval
  }

  const persist = () => {
    writeLocal()
    if (!authStore.isLoggedIn) return
    clearTimeout(saveTimer)
    saveTimer = setTimeout(() => {
      updateUserSettings({
        innerVoiceConfig: JSON.stringify(serialize())
      }).catch(() => {
        // 网络异常时保留本地缓存，下次修改或登录时再同步
      })
    }, 350)
  }

  const load = async (force = false) => {
    const key = storageKey.value
    if (!force && loadedFor === key) return
    loadedFor = key

    const cached = readLocal(key)
    apply(cached || normalize(null))

    if (!authStore.isLoggedIn) return

    try {
      const settings = await getUserSettings()
      const remoteRaw = settings?.innerVoiceConfig
      const remote = remoteRaw ? normalize(JSON.parse(remoteRaw)) : null

      if (remote) {
        apply(remote)
        writeLocal()
        return
      }

      const guestConfig = readLocal(GUEST_KEY)
      if (guestConfig?.voices.length) {
        apply(guestConfig)
        persist()
      }
    } catch {
      // 接口异常时继续使用本地缓存
    }
  }

  const addVoice = (text) => {
    const value = String(text || '').trim()
    if (!value) return { ok: false, reason: '心声不能为空' }
    if (value.length > MAX_INNER_VOICE_TEXT) {
      return { ok: false, reason: `心声不能超过 ${MAX_INNER_VOICE_TEXT} 个字符` }
    }
    if (voices.value.some((voice) => voice.text === value)) {
      return { ok: false, reason: '这条心声已经存在' }
    }
    if (voices.value.length >= MAX_INNER_VOICE_LIBRARY) {
      return { ok: false, reason: `最多保存 ${MAX_INNER_VOICE_LIBRARY} 条心声` }
    }

    const voice = {
      id: createId(),
      text: value,
      createdAt: new Date().toISOString()
    }
    voices.value.unshift(voice)
    if (selectedIds.value.length < MAX_INNER_VOICES) {
      selectedIds.value = [voice.id, ...selectedIds.value]
    }
    persist()
    return { ok: true, voice }
  }

  const updateVoice = (id, text) => {
    const value = String(text || '').trim()
    if (!value) return { ok: false, reason: '心声不能为空' }
    if (value.length > MAX_INNER_VOICE_TEXT) {
      return { ok: false, reason: `心声不能超过 ${MAX_INNER_VOICE_TEXT} 个字符` }
    }
    const voice = voices.value.find((item) => item.id === id)
    if (!voice) return { ok: false, reason: '心声不存在' }
    if (voices.value.some((item) => item.id !== id && item.text === value)) {
      return { ok: false, reason: '这条心声已经存在' }
    }

    voice.text = value
    persist()
    return { ok: true }
  }

  const removeVoice = (id) => {
    voices.value = voices.value.filter((voice) => voice.id !== id)
    selectedIds.value = selectedIds.value.filter((selectedId) => selectedId !== id)
    persist()
  }

  const toggleVoice = (id) => {
    if (selectedIds.value.includes(id)) {
      selectedIds.value = selectedIds.value.filter((selectedId) => selectedId !== id)
      persist()
      return true
    }
    if (isSelectionFull.value) return false
    selectedIds.value = [...selectedIds.value, id]
    persist()
    return true
  }

  const fillSelection = () => {
    const next = [...selectedIds.value]
    for (const voice of allVoices.value) {
      if (next.length >= MAX_INNER_VOICES) break
      if (!next.includes(voice.id)) next.push(voice.id)
    }
    if (next.length === selectedIds.value.length) return next.length
    selectedIds.value = next
    persist()
    return next.length
  }

  const clearSelection = () => {
    if (!selectedIds.value.length) return
    selectedIds.value = []
    persist()
  }

  const setEnabled = (value) => {
    enabled.value = Boolean(value)
    persist()
  }

  const setInterval = (value) => {
    if (!INTERVALS.includes(Number(value))) return
    interval.value = Number(value)
    persist()
  }

  const openManager = () => {
    managerOpen.value = true
    load()
  }

  const closeManager = () => {
    managerOpen.value = false
  }

  // 登录/切换账号后重新加载对应配置
  watch(
    () => [authStore.isLoggedIn, authStore.user?.id],
    () => load(true)
  )

  return {
    voices,
    recommendedVoices,
    allVoices,
    selectedIds,
    selectedVoices,
    selectedCount,
    customCount,
    isSelectionFull,
    enabled,
    interval,
    managerOpen,
    load,
    addVoice,
    updateVoice,
    removeVoice,
    toggleVoice,
    fillSelection,
    clearSelection,
    setEnabled,
    setInterval,
    openManager,
    closeManager
  }
})
