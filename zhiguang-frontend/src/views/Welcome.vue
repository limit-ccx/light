<template>
  <div ref="rootRef" class="nebula">
    <canvas ref="canvasRef" class="nb-stage" aria-hidden="true"></canvas>
    <div class="nb-veil" aria-hidden="true"></div>
    <div ref="grainRef" class="nb-grain" aria-hidden="true"></div>

    <div v-if="loading" class="nb-loader" role="status" aria-live="polite">
      <div class="lg">
        <div class="ring">
          <svg viewBox="0 0 96 96">
            <circle class="bg" cx="48" cy="48" r="44"></circle>
            <circle ref="arcRef" class="fg" cx="48" cy="48" r="44" stroke-dasharray="276.5" stroke-dashoffset="276.5"></circle>
          </svg>
          <div ref="pctRef" class="pct">0%</div>
        </div>
        <div class="wm">纸域空间</div>
        <div ref="subRef" class="sub">点亮第一颗星 · Seeding light</div>
        <div class="bar"><i ref="barRef"></i></div>
      </div>
    </div>

    <div class="nb-frame" aria-hidden="true">
      <span class="frame-mark tl"><i>✦</i>纸域空间<em>PAPER SPACE</em></span>
      <span class="frame-mark tr">WRITE TO THINK</span>
      <span class="frame-mark bl">书写<i>·</i>思考<i>·</i>沉淀</span>
    </div>

    <main class="nb-hero">
      <div class="inst">
        <p class="rule"><i></i><s></s></p>
        <div class="title-wrap">
          <span class="title-orbit" aria-hidden="true"></span>
          <span class="title-spark" aria-hidden="true"></span>
          <h1><span class="split">纸域空间</span></h1>
        </div>
        <p class="tagline">在纸页之间<i>·</i>遇见更大的世界</p>
        <div class="lede">
          <p class="lede-lead">这里是你的<em>知识星图</em>，</p>
          <p class="lede-sub">汇聚优质内容、思维碰撞与深度沉淀，<br>让每一次阅读，都成为向内生长的力量。</p>
        </div>
        <div class="acts">
          <span class="mag">
            <span class="btn-flare" aria-hidden="true"></span>
            <button class="btn enter" @click="enterApp">
              <span class="btn-shine" aria-hidden="true"></span>
              进入空间<i class="arrow">→</i>
            </button>
          </span>
        </div>
        <div class="hint">
          <span class="hint-cue">拖拽画面，光点会为你让路</span>
          <span class="hint-keys"><kbd>Enter</kbd>进入站点<i>/</i><kbd>R</kbd>减弱动效</span>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import gsap from 'gsap'
import { createNebulaScene } from '@/three/nebulaScene'

const router = useRouter()
const rootRef = ref(null)
const canvasRef = ref(null)
const arcRef = ref(null)
const barRef = ref(null)
const pctRef = ref(null)
const subRef = ref(null)
const grainRef = ref(null)

const loading = ref(true)
const motionOn = ref(!matchMedia('(prefers-reduced-motion: reduce)').matches)

let scene = null
let cycle = null
let bootTween = null
const cleanups = []

const mqNarrow = () => matchMedia('(max-width: 1080px)').matches
const $ = (s) => (rootRef.value ? rootRef.value.querySelector(s) : null)
const $$ = (s) => (rootRef.value ? [...rootRef.value.querySelectorAll(s)] : [])
const tokenize = (t) => t.match(/[A-Za-z0-9'\-–—&%.,;:!?()×/]+|\s+|[^\s]/g) || []

function enterApp() {
  router.push('/home')
}

/* ---------- 文字进场 ---------- */
function splitTokens(el) {
  if (el.dataset.split === 'done') return [...el.querySelectorAll('.tk>i')]
  const text = el.textContent.replace(/\s+/g, ' ').trim()
  el.textContent = ''
  el.dataset.split = 'done'
  const frag = document.createDocumentFragment()
  for (const t of tokenize(text)) {
    if (/^\s+$/.test(t)) { frag.append(document.createTextNode(' ')); continue }
    const w = document.createElement('span')
    w.className = 'tk'
    const i = document.createElement('i')
    i.textContent = t
    w.append(i)
    frag.append(w)
  }
  el.append(frag)
  return [...el.querySelectorAll('.tk>i')]
}

function maskReveal(el, { dur = 0.44, stag = 0.028, y = 112, ease = 'power3.out', delay = 0 } = {}) {
  const parts = splitTokens(el)
  if (!parts.length) return
  if (!motionOn.value) { gsap.set(parts, { yPercent: 0, opacity: 1 }); return }
  gsap.set(parts, { yPercent: y, opacity: 0 })
  gsap.to(parts, { yPercent: 0, opacity: 1, duration: dur, stagger: stag, ease, delay, overwrite: true })
}

function settle(el) {
  // 中文不能用拉丁文的负字距，落位到轻微加宽
  if (!motionOn.value) { gsap.set(el, { letterSpacing: '.06em', opacity: 1 }); return }
  gsap.fromTo(el, { letterSpacing: '.42em', opacity: 0.2 },
    { letterSpacing: '.06em', opacity: 1, duration: 0.7, ease: 'power3.out', overwrite: true })
}

function revealHero() {
  $$('.split').forEach((el, i) => {
    if (el.tagName === 'H1' || el.closest('h1')) settle(el)
    maskReveal(el, { dur: 0.56, stag: 0.036, delay: i * 0.1 })
  })
}

/* 按钮光斑跟随指针 */
function trackSpotlight(el) {
  const move = (e) => {
    const r = el.getBoundingClientRect()
    el.style.setProperty('--mx', (((e.clientX - r.left) / r.width) * 100).toFixed(1) + '%')
    el.style.setProperty('--my', (((e.clientY - r.top) / r.height) * 100).toFixed(1) + '%')
  }
  el.addEventListener('pointermove', move)
  cleanups.push(() => el.removeEventListener('pointermove', move))
}

/* ---------- 形态自动循环 ---------- */
function startCycle() {
  if (!scene || cycle) return
  const o = { v: scene.state.morphNow }
  cycle = gsap.to(o, {
    v: 4, duration: 9, ease: 'sine.inOut', repeat: -1, yoyo: true,
    onUpdate: () => scene?.setMorph(o.v, true)
  })
}

function stopCycle() {
  cycle?.kill()
  cycle = null
}

function applyMotion() {
  document.documentElement.classList.toggle('nb-reduce', !motionOn.value)
  scene?.setMotion(motionOn.value)
  if (grainRef.value) grainRef.value.style.animationPlayState = motionOn.value ? 'running' : 'paused'
  if (motionOn.value) startCycle()
  else {
    stopCycle()
    scene?.setMorph(0, false)
    revealHero()
  }
}

watch(motionOn, applyMotion)

/* ---------- 指针与键盘 ---------- */
function bindInput() {
  const move = (e) => scene?.setPointer(e.clientX, e.clientY)
  const down = () => scene?.setPressed(true)
  const up = () => scene?.setPressed(false)
  const leave = () => scene?.clearForce()
  const key = (e) => {
    if (e.target.matches('input,textarea,select') || e.metaKey || e.ctrlKey || e.altKey) return
    if (e.key === 'Enter') { enterApp(); e.preventDefault() }
    else if (e.key.toLowerCase() === 'r') motionOn.value = !motionOn.value
  }
  const onResize = () => scene?.resize()

  addEventListener('pointermove', move, { passive: true })
  addEventListener('pointerdown', down)
  addEventListener('pointerup', up)
  addEventListener('pointerleave', leave)
  addEventListener('keydown', key)
  addEventListener('resize', onResize)
  cleanups.push(() => {
    removeEventListener('pointermove', move)
    removeEventListener('pointerdown', down)
    removeEventListener('pointerup', up)
    removeEventListener('pointerleave', leave)
    removeEventListener('keydown', key)
    removeEventListener('resize', onResize)
  })
}

function magnetize(el) {
  const pad = (parseFloat(el.dataset.mag || 15) * 8) + 46
  const xTo = gsap.quickTo(el, 'x', { duration: 0.45, ease: 'power3.out' })
  const yTo = gsap.quickTo(el, 'y', { duration: 0.45, ease: 'power3.out' })
  const move = (e) => {
    if (!motionOn.value) return
    const r = el.getBoundingClientRect()
    const dx = e.clientX - (r.left + r.width / 2)
    const dy = e.clientY - (r.top + r.height / 2)
    if (Math.hypot(dx, dy) < pad) { xTo(dx * 0.34); yTo(dy * 0.5) } else { xTo(0); yTo(0) }
  }
  const leave = () => { xTo(0); yTo(0) }
  el.addEventListener('pointermove', move)
  el.addEventListener('pointerleave', leave)
  cleanups.push(() => {
    el.removeEventListener('pointermove', move)
    el.removeEventListener('pointerleave', leave)
  })
}

/* ---------- 生命周期 ---------- */
function finishBoot() {
  if (!scene) return
  loading.value = false
  document.documentElement.classList.remove('nb-locked')
  rootRef.value?.style.setProperty('--grain', '0.14')

  scene.start()
  applyMotion()

  bootTween = gsap.timeline({ onComplete: () => { $('.nb-loader')?.remove() } })
  bootTween.to(barRef.value, { scaleX: 1, duration: 0.3 }, 0)
    .to('.nb-loader .lg', { opacity: 0, scale: 0.96, duration: 0.45, ease: 'power2.in' }, '>-0.05')
    .to('.nb-loader', { yPercent: -100, duration: 0.9, ease: 'expo.inOut' }, '<0.12')
    .add(() => revealHero(), '-=0.6')
}

onMounted(async () => {
  document.body.classList.add('nb-page')
  document.documentElement.classList.add('nb-locked')
  await nextTick()

  try {
    scene = createNebulaScene({ canvas: canvasRef.value, isNarrow: mqNarrow, initial: { motion: motionOn.value } })
  } catch (err) {
    console.error('[Welcome] 星云场景初始化失败，降级为静态封面', err)
    loading.value = false
    document.documentElement.classList.remove('nb-locked')
    $$('.split, .split-p').forEach((el) => { el.style.opacity = 1 })
    return
  }

  bindInput()
  $$('.mag').forEach(magnetize)
  $$('.btn').forEach(trackSpotlight)
  scene.setTurb(0.3)
  scene.setSize(1.6)
  scene.setBloom(0.9)
  scene.setSpin(0.35)
  scene.setMotion(motionOn.value)
  rootRef.value?.style.setProperty('--grain', '0.14')

  gsap.set(barRef.value, { scaleX: 0.7 })
  pctRef.value.textContent = '100%'
  arcRef.value?.setAttribute('stroke-dashoffset', '0')
  subRef.value.textContent = '星图就绪 READY'
  requestAnimationFrame(() => { scene.compile(); finishBoot() })
})

onBeforeUnmount(() => {
  cleanups.forEach((fn) => { try { fn() } catch (_) {} })
  cleanups.length = 0
  stopCycle()
  bootTween?.kill()
  gsap.killTweensOf('*')
  scene?.dispose()
  scene = null
  document.documentElement.classList.remove('nb-locked', 'nb-reduce')
  document.body.classList.remove('nb-page')
})
</script>

<style lang="scss">
/* 命名空间统一在 .nebula 下，避免污染站点其余部分 */
.nebula {
  --sans: "Segoe UI Variable Text", "Segoe UI", "Microsoft YaHei UI", "PingFang SC", system-ui, sans-serif;
  /* 中文主标题走明朝体：Noto Serif SC 由 index.html 预载，离线时退到系统衬线 */
  --serif: "Noto Serif SC", "Songti SC", "SimSun", "STSong", Georgia, serif;
  --mono: "Cascadia Mono", Consolas, "SF Mono", ui-monospace, monospace;
  --void: #040b1c;
  --ice: #eaf2ff;
  --ice-2: rgba(206, 224, 255, .66);
  --ice-3: rgba(170, 196, 255, .4);
  --azure: #79d5ff;
  --lav: #b7a6ff;
  --line: rgba(150, 185, 255, .14);
  --line-2: rgba(150, 185, 255, .3);
  --ease-out: cubic-bezier(.16, 1, .3, 1);
  --t2: 220ms;
  --t3: 360ms;
  --t4: 640ms;
  --grain: .14;

  position: relative;
  width: 100%;
  min-height: 100vh;
  background: var(--void);
  color: var(--ice);
  font-family: var(--sans);
  font-size: 16px;
  line-height: 1.72;
  -webkit-font-smoothing: antialiased;

  *, *::before, *::after { box-sizing: border-box }
  p { margin: 0 }
  button { font: inherit; color: inherit; background: none; border: 0; cursor: pointer }
  :focus-visible { outline: 1px solid var(--azure); outline-offset: 3px }
  ::selection { background: rgba(121, 213, 255, .3) }
  h1 {
    margin: 0;
    font-family: var(--serif);
    font-weight: 600;
    /* 中文不适合负字距，改成轻微加宽 */
    letter-spacing: .06em;
    line-height: 1.06;
    color: #f6faff;
    font-size: clamp(3rem, 8.2vw, 6.6rem);
    /* 不能用 text-shadow：内层 .tk 的 overflow:hidden 会把光晕裁成矩形，改用 drop-shadow 跟随字形 */
    filter: drop-shadow(0 0 26px rgba(121, 213, 255, .45)) drop-shadow(0 0 70px rgba(61, 107, 255, .35));
  }
}

/* 横向裁切必须挂在 body 上：放在 .nebula 这个 div 上会让它自己变成滚动容器 */
body.nb-page { overflow-x: hidden }
html.nb-locked body { overflow: hidden; height: 100vh }
html.nb-reduce .nebula *,
html.nb-reduce .nebula *::before,
html.nb-reduce .nebula *::after {
  animation-duration: 1ms !important;
  animation-iteration-count: 1 !important;
  transition-duration: 1ms !important;
}

/* ===== 星云舞台 ===== */
.nb-stage {
  position: fixed; inset: 0; width: 100%; height: 100%; z-index: 0; display: block;
  background: radial-gradient(130% 100% at 42% 34%, #14306b 0%, #0a1c44 34%, #050f28 62%, #020814 100%);
}
/* 地平线辉光 + 水面反光，对应参考图的下半部 */
.nb-veil {
  position: fixed; inset: 0; z-index: 1; pointer-events: none;
  background:
    radial-gradient(72% 26% at 38% 74%, rgba(96, 168, 255, .3), transparent 70%),
    linear-gradient(180deg, rgba(2, 6, 18, .55) 0, rgba(2, 6, 18, 0) 22%, rgba(2, 6, 18, 0) 66%, rgba(3, 10, 28, .72) 88%, rgba(1, 5, 16, .95) 100%),
    radial-gradient(105% 85% at 50% 48%, rgba(0, 0, 0, 0) 46%, rgba(1, 4, 14, .5) 82%, rgba(0, 2, 10, .88) 100%),
    linear-gradient(90deg, rgba(2, 6, 18, 0) 34%, rgba(2, 6, 18, .5) 64%, rgba(2, 6, 18, .8) 100%);
}
.nb-grain {
  position: fixed; inset: -110px; z-index: 2; pointer-events: none; opacity: var(--grain);
  mix-blend-mode: soft-light; transition: opacity var(--t4) var(--ease-out);
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='170' height='170'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='3' stitchTiles='stitch'/><feColorMatrix type='saturate' values='0'/></filter><rect width='170' height='170' filter='url(%23n)'/></svg>");
  animation: nbGrainShift 1.05s steps(3, end) infinite;
}
@keyframes nbGrainShift {
  0% { transform: translate3d(0, 0, 0) }
  33% { transform: translate3d(-11px, 7px, 0) }
  66% { transform: translate3d(9px, -9px, 0) }
  100% { transform: translate3d(0, 0, 0) }
}

/* ===== 加载器 ===== */
.nb-loader {
  position: fixed; inset: 0; z-index: 60; background: var(--void); display: grid; place-items: center;
  .lg { display: grid; gap: 20px; justify-items: center; width: min(520px, 80vw); text-align: center }
  .ring { position: relative; width: 96px; height: 96px;
    svg { width: 100%; height: 100%; transform: rotate(-90deg) }
    circle { fill: none; stroke-width: 1.5 }
    .bg { stroke: rgba(150, 185, 255, .14) }
    .fg { stroke: var(--azure); stroke-linecap: round; filter: drop-shadow(0 0 8px rgba(121, 213, 255, .8)) }
  }
  .pct { position: absolute; inset: 0; display: grid; place-items: center; font-family: var(--mono); font-size: 19px; color: var(--ice); font-variant-numeric: tabular-nums }
  .wm { font-family: var(--serif); font-style: italic; font-size: 22px; letter-spacing: .34em; text-indent: .34em }
  .sub { font-family: var(--mono); font-size: 10.5px; letter-spacing: .3em; text-transform: uppercase; color: var(--ice-3) }
  .bar { width: 100%; height: 1px; background: rgba(150, 185, 255, .16); position: relative; overflow: hidden;
    i { position: absolute; inset: 0; transform: scaleX(0); transform-origin: left; background: linear-gradient(90deg, #3d6bff, var(--azure), var(--lav)) }
  }
}

/* ===== 四角版记 ===== */
.nb-frame { position: fixed; inset: 0; z-index: 3; pointer-events: none }
.frame-mark {
  position: fixed; display: flex; align-items: center; gap: 10px;
  font-family: var(--mono); font-size: 10px; letter-spacing: .28em; text-transform: uppercase; color: var(--ice-3);
  i { font-style: normal; color: var(--azure); text-shadow: 0 0 10px var(--azure) }
  em { font-style: normal; color: rgba(170, 196, 255, .26); padding-left: 2px }
  &.tl { top: 26px; left: clamp(20px, 4vw, 44px) }
  &.tr { top: 26px; right: clamp(20px, 4vw, 44px) }
  &.bl { bottom: 26px; left: clamp(20px, 4vw, 44px); letter-spacing: .34em;
    i { margin: 0 1px; font-size: 8px }
  }
}

/* ===== 文案块 ===== */
.nb-hero {
  position: relative; z-index: 3; min-height: 100vh;
  display: grid; align-content: center;
  padding: clamp(80px, 14vh, 160px) clamp(20px, 6vw, 96px);
  max-width: 1500px; margin: 0 auto;
}
.inst { max-width: 640px; margin-left: auto }

/* 标题上方的细线与菱形 */
.rule {
  display: flex; align-items: center; gap: 12px; margin: 0 0 22px;
  i { flex: none; width: 7px; height: 7px; transform: rotate(45deg); background: var(--azure); box-shadow: 0 0 12px var(--azure) }
  s { flex: 1; height: 1px; text-decoration: none; background: linear-gradient(90deg, var(--line-2), transparent) }
}

/* 标题外围的椭圆轨道与右上角四角星，对应参考图穿过字面的光环 */
.title-wrap { position: relative; display: inline-block }
.title-orbit {
  position: absolute; left: -7%; right: -5%; top: -34%; bottom: -30%;
  border: 1px solid rgba(150, 195, 255, .26);
  border-radius: 50%;
  transform: rotate(-9deg);
  pointer-events: none;
  animation: orbitDrift 11s ease-in-out infinite;
}
.title-spark {
  position: absolute; right: -14px; top: -6px; width: 16px; height: 16px;
  background: linear-gradient(180deg, #ffffff, rgba(190, 225, 255, .7));
  clip-path: polygon(50% 0%, 61% 39%, 100% 50%, 61% 61%, 50% 100%, 39% 61%, 0% 50%, 39% 39%);
  filter: drop-shadow(0 0 8px rgba(150, 210, 255, .9));
  animation: nbTwinkle 3.2s ease-in-out infinite;
  pointer-events: none;
}
@keyframes orbitDrift {
  0%, 100% { transform: rotate(-9deg) scale(1) }
  50% { transform: rotate(-6deg) scale(1.03) }
}
@keyframes nbTwinkle {
  0%, 100% { opacity: .35; transform: scale(.7) rotate(0deg) }
  50% { opacity: 1; transform: scale(1.15) rotate(45deg) }
}

/* 副题：宽字距明朝体，中间用间隔点 */
.tagline {
  display: flex; align-items: center; gap: .9em; margin: 26px 0 0;
  font-family: var(--serif); font-weight: 400;
  font-size: clamp(.95rem, 1.5vw, 1.28rem); letter-spacing: .34em;
  color: rgba(190, 219, 255, .92);
  text-shadow: 0 0 22px rgba(121, 213, 255, .45);
  i { font-style: normal; color: var(--azure); text-shadow: 0 0 12px var(--azure) }
}
/* 正文：首句用明朝体抬起来，支撑句压暗缩小，左侧细轴把三行绑成一块 */
.lede {
  position: relative; margin: 28px 0 0; padding-left: 18px;
  &::before { content: ""; position: absolute; left: 0; top: 7px; bottom: 10px; width: 1px;
    background: linear-gradient(180deg, rgba(121, 213, 255, .6), rgba(121, 213, 255, .05)) }
}
.lede-lead {
  font-family: var(--serif); font-size: 17px; font-weight: 400; letter-spacing: .1em;
  color: rgba(226, 238, 255, .88);
  em { font-style: normal; color: #9fd4ff; text-shadow: 0 0 18px rgba(121, 213, 255, .62) }
}
.lede-sub { margin-top: 11px; font-size: 12.5px; line-height: 2.1; letter-spacing: .05em; color: rgba(160, 186, 228, .5) }

/* 提示行：拆成「交互暗示」与「快捷键」两段，键位做成实体键帽 */
.hint { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 16px; margin: 28px 0 0 }
.hint-cue { font-family: var(--serif); font-size: 12px; letter-spacing: .16em; color: rgba(170, 196, 255, .46) }
.hint-keys {
  display: inline-flex; align-items: center; gap: 8px;
  font-family: var(--mono); font-size: 10px; letter-spacing: .12em; color: var(--ice-3);
  kbd { font-family: var(--mono); font-size: 9.5px; padding: 3px 7px; border-radius: 4px;
    border: 1px solid var(--line-2); background: rgba(121, 213, 255, .07); color: var(--azure);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, .08), 0 0 10px rgba(121, 213, 255, .16) }
  i { font-style: normal; color: rgba(170, 196, 255, .26); margin: 0 2px }
}
.tk { display: inline-block; overflow: hidden; vertical-align: top; padding-bottom: .14em; margin-bottom: -.14em;
  > i { display: inline-block; font-style: inherit; will-change: transform }
}

.acts { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-top: 36px }
/* h1 由 GSAP 逐字揭示，其余各行用 CSS 错峰上浮接住，避免半动画断层 */
.rule, .title-wrap, .tagline, .lede, .acts, .hint { animation: nbRise 0.62s var(--ease-out) both }
.rule { animation-delay: 0.04s }
.title-wrap { animation-delay: 0.1s }
.tagline { animation-delay: 0.52s }
.lede { animation-delay: 0.64s }
.acts { animation-delay: 0.78s }
.hint { animation-delay: 0.9s }
@keyframes nbRise {
  from { opacity: 0; transform: translateY(14px) }
  to { opacity: 1; transform: none }
}

/* ===== 进入空间按钮 ===== */
.mag { position: relative; display: inline-block }
/* 按钮左侧的镜头光晕条，必须放在按钮外，否则会被 overflow:hidden 裁掉 */
.btn-flare {
  position: absolute; left: -76px; top: 50%; width: 86px; height: 2px;
  transform: translateY(-50%); pointer-events: none;
  background: linear-gradient(90deg, transparent, rgba(160, 215, 255, .9));
  filter: blur(1.2px);
  animation: nbFlare 4.2s ease-in-out infinite;
}
@keyframes nbFlare {
  0%, 100% { opacity: .25; transform: translateY(-50%) scaleX(.7) }
  50% { opacity: .95; transform: translateY(-50%) scaleX(1) }
}
.btn {
  position: relative; display: inline-flex; align-items: center; gap: 12px; cursor: pointer; padding: 14px 28px;
  border: 1px solid var(--line-2); border-radius: 100px; background: rgba(10, 22, 52, .5); color: var(--ice);
  font-family: var(--mono); font-size: 11px; letter-spacing: .24em; text-transform: uppercase; overflow: hidden;
  transition: border-color var(--t2) linear, box-shadow var(--t3) var(--ease-out), transform var(--t3) var(--ease-out);
  /* 光斑跟随指针，--mx/--my 由 trackSpotlight 写入 */
  &::after { content: ""; position: absolute; inset: 0; z-index: 1; opacity: 0; transition: opacity var(--t3) linear;
    background: radial-gradient(150px 70px at var(--mx, 50%) var(--my, 50%), rgba(255, 255, 255, .34), transparent 68%) }
  &:hover::after { opacity: 1 }
  .arrow { font-style: normal; transition: transform var(--t3) var(--ease-out) }
  &:hover .arrow { transform: translateX(6px) }
  &.enter {
    z-index: 1; border-color: transparent; color: #fff; padding: 16px 34px; font-size: 12px;
    background: linear-gradient(100deg, #2f66ff 0%, #4d8dff 48%, #7cc4ff 100%);
    box-shadow: 0 0 0 1px rgba(121, 213, 255, .38), 0 16px 42px -16px rgba(47, 102, 255, .95), 0 0 46px rgba(61, 140, 255, .4);
    animation: nbEnterBreath 3.6s ease-in-out infinite;
    &:hover { transform: translateY(-2px);
      box-shadow: 0 0 0 1px rgba(160, 220, 255, .6), 0 22px 54px -16px rgba(47, 102, 255, 1), 0 0 66px rgba(90, 170, 255, .62) }
  }
}
/* 掠过式高光 */
.btn-shine {
  position: absolute; top: -60%; left: -140%; width: 55%; height: 220%; z-index: 1; pointer-events: none;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, .55), transparent);
  transform: skewX(-18deg);
  animation: nbShine 3.6s ease-in-out infinite;
}
@keyframes nbShine {
  0%, 58% { left: -140% }
  92%, 100% { left: 175% }
}
@keyframes nbEnterBreath {
  0%, 100% { box-shadow: 0 0 0 1px rgba(121, 213, 255, .38), 0 16px 42px -16px rgba(47, 102, 255, .95), 0 0 40px rgba(61, 140, 255, .34) }
  50% { box-shadow: 0 0 0 1px rgba(150, 220, 255, .55), 0 18px 46px -16px rgba(47, 102, 255, 1), 0 0 64px rgba(90, 170, 255, .58) }
}

@media (max-width: 1080px) {
  .nb-hero { padding-inline: clamp(18px, 5vw, 40px) }
  .inst { max-width: none; margin-left: 0 }
  .frame-mark.tr, .frame-mark.bl { display: none }
  .nb-stage { opacity: .78 }
  .nb-veil { background: linear-gradient(180deg, rgba(2, 6, 18, .3), rgba(2, 6, 18, .66) 46%, rgba(1, 5, 16, .9)) }
}
@media (prefers-reduced-motion: reduce) {
  .nb-grain { animation: none }
}
</style>
