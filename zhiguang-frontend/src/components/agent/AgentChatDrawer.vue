<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    size="420px"
    class="agent-drawer"
    :append-to-body="true"
  >
    <template #header>
      <div class="agent-header">
        <div class="agent-avatar">🌟</div>
        <div class="agent-title">
          <strong>纸域空间导览助手</strong>
          <span class="agent-quota" v-if="quotaLeft >= 0">今日剩余 {{ quotaLeft }} 次</span>
        </div>
        <button type="button" class="agent-new-chat" title="新对话" @click="startNewChat">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5v14M5 12h14"/>
          </svg>
          新对话
        </button>
      </div>
    </template>

    <div class="agent-body" ref="bodyRef">
      <!-- 欢迎语（固定身份提示） -->
      <div v-if="messages.length === 0" class="agent-welcome">
        <div class="welcome-orb">🌟</div>
        <p class="welcome-copy">
          你好呀 🌟 我是纸域空间导览助手，只负责两件事：<strong>介绍平台功能</strong>、<strong>解答使用疑问</strong>。
          其余的精彩，交给你沉浸式探索～
        </p>
        <div class="quick-questions">
          <button
            v-for="q in quickQuestions"
            :key="q"
            type="button"
            class="quick-q"
            :disabled="streaming"
            @click="send(q)"
          >{{ q }}</button>
        </div>
      </div>

      <!-- 消息列表 -->
      <template v-for="(msg, index) in messages" :key="index">
        <div v-if="msg.role === 'user'" class="msg msg-user">
          <div class="bubble user-bubble">{{ msg.content }}</div>
        </div>
        <div v-else class="msg msg-assistant">
          <div class="assistant-meta" v-if="msg.toolName">
            <span class="tool-chip" :class="{ running: msg.toolRunning }">
              {{ msg.toolRunning ? '正在查询' : '已查询' }}：{{ toolLabel(msg.toolName) }}
            </span>
          </div>
          <div class="bubble assistant-bubble" :class="{ empty: !msg.content && msg.streaming }">
            <span v-if="!msg.content && msg.streaming" class="typing-dots"><i></i><i></i><i></i></span>
            <template v-else>{{ msg.content }}</template>
          </div>
          <div
            v-if="msg.role === 'assistant' && msg.content && !msg.streaming && msg.done"
            class="msg-actions"
          >
            <button
              type="button"
              class="fb-btn"
              :class="{ active: msg.feedback === 'LIKE' }"
              title="有帮助"
              @click="feedback(msg, 'LIKE')"
            >👍</button>
            <button
              type="button"
              class="fb-btn"
              :class="{ active: msg.feedback === 'DISLIKE' }"
              title="没帮助"
              @click="feedback(msg, 'DISLIKE')"
            >👎</button>
          </div>
        </div>
      </template>

      <!-- 错误提示 -->
      <div v-if="errorText" class="agent-error">{{ errorText }}</div>
    </div>

    <div class="agent-input-area">
      <textarea
        v-model="input"
        class="agent-input"
        rows="2"
        maxlength="500"
        :disabled="streaming"
        :placeholder="streaming ? '助手思考中…' : '问我纸域空间的功能…'"
        @keydown.enter.exact.prevent="onEnter"
      ></textarea>
      <button
        type="button"
        class="agent-send"
        :disabled="streaming || !input.trim()"
        @click="send()"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
        </svg>
      </button>
    </div>
    <div class="agent-foot">仅做功能介绍与解答 · 其余交给沉浸式探索</div>
  </el-drawer>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { agentChatStream, getAgentHistory, clearAgentHistory, getAgentQuota, sendAgentFeedback } from '@/api/agent'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const quickQuestions = [
  '纸域空间有哪些功能？',
  'AI 润色是什么？',
  '私信有什么规则？',
  '树洞是匿名的吗？'
]

const TOOL_LABELS = {
  get_feature_catalog: '平台功能目录',
  get_my_overview: '我的概览'
}
const toolLabel = (name) => TOOL_LABELS[name] || name

const messages = ref([])
const input = ref('')
const streaming = ref(false)
const errorText = ref('')
const conversationId = ref('')
const quotaLeft = ref(-1)
const bodyRef = ref(null)
let abortFn = null

watch(visible, (open) => {
  if (open) {
    restoreHistory()
    refreshQuota()
  } else if (abortFn) {
    abortFn()
    abortFn = null
    streaming.value = false
  }
})

const restoreHistory = async () => {
  errorText.value = ''
  try {
    const data = await getAgentHistory()
    conversationId.value = data?.conversationId || ''
    messages.value = (data?.messages || []).map(m => ({
      role: m.role,
      content: m.content,
      done: true
    }))
    scrollToBottom()
  } catch {
    messages.value = []
  }
}

const refreshQuota = async () => {
  try {
    const data = await getAgentQuota()
    quotaLeft.value = Number(data?.remaining ?? -1)
  } catch {
    quotaLeft.value = -1
  }
}

const startNewChat = async () => {
  if (streaming.value) return
  try { await clearAgentHistory() } catch {}
  conversationId.value = ''
  messages.value = []
  errorText.value = ''
}

const onEnter = (e) => {
  // 输入法组合中的回车不发送
  if (e.isComposing) return
  send()
}

const send = (preset) => {
  const content = (typeof preset === 'string' ? preset : input.value).trim()
  if (!content || streaming.value) return
  input.value = ''
  errorText.value = ''

  messages.value.push({ role: 'user', content, done: true })
  const assistantMsg = {
    role: 'assistant',
    content: '',
    streaming: true,
    done: false,
    toolName: '',
    toolRunning: false,
    feedback: '',
    msgId: 'm' + Date.now()
  }
  messages.value.push(assistantMsg)
  streaming.value = true
  scrollToBottom()

  abortFn = agentChatStream(
    {
      message: content,
      conversationId: conversationId.value || undefined,
      pageContext: { route: location.pathname }
    },
    {
      onStatus: () => {
        scrollToBottom()
      },
      onTool: (payload) => {
        assistantMsg.toolName = payload.name || ''
        assistantMsg.toolRunning = payload.status === 'running'
        scrollToBottom()
      },
      onDelta: (text) => {
        assistantMsg.content += text
        scrollToBottom()
      },
      onDone: (payload) => {
        assistantMsg.streaming = false
        assistantMsg.done = true
        streaming.value = false
        conversationId.value = payload?.conversationId || conversationId.value
        if (payload && payload.quotaLeft !== undefined) {
          quotaLeft.value = Number(payload.quotaLeft)
        }
        if (payload?.truncated) {
          assistantMsg.content += '\n（回答因长度限制被截断）'
        }
        scrollToBottom()
      },
      onError: (err) => {
        assistantMsg.streaming = false
        assistantMsg.done = true
        streaming.value = false
        errorText.value = err?.message || '助手暂时不可用，请稍后重试'
        if (err?.code === 'QUOTA_EXCEEDED' && quotaLeft.value >= 0) {
          quotaLeft.value = 0
        }
        scrollToBottom()
      }
    }
  )
}

const feedback = async (msg, rating) => {
  if (msg.feedback) return
  msg.feedback = rating
  try {
    await sendAgentFeedback({
      conversationId: conversationId.value,
      messageId: msg.msgId,
      rating
    })
  } catch {
    // 反馈失败不打扰用户
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    const el = bodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
</script>

<style lang="scss" scoped>
.agent-drawer {
  :deep(.el-drawer__header) {
    margin-bottom: 0;
    padding: 14px 18px;
    border-bottom: 1px solid var(--color-divider);
  }

  :deep(.el-drawer__body) {
    display: flex;
    flex-direction: column;
    padding: 0;
    overflow: hidden;
  }
}

.agent-header {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.agent-avatar {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  border-radius: 12px;
}

.agent-title {
  flex: 1;
  display: flex;
  flex-direction: column;

  strong {
    font-size: 15px;
    color: var(--color-text);
  }

  .agent-quota {
    font-size: 11.5px;
    color: var(--color-text-muted);
  }
}

.agent-new-chat {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  cursor: pointer;

  &:hover {
    color: var(--color-primary);
    border-color: rgba(124, 107, 255, 0.4);
  }
}

.agent-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--color-bg);
}

.agent-welcome {
  text-align: center;
  padding: 28px 10px 10px;
}

.welcome-orb {
  width: 56px;
  height: 56px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  border-radius: 50%;
  box-shadow: 0 6px 24px rgba(124, 107, 255, 0.35);
}

.welcome-copy {
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--color-text-secondary);
  margin-bottom: 18px;

  strong {
    color: var(--color-text);
  }
}

.quick-questions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-q {
  padding: 10px 14px;
  font-size: 13px;
  text-align: left;
  color: var(--color-text-secondary);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover:not(:disabled) {
    color: var(--color-primary);
    border-color: rgba(124, 107, 255, 0.4);
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.msg {
  display: flex;
  margin-bottom: 14px;

  &.msg-user {
    justify-content: flex-end;
  }

  &.msg-assistant {
    flex-direction: column;
  }
}

.bubble {
  max-width: 85%;
  padding: 10px 14px;
  font-size: 13.5px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: 14px;

  &.user-bubble {
    color: #fff;
    background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
    border-bottom-right-radius: 4px;
  }

  &.assistant-bubble {
    color: var(--color-text);
    background: var(--color-card);
    border: 1px solid var(--color-border);
    border-bottom-left-radius: 4px;

    &.empty {
      min-height: 38px;
      display: flex;
      align-items: center;
    }
  }
}

.assistant-meta {
  margin-bottom: 4px;
}

.tool-chip {
  display: inline-block;
  padding: 2px 10px;
  font-size: 11.5px;
  color: var(--color-text-muted);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 999px;

  &.running {
    color: var(--color-primary);
    border-color: rgba(124, 107, 255, 0.4);
  }
}

.msg-actions {
  display: flex;
  gap: 6px;
  margin-top: 4px;
}

.fb-btn {
  padding: 2px 6px;
  font-size: 12px;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  opacity: 0.5;
  transition: all 0.15s;

  &:hover {
    opacity: 1;
    background: var(--color-bg-elevated);
  }

  &.active {
    opacity: 1;
    background: rgba(124, 107, 255, 0.12);
  }
}

.typing-dots {
  display: inline-flex;
  gap: 4px;

  i {
    width: 6px;
    height: 6px;
    background: var(--color-text-muted);
    border-radius: 50%;
    animation: agent-dot 1.2s infinite ease-in-out;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes agent-dot {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.agent-error {
  margin-bottom: 14px;
  padding: 10px 12px;
  font-size: 12.5px;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.25);
  border-radius: 10px;
}

.agent-input-area {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 12px 14px;
  border-top: 1px solid var(--color-divider);
  background: var(--color-card);
}

.agent-input {
  flex: 1;
  padding: 9px 12px;
  font-size: 13.5px;
  font-family: inherit;
  color: var(--color-text);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  resize: none;

  &:focus {
    outline: none;
    border-color: rgba(124, 107, 255, 0.5);
  }

  &:disabled {
    opacity: 0.6;
  }
}

.agent-send {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover:not(:disabled) {
    box-shadow: 0 4px 16px rgba(124, 107, 255, 0.45);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.agent-foot {
  padding: 6px 14px 10px;
  font-size: 11px;
  text-align: center;
  color: var(--color-text-muted);
  background: var(--color-card);
}

/* 移动端全屏 */
@media (max-width: 768px) {
  .agent-drawer {
    :deep(.el-drawer) {
      width: 100% !important;
    }
  }
}
</style>
