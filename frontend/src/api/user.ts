import request from '@/utils/request'
import type {
  ApiResponse,
  LoginUser,
  UserVO,
  UserLoginRequest,
  UserRegisterRequest,
  UserUpdateRequest,
  ChangePasswordRequest,
  UserQueryRequest,
  PageResponse,
  DeleteRequest
} from '@/types'

// ==================== 用户认证接口 ====================

// 用户登录
export function login(data: UserLoginRequest) {
  return request.post<ApiResponse<LoginUser>>('/user/login', data)
}

// 用户注册
export function register(data: UserRegisterRequest) {
  return request.post<ApiResponse<number>>('/user/register', data)
}

// 用户登出
export function logout() {
  return request.post<ApiResponse<boolean>>('/user/logout', undefined, { silentAuth: true } as any)
}

// 获取当前登录用户
export function getLoginUser() {
  return request.get<ApiResponse<LoginUser>>('/user/get/loginUser', { silentAuth: true } as any)
}

// ==================== 用户信息接口 ====================

// 根据id获取用户（管理员）
export function getUserById(id: string) {
  return request.get<ApiResponse<any>>('/user/get', { params: { id } })
}

// 根据id获取用户包装类（已脱敏）
export function getUserVOById(id: string) {
  return request.get<ApiResponse<UserVO>>('/user/get/vo', { params: { id } })
}

// 用户更新自己的信息
export function updateMyInfo(data: UserUpdateRequest) {
  return request.post<ApiResponse<boolean>>('/user/update/my', data)
}

// 更新用户（管理员）
export function updateUser(data: UserUpdateRequest) {
  return request.post<ApiResponse<boolean>>('/user/update', data)
}

// 上传用户头像
export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResponse<string>>('/user/upload/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 修改密码
export function changePassword(data: ChangePasswordRequest) {
  return request.post<ApiResponse<boolean>>('/user/changePassword', data)
}

// 用户签到
export function signIn() {
  return request.post<ApiResponse<number>>('/user/signIn')
}

// 获取我邀请的用户列表
export function getMyInvited() {
  return request.post<ApiResponse<UserVO[]>>('/user/myInvited')
}

// ==================== 管理员接口 ====================

// 分页获取用户列表（管理员）
export function getUserList(data: UserQueryRequest) {
  return request.post<ApiResponse<PageResponse<UserVO>>>('/user/list/page/vo', data)
}

// 创建用户（管理员）
export function createUser(data: {
  userName: string
  userAccount: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
}) {
  return request.post<ApiResponse<number>>('/user/add', data)
}

// 删除用户（管理员）
export function deleteUser(id: string) {
  return request.post<ApiResponse<boolean>>('/user/delete', { id })
}
