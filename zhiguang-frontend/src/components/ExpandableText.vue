<template>
  <div class="expandable-text">
    <div class="text-content" :class="{ 'is-collapsed': needsTruncation && !expanded }">
      <span class="text-inner">{{ content }}</span>
    </div>
    <button
      v-if="needsTruncation"
      class="expand-btn"
      @click.stop="expanded = !expanded"
    >
      {{ expanded ? '收起' : '展开全文' }}
    </button>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  content: { type: String, required: true },
  maxLength: { type: Number, default: 200 }
})

const expanded = ref(false)
const needsTruncation = computed(() => props.content.length > props.maxLength)
</script>

<style lang="scss" scoped>
.expandable-text {
  position: relative;
}

.text-content {
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-text);
  white-space: pre-wrap;
  word-break: break-word;

  &.is-collapsed {
    position: relative;
    max-height: 7.2em; /* approximately 4 lines at 1.8 line-height */
    overflow: hidden;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 3em;
      background: linear-gradient(transparent, var(--color-card, #fff));
      pointer-events: none;
    }
  }
}

.expand-btn {
  display: inline-block;
  margin-top: 6px;
  padding: 4px 0;
  border: none;
  background: none;
  color: var(--color-primary, #1A73E8);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.8;
  }
}
</style>
