<template>
  <div class="stat-card" :style="{ '--accent': color || 'var(--c-primary)' }">
    <div class="stat-card-mark">
      <component :is="icon" />
    </div>
    <div class="stat-card-info">
      <div class="stat-card-label">{{ label }}</div>
      <div class="stat-card-value">{{ value }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  label: string
  value: string | number
  icon: Component
  color?: string
}>()
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 15px;
  background: var(--bg-card);
  border-radius: var(--r-lg);
  border: 1px solid var(--border-light);
  box-shadow: none;
}

.stat-card-mark {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 12%, transparent);
  border: 1px solid color-mix(in srgb, var(--accent) 22%, transparent);
  font-size: 15px;
  flex-shrink: 0;
}

.stat-card-info {
  min-width: 0;
}

.stat-card-label {
  font-size: 12px;
  color: var(--t-muted);
  margin-bottom: 1px;
}

.stat-card-value {
  font-size: 19px;
  font-weight: 800;
  color: var(--t-primary);
  letter-spacing: -0.45px;
  line-height: 1.2;
}
</style>