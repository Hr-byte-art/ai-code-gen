<template>
  <div class="admin-layout">
    <header class="admin-header">
      <div class="admin-header-inner">
        <div class="admin-header-left">
          <router-link to="/" class="admin-logo-link">
            <img src="@/assets/logo.svg" alt="Logo" class="admin-logo" />
            <span class="admin-logo-text">{{ $t('brand.name') }}</span>
          </router-link>
          <span class="admin-badge">后台</span>
        </div>
        <nav class="admin-nav">
          <router-link to="/admin/appManage" :class="['admin-nav-link', { active: route.path === '/admin/appManage' }]">
            {{ $t('sidebar.appManage') }}
          </router-link>
          <router-link to="/admin/userManage" :class="['admin-nav-link', { active: route.path === '/admin/userManage' }]">
            {{ $t('sidebar.userManage') }}
          </router-link>
          <router-link to="/admin/chatManage" :class="['admin-nav-link', { active: route.path.startsWith('/admin/chatManage') }]">
            {{ $t('sidebar.chatManage') }}
          </router-link>
          <router-link to="/admin/skillManage" :class="['admin-nav-link', { active: route.path === '/admin/skillManage' }]">
            技能管理
          </router-link>
          <router-link to="/admin/vipCodeManage" :class="['admin-nav-link', { active: route.path === '/admin/vipCodeManage' }]">
            会员码
          </router-link>
          <router-link to="/admin/agentTrace" :class="['admin-nav-link', { active: route.path === '/admin/agentTrace' }]">
            Agent 追踪
          </router-link>
          <router-link to="/admin/mcpManage" :class="['admin-nav-link', { active: route.path === '/admin/mcpManage' }]">
            MCP 管理
          </router-link>
        </nav>
        <div class="admin-header-right">
          <ThemeToggle />
          <router-link to="/app" class="back-link">
            <ArrowLeftOutlined /> {{ $t('nav.backToWorkbench') }}
          </router-link>
        </div>
      </div>
    </header>
    <main class="admin-main">
      <router-view />
      <LegalFooter variant="compact" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import LegalFooter from '@/components/common/LegalFooter.vue'

const route = useRoute()
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-body);
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-light);
  height: var(--header-height);
  backdrop-filter: blur(14px);
}

.admin-header-inner {
  max-width: 100%;
  padding: 0 16px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.admin-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-logo-link {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.admin-logo {
  width: 25px;
  height: 25px;
  border-radius: 7px;
}

.admin-logo-text {
  font-size: 14px;
  font-weight: 850;
  color: var(--t-primary);
}

.admin-badge {
  font-size: 11px;
  font-weight: 800;
  color: var(--c-warning);
  background: #f6ede1;
  border: 1px solid #e8d4bb;
  padding: 3px 8px;
  border-radius: var(--r-full);
}

.admin-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.admin-nav-link {
  padding: 6px 13px;
  border-radius: var(--r-md);
  font-size: 13px;
  font-weight: 700;
  color: var(--t-muted);
  text-decoration: none;
  transition: background var(--t-fast), color var(--t-fast);
}

.admin-nav-link:hover {
  color: var(--t-primary);
  background: var(--bg-hover);
}

.admin-nav-link.active {
  color: var(--c-primary);
  background: var(--c-primary-50);
}

.admin-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  font-weight: 700;
  color: var(--t-muted);
  text-decoration: none;
  padding: 6px 10px;
  border-radius: var(--r-md);
  transition: background var(--t-fast), color var(--t-fast);
}

.back-link:hover {
  color: var(--c-primary);
  background: var(--c-primary-50);
}

.admin-main {
  flex: 1;
  padding: 24px;
}

@media (max-width: 768px) {
  .admin-nav { display: none; }
  .admin-main { padding: 16px; }
}
</style>