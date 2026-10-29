<template>
  <Teleport to="body">
    <div class="book-intro" :class="{ 'is-revealing': revealing }" role="presentation">
      <div ref="host" class="intro-canvas" aria-hidden="true"></div>
      <div class="intro-vignette" aria-hidden="true"></div>
      <div class="intro-hud"><b>纸域空间 //</b> 写 作 即 思 考</div>
      <button class="intro-skip" type="button" @click="skip">跳过</button>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { createMagicBookScene } from '@/three/magicBookScene'

const emit = defineEmits(['reveal', 'done'])

const host = ref(null)
const revealing = ref(false)

let scene = null
let fadeTimer = 0
let done = false

const finish = () => {
  if (done) return
  done = true
  clearTimeout(fadeTimer)
  scene?.dispose()
  scene = null
  emit('done')
}

const beginFadeOut = () => {
  if (revealing.value) return
  revealing.value = true
  fadeTimer = window.setTimeout(finish, 780)
}

const skip = () => {
  if (done) return
  if (scene) {
    scene.skip()
  } else {
    emit('reveal')
    finish()
  }
}

const onKeydown = (e) => {
  if (e.key === 'Escape') skip()
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)

  if (!host.value) {
    emit('reveal')
    finish()
    return
  }

  // ?introAt=秒数 可直接跳到时间轴某一刻，便于预览
  const startAt = Number(new URLSearchParams(window.location.search).get('introAt')) || 0

  try {
    scene = createMagicBookScene({
      host: host.value,
      startAt: Math.max(0, startAt),
      onReveal: () => emit('reveal'),
      onFinished: beginFadeOut
    })
  } catch (err) {
    console.error('[BookIntro] three.js 场景初始化失败，跳过入场动画', err)
    emit('reveal')
    finish()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  clearTimeout(fadeTimer)
  scene?.dispose()
  scene = null
})
</script>

<style lang="scss" scoped>
.book-intro {
  position: fixed;
  inset: 0;
  z-index: 10050;
  overflow: hidden;
  background: #04060d;
  animation: intro-veil-in 0.35s ease both;
}

.book-intro.is-revealing {
  animation: intro-veil-out 0.78s ease 0.05s both;
  pointer-events: none;
}

.intro-canvas {
  position: absolute;
  inset: 0;

  :deep(canvas) {
    display: block;
    width: 100%;
    height: 100%;
  }
}

.intro-vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(ellipse at 50% 42%, rgba(0, 0, 0, 0) 42%, rgba(2, 4, 10, 0.55) 100%);
}

.intro-hud {
  position: absolute;
  left: 26px;
  bottom: 22px;
  pointer-events: none;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: rgba(178, 202, 255, 0.72);
  text-shadow: 0 0 12px rgba(120, 150, 255, 0.8);

  b {
    color: #cfe0ff;
    font-weight: 600;
  }
}

.intro-skip {
  position: absolute;
  right: 26px;
  bottom: 20px;
  height: 34px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(226, 232, 240, 0.82);
  font-family: inherit;
  font-size: 13px;
  letter-spacing: 0.08em;
  cursor: pointer;
  backdrop-filter: blur(10px);
  transition: all 0.2s ease;

  &:hover {
    color: #fff;
    border-color: rgba(147, 197, 253, 0.6);
    background: rgba(59, 130, 246, 0.16);
    box-shadow: 0 8px 22px rgba(37, 99, 235, 0.28);
  }
}

@keyframes intro-veil-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes intro-veil-out {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}

@media (max-width: 640px) {
  .intro-hud {
    left: 50%;
    bottom: 66px;
    transform: translateX(-50%);
    white-space: nowrap;
  }

  .intro-skip {
    right: 50%;
    bottom: 24px;
    transform: translateX(50%);
  }
}
</style>
