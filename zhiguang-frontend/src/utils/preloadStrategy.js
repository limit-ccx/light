/**
 * 路由预加载策略
 * <p>
 * 用于优化用户体验，在用户可能访问的页面之前预加载组件
 */
import { useAuthStore } from '@/store/auth'

// 预加载策略配置
const preloadStrategies = {
  // 用户登录后预加载的核心页面
  authPages: [
    'Profile',
    'CreateArticle',
    'Knowledge',
    'Messages',
    'GrowthCenter',
    'SignIn'
  ],
  
  // 高频访问页面
  highFrequencyPages: [
    'Home',
    'ArticleList',
    'Search',
    'Columns'
  ],
  
  // 次级页面（基于当前页面推测）
  predictivePages: {
    Home: ['ArticleList', 'Search'],
    ArticleList: ['ArticleDetail', 'CreateArticle'],
    ArticleDetail: ['ArticleVersions', 'Knowledge', 'Profile'],
    Search: ['ArticleDetail', 'KnowledgeGraph'],
    Columns: ['ColumnDetail'],
    Collections: ['CollectionDetail']
  }
}

/**
 * 预加载路由组件
 * @param {string} routeName - 路由名称
 */
export function preloadRoute(routeName) {
  // 动态导入路由组件
  const route = router.resolve({ name: routeName })
  if (route && route.matched.length > 0) {
    const component = route.matched[0].components.default
    if (typeof component === 'function') {
      component().catch(err => {
        console.warn(`Failed to preload route: ${routeName}`, err)
      })
    }
  }
}

/**
 * 批量预加载路由
 * @param {string[]} routeNames - 路由名称数组
 */
export function preloadRoutes(routeNames) {
  routeNames.forEach(routeName => {
    setTimeout(() => preloadRoute(routeName), 100)
  })
}

/**
 * 根据用户状态预加载页面
 */
export function preloadBasedOnAuth() {
  const authStore = useAuthStore()
  
  if (authStore.isLoggedIn) {
    // 登录用户预加载认证相关页面
    preloadRoutes(preloadStrategies.authPages)
  }
  
  // 所有用户预加载高频页面
  preloadRoutes(preloadStrategies.highFrequencyPages)
}

/**
 * 根据当前页面预加载相关页面
 * @param {string} currentRouteName - 当前路由名称
 */
export function preloadPredictive(currentRouteName) {
  const predictivePages = preloadStrategies.predictivePages[currentRouteName]
  
  if (predictivePages) {
    // 延迟预加载，避免阻塞当前页面
    setTimeout(() => preloadRoutes(predictivePages), 2000)
  }
}

/**
 * 初始化预加载策略
 */
export function initializePreloadStrategy(router) {
  // 应用加载完成后预加载高频页面
  window.addEventListener('load', () => {
    preloadBasedOnAuth()
  })
  
  // 路由变化后预加载相关页面
  router.afterEach((to) => {
    preloadPredictive(to.name)
  })
  
  // 用户空闲时预加载其他页面
  if ('requestIdleCallback' in window) {
    requestIdleCallback(() => {
      preloadRoutes(preloadStrategies.highFrequencyPages)
    })
  }
}

// 导出默认策略
export default {
  preloadRoute,
  preloadRoutes,
  preloadBasedOnAuth,
  preloadPredictive,
  initializePreloadStrategy
}