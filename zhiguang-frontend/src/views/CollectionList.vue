<template>
  <div class="collection-list-page content-page">
    <Header />

    <main class="page-main">
      <div class="container">
        <ContentHero
          eyebrow="COLLECT · 主题阅读"
          title="文章合集"
          description="把散落的文章收进同一个主题。像整理书架一样，按一条线索系统读完。"
          active="collections"
          :stats="heroStats"
        />

        <div class="section-head">
          <div>
            <h2>全部合集</h2>
            <p>{{ collections.length }} 个由创作者策划的主题书单</p>
          </div>
        </div>

        <div v-if="loading" class="collection-grid">
          <div v-for="i in 4" :key="i" class="collection-card is-skeleton">
            <span class="sk sk-line" style="width: 32%; height: 18px"></span>
            <span class="sk sk-line" style="width: 100%"></span>
            <span class="sk sk-line" style="width: 70%"></span>
          </div>
        </div>

        <div v-else-if="collections.length" class="collection-grid">
          <article
            v-for="(collection, index) in collections"
            :key="collection.id"
            class="collection-card"
            :class="`accent-${index % 4}`"
            @click="openCollection(collection)"
          >
            <div class="collection-top">
              <span class="collection-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="collection-count">{{ collection.articleCount || 0 }} 篇</span>
            </div>

            <h3>{{ collection.name }}</h3>
            <p class="collection-desc">{{ collection.description || '策划者还没有写说明，点开看看收录了什么。' }}</p>

            <div class="collection-foot">
              <div class="collection-author">
                <span class="author-dot"></span>
                <span>策划 · {{ collection.author?.username || '匿名作者' }}</span>
              </div>
              <span class="collection-arrow">打开合集 →</span>
            </div>
          </article>
        </div>

        <div v-else class="empty-state">
          <div class="empty-books"><span></span><span></span><span></span></div>
          <h3>还没有合集</h3>
          <p>把几篇相关的文章收在一起，就是一份小小的书单。</p>
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
import { getCollections } from '@/api/collection'

const router = useRouter()
const collections = ref([])
const loading = ref(false)

const heroStats = computed(() => {
  const articles = collections.value.reduce((sum, c) => sum + (c.articleCount || 0), 0)
  const curators = new Set(collections.value.map((c) => c.author?.id).filter(Boolean)).size
  return [
    { value: collections.value.length, label: '个合集' },
    { value: articles, label: '篇文章' },
    { value: curators, label: '位策划' }
  ]
})

const openCollection = (collection) => {
  router.push(`/collection/${collection.id}`)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getCollections(0, 60)
    collections.value = res?.list || []
  } catch (e) {
    collections.value = []
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
    radial-gradient(70% 40% at 90% 4%, var(--content-glow-b), transparent 60%),
    radial-gradient(60% 40% at 4% 12%, var(--content-glow-a), transparent 62%),
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

.collection-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
  padding-bottom: 10px;
}

.collection-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 24px 26px 20px;
  border-radius: 20px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;

  /* 叠放的书页 */
  &::before,
  &::after {
    content: '';
    position: absolute;
    left: 12px;
    right: 12px;
    bottom: -1px;
    height: 100%;
    border-radius: 20px;
    background: var(--content-stack);
    border: 1px solid var(--content-line);
    z-index: -1;
    transition: transform 0.25s ease;
  }

  &::before {
    transform: translateY(5px) scale(0.985);
  }

  &::after {
    transform: translateY(10px) scale(0.97);
    opacity: 0.6;
  }

  &.accent-0 { --collection-accent: #1a73e8; }
  &.accent-1 { --collection-accent: #7c5cff; }
  &.accent-2 { --collection-accent: #ec4899; }
  &.accent-3 { --collection-accent: #0ea5a4; }

  &:hover {
    transform: translateY(-4px);
    border-color: color-mix(in srgb, var(--collection-accent) 30%, transparent);
    box-shadow: var(--content-shadow-lg);

    &::before { transform: translateY(8px) scale(0.985); }
    &::after { transform: translateY(15px) scale(0.97); }

    .collection-arrow {
      color: var(--collection-accent);
    }
  }
}

.collection-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.collection-index {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.04em;
  color: transparent;
  background: linear-gradient(135deg, var(--collection-accent), var(--content-ink-3));
  -webkit-background-clip: text;
  background-clip: text;
  opacity: 0.9;
}

.collection-count {
  padding: 4px 11px;
  border-radius: 999px;
  color: var(--collection-accent);
  background: color-mix(in srgb, var(--collection-accent) 9%, transparent);
  font-size: 11.5px;
  font-weight: 500;
}

.collection-card h3 {
  margin: 2px 0 0;
  font-size: 19px;
  font-weight: 600;
  color: var(--content-ink);
  letter-spacing: -0.01em;
}

.collection-desc {
  margin: 0;
  min-height: 44px;
  color: var(--content-ink-2);
  font-size: 13.5px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.collection-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 6px;
  padding-top: 14px;
  border-top: 1px solid var(--color-divider);
}

.collection-author {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--content-ink-3);
  font-size: 12px;
}

.author-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--collection-accent);
  box-shadow: 0 0 8px color-mix(in srgb, var(--collection-accent) 55%, transparent);
}

.collection-arrow {
  color: var(--content-ink-2);
  font-size: 12.5px;
  font-weight: 500;
  transition: color 0.2s ease;
}

/* 骨架 */
.collection-card.is-skeleton {
  cursor: default;
  pointer-events: none;
  gap: 14px;

  &::before,
  &::after {
    display: none;
  }
}

.sk {
  display: block;
  border-radius: 8px;
  background: linear-gradient(90deg, var(--content-sk-a) 0%, var(--content-sk-b) 50%, var(--content-sk-a) 100%);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
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

.empty-books {
  display: flex;
  align-items: flex-end;
  gap: 5px;
  height: 52px;

  span {
    width: 14px;
    border-radius: 4px;
    background: linear-gradient(180deg, rgba(26, 115, 232, 0.25), rgba(168, 85, 247, 0.18));

    &:nth-child(1) { height: 38px; }
    &:nth-child(2) { height: 50px; }
    &:nth-child(3) { height: 32px; }
  }
}

.empty-action {
  margin-top: 8px;
  color: #1a73e8;
  font-size: 13.5px;
  font-weight: 500;
}

@media (max-width: 860px) {
  .collection-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .section-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .collection-card {
    padding: 20px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .collection-card,
  .collection-card::before,
  .collection-card::after,
  .sk {
    transition: none;
    animation: none;
  }
}
</style>
