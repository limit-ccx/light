<template>
  <div ref="previewRef" class="markdown-preview" v-html="renderedHtml"></div>
</template>

<script setup>
import { ref, watch, onMounted, nextTick } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import renderMathInElement from 'katex/contrib/auto-render'
import { useThemeStore } from '@/store/theme'
import lightHljsCss from 'highlight.js/styles/github.css?url'
import darkHljsCss from 'highlight.js/styles/github-dark.css?url'
import 'katex/dist/katex.min.css'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

const themeStore = useThemeStore()
const previewRef = ref(null)
const renderedHtml = ref('')

const escapeHtml = (text) => {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

const renderer = new marked.Renderer()

renderer.code = ({ text, lang }) => {
  const language = lang && hljs.getLanguage(lang) ? lang : 'plaintext'
  const highlighted = hljs.highlight(text, { language }).value
  return `<pre><code class="hljs language-${language}">${highlighted}</code></pre>\n`
}

renderer.codespan = ({ text }) => {
  // marked 默认 renderer 会对 text 转义，这里必须保持转义，否则构成 XSS 注入点
  return `<code>${escapeHtml(text)}</code>`
}

marked.use({
  renderer,
  breaks: true,
  gfm: true
})

const renderMarkdown = async () => {
  if (!props.content) {
    renderedHtml.value = ''
    return
  }
  try {
    // marked 默认透传原始 HTML，输出必须经 DOMPurify 净化后才能进 v-html
    const rawHtml = await marked.parse(props.content)
    renderedHtml.value = DOMPurify.sanitize(rawHtml, {
      ADD_ATTR: ['target']
    })
  } catch (error) {
    console.error('Markdown 渲染失败:', error)
    renderedHtml.value = `<p>${escapeHtml(props.content)}</p>`
  }
}

const renderMath = () => {
  if (!previewRef.value) return
  try {
    renderMathInElement(previewRef.value, {
      delimiters: [
        { left: '$$', right: '$$', display: true },
        { left: '$', right: '$', display: false }
      ],
      throwOnError: false,
      ignoredTags: ['script', 'noscript', 'style', 'textarea', 'pre', 'code']
    })
  } catch (error) {
    console.error('数学公式渲染失败:', error)
  }
}

const applyHljsTheme = () => {
  const id = 'markdown-preview-hljs-theme'
  let link = document.getElementById(id)
  if (!link) {
    link = document.createElement('link')
    link.id = id
    link.rel = 'stylesheet'
    document.head.appendChild(link)
  }
  link.href = themeStore.isDark() ? darkHljsCss : lightHljsCss
}

watch(() => props.content, async () => {
  await renderMarkdown()
  await nextTick()
  renderMath()
}, { immediate: true })

watch(() => themeStore.theme, () => {
  applyHljsTheme()
})

onMounted(() => {
  applyHljsTheme()
  renderMath()
})
</script>

<style lang="scss" scoped>
.markdown-preview {
  font-size: 16px;
  line-height: 1.85;
  color: var(--color-text);
  word-break: break-word;

  :deep(p) {
    margin-bottom: 16px;
    text-align: justify;
  }

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4),
  :deep(h5),
  :deep(h6) {
    margin: 28px 0 16px;
    color: var(--color-text);
    font-weight: 500;
    letter-spacing: -0.01em;
  }

  :deep(h1) { font-size: 28px; }
  :deep(h2) { font-size: 24px; }
  :deep(h3) { font-size: 20px; }
  :deep(h4) { font-size: 18px; }
  :deep(h5) { font-size: 16px; }
  :deep(h6) { font-size: 15px; }

  :deep(ul),
  :deep(ol) {
    padding-left: 24px;
    margin-bottom: 16px;
  }

  :deep(li) {
    margin-bottom: 8px;
  }

  :deep(a) {
    color: var(--color-primary);
    text-decoration: none;

    &:hover {
      color: var(--color-primary-light);
      text-decoration: underline;
    }
  }

  :deep(pre) {
    background: var(--color-bg-elevated);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    padding: 16px;
    overflow-x: auto;
    margin: 16px 0;
  }

  :deep(code) {
    background: var(--color-bg);
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Consolas', 'Courier New', monospace;
    font-size: 14px;
    color: var(--color-text);
  }

  :deep(pre code) {
    background: transparent;
    padding: 0;
    border-radius: 0;
    font-size: 14px;
  }

  :deep(blockquote) {
    border-left: 4px solid var(--color-primary);
    padding-left: 16px;
    margin: 16px 0;
    color: var(--color-text-secondary);
    font-style: italic;
  }

  :deep(table) {
    border-collapse: collapse;
    width: 100%;
    margin: 16px 0;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid var(--color-border);
    padding: 8px 12px;
  }

  :deep(th) {
    background: var(--color-bg-soft);
    font-weight: 500;
  }

  :deep(img) {
    max-width: 100%;
    border-radius: var(--radius-md);
    display: block;
  }

  :deep(hr) {
    border: none;
    border-top: 1px solid var(--color-divider);
    margin: 24px 0;
  }

  :deep(.katex) {
    color: var(--color-text);
  }

  :deep(.katex-display) {
    margin: 16px 0;
    overflow-x: auto;
    overflow-y: hidden;
  }
}
</style>
