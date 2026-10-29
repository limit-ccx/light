<template>
  <article class="article-card" @click="goToDetail">
    <div class="card-header">
      <div class="author-info">
        <img :src="article.author?.avatar || defaultAvatar" alt="头像" class="author-avatar" @error="onAvatarError" />
        <div class="author-meta">
          <span class="author-name">{{ article.author?.username || '匿名用户' }}</span>
          <span class="publish-time">{{ formatTime(article.createdAt) }}</span>
        </div>
      </div>
      <div class="tags" v-if="article.tags?.length">
        <span v-for="tag in article.tags.slice(0, 2)" :key="tag" class="tag">{{ tag }}</span>
      </div>
    </div>

    <h3 class="card-title">{{ article.title }}</h3>
    <p class="card-summary">{{ article.summary || '' }}</p>

    <div class="card-footer">
      <div class="stats">
        <span class="stat-item">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
          {{ article.viewCount || 0 }}
        </span>
        <span class="stat-item stat-like" :class="{ active: article.isLiked }" @click.stop="handleLike">
          <svg width="16" height="16" viewBox="0 0 24 24" :fill="article.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
          </svg>
          {{ article.likeCount || 0 }}
        </span>
        <span class="stat-item stat-favorite" :class="{ active: article.isFavorited }" @click.stop="handleFavorite">
          <svg width="16" height="16" viewBox="0 0 24 24" :fill="article.isFavorited ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
            <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>
          </svg>
          {{ article.favoriteCount || 0 }}
        </span>
      </div>
      <span class="read-more">阅读全文 →</span>
    </div>
  </article>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  article: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['like', 'favorite'])

const router = useRouter()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const onAvatarError = (event) => {
  if (event.target.src !== defaultAvatar) event.target.src = defaultAvatar
}

const goToDetail = () => {
  router.push(`/articles/${props.article.id}`)
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

const handleLike = () => {
  emit('like', props.article.id)
}

const handleFavorite = () => {
  emit('favorite', props.article.id)
}
</script>

<style lang="scss" scoped>
.article-card {
  padding: 24px;
  cursor: pointer;
  border-radius: 24px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
  transition: all var(--transition-normal);

  &:hover {
    transform: translateY(-3px);
    box-shadow: var(--content-shadow-lg);
    border-color: var(--content-line-strong);
  }
}

.card-header {
  @include flex-between;
  margin-bottom: 16px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  object-fit: cover;
  border: 1px solid var(--content-line);
}

.author-meta {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.author-name {
  font-size: 13.5px;
  color: var(--content-ink);
  font-weight: 500;
}

.publish-time {
  font-size: 12px;
  color: var(--content-ink-3);
}

.tags {
  display: flex;
  gap: 8px;
}

.tag {
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-primary);
  background: var(--color-google-blue-light);
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);

  &:nth-child(even) {
    color: var(--color-success);
    background: var(--color-google-green-light);
  }
}

.card-title {
  font-size: 19px;
  font-weight: 500;
  color: var(--content-ink);
  margin-bottom: 10px;
  line-height: 1.4;
  letter-spacing: -0.01em;
  @include text-ellipsis(1);
  transition: color var(--transition-fast);

  .article-card:hover & {
    color: var(--color-primary);
  }
}

.card-summary {
  font-size: 14px;
  color: var(--content-ink-2);
  line-height: 1.7;
  margin-bottom: 20px;
  @include text-ellipsis(2);
}

.card-footer {
  @include flex-between;
  padding-top: 14px;
  border-top: 1px solid var(--content-line);
}

.stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--content-ink-3);
  transition: all var(--transition-fast);

  svg {
    flex-shrink: 0;
  }

  &.active {
    color: var(--color-primary);
    
    &.stat-like {
      color: var(--color-google-red);
    }
  }

  &:not(.active):hover {
    color: var(--content-ink);
    cursor: pointer;
  }
}

.read-more {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.4);
  background: linear-gradient(180deg, rgba(30, 41, 59, 0.55) 0%, rgba(15, 23, 42, 0.66) 100%);
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  opacity: 0;
  transform: translateX(-8px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.16);
  transition: opacity 0.2s ease, transform 0.2s cubic-bezier(0.4, 0, 0.2, 1),
    box-shadow 0.2s ease, border-color 0.2s ease;
}

.article-card:hover .read-more {
  opacity: 1;
  transform: translateX(0);
  border-color: rgba(147, 197, 253, 0.5);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.22);
}
</style>
