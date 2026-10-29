import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 中文衬线字标必须自托管：Google Fonts 在部分网络下不可达，会静默退回系统宋体
import '@fontsource/noto-serif-sc/400.css'
import '@fontsource/noto-serif-sc/600.css'
import './utils/toastDedupe'
import App from './App.vue'
import router from './router'
import './styles/global.scss'
import './styles/content-pages.scss'
import './styles/element-plus.scss'
import './styles/tech-buttons.scss'
import { useThemeStore } from './store/theme'

const app = createApp(App)

app.use(createPinia())

// 初始化主题（必须在 mount 前调用，避免闪白）
const themeStore = useThemeStore()
themeStore.applyTheme(themeStore.theme)

app.use(router)
app.use(ElementPlus)

app.mount('#app')