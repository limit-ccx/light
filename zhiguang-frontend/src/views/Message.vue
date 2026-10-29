<template>
  <div class="message-page">
    <Header />
    <main class="main-content">
      <div class="container">
        <header class="page-head">
          <div class="page-head-copy">
            <span class="page-eyebrow"><i></i>Messages</span>
            <h1>私信</h1>
            <p>和喜欢的创作者保持联系，消息会实时送达。</p>
          </div>
          <div class="page-head-meta">
            <span class="meta-chip"><b>{{ conversations.length }}</b> 个会话</span>
            <span v-if="totalUnread > 0" class="meta-chip is-accent"><b>{{ totalUnread }}</b> 条未读</span>
            <button type="button" class="new-chat-btn" @click="showNewConversationDialog">
              <el-icon><Plus /></el-icon>
              新对话
            </button>
          </div>
        </header>

        <div class="message-shell">
          <!-- 左侧会话列表 -->
          <aside class="message-sidebar" :class="{ 'mobile-hidden': mobileShowChat }">
            <div class="sidebar-head">
              <div>
                <span class="sidebar-eyebrow">Inbox</span>
                <h2>会话</h2>
              </div>
              <button type="button" class="compose-btn" aria-label="发起新对话" @click="showNewConversationDialog">
                <el-icon><Plus /></el-icon>
              </button>
            </div>

            <div class="sidebar-search">
              <el-input
                v-model="globalSearchKeyword"
                placeholder="搜索全部消息..."
                clearable
                class="mail-search"
                @clear="clearGlobalSearch"
                @input="onGlobalSearchInput"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>

            <div class="conversation-list-wrap">
              <!-- 全局搜索结果 -->
              <div v-if="globalSearchKeyword" class="search-results-panel">
                <div class="search-panel-header">
                  <span>全局搜索结果</span>
                  <button type="button" class="link-btn" @click="clearGlobalSearch">清除</button>
                </div>
                <div v-if="globalSearchLoading" class="search-state">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  搜索中...
                </div>
                <div v-else-if="globalSearchError" class="search-state">
                  搜索失败
                  <button type="button" class="link-btn" @click="doGlobalSearch">重试</button>
                </div>
                <template v-else>
                  <div v-if="globalSearchResults.length === 0" class="search-state">没有找到相关消息</div>
                  <button
                    v-for="result in globalSearchResults"
                    :key="result.id"
                    type="button"
                    class="search-result-item"
                    @click="jumpToSearchResult(result)"
                  >
                    <img :src="getPartner(result).avatar || defaultAvatar" class="search-result-avatar" alt="" />
                    <div class="search-result-info">
                      <div class="search-result-name">{{ getPartner(result).username }}</div>
                      <div class="search-result-content" v-html="highlight(result.content, globalSearchKeyword)"></div>
                      <div class="search-result-time">{{ formatTime(result.createdAt) }}</div>
                    </div>
                  </button>
                </template>
              </div>

              <!-- 会话列表 -->
              <div v-else class="conversation-list">
                <div v-if="loadingConversations" class="conv-skeleton">
                  <div v-for="n in 4" :key="n" class="conv-skeleton-row">
                    <span class="sk-avatar"></span>
                    <span class="sk-lines"><i></i><i></i></span>
                  </div>
                </div>
                <div v-else-if="conversationsError" class="conversation-state">
                  <span class="state-orb"><el-icon><Warning /></el-icon></span>
                  <p>会话加载失败</p>
                  <button type="button" class="state-btn" @click="loadConversations">重新加载</button>
                </div>
                <template v-else>
                  <button
                    v-for="conv in conversations"
                    :key="getPartner(conv).id || conv.id"
                    type="button"
                    class="conversation-item"
                    :class="{ active: currentPartner?.id === getPartner(conv).id }"
                    @click="selectConversation(conv)"
                  >
                    <span class="conv-avatar-wrap">
                      <img :src="getPartner(conv).avatar || defaultAvatar" class="conv-avatar" alt="" />
                      <span class="conv-online-status"></span>
                    </span>
                    <span class="conv-info">
                      <span class="conv-name-row">
                        <span class="conv-name">{{ getPartner(conv).username }}</span>
                        <span v-if="conv.mutualFollow === false" class="conv-tag">待互关</span>
                        <span class="conv-time">{{ formatConversationTime(conv.createdAt) }}</span>
                      </span>
                      <span class="conv-preview">
                        <em v-if="isSelf(conv)">你：</em>{{ conv.content }}
                      </span>
                    </span>
                    <span v-if="hasUnread(conv)" class="conv-unread">{{ getUnreadCount(conv) }}</span>
                  </button>

                  <div v-if="conversations.length === 0" class="inbox-empty">
                    <span class="state-orb"><el-icon><ChatDotRound /></el-icon></span>
                    <h4>还没有私信</h4>
                    <p>找到喜欢的创作者，发起第一段对话吧。</p>
                    <button type="button" class="state-btn" @click="showNewConversationDialog">
                      <el-icon><Plus /></el-icon>
                      发起新对话
                    </button>
                  </div>
                </template>
              </div>
            </div>

            <footer class="sidebar-foot">
              <span class="live-dot"></span>
              消息实时同步
            </footer>
          </aside>

          <!-- 右侧聊天窗口 -->
          <section class="message-main" :class="{ 'mobile-hidden': !mobileShowChat && isMobile }">
            <div v-if="!currentPartner" class="message-placeholder">
              <div class="placeholder-orbit" aria-hidden="true">
                <span class="po-ring po-ring-1"></span>
                <span class="po-ring po-ring-2"></span>
                <span class="po-core">
                  <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                    <path d="M8 9h8M8 13h5" />
                  </svg>
                </span>
              </div>
              <h3>选择一个对话</h3>
              <p>从左侧会话继续聊天，或发起一段新的对话。</p>
              <button type="button" class="placeholder-action" @click="showNewConversationDialog">
                <el-icon><Plus /></el-icon>
                发起新对话
              </button>
            </div>

            <template v-else>
              <header class="chat-head">
                <div class="chat-person">
                  <button
                    v-if="isMobile"
                    type="button"
                    class="chat-back"
                    aria-label="返回会话列表"
                    @click="mobileShowChat = false"
                  >
                    <el-icon><ArrowLeft /></el-icon>
                  </button>
                  <router-link :to="`/user/${currentPartner.id}`" class="chat-person-link">
                    <span class="chat-avatar-wrap">
                      <img :src="currentPartner.avatar || defaultAvatar" class="chat-avatar" alt="" />
                      <span class="chat-online"></span>
                    </span>
                    <span class="chat-person-copy">
                      <strong>{{ currentPartner.username }}</strong>
                      <small><i></i>在线</small>
                    </span>
                  </router-link>
                </div>
                <div class="chat-actions">
                  <button
                    type="button"
                    class="icon-btn"
                    :class="{ active: convSearchVisible }"
                    title="搜索当前对话"
                    aria-label="搜索当前对话"
                    @click="toggleConvSearch"
                  >
                    <el-icon><Search /></el-icon>
                  </button>
                </div>
              </header>

              <!-- 当前对话搜索面板 -->
              <div v-if="convSearchVisible" class="conv-search-panel">
                <div class="conv-search-input-row">
                  <el-input
                    v-model="convSearchKeyword"
                    placeholder="搜索当前对话..."
                    clearable
                    @clear="clearConvSearch"
                    @input="onConvSearchInput"
                  >
                    <template #prefix>
                      <el-icon><Search /></el-icon>
                    </template>
                  </el-input>
                  <button type="button" class="icon-btn" aria-label="关闭搜索" @click="toggleConvSearch">
                    <el-icon><Close /></el-icon>
                  </button>
                </div>
                <div v-if="convSearchLoading" class="search-state">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  搜索中...
                </div>
                <div v-else-if="convSearchError" class="search-state">
                  搜索失败
                  <button type="button" class="link-btn" @click="doConvSearch">重试</button>
                </div>
                <div v-else-if="convSearchKeyword && convSearchResults.length === 0" class="search-state">
                  没有找到相关消息
                </div>
                <button
                  v-for="msg in convSearchResults"
                  :key="msg.id"
                  type="button"
                  class="search-result-item is-message"
                  @click="jumpToConvMessage(msg)"
                >
                  <div class="search-result-info">
                    <div class="search-result-content" v-html="highlight(msg.content, convSearchKeyword)"></div>
                    <div class="search-result-time">{{ formatTime(msg.createdAt) }}</div>
                  </div>
                </button>
              </div>

              <div v-if="messagesError" class="messages-error">
                消息加载失败
                <button type="button" class="state-btn small" @click="reloadCurrentMessages">重新加载</button>
              </div>

              <div class="message-list" ref="messageListRef" @scroll="handleScroll">
                <div v-if="hasMore" class="load-more-bar">
                  <button v-if="!loadingMore && !loadingMoreError" type="button" class="load-more-btn" @click="loadMoreMessages">
                    加载更早消息
                  </button>
                  <span v-else-if="loadingMore" class="load-more-hint">
                    <el-icon class="is-loading"><Loading /></el-icon>
                    加载中...
                  </span>
                  <span v-else-if="loadingMoreError" class="load-more-hint is-error">
                    加载失败
                    <button type="button" class="link-btn" @click="loadMoreMessages">重试</button>
                  </span>
                </div>

                <div v-if="loadingMessages && !messages.length" class="chat-loading">
                  <div v-for="n in 3" :key="n" class="msg-skeleton" :class="{ self: n === 2 }"></div>
                </div>

                <template v-for="(msg, index) in messages" :key="msg.id || msg.clientMessageId">
                  <div v-if="shouldShowDateDivider(index)" class="date-divider">
                    <span class="date-divider-line"></span>
                    <span class="date-divider-text">{{ formatDateDivider(msg.createdAt) }}</span>
                    <span class="date-divider-line"></span>
                  </div>

                  <div
                    :id="`msg-${msg.id}`"
                    class="message-item"
                    :class="{ self: isSelf(msg) }"
                  >
                    <img :src="msg.sender?.avatar || defaultAvatar" class="msg-avatar" alt="" />
                    <div class="msg-content">
                      <div class="msg-bubble">{{ msg.content }}</div>
                      <div class="msg-meta">
                        <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
                        <span v-if="isSelf(msg)" class="msg-status">
                          <el-icon v-if="msg.status === 'SENDING'" class="is-loading"><Loading /></el-icon>
                          <span v-else-if="msg.status === 'FAILED'" class="status-failed" @click="retryMessage(msg)">
                            发送失败，点击重试
                          </span>
                          <template v-else>
                            <span class="status-read" :class="{ unread: !msg.isRead }">
                              <el-icon v-if="msg.isRead"><Check /></el-icon>
                              <el-icon v-else><Clock /></el-icon>
                              {{ getReadStatusText(msg) }}
                            </span>
                          </template>
                        </span>
                        <button
                          v-if="isSelf(msg) && msg.status !== 'SENDING' && msg.id && !String(msg.id).startsWith('temp-')"
                          type="button"
                          class="msg-delete-btn"
                          title="删除消息"
                          aria-label="删除消息"
                          @click="handleDeleteMessage(msg.id)"
                        >
                          <el-icon><Close /></el-icon>
                        </button>
                      </div>
                    </div>
                  </div>
                </template>

                <div v-if="!messages.length && !loadingMessages && !messagesError" class="chat-empty">
                  <span class="chat-empty-icon"><el-icon><ChatDotRound /></el-icon></span>
                  <p>还没有消息，先打个招呼吧。</p>
                </div>

                <div v-if="partnerTyping" class="typing-indicator">
                  <img :src="currentPartner?.avatar || defaultAvatar" class="typing-avatar" alt="" />
                  <div class="typing-bubble">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </div>
              </div>

              <div class="message-input-area">
                <!-- 未互关且打招呼额度已用完：锁定输入并引导互关 -->
                <div v-if="!chatStatus.canSend" class="composer-lock">
                  <el-icon><Lock /></el-icon>
                  <span>你们还未互相关注，只能发送一条消息。互相关注成为好友后即可继续畅聊</span>
                  <router-link :to="'/user/' + currentPartner?.id" class="lock-link">去 TA 的主页</router-link>
                </div>
                <div class="composer" :class="{ 'composer-disabled': !chatStatus.canSend }">
                  <el-input
                    v-model="inputContent"
                    type="textarea"
                    maxlength="2000"
                    show-word-limit
                    :rows="2"
                    :disabled="!chatStatus.canSend"
                    :placeholder="chatStatus.canSend ? '输入消息… (Ctrl + Enter 发送)' : '互相关注后才能继续聊天'"
                    class="composer-input"
                    @keyup.enter.ctrl="sendMessage"
                  />
                  <div class="composer-foot">
                    <span v-if="chatStatus.canSend">Ctrl + Enter 发送</span>
                    <span v-else class="composer-hint">非好友仅可发送一条消息</span>
                    <button
                      type="button"
                      class="send-btn"
                      :disabled="!inputContent.trim() || !currentPartner || !chatStatus.canSend"
                      @click="sendMessage"
                    >
                      <el-icon><Promotion /></el-icon>
                      <span>发送</span>
                    </button>
                  </div>
                </div>
              </div>
            </template>
          </section>
        </div>
      </div>
    </main>
    <Footer />

    <!-- 新对话对话框 -->
    <el-dialog
      v-model="newConversationVisible"
      title="发起新对话"
      width="440px"
      class="new-chat-dialog"
    >
      <p class="dialog-intro">从好友列表发起聊天，或搜索用户名给创作者打个招呼（互关前仅可发送一条消息）。</p>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户名..."
        class="dialog-search"
        clearable
        @input="handleSearchUsers"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <div v-if="!searchKeyword.trim()" class="user-search-list">
        <div v-if="friendList.length" class="friend-group-title">我的好友</div>
        <button
          v-for="user in friendList"
          :key="'friend-' + user.id"
          type="button"
          class="user-search-item"
          @click="startConversation(user)"
        >
          <img :src="user.avatar || defaultAvatar" class="user-avatar" alt="" />
          <span class="user-search-copy">
            <strong>{{ user.username }}</strong>
            <small>好友 · 直接聊天</small>
          </span>
          <span class="user-search-arrow">→</span>
        </button>
        <p v-if="!friendList.length" class="friend-empty">
          还没有互相关注的好友，先去关注感兴趣的创作者吧
        </p>
      </div>
      <div v-else class="user-search-list">
        <button
          v-for="user in searchedUsers"
          :key="user.id"
          type="button"
          class="user-search-item"
          @click="startConversation(user)"
        >
          <img :src="user.avatar || defaultAvatar" class="user-avatar" alt="" />
          <span class="user-search-copy">
            <strong>{{ user.username }}</strong>
            <small>发起私信</small>
          </span>
          <span class="user-search-arrow">→</span>
        </button>
        <div v-if="searchedUsers.length === 0 && searchKeyword" class="user-search-empty">
          未找到匹配的用户
        </div>
        <div v-else-if="!searchKeyword" class="user-search-empty">
          输入用户名开始搜索
        </div>
      </div>
    </el-dialog>
  </div>
</template>


<script setup>
import {
  ref,
  computed,
  onMounted,
  onUnmounted,
  nextTick
} from 'vue'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'
import {
  Lock,
  Plus, Search, ArrowLeft, Close, Loading, Check, Clock, Promotion, Warning, ChatDotRound
} from '@element-plus/icons-vue'
import { useMessageStream } from '@/composables/useMessageStream'
import {
  getConversationList,
  getConversation,
  getMessagesBefore,
  sendMessage as apiSendMessage,
  markConversationAsRead,
  getFriendList,
  getChatStatus,
  searchUsers,
  searchMessages,
  searchConversationMessages,
  deleteMessage
} from '@/api/message'

const PAGE_SIZE = 20
const authStore = useAuthStore()
const currentUserId = computed(() => authStore.user?.id)
const defaultAvatar = 'https://www.w3schools.com/w3images/avatar2.png'

const conversations = ref([])
const currentPartner = ref(null)
const messages = ref([])
const inputContent = ref('')
const messageListRef = ref(null)

const loadingConversations = ref(false)
const conversationsError = ref(false)
const loadingMessages = ref(false)
const messagesError = ref(false)
const loadingMore = ref(false)
const loadingMoreError = ref(false)
const hasMore = ref(false)
const preloadedOlder = ref([])

const isMobile = ref(window.innerWidth <= 768)
const mobileShowChat = ref(false)
const partnerTyping = ref(false)
let partnerTypingTimer = null

// 新对话
const newConversationVisible = ref(false)
const searchKeyword = ref('')
const searchedUsers = ref([])
// 互关聊天规则：未互关只能发一条打招呼消息
const chatStatus = ref({ mutualFollow: true, canSend: true })
const friendList = ref([])
let chatStatusPartnerId = null
let userSearchTimer = null

// 全局消息搜索
const globalSearchKeyword = ref('')
const globalSearchResults = ref([])
const globalSearchLoading = ref(false)
const globalSearchError = ref(false)
let globalSearchTimer = null

// 当前对话搜索
const convSearchVisible = ref(false)
const convSearchKeyword = ref('')
const convSearchResults = ref([])
const convSearchLoading = ref(false)
const convSearchError = ref(false)
let convSearchTimer = null

const getPartner = (msg) => {
  if (!msg) return {}
  if (msg.partner) return msg.partner
  const sender = msg.sender || {}
  const receiver = msg.receiver || {}
  const senderId = sender.id ?? msg.senderId
  const receiverId = receiver.id ?? msg.receiverId
  return senderId === currentUserId.value
    ? (receiver || {})
    : (sender || {})
}

const hasUnread = (conv) => {
  // 新会话 DTO 携带服务端统计的真实未读数；旧结构兜底 0（此前兜底 1 导致红点失真）
  return Number(conv.unreadCount ?? conv.unread ?? 0) > 0
}

const isSelf = (msg) => {
  if (msg.lastMessageFromMe !== undefined) return !!msg.lastMessageFromMe
  const senderId = msg.sender?.id ?? msg.senderId
  return senderId === currentUserId.value
}

const generateUUID = () => {
  if (crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatConversationTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = (now - date) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 604800) return `${Math.floor(diff / 86400)}天前`
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const formatDateDivider = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const msgDay = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const diffDays = (today - msgDay) / 86400000
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) {
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return weekdays[date.getDay()]
  }
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

const shouldShowDateDivider = (index) => {
  if (index === 0) return true
  const current = new Date(messages.value[index]?.createdAt)
  const prev = new Date(messages.value[index - 1]?.createdAt)
  if (isNaN(current.getTime()) || isNaN(prev.getTime())) return false
  return (
    current.getFullYear() !== prev.getFullYear() ||
    current.getMonth() !== prev.getMonth() ||
    current.getDate() !== prev.getDate()
  )
}

const getReadStatusText = (msg) => {
  if (!isSelf(msg)) return ''
  if (msg.status === 'SENDING') return ''
  if (msg.status === 'FAILED') return ''
  return msg.isRead ? '已读' : '未读'
}

const getUnreadCount = (conv) => {
  const count = Number(conv.unreadCount ?? conv.unread ?? 0)
  return count > 99 ? '99+' : count
}

const totalUnread = computed(() =>
  conversations.value.reduce((sum, conv) => {
    if (!hasUnread(conv)) return sum
    const raw = Number(conv.unreadCount ?? conv.unread ?? 0)
    return sum + (Number.isFinite(raw) && raw > 0 ? raw : 1)
  }, 0)
)

const triggerPartnerTyping = () => {
  partnerTyping.value = true
  if (partnerTypingTimer) clearTimeout(partnerTypingTimer)
  partnerTypingTimer = setTimeout(() => {
    partnerTyping.value = false
  }, 2000)
}

const escapeHtml = (value) =>
  String(value).replace(/[&<>"']/g, (char) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[char]))

const highlight = (text, keyword) => {
  if (!text) return ''
  const escapedText = escapeHtml(text)
  if (!keyword) return escapedText
  const safe = escapeHtml(keyword).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const reg = new RegExp(`(${safe})`, 'gi')
  return escapedText.replace(reg, '<mark>$1</mark>')
}

const scrollToBottom = () => {
  nextTick(() => {
    const el = messageListRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

const preserveScroll = (callback) => {
  const el = messageListRef.value
  if (!el) {
    callback()
    return
  }
  const oldHeight = el.scrollHeight
  const oldTop = el.scrollTop
  callback()
  nextTick(() => {
    const newEl = messageListRef.value
    if (newEl) {
      newEl.scrollTop = oldTop + (newEl.scrollHeight - oldHeight)
    }
  })
}

const emitUnreadRefresh = () => {
  window.dispatchEvent(new CustomEvent('unread-refresh', { bubbles: true }))
}

const loadConversations = async () => {
  loadingConversations.value = true
  conversationsError.value = false
  try {
    const data = await getConversationList()
    conversations.value = (data || []).map(c => ({
      ...c,
      // 兼容模板既有字段（新 DTO：lastMessagePreview/lastMessageAt）
      createdAt: c.lastMessageAt ?? c.createdAt,
      content: c.lastMessagePreview ?? c.content
    }))
  } catch {
    conversationsError.value = true
    conversations.value = []
  } finally {
    loadingConversations.value = false
  }
}

const selectConversation = async (conv) => {
  currentPartner.value = getPartner(conv)
  mobileShowChat.value = true
  refreshChatStatus(currentPartner.value.id)
  if (convSearchVisible.value) {
    convSearchVisible.value = false
    clearConvSearch()
  }
  await loadInitialMessages(currentPartner.value.id)
  await markReadAndRefresh(currentPartner.value.id)
}

const refreshChatStatus = async (partnerId) => {
  if (!partnerId) return
  chatStatusPartnerId = partnerId
  chatStatus.value = { mutualFollow: true, canSend: true }
  try {
    const status = await getChatStatus(partnerId)
    if (chatStatusPartnerId === partnerId) {
      chatStatus.value = {
        mutualFollow: !!status?.mutualFollow,
        canSend: status?.canSend !== false
      }
    }
  } catch {
    // 状态获取失败不禁用输入（默认放行，由后端最终校验）
  }
}

const markReadAndRefresh = async (partnerId) => {
  try {
    await markConversationAsRead(partnerId)
    // 本地清零会话未读，不再全量重载会话列表
    const conv = conversations.value.find(c => getPartner(c)?.id === partnerId)
    if (conv) conv.unreadCount = 0
    emitUnreadRefresh()
  } catch {}
}

const reloadCurrentMessages = () => {
  if (!currentPartner.value) return
  loadInitialMessages(currentPartner.value.id)
}

const loadInitialMessages = async (partnerId) => {
  messages.value = []
  preloadedOlder.value = []
  hasMore.value = false
  loadingMessages.value = true
  messagesError.value = false
  try {
    const data = await getConversation(partnerId, { page: 0, size: PAGE_SIZE })
    messages.value = (data?.list || []).reverse()
    hasMore.value = (data?.list || []).length === PAGE_SIZE
    scrollToBottom()
    prefetchOlderMessages()
  } catch {
    messagesError.value = true
    messages.value = []
  } finally {
    loadingMessages.value = false
  }
}

const prefetchOlderMessages = async () => {
  if (!hasMore.value || preloadedOlder.value.length > 0 || loadingMore.value || !currentPartner.value) return
  const oldest = messages.value[0]
  if (!oldest) return
  loadingMore.value = true
  loadingMoreError.value = false
  try {
    const data = await getMessagesBefore(currentPartner.value.id, oldest.createdAt, PAGE_SIZE)
    preloadedOlder.value = (data?.list || []).reverse()
  } catch {
    // 静默失败，不打扰用户，用户滚动到顶部时可重试
  } finally {
    loadingMore.value = false
  }
}

const applyPreloadedMessages = () => {
  if (preloadedOlder.value.length === 0) return
  preserveScroll(() => {
    messages.value.unshift(...preloadedOlder.value)
    preloadedOlder.value = []
  })
}

const loadMoreMessages = async () => {
  if (loadingMore.value || !hasMore.value || !currentPartner.value) return

  if (preloadedOlder.value.length > 0) {
    applyPreloadedMessages()
    prefetchOlderMessages()
    return
  }

  const oldest = messages.value[0]
  if (!oldest) return

  loadingMore.value = true
  loadingMoreError.value = false
  try {
    const data = await getMessagesBefore(currentPartner.value.id, oldest.createdAt, PAGE_SIZE)
    const list = (data?.list || []).reverse()
    if (list.length > 0) {
      preserveScroll(() => {
        messages.value.unshift(...list)
      })
      hasMore.value = list.length === PAGE_SIZE
      prefetchOlderMessages()
    } else {
      hasMore.value = false
    }
  } catch {
    loadingMoreError.value = true
  } finally {
    loadingMore.value = false
  }
}

const handleScroll = () => {
  const el = messageListRef.value
  if (!el) return
  if (el.scrollTop <= 30 && hasMore.value && !loadingMessages.value) {
    loadMoreMessages()
  }
}

const createTempMessage = (content, clientMessageId) => {
  return {
    id: `temp-${clientMessageId}`,
    clientMessageId,
    content,
    status: 'SENDING',
    sender: {
      id: currentUserId.value,
      username: authStore.user?.username,
      avatar: authStore.user?.avatar
    },
    receiver: currentPartner.value,
    createdAt: new Date().toISOString()
  }
}

const replaceTempMessage = (clientMessageId, sentMessage) => {
  const idx = messages.value.findIndex(m => m.clientMessageId === clientMessageId)
  if (idx !== -1) {
    messages.value[idx] = { ...(sentMessage || {}), status: 'SENT' }
  }
}

const updateTempStatus = (clientMessageId, status) => {
  const msg = messages.value.find(m => m.clientMessageId === clientMessageId)
  if (msg) {
    msg.status = status
  }
}

const sendMessage = async () => {
  const content = inputContent.value.trim()
  if (!content || !currentPartner.value) return

  const clientMessageId = generateUUID()
  const temp = createTempMessage(content, clientMessageId)
  messages.value.push(temp)
  inputContent.value = ''
  scrollToBottom()

  try {
    const sent = await apiSendMessage(currentPartner.value.id, content, clientMessageId)
    replaceTempMessage(clientMessageId, sent)
    loadConversations()
    // 非好友打招呼：一条额度已用完，锁定输入
    if (!chatStatus.value.mutualFollow) {
      chatStatus.value.canSend = false
    }
  } catch (err) {
    updateTempStatus(clientMessageId, 'FAILED')
    ElMessage.error(err?.message || '发送失败')
    if (err && typeof err.message === 'string' && err.message.includes('互相关注')) {
      chatStatus.value.canSend = false
    }
  }
}

const retryMessage = async (msg) => {
  if (!msg.clientMessageId || msg.status !== 'FAILED' || !currentPartner.value) return
  updateTempStatus(msg.clientMessageId, 'SENDING')
  try {
    const sent = await apiSendMessage(currentPartner.value.id, msg.content, msg.clientMessageId)
    replaceTempMessage(msg.clientMessageId, sent)
    loadConversations()
  } catch {
    updateTempStatus(msg.clientMessageId, 'FAILED')
    ElMessage.error('重发失败')
  }
}

const handleDeleteMessage = async (messageId) => {
  if (!messageId || String(messageId).startsWith('temp-')) return
  try {
    await deleteMessage(messageId)
    messages.value = messages.value.filter(m => m.id !== messageId)
    ElMessage.success('消息已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

// SSE 事件处理
const handleStreamEvent = (type, payload) => {
  if (type === 'NEW_MESSAGE') {
    handleNewMessage(payload)
  } else if (type === 'MESSAGE_SENT') {
    handleMessageSent(payload)
  }
}

const handleNewMessage = (message) => {
  if (!message) return
  const partnerId = message.sender?.id === currentUserId.value
    ? (message.receiver?.id ?? message.receiverId)
    : (message.sender?.id ?? message.senderId)
  const partnerBrief = message.sender?.id === currentUserId.value ? message.receiver : message.sender
  if (!partnerId) return

  const isCurrentChat = currentPartner.value && partnerId === currentPartner.value.id
  if (isCurrentChat) {
    const exists = messages.value.some(m =>
      (m.id && m.id === message.id) ||
      (m.clientMessageId && m.clientMessageId === message.clientMessageId)
    )
    if (!exists) {
      messages.value.push(message)
      triggerPartnerTyping()
      scrollToBottom()
    }
    // 正在看这个会话：自动标已读
    markConversationAsRead(partnerId).then(() => {
      const conv = conversations.value.find(c => getPartner(c)?.id === partnerId)
      if (conv) conv.unreadCount = 0
      emitUnreadRefresh()
    }).catch(() => {})
  }

  // 本地 upsert 会话条目（用 SSE 附带的用户摘要，陌生人首条私信无需回源）
  if (partnerBrief) {
    upsertConversation({
      partnerId,
      brief: partnerBrief,
      preview: message.content,
      at: message.createdAt,
      fromMe: false,
      read: isCurrentChat
    })
  } else {
    // 兜底：事件缺少用户摘要时回源拉取列表
    loadConversations()
    if (!isCurrentChat) emitUnreadRefresh()
  }
}

/** 本地维护会话列表条目：更新预览/时间/未读并按最新排序，避免每条消息全量拉取 */
const upsertConversation = ({ partnerId, brief, preview, at, fromMe, read }) => {
  const idx = conversations.value.findIndex(c => getPartner(c)?.id === partnerId)
  const prevUnread = idx >= 0 ? Number(conversations.value[idx].unreadCount ?? 0) : 0
  const patch = {
    ...(brief ? { partner: brief } : {}),
    content: preview,
    createdAt: at,
    lastMessagePreview: preview,
    lastMessageAt: at,
    lastMessageFromMe: fromMe,
    unreadCount: read ? 0 : prevUnread + 1,
    // 陌生人发来的新会话必然未互关；既有会话保留原标记（互关状态变化由下次列表刷新纠正）
    mutualFollow: idx >= 0 ? conversations.value[idx].mutualFollow !== false : false
  }
  if (idx >= 0) {
    conversations.value[idx] = { ...conversations.value[idx], ...patch }
  } else {
    conversations.value.unshift({ partner: brief, ...patch })
  }
  conversations.value.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
}

const handleMessageSent = (payload) => {
  const { clientMessageId, message } = payload || {}
  if (clientMessageId) {
    replaceTempMessage(clientMessageId, message)
    loadConversations()
  }
}

const { close: closeMessageStream } = useMessageStream({
  onEvent: handleStreamEvent
})

// 新对话
const loadFriendList = async () => {
  try {
    const data = await getFriendList()
    friendList.value = data || []
  } catch {
    friendList.value = []
  }
}

const showNewConversationDialog = () => {
  loadFriendList()
  newConversationVisible.value = true
  searchKeyword.value = ''
  searchedUsers.value = []
}

const handleSearchUsers = () => {
  if (userSearchTimer) {
    clearTimeout(userSearchTimer)
  }
  userSearchTimer = setTimeout(async () => {
    if (!searchKeyword.value.trim()) {
      searchedUsers.value = []
      return
    }
    try {
      const data = await searchUsers(searchKeyword.value.trim())
      searchedUsers.value = data || []
    } catch {
      searchedUsers.value = []
    }
  }, 300)
}

const startConversation = (user) => {
  currentPartner.value = user
  messages.value = []
  newConversationVisible.value = false
  mobileShowChat.value = true
  const existing = conversations.value.find(c => getPartner(c).id === user.id)
  if (existing) {
    selectConversation(existing)
  }
}

// 全局搜索
const onGlobalSearchInput = () => {
  if (globalSearchTimer) {
    clearTimeout(globalSearchTimer)
  }
  globalSearchTimer = setTimeout(() => {
    doGlobalSearch()
  }, 400)
}

const clearGlobalSearch = () => {
  globalSearchKeyword.value = ''
  globalSearchResults.value = []
  globalSearchError.value = false
}

const doGlobalSearch = async () => {
  const keyword = globalSearchKeyword.value.trim()
  if (!keyword) {
    globalSearchResults.value = []
    return
  }
  globalSearchLoading.value = true
  globalSearchError.value = false
  try {
    const data = await searchMessages(keyword, 0, 20)
    globalSearchResults.value = data?.list || data || []
  } catch {
    globalSearchError.value = true
    globalSearchResults.value = []
  } finally {
    globalSearchLoading.value = false
  }
}

const jumpToSearchResult = async (result) => {
  clearGlobalSearch()
  const partner = getPartner(result)
  if (!partner.id) return
  currentPartner.value = partner
  mobileShowChat.value = true
  await loadInitialMessages(partner.id)
  await ensureMessageVisible(result.id)
  await markReadAndRefresh(partner.id)
}

// 当前对话搜索
const toggleConvSearch = () => {
  convSearchVisible.value = !convSearchVisible.value
  if (!convSearchVisible.value) {
    clearConvSearch()
  }
}

const onConvSearchInput = () => {
  if (convSearchTimer) {
    clearTimeout(convSearchTimer)
  }
  convSearchTimer = setTimeout(() => {
    doConvSearch()
  }, 400)
}

const clearConvSearch = () => {
  convSearchKeyword.value = ''
  convSearchResults.value = []
  convSearchError.value = false
}

const doConvSearch = async () => {
  const keyword = convSearchKeyword.value.trim()
  if (!keyword || !currentPartner.value) {
    convSearchResults.value = []
    return
  }
  convSearchLoading.value = true
  convSearchError.value = false
  try {
    const data = await searchConversationMessages(currentPartner.value.id, keyword, 0, 20)
    convSearchResults.value = data?.list || data || []
  } catch {
    convSearchError.value = true
    convSearchResults.value = []
  } finally {
    convSearchLoading.value = false
  }
}

const jumpToConvMessage = async (msg) => {
  if (!msg.id) return
  await ensureMessageVisible(msg.id)
  convSearchVisible.value = false
  clearConvSearch()
}

const ensureMessageVisible = async (messageId) => {
  let found = messages.value.some(m => m.id === messageId)
  let guard = 0
  while (!found && hasMore.value && guard < 10) {
    await loadMoreMessages()
    found = messages.value.some(m => m.id === messageId)
    guard += 1
  }
  nextTick(() => {
    const el = document.getElementById(`msg-${messageId}`)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      el.classList.add('message-highlight')
      setTimeout(() => el.classList.remove('message-highlight'), 2000)
    }
  })
}

// 移动端适配
const handleResize = () => {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) {
    mobileShowChat.value = false
  }
}

onMounted(() => {
  loadConversations()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  closeMessageStream()
  if (userSearchTimer) clearTimeout(userSearchTimer)
  if (globalSearchTimer) clearTimeout(globalSearchTimer)
  if (convSearchTimer) clearTimeout(convSearchTimer)
  if (partnerTypingTimer) clearTimeout(partnerTypingTimer)
})
</script>

<style lang="scss" scoped>
.message-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(56% 38% at 90% 0%, var(--content-glow-b), transparent 62%),
    radial-gradient(48% 34% at 4% 8%, var(--content-glow-a), transparent 62%),
    var(--color-bg);
}

.main-content {
  flex: 1;
  padding: calc(var(--header-height) + 26px) 0 36px;
}

/* ===== 页头 ===== */
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}

.page-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid var(--content-hero-line);
  background: var(--content-eyebrow);
  color: var(--content-ink-3);
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: linear-gradient(135deg, #4285f4, #a855f7);
    box-shadow: 0 0 10px rgba(66, 133, 244, 0.6);
  }
}

.page-head h1 {
  margin: 0 0 6px;
  color: var(--content-ink);
  font-size: clamp(28px, 3.2vw, 34px);
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.15;
}

.page-head p {
  margin: 0;
  color: var(--content-ink-2);
  font-size: 13.5px;
}

.page-head-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 11px;
  border-radius: 999px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--content-ink-3);
  font-size: 11.5px;
  white-space: nowrap;

  b {
    color: var(--content-ink);
    font-size: 13px;
    font-variant-numeric: tabular-nums;
  }

  &.is-accent {
    color: #b45309;
    background: rgba(245, 158, 11, 0.12);
    border-color: rgba(245, 158, 11, 0.2);

    b {
      color: #b45309;
    }
  }
}

:global([data-theme='dark']) .meta-chip.is-accent {
  color: #fcd34d;

  b {
    color: #fcd34d;
  }
}

.new-chat-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 17px;
  border: none;
  border-radius: 11px;
  color: #fff;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(120deg, #1a73e8, #6f5cf6 62%, #8b5cf6);
  box-shadow: 0 10px 22px rgba(47, 99, 235, 0.26);
  transition: transform var(--transition-fast), filter var(--transition-fast);

  &:hover {
    transform: translateY(-1px);
    filter: brightness(1.05);
  }
}

/* ===== 主容器 ===== */
.message-shell {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  height: clamp(600px, calc(100vh - var(--header-height) - 190px), 840px);
  overflow: hidden;
  border-radius: 28px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow-lg);
}

/* ===== 会话侧栏 ===== */
.message-sidebar {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--content-surface-tint);
  border-right: 1px solid var(--content-line);
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 18px 12px;
}

.sidebar-eyebrow {
  display: block;
  margin-bottom: 3px;
  color: var(--content-ink-3);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.sidebar-head h2 {
  margin: 0;
  color: var(--content-ink);
  font-size: 19px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.compose-btn {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 12px;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  background: linear-gradient(135deg, #1a73e8, #8b5cf6);
  box-shadow: 0 8px 18px rgba(47, 99, 235, 0.25);
  transition: transform var(--transition-fast);

  &:hover {
    transform: translateY(-1px) scale(1.03);
  }
}

.sidebar-search {
  padding: 0 14px 12px;

  :deep(.el-input__wrapper) {
    padding: 2px 12px;
    border-radius: 12px;
    background: var(--content-surface);
    box-shadow: 0 0 0 1px var(--content-chip-line) inset;
  }

  :deep(.el-input__inner) {
    height: 36px;
    color: var(--content-ink);
    font-size: 12.5px;

    &::placeholder {
      color: var(--content-ink-3);
    }
  }

  :deep(.el-input__prefix-inner) {
    color: var(--content-ink-3);
  }
}

.conversation-list-wrap {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 2px 10px 10px;
}

.conversation-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  padding: 11px 10px;
  border-radius: 14px;
  border: 1px solid transparent;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);

  &:hover {
    background: var(--content-surface);
    border-color: var(--content-chip-line);
  }

  &.active {
    background:
      linear-gradient(120deg, var(--content-glow-a), transparent 62%),
      var(--content-surface);
    border-color: var(--content-line-strong);
    box-shadow: inset 3px 0 0 var(--color-primary);
  }
}

.conv-avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.conv-avatar {
  display: block;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--content-surface);
  box-shadow: var(--content-shadow);
}

.conv-online-status {
  position: absolute;
  right: 1px;
  bottom: 1px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #34d399;
  border: 2px solid var(--content-surface-tint);
  box-shadow: 0 0 0 2px rgba(52, 211, 153, 0.12);
}

.conv-info {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.conv-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.conv-name {
  overflow: hidden;
  color: var(--content-ink);
  font-size: 13.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  flex-shrink: 0;
  color: var(--content-ink-3);
  font-size: 10.5px;
}

.conv-preview {
  display: block;
  overflow: hidden;
  color: var(--content-ink-2);
  font-size: 12px;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;

  em {
    color: var(--color-primary);
    font-style: normal;
  }
}

.conv-unread {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  background: linear-gradient(135deg, #1a73e8, #6f5cf6);
  box-shadow: 0 4px 10px rgba(47, 99, 235, 0.28);
}

/* 会话骨架 */
.conv-skeleton {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 4px;
}

.conv-skeleton-row {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 11px 10px;
}

.sk-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--content-sk-a);
}

.sk-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 7px;

  i {
    display: block;
    height: 9px;
    border-radius: 4px;
    background: var(--content-sk-a);

    &:last-child {
      width: 62%;
      opacity: 0.7;
    }
  }
}

.conv-skeleton-row,
.msg-skeleton {
  background: linear-gradient(100deg, var(--content-sk-a) 28%, var(--content-sk-b) 50%, var(--content-sk-a) 72%);
  background-size: 220% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}

.conversation-state,
.inbox-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 44px 20px;
  text-align: center;
}

.state-orb {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  margin-bottom: 12px;
  border-radius: 17px;
  color: #fff;
  font-size: 22px;
  background: linear-gradient(135deg, #1a73e8, #8b5cf6);
  box-shadow: 0 14px 28px rgba(70, 90, 220, 0.24);
}

.conversation-state p,
.inbox-empty h4 {
  margin: 0;
}

.conversation-state p {
  margin-bottom: 12px;
  color: var(--content-ink-3);
  font-size: 12.5px;
}

.inbox-empty h4 {
  margin-bottom: 5px;
  color: var(--content-ink);
  font-size: 15px;
  font-weight: 600;
}

.inbox-empty p {
  max-width: 230px;
  margin: 0 0 14px;
  color: var(--content-ink-3);
  font-size: 12px;
  line-height: 1.6;
}

.state-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 14px;
  border: 1px solid var(--content-chip-line);
  border-radius: 10px;
  background: var(--content-chip);
  color: var(--color-primary);
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    border-color: var(--content-line-strong);
    background: var(--content-surface);
  }

  &.small {
    height: 28px;
    padding: 0 10px;
  }
}

.sidebar-foot {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 18px;
  border-top: 1px solid var(--content-line);
  color: var(--content-ink-3);
  font-size: 10.5px;
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 0 3px rgba(52, 211, 153, 0.14);
}

/* ===== 搜索 ===== */
.search-results-panel {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px;
}

.search-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 4px 4px 8px;
  color: var(--content-ink);
  font-size: 11.5px;
  font-weight: 600;
}

.link-btn {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--color-primary);
  font-family: inherit;
  font-size: 11.5px;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.search-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 22px 12px;
  color: var(--content-ink-3);
  font-size: 12px;
  text-align: center;
}

.search-result-item {
  display: flex;
  width: 100%;
  align-items: flex-start;
  gap: 10px;
  padding: 9px 8px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    border-color: var(--content-chip-line);
    background: var(--content-surface);
  }

  &.is-message {
    display: block;
    border-bottom: 1px solid var(--content-line);
    border-radius: 0;

    &:last-child {
      border-bottom: none;
    }
  }
}

.search-result-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--content-line);
}

.search-result-info {
  flex: 1;
  min-width: 0;
}

.search-result-name {
  margin-bottom: 2px;
  color: var(--content-ink);
  font-size: 12.5px;
  font-weight: 600;
}

.search-result-content {
  color: var(--content-ink-2);
  font-size: 12.5px;
  line-height: 1.55;
  word-break: break-word;

  :deep(mark) {
    padding: 0 2px;
    border-radius: 3px;
    background: rgba(251, 188, 5, 0.24);
    color: var(--content-ink);
  }
}

.search-result-time {
  margin-top: 3px;
  color: var(--content-ink-3);
  font-size: 10.5px;
}

/* ===== 聊天区 ===== */
.message-main {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background: var(--content-surface);
}

.message-placeholder {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 40px;
  text-align: center;
  background:
    radial-gradient(60% 50% at 50% 20%, var(--content-glow-a), transparent 65%),
    var(--content-surface);

  h3 {
    margin: 10px 0 0;
    color: var(--content-ink);
    font-size: 18px;
    font-weight: 600;
  }

  p {
    margin: 0;
    color: var(--content-ink-2);
    font-size: 13px;
  }
}

.placeholder-orbit {
  position: relative;
  width: 132px;
  height: 132px;
}

.po-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid var(--content-line-strong);

  &.po-ring-1 {
    inset: 0;
    border-style: dashed;
    animation: orbitSpin 26s linear infinite;
  }

  &.po-ring-2 {
    inset: 18px;
    opacity: 0.6;
    animation: orbitSpin 19s linear infinite reverse;
  }
}

.po-core {
  position: absolute;
  top: 50%;
  left: 50%;
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  margin: -27px 0 0 -27px;
  border-radius: 18px;
  color: #fff;
  background: linear-gradient(135deg, #1a73e8, #8b5cf6);
  box-shadow: 0 16px 34px rgba(70, 90, 220, 0.3);
}

@keyframes orbitSpin {
  to { transform: rotate(360deg); }
}

.placeholder-action {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 36px;
  margin-top: 14px;
  padding: 0 16px;
  border: 1px solid var(--content-chip-line);
  border-radius: 10px;
  background: var(--content-chip);
  color: var(--color-primary);
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    border-color: var(--content-line-strong);
    background: var(--content-surface);
  }
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 66px;
  padding: 11px 20px;
  border-bottom: 1px solid var(--content-line);
  background: var(--content-surface);
  z-index: 3;
}

.chat-person {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.chat-back {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  margin-right: 2px;
  border: 1px solid var(--content-chip-line);
  border-radius: 10px;
  background: var(--content-chip);
  color: var(--content-ink-2);
  font-size: 16px;
  cursor: pointer;
}

.chat-person-link {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.chat-avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.chat-avatar {
  display: block;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--content-surface);
  box-shadow: var(--content-shadow);
}

.chat-online {
  position: absolute;
  right: 1px;
  bottom: 1px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #34d399;
  border: 2px solid var(--content-surface);
}

.chat-person-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;

  strong {
    overflow: hidden;
    color: var(--content-ink);
    font-size: 14.5px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    color: #15803d;
    font-size: 11px;

    i {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #34d399;
    }
  }
}

:global([data-theme='dark']) .chat-person-copy small {
  color: #86efac;
}

.chat-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.icon-btn {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--content-chip-line);
  border-radius: 10px;
  background: var(--content-chip);
  color: var(--content-ink-3);
  font-size: 15px;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover,
  &.active {
    border-color: var(--content-line-strong);
    color: var(--color-primary);
    background: var(--content-surface);
  }
}

.conv-search-panel {
  position: absolute;
  top: 66px;
  left: 0;
  right: 0;
  z-index: 20;
  max-height: 360px;
  overflow-y: auto;
  padding: 12px 16px;
  border-bottom: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow-lg);
}

.conv-search-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;

  :deep(.el-input__wrapper) {
    padding: 2px 12px;
    border-radius: 10px;
    background: var(--content-surface-tint);
    box-shadow: 0 0 0 1px var(--content-chip-line) inset;
  }

  :deep(.el-input__inner) {
    height: 34px;
    color: var(--content-ink);
    font-size: 12.5px;
  }
}

.messages-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--content-line);
  color: var(--color-error);
  font-size: 12.5px;
  background: rgba(234, 67, 53, 0.06);
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 20px 24px;
  background:
    radial-gradient(60% 42% at 50% 0%, var(--content-glow-a), transparent 65%),
    var(--content-surface);
}

.chat-loading {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 10px 0;
}

.msg-skeleton {
  width: 46%;
  height: 46px;
  border-radius: 16px;

  &.self {
    align-self: flex-end;
  }
}

.load-more-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px 0 12px;
}

.load-more-btn {
  padding: 4px 12px;
  border: 1px solid var(--content-chip-line);
  border-radius: 999px;
  background: var(--content-chip);
  color: var(--content-ink-3);
  font-family: inherit;
  font-size: 11.5px;
  cursor: pointer;

  &:hover {
    color: var(--color-primary);
    border-color: var(--content-line-strong);
  }
}

.load-more-hint {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--content-ink-3);
  font-size: 11.5px;

  &.is-error {
    color: var(--color-error);
  }
}

.date-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px 0 10px;
  flex-shrink: 0;
}

.date-divider-line {
  max-width: 72px;
  flex: 1;
  height: 1px;
  background: var(--content-line);
}

.date-divider-text {
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--content-ink-3);
  font-size: 10.5px;
  font-weight: 500;
}

.message-item {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 5px 0;
  animation: fadeInUp 0.25s ease-out;

  &.self {
    flex-direction: row-reverse;

    .msg-avatar {
      display: none;
    }

    .msg-bubble {
      border-radius: 18px 18px 6px 18px;
      color: #fff;
      background: linear-gradient(135deg, #1a73e8 0%, #5b6cf5 58%, #8b5cf6 100%);
      border-color: transparent;
      box-shadow: 0 10px 24px rgba(47, 99, 235, 0.24);
    }

    .msg-meta {
      justify-content: flex-end;
    }

    .msg-time {
      color: var(--content-ink-3);
    }
  }
}

.message-highlight .msg-bubble {
  animation: glowPulse 1.5s ease-in-out;
}

.msg-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--content-surface);
  box-shadow: var(--content-shadow);
}

.msg-content {
  display: flex;
  max-width: 66%;
  flex-direction: column;
  gap: 4px;
}

.msg-bubble {
  padding: 11px 15px;
  border-radius: 18px 18px 18px 6px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);
  color: var(--content-ink);
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: var(--content-shadow);
}

.msg-meta {
  position: relative;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 4px;
}

.msg-time {
  color: var(--content-ink-3);
  font-size: 10.5px;
}

.msg-status {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--content-ink-3);
  font-size: 10.5px;

  .status-failed {
    color: var(--color-error);
    cursor: pointer;
    text-decoration: underline;
  }
}

.status-read {
  display: inline-flex;
  align-items: center;
  gap: 2px;

  &.unread {
    color: var(--content-ink-3);
  }

  &:not(.unread) {
    color: var(--color-success);
  }
}

.msg-delete-btn {
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--content-ink-3);
  font-size: 11px;
  cursor: pointer;
  opacity: 0;
  transition: all var(--transition-fast);

  &:hover {
    color: var(--color-error);
    background: rgba(234, 67, 53, 0.1);
  }
}

.message-item:hover .msg-delete-btn {
  opacity: 1;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 20px;
  color: var(--content-ink-3);
  font-size: 12.5px;

  p {
    margin: 0;
  }
}

.chat-empty-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: var(--content-chip);
  border: 1px solid var(--content-chip-line);
  color: var(--content-ink-3);
  font-size: 18px;
}

.typing-indicator {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 5px 0;
  animation: fadeInUp 0.2s ease-out;
}

.typing-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--content-surface);
}

.typing-bubble {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 56px;
  padding: 13px 16px;
  border-radius: 18px 18px 18px 6px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);

  span {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: var(--content-ink-3);
    animation: typingBounce 1.4s infinite ease-in-out both;

    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

/* ===== 输入区 ===== */
.conv-tag {
  flex-shrink: 0;
  padding: 1px 6px;
  font-size: 10.5px;
  font-weight: 600;
  color: #d97706;
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: 999px;
}

.composer-lock {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 14px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #92400e;
  background: rgba(245, 158, 11, 0.08);
  border: 1px dashed rgba(245, 158, 11, 0.4);
  border-radius: 10px;

  .lock-link {
    color: var(--color-primary);
    font-weight: 600;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

.composer-disabled {
  opacity: 0.75;
}

.composer-hint {
  color: var(--color-text-muted);
}

.friend-group-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  padding: 6px 4px 2px;
}

.friend-empty {
  padding: 18px 8px;
  font-size: 13px;
  color: var(--color-text-muted);
  text-align: center;
}

.message-input-area {
  padding: 12px 20px 16px;
  border-top: 1px solid var(--content-line);
  background: var(--content-surface);
}

.composer {
  padding: 10px 12px 8px;
  border-radius: 18px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-surface-tint);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);

  &:focus-within {
    border-color: var(--content-line-strong);
    box-shadow: 0 0 0 4px var(--content-glow-a);
  }

  :deep(.el-textarea__inner) {
    padding: 2px;
    border: none;
    border-radius: 0;
    background: transparent;
    box-shadow: none;
    color: var(--content-ink);
    font-family: inherit;
    font-size: 14px;
    line-height: 1.6;
    resize: none;

    &:focus {
      box-shadow: none;
    }

    &::placeholder {
      color: var(--content-ink-3);
    }
  }
}

.composer-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 4px;
  color: var(--content-ink-3);
  font-size: 10.5px;
}

.send-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 34px;
  padding: 0 18px;
  border: none;
  border-radius: 10px;
  color: #fff;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(120deg, #1a73e8, #6f5cf6);
  box-shadow: 0 8px 18px rgba(47, 99, 235, 0.24);
  transition: transform var(--transition-fast), opacity var(--transition-fast);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.42;
    cursor: not-allowed;
    box-shadow: none;
  }
}

/* ===== 新对话 ===== */
.dialog-intro {
  margin: -4px 0 12px;
  color: var(--content-ink-3);
  font-size: 12.5px;
}

.dialog-search {
  :deep(.el-input__wrapper) {
    padding: 2px 12px;
    border-radius: 11px;
    background: var(--content-surface-tint);
    box-shadow: 0 0 0 1px var(--content-chip-line) inset;
  }

  :deep(.el-input__inner) {
    height: 38px;
    color: var(--content-ink);
    font-size: 13px;
  }
}

.user-search-list {
  max-height: 320px;
  margin-top: 12px;
  overflow-y: auto;
}

.user-search-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 11px;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 13px;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    border-color: var(--content-chip-line);
    background: var(--content-surface-tint);
  }
}

.user-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--content-line);
}

.user-search-copy {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 2px;

  strong {
    overflow: hidden;
    color: var(--content-ink);
    font-size: 13.5px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    color: var(--content-ink-3);
    font-size: 11px;
  }
}

.user-search-arrow {
  flex-shrink: 0;
  color: var(--color-primary);
  font-size: 14px;
}

.user-search-empty {
  padding: 26px 16px;
  color: var(--content-ink-3);
  font-size: 12.5px;
  text-align: center;
}

@keyframes typingBounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* ===== 响应式 ===== */
@media (max-width: 1000px) {
  .message-shell {
    grid-template-columns: 286px minmax(0, 1fr);
  }

  .msg-content {
    max-width: 72%;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: var(--header-height) 0 0;
  }

  .main-content .container {
    max-width: none;
    width: 100%;
    padding: 0;
  }

  .page-head {
    display: none;
  }

  .message-shell {
    grid-template-columns: 1fr;
    height: calc(100vh - var(--header-height));
    border: none;
    border-radius: 0;
    box-shadow: none;
  }

  .message-sidebar,
  .message-main {
    min-height: 0;

    &.mobile-hidden {
      display: none;
    }
  }

  .message-sidebar {
    border-right: none;
  }

  .message-main {
    position: fixed;
    top: var(--header-height);
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 50;
    background: var(--content-surface);
  }

  .sidebar-head {
    padding: 16px 16px 10px;
  }

  .sidebar-search {
    padding: 0 12px 10px;
  }

  .conversation-list {
    padding: 2px 8px 12px;
  }

  .sidebar-foot {
    padding-bottom: calc(10px + env(safe-area-inset-bottom));
  }

  .message-list {
    padding: 14px 14px 8px;
  }

  .msg-content {
    max-width: 78%;
  }

  .msg-bubble {
    padding: 10px 13px;
    font-size: 14.5px;
  }

  .msg-avatar,
  .typing-avatar {
    width: 32px;
    height: 32px;
  }

  .chat-head {
    min-height: 62px;
    padding: 9px 12px;
  }

  .chat-avatar {
    width: 38px;
    height: 38px;
  }

  .conv-search-panel {
    top: 62px;
    padding: 10px 12px;
  }

  .message-input-area {
    padding: 9px 12px calc(10px + env(safe-area-inset-bottom));
  }

  .composer {
    padding: 8px 10px 6px;
  }

  .composer-foot > span {
    display: none;
  }

  .composer-foot {
    justify-content: flex-end;
  }

  .send-btn {
    height: 32px;
    padding: 0 14px;
  }

  .message-placeholder {
    padding: 24px;
  }

  .msg-delete-btn {
    opacity: 1;
  }
}

@media (max-width: 420px) {
  .send-btn span {
    display: none;
  }

  .send-btn {
    width: 38px;
    padding: 0;
    justify-content: center;
  }
}
</style>
