import request from '@/utils/request'
import type { ApiResponse, CodeSkill } from '@/types'

// 获取所有可用技能（用户可见）
export function listSkills() {
  return request.get<ApiResponse<CodeSkill[]>>('/skill/list')
}

// 根据标识获取技能
export function getSkillByKey(key: string) {
  return request.get<ApiResponse<CodeSkill>>('/skill/get', { params: { key } })
}

// ==================== 管理员接口 ====================

// 获取所有技能（含禁用）
export function adminListSkills() {
  return request.get<ApiResponse<CodeSkill[]>>('/skill/admin/list')
}

// 新增技能
export function addSkill(data: Partial<CodeSkill>) {
  return request.post<ApiResponse<number>>('/skill/admin/add', data)
}

// 更新技能
export function updateSkill(data: Partial<CodeSkill>) {
  return request.post<ApiResponse<boolean>>('/skill/admin/update', data)
}

// 删除技能
export function deleteSkill(id: string) {
  return request.post<ApiResponse<boolean>>('/skill/admin/delete', null, { params: { id } })
}
