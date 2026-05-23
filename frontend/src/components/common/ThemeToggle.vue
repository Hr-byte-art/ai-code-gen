<template>
  <a-dropdown>
    <button class="theme-toggle" :title="$t('theme.' + themeStore.mode)">
      <span class="theme-icon">{{ themeStore.resolved === 'light' ? '☀' : '☾' }}</span>
    </button>
    <template #overlay>
      <a-menu @click="handleChange">
        <a-menu-item key="light">☀ {{ $t('theme.light') }}</a-menu-item>
        <a-menu-item key="dark">☾ {{ $t('theme.dark') }}</a-menu-item>
        <a-menu-item key="system">⊞ {{ $t('theme.system') }}</a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup lang="ts">
import { useThemeStore } from '@/stores/theme'
const themeStore = useThemeStore()
const handleChange = ({ key }: { key: string }) => { themeStore.setMode(key as 'light' | 'dark' | 'system') }
</script>

<style scoped>
.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  background: #fff;
  cursor: pointer;
  color: var(--t-muted);
  transition: border-color 0.15s;
  padding: 0;
}

.theme-toggle:hover {
  border-color: var(--c-primary-200);
  color: var(--c-primary);
}

.theme-icon {
  font-size: 14px;
  line-height: 1;
}
</style>
