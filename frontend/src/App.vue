<template>
  <component :is="layoutComponent" />
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import BrandLayout from '@/layouts/brand/BrandLayout.vue'
import WorkbenchLayout from '@/layouts/workbench/WorkbenchLayout.vue'
import AdminLayout from '@/layouts/admin/AdminLayout.vue'

const route = useRoute()
const themeStore = useThemeStore()

const layouts: Record<string, any> = {
  brand: BrandLayout,
  workbench: WorkbenchLayout,
  admin: AdminLayout,
}

const layoutComponent = computed(() => {
  const layoutName = (route.meta.layout as string) || 'brand'
  return layouts[layoutName] || BrandLayout
})

onMounted(() => {
  themeStore.init()
})
</script>

<style>
:root {
  --c-primary: #315c53;
  --c-primary-light: #47766c;
  --c-primary-dark: #203f39;
  --c-primary-50: #edf5f2;
  --c-primary-100: #d8e8e3;
  --c-primary-200: #b9d4cc;

  --c-cta: #b66a35;
  --c-cta-light: #d1844b;
  --c-cta-dark: #8f4f27;
  --c-accent: #6e5f46;
  --c-success: #2f7d5b;
  --c-warning: #a46720;
  --c-error: #b94343;
  --c-info: #486d80;

  --header-height: 56px;
  --sidebar-width: 224px;
  --sidebar-collapsed-width: 0px;
  --page-max-width: 1280px;

  --bg-body: #f4f1ea;
  --bg-card: #fbfaf6;
  --bg-elevated: #f7f4ee;
  --bg-header: rgba(251, 250, 246, 0.92);
  --bg-sidebar: #f7f4ee;
  --bg-input: #fffdf8;
  --bg-hover: #ece7dd;
  --bg-active: #e4eee9;
  --bg-soft: #eee8dc;

  --t-primary: #20231f;
  --t-secondary: #44483f;
  --t-muted: #717568;
  --t-light: #9b9b8e;
  --t-inverse: #f8f3e8;

  --border: #d8d1c2;
  --border-light: #e8e1d4;
  --border-strong: #bcb3a3;

  --shadow-xs: 0 1px 1px rgba(58, 49, 37, 0.04);
  --shadow-sm: 0 8px 20px rgba(58, 49, 37, 0.06);
  --shadow-md: 0 16px 34px rgba(58, 49, 37, 0.09);
  --shadow-lg: 0 22px 48px rgba(58, 49, 37, 0.12);
  --shadow-primary: 0 8px 18px rgba(49, 92, 83, 0.2);
  --shadow-primary-lg: 0 14px 28px rgba(49, 92, 83, 0.24);
  --shadow-cta: 0 10px 24px rgba(182, 106, 53, 0.24);

  --r-sm: 7px;
  --r-md: 10px;
  --r-lg: 14px;
  --r-xl: 18px;
  --r-2xl: 24px;
  --r-full: 9999px;

  --ease: cubic-bezier(0.16, 1, 0.3, 1);
  --t-fast: 150ms var(--ease);
  --t-base: 220ms var(--ease);
  --t-slow: 360ms var(--ease);
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html {
  overflow-x: hidden;
  scroll-behavior: smooth;
}

body {
  font-family: ui-sans-serif, -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--t-primary);
  background:
    radial-gradient(circle at 12% 0%, rgba(49, 92, 83, 0.08), transparent 28rem),
    linear-gradient(180deg, #fbfaf6 0%, var(--bg-body) 46%);
  overflow-x: hidden;
  line-height: 1.5;
}

::selection {
  background: rgba(49, 92, 83, 0.16);
  color: var(--t-primary);
}

::-webkit-scrollbar { width: 7px; height: 7px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #c7bead; border-radius: 999px; }
::-webkit-scrollbar-thumb:hover { background: #a99f8f; }

.ant-layout-footer { background: transparent !important; padding: 0 !important; }

.ant-btn {
  border-radius: var(--r-md) !important;
  font-weight: 650 !important;
  font-size: 14px !important;
  transition: background var(--t-fast), border-color var(--t-fast), color var(--t-fast), box-shadow var(--t-fast), transform var(--t-fast) !important;
  cursor: pointer !important;
  letter-spacing: 0;
}

.ant-btn-primary {
  background: var(--c-primary) !important;
  border-color: var(--c-primary) !important;
  box-shadow: var(--shadow-primary) !important;
  color: var(--t-inverse) !important;
}

.ant-btn-primary:hover {
  background: var(--c-primary-light) !important;
  border-color: var(--c-primary-light) !important;
  box-shadow: var(--shadow-primary-lg) !important;
  transform: translateY(-1px);
}

.ant-btn-primary:active {
  background: var(--c-primary-dark) !important;
  transform: translateY(0);
  box-shadow: var(--shadow-primary) !important;
}

.ant-btn-default {
  background: var(--bg-card) !important;
  border-color: var(--border) !important;
  color: var(--t-secondary) !important;
}

.ant-btn-default:hover {
  border-color: var(--c-primary) !important;
  color: var(--c-primary) !important;
  background: var(--c-primary-50) !important;
}

.ant-btn-link {
  color: var(--c-primary) !important;
  font-weight: 650 !important;
}

.ant-btn-dangerous {
  color: var(--c-error) !important;
}

.ant-input, .ant-input-affix-wrapper, .ant-select-selector, .ant-picker, .ant-input-number {
  border-radius: var(--r-md) !important;
  border-color: var(--border) !important;
  transition: border-color var(--t-fast), box-shadow var(--t-fast), background var(--t-fast) !important;
  background: var(--bg-input) !important;
  color: var(--t-primary) !important;
}

.ant-input:focus, .ant-input-affix-wrapper:focus, .ant-input-affix-wrapper-focused,
.ant-select-focused .ant-select-selector {
  border-color: var(--c-primary) !important;
  box-shadow: 0 0 0 3px rgba(49, 92, 83, 0.12) !important;
}

.ant-input::placeholder { color: var(--t-light) !important; }

.ant-card {
  border-radius: var(--r-lg) !important;
  border: 1px solid var(--border-light) !important;
  box-shadow: none !important;
  background: var(--bg-card) !important;
}

.ant-card:hover {
  box-shadow: var(--shadow-xs) !important;
}

.ant-table-wrapper {
  border-radius: var(--r-lg);
  overflow: hidden;
  border: 1px solid var(--border-light);
  background: var(--bg-card);
}

.ant-table {
  background: var(--bg-card) !important;
}

.ant-table-thead > tr > th {
  background: var(--bg-soft) !important;
  font-weight: 750 !important;
  color: var(--t-secondary) !important;
  font-size: 12px !important;
  text-transform: none;
  letter-spacing: 0.2px;
  border-bottom: 1px solid var(--border) !important;
}

.ant-table-tbody > tr > td {
  border-bottom: 1px solid var(--border-light) !important;
  transition: background var(--t-fast);
  color: var(--t-secondary);
}

.ant-table-tbody > tr:hover > td {
  background: var(--bg-hover) !important;
}

.ant-tag {
  border-radius: var(--r-full) !important;
  font-weight: 650 !important;
  font-size: 11px !important;
  padding: 2px 9px !important;
  border: 1px solid transparent !important;
  line-height: 1.6;
}

.ant-tag-blue { background: var(--c-primary-50) !important; color: var(--c-primary) !important; border-color: var(--c-primary-100) !important; }
.ant-tag-green { background: #eaf4ee !important; color: #2f7d5b !important; border-color: #cfe5d8 !important; }
.ant-tag-red { background: #f7e8e4 !important; color: #b94343 !important; border-color: #e7c8c1 !important; }
.ant-tag-gold, .ant-tag-orange { background: #f6ede1 !important; color: #a46720 !important; border-color: #e8d4bb !important; }

.ant-modal .ant-modal-content {
  border-radius: var(--r-2xl) !important;
  box-shadow: var(--shadow-lg) !important;
  background: var(--bg-card) !important;
}

.ant-modal .ant-modal-header {
  border-bottom: 1px solid var(--border-light) !important;
  background: var(--bg-card) !important;
}

.ant-descriptions-bordered .ant-descriptions-item-label {
  background: var(--bg-hover) !important;
  font-weight: 650 !important;
  color: var(--t-secondary) !important;
  font-size: 13px !important;
}

.ant-pagination .ant-pagination-item-active {
  background: var(--c-primary) !important;
  border-color: var(--c-primary) !important;
}

.ant-pagination .ant-pagination-item-active a { color: var(--t-inverse) !important; }
.ant-statistic-content-value { font-weight: 800 !important; letter-spacing: -0.5px; }
.ant-avatar { border: 2px solid var(--border-light); }
.ant-empty-description { color: var(--t-muted) !important; font-size: 13px !important; }

.ant-dropdown-menu {
  border-radius: var(--r-lg) !important;
  box-shadow: var(--shadow-lg) !important;
  border: 1px solid var(--border-light) !important;
  background: var(--bg-card) !important;
  padding: 4px !important;
}

.ant-dropdown-menu-item {
  border-radius: var(--r-md) !important;
  font-weight: 500;
  transition: background var(--t-fast);
}

.ant-menu-horizontal { border-bottom: none !important; }
.ant-menu-horizontal > .ant-menu-item-selected { color: var(--c-primary) !important; border-bottom-color: var(--c-primary) !important; }

.ant-progress-bg {
  background: linear-gradient(90deg, var(--c-primary), var(--c-cta)) !important;
}

.ant-tooltip-inner {
  border-radius: var(--r-md) !important;
  font-size: 12px !important;
}

.ant-checkbox-checked .ant-checkbox-inner {
  background-color: var(--c-primary) !important;
  border-color: var(--c-primary) !important;
}
</style>