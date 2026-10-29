<template>
  <div class="tree-hole-page">
    <div class="nh-ambient" aria-hidden="true">
      <span class="nh-orb orb-a"></span>
      <span class="nh-orb orb-b"></span>
      <span class="nh-orb orb-c"></span>
      <span class="nh-grain"></span>
    </div>

    <header class="nh-bar">
      <router-link class="nh-back" to="/home">
        <span v-html="icon.back"></span>
        <span class="nh-back-text">回站点</span>
      </router-link>
      <div class="nh-brand">
        <span class="nh-brand-mark" v-html="icon.moon"></span>
        <span>树洞世界</span>
      </div>
      <nav class="nh-bar-actions">
        <button type="button" class="nh-btn nh-btn-ghost" @click="showRules = true">
          <span v-html="icon.info"></span>
          <span class="nh-btn-text">温柔规则</span>
        </button>
        <button type="button" class="nh-btn nh-btn-primary" @click="showComposer = true">
          <span v-html="icon.write"></span>
          <span>写下心事</span>
        </button>
      </nav>
    </header>

    <main class="nh-main">
      <section class="nh-hero">
        <div class="nh-hero-copy">
          <span class="nh-badge">
            <span v-html="icon.moon"></span>
            月光未寄 · 无需登录
          </span>
          <h1 class="nh-title">把心事交给<em>夜晚</em></h1>
          <p class="nh-lede">没有目光，没有姓名。只有微风、星辰，和愿意倾听的陌生人。</p>
          <ul class="nh-facts">
            <li><span v-html="icon.incognito"></span>不收集账号、IP 与设备信息</li>
            <li><span v-html="icon.lock"></span>内容加密存储，只以随机代号示人</li>
            <li><span v-html="icon.shield"></span>自动审核加人工复核，随时可举报</li>
          </ul>
        </div>

        <aside class="nh-metrics">
          <div class="nh-metric">
            <b>{{ totalPosts }}</b>
            <span>封信</span>
          </div>
          <div class="nh-metric">
            <b :title="`最近 ${mapPosts.length} 封心事收到的拥抱`">{{ recentHugs }}</b>
            <span>个拥抱</span>
          </div>
          <div class="nh-metric">
            <b>∞</b>
            <span>匿名身份</span>
          </div>
        </aside>
      </section>

      <section class="nh-block">
        <div class="nh-block-head">
          <h2>情绪星图</h2>
          <p>最近 {{ mapPosts.length }} 封心事的分布，点一颗星直接读它</p>
        </div>
        <EmotionStarMap
          :posts="mapPosts"
          :active-emotion="currentEmotion"
          @select="openPostDetail"
          @filter="selectEmotion"
        />
      </section>

      <section class="nh-rail">
        <div class="nh-chips">
          <button
            v-for="tag in emotionTags"
            :key="tag.key"
            type="button"
            class="nh-chip"
            :class="{ 'is-active': currentEmotion === tag.key }"
            :style="chipStyle(tag)"
            @click="selectEmotion(tag.key)"
          >
            <span class="nh-chip-icon" v-html="emotionIcon(tag.key)"></span>
            <span>{{ tag.label }}</span>
            <em v-if="statCount(tag.key)">{{ statCount(tag.key) }}</em>
          </button>
        </div>
        <div class="nh-rail-side">
          <div class="nh-seg">
            <button
              type="button"
              :class="{ 'is-active': currentSort === 'recommend' }"
              @click="currentSort = 'recommend'"
            >
              <span v-html="icon.star"></span>推荐
            </button>
            <button
              type="button"
              :class="{ 'is-active': currentSort === 'latest' }"
              @click="currentSort = 'latest'"
            >
              <span v-html="icon.timer"></span>最新
            </button>
          </div>
          <span class="nh-rail-count">{{ totalPosts }} 封被夜风接过</span>
        </div>
      </section>

      <section class="nh-letters">
        <div v-if="loading" class="nh-masonry">
          <div v-for="n in 6" :key="n" class="nh-letter is-skeleton">
            <span class="sk sk-line" style="width: 42%"></span>
            <span class="sk sk-block"></span>
            <span class="sk sk-block" style="width: 70%"></span>
            <span class="sk sk-line" style="width: 55%"></span>
          </div>
        </div>

        <div v-else-if="loadError && posts.length === 0" class="nh-empty">
          <span class="nh-empty-icon" v-html="icon.cloudMoon"></span>
          <h3>夜雾太浓</h3>
          <p>暂时没有加载出来，请再试一次。</p>
          <button type="button" class="nh-btn nh-btn-ghost" @click="loadPosts(true)">
            <span v-html="icon.retry"></span>重新加载
          </button>
        </div>

        <div v-else-if="posts.length === 0" class="nh-empty">
          <span class="nh-empty-icon" v-html="icon.letter"></span>
          <h3>这里很安静</h3>
          <p>做第一个把秘密交给夜晚的人吧。</p>
          <button type="button" class="nh-btn nh-btn-primary" @click="showComposer = true">
            <span v-html="icon.write"></span>写下第一封信
          </button>
        </div>

        <template v-else>
          <div class="nh-masonry">
            <article
              v-for="post in posts"
              :key="post.id"
              class="nh-letter"
              :style="{ '--nh-accent': nightColor(post.emotionTag) }"
              @click="openPostDetail(post)"
            >
              <div class="nh-letter-top">
                <AnonAvatar :identity="post.anonIdentity" compact />
                <span class="nh-tag" :style="tagStyle(post.emotionTag)">
                  <span v-html="emotionIcon(post.emotionTag)"></span>
                  {{ emotionLabel(post.emotionTag) }}
                </span>
              </div>
              <div class="nh-letter-body">
                <ExpandableText :content="post.content" :max-length="170" />
              </div>
              <div class="nh-letter-foot">
                <time>{{ formatTime(post.createdAt) }}</time>
                <div class="nh-acts" @click.stop>
                  <button
                    type="button"
                    class="nh-act"
                    :class="{ 'is-on': post.userReacted?.like }"
                    @click="toggleReaction(post, 'LIKE')"
                  >
                    <span v-html="icon.heart"></span>
                    <b>{{ post.likeCount || 0 }}</b>
                  </button>
                  <button
                    type="button"
                    class="nh-act"
                    :class="{ 'is-on': post.userReacted?.empathy }"
                    @click="toggleReaction(post, 'EMPATHY')"
                  >
                    <span v-html="icon.hug"></span>
                    <b>{{ post.empathyCount || 0 }}</b>
                  </button>
                  <button type="button" class="nh-act" @click="openPostDetail(post)">
                    <span v-html="icon.comment"></span>
                    <b>{{ post.replyCount || 0 }}</b>
                  </button>
                </div>
              </div>
            </article>
          </div>

          <div class="nh-more">
            <button
              v-if="hasMore"
              type="button"
              class="nh-btn nh-btn-ghost"
              :disabled="loadingMore"
              @click="loadMore"
            >
              <span v-if="!loadingMore" v-html="icon.chevronDown"></span>
              <span>{{ loadingMore ? '读取中…' : '加载更多心事' }}</span>
            </button>
            <span v-else class="nh-end">夜风到这里就吹完了</span>
          </div>
        </template>
      </section>
    </main>

    <footer class="nh-foot">
      <span>树洞由匿名旅人共同守护 · 请温柔表达，别写下真实身份</span>
      <router-link to="/home">回到纸域空间首页</router-link>
    </footer>

    <!-- 心事详情 -->
    <NightOverlay v-model="showDetail" variant="panel">
      <template #head>
        <div v-if="currentPost" class="nh-detail-head">
          <span class="nh-tag" :style="tagStyle(currentPost.emotionTag)">
            <span v-html="emotionIcon(currentPost.emotionTag)"></span>
            {{ emotionLabel(currentPost.emotionTag) }}
          </span>
          <time>{{ formatTime(currentPost.createdAt) }}</time>
        </div>
      </template>

      <div v-if="currentPost" class="nh-detail">
        <div class="nh-detail-id">
          <AnonAvatar :identity="currentPost.anonIdentity" />
          <span class="nh-detail-note">这位旅人没有名字</span>
        </div>
        <p class="nh-detail-content">{{ currentPost.content }}</p>

        <div class="nh-detail-acts">
          <button
            type="button"
            class="nh-act nh-act-lg"
            :class="{ 'is-on': currentPost.userReacted?.like }"
            @click="toggleReaction(currentPost, 'LIKE')"
          >
            <span v-html="icon.heart"></span>{{ currentPost.likeCount || 0 }}
          </button>
          <button
            type="button"
            class="nh-act nh-act-lg"
            :class="{ 'is-on': currentPost.userReacted?.empathy }"
            @click="toggleReaction(currentPost, 'EMPATHY')"
          >
            <span v-html="icon.hug"></span>{{ currentPost.empathyCount || 0 }}
          </button>
          <span class="nh-flex-1"></span>
          <button type="button" class="nh-link-btn" @click="openReportDialog('POST', currentPost.id)">
            <span v-html="icon.flag"></span>举报
          </button>
          <button
            v-if="currentPost.isOwner"
            type="button"
            class="nh-link-btn nh-link-danger"
            @click="deleteCurrentPost"
          >
            <span v-html="icon.trash"></span>删除
          </button>
        </div>

        <div class="nh-divider"><span>温暖的回应 {{ replies.length }}</span></div>

        <div class="nh-replies">
          <p v-if="detailLoading" class="nh-quiet">夜风中，正在读取…</p>
          <template v-else>
            <AiWarmCard v-for="reply in aiReplies" :key="`ai-${reply.id}`" :content="reply.content" />
            <div v-for="reply in humanReplies" :key="reply.id" class="nh-reply">
              <AnonAvatar :identity="reply.anonIdentity" compact />
              <div class="nh-reply-bubble">
                <p>{{ reply.content }}</p>
                <div class="nh-reply-foot">
                  <time>{{ formatTime(reply.createdAt) }}</time>
                  <button type="button" class="nh-act nh-act-sm" @click="toggleReplyReaction(reply)">
                    <span v-html="icon.heart"></span>
                    <b>{{ reply.likeCount || 0 }}</b>
                  </button>
                  <button type="button" class="nh-link-btn" @click="openReportDialog('REPLY', reply.id)">
                    举报
                  </button>
                  <button
                    v-if="reply.isOwner"
                    type="button"
                    class="nh-link-btn nh-link-danger"
                    @click="deleteReply(reply)"
                  >
                    删除
                  </button>
                </div>
              </div>
            </div>
            <p v-if="replies.length === 0" class="nh-quiet">风中没有回声，你可以做第一个。</p>
          </template>
        </div>
      </div>

      <template #footer>
        <div class="nh-reply-form">
          <textarea
            v-model="replyForm.content"
            class="nh-textarea"
            rows="3"
            maxlength="1000"
            placeholder="给予一丝温暖…"
          ></textarea>
          <div class="nh-reply-form-foot">
            <span class="nh-counter">{{ replyForm.content.length }}/1000</span>
            <button
              type="button"
              class="nh-btn nh-btn-primary"
              :disabled="replySubmitting || !replyForm.content.trim()"
              @click="submitReply"
            >
              <span v-html="icon.send"></span>
              {{ replySubmitting ? '正在送出…' : '回应' }}
            </button>
          </div>
        </div>
      </template>
    </NightOverlay>

    <!-- 温柔规则 -->
    <NightOverlay v-model="showRules" variant="panel" title="匿名树洞使用规则">
      <div class="nh-rules">
        <section class="nh-rule-card">
          <h4><span class="nh-rule-icon" v-html="icon.incognito"></span>完全匿名</h4>
          <p>
            树洞只在你的浏览器里生成一段随机标识，用于防止刷屏与重复点赞。
            它与账号、IP、设备信息无关，我们也无法据此找到你。
          </p>
        </section>
        <section class="nh-rule-card">
          <h4><span class="nh-rule-icon" v-html="icon.write"></span>如何倾诉</h4>
          <ol>
            <li>点右上角「写下心事」</li>
            <li>选一个此刻的情绪</li>
            <li>写下 5–2000 字</li>
            <li>放飞，无需登录</li>
          </ol>
        </section>
        <section class="nh-rule-card">
          <h4><span class="nh-rule-icon" v-html="icon.hug"></span>温暖的互动</h4>
          <p>喜欢与拥抱都是无声的陪伴。回应时请带着善意，这里拒绝嘲讽、攻击与评判。</p>
        </section>
        <section class="nh-rule-card">
          <h4><span class="nh-rule-icon" v-html="icon.shield"></span>内容安全</h4>
          <p>辱骂、广告、违法内容会被自动拦截；高风险内容先进入待审。看到不适内容请点举报。</p>
        </section>
        <section class="nh-rule-card">
          <h4><span class="nh-rule-icon" v-html="icon.clock"></span>防滥用限制</h4>
          <ul>
            <li>每小时最多发布 5 封心事</li>
            <li>每小时最多回应 10 次</li>
            <li>每小时最多点赞 / 拥抱 30 次</li>
          </ul>
        </section>
      </div>
    </NightOverlay>

    <!-- 举报 -->
    <NightOverlay v-model="showReport" variant="modal" title="举报这封心事">
      <p class="nh-report-hint">帮助我们维持这个角落的温度，请选择最接近的原因。</p>
      <div class="nh-report-options">
        <label v-for="reason in reportReasons" :key="reason" class="nh-report-option">
          <input v-model="reportForm.reason" type="radio" name="report-reason" :value="reason" />
          <span>{{ reason }}</span>
        </label>
      </div>
      <template #footer>
        <div class="nh-dialog-actions">
          <button type="button" class="nh-btn nh-btn-ghost" @click="showReport = false">取消</button>
          <button
            type="button"
            class="nh-btn nh-btn-primary"
            :disabled="reportSubmitting || !reportForm.reason"
            @click="submitReport"
          >
            {{ reportSubmitting ? '提交中…' : '提交举报' }}
          </button>
        </div>
      </template>
    </NightOverlay>

    <!-- 沉浸书写 -->
    <NightOverlay v-model="showComposer" variant="fullscreen">
      <template #head>
        <div class="nh-composer-head">
          <h3>写下你的心事</h3>
          <p>此刻的情绪，决定它落在星图的哪一片光域</p>
        </div>
      </template>

      <div class="nh-composer">
        <div class="nh-emotion-grid">
          <button
            v-for="tag in postableEmotions"
            :key="tag.key"
            type="button"
            class="nh-emotion-tile"
            :class="{ 'is-active': postForm.emotionTag === tag.key }"
            :style="tileStyle(tag)"
            @click="postForm.emotionTag = tag.key"
          >
            <span v-html="emotionIcon(tag.key)"></span>
            <span>{{ tag.label }}</span>
          </button>
        </div>

        <textarea
          ref="composerInput"
          v-model="postForm.content"
          class="nh-composer-input"
          rows="9"
          maxlength="2000"
          placeholder="夜风会带走你的秘密…"
        ></textarea>

        <div class="nh-composer-meta">
          <span class="nh-note"><span v-html="icon.lock"></span>绝对匿名。没有追踪，只有倾听。</span>
          <span class="nh-counter">{{ postForm.content.length }}/2000</span>
        </div>

        <div v-if="myAnonIdentity" class="nh-identity">
          <AnonAvatar :identity="myAnonIdentity" />
          <span>本次的身份代号</span>
        </div>
      </div>

      <template #footer>
        <div class="nh-dialog-actions">
          <button type="button" class="nh-btn nh-btn-ghost" @click="showComposer = false">再想想</button>
          <button
            type="button"
            class="nh-btn nh-btn-primary"
            :disabled="submitting || postForm.content.trim().length < 5"
            @click="submitPost"
          >
            <span v-html="icon.send"></span>
            {{ submitting ? '正在放飞…' : '放飞心事' }}
          </button>
        </div>
      </template>
    </NightOverlay>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AnonAvatar from '@/components/AnonAvatar.vue'
import ExpandableText from '@/components/ExpandableText.vue'
import AiWarmCard from '@/components/AiWarmCard.vue'
import EmotionStarMap from '@/components/treeHole/EmotionStarMap.vue'
import NightOverlay from '@/components/treeHole/NightOverlay.vue'
import {
  emotionTags,
  getTreeHolePosts,
  createTreeHolePost,
  getTreeHolePostDetail,
  deleteTreeHolePost,
  createTreeHoleReply,
  deleteTreeHoleReply,
  toggleTreeHoleReaction,
  reportTreeHoleContent,
  getTreeHoleEmotionStats,
  ensureTreeHoleIdentity,
  getTreeHoleProfile
} from '@/api/treeHole'
import { NIGHT_ICONS as icon, emotionIcon, nightColor } from '@/data/treeHoleNight'

const PAGE_SIZE = 20
const MAP_SIZE = 60

const reportReasons = [
  '人身攻击或侮辱',
  '色情低俗内容',
  '暴力或危险行为',
  '广告骚扰或诈骗',
  '泄露个人隐私',
  '其他违规内容'
]

const posts = ref([])
const mapPosts = ref([])
const replies = ref([])
const currentPost = ref(null)
const totalPosts = ref(0)
const emotionStats = ref({})
const currentEmotion = ref('all')
const currentSort = ref('recommend')
const currentPage = ref(0)
const hasMore = ref(true)
const loading = ref(false)
const loadingMore = ref(false)
const detailLoading = ref(false)
const loadError = ref(false)
const submitting = ref(false)
const replySubmitting = ref(false)
const reportSubmitting = ref(false)

const showDetail = ref(false)
const showRules = ref(false)
const showReport = ref(false)
const showComposer = ref(false)

const postForm = ref({ content: '', emotionTag: 'other' })
const replyForm = ref({ content: '' })
const reportForm = ref({ targetType: 'POST', targetId: null, reason: reportReasons[0] })
const composerInput = ref(null)
const myAnonIdentity = ref(null)

let suppressSortWatch = false

// HTTP 错误已由 request 拦截器统一提示，这里只补业务码错误（HTTP 200 + code≠200）
const showError = (e, fallback) => {
  if (typeof e?.businessCode === 'number') ElMessage.error(e.message || fallback)
}

const labelMap = Object.fromEntries(emotionTags.map((t) => [t.key, t.label]))
const postableEmotions = computed(() => emotionTags.filter((t) => t.key !== 'all'))
const emotionLabel = (key) => labelMap[key] || '其他'
const statCount = (key) => (key === 'all' ? totalPosts.value : emotionStats.value[key] || 0)
const recentHugs = computed(() => mapPosts.value.reduce((sum, p) => sum + (p.empathyCount || 0), 0))
const aiReplies = computed(() => replies.value.filter((r) => r.isAiGenerated))
const humanReplies = computed(() => replies.value.filter((r) => !r.isAiGenerated))

const chipStyle = (tag) => {
  const color = nightColor(tag.key)
  if (currentEmotion.value !== tag.key) return { color, borderColor: `${color}33` }
  return {
    color,
    borderColor: color,
    backgroundColor: `${color}24`,
    boxShadow: `0 0 22px ${color}33`
  }
}

const tagStyle = (key) => {
  const color = nightColor(key)
  return { color, borderColor: `${color}44`, backgroundColor: `${color}18` }
}

const tileStyle = (tag) => {
  const color = nightColor(tag.key)
  return postForm.value.emotionTag === tag.key
    ? { color, borderColor: color, backgroundColor: `${color}1f`, boxShadow: `0 0 24px ${color}2e` }
    : { borderColor: `${color}2e`, color: `${color}cc` }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const diff = Math.floor((Date.now() - date.getTime()) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  if (diff < 604800) return `${Math.floor(diff / 86400)} 天前`
  return date.toLocaleDateString('zh-CN')
}

const loadMyAnonIdentity = () => {
  // 昵称/颜色改为服务端按身份哈希派生（随身份 token 下发并缓存），
  // 不再在前端用本地算法模拟（与服务端展示不一致，且依赖已废弃的旧明文 id）
  const profile = getTreeHoleProfile()
  if (!profile || !profile.displayName) return
  myAnonIdentity.value = {
    displayName: profile.displayName,
    color: profile.color || '#7C6BFF'
  }
}

const normalizePost = (p) => ({ ...p, userReacted: p.userReacted || { like: false, empathy: false } })

const loadPosts = async (reset = true) => {
  if (reset) {
    currentPage.value = 0
    hasMore.value = true
    posts.value = []
    loadError.value = false
  }
  loading.value = reset
  loadingMore.value = !reset
  try {
    const params = { sort: currentSort.value, page: currentPage.value, size: PAGE_SIZE }
    if (currentEmotion.value && currentEmotion.value !== 'all') params.emotion = currentEmotion.value
    const data = await getTreeHolePosts(params)
    const list = (data.posts || []).map(normalizePost)
    posts.value = reset ? list : posts.value.concat(list)
    totalPosts.value = data.total || 0
    hasMore.value = currentPage.value < (data.pages || 1) - 1
  } catch (e) {
    loadError.value = reset
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMapPosts = async () => {
  try {
    const data = await getTreeHolePosts({ sort: 'recommend', page: 0, size: MAP_SIZE })
    mapPosts.value = (data.posts || []).map(normalizePost)
  } catch (e) {
    mapPosts.value = []
  }
}

const loadEmotionStats = async () => {
  try {
    emotionStats.value = (await getTreeHoleEmotionStats()) || {}
  } catch (e) {
    emotionStats.value = {}
  }
}

const loadMore = () => {
  currentPage.value += 1
  loadPosts(false)
}

const selectEmotion = (key) => {
  if (currentEmotion.value === key) return
  currentEmotion.value = key
  loadPosts(true)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(currentSort, () => {
  if (suppressSortWatch) {
    suppressSortWatch = false
    return
  }
  loadPosts(true)
  window.scrollTo({ top: 0, behavior: 'smooth' })
})

watch(showComposer, async (open) => {
  if (!open) return
  await nextTick()
  composerInput.value?.focus()
})

const applyDetail = (data) => {
  const post = normalizePost(data.post)
  const ur = data.userReactions || {}
  post.userReacted = {
    like: !!ur.postLike || !!ur.like,
    empathy: !!ur.postEmpathy || !!ur.empathy
  }
  currentPost.value = post
  replies.value = (data.replies || []).sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
}

const openPostDetail = async (post) => {
  currentPost.value = post
  replies.value = []
  replyForm.value.content = ''
  showDetail.value = true
  detailLoading.value = true
  try {
    applyDetail(await getTreeHolePostDetail(post.id))
  } catch (e) {
    showError(e, '加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

const syncListItem = () => {
  if (!currentPost.value) return
  const id = currentPost.value.id
  const idx = posts.value.findIndex((p) => p.id === id)
  if (idx !== -1) posts.value[idx] = { ...posts.value[idx], ...currentPost.value }
  const mapIdx = mapPosts.value.findIndex((p) => p.id === id)
  if (mapIdx !== -1) mapPosts.value[mapIdx] = { ...mapPosts.value[mapIdx], ...currentPost.value }
}

const submitPost = async () => {
  const content = postForm.value.content.trim()
  if (content.length < 5) {
    ElMessage.warning('内容至少需要 5 个字符')
    return
  }
  submitting.value = true
  try {
    await createTreeHolePost({ content, emotionTag: postForm.value.emotionTag })
    ElMessage.success('心事已交给夜风')
    showComposer.value = false
    postForm.value = { content: '', emotionTag: 'other' }
    if (currentSort.value !== 'latest') {
      suppressSortWatch = true
      currentSort.value = 'latest'
    }
    currentEmotion.value = 'all'
    await Promise.all([loadPosts(true), loadMapPosts(), loadEmotionStats()])
  } catch (e) {
    showError(e, '发布失败')
  } finally {
    submitting.value = false
  }
}

const submitReply = async () => {
  const content = replyForm.value.content.trim()
  if (!content || !currentPost.value) return
  replySubmitting.value = true
  try {
    await createTreeHoleReply({ postId: currentPost.value.id, content })
    ElMessage.success('回应已送出')
    replyForm.value.content = ''
    applyDetail(await getTreeHolePostDetail(currentPost.value.id))
    syncListItem()
  } catch (e) {
    showError(e, '回复失败')
  } finally {
    replySubmitting.value = false
  }
}

const toggleReaction = async (post, type) => {
  const prev = { like: post.likeCount || 0, empathy: post.empathyCount || 0, reacted: { ...post.userReacted } }
  const field = type === 'LIKE' ? 'like' : 'empathy'
  const wasActive = !!post.userReacted?.[field]
  if (type === 'LIKE') {
    post.likeCount = prev.like + (wasActive ? -1 : 1)
  } else {
    post.empathyCount = prev.empathy + (wasActive ? -1 : 1)
  }
  post.userReacted = { ...post.userReacted, [field]: !wasActive }

  try {
    await toggleTreeHoleReaction({ targetType: 'POST', targetId: post.id, reactionType: type })
    syncListItem()
  } catch (e) {
    post.likeCount = prev.like
    post.empathyCount = prev.empathy
    post.userReacted = prev.reacted
    showError(e, '操作失败')
  }
}

const toggleReplyReaction = async (reply) => {
  const prev = reply.likeCount || 0
  try {
    const res = await toggleTreeHoleReaction({
      targetType: 'REPLY',
      targetId: reply.id,
      reactionType: 'LIKE'
    })
    reply.likeCount = res.active ? prev + 1 : prev - 1
  } catch (e) {
    reply.likeCount = prev
    showError(e, '操作失败')
  }
}

const openReportDialog = (targetType, targetId) => {
  reportForm.value = { targetType, targetId, reason: reportReasons[0] }
  showReport.value = true
}

const submitReport = async () => {
  if (!reportForm.value.reason) {
    ElMessage.warning('请选择举报原因')
    return
  }
  reportSubmitting.value = true
  try {
    await reportTreeHoleContent(reportForm.value)
    ElMessage.success('举报已提交，感谢你的监督')
    showReport.value = false
  } catch (e) {
    showError(e, '举报失败')
  } finally {
    reportSubmitting.value = false
  }
}

const confirmBox = (message) =>
  ElMessageBox.confirm(message, '提示', { type: 'warning' }).then(() => true).catch(() => false)

const deleteCurrentPost = async () => {
  if (!(await confirmBox('确定要删除这封心事吗？'))) return
  try {
    await deleteTreeHolePost(currentPost.value.id)
    ElMessage.success('已删除')
    showDetail.value = false
    await Promise.all([loadPosts(true), loadMapPosts()])
  } catch (e) {
    showError(e, '删除失败')
  }
}

const deleteReply = async (reply) => {
  if (!(await confirmBox('确定要删除这条回应吗？'))) return
  try {
    await deleteTreeHoleReply(reply.id)
    ElMessage.success('已删除')
    replies.value = replies.value.filter((r) => r.id !== reply.id)
    if (currentPost.value) {
      currentPost.value.replyCount = Math.max(0, (currentPost.value.replyCount || 0) - 1)
      syncListItem()
    }
  } catch (e) {
    showError(e, '删除失败')
  }
}

onMounted(async () => {
  // 先确保匿名身份（loadPosts 内部也会等待它），拿到服务端派生的昵称档案
  await ensureTreeHoleIdentity().catch(() => {})
  loadMyAnonIdentity()
  loadPosts(true)
  loadMapPosts()
  loadEmotionStats()
})
</script>

<style lang="scss" scoped>
$nh-text: #e6ecff;
$nh-text-soft: rgba(206, 220, 250, 0.78);
$nh-text-dim: rgba(170, 190, 235, 0.6);
$nh-line: rgba(148, 170, 220, 0.14);
$nh-surface: rgba(255, 255, 255, 0.045);
$nh-cobalt: #4f7df9;

.tree-hole-page {
  position: relative;
  min-height: 100vh;
  padding-bottom: 40px;
  background:
    radial-gradient(120% 70% at 50% -10%, rgba(58, 82, 176, 0.42), transparent 60%),
    radial-gradient(80% 60% at 8% 12%, rgba(120, 84, 200, 0.22), transparent 62%),
    linear-gradient(180deg, #080c1a 0%, #05070f 55%, #04060d 100%);
  color: $nh-text;
  --color-text: #{$nh-text};
  --color-text-secondary: #{$nh-text-soft};
  --color-text-muted: #{$nh-text-dim};
  --color-card: #101830;
}

.nh-ambient {
  position: fixed;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.nh-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.5;
  animation: nh-drift 22s ease-in-out infinite;
}

.orb-a { width: 40vw; height: 40vw; left: -8vw; top: 4vh; background: rgba(79, 125, 249, 0.3); }
.orb-b { width: 34vw; height: 34vw; right: -6vw; top: 26vh; background: rgba(160, 120, 255, 0.22); animation-delay: -7s; }
.orb-c { width: 30vw; height: 30vw; left: 26vw; bottom: -10vh; background: rgba(60, 200, 220, 0.14); animation-delay: -13s; }

.nh-grain {
  position: absolute;
  inset: 0;
  opacity: 0.05;
  background-image: radial-gradient(rgba(255, 255, 255, 0.6) 0.6px, transparent 0.6px);
  background-size: 3px 3px;
}

@keyframes nh-drift {
  0%, 100% { transform: translate3d(0, 0, 0); }
  50% { transform: translate3d(2.5vw, -3vh, 0); }
}

.nh-bar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px clamp(16px, 4vw, 48px);
  border-bottom: 1px solid $nh-line;
  background: rgba(6, 9, 20, 0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.nh-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid $nh-line;
  color: $nh-text-soft;
  font-size: 13px;
  transition: all 0.2s ease;

  &:hover {
    color: #fff;
    border-color: rgba(160, 190, 255, 0.45);
    background: rgba(90, 120, 220, 0.16);
  }
}

.nh-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: auto;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.14em;
}

.nh-brand-mark {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: #ffe6a8;
  background: rgba(255, 214, 130, 0.14);
  box-shadow: 0 0 18px rgba(255, 210, 120, 0.28);
}

.nh-bar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nh-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-family: inherit;
  font-size: 13px;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: all 0.22s ease;

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
}

.nh-btn-lg {
  height: 46px;
  padding: 0 24px;
  font-size: 14px;
}

.nh-btn-primary {
  color: #f4f7ff;
  background: linear-gradient(120deg, $nh-cobalt 0%, #6f5bf0 55%, #9b6cf5 100%);
  box-shadow: 0 12px 30px rgba(60, 80, 200, 0.4);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 16px 38px rgba(80, 100, 230, 0.5);
  }
}

.nh-btn-ghost {
  color: $nh-text-soft;
  border-color: $nh-line;
  background: $nh-surface;

  &:hover:not(:disabled) {
    color: #fff;
    border-color: rgba(160, 190, 255, 0.42);
    background: rgba(90, 120, 220, 0.16);
  }
}

.nh-main {
  position: relative;
  z-index: 1;
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 clamp(16px, 4vw, 32px);
}

.nh-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 28px;
  align-items: end;
  padding: clamp(36px, 6vw, 68px) 0 28px;
}

.nh-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 214, 130, 0.28);
  background: rgba(255, 214, 130, 0.08);
  color: #ffe1a6;
  font-size: 12px;
  letter-spacing: 0.12em;
}

.nh-title {
  margin: 18px 0 12px;
  font-size: clamp(30px, 5.4vw, 52px);
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.02em;

  em {
    font-style: normal;
    background: linear-gradient(110deg, #9ec1ff 0%, #c9b8ff 55%, #8fe3ff 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.nh-lede {
  max-width: 46ch;
  margin: 0 0 24px;
  color: $nh-text-soft;
  font-size: 15px;
  line-height: 1.9;
}

.nh-hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.nh-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 22px;
  margin: 26px 0 0;
  padding: 0;
  list-style: none;
  color: $nh-text-dim;
  font-size: 12.5px;

  li {
    display: flex;
    align-items: center;
    gap: 7px;
  }
}

.nh-metrics {
  display: flex;
  gap: 22px;
  padding: 20px 24px;
  border-radius: 20px;
  border: 1px solid $nh-line;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.02));
  backdrop-filter: blur(10px);
}

.nh-metric {
  display: flex;
  flex-direction: column;
  gap: 4px;

  b {
    font-size: 26px;
    font-weight: 600;
    line-height: 1;
    background: linear-gradient(120deg, #d8e6ff, #b9a6ff);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  span {
    color: $nh-text-dim;
    font-size: 12px;
    letter-spacing: 0.08em;
  }
}

.nh-block {
  margin-top: 12px;
}

.nh-block-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;

  h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 0.1em;
  }

  p {
    margin: 0;
    color: $nh-text-dim;
    font-size: 12px;
  }
}

.nh-rail {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin: 26px 0 18px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid $nh-line;
  background: rgba(255, 255, 255, 0.035);
  backdrop-filter: blur(8px);
}

.nh-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.nh-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.03);
  font-family: inherit;
  font-size: 12.5px;
  cursor: pointer;
  transition: all 0.2s ease;

  em {
    font-style: normal;
    font-size: 11px;
    opacity: 0.7;
  }

  &:hover {
    transform: translateY(-1px);
  }
}

.nh-chip-icon {
  display: inline-flex;
}

.nh-rail-side {
  display: flex;
  align-items: center;
  gap: 14px;
}

.nh-seg {
  display: inline-flex;
  padding: 3px;
  border-radius: 999px;
  border: 1px solid $nh-line;
  background: rgba(255, 255, 255, 0.04);

  button {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    height: 28px;
    padding: 0 14px;
    border: none;
    border-radius: 999px;
    background: transparent;
    color: $nh-text-dim;
    font-family: inherit;
    font-size: 12.5px;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .is-active {
    color: #fff;
    background: linear-gradient(120deg, rgba(79, 125, 249, 0.9), rgba(140, 100, 240, 0.9));
    box-shadow: 0 6px 18px rgba(70, 100, 220, 0.35);
  }
}

.nh-rail-count {
  color: $nh-text-dim;
  font-size: 12px;
  white-space: nowrap;
}

.nh-masonry {
  column-count: 3;
  column-gap: 16px;
}

.nh-letter {
  position: relative;
  break-inside: avoid;
  margin-bottom: 16px;
  padding: 18px 18px 14px;
  border-radius: 18px;
  border: 1px solid $nh-line;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.055), rgba(255, 255, 255, 0.022));
  backdrop-filter: blur(8px);
  cursor: pointer;
  transition: transform 0.24s ease, border-color 0.24s ease, box-shadow 0.24s ease;

  &:hover {
    transform: translateY(-3px);
    border-color: rgba(160, 190, 255, 0.34);
    box-shadow: 0 22px 44px rgba(3, 6, 16, 0.55);
  }
}

.nh-letter-top,
.nh-letter-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.nh-letter-body {
  margin: 14px 0 12px;
}

.nh-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid;
  font-size: 11.5px;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.nh-letter-foot {
  padding-top: 10px;
  border-top: 1px solid rgba(148, 170, 220, 0.1);

  time {
    color: $nh-text-dim;
    font-size: 11.5px;
  }
}

.nh-acts {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nh-act {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 9px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: transparent;
  color: $nh-text-dim;
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;

  b {
    font-weight: 500;
    font-variant-numeric: tabular-nums;
  }

  &:hover {
    color: #fff;
    background: rgba(255, 255, 255, 0.07);
  }

  &.is-on {
    color: #ffb3c8;
    background: rgba(255, 120, 160, 0.14);
  }
}

.nh-act-lg { height: 38px; padding: 0 16px; font-size: 13px; }
.nh-act-sm { height: 26px; padding: 0 8px; font-size: 11.5px; }

.nh-letter.is-skeleton {
  cursor: default;
  pointer-events: none;
}

.sk {
  display: block;
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.11) 50%, rgba(255, 255, 255, 0.05) 100%);
  background-size: 200% 100%;
  animation: sk-shimmer 1.5s ease-in-out infinite;
}

.sk-line { height: 12px; margin-bottom: 12px; }
.sk-block { height: 46px; margin-bottom: 12px; }

@keyframes sk-shimmer {
  from { background-position: 120% 0; }
  to { background-position: -20% 0; }
}

.nh-more {
  display: flex;
  justify-content: center;
  margin-top: 10px;
}

.nh-end {
  color: $nh-text-dim;
  font-size: 12.5px;
  letter-spacing: 0.1em;
}

.nh-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 56px 24px;
  border-radius: 22px;
  border: 1px dashed rgba(148, 170, 220, 0.24);
  background: rgba(255, 255, 255, 0.025);
  text-align: center;

  h3 { margin: 6px 0 0; font-size: 17px; }
  p { margin: 0; color: $nh-text-dim; font-size: 13px; }
}

.nh-empty-icon {
  display: grid;
  place-items: center;
  width: 84px;
  height: 84px;
  border-radius: 50%;
  color: rgba(180, 200, 255, 0.55);
  background: radial-gradient(circle, rgba(120, 150, 255, 0.16), transparent 70%);
}

.nh-detail-head {
  display: flex;
  align-items: center;
  gap: 12px;

  time {
    color: $nh-text-dim;
    font-size: 12px;
  }
}

.nh-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.nh-detail-id {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nh-detail-note {
  color: $nh-text-dim;
  font-size: 12px;
}

.nh-detail-content {
  margin: 0;
  font-size: 16px;
  line-height: 2;
  white-space: pre-wrap;
  word-break: break-word;
}

.nh-detail-acts {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 4px;
}

.nh-flex-1 { flex: 1; }

.nh-link-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 10px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: $nh-text-dim;
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    color: #fff;
    background: rgba(255, 255, 255, 0.06);
  }
}

.nh-link-danger:hover {
  color: #ff9fb0;
  background: rgba(255, 90, 120, 0.12);
}

.nh-divider {
  position: relative;
  height: 1px;
  background: linear-gradient(90deg, rgba(148, 170, 220, 0.24), transparent);

  span {
    position: absolute;
    top: -9px;
    left: 0;
    padding-right: 12px;
    background: #0b1124;
    color: $nh-text-dim;
    font-size: 12px;
    letter-spacing: 0.06em;
  }
}

.nh-replies {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.nh-reply {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.nh-reply-bubble {
  flex: 1;
  min-width: 0;
  padding: 12px 14px;
  border-radius: 4px 16px 16px 16px;
  border: 1px solid rgba(148, 170, 220, 0.12);
  background: rgba(255, 255, 255, 0.04);

  p {
    margin: 0 0 8px;
    font-size: 14px;
    line-height: 1.8;
    white-space: pre-wrap;
    word-break: break-word;
  }
}

.nh-reply-foot {
  display: flex;
  align-items: center;
  gap: 8px;

  time {
    margin-right: auto;
    color: $nh-text-dim;
    font-size: 11.5px;
  }
}

.nh-quiet {
  margin: 0;
  padding: 18px 0;
  color: $nh-text-dim;
  font-size: 13px;
  text-align: center;
}

.nh-reply-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nh-textarea,
.nh-composer-input {
  width: 100%;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(148, 170, 220, 0.18);
  background: rgba(6, 9, 20, 0.55);
  color: $nh-text;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.8;
  resize: vertical;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &::placeholder {
    color: rgba(160, 180, 225, 0.4);
  }

  &:focus {
    outline: none;
    border-color: rgba(150, 180, 255, 0.5);
    box-shadow: 0 0 0 3px rgba(90, 120, 220, 0.16);
  }
}

.nh-reply-form-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.nh-counter {
  color: $nh-text-dim;
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
}

.nh-rules {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.nh-rule-card {
  position: relative;
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid rgba(148, 170, 220, 0.16);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.055), rgba(255, 255, 255, 0.025));
  transition: border-color 0.2s ease, transform 0.2s ease;

  &:nth-child(1) { --rule-accent: #7ea4ff; }
  &:nth-child(2) { --rule-accent: #c9b8ff; }
  &:nth-child(3) { --rule-accent: #ff9fb8; }
  &:nth-child(4) { --rule-accent: #6ee7c8; }
  &:nth-child(5) { --rule-accent: #ffd38a; }

  &:hover {
    border-color: color-mix(in srgb, var(--rule-accent) 38%, transparent);
    transform: translateY(-1px);
  }

  h4 {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 0 0 10px;
    font-size: 14px;
    font-weight: 600;
    color: #e7eeff;
  }

  p {
    margin: 0;
    color: $nh-text-soft;
    font-size: 13.5px;
    line-height: 1.9;
  }

  li {
    color: $nh-text-soft;
    font-size: 13.5px;
    line-height: 1.9;
  }

  ol,
  ul {
    margin: 0;
    padding-left: 20px;
  }
}

.nh-rule-icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 10px;
  color: var(--rule-accent);
  background: color-mix(in srgb, var(--rule-accent) 14%, transparent);
  border: 1px solid color-mix(in srgb, var(--rule-accent) 26%, transparent);
}

.nh-report-hint {
  margin: 0 0 14px;
  color: $nh-text-soft;
  font-size: 13px;
  line-height: 1.8;
}

.nh-report-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nh-report-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.03);
  color: $nh-text-soft;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover {
    border-color: rgba(150, 180, 255, 0.3);
    color: #fff;
  }

  input {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
    appearance: none;
    -webkit-appearance: none;
    border-radius: 50%;
    border: 1.5px solid rgba(160, 185, 235, 0.45);
    background: transparent;
    display: grid;
    place-items: center;
    cursor: pointer;
    transition: border-color 0.18s ease, box-shadow 0.18s ease;

    &:checked {
      border-color: #7ea4ff;
      box-shadow: 0 0 12px rgba(126, 164, 255, 0.4);
    }

    &:checked::after {
      content: '';
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: #7ea4ff;
    }
  }
}

.nh-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.nh-composer-head {
  h3 {
    margin: 0 0 4px;
    font-size: 17px;
    font-weight: 600;
  }

  p {
    margin: 0;
    color: $nh-text-dim;
    font-size: 12.5px;
  }
}

.nh-composer {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.nh-emotion-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: 8px;
}

.nh-emotion-tile {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 42px;
  border-radius: 13px;
  border: 1px solid;
  background: rgba(255, 255, 255, 0.03);
  font-family: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-1px);
  }
}

.nh-composer-input {
  min-height: 220px;
  font-size: 15px;
  line-height: 2;
}

.nh-composer-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.nh-note {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: $nh-text-dim;
  font-size: 12px;
}

.nh-identity {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px dashed rgba(148, 170, 220, 0.22);
  background: rgba(255, 255, 255, 0.025);

  span {
    color: $nh-text-dim;
    font-size: 12px;
  }
}

.nh-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px 16px;
  margin-top: 46px;
  padding: 22px 16px 0;
  border-top: 1px solid $nh-line;
  color: $nh-text-dim;
  font-size: 12px;

  a {
    color: rgba(180, 205, 255, 0.8);
  }
}

@media (max-width: 1080px) {
  .nh-masonry { column-count: 2; }
}

@media (max-width: 860px) {
  .nh-hero {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .nh-metrics { justify-content: space-between; }
}

@media (max-width: 640px) {
  .nh-btn-text { display: none; }
  .nh-back-text { display: none; }
  .nh-back { padding: 7px 11px; }
  .nh-bar { gap: 10px; }
  .nh-btn { flex-shrink: 0; padding: 0 13px; white-space: nowrap; }
  .nh-brand { font-size: 14px; white-space: nowrap; }
  .nh-masonry { column-count: 1; }
  .nh-rail { padding: 12px; }
  .nh-rail-side { width: 100%; justify-content: space-between; }
  .nh-hero { padding-top: 28px; }
  .nh-bar { padding: 12px 16px; }
  .nh-brand { letter-spacing: 0.08em; }
}

@media (prefers-reduced-motion: reduce) {
  .nh-orb { animation: none; }
  .nh-letter,
  .sk { transition: none; animation: none; }
}
</style>
