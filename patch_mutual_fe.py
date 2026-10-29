# -*- coding: utf-8 -*-
import io

# ============ api/message.js ============
p = r'D:\prdemo\light-note\zhiguang-frontend\src\api\message.js'
with io.open(p, 'r', encoding='utf-8') as f:
    s = f.read()
old = '''export const searchUsers = (keyword) => {'''
new = '''/** 好友列表（互相关注的用户，发起新对话默认展示） */
export const getFriendList = () => {
  return request({
    url: '/api/messages/friends',
    method: 'get'
  })
}

/** 与某用户的聊天资格（未互关只能发一条打招呼消息） */
export const getChatStatus = (partnerId) => {
  return request({
    url: `/api/messages/chat-status/${partnerId}`,
    method: 'get'
  })
}

export const searchUsers = (keyword) => {'''
assert old in s
s = s.replace(old, new, 1)
with io.open(p, 'w', encoding='utf-8', newline='') as f:
    f.write(s)
print('api patched')

# ============ Message.vue ============
p2 = r'D:\prdemo\light-note\zhiguang-frontend\src\views\Message.vue'
with io.open(p2, 'r', encoding='utf-8') as f:
    s2 = f2 = f.read()

# 1) import 新 api
old2 = '''  markConversationAsRead,
  searchUsers,'''
new2 = '''  markConversationAsRead,
  getFriendList,
  getChatStatus,
  searchUsers,'''
assert old2 in s2
s2 = s2.replace(old2, new2, 1)

# 2) 状态
old3 = '''const newConversationVisible = ref(false)
const searchKeyword = ref('')
const searchedUsers = ref([])'''
new3 = '''const newConversationVisible = ref(false)
const searchKeyword = ref('')
const searchedUsers = ref([])
// 互关聊天规则：未互关只能发一条打招呼消息
const chatStatus = ref({ mutualFollow: true, canSend: true })
const friendList = ref([])
let chatStatusPartnerId = null'''
assert old3 in s2
s2 = s2.replace(old3, new3, 1)

# 3) 会话项"待互关"标签
old4 = '''                      <span class="conv-name">{{ getPartner(conv).username }}</span>
                        <span class="conv-time">{{ formatConversationTime(conv.createdAt) }}</span>'''
new4 = '''                      <span class="conv-name">{{ getPartner(conv).username }}</span>
                        <span v-if="conv.mutualFollow === false" class="conv-tag">待互关</span>
                        <span class="conv-time">{{ formatConversationTime(conv.createdAt) }}</span>'''
assert old4 in s2
s2 = s2.replace(old4, new4, 1)

# 4) selectConversation 拉取聊天资格
old5 = '''const selectConversation = async (conv) => {
  currentPartner.value = getPartner(conv)
  mobileShowChat.value = true'''
new5 = '''const selectConversation = async (conv) => {
  currentPartner.value = getPartner(conv)
  mobileShowChat.value = true
  refreshChatStatus(currentPartner.value.id)'''
assert old5 in s2
s2 = s2.replace(old5, new5, 1)

# 在 markReadAndRefresh 前插入 refreshChatStatus
old6 = '''const markReadAndRefresh = async (partnerId) => {'''
new6 = '''const refreshChatStatus = async (partnerId) => {
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

const markReadAndRefresh = async (partnerId) => {'''
assert old6 in s2
s2 = s2.replace(old6, new6, 1)

# 5) 输入区：锁定提示条 + 输入禁用
old7 = '''              <div class="message-input-area">
                <div class="composer">
                  <el-input
                    v-model="inputContent"
                    type="textarea"
                    maxlength="2000"
                    show-word-limit
                    :rows="2"
                    placeholder="输入消息… (Ctrl + Enter 发送)"
                    class="composer-input"
                    @keyup.enter.ctrl="sendMessage"
                  />
                  <div class="composer-foot">
                    <span>Ctrl + Enter 发送</span>
                    <button
                      type="button"
                      class="send-btn"
                      :disabled="!inputContent.trim() || !currentPartner"
                      @click="sendMessage"
                    >
                      <el-icon><Promotion /></el-icon>
                      <span>发送</span>
                    </button>
                  </div>
                </div>
              </div>'''
new7 = '''              <div class="message-input-area">
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
                    <span v-else-if="!chatStatus.mutualFollow" class="composer-hint">非好友仅可发送一条消息</span>
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
              </div>'''
assert old7 in s2
s2 = s2.replace(old7, new7, 1)

# 6) sendMessage：成功耗尽额度/失败提示联动
old8 = '''    const sent = await apiSendMessage(currentPartner.value.id, content, clientMessageId)'''
new8 = '''    const sent = await apiSendMessage(currentPartner.value.id, content, clientMessageId)
    // 非好友打招呼：一条额度已用完，锁定输入
    if (!chatStatus.value.mutualFollow) {
      chatStatus.value.canSend = false
    }'''
assert old8 in s2
s2 = s2.replace(old8, new8, 1)

# sendMessage 的 catch：互关类错误同步锁定状态
old9 = '''  } catch {
    updateTempStatus(clientMessageId, 'failed')
  }
}'''
new9 = '''  } catch (err) {
    updateTempStatus(clientMessageId, 'failed')
    if (err && typeof err.message === 'string' && err.message.includes('互相关注')) {
      chatStatus.value.canSend = false
    }
  }
}'''
assert old9 in s2
s2 = s2.replace(old9, new9, 1)

# 7) 新对话对话框：好友列表（无搜索词时默认展示）
old10 = '''      <p class="dialog-intro">搜索用户名，找到你想联系的创作者。</p>'''
new10 = '''      <p class="dialog-intro">从好友列表发起聊天，或搜索用户名给陌生创作者打个招呼（互关前仅可发送一条消息）。</p>'''
assert old10 in s2
s2 = s2.replace(old10, new10, 1)

old11 = '''      <div class="user-search-list">
        <button
          v-for="user in searchedUsers"'''
new11 = '''      <div v-if="!searchKeyword.trim()" class="user-search-list">
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
          v-for="user in searchedUsers"'''
assert old11 in s2
s2 = s2.replace(old11, new11, 1)

# 8) 打开对话框加载好友列表
old12 = '''const showNewConversationDialog = () => {'''
new12 = '''const loadFriendList = async () => {
  try {
    const data = await getFriendList()
    friendList.value = data || []
  } catch {
    friendList.value = []
  }
}

const showNewConversationDialog = () => {
  loadFriendList()'''
assert old12 in s2
s2 = s2.replace(old12, new12, 1)

# 9) handleNewMessage upsert 保留/初始化 mutualFollow
old13 = '''  const patch = {
    ...(brief ? { partner: brief } : {}),
    content: preview,
    createdAt: at,
    lastMessagePreview: preview,
    lastMessageAt: at,
    lastMessageFromMe: fromMe,
    unreadCount: read ? 0 : prevUnread + 1
  }'''
new13 = '''  const patch = {
    ...(brief ? { partner: brief } : {}),
    content: preview,
    createdAt: at,
    lastMessagePreview: preview,
    lastMessageAt: at,
    lastMessageFromMe: fromMe,
    unreadCount: read ? 0 : prevUnread + 1,
    // 陌生人发来的新会话必然未互关；既有会话保留原标记（互关状态变化由下次列表刷新纠正）
    mutualFollow: idx >= 0 ? conversations.value[idx].mutualFollow !== false : false
  }'''
assert old13 in s2
s2 = s2.replace(old13, new13, 1)

# 10) 样式：待互关标签、锁定提示、好友分组标题
old14 = '''.message-input-area {'''
new14 = '''.conv-tag {
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

.message-input-area {'''
assert old14 in s2
s2 = s2.replace(old14, new14, 1)

# 11) Lock 图标 import（与 Promotion 等一起）
if '@element-plus/icons-vue' in s2 and 'Lock' not in s2.split('<script')[1].split('</script>')[0]:
    import re
    m = re.search(r"import \{([^}]*)\} from '@element-plus/icons-vue'", s2)
    if m:
        names = m.group(1)
        if 'Lock' not in names:
            s2 = s2.replace(m.group(0), m.group(0).replace('{', '{\n  Lock,', 1), 1)
            print('Lock icon imported')
    else:
        print('WARN: icons import not found, checking...')
        print([l for l in s2.split('\n') if 'icons-vue' in l][:3])

with io.open(p2, 'w', encoding='utf-8', newline='') as f:
    f.write(s2)
print('Message.vue patched')
