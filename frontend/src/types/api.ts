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
  id: string
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
  id: string
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
  inviteUser: string
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
  id: string
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
  id?: string
  userName?: string
  userAccount?: string
  userProfile?: string
  userRole?: string
}

// ==================== 应用相关类型 ====================

export interface AppVO {
  id: string
  appName: string
  cover: string
  initPrompt: string
  codeGenType: string
  deployKey: string
  deployedTime: string
  priority: number
  userId: string
  createTime: string
  updateTime: string
  user: UserVO
}

export interface AppAddRequest {
  initPrompt: string
  templateKey?: string
  codeGenType?: string
}

export interface RoutingRecommendation {
  recommendedType: string
  recommendedName: string
  reason: string
  fullstack: boolean
  pointCost: number
  alternativeType: string
  alternativeName: string
  alternativePointCost: number
}

export interface AppUpdateRequest {
  id: string
  appName: string
}

export interface AppAdminUpdateRequest {
  id: string
  appName?: string
  cover?: string
  priority?: number
}

export interface AppDeployRequest {
  appId: string
}

export interface AppQueryRequest extends PageRequest {
  id?: string
  appName?: string
  cover?: string
  initPrompt?: string
  codeGenType?: string
  deployKey?: string
  priority?: number
  userId?: string
}

// ==================== Token相关类型 ====================

export interface UserTokenSummaryDTO {
  userId: string
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
  id: string
  userId: string
  userName: string
  appId: string
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
  id: string
  message: string
  messageType: string
  appId: string
  userId: string
  createTime: string
  updateTime: string
  isDelete: number
}

export interface ChatHistoryQueryRequest extends PageRequest {
  id?: string
  message?: string
  messageType?: string
  appId?: string
  userId?: string
  lastCreateTime?: string
}

// ==================== 会员相关类型 ====================

export interface VipCode {
  id: string
  vipCode: string
  effectiveDay: number
  expDate: string
  idDelete: number
  createTime: string
  updateTime: string
  createUser: string
  useNum: number
  maxUseNum: number
}

export interface VipCodeAddRequest {
  vipCode: string
  effectiveDay: number
  expDate: string
  maxUseNum: number
  creatorId: string
}

export interface VipCodeUpdateRequest {
  id: string
  expDate: string
  maxUseNum: number
}

// ==================== 配额相关类型 ====================

export interface UserQuotaVO {
  dailyGenUsed: number
  dailyGenLimit: number
  monthlyGenUsed: number
  monthlyGenLimit: number
  dailyTokenUsed: number
  dailyTokenLimit: number
  monthlyTokenUsed: number
  monthlyTokenLimit: number
}

export interface RecentGeneration {
  appId: string
  appName: string
  codeGenType: string
  tokenUsed: number
  createTime: string
}

export interface GenerationStatsDTO {
  totalGenerations: number
  totalTokens: number
  htmlCount: number
  multiFileCount: number
  vueCount: number
  deployCount: number
  recentRecords: RecentGeneration[]
}

export interface CodeTemplate {
  id: string
  templateName: string
  templateKey: string
  description: string
  codeGenType: string
  templateContent: string
  previewUrl: string
  useCount: number
  status: number
  createTime: string
  updateTime: string
}

export interface CodeSkill {
  id: string
  name: string
  skillKey: string
  description: string
  systemPrompt: string
  codeGenType: string
  pointCost: number
  toolNames: string | null
  buildStrategy: string
  modelStrategy: string
  isActive: number
  sortOrder: number
  customTools: string | null
  hooks: string | null
  contentHash: string | null
  source: string | null
  createTime: string
  updateTime: string
}

// ==================== 通用请求类型 ====================

export interface DeleteRequest {
  id: string
}
