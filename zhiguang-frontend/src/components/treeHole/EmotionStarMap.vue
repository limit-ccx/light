<template>
  <div class="star-map">
    <div class="map-sky" ref="wrapEl">
    <canvas
      ref="canvasEl"
      role="img"
      :aria-label="ariaLabel"
      @pointermove="onPointerMove"
      @pointerdown="onPointerDown"
      @pointerleave="hoverKey = null"
    ></canvas>

    <p class="map-hint">
      <span v-html="iconStar"></span>
      每颗星是一封心事：越亮，收到的拥抱越多
    </p>

    <div v-if="hoverStar" class="map-tip" :style="tipStyle">
      <span class="tip-emotion" :style="{ color: hoverStar.color }">
        <span class="tip-dot" :style="{ background: hoverStar.color }"></span>
        {{ hoverStar.label }}
      </span>
      <p class="tip-text">{{ hoverStar.excerpt }}</p>
      <div class="tip-meta">
        <span>🫂 {{ hoverStar.empathyCount }}</span>
        <span>❤️ {{ hoverStar.likeCount }}</span>
        <span>💬 {{ hoverStar.replyCount }}</span>
      </div>
    </div>

    <p v-if="!stars.length && !loading" class="map-empty">夜空还没有星星，等第一封心事点亮它。</p>
    </div>

    <div v-if="stars.length" class="map-legend">
      <button
        v-for="item in legend"
        :key="item.key"
        type="button"
        class="legend-item"
        :class="{ active: activeEmotion === item.key }"
        :style="{ '--legend-color': item.color, background: activeEmotion === item.key ? item.color + '2b' : null }"
        @click="$emit('filter', activeEmotion === item.key ? 'all' : item.key)"
      >
        <i :style="{ background: item.color, boxShadow: `0 0 10px ${item.color}` }"></i>
        <span class="legend-label">{{ item.label }}</span>
        <span class="legend-count">{{ item.count }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { emotionTags } from '@/api/treeHole'
import { NIGHT_EMOTION_ORDER, emotionIcon, nightColor } from '@/data/treeHoleNight'

const props = defineProps({
  posts: { type: Array, default: () => [] },
  activeEmotion: { type: String, default: 'all' },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['select', 'filter'])

const wrapEl = ref(null)
const canvasEl = ref(null)
const hoverKey = ref(null)

const iconStar = emotionIcon('excitement')
const labelMap = Object.fromEntries(emotionTags.map((t) => [t.key, t.label]))

const reducedMotion =
  typeof window !== 'undefined' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

// 用帖子 id 生成稳定随机数，保证每次刷新星位不跳
const seeded = (id, salt) => {
  let h = (Math.abs(Number(id) || 0) * 2654435761 + salt * 40503) >>> 0
  h ^= h >> 16
  h = Math.imul(h, 2246822519) >>> 0
  h ^= h >> 13
  return (h >>> 0) / 4294967295
}

const engagement = (post) =>
  (post.likeCount || 0) + (post.empathyCount || 0) * 1.6 + (post.replyCount || 0) * 2.2

const sectorIndex = (key) => {
  const i = NIGHT_EMOTION_ORDER.indexOf(key)
  return i === -1 ? NIGHT_EMOTION_ORDER.length - 1 : i
}

const stars = computed(() => {
  const list = props.posts || []
  const maxEngagement = Math.max(1, ...list.map(engagement))
  const sectors = NIGHT_EMOTION_ORDER.length
  return list.map((post) => {
    const key = NIGHT_EMOTION_ORDER.includes(post.emotionTag) ? post.emotionTag : 'other'
    const e = engagement(post) / maxEngagement
    return {
      id: post.id,
      post,
      emotion: key,
      color: nightColor(key),
      label: labelMap[key] || '其他',
      excerpt: (post.content || '').replace(/\s+/g, ' ').slice(0, 46),
      likeCount: post.likeCount || 0,
      empathyCount: post.empathyCount || 0,
      replyCount: post.replyCount || 0,
      u: (-0.88 + (sectorIndex(key) / (sectors - 1)) * 1.76) + (seeded(post.id, 3) - 0.5) * 0.16,
      v: 0.2 + e * 0.62 + seeded(post.id, 7) * 0.18,
      size: 2.4 + e * 4.6,
      phase: seeded(post.id, 11) * Math.PI * 2,
      x: 0,
      y: 0
    }
  })
})

const legend = computed(() => {
  const counts = {}
  stars.value.forEach((s) => { counts[s.emotion] = (counts[s.emotion] || 0) + 1 })
  return NIGHT_EMOTION_ORDER.filter((k) => counts[k]).map((k) => ({
    key: k,
    label: labelMap[k],
    color: nightColor(k),
    count: counts[k]
  }))
})

const hoverStar = computed(() => stars.value.find((s) => s.id === hoverKey.value) || null)

const ariaLabel = computed(() =>
  `情绪星图，共 ${stars.value.length} 封心事` +
  (legend.value.length ? `：${legend.value.map((l) => `${l.label}${l.count}`).join('，')}` : '')
)

// 顶部星星的悬浮卡改为向下展开，避免被星图边缘裁掉
const tipStyle = computed(() => {
  const star = hoverStar.value
  const box = size
  if (!star || !box.w) return { opacity: 0 }
  const half = 130
  const left = Math.min(Math.max(star.x, half), Math.max(half, box.w - half))
  const below = star.y < 150
  return {
    left: `${left}px`,
    top: `${below ? star.y + star.size + 10 : star.y - star.size - 10}px`,
    transform: below ? 'translate(-50%, 0)' : 'translate(-50%, -100%)',
    borderColor: `${star.color}55`
  }
})

let size = { w: 0, h: 0, dpr: 1 }
let ctx = null
let rafId = 0
let time = 0
let resizeObserver = null

const layout = () => {
  const { w, h } = size
  if (!w || !h) return
  const cx = w / 2
  const baseY = h * 0.88
  const rx = w * 0.44
  const sky = h * 0.78
  stars.value.forEach((s) => {
    s.x = cx + s.u * rx
    s.y = baseY - Math.min(0.98, s.v) * sky
  })
}

const draw = () => {
  if (!ctx) return
  const { w, h } = size
  ctx.clearRect(0, 0, w, h)

  const cx = w / 2
  const holeY = h * 0.88

  // 情绪星云：每个有星星的情绪扇区铺一层极淡的光晕
  legend.value.forEach((item) => {
    const idx = NIGHT_EMOTION_ORDER.indexOf(item.key)
    const u = -0.88 + (idx / (NIGHT_EMOTION_ORDER.length - 1)) * 1.76
    const nx = cx + u * w * 0.44
    const ny = h * 0.42
    const r = Math.min(w, h) * 0.42
    const dim = props.activeEmotion !== 'all' && props.activeEmotion !== item.key
    const g = ctx.createRadialGradient(nx, ny, 0, nx, ny, r)
    g.addColorStop(0, hexA(item.color, dim ? 0.02 : 0.075))
    g.addColorStop(1, hexA(item.color, 0))
    ctx.fillStyle = g
    ctx.fillRect(nx - r, ny - r, r * 2, r * 2)
  })

  // 树洞本体
  const holeR = Math.max(46, Math.min(96, w * 0.09))
  const holeGlow = ctx.createRadialGradient(cx, holeY, 0, cx, holeY, holeR * 3.4)
  holeGlow.addColorStop(0, 'rgba(150, 178, 255, 0.5)')
  holeGlow.addColorStop(0.35, 'rgba(96, 122, 224, 0.18)')
  holeGlow.addColorStop(1, 'rgba(20, 30, 70, 0)')
  ctx.fillStyle = holeGlow
  ctx.fillRect(cx - holeR * 3.4, holeY - holeR * 3.4, holeR * 6.8, holeR * 6.8)

  ctx.beginPath()
  ctx.ellipse(cx, holeY, holeR, holeR * 0.3, 0, 0, Math.PI * 2)
  ctx.fillStyle = 'rgba(4, 6, 14, 0.72)'
  ctx.fill()
  ctx.strokeStyle = 'rgba(178, 202, 255, 0.55)'
  ctx.lineWidth = 1.4
  ctx.stroke()

  // 星
  stars.value.forEach((s) => {
    const dimmed = props.activeEmotion !== 'all' && s.emotion !== props.activeEmotion
    const tw = reducedMotion ? 0.85 : 0.72 + 0.28 * Math.sin(time * 1.6 + s.phase)
    const hovered = hoverKey.value === s.id
    const alpha = (dimmed ? 0.18 : 0.95) * tw
    const r = s.size * (hovered ? 1.5 : 1)

    if (hovered) {
      ctx.beginPath()
      ctx.moveTo(cx, holeY)
      ctx.lineTo(s.x, s.y)
      ctx.strokeStyle = hexA(s.color, 0.28)
      ctx.lineWidth = 1
      ctx.stroke()
    }

    const g = ctx.createRadialGradient(s.x, s.y, 0, s.x, s.y, r * 3.2)
    g.addColorStop(0, hexA(s.color, alpha))
    g.addColorStop(0.4, hexA(s.color, alpha * 0.28))
    g.addColorStop(1, hexA(s.color, 0))
    ctx.fillStyle = g
    ctx.beginPath()
    ctx.arc(s.x, s.y, r * 3.2, 0, Math.PI * 2)
    ctx.fill()

    ctx.beginPath()
    ctx.arc(s.x, s.y, r * 0.72, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(255, 255, 255, ${Math.min(1, alpha + 0.15)})`
    ctx.fill()

    if (hovered) {
      ctx.beginPath()
      ctx.arc(s.x, s.y, r * 2.1, 0, Math.PI * 2)
      ctx.strokeStyle = hexA(s.color, 0.85)
      ctx.lineWidth = 1.4
      ctx.stroke()
    }
  })
}

const hexA = (hex, a) => {
  const v = hex.replace('#', '')
  const n = parseInt(v.length === 3 ? v.split('').map((c) => c + c).join('') : v, 16)
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${a})`
}

const loop = () => {
  time += 0.016
  draw()
  rafId = requestAnimationFrame(loop)
}

const resize = () => {
  const canvas = canvasEl.value
  const wrap = wrapEl.value
  if (!canvas || !wrap) return
  const w = wrap.clientWidth
  const h = wrap.clientHeight
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  size = { w, h, dpr }
  canvas.width = Math.max(1, w * dpr)
  canvas.height = Math.max(1, h * dpr)
  canvas.style.width = `${w}px`
  canvas.style.height = `${h}px`
  ctx = canvas.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  layout()
  draw()
}

const hitTest = (event) => {
  const canvas = canvasEl.value
  if (!canvas) return null
  const rect = canvas.getBoundingClientRect()
  const px = event.clientX - rect.left
  const py = event.clientY - rect.top
  let best = null
  let bestDist = 0
  stars.value.forEach((s) => {
    const d = Math.hypot(s.x - px, s.y - py)
    const reach = Math.max(15, s.size + 9)
    if (d <= reach && (!best || d < bestDist)) {
      best = s
      bestDist = d
    }
  })
  return best
}

const onPointerMove = (event) => {
  const hit = hitTest(event)
  hoverKey.value = hit ? hit.id : null
  if (canvasEl.value) canvasEl.value.style.cursor = hit ? 'pointer' : 'default'
  if (reducedMotion) draw()
}

const onPointerDown = (event) => {
  const hit = hitTest(event)
  if (hit) emit('select', hit.post)
}

watch(
  () => [props.posts, props.activeEmotion],
  () => {
    layout()
    if (reducedMotion) draw()
  },
  { deep: false }
)

onMounted(() => {
  resize()
  resizeObserver = new ResizeObserver(resize)
  if (wrapEl.value) resizeObserver.observe(wrapEl.value)
  if (!reducedMotion) rafId = requestAnimationFrame(loop)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.star-map {
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid rgba(148, 170, 220, 0.14);
  background: linear-gradient(180deg, rgba(9, 13, 28, 0.6), rgba(6, 9, 20, 0.85));
}

.map-sky {
  position: relative;
  height: clamp(270px, 34vw, 380px);
  background:
    radial-gradient(120% 80% at 50% 108%, rgba(84, 112, 214, 0.24), transparent 62%),
    linear-gradient(180deg, rgba(9, 13, 28, 0.86) 0%, rgba(6, 9, 20, 0.94) 100%);
}

canvas {
  display: block;
  width: 100%;
  height: 100%;
  touch-action: manipulation;
}

.map-hint {
  position: absolute;
  top: 14px;
  left: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(148, 170, 220, 0.16);
  background: rgba(10, 15, 32, 0.55);
  color: rgba(200, 216, 255, 0.72);
  font-size: 12px;
  letter-spacing: 0.02em;
  pointer-events: none;
  backdrop-filter: blur(6px);
}

.map-hint :deep(svg) {
  color: #c9b8ff;
}

.map-tip {
  position: absolute;
  z-index: 5;
  width: min(270px, 72vw);
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid;
  background: rgba(12, 17, 36, 0.94);
  box-shadow: 0 18px 40px rgba(2, 4, 12, 0.6);
  pointer-events: none;
  transition: left 0.12s ease, top 0.12s ease;
}

.tip-emotion {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.tip-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.tip-text {
  margin: 7px 0 8px;
  font-size: 13px;
  line-height: 1.65;
  color: rgba(226, 235, 255, 0.9);
}

.tip-meta {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: rgba(180, 198, 240, 0.7);
}

.map-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 12px 14px;
  border-top: 1px solid rgba(148, 170, 220, 0.1);
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid rgba(148, 170, 220, 0.16);
  background: rgba(9, 13, 28, 0.6);
  color: rgba(214, 226, 255, 0.78);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(6px);
}

.legend-item i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-item:hover {
  border-color: var(--legend-color);
  color: #fff;
}

.legend-item.active {
  border-color: var(--legend-color);
  color: #fff;
}

.legend-count {
  font-variant-numeric: tabular-nums;
  opacity: 0.7;
}

.map-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  margin: 0;
  font-size: 13px;
  color: rgba(190, 208, 245, 0.6);
}

@media (max-width: 640px) {
  .star-map {
    border-radius: 18px;
  }

  .map-sky {
    height: 260px;
  }

  .map-hint {
    font-size: 11px;
    padding: 5px 10px;
  }

  .legend-item {
    padding: 4px 9px;
    font-size: 11.5px;
  }
}
</style>
