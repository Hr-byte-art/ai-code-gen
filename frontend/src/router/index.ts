import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomePage.vue'),
      meta: { title: '首页' }
    },
    {
      path: '/user/login',
      name: 'login',
      component: () => import('@/views/user/UserLoginPage.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/user/register',
      name: 'register',
      component: () => import('@/views/user/UserRegisterPage.vue'),
      meta: { title: '注册' }
    },
    {
      path: '/user/center',
      name: 'userCenter',
      component: () => import('@/views/user/UserCenterPage.vue'),
      meta: { title: '个人中心', requiresAuth: true }
    },
    {
      path: '/app',
      name: 'appList',
      component: () => import('@/views/app/AppListPage.vue'),
      meta: { title: '我的应用', requiresAuth: true }
    },
    {
      path: '/app/chat/:id',
      name: 'appChat',
      component: () => import('@/views/app/AppChatPage.vue'),
      meta: { title: '应用对话', requiresAuth: true }
    },
    {
      path: '/app/edit/:id',
      name: 'appEdit',
      component: () => import('@/views/app/AppEditPage.vue'),
      meta: { title: '编辑应用', requiresAuth: true }
    },
    {
      path: '/token',
      name: 'tokenOverview',
      component: () => import('@/views/token/TokenOverviewPage.vue'),
      meta: { title: 'Token概览', requiresAuth: true }
    },
    {
      path: '/token/details',
      name: 'tokenDetails',
      component: () => import('@/views/token/TokenDetailsPage.vue'),
      meta: { title: 'Token详情', requiresAuth: true }
    },
    {
      path: '/token/model',
      name: 'modelStatistics',
      component: () => import('@/views/token/ModelStatisticsPage.vue'),
      meta: { title: '模型统计', requiresAuth: true }
    },
    {
      path: '/token/ranking',
      name: 'tokenRanking',
      component: () => import('@/views/token/TokenRankingPage.vue'),
      meta: { title: '排行榜', requiresAuth: true }
    },
    {
      path: '/token/test',
      name: 'apiTest',
      component: () => import('@/views/token/ApiTestPage.vue'),
      meta: { title: 'API测试', requiresAuth: true }
    },
    {
      path: '/admin/appManage',
      name: 'appManage',
      component: () => import('@/views/admin/AppManagePage.vue'),
      meta: { title: '应用管理', requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/admin/chatManage',
      name: 'chatManage',
      component: () => import('@/views/admin/ChatManagePage.vue'),
      meta: { title: '聊天管理', requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/admin/userManage',
      name: 'userManage',
      component: () => import('@/views/admin/UserManagePage.vue'),
      meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true }
    }
  ]
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || '熵擎 AI 应用生成平台'} - 熵擎 AI`

  // 检查是否需要登录
  if (to.meta.requiresAuth) {
    const userStore = useUserStore()

    // 如果还没有获取用户信息，先获取
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
      } catch {
        // 获取失败，跳转登录
        next({
          path: '/user/login',
          query: { redirect: to.fullPath }
        })
        return
      }
    }

    // 检查是否已登录
    if (!userStore.isLoggedIn) {
      next({
        path: '/user/login',
        query: { redirect: to.fullPath }
      })
      return
    }

    // 检查是否需要管理员权限
    if (to.meta.requiresAdmin && !userStore.isAdmin) {
      next({ path: '/' })
      return
    }
  }

  next()
})

export default router
