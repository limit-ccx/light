<template>
  <div class="collection-detail-page content-page">
    <Header />

    <main class="page-main">
      <div class="container">
        <div v-if="loading" class="detail-skeleton">
          <span class="sk sk-cover"></span>
          <div class="sk-lines">
            <span class="sk sk-line" style="width: 35%; height: 26px"></span>
            <span class="sk sk-line" style="width: 80%"></span>
            <span class="sk sk-line" style="width: 55%"></span>
          </div>
        </div>

        <template v-else-if="collection">
          <section class="detail-hero">
            <div class="hero-tag">
              <span class="tag-label">COLLECTION</span>
              <b>{{ collection.articleCount || articles.length }}</b>
              <span class="tag-sub">篇文章</span>
            </div>

            <div class="hero-info">
              <span class="eyebrow">COLLECT · 主题书单</span>
              <h1>{{ collection.name }}</h1>
              <p class="desc">{{ collection.description || '策划者还没有写说明。' }}</p>

              <div class="meta-row">
                <span class="curator">
                  <span class="curator-dot"></span>
                  策划 · {{ collection.author?.username || '匿名作者' }}
                </span>
                <span class="stat">{{ collection.articleCount || articles.length }} 篇文章</span>
              </div>

              <router-link to="/collections" class="back-link">← 回到合集列表</router-link>
            </div>
          </section>

          <div class="section-head">
            <h2>合集内容</h2>
            <span>{{ articles.length }} 篇</span>
          </div>

          <div v-if="articles.length" class="article-grid">
            <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
          </div>

          <div v-else class="empty-state">
            <div class="empty-books"><span></span><span></span><span></span></div>
            <h3>这份书单还是空的</h3>
            <p>策划者还没有把文章收进来，稍后再来看看。</p>
          </div>
        </template>

        <div v-else class="empty-state">
          <h3>合集不存在或已下架</h3>
          <router-link to="/collections" class="empty-action">返回合集列表 →</router-link>
        </div>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import ArticleCard from '@/components/ArticleCard.vue'
import { getCollection, getCollectionArticles } from '@/api/collection'

const route = useRoute()
const collection = ref(null)
const articles = ref([])
const loading = ref(true)

const load = async () => {
  loading.value = true
  try {
    const [detail, list] = await Promise.all([
      getCollection(route.params.id),
      getCollectionArticles(route.params.id, 0, 60)
    ])
    collection.value = detail
    articles.value = list?.list || []
  } catch (e) {
    collection.value = null
    articles.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
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

.detail-hero {
  position: relative;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 30px;
  padding: 28px;
  border-radius: 28px;
  border: 1px solid var(--content-line);
  background:
    radial-gradient(90% 120% at 0% 0%, rgba(26, 115, 232, 0.08), transparent 55%),
    radial-gradient(90% 120% at 100% 100%, rgba(236, 72, 153, 0.08), transparent 58%),
    var(--content-surface);
  box-shadow: var(--content-shadow);
}

.hero-tag {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 220px;
  border-radius: 20px;
  background:
    radial-gradient(120% 100% at 20% 0%, rgba(255, 255, 255, 0.22), transparent 55%),
    linear-gradient(140deg, #1a73e8 0%, #6f5cf6 55%, #b14bd8 100%);
  color: #fff;
  box-shadow: 0 18px 36px rgba(70, 80, 200, 0.28);

  /* 叠放的书页只露出底部边缘，不盖住封面内容 */
  &::before,
  &::after {
    content: '';
    position: absolute;
    bottom: -8px;
    height: 16px;
    border-radius: 0 0 16px 16px;
    background: var(--content-stack);
    border: 1px solid var(--content-line);
  }

  &::before {
    left: 12px;
    right: 12px;
  }

  &::after {
    bottom: -14px;
    left: 22px;
    right: 22px;
    height: 14px;
    opacity: 0.65;
  }

  b {
    position: relative;
    z-index: 1;
    font-size: 64px;
    font-weight: 700;
    line-height: 1;
    letter-spacing: -0.04em;
  }
}

.tag-label,
.tag-sub {
  position: relative;
  z-index: 1;
}

.tag-label {
  margin-bottom: 6px;
  font-size: 10px;
  letter-spacing: 0.28em;
  opacity: 0.8;
}

.tag-sub {
  font-size: 12px;
  opacity: 0.85;
}

.hero-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
}

.eyebrow {
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(124, 92, 255, 0.09);
  color: #6f5cf6;
  font-size: 11.5px;
  letter-spacing: 0.14em;
}

.hero-info h1 {
  margin: 14px 0 10px;
  font-size: clamp(24px, 3vw, 34px);
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.02em;
  color: var(--content-ink);
}

.desc {
  margin: 0 0 18px;
  max-width: 62ch;
  color: var(--content-ink-2);
  font-size: 14px;
  line-height: 1.8;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 18px;
  color: var(--content-ink-3);
  font-size: 12.5px;
}

.curator {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.curator-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a73e8, #b14bd8);
  box-shadow: 0 0 8px rgba(124, 92, 255, 0.5);
}

.stat {
  padding-left: 16px;
  border-left: 1px solid var(--color-divider);
}

.back-link {
  margin-top: 20px;
  color: var(--color-primary);
  font-size: 13px;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 36px 0 16px;

  h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: var(--content-ink);
  }

  span {
    color: var(--content-ink-3);
    font-size: 12.5px;
  }
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

/* 骨架 */
.detail-skeleton {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 30px;
  padding: 28px;
  border-radius: 28px;
  background: #fff;
}

.sk {
  display: block;
  border-radius: 10px;
  background: linear-gradient(90deg, var(--content-sk-a) 0%, var(--content-sk-b) 50%, var(--content-sk-a) 100%);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}

.sk-cover { height: 220px; border-radius: 20px; }
.sk-line { height: 13px; margin-bottom: 14px; }
.sk-lines { display: flex; flex-direction: column; justify-content: center; }

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
  color: var(--color-primary);
  font-size: 13.5px;
  font-weight: 500;
}

@media (max-width: 900px) {
  .detail-hero,
  .detail-skeleton {
    grid-template-columns: 1fr;
  }

  .hero-tag {
    min-height: 170px;
  }

  .article-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .detail-hero {
    padding: 18px;
    border-radius: 22px;
  }

  .stat {
    padding-left: 12px;
  }
}
</style>
