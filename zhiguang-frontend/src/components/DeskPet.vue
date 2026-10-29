<template>
  <!-- 桌宠本体 -->
  <div
    v-if="active"
    ref="petRef"
    class="desk-pet"
    :class="{ 'is-dragging': dragging, 'is-paused': paused, 'is-3d': use3D }"
    :style="{ transform: `translate3d(${pos.x}px, ${pos.y}px, 0)` }"
    role="button"
    tabindex="0"
    :aria-label="`桌宠：${currentAction.label}，点击切换动作，右键打开菜单`"
    @pointerdown="onPointerDown"
    @pointerenter="hover = true"
    @pointerleave="hover = false"
    @contextmenu.prevent="openMenu"
    @keydown.enter.prevent="randomAction"
    @keydown.space.prevent="randomAction"
    @keydown.esc="closeMenu"
  >
    <div v-if="use3D" ref="stageRef" class="pet-stage" aria-hidden="true"></div>

    <template v-else>
      <span class="pet-shadow"></span>

      <div class="pet-facing" :style="{ transform: `scaleX(${facing})` }">
        <div class="pet-bob">
          <img
            :key="currentKey"
            class="pet-img"
            :src="currentAction.src"
            :alt="currentAction.label"
            draggable="false"
          />
        </div>
      </div>
    </template>

    <span class="pet-tip" :class="{ 'is-meteor': announce?.tone === 'meteor', 'is-rainbow': announce?.tone === 'rainbow', 'is-below': pos.y < 96 }">
      <strong class="tip-cn">{{ announce ? announce.cn : currentAction.label }}</strong>
      <em class="tip-en">{{ announce ? announce.en : currentAction.en }}</em>
      <span class="tip-tail"></span>
    </span>
  </div>

  <!-- 隐藏后的召回入口 -->
  <button
    v-else-if="visible === false && !isAdmin"
    class="pet-recall"
    title="召回桌宠"
    aria-label="召回桌宠"
    @click="revealPet"
  >
    🐾
  </button>

  <!-- 右键菜单 -->
  <Teleport to="body">
    <div
      v-if="menu.open"
      class="pet-menu"
      role="menu"
      :style="{ left: menu.x + 'px', top: menu.y + 'px' }"
      @contextmenu.prevent
    >
      <p class="pet-menu-title">{{ currentAction.label }}</p>

      <button class="pet-menu-item" role="menuitem" @click="cycleAction">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-3-6.7"/><path d="M21 4v5h-5"/></svg>
        切换动作
      </button>

      <button class="pet-menu-item" role="menuitem" @click="togglePause">
        <svg v-if="manualPaused" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 4 12 8-12 8z"/></svg>
        <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M9 5v14M15 5v14"/></svg>
        {{ manualPaused ? '继续游走' : '暂停游走' }}
      </button>

      <button class="pet-menu-item" role="menuitem" @click="goCorner">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20V4h16"/><path d="M9 15l4-4 4 4"/></svg>
        回到角落
      </button>

      <button class="pet-menu-item is-danger" role="menuitem" @click="hidePet">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.9 17.9A9 9 0 0 1 3.5 9"/><path d="M8.6 5.2A9 9 0 0 1 20.5 15"/><path d="M3 3l18 18"/></svg>
        隐藏桌宠
      </button>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { createPetScene } from '@/three/petScene'
import { useThemeStore } from '@/store/theme'

const ACTIONS = [
  { key: 'happy', label: '开心', en: 'HAPPY', src: '/pet/pet_happy.png' },
  { key: 'cheer', label: '欢呼', en: 'CHEER', src: '/pet/pet_cheer.png' },
  { key: 'think', label: '思考', en: 'THINK', src: '/pet/pet_think.png' },
  { key: 'tired', label: '疲惫', en: 'TIRED', src: '/pet/pet_tired.png' }
]

const STORAGE_KEY = 'light-note:pet-visible'
const EDGE = 8
const IDLE_MS = 6000

const route = useRoute()
const themeStore = useThemeStore()
const petRef = ref(null)
const stageRef = ref(null)
const use3D = ref(true)
const hover = ref(false)
let petScene = null

const visible = ref(localStorage.getItem(STORAGE_KEY) !== 'false')
const isAdmin = computed(() => route.path.startsWith('/admin'))
const active = computed(() => visible.value && !isAdmin.value)

const currentKey = ref('happy')
const currentAction = computed(() => ACTIONS.find((a) => a.key === currentKey.value) || ACTIONS[0])

const pos = reactive({ x: EDGE, y: 200 })
const dir = reactive({ x: 1, y: 1 })
const facing = ref(1)
const box = reactive({ w: 132, h: 176 })

const dragging = ref(false)
const manualPaused = ref(false)
const menu = reactive({ open: false, x: 0, y: 0 })
const meteorShower = ref(false)
const rainbowVisible = ref(false)
let meteorTimer = 0
let preShowerKey = null

let rainbowTimer = 0
let preRainbowKey = null

const announce = computed(() => {
  if (meteorShower.value) return { cn: '流星雨', en: 'METEOR SHOWER', tone: 'meteor' }
  if (rainbowVisible.value) return { cn: '彩虹', en: 'RAINBOW', tone: 'rainbow' }
  return null
})

const paused = computed(() => dragging.value || menu.open || manualPaused.value)

const speed = reactive({ x: 0.25, y: 0.12 })

let raf = 0
let lastTs = 0
let dragStart = null
let dragMoved = false
let lastInteraction = 0
let idleTimer = 0

const reducedMotion = () =>
  typeof window !== 'undefined' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

const clamp = (v, min, max) => Math.min(Math.max(v, min), Math.max(min, max))

const maxX = () => window.innerWidth - box.w - EDGE
const maxY = () => window.innerHeight - box.h - EDGE

function measure() {
  const el = petRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  if (rect.width) box.w = rect.width
  if (rect.height) box.h = rect.height
  pos.x = clamp(pos.x, EDGE, maxX())
  pos.y = clamp(pos.y, EDGE, maxY())
}

function touch() {
  lastInteraction = Date.now()
}

function faceTowards(dx) {
  if (Math.abs(dx) < 1) return
  facing.value = dx < 0 ? -1 : 1
}

/* ===== 动作切换 ===== */
function pick(pool, excludeCurrent = true) {
  const list = pool.filter((a) => !excludeCurrent || a.key !== currentKey.value)
  return (list.length ? list : pool)[Math.floor(Math.random() * (list.length || pool.length))]
}

function setAction(key) {
  if (currentKey.value === key) return
  currentKey.value = key
  touch()
}

function randomAction() {
  const next = pick(ACTIONS)
  if (next) setAction(next.key)
}

function cycleAction() {
  const idx = ACTIONS.findIndex((a) => a.key === currentKey.value)
  setAction(ACTIONS[(idx + 1) % ACTIONS.length].key)
  closeMenu()
}

function idleAction() {
  if (meteorShower.value || rainbowVisible.value) return
  const next = pick(ACTIONS.filter((a) => a.key !== 'cheer'))
  if (next) setAction(next.key)
  touch()
}

/* ===== 流星雨联动 ===== */
function onMeteorShower(e) {
  const active = !!e?.detail?.active
  clearTimeout(meteorTimer)

  if (active) {
    if (!meteorShower.value) preShowerKey = currentKey.value
    meteorShower.value = true
    setAction('cheer')
    touch()
    const hold = Number(e?.detail?.duration) || 4200
    meteorTimer = window.setTimeout(() => {
      meteorShower.value = false
      if (preShowerKey) setAction(preShowerKey)
      preShowerKey = null
      touch()
    }, hold + 1400)
  } else {
    meteorShower.value = false
    if (preShowerKey) setAction(preShowerKey)
    preShowerKey = null
    touch()
  }
}

function onRainbow(e) {
  const active = !!e?.detail?.active
  clearTimeout(rainbowTimer)

  if (active) {
    if (!rainbowVisible.value) preRainbowKey = currentKey.value
    rainbowVisible.value = true
    setAction('happy')
    touch()
    const hold = Number(e?.detail?.duration) || 5200
    rainbowTimer = window.setTimeout(() => {
      rainbowVisible.value = false
      if (preRainbowKey) setAction(preRainbowKey)
      preRainbowKey = null
      touch()
    }, hold + 1200)
  } else {
    rainbowVisible.value = false
    if (preRainbowKey) setAction(preRainbowKey)
    preRainbowKey = null
    touch()
  }
}

/* ===== 拖动 ===== */
function onPointerDown(e) {
  if (e.button !== 0) return
  dragging.value = true
  dragMoved = false
  dragStart = { x: e.clientX, y: e.clientY, px: pos.x, py: pos.y }
  lastTs = 0
  try {
    e.currentTarget.setPointerCapture?.(e.pointerId)
  } catch {
    // 合成事件或已释放的指针，忽略即可
  }
  e.preventDefault()
}

function onPointerMove(e) {
  if (!dragging.value || !dragStart) return
  const dx = e.clientX - dragStart.x
  const dy = e.clientY - dragStart.y
  if (Math.hypot(dx, dy) > 4) dragMoved = true
  pos.x = clamp(dragStart.px + dx, EDGE, maxX())
  pos.y = clamp(dragStart.py + dy, EDGE, maxY())
  faceTowards(dx)
}

function onPointerUp() {
  if (!dragging.value) return
  dragging.value = false
  dragStart = null
  if (!dragMoved) randomAction()
  touch()
}

/* ===== 菜单 ===== */
function openMenu(e) {
  const mw = 188
  const mh = 212
  menu.x = clamp(e.clientX, EDGE, window.innerWidth - mw - EDGE)
  menu.y = clamp(e.clientY, EDGE, window.innerHeight - mh - EDGE)
  menu.open = true
  touch()
}

function closeMenu() {
  menu.open = false
}

function togglePause() {
  manualPaused.value = !manualPaused.value
  touch()
  closeMenu()
}

function goCorner() {
  manualPaused.value = false
  pos.x = EDGE
  pos.y = maxY()
  dir.x = 1
  dir.y = -1
  facing.value = 1
  touch()
  closeMenu()
}

function hidePet() {
  visible.value = false
  localStorage.setItem(STORAGE_KEY, 'false')
  closeMenu()
}

function revealPet() {
  visible.value = true
  localStorage.setItem(STORAGE_KEY, 'true')
  manualPaused.value = false
  measure()
  touch()
}

/* ===== 游走循环 ===== */
function tick(ts) {
  raf = requestAnimationFrame(tick)
  if (!lastTs) {
    lastTs = ts
    return
  }
  const dt = Math.min(ts - lastTs, 64)
  lastTs = ts

  if (paused.value) return

  const k = dt / 16.6667
  let nx = pos.x + dir.x * speed.x * k
  let ny = pos.y + dir.y * speed.y * k

  if (nx <= EDGE) {
    nx = EDGE
    dir.x = 1
  } else if (nx >= maxX()) {
    nx = maxX()
    dir.x = -1
  }

  if (ny <= EDGE) {
    ny = EDGE
    dir.y = 1
  } else if (ny >= maxY()) {
    ny = maxY()
    dir.y = -1
  }

  pos.x = nx
  pos.y = ny
  faceTowards(dir.x)
}

/* ===== 事件 ===== */
function onResize() {
  pos.x = clamp(pos.x, EDGE, maxX())
  pos.y = clamp(pos.y, EDGE, maxY())
  measure()
}

function onDocPointerDown(e) {
  if (menu.open && !e.target.closest('.pet-menu')) closeMenu()
}

function onKeydown(e) {
  if (e.key === 'Escape') closeMenu()
}

/* ===== 3D 场景 ===== */
const moodOf = (a) => (a?.tone === 'meteor' ? 'meteor' : a?.tone === 'rainbow' ? 'rainbow' : null)

async function ensureScene() {
  if (petScene) return
  if (window.matchMedia('(max-width: 768px)').matches) {
    use3D.value = false
    return
  }
  await nextTick()
  const host = stageRef.value
  if (!host) {
    use3D.value = false
    return
  }
  try {
    petScene = createPetScene({
      host,
      poses: ACTIONS.map(({ key, src }) => ({ key, src })),
      startKey: currentKey.value,
      dark: themeStore.isDark(),
      reducedMotion: reducedMotion()
    })
    petScene.setFacing(facing.value)
    petScene.setPaused(paused.value)
    petScene.setMood(moodOf(announce.value))
  } catch (err) {
    console.warn('[DeskPet] WebGL 初始化失败，回落到平面立绘', err)
    petScene = null
    use3D.value = false
  }
}

function teardownScene() {
  if (!petScene) return
  petScene.dispose()
  petScene = null
}

watch(currentKey, (k) => petScene?.setAction(k))
watch(facing, (f) => petScene?.setFacing(f))
watch(dragging, (d) => petScene?.setDragging(d))
watch(paused, (p) => petScene?.setPaused(p))
watch(hover, (h) => petScene?.setHover(h))
watch(announce, (a) => petScene?.setMood(moodOf(a)))
watch(() => themeStore.theme, (t) => petScene?.setTheme(t === 'dark'))

onMounted(() => {
  // 默认停在左下角，避开右下角的悬浮球
  pos.x = EDGE + 8
  pos.y = Math.max(EDGE, window.innerHeight - box.h - 32)
  dir.x = 1
  dir.y = -1

  // 预加载全部表情，避免切换时闪烁
  ACTIONS.forEach((a) => {
    const img = new Image()
    img.src = a.src
  })

  const el = petRef.value
  if (el) {
    const styles = getComputedStyle(el)
    const sx = parseFloat(styles.getPropertyValue('--speed-x'))
    const sy = parseFloat(styles.getPropertyValue('--speed-y'))
    if (!Number.isNaN(sx)) speed.x = sx
    if (!Number.isNaN(sy)) speed.y = sy
  }

  if (reducedMotion()) manualPaused.value = true

  requestAnimationFrame(() => {
    measure()
    raf = requestAnimationFrame(tick)
  })

  window.addEventListener('pointermove', onPointerMove, { passive: true })
  window.addEventListener('pointerup', onPointerUp)
  window.addEventListener('resize', onResize, { passive: true })
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('light-note:meteor-shower', onMeteorShower)
  window.addEventListener('light-note:rainbow', onRainbow)
  document.addEventListener('pointerdown', onDocPointerDown, true)
  touch()

  if (active.value) ensureScene()

  idleTimer = window.setInterval(() => {
    if (dragging.value || menu.open) return
    if (Date.now() - lastInteraction >= IDLE_MS) idleAction()
  }, 1000)
})

onUnmounted(() => {
  teardownScene()
  cancelAnimationFrame(raf)
  clearInterval(idleTimer)
  clearTimeout(meteorTimer)
  clearTimeout(rainbowTimer)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('light-note:meteor-shower', onMeteorShower)
  window.removeEventListener('light-note:rainbow', onRainbow)
  document.removeEventListener('pointerdown', onDocPointerDown, true)
})

watch(active, (on) => {
  if (on) {
    ensureScene()
    requestAnimationFrame(() => {
      measure()
      lastTs = 0
    })
  } else {
    teardownScene()
  }
})
</script>

<style lang="scss" scoped>
/* ============================================================
   桌面桌宠
   游走速度可通过 CSS 变量调整（建议 0.25 / 0.12）
   ============================================================ */
.desk-pet {
  --speed-x: 0.25px;
  --speed-y: 0.12px;
  --pet-h: 168px;
  --pet-w: 132px;

  position: fixed;
  left: 0;
  top: 0;
  z-index: 9000;
  width: auto;
  height: var(--pet-h);
  cursor: grab;
  touch-action: none;
  user-select: none;
  -webkit-user-select: none;
  will-change: transform;
  transition: opacity 0.25s ease;

  /* 3D 舞台：底部多留出一段，容纳地面接触阴影与光环 */
  &.is-3d {
    width: var(--pet-w);
    height: calc(var(--pet-h) + 26px);
  }

  &:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 6px;
    border-radius: 12px;
  }

  &.is-dragging {
    cursor: grabbing;

    .pet-img {
      transform: scale(1.04) rotate(-2deg);
    }

    .pet-shadow {
      opacity: 0.28;
      transform: translateX(-50%) scale(0.92);
    }
  }

  &.is-paused .pet-bob {
    animation-play-state: paused;
  }
}

.pet-stage {
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.pet-facing {
  height: 100%;
  transform-origin: center center;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.pet-bob {
  height: 100%;
  animation: pet-bob 2.8s ease-in-out infinite;
}

.pet-img {
  height: var(--pet-h);
  width: auto;
  display: block;
  pointer-events: none;
  filter: drop-shadow(0 10px 18px rgba(80, 60, 120, 0.22));
  transition: transform 0.22s ease;
  animation: pet-pop 0.26s cubic-bezier(0.34, 1.4, 0.64, 1);
}

.pet-shadow {
  position: absolute;
  left: 50%;
  bottom: -4px;
  width: 64%;
  height: 12px;
  transform: translateX(-50%);
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(40, 30, 70, 0.35), transparent 72%);
  filter: blur(5px);
  opacity: 0.55;
  transition: opacity 0.2s ease, transform 0.2s ease;
  pointer-events: none;
}

.pet-tip {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 10px);
  transform: translateX(-50%) translateY(6px) scale(0.96);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 7px 15px 6px;
  border-radius: 14px;
  background: var(--color-bg-elevated);
  border: 1px solid rgba(184, 146, 255, 0.42);
  box-shadow:
    0 10px 26px rgba(120, 80, 200, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.22s ease, transform 0.24s cubic-bezier(0.34, 1.3, 0.64, 1),
    box-shadow 0.22s ease;
}

.tip-cn {
  font-family: var(--font-artistic);
  font-size: 19px;
  font-weight: 400;
  line-height: 1.05;
  letter-spacing: 0.08em;
  background: linear-gradient(120deg, #8b5cf6 0%, #db2777 62%, #f472b6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.tip-en {
  font-style: normal;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: #b39ddb;
  text-transform: uppercase;
}

.tip-tail {
  position: absolute;
  left: 50%;
  bottom: -5px;
  width: 10px;
  height: 10px;
  transform: translateX(-50%) rotate(45deg);
  background: var(--color-bg-elevated);
  border-right: 1px solid rgba(184, 146, 255, 0.42);
  border-bottom: 1px solid rgba(184, 146, 255, 0.42);
  border-radius: 2px;
}

/* 桌宠靠近顶部时，气泡翻到下方 */
.pet-tip.is-below {
  top: calc(100% + 10px);
  bottom: auto;

  .tip-tail {
    top: -5px;
    bottom: auto;
    border-right: none;
    border-bottom: none;
    border-left: 1px solid rgba(184, 146, 255, 0.42);
    border-top: 1px solid rgba(184, 146, 255, 0.42);
  }
}

.desk-pet:hover .pet-tip,
.pet-tip.is-meteor,
.pet-tip.is-rainbow {
  opacity: 1;
  transform: translateX(-50%) translateY(0) scale(1);
}

/* 彩虹状态：七彩渐变文字 + 柔和彩色光晕 */
.pet-tip.is-rainbow {
  border-color: rgba(167, 139, 250, 0.55);
  animation: rainbow-tip-glow 2.6s ease-in-out infinite;

  .tip-cn {
    background: linear-gradient(
      100deg,
      #f472b6 0%,
      #fbbf24 24%,
      #34d399 48%,
      #38bdf8 70%,
      #a78bfa 100%
    );
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    letter-spacing: 0.14em;
  }

  .tip-en {
    color: #a78bfa;
  }

  .tip-tail {
    border-color: rgba(167, 139, 250, 0.55);
  }
}

@keyframes rainbow-tip-glow {
  0%, 100% {
    box-shadow: 0 10px 26px rgba(167, 139, 250, 0.24), 0 0 16px rgba(56, 189, 248, 0.14);
  }
  50% {
    box-shadow: 0 12px 32px rgba(244, 114, 182, 0.28), 0 0 24px rgba(52, 211, 153, 0.2);
  }
}

/* 流星雨状态：冷色描边 + 呼吸光 */
.pet-tip.is-meteor {
  border-color: rgba(125, 211, 252, 0.6);
  animation: meteor-tip-glow 1.8s ease-in-out infinite;

  .tip-cn {
    background: linear-gradient(120deg, #38bdf8 0%, #818cf8 55%, #c084fc 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    letter-spacing: 0.12em;
  }

  .tip-en {
    color: #7dd3fc;
  }

  .tip-tail {
    border-color: rgba(125, 211, 252, 0.6);
  }
}

@keyframes meteor-tip-glow {
  0%, 100% {
    box-shadow: 0 10px 26px rgba(56, 189, 248, 0.26), 0 0 16px rgba(139, 92, 246, 0.18);
  }
  50% {
    box-shadow: 0 12px 32px rgba(56, 189, 248, 0.42), 0 0 26px rgba(139, 92, 246, 0.3);
  }
}

@keyframes pet-bob {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

@keyframes pet-pop {
  from { opacity: 0.4; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
}

/* ===== 召回按钮 ===== */
.pet-recall {
  position: fixed;
  left: 18px;
  bottom: 22px;
  z-index: 9000;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: var(--color-bg-elevated);
  box-shadow: var(--shadow-md);
  font-size: 17px;
  line-height: 1;
  cursor: pointer;
  opacity: 0.85;
  transition: all 0.2s ease;

  &:hover {
    opacity: 1;
    transform: translateY(-2px);
    box-shadow: var(--shadow-lg);
  }
}

/* ===== 右键菜单 ===== */
.pet-menu {
  position: fixed;
  z-index: 10001;
  min-width: 176px;
  padding: 6px;
  border-radius: 14px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-lg);
  backdrop-filter: blur(18px);
  animation: menu-in 0.14s ease both;
}

@keyframes menu-in {
  from { opacity: 0; transform: translateY(-4px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.pet-menu-title {
  margin: 4px 8px 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.pet-menu-item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 9px 10px;
  border-radius: 9px;
  border: none;
  background: transparent;
  color: var(--color-text);
  font-size: 13px;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.16s ease, color 0.16s ease;

  svg {
    flex-shrink: 0;
    opacity: 0.75;
  }

  &:hover {
    background: var(--color-google-blue-light);
    color: var(--color-primary);
  }

  &.is-danger:hover {
    background: rgba(234, 67, 53, 0.1);
    color: #d93025;
  }
}

/* ===== 响应式 / 减少动效 ===== */
@media (max-width: 768px) {
  .desk-pet,
  .pet-recall {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .pet-bob,
  .pet-img {
    animation: none;
  }
}
</style>
