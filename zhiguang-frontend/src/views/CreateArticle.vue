<template>
  <div class="create-article-page">
    <Header />

    <main class="main-content">
      <div class="container">
        <div class="page-header">
          <h1 class="page-title">{{ isEditing ? '编辑文章' : '写文章' }}</h1>
          <p class="page-subtitle">用文字记录灵感，让思想发光</p>
        </div>

        <div class="editor-container">
          <div class="editor-main">
            <input
              v-model="form.title"
              type="text"
              class="title-input"
              placeholder="请输入文章标题..."
            />

            <div class="editor-toolbar">
              <div class="toolbar-group">
                <button type="button" class="toolbar-btn" @click="formatBold" title="加粗">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/>
                    <path d="M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/>
                  </svg>
                </button>
                <button type="button" class="toolbar-btn" @click="formatItalic" title="斜体">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="19" y1="4" x2="10" y2="4"/>
                    <line x1="14" y1="20" x2="5" y2="20"/>
                    <line x1="15" y1="4" x2="9" y2="20"/>
                  </svg>
                </button>
                <button type="button" class="toolbar-btn" @click="formatHeading" title="标题">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M4 12h16M4 4v16M20 4v16"/>
                  </svg>
                </button>
              </div>

              <div class="toolbar-group">
                <button type="button" class="toolbar-btn" @click="formatQuote" title="引用">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V21z"/>
                  </svg>
                </button>
                <button type="button" class="toolbar-btn" @click="formatCode" title="代码">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="16 18 22 12 16 6"/>
                    <polyline points="8 6 2 12 8 18"/>
                  </svg>
                </button>
                <button type="button" class="toolbar-btn" @click="formatLink" title="链接">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
                    <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
                  </svg>
                </button>
                <button type="button" class="toolbar-btn" @click="triggerCoverUpload" title="封面图片">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                    <circle cx="8.5" cy="8.5" r="1.5"/>
                    <polyline points="21 15 16 10 5 21"/>
                  </svg>
                </button>
              </div>

              <div class="toolbar-divider"></div>

              <!-- AI 润色按钮组 -->
              <div class="ai-polish-group" v-click-outside="closeAiMenu">
                <button
                  type="button"
                  class="toolbar-btn ai-btn"
                  :class="{ loading: aiPolishing }"
                  @click="toggleAiMenu"
                  :disabled="aiPolishing"
                  title="AI 润色"
                >
                  <span v-if="aiPolishing" class="ai-spinner">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
                      <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                    </svg>
                  </span>
                  <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                    <path d="M2 17l10 5 10-5"/>
                    <path d="M2 12l10 5 10-5"/>
                  </svg>
                  <span class="ai-btn-text">{{ aiPolishing ? '润色中...' : 'AI 润色' }}</span>
                  <svg v-if="!aiPolishing" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-left:2px">
                    <polyline points="6 9 12 15 18 9"/>
                  </svg>
                </button>

                <!-- 下拉菜单 -->
                <Transition name="dropdown">
                  <div v-if="showAiMenu" class="ai-dropdown">
                    <div class="ai-dropdown-header">
                      <span>选择润色模式</span>
                    </div>
                    <button
                      v-for="mode in aiModes"
                      :key="mode.key"
                      type="button"
                      class="ai-dropdown-item"
                      @click="triggerPolish(mode.key)"
                    >
                      <div class="ai-mode-icon-wrapper" :style="{ background: mode.gradient }">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path v-if="mode.key === 'polish'" d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                          <path v-if="mode.key === 'polish'" d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                          <path v-if="mode.key === 'formal'" d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                          <polyline v-if="mode.key === 'formal'" points="14 2 14 8 20 8"/>
                          <circle v-if="mode.key === 'casual'" cx="12" cy="12" r="10"/>
                          <path v-if="mode.key === 'casual'" d="M8 14s1.5 2 4 2 4-2 4-2"/>
                          <line v-if="mode.key === 'casual'" x1="9" y1="9" x2="9.01" y2="9"/>
                          <line v-if="mode.key === 'casual'" x1="15" y1="9" x2="15.01" y2="9"/>
                          <path v-if="mode.key === 'concise'" d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                          <polyline v-if="mode.key === 'concise'" points="14 2 14 8 20 8"/>
                          <line v-if="mode.key === 'concise'" x1="16" y1="13" x2="8" y2="13"/>
                          <line v-if="mode.key === 'concise'" x1="16" y1="17" x2="8" y2="17"/>
                          <polyline v-if="mode.key === 'concise'" points="10 9 9 9 8 9"/>
                          <path v-if="mode.key === 'expand'" d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                          <path v-if="mode.key === 'expand'" d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                          <line v-if="mode.key === 'expand'" x1="18" y1="2" x2="22" y2="6"/>
                        </svg>
                      </div>
                      <div class="ai-mode-info">
                        <div class="ai-mode-name">{{ mode.name }}</div>
                        <div class="ai-mode-desc">{{ mode.desc }}</div>
                      </div>
                    </button>
                  </div>
                </Transition>
              </div>
            </div>

            <textarea
              ref="editorRef"
              v-model="form.content"
              class="content-editor"
              placeholder="写下你的想法..."
            ></textarea>

            <div class="preview-section">
              <div class="preview-header">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
                <span>实时预览</span>
              </div>
              <div class="preview-content">
                <MarkdownPreview :content="form.content" />
              </div>
            </div>
          </div>

          <aside class="editor-sidebar">
            <div class="sidebar-card ai-assistant-card">
              <h4 class="sidebar-title">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                  <path d="M2 17l10 5 10-5"/>
                  <path d="M2 12l10 5 10-5"/>
                </svg>
                AI 写作助手
              </h4>

              <div class="ai-assistant-actions">
                <button
                  type="button"
                  class="ai-assistant-btn ai-assist-btn"
                  :class="{ loading: aiLoading.assist }"
                  :disabled="aiLoading.assist"
                  @click="handleAiAssist"
                >
                  <span v-if="aiLoading.assist" class="ai-spinner">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
                      <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                    </svg>
                  </span>
                  <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
                  </svg>
                  <span>{{ aiLoading.assist ? '生成中...' : '一键生成（标题+摘要+标签）' }}</span>
                </button>

                <button
                  type="button"
                  class="ai-assistant-btn"
                  :class="{ loading: aiLoading.titles }"
                  :disabled="aiLoading.titles"
                  @click="handleGenerateTitles"
                >
                  <span v-if="aiLoading.titles" class="ai-spinner">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
                      <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                    </svg>
                  </span>
                  <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M4 12h16M4 4v16M20 4v16"/>
                  </svg>
                  <span>{{ aiLoading.titles ? '生成中...' : '生成标题' }}</span>
                </button>

                <button
                  type="button"
                  class="ai-assistant-btn"
                  :class="{ loading: aiLoading.summary }"
                  :disabled="aiLoading.summary"
                  @click="handleGenerateSummary"
                >
                  <span v-if="aiLoading.summary" class="ai-spinner">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
                      <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                    </svg>
                  </span>
                  <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                    <line x1="16" y1="13" x2="8" y2="13"/>
                    <line x1="16" y1="17" x2="8" y2="17"/>
                    <polyline points="10 9 9 9 8 9"/>
                  </svg>
                  <span>{{ aiLoading.summary ? '生成中...' : '生成摘要' }}</span>
                </button>

                <button
                  type="button"
                  class="ai-assistant-btn"
                  :class="{ loading: aiLoading.tags }"
                  :disabled="aiLoading.tags"
                  @click="handleGenerateTags"
                >
                  <span v-if="aiLoading.tags" class="ai-spinner">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
                      <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                    </svg>
                  </span>
                  <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
                    <line x1="7" y1="7" x2="7.01" y2="7"/>
                  </svg>
                  <span>{{ aiLoading.tags ? '生成中...' : '生成标签' }}</span>
                </button>

                <button
                  type="button"
                  class="ai-assistant-btn"
                  :class="{ loading: aiLoading.structure }"
                  :disabled="aiLoading.structure"
                  @click="handleOptimizeStructure"
                >
                  <span v-if="aiLoading.structure" class="ai-spinner">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
                      <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                    </svg>
                  </span>
                  <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="8" y1="6" x2="21" y2="6"/>
                    <line x1="8" y1="12" x2="21" y2="12"/>
                    <line x1="8" y1="18" x2="21" y2="18"/>
                    <line x1="3" y1="6" x2="3.01" y2="6"/>
                    <line x1="3" y1="12" x2="3.01" y2="12"/>
                    <line x1="3" y1="18" x2="3.01" y2="18"/>
                  </svg>
                  <span>{{ aiLoading.structure ? '优化中...' : '优化结构' }}</span>
                </button>

                <button
                  type="button"
                  class="ai-assistant-btn"
                  :class="{ loading: aiLoading.correct }"
                  :disabled="aiLoading.correct"
                  @click="handleAutoCorrect"
                >
                  <span v-if="aiLoading.correct" class="ai-spinner">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
                      <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                    </svg>
                  </span>
                  <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 11l3 3L22 4"/>
                    <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
                  </svg>
                  <span>{{ aiLoading.correct ? '纠错中...' : '自动纠错' }}</span>
                </button>
              </div>

              <button
                v-if="preAiContent !== null"
                type="button"
                class="ai-undo-btn"
                @click="undoAiAccept"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 7v6h6"/>
                  <path d="M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6 2.3L3 13"/>
                </svg>
                撤销上次 AI 修改
              </button>

              <Transition name="dropdown">
                <div v-if="generatedTitleCandidates.length" class="title-candidates">
                  <div class="candidates-header">点击选择一个标题</div>
                  <button
                    v-for="(title, index) in generatedTitleCandidates"
                    :key="index"
                    type="button"
                    class="title-candidate-item"
                    @click="applyTitle(title)"
                  >
                    {{ title }}
                  </button>
                </div>
              </Transition>
            </div>

            <div class="sidebar-card">
              <h4 class="sidebar-title">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="3"/>
                  <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
                </svg>
                文章设置
              </h4>

              <div class="form-group">
                <label class="form-label">标签</label>
                <div class="tags-input">
                  <TransitionGroup name="tag">
                    <span v-for="tag in form.tags" :key="tag" class="tag">
                      {{ tag }}
                      <button type="button" class="tag-remove" @click="removeTag(tag)">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                          <line x1="18" y1="6" x2="6" y2="18"/>
                          <line x1="6" y1="6" x2="18" y2="18"/>
                        </svg>
                      </button>
                    </span>
                  </TransitionGroup>
                  <input
                    v-model="tagInput"
                    type="text"
                    class="tag-input-field"
                    placeholder="添加标签后回车"
                    @keydown.enter.prevent="addTag"
                  />
                </div>
                <p class="form-hint">按 Enter 添加标签，最多5个</p>
              </div>

              <div class="form-group">
                <label class="form-label">封面图片</label>
                <div class="cover-upload" @click="triggerCoverUpload">
                  <img v-if="form.cover" :src="form.cover" alt="封面" class="cover-preview" />
                  <div v-else class="cover-placeholder">
                    <div class="cover-icon">
                      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                        <circle cx="8.5" cy="8.5" r="1.5"/>
                        <polyline points="21 15 16 10 5 21"/>
                      </svg>
                    </div>
                    <span>点击上传封面</span>
                  </div>
                </div>
                <input ref="coverInputRef" type="file" accept="image/*" hidden @change="handleCoverChange" />
              </div>

              <div class="form-group">
                <label class="form-label">摘要</label>
                <textarea
                  v-model="form.summary"
                  class="summary-input"
                  placeholder="简要描述文章内容..."
                  rows="3"
                ></textarea>
              </div>
            </div>

            <div class="action-buttons">
              <button type="button" class="btn-secondary" @click="saveDraft" :disabled="saving || !form.title.trim()">
                <svg v-if="saving" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
                  <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                </svg>
                <span v-else>保存草稿</span>
              </button>
              <button type="button" class="btn-primary" @click="publishArticle" :disabled="publishing || !canPublish">
                <svg v-if="publishing" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
                  <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                </svg>
                <span v-else>发布文章</span>
              </button>
            </div>
          </aside>
        </div>
      </div>
    </main>

    <!-- AI 润色对比弹窗（结构优化/自动纠错仅展示建议，不提供"采纳替换"） -->
    <AiPolishDialog
      v-model="aiDialogVisible"
      :original-content="aiOriginalContent"
      :polished-content="aiPolishedContent"
      :mode="aiCurrentMode"
      :model="aiModelName"
      :advice-only="['structure', 'correct'].includes(aiCurrentMode)"
      :generating="aiPolishing"
      :error-text="aiStreamError"
      @accept="handleAiAccept"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import Header from '@/components/Header.vue'
import AiPolishDialog from '@/components/AiPolishDialog.vue'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { polishContent, polishContentStream, aiAssist, generateTitles, generateSummary, generateTags, optimizeStructure, autoCorrect } from '@/api/ai'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const editorRef = ref(null)
const coverInputRef = ref(null)
const saving = ref(false)
const publishing = ref(false)
const tagInput = ref('')
const articleId = ref(null)

// ===== AI 润色相关状态 =====
const aiPolishing = ref(false)
const showAiMenu = ref(false)
const aiDialogVisible = ref(false)
const aiOriginalContent = ref('')
const aiPolishedContent = ref('')
const aiCurrentMode = ref('polish')
const aiModelName = ref('')
const aiStreamError = ref('')
/** 采纳前的编辑器快照，用于"撤销上次 AI 修改" */
const preAiContent = ref(null)
/** 当前流式请求的中止函数 */
let abortPolish = null

// ===== AI 写作助手相关状态 =====
const aiLoading = reactive({
  assist: false,
  titles: false,
  summary: false,
  tags: false,
  structure: false,
  correct: false
})
const generatedTitleCandidates = ref([])

const aiModes = [
  { key: 'polish',  name: '智能润色', desc: '提升流畅性和文学性', gradient: 'linear-gradient(135deg, #7C6BFF, #6366F1)' },
  { key: 'formal',  name: '正式化',   desc: '改写为正式书面语风格', gradient: 'linear-gradient(135deg, #3B82F6, #1D4ED8)' },
  { key: 'casual',  name: '轻松化',   desc: '改写为轻松口语化风格', gradient: 'linear-gradient(135deg, #F59E0B, #D97706)' },
  { key: 'concise', name: '精简提炼', desc: '去冗余，保留核心信息', gradient: 'linear-gradient(135deg, #10B981, #059669)' },
  { key: 'expand',  name: '扩写丰富', desc: '增加细节，内容更饱满', gradient: 'linear-gradient(135deg, #EC4899, #DB2777)' }
]

// 点击外部关闭下拉菜单
const vClickOutside = {
  mounted(el, binding) {
    el._clickOutsideHandler = (event) => {
      if (!el.contains(event.target)) binding.value()
    }
    document.addEventListener('click', el._clickOutsideHandler)
  },
  unmounted(el) {
    document.removeEventListener('click', el._clickOutsideHandler)
  }
}

const toggleAiMenu = () => {
  showAiMenu.value = !showAiMenu.value
}

const closeAiMenu = () => {
  showAiMenu.value = false
}

const triggerPolish = (mode) => {
  showAiMenu.value = false

  if (!form.content.trim()) {
    ElMessage.warning('请先输入文章内容')
    return
  }

  // 流式润色：立即打开对比弹窗，右侧打字机式增量渲染
  aiPolishing.value = true
  aiCurrentMode.value = mode
  aiOriginalContent.value = form.content
  aiPolishedContent.value = ''
  aiStreamError.value = ''
  aiModelName.value = ''
  aiDialogVisible.value = true

  abortPolish = polishContentStream(
    { content: form.content, mode, title: form.title || undefined },
    {
      onDelta: (text) => {
        aiPolishedContent.value += text
      },
      onDone: (info) => {
        aiPolishing.value = false
        aiModelName.value = info?.model || ''
        if (info?.truncated) {
          ElMessage.warning('AI 输出因长度限制被截断，请检查结果完整性')
        }
      },
      onError: (message) => {
        // 流式失败时回退到同步接口重试一次
        fallbackPolish(mode, message)
      }
    }
  )
}

// 关闭弹窗时中断进行中的流式请求
watch(aiDialogVisible, (visible) => {
  if (!visible && abortPolish) {
    abortPolish()
    abortPolish = null
    aiPolishing.value = false
  }
})

const fallbackPolish = async (mode, streamMessage) => {
  try {
    const result = await polishContent({
      content: form.content,
      mode,
      title: form.title || undefined
    })
    aiPolishedContent.value = result.polishedContent
    aiModelName.value = result.model || ''
    aiStreamError.value = ''
  } catch (error) {
    aiStreamError.value = error?.message || streamMessage || 'AI 润色失败，请稍后重试'
  } finally {
    aiPolishing.value = false
  }
}

const handleAiAssist = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请先输入文章内容')
    return
  }

  aiLoading.assist = true
  try {
    const result = await aiAssist({
      content: form.content,
      title: form.title || undefined
    })
    if (result?.titles?.length) {
      generatedTitleCandidates.value = result.titles.slice(0, 5)
    }
    if (result?.summary) {
      form.summary = result.summary
    }
    if (result?.tags?.length) {
      const merged = [...form.tags, ...result.tags]
        .filter((tag, index, arr) => arr.indexOf(tag) === index)
        .slice(0, 5)
      form.tags = merged
    }
    ElMessage.success('AI 一键生成完成')
  } catch (error) {
    ElMessage.error(error?.message || '一键生成失败，请稍后重试')
  } finally {
    aiLoading.assist = false
  }
}

const handleAiAccept = (polishedText) => {
  preAiContent.value = form.content
  form.content = polishedText
  ElMessage.success('已采纳润色结果')
}

const undoAiAccept = () => {
  if (preAiContent.value === null) return
  form.content = preAiContent.value
  preAiContent.value = null
  ElMessage.success('已撤销 AI 修改')
}
// ===== AI 润色结束 =====

// ===== AI 写作助手功能 =====
const handleGenerateTitles = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请先输入文章内容')
    return
  }

  aiLoading.titles = true
  generatedTitleCandidates.value = []

  try {
    const result = await generateTitles({
      content: form.content,
      title: form.title || undefined
    })
    const titles = result.titles || result

    if (Array.isArray(titles) && titles.length) {
      generatedTitleCandidates.value = titles.slice(0, 5)
    } else {
      ElMessage.warning('未生成到合适的标题')
    }
  } catch (error) {
    ElMessage.error(error?.message || '生成标题失败，请稍后重试')
  } finally {
    aiLoading.titles = false
  }
}

const applyTitle = (title) => {
  form.title = title
  generatedTitleCandidates.value = []
  ElMessage.success('已填入标题')
}

const handleGenerateSummary = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请先输入文章内容')
    return
  }

  aiLoading.summary = true

  try {
    const result = await generateSummary({
      content: form.content,
      title: form.title || undefined
    })
    form.summary = result.summary || result
    ElMessage.success('摘要生成成功')
  } catch (error) {
    ElMessage.error(error?.message || '生成摘要失败，请稍后重试')
  } finally {
    aiLoading.summary = false
  }
}

const handleGenerateTags = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请先输入文章内容')
    return
  }

  aiLoading.tags = true

  try {
    const result = await generateTags({
      content: form.content,
      title: form.title || undefined
    })
    const newTags = result.tags || result

    if (Array.isArray(newTags) && newTags.length) {
      const merged = [...form.tags, ...newTags]
        .filter((tag, index, arr) => arr.indexOf(tag) === index)
        .slice(0, 5)
      form.tags = merged
      ElMessage.success('标签生成成功')
    } else {
      ElMessage.warning('未生成到合适的标签')
    }
  } catch (error) {
    ElMessage.error(error?.message || '生成标签失败，请稍后重试')
  } finally {
    aiLoading.tags = false
  }
}

const handleOptimizeStructure = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请先输入文章内容')
    return
  }

  aiLoading.structure = true

  try {
    const result = await optimizeStructure({
      content: form.content,
      title: form.title || undefined
    })

    aiOriginalContent.value = result.originalContent
    aiPolishedContent.value = result.polishedContent
    aiModelName.value = result.model || ''
    aiCurrentMode.value = 'structure'
    aiDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '优化结构失败，请稍后重试')
  } finally {
    aiLoading.structure = false
  }
}

const handleAutoCorrect = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请先输入文章内容')
    return
  }

  aiLoading.correct = true

  try {
    const result = await autoCorrect({
      content: form.content,
      title: form.title || undefined
    })

    aiOriginalContent.value = result.originalContent
    aiPolishedContent.value = result.polishedContent
    aiModelName.value = result.model || ''
    aiCurrentMode.value = 'correct'
    aiDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '自动纠错失败，请稍后重试')
  } finally {
    aiLoading.correct = false
  }
}
// ===== AI 写作助手结束 =====

const isEditing = computed(() => !!articleId.value)

const canPublish = computed(() => {
  return form.title.trim() && form.content.trim()
})

const form = reactive({
  title: '',
  content: '',
  tags: [],
  cover: '',
  summary: ''
})

onMounted(() => {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  if (route.params.id) {
    articleId.value = route.params.id
    loadArticle()
  }
})

const loadArticle = async () => {
  try {
    const article = await request({
      url: `/api/articles/${articleId.value}`,
      method: 'get'
    })
    form.title = article.title
    form.content = article.content
    form.summary = article.summary || ''
    form.cover = article.cover || ''
    form.tags = article.tags || []
  } catch (error) {
    ElMessage.error('加载文章失败')
  }
}

const addTag = () => {
  const tag = tagInput.value.trim()
  if (tag && form.tags.length < 5 && !form.tags.includes(tag)) {
    form.tags.push(tag)
    tagInput.value = ''
  }
}

const removeTag = (tag) => {
  form.tags = form.tags.filter(t => t !== tag)
}

const triggerCoverUpload = () => {
  coverInputRef.value?.click()
}

const handleCoverChange = async (e) => {
  const file = e.target.files[0]
  if (file) {
    try {
      // 上传图片到服务器
      const formData = new FormData()
      formData.append('file', file)
      
      const res = await request({
        url: '/api/upload/cover',
        method: 'post',
        data: formData,
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      
      // request 拦截器已返回 payload.data，即 URL 字符串
      if (res) {
        form.cover = res
        ElMessage.success('封面图片上传成功')
      }
    } catch (err) {
      console.error('封面上传失败', err)
      ElMessage.error('封面图片上传失败')
    }
  }
}

const formatBold = () => insertFormat('**', '**')
const formatItalic = () => insertFormat('*', '*')
const formatHeading = () => insertFormat('\n## ', '\n')
const formatQuote = () => insertFormat('\n> ', '\n')
const formatCode = () => insertFormat('`', '`')
const formatLink = () => insertFormat('[', '](url)')

const insertFormat = (before, after) => {
  const editor = editorRef.value
  if (!editor) return

  const start = editor.selectionStart
  const end = editor.selectionEnd
  const selectedText = form.content.substring(start, end)
  const newText = form.content.substring(0, start) + before + selectedText + after + form.content.substring(end)
  form.content = newText
}

const saveDraft = async () => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入文章标题')
    return
  }

  saving.value = true
  try {
    const data = {
      title: form.title,
      content: form.content,
      summary: form.summary,
      cover: form.cover,
      tags: form.tags,
      status: 1
    }

    if (isEditing.value) {
      await request({
        url: `/api/articles/${articleId.value}`,
        method: 'put',
        data
      })
    } else {
      const created = await request({
        url: '/api/articles',
        method: 'post',
        data
      })
      articleId.value = created.id
    }

    ElMessage.success('草稿保存成功')
  } catch (error) {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

const publishArticle = async () => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入文章标题')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请输入文章内容')
    return
  }

  publishing.value = true
  try {
    const data = {
      title: form.title,
      content: form.content,
      summary: form.summary,
      cover: form.cover,
      tags: form.tags,
      status: 2
    }

    if (isEditing.value) {
      await request({
        url: `/api/articles/${articleId.value}`,
        method: 'put',
        data
      })
    } else {
      const created = await request({
        url: '/api/articles',
        method: 'post',
        data
      })
      articleId.value = created.id
    }

    ElMessage.success('文章发布成功')
    router.push(`/articles/${articleId.value}`)
  } catch (error) {
    ElMessage.error('发布失败，请重试')
  } finally {
    publishing.value = false
  }
}
</script>

<style lang="scss" scoped>
.create-article-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
}

.main-content {
  flex: 1;
  padding: calc(var(--header-height) + 32px) 0 40px;
}

.page-header {
  margin-bottom: 32px;
  animation: fadeInUp 0.5s ease;
}

.page-title {
  font-size: 28px;
  font-weight: 500;
  color: var(--color-text);
  letter-spacing: -0.02em;
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-muted);
  font-weight: 400;
}

.editor-container {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 32px;
}

.editor-main {
  @include card-static;
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: fadeInUp 0.6s ease 0.1s both;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-card-border);
}

.title-input {
  width: 100%;
  padding: 32px 36px 20px;
  font-size: 28px;
  font-weight: 500;
  border: none;
  background: transparent;
  color: var(--color-text);
  letter-spacing: -0.01em;
  transition: all var(--transition-fast);

  &::placeholder {
    color: var(--color-text-muted);
    font-weight: 400;
  }

  &:focus {
    outline: none;
  }
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 28px;
  border-bottom: 1px solid var(--color-divider);
  background: var(--color-bg);
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 4px;
}

.toolbar-btn {
  width: 36px;
  height: 36px;
  @include flex-center;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  cursor: pointer;

  &:hover {
    background: rgba(0, 0, 0, 0.05);
    color: var(--color-text);
  }

  &:active {
    transform: scale(0.95);
  }
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: var(--color-divider);
  margin: 0 8px;
}

.content-editor {
  flex: 1;
  width: 100%;
  min-height: 360px;
  padding: 28px 36px 40px;
  font-size: 16px;
  line-height: 1.85;
  border: none;
  border-bottom: 1px solid var(--color-divider);
  background: transparent;
  color: var(--color-text);
  resize: vertical;
  font-family: var(--font-family);

  &::placeholder {
    color: var(--color-text-muted);
  }

  &:focus {
    outline: none;
  }
}

.preview-section {
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  min-height: 320px;
  max-height: 520px;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 36px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-divider);
  background: var(--color-bg-elevated);
}

.preview-content {
  flex: 1;
  padding: 28px 36px 40px;
  overflow-y: auto;
}

.editor-sidebar {
  position: sticky;
  top: calc(var(--header-height) + 28px);
  height: fit-content;
}

.sidebar-card {
  @include card-static;
  padding: 24px;
  margin-bottom: 24px;
  animation: fadeInUp 0.6s ease 0.2s both;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-card-border);
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--color-divider);
  color: var(--color-text);

  svg {
    color: var(--color-primary);
  }
}

.form-group {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
}

.form-label {
  display: block;
  font-size: 13.5px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--color-text-secondary);
}

.form-hint {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 8px;
}

.tags-input {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-elevated);
  transition: all var(--transition-fast);

  &:focus-within {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px var(--color-accent-glow);
  }
}

.tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-primary);
  background: var(--color-google-blue-light);
  border-radius: var(--radius-sm);
  border: 1px solid rgba(26, 115, 232, 0.15);
  transition: all var(--transition-fast);

  &:hover {
    background: rgba(26, 115, 232, 0.15);
  }
}

.tag-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  color: var(--color-primary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);

  &:hover {
    color: var(--color-error);
    background: var(--color-google-red-light);
  }
}

.tag-input-field {
  flex: 1;
  min-width: 80px;
  border: none;
  font-size: 13px;
  background: transparent;
  color: var(--color-text);

  &::placeholder {
    color: var(--color-text-muted);
  }
}

.cover-upload {
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    border-color: var(--color-primary-light);
    background: rgba(26, 115, 232, 0.02);
  }
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 32px;
  color: var(--color-text-muted);

  .cover-icon {
    width: 48px;
    height: 48px;
    @include flex-center;
    background: var(--color-google-blue-light);
    border-radius: var(--radius-md);
    color: var(--color-primary);
    transition: all var(--transition-fast);
  }

  &:hover .cover-icon {
    background: rgba(26, 115, 232, 0.15);
    transform: scale(1.05);
  }

  span {
    font-size: 13px;
  }
}

.cover-preview {
  width: 100%;
  height: 160px;
  object-fit: cover;
}

.summary-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13.5px;
  resize: vertical;
  background: var(--color-bg-elevated);
  color: var(--color-text);
  transition: all var(--transition-fast);
  font-family: var(--font-family);

  &:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px var(--color-accent-glow);
    outline: none;
  }

  &::placeholder {
    color: var(--color-text-muted);
  }
}

.action-buttons {
  display: flex;
  gap: 12px;
  animation: fadeInUp 0.6s ease 0.3s both;

  button {
    flex: 1;
    justify-content: center;
  }
}

/* ===== AI 润色样式 ===== */
.ai-polish-group {
  position: relative;
  display: flex;
  align-items: center;
}

.ai-btn {
  display: flex !important;
  align-items: center;
  gap: 6px;
  width: auto !important;
  padding: 0 16px !important;
  height: 36px !important;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-primary) !important;
  background: var(--color-google-blue-light) !important;
  border-radius: var(--radius-full) !important;
  border: 1px solid rgba(26, 115, 232, 0.25) !important;
  transition: all var(--transition-fast);
  white-space: nowrap;
  cursor: pointer;

  &:hover:not(:disabled) {
    background: rgba(26, 115, 232, 0.15) !important;
    box-shadow: 0 1px 3px rgba(26, 115, 232, 0.08);
    transform: translateY(-1px);
  }

  &.loading {
    opacity: 0.7;
    cursor: not-allowed;
    animation: glowPulse 2s ease-in-out infinite;
  }
}

.ai-btn-text {
  font-size: 13px;
  font-weight: 600;
}

.ai-spinner .spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.ai-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  z-index: 100;
  min-width: 240px;
  @include liquid-glass;
  border-radius: var(--radius-lg);
  box-shadow:
    inset 1px 1px var(--color-liquid-glass-shine),
    inset -1px -1px rgba(255, 255, 255, 0.14),
    var(--shadow-lg);
  overflow: hidden;
  padding: 6px;
}

.ai-dropdown-header {
  padding: 10px 14px 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.ai-dropdown-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 14px;
  text-align: left;
  transition: all var(--transition-fast);
  cursor: pointer;
  border: none;
  background: transparent;
  color: inherit;
  border-radius: var(--radius-md);

  &:hover {
    background: var(--color-google-blue-light);
    color: var(--color-primary);
  }
}

.ai-mode-icon-wrapper {
  width: 36px;
  height: 36px;
  @include flex-center;
  border-radius: var(--radius-md);
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.ai-mode-info {
  flex: 1;
}

.ai-mode-name {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-text);
  line-height: 1.3;
  
  .ai-dropdown-item:hover & {
    color: var(--color-primary);
  }
}

.ai-mode-desc {
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.3;
  margin-top: 2px;
  
  .ai-dropdown-item:hover & {
    color: rgba(26, 115, 232, 0.7);
  }
}

/* ===== AI 写作助手样式 ===== */
.ai-assistant-card {
  .sidebar-title svg {
    color: var(--color-primary);
  }
}

.ai-assistant-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.ai-assistant-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 12px;
  height: 36px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  cursor: pointer;

  svg {
    flex-shrink: 0;
    color: var(--color-primary);
  }

  &:hover:not(:disabled) {
    color: var(--color-primary);
    background: var(--color-google-blue-light);
    border-color: rgba(26, 115, 232, 0.25);
    transform: translateY(-1px);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &.loading {
    opacity: 0.7;
    cursor: not-allowed;
  }
}

/* 一键生成：主操作样式，区别于单项生成按钮 */
.ai-assist-btn {
  height: 40px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  border-color: transparent;
  box-shadow: 0 2px 12px rgba(124, 107, 255, 0.3);

  svg {
    color: #fff;
  }

  &:hover:not(:disabled) {
    color: #fff;
    background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
    border-color: transparent;
    box-shadow: 0 4px 18px rgba(124, 107, 255, 0.45);
    transform: translateY(-1px);
  }
}

/* 撤销上次 AI 修改 */
.ai-undo-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  margin-top: 10px;
  padding: 0 12px;
  height: 32px;
  font-size: 12.5px;
  color: var(--color-text-muted);
  background: transparent;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  cursor: pointer;

  &:hover {
    color: var(--color-primary);
    border-color: rgba(124, 107, 255, 0.4);
  }
}

.title-candidates {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--color-divider);
}

.candidates-header {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-muted);
  margin-bottom: 8px;
}

.title-candidate-item {
  display: block;
  width: 100%;
  padding: 10px 12px;
  margin-bottom: 8px;
  text-align: left;
  font-size: 13.5px;
  color: var(--color-text);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  cursor: pointer;

  &:last-child {
    margin-bottom: 0;
  }

  &:hover {
    color: var(--color-primary);
    background: var(--color-google-blue-light);
    border-color: rgba(26, 115, 232, 0.25);
  }
}

/* ===== 动画 ===== */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.96);
}

.tag-enter-active,
.tag-leave-active {
  transition: all 0.2s ease;
}

.tag-enter-from,
.tag-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

@media (max-width: 1024px) {
  .editor-container {
    grid-template-columns: 1fr;
  }

  .editor-sidebar {
    position: static;
  }
}
</style>
