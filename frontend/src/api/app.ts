import request from '@/utils/request'
import type {
  ApiResponse,
  AppVO,
  AppAddRequest,
  AppUpdateRequest,
  AppAdminUpdateRequest,
  AppDeployRequest,
  AppQueryRequest,
  RoutingRecommendation,
  AppBuildStatus,
  GenerationTaskState,
  AppReviewReport,
  PageResponse
} from '@/types'

// ==================== 应用基础接口 ====================

// 获取路由推荐（AI 预判全栈/前端）
export function getRoutingRecommendation(data: AppAddRequest) {
  return request.post<ApiResponse<RoutingRecommendation>>('/app/routing/recommend', data)
}

// 创建应用
export function createApp(data: AppAddRequest) {
  return request.post<ApiResponse<number>>('/app/create', data)
}

// 更新应用（用户只能更新自己的应用名称）
export function updateApp(data: AppUpdateRequest) {
  return request.post<ApiResponse<boolean>>('/app/update', data)
}

// 删除应用（用户只能删除自己的应用）
export function deleteApp(id: string) {
  return request.post<ApiResponse<boolean>>('/app/delete', { id })
}

// 根据id获取应用详情
export function getAppById(id: string) {
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
export async function deployApp(appId: string): Promise<string | null> {
  const res: any = await request.post<ApiResponse<string>>('/app/deploy', { appId })
  // Axios 拦截器已提取 response.data，所以 res 就是 { code, data, message }
  return res?.data || null
}

// 查询已部署访问地址，不触发构建或启动
export async function getDeployedAppUrl(appId: string): Promise<string | null> {
  const res: any = await request.get<ApiResponse<string>>('/app/deploy/url', { params: { appId } })
  return res?.data || null
}

// 获取应用代码审查报告
export async function getAppReviewReport(appId: string): Promise<AppReviewReport> {
  const res: any = await request.get<ApiResponse<AppReviewReport>>(`/app/review/report/${appId}`)
  return res.data
}

// 取消应用代码生成任务
export function cancelApp(appId: string) {
  return request.post<ApiResponse<boolean>>(`/app/cancel/${appId}`)
}

// 获取应用构建状态（轮询查询）
export async function getAppBuildStatus(appId: string): Promise<AppBuildStatus> {
  const res: any = await request.get<ApiResponse<AppBuildStatus>>(`/app/build/status/${appId}`)
  return res.data
}

// 重新构建应用
export async function rebuildApp(appId: string): Promise<AppBuildStatus> {
  const res: any = await request.post<ApiResponse<AppBuildStatus>>(`/app/build/rebuild/${appId}`)
  return res.data
}

// 查询是否存在运行中的代码生成流
export function hasActiveGenerationStream(appId: string) {
  return request.get<ApiResponse<boolean>>('/app/chat/gen/active', { params: { appId } })
}

// 查询生成任务状态
export async function getGenerationTaskState(appId: string): Promise<GenerationTaskState> {
  const res: any = await request.get<ApiResponse<GenerationTaskState>>(`/app/generation/status/${appId}`)
  return res.data
}

// 应用下载
export function downloadApp(appId: string) {
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
export function getAdminAppById(id: string) {
  return request.get<ApiResponse<AppVO>>('/app/admin/get/vo', { params: { id } })
}

// 更新应用（管理员）
export function adminUpdateApp(data: AppAdminUpdateRequest) {
  return request.post<ApiResponse<boolean>>('/app/admin/update', data)
}

// 删除应用（管理员）
export function adminDeleteApp(id: string) {
  return request.post<ApiResponse<boolean>>('/app/admin/delete', { id })
}

// 清除应用相关缓存（管理员）
export function clearAppCache() {
  return request.post<ApiResponse<boolean>>('/app/admin/cache/clear')
}
