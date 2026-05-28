import request from '@/utils/request'
import type {
  ApiResponse,
  ChatHistory,
  ChatHistoryQueryRequest,
  PageResponse
} from '@/types'

// ==================== 对话历史接口 ====================

// 分页查询某个应用的对话历史（游标查询）
export function getChatHistory(appId: string, params?: {
  pageSize?: number
  lastCreateTime?: string
}) {
  return request.get<ApiResponse<PageResponse<ChatHistory>>>(`/chatHistory/app/${appId}`, { params })
}

// ==================== 管理员接口 ====================

// 管理员分页查询所有对话历史
export function getAdminChatHistory(data: ChatHistoryQueryRequest) {
  return request.post<ApiResponse<PageResponse<ChatHistory>>>('/chatHistory/admin/list/page/vo', data)
}
