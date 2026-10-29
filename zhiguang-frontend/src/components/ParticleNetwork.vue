<template>
  <canvas
    ref="canvasRef"
    class="particle-network"
  ></canvas>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  interactive: { type: Boolean, default: true },
  density: { type: Number, default: 1 },
  glowIntensity: { type: Number, default: 1 }
})

const canvasRef = ref(null)
let animationId = null
let cleanupFns = []

/* ========== 配置 ========== */
const config = {
  baseParticleCount: 140,
  starCount: 70,
  glowOrbCount: 12,
  maxSpeed: 1.2,
  linkDistance: 170,
  mouseLinkDistance: 220,
  mouseRepelDistance: 100,
  mouseRepelForce: 0.04,
  meteorInterval: 9000,
  ambientFirstDelay: 11000,
  ambientMinGap: 26000,
  ambientMaxGap: 44000,
  showerDuration: 3800,
  rainbowDuration: 5200,
  showerMeteorMin: 18,
  showerMeteorMax: 28,
  colorsLight: {
    primary: [10, 80, 190],
    secondary: [30, 100, 210],
    accent1: [15, 130, 50],
    accent2: [130, 40, 210],
    accent3: [190, 30, 120]
  },
  colorsDark: {
    primary: [138, 180, 248],
    secondary: [174, 203, 250],
    accent1: [129, 201, 149],
    accent2: [197, 138, 249],
    accent3: [242, 139, 189]
  }
}

function isDark() {
  return document.documentElement.getAttribute('data-theme') === 'dark'
    || (!document.documentElement.getAttribute('data-theme') && window.matchMedia('(prefers-color-scheme: dark)').matches)
}

function getColorSet() {
  return isDark() ? config.colorsDark : config.colorsLight
}

function rgba(colorArr, alpha) {
  const multiplier = isDark() ? 1.0 : 2.2
  return `rgba(${colorArr[0]},${colorArr[1]},${colorArr[2]},${Math.min(alpha * multiplier, 1)})`
}

function lerpColor(a, b, t) {
  return [
    a[0] + (b[0] - a[0]) * t,
    a[1] + (b[1] - a[1]) * t,
    a[2] + (b[2] - a[2]) * t
  ]
}

function randomFromArray(arr) {
  return arr[Math.floor(Math.random() * arr.length)]
}

/* ========== 粒子类 ========== */
class Particle {
  constructor(canvas, type = 'normal') {
    this.canvas = canvas
    this.type = type
    this.init()
  }

  init() {
    const c = this.canvas
    this.x = Math.random() * c.width
    this.y = Math.random() * c.height
    this.depth = 0.3 + Math.random() * 0.7

    if (this.type === 'normal') {
      this.vx = (Math.random() - 0.5) * 0.6
      this.vy = (Math.random() - 0.5) * 0.6
      this.radius = (1 + Math.random() * 2) * this.depth
      this.pulsePhase = Math.random() * Math.PI * 2
      this.pulseSpeed = 0.008 + Math.random() * 0.012
      const colorKeys = Object.keys(getColorSet())
      this.colorKey = randomFromArray(colorKeys)
    } else if (this.type === 'glow') {
      this.vx = (Math.random() - 0.5) * 0.15
      this.vy = (Math.random() - 0.5) * 0.15
      this.radius = 20 + Math.random() * 40
      this.pulsePhase = Math.random() * Math.PI * 2
      this.pulseSpeed = 0.003 + Math.random() * 0.005
      const colorKeys = Object.keys(getColorSet())
      this.colorKey = randomFromArray(colorKeys)
    }
  }

  update(mouse) {
    const c = this.canvas
    const cssW = c._cssW || c.width
    const cssH = c._cssH || c.height

    if (this.type === 'normal' && mouse.x != null) {
      const dx = mouse.x - this.x
      const dy = mouse.y - this.y
      const dist = Math.hypot(dx, dy)

      if (dist < config.mouseRepelDistance && dist > 0) {
        const force = ((config.mouseRepelDistance - dist) / config.mouseRepelDistance) * config.mouseRepelForce
        this.vx -= (dx / dist) * force
        this.vy -= (dy / dist) * force
      } else if (dist < 180 && dist > 0) {
        const force = (180 - dist) / 180 * 0.008
        this.vx += (dx / dist) * force
        this.vy += (dy / dist) * force
      }
    }

    this.x += this.vx * this.depth
    this.y += this.vy * this.depth

    const margin = this.type === 'glow' ? this.radius : 0
    if (this.x < -margin) this.x = cssW + margin
    if (this.x > cssW + margin) this.x = -margin
    if (this.y < -margin) this.y = cssH + margin
    if (this.y > cssH + margin) this.y = -margin

    const speed = Math.hypot(this.vx, this.vy)
    const maxSpd = config.maxSpeed * (this.type === 'glow' ? 0.3 : 1)
    if (speed > maxSpd) {
      this.vx = (this.vx / speed) * maxSpd
      this.vy = (this.vy / speed) * maxSpd
    }

    // gentle friction
    this.vx *= 0.999
    this.vy *= 0.999

    // minimum speed to keep things alive
    if (speed < 0.1 && this.type === 'normal') {
      this.vx += (Math.random() - 0.5) * 0.05
      this.vy += (Math.random() - 0.5) * 0.05
    }

    this.pulsePhase += this.pulseSpeed
  }

  draw(ctx, glowIntensity) {
    const colors = getColorSet()
    const color = colors[this.colorKey]

    if (this.type === 'glow') {
      const pulse = 0.5 + 0.5 * Math.sin(this.pulsePhase)
      const alpha = (0.02 + 0.02 * pulse) * glowIntensity
      const grad = ctx.createRadialGradient(this.x, this.y, 0, this.x, this.y, this.radius)
      grad.addColorStop(0, rgba(color, alpha * 1.5))
      grad.addColorStop(0.4, rgba(color, alpha * 0.6))
      grad.addColorStop(1, rgba(color, 0))
      ctx.fillStyle = grad
      ctx.beginPath()
      ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2)
      ctx.fill()
      return
    }

    // Normal particle with glow
    const pulse = 0.7 + 0.3 * Math.sin(this.pulsePhase)
    const r = this.radius * pulse

    // Outer glow
    if (glowIntensity > 0) {
      const glowR = r * (3 + glowIntensity * 2)
      const glowGrad = ctx.createRadialGradient(this.x, this.y, r * 0.5, this.x, this.y, glowR)
      glowGrad.addColorStop(0, rgba(color, 0.15 * glowIntensity))
      glowGrad.addColorStop(0.5, rgba(color, 0.04 * glowIntensity))
      glowGrad.addColorStop(1, rgba(color, 0))
      ctx.fillStyle = glowGrad
      ctx.beginPath()
      ctx.arc(this.x, this.y, glowR, 0, Math.PI * 2)
      ctx.fill()
    }

    // Core
    const coreGrad = ctx.createRadialGradient(this.x, this.y, 0, this.x, this.y, r)
    coreGrad.addColorStop(0, rgba([255, 255, 255], 0.9))
    coreGrad.addColorStop(0.3, rgba(color, 0.8))
    coreGrad.addColorStop(1, rgba(color, 0.3))
    ctx.fillStyle = coreGrad
    ctx.beginPath()
    ctx.arc(this.x, this.y, r, 0, Math.PI * 2)
    ctx.fill()
  }
}

/* ========== 星屑 ========== */
class Star {
  constructor(w, h) {
    this.x = Math.random() * w
    this.y = Math.random() * h
    this.r = 0.4 + Math.random() * 1.2
    this.phase = Math.random() * Math.PI * 2
    this.speed = 0.012 + Math.random() * 0.03
    this.depth = 0.25 + Math.random() * 0.75
    this.drift = (Math.random() - 0.5) * 0.06
  }

  update(h) {
    this.phase += this.speed
    this.y += this.drift
    if (this.y < -4) this.y = h + 4
    if (this.y > h + 4) this.y = -4
  }

  draw(ctx) {
    const twinkle = 0.5 + 0.5 * Math.sin(this.phase)
    const alpha = (0.12 + 0.5 * twinkle) * this.depth
    const color = isDark() ? [208, 226, 255] : [96, 132, 208]
    ctx.fillStyle = `rgba(${color[0]},${color[1]},${color[2]},${alpha.toFixed(3)})`
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.r * (0.8 + twinkle * 0.5), 0, Math.PI * 2)
    ctx.fill()
  }
}

/* ========== 彩虹（亮色模式的环境效果） ========== */
class Rainbow {
  constructor(w, h) {
    this.t = 0
    this.fadeIn = 900
    this.hold = 2400
    this.fadeOut = 1900
    this.total = this.fadeIn + this.hold + this.fadeOut
    this.cx = w * 0.5
    this.cy = h * 1.04
    this.radius = Math.min(w * 0.46, h * 0.94)
    this.band = Math.max(6, Math.min(w, h) * 0.019)
    this.active = true
    this.bands = [
      [255, 158, 205],
      [255, 214, 165],
      [253, 246, 165],
      [167, 243, 208],
      [165, 216, 255],
      [196, 181, 253]
    ]
  }

  update(dt) {
    this.t += dt
    if (this.t >= this.total) this.active = false
  }

  opacity() {
    if (this.t < this.fadeIn) return this.t / this.fadeIn
    if (this.t < this.fadeIn + this.hold) return 1
    return Math.max(0, 1 - (this.t - this.fadeIn - this.hold) / this.fadeOut)
  }

  draw(ctx) {
    const a = this.opacity()
    if (a <= 0) return

    const start = Math.PI * 1.06
    const end = Math.PI * 1.94
    const light = !isDark()

    ctx.save()
    ctx.lineCap = 'round'

    this.bands.forEach((c, i) => {
      const r = this.radius + i * this.band * 1.2

      // 外层柔光
      ctx.beginPath()
      ctx.arc(this.cx, this.cy, r, start, end)
      ctx.strokeStyle = `rgba(${c[0]},${c[1]},${c[2]},${((light ? 0.1 : 0.05) * a).toFixed(3)})`
      ctx.lineWidth = this.band * 3.2
      ctx.stroke()

      // 主色带
      ctx.beginPath()
      ctx.arc(this.cx, this.cy, r, start, end)
      ctx.strokeStyle = `rgba(${c[0]},${c[1]},${c[2]},${((light ? 0.34 : 0.2) * a).toFixed(3)})`
      ctx.lineWidth = this.band
      ctx.stroke()
    })

    ctx.restore()
  }
}

/* ========== 流星类 ========== */
class Meteor {
  constructor(canvas) {
    this.canvas = canvas
    this.reset()
  }

  reset() {
    const c = this.canvas
    this.active = true
    this.x = Math.random() * c.width * 1.15 - c.width * 0.12
    this.y = -30 - Math.random() * 80
    const angle = Math.PI / 3.5 + (Math.random() - 0.5) * 0.34
    const speed = 6 + Math.random() * 6
    this.vx = Math.cos(angle) * speed
    this.vy = Math.sin(angle) * speed
    this.trail = []
    this.maxTrail = 26 + Math.floor(Math.random() * 18)
    this.life = 1
    this.decay = 0.004 + Math.random() * 0.005
    this.radius = 1.2 + Math.random() * 1.8

    // 少量「火流星」：更大、更慢、拖尾更长
    this.fireball = Math.random() < 0.14
    if (this.fireball) {
      this.radius *= 1.9
      this.maxTrail += 16
      this.decay *= 0.85
      this.vx *= 0.88
      this.vy *= 0.88
    }

    const colorKeys = Object.keys(getColorSet())
    this.colorKey = randomFromArray(colorKeys)
  }

  update() {
    if (!this.active) return
    this.trail.unshift({ x: this.x, y: this.y })
    if (this.trail.length > this.maxTrail) this.trail.pop()
    this.x += this.vx
    this.y += this.vy
    this.life -= this.decay
    if (this.life <= 0 || this.x > this.canvas.width + 80 || this.y > this.canvas.height + 80) {
      this.active = false
    }
  }

  draw(ctx) {
    if (!this.active || this.trail.length < 2) return
    const colors = getColorSet()
    const color = colors[this.colorKey]

    // 尾部柔光
    for (let i = 0; i < this.trail.length - 1; i++) {
      const t = 1 - i / this.trail.length
      const alpha = t * t * this.life * 0.16
      ctx.beginPath()
      ctx.moveTo(this.trail[i].x, this.trail[i].y)
      ctx.lineTo(this.trail[i + 1].x, this.trail[i + 1].y)
      ctx.strokeStyle = rgba(color, alpha)
      ctx.lineWidth = this.radius * t * 5
      ctx.lineCap = 'round'
      ctx.stroke()
    }

    // 明亮核心轨迹
    for (let i = 0; i < this.trail.length - 1; i++) {
      const t = 1 - i / this.trail.length
      const alpha = t * t * this.life * 0.9
      const width = this.radius * t * 2.2

      ctx.beginPath()
      ctx.moveTo(this.trail[i].x, this.trail[i].y)
      ctx.lineTo(this.trail[i + 1].x, this.trail[i + 1].y)
      ctx.strokeStyle = rgba(color, alpha)
      ctx.lineWidth = width
      ctx.lineCap = 'round'
      ctx.stroke()
    }

    // 白热核心（贴近头部的几段）
    const coreLen = Math.min(5, this.trail.length - 1)
    for (let i = 0; i < coreLen - 1; i++) {
      const t = 1 - i / coreLen
      ctx.beginPath()
      ctx.moveTo(this.trail[i].x, this.trail[i].y)
      ctx.lineTo(this.trail[i + 1].x, this.trail[i + 1].y)
      ctx.strokeStyle = `rgba(255,255,255,${(0.5 * this.life * t).toFixed(3)})`
      ctx.lineWidth = this.radius * t * 1.1
      ctx.lineCap = 'round'
      ctx.stroke()
    }

    // 头部十字耀斑
    const flare = this.radius * (this.fireball ? 7 : 4.2)
    ctx.strokeStyle = `rgba(255,255,255,${(this.life * 0.42).toFixed(3)})`
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(this.x - flare, this.y)
    ctx.lineTo(this.x + flare, this.y)
    ctx.stroke()
    ctx.beginPath()
    ctx.moveTo(this.x, this.y - flare)
    ctx.lineTo(this.x, this.y + flare)
    ctx.stroke()

    // 头部高光
    const headGrad = ctx.createRadialGradient(this.x, this.y, 0, this.x, this.y, this.radius * 5)
    headGrad.addColorStop(0, rgba([255, 255, 255], Math.min(this.life * 0.95, 1)))
    headGrad.addColorStop(0.25, rgba(color, this.life * 0.55))
    headGrad.addColorStop(1, rgba(color, 0))
    ctx.fillStyle = headGrad
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.radius * 5, 0, Math.PI * 2)
    ctx.fill()
  }
}

/* ========== 涟漪效果类 ========== */
class Ripple {
  constructor(x, y) {
    this.x = x
    this.y = y
    this.radius = 0
    this.maxRadius = 120 + Math.random() * 60
    this.life = 1
    this.speed = 3 + Math.random() * 2
  }

  update() {
    this.radius += this.speed
    this.life = 1 - this.radius / this.maxRadius
    return this.life > 0
  }

  draw(ctx) {
    if (this.life <= 0) return
    const colors = getColorSet()
    const alpha = this.life * 0.3

    ctx.beginPath()
    ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2)
    ctx.strokeStyle = rgba(colors.primary, alpha)
    ctx.lineWidth = 2 * this.life
    ctx.stroke()

    // Inner ring
    if (this.radius > 20) {
      ctx.beginPath()
      ctx.arc(this.x, this.y, this.radius * 0.6, 0, Math.PI * 2)
      ctx.strokeStyle = rgba(colors.accent2, alpha * 0.5)
      ctx.lineWidth = 1 * this.life
      ctx.stroke()
    }
  }
}

/* ========== 空间网格 (性能优化) ========== */
class SpatialGrid {
  constructor(cellSize) {
    this.cellSize = cellSize
    this.cells = new Map()
  }

  clear() {
    this.cells.clear()
  }

  getKey(x, y) {
    const cx = Math.floor(x / this.cellSize)
    const cy = Math.floor(y / this.cellSize)
    return `${cx},${cy}`
  }

  insert(particle) {
    const key = this.getKey(particle.x, particle.y)
    if (!this.cells.has(key)) this.cells.set(key, [])
    this.cells.get(key).push(particle)
  }

  getNearby(particle) {
    const cx = Math.floor(particle.x / this.cellSize)
    const cy = Math.floor(particle.y / this.cellSize)
    const nearby = []
    for (let dx = -1; dx <= 1; dx++) {
      for (let dy = -1; dy <= 1; dy++) {
        const key = `${cx + dx},${cy + dy}`
        const cell = this.cells.get(key)
        if (cell) nearby.push(...cell)
      }
    }
    return nearby
  }
}

/* ========== 主渲染逻辑 ========== */
function initCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const dpr = window.devicePixelRatio || 1

  const mouse = { x: null, y: null }
  let particles = []
  let glowOrbs = []
  let meteors = []
  let ripples = []
  let stars = []
  let rainbows = []
  const grid = new SpatialGrid(config.linkDistance)

  // Reduced motion check
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  function resize() {
    const parent = canvas.parentElement
    const w = parent.offsetWidth
    const h = parent.offsetHeight
    canvas.width = w * dpr
    canvas.height = h * dpr
    canvas.style.width = w + 'px'
    canvas.style.height = h + 'px'
    canvas._cssW = w
    canvas._cssH = h
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    initParticles()
  }

  function initParticles() {
    const w = canvas._cssW
    const h = canvas._cssH
    const area = w * h

    const normalCount = Math.min(
      Math.floor(config.baseParticleCount * props.density),
      Math.floor(area / 8000)
    )

    particles = []
    for (let i = 0; i < normalCount; i++) {
      const p = new Particle({ width: w, height: h }, 'normal')
      p._idx = i
      particles.push(p)
    }

    glowOrbs = []
    const orbCount = Math.min(config.glowOrbCount, Math.floor(area / 40000) + 2)
    for (let i = 0; i < orbCount; i++) {
      glowOrbs.push(new Particle({ width: w, height: h }, 'glow'))
    }

    meteors = []
    ripples = []
    rainbows = []

    // 星屑层：远景闪烁小点
    stars = []
    const starCount = Math.min(config.starCount, Math.floor(area / 12000) + 24)
    for (let i = 0; i < starCount; i++) {
      stars.push(new Star(w, h))
    }
  }

  // Debounced resize
  let resizeTimer
  const debouncedResize = () => {
    clearTimeout(resizeTimer)
    resizeTimer = setTimeout(resize, 200)
  }

  // Mouse handlers
  const handleMouseMove = (e) => {
    if (!props.interactive) return
    const rect = canvas.getBoundingClientRect()
    mouse.x = e.clientX - rect.left
    mouse.y = e.clientY - rect.top
  }

  const handleMouseLeave = () => {
    mouse.x = null
    mouse.y = null
  }

  const handleClick = (e) => {
    if (!props.interactive) return
    const rect = canvas.getBoundingClientRect()
    const clickX = e.clientX - rect.left
    const clickY = e.clientY - rect.top

    // Add ripple
    ripples.push(new Ripple(clickX, clickY))

    // Push nearby particles
    particles.forEach(p => {
      const dx = p.x - clickX
      const dy = p.y - clickY
      const dist = Math.hypot(dx, dy)
      if (dist < 150 && dist > 0) {
        const force = ((150 - dist) / 150) * 0.8
        p.vx += (dx / dist) * force
        p.vy += (dy / dist) * force
      }
    })
  }

  // 环境效果：暗色模式流星雨 / 亮色模式彩虹
  let meteorTimer = null
  let ambientTimer = null
  let effectEndTimer = null
  let showerTimeouts = []

  const canvasSize = () => ({
    width: canvas._cssW || canvas.width,
    height: canvas._cssH || canvas.height
  })

  const notify = (name, detail) => {
    window.dispatchEvent(new CustomEvent(name, { detail }))
  }

  function spawnMeteor() {
    // 亮色模式下不出现流星
    if (isDark() && !prefersReducedMotion) meteors.push(new Meteor(canvasSize()))
    meteorTimer = setTimeout(spawnMeteor, config.meteorInterval + Math.random() * 6000)
  }

  function scheduleAmbient(delay) {
    clearTimeout(ambientTimer)
    ambientTimer = setTimeout(runAmbient, delay)
  }

  function runAmbient() {
    if (!prefersReducedMotion) {
      if (isDark()) startShower()
      else startRainbow()
    }
    scheduleAmbient(
      config.ambientMinGap + Math.random() * (config.ambientMaxGap - config.ambientMinGap)
    )
  }

  function startShower() {
    const count =
      config.showerMeteorMin +
      Math.floor(Math.random() * (config.showerMeteorMax - config.showerMeteorMin))

    showerTimeouts.forEach(clearTimeout)
    showerTimeouts = []
    for (let i = 0; i < count; i++) {
      showerTimeouts.push(
        setTimeout(() => {
          meteors.push(new Meteor(canvasSize()))
        }, i * (55 + Math.random() * 85))
      )
    }

    notify('light-note:meteor-shower', { active: true, duration: config.showerDuration })

    clearTimeout(effectEndTimer)
    effectEndTimer = setTimeout(() => {
      notify('light-note:meteor-shower', { active: false })
    }, config.showerDuration)
  }

  function startRainbow() {
    const { width, height } = canvasSize()
    rainbows.push(new Rainbow(width, height))

    notify('light-note:rainbow', { active: true, duration: config.rainbowDuration })

    clearTimeout(effectEndTimer)
    effectEndTimer = setTimeout(() => {
      notify('light-note:rainbow', { active: false })
    }, config.rainbowDuration)
  }

  meteorTimer = setTimeout(spawnMeteor, config.meteorInterval * 0.6)
  scheduleAmbient(config.ambientFirstDelay)

  // Event listeners
  window.addEventListener('resize', debouncedResize)
  canvas.addEventListener('mousemove', handleMouseMove)
  canvas.addEventListener('mouseleave', handleMouseLeave)
  canvas.addEventListener('click', handleClick)

  cleanupFns.push(
    () => window.removeEventListener('resize', debouncedResize),
    () => canvas.removeEventListener('mousemove', handleMouseMove),
    () => canvas.removeEventListener('mouseleave', handleMouseLeave),
    () => canvas.removeEventListener('click', handleClick),
    () => clearTimeout(meteorTimer),
    () => clearTimeout(ambientTimer),
    () => clearTimeout(effectEndTimer),
    () => showerTimeouts.forEach(clearTimeout),
    () => {
      // 离开页面时告知外部效果已结束，避免桌宠状态卡住
      notify('light-note:meteor-shower', { active: false })
      notify('light-note:rainbow', { active: false })
    },
    () => clearTimeout(resizeTimer)
  )

  resize()

  // Connection drawing with gradient
  function drawConnection(p1, p2, dist, maxDist) {
    const opacity = (1 - dist / maxDist)
    const colors = getColorSet()
    const c1 = colors[p1.colorKey]
    const c2 = colors[p2.colorKey]
    const midColor = lerpColor(c1, c2, 0.5)

    const grad = ctx.createLinearGradient(p1.x, p1.y, p2.x, p2.y)
    grad.addColorStop(0, rgba(c1, opacity * 0.12))
    grad.addColorStop(0.5, rgba(midColor, opacity * 0.18))
    grad.addColorStop(1, rgba(c2, opacity * 0.12))

    ctx.beginPath()
    ctx.moveTo(p1.x, p1.y)
    ctx.lineTo(p2.x, p2.y)
    ctx.strokeStyle = grad
    ctx.lineWidth = opacity * 1.2 + 0.3
    ctx.stroke()
  }

  // Mouse connection drawing
  function drawMouseConnections(p) {
    if (mouse.x == null) return
    const dx = mouse.x - p.x
    const dy = mouse.y - p.y
    const dist = Math.hypot(dx, dy)
    if (dist >= config.mouseLinkDistance) return

    const opacity = (1 - dist / config.mouseLinkDistance)
    const colors = getColorSet()
    const color = colors[p.colorKey]

    const grad = ctx.createLinearGradient(mouse.x, mouse.y, p.x, p.y)
    grad.addColorStop(0, rgba(colors.primary, opacity * 0.25))
    grad.addColorStop(0.5, rgba(color, opacity * 0.15))
    grad.addColorStop(1, rgba(color, opacity * 0.05))

    ctx.beginPath()
    ctx.moveTo(mouse.x, mouse.y)
    ctx.lineTo(p.x, p.y)
    ctx.strokeStyle = grad
    ctx.lineWidth = opacity * 1.5
    ctx.stroke()
  }

  // Mouse cursor glow
  function drawCursorGlow() {
    if (mouse.x == null) return
    const colors = getColorSet()
    const radius = 80
    const grad = ctx.createRadialGradient(mouse.x, mouse.y, 0, mouse.x, mouse.y, radius)
    grad.addColorStop(0, rgba(colors.primary, 0.06 * props.glowIntensity))
    grad.addColorStop(0.4, rgba(colors.accent2, 0.03 * props.glowIntensity))
    grad.addColorStop(1, rgba(colors.primary, 0))
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.arc(mouse.x, mouse.y, radius, 0, Math.PI * 2)
    ctx.fill()
  }

  /* ========== 动画循环 ========== */
  let lastTime = 0
  function animate(time) {
    if (document.visibilityState === 'hidden') {
      animationId = requestAnimationFrame(animate)
      return
    }

    const delta = Math.min(time - lastTime, 32) // cap at ~30fps worth of delta
    lastTime = time

    const w = canvas._cssW || canvas.width
    const h = canvas._cssH || canvas.height

    ctx.clearRect(0, 0, w, h)

    // 彩虹层（亮色模式的环境效果）
    rainbows = rainbows.filter(r => r.active)
    rainbows.forEach(r => {
      r.update(delta)
      r.draw(ctx)
    })

    // 星屑层（远景闪烁）
    stars.forEach(s => s.update(h))
    stars.forEach(s => s.draw(ctx))

    // Update and draw glow orbs (background layer)
    glowOrbs.forEach(orb => {
      orb.update(mouse)
      orb.draw(ctx, props.glowIntensity)
    })

    // Build spatial grid
    grid.clear()
    particles.forEach(p => grid.insert(p))

    // Update particles
    particles.forEach(p => p.update(mouse))

    // Draw connections using spatial grid (index-based for performance)
    const linkDist = config.linkDistance
    const drawn = new Set()
    for (let i = 0; i < particles.length; i++) {
      const p1 = particles[i]
      const nearby = grid.getNearby(p1)
      for (let n = 0; n < nearby.length; n++) {
        const p2 = nearby[n]
        if (p1 === p2) continue
        const j = p2._idx
        const pairKey = i < j ? i * 10000 + j : j * 10000 + i
        if (drawn.has(pairKey)) continue

        const dx = p1.x - p2.x
        const dy = p1.y - p2.y
        const dist = Math.hypot(dx, dy)
        if (dist < linkDist) {
          drawConnection(p1, p2, dist, linkDist)
          drawn.add(pairKey)
        }
      }
    }

    // Draw particles
    particles.forEach(p => {
      p.draw(ctx, props.glowIntensity)
      drawMouseConnections(p)
    })

    // Draw cursor glow
    drawCursorGlow()

    // Update and draw meteors
    meteors = meteors.filter(m => m.active)
    meteors.forEach(m => {
      m.update()
      m.draw(ctx)
    })

    // Update and draw ripples
    ripples = ripples.filter(r => r.update())
    ripples.forEach(r => r.draw(ctx))

    animationId = requestAnimationFrame(animate)
  }

  animationId = requestAnimationFrame(animate)
}

/* ========== 主题变化监听 ========== */
let themeObserver = null

onMounted(() => {
  initCanvas()

  // Watch for theme changes
  themeObserver = new MutationObserver(() => {
    // Particles will pick up new colors on next frame automatically
  })
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme']
  })

  onUnmounted(() => {
    if (animationId) cancelAnimationFrame(animationId)
    if (themeObserver) themeObserver.disconnect()
    cleanupFns.forEach(fn => fn())
    cleanupFns = []
  })
})
</script>

<style scoped>
.particle-network {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
</style>
