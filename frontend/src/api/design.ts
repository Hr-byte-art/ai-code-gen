import request from '@/utils/request'
import type { ApiResponse } from '@/types'

export interface DesignTemplateInfo {
  key: string
  name: string
  description: string
}

// 获取所有设计模板列表
export function getDesignTemplateList() {
  return request.get<ApiResponse<DesignTemplateInfo[]>>('/design/list')
}

// 获取设计模板内容
export function getDesignTemplate(key: string) {
  return request.get<ApiResponse<string>>('/design/get', { params: { key } })
}
