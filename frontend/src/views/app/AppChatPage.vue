<template>
  <div class="chat-page">
    <div class="chat-container">
      <!-- 左侧对话列表 -->
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <h3 class="sidebar-title">对话列表</h3>
          <a-button type="primary" size="small" @click="createNewChat" class="new-chat-btn">
            <PlusOutlined />
            新建
          </a-button>
        </div>

        <div class="chat-list">
          <div
            v-for="chat in chatList"
            :key="chat.id"
            :class="['chat-item', { active: currentChatId === chat.id }]"
            @click="switchChat(chat.id)"
          >
            <div class="chat-item-icon">
              <MessageOutlined />
            </div>
            <div class="chat-item-info">
              <div class="chat-item-title">{{ chat.message?.substring(0, 20) || '新对话' }}...</div>
              <div class="chat-item-time">{{ formatTime(chat.createTime) }}</div>
            </div>
          </div>
          <a-empty v-if="chatList.length === 0" description="暂无对话记录" :image-style="{ height: '40px' }" />
        </div>
      </div>

      <!-- 右侧聊天区域 -->
      <div class="chat-main">
        <!-- 应用信息头部 -->
        <div class="chat-header">
          <div class="app-info">
            <a-avatar :size="36" :src="appInfo.cover" class="app-avatar">
              <template #icon><AppstoreOutlined /></template>
            </a-avatar>
            <div class="app-details">
              <h2 class="app-name">{{ appInfo.appName || '加载中...' }}</h2>
              <p class="app-description">{{ appInfo.initPrompt }}</p>
            </div>
          </div>
          <div class="header-actions">
            <a-button size="small" @click="goToEdit" class="header-btn">
              <EditOutlined />
              编辑
            </a-button>
            <a-button size="small" @click="handleDeploy" :loading="deploying" class="header-btn">
              <CloudUploadOutlined />
              部署
            </a-button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="messages-container" ref="messagesContainer">
          <div class="messages-list">
            <div v-if="chatStore.loading" class="loading-more">
              <a-spin size="small" />
              <span>加载中...</span>
            </div>
            <div v-if="chatStore.pagination.hasMore && !chatStore.loading" class="load-more-btn">
              <a-button type="link" @click="loadMore" size="small">加载更多历史消息</a-button>
            </div>

            <div
              v-for="msg in chatStore.messages"
              :key="msg.id"
              :class="['message-item', msg.messageType]"
            >
              <a-avatar :size="34" :class="['msg-avatar', msg.messageType]">
                <template #icon>
                  <UserOutlined v-if="msg.messageType === 'user'" />
                  <RobotOutlined v-else />
                </template>
              </a-avatar>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-sender">{{ msg.messageType === 'user' ? '我' : 'AI 助手' }}</span>
                  <span class="message-time">{{ formatTime(msg.createTime) }}</span>
                </div>
                <div class="message-text" v-html="formatMessage(msg.message)"></div>
              </div>
            </div>

            <!-- 加载状态 -->
            <div v-if="sending" class="message-item assistant">
              <a-avatar :size="34" class="msg-avatar assistant">
                <template #icon><RobotOutlined /></template>
              </a-avatar>
              <div class="message-content">
                <div class="message-text loading">
                  <span class="typing-indicator">
                    <span></span><span></span><span></span>
                  </span>
                  <span>AI 正在思考...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-textarea
              v-model:value="inputMessage"
              :rows="3"
              placeholder="输入你的问题... (Enter 发送，Shift+Enter 换行)"
              @keydown.enter.exact.prevent="sendMessage"
              :disabled="sending"
              class="chat-input"
            />
            <div class="input-actions">
              <a-button
                type="primary"
                :loading="sending"
                @click="sendMessage"
                class="send-btn"
              >
                <SendOutlined />
                发送
              </a-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  AppstoreOutlined,
  EditOutlined,
  CloudUploadOutlined,
  UserOutlined,
  RobotOutlined,
  SendOutlined,
  MessageOutlined
} from '@ant-design/icons-vue'
import { useChatStore } from '@/stores/chat'
import { getAppById, deployApp } from '@/api/app'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()

const appId = Number(route.params.id)

const appInfo = ref<any>({})
const chatList = ref<any[]>([])
const currentChatId = ref<number | null>(null)
const inputMessage = ref('')
const sending = ref(false)
const deploying = ref(false)
const messagesContainer = ref<HTMLElement>()

const fetchAppInfo = async () => {
  try {
    const res = await getAppById(appId)
    appInfo.value = res.data
  } catch (error) {
    console.error('获取应用信息失败:', error)
  }
}

const fetchChatHistory = async () => {
  await chatStore.fetchChatHistory(appId)
  await nextTick()
  scrollToBottom()
}

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || sending.value) return

  chatStore.addUserMessage(content)
  inputMessage.value = ''
  sending.value = true

  await nextTick()
  scrollToBottom()

  try {
    chatStore.addAiMessage('')

    const eventSource = new EventSource(
      `${import.meta.env.VITE_API_BASE_URL}/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(content)}`,
      { withCredentials: true }
    )

    let fullResponse = ''

    eventSource.onmessage = (event) => {
      const data = event.data
      if (data === '[DONE]') {
        eventSource.close()
        sending.value = false
        fetchChatList()
        return
      }
      fullResponse += data
      chatStore.updateLastMessage(fullResponse)
      scrollToBottom()
    }

    eventSource.onerror = (error) => {
      console.error('SSE错误:', error)
      eventSource.close()
      sending.value = false
      message.error('AI响应失败，请重试')
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    sending.value = false
    message.error('发送失败，请重试')
  }
}

const formatMessage = (content: string) => {
  if (!content) return ''
  return content
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString('zh-CN')
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const fetchChatList = async () => {
  try {
    const { getChatHistory } = await import('@/api/chat')
    const res: any = await getChatHistory(appId, { pageSize: 50 })
    chatList.value = res.data?.records || []
  } catch (error) {
    console.error('获取对话列表失败:', error)
  }
}

const createNewChat = () => {
  chatStore.clearMessages()
  currentChatId.value = null
}

const switchChat = (chatId: number) => {
  currentChatId.value = chatId
}

const loadMore = async () => {
  await chatStore.loadMore(appId)
}

const goToEdit = () => {
  router.push(`/app/edit/${appId}`)
}

const handleDeploy = async () => {
  deploying.value = true
  try {
    await deployApp(appId)
    message.success('部署请求已提交')
  } catch (error) {
    message.error('部署失败')
  } finally {
    deploying.value = false
  }
}

watch(() => chatStore.messages, () => {
  nextTick(() => scrollToBottom())
}, { deep: true })

onMounted(() => {
  fetchAppInfo()
  fetchChatHistory()
  fetchChatList()
})
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 64px);
  background: var(--bg-page);
}

.chat-container {
  display: flex;
  height: 100%;
}

/* Sidebar */
.chat-sidebar {
  width: 260px;
  background: var(--bg-card);
  border-right: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.new-chat-btn {
  border-radius: 8px !important;
  font-size: 12px !important;
  height: 28px !important;
  padding: 0 10px !important;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.chat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-bottom: 2px;
}

.chat-item:hover {
  background: #f1f5f9;
}

.chat-item.active {
  background: rgba(59, 130, 246, 0.08);
}

.chat-item.active .chat-item-icon {
  color: var(--color-primary);
}

.chat-item-icon {
  font-size: 14px;
  color: #94a3b8;
  flex-shrink: 0;
}

.chat-item-info {
  min-width: 0;
  flex: 1;
}

.chat-item-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-item-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

/* Chat Main */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: 12px 24px;
  background: var(--bg-card);
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.app-info {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.app-avatar {
  flex-shrink: 0;
}

.app-details {
  min-width: 0;
}

.app-name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.app-description {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 350px;
}

.header-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.header-btn {
  border-radius: 8px !important;
  font-size: 12px !important;
  height: 30px !important;
}

/* Messages */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.messages-list {
  max-width: 780px;
  margin: 0 auto;
}

.loading-more,
.load-more-btn {
  text-align: center;
  padding: 12px;
  color: var(--text-muted);
  font-size: 13px;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
  border: none !important;
}

.msg-avatar.user {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
}

.msg-avatar.assistant {
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
  color: #fff;
}

.message-content {
  max-width: 70%;
}

.message-header {
  margin-bottom: 4px;
}

.message-item.user .message-header {
  text-align: right;
}

.message-sender {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}

.message-time {
  font-size: 11px;
  color: #94a3b8;
  margin-left: 8px;
}

.message-text {
  background: var(--bg-card);
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-primary);
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
}

.message-item.user .message-text {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  border: none;
}

.message-text.loading {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Typing indicator */
.typing-indicator {
  display: inline-flex;
  gap: 3px;
  align-items: center;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

/* Input */
.input-container {
  padding: 16px 24px;
  background: var(--bg-card);
  border-top: 1px solid #f1f5f9;
}

.input-wrapper {
  max-width: 780px;
  margin: 0 auto;
  position: relative;
}

.chat-input {
  border-radius: 14px !important;
  border-color: #e2e8f0 !important;
  padding-right: 100px;
  font-size: 14px;
}

.chat-input:focus {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.08) !important;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

.send-btn {
  border-radius: 10px !important;
  height: 32px !important;
  padding: 0 16px !important;
  font-size: 13px !important;
}

@media (max-width: 768px) {
  .chat-sidebar {
    display: none;
  }
  .chat-header {
    padding: 10px 16px;
  }
  .messages-container {
    padding: 16px;
  }
  .input-container {
    padding: 12px 16px;
  }
}
</style>
