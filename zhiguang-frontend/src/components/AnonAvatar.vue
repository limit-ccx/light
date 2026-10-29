<template>
  <div class="anon-avatar" :class="{ 'compact': compact }">
    <div
      class="avatar-circle"
      :style="{
        backgroundColor: `${identity?.color || '#94a3b8'}18`,
        borderColor: `${identity?.color || '#94a3b8'}35`,
        color: identity?.color || '#94a3b8',
        boxShadow: `0 0 16px ${identity?.color || '#94a3b8'}18`
      }"
    >
      <span class="avatar-text">{{ avatarText }}</span>
    </div>
    <span v-if="!compact" class="avatar-name" :style="{ color: identity?.color || '#94a3b8' }">
      {{ identity?.displayName || '匿名旅人' }}
    </span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  identity: {
    type: Object,
    default: () => ({ displayName: '匿名旅人', color: '#94a3b8' })
  },
  compact: {
    type: Boolean,
    default: false
  }
})

const avatarText = computed(() => {
  const name = props.identity?.displayName || '匿名旅人'
  // 取前两个中文字符，更安静
  const chars = name.match(/[\u4e00-\u9fa5]/g) || []
  return chars.slice(0, 2).join('') || name.slice(0, 2) || '匿'
})
</script>

<style lang="scss" scoped>
.anon-avatar {
  display: flex;
  align-items: center;
  gap: 8px;

  &.compact {
    gap: 0;
  }
}

.avatar-circle {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1.5px solid;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;

  &:hover {
    transform: scale(1.08);
  }
}

.avatar-name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  opacity: 0.92;
}
</style>
