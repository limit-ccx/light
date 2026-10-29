<template>
  <div class="ai-warm-card">
    <div class="ai-glow"></div>
    <div class="ai-header">
      <div class="ai-avatar">
        <span class="ai-avatar-icon">✨</span>
      </div>
      <div class="ai-info">
        <span class="ai-name">纸域空间小暖</span>
        <span class="ai-badge">AI 伙伴</span>
      </div>
    </div>
    <div class="ai-content">
      <span class="typing-text">{{ displayText }}</span>
      <span v-if="isTyping" class="typing-cursor">|</span>
    </div>
    <div class="ai-footer">
      <span class="ai-disclaimer">AI 回应仅供参考，请温和对待自己 💛</span>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  content: { type: String, required: true },
  animate: { type: Boolean, default: true }
})

const displayText = ref('')
const isTyping = ref(false)

const typeText = async (text) => {
  if (!props.animate) {
    displayText.value = text
    return
  }
  displayText.value = ''
  isTyping.value = true
  for (let i = 0; i < text.length; i++) {
    displayText.value = text.slice(0, i + 1)
    await new Promise(r => setTimeout(r, 30 + Math.random() * 20))
  }
  isTyping.value = false
}

onMounted(() => {
  if (props.animate) {
    setTimeout(() => typeText(props.content), 300)
  } else {
    displayText.value = props.content
  }
})

watch(() => props.content, (newContent) => {
  typeText(newContent)
})
</script>

<style lang="scss" scoped>
.ai-warm-card {
  position: relative;
  border-radius: 16px;
  padding: 20px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.08) 0%, rgba(236, 72, 153, 0.08) 50%, rgba(139, 92, 246, 0.08) 100%);
  border: 1px solid rgba(251, 191, 36, 0.2);
  backdrop-filter: blur(10px);
}

.ai-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 30% 30%, rgba(251, 191, 36, 0.06) 0%, transparent 60%);
  pointer-events: none;
  animation: glow-rotate 20s linear infinite;
}

@keyframes glow-rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  position: relative;
  z-index: 1;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fbbf24, #f472b6);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(251, 191, 36, 0.3);

  .ai-avatar-icon {
    font-size: 20px;
  }
}

.ai-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ai-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text, #1e293b);
}

.ai-badge {
  font-size: 11px;
  font-weight: 600;
  color: #f59e0b;
  background: rgba(251, 191, 36, 0.15);
  padding: 1px 8px;
  border-radius: 999px;
  width: fit-content;
}

.ai-content {
  position: relative;
  z-index: 1;
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text, #334155);
  margin-bottom: 14px;
  min-height: 40px;

  .typing-text {
    white-space: pre-wrap;
    word-break: break-word;
  }

  .typing-cursor {
    display: inline-block;
    animation: blink 0.8s infinite;
    color: #f59e0b;
    font-weight: bold;
  }
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.ai-footer {
  position: relative;
  z-index: 1;
}

.ai-disclaimer {
  font-size: 11px;
  color: var(--color-text-muted, #94a3b8);
  font-style: italic;
}

[data-theme="dark"] .ai-warm-card {
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.05) 0%, rgba(236, 72, 153, 0.05) 50%, rgba(139, 92, 246, 0.05) 100%);
  border-color: rgba(251, 191, 36, 0.15);
}

[data-theme="dark"] .ai-badge {
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.1);
}
</style>
