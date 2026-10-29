<template>
  <div id="app">
    <router-view />

    <!-- 悬浮球助手 -->
    <FloatingBall
      v-if="showFloatingBall"
      :unread-count="floatingUnread"
      @menu-click="onFloatingMenuClick"
    />

    <!-- AI 导览助手对话抽屉 -->
    <AgentChatDrawer v-model="agentDrawerVisible" />

    <!-- 桌面桌宠 -->
    <DeskPet v-if="showDeskPet" />

    <!-- 心声管理 -->
    <InnerVoiceManager />

    <!-- 首次访问：书籍展开入场 -->
    <BookIntro v-if="introVisible" @reveal="handleIntroReveal" @done="finishIntro" />
  </div>
</template>

<script setup>
import { ref, computed, defineAsyncComponent, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FloatingBall from './components/FloatingBall.vue'
import AgentChatDrawer from './components/agent/AgentChatDrawer.vue'
import DeskPet from './components/DeskPet.vue'
import InnerVoiceManager from './components/innerVoice/InnerVoiceManager.vue'
import { useInnerVoiceStore } from './store/innerVoice'
import { useAuthStore } from './store/auth'
import { getUnreadMessageCount } from '@/api/message'

// three.js 入场体量较大，仅首次进入首页时才加载
const BookIntro = defineAsyncComponent(() => import('./components/BookIntro.vue'))

const INTRO_KEY = 'light-note:intro-seen-v1'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const innerVoiceStore = useInnerVoiceStore()
const agentDrawerVisible = ref(false)

/* 悬浮球未读数：没有未读消息时不显示角标 */
const floatingUnread = ref(0)
let unreadTimer = null

const refreshFloatingUnread = async () => {
  if (!authStore.isLoggedIn) {
    floatingUnread.value = 0
    return
  }
  try {
    const data = await getUnreadMessageCount()
    floatingUnread.value = Number(data?.count) || 0
  } catch {
    floatingUnread.value = 0
  }
}

onMounted(() => {
  refreshFloatingUnread()
  unreadTimer = window.setInterval(refreshFloatingUnread, 60000)
})

onUnmounted(() => {
  clearInterval(unreadTimer)
})

watch(
  () => [route.fullPath, authStore.isLoggedIn],
  () => refreshFloatingUnread()
)

const isAdminRoute = computed(() => route.path.startsWith('/admin'))

// 初始化时路由尚未解析，route.path 恒为 '/'，必须用真实地址判断，
// 否则直接打开 /login、/register 会被入场层盖住
const initialPath = window.location.pathname.replace(/\/+$/, '') || '/'

const isGuestRoute = computed(() =>
  route.meta.guestOnly === true || ['/login', '/register'].includes(initialPath)
)

// 树洞是沉浸式页面，悬浮助手与桌宠会压住星图和内容
const isImmersiveRoute = computed(() => route.path === '/tree-hole' || initialPath === '/tree-hole')
// 内容阅读页保留悬浮助手，但收起装饰桌宠，避免压住正文
const isContentRoute = computed(() => {
  const contentPattern = /^\/(articles|columns?|collections?)(\/|$)/
  return contentPattern.test(route.path) || contentPattern.test(initialPath)
})
// 知识库与星图是数据工作台，左侧目录/画布需要完整可见
const isWorkbenchRoute = computed(() => {
  const workbenchPattern = /^\/(knowledge|knowledge-graph|avatar-shop|messages)(\/|$)/
  return workbenchPattern.test(route.path) || workbenchPattern.test(initialPath)
})
// 个人中心是表单页，悬浮助手和桌宠会压住头像、标签页
const isProfileRoute = computed(() => {
  const profilePattern = /^\/profile(\/|$)/
  return profilePattern.test(route.path) || profilePattern.test(initialPath)
})
// 星云落地页是全屏动效页面，悬浮助手与桌宠会压住粒子、仪表台与形态轨
const isLandingRoute = computed(() => route.path === '/' || initialPath === '/')

const showFloatingBall = computed(
  () =>
    !isAdminRoute.value &&
    !isLandingRoute.value &&
    !isGuestRoute.value &&
    !isImmersiveRoute.value &&
    !isWorkbenchRoute.value &&
    !isProfileRoute.value
)
const showDeskPet = computed(
  () =>
    !isLandingRoute.value &&
    !isGuestRoute.value &&
    !isImmersiveRoute.value &&
    !isContentRoute.value &&
    !isWorkbenchRoute.value &&
    !isProfileRoute.value
)

// ?intro=1 可强制预览入场动画，且不会写入已看过标记
const forceIntro = new URLSearchParams(window.location.search).get('intro') === '1'

// 书本入场动画属于项目本身，因此挂在首页 /home 而不是星云落地页 /
const shouldPlayIntro = () =>
  initialPath === '/home' &&
  (forceIntro || !localStorage.getItem(INTRO_KEY)) &&
  !window.matchMedia('(prefers-reduced-motion: reduce)').matches

// 在 setup 阶段就挂载入场层，不等路由懒加载完成
const introVisible = ref(shouldPlayIntro())

if (introVisible.value) {
  document.documentElement.classList.add('intro-playing')
  document.body.style.overflow = 'hidden'
}

const hideCurtain = () => {
  const el = document.getElementById('boot-curtain')
  if (!el) return
  el.classList.add('is-hidden')
  setTimeout(() => el.remove(), 500)
}

if (!introVisible.value) {
  // 没有入场动画时，首帧渲染完即移除启动遮罩
  requestAnimationFrame(() => requestAnimationFrame(hideCurtain))
}

const handleIntroReveal = () => {
  document.documentElement.classList.remove('intro-playing')
  document.documentElement.classList.add('intro-revealing')
}

let introWatchdog = 0

const finishIntro = () => {
  clearTimeout(introWatchdog)
  if (!forceIntro) localStorage.setItem(INTRO_KEY, '1')
  introVisible.value = false
  document.documentElement.classList.remove('intro-playing', 'intro-revealing')
  document.body.style.overflow = ''
  hideCurtain()
}

// 入场层加载/渲染异常时兜底，避免首页一直被遮罩挡住
if (introVisible.value) {
  introWatchdog = window.setTimeout(finishIntro, 15000)
}

// 入场还没结束就跳去别的页面（如登录页）时立即收起
watch(
  () => route.path,
  (path) => {
    if (introVisible.value && path !== '/home') finishIntro()
  }
)

const FLOATING_ROUTES = {
  write: '/create',
  messages: '/messages',
  signin: '/sign-in',
  profile: '/profile'
}

const onFloatingMenuClick = (id) => {
  if (id === 'innerVoice') {
    innerVoiceStore.openManager()
    return
  }
  // AI 导览助手：仅登录用户可用（设计文档 §1），未登录引导登录
  if (id === 'agent') {
    if (!authStore.isLoggedIn) {
      router.push({ name: 'Login', query: { redirect: route.fullPath } })
      return
    }
    agentDrawerVisible.value = true
    return
  }
  const target = FLOATING_ROUTES[id]
  if (target && route.path !== target) router.push(target)
}
</script>

<style lang="scss">
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  width: 100%;
  min-height: 100vh;
  font-family: var(--font-family);
  background-color: var(--color-bg);
  color: var(--color-text);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

a {
  text-decoration: none;
  color: inherit;
}

ul, ol {
  list-style: none;
}

button {
  cursor: pointer;
  border: none;
  outline: none;
  background: none;
  font-family: inherit;
}

input, textarea {
  font-family: inherit;
  outline: none;
}

img {
  max-width: 100%;
  display: block;
}

/* ===== 选中文字高亮 ===== */
::selection {
  background: rgba(124, 107, 255, 0.3);
  color: var(--color-text);
}

/* ===== 全局微交互 ===== */

/* 按钮点击反馈 */
button:not(:disabled):active {
  transform: scale(0.97);
}

/* 链接下划线动画 */
a:not(.btn-primary):not(.btn-secondary):not(.btn-text) {
  position: relative;

  &::after {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 0;
    width: 0;
    height: 1px;
    background: linear-gradient(90deg, var(--color-primary), var(--color-secondary));
    transition: width var(--transition-normal);
  }

  &:hover::after {
    width: 100%;
  }
}

/* 输入框聚焦发光 */
input:focus,
textarea:focus {
  transition: all var(--transition-fast);
}

/* 卡片悬浮抬升效果 */
.card-hover {
  transition: all var(--transition-normal);

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-lg);
  }
}

/* 图片悬浮缩放 */
.img-hover {
  overflow: hidden;

  img {
    transition: transform var(--transition-slow);
  }

  &:hover img {
    transform: scale(1.05);
  }
}

/* 加载旋转动画 */
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.spin {
  animation: spin 0.8s linear infinite;
}

/* 脉冲发光 */
@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 0 5px rgba(124, 107, 255, 0.2);
  }
  50% {
    box-shadow: 0 0 20px rgba(124, 107, 255, 0.5);
  }
}

.pulse-glow {
  animation: pulse-glow 2s ease-in-out infinite;
}

/* 渐变文字 */
.gradient-text {
  background: linear-gradient(135deg, var(--color-primary-light), var(--color-secondary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 玻璃拟态面板 */
.glass-panel {
  @include liquid-glass;
  border-radius: var(--radius-lg);
}

/* ===== 入场动画与主页内容的衔接 ===== */
html.intro-playing {
  overflow: hidden;
}

html.intro-playing #app {
  opacity: 0;
}

html.intro-revealing #app {
  opacity: 1;
  transition: opacity 0.7s cubic-bezier(0.4, 0, 0.2, 1) 0.1s;
}

html.intro-playing .home-page .header,
html.intro-playing .home-page .hero-content,
html.intro-playing .home-page .content-section {
  opacity: 0;
}

html.intro-playing .home-page .header {
  transform: translateY(-10px);
}

html.intro-playing .home-page .hero-content,
html.intro-playing .home-page .content-section {
  transform: translateY(16px);
}

html.intro-revealing .home-page .header {
  opacity: 1;
  transform: none;
  transition: opacity 0.5s ease 0.08s, transform 0.6s cubic-bezier(0.22, 1, 0.36, 1) 0.08s;
}

html.intro-revealing .home-page .hero-content {
  opacity: 1;
  transform: none;
  transition: opacity 0.6s ease 0.2s, transform 0.75s cubic-bezier(0.22, 1, 0.36, 1) 0.2s;
}

html.intro-revealing .home-page .content-section {
  opacity: 1;
  transform: none;
  transition: opacity 0.6s ease 0.34s, transform 0.75s cubic-bezier(0.22, 1, 0.36, 1) 0.34s;
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
