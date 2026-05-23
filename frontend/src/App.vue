<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
</script>

<template>
  <BasicLayout />
</template>

<style>
:root {
  --color-primary: #3b82f6;
  --color-primary-dark: #2563eb;
  --color-primary-light: #93bbfd;
  --color-primary-bg: rgba(59, 130, 246, 0.06);
  --color-primary-border: rgba(59, 130, 246, 0.15);
  --color-success: #10b981;
  --color-warning: #f59e0b;
  --color-error: #ef4444;
  --color-purple: #8b5cf6;
  --text-primary: #0f172a;
  --text-secondary: #334155;
  --text-muted: #64748b;
  --text-light: #94a3b8;
  --bg-page: #f8fafc;
  --bg-card: #ffffff;
  --bg-elevated: #ffffff;
  --shadow-sm: 0 1px 3px rgba(15, 23, 42, 0.04), 0 1px 2px rgba(15, 23, 42, 0.06);
  --shadow-md: 0 4px 16px rgba(15, 23, 42, 0.06), 0 2px 4px rgba(15, 23, 42, 0.04);
  --shadow-lg: 0 12px 40px rgba(15, 23, 42, 0.08), 0 4px 12px rgba(15, 23, 42, 0.04);
  --shadow-primary: 0 4px 16px rgba(59, 130, 246, 0.18);
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --transition-fast: 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-base: 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell,
    'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
  color: var(--text-primary);
  background: var(--bg-page);
}

html {
  overflow-x: hidden;
}

/* Scrollbar */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* Ant Design 全局覆盖 */
.ant-layout-footer {
  background: transparent !important;
  padding: 0 !important;
}

.ant-btn {
  border-radius: var(--radius-sm) !important;
  font-weight: 500 !important;
  transition: all var(--transition-fast) !important;
}

.ant-btn-primary {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  border: none !important;
  box-shadow: var(--shadow-primary) !important;
  font-weight: 600 !important;
}

.ant-btn-primary:hover {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%) !important;
  transform: translateY(-1px) !important;
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.28) !important;
}

.ant-btn-default {
  border-color: #e2e8f0 !important;
  color: var(--text-secondary) !important;
}

.ant-btn-default:hover {
  border-color: var(--color-primary) !important;
  color: var(--color-primary) !important;
}

.ant-menu-horizontal > .ant-menu-item-selected {
  color: var(--color-primary) !important;
  border-bottom-color: var(--color-primary) !important;
}

.ant-menu-horizontal > .ant-menu-item {
  font-weight: 500 !important;
}

.ant-card {
  border-radius: var(--radius-md) !important;
  border: 1px solid #f1f5f9 !important;
  box-shadow: var(--shadow-sm) !important;
  transition: box-shadow var(--transition-base), transform var(--transition-base) !important;
}

.ant-card:hover {
  box-shadow: var(--shadow-md) !important;
}

.ant-table {
  border-radius: var(--radius-md) !important;
  overflow: hidden;
}

.ant-table-thead > tr > th {
  background: #f8fafc !important;
  font-weight: 600 !important;
  color: var(--text-secondary) !important;
  border-bottom: 2px solid #e2e8f0 !important;
}

.ant-table-tbody > tr:hover > td {
  background: #f8fafc !important;
}

.ant-tag {
  border-radius: 6px !important;
  font-weight: 500 !important;
  border: none !important;
}

.ant-tag-blue {
  background-color: rgba(59, 130, 246, 0.1) !important;
  color: #2563eb !important;
}

.ant-tag-green {
  background-color: rgba(16, 185, 129, 0.1) !important;
  color: #059669 !important;
}

.ant-tag-red {
  background-color: rgba(239, 68, 68, 0.1) !important;
  color: #dc2626 !important;
}

.ant-tag-gold,
.ant-tag-orange {
  background-color: rgba(245, 158, 11, 0.1) !important;
  color: #d97706 !important;
}

.ant-input,
.ant-input-affix-wrapper,
.ant-picker,
.ant-select-selector,
.ant-input-number {
  border-radius: var(--radius-sm) !important;
  transition: all var(--transition-fast) !important;
}

.ant-input:focus,
.ant-input-affix-wrapper:focus,
.ant-input-affix-wrapper-focused,
.ant-picker:focus,
.ant-select-focused .ant-select-selector {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important;
}

.ant-modal .ant-modal-content {
  border-radius: var(--radius-lg) !important;
  overflow: hidden;
}

.ant-modal .ant-modal-header {
  border-bottom: 1px solid #f1f5f9 !important;
}

.ant-descriptions-bordered .ant-descriptions-item-label {
  background: #f8fafc !important;
  font-weight: 500 !important;
  color: var(--text-secondary) !important;
}

.ant-pagination .ant-pagination-item-active {
  border-color: var(--color-primary) !important;
}

.ant-pagination .ant-pagination-item-active a {
  color: var(--color-primary) !important;
}

.ant-statistic-content-value {
  font-weight: 700 !important;
  letter-spacing: -0.5px;
}

.ant-avatar {
  border: 2px solid #f1f5f9;
}

.ant-spin-dot-item {
  background-color: var(--color-primary) !important;
}

/* Selection color */
::selection {
  background: rgba(59, 130, 246, 0.15);
  color: var(--text-primary);
}
</style>
