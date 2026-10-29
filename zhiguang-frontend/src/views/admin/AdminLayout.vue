<template>
  <div class="admin-layout">
    <!-- 顶部窄导航栏 -->
    <header class="admin-topbar">
      <div class="topbar-left">
        <router-link to="/admin" class="brand" aria-label="纸域空间运营中心">
          <span class="brand-mark">
            <img src="/brand/logo-light.png" alt="" aria-hidden="true" draggable="false" />
          </span>
          <span v-if="!isMobile" class="brand-text">
            <strong>纸域空间</strong><span>运营中心</span>
          </span>
        </router-link>

        <!-- 回到前台（原侧栏底部的入口） -->
        <router-link
          to="/home"
          class="icon-btn back-btn"
          title="回到前台"
          aria-label="回到前台"
        >
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5"/><path d="m12 19-7-7 7-7"/>
          </svg>
        </router-link>

        <el-breadcrumb v-if="!isMobile" separator="/" class="topbar-crumb">
          <el-breadcrumb-item :to="{ path: '/home' }">纸域空间平台</el-breadcrumb-item>
          <el-breadcrumb-item>运营中心</el-breadcrumb-item>
          <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="topbar-right">
        <el-select
          v-model="jumpTarget"
          filterable
          placeholder="搜索页面 / 功能"
          class="topbar-search"
          popper-class="admin-select-popper"
          @change="handleJump"
        >
          <template #prefix>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
              <circle cx="11" cy="11" r="7"/><path d="m20 20-3.2-3.2"/>
            </svg>
          </template>
          <el-option v-for="opt in flatMenu" :key="opt.path" :label="opt.label" :value="opt.path" />
        </el-select>

        <button class="icon-btn" @click="reloadPage" aria-label="刷新当前页">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12a9 9 0 1 1-3-6.7"/><path d="M21 4v5h-5"/>
          </svg>
        </button>

        <button
          v-if="authStore.canAccessModule('messages')"
          class="icon-btn bell-btn"
          @click="router.push('/admin/messages')"
          aria-label="后台消息提示"
        >
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8a6 6 0 1 0-12 0c0 6-2.5 7.5-2.5 7.5h17S18 14 18 8z"/>
            <path d="M13.7 20a2 2 0 0 1-3.4 0"/>
          </svg>
          <span v-if="reminderBadge > 0" class="bell-dot"></span>
        </button>

        <span class="topbar-divider"></span>

        <el-dropdown @command="handleCommand" trigger="click" popper-class="admin-dropdown-popper">
          <div class="admin-user">
            <span class="user-ring">
              <span class="user-avatar">
                <img
                  v-if="authStore.user?.avatar && !avatarBroken"
                  :src="authStore.user.avatar"
                  alt="管理员头像"
                  @error="avatarBroken = true"
                />
                <span v-else class="avatar-fallback">{{ avatarInitial }}</span>
              </span>
            </span>
            <span v-if="!isMobile" class="user-meta">
              <span class="user-name">{{ authStore.user?.username || '管理员' }}</span>
              <span class="user-role">{{ roleLabel }}</span>
            </span>
            <svg v-if="!isMobile" class="user-caret" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="m6 9 6 6 6-6"/></svg>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="agent">运营助手</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="admin-body-row">
      <!-- 主内容 -->
      <main class="admin-main">
        <router-view v-slot="{ Component }">
          <component :is="Component" :key="viewKey" />
        </router-view>
      </main>

      <!-- 底部玻璃停靠栏（Dock）：距离衰减放大，点击时主内容从其位置 FLIP 展开 -->
      <nav
        v-if="dockItems.length"
        ref="dockRef"
        class="admin-dock"
        aria-label="快捷模块"
        @pointermove="onDockMove"
        @pointerleave="resetDockScale"
      >
        <button
          v-for="item in dockItems"
          :key="item.path"
          type="button"
          class="dock-item"
          :class="{ 'is-active': isDockActive(item.path) }"
          :aria-label="item.label"
          @click="goFromDock(item, $event)"
        >
          <span class="dock-icon" v-html="item.icon"></span>
          <span v-if="item.tag" class="dock-badge" :aria-label="`待办 ${item.tag}`">{{ item.tag }}</span>
          <span class="dock-tip">{{ item.label }}</span>
        </button>
      </nav>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { getFeedbackStats, getReminderStats } from '@/api/admin'

import '@/styles/admin-theme.scss'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isMobile = ref(window.innerWidth <= 992)
const jumpTarget = ref('')
const viewKey = ref(route.fullPath)
const avatarBroken = ref(false)

const icon = (paths) =>
  `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">${paths}</svg>`

const ALL_MENU = [
  { path: '/admin', label: '运营仪表盘', module: 'dashboard', icon: icon('<path d="M3 3v18h18"/><path d="M7 15l4-5 3 3 5-7"/>') },
  { path: '/admin/statistics', label: '数据统计', module: 'statistics', icon: icon('<path d="M18 20V10M12 20V4M6 20v-6"/>') },
  { path: '/admin/content', label: '内容审核', module: 'content', tag: '待办', icon: icon('<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="m9 15 2 2 4-4"/>') },
  { path: '/admin/tree-hole', label: '树洞管理', module: 'community', icon: icon('<path d="M21 11.5a8.38 8.38 0 0 1-9 8.4 8.5 8.5 0 0 1-3.4-.7L3 21l1.8-4.6A8.38 8.38 0 0 1 3.5 11 8.5 8.5 0 0 1 12 3a8.38 8.38 0 0 1 9 8.5z"/>') },
  { path: '/admin/recommend', label: '内容推荐', module: 'content', icon: icon('<path d="m12 2 3.1 6.3 6.9 1-5 4.8 1.2 6.8L12 17.8 5.8 21l1.2-6.8-5-4.8 6.9-1z"/>') },
  { path: '/admin/users', label: '用户管理', module: 'user', icon: icon('<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.9"/>') },
  { path: '/admin/operations', label: '运营管理', module: 'operations', icon: icon('<path d="M12 2 4 6v6c0 5 3.4 8.6 8 10 4.6-1.4 8-5 8-10V6z"/><circle cx="12" cy="10" r="2.6"/><path d="M8.5 16.5a4 4 0 0 1 7 0"/>') },
  { path: '/admin/messages', label: '消息提示', module: 'messages', icon: icon('<path d="M18 8a6 6 0 1 0-12 0c0 6-2.5 7.5-2.5 7.5h17S18 14 18 8z"/><path d="M13.7 20a2 2 0 0 1-3.4 0"/>') },
  { path: '/admin/feedback', label: '建议反馈', module: 'feedback', icon: icon('<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><path d="M8 9h8M8 13h5"/>') },
  { path: '/admin/config', label: '系统配置', module: 'system', icon: icon('<circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.2 2.2M16.9 16.9l2.2 2.2M19.1 4.9l-2.2 2.2M7.1 16.9l-2.2 2.2"/>') },
  { path: '/admin/logs', label: '操作日志', module: 'system', icon: icon('<path d="M4 4h16v16H4z"/><path d="M8 9h8M8 13h8M8 17h4"/>') }
]

const feedbackPending = ref(0)
const reminderBadge = ref(0)

// 右侧导航只展示当前职位有权限的模块
const flatMenu = computed(() =>
  ALL_MENU.filter((item) => authStore.canAccessModule(item.module)).map((item) => {
    let tag = item.tag
    if (item.path === '/admin/feedback' && feedbackPending.value > 0) {
      tag = String(feedbackPending.value)
    }
    if (item.path === '/admin/messages' && reminderBadge.value > 0) {
      tag = String(reminderBadge.value)
    }
    return { ...item, tag }
  })
)

const pageTitle = computed(
  () => flatMenu.value.find((i) => i.path === route.path)?.label || '运营仪表盘'
)

const POSITION_LABELS = {
  SUPER: '超级管理员',
  OPERATION: '运营管理',
  CONTENT: '内容运营',
  COMMUNITY: '社区运营',
  USER: '用户运营',
  FEEDBACK: '反馈运营',
  SYSTEM: '系统运维',
  DATA: '数据分析'
}

const roleLabel = computed(() => POSITION_LABELS[authStore.adminPosition] || '管理员')

const avatarInitial = computed(() =>
  (authStore.user?.username || 'A').slice(0, 1).toUpperCase()
)

const handleJump = (path) => {
  if (path) router.push(path)
  jumpTarget.value = ''
}

const reloadPage = () => {
  viewKey.value = `${route.fullPath}?r=${Date.now()}`
}

const handleResize = () => {
  const mobile = window.innerWidth <= 992
  if (mobile !== isMobile.value) {
    isMobile.value = mobile
  }
}

const handleCommand = (command) => {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'agent') {
    // 运营助手（AI 管理员助手）入口，见《AI管理员助手-设计方案.md》
    router.push('/admin/agent')
  }
}

watch(() => route.fullPath, (p) => { viewKey.value = p })

const loadReminderBadge = async () => {
  try {
    const stats = await getReminderStats()
    reminderBadge.value = authStore.isSuperAdmin
      ? Number(stats?.sentCompleted) || 0
      : Number(stats?.unread) || 0
  } catch {
    reminderBadge.value = 0
  }
}

/* ===== 玻璃效果 6：底部 Dock（承载全部模块，替代原侧栏） ===== */
const dockRef = ref(null)
const dockItems = computed(() => flatMenu.value)
const isDockActive = (p) => (p === '/admin' ? route.path === '/admin' : route.path.startsWith(p))

function onDockMove(e) {
  const items = dockRef.value?.querySelectorAll('.dock-item')
  if (!items) return
  items.forEach((el) => {
    const r = el.getBoundingClientRect()
    const d = Math.abs(e.clientX - (r.left + r.width / 2))
    el.style.setProperty('--dock-scale', (1 + 0.5 * Math.max(0, 1 - d / 120)).toFixed(3))
  })
}

function resetDockScale() {
  dockRef.value?.querySelectorAll('.dock-item').forEach((el) => el.style.removeProperty('--dock-scale'))
}

/* ===== 玻璃效果 3：模块就地放大（FLIP） ===== */
const reduceMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches

async function goFromDock(item, ev) {
  if (item.path === route.path) return
  const from = ev.currentTarget.getBoundingClientRect()
  await router.push(item.path)
  if (reduceMotion()) return
  await nextTick()
  const main = document.querySelector('.admin-layout .admin-main')
  if (!main) return
  const last = main.getBoundingClientRect()
  if (!last.width || !last.height) return

  const sx = Math.max(0.08, from.width / last.width)
  const sy = Math.max(0.08, from.height / last.height)
  const dx = from.left + from.width / 2 - (last.left + last.width / 2)
  const dy = from.top + from.height / 2 - (last.top + last.height / 2)

  main.style.transition = 'none'
  main.style.transformOrigin = 'center center'
  main.style.transform = `translate(${dx}px, ${dy}px) scale(${sx}, ${sy})`
  main.style.opacity = '0'

  requestAnimationFrame(() => {
    main.style.transition = 'transform 420ms cubic-bezier(0.22,1,0.36,1), opacity 300ms ease'
    main.style.transform = 'none'
    main.style.opacity = '1'
    window.setTimeout(() => {
      main.style.transition = ''
      main.style.transform = ''
      main.style.transformOrigin = ''
      main.style.opacity = ''
    }, 460)
  })
}

/* ===== 玻璃效果 2：卡片光标跟随光斑，坐标写入 --mx/--my ===== */
let glowRaf = 0
let glowEl = null
let glowX = 0
let glowY = 0

function onGlowMove(e) {
  const el = e.target?.closest?.('.el-card')
  if (!el) return
  glowEl = el
  glowX = e.clientX
  glowY = e.clientY
  if (glowRaf) return
  glowRaf = requestAnimationFrame(() => {
    glowRaf = 0
    if (!glowEl) return
    const r = glowEl.getBoundingClientRect()
    glowEl.style.setProperty('--mx', Math.round(glowX - r.left) + 'px')
    glowEl.style.setProperty('--my', Math.round(glowY - r.top) + 'px')
  })
}

onMounted(async () => {
  window.addEventListener('resize', handleResize, { passive: true })
  window.addEventListener('light-note:admin-messages-read', loadReminderBadge)
  window.addEventListener('pointermove', onGlowMove, { passive: true })
  try {
    const stats = await getFeedbackStats()
    feedbackPending.value = Number(stats?.pending) || 0
  } catch {
    // 读不到统计不影响导航
  }
  loadReminderBadge()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('light-note:admin-messages-read', loadReminderBadge)
  window.removeEventListener('pointermove', onGlowMove)
  if (glowRaf) cancelAnimationFrame(glowRaf)
})
</script>

<style lang="scss" scoped>
@use "@/styles/admin-mixins.scss" as *;

.admin-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: var(--admin-float-gap);
  gap: var(--admin-float-gap);
  box-sizing: border-box;
}

/* ===== 顶部窄导航栏 ===== */
.admin-topbar {
  position: sticky;
  top: var(--admin-float-gap);
  z-index: 90;
  height: var(--admin-header-height);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--admin-space-4);
  padding: 0 var(--admin-space-3);
  @include admin-glass(0.78, 26px);
  border-radius: var(--admin-radius-xl);
  box-shadow: var(--admin-shadow-float), var(--admin-inset-highlight);
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  gap: var(--admin-space-2);
  min-width: 0;
}

.topbar-crumb {
  margin-left: var(--admin-space-2);
}

/* 品牌 */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-right: 6px;
  text-decoration: none;
  color: var(--admin-text);
  white-space: nowrap;
}

.brand-mark {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  box-shadow: 0 8px 18px rgba(47, 91, 212, 0.24);
  transition: transform var(--admin-transition-base), box-shadow var(--admin-transition-base);

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.brand:hover .brand-mark {
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 12px 24px rgba(47, 91, 212, 0.4);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;

  strong {
    font-size: var(--admin-text-base);
    font-weight: var(--admin-font-bold);
    letter-spacing: 0.02em;
  }

  span {
    font-size: var(--admin-text-xs);
    color: var(--admin-text-muted);
    font-weight: var(--admin-font-medium);
  }
}

/* 图标按钮 */
.icon-btn {
  position: relative;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--admin-radius-md);
  border: 1px solid transparent;
  background: transparent;
  color: var(--admin-text-secondary);
  cursor: pointer;
  text-decoration: none;
  flex-shrink: 0;
  transition: all var(--admin-transition-base);

  &:hover {
    background: rgba(255, 255, 255, 0.9);
    border-color: rgba(191, 211, 255, 0.9);
    color: var(--admin-primary-600);
    transform: translateY(-1px);
    box-shadow: var(--admin-shadow-sm);
  }

  &.is-active {
    background: var(--admin-primary-50);
    color: var(--admin-primary-600);
    border-color: var(--admin-primary-200);
  }
}

.bell-dot {
  position: absolute;
  top: 8px;
  right: 9px;
  width: 7px;
  height: 7px;
  border-radius: var(--admin-radius-full);
  background: var(--admin-error);
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.95), 0 0 8px rgba(244, 63, 94, 0.55);
}

.topbar-divider {
  width: 1px;
  height: 24px;
  background: var(--admin-border);
  margin: 0 var(--admin-space-1);
}

/* 搜索胶囊 */
.topbar-search {
  width: 240px;

  :deep(.el-input__wrapper) {
    height: 38px;
    border-radius: var(--admin-radius-full);
    background: rgba(241, 245, 249, 0.85);
    box-shadow: 0 0 0 1px rgba(226, 232, 240, 0.9) inset;
    padding-left: 14px;
  }

  :deep(.el-input__inner) {
    font-size: var(--admin-text-sm);
  }

  :deep(.el-input__prefix) {
    color: var(--admin-text-muted);
  }
}

/* 用户 */
.admin-user {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 44px;
  padding: 0 10px 0 4px;
  border-radius: var(--admin-radius-full);
  cursor: pointer;
  outline: none;
  border: 1px solid transparent;
  transition: all var(--admin-transition-base);

  &:hover {
    background: rgba(255, 255, 255, 0.92);
    border-color: rgba(191, 211, 255, 0.9);
    box-shadow: var(--admin-shadow-sm);
  }
}

.user-ring {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: var(--admin-radius-full);
  background: linear-gradient(135deg, rgba(121, 213, 223, 0.9), rgba(53, 179, 201, 0.9));
  padding: 2px;
  box-shadow: 0 4px 12px rgba(53, 179, 201, 0.28);
}

.user-avatar {
  width: 100%;
  height: 100%;
  border-radius: var(--admin-radius-full);
  overflow: hidden;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.avatar-fallback {
  font-size: var(--admin-text-base);
  font-weight: var(--admin-font-bold);
  color: var(--admin-primary-600);
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.user-name {
  font-size: var(--admin-text-sm);
  font-weight: var(--admin-font-semibold);
  color: var(--admin-text);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role {
  font-size: var(--admin-text-xs);
  color: var(--admin-text-muted);
}

.user-caret {
  color: var(--admin-text-muted);
}

/* ===== 主体 ===== */
.admin-body-row {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: var(--admin-float-gap);
}

/* ===== 玻璃效果 6：底部 Dock ===== */
.admin-dock {
  position: fixed;
  left: 50%;
  bottom: calc(var(--admin-float-gap) + 2px);
  z-index: 34;
  display: flex;
  align-items: flex-end;
  gap: 6px;
  padding: 8px 11px;
  transform: translateX(-50%);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.82);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(24px) saturate(140%);
  -webkit-backdrop-filter: blur(24px) saturate(140%);
  box-shadow:
    inset 1px 1px rgba(255, 255, 255, 0.9),
    inset -1px -1px rgba(140, 170, 220, 0.14),
    0 18px 44px -22px rgba(47, 91, 212, 0.55);
}

.dock-item {
  position: relative;
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: var(--admin-sidebar-text);
  cursor: pointer;
  transform: scale(var(--dock-scale, 1));
  transform-origin: bottom center;
  transition: transform 130ms ease-out, background-color var(--admin-transition-base),
    color var(--admin-transition-base);

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: radial-gradient(120px circle at 50% 120%, rgba(89, 139, 253, 0.22), transparent 70%);
    opacity: 0;
    transition: opacity var(--admin-transition-base);
    pointer-events: none;
  }

  &:hover {
    background: var(--admin-sidebar-hover);
    color: var(--admin-primary-600);

    &::after { opacity: 1; }
  }

  &.is-active {
    background: var(--admin-grad-primary);
    color: #fff;
    box-shadow: 0 8px 18px -8px rgba(47, 91, 212, 0.7);
  }
}

.dock-icon {
  display: flex;
  align-items: center;
  justify-content: center;

  :deep(svg) { display: block; width: 19px; height: 19px; }
}

.dock-tip {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 50%;
  transform: translateX(-50%) translateY(4px);
  padding: 4px 10px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.82);
  color: #fff;
  font-size: var(--admin-text-xs);
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.dock-item:hover .dock-tip {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

/* 待办角标：如内容审核的待审数 */
.dock-badge {
  position: absolute;
  top: 3px;
  right: 3px;
  min-width: 15px;
  height: 15px;
  padding: 0 4px;
  border-radius: 9999px;
  background: var(--admin-error);
  color: #fff;
  font-size: 9px;
  font-weight: var(--admin-font-semibold);
  line-height: 15px;
  text-align: center;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.9);
}

/* 主内容 */
.admin-main {
  flex: 1;
  min-width: 0;
  /* 底部 Dock 常驻，留出热区避免遮挡内容 */
  padding-bottom: 78px;
}

.mask-fade-enter-active,
.mask-fade-leave-active {
  transition: opacity 0.2s ease;
}

.mask-fade-enter-from,
.mask-fade-leave-to {
  opacity: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 992px) {
  .admin-layout {
    padding: var(--admin-space-3);
    gap: var(--admin-space-3);
  }

  /* 侧栏已移除，Dock 是唯一导航：窄屏改为整条横向滚动、图标收紧 */
  .admin-dock {
    left: var(--admin-space-3);
    right: var(--admin-space-3);
    transform: none;
    justify-content: flex-start;
    overflow-x: auto;
    padding: 7px 9px;
    gap: 4px;
    scrollbar-width: none;

    &::-webkit-scrollbar { display: none; }
  }

  .dock-item {
    flex: 0 0 auto;
    width: 38px;
    height: 38px;
  }

  .admin-main {
    padding-bottom: 74px;
  }

  .admin-topbar {
    top: var(--admin-space-3);
    padding: 0 var(--admin-space-2);
  }

  .topbar-search {
    width: 160px;
  }
}

@media (max-width: 640px) {
  .topbar-search,
  .topbar-divider {
    display: none;
  }

  .admin-main {
    padding-bottom: 70px;
  }
}
</style>
