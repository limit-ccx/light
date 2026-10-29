<template>
  <div class="column-list-page content-page">
    <Header />

    <main class="page-main">
      <div class="container">
        <ContentHero
          eyebrow="CURATED · 系统学习"
          title="知识专栏"
          description="由创作者持续打理的专题连载。订阅一个专栏，沿着一条线把知识读深。"
          active="columns"
          :stats="heroStats"
        />

        <div class="section-head">
          <div>
            <h2>全部专栏</h2>
            <p>{{ columns.length }} 个持续更新的主题岛屿</p>
          </div>
        </div>

        <div v-if="loading" class="column-grid">
          <div v-for="i in 6" :key="i" class="column-card is-skeleton">
            <span class="sk sk-cover"></span>
            <div class="sk-lines">
              <span class="sk sk-line" style="width: 55%; height: 18px"></span>
              <span class="sk sk-line" style="width: 100%"></span>
              <span class="sk sk-line" style="width: 80%"></span>
            </div>
          </div>
        </div>

        <div v-else-if="columns.length" class="column-grid">
          <article
            v-for="(column, index) in columns"
            :key="column.id"
            class="column-card"
            @click="openColumn(column)"
          >
            <div class="column-cover" :class="`tone-${index % 4}`">
              <img
                v-if="column.cover"
                :src="column.cover"
                alt=""
                loading="lazy"
                @error="hideBrokenImage"
              />
              <span v-else class="cover-letter">{{ (column.name || '专')[0] }}</span>
              <span class="cover-shade"></span>
              <span class="cover-chip">专栏</span>
              <span class="cover-count">{{ column.articleCount || 0 }} 篇</span>
            </div>

            <div class="column-body">
              <h3>{{ column.name }}</h3>
              <p class="column-desc">{{ column.description || '作者还没有写简介，先进入看看。' }}</p>

              <div class="column-author">
                <img :src="column.author?.avatar || defaultAvatar" alt="" />
                <span>{{ column.author?.username || '匿名作者' }}</span>
              </div>

              <div class="column-foot">
                <span class="subscribers">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                    <circle cx="9" cy="7" r="4" />
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                  </svg>
                  {{ column.subscriberCount || 0 }} 人订阅
                </span>
                <span class="enter-link">进入专栏 →</span>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="empty-state">
          <div class="empty-orbit"><span></span></div>
          <h3>还没有专栏</h3>
          <p>成为第一个把知识串成连载的人。</p>
        </div>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import ContentHero from '@/components/content/ContentHero.vue'
import { getColumns } from '@/api/column'

const router = useRouter()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const columns = ref([])
const loading = ref(false)

const heroStats = computed(() => {
  const articles = columns.value.reduce((sum, c) => sum + (c.articleCount || 0), 0)
  const subscribers = columns.value.reduce((sum, c) => sum + (c.subscriberCount || 0), 0)
  return [
    { value: columns.value.length, label: '个专栏' },
    { value: articles, label: '篇连载' },
    { value: subscribers, label: '次订阅' }
  ]
})

const hideBrokenImage = (event) => {
  event.target.style.display = 'none'
}

const openColumn = (column) => {
  router.push(`/column/${column.id}`)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getColumns(0, 60)
    columns.value = res?.list || []
  } catch (e) {
    columns.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.content-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(70% 40% at 88% 0%, var(--content-glow-a), transparent 60%),
    radial-gradient(60% 40% at 6% 16%, var(--content-glow-b), transparent 62%),
    var(--color-bg);
}

.page-main {
  flex: 1;
  padding: calc(var(--header-height) + 28px) 0 56px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin: 34px 0 18px;

  h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: var(--content-ink);
    letter-spacing: -0.01em;
  }

  p {
    margin: 4px 0 0;
    color: var(--content-ink-3);
    font-size: 13px;
  }
}

.head-link {
  color: #1a73e8;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.column-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.column-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 22px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;

  &:hover {
    transform: translateY(-5px);
    border-color: var(--content-line-strong);
    box-shadow: var(--content-shadow-lg);

    .enter-link {
      color: var(--color-primary);
    }

    .column-cover img {
      transform: scale(1.04);
    }
  }
}

.column-cover {
  position: relative;
  height: 168px;
  overflow: hidden;
  background: linear-gradient(135deg, #1e2a52, #43307a);

  &.tone-1 { background: linear-gradient(135deg, #173a5e, #1f6f8b); }
  &.tone-2 { background: linear-gradient(135deg, #3a2148, #7a2f5a); }
  &.tone-3 { background: linear-gradient(135deg, #1d3b33, #2f6b57); }

  img {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease;
  }
}

.cover-letter {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: rgba(255, 255, 255, 0.28);
  font-size: 68px;
  font-weight: 700;
}

.cover-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(10, 16, 32, 0.08) 0%, rgba(10, 16, 32, 0.6) 100%);
}

.cover-chip,
.cover-count {
  position: absolute;
  top: 14px;
  z-index: 1;
  padding: 5px 11px;
  border-radius: 999px;
  font-size: 11.5px;
  line-height: 1;
  backdrop-filter: blur(8px);
}

.cover-chip {
  left: 14px;
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.28);
  letter-spacing: 0.08em;
}

.cover-count {
  right: 14px;
  color: #fff;
  background: rgba(12, 18, 34, 0.38);
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.column-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 8px;
  padding: 18px 18px 16px;

  h3 {
    margin: 0;
    font-size: 17px;
    font-weight: 600;
    color: var(--content-ink);
    letter-spacing: -0.01em;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.column-desc {
  margin: 0;
  min-height: 42px;
  color: var(--content-ink-2);
  font-size: 13px;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.column-author {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
  color: var(--content-ink-3);
  font-size: 12px;

  img {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    object-fit: cover;
    border: 1px solid rgba(0, 0, 0, 0.05);
  }
}

.column-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--color-divider);
}

.subscribers {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--content-ink-3);
  font-size: 12px;
}

.enter-link {
  color: var(--content-ink-2);
  font-size: 12.5px;
  font-weight: 500;
  transition: color 0.2s ease;
}

/* 骨架 */
.column-card.is-skeleton {
  cursor: default;
  pointer-events: none;
}

.sk {
  display: block;
  border-radius: 8px;
  background: linear-gradient(90deg, var(--content-sk-a) 0%, var(--content-sk-b) 50%, var(--content-sk-a) 100%);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}

.sk-cover {
  height: 168px;
  border-radius: 0;
}

.sk-lines {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
}

.sk-line { height: 13px; }

@keyframes shimmer {
  from { background-position: 120% 0; }
  to { background-position: -20% 0; }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 76px 24px;
  border-radius: 24px;
  border: 1px dashed var(--content-line-strong);
  background: var(--content-veil);
  text-align: center;

  h3 { margin: 10px 0 0; font-size: 18px; color: var(--content-ink); }
  p { margin: 0; color: var(--content-ink-3); font-size: 13.5px; }
}

.empty-orbit {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 1px dashed rgba(26, 115, 232, 0.3);
  animation: spin 12s linear infinite;

  span {
    position: absolute;
    top: -4px;
    left: 50%;
    width: 9px;
    height: 9px;
    margin-left: -4.5px;
    border-radius: 50%;
    background: linear-gradient(135deg, #1a73e8, #a855f7);
    box-shadow: 0 0 12px rgba(26, 115, 232, 0.6);
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-action {
  margin-top: 8px;
  color: #1a73e8;
  font-size: 13.5px;
  font-weight: 500;
}

@media (max-width: 1024px) {
  .column-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .column-grid {
    grid-template-columns: 1fr;
  }

  .section-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .column-card,
  .column-cover img,
  .empty-orbit {
    transition: none;
    animation: none;
  }
}
</style>
