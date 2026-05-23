// ==================== 通用类型 ====================

export interface ApiResponse<T> {
  code: number
  data: T
  message?: string
}

export interface PageResponse<T> {
  records: T[]
  pageNumber: number
  pageSize: number
  totalPage: number
  totalRow: number
  optimizeCountQuery?: boolean
}

export interface PageRequest {
  pageNum?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
}

// ==================== 用户相关类型 ====================

export interface LoginUser {
  id: number
  userAccount: string
  userName: string
  userAvatar: string
  userProfile: string
  userRole: string
  createTime: string
  updateTime: string
  vipExpireTime: string
  vipNumber: number
  shareCode: string
  integral: number
  recentlySignedIn: string
}

export interface UserVO {
  id: number
  userAccount: string
  userName: string
  userAvatar: string
  userProfile: string
  userRole: string
  createTime: string
  vipExpireTime: string
  vipCode: string
  vipNumber: number
  shareCode: string
  inviteUser: number
  integral: number
}

export interface UserLoginRequest {
  userAccount: string
  userPassword: string
}

export interface UserRegisterRequest {
  userAccount: string
  userPassword: string
  checkPassword: string
  shareCode?: string
}

export interface UserUpdateRequest {
  id: number
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface UserQueryRequest extends PageRequest {
  id?: number
  userName?: string
  userAccount?: string
  userProfile?: string
  userRole?: string
}

// ==================== 应用相关类型 ====================

export interface AppVO {
  id: number
  appName: string
  cover: string
  initPrompt: string
  codeGenType: string
  deployKey: string
  deployedTime: string
  priority: number
  userId: number
  createTime: string
  updateTime: string
  user: UserVO
}

export interface AppAddRequest {
  initPrompt: string
}

export interface AppUpdateRequest {
  id: number
  appName: string
}

export interface AppAdminUpdateRequest {
  id: number
  appName?: string
  cover?: string
  priority?: number
}

export interface AppDeployRequest {
  appId: number
}

export interface AppQueryRequest extends PageRequest {
  id?: number
  appName?: string
  cover?: string
  initPrompt?: string
  codeGenType?: string
  deployKey?: string
  priority?: number
  userId?: number
}

// ==================== Token相关类型 ====================

export interface UserTokenSummaryDTO {
  userId: number
  userName: string
  userAvatar: string
  totalInputTokens: number
  totalOutputTokens: number
  totalTokens: number
  appCount: number
  lastUsedTime: string
  primaryModel: string
  ranking: number
}

export interface TokenDetailDTO {
  id: number
  userId: number
  userName: string
  appId: number
  appName: string
  modelName: string
  aiCallPurpose: string
  inputTokens: number
  outputTokens: number
  totalTokens: number
  createTime: string
  purposeDescription: string
}

export interface SystemTokenSummaryDTO {
  totalInputTokens: number
  totalOutputTokens: number
  totalTokens: number
  totalUsers: number
  totalApps: number
  totalCalls: number
  tokenByPurpose: Record<string, number>
  tokenByModel: Record<string, number>
  statisticsStartTime: string
  statisticsEndTime: string
  avgTokenPerUser: number
  avgTokenPerApp: number
}

export interface ModelTokenSummaryDTO {
  modelName: string
  modelDisplayName: string
  totalInputTokens: number
  totalOutputTokens: number
  totalTokens: number
  callCount: number
  userCount: number
  appCount: number
  avgTokensPerCall: number
  avgTokensPerUser: number
  firstUsedTime: string
  lastUsedTime: string
  ranking: number
  percentage: number
  purposeDistribution: string
  modelProvider: string
  estimatedCost: number
}

export interface TokenRankingDTO {
  rankings: UserTokenSummaryDTO[]
  totalUsers: number
  currentPage: number
  pageSize: number
  totalPages: number
  rankingType: string
}

export interface ModelTokenRankingDTO {
  rankings: ModelTokenSummaryDTO[]
  totalModels: number
  statisticsStartTime: string
  statisticsEndTime: string
  totalTokens: number
  totalCalls: number
  rankingType: string
  mostActiveModel: string
  mostEfficientModel: string
}

// ==================== 对话历史类型 ====================

export interface ChatHistory {
  id: number
  message: string
  messageType: string
  appId: number
  userId: number
  createTime: string
  updateTime: string
  isDelete: number
}

export interface ChatHistoryQueryRequest extends PageRequest {
  id?: number
  message?: string
  messageType?: string
  appId?: number
  userId?: number
  lastCreateTime?: string
}

// ==================== 会员相关类型 ====================

export interface VipCode {
  id: number
  vipCode: string
  effectiveDay: number
  expDate: string
  idDelete: number
  createTime: string
  updateTime: string
  createUser: number
  useNum: number
  maxUseNum: number
}

export interface VipCodeAddRequest {
  vipCode: string
  effectiveDay: number
  expDate: string
  maxUseNum: number
  creatorId: number
}

export interface VipCodeUpdateRequest {
  id: number
  expDate: string
  maxUseNum: number
}

// ==================== 通用请求类型 ====================

export interface DeleteRequest {
  id: number
}
