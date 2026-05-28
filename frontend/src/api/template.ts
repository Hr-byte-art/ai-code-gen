import request from '@/utils/request'
import type { ApiResponse, CodeTemplate } from '@/types'

// 获取所有可用模板
export function listTemplates() {
  return request.get<ApiResponse<CodeTemplate[]>>('/template/list')
}

// 根据标识获取模板
export function getTemplate(key: string) {
  return request.get<ApiResponse<CodeTemplate>>('/template/get', { params: { key } })
}
