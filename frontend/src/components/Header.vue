<template>
  <header class="header">
    <div class="header-left">
      <router-link to="/" class="logo-link">
        <img src="@/assets/logo.svg" alt="Logo" class="logo" />
        <h1 class="site-title">熵擎 AI</h1>
      </router-link>
    </div>

    <nav class="main-menu">
      <a-menu mode="horizontal" :selected-keys="selectedKeys" @click="handleMenuClick">
        <a-menu-item key="/">
          <HomeOutlined />
          <span>首页</span>
        </a-menu-item>
        <a-menu-item key="/app">
          <AppstoreOutlined />
          <span>应用</span>
        </a-menu-item>
        <a-menu-item key="/token">
          <CloudServerOutlined />
          <span>Token</span>
        </a-menu-item>
        <a-sub-menu key="admin" v-if="isAdmin">
          <template #title>
            <SettingOutlined />
            <span>管理</span>
          </template>
          <a-menu-item key="/admin/appManage">应用管理</a-menu-item>
          <a-menu-item key="/admin/chatManage">聊天管理</a-menu-item>
          <a-menu-item key="/admin/userManage">用户管理</a-menu-item>
        </a-sub-menu>
      </a-menu>
    </nav>

    <div class="header-right">
      <div class="user-info" v-if="isLoggedIn">
        <a-dropdown>
          <a class="user-dropdown" @click.prevent>
            <a-avatar :src="userAvatar" :size="32">
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <span class="username">{{ username }}</span>
            <DownOutlined class="dropdown-arrow" />
          </a>
          <template #overlay>
            <a-menu @click="handleUserMenuClick">
              <a-menu-item key="/user/center">
                <UserOutlined />
                <span>个人中心</span>
              </a-menu-item>
              <a-menu-item key="logout">
                <LogoutOutlined />
                <span>退出登录</span>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
      <div class="auth-buttons" v-else>
        <a-button type="link" @click="goToLogin">登录</a-button>
        <a-button type="primary" @click="goToRegister">注册</a-button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  HomeOutlined,
  AppstoreOutlined,
  SettingOutlined,
  UserOutlined,
  LogoutOutlined,
  DownOutlined,
  CloudServerOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 用户状态
const isLoggedIn = computed(() => userStore.isLoggedIn)
const username = computed(() => userStore.username || '用户')
const userAvatar = computed(() => userStore.userAvatar)
const isAdmin = computed(() => userStore.isAdmin)

// 初始化用户信息
onMounted(() => {
  if (!userStore.userInfo) {
    userStore.fetchUserInfo()
  }
})

// 菜单状态
const selectedKeys = ref<string[]>([])

watch(
  () => route.path,
  (path) => {
    if (path.startsWith('/admin')) {
      selectedKeys.value = [path]
    } else if (path.startsWith('/token')) {
      selectedKeys.value = ['/token']
    } else if (path.startsWith('/app')) {
      selectedKeys.value = ['/app']
    } else {
      selectedKeys.value = [path]
    }
  },
  { immediate: true }
)

// 菜单点击处理
const handleMenuClick = ({ key }: { key: string }) => {
  if (key.startsWith('/')) {
    router.push(key)
  }
}

const handleUserMenuClick = ({ key }: { key: string }) => {
  if (key === 'logout') {
    handleLogout()
  } else if (key.startsWith('/')) {
    router.push(key)
  }
}

// 登出处理
const handleLogout = async () => {
  await userStore.logout()
  router.push('/')
}

// 跳转登录/注册
const goToLogin = () => {
  router.push('/user/login')
}

const goToRegister = () => {
  router.push('/user/register')
}
</script>

<style scoped>
.header {
  background: rgba(255, 255, 255, 0.82);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  backdrop-filter: blur(16px) saturate(180%);
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 100;
  transition: background var(--transition-base);
}

.header-left {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.logo {
  height: 36px;
  width: 36px;
  border-radius: 10px;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
  transition: transform var(--transition-base);
}

.logo-link:hover .logo {
  transform: scale(1.05);
}

.site-title {
  margin: 0;
  font-size: 18px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.main-menu {
  flex: 1;
  display: flex;
  justify-content: center;
}

.main-menu :deep(.ant-menu) {
  border-bottom: none;
  background: transparent;
  line-height: 62px;
}

.main-menu :deep(.ant-menu-item) {
  color: #475569;
  font-weight: 500;
  padding: 0 20px;
  transition: color var(--transition-fast);
}

.main-menu :deep(.ant-menu-item:hover) {
  color: var(--color-primary);
}

.main-menu :deep(.ant-menu-item-selected) {
  color: var(--color-primary) !important;
  font-weight: 600;
}

.main-menu :deep(.ant-menu-submenu-title) {
  color: #475569;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 10px;
  transition: all var(--transition-fast);
}

.user-dropdown:hover {
  background: rgba(59, 130, 246, 0.06);
}

.dropdown-arrow {
  font-size: 10px;
  color: #94a3b8;
  transition: transform var(--transition-fast);
}

.username {
  color: #334155;
  font-size: 14px;
  font-weight: 500;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auth-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}

.auth-buttons :deep(.ant-btn-link) {
  color: #475569;
  font-weight: 500;
}

.auth-buttons :deep(.ant-btn-link:hover) {
  color: var(--color-primary);
}

/* 响应式 */
@media (max-width: 1200px) {
  .header {
    padding: 0 20px;
  }
  .site-title {
    font-size: 16px;
  }
}

@media (max-width: 768px) {
  .header {
    padding: 0 12px;
  }
  .site-title {
    display: none;
  }
}
</style>
