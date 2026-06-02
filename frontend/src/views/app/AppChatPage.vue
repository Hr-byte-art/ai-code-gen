<template>
  <div class="generation-page">
    <section class="work-context">
      <div class="context-main">
        <div class="asset-mark">
          <AppstoreOutlined />
        </div>
        <div class="context-copy">
          <div class="context-eyebrow">生成工作台</div>
          <h1 class="context-title">{{ appInfo.appName || '加载应用中' }}</h1>
          <div class="context-meta">
            <span :class="['status-pill', appInfo.deployedTime ? 'online' : 'draft']">
              {{ appInfo.deployedTime ? '已上线' : '迭代中' }}
            </span>
            <span v-if="appInfo.codeGenType" class="meta-chip">{{ appInfo.codeGenType }}</span>
            <span class="meta-chip">应用 ID {{ appId }}</span>
          </div>
        </div>
      </div>
      <div class="context-actions">
        <a-button @click="goToEdit" :disabled="deploying || sending"><EditOutlined /> 交付页</a-button>
        <a-button @click="goToPreview" :disabled="deploying || sending"><EyeOutlined /> 预览</a-button>
        <a-button type="primary" @click="handleDeploy" :loading="deploying" :disabled="sending">
          <CloudUploadOutlined /> {{ deploying ? '部署中...' : (appInfo.deployedTime ? '重新部署' : '部署') }}
        </a-button>
      </div>
    </section>

    <section class="workspace-grid">
      <aside class="brief-panel">
        <div class="panel-section">
          <div class="panel-title">当前阶段</div>
          <div class="stage-list">
            <div class="stage-item done">
              <span class="stage-index">01</span>
              <span>需求已建档</span>
            </div>
            <div class="stage-item active">
              <span class="stage-index">02</span>
              <span>对话迭代</span>
            </div>
            <div :class="['stage-item', appInfo.deployKey ? 'done' : '']">
              <span class="stage-index">03</span>
              <span>预览确认</span>
            </div>
            <div :class="['stage-item', appInfo.deployedTime ? 'done' : '']">
              <span class="stage-index">04</span>
              <span>部署交付</span>
            </div>
          </div>
        </div>

        <div class="panel-section">
          <div class="panel-title">可直接这样说</div>
          <button v-for="item in promptHints" :key="item" class="hint-btn" @click="useHint(item)">
            {{ item }}
          </button>
        </div>
      </aside>

      <main class="conversation-panel">
        <div class="messages" ref="messagesContainer">
          <div class="messages-inner">
            <div v-if="chatStore.messages.length === 0 && !sending" class="chat-empty">
              <div class="empty-icon"><RobotOutlined /></div>
              <h3 class="empty-title">从一个明确修改点开始</h3>
              <p class="empty-desc">比如先补页面结构、调整配色、增加登录态提示，或者要求重新生成某个模块。</p>
            </div>

            <div v-if="chatStore.loading" class="load-more"><a-spin size="small" /> 加载历史记录</div>
            <div v-if="chatStore.pagination.hasMore && !chatStore.loading" class="load-more">
              <a-button type="link" size="small" @click="loadMore">加载更早记录</a-button>
            </div>

            <div v-for="msg in chatStore.messages" :key="msg.id" :class="['msg', msg.messageType]">
              <div :class="['msg-rail', msg.messageType]">
                <UserOutlined v-if="msg.messageType === 'user'" />
                <RobotOutlined v-else />
              </div>
              <div class="msg-body">
                <div class="msg-head">
                  <span class="msg-name">{{ msg.messageType === 'user' ? '你提交的修改' : '生成反馈' }}</span>
                  <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
                </div>
                <MarkdownRenderer v-if="msg.messageType === 'ai'" :content="msg.message" />
                <div v-else class="msg-text">{{ msg.message }}</div>
              </div>
            </div>

            <div v-if="sending" class="msg assistant">
              <div class="msg-rail assistant"><RobotOutlined /></div>
              <div class="msg-body">
                <div class="msg-head">
                  <span class="msg-name">生成反馈</span>
                </div>
                <div class="msg-text typing">
                  <span class="dots"><i></i><i></i><i></i></span>
                  正在生成并写入应用资产
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="composer">
          <div class="composer-head">
            <span>下一条修改指令</span>
            <span>Enter 发送</span>
          </div>
          <div class="quick-actions">
            <button class="quick-btn" @click="inputMessage = '把页面配色改成深色主题'">深色主题</button>
            <button class="quick-btn" @click="inputMessage = '把导航栏改成顶部固定布局'">顶部导航</button>
            <button class="quick-btn" @click="inputMessage = '添加一个登录弹窗'">登录弹窗</button>
            <button class="quick-btn" @click="inputMessage = '优化移动端响应式布局'">移动端适配</button>
          </div>
          <div class="input-wrap">
            <a-textarea
              v-model:value="inputMessage"
              :rows="3"
              placeholder="写清楚你想改哪里、改成什么效果。"
              @keydown.enter.exact.prevent="handleSendMessage"
              :disabled="sending"
              class="input"
              ref="inputTextareaRef"
            />
            <a-button type="primary" :loading="sending" @click="handleSendMessage" class="send-btn">
              <SendOutlined /> 发送
            </a-button>
          </div>
        </div>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  AppstoreOutlined, EditOutlined, EyeOutlined, CloudUploadOutlined,
  UserOutlined, RobotOutlined, SendOutlined
} from '@ant-design/icons-vue'
import { useChatStore } from '@/stores/chat'
import { getAppById, deployApp, hasActiveGenerationStream } from '@/api/app'
import MarkdownRenderer from '@/components/common/MarkdownRenderer.vue'
import { formatDateTime as formatTime } from '@/utils/time'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()

const appId = route.params.id as string
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
const appInfo = ref<any>({})
const inputMessage = ref('')
const sending = ref(false)
const deploying = ref(false)
const messagesContainer = ref<HTMLElement>()
const inputTextareaRef = ref()
let activeStream: EventSource | null = null

const promptHints = [
  '把首页首屏改得更像真实产品官网',
  '补一个空状态和错误提示',
  '调整移动端布局，优先保证可读性',
  '把按钮和表单文案改得更具体',
]

const fetchAppInfo = async () => {
  try {
    const res = await getAppById(appId)
    appInfo.value = res.data

    // 全栈项目：自动获取 Express URL
    if (appInfo.value?.codeGenType === 'fullstack' && appInfo.value?.deployKey) {
      try {
        const url = await deployApp(appId)
        if (url) expressDeployUrl.value = url
      } catch (e) { /* Express 启动失败不影响页面加载 */ }
    }
  } catch (e) {
    message.error('应用不存在或已被删除')
    router.push('/app')
  }
}

const fetchChatHistory = async () => {
  await chatStore.fetchChatHistory(appId)
  await nextTick()
  scrollToBottom()
}

const normalizeStreamChunk = (data: string) => {
  if (!data || data === '[DONE]') return ''
  try {
    const parsed = JSON.parse(data)
    const type = parsed?.type
    // AI 响应：提取文本内容
    if (type === 'ai_response') {
      return typeof parsed?.data === 'string' ? parsed.data : ''
    }
    // 代码审查结果：格式化显示
    if (type === 'review_result') {
      const passed = parsed.passed ? '通过' : '未通过'
      const score = parsed.score ?? 0
      const summary = parsed.summary || ''
      return `\n\n---\n**代码审查** — ${passed}（${score}分）${summary ? '：' + summary : ''}\n---\n\n`
    }
    // 工具请求/执行：不显示在聊天中
    if (type === 'tool_request' || type === 'tool_executed') {
      return ''
    }
    return typeof parsed?.d === 'string' ? parsed.d : data
  } catch {
    return data
  }
}

const parseStreamErrorMessage = (data: string) => {
  if (!data) return '生成失败，请稍后重试'
  try {
    const parsed = JSON.parse(data)
    return parsed?.message || parsed?.error || '生成失败，请稍后重试'
  } catch {
    return data
  }
}

const appendStreamChunk = (rawData: string, currentContent: string) => {
  const chunk = normalizeStreamChunk(rawData)
  if (!chunk) return currentContent
  const nextContent = currentContent + chunk
  chatStore.updateLastMessage(nextContent)
  scrollToBottom()
  return nextContent
}

const bindGenerationStream = (es: EventSource, initialContent = '', showErrorToast = true) => {
  activeStream?.close()
  activeStream = es
  sending.value = true
  let full = initialContent
  const handleChunk = (e: MessageEvent) => {
    full = appendStreamChunk(e.data, full)
  }
  es.addEventListener('message', handleChunk)
  es.addEventListener('chunk', handleChunk)
  es.addEventListener('bizError', (e) => {
    const errorMessage = parseStreamErrorMessage((e as MessageEvent).data)
    es.close()
    if (activeStream === es) activeStream = null
    chatStore.updateLastMessage(errorMessage)
    if (showErrorToast) message.error(errorMessage)
    finishSending()
  })
  es.addEventListener('done', () => {
    es.close()
    if (activeStream === es) activeStream = null
    finishSending()
  })
  es.onerror = () => {
    es.close()
    if (activeStream === es) activeStream = null
    finishSending()
    if (showErrorToast) message.error('生成响应失败')
  }
}

const finishSending = async () => {
  sending.value = false
  await fetchAppInfo()
}

const clearInputMessage = async () => {
  inputMessage.value = ''
  await nextTick()
  const textarea = inputTextareaRef.value?.resizableTextArea?.textArea
  if (textarea instanceof HTMLTextAreaElement) textarea.value = ''
}

const handleSendMessage = () => {
  sendMessage()
}

const sendMessage = async (presetContent?: string) => {
  const content = (typeof presetContent === 'string' ? presetContent : inputMessage.value).trim()
  if (!content || sending.value) return
  chatStore.addUserMessage(content)
  await clearInputMessage()
  sending.value = true
  await nextTick()
  scrollToBottom()
  try {
    chatStore.addAiMessage('')
    const es = new EventSource(
      `${apiBaseUrl}/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(content)}`,
      { withCredentials: true }
    )
    bindGenerationStream(es)
  } catch (e) { sending.value = false; message.error('发送失败') }
}

const scrollToBottom = () => {
  if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
}

const useHint = (hint: string) => { inputMessage.value = hint }
const goToEdit = () => router.push(`/app/edit/${appId}`)
const goToPreview = async () => {
  if (expressDeployUrl.value) {
    window.open(expressDeployUrl.value, '_blank')
  } else if (appInfo.value.deployKey) {
    // 全栈项目需要通过 deployApp 获取 Express URL
    if (appInfo.value.codeGenType === 'fullstack') {
      const url = await deployApp(appId)
      if (url) { expressDeployUrl.value = url; window.open(url, '_blank'); return }
    }
    window.open(`/api/code_deploy/${appInfo.value.deployKey}/index.html`, '_blank')
  } else {
    window.open(`/api/static/preview/${appId}/index.html`, '_blank')
  }
}
const expressDeployUrl = ref<string>('')
const handleDeploy = async () => {
  deploying.value = true
  try {
    const url = await deployApp(appId)
    if (url) expressDeployUrl.value = url
    message.success('部署成功！')
    await fetchAppInfo()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '部署失败，请重试')
  } finally { deploying.value = false }
}

const loadMore = async () => { await chatStore.loadMore(appId) }

const autoGenerateFromInitPrompt = async () => {
  if (route.query.autoGenerate !== '1') return
  if (!appInfo.value?.initPrompt || chatStore.messages.length > 0) return
  router.replace({ path: route.path, query: {} })
  await sendMessage(appInfo.value.initPrompt)
}

const resumeActiveGenerationStream = async () => {
  if (route.query.autoGenerate === '1' || sending.value) return
  try {
    const res = await hasActiveGenerationStream(appId)
    if (!res.data) return
    chatStore.addAiMessage('')
    await nextTick()
    scrollToBottom()
    const es = new EventSource(
      `${apiBaseUrl}/app/chat/gen/stream?appId=${appId}`,
      { withCredentials: true }
    )
    bindGenerationStream(es, '', false)
  } catch (e) {
    // 恢复订阅失败不影响历史消息展示
  }
}

watch(() => chatStore.messages, () => nextTick(scrollToBottom), { deep: true })
onMounted(async () => {
  await fetchAppInfo()
  await fetchChatHistory()
  await autoGenerateFromInitPrompt()
  await resumeActiveGenerationStream()
})

onUnmounted(() => {
  activeStream?.close()
  activeStream = null
})
</script>

<style scoped>
.generation-page {
  min-height: calc(100vh - var(--header-height));
  padding: 22px;
}

.work-context {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  max-width: 1360px;
  margin: 0 auto 18px;
  padding: 16px 18px;
  border: 1px solid var(--border-light);
  border-radius: var(--r-xl);
  background: var(--bg-card);
}

.context-main {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.asset-mark {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 15px;
  color: var(--c-primary);
  background: var(--c-primary-50);
  border: 1px solid var(--c-primary-100);
  font-size: 18px;
}

.context-copy {
  min-width: 0;
}

.context-eyebrow {
  color: var(--t-light);
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.08em;
}

.context-title {
  margin: 3px 0 7px;
  color: var(--t-primary);
  font-size: 22px;
  font-weight: 850;
  line-height: 1.15;
  letter-spacing: -0.5px;
}

.context-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
}

.status-pill,
.meta-chip {
  padding: 3px 8px;
  border-radius: var(--r-full);
  font-size: 11px;
  font-weight: 750;
}

.status-pill.online {
  color: var(--c-success);
  background: #eaf4ee;
  border: 1px solid #cfe5d8;
}

.status-pill.draft {
  color: var(--c-warning);
  background: #f6ede1;
  border: 1px solid #e8d4bb;
}

.meta-chip {
  color: var(--t-muted);
  background: var(--bg-soft);
  border: 1px solid var(--border-light);
}

.context-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.workspace-grid {
  max-width: 1360px;
  height: calc(100vh - var(--header-height) - 118px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 18px;
  min-height: 600px;
}

.brief-panel,
.conversation-panel {
  min-height: 0;
  border: 1px solid var(--border-light);
  border-radius: var(--r-xl);
  background: var(--bg-card);
}

.brief-panel {
  padding: 16px;
}

.panel-section + .panel-section {
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}

.panel-title {
  margin-bottom: 12px;
  color: var(--t-primary);
  font-size: 13px;
  font-weight: 850;
}

.stage-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stage-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 10px;
  border-radius: var(--r-md);
  color: var(--t-muted);
  background: transparent;
  font-size: 13px;
  font-weight: 700;
}

.stage-item.active {
  color: var(--c-primary);
  background: var(--c-primary-50);
}

.stage-item.done {
  color: var(--t-secondary);
}

.stage-index {
  color: var(--t-light);
  font-size: 11px;
  font-weight: 850;
}

.hint-btn {
  width: 100%;
  display: block;
  text-align: left;
  padding: 10px 11px;
  border: 1px solid var(--border-light);
  border-radius: var(--r-md);
  background: var(--bg-soft);
  color: var(--t-secondary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 650;
  line-height: 1.55;
  transition: border-color var(--t-fast), background var(--t-fast), color var(--t-fast);
}

.hint-btn + .hint-btn {
  margin-top: 8px;
}

.hint-btn:hover {
  color: var(--c-primary);
  border-color: var(--c-primary-200);
  background: var(--c-primary-50);
}

.conversation-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 22px;
}

.messages-inner {
  max-width: 880px;
  margin: 0 auto;
}

.chat-empty {
  max-width: 460px;
  margin: 80px auto;
  text-align: center;
}

.empty-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: var(--c-primary-50);
  color: var(--c-primary);
  font-size: 22px;
}

.empty-title {
  margin: 0 0 8px;
  color: var(--t-primary);
  font-size: 18px;
  font-weight: 850;
}

.empty-desc {
  margin: 0;
  color: var(--t-muted);
  font-size: 14px;
  line-height: 1.7;
}

.load-more {
  text-align: center;
  padding: 10px;
  color: var(--t-muted);
  font-size: 13px;
}

.msg {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 11px;
  margin-bottom: 18px;
}

.msg-rail {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  background: var(--bg-soft);
  color: var(--t-muted);
}

.msg-rail.user {
  color: var(--c-primary);
  background: var(--c-primary-50);
}

.msg-rail.assistant {
  color: var(--c-cta);
  background: #f6ede1;
}

.msg-body {
  min-width: 0;
}

.msg-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 5px;
}

.msg-name {
  font-size: 12px;
  font-weight: 850;
  color: var(--t-secondary);
}

.msg-time {
  font-size: 11px;
  color: var(--t-light);
}

.msg-text {
  width: fit-content;
  max-width: 100%;
  padding: 11px 13px;
  border-radius: var(--r-lg);
  background: var(--bg-soft);
  border: 1px solid var(--border-light);
  color: var(--t-primary);
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg.user .msg-text {
  background: var(--c-primary-50);
  border-color: var(--c-primary-100);
  color: var(--t-primary);
}

.msg.assistant .msg-text {
  background: var(--bg-card);
}

.msg-text.typing {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dots {
  display: inline-flex;
  gap: 3px;
}

.dots i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--c-primary);
  display: block;
  animation: dotPulse 1.4s infinite;
}

.dots i:nth-child(2) { animation-delay: 0.2s; }
.dots i:nth-child(3) { animation-delay: 0.4s; }

@keyframes dotPulse {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.3; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.composer {
  padding: 14px 18px 18px;
  border-top: 1px solid var(--border-light);
  background: color-mix(in srgb, var(--bg-card) 86%, var(--bg-soft));
}

.composer-head {
  max-width: 880px;
  margin: 0 auto 8px;
  display: flex;
  justify-content: space-between;
  color: var(--t-light);
  font-size: 11px;
  font-weight: 750;
}

.quick-actions {
  max-width: 880px;
  margin: 0 auto 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.quick-btn {
  padding: 5px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-primary);
  background: var(--c-primary-50);
  border: 1px solid var(--c-primary-200);
  border-radius: var(--r-md);
  cursor: pointer;
  transition: all var(--t-fast);
}

.quick-btn:hover {
  background: var(--c-primary-100);
  border-color: var(--c-primary-300);
}

.input-wrap {
  max-width: 880px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: end;
}

.input {
  font-size: 14px !important;
}

.send-btn {
  height: 44px;
  padding: 0 18px !important;
}

@media (max-width: 980px) {
  .workspace-grid {
    height: auto;
    grid-template-columns: 1fr;
  }

  .brief-panel {
    order: 2;
  }

  .conversation-panel {
    min-height: 640px;
  }
}

@media (max-width: 768px) {
  .generation-page { padding: 14px; }
  .work-context { align-items: flex-start; flex-direction: column; }
  .context-actions { width: 100%; flex-wrap: wrap; }
  .input-wrap { grid-template-columns: 1fr; }
  .send-btn { width: 100%; }
}
</style>