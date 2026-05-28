import request from '@/utils/request'
import type { ApiResponse, UserQuotaVO } from '@/types'

// 获取当前用户配额使用情况
export function getMyQuota() {
  return request.get<ApiResponse<UserQuotaVO>>('/quota/my')
}
