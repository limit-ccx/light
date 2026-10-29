import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const getPreferredTheme = () => {
    const savedTheme = localStorage.getItem('theme')
    if (savedTheme) return savedTheme
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }

  const theme = ref(getPreferredTheme())

  const isDark = () => theme.value === 'dark'

  const applyTheme = (t, options = {}) => {
    document.documentElement.setAttribute('data-theme', t)
    document.documentElement.style.colorScheme = t
    if (options.persist !== false) {
      localStorage.setItem('theme', t)
    }
    theme.value = t

    // 明暗两套 logo 同步到浏览器标签页图标
    const favicon = document.getElementById('app-favicon')
    if (favicon) {
      favicon.href = t === 'dark' ? '/brand/logo-dark.png' : '/brand/logo-light.png'
    }
  }

  const toggleTheme = () => {
    applyTheme(isDark() ? 'light' : 'dark')
  }

  applyTheme(theme.value, { persist: Boolean(localStorage.getItem('theme')) })

  if (window.matchMedia) {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaQuery.addEventListener('change', (e) => {
      if (!localStorage.getItem('theme')) {
        applyTheme(e.matches ? 'dark' : 'light', { persist: false })
      }
    })
  }

  return { theme, isDark, toggleTheme, applyTheme }
})
