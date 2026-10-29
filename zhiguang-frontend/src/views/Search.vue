<template>
  <InfoLayout
    eyebrow="Search"
    title="搜索与问答"
    description="在全部文章中找到答案，或让 AI 基于文章库直接回答你的问题。"
    :particles="false"
  >
    <div class="ln-panel search-panel">
      <div class="search-modes" role="tablist" aria-label="搜索模式">
        <button
          v-for="m in modes"
          :key="m.key"
          class="mode-btn"
          :class="{ active: mode === m.key }"
          role="tab"
          :aria-selected="mode === m.key"
          @click="switchMode(m.key)"
        >
          {{ m.label }}
        </button>
      </div>

      <div class="search-box">
        <div class="search-field">
          <span class="search-icon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <circle cx="11" cy="11" r="7"></circle>
              <line x1="20" y1="20" x2="16.2" y2="16.2"></line>
            </svg>
          </span>
          <input
            v-model="keyword"
            type="text"
            class="search-input"
            :placeholder="inputPlaceholder"
            @keydown.enter="doSearch()"
          />
        </div>
        <button class="search-btn" :disabled="loading || !keyword.trim()" @click="doSearch()">
          {{ mode === 'rag' ? '提问' : '搜索' }}
        </button>
      </div>
    </div>

    <!-- 普通搜索 / 语义搜索 结果 -->
    <section v-if="mode !== 'rag'" v-show="results.length > 0 || searched" class="search-result">
      <div class="result-head">
        <h2 class="result-title">
          {{ resultTitle }}
          <span v-if="searched" class="result-count">{{ loading ? '搜索中…' : `${total} 篇` }}</span>
        </h2>
        <span class="result-mode">{{ modeLabel }}</span>
      </div>

      <div v-if="loading && !results.length" class="result-skeleton">
        <span v-for="n in 3" :key="n"></span>
      </div>

      <div v-else-if="results.length === 0 && searched" class="ln-panel ln-empty">
        未找到相关文章，换个关键词试试
      </div>

      <div v-else class="result-list" :class="{ 'is-refreshing': loading }">
        <ArticleCard v-for="article in results" :key="article.id" :article="article" />
      </div>

      <div v-if="totalPages > 1" class="pager">
        <button class="pager-btn" :disabled="page <= 0 || loading" @click="goPage(page - 1)">上一页</button>
        <span class="pager-info">{{ page + 1 }} / {{ totalPages }}</span>
        <button class="pager-btn" :disabled="page >= totalPages - 1 || loading" @click="goPage(page + 1)">下一页</button>
      </div>
    </section>

    <!-- RAG 问答结果 -->
    <section v-else v-show="ragSearched" class="rag-result">
      <div v-if="ragLoading" class="ln-panel rag-loading">
        <span class="rag-spinner" aria-hidden="true"></span>
        AI 正在阅读文章并整理答案…
      </div>

      <template v-else>
        <article v-if="ragAnswer" class="rag-answer">
          <div class="rag-answer-head">
            <span class="rag-badge">AI 回答</span>
            <span class="rag-note">基于站内文章生成，请自行核对关键信息</span>
          </div>
          <div class="answer-content">{{ ragAnswer }}</div>
        </article>

        <div v-if="ragSources.length > 0" class="rag-sources">
          <h2 class="result-title">引用来源 <span class="result-count">{{ ragSources.length }} 篇</span></h2>
          <div class="result-list">
            <ArticleCard
              v-for="article in ragSources"
              :key="article.id"
              :article="article"
            />
          </div>
        </div>

        <div v-if="!ragAnswer && ragSources.length === 0" class="ln-panel ln-empty">
          未找到相关文章，暂时无法生成回答
        </div>
      </template>
    </section>
  </InfoLayout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import InfoLayout from '@/components/content/InfoLayout.vue'
import ArticleCard from '@/components/ArticleCard.vue'
import { search, vectorSearch, ragAsk } from '@/api/search'

const route = useRoute()
const router = useRouter()

const modes = [
  { key: 'normal', label: '普通搜索' },
  { key: 'semantic', label: '语义搜索' },
  { key: 'rag', label: 'RAG 问答' }
]

const mode = ref(route.query.mode || 'normal')
const keyword = ref(route.query.q || '')
const results = ref([])
const total = ref(0)
const page = ref(0)
const totalPages = ref(0)
const searched = ref(false)
const loading = ref(false)

const ragAnswer = ref('')
const ragSources = ref([])
const ragSearched = ref(false)
const ragLoading = ref(false)

const inputPlaceholder = computed(() => {
  if (mode.value === 'rag') return '输入问题，AI 将基于文章库生成回答...'
  if (mode.value === 'semantic') return '输入语义描述搜索相关文章...'
  return '输入关键词搜索文章...'
})

const resultTitle = computed(() => {
  if (mode.value === 'semantic') return '语义搜索结果'
  return '搜索结果'
})

const modeLabel = computed(() => modes.find((m) => m.key === mode.value)?.label || '')

let requestSeq = 0

const switchMode = (newMode) => {
  if (mode.value === newMode) return
  mode.value = newMode
  page.value = 0
  results.value = []
  total.value = 0
  totalPages.value = 0
  searched.value = false
  ragAnswer.value = ''
  ragSources.value = []
  ragSearched.value = false
  updateQuery()
}

const updateQuery = () => {
  router.replace({ query: { q: keyword.value, mode: mode.value } })
}

const goPage = (nextPage) => {
  page.value = nextPage
  doSearch(false)
}

const doSearch = async (reset = true) => {
  if (!keyword.value.trim()) return
  if (reset) {
    page.value = 0
    results.value = []
    total.value = 0
    totalPages.value = 0
  }
  updateQuery()

  if (mode.value === 'rag') {
    await doRag()
    return
  }

  const seq = ++requestSeq
  searched.value = true
  loading.value = true
  try {
    const res = mode.value === 'semantic'
      ? await vectorSearch(keyword.value, page.value)
      : await search(keyword.value, page.value)
    if (seq !== requestSeq) return
    results.value = res.list || []
    total.value = res.total || 0
    totalPages.value = res.totalPages || 0
  } catch (e) {
    if (seq !== requestSeq) return
    results.value = []
    total.value = 0
    totalPages.value = 0
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

const doRag = async () => {
  ragSearched.value = true
  ragLoading.value = true
  ragAnswer.value = ''
  ragSources.value = []
  try {
    const res = await ragAsk(keyword.value)
    ragAnswer.value = res.answer || ''
    ragSources.value = res.sources || []
  } catch (e) {
    ragAnswer.value = ''
    ragSources.value = []
  } finally {
    ragLoading.value = false
  }
}

if (keyword.value) doSearch()
</script>

<style lang="scss" scoped>
.search-panel {
  padding: 18px;
}

.search-modes {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border-radius: 999px;
  border: 1px solid var(--content-line);
  background: var(--content-tabs);
}

.mode-btn {
  height: 34px;
  padding: 0 18px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--content-ink-2);
  font-family: inherit;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    color: var(--color-primary);
  }

  &.active {
    color: #fff;
    background: linear-gradient(120deg, #1a73e8, #5b6cf5 60%, #8b5cf6);
    box-shadow: 0 8px 20px rgba(47, 99, 235, 0.28);
  }
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
}

.search-field {
  position: relative;
  flex: 1;
  min-width: 0;
  display: flex;
}

.search-icon {
  position: absolute;
  left: 18px;
  top: 50%;
  transform: translateY(-50%);
  display: inline-flex;
  color: var(--content-ink-3);
  pointer-events: none;
}

.search-input {
  width: 100%;
  min-width: 0;
  height: 52px;
  padding: 0 18px 0 48px;
  border-radius: 14px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-surface-tint);
  color: var(--content-ink);
  font-family: inherit;
  font-size: 15px;
  outline: none;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);

  &::placeholder {
    color: var(--content-ink-3);
  }

  &:focus {
    border-color: var(--content-line-strong);
    box-shadow: 0 0 0 4px var(--content-glow-a);
  }
}

.search-btn {
  height: 52px;
  padding: 0 30px;
  border: none;
  border-radius: 14px;
  color: #fff;
  font-family: inherit;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(120deg, #1a73e8, #5b6cf5 60%, #8b5cf6);
  box-shadow: 0 10px 24px rgba(47, 99, 235, 0.28);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast), opacity var(--transition-fast);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 14px 30px rgba(47, 99, 235, 0.34);
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
    box-shadow: none;
  }
}

.search-result,
.rag-result {
  margin-top: 32px;
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.result-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin: 0;
  color: var(--content-ink);
  font-size: 19px;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.result-count {
  color: var(--content-ink-3);
  font-size: 13px;
  font-weight: 400;
}

.result-mode {
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--content-ink-3);
  font-size: 12px;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: opacity var(--transition-fast);

  &.is-refreshing {
    opacity: 0.45;
    pointer-events: none;
  }
}

.result-skeleton {
  display: flex;
  flex-direction: column;
  gap: 16px;

  span {
    display: block;
    height: 132px;
    border-radius: 20px;
    background: linear-gradient(100deg, var(--content-sk-a) 28%, var(--content-sk-b) 50%, var(--content-sk-a) 72%);
    background-size: 220% 100%;
    animation: shimmer 1.4s ease-in-out infinite;
  }
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 26px;
}

.pager-btn {
  height: 38px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--content-ink-2);
  font-family: inherit;
  font-size: 13.5px;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover:not(:disabled) {
    border-color: var(--content-line-strong);
    color: var(--color-primary);
  }

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
}

.pager-info {
  color: var(--content-ink-3);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.rag-loading {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 28px;
  color: var(--content-ink-2);
  font-size: 14px;
}

.rag-spinner {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid var(--content-line);
  border-top-color: var(--color-primary);
  animation: ragSpin 0.8s linear infinite;
}

@keyframes ragSpin {
  to {
    transform: rotate(360deg);
  }
}

.rag-answer {
  position: relative;
  padding: clamp(22px, 3vw, 32px);
  border-radius: 24px;
  border: 1px solid transparent;
  background:
    linear-gradient(var(--content-surface), var(--content-surface)) padding-box,
    linear-gradient(120deg, rgba(26, 115, 232, 0.5), rgba(139, 92, 246, 0.42), rgba(236, 72, 153, 0.4)) border-box;
  box-shadow: var(--content-shadow);
}

.rag-answer-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.rag-badge {
  padding: 4px 12px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  background: linear-gradient(120deg, #1a73e8, #8b5cf6);
}

.rag-note {
  color: var(--content-ink-3);
  font-size: 12px;
}

.answer-content {
  color: var(--content-ink);
  font-size: 15px;
  line-height: 1.9;
  white-space: pre-wrap;
}

.rag-sources {
  margin-top: 28px;

  .result-title {
    margin-bottom: 16px;
  }
}

@media (max-width: 560px) {
  .search-panel {
    padding: 14px;
  }

  .mode-btn {
    flex: 1;
    padding: 0 10px;
  }

  .search-modes {
    display: flex;
    width: 100%;
  }

  .search-box {
    flex-wrap: wrap;
  }

  .search-field {
    flex-basis: 100%;
  }

  .search-btn {
    width: 100%;
  }
}
</style>
