<template>
  <div class="wb-layout">
    <header class="wb-header">
      <div class="wb-header-inner">
        <div class="wb-header-left">
          <button class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
            <MenuUnfoldOutlined v-if="sidebarCollapsed" />
            <MenuFoldOutlined v-else />
          </button>
          <router-link to="/app" class="wb-logo-link">
            <img src="@/assets/logo.svg" alt="Logo" class="wb-logo" />
            <span class="wb-logo-text">{{ $t('brand.name') }}</span>
          </router-link>
          <span class="wb-scope">工作台</span>
        </div>
        <nav class="wb-nav">
          <router-link to="/app" :class="['wb-nav-link', { active: isAppSection }]">
            <AppstoreOutlined /> 应用资产
          </router-link>
          <router-link to="/token" :class="['wb-nav-link', { active: isTokenSection }]">
            <CloudServerOutlined /> 资源账单
          </router-link>
          <router-link v-if="isAdmin" to="/admin/appManage" class="wb-nav-link">
            <SettingOutlined /> {{ $t('nav.admin') }}
          </router-link>
        </nav>
        <div class="wb-header-right">
          <ThemeToggle />
          <a-dropdown>
            <button class="wb-user-btn">
              <a-avatar :src="userAvatar" :size="26">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <span class="wb-user-name">{{ username }}</span>
            </button>
            <template #overlay>
              <a-menu @click="handleMenuClick">
                <a-menu-item key="/user/center">
                  <UserOutlined /> {{ $t('nav.personalCenter') }}
                </a-menu-item>
                <a-menu-item key="logout">
                  <LogoutOutlined /> {{ $t('nav.logout') }}
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>
    </header>
    <div class="wb-body">
      <aside :class="['wb-sidebar', { collapsed: sidebarCollapsed }]">
        <WorkbenchSidebar />
      </aside>
      <main class="wb-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  MenuFoldOutlined, MenuUnfoldOutlined,
  AppstoreOutlined, CloudServerOutlined, SettingOutlined,
  UserOutlined, LogoutOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import WorkbenchSidebar from '@/components/workbench/WorkbenchSidebar.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sidebarCollapsed = ref(false)
const username = computed(() => userStore.username || '用户')
const userAvatar = computed(() => userStore.userAvatar)
const isAdmin = computed(() => userStore.isAdmin)

const isAppSection = computed(() => route.path.startsWith('/app') || route.path === '/user/center')
const isTokenSection = computed(() => route.path.startsWith('/token'))

onMounted(() => { if (!userStore.userInfo) userStore.fetchUserInfo() })

const handleMenuClick = ({ key }: { key: string }) => {
  if (key === 'logout') { userStore.logout().then(() => router.push('/')) }
  else if (key.startsWith('/')) router.push(key)
}
</script>

<style scoped>
.wb-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-body);
}

.wb-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-light);
  height: var(--header-height);
  backdrop-filter: blur(14px);
}

.wb-header-inner {
  max-width: 100%;
  padding: 0 16px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.wb-header-left {
  display: flex;
  align-items: center;
  gap: 9px;
}

.sidebar-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid transparent;
  background: transparent;
  border-radius: var(--r-md);
  cursor: pointer;
  color: var(--t-muted);
  transition: background var(--t-fast), color var(--t-fast), border-color var(--t-fast);
}

.sidebar-toggle:hover {
  background: var(--bg-hover);
  color: var(--t-primary);
  border-color: var(--border-light);
}

.wb-logo-link {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.wb-logo {
  width: 25px;
  height: 25px;
  border-radius: 7px;
}

.wb-logo-text {
  font-size: 14px;
  font-weight: 850;
  color: var(--t-primary);
}

.wb-scope {
  padding: 3px 8px;
  border: 1px solid var(--border-light);
  border-radius: var(--r-full);
  color: var(--t-muted);
  background: var(--bg-soft);
  font-size: 11px;
  font-weight: 750;
}

.wb-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.wb-nav-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--r-md);
  font-size: 13px;
  font-weight: 700;
  color: var(--t-muted);
  text-decoration: none;
  transition: color var(--t-fast), background var(--t-fast);
}

.wb-nav-link:hover {
  color: var(--t-primary);
  background: var(--bg-hover);
}

.wb-nav-link.active {
  color: var(--c-primary);
  background: var(--c-primary-50);
}

.wb-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wb-user-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 4px 10px 4px 4px;
  border: 1px solid var(--border);
  border-radius: var(--r-full);
  background: var(--bg-card);
  cursor: pointer;
  transition: border-color var(--t-fast), background var(--t-fast);
}

.wb-user-btn:hover {
  border-color: var(--c-primary-200);
  background: var(--c-primary-50);
}

.wb-user-name {
  font-size: 12px;
  font-weight: 700;
  color: var(--t-secondary);
}

.wb-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.wb-sidebar {
  width: var(--sidebar-width);
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-light);
  flex-shrink: 0;
  overflow-y: auto;
  transition: width var(--t-base);
}

.wb-sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
  overflow: hidden;
}

.wb-main {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .wb-sidebar {
    position: fixed;
    left: 0;
    top: var(--header-height);
    bottom: 0;
    z-index: 50;
    width: var(--sidebar-width);
    transform: translateX(-100%);
    transition: transform var(--t-base);
  }

  .wb-sidebar:not(.collapsed) {
    transform: translateX(0);
  }

  .wb-nav,
  .wb-scope,
  .wb-user-name {
    display: none;
  }
}
</style>