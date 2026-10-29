<template>
  <div class="agent-ide" :class="{ 'no-rail': !railOpen }">

    <!-- 左侧：历史对话（任务列表） -->
    <aside class="ide-rail" :class="{ open: railOpen }">
      <div class="rail-inner">
        <div class="rail-brand">
          <div class="rail-orb" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3.5l1.8 4.9 4.9 1.8-4.9 1.8L12 16.9l-1.8-4.9L5.3 10.2l4.9-1.8L12 3.5z" />
              <path d="M18.6 15.4l.8 2.1 2.1.8-2.1.8-.8 2.1-.8-2.1-2.1-.8 2.1-.8.8-2.1z" />
            </svg>
          </div>
          <div class="rail-copy">
            <span class="rail-eyebrow">AI OPERATIONS COPILOT</span>
            <h2>运营助手</h2>
            <p>查数据 · 答口径 · 生成待确认操作</p>
          </div>
        </div>

        <button type="button" class="rail-new" :disabled="streaming" @click="newChat">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          <span>新建对话</span>
          <kbd>Ctrl+N</kbd>
        </button>

        <div class="rail-search">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" /><path d="m20 20-3.2-3.2" />
          </svg>
          <input v-model="historyQuery" type="text" placeholder="搜索历史对话" />
        </div>

        <div class="rail-scroll">
          <div v-if="historyLoading && !historyList.length" class="rail-loading">
            <span class="loading-dots" aria-hidden="true"><i></i><i></i><i></i></span>加载中…
          </div>
          <template v-else-if="filteredGroups.length">
            <div v-for="g in filteredGroups" :key="g.label" class="rail-group">
              <div class="rail-group-label">{{ g.label }}</div>
              <button
                v-for="(pair, pi) in g.items"
                :key="g.label + pi"
                type="button"
                class="rail-item"
                :class="{ active: historyView && historyView.time === pair.time && historyView.question === pair.question }"
                @click="openHistoryPair(pair)"
              >
                <span class="ri-time">{{ pair.timeShort }}</span>
                <p class="ri-q">{{ pair.question }}</p>
                <span v-if="pair.model" class="ri-model">{{ pair.model }}</span>
              </button>
            </div>
          </template>
          <div v-else class="rail-empty">
            <span class="empty-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 11.5a8.4 8.4 0 0 1-8.5 8.5H5l-2 2V11.5A8.5 8.5 0 0 1 11.5 3h1A8.5 8.5 0 0 1 21 11.5z" />
                <path d="M8 10h8M8 14h5" />
              </svg>
            </span>
            <p>暂无历史对话</p>
            <span>发起一次对话后自动保存</span>
          </div>
        </div>

        <div class="rail-foot">
          <span class="rail-status"><i class="dot"></i>服务正常 · v2.0</span>
          <span v-if="quotaLeft >= 0" class="rail-quota">今日剩余 <b>{{ quotaLeft }}</b> 次</span>
        </div>
      </div>
    </aside>

    <!-- 中间：对话主区 -->
    <main class="ide-main">
      <div class="main-tabs">
        <div class="main-tab" :title="sessionTitle">
          <span class="tab-dot" :class="{ busy: streaming, past: !!historyView }" aria-hidden="true"></span>
          <span class="tab-title">{{ sessionTitle }}</span>
          <span class="tab-sub">{{ historyView ? '历史回看' : streaming ? '正在生成' : '在线' }}</span>
        </div>

        <div class="tabs-spacer"></div>


        <button
          type="button"
          class="tab-icon"
          :class="{ on: railOpen }"
          title="历史对话"
          aria-label="历史对话"
          @click="railOpen = !railOpen"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" />
          </svg>
        </button>
      </div>

      <!-- 待确认操作（含历史遗留） -->
      <section v-if="pendingActions.length" class="pending-bar">
        <div class="pending-head">
          <span class="pending-badge" aria-hidden="true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10.3 3.9L2.4 17.4A2 2 0 0 0 4.1 20.4h15.8a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />
              <path d="M12 9v4M12 17h.01" />
            </svg>
          </span>
          <div class="pending-copy">
            <strong>待确认操作 · {{ pendingActions.length }} 项</strong>
            <span>以下变更需要你确认后才会执行</span>
          </div>
          <div class="pending-ops">
            <button type="button" class="link-ok" :disabled="batchBusy" @click="confirmAll">全部确认</button>
            <button type="button" class="link-danger" :disabled="batchBusy" @click="rejectAll">全部取消</button>
          </div>
        </div>
        <div class="pending-cards">
          <div
            v-for="action in pendingActions"
            :key="action.operationId"
            class="pending-card"
            :class="{ strict: action.riskLevel === 'STRICT' }"
          >
            <span class="pc-risk" :class="{ strict: action.riskLevel === 'STRICT' }">
              {{ action.riskLevel === 'STRICT' ? '高风险' : '待确认' }}
            </span>
            <div class="pc-main">
              <strong>{{ action.targetSummary }}</strong>
              <span class="pc-meta">{{ action.tool }}<template v-if="action.reason"> · {{ action.reason }}</template></span>
            </div>
            <div class="pc-actions">
              <button type="button" class="mini-btn primary" :disabled="action.busy" @click="confirm(action, true)">确认执行</button>
              <button type="button" class="mini-btn" :disabled="action.busy" @click="confirm(action, false)">取消</button>
            </div>
          </div>
        </div>
      </section>

      <!-- 对话区 -->
      <section class="chat-shell" :class="{ 'is-streaming': streaming }">
        <div class="chat-area" ref="chatBodyRef">
          <!-- 历史回看模式：点击左侧历史问题后在此展开该轮完整问答 -->
          <template v-if="historyView">
            <div class="history-view-banner">
              <span>正在查看 {{ historyView.time }} 的历史对话<template v-if="historyView.model"> · {{ historyView.model }}</template></span>
              <button type="button" class="ghost-btn" @click="historyView = null">返回当前对话</button>
            </div>
            <div class="row row-user">
              <div class="bubble user-bubble">{{ historyView.question }}</div>
            </div>
            <div class="row row-ai">
              <div class="avatar avatar-ai">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                  <path d="M12 2l2.2 6.1L20.5 10l-6.3 1.9L12 18l-2.2-6.1L3.5 10l6.3-1.9L12 2z" />
                </svg>
              </div>
              <div class="bubble ai-bubble">{{ historyView.answer }}</div>
            </div>
          </template>

          <template v-else>
            <div v-if="messages.length === 0" class="welcome">
              <div class="welcome-orb">
                <span class="orb-ring" aria-hidden="true"></span>
                <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                  <path d="M12 2l2.2 6.1L20.5 10l-6.3 1.9L12 18l-2.2-6.1L3.5 10l6.3-1.9L12 2z" />
                  <path d="M18.6 16.4l.7 1.9 1.9.7-1.9.7-.7 1.9-.7-1.9-1.9-.7 1.9-.7.7-1.9z" opacity=".8" />
                </svg>
              </div>
              <span class="welcome-eyebrow">纸域空间 · AI COPILOT</span>
              <h3>你好，我是运营助手</h3>
              <p class="welcome-desc">
                查平台数据、检索运营文档（审核政策 / 数据口径），辅助<b>内容审核、树洞管理、内容推荐、建议反馈</b>。
                所有写操作都会先生成待确认卡片 —— 你确认后才会执行。
              </p>
              <div class="cap-row">
                <span class="cap-chip"><i></i>数据查询</span>
                <span class="cap-chip"><i></i>审核处置</span>
                <span class="cap-chip"><i></i>政策问答</span>
                <span class="cap-chip"><i></i>待办跟进</span>
              </div>
              <div class="layout-hint">
                <span>左侧 · 历史对话</span>
                <span>输入框下方 · 模型与思考强度</span>
              </div>
              <div class="quick-grid">
                <button
                  v-for="(q, qi) in quickQuestions"
                  :key="q"
                  type="button"
                  class="quick-q"
                  :disabled="streaming"
                  @click="send(q)"
                >
                  <span class="quick-index">0{{ qi + 1 }}</span>
                  <span class="quick-text">{{ q }}</span>
                  <svg class="quick-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M7 17L17 7M9 7h8v8" />
                  </svg>
                </button>
              </div>
            </div>

            <template v-for="(msg, mi) in messages" :key="mi">
              <!-- 纯文本消息（历史/用户） -->
              <div v-if="msg.kind === 'user'" class="row row-user">
                <div class="bubble user-bubble">{{ msg.text }}</div>
              </div>
              <div v-else-if="msg.kind === 'assistant-plain'" class="row row-ai">
                <div class="avatar avatar-ai">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                    <path d="M12 2l2.2 6.1L20.5 10l-6.3 1.9L12 18l-2.2-6.1L3.5 10l6.3-1.9L12 2z" />
                  </svg>
                </div>
                <div class="bubble ai-bubble">{{ msg.text }}</div>
              </div>

              <!-- ReAct 过程消息（分段渲染） -->
              <div v-else-if="msg.kind === 'react'" class="row row-ai react-msg">
                <div class="avatar avatar-ai">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                    <path d="M12 2l2.2 6.1L20.5 10l-6.3 1.9L12 18l-2.2-6.1L3.5 10l6.3-1.9L12 2z" />
                  </svg>
                </div>
                <div class="react-block">
                  <div v-if="msg.modelTag" class="model-tag">
                    <i class="status-dot ready"></i>{{ msg.modelTag }}
                  </div>
                  <template v-for="(seg, si) in msg.segments" :key="si">
                    <div v-if="seg.type === 'text' && hasActionAfter(msg, si)" class="think-line">
                      <span class="think-tag">思考</span>
                      <span class="think-text">{{ seg.content }}</span>
                    </div>
                    <div v-else-if="seg.type === 'action'" class="action-line">
                      <span class="action-chip" :class="seg.status">
                        <i class="chip-dot"></i>{{ chipLabel(seg) }}
                      </span>
                    </div>
                    <div v-else-if="seg.type === 'draft'" class="draft-inline">
                      <span class="draft-chip">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                          <path d="M9 4h6v3H9z" />
                          <path d="M15 5h2a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h2" />
                        </svg>
                        待确认：{{ seg.payload.targetSummary }}
                      </span>
                    </div>
                    <div v-else-if="seg.type === 'ask'" class="ask-card">
                      <div class="ask-title">
                        <span class="ask-icon" aria-hidden="true">?</span>助手征询
                      </div>
                      <p class="ask-question">{{ seg.payload.question }}</p>
                      <p v-if="seg.payload.suggestion" class="ask-suggestion">
                        <span>建议</span>{{ seg.payload.suggestion }}
                      </p>
                      <div v-if="seg.payload.options && seg.payload.options.length" class="ask-options">
                        <button
                          v-for="(opt, oi) in seg.payload.options"
                          :key="oi"
                          type="button"
                          class="ask-opt"
                          :disabled="streaming"
                          @click="send(typeof opt === 'string' ? opt : String(opt))"
                        >
                          {{ typeof opt === 'string' ? opt : JSON.stringify(opt) }}
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M9 6l6 6-6 6" />
                          </svg>
                        </button>
                      </div>
                    </div>
                    <div v-else-if="seg.type === 'text' && seg.content" class="bubble ai-bubble final-bubble">{{ seg.content }}</div>
                  </template>
                  <div v-if="msg.streaming && !lastSegmentIsText(msg)" class="typing-dots"><i></i><i></i><i></i></div>
                </div>
              </div>
            </template>

            <div v-if="errorText" class="chat-error">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <circle cx="12" cy="12" r="9" />
                <path d="M12 8v4M12 16h.01" />
              </svg>
              <span>{{ errorText }}</span>
            </div>
          </template>
        </div>
      </section>

      <footer class="composer">
        <div class="composer-field">
          <textarea
            v-model="input"
            rows="2"
            maxlength="800"
            :placeholder="streaming ? '继续输入以排队后续修改…' : '例：过一遍待审核的文章 / 今天的核心指标'"
            @keydown.enter.exact.prevent="onEnter"
          ></textarea>
          <div class="composer-foot">
            <span class="composer-hint">
              <template v-if="streaming">生成中：可"插入"排队问题，或"停止"当前生成</template>
              <template v-else><kbd>Enter</kbd> 发送 · 写操作需确认后执行</template>
            </span>
            <span class="composer-count">{{ input.length }}<i>/800</i></span>
          </div>
        </div>
        <!-- 生成中：插入排队问题 -->
        <button
          v-if="streaming"
          type="button"
          class="insert-btn"
          :disabled="!input.trim()"
          title="当前回答结束后处理这个问题"
          @click="insert"
        >
          插入<span v-if="queueCount" class="insert-badge">{{ queueCount }}</span>
        </button>
        <!-- 生成中：停止 -->
        <button
          v-if="streaming"
          type="button"
          class="stop-btn"
          title="停止生成"
          @click="stop"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <rect x="6" y="6" width="12" height="12" rx="2" />
          </svg>
        </button>
        <!-- 空闲：发送 -->
        <button
          v-else
          type="button"
          class="send-btn"
          :disabled="!input.trim()"
          title="发送"
          @click="send()"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
          </svg>
        </button>
      </footer>

      <div class="composer-aux">
        <div class="tabs-model">
          <el-select
            ref="modelSelectRef"
            v-model="provider"
            class="model-picker"
            popper-class="agent-model-popper"
            :disabled="streaming"
            placeholder="选择模型"
          >
            <template #header>
              <div class="mp-plan">
                <span class="mp-plan-name">AI 助手</span>
                <span class="mp-plan-tag">免费额度</span>
                <span class="mp-plan-quota">今日剩余 {{ quotaLeft >= 0 ? quotaLeft : '—' }} 次</span>
              </div>
              <button type="button" class="mp-config" @click="goModelConfig">
                <span class="mpc-icon" aria-hidden="true">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="7.5" cy="15.5" r="4.5" />
                    <path d="M10.7 12.3L20 3M16.5 6.5L19.5 9.5" />
                  </svg>
                </span>
                <span class="mpc-label">模型密钥配置</span>
                <span v-if="models.length" class="mpc-badge">{{ readyCount }}/{{ models.length }} 可用</span>
                <svg class="mpc-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M9 6l6 6-6 6" />
                </svg>
              </button>
              <div class="mp-group">模型</div>
            </template>
            <el-option
              v-for="m in modelOptions"
              :key="m.provider"
              :value="m.provider"
              :label="modelLabel(m)"
              :disabled="!m.ready"
            >
              <span class="model-option">
                <span class="mo-provider">{{ m.provider }}</span>
                <span class="mo-tail">
                  <span v-if="m.model" class="mo-model">{{ m.model }}</span>
                  <span v-if="m.ready && m.reasoningModel" class="mo-tag">推理</span>
                  <span v-else-if="!m.ready" class="mo-tag muted">未配置</span>
                  <svg v-if="m.provider === provider" class="mo-check" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M20 6L9 17l-5-5" />
                  </svg>
                </span>
              </span>
            </el-option>
            <template #footer>
              <div class="model-effort" :class="{ deep: effort === 'deep' }">
                <div class="me-head">
                  <span class="me-label">思考强度</span>
                  <span class="me-value">{{ effort === 'deep' ? '深度思考' : '标准' }}</span>
                  <span class="me-kbd">Alt+T</span>
                </div>
                <div
                  class="effort-slider"
                  :class="{ deep: effort === 'deep', locked: streaming }"
                  role="switch"
                  :aria-checked="effort === 'deep'"
                  :aria-disabled="streaming"
                  :tabindex="streaming ? -1 : 0"
                  title="切换思考强度（Alt+T）"
                  @click.stop="toggleEffort"
                  @keydown.enter.prevent="toggleEffort"
                  @keydown.space.prevent="toggleEffort"
                >
                  <i class="effort-slider-fill"></i>
                  <i class="effort-slider-ticks" aria-hidden="true"></i>
                  <i class="effort-slider-thumb"></i>
                </div>
                <div class="me-scale">
                  <span :class="{ on: effort === 'standard' }">标准</span>
                  <span :class="{ on: effort === 'deep' }">深度思考</span>
                </div>
              </div>
              <button type="button" class="mp-confirm" @click="closeModelPicker">选择模型</button>
            </template>
          </el-select>
          <span class="plan-badge">免费</span>
        </div>
        <button
          type="button"
          class="effort-tag"
          :class="{ deep: effort === 'deep' }"
          :disabled="streaming"
          title="切换思考强度"
          @click="toggleEffort"
        >
          <i class="effort-dot"></i>{{ effort === 'deep' ? '深度思考' : '标准模式' }}
        </button>

        <div class="tabs-spacer"></div>

        <button type="button" class="aux-config" title="配置模型平台密钥" @click="goModelConfig">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="7.5" cy="15.5" r="4.5" />
            <path d="M10.7 12.3L20 3M16.5 6.5L19.5 9.5" />
          </svg>
          <span>模型密钥配置</span>
        </button>
      </div>
    </main>

  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  adminAgentChatStream, getAdminAgentHistory, clearAdminAgentHistory,
  getPendingActions, confirmPendingAction, getAgentModels, getChatHistory,
  stopAgentChat, insertAgentQuestion
} from '@/api/adminAgent'

const quickQuestions = ['待审核队列有多少？', '今天的核心指标', '树洞审核政策要点', '文章审核标准是什么？']

const router = useRouter()
const messages = ref([])
const input = ref('')
const streaming = ref(false)
const errorText = ref('')
const quotaLeft = ref(-1)
const pendingActions = ref([])
const chatBodyRef = ref(null)
let abortFn = null

// 模型选择与思考强度（本地记忆：刷新后保持上次选择）
const AGENT_PREF_KEY = 'admin-agent:pref'
const savedPref = (() => {
  try { return JSON.parse(localStorage.getItem(AGENT_PREF_KEY) || '{}') } catch { return {} }
})()
const provider = ref(savedPref.provider || 'deepseek')
const effort = ref(savedPref.effort || 'standard')
const models = ref([])

// 选择变化即持久化
watch([provider, effort], () => {
  try {
    localStorage.setItem(AGENT_PREF_KEY, JSON.stringify({
      provider: provider.value,
      effort: effort.value
    }))
  } catch { /* ignore */ }
})
const modelSelectRef = ref(null)

// 布局：左历史 / 中对话（模型配置在输入框下方）
const MOBILE = 992
const railOpen = ref(window.innerWidth >= MOBILE)
const railIsOverlay = ref(window.innerWidth < MOBILE)

// 持久化历史（左侧列表）
const historyList = ref([])
const historyLoading = ref(false)
const historyQuery = ref('')
const historyView = ref(null)

// 生成中排队插入计数
const queueCount = ref(0)

const modelOptions = computed(() =>
  models.value.length ? models.value : [{ provider: provider.value, model: '', ready: true }]
)

const readyCount = computed(() => models.value.filter(m => m.ready).length)

const modelLabel = (m) => (m.model ? `${m.provider} · ${m.model}` : m.provider)

const sessionTitle = computed(() => {
  if (historyView.value) return `历史回看 · ${historyView.value.time}`
  const firstUser = messages.value.find(m => m.kind === 'user')
  if (firstUser) {
    return firstUser.text.length > 24 ? firstUser.text.slice(0, 24) + '…' : firstUser.text
  }
  return '新对话'
})

const toggleEffort = () => {
  if (streaming.value) return
  effort.value = effort.value === 'deep' ? 'standard' : 'deep'
}

// Alt+T 切换思考强度（Ctrl+T 被浏览器占用，无法在页面内拦截）
const onShortcut = (e) => {
  if (e.altKey && !e.ctrlKey && !e.metaKey && (e.key === 't' || e.key === 'T')) {
    e.preventDefault()
    toggleEffort()
  }
  if (e.ctrlKey && !e.altKey && !e.metaKey && (e.key === 'n' || e.key === 'N')) {
    e.preventDefault()
    if (!streaming.value) newChat()
  }
}

const goModelConfig = () => {
  modelSelectRef.value?.blur?.()
  router.push('/admin/config')
}

const closeModelPicker = () => {
  modelSelectRef.value?.blur?.()
}

const loadModels = async () => {
  try {
    const data = await getAgentModels()
    models.value = data || []
    // 记忆的平台不可用（如密钥被移除）时才回落到第一个可用平台
    const current = models.value.find(m => m.provider === provider.value)
    if (!current || !current.ready) {
      const firstReady = models.value.find(m => m.ready)
      if (firstReady) provider.value = firstReady.provider
    }
  } catch { models.value = [] }
}

const formatHistoryTime = (t) => {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

const shortTime = (t) => {
  const d = new Date(t)
  if (isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  const sameDay = d.toDateString() === new Date().toDateString()
  return sameDay
    ? `${pad(d.getHours())}:${pad(d.getMinutes())}`
    : `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const dayLabel = (t) => {
  const d = new Date(t)
  if (isNaN(d.getTime())) return '更早'
  const pad = (n) => String(n).padStart(2, '0')
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const that = new Date(d); that.setHours(0, 0, 0, 0)
  const diff = (today - that) / 86400000
  if (diff <= 0) return '今天'
  if (diff <= 1) return '昨天'
  if (diff <= 7) return '本周'
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

const loadHistory = async () => {
  if (!historyList.value.length) historyLoading.value = true
  try {
    const data = await getChatHistory(0, 50)
    historyList.value = data?.list || []
  } catch { historyList.value = [] }
  historyLoading.value = false
}

/** 扁平日志按时间正序配对：每条用户提问 + 紧随其后的助手回答 */
const historyPairs = computed(() => {
  const asc = [...historyList.value].sort(
    (a, b) => new Date(a.createdAt) - new Date(b.createdAt))
  const pairs = []
  for (let i = 0; i < asc.length; i++) {
    const item = asc[i]
    if (item.role !== 'user') continue
    const next = asc[i + 1]
    const answer = next && next.role === 'assistant' ? next.content : '（无回答记录）'
    pairs.push({
      question: item.content,
      answer,
      time: formatHistoryTime(item.createdAt),
      timeShort: shortTime(item.createdAt),
      rawTime: item.createdAt,
      model: item.model || ''
    })
  }
  return pairs.reverse() // 最近优先
})

/** 左侧列表按日期分组 + 搜索过滤 */
const filteredGroups = computed(() => {
  const q = historyQuery.value.trim().toLowerCase()
  const groups = []
  const map = new Map()
  for (const p of historyPairs.value) {
    if (q && !p.question.toLowerCase().includes(q)) continue
    const label = dayLabel(p.rawTime)
    if (!map.has(label)) {
      const g = { label, items: [] }
      map.set(label, g)
      groups.push(g)
    }
    map.get(label).items.push(p)
  }
  return groups
})

const openHistoryPair = (pair) => {
  historyView.value = pair
  if (railIsOverlay.value) railOpen.value = false
  nextTick(scrollBottom)
}

onMounted(() => {
  window.addEventListener('keydown', onShortcut)
  window.addEventListener('resize', handleResize, { passive: true })
  restoreHistory()
  loadPending()
  loadModels()
  loadHistory()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onShortcut)
  window.removeEventListener('resize', handleResize)
})

const handleResize = () => {
  const railOverlay = window.innerWidth < MOBILE
  if (railOverlay !== railIsOverlay.value) {
    railIsOverlay.value = railOverlay
    railOpen.value = !railOverlay
  }
}

const restoreHistory = async () => {
  try {
    const data = await getAdminAgentHistory()
    messages.value = (data?.messages || []).map(m => ({
      kind: m.role === 'user' ? 'user' : 'assistant-plain',
      text: m.content
    }))
    scrollBottom()
  } catch { /* ignore */ }
}

const loadPending = async () => {
  try {
    pendingActions.value = (await getPendingActions()) || []
  } catch { pendingActions.value = [] }
}

const newChat = async () => {
  if (streaming.value) return
  try { await clearAdminAgentHistory() } catch { /* ignore */ }
  messages.value = []
  historyView.value = null
  errorText.value = ''
  queueCount.value = 0
}

const onEnter = (e) => { if (!e.isComposing) send() }

const send = (preset) => {
  const text = (typeof preset === 'string' ? preset : input.value).trim()
  if (!text || streaming.value) return
  input.value = ''
  errorText.value = ''
  historyView.value = null
  messages.value.push({ kind: 'user', text })

  const reactMsg = { kind: 'react', segments: [], streaming: true }
  messages.value.push(reactMsg)
  streaming.value = true
  scrollBottom()

  const pushText = (piece, isThinking) => {
    const segs = reactMsg.segments
    const last = segs[segs.length - 1]
    if (last && last.type === 'text') {
      last.content += piece
    } else {
      segs.push({ type: 'text', content: piece, thinking: isThinking })
    }
    scrollBottom()
  }

  abortFn = adminAgentChatStream(text, { provider: provider.value, effort: effort.value }, {
    onAction: (payload) => {
      // action 到来：后续文本属于新段落（思考）；结束当前 text 段
      reactMsg.segments.push({ type: 'action', name: payload.name, status: payload.status })
      scrollBottom()
    },
    onObservation: (payload) => {
      const segs = reactMsg.segments
      for (let i = segs.length - 1; i >= 0; i--) {
        if (segs[i].type === 'action' && segs[i].name === payload.name) {
          segs[i].summary = payload.summary
          break
        }
      }
      scrollBottom()
    },
    onDelta: (piece) => {
      // 追加时不判思考/回答（渲染时用 hasActionAfter 回看：
      // 其后仍有工具调用的文本段=思考，最后一段=最终回答）
      pushText(piece, false)
      scrollBottom()
    },
    onDraft: (payload) => {
      reactMsg.segments.push({ type: 'draft', payload })
      pendingActions.value.push({ ...payload, busy: false })
      scrollBottom()
    },
    onAsk: (payload) => {
      reactMsg.segments.push({ type: 'ask', payload })
      scrollBottom()
    },
    onDone: (payload) => {
      reactMsg.streaming = false
      streaming.value = false
      queueCount.value = 0
      if (payload && payload.quotaLeft !== undefined) quotaLeft.value = Number(payload.quotaLeft)
      if (payload && payload.model) reactMsg.modelTag = payload.model
      loadHistory()
      scrollBottom()
    },
    onError: (err) => {
      reactMsg.streaming = false
      streaming.value = false
      queueCount.value = 0
      errorText.value = err?.message || '助手暂时不可用，请稍后重试'
      scrollBottom()
    }
  })
}

const hasActionAfter = (msg, segIndex) => {
  const segs = msg.segments
  for (let i = segIndex + 1; i < segs.length; i++) {
    if (segs[i].type === 'action' || segs[i].type === 'draft') return true
  }
  return false
}

const lastSegmentIsText = (msg) => {
  const segs = msg.segments
  return segs.length > 0 && segs[segs.length - 1].type === 'text'
}

const chipLabel = (seg) => {
  const labels = {
    query_statistics: '查询统计', query_content_detail: '查询内容', query_user: '查询用户',
    search_knowledge: '检索知识库', review_article: '审核文章', review_comment: '审核评论',
    handle_report: '处理举报', moderate_tree_hole: '树洞管理', set_recommendation: '推荐位',
    resolve_feedback: '处理反馈'
  }
  const name = labels[seg.name] || seg.name
  return seg.status === 'running' ? name + '…' : seg.status === 'cached' ? name + '（已查过）' : name
}

/** 停止生成：后端置中断标志 + 本地断流；已生成内容保留，可继续追问 */
const stop = async () => {
  if (abortFn) { abortFn(); abortFn = null }
  try { await stopAgentChat() } catch { /* ignore */ }
  streaming.value = false
  const last = messages.value[messages.value.length - 1]
  if (last && last.kind === 'react') {
    last.streaming = false
    last.done = true
  }
  errorText.value = ''
}

/** 插入对话：生成进行中追加问题，本轮结束后处理 */
const insert = async () => {
  const text = input.value.trim()
  if (!text || !streaming.value) return
  try {
    await insertAgentQuestion(text)
    input.value = ''
    queueCount.value += 1
    messages.value.push({ kind: 'assistant-plain', text: '📌 已插入问题（当前回答结束后处理）：' + text })
    scrollBottom()
  } catch (e) {
    errorText.value = e?.message || '插入失败'
  }
}

const confirm = async (action, approve) => {
  action.busy = true
  try {
    const result = await confirmPendingAction(action.operationId, approve)
    messages.value.push({ kind: 'assistant-plain', text: result || (approve ? '已执行' : '已取消') })
    pendingActions.value = pendingActions.value.filter(a => a.operationId !== action.operationId)
    scrollBottom()
  } catch (e) {
    action.busy = false
    errorText.value = e?.message || '操作失败'
  }
}

const batchBusy = ref(false)

const confirmAll = async () => {
  batchBusy.value = true
  for (const action of [...pendingActions.value]) {
    await confirm(action, true)
  }
  batchBusy.value = false
}

const rejectAll = async () => {
  batchBusy.value = true
  for (const action of [...pendingActions.value]) {
    await confirm(action, false)
  }
  batchBusy.value = false
}

const scrollBottom = () => {
  nextTick(() => {
    const el = chatBodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
</script>

<style lang="scss" scoped>
@use "@/styles/admin-mixins.scss" as *;

/* ===== 三栏工作台 ===== */
.agent-ide {
  display: grid;
  grid-template-columns: 268px minmax(0, 1fr);
  gap: 14px;
  height: calc(100vh - 112px);
  min-height: 600px;
  font-family: var(--admin-font-family);
  color: var(--admin-text);
}

.agent-ide.no-rail {
  grid-template-columns: 0 minmax(0, 1fr);
}

/* ===== 左侧：历史对话 ===== */
.ide-rail {
  min-width: 0;
  overflow: hidden;
  display: flex;
  @include admin-glass(0.72, 24px);
  border-radius: var(--admin-radius-2xl);
  box-shadow: var(--admin-shadow-sidebar), var(--admin-inset-highlight);
}

.rail-inner {
  width: 268px;
  min-width: 268px;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px 14px;
  box-sizing: border-box;
}

.rail-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.rail-orb {
  position: relative;
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--admin-grad-hero);
  border-radius: 14px;
  box-shadow: 0 8px 22px rgba(47, 91, 212, 0.32), inset 0 1px 0 rgba(255, 255, 255, 0.35);
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.5), transparent 58%);
  }

  svg {
    position: relative;
    z-index: 1;
  }
}

.rail-copy {
  min-width: 0;

  h2 {
    margin: 0;
    font-size: var(--admin-text-lg);
    font-weight: var(--admin-font-bold);
    line-height: 1.2;
    letter-spacing: -0.02em;
    color: var(--admin-text);
  }

  p {
    margin: 3px 0 0;
    font-size: 11px;
    line-height: 1.5;
    color: var(--admin-text-secondary);
  }
}

.rail-eyebrow {
  display: block;
  margin-bottom: 2px;
  font-family: var(--admin-font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--admin-primary-500);
}

.rail-new {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  height: 40px;
  margin-top: 14px;
  padding: 0 12px;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: var(--admin-grad-primary);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(47, 91, 212, 0.26);
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;

  kbd {
    margin-left: auto;
    padding: 2px 6px;
    font-family: var(--admin-font-mono);
    font-size: 9.5px;
    color: rgba(255, 255, 255, 0.85);
    background: rgba(255, 255, 255, 0.18);
    border: 1px solid rgba(255, 255, 255, 0.25);
    border-radius: 5px;
  }

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 12px 26px rgba(47, 91, 212, 0.34);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.rail-search {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  margin-top: 10px;
  padding: 0 11px;
  background: rgba(241, 245, 249, 0.85);
  border: 1px solid var(--admin-divider);
  border-radius: 10px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  svg {
    flex-shrink: 0;
    color: var(--admin-text-muted);
  }

  &:focus-within {
    border-color: var(--admin-primary-300);
    box-shadow: 0 0 0 3px rgba(89, 139, 253, 0.1);
  }

  input {
    flex: 1;
    min-width: 0;
    font-family: inherit;
    font-size: 12px;
    color: var(--admin-text);
    background: transparent;
    border: none;
    outline: none;

    &::placeholder {
      color: var(--admin-text-muted);
    }
  }
}

.rail-scroll {
  flex: 1;
  min-height: 0;
  margin-top: 12px;
  overflow-y: auto;
  overflow-x: hidden;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(89, 139, 253, 0.22);
    border-radius: 999px;
  }
}

.rail-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 30px 10px;
  font-size: 12px;
  color: var(--admin-text-muted);
}

.loading-dots {
  display: inline-flex;
  gap: 3px;

  i {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: var(--admin-primary-300);
    animation: agent-dot 1.2s infinite ease-in-out;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

.rail-group + .rail-group {
  margin-top: 14px;
}

.rail-group-label {
  padding: 0 4px 6px;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--admin-text-muted);
}

.rail-item {
  position: relative;
  display: block;
  width: 100%;
  padding: 9px 11px;
  font-family: inherit;
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 11px;
  cursor: pointer;
  transition: background-color 0.18s ease, border-color 0.18s ease;

  & + .rail-item {
    margin-top: 4px;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.9);
    border-color: var(--admin-primary-100);
  }

  &.active {
    background: var(--admin-primary-50);
    border-color: var(--admin-primary-200);

    .ri-q {
      color: var(--admin-primary-700);
    }
  }
}

.ri-time {
  font-family: var(--admin-font-mono);
  font-size: 9.5px;
  color: var(--admin-text-muted);
}

.ri-q {
  display: -webkit-box;
  margin: 3px 0 0;
  font-size: 12px;
  line-height: 1.55;
  color: var(--admin-text-secondary);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}

.ri-model {
  display: inline-block;
  max-width: 100%;
  margin-top: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 1px 7px;
  font-size: 9.5px;
  font-weight: 600;
  color: var(--admin-primary-700);
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(89, 139, 253, 0.16);
  border-radius: 999px;
}

.rail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 16px;
  text-align: center;

  .empty-icon {
    width: 44px;
    height: 44px;
    margin-bottom: 10px;
    display: grid;
    place-items: center;
    color: var(--admin-primary-400);
    background: var(--admin-primary-50);
    border-radius: 14px;
  }

  p {
    margin: 0;
    font-size: 12.5px;
    font-weight: 600;
    color: var(--admin-text-secondary);
  }

  span {
    margin-top: 4px;
    font-size: 10.5px;
    color: var(--admin-text-muted);
  }
}

.rail-foot {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--admin-divider);
}

.rail-status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 11px;
  color: var(--admin-text-muted);

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--admin-success);
    box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.14);
  }
}

.rail-quota {
  font-size: 11px;
  color: var(--admin-text-secondary);

  b {
    font-weight: 700;
    color: var(--admin-primary-700);
    font-variant-numeric: tabular-nums;
  }
}

/* ===== 中间：对话主区 ===== */
.ide-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.main-tabs {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 10px;
  @include admin-glass(0.7, 24px);
  border-radius: var(--admin-radius-xl);
  box-shadow: var(--admin-shadow-sm), var(--admin-inset-highlight);
}

.main-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  max-width: 46%;
  padding: 7px 12px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--admin-divider);
  border-radius: 12px;
  box-shadow: var(--admin-shadow-xs);
}

.tab-dot {
  flex-shrink: 0;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--admin-success);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.14);

  &.busy {
    background: var(--admin-accent-500);
    box-shadow: 0 0 0 3px rgba(53, 179, 201, 0.16);
    animation: live-pulse 1.4s ease-in-out infinite;
  }

  &.past {
    background: var(--admin-slate-300);
    box-shadow: none;
  }
}

@keyframes live-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}

.tab-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
  color: var(--admin-text);
}

.tab-sub {
  flex-shrink: 0;
  font-size: 10.5px;
  color: var(--admin-text-muted);
}

.tabs-spacer {
  flex: 1;
}

.tabs-model {
  display: flex;
  align-items: center;
  gap: 6px;
}

.model-picker {
  width: 196px;
}

:deep(.model-picker .el-select__wrapper) {
  min-height: 32px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  background-color: rgba(255, 255, 255, 0.9) !important;
  box-shadow: 0 0 0 1px rgba(203, 213, 225, 0.85) inset !important;
  transition: box-shadow var(--admin-transition-base), background-color var(--admin-transition-base);
}

:deep(.model-picker .el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--admin-primary-300) inset !important;
}

:deep(.model-picker .el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--admin-primary-400) inset, var(--admin-shadow-glow) !important;
}

:deep(.model-picker .el-select__selected-item) {
  font-weight: 600;
  font-size: 12px;
  color: var(--admin-text) !important;
}

.plan-badge {
  flex-shrink: 0;
  padding: 2px 8px;
  font-size: 9.5px;
  font-weight: 700;
  color: #047857;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.24);
  border-radius: 999px;
}

.effort-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  font-family: inherit;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--admin-accent-700);
  background: var(--admin-accent-50);
  border: 1px solid var(--admin-accent-100);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s ease;

  .effort-dot {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: var(--admin-primary-500);
    transition: background 0.2s ease;
  }

  &.deep .effort-dot {
    background: var(--admin-accent-500);
    box-shadow: 0 0 0 2px rgba(53, 179, 201, 0.14);
  }

  &:hover:not(:disabled) {
    background: var(--admin-accent-100);
    border-color: var(--admin-accent-200);
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.tab-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  color: var(--admin-text-muted);
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(203, 213, 225, 0.9);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover {
    color: var(--admin-primary-600);
    border-color: var(--admin-primary-300);
  }

  &.on {
    color: var(--admin-primary-700);
    background: var(--admin-primary-50);
    border-color: var(--admin-primary-200);
  }
}

/* ===== 待确认操作 ===== */
.pending-bar {
  flex-shrink: 0;
  padding: 14px 16px;
  background: linear-gradient(135deg, rgba(255, 247, 237, 0.95), rgba(255, 251, 235, 0.85));
  border: 1px solid rgba(245, 158, 11, 0.28);
  border-radius: var(--admin-radius-lg);
  box-shadow: 0 10px 26px rgba(245, 158, 11, 0.12);
  backdrop-filter: blur(18px) saturate(140%);
  -webkit-backdrop-filter: blur(18px) saturate(140%);
}

.pending-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.pending-badge {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  color: #B45309;
  background: rgba(245, 158, 11, 0.14);
  border: 1px solid rgba(245, 158, 11, 0.22);
  border-radius: 10px;
}

.pending-copy {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;

  strong {
    font-size: 13px;
    font-weight: 700;
    color: #92400E;
  }

  span {
    font-size: 11.5px;
    color: var(--admin-text-secondary);
  }
}

.pending-ops {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  margin-left: auto;
}

.link-ok,
.link-danger {
  height: 28px;
  padding: 0 12px;
  font-family: inherit;
  font-size: 11.5px;
  font-weight: 600;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.18s ease;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.link-ok {
  color: var(--admin-success-text);
  background: var(--admin-success-light);
  border-color: rgba(16, 185, 129, 0.2);

  &:hover:not(:disabled) {
    background: #DDF3E6;
    transform: translateY(-1px);
  }
}

.link-danger {
  color: var(--admin-error-text);
  background: var(--admin-error-light);
  border-color: rgba(244, 63, 94, 0.2);

  &:hover:not(:disabled) {
    background: #FBE0E3;
    transform: translateY(-1px);
  }
}

.pending-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.pending-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(245, 158, 11, 0.18);
  border-radius: 12px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    border-color: rgba(245, 158, 11, 0.4);
    box-shadow: 0 6px 16px rgba(245, 158, 11, 0.12);
  }

  &.strict {
    background: rgba(255, 241, 242, 0.75);
    border-color: rgba(244, 63, 94, 0.3);
  }
}

.pc-risk {
  flex-shrink: 0;
  padding: 2px 8px;
  font-size: 10px;
  font-weight: 700;
  color: #B45309;
  background: rgba(245, 158, 11, 0.12);
  border-radius: 999px;

  &.strict {
    color: var(--admin-error-text);
    background: rgba(244, 63, 94, 0.12);
  }
}

.pc-main {
  flex: 1;
  min-width: 0;

  strong {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 12.5px;
    color: var(--admin-text);
  }

  .pc-meta {
    display: block;
    margin-top: 1px;
    font-family: var(--admin-font-mono);
    font-size: 11px;
    color: var(--admin-text-muted);
  }
}

.pc-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.mini-btn {
  height: 28px;
  padding: 0 12px;
  font-family: inherit;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--admin-text-secondary);
  background: #fff;
  border: 1px solid var(--admin-border-strong);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover:not(:disabled) {
    color: var(--admin-primary-700);
    border-color: var(--admin-primary-300);
  }

  &.primary {
    color: #fff;
    background: var(--admin-grad-primary);
    border-color: transparent;
    box-shadow: 0 4px 12px rgba(47, 91, 212, 0.24);

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 8px 18px rgba(47, 91, 212, 0.3);
    }
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

/* ===== 对话壳 ===== */
.chat-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background-color: rgba(255, 255, 255, 0.72);
  background-image: linear-gradient(180deg, rgba(255, 255, 255, 0.5) 0%, rgba(255, 255, 255, 0) 42%);
  border: 1px solid var(--admin-glass-border);
  border-radius: var(--admin-radius-xl);
  box-shadow: var(--admin-shadow-md), var(--admin-inset-highlight);
  backdrop-filter: blur(26px) saturate(175%);
  -webkit-backdrop-filter: blur(26px) saturate(175%);
  overflow: hidden;
}

/* ===== 对话滚动区 ===== */
.chat-area {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 22px 22px 26px;
  background-image: radial-gradient(circle at 1px 1px, rgba(148, 163, 184, 0.16) 1px, transparent 0);
  background-size: 22px 22px;

  &::-webkit-scrollbar {
    width: 9px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(148, 163, 184, 0.35);
    border: 2px solid transparent;
    border-radius: 999px;
    background-clip: padding-box;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: rgba(122, 157, 254, 0.5);
    background-clip: padding-box;
  }
}

/* 欢迎区 */
.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  min-height: 100%;
  padding: 16px 12px;
  text-align: center;
}

.welcome-orb {
  position: relative;
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--admin-grad-hero);
  border-radius: 22px;
  box-shadow: 0 14px 34px rgba(47, 91, 212, 0.32), inset 0 1px 0 rgba(255, 255, 255, 0.4);

  svg {
    position: relative;
    z-index: 1;
    filter: drop-shadow(0 2px 6px rgba(15, 23, 42, 0.2));
  }
}

.orb-ring {
  position: absolute;
  inset: -9px;
  z-index: 0;
  border-radius: 28px;
  background: conic-gradient(from 0deg, rgba(122, 157, 254, 0.5), rgba(53, 179, 201, 0.5), rgba(244, 114, 182, 0.45), rgba(122, 157, 254, 0.5));
  filter: blur(9px);
  opacity: 0.65;
  animation: orb-spin 9s linear infinite;
}

@keyframes orb-spin {
  to { transform: rotate(360deg); }
}

.welcome-eyebrow {
  font-family: var(--admin-font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--admin-primary-500);
}

.welcome h3 {
  margin: 8px 0 0;
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.02em;
  background: var(--admin-grad-text);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.welcome-desc {
  max-width: 560px;
  margin: 8px 0 0;
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--admin-text-secondary);

  b {
    font-weight: 600;
    color: var(--admin-text);
  }
}

.cap-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
  margin-top: 14px;
}

.cap-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 600;
  color: var(--admin-text-secondary);
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--admin-divider);
  border-radius: 999px;

  i {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: var(--admin-primary-400);
  }

  &:nth-child(2) i { background: var(--admin-accent-400); }
  &:nth-child(3) i { background: var(--admin-cyan); }
  &:nth-child(4) i { background: #F472B6; }
}

.layout-hint {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
  margin-top: 10px;
  font-size: 10.5px;
  color: var(--admin-text-muted);

  span {
    padding: 3px 10px;
    background: rgba(255, 255, 255, 0.7);
    border: 1px dashed var(--admin-divider);
    border-radius: 999px;
  }
}

.quick-grid {
  width: 100%;
  max-width: 680px;
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quick-q {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 500;
  text-align: left;
  color: var(--admin-text-secondary);
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--admin-divider);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;

  .quick-index {
    flex-shrink: 0;
    font-family: var(--admin-font-mono);
    font-size: 10px;
    font-weight: 700;
    color: var(--admin-primary-400);
  }

  .quick-text {
    flex: 1;
    min-width: 0;
  }

  .quick-arrow {
    flex-shrink: 0;
    color: var(--admin-slate-300);
    transition: all 0.2s ease;
  }

  &:hover:not(:disabled) {
    color: var(--admin-primary-700);
    background: #fff;
    border-color: var(--admin-primary-200);
    transform: translateY(-2px);
    box-shadow: 0 10px 24px rgba(47, 91, 212, 0.1);

    .quick-arrow {
      color: var(--admin-primary-500);
      transform: translate(2px, -2px);
    }
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

/* 消息行 */
.row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
}

.row-user {
  justify-content: flex-end;
}

.row-ai {
  justify-content: flex-start;
}

.avatar {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  margin-top: 2px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
}

.avatar-ai {
  background: var(--admin-grad-hero);
  box-shadow: 0 4px 12px rgba(47, 91, 212, 0.28);
}

.bubble {
  max-width: min(78%, 620px);
  padding: 11px 15px;
  font-size: 13.5px;
  line-height: 1.78;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: 14px;
}

.row-user .bubble {
  margin-left: auto;
}

.user-bubble {
  color: #fff;
  background: linear-gradient(135deg, #3F72F0, #35B3C9 55%, #2A93A8);
  border-bottom-right-radius: 5px;
  box-shadow: 0 8px 22px rgba(47, 91, 212, 0.24);
}

.ai-bubble {
  color: var(--admin-text);
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--admin-divider);
  border-bottom-left-radius: 5px;
  box-shadow: var(--admin-shadow-xs);
}

/* ReAct 分段 */
.react-msg {
  align-items: flex-start;
}

.react-block {
  flex: 1;
  min-width: 0;
  max-width: min(82%, 700px);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.model-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 9px;
  font-family: var(--admin-font-mono);
  font-size: 10.5px;
  font-weight: 600;
  color: var(--admin-text-secondary);
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid var(--admin-divider);
  border-radius: 999px;
}

.think-line {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(53, 179, 201, 0.055);
  border-left: 2px solid rgba(53, 179, 201, 0.35);
  border-radius: 0 10px 10px 0;
  font-size: 12.5px;
}

.think-tag {
  flex-shrink: 0;
  height: fit-content;
  padding: 1px 7px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--admin-accent-700);
  background: rgba(53, 179, 201, 0.12);
  border-radius: 5px;
}

.think-text {
  font-style: italic;
  line-height: 1.65;
  color: var(--admin-text-muted);
}

.action-line {
  display: flex;
}

.action-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 11px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--admin-primary-700);
  background: var(--admin-primary-50);
  border: 1px solid rgba(89, 139, 253, 0.22);
  border-radius: 999px;

  .chip-dot {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: var(--admin-primary-500);
  }

  &.running .chip-dot {
    animation: live-pulse 1.2s ease-in-out infinite;
  }

  &.cached {
    color: var(--admin-text-secondary);
    background: var(--admin-slate-100);
    border-color: var(--admin-divider);

    .chip-dot { background: var(--admin-slate-400); }
  }

  &.rejected {
    color: var(--admin-error-text);
    background: var(--admin-error-light);
    border-color: rgba(244, 63, 94, 0.24);

    .chip-dot { background: var(--admin-error); }
  }
}

.draft-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 13px;
  font-size: 12px;
  font-weight: 600;
  color: #92400E;
  background: rgba(255, 247, 237, 0.92);
  border: 1px dashed rgba(245, 158, 11, 0.5);
  border-radius: 10px;
}

.ask-card {
  width: 100%;
  padding: 14px 16px;
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.95), rgba(245, 243, 255, 0.9));
  border: 1px solid rgba(122, 157, 254, 0.28);
  border-radius: 14px;
  box-shadow: var(--admin-shadow-xs);
}

.ask-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 11.5px;
  font-weight: 700;
  color: var(--admin-primary-700);
}

.ask-icon {
  width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: var(--admin-grad-primary);
  border-radius: 50%;
}

.ask-question {
  margin: 0 0 8px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--admin-text);
  white-space: pre-wrap;
}

.ask-suggestion {
  margin: 0 0 10px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--admin-text-secondary);

  span {
    display: inline-block;
    margin-right: 6px;
    padding: 1px 6px;
    font-size: 10px;
    font-weight: 700;
    color: #0E7490;
    background: rgba(34, 211, 238, 0.12);
    border-radius: 5px;
  }
}

.ask-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ask-opt {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  color: var(--admin-primary-700);
  background: #fff;
  border: 1px solid var(--admin-primary-200);
  border-radius: 9px;
  cursor: pointer;
  transition: all 0.18s ease;

  svg {
    color: var(--admin-primary-400);
  }

  &:hover:not(:disabled) {
    background: var(--admin-primary-50);
    border-color: var(--admin-primary-300);
    transform: translateY(-1px);
    box-shadow: var(--admin-shadow-sm);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.final-bubble {
  margin-top: 0;
}

.typing-dots {
  display: inline-flex;
  gap: 4px;
  padding: 9px 13px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid var(--admin-divider);
  border-radius: 12px;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--admin-primary-400);
    animation: agent-dot 1.2s infinite ease-in-out;

    &:nth-child(2) {
      background: var(--admin-accent-400);
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      background: #F472B6;
      animation-delay: 0.4s;
    }
  }
}

@keyframes agent-dot {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.35; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.chat-error {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  max-width: 640px;
  padding: 10px 13px;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--admin-error-text);
  background: var(--admin-error-light);
  border: 1px solid rgba(244, 63, 94, 0.24);
  border-radius: 10px;

  svg {
    flex-shrink: 0;
    margin-top: 1px;
  }
}

/* 历史回看横幅 */
.history-view-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 9px 14px;
  margin-bottom: 18px;
  font-size: 12px;
  color: var(--admin-primary-700);
  background: rgba(239, 246, 255, 0.92);
  border: 1px solid var(--admin-primary-200);
  border-radius: 12px;
  box-shadow: var(--admin-shadow-xs);

  .ghost-btn {
    height: 28px;
    padding: 0 10px;
    font-size: 11px;
  }
}

/* ===== 输入区 ===== */
.composer {
  flex-shrink: 0;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 10px;
  background-color: rgba(255, 255, 255, 0.78);
  background-image: linear-gradient(180deg, rgba(255, 255, 255, 0.5) 0%, rgba(255, 255, 255, 0) 42%);
  border: 1px solid var(--admin-glass-border);
  border-radius: var(--admin-radius-xl);
  box-shadow: var(--admin-shadow-md), var(--admin-inset-highlight);
  backdrop-filter: blur(26px) saturate(175%);
  -webkit-backdrop-filter: blur(26px) saturate(175%);
}

.composer-field {
  flex: 1;
  min-width: 0;
  padding: 8px 12px 6px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(203, 213, 225, 0.8);
  border-radius: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:focus-within {
    border-color: var(--admin-primary-300);
    box-shadow: 0 0 0 3px rgba(89, 139, 253, 0.12);
  }

  textarea {
    display: block;
    width: 100%;
    max-height: 120px;
    padding: 2px 0;
    font-family: inherit;
    font-size: 13.5px;
    line-height: 1.65;
    color: var(--admin-text);
    background: transparent;
    border: none;
    outline: none;
    resize: none;
    box-sizing: border-box;

    &::placeholder {
      color: var(--admin-text-muted);
    }

    &:disabled {
      opacity: 0.6;
    }
  }
}

.composer-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 4px;
}

.composer-hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10.5px;
  color: var(--admin-text-muted);

  kbd {
    padding: 1px 5px;
    font-family: var(--admin-font-mono);
    font-size: 9.5px;
    color: var(--admin-text-secondary);
    background: var(--admin-slate-100);
    border: 1px solid var(--admin-border);
    border-radius: 4px;
  }
}

.composer-count {
  font-size: 10.5px;
  color: var(--admin-text-muted);
  font-variant-numeric: tabular-nums;

  i {
    font-style: normal;
    color: var(--admin-slate-300);
  }
}

.stop-btn {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #FB7185, #E11D48);
  border: none;
  border-radius: 14px;
  cursor: pointer;
  animation: stop-pulse 1.8s ease-out infinite;
  transition: transform 0.2s ease;

  &:hover {
    transform: translateY(-2px);
  }
}

@keyframes stop-pulse {
  0% { box-shadow: 0 0 0 0 rgba(244, 63, 94, 0.38); }
  70%, 100% { box-shadow: 0 0 0 8px rgba(244, 63, 94, 0); }
}

.insert-btn {
  flex-shrink: 0;
  position: relative;
  height: 44px;
  padding: 0 16px;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--admin-primary-700);
  background: var(--admin-primary-50);
  border: 1px solid var(--admin-primary-200);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    background: var(--admin-primary-100);
    border-color: var(--admin-primary-300);
    transform: translateY(-1px);
    box-shadow: var(--admin-shadow-sm);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.insert-badge {
  display: inline-grid;
  place-items: center;
  min-width: 16px;
  height: 16px;
  margin-left: 6px;
  padding: 0 4px;
  font-size: 9.5px;
  font-weight: 700;
  color: #fff;
  background: var(--admin-primary-500);
  border-radius: 999px;
}

.send-btn {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--admin-grad-hero);
  border: none;
  border-radius: 14px;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(47, 91, 212, 0.3);
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 12px 26px rgba(47, 91, 212, 0.4);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
    box-shadow: none;
  }
}



/* ===== 输入框下方：模型配置行 ===== */
.composer-aux {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 12px;
  background-color: rgba(255, 255, 255, 0.78);
  background-image: linear-gradient(180deg, rgba(255, 255, 255, 0.45) 0%, rgba(255, 255, 255, 0) 40%);
  border: 1px solid var(--admin-glass-border);
  border-radius: var(--admin-radius-xl);
  box-shadow: var(--admin-shadow-sm), var(--admin-inset-highlight);
  backdrop-filter: blur(26px) saturate(175%);
  -webkit-backdrop-filter: blur(26px) saturate(175%);
}

.aux-config {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 28px;
  padding: 0 11px;
  font-family: inherit;
  font-size: 11px;
  font-weight: 600;
  color: var(--admin-text-secondary);
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid var(--admin-divider);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s ease;

  svg {
    color: var(--admin-primary-500);
  }

  &:hover {
    color: var(--admin-primary-700);
    border-color: var(--admin-primary-300);
    transform: translateY(-1px);
    box-shadow: var(--admin-shadow-sm);
  }
}

/* ===== 模型下拉面板：teleport 到 body，需要全局样式并压过站点暗色选择器 ===== */
:global(.el-select__popper.agent-model-popper.el-popper) {
  min-width: 300px !important;
  padding: 4px !important;
  color-scheme: light;
  background: rgba(255, 255, 255, 0.97) !important;
  border: 1px solid rgba(255, 255, 255, 0.95) !important;
  border-radius: 16px !important;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.15), 0 0 0 1px rgba(203, 213, 225, 0.5) !important;
  backdrop-filter: blur(26px) saturate(175%);
  -webkit-backdrop-filter: blur(26px) saturate(175%);
}

:global(.el-select__popper.agent-model-popper.el-popper .el-popper__arrow::before) {
  background: #fff !important;
  border-color: rgba(203, 213, 225, 0.6) !important;
}

:global(.el-select__popper.agent-model-popper .el-select-dropdown__header) {
  padding: 6px 6px 0;
}

:global(.el-select__popper.agent-model-popper .el-select-dropdown__footer) {
  padding: 12px 14px 12px;
  margin-top: 6px;
  border-top: 1px solid var(--admin-divider);
}

/* 面板顶部：计划行 + 密钥配置入口（对应例图第一行的可点击条目） */
.mp-plan {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 4px 6px 8px;
}

.mp-plan-name {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--admin-text);
}

.mp-plan-tag {
  padding: 1px 7px;
  font-size: 9.5px;
  font-weight: 600;
  color: #047857;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.24);
  border-radius: 999px;
}

.mp-plan-quota {
  margin-left: auto;
  font-family: var(--admin-font-mono);
  font-size: 10px;
  color: var(--admin-text-muted);
}

.mp-config {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  font-family: inherit;
  text-align: left;
  background: var(--admin-slate-50);
  border: 1px solid var(--admin-divider);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover {
    background: var(--admin-primary-50);
    border-color: var(--admin-primary-200);

    .mpc-arrow {
      color: var(--admin-primary-600);
      transform: translateX(2px);
    }
  }
}

.mpc-icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  color: var(--admin-primary-600);
  background: #fff;
  border: 1px solid var(--admin-divider);
  border-radius: 6px;
}

.mpc-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--admin-text);
}

.mpc-badge {
  margin-left: auto;
  padding: 1px 7px;
  font-size: 9.5px;
  font-weight: 600;
  color: var(--admin-text-muted);
  background: #fff;
  border: 1px solid var(--admin-divider);
  border-radius: 999px;
}

.mpc-arrow {
  flex-shrink: 0;
  color: var(--admin-slate-300);
  transition: all 0.18s ease;
}

.mp-group {
  padding: 10px 10px 4px;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--admin-text-muted);
}

:global(.el-select__popper.agent-model-popper.el-popper .el-select-dropdown__item) {
  height: 42px !important;
  line-height: 42px !important;
  margin: 1px 4px !important;
  padding: 0 12px !important;
  border-radius: 10px !important;
  color: var(--admin-text-secondary) !important;
}

:global(.el-select__popper.agent-model-popper.el-popper .el-select-dropdown__item.is-hovering) {
  background: var(--admin-primary-50) !important;
}

:global(.el-select__popper.agent-model-popper.el-popper .el-select-dropdown__item.is-selected) {
  background: var(--admin-primary-50) !important;
  color: var(--admin-primary-700) !important;
}

:global(.el-select__popper.agent-model-popper.el-popper .el-select-dropdown__item.is-selected .mo-provider) {
  color: var(--admin-primary-700) !important;
}

:global(.el-select__popper.agent-model-popper.el-popper .el-select-dropdown__item.is-disabled) {
  opacity: 0.45;
}

.model-option {
  display: flex;
  align-items: center;
  width: 100%;
}

.status-dot {
  flex-shrink: 0;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--admin-slate-300);

  &.ready {
    background: var(--admin-success);
    box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
  }
}

.mo-provider {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--admin-text);
}

.mo-tail {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  margin-left: auto;
}

.mo-model {
  max-width: 118px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--admin-font-mono);
  font-size: 10px;
  color: var(--admin-text-muted);
}

.mo-tag {
  flex-shrink: 0;
  padding: 1px 6px;
  font-size: 9.5px;
  font-weight: 600;
  line-height: 1.5;
  color: var(--admin-accent-700);
  background: var(--admin-accent-50);
  border: 1px solid var(--admin-accent-100);
  border-radius: 5px;

  &.muted {
    color: var(--admin-text-muted);
    background: var(--admin-slate-100);
    border-color: var(--admin-divider);
  }
}

.mo-check {
  flex-shrink: 0;
  color: var(--admin-primary-600);
}

/* 思考强度：下拉面板底部整行滑杆 */
.model-effort {
  padding: 2px 2px 0;
}

.me-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.me-label {
  font-size: 11.5px;
  color: var(--admin-text-muted);
}

.me-value {
  font-size: 12px;
  font-weight: 600;
  color: var(--admin-primary-700);
  transition: color 0.25s ease;
}

.me-kbd {
  margin-left: auto;
  padding: 1px 6px;
  font-family: var(--admin-font-mono);
  font-size: 9.5px;
  letter-spacing: 0.04em;
  color: var(--admin-text-muted);
  background: var(--admin-slate-100);
  border: 1px solid var(--admin-divider);
  border-radius: 5px;
}

.model-effort.deep .me-value {
  color: var(--admin-accent-600);
}

.model-effort.deep .me-scale span:last-child.on {
  color: var(--admin-accent-700);
}

.effort-slider {
  position: relative;
  height: 28px;
  margin-top: 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(52, 211, 153, 0.18), rgba(34, 211, 238, 0.18) 32%, rgba(89, 139, 253, 0.18) 64%, rgba(53, 179, 201, 0.18));
  box-shadow: inset 0 1px 3px rgba(15, 23, 42, 0.08);
  cursor: pointer;
  outline: none;
  user-select: none;

  &:hover .effort-slider-thumb {
    box-shadow: 0 3px 10px rgba(15, 23, 42, 0.3), 0 0 0 6px rgba(255, 255, 255, 0.6);
  }

  &:active .effort-slider-thumb {
    width: 24px;
    height: 24px;
    top: 2px;
  }

  &:focus-visible {
    box-shadow: 0 0 0 3px rgba(89, 139, 253, 0.2), inset 0 1px 3px rgba(15, 23, 42, 0.08);
  }

  &.locked {
    opacity: 0.55;
    pointer-events: none;
  }
}

.effort-slider-fill {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(90deg, #34D399 0%, #35B3C9 30%, #598BFD 62%, #35B3C9 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35);
  transform: scaleX(0.055);
  transform-origin: left center;
  will-change: transform;
  transition: transform 0.46s cubic-bezier(0.22, 0.61, 0.36, 1);
}

/* 轨道上的细点，呼应例图渐变条上的光点 */
.effort-slider-ticks {
  position: absolute;
  inset: 0;
  z-index: 1;
  border-radius: inherit;
  pointer-events: none;
  background-image: radial-gradient(circle at center, rgba(255, 255, 255, 0.85) 1.2px, transparent 1.4px);
  background-size: 16px 100%;
  background-position: 10px center;
  opacity: 0.45;
}

.effort-slider-thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  z-index: 2;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.28), 0 0 0 4px rgba(255, 255, 255, 0.5);
  will-change: left;
  transition: left 0.46s cubic-bezier(0.22, 0.61, 0.36, 1), width 0.18s ease, height 0.18s ease, box-shadow 0.3s ease;
}

.effort-slider.deep .effort-slider-fill {
  transform: scaleX(0.945);
}

.effort-slider.deep .effort-slider-thumb {
  left: calc(100% - 25px);
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.28), 0 0 0 4px rgba(255, 255, 255, 0.55);
}

.me-scale {
  display: flex;
  justify-content: space-between;
  margin-top: 7px;
  font-size: 9.5px;
  color: var(--admin-text-muted);

  span {
    transition: color 0.2s ease;
  }

  span.on {
    font-weight: 600;
    color: var(--admin-text-secondary);
  }
}

.mp-confirm {
  width: 100%;
  height: 32px;
  margin-top: 12px;
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: var(--admin-grad-primary);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 16px rgba(47, 91, 212, 0.28);
  }
}

/* ===== 响应式：窄屏终端/历史收为浮层 ===== */
@media (max-width: 1279px) {
  .agent-ide {
    grid-template-columns: 248px minmax(0, 1fr);
  }

  .agent-ide.no-rail {
    grid-template-columns: 0 minmax(0, 1fr);
  }

  .rail-inner {
    width: 248px;
    min-width: 248px;
  }
}

@media (max-width: 991px) {
  .agent-ide,
  .agent-ide.no-rail {
    grid-template-columns: minmax(0, 1fr);
    height: auto;
    min-height: calc(100vh - 140px);
  }

  .ide-rail {
    position: fixed;
    top: calc(var(--admin-header-height) + var(--admin-float-gap) * 2);
    left: var(--admin-float-gap);
    bottom: var(--admin-float-gap);
    width: 280px;
    z-index: 95;
    transform: translateX(calc(-100% - 24px));
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .ide-rail.open {
    transform: translateX(0);
  }

  .rail-inner {
    width: 280px;
    min-width: 280px;
  }

  .chat-shell {
    min-height: 420px;
  }

  .main-tab {
    max-width: 100%;
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }

  .bubble {
    max-width: 88%;
  }

  .react-block {
    max-width: 92%;
  }

  .pending-card {
    flex-wrap: wrap;
  }

  .pc-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .composer-hint {
    display: none;
  }

  .model-picker {
    width: 150px;
  }
}
</style>
