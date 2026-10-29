<template>
  <div class="tag-cloud">
    <h3 class="tag-cloud-title">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
        <line x1="7" y1="7" x2="7.01" y2="7"/>
      </svg>
      热门标签
    </h3>
    <div class="tag-cloud-list">
      <router-link
        v-for="item in tags"
        :key="item.tag"
        :to="{ path: '/articles', query: { tag: item.tag } }"
        class="tag-cloud-item"
        :style="getTagStyle(item.count)"
      >
        {{ item.tag }}
        <span class="tag-count">{{ item.count }}</span>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTagCloud } from '@/api/article'

const tags = ref([])

const getTagStyle = (count) => {
  const max = Math.max(...tags.value.map(t => t.count), 1)
  const min = Math.min(...tags.value.map(t => t.count), 1)
  const ratio = max === min ? 0.5 : (count - min) / (max - min)
  const fontSize = 0.85 + ratio * 0.6
  const opacity = 0.6 + ratio * 0.4
  return {
    fontSize: `${fontSize}rem`,
    opacity: opacity
  }
}

onMounted(async () => {
  try {
    const res = await getTagCloud()
    tags.value = res.data || []
  } catch {
    tags.value = []
  }
})
</script>

<style lang="scss" scoped>
.tag-cloud {
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  @include liquid-glass;
}

.tag-cloud-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-primary);
}

.tag-cloud-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.tag-cloud-item {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: var(--bg-primary);
  color: var(--text-primary);
  text-decoration: none;
  transition: all var(--transition-fast);
  border: 1px solid var(--border-color);

  &:hover {
    background: var(--accent-primary);
    color: white;
    border-color: var(--accent-primary);
    transform: translateY(-2px);
  }
}

.tag-count {
  font-size: 0.7em;
  opacity: 0.7;
  background: var(--bg-secondary);
  padding: 0.1rem 0.4rem;
  border-radius: 999px;
}

.tag-cloud-item:hover .tag-count {
  background: rgba(255,255,255,0.2);
  color: white;
}
</style>
