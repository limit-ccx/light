<template>
  <Teleport to="body">
    <Transition name="th-ov">
      <div
        v-if="modelValue"
        class="th-ov"
        :class="`is-${variant}`"
        @click.self="onBackdrop"
      >
        <div
          ref="surfaceEl"
          class="th-ov-surface"
          :style="surfaceStyle"
          role="dialog"
          aria-modal="true"
          :aria-label="title || '详情'"
          tabindex="-1"
        >
          <header v-if="title || $slots.head" class="th-ov-head">
            <div class="th-ov-head-main">
              <slot name="head">{{ title }}</slot>
            </div>
            <button type="button" class="th-ov-close" aria-label="关闭" @click="close">
              <span v-html="closeIcon"></span>
            </button>
          </header>

          <div class="th-ov-body">
            <slot />
          </div>

          <footer v-if="$slots.footer" class="th-ov-foot">
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { NIGHT_ICONS } from '@/data/treeHoleNight'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  variant: { type: String, default: 'panel' },
  title: { type: String, default: '' },
  width: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'close'])

const closeIcon = NIGHT_ICONS.close
const surfaceEl = ref(null)
let scrollLocks = 0
let lastOverflow = ''
let keydownHandler = null

const surfaceStyle = computed(() =>
  props.variant === 'panel' && props.width ? { width: props.width } : {}
)

const close = () => {
  emit('update:modelValue', false)
  emit('close')
}

const onBackdrop = () => {
  if (props.variant !== 'fullscreen') close()
}

const releaseLock = () => {
  if (!scrollLocks) return
  scrollLocks -= 1
  if (scrollLocks === 0) {
    document.body.style.overflow = lastOverflow
  }
}

watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      if (scrollLocks === 0) {
        lastOverflow = document.body.style.overflow
        document.body.style.overflow = 'hidden'
      }
      scrollLocks += 1
      keydownHandler = (e) => {
        if (e.key === 'Escape') close()
      }
      window.addEventListener('keydown', keydownHandler)
      await nextTick()
      surfaceEl.value?.focus?.()
    } else {
      if (keydownHandler) {
        window.removeEventListener('keydown', keydownHandler)
        keydownHandler = null
      }
      releaseLock()
    }
  }
)

onBeforeUnmount(() => {
  if (keydownHandler) window.removeEventListener('keydown', keydownHandler)
  if (props.modelValue) releaseLock()
})
</script>

<style scoped>
.th-ov {
  position: fixed;
  inset: 0;
  z-index: 10060;
  display: flex;
  background: rgba(3, 6, 15, 0.72);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}

.th-ov-surface {
  position: relative;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, rgba(17, 24, 45, 0.96) 0%, rgba(9, 13, 27, 0.97) 100%);
  border: 1px solid rgba(148, 170, 220, 0.16);
  box-shadow: 0 40px 90px rgba(2, 4, 12, 0.65);
  color: #e6ecff;
  outline: none;

  /* 让复用的浅色子组件（ExpandableText / AiWarmCard）在暗色里也读得清 */
  --color-text: #e6ecff;
  --color-text-secondary: rgba(206, 220, 250, 0.78);
  --color-text-muted: rgba(178, 196, 235, 0.62);
  --color-card: #111a33;
}

.is-panel {
  justify-content: flex-end;
}

.is-panel .th-ov-surface {
  width: min(600px, 100vw);
  height: 100%;
  border-radius: 0;
  border-top: none;
  border-bottom: none;
  border-right: none;
}

.is-modal {
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.is-modal .th-ov-surface {
  width: min(460px, 100%);
  border-radius: 22px;
}

.is-fullscreen .th-ov-surface {
  width: min(880px, 100%);
  height: min(92vh, 860px);
  margin: auto;
  border-radius: 26px;
}

.th-ov-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px 14px;
  border-bottom: 1px solid rgba(148, 170, 220, 0.12);
  flex-shrink: 0;
}

.th-ov-head-main {
  flex: 1;
  min-width: 0;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.th-ov-close {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 50%;
  border: 1px solid rgba(148, 170, 220, 0.18);
  background: rgba(255, 255, 255, 0.04);
  color: rgba(214, 226, 255, 0.8);
  cursor: pointer;
  transition: all 0.2s ease;
}

.th-ov-close:hover {
  color: #fff;
  border-color: rgba(160, 190, 255, 0.45);
  background: rgba(90, 120, 220, 0.22);
}

.th-ov-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 22px 24px 26px;
  scrollbar-width: thin;
  scrollbar-color: rgba(140, 165, 225, 0.35) transparent;
}

.th-ov-body::-webkit-scrollbar {
  width: 8px;
}

.th-ov-body::-webkit-scrollbar-thumb {
  background: rgba(140, 165, 225, 0.28);
  border-radius: 8px;
}

.th-ov-foot {
  flex-shrink: 0;
  padding: 14px 24px 20px;
  border-top: 1px solid rgba(148, 170, 220, 0.12);
}

.th-ov-enter-active,
.th-ov-leave-active {
  transition: opacity 0.26s ease;
}

.th-ov-enter-active .th-ov-surface,
.th-ov-leave-active .th-ov-surface {
  transition: transform 0.32s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.26s ease;
}

.th-ov-enter-from,
.th-ov-leave-to {
  opacity: 0;
}

.is-panel .th-ov-enter-from .th-ov-surface,
.is-panel .th-ov-leave-to .th-ov-surface {
  transform: translateX(28px);
}

.is-modal .th-ov-enter-from .th-ov-surface,
.is-modal .th-ov-leave-to .th-ov-surface,
.is-fullscreen .th-ov-enter-from .th-ov-surface,
.is-fullscreen .th-ov-leave-to .th-ov-surface {
  transform: translateY(14px) scale(0.985);
}

@media (max-width: 640px) {
  .is-fullscreen .th-ov-surface,
  .is-panel .th-ov-surface {
    width: 100vw;
    height: 100%;
    border-radius: 0;
  }

  .th-ov-head,
  .th-ov-body,
  .th-ov-foot {
    padding-left: 18px;
    padding-right: 18px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .th-ov-enter-active,
  .th-ov-leave-active,
  .th-ov-enter-active .th-ov-surface,
  .th-ov-leave-active .th-ov-surface {
    transition-duration: 0.01ms;
  }
}
</style>
