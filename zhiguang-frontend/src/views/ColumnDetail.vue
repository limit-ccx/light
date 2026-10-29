<template>
  <div class="column-detail-page content-page">
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

        <template v-else-if="column">
          <section class="detail-hero">
            <div class="detail-cover" :class="`tone-${(column.id || 0) % 4}`">
              <img v-if="column.cover" :src="column.cover" alt="" @error="hideBrokenImage" />
              <span v-else class="cover-letter">{{ (column.name || '专')[0] }}</span>
              <span class="cover-shade"></span>
              <span class="cover-chip">专栏</span>
            </div>

            <div class="detail-info">
              <span class="eyebrow">COLUMN · 持续连载</span>
              <h1>{{ column.name }}</h1>
              <p class="desc">{{ column.description || '作者还没有写简介。' }}</p>

              <div class="meta-row">
                <span class="author">
                  <img :src="column.author?.avatar || defaultAvatar" alt="" @error="hideBrokenImage" />
                  <b>{{ column.author?.username || '匿名作者' }}</b>
                </span>
                <span class="stat">{{ column.articleCount || articles.length }} 篇文章</span>
                <span class="stat">{{ column.subscriberCount || 0 }} 人订阅</span>
              </div>

              <div class="actions">
                <button
                  type="button"
                  class="subscribe-btn"
                  :class="{ 'is-subscribed': column.isSubscribed }"
                  :disabled="subscribing"
                  @click="column.isSubscribed ? handleUnsubscribe() : handleSubscribe()"
                >
                  {{ subscribing ? '处理中…' : column.isSubscribed ? '已订阅' : '订阅专栏' }}
                </button>
                <span class="hint">{{ column.isSubscribed ? '有更新时，会在你的主页提醒你' : '订阅后，新的连载会出现在你的主页' }}</span>
              </div>
            </div>
          </section>

          <div class="section-head">
            <h2>专栏文章</h2>
            <span>{{ articles.length }} 篇</span>
          </div>

          <div v-if="articles.length" class="article-grid">
            <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
          </div>

          <div v-else class="empty-state">
            <div class="empty-art"><span></span><span></span><span></span></div>
            <h3>这个专栏还在酝酿</h3>
            <p>作者还没有把文章收进来，稍后再来看看。</p>
          </div>
        </template>

        <div v-else class="empty-state">
          <h3>专栏不存在或已下架</h3>
          <router-link to="/columns" class="empty-action">返回专栏列表 →</router-link>
        </div>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import ArticleCard from '@/components/ArticleCard.vue'
import { getColumn, getColumnArticles, subscribeColumn, unsubscribeColumn } from '@/api/column'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const column = ref(null)
const articles = ref([])
const loading = ref(true)
const subscribing = ref(false)

const hideBrokenImage = (event) => {
  event.target.style.display = 'none'
}

const load = async () => {
  loading.value = true
  try {
    const [detail, list] = await Promise.all([
      getColumn(route.params.id),
      getColumnArticles(route.params.id, 0, 60)
    ])
    column.value = detail
    articles.value = list?.list || []
  } catch (e) {
    column.value = null
    articles.value = []
  } finally {
    loading.value = false
  }
}

const requireLogin = () => {
  if (authStore.isLoggedIn) return true
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

const handleSubscribe = async () => {
  if (!requireLogin()) return
  subscribing.value = true
  try {
    await subscribeColumn(route.params.id)
    column.value.isSubscribed = true
    column.value.subscriberCount = (column.value.subscriberCount || 0) + 1
    ElMessage.success('订阅成功')
  } catch (e) {
    // 拦截器已提示
  } finally {
    subscribing.value = false
  }
}

const handleUnsubscribe = async () => {
  if (!requireLogin()) return
  subscribing.value = true
  try {
    await unsubscribeColumn(route.params.id)
    column.value.isSubscribed = false
    column.value.subscriberCount = Math.max(0, (column.value.subscriberCount || 1) - 1)
    ElMessage.success('已取消订阅')
  } catch (e) {
    // 拦截器已提示
  } finally {
    subscribing.value = false
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
    radial-gradient(70% 40% at 90% 0%, var(--content-glow-a), transparent 60%),
    radial-gradient(60% 40% at 4% 16%, var(--content-glow-b), transparent 62%),
    var(--color-bg);
}

.page-main {
  flex: 1;
  padding: calc(var(--header-height) + 28px) 0 56px;
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(240px, 340px) minmax(0, 1fr);
  gap: 30px;
  padding: 24px;
  border-radius: 28px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
}

.detail-cover {
  position: relative;
  min-height: 230px;
  overflow: hidden;
  border-radius: 20px;
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
  }
}

.cover-letter {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: rgba(255, 255, 255, 0.26);
  font-size: 92px;
  font-weight: 700;
}

.cover-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(10, 16, 32, 0.05) 0%, rgba(10, 16, 32, 0.45) 100%);
}

.cover-chip {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 5px 11px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  font-size: 11.5px;
  letter-spacing: 0.08em;
  backdrop-filter: blur(8px);
}

.detail-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding-right: 8px;
}

.eyebrow {
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(26, 115, 232, 0.07);
  color: #1a73e8;
  font-size: 11.5px;
  letter-spacing: 0.14em;
}

.detail-info h1 {
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

.author {
  display: inline-flex;
  align-items: center;
  gap: 8px;

  img {
    width: 26px;
    height: 26px;
    border-radius: 50%;
    object-fit: cover;
    border: 1px solid rgba(0, 0, 0, 0.05);
  }

  b {
    font-weight: 500;
    color: var(--content-ink-2);
  }
}

.stat {
  padding-left: 16px;
  border-left: 1px solid var(--color-divider);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 22px;
}

.subscribe-btn {
  height: 42px;
  padding: 0 26px;
  border-radius: 999px;
  border: none;
  background: linear-gradient(120deg, #1a73e8, #6f5cf6);
  color: #fff;
  font-family: inherit;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 12px 26px rgba(47, 99, 235, 0.28);
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 16px 32px rgba(47, 99, 235, 0.34);
  }

  &.is-subscribed {
    background: var(--color-google-blue-light);
    color: var(--color-primary);
    box-shadow: none;
    border: 1px solid var(--content-line-strong);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.hint {
  color: var(--content-ink-3);
  font-size: 12px;
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
  grid-template-columns: 340px 1fr;
  gap: 30px;
  padding: 24px;
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

.sk-cover { height: 230px; border-radius: 20px; }
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

.empty-art {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 54px;

  span {
    width: 16px;
    border-radius: 6px;
    background: linear-gradient(180deg, rgba(26, 115, 232, 0.24), rgba(168, 85, 247, 0.18));

    &:nth-child(1) { height: 30px; }
    &:nth-child(2) { height: 50px; }
    &:nth-child(3) { height: 40px; }
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

  .detail-cover {
    min-height: 200px;
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

  .actions {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .subscribe-btn {
    width: 100%;
    white-space: nowrap;
  }
}
</style>
