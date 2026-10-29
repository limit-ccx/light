<template>
  <canvas ref="canvasRef" class="star-field" :style="canvasStyle"></canvas>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'

const props = defineProps({
  starCount: { type: Number, default: 260 },
  shootingStarInterval: { type: Number, default: 1400 },
  speed: { type: Number, default: 1 }
})

const canvasRef = ref(null)
let ctx = null
let animationId = null
let shootingTimer = null
let width = 0
let height = 0
let dpr = 1

const stars = []
const shootingStars = []

const canvasStyle = computed(() => ({
  width: '100%',
  height: '100%'
}))

class Star {
  constructor() {
    this.reset()
    this.blinkOffset = Math.random() * Math.PI * 2
  }

  reset() {
    this.x = Math.random() * width
    this.y = Math.random() * height
    this.baseRadius = Math.random() * 1.4 + 0.3
    this.radius = this.baseRadius
    this.alpha = Math.random() * 0.6 + 0.2
    this.blinkSpeed = Math.random() * 0.03 + 0.01
    this.color = this.pickColor()
    this.depth = Math.random() * 0.8 + 0.2
    this.vx = (Math.random() - 0.5) * 0.035 * this.depth
    this.vy = (Math.random() - 0.5) * 0.035 * this.depth
  }

  pickColor() {
    const palette = [
      '255, 255, 255',
      '200, 220, 255',
      '255, 240, 200',
      '215, 200, 255'
    ]
    return palette[Math.floor(Math.random() * palette.length)]
  }

  update(time) {
    const blink = Math.sin(time * this.blinkSpeed + this.blinkOffset)
    this.currentAlpha = this.alpha * (0.55 + 0.45 * blink)

    this.x += this.vx
    this.y += this.vy
    if (this.x < -4) this.x = width + 4
    else if (this.x > width + 4) this.x = -4
    if (this.y < -4) this.y = height + 4
    else if (this.y > height + 4) this.y = -4
  }

  draw() {
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(${this.color}, ${this.currentAlpha})`
    ctx.fill()

    // 最亮的星加柔光，避免每帧为大量星星创建渐变
    if (this.baseRadius > 1.45) {
      const glow = ctx.createRadialGradient(
        this.x, this.y, 0,
        this.x, this.y, this.radius * 4
      )
      glow.addColorStop(0, `rgba(${this.color}, ${this.currentAlpha * 0.35})`)
      glow.addColorStop(1, 'rgba(255,255,255,0)')
      ctx.beginPath()
      ctx.arc(this.x, this.y, this.radius * 4, 0, Math.PI * 2)
      ctx.fillStyle = glow
      ctx.fill()
    }

    // 亮星十字星芒
    if (this.baseRadius > 1.2) {
      const ray = this.radius * 6
      ctx.save()
      ctx.strokeStyle = `rgba(${this.color}, ${this.currentAlpha * 0.14})`
      ctx.lineWidth = 0.6
      ctx.beginPath()
      ctx.moveTo(this.x - ray, this.y)
      ctx.lineTo(this.x + ray, this.y)
      ctx.moveTo(this.x, this.y - ray)
      ctx.lineTo(this.x, this.y + ray)
      ctx.stroke()
      ctx.restore()
    }
  }
}

class ShootingStar {
  constructor() {
    this.reset()
  }

  reset() {
    const startSide = Math.random() > 0.5 ? 'top' : 'left'
    if (startSide === 'top') {
      this.x = Math.random() * width * 0.8
      this.y = -20
    } else {
      this.x = -20
      this.y = Math.random() * height * 0.5
    }
    this.length = Math.random() * 80 + 60
    this.speed = (Math.random() * 6 + 6) * props.speed
    this.angle = Math.PI / 4 + (Math.random() * 0.2 - 0.1)
    this.vx = Math.cos(this.angle) * this.speed
    this.vy = Math.sin(this.angle) * this.speed
    this.alpha = 1
    this.decay = Math.random() * 0.015 + 0.008
    this.dead = false
    this.hue = Math.random() > 0.7 ? 220 : 40 // 蓝白或金白
  }

  update() {
    this.x += this.vx
    this.y += this.vy
    this.alpha -= this.decay
    if (this.alpha <= 0 || this.x > width + 100 || this.y > height + 100) {
      this.dead = true
    }
  }

  draw() {
    if (this.dead) return
    const tailX = this.x - this.vx * (this.length / this.speed)
    const tailY = this.y - this.vy * (this.length / this.speed)

    const grad = ctx.createLinearGradient(this.x, this.y, tailX, tailY)
    grad.addColorStop(0, `rgba(255, 255, 255, ${this.alpha})`)
    grad.addColorStop(0.4, `rgba(${this.hue === 220 ? '180, 220, 255' : '255, 240, 180'}, ${this.alpha * 0.7})`)
    grad.addColorStop(1, 'rgba(255, 255, 255, 0)')

    ctx.beginPath()
    ctx.moveTo(this.x, this.y)
    ctx.lineTo(tailX, tailY)
    ctx.strokeStyle = grad
    ctx.lineWidth = 2
    ctx.lineCap = 'round'
    ctx.stroke()

    // 流星头部光点
    ctx.beginPath()
    ctx.arc(this.x, this.y, 2, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(255, 255, 255, ${this.alpha})`
    ctx.shadowBlur = 10
    ctx.shadowColor = `rgba(${this.hue === 220 ? '180, 220, 255' : '255, 240, 180'}, 0.8)`
    ctx.fill()
    ctx.shadowBlur = 0
  }
}

const resize = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  dpr = window.devicePixelRatio || 1
  width = rect.width
  height = rect.height
  canvas.width = Math.floor(width * dpr)
  canvas.height = Math.floor(height * dpr)
  ctx = canvas.getContext('2d')
  ctx.scale(dpr, dpr)

  stars.length = 0
  for (let i = 0; i < props.starCount; i++) {
    stars.push(new Star())
  }
}

let lastTime = 0
const animate = (time) => {
  if (!ctx) return
  ctx.clearRect(0, 0, width, height)

  // 绘制星星
  stars.forEach(star => {
    star.update(time * 0.001)
    star.draw()
  })

  // 绘制流星
  for (let i = shootingStars.length - 1; i >= 0; i--) {
    const s = shootingStars[i]
    s.update()
    s.draw()
    if (s.dead) {
      shootingStars.splice(i, 1)
    }
  }

  animationId = requestAnimationFrame(animate)
}

const spawnShootingStar = () => {
  if (shootingStars.length < 3) {
    shootingStars.push(new ShootingStar())
  }
}

onMounted(() => {
  resize()
  window.addEventListener('resize', resize)
  animationId = requestAnimationFrame(animate)
  shootingTimer = setInterval(spawnShootingStar, props.shootingStarInterval)
})

onUnmounted(() => {
  window.removeEventListener('resize', resize)
  if (animationId) cancelAnimationFrame(animationId)
  if (shootingTimer) clearInterval(shootingTimer)
})
</script>

<style scoped>
.star-field {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
</style>
