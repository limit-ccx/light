<template>
  <Teleport to="body">
    <Transition name="iv-fade">
      <div
        v-if="store.managerOpen"
        class="iv-overlay"
        @click.self="store.closeManager()"
      >
        <section
          ref="dialogRef"
          class="iv-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="inner-voice-title"
          tabindex="-1"
        >
          <header class="iv-head">
            <div class="iv-head-copy">
              <span class="iv-eyebrow"><i></i>Inner Voice</span>
              <h2 id="inner-voice-title">心声</h2>
              <p>从推荐心声里挑选，或写下自己的心里话；最多选 10 条在悬浮球轮播。</p>
            </div>
            <button type="button" class="iv-close" aria-label="关闭" @click="store.closeManager()">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
                <path d="M6 6l12 12M18 6 6 18" />
              </svg>
            </button>
          </header>

          <div class="iv-options">
            <label class="iv-option">
              <span class="iv-option-label">在悬浮球展示</span>
              <el-switch
                :model-value="store.enabled"
                @change="store.setEnabled"
              />
            </label>

            <label class="iv-option">
              <span class="iv-option-label">轮播间隔</span>
              <el-select
                :model-value="store.interval"
                size="small"
                class="iv-interval"
                @change="store.setInterval"
              >
                <el-option :value="5000" label="5 秒" />
                <el-option :value="8000" label="8 秒" />
                <el-option :value="15000" label="15 秒" />
                <el-option :value="30000" label="30 秒" />
              </el-select>
            </label>

            <span class="iv-meter">
              <b>{{ store.selectedCount }}</b> / {{ MAX_INNER_VOICES }} 已选
            </span>
          </div>

          <div class="iv-compose">
            <textarea
              v-model="draft"
              rows="2"
              :maxlength="MAX_INNER_VOICE_TEXT"
              placeholder="写下此刻的心里话…"
              @keydown="onDraftKeydown"
            ></textarea>
            <div class="iv-compose-foot">
              <span>{{ draft.length }}/{{ MAX_INNER_VOICE_TEXT }} · Ctrl + Enter 添加</span>
              <button type="button" class="iv-add" :disabled="!draft.trim()" @click="handleAdd">
                添加心声
              </button>
            </div>
          </div>

          <div class="iv-list-head">
            <div class="iv-tabs" role="tablist" aria-label="心声来源">
              <button
                type="button"
                role="tab"
                :class="{ active: activeTab === 'recommended' }"
                :aria-selected="activeTab === 'recommended'"
                @click="switchTab('recommended')"
              >
                推荐心声 <span>{{ store.recommendedVoices.length }}</span>
              </button>
              <button
                type="button"
                role="tab"
                :class="{ active: activeTab === 'mine' }"
                :aria-selected="activeTab === 'mine'"
                @click="switchTab('mine')"
              >
                我的心声 <span>{{ store.customCount }}</span>
              </button>
            </div>
            <div class="iv-list-actions">
              <button type="button" @click="handleFill">选满 {{ MAX_INNER_VOICES }} 条</button>
              <button type="button" :disabled="!store.selectedCount" @click="store.clearSelection()">
                清空选择
              </button>
            </div>
          </div>

          <div v-if="activeTab === 'recommended'" class="iv-search">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <circle cx="11" cy="11" r="7"></circle>
              <line x1="20" y1="20" x2="16.2" y2="16.2"></line>
            </svg>
            <input v-model="searchKeyword" type="text" placeholder="搜索推荐心声…" />
            <span v-if="searchKeyword">{{ filteredRecommended.length }} 条</span>
          </div>

          <div class="iv-list">
            <article
              v-for="voice in visibleVoices"
              :key="voice.id"
              class="iv-item"
              :class="{
                'is-selected': isSelected(voice.id),
                'is-editing': editingId === voice.id,
                'is-recommended': voice.recommended
              }"
              @click="onItemClick(voice, $event)"
            >
              <label class="iv-check">
                <input
                  type="checkbox"
                  :checked="isSelected(voice.id)"
                  :disabled="!isSelected(voice.id) && store.isSelectionFull"
                  :aria-label="`选择心声：${voice.text}`"
                  @change="toggleVoice(voice)"
                />
              </label>

              <div class="iv-item-body">
                <template v-if="editingId === voice.id">
                  <textarea
                    v-model="editDraft"
                    rows="2"
                    :maxlength="MAX_INNER_VOICE_TEXT"
                    @keydown="onEditKeydown"
                  ></textarea>
                  <div class="iv-edit-foot">
                    <span>{{ editDraft.length }}/{{ MAX_INNER_VOICE_TEXT }}</span>
                    <div>
                      <button type="button" class="iv-mini primary" @click="saveEdit(voice)">保存</button>
                      <button type="button" class="iv-mini" @click="cancelEdit">取消</button>
                    </div>
                  </div>
                </template>

                <template v-else>
                  <p class="iv-text">{{ voice.text }}</p>
                  <div class="iv-meta">
                    <span>{{ voice.recommended ? '推荐心声' : formatDate(voice.createdAt) }}</span>
                    <span v-if="isSelected(voice.id)" class="iv-tag">轮播中</span>
                  </div>
                </template>
              </div>

              <div v-if="!voice.recommended && editingId !== voice.id" class="iv-item-actions">
                <button type="button" class="iv-mini" @click="startEdit(voice)">编辑</button>
                <button type="button" class="iv-mini danger" @click="removeVoice(voice)">删除</button>
              </div>
            </article>

            <div v-if="activeTab === 'mine' && !store.customCount" class="iv-empty">
              <span class="iv-empty-glyph" aria-hidden="true">“</span>
              <h4>还没有写下心声</h4>
              <p>把想对自己说的话写在上面，之后就能和推荐心声一起挑选轮播。</p>
            </div>

            <div v-if="activeTab === 'recommended' && !filteredRecommended.length" class="iv-empty">
              <h4>没有匹配的推荐心声</h4>
              <p>换个关键词试试。</p>
            </div>
          </div>

          <footer class="iv-foot">
            <span>推荐心声可直接选用；自己写的心声登录后会同步到账号。</span>
            <button type="button" class="iv-done" @click="store.closeManager()">完成</button>
          </footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  MAX_INNER_VOICES,
  MAX_INNER_VOICE_TEXT,
  useInnerVoiceStore
} from '@/store/innerVoice'

const store = useInnerVoiceStore()

const dialogRef = ref(null)
const draft = ref('')
const editingId = ref('')
const editDraft = ref('')
const activeTab = ref('recommended')
const searchKeyword = ref('')

const filteredRecommended = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return store.recommendedVoices
  return store.recommendedVoices.filter((voice) =>
    voice.text.toLowerCase().includes(keyword)
  )
})

const visibleVoices = computed(() =>
  activeTab.value === 'recommended' ? filteredRecommended.value : store.voices
)

let previousOverflow = ''

const lockScroll = (lock) => {
  if (lock) {
    previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = previousOverflow
  }
}

watch(
  () => store.managerOpen,
  async (open) => {
    lockScroll(open)
    if (open) {
      activeTab.value = 'recommended'
      searchKeyword.value = ''
      await nextTick()
      dialogRef.value?.focus?.()
    } else {
      draft.value = ''
      cancelEdit()
    }
  }
)

const onKeydown = (event) => {
  if (event.key === 'Escape' && store.managerOpen) {
    store.closeManager()
  }
}

onMounted(() => {
  store.load()
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  lockScroll(false)
})

const isSelected = (id) => store.selectedIds.includes(id)

const toggleVoice = (voice) => {
  const ok = store.toggleVoice(voice.id)
  if (!ok) {
    ElMessage.info(`最多选择 ${MAX_INNER_VOICES} 条心声轮播`)
  }
}

// 整张卡片都是点击热区：编辑态、按钮、输入框除外
const onItemClick = (voice, event) => {
  if (editingId.value === voice.id) return
  if (event.target.closest('button, textarea, input, a, .iv-item-actions')) return
  toggleVoice(voice)
}

const switchTab = (tab) => {
  if (tab === activeTab.value) return
  cancelEdit()
  activeTab.value = tab
}

const handleAdd = () => {
  const result = store.addVoice(draft.value)
  if (!result.ok) {
    ElMessage.warning(result.reason)
    return
  }
  draft.value = ''
  activeTab.value = 'mine'
  ElMessage.success('已添加心声')
}

const handleFill = () => {
  const count = store.fillSelection()
  if (count >= MAX_INNER_VOICES) {
    ElMessage.success(`已选满 ${MAX_INNER_VOICES} 条`)
  } else {
    ElMessage.info(count ? `已选择 ${count} 条心声` : '还没有可选择的文案')
  }
}

const startEdit = (voice) => {
  editingId.value = voice.id
  editDraft.value = voice.text
}

const cancelEdit = () => {
  editingId.value = ''
  editDraft.value = ''
}

const saveEdit = (voice) => {
  const result = store.updateVoice(voice.id, editDraft.value)
  if (!result.ok) {
    ElMessage.warning(result.reason)
    return
  }
  cancelEdit()
  ElMessage.success('已保存')
}

const removeVoice = async (voice) => {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确定删除这条心声吗？', '删除心声', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    store.removeVoice(voice.id)
    if (editingId.value === voice.id) cancelEdit()
    ElMessage.success('已删除')
  } catch {
    // 用户取消
  }
}

const onDraftKeydown = (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault()
    handleAdd()
  }
}

const onEditKeydown = (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault()
    const voice = store.voices.find((item) => item.id === editingId.value)
    if (voice) saveEdit(voice)
  }
  if (event.key === 'Escape') {
    event.preventDefault()
    cancelEdit()
  }
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}
</script>

<style lang="scss" scoped>
.iv-overlay {
  position: fixed;
  inset: 0;
  z-index: 10020;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(8, 12, 20, 0.46);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.iv-dialog {
  display: flex;
  flex-direction: column;
  width: min(760px, 100%);
  max-height: min(86vh, 820px);
  overflow: hidden;
  border-radius: 28px;
  border: 1px solid var(--content-line);
  background:
    radial-gradient(80% 60% at 100% 0%, var(--content-glow-b), transparent 58%),
    var(--content-surface);
  box-shadow: 0 40px 100px rgba(8, 16, 36, 0.32);
  outline: none;
}

.iv-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 26px 28px 18px;
}

.iv-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: var(--content-ink-3);
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.16em;
  text-transform: uppercase;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: linear-gradient(135deg, #4285f4, #a855f7);
    box-shadow: 0 0 10px rgba(66, 133, 244, 0.6);
  }
}

.iv-head h2 {
  margin: 0 0 7px;
  color: var(--content-ink);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.iv-head p {
  margin: 0;
  color: var(--content-ink-2);
  font-size: 13.5px;
  line-height: 1.7;
}

.iv-close {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--content-ink-3);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    color: var(--content-ink);
    border-color: var(--content-line-strong);
  }
}

.iv-options {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 22px;
  margin: 0 28px 16px;
  padding: 12px 16px;
  border-radius: 16px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);
}

.iv-option {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.iv-option-label {
  color: var(--content-ink-2);
  font-size: 12.5px;
}

.iv-interval {
  width: 92px;

  :deep(.el-select__wrapper) {
    min-height: 28px;
    border-radius: 9px;
  }
}

.iv-meter {
  margin-left: auto;
  color: var(--content-ink-3);
  font-size: 12.5px;

  b {
    color: var(--color-primary);
    font-size: 15px;
    font-variant-numeric: tabular-nums;
  }
}

.iv-compose {
  margin: 0 28px 18px;
  padding: 12px 14px;
  border-radius: 18px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);

  &:focus-within {
    border-color: var(--content-line-strong);
    box-shadow: 0 0 0 4px var(--content-glow-a);
  }

  textarea {
    width: 100%;
    border: none;
    outline: none;
    resize: none;
    background: transparent;
    color: var(--content-ink);
    font-family: inherit;
    font-size: 14.5px;
    line-height: 1.7;

    &::placeholder {
      color: var(--content-ink-3);
    }
  }
}

.iv-compose-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 6px;
  color: var(--content-ink-3);
  font-size: 11.5px;
}

.iv-add {
  height: 34px;
  padding: 0 18px;
  border: none;
  border-radius: 10px;
  color: #fff;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(120deg, #1a73e8, #6f5cf6);
  box-shadow: 0 8px 18px rgba(47, 99, 235, 0.24);
  transition: transform var(--transition-fast), opacity var(--transition-fast);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
    box-shadow: none;
  }
}

.iv-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 28px 10px;
}

.iv-tabs {
  display: inline-flex;
  gap: 3px;
  padding: 3px;
  border-radius: 999px;
  border: 1px solid var(--content-line);
  background: var(--content-tabs);

  button {
    height: 30px;
    padding: 0 13px;
    border: none;
    border-radius: 999px;
    background: transparent;
    color: var(--content-ink-2);
    font-family: inherit;
    font-size: 12.5px;
    cursor: pointer;
    transition: all var(--transition-fast);

    span {
      margin-left: 5px;
      color: var(--content-ink-3);
      font-size: 11px;
    }

    &:hover {
      color: var(--color-primary);
    }

    &.active {
      color: var(--color-primary);
      background: var(--content-surface);
      box-shadow: var(--content-shadow);
      font-weight: 600;

      span {
        color: var(--color-primary);
      }
    }
  }
}

.iv-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 28px 10px;
  padding: 0 12px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);
  color: var(--content-ink-3);

  input {
    flex: 1;
    min-width: 0;
    border: none;
    outline: none;
    background: transparent;
    color: var(--content-ink);
    font-family: inherit;
    font-size: 12.5px;

    &::placeholder {
      color: var(--content-ink-3);
    }
  }

  span {
    flex-shrink: 0;
    font-size: 11px;
  }
}

.iv-list-actions {
  display: flex;
  gap: 14px;

  button {
    padding: 0;
    border: none;
    background: transparent;
    color: var(--color-primary);
    font-family: inherit;
    font-size: 12px;
    cursor: pointer;

    &:disabled {
      color: var(--content-ink-3);
      cursor: not-allowed;
    }

    &:hover:not(:disabled) {
      text-decoration: underline;
    }
  }
}

.iv-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 28px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.iv-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 18px;
  min-height: 76px;
  border-radius: 16px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  cursor: pointer;
  transition: border-color var(--transition-fast), background var(--transition-fast),
    box-shadow var(--transition-fast), transform var(--transition-fast);

  &:hover {
    border-color: var(--content-line-strong);
    background: var(--content-surface-tint);
    box-shadow: 0 6px 18px rgba(59, 130, 246, 0.08);
  }

  &:active {
    transform: scale(0.998);
  }

  &.is-editing {
    cursor: default;

    &:hover {
      background: var(--content-surface);
      box-shadow: none;
    }
  }

  &.is-selected {
    border-color: var(--content-line-strong);
    background:
      linear-gradient(120deg, var(--content-glow-a), transparent 60%),
      var(--content-surface-tint);
  }
}

.iv-check {
  display: flex;
  flex-shrink: 0;
  padding: 6px;
  margin: -6px 0 0 -6px;
  border-radius: 10px;
  transition: background-color var(--transition-fast);

  &:hover {
    background: rgba(59, 130, 246, 0.08);
  }

  input {
    width: 18px;
    height: 18px;
    accent-color: var(--color-primary);
    cursor: pointer;

    &:disabled {
      cursor: not-allowed;
      opacity: 0.4;
    }
  }
}

.iv-item-body {
  flex: 1;
  min-width: 0;
}

.iv-text {
  margin: 0;
  color: var(--content-ink);
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.iv-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 5px;
  color: var(--content-ink-3);
  font-size: 11.5px;
}

.iv-tag {
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(26, 115, 232, 0.1);
  color: var(--color-primary);
  font-size: 10.5px;
  font-weight: 600;
}

.iv-item-actions {
  display: flex;
  flex-shrink: 0;
  gap: 4px;
}

.iv-mini {
  height: 28px;
  padding: 0 10px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--content-ink-3);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    color: var(--content-ink);
    background: var(--content-chip);
    border-color: var(--content-chip-line);
  }

  &.danger:hover {
    color: var(--color-error);
    background: rgba(234, 67, 53, 0.1);
    border-color: transparent;
  }

  &.primary {
    color: #fff;
    background: linear-gradient(120deg, #1a73e8, #6f5cf6);
  }
}

.iv-item-body textarea {
  width: 100%;
  padding: 9px 11px;
  border-radius: 10px;
  border: 1px solid var(--content-line-strong);
  background: var(--content-surface);
  color: var(--content-ink);
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  outline: none;
}

.iv-edit-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 6px;
  color: var(--content-ink-3);
  font-size: 11.5px;

  div {
    display: flex;
    gap: 4px;
  }
}

.iv-empty {
  padding: 34px 20px 26px;
  border-radius: 18px;
  border: 1px dashed var(--content-line-strong);
  background: var(--content-surface-tint);
  text-align: center;

  .iv-empty-glyph {
    display: block;
    margin-bottom: 2px;
    color: var(--content-line-strong);
    font-family: var(--font-serif);
    font-size: 46px;
    line-height: 1;
  }

  h4 {
    margin: 0 0 6px;
    color: var(--content-ink);
    font-size: 15px;
    font-weight: 600;
  }

  p {
    max-width: 380px;
    margin: 0 auto;
    color: var(--content-ink-2);
    font-size: 13px;
    line-height: 1.7;
  }
}

.iv-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 28px;
  border-top: 1px solid var(--content-line);
  background: var(--content-surface-soft);
  color: var(--content-ink-3);
  font-size: 11.5px;
}

.iv-done {
  height: 34px;
  padding: 0 22px;
  border: none;
  border-radius: 10px;
  color: #fff;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(120deg, #1a73e8, #6f5cf6);
  box-shadow: 0 8px 18px rgba(47, 99, 235, 0.22);
}

.iv-fade-enter-active,
.iv-fade-leave-active {
  transition: opacity 0.22s ease;

  .iv-dialog {
    transition: transform 0.22s cubic-bezier(0.2, 0.8, 0.2, 1);
  }
}

.iv-fade-enter-from,
.iv-fade-leave-to {
  opacity: 0;

  .iv-dialog {
    transform: translateY(12px) scale(0.98);
  }
}

@media (max-width: 640px) {
  .iv-overlay {
    align-items: stretch;
    padding: 12px;
  }

  .iv-dialog {
    width: 100%;
    max-height: calc(100vh - 24px);
    border-radius: 22px;
  }

  .iv-head {
    padding: 20px 18px 14px;
  }

  .iv-head h2 {
    font-size: 22px;
  }

  .iv-options {
    margin: 0 18px 14px;
    gap: 10px 16px;
  }

  .iv-meter {
    width: 100%;
    margin-left: 0;
  }

  .iv-compose {
    margin: 0 18px 16px;
  }

  .iv-list-head {
    padding: 0 18px 10px;
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .iv-tabs {
    width: 100%;

    button {
      flex: 1;
    }
  }

  .iv-search {
    margin: 0 18px 10px;
  }

  .iv-list {
    padding: 0 18px 14px;
  }

  .iv-item {
    flex-wrap: wrap;
    padding: 12px;
  }

  .iv-item-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .iv-foot {
    padding: 12px 18px;
  }
}
</style>
