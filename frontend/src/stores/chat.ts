import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getChatHistory } from '@/api/chat'
import { message } from 'ant-design-vue'

export const useChatStore = defineStore('chat', () => {
  // 状态
  const messages = ref<any[]>([])
  const loading = ref(false)
  const sending = ref(false)

  // 分页参数（游标查询）
  const pagination = ref({
    pageSize: 20,
    lastCreateTime: null as string | null,
    hasMore: true
  })

  // 获取对话历史
  async function fetchChatHistory(appId: string, isLoadMore = false) {
    if (loading.value) return

    loading.value = true
    try {
      const params: any = {
        pageSize: pagination.value.pageSize
      }

      // 如果是加载更多，传入最后一条消息的创建时间
      if (isLoadMore && pagination.value.lastCreateTime) {
        params.lastCreateTime = pagination.value.lastCreateTime
      }

      const res: any = await getChatHistory(appId, params)
      const records = res.data?.records || []

      if (isLoadMore) {
        // 加载更多时追加到列表前面（历史消息在前）
        messages.value = [...records.reverse(), ...messages.value]
      } else {
        // 首次加载，反转顺序（后端返回的是倒序）
        messages.value = records.reverse()
      }

      // 更新分页参数
      if (records.length > 0) {
        pagination.value.lastCreateTime = records[0].createTime
        pagination.value.hasMore = records.length >= pagination.value.pageSize
      } else {
        pagination.value.hasMore = false
      }

      return records
    } catch (error) {
      console.error('获取对话历史失败:', error)
      return []
    } finally {
      loading.value = false
    }
  }

  // 加载更多历史消息
  async function loadMore(appId: string) {
    if (!pagination.value.hasMore) return
    return await fetchChatHistory(appId, true)
  }

  // 添加用户消息（本地）
  function addUserMessage(content: string) {
    messages.value.push({
      id: Date.now(),
      message: content,
      messageType: 'user',
      createTime: new Date().toISOString()
    })
  }

  // 添加AI消息（本地）
  function addAiMessage(content: string) {
    messages.value.push({
      id: Date.now() + 1,
      message: content,
      messageType: 'ai',
      thinking: '',
      createTime: new Date().toISOString()
    })
  }

  // 更新最后一条消息（用于流式响应）
  function updateLastMessage(content: string) {
    if (messages.value.length > 0) {
      messages.value[messages.value.length - 1].message = content
    }
  }

  // 更新最后一条消息的思考内容
  function updateLastThinking(content: string) {
    if (messages.value.length > 0) {
      messages.value[messages.value.length - 1].thinking = content
    }
  }

  // 清空消息
  function clearMessages() {
    messages.value = []
    pagination.value.lastCreateTime = null
    pagination.value.hasMore = true
  }

  // 重置分页
  function resetPagination() {
    pagination.value.lastCreateTime = null
    pagination.value.hasMore = true
  }

  return {
    messages,
    loading,
    sending,
    pagination,
    fetchChatHistory,
    loadMore,
    addUserMessage,
    addAiMessage,
    updateLastMessage,
    updateLastThinking,
    clearMessages,
    resetPagination
  }
})
