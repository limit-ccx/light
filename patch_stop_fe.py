# -*- coding: utf-8 -*-
import io

base = r'D:\prdemo\light-note\zhiguang-frontend'

# ===== api =====
p = base + r'\src\api\adminAgent.js'
with io.open(p, 'r', encoding='utf-8') as f: s = f.read()
old = "export const getAgentModels = () =>"
new = """/** 停止当前生成 */
export const stopAgentChat = () =>
  request({ url: '/api/admin/agent/stop', method: 'post' })

/** 插入对话（生成进行中追加问题，本轮结束后处理） */
export const insertAgentQuestion = (message) =>
  request({ url: '/api/admin/agent/insert', method: 'post', data: { message } })

export const getAgentModels = () =>"""
assert old in s
s = s.replace(old, new, 1)
with io.open(p, 'w', encoding='utf-8', newline='') as f: f.write(s)
print('api stop/insert added')

# ===== AdminAgent.vue =====
p2 = base + r'\src\views\admin\AdminAgent.vue'
with io.open(p2, 'r', encoding='utf-8') as f: s2 = f.read()

# 1) import
old = '''  getPendingActions, confirmPendingAction, getAgentModels, getChatHistory
} from '@/api/adminAgent\''''
new = '''  getPendingActions, confirmPendingAction, getAgentModels, getChatHistory,
  stopAgentChat, insertAgentQuestion
} from '@/api/adminAgent\''''
assert old in s2
s2 = s2.replace(old, new, 1)

# 2) 输入区：streaming 时 停止+插入，否则 发送
old = '''    <footer class="chat-input">
      <textarea
        v-model="input"
        rows="2"
        maxlength="800"
        :disabled="streaming"
        placeholder="例：过一遍待审核的文章 / 今天的核心指标 / 树洞审核政策要点"
        @keydown.enter.exact.prevent="onEnter"
      ></textarea>
      <button type="button" class="send-btn" :disabled="streaming || !input.trim()" @click="send()">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
        </svg>
      </button>
    </footer>'''
new = '''    <footer class="chat-input">
      <textarea
        v-model="input"
        rows="2"
        maxlength="800"
        placeholder="例：过一遍待审核的文章 / 今天的核心指标 / 树洞审核政策要点"
        @keydown.enter.exact.prevent="onEnter"
      ></textarea>
      <!-- 生成中：插入问题 -->
      <button
        v-if="streaming"
        type="button"
        class="insert-btn"
        :disabled="!input.trim()"
        title="当前回答结束后处理这个问题"
        @click="insert"
      >插入</button>
      <!-- 生成中：停止 -->
      <button v-if="streaming" type="button" class="stop-btn" title="停止生成" @click="stop">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
          <rect x="6" y="6" width="12" height="12" rx="2"/>
        </svg>
      </button>
      <!-- 空闲：发送 -->
      <button v-if="!streaming" type="button" class="send-btn" :disabled="!input.trim()" @click="send()">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
        </svg>
      </button>
    </footer>'''
assert old in s2
s2 = s2.replace(old, new, 1)

# 3) script：stop / insert
old = '''const confirm = async (action, approve) => {'''
new = '''/** 停止生成：后端置中断标志 + 本地断流；已生成内容保留，可继续追问 */
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
    messages.value.push({ kind: 'assistant-plain', text: '📌 已插入问题（当前回答结束后处理）：' + text })
    scrollBottom()
  } catch (e) {
    errorText.value = e?.message || '插入失败'
  }
}

const confirm = async (action, approve) => {'''
assert old in s2
s2 = s2.replace(old, new, 1)

# 4) 样式
old = '''.send-btn {'''
new = '''.stop-btn {
  width: 40px; height: 40px;
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  background: #ef4444;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  animation: stop-pulse 1.2s infinite;
  &:hover { background: #dc2626; }
}

@keyframes stop-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(239, 68, 68, 0); }
}

.insert-btn {
  flex-shrink: 0;
  padding: 0 14px;
  height: 40px;
  font-size: 13px;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.08);
  border: 1px solid rgba(37, 99, 235, 0.35);
  border-radius: 12px;
  cursor: pointer;
  &:hover:not(:disabled) { background: rgba(37, 99, 235, 0.15); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.send-btn {'''
assert old in s2
s2 = s2.replace(old, new, 1)

with io.open(p2, 'w', encoding='utf-8', newline='') as f: f.write(s2)
print('frontend stop/insert done')
