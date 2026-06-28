import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getLoginUser, logout as logoutApi } from '@/api/user'
import { message } from 'ant-design-vue'
import type { LoginUser, UserLoginRequest } from '@/types'

export const useUserStore = defineStore('user', () => {
  // 状态
  const userInfo = ref<LoginUser | null>(null)

  // 计算属性
  const isLoggedIn = computed(() => !!userInfo.value)
  const username = computed(() => userInfo.value?.userName || userInfo.value?.userAccount || '')
  const userAvatar = computed(() => userInfo.value?.userAvatar || '')
  const isAdmin = computed(() => userInfo.value?.userRole === 'admin')
  const isVip = computed(() => userInfo.value?.userRole === 'vip')
  const canUsePremiumGeneration = computed(() => isVip.value || isAdmin.value)
  const userId = computed(() => userInfo.value?.id)

  // 登录
  async function login(credentials: UserLoginRequest) {
    try {
      const res: any = await loginApi(credentials)
      userInfo.value = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
      message.success('登录成功')
      return true
    } catch (error) {
      return false
    }
  }

  // 获取当前登录用户信息
  async function fetchUserInfo() {
    try {
      const res: any = await getLoginUser()
      userInfo.value = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
      return res.data
    } catch (error) {
      console.error('获取用户信息失败:', error)
      userInfo.value = null
      return null
    }
  }

  // 退出登录
  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      console.error('登出请求失败:', error)
    } finally {
      userInfo.value = null
      localStorage.removeItem('user')
      message.success('已退出登录')
    }
  }

  // 初始化用户信息（从localStorage恢复）
  function initUserInfo() {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      try {
        userInfo.value = JSON.parse(userStr)
      } catch (error) {
        console.error('解析用户信息失败:', error)
        localStorage.removeItem('user')
      }
    }
  }

  // 更新用户信息
  function updateUserInfo(data: Partial<LoginUser>) {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...data }
      localStorage.setItem('user', JSON.stringify(userInfo.value))
    }
  }

  // 初始化
  initUserInfo()

  return {
    userInfo,
    isLoggedIn,
    username,
    userAvatar,
    isAdmin,
    isVip,
    canUsePremiumGeneration,
    userId,
    login,
    fetchUserInfo,
    logout,
    initUserInfo,
    updateUserInfo
  }
})
