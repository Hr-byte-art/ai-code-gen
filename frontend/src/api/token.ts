import request from '@/utils/request'
import type {
  ApiResponse,
  UserTokenSummaryDTO,
  TokenDetailDTO,
  SystemTokenSummaryDTO,
  ModelTokenSummaryDTO,
  TokenRankingDTO,
  ModelTokenRankingDTO,
  GenerationStatsDTO,
  PageResponse
} from '@/types'

// ==================== 用户Token接口 ====================

// 获取当前用户Token消耗汇总
export function getUserTokenSummary() {
  return request.get<ApiResponse<UserTokenSummaryDTO>>('/token/user/summary')
}

// 获取当前用户Token使用详情
export function getUserTokenDetails(params?: { page?: number; pageSize?: number }) {
  return request.get<ApiResponse<TokenDetailDTO[]>>('/token/user/details', { params })
}

// 获取指定用户Token消耗汇总（管理员）
export function getUserTokenSummaryById(userId: string) {
  return request.get<ApiResponse<UserTokenSummaryDTO>>(`/token/user/${userId}/summary`)
}

// 获取指定用户Token使用详情（管理员）
export function getUserTokenDetailsById(userId: string, params?: { page?: number; pageSize?: number }) {
  return request.get<ApiResponse<TokenDetailDTO[]>>(`/token/user/${userId}/details`, { params })
}

// ==================== 系统Token接口 ====================

// 获取系统总Token消耗统计（管理员）
export function getSystemTokenSummary(params?: { startTime?: string; endTime?: string }) {
  return request.get<ApiResponse<SystemTokenSummaryDTO>>('/token/system/summary', { params })
}

// ==================== 排行榜接口 ====================

// 获取用户Token消耗排行榜
export function getUserTokenRanking(params?: { page?: number; pageSize?: number }) {
  return request.get<ApiResponse<TokenRankingDTO>>('/token/ranking', { params })
}

// ==================== 模型Token接口 ====================

// 获取指定模型Token消耗汇总
export function getModelTokenSummary(modelName: string, params?: { startTime?: string; endTime?: string }) {
  return request.get<ApiResponse<ModelTokenSummaryDTO>>(`/token/model/${modelName}/summary`, { params })
}

// 获取模型Token消耗排行榜
export function getModelTokenRanking(params?: { startTime?: string; endTime?: string; limit?: number }) {
  return request.get<ApiResponse<ModelTokenRankingDTO>>('/token/model/ranking', { params })
}

// 获取所有模型Token消耗列表（管理员）
export function getAllModelTokenSummary(params?: { startTime?: string; endTime?: string }) {
  return request.get<ApiResponse<ModelTokenSummaryDTO[]>>('/token/model/all', { params })
}

// ==================== 应用Token接口 ====================

// 获取应用Token使用详情
export function getAppTokenDetails(appId: string, params?: { page?: number; pageSize?: number }) {
  return request.get<ApiResponse<TokenDetailDTO[]>>(`/token/app/${appId}/details`, { params })
}

// ==================== 生成统计接口 ====================

// 获取用户生成历史统计
export function getUserGenerationStats() {
  return request.get<ApiResponse<GenerationStatsDTO>>('/token/user/generation-stats')
}
