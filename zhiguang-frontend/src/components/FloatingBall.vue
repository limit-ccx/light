<template>
  <div
    ref="rootRef"
    class="floating-ball-container"
    :class="{ 'is-left': panelPlacement.side === 'left', 'is-up': panelPlacement.up }"
    :style="{ transform: `translate3d(${position.x}px, ${position.y}px, 0)` }"
  >
    <!-- 快捷面板 -->
    <div
      v-if="isOpen"
      class="quick-panel"
      role="dialog"
      aria-label="快捷菜单"
      @pointerdown.stop
    >
        <header class="panel-head">
          <span class="panel-avatar">
            <img :src="avatarUrl" alt="" aria-hidden="true" />
          </span>
          <span class="panel-copy">
            <strong class="panel-brand">light<i class="brand-dot"></i></strong>
            <em>AI WRITING ASSISTANT</em>
          </span>
          <button class="panel-close" aria-label="关闭菜单" @click="closeMenu">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M6 6l12 12M18 6 6 18"/></svg>
          </button>
        </header>

        <ul class="panel-list">
          <li v-for="item in menuItems" :key="item.id">
            <button class="panel-item" @click="handleMenuClick(item)">
              <span class="item-icon" v-html="item.icon"></span>
              <span class="item-label">{{ item.label }}</span>
              <span v-if="item.id === 'messages' && unreadCount > 0" class="item-badge">{{ unreadCount }}</span>
              <span v-else-if="item.id === 'innerVoice' && innerVoice.selectedCount" class="item-badge">{{ innerVoice.selectedCount }}</span>
              <svg class="item-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 6 6 6-6 6"/></svg>
            </button>
          </li>
        </ul>

        <footer class="panel-foot">
          <span class="foot-status">
            <span class="foot-dot"></span> 在线 · 随时待命
          </span>
          <button class="foot-link" @click="handleFootAction">
            {{ voices.length ? '换个句子' : '设置心声' }}
          </button>
        </footer>
      </div>

    <!-- 心声气泡 -->
    <div
      v-if="showSoupBubble"
      class="soup-bubble"
      :class="{ 'is-fading': isSoupFading }"
      @mouseenter="isSoupPaused = true"
      @mouseleave="isSoupPaused = false"
      @click.stop="nextSoup"
    >
      <em class="soup-eyebrow">INNER VOICE</em>
      <p class="soup-text">{{ currentVoice }}</p>
      <span class="soup-hint">点击换一句</span>
      <span class="soup-tail"></span>
    </div>

    <!-- 悬浮球 -->
    <div class="ball-bob" :class="{ 'is-idle': !isDragging && !isOpen }">
      <button
        ref="ballRef"
        class="floating-ball"
        :class="{ 'is-open': isOpen, 'is-dragging': isDragging }"
        type="button"
        :aria-expanded="isOpen"
        aria-label="light · 打开快捷菜单"
        @pointerdown="onPointerDown"
        @keydown.enter.prevent="toggleMenu"
        @keydown.space.prevent="toggleMenu"
        @mouseenter="isSoupPaused = true"
        @mouseleave="isSoupPaused = false"
        @contextmenu.prevent
      >
        <span class="ball-ring"></span>
        <span class="ball-inner">
          <img class="ball-avatar" :src="avatarUrl" alt="" aria-hidden="true" draggable="false" />
          <span class="ball-sheen"></span>
        </span>
        <span v-if="unreadCount > 0" class="ball-badge" :aria-label="`${unreadCount} 条未读消息`">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        <span class="ball-status"></span>
      </button>
    </div>

    <!-- 悬停姓名条 -->
    <span class="ball-name" aria-hidden="true"><b>light</b><i class="name-dot"></i></span>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useInnerVoiceStore } from '@/store/innerVoice'

const props = defineProps({
  draggable: { type: Boolean, default: true },
  initialPosition: { type: Object, default: () => ({ x: 0, y: 0 }) },
  avatarUrl: { type: String, default: '/pet/assistant.png' },
  unreadCount: { type: Number, default: 0 }
})

const emit = defineEmits(['menu-click'])

const STORAGE_KEY = 'light-note:ball-pos'
const BALL_SIZE = 68
const EDGE = 12

const ballSize = () => (window.innerWidth <= 640 ? 56 : BALL_SIZE)

const rootRef = ref(null)
const ballRef = ref(null)
const isOpen = ref(false)
const isDragging = ref(false)
const position = ref({ x: 0, y: 0 })
const showSoup = ref(false)
const panelPlacement = ref({ side: 'left', up: true })

let dragStart = null
let dragMoved = false
let lastPointer = { x: 0, y: 0 }

/* ===== 心声 ===== */
const innerVoice = useInnerVoiceStore()
const voices = computed(() => innerVoice.selectedVoices.map((voice) => voice.text))
const currentVoiceIndex = ref(0)
const isSoupPaused = ref(false)
const isSoupFading = ref(false)
let soupTimer = null
let soupFadeTimer = null

const currentVoice = computed(() => {
  if (!voices.value.length) return ''
  return voices.value[currentVoiceIndex.value % voices.value.length] || ''
})

const showSoupBubble = computed(
  () => innerVoice.enabled &&
    voices.value.length > 0 &&
    showSoup.value &&
    !isOpen.value &&
    !innerVoice.managerOpen
)

const randomIndex = (length, exclude = -1) => {
  if (length <= 1) return 0
  let next = Math.floor(Math.random() * length)
  if (next === exclude) next = (next + 1) % length
  return next
}

const nextSoup = () => {
  if (!voices.value.length) {
    closeMenu()
    innerVoice.openManager()
    return
  }
  isSoupFading.value = true
  clearTimeout(soupFadeTimer)
  soupFadeTimer = setTimeout(() => {
    currentVoiceIndex.value = randomIndex(voices.value.length, currentVoiceIndex.value)
    isSoupFading.value = false
  }, 300)
  showSoup.value = true
}

const startSoupAutoPlay = () => {
  if (!innerVoice.enabled || innerVoice.interval <= 0 || !voices.value.length) return
  stopSoupAutoPlay()
  soupTimer = setInterval(() => {
    if (!isSoupPaused.value && !isOpen.value) nextSoup()
  }, innerVoice.interval)
}

const stopSoupAutoPlay = () => {
  if (soupTimer) {
    clearInterval(soupTimer)
    soupTimer = null
  }
}

const handleFootAction = () => {
  if (voices.value.length) {
    nextSoup()
  } else {
    closeMenu()
    innerVoice.openManager()
  }
}

let soupShowTimer = null
const scheduleSoup = () => {
  clearTimeout(soupShowTimer)
  soupShowTimer = setTimeout(() => {
    if (innerVoice.enabled && voices.value.length && !isOpen.value) {
      showSoup.value = true
    }
  }, 900)
}

watch(
  () => voices.value.length,
  (length) => {
    if (!length) {
      stopSoupAutoPlay()
      showSoup.value = false
      return
    }
    currentVoiceIndex.value = randomIndex(length)
    if (innerVoice.enabled) {
      scheduleSoup()
      startSoupAutoPlay()
    }
  }
)

watch(
  () => innerVoice.enabled,
  (enabled) => {
    if (enabled && voices.value.length) {
      scheduleSoup()
      startSoupAutoPlay()
    } else {
      showSoup.value = false
      stopSoupAutoPlay()
    }
  }
)

watch(
  () => innerVoice.interval,
  () => {
    if (innerVoice.enabled && voices.value.length) startSoupAutoPlay()
  }
)

/* ===== 菜单 ===== */
const icon = (paths) =>
  `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">${paths}</svg>`

const menuItems = [
  { id: 'agent', label: 'AI 导览助手', icon: icon('<path d="M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z"/><path d="M19 15l.9 2.1L22 18l-2.1.9L19 21l-.9-2.1L16 18l2.1-.9z"/>') },
  { id: 'write', label: '写一篇文章', icon: icon('<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/>') },
  { id: 'messages', label: '查看消息', icon: icon('<path d="M21 11.5a8.4 8.4 0 0 1-9 8.4 8.5 8.5 0 0 1-3.4-.7L3 21l1.8-4.6A8.4 8.4 0 0 1 3.5 11 8.5 8.5 0 0 1 12 3a8.4 8.4 0 0 1 9 8.5z"/>') },
  { id: 'signin', label: '今日签到', icon: icon('<rect x="3" y="5" width="18" height="16" rx="3"/><path d="M8 3v4M16 3v4M3 11h18"/><path d="m9 16 2 2 4-4"/>') },
  { id: 'innerVoice', label: '心声', icon: icon('<path d="M21 11.5a8.4 8.4 0 0 1-9 8.4 8.5 8.5 0 0 1-3.4-.7L3 21l1.8-4.6A8.4 8.4 0 0 1 3.5 11 8.5 8.5 0 0 1 12 3a8.4 8.4 0 0 1 9 8.5z"/><path d="M8.5 10.5h7M8.5 13.5h4.5"/>') },
  { id: 'profile', label: '个人中心', icon: icon('<circle cx="12" cy="8" r="4"/><path d="M4 21v-1a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v1"/>') }
]

const toggleMenu = () => {
  if (isDragging.value) return
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    isSoupPaused.value = true
    computePlacement()
  }
}

const closeMenu = () => {
  isOpen.value = false
  isSoupPaused.value = false
  ballRef.value?.focus?.()
}

const handleMenuClick = (item) => {
  emit('menu-click', item.id)
  isOpen.value = false
  isSoupPaused.value = false
}

const computePlacement = () => {
  const side = position.value.x > window.innerWidth / 2 ? 'left' : 'right'
  const up = position.value.y > window.innerHeight / 2
  panelPlacement.value = { side, up }
}

/* ===== 拖拽 ===== */
const clamp = (v, min, max) => Math.min(Math.max(v, min), Math.max(min, max))

const bounds = () => {
  const size = ballSize()
  return {
    maxX: window.innerWidth - size - EDGE,
    maxY: window.innerHeight - size - EDGE
  }
}

const onPointerDown = (e) => {
  if (e.button !== 0) return
  dragStart = { x: e.clientX, y: e.clientY, px: position.value.x, py: position.value.y }
  dragMoved = false
  lastPointer = { x: e.clientX, y: e.clientY }
  isDragging.value = false
  try {
    e.currentTarget.setPointerCapture?.(e.pointerId)
  } catch {
    // 合成事件忽略
  }
  e.preventDefault()
}

const onPointerMove = (e) => {
  if (!dragStart) return
  const dx = e.clientX - dragStart.x
  const dy = e.clientY - dragStart.y
  if (!dragMoved && Math.hypot(dx, dy) > 4) {
    dragMoved = true
    isDragging.value = true
    isOpen.value = false
  }
  lastPointer = { x: e.clientX, y: e.clientY }
  if (!dragMoved) return
  const { maxX, maxY } = bounds()
  position.value = {
    x: clamp(dragStart.px + dx, EDGE, maxX),
    y: clamp(dragStart.py + dy, EDGE, maxY)
  }
}

const onPointerUp = () => {
  if (!dragStart) return
  const wasDragging = dragMoved
  dragStart = null
  isDragging.value = false
  if (wasDragging) {
    persistPosition()
    computePlacement()
  } else {
    toggleMenu()
  }
}

const persistPosition = () => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(position.value))
  } catch {
    // 隐私模式下忽略
  }
}

/* ===== 其它事件 ===== */
const onResize = () => {
  const { maxX, maxY } = bounds()
  position.value = {
    x: clamp(position.value.x, EDGE, maxX),
    y: clamp(position.value.y, EDGE, maxY)
  }
  persistPosition()
  if (isOpen.value) computePlacement()
}

const onDocumentPointerDown = (e) => {
  if (!isOpen.value) return
  if (rootRef.value?.contains(e.target)) return
  closeMenu()
}

const onKeydown = (e) => {
  if (e.key === 'Escape' && isOpen.value) closeMenu()
}

const readStoredPosition = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const p = JSON.parse(raw)
    if (typeof p?.x === 'number' && typeof p?.y === 'number') return p
  } catch {
    // ignore
  }
  return null
}

onMounted(() => {
  const { maxX, maxY } = bounds()
  const stored = readStoredPosition()
  const fallback = {
    x: maxX - 8,
    y: Math.round(window.innerHeight * (window.innerWidth <= 640 ? 0.78 : 0.62))
  }
  const init = props.initialPosition?.x || props.initialPosition?.y ? props.initialPosition : stored || fallback
  position.value = {
    x: clamp(init.x, EDGE, maxX),
    y: clamp(init.y, EDGE, maxY)
  }
  computePlacement()

  innerVoice.load()
  if (innerVoice.enabled && voices.value.length) {
    currentVoiceIndex.value = randomIndex(voices.value.length)
    // 首屏稍晚出现，避免和入场动画抢焦点
    scheduleSoup()
    startSoupAutoPlay()
  } else {
    showSoup.value = false
  }

  window.addEventListener('pointermove', onPointerMove, { passive: true })
  window.addEventListener('pointerup', onPointerUp)
  window.addEventListener('resize', onResize, { passive: true })
  window.addEventListener('keydown', onKeydown)
  document.addEventListener('pointerdown', onDocumentPointerDown, true)
})

onUnmounted(() => {
  stopSoupAutoPlay()
  clearTimeout(soupFadeTimer)
  clearTimeout(soupShowTimer)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('keydown', onKeydown)
  document.removeEventListener('pointerdown', onDocumentPointerDown, true)
})
</script>

<style lang="scss" scoped>
/* ============================================================
   纸域空间助手 · 悬浮球
   原型：粉紫 + 冰蓝的kawaii科技感，玻璃拟态面板
   ============================================================ */
.floating-ball-container {
  position: fixed;
  left: 0;
  top: 0;
  z-index: 9999;
  width: 68px;
  height: 68px;
  user-select: none;
  -webkit-user-select: none;
}

/* 漂浮层与定位层分离，避免动画覆盖 hover 缩放 */
.ball-bob {
  width: 68px;
  height: 68px;

  &.is-idle {
    animation: ball-float 4.2s ease-in-out infinite;
  }
}

/* ===== 悬浮球 ===== */
.floating-ball {
  position: relative;
  width: 68px;
  height: 68px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: transparent;
  cursor: grab;
  touch-action: none;
  transition: transform 0.28s cubic-bezier(0.34, 1.4, 0.64, 1),
    filter 0.28s ease;
  filter: drop-shadow(0 10px 22px rgba(150, 110, 255, 0.35));

  &:focus-visible {
    outline: 3px solid rgba(139, 92, 246, 0.55);
    outline-offset: 4px;
  }

  &:hover {
    transform: scale(1.07);
    filter: drop-shadow(0 14px 28px rgba(150, 110, 255, 0.5));
  }

  &:active {
    transform: scale(0.97);
  }

  &.is-dragging {
    cursor: grabbing;
    transform: scale(1.03);
  }

  &.is-open {
    transform: scale(1.04);
    filter: drop-shadow(0 12px 30px rgba(139, 92, 246, 0.55));
  }
}

@keyframes ball-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

/* 外圈流光描边 */
.ball-ring {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  background: conic-gradient(
    from 0deg,
    #ff9ecd,
    #b892ff,
    #7de3ff,
    #b892ff,
    #ff9ecd
  );
  animation: ring-spin 6s linear infinite;
  opacity: 0.95;
}

@keyframes ring-spin {
  to { transform: rotate(360deg); }
}

.ball-inner {
  position: absolute;
  inset: 1px;
  border-radius: 50%;
  overflow: hidden;
  background:
    radial-gradient(circle at 32% 24%, rgba(255, 255, 255, 0.9), transparent 58%),
    linear-gradient(150deg, #f3e8ff 0%, #dbeafe 52%, #fce7f3 100%);
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.85);
}

.ball-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
  transform: scale(1.02);
}

.ball-sheen {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.55) 0%, transparent 42%);
  pointer-events: none;
}

/* 未读角标 */
.ball-badge {
  position: absolute;
  top: -3px;
  right: -3px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff7aa2, #f43f5e);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  border: 2px solid #fff;
  box-shadow: 0 4px 12px rgba(244, 63, 94, 0.45);
  animation: badge-pulse 2.4s ease-in-out infinite;
  z-index: 3;
}

@keyframes badge-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.08); }
}

/* 在线状态点 */
.ball-status {
  position: absolute;
  right: 2px;
  bottom: 4px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #34d399;
  border: 2.5px solid #fff;
  box-shadow: 0 0 8px rgba(52, 211, 153, 0.9);
  z-index: 3;
}

/* 悬停姓名条 */
.ball-name {
  position: absolute;
  right: calc(100% + 10px);
  top: 50%;
  transform: translateY(-50%) translateX(4px);
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 11px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #6b7280;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  box-shadow: 0 4px 14px rgba(150, 110, 255, 0.2);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.ball-name b {
  font-family: 'Inter', 'PingFang SC', -apple-system, sans-serif;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.12em;
  line-height: 1;
  text-transform: lowercase;
  background: linear-gradient(100deg, #22d3ee 0%, #6366f1 52%, #ec4899 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 品牌名后的小光点 */
.name-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #22d3ee;
  box-shadow: 0 0 7px rgba(34, 211, 238, 0.9);
  flex-shrink: 0;
}

.floating-ball-container:hover .ball-name {
  opacity: 1;
  transform: translateY(-50%) translateX(0);
}

/* 小球在左半屏时，姓名条放右侧 */
.floating-ball-container:not(.is-left) .ball-name {
  right: auto;
  left: calc(100% + 10px);
}

/* ============================================================
   快捷面板
   ============================================================ */
.quick-panel {
  position: absolute;
  width: 236px;
  padding: 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(24px) saturate(160%);
  -webkit-backdrop-filter: blur(24px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow:
    0 18px 40px rgba(120, 80, 200, 0.18),
    0 0 0 1px rgba(184, 146, 255, 0.12);
  transform-origin: bottom right;
  z-index: 2;
  animation: panel-in 0.22s cubic-bezier(0.34, 1.3, 0.64, 1) backwards;
}

.floating-ball-container.is-left .quick-panel {
  right: calc(100% + 12px);
}

.floating-ball-container:not(.is-left) .quick-panel {
  left: calc(100% + 12px);
  transform-origin: bottom left;
}

.floating-ball-container.is-up .quick-panel {
  bottom: 0;
}

.floating-ball-container:not(.is-up) .quick-panel {
  top: 0;
}

.panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 4px 10px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.22);
}

.panel-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(140deg, #f3e8ff, #dbeafe);
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.9), 0 4px 10px rgba(139, 92, 246, 0.25);

  img { width: 100%; height: 100%; object-fit: cover; }
}

.panel-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;

  .panel-brand {
    display: inline-flex;
    align-items: baseline;
    gap: 4px;
    font-family: 'Inter', 'PingFang SC', -apple-system, sans-serif;
    font-size: 18px;
    font-weight: 800;
    letter-spacing: 0.04em;
    line-height: 1.1;
    text-transform: lowercase;
    background: linear-gradient(100deg, #22d3ee 0%, #6366f1 50%, #ec4899 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  /* 品牌名后的小光点 */
  .brand-dot {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: linear-gradient(135deg, #22d3ee, #ec4899);
    box-shadow: 0 0 8px rgba(99, 102, 241, 0.85);
    transform: translateY(-7px);
  }

  em {
    font-style: normal;
    font-size: 8px;
    font-weight: 700;
    letter-spacing: 0.2em;
    color: #b39ddb;
    text-transform: uppercase;
  }
}

.panel-close {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: none;
  background: rgba(148, 163, 184, 0.12);
  color: #8b93a3;
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover {
    background: rgba(139, 92, 246, 0.14);
    color: #7c3aed;
  }
}

.panel-list {
  list-style: none;
  margin: 6px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.panel-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 8px;
  border: none;
  border-radius: 11px;
  background: transparent;
  color: #4b5563;
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.16s ease, color 0.16s ease, transform 0.16s ease;

  &:hover {
    background: linear-gradient(90deg, rgba(184, 146, 255, 0.16), rgba(125, 227, 255, 0.1));
    color: #6d28d9;
    transform: translateX(2px);
  }

  &:hover .item-arrow {
    opacity: 1;
    transform: translateX(0);
  }
}

.item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 9px;
  flex-shrink: 0;
  color: #8b5cf6;
  background: linear-gradient(140deg, rgba(184, 146, 255, 0.2), rgba(125, 227, 255, 0.16));
}

.item-label {
  flex: 1;
  letter-spacing: 0.02em;
}

.item-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #f43f5e;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
}

.item-arrow {
  color: #c4b5fd;
  opacity: 0;
  transform: translateX(-3px);
  transition: all 0.18s ease;
}

.panel-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 6px 2px;
  border-top: 1px solid rgba(148, 163, 184, 0.22);
  font-size: 11px;
  color: #9aa3b2;
}

.foot-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.foot-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 6px rgba(52, 211, 153, 0.9);
}

.foot-link {
  border: none;
  background: transparent;
  color: #8b5cf6;
  font-size: 11px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 6px;

  &:hover {
    background: rgba(139, 92, 246, 0.1);
  }
}

/* 面板入场（关闭即时移除，避免过渡在后台标签页卡住节点） */
@keyframes panel-in {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(6px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* ============================================================
   心声气泡
   ============================================================ */
.soup-bubble {
  position: absolute;
  right: calc(100% + 12px);
  bottom: 2px;
  width: 220px;
  padding: 11px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow:
    0 10px 26px rgba(120, 80, 200, 0.16),
    0 0 0 1px rgba(184, 146, 255, 0.1);
  cursor: pointer;
  animation: soup-in 0.3s cubic-bezier(0.34, 1.3, 0.64, 1) backwards;
  transition: opacity 0.25s ease, transform 0.25s ease, box-shadow 0.25s ease;

  &.is-fading {
    opacity: 0;
    transform: translateY(3px);
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow:
      0 14px 32px rgba(120, 80, 200, 0.22),
      0 0 0 1px rgba(184, 146, 255, 0.18);
  }
}

/* 小球在左半屏时，气泡放右侧；在右半屏时保持默认的左侧 */
.floating-ball-container:not(.is-left) .soup-bubble {
  right: auto;
  left: calc(100% + 12px);
}

.soup-eyebrow {
  display: block;
  margin-bottom: 4px;
  font-style: normal;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: #b39ddb;
  text-transform: uppercase;
}

.soup-text {
  margin: 0;
  font-family: var(--font-artistic);
  font-size: 15px;
  line-height: 1.55;
  letter-spacing: 0.01em;
  color: #5b5566;
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.soup-hint {
  display: block;
  margin-top: 5px;
  font-size: 10px;
  color: #b892ff;
  opacity: 0;
  transform: translateY(-3px);
  transition: all 0.22s ease;
}

.soup-bubble:hover .soup-hint {
  opacity: 1;
  transform: translateY(0);
}

.soup-tail {
  position: absolute;
  right: -5px;
  bottom: 16px;
  width: 10px;
  height: 10px;
  background: rgba(255, 255, 255, 0.9);
  transform: rotate(45deg);
  border-radius: 2px;
}

.floating-ball-container:not(.is-left) .soup-tail {
  right: auto;
  left: -5px;
}

/* 气泡入场 */
@keyframes soup-in {
  from {
    opacity: 0;
    transform: translateX(10px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* ============================================================
   深色主题适配
   ============================================================ */
:global([data-theme='dark']) {
  .quick-panel {
    background: rgba(24, 26, 38, 0.9);
    border-color: rgba(255, 255, 255, 0.12);
    box-shadow: 0 18px 40px rgba(0, 0, 0, 0.45), 0 0 0 1px rgba(168, 139, 250, 0.16);
  }

  .panel-copy strong { color: #ede9fe; }
  .panel-copy em { color: #9ca3af; }
  .panel-head, .panel-foot { border-color: rgba(255, 255, 255, 0.1); }
  .panel-item { color: #cbd5e1; }
  .panel-item:hover { color: #e9d5ff; background: linear-gradient(90deg, rgba(139, 92, 246, 0.24), rgba(34, 211, 238, 0.12)); }
  .panel-close { background: rgba(255, 255, 255, 0.08); color: #cbd5e1; }
  .panel-foot { color: #94a3b8; }
  .ball-name { background: rgba(30, 32, 46, 0.92); color: #e2e8f0; }
  .soup-bubble { background: rgba(28, 30, 44, 0.92); border-color: rgba(255, 255, 255, 0.12); }
  .soup-text { color: #d8dbe6; }
  .soup-tail { background: rgba(28, 30, 44, 0.92); }
  .ball-inner { box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.45); }
  .ball-status { border-color: #1c1f2e; }
  .ball-badge { border-color: #1c1f2e; }
}

/* ============================================================
   响应式 / 减少动效
   ============================================================ */
@media (max-width: 640px) {
  .floating-ball-container,
  .ball-bob,
  .floating-ball {
    width: 56px;
    height: 56px;
  }

  .quick-panel { width: 210px; }
  .soup-bubble { width: 182px; }
}

@media (prefers-reduced-motion: reduce) {
  .ball-bob.is-idle,
  .ball-ring,
  .ball-badge {
    animation: none;
  }
}
</style>
