import * as THREE from 'three'
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js'
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js'
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js'
import { OutputPass } from 'three/addons/postprocessing/OutputPass.js'

const clamp01 = (x) => Math.min(1, Math.max(0, x))
const seg = (t, a, b) => clamp01((t - a) / (b - a))
const easeOutCubic = (x) => 1 - Math.pow(1 - x, 3)
const easeInOutCubic = (x) =>
  x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2
const easeInOutSine = (x) => -(Math.cos(Math.PI * x) - 1) / 2
const lerp = THREE.MathUtils.lerp
const rnd = (a, b) => a + Math.random() * (b - a)
const TAU = Math.PI * 2

const FONT = '"Microsoft YaHei", "PingFang SC", "Segoe UI", sans-serif'

function makeCanvas(w, h) {
  const c = document.createElement('canvas')
  c.width = w
  c.height = h
  return [c, c.getContext('2d')]
}

function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

function bgTexture() {
  const [c, ctx] = makeCanvas(64, 512)
  const g = ctx.createLinearGradient(0, 0, 0, 512)
  g.addColorStop(0.0, '#0b1428')
  g.addColorStop(0.42, '#0a1124')
  g.addColorStop(0.78, '#070c1a')
  g.addColorStop(1.0, '#04060e')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, 64, 512)
  const rg = ctx.createRadialGradient(32, 40, 4, 32, 40, 260)
  rg.addColorStop(0, 'rgba(150,180,255,0.30)')
  rg.addColorStop(1, 'rgba(150,180,255,0)')
  ctx.fillStyle = rg
  ctx.fillRect(0, 0, 64, 512)
  const t = new THREE.CanvasTexture(c)
  t.colorSpace = THREE.SRGBColorSpace
  return t
}

function glowDiscTexture() {
  const [c, ctx] = makeCanvas(512, 512)
  const g = ctx.createRadialGradient(256, 256, 10, 256, 256, 250)
  g.addColorStop(0.0, 'rgba(190,210,255,0.55)')
  g.addColorStop(0.28, 'rgba(130,160,255,0.26)')
  g.addColorStop(0.62, 'rgba(80,110,220,0.10)')
  g.addColorStop(1.0, 'rgba(0,0,0,0)')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, 512, 512)
  const t = new THREE.CanvasTexture(c)
  t.colorSpace = THREE.SRGBColorSpace
  return t
}

function dotTexture() {
  const [c, ctx] = makeCanvas(128, 128)
  const g = ctx.createRadialGradient(64, 64, 0, 64, 64, 64)
  g.addColorStop(0.0, 'rgba(255,255,255,1)')
  g.addColorStop(0.25, 'rgba(220,230,255,0.9)')
  g.addColorStop(0.55, 'rgba(150,170,255,0.35)')
  g.addColorStop(1.0, 'rgba(0,0,0,0)')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, 128, 128)
  return new THREE.CanvasTexture(c)
}

function sparkTexture() {
  const [c, ctx] = makeCanvas(128, 128)
  const g = ctx.createRadialGradient(64, 64, 0, 64, 64, 58)
  g.addColorStop(0.0, 'rgba(255,255,255,1)')
  g.addColorStop(0.18, 'rgba(235,242,255,0.95)')
  g.addColorStop(0.45, 'rgba(160,185,255,0.35)')
  g.addColorStop(1.0, 'rgba(0,0,0,0)')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, 128, 128)
  ctx.globalCompositeOperation = 'lighter'
  const ray = (x0, y0, x1, y1) => {
    const lg = ctx.createLinearGradient(x0, y0, x1, y1)
    lg.addColorStop(0, 'rgba(255,255,255,0)')
    lg.addColorStop(0.5, 'rgba(255,255,255,0.85)')
    lg.addColorStop(1, 'rgba(255,255,255,0)')
    ctx.strokeStyle = lg
    ctx.lineWidth = 1.4
    ctx.beginPath()
    ctx.moveTo(x0, y0)
    ctx.lineTo(x1, y1)
    ctx.stroke()
  }
  ray(6, 64, 122, 64)
  ray(64, 6, 64, 122)
  return new THREE.CanvasTexture(c)
}

function pageTexture(side) {
  const W = 1024
  const H = 1400
  const [c, ctx] = makeCanvas(W, H)
  const base = ctx.createLinearGradient(0, 0, W, H)
  base.addColorStop(0, '#e4d6b8')
  base.addColorStop(0.5, '#dbcbab')
  base.addColorStop(1, '#cdbb98')
  ctx.fillStyle = base
  ctx.fillRect(0, 0, W, H)
  for (let i = 0; i < 6000; i++) {
    const a = Math.random() * 0.05
    ctx.fillStyle = Math.random() > 0.5 ? `rgba(120,95,50,${a})` : `rgba(255,250,235,${a})`
    ctx.fillRect(Math.random() * W, Math.random() * H, 1.4, 1.4)
  }
  const spineX = side === 'right' ? 0 : W
  const sg = ctx.createLinearGradient(spineX, 0, spineX + (side === 'right' ? 1 : -1) * 170, 0)
  sg.addColorStop(0, 'rgba(110,90,50,0.32)')
  sg.addColorStop(1, 'rgba(110,90,50,0)')
  ctx.fillStyle = sg
  ctx.fillRect(0, 0, W, H)
  const eg = ctx.createRadialGradient(W / 2, H / 2, H * 0.32, W / 2, H / 2, H * 0.78)
  eg.addColorStop(0, 'rgba(0,0,0,0)')
  eg.addColorStop(1, 'rgba(90,70,35,0.22)')
  ctx.fillStyle = eg
  ctx.fillRect(0, 0, W, H)

  const ink = 'rgba(60,52,38,0.78)'
  const inkFaint = 'rgba(60,52,38,0.34)'
  ctx.strokeStyle = ink
  ctx.fillStyle = ink

  ctx.fillStyle = 'rgba(48,42,30,0.85)'
  ctx.font = 'italic 700 30px Georgia, serif'
  ctx.textAlign = 'center'
  ctx.fillText(side === 'right' ? 'THE PATHS OF LIGHT' : 'ANATOMY OF CELESTIAL MOTION', W / 2, 92)
  ctx.strokeStyle = inkFaint
  ctx.lineWidth = 1.5
  ctx.beginPath()
  ctx.moveTo(180, 112)
  ctx.lineTo(W - 180, 112)
  ctx.stroke()

  const drawAstrolabe = (cx, cy, r) => {
    ctx.save()
    ctx.translate(cx, cy)
    ctx.strokeStyle = ink
    ctx.fillStyle = ink
    ctx.lineWidth = 2
    for (let k = 1; k <= 3; k++) {
      ctx.beginPath()
      ctx.arc(0, 0, (r * k) / 3, 0, TAU)
      ctx.stroke()
    }
    for (let k = 0; k < 10; k++) {
      const a = (k / 10) * TAU
      ctx.beginPath()
      ctx.moveTo(0, 0)
      ctx.lineTo(Math.cos(a) * r, Math.sin(a) * r)
      ctx.stroke()
    }
    const pts = []
    for (let k = 0; k < 7; k++) {
      const a = k * 2.399
      const rr = r * (0.35 + (0.55 * ((k * 37) % 100)) / 100)
      pts.push([Math.cos(a) * rr, Math.sin(a) * rr])
    }
    ctx.strokeStyle = 'rgba(40,36,26,0.85)'
    ctx.lineWidth = 2.2
    ctx.beginPath()
    pts.forEach((p, i) => (i === 0 ? ctx.moveTo(p[0], p[1]) : ctx.lineTo(p[0], p[1])))
    ctx.closePath()
    ctx.stroke()
    pts.forEach((p) => {
      ctx.beginPath()
      ctx.arc(p[0], p[1], 7, 0, TAU)
      ctx.fill()
    })
    for (let k = 0; k < 26; k++) {
      const a = Math.random() * TAU
      const rr = r * (0.5 + Math.random() * 0.55)
      ctx.beginPath()
      ctx.arc(Math.cos(a) * rr, Math.sin(a) * rr, 2.2, 0, TAU)
      ctx.fill()
    }
    ctx.restore()
  }

  const drawDiagram = (cx, cy, r) => {
    ctx.save()
    ctx.translate(cx, cy)
    ctx.strokeStyle = ink
    ctx.fillStyle = ink
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(0, -r * 0.9)
    ctx.lineTo(r * 0.9, r * 0.6)
    ctx.lineTo(-r * 0.9, r * 0.6)
    ctx.closePath()
    ctx.stroke()
    ctx.beginPath()
    ctx.moveTo(0, -r * 0.9)
    ctx.lineTo(0, r * 0.6)
    ctx.stroke()
    ctx.beginPath()
    ctx.arc(0, r * 0.6, r * 0.32, 0, TAU)
    ctx.stroke()
    ctx.beginPath()
    for (let i = 0; i <= 60; i++) {
      const x = -r + (i * 2 * r) / 60
      const y = r * 0.7 - x * x * 0.0035
      i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)
    }
    ctx.stroke()
    for (let k = 0; k < 10; k++) {
      ctx.beginPath()
      ctx.arc(rnd(-r, r), rnd(-r, r * 0.6), 3.2, 0, TAU)
      ctx.fill()
    }
    ctx.restore()
  }

  const colL = 110
  const colR = W / 2 + 30
  const colW = W / 2 - 150
  const drawLines = (x, y, w, rows, lh, gapEvery = 0) => {
    ctx.fillStyle = inkFaint
    for (let i = 0; i < rows; i++) {
      if (gapEvery && i % gapEvery === 0) y += 16
      ctx.fillRect(x, y, w * (0.55 + Math.random() * 0.45), 4)
      y += lh
    }
  }
  if (side === 'right') {
    drawAstrolabe(W / 2, 330, 190)
    drawLines(colL, 600, colW, 9, 26)
    drawLines(colR, 600, colW, 9, 26)
    drawDiagram(W / 2 - 180, 980, 130)
    drawAstrolabe(W / 2 + 200, 980, 120)
    drawLines(colL, 1180, colW, 5, 26)
    drawLines(colR, 1180, colW, 5, 26)
  } else {
    drawDiagram(W / 2, 330, 180)
    drawLines(colL, 600, colW, 9, 26)
    drawLines(colR, 600, colW, 9, 26)
    drawAstrolabe(W / 2 - 180, 980, 125)
    drawDiagram(W / 2 + 200, 980, 130)
    drawLines(colL, 1180, colW, 5, 26)
    drawLines(colR, 1180, colW, 5, 26)
  }
  ctx.fillStyle = 'rgba(48,42,30,0.7)'
  ctx.font = 'italic 24px Georgia, serif'
  ctx.textAlign = side === 'right' ? 'right' : 'left'
  ctx.fillText(side === 'right' ? '37' : '36', side === 'right' ? W - 90 : 90, H - 60)

  const t = new THREE.CanvasTexture(c)
  t.colorSpace = THREE.SRGBColorSpace
  t.anisotropy = 8
  return t
}

function pageEdgeTexture() {
  const [c, ctx] = makeCanvas(512, 96)
  ctx.fillStyle = '#e6ddc6'
  ctx.fillRect(0, 0, 512, 96)
  for (let y = 3; y < 96; y += 3.4) {
    ctx.fillStyle = `rgba(120,105,70,${0.12 + Math.random() * 0.18})`
    ctx.fillRect(0, y, 512, 1)
  }
  const t = new THREE.CanvasTexture(c)
  t.colorSpace = THREE.SRGBColorSpace
  t.wrapS = t.wrapT = THREE.RepeatWrapping
  return t
}

function coverTexture() {
  const W = 1024
  const H = 760
  const [c, ctx] = makeCanvas(W, H)
  const g = ctx.createLinearGradient(0, 0, W, H)
  g.addColorStop(0, '#26366a')
  g.addColorStop(0.5, '#1b2854')
  g.addColorStop(1, '#121c3e')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, W, H)
  for (let i = 0; i < 9000; i++) {
    const a = Math.random() * 0.06
    ctx.fillStyle = Math.random() > 0.5 ? `rgba(140,160,220,${a})` : `rgba(5,10,30,${a})`
    ctx.fillRect(Math.random() * W, Math.random() * H, 1.5, 1.5)
  }
  const rg = ctx.createRadialGradient(W / 2, H / 2, H * 0.3, W / 2, H / 2, H * 0.75)
  rg.addColorStop(0, 'rgba(0,0,0,0)')
  rg.addColorStop(1, 'rgba(0,0,0,0.4)')
  ctx.fillStyle = rg
  ctx.fillRect(0, 0, W, H)
  const gold = '#c8a45a'
  ctx.strokeStyle = gold
  ctx.lineWidth = 3
  ctx.strokeRect(46, 46, W - 92, H - 92)
  ctx.strokeStyle = 'rgba(220,190,120,0.75)'
  ctx.lineWidth = 1.4
  ctx.strokeRect(64, 64, W - 128, H - 128)
  const corner = (fx, fy) => {
    ctx.save()
    ctx.translate(fx ? W - 46 : 46, fy ? H - 46 : 46)
    ctx.scale(fx ? -1 : 1, fy ? -1 : 1)
    ctx.strokeStyle = gold
    ctx.lineWidth = 3
    ctx.beginPath()
    ctx.arc(0, 0, 34, 0, Math.PI / 2)
    ctx.stroke()
    ctx.beginPath()
    ctx.arc(0, 0, 20, 0, Math.PI / 2)
    ctx.stroke()
    ctx.restore()
  }
  corner(0, 0)
  corner(1, 0)
  corner(0, 1)
  corner(1, 1)
  ctx.save()
  ctx.translate(W / 2, H / 2)
  ctx.strokeStyle = 'rgba(210,180,110,0.9)'
  ctx.lineWidth = 3
  ctx.beginPath()
  ctx.ellipse(0, 0, 92, 118, 0, 0, TAU)
  ctx.stroke()
  ctx.lineWidth = 1.6
  ctx.beginPath()
  ctx.ellipse(0, 0, 74, 100, 0, 0, TAU)
  ctx.stroke()
  ctx.fillStyle = 'rgba(220,190,120,0.95)'
  ctx.beginPath()
  for (let k = 0; k < 10; k++) {
    const a = -Math.PI / 2 + (k * Math.PI) / 5
    const r = k % 2 ? 18 : 38
    const x = Math.cos(a) * r
    const y = Math.sin(a) * r
    k === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)
  }
  ctx.closePath()
  ctx.fill()
  ctx.font = 'italic 26px Georgia, "Songti SC", "STSong", SimSun, serif'
  ctx.textAlign = 'center'
  ctx.fillStyle = 'rgba(210,180,110,0.85)'
  ctx.fillText('纸域空间', 0, 86)
  ctx.restore()
  const t = new THREE.CanvasTexture(c)
  t.colorSpace = THREE.SRGBColorSpace
  t.anisotropy = 8
  return t
}

function spineEmblemTexture() {
  const [c, ctx] = makeCanvas(256, 256)
  ctx.strokeStyle = 'rgba(210,180,110,0.95)'
  ctx.lineWidth = 6
  ctx.beginPath()
  ctx.ellipse(128, 128, 76, 96, 0, 0, TAU)
  ctx.stroke()
  ctx.fillStyle = 'rgba(220,190,120,0.95)'
  ctx.beginPath()
  for (let k = 0; k < 10; k++) {
    const a = -Math.PI / 2 + (k * Math.PI) / 5
    const r = k % 2 ? 12 : 30
    const x = 128 + Math.cos(a) * r
    const y = 128 + Math.sin(a) * r
    k === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)
  }
  ctx.closePath()
  ctx.fill()
  return new THREE.CanvasTexture(c)
}

function uiSearchTexture() {
  const [c, ctx] = makeCanvas(360, 110)
  ctx.shadowColor = '#9fc8ff'
  ctx.shadowBlur = 22
  ctx.strokeStyle = 'rgba(210,230,255,0.95)'
  ctx.lineWidth = 5
  roundRect(ctx, 8, 8, 344, 94, 47)
  ctx.stroke()
  ctx.shadowBlur = 0
  ctx.lineWidth = 5
  ctx.beginPath()
  ctx.arc(52, 55, 17, 0, TAU)
  ctx.stroke()
  ctx.beginPath()
  ctx.moveTo(65, 68)
  ctx.lineTo(80, 82)
  ctx.stroke()
  ctx.fillStyle = 'rgba(200,220,255,0.8)'
  ctx.font = `26px ${FONT}`
  ctx.textAlign = 'left'
  ctx.fillText('搜索...', 100, 64)
  return new THREE.CanvasTexture(c)
}

function uiPillTexture(word) {
  const [c, ctx] = makeCanvas(300, 110)
  ctx.shadowColor = '#a890ff'
  ctx.shadowBlur = 20
  const g = ctx.createLinearGradient(0, 0, 300, 110)
  g.addColorStop(0, 'rgba(120,140,255,0.22)')
  g.addColorStop(1, 'rgba(150,120,255,0.18)')
  ctx.fillStyle = g
  roundRect(ctx, 8, 8, 284, 94, 47)
  ctx.fill()
  ctx.strokeStyle = 'rgba(200,215,255,0.9)'
  ctx.lineWidth = 4
  roundRect(ctx, 8, 8, 284, 94, 47)
  ctx.stroke()
  ctx.shadowBlur = 0
  ctx.fillStyle = '#dfe8ff'
  ctx.font = `600 30px ${FONT}`
  ctx.textAlign = 'center'
  ctx.fillText(word, 150, 67)
  return new THREE.CanvasTexture(c)
}

function uiTagTexture(word) {
  const [c, ctx] = makeCanvas(260, 110)
  ctx.shadowColor = '#8fb8ff'
  ctx.shadowBlur = 18
  ctx.strokeStyle = 'rgba(170,205,255,0.9)'
  ctx.lineWidth = 4
  roundRect(ctx, 10, 14, 240, 82, 16)
  ctx.stroke()
  ctx.shadowBlur = 0
  ctx.fillStyle = '#d7e5ff'
  ctx.font = `600 30px ${FONT}`
  ctx.textAlign = 'center'
  ctx.fillText(word, 130, 66)
  return new THREE.CanvasTexture(c)
}

function uiVertTexture() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  const [c, ctx] = makeCanvas(72, 460)
  ctx.shadowColor = '#9d8bff'
  ctx.shadowBlur = 14
  ctx.fillStyle = 'rgba(196,205,255,0.92)'
  ctx.font = '600 34px Consolas, monospace'
  ctx.textAlign = 'center'
  let s = ''
  for (let i = 0; i < 11; i++) s += chars[Math.floor(Math.random() * chars.length)]
  for (let i = 0; i < s.length; i++) ctx.fillText(s[i], 36, 42 + i * 38)
  return new THREE.CanvasTexture(c)
}

function uiIconTexture(kind) {
  const [c, ctx] = makeCanvas(150, 150)
  ctx.shadowColor = '#9fc0ff'
  ctx.shadowBlur = 18
  ctx.strokeStyle = 'rgba(205,225,255,0.95)'
  ctx.fillStyle = 'rgba(205,225,255,0.95)'
  ctx.lineWidth = 6
  ctx.beginPath()
  ctx.arc(75, 75, 56, 0, TAU)
  ctx.stroke()
  ctx.lineWidth = 5
  if (kind === 0) {
    ctx.beginPath()
    ctx.arc(75, 75, 16, 0, TAU)
    ctx.stroke()
    for (let k = 0; k < 8; k++) {
      const a = (k / 8) * TAU
      ctx.beginPath()
      ctx.moveTo(75 + Math.cos(a) * 26, 75 + Math.sin(a) * 26)
      ctx.lineTo(75 + Math.cos(a) * 36, 75 + Math.sin(a) * 36)
      ctx.stroke()
    }
  } else if (kind === 1) {
    ;[34, 58, 82, 106].forEach((x, i) => {
      const h = 20 + (i % 3) * 14
      ctx.fillRect(x - 7, 108 - h, 14, h)
    })
  } else if (kind === 2) {
    ctx.strokeRect(40, 58, 70, 48)
    ctx.beginPath()
    ctx.moveTo(40, 58)
    ctx.lineTo(75, 84)
    ctx.lineTo(110, 58)
    ctx.stroke()
  } else {
    ctx.beginPath()
    ctx.arc(75, 62, 16, 0, TAU)
    ctx.stroke()
    ctx.beginPath()
    ctx.arc(75, 118, 34, Math.PI * 1.05, Math.PI * 1.95)
    ctx.stroke()
  }
  return new THREE.CanvasTexture(c)
}

/*
 * 首页入场：书本展开 → 书沟蓄能 → 粒子爆发上升 → 浮游知识卡片 → 交叉淡出首页
 * 返回 { skip, dispose }，遮罩淡出与卸载由组件负责。
 */
export function createMagicBookScene({ host, onReveal, onFinished, startAt = 0 }) {
  const isMobile = window.matchMedia('(max-width: 768px)').matches
  const resources = []
  const keep = (t) => {
    resources.push(t)
    return t
  }

  const renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: 'high-performance' })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, isMobile ? 1.5 : 2))
  renderer.setSize(host.clientWidth, host.clientHeight)
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 0.96
  renderer.outputColorSpace = THREE.SRGBColorSpace
  host.appendChild(renderer.domElement)

  const scene = new THREE.Scene()
  scene.fog = new THREE.FogExp2(0x070b18, 0.028)
  scene.background = keep(bgTexture())
  const stage = new THREE.Group()
  stage.scale.setScalar(isMobile ? 0.62 : 1)
  scene.add(stage)

  const camera = new THREE.PerspectiveCamera(isMobile ? 52 : 42, host.clientWidth / host.clientHeight, 0.1, 100)
  camera.position.set(0.2, 2.1, 8.4)

  const composer = new EffectComposer(renderer)
  composer.addPass(new RenderPass(scene, camera))
  const bloom = new UnrealBloomPass(
    new THREE.Vector2(host.clientWidth, host.clientHeight),
    0.32,
    0.4,
    0.9
  )
  composer.addPass(bloom)
  composer.addPass(new OutputPass())

  const dotTex = keep(dotTexture())
  const sparkTex = keep(sparkTexture())
  const glowTex = keep(glowDiscTexture())

  /* ---------- 灯光 ---------- */
  scene.add(new THREE.AmbientLight(0x33457a, 0.4))
  const keyLight = new THREE.DirectionalLight(0xbcd2ff, 0.42)
  keyLight.position.set(-3.5, 8, -3)
  scene.add(keyLight)
  const fillLight = new THREE.DirectionalLight(0x4a5aa0, 0.34)
  fillLight.position.set(4, 3, 5)
  scene.add(fillLight)
  const spot = new THREE.SpotLight(0xf3e8d2, 4.5, 22, 0.9, 0.7, 1.6)
  spot.position.set(-1.4, 6.3, -0.9)
  spot.target.position.set(0.1, 0.15, 0)
  scene.add(spot, spot.target)
  const magicLight = new THREE.PointLight(0x889bff, 0, 7, 1.8)
  magicLight.position.set(0, 0.7, 0)
  scene.add(magicLight)

  /* ---------- 光束 ---------- */
  const beamMat = new THREE.ShaderMaterial({
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
    side: THREE.DoubleSide,
    uniforms: { uTime: { value: 0 }, uOpacity: { value: 0.7 } },
    vertexShader: `
      varying vec2 vUv;
      void main() { vUv = uv; gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }
    `,
    fragmentShader: `
      varying vec2 vUv;
      uniform float uTime; uniform float uOpacity;
      void main() {
        float top = smoothstep(0.10, 0.95, vUv.y);
        float rays = 0.75 + 0.25 * sin(vUv.x * 31.4159 + uTime * 0.25) * sin(vUv.x * 18.849 - uTime * 0.18);
        float edge = smoothstep(0.0, 0.18, vUv.y);
        float a = top * rays * 0.17 * edge * uOpacity;
        gl_FragColor = vec4(0.62, 0.74, 1.0, a);
      }
    `
  })
  const makeBeam = (radius, height, opacity) => {
    const m = beamMat.clone()
    m.uniforms.uOpacity = { value: opacity }
    const cone = new THREE.Mesh(new THREE.ConeGeometry(radius, height, 48, 1, true), m)
    const g = new THREE.Group()
    g.position.set(-0.9, 7.2, -1.6)
    g.lookAt(0.2, 0.2, 0)
    cone.rotation.x = -Math.PI / 2
    cone.position.z = -height / 2
    g.add(cone)
    scene.add(g)
    return g
  }
  const beam1 = makeBeam(2.0, 7.4, 0.9)
  const beam2 = makeBeam(1.1, 7.4, 0.6)
  const sourceGlow = new THREE.Sprite(
    new THREE.SpriteMaterial({
      map: glowTex,
      color: 0xbfd2ff,
      transparent: true,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
      opacity: 0.65
    })
  )
  sourceGlow.scale.set(3.2, 3.2, 1)
  sourceGlow.position.copy(beam1.position)
  scene.add(sourceGlow)

  /* ---------- 星尘 ---------- */
  const starGeo = new THREE.BufferGeometry()
  {
    const N = isMobile ? 120 : 240
    const pos = new Float32Array(N * 3)
    const scl = new Float32Array(N)
    for (let i = 0; i < N; i++) {
      pos[i * 3] = rnd(-16, 16)
      pos[i * 3 + 1] = rnd(0.4, 11)
      pos[i * 3 + 2] = rnd(-12, 6)
      scl[i] = rnd(0.4, 1.4)
    }
    starGeo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
    starGeo.setAttribute('aScale', new THREE.BufferAttribute(scl, 1))
  }
  const starMat = new THREE.ShaderMaterial({
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
    uniforms: { uTime: { value: 0 }, uMap: { value: dotTex } },
    vertexShader: `
      attribute float aScale;
      uniform float uTime;
      varying float vTw;
      void main() {
        vec3 p = position;
        p.y += sin(uTime * 0.15 + position.x * 2.0 + position.z) * 0.25;
        p.x += sin(uTime * 0.1 + position.z) * 0.12;
        vec4 mv = modelViewMatrix * vec4(p, 1.0);
        gl_Position = projectionMatrix * mv;
        gl_PointSize = aScale * 1.0 * (60.0 / -mv.z);
        vTw = 0.4 + 0.6 * abs(sin(uTime * 0.8 + position.x * 3.0 + position.z * 2.0));
      }
    `,
    fragmentShader: `
      uniform sampler2D uMap; varying float vTw;
      void main() {
        vec4 t = texture2D(uMap, gl_PointCoord);
        gl_FragColor = vec4(0.72, 0.82, 1.0, t.a * vTw * 0.55);
      }
    `
  })
  stage.add(new THREE.Points(starGeo, starMat))

  /* ---------- 书本 ---------- */
  const PW = 1.55
  const PH = 2.15
  const PT = 0.34
  const CW = 3.3
  const CH = 2.32
  const CT = 0.06
  const TOP = CT + PT
  const GOLD = 0xcaa55e

  const book = new THREE.Group()
  stage.add(book)

  const leatherPlain = new THREE.MeshStandardMaterial({ color: 0x223466, roughness: 0.72, metalness: 0.18, emissive: 0x0a1734, emissiveIntensity: 0.8 })
  const coverTopMat = new THREE.MeshStandardMaterial({
    color: 0xffffff,
    roughness: 0.68,
    metalness: 0.15,
    map: keep(coverTexture())
  })
  const pageEdgeTex = keep(pageEdgeTexture())
  const pageSideMat = new THREE.MeshStandardMaterial({ color: 0xd8c8a8, roughness: 0.9, map: pageEdgeTex })
  const pagePlainMat = new THREE.MeshStandardMaterial({ color: 0xc4b28f, roughness: 0.95 })
  const goldMat = new THREE.MeshStandardMaterial({ color: GOLD, roughness: 0.28, metalness: 0.92 })

  const backCover = new THREE.Mesh(new THREE.BoxGeometry(CW, CT, CH), leatherPlain)
  backCover.position.set(CW / 2, CT / 2, 0)
  book.add(backCover)

  const spine = new THREE.Mesh(new THREE.CylinderGeometry(0.135, 0.15, CH + 0.08, 32, 1, false), leatherPlain)
  spine.rotation.x = Math.PI / 2
  spine.position.set(-0.02, 0.135, 0)
  book.add(spine)
  ;[-0.78, 0.78].forEach((z) => {
    const band = new THREE.Mesh(new THREE.CylinderGeometry(0.155, 0.16, 0.07, 32), goldMat)
    band.rotation.x = Math.PI / 2
    band.position.set(-0.02, 0.15, z)
    book.add(band)
  })
  const emblem = new THREE.Mesh(
    new THREE.PlaneGeometry(0.17, 0.17),
    new THREE.MeshStandardMaterial({
      map: keep(spineEmblemTexture()),
      transparent: true,
      roughness: 0.4,
      metalness: 0.4,
      emissive: 0x2a2008,
      emissiveIntensity: 0.4
    })
  )
  emblem.position.set(-0.165, 0.17, 0)
  emblem.rotation.y = -Math.PI / 2
  book.add(emblem)

  const closedBlock = new THREE.Mesh(
    new THREE.BoxGeometry(CW - 0.16, PT, PH - 0.08),
    [pageSideMat, pageSideMat, pagePlainMat, pagePlainMat, pageSideMat, pageSideMat]
  )
  closedBlock.position.set(CW / 2, CT + PT / 2, 0)
  book.add(closedBlock)

  const pageTexR = keep(pageTexture('right'))
  const pageTexL = keep(pageTexture('left'))
  pageTexL.wrapS = THREE.RepeatWrapping
  pageTexL.repeat.x = -1
  pageTexL.offset.x = 1
  const blockMats = [pageSideMat, pageSideMat, pagePlainMat, pagePlainMat, pageSideMat, pageSideMat]

  const makePageBlock = (side) => {
    const geo = new THREE.BoxGeometry(PW - 0.02, PT, PH - 0.08)
    const m = new THREE.Mesh(geo, blockMats)
    m.position.set(side === 'right' ? PW / 2 : -PW / 2, CT + PT / 2, 0)
    return m
  }
  const openR = makePageBlock('right')
  const openL = makePageBlock('left')
  openR.visible = openL.visible = false
  book.add(openR, openL)

  const curvedPageTop = (side) => {
    const geo = new THREE.PlaneGeometry(PW, PH, 32, 24)
    geo.rotateX(-Math.PI / 2)
    geo.translate(side === 'right' ? PW / 2 : -PW / 2, 0, 0)
    const pos = geo.attributes.position
    for (let i = 0; i < pos.count; i++) {
      const d = Math.abs(pos.getX(i)) / PW
      pos.setY(i, -(1 - d) * (1 - d) * 0.05)
    }
    geo.computeVertexNormals()
    const mat = new THREE.MeshStandardMaterial({
      color: 0xcbb997,
      roughness: 0.95,
      map: side === 'right' ? pageTexR : pageTexL,
      side: THREE.DoubleSide
    })
    const mesh = new THREE.Mesh(geo, mat)
    mesh.position.y = TOP
    mesh.visible = false
    return mesh
  }
  const topR = curvedPageTop('right')
  const topL = curvedPageTop('left')
  book.add(topR, topL)

  const coverPivot = new THREE.Group()
  coverPivot.position.set(0, TOP + CT / 2, 0)
  book.add(coverPivot)
  const coverMesh = new THREE.Mesh(
    new THREE.BoxGeometry(CW, CT, CH),
    [leatherPlain, leatherPlain, coverTopMat, leatherPlain, leatherPlain, leatherPlain]
  )
  coverMesh.position.set(CW / 2, 0, 0)
  coverPivot.add(coverMesh)
  const cornerGeo = new THREE.BoxGeometry(0.16, 0.008, 0.16)
  ;[
    [CW - 0.14, CH / 2 - 0.14],
    [CW - 0.14, -CH / 2 + 0.14],
    [0.14, CH / 2 - 0.14],
    [0.14, -CH / 2 + 0.14]
  ].forEach(([x, z]) => {
    const cn = new THREE.Mesh(cornerGeo, goldMat)
    cn.position.set(x, CT / 2 + 0.004, z)
    coverMesh.add(cn)
  })

  const flips = []
  for (let i = 0; i < 3; i++) {
    const geo = new THREE.PlaneGeometry(PW, PH - 0.06, 26, 3)
    geo.translate(PW / 2, 0, 0)
    const g = new THREE.Group()
    g.position.set(0, TOP + 0.012 + i * 0.004, 0)
    const meshA = new THREE.Mesh(
      geo,
      new THREE.MeshStandardMaterial({ color: 0xd6c6a6, roughness: 0.95, map: pageTexR, side: THREE.FrontSide })
    )
    const meshB = new THREE.Mesh(
      geo.clone(),
      new THREE.MeshStandardMaterial({ color: 0xd6c6a6, roughness: 0.95, map: pageTexL, side: THREE.BackSide })
    )
    meshA.rotation.x = -Math.PI / 2
    meshB.rotation.x = -Math.PI / 2
    g.add(meshA, meshB)
    g.visible = false
    g.userData = { geo: meshA.geometry, base: meshA.geometry.attributes.position.array.slice() }
    book.add(g)
    flips.push(g)
  }
  const bendPage = (group, theta) => {
    const geo = group.userData.geo
    const base = group.userData.base
    const pos = geo.attributes.position
    for (let i = 0; i < pos.count; i++) {
      const bx = base[i * 3]
      const by = base[i * 3 + 1]
      const u = bx / PW
      const a = theta * (0.72 + 0.28 * u)
      const lift = Math.sin(theta) * Math.sin(u * Math.PI) * 0.05
      pos.setXYZ(i, Math.cos(a) * bx, by, Math.sin(a) * bx + lift)
    }
    pos.needsUpdate = true
    geo.computeVertexNormals()
    const pos2 = group.children[1].geometry.attributes.position
    pos2.copy(pos)
    pos2.needsUpdate = true
    group.children[1].geometry.computeVertexNormals()
  }

  const grooveGlow = new THREE.Mesh(
    new THREE.CylinderGeometry(0.05, 0.05, PH - 0.1, 16, 1, true),
    new THREE.MeshBasicMaterial({
      color: 0x9db4ff,
      transparent: true,
      opacity: 0,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    })
  )
  grooveGlow.rotation.x = Math.PI / 2
  grooveGlow.position.set(0, TOP + 0.02, 0)
  book.add(grooveGlow)

  /* ---------- 魔法粒子 ---------- */
  const PARTICLE_N = isMobile ? 1100 : 2600
  const pGeo = new THREE.BufferGeometry()
  {
    const start = new Float32Array(PARTICLE_N * 3)
    const vel = new Float32Array(PARTICLE_N * 3)
    const seed = new Float32Array(PARTICLE_N)
    const size = new Float32Array(PARTICLE_N)
    const col = new Float32Array(PARTICLE_N * 3)
    const palette = [
      new THREE.Color(0xb9cbff),
      new THREE.Color(0x93a8ff),
      new THREE.Color(0xc7b4ff),
      new THREE.Color(0xffffff),
      new THREE.Color(0x7fa0ff),
      new THREE.Color(0xa88bff)
    ]
    for (let i = 0; i < PARTICLE_N; i++) {
      if (Math.random() < 0.32) {
        start[i * 3] = rnd(-0.06, 0.06)
        start[i * 3 + 1] = TOP + 0.03
        start[i * 3 + 2] = rnd(-PH / 2 + 0.1, PH / 2 - 0.1)
      } else {
        const side = Math.random() < 0.5 ? -1 : 1
        start[i * 3] = side * rnd(0.1, PW - 0.12)
        start[i * 3 + 1] = TOP - 0.02
        start[i * 3 + 2] = rnd(-PH / 2 + 0.12, PH / 2 - 0.12)
      }
      const fromGroove = Math.abs(start[i * 3]) < 0.1
      vel[i * 3] = start[i * 3] * rnd(0.5, 1.1) + rnd(-0.2, 0.2)
      vel[i * 3 + 1] = rnd(0.9, 2.2) * (fromGroove ? 1.35 : 1)
      vel[i * 3 + 2] = rnd(-0.35, 0.35)
      seed[i] = Math.random() * 100
      size[i] = rnd(0.5, 1.45)
      const cc = palette[Math.floor(Math.random() * palette.length)]
      col[i * 3] = cc.r
      col[i * 3 + 1] = cc.g
      col[i * 3 + 2] = cc.b
    }
    pGeo.setAttribute('position', new THREE.BufferAttribute(start.slice(), 3))
    pGeo.setAttribute('aStart', new THREE.BufferAttribute(start, 3))
    pGeo.setAttribute('aVel', new THREE.BufferAttribute(vel, 3))
    pGeo.setAttribute('aSeed', new THREE.BufferAttribute(seed, 1))
    pGeo.setAttribute('aSize', new THREE.BufferAttribute(size, 1))
    pGeo.setAttribute('aColor', new THREE.BufferAttribute(col, 3))
  }
  const pMat = new THREE.ShaderMaterial({
    transparent: true,
    depthWrite: false,
    blending: THREE.NormalBlending,
    uniforms: {
      uTime: { value: 0 },
      uBurst: { value: 0 },
      uRise: { value: 0 },
      uFade: { value: 0 },
      uCharge: { value: 0 },
      uMap: { value: sparkTex },
      uPR: { value: Math.min(window.devicePixelRatio, isMobile ? 1.15 : 2) }
    },
    vertexShader: `
      attribute vec3 aStart, aVel, aColor;
      attribute float aSeed, aSize;
      uniform float uTime, uBurst, uRise, uFade, uCharge, uPR;
      varying vec3 vColor; varying float vAlpha;
      float eo3(float x){ return 1.0 - pow(1.0 - x, 3.0); }
      float ei3(float x){ return x < 0.5 ? 4.0*x*x*x : 1.0 - pow(-2.0*x + 2.0, 3.0)/2.0; }
      void main() {
        float w1 = sin(uTime * 1.4 + aSeed * 0.6);
        float w2 = cos(uTime * 1.1 + aSeed * 0.4);
        vec3 pBurst = aStart + aVel * 1.25 + vec3(0.0, 0.28, 0.0);
        vec3 pRise = aStart + aVel * vec3(2.2, 3.1, 1.1)
          + vec3(w1 * 0.55, 0.9 + w2 * 0.35, w2 * 0.45);
        vec3 p = aStart;
        p = mix(p, pBurst, eo3(uBurst));
        p = mix(p, pRise, ei3(uRise));
        vec4 mv = modelViewMatrix * vec4(p, 1.0);
        gl_Position = projectionMatrix * mv;
        gl_PointSize = aSize * uPR * (40.0 / -mv.z);
        vColor = aColor;
        float tw = 0.55 + 0.45 * sin(uTime * 3.0 + aSeed * 5.0);
        float appear = smoothstep(0.0, 0.18, uBurst);
        vAlpha = (0.35 + 0.65 * tw) * max(0.18 * uCharge * tw, appear) * (1.0 - uFade);
      }
    `,
    fragmentShader: `
      uniform sampler2D uMap;
      varying vec3 vColor; varying float vAlpha;
      void main() {
        vec4 t = texture2D(uMap, gl_PointCoord);
        gl_FragColor = vec4(vColor * 1.2, t.a * vAlpha);
      }
    `
  })
  const burstPoints = new THREE.Points(pGeo, pMat)
  burstPoints.frustumCulled = false
  stage.add(burstPoints)

  /* ---------- 垂直光流（参考例图的点状光柱） ---------- */
  const STREAM_COLS = isMobile ? 14 : 30
  const STREAM_EACH = isMobile ? 11 : 20
  const sGeo = new THREE.BufferGeometry()
  {
    const total = STREAM_COLS * STREAM_EACH
    const colX = new Float32Array(total)
    const colZ = new Float32Array(total)
    const phase = new Float32Array(total)
    const speed = new Float32Array(total)
    const seed = new Float32Array(total)
    const size = new Float32Array(total)
    const col = new Float32Array(total * 3)
    const palette = [
      new THREE.Color(0xffffff),
      new THREE.Color(0xcfe0ff),
      new THREE.Color(0x9fb8ff),
      new THREE.Color(0xc9b8ff)
    ]
    let k = 0
    for (let c = 0; c < STREAM_COLS; c++) {
      const central = c < 3
      const angle = rnd(0, TAU)
      const radius = rnd(0.2, 2.7)
      const x = central ? rnd(-0.16, 0.16) : Math.cos(angle) * radius
      const z = central ? rnd(-0.2, 0.35) : Math.sin(angle) * rnd(0.15, 0.95)
      const spd = rnd(0.035, 0.085)
      for (let j = 0; j < STREAM_EACH; j++) {
        colX[k] = x
        colZ[k] = z
        phase[k] = j / STREAM_EACH + rnd(-0.015, 0.015)
        speed[k] = spd
        seed[k] = rnd(0, 100)
        size[k] = central ? rnd(1.5, 2.4) : rnd(0.75, 1.55)
        const cc = palette[Math.floor(Math.random() * palette.length)]
        col[k * 3] = cc.r
        col[k * 3 + 1] = cc.g
        col[k * 3 + 2] = cc.b
        k++
      }
    }
    sGeo.setAttribute('position', new THREE.BufferAttribute(new Float32Array(total * 3), 3))
    sGeo.setAttribute('aColX', new THREE.BufferAttribute(colX, 1))
    sGeo.setAttribute('aColZ', new THREE.BufferAttribute(colZ, 1))
    sGeo.setAttribute('aPhase', new THREE.BufferAttribute(phase, 1))
    sGeo.setAttribute('aSpeed', new THREE.BufferAttribute(speed, 1))
    sGeo.setAttribute('aSeed', new THREE.BufferAttribute(seed, 1))
    sGeo.setAttribute('aSize', new THREE.BufferAttribute(size, 1))
    sGeo.setAttribute('aColor', new THREE.BufferAttribute(col, 3))
  }
  const sMat = new THREE.ShaderMaterial({
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
    uniforms: {
      uTime: { value: 0 },
      uStream: { value: 0 },
      uFade: { value: 0 },
      uMap: { value: sparkTex },
      uPR: { value: Math.min(window.devicePixelRatio, isMobile ? 1.2 : 2) }
    },
    vertexShader: `
      attribute float aColX, aColZ, aPhase, aSpeed, aSeed, aSize;
      attribute vec3 aColor;
      uniform float uTime, uStream, uFade, uPR;
      varying vec3 vColor; varying float vAlpha;
      void main() {
        float k = fract(aPhase + uTime * aSpeed);
        float y = 0.5 + k * 4.7;
        vec3 p = vec3(aColX + sin(uTime * 0.5 + aSeed) * 0.035, y, aColZ);
        vec4 mv = modelViewMatrix * vec4(p, 1.0);
        gl_Position = projectionMatrix * mv;
        gl_PointSize = aSize * uPR * (44.0 / -mv.z);
        vColor = aColor;
        float tw = 0.5 + 0.5 * sin(uTime * 2.6 + aSeed * 3.1);
        float edge = smoothstep(0.0, 0.07, k) * (1.0 - smoothstep(0.78, 1.0, k));
        vAlpha = uStream * (1.0 - uFade) * (0.35 + 0.65 * tw) * edge;
      }
    `,
    fragmentShader: `
      uniform sampler2D uMap;
      varying vec3 vColor; varying float vAlpha;
      void main() {
        vec4 t = texture2D(uMap, gl_PointCoord);
        gl_FragColor = vec4(vColor * 1.15, t.a * vAlpha);
      }
    `
  })
  const streamPoints = new THREE.Points(sGeo, sMat)
  streamPoints.frustumCulled = false
  stage.add(streamPoints)

  /* ---------- 飘浮知识卡片 ---------- */
  const uiSprites = []
  const uiTex = [
    keep(uiSearchTexture()),
    ...['记录', '思考', '连接', '灵感', '沉淀', '知识', 'AI 摘要'].map((w) => keep(uiPillTexture(w))),
    ...['纸域空间', '写作', '阅读', '笔记', '灵感', '分享'].map((w) => keep(uiTagTexture(w))),
    ...[0, 1, 2, 3, 0, 2, 1, 3].map((k) => keep(uiIconTexture(k))),
    ...[0, 1, 2, 3, 4, 5].map(() => keep(uiVertTexture()))
  ]
  const uiScales = [
    [1.15, 0.36],
    [0.85, 0.32],
    [0.78, 0.3],
    [0.78, 0.3],
    [0.7, 0.28],
    [0.8, 0.3],
    [0.9, 0.33],
    [0.8, 0.3],
    [0.7, 0.27],
    [0.62, 0.26],
    [0.62, 0.26],
    [0.64, 0.27],
    [0.6, 0.26],
    [0.6, 0.25],
    [0.32, 0.32],
    [0.32, 0.32],
    [0.32, 0.32],
    [0.32, 0.32],
    [0.32, 0.32],
    [0.32, 0.32],
    [0.32, 0.32],
    [0.32, 0.32],
    [0.24, 1.35],
    [0.24, 1.35],
    [0.24, 1.35],
    [0.24, 1.35],
    [0.24, 1.35],
    [0.24, 1.35]
  ]
  // 手排布：绕开书脊正上方，形成左右两翼
  const uiLayout = [
    [-3.6, 2.1], [-2.6, 3.0], [-1.2, 3.9], [0.3, 4.5], [1.7, 4.5], [2.7, 3.8], [3.5, 2.9], [4.0, 2.0],
    [-4.0, 2.9], [-3.0, 4.0], [-1.8, 4.9], [1.2, 5.0], [2.6, 4.6], [3.8, 3.6],
    [-4.2, 1.9], [-2.2, 2.5], [-0.8, 2.7], [0.8, 2.6], [2.3, 2.3], [3.2, 1.6], [-1.0, 3.4], [1.6, 3.2],
    [-4.4, 3.6], [4.4, 3.3], [-3.4, 4.8], [3.6, 4.8], [-0.9, 5.3], [0.9, 5.3]
  ]
  const mobileKeep = new Set([0, 1, 2, 3, 4, 5, 10, 11, 14, 16, 22, 24])
  for (let i = 0; i < uiTex.length; i++) {
    if (isMobile && !mobileKeep.has(i)) continue
    const mat = new THREE.SpriteMaterial({
      map: uiTex[i],
      transparent: true,
      opacity: 0,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    })
    const s = new THREE.Sprite(mat)
    const start = new THREE.Vector3(rnd(-1.4, 1.4), TOP + rnd(0.05, 0.35), rnd(-0.7, 0.7))
    const [ex, ey] = uiLayout[i] || [rnd(-3, 3), rnd(2, 4)]
    const fitX = isMobile ? 0.62 : 1
    const end = new THREE.Vector3(ex * fitX, ey, rnd(-0.6, 1.2))
    const sc = uiScales[i] || [0.6, 0.28]
    s.userData = {
      start,
      end,
      baseScale: sc[0],
      aspect: sc[1] / sc[0],
      delay: Math.random() * 0.35,
      seed: Math.random() * 50,
      spin: rnd(-0.5, 0.5)
    }
    s.scale.set(sc[0], sc[1], 1)
    s.position.copy(start)
    s.visible = false
    stage.add(s)
    uiSprites.push(s)
  }

  /* ---------- 相机 ---------- */
  const camKeys = [
    { t: 0.0, p: [0.8, 3.25, 7.2], l: [1.05, 0.3, 0] },
    { t: 1.4, p: [0.75, 2.95, 6.35], l: [0.95, 0.35, 0] },
    { t: 2.6, p: [0.0, 3.05, 6.6], l: [0.0, 0.45, 0] },
    { t: 4.4, p: [0.0, 2.95, 7.0], l: [0.0, 1.0, 0] },
    { t: 20, p: [0.0, 2.95, 7.0], l: [0.0, 1.0, 0] }
  ]
  const camAt = (time) => {
    let a = camKeys[0]
    let b = camKeys[camKeys.length - 1]
    for (let i = 0; i < camKeys.length - 1; i++) {
      if (time >= camKeys[i].t && time <= camKeys[i + 1].t) {
        a = camKeys[i]
        b = camKeys[i + 1]
        break
      }
    }
    const k = easeInOutSine(seg(time, a.t, b.t))
    return {
      p: [lerp(a.p[0], b.p[0], k), lerp(a.p[1], b.p[1], k), lerp(a.p[2], b.p[2], k)],
      l: [lerp(a.l[0], b.l[0], k), lerp(a.l[1], b.l[1], k), lerp(a.l[2], b.l[2], k)]
    }
  }

  const mouse = { x: 0, y: 0 }
  const onPointerMove = (e) => {
    mouse.x = (e.clientX / window.innerWidth - 0.5) * 2
    mouse.y = (e.clientY / window.innerHeight - 0.5) * 2
  }
  window.addEventListener('pointermove', onPointerMove, { passive: true })

  /* ---------- 时间轴 ---------- */
  const T = {
    cover: [0.5, 1.35],
    page1: [1.3, 1.75],
    page2: [1.7, 2.1],
    page3: [2.05, 2.45],
    charge: [2.3, 2.9],
    burst: [2.75, 3.55],
    rise: [3.3, 4.6],
    fade: [4.45, 5.05]
  }
  const REVEAL_T = 4.4
  const END_T = 4.9

  const updateTimeline = (t) => {
    const coverK = easeInOutCubic(seg(t, T.cover[0], T.cover[1]))
    coverPivot.rotation.z = coverK * Math.PI
    coverPivot.position.y = lerp(TOP + CT / 2, CT / 2 + 0.005, coverK)

    const opened = t > T.page1[0]
    closedBlock.visible = !opened
    openR.visible = openL.visible = opened
    topR.visible = topL.visible = opened
    grooveGlow.visible = opened

    const pageRanges = [T.page1, T.page2, T.page3]
    flips.forEach((g, i) => {
      const r = pageRanges[i]
      const k = seg(t, r[0], r[1])
      if (k > 0 && k < 1) {
        g.visible = true
        let theta = easeInOutCubic(k) * Math.PI
        if (k > 0.85) theta += Math.sin(((k - 0.85) / 0.15) * Math.PI) * 0.06
        bendPage(g, theta)
      } else if (k >= 1) {
        g.visible = true
        bendPage(g, Math.PI)
      } else {
        g.visible = false
      }
    })

    const chargeK = easeOutCubic(seg(t, T.charge[0], T.charge[1]))
    const burstK = seg(t, T.burst[0], T.burst[1])
    grooveGlow.material.opacity = chargeK * (0.16 + 0.1 * Math.sin(t * 12)) + burstK * 0.12
    grooveGlow.scale.setScalar(1 + chargeK * 0.4 + burstK * 0.5)
    magicLight.intensity =
      chargeK * 2 + burstK * 8 * (1 - easeOutCubic(seg(t, T.burst[1], 4.6))) + 1.5

    const fadeK = easeInOutCubic(seg(t, T.fade[0], T.fade[1]))
    pMat.uniforms.uCharge.value = chargeK
    pMat.uniforms.uBurst.value = easeOutCubic(seg(t, T.burst[0], T.burst[1]))
    pMat.uniforms.uRise.value = easeInOutCubic(seg(t, T.rise[0], T.rise[1]))
    pMat.uniforms.uFade.value = fadeK
    sMat.uniforms.uStream.value = easeOutCubic(seg(t, 3.05, 3.7))
    sMat.uniforms.uFade.value = fadeK

    const riseK = seg(t, T.rise[0], T.rise[1])
    uiSprites.forEach((s) => {
      const d = s.userData
      const k = easeOutCubic(clamp01((riseK - d.delay) / (1 - d.delay)))
      if (k > 0.01) {
        s.visible = true
        const wob = Math.sin(t * 1.2 + d.seed) * 0.12
        const wob2 = Math.cos(t * 0.9 + d.seed * 1.4) * 0.1
        s.position.set(
          lerp(d.start.x, d.end.x, k) + wob,
          lerp(d.start.y, d.end.y, k) + wob2,
          lerp(d.start.z, d.end.z, k)
        )
        const appear = easeOutCubic(seg(t, T.rise[0] + d.delay, T.rise[0] + d.delay + 0.5))
        const sc = d.baseScale * (0.6 + 0.4 * appear) * (1 - fadeK * 0.5)
        s.scale.set(sc, sc * d.aspect, 1)
        s.material.opacity = appear * (1 - fadeK)
        s.material.rotation = Math.sin(t * 0.5 + d.seed) * 0.08 * d.spin * 2
      } else {
        s.visible = false
      }
    })

    beam1.children[0].material.uniforms.uOpacity.value =
      (0.72 + Math.sin(t * 0.7) * 0.1) * (1 - fadeK * 0.6)
    beam2.children[0].material.uniforms.uOpacity.value =
      (0.45 + Math.sin(t * 0.9 + 1) * 0.08) * (1 - fadeK * 0.6)
    sourceGlow.material.opacity = 0.65 * (1 - fadeK * 0.6)
  }

  /* ---------- 主循环 ---------- */
  const clock = new THREE.Clock()
  let elapsed = startAt
  let rafId = 0
  let revealSent = false
  let finished = false
  let disposed = false

  const emitReveal = () => {
    if (revealSent) return
    revealSent = true
    onReveal?.()
  }
  const emitFinished = () => {
    if (finished) return
    finished = true
    onFinished?.()
  }

  const animate = () => {
    if (disposed) return
    rafId = requestAnimationFrame(animate)
    const dt = Math.min(clock.getDelta(), 0.05)
    elapsed += dt
    const t = elapsed

    updateTimeline(t)

    starMat.uniforms.uTime.value = t
    beam1.children[0].material.uniforms.uTime.value = t
    beam2.children[0].material.uniforms.uTime.value = t
    pMat.uniforms.uTime.value = t
    sMat.uniforms.uTime.value = t

    const c = camAt(Math.min(t, END_T))
    const aspect = window.innerWidth / window.innerHeight
    const zFit = aspect < 1 ? 1 + (1 - aspect) * 0.35 : 1
    camera.position.set(c.p[0] + mouse.x * 0.22, c.p[1] - mouse.y * 0.12, c.p[2] * zFit)
    camera.lookAt(c.l[0] + mouse.x * 0.1, c.l[1], c.l[2])

    composer.render()

    if (t >= REVEAL_T) emitReveal()
    if (t >= END_T) emitFinished()
  }

  animate()

  const onResize = () => {
    const w = host.clientWidth
    const h = host.clientHeight
    if (!w || !h) return
    camera.aspect = w / h
    camera.updateProjectionMatrix()
    renderer.setSize(w, h)
    composer.setSize(w, h)
  }
  window.addEventListener('resize', onResize)

  const onContextLost = (e) => {
    e.preventDefault()
    emitFinished()
  }
  renderer.domElement.addEventListener('webglcontextlost', onContextLost)

  const skip = () => {
    emitReveal()
    emitFinished()
  }

  const dispose = () => {
    if (disposed) return
    disposed = true
    cancelAnimationFrame(rafId)
    window.removeEventListener('pointermove', onPointerMove)
    window.removeEventListener('resize', onResize)
    renderer.domElement.removeEventListener('webglcontextlost', onContextLost)
    scene.traverse((obj) => {
      obj.geometry?.dispose()
      const mats = Array.isArray(obj.material) ? obj.material : obj.material ? [obj.material] : []
      mats.forEach((m) => {
        Object.keys(m).forEach((k) => {
          const v = m[k]
          if (v && v.isTexture) v.dispose()
        })
        m.dispose()
      })
    })
    resources.forEach((r) => r?.dispose?.())
    starGeo.dispose()
    pGeo.dispose()
    sGeo.dispose()
    starMat.dispose()
    pMat.dispose()
    sMat.dispose()
    beamMat.dispose()
    composer.passes?.forEach((p) => p.dispose?.())
    composer.dispose?.()
    renderer.dispose()
    renderer.forceContextLoss?.()
    if (renderer.domElement.parentNode) renderer.domElement.parentNode.removeChild(renderer.domElement)
  }

  return { skip, dispose }
}
