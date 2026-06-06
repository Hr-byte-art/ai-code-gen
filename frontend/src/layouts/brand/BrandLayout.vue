<template>
  <div class="brand-layout">
    <header class="brand-header">
      <div class="brand-header-inner">
        <router-link to="/" class="brand-logo-link">
          <img src="@/assets/logo.svg" alt="Logo" class="brand-logo" />
          <span class="brand-logo-text">{{ $t('brand.name') }}</span>
        </router-link>
        <nav class="brand-nav">
          <router-link to="/" :class="['brand-nav-link', { active: route.path === '/' }]">
            {{ $t('nav.home') }}
          </router-link>
          <template v-if="isLoggedIn">
            <router-link to="/app" class="brand-nav-link primary-link">
              <AppstoreOutlined /> 工作台
            </router-link>
            <router-link to="/token" class="brand-nav-link">
              <CloudServerOutlined /> {{ $t('nav.token') }}
            </router-link>
            <a-dropdown>
              <button class="brand-user-btn">
                <a-avatar :src="userAvatar" :size="24">
                  <template #icon><UserOutlined /></template>
                </a-avatar>
                <span class="brand-user-name">{{ username }}</span>
              </button>
              <template #overlay>
                <a-menu @click="handleMenuClick">
                  <a-menu-item key="/user/center"><UserOutlined /> {{ $t('nav.personalCenter') }}</a-menu-item>
                  <a-menu-item key="logout"><LogoutOutlined /> {{ $t('nav.logout') }}</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
          <template v-else>
            <router-link to="/user/login" :class="['brand-nav-link', { active: route.path === '/user/login' }]">
              {{ $t('nav.login') }}
            </router-link>
            <router-link to="/user/register" class="brand-register-btn">
              {{ $t('nav.register') }}
            </router-link>
          </template>
        </nav>
      </div>
    </header>
    <main class="brand-main">
      <router-view />
    </main>
    <LegalFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AppstoreOutlined, CloudServerOutlined,
  UserOutlined, LogoutOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import LegalFooter from '@/components/common/LegalFooter.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const username = computed(() => userStore.username || '用户')
const userAvatar = computed(() => userStore.userAvatar)

onMounted(() => { if (!userStore.userInfo) userStore.fetchUserInfo() })

const handleMenuClick = ({ key }: { key: string }) => {
  if (key === 'logout') { userStore.logout().then(() => router.push('/')) }
  else if (key.startsWith('/')) { router.push(key) }
}
</script>

<style scoped>
.brand-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.brand-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-light);
  backdrop-filter: blur(14px);
}

.brand-header-inner {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 24px;
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand-logo-link {
  display: flex;
  align-items: center;
  gap: 9px;
  text-decoration: none;
}

.brand-logo {
  width: 28px;
  height: 28px;
  border-radius: 8px;
}

.brand-logo-text {
  font-size: 15px;
  font-weight: 850;
  color: var(--t-primary);
  letter-spacing: -0.2px;
}

.brand-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.brand-nav-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 11px;
  border-radius: var(--r-md);
  font-size: 13px;
  font-weight: 650;
  color: var(--t-muted);
  text-decoration: none;
  transition: color var(--t-fast), background var(--t-fast);
}

.brand-nav-link:hover {
  color: var(--t-primary);
  background: var(--bg-hover);
}

.brand-nav-link.active {
  color: var(--c-primary);
  background: var(--c-primary-50);
}

.primary-link {
  color: var(--t-primary);
}

.brand-register-btn {
  padding: 6px 14px;
  border-radius: var(--r-md);
  font-size: 13px;
  font-weight: 750;
  color: var(--t-inverse);
  background: var(--c-primary);
  text-decoration: none;
  transition: background var(--t-fast), transform var(--t-fast);
}

.brand-register-btn:hover {
  background: var(--c-primary-light);
  transform: translateY(-1px);
}

.brand-user-btn {
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

.brand-user-btn:hover {
  border-color: var(--c-primary-200);
  background: var(--c-primary-50);
}

.brand-user-name {
  font-size: 13px;
  font-weight: 650;
  color: var(--t-secondary);
}

.brand-main {
  flex: 1;
}

@media (max-width: 768px) {
  .brand-header-inner { padding: 0 16px; }
  .brand-nav { gap: 0; }
  .brand-nav-link { padding: 5px 8px; font-size: 12px; }
}
</style>