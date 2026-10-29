import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Welcome', component: () => import('@/views/Welcome.vue') },
  { path: '/home', name: 'Home', component: () => import('@/views/Home.vue') },
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { guestOnly: true } },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue'), meta: { guestOnly: true } },
  { path: '/articles', name: 'ArticleList', component: () => import('@/views/ArticleList.vue') },
  { path: '/articles/:id', name: 'ArticleDetail', component: () => import('@/views/ArticleDetail.vue') },
  { path: '/articles/:id/versions', name: 'ArticleVersions', component: () => import('@/views/ArticleVersions.vue'), meta: { requiresAuth: true } },
  { path: '/create', name: 'CreateArticle', component: () => import('@/views/CreateArticle.vue'), meta: { requiresAuth: true } },
  { path: '/edit/:id', name: 'EditArticle', component: () => import('@/views/CreateArticle.vue'), meta: { requiresAuth: true } },
  { path: '/profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { requiresAuth: true } },
  { path: '/user/:id', name: 'UserProfile', component: () => import('@/views/UserProfile.vue') },
  { path: '/creator/apply', name: 'CreatorApply', component: () => import('@/views/CreatorApply.vue'), meta: { requiresAuth: true } },
  { path: '/knowledge', name: 'Knowledge', component: () => import('@/views/Knowledge.vue'), meta: { requiresAuth: true } },
  { path: '/messages', name: 'Messages', component: () => import('@/views/Message.vue'), meta: { requiresAuth: true } },
  { path: '/about', name: 'About', component: () => import('@/views/About.vue') },
  { path: '/join-us', name: 'JoinUs', component: () => import('@/views/JoinUs.vue') },
  { path: '/contact', name: 'Contact', component: () => import('@/views/Contact.vue') },
  { path: '/guide', name: 'Guide', component: () => import('@/views/Guide.vue') },
  { path: '/faq', name: 'FAQ', component: () => import('@/views/FAQ.vue') },
  { path: '/feedback', name: 'Feedback', component: () => import('@/views/Feedback.vue') },
  { path: '/privacy', name: 'Privacy', component: () => import('@/views/Privacy.vue') },
  { path: '/terms', name: 'Terms', component: () => import('@/views/Terms.vue') },
  { path: '/columns', name: 'Columns', component: () => import('@/views/ColumnList.vue') },
  { path: '/column/:id', name: 'ColumnDetail', component: () => import('@/views/ColumnDetail.vue') },
  { path: '/collections', name: 'Collections', component: () => import('@/views/CollectionList.vue') },
  { path: '/collection/:id', name: 'CollectionDetail', component: () => import('@/views/CollectionDetail.vue') },
  { path: '/search', name: 'Search', component: () => import('@/views/Search.vue') },
  { path: '/knowledge-graph', name: 'KnowledgeGraph', component: () => import('@/views/KnowledgeGraph.vue'), meta: { requiresAuth: true } },
  { path: '/growth-center', name: 'GrowthCenter', component: () => import('@/views/GrowthCenter.vue'), meta: { requiresAuth: true } },
  { path: '/sign-in', name: 'SignIn', component: () => import('@/views/SignIn.vue'), meta: { requiresAuth: true } },
  { path: '/avatar-shop', name: 'AvatarShop', component: () => import('@/views/AvatarShop.vue'), meta: { requiresAuth: true } },
  { path: '/tree-hole', name: 'TreeHole', component: () => import('@/views/TreeHole.vue') },

  // ========== 运营后台 ==========
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { module: 'dashboard' } },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UserManage.vue'), meta: { module: 'user' } },
      { path: 'operations', name: 'AdminOperations', component: () => import('@/views/admin/OperationsManage.vue'), meta: { module: 'operations' } },
      { path: 'messages', name: 'AdminMessages', component: () => import('@/views/admin/AdminMessages.vue'), meta: { module: 'messages' } },
      { path: 'content', name: 'AdminContent', component: () => import('@/views/admin/ContentReview.vue'), meta: { module: 'content' } },
      { path: 'statistics', name: 'AdminStatistics', component: () => import('@/views/admin/Statistics.vue'), meta: { module: 'statistics' } },
      { path: 'recommend', name: 'AdminRecommend', component: () => import('@/views/admin/RecommendManage.vue'), meta: { module: 'content' } },
      { path: 'config', name: 'AdminConfig', component: () => import('@/views/admin/SystemConfig.vue'), meta: { module: 'system' } },
      { path: 'logs', name: 'AdminLogs', component: () => import('@/views/admin/OperationLog.vue'), meta: { module: 'system' } },
      { path: 'tree-hole', name: 'AdminTreeHole', component: () => import('@/views/admin/TreeHoleModeration.vue'), meta: { module: 'community' } },
      { path: 'feedback', name: 'AdminFeedback', component: () => import('@/views/admin/FeedbackManage.vue'), meta: { module: 'feedback' } },
      { path: 'agent', name: 'AdminAgent', component: () => import('@/views/admin/AdminAgent.vue'), meta: { module: 'system' } }
    ]
  },

  { path: '/:pathMatch(.*)*', name: 'Error', component: () => import('@/views/Error.vue'), meta: { code: 404 } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
})

router.beforeEach(async (to, from, next) => {
  // 登录态统一以 auth store 为准（token 现存于 sessionStorage，store 内含旧数据迁移）。
  // 此前这里直接读 localStorage.getItem('token')，而 persist 已不再写 localStorage
  // 且会清掉旧值，导致"页面显示已登录、requiresAuth 路由却全部跳登录页"。
  const { useAuthStore } = await import('@/store/auth')
  const authStore = useAuthStore()
  const isLoggedIn = authStore.isLoggedIn
  if (to.meta.requiresAuth && !isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.guestOnly && isLoggedIn) {
    next({ name: 'Home' })
  } else if (to.meta.requiresAdmin && isLoggedIn) {
    if (!authStore.isAdmin) {
      next({ name: 'Home' })
    } else if (to.meta.module && !authStore.canAccessModule(to.meta.module)) {
      // 职位无该模块权限 → 回到看板
      next({ name: 'AdminDashboard' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
