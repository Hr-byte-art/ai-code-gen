import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getMyAppList,
  getGoodAppList,
  getAppById,
  createApp as createAppApi,
  updateApp as updateAppApi,
  deleteApp as deleteAppApi,
  deployApp as deployAppApi,
  getAppBuildStatus
} from '@/api/app'
import { message } from 'ant-design-vue'

export const useAppStore = defineStore('app', () => {
  // 状态
  const myAppList = ref<any[]>([])
  const goodAppList = ref<any[]>([])
  const currentApp = ref<any>(null)
  const loading = ref(false)

  // 分页参数
  const myAppPagination = ref({
    pageNum: 1,
    pageSize: 12,
    total: 0
  })

  const goodAppPagination = ref({
    pageNum: 1,
    pageSize: 12,
    total: 0
  })

  // 获取我的应用列表
  async function fetchMyAppList(params?: {
    pageNum?: number
    pageSize?: number
    appName?: string
    codeGenType?: string
  }) {
    loading.value = true
    try {
      const res: any = await getMyAppList({
        ...myAppPagination.value,
        ...params
      })
      myAppList.value = res.data?.records || []
      myAppPagination.value.total = res.data?.totalRow || 0
      return res.data
    } catch (error) {
      console.error('获取我的应用列表失败:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  // 获取精选应用列表
  async function fetchGoodAppList(params?: {
    pageNum?: number
    pageSize?: number
    appName?: string
  }) {
    loading.value = true
    try {
      const res: any = await getGoodAppList({
        ...goodAppPagination.value,
        ...params
      })
      goodAppList.value = res.data?.records || []
      goodAppPagination.value.total = res.data?.totalRow || 0
      return res.data
    } catch (error) {
      console.error('获取精选应用列表失败:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  // 获取应用详情
  async function fetchAppDetail(id: string) {
    loading.value = true
    try {
      const res = await getAppById(id)
      currentApp.value = res.data
      return res.data
    } catch (error) {
      console.error('获取应用详情失败:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  // 创建应用
  async function createApp(initPrompt: string, templateKey?: string) {
    loading.value = true
    try {
      const params: any = { initPrompt }
      if (templateKey) { params.templateKey = templateKey }
      const res = await createAppApi(params)
      message.success('应用创建成功')
      return res.data
    } catch (error) {
      message.error('创建应用失败')
      return null
    } finally {
      loading.value = false
    }
  }

  // 更新应用
  async function updateApp(id: string, appName: string) {
    loading.value = true
    try {
      const res = await updateAppApi({ id, appName })
      message.success('更新成功')
      // 更新本地数据
      if (currentApp.value?.id === id) {
        currentApp.value.appName = appName
      }
      return res.data
    } catch (error) {
      message.error('更新失败')
      return null
    } finally {
      loading.value = false
    }
  }

  // 删除应用
  async function deleteApp(id: string) {
    loading.value = true
    try {
      await deleteAppApi(id)
      message.success('删除成功')
      // 从列表中移除
      myAppList.value = myAppList.value.filter(app => app.id !== id)
      goodAppList.value = goodAppList.value.filter(app => app.id !== id)
      return true
    } catch (error) {
      message.error('删除失败')
      return false
    } finally {
      loading.value = false
    }
  }

  // 部署应用
  async function deployApp(appId: string): Promise<string | null> {
    loading.value = true
    try {
      const res = await deployAppApi(appId)
      const deployUrl = (res as any)?.data?.data || null
      message.success('部署成功')
      return deployUrl
    } catch (error) {
      message.error('部署失败')
      return null
    } finally {
      loading.value = false
    }
  }

  // 获取构建状态
  async function fetchBuildStatus(appId: string) {
    try {
      const res = await getAppBuildStatus(appId)
      return res.data
    } catch (error) {
      console.error('获取构建状态失败:', error)
      return null
    }
  }

  // 设置分页
  function setMyAppPagination(pageNum: number, pageSize?: number) {
    myAppPagination.value.pageNum = pageNum
    if (pageSize) {
      myAppPagination.value.pageSize = pageSize
    }
  }

  function setGoodAppPagination(pageNum: number, pageSize?: number) {
    goodAppPagination.value.pageNum = pageNum
    if (pageSize) {
      goodAppPagination.value.pageSize = pageSize
    }
  }

  // 清空当前应用
  function clearCurrentApp() {
    currentApp.value = null
  }

  return {
    myAppList,
    goodAppList,
    currentApp,
    loading,
    myAppPagination,
    goodAppPagination,
    fetchMyAppList,
    fetchGoodAppList,
    fetchAppDetail,
    createApp,
    updateApp,
    deleteApp,
    deployApp,
    fetchBuildStatus,
    setMyAppPagination,
    setGoodAppPagination,
    clearCurrentApp
  }
})
