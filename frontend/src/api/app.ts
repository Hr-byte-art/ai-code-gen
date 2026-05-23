import request from '@/utils/request'
import type {
  ApiResponse,
  AppVO,
  AppAddRequest,
  AppUpdateRequest,
  AppAdminUpdateRequest,
  AppDeployRequest,
  AppQueryRequest,
  PageResponse
} from '@/types'

// ==================== 应用基础接口 ====================

// 创建应用
export function createApp(data: AppAddRequest) {
  return request.post<ApiResponse<number>>('/app/create', data)
}

// 更新应用（用户只能更新自己的应用名称）
export function updateApp(data: AppUpdateRequest) {
  return request.post<ApiResponse<boolean>>('/app/update', data)
}

// 删除应用（用户只能删除自己的应用）
export function deleteApp(id: number) {
  return request.post<ApiResponse<boolean>>('/app/delete', { id })
}

// 根据id获取应用详情
export function getAppById(id: number) {
  return request.get<ApiResponse<AppVO>>('/app/get/vo', { params: { id } })
}

// 分页获取当前用户创建的应用列表
export function getMyAppList(data: AppQueryRequest) {
  return request.post<ApiResponse<PageResponse<AppVO>>>('/app/my/list/page/vo', data)
}

// 分页获取精选应用列表
export function getGoodAppList(data: AppQueryRequest) {
  return request.post<ApiResponse<PageResponse<AppVO>>>('/app/good/list/page/vo', data)
}

// ==================== 应用操作接口 ====================

// 应用部署
export function deployApp(appId: number) {
  return request.post<ApiResponse<string>>('/app/deploy', { appId })
}

// 取消应用代码生成任务
export function cancelApp(appId: number) {
  return request.post<ApiResponse<boolean>>(`/app/cancel/${appId}`)
}

// 获取应用构建状态（轮询查询）
export function getAppBuildStatus(appId: number) {
  return request.get<ApiResponse<Record<string, any>>>(`/app/build/status/${appId}`)
}

// 应用下载
export function downloadApp(appId: number) {
  return request.get(`/app/download/${appId}`, { responseType: 'blob' })
}

// 上传图片
export function uploadPicture(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResponse<string>>('/app/upload/Pictures', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ==================== 管理员接口 ====================

// 分页获取应用列表（管理员）
export function getAdminAppList(data: AppQueryRequest) {
  return request.post<ApiResponse<PageResponse<AppVO>>>('/app/admin/list/page/vo', data)
}

// 管理员根据id获取应用详情
export function getAdminAppById(id: number) {
  return request.get<ApiResponse<AppVO>>('/app/admin/get/vo', { params: { id } })
}

// 更新应用（管理员）
export function adminUpdateApp(data: AppAdminUpdateRequest) {
  return request.post<ApiResponse<boolean>>('/app/admin/update', data)
}

// 删除应用（管理员）
export function adminDeleteApp(id: number) {
  return request.post<ApiResponse<boolean>>('/app/admin/delete', { id })
}

// 清除应用相关缓存（管理员）
export function clearAppCache() {
  return request.post<ApiResponse<boolean>>('/app/admin/cache/clear')
}
