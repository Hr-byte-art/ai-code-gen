<template>
  <div class="admin-page">
    <PageHeader title="聊天详情" description="只读查看某个应用的完整对话记录，适合排查生成链路和审计问题。" eyebrow="后台" />

    <div class="detail-actions">
      <a-button @click="goBack">返回聊天管理</a-button>
      <a-tag color="blue">只读模式</a-tag>
    </div>

    <a-spin :spinning="loadingApp">
      <div class="app-card">
        <div class="app-main">
          <a-avatar :size="42" :src="appInfo?.cover">
            <template #icon><AppstoreOutlined /></template>
          </a-avatar>
          <div class="app-copy">
            <div class="app-name">{{ appInfo?.appName || '应用信息加载中' }}</div>
            <div class="app-desc">{{ appInfo?.initPrompt || '-' }}</div>
          </div>
        </div>
        <div class="app-meta">
          <span>应用 ID：{{ appId }}</span>
          <span>用户 ID：{{ appInfo?.userId || '-' }}</span>
          <span>生成类型：{{ appInfo?.codeGenType || '默认' }}</span>
          <span>创建时间：{{ ft(appInfo?.createTime) }}</span>
        </div>
      </div>
    </a-spin>

    <a-alert
      class="readonly-alert"
      type="info"
      show-icon
      message="当前页面仅用于查看历史对话，不提供输入框、发送、继续生成或部署操作。"
    />

    <div class="card">
      <div class="card-head">
        <div>
          <div class="section-title">对话记录</div>
          <div class="section-desc">按时间正序展示，方便还原用户与 AI 的完整交互链路。</div>
        </div>
        <a-segmented v-model:value="messageType" :options="messageTypeOptions" size="small" @change="handleTypeChange" />
      </div>

      <a-spin :spinning="loadingChats">
        <a-empty v-if="chatList.length === 0" description="暂无对话记录" />
        <div v-else class="timeline">
          <div v-for="item in chatList" :key="item.id" :class="['chat-item', item.messageType]">
            <div :class="['chat-avatar', item.messageType]">
              <UserOutlined v-if="item.messageType === 'user'" />
              <RobotOutlined v-else />
            </div>
            <div class="chat-body">
              <div class="chat-head">
                <div class="chat-name">
                  {{ item.messageType === 'user' ? '用户消息' : 'AI 回复' }}
                  <a-tag :color="item.messageType === 'user' ? 'blue' : 'green'" class="type-tag">
                    {{ item.messageType === 'user' ? 'USER' : 'AI' }}
                  </a-tag>
                </div>
                <div class="chat-time">{{ ft(item.createTime) }}</div>
              </div>
              <MarkdownRenderer v-if="item.messageType === 'ai'" class="chat-markdown" :content="item.message" />
              <pre v-else class="chat-message">{{ item.message }}</pre>
            </div>
          </div>
        </div>
      </a-spin>

      <div class="pager">
        <a-pagination
          v-model:current="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :show-size-changer="true"
          :page-size-options="['10', '20', '50', '100']"
          :show-total="(total: number) => `共 ${total} 条`"
          @change="fetchChatList"
          @showSizeChange="fetchChatList"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AppstoreOutlined, RobotOutlined, UserOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import MarkdownRenderer from '@/components/common/MarkdownRenderer.vue'
import { getAdminAppById } from '@/api/app'
import { getAdminChatHistory } from '@/api/chat'
import { formatDateTime as ft } from '@/utils/time'
import type { AppVO, ChatHistory } from '@/types'

const route = useRoute()
const router = useRouter()

const appId = computed(() => route.params.appId as string)
const loadingApp = ref(false)
const loadingChats = ref(false)
const appInfo = ref<AppVO | null>(null)
const chatList = ref<ChatHistory[]>([])
const messageType = ref('')

const messageTypeOptions = [
  { label: '全部', value: '' },
  { label: '用户', value: 'user' },
  { label: 'AI', value: 'ai' },
]

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

const fetchAppInfo = async () => {
  loadingApp.value = true
  try {
    const res: any = await getAdminAppById(appId.value)
    appInfo.value = res.data || null
  } catch (e) {
    router.push('/admin/chatManage')
  } finally {
    loadingApp.value = false
  }
}

const fetchChatList = async () => {
  loadingChats.value = true
  try {
    const res: any = await getAdminChatHistory({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      appId: appId.value,
      messageType: messageType.value || undefined,
      sortField: 'createTime',
      sortOrder: 'ascend',
    })
    chatList.value = res.data?.records || []
    pagination.total = res.data?.totalRow || 0
  } finally {
    loadingChats.value = false
  }
}

const handleTypeChange = () => {
  pagination.current = 1
  fetchChatList()
}

const goBack = () => {
  router.push('/admin/chatManage')
}

onMounted(() => {
  fetchAppInfo()
  fetchChatList()
})
</script>

<style scoped>
.admin-page {
  max-width: 1200px;
  margin: 0 auto;
}

.detail-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.app-card,
.card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--r-lg);
  padding: 16px;
}

.app-card {
  margin-bottom: 12px;
}

.app-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.app-copy {
  min-width: 0;
}

.app-name {
  font-size: 16px;
  font-weight: 800;
  color: var(--t-primary);
  margin-bottom: 6px;
}

.app-desc {
  font-size: 13px;
  line-height: 1.7;
  color: var(--t-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.app-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-top: 14px;
  font-size: 12px;
  color: var(--t-light);
}

.readonly-alert {
  margin-bottom: 12px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 800;
  color: var(--t-primary);
}

.section-desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--t-light);
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chat-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.chat-avatar {
  width: 30px;
  height: 30px;
  border-radius: var(--r-full);
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  margin-top: 3px;
}

.chat-avatar.user {
  color: #1677ff;
  background: #e6f4ff;
}

.chat-avatar.ai {
  color: #389e0d;
  background: #f6ffed;
}

.chat-body {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--border-light);
  border-radius: var(--r-lg);
  padding: 12px;
  background: var(--bg-soft);
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.chat-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 800;
  color: var(--t-primary);
}

.type-tag {
  margin-inline-end: 0;
}

.chat-time {
  font-size: 12px;
  color: var(--t-light);
  white-space: nowrap;
}

.chat-message {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.7;
  color: var(--t-secondary);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 768px) {
  .card-head,
  .chat-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .detail-actions {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }
}
</style>