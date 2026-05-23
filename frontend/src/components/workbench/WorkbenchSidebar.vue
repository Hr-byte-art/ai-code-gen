<template>
  <nav class="wb-sidebar-nav">
    <div class="sidebar-panel">
      <div class="sidebar-caption">生产流</div>
      <router-link to="/app" :class="['sidebar-link', { active: route.path === '/app' }]">
        <AppstoreOutlined />
        <span>应用资产</span>
      </router-link>
      <router-link to="/user/center" :class="['sidebar-link', { active: route.path === '/user/center' }]">
        <UserOutlined />
        <span>账户资料</span>
      </router-link>
    </div>

    <div class="sidebar-panel">
      <div class="sidebar-caption">资源账单</div>
      <router-link to="/token" :class="['sidebar-link', { active: route.path === '/token' }]">
        <DashboardOutlined />
        <span>概览</span>
      </router-link>
      <router-link to="/token/details" :class="['sidebar-link', { active: route.path === '/token/details' }]">
        <BarChartOutlined />
        <span>消耗明细</span>
      </router-link>
      <router-link to="/token/model" :class="['sidebar-link', { active: route.path === '/token/model' }]">
        <FundOutlined />
        <span>模型分布</span>
      </router-link>
      <router-link to="/token/ranking" :class="['sidebar-link', { active: route.path === '/token/ranking' }]">
        <TrophyOutlined />
        <span>排行榜</span>
      </router-link>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import {
  AppstoreOutlined, DashboardOutlined, BarChartOutlined,
  TrophyOutlined, FundOutlined, UserOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
</script>

<style scoped>
.wb-sidebar-nav {
  padding: 14px 10px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sidebar-panel {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.sidebar-panel:last-child {
  border-bottom: none;
}

.sidebar-caption {
  font-size: 11px;
  font-weight: 850;
  color: var(--t-light);
  letter-spacing: 0.08em;
  padding: 3px 10px 8px;
}

.sidebar-link {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border-radius: var(--r-md);
  font-size: 13px;
  font-weight: 700;
  color: var(--t-muted);
  text-decoration: none;
  transition: background var(--t-fast), color var(--t-fast);
}

.sidebar-link:hover {
  color: var(--t-primary);
  background: var(--bg-hover);
}

.sidebar-link.active {
  color: var(--c-primary);
  background: var(--c-primary-50);
}
</style>