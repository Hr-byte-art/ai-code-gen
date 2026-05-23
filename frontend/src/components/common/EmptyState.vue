<template>
  <div class="empty-state">
    <div class="empty-icon">
      <InboxOutlined />
    </div>
    <p class="empty-title">{{ title || $t('common.noData') }}</p>
    <p v-if="description" class="empty-desc">{{ description }}</p>
    <div v-if="$slots.action" class="empty-action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { InboxOutlined } from '@ant-design/icons-vue'

defineProps<{
  title?: string
  description?: string
}>()
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  padding: 44px 24px;
  text-align: center;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--bg-card) 86%, transparent), transparent),
    repeating-linear-gradient(135deg, transparent 0 12px, color-mix(in srgb, var(--border-light) 36%, transparent) 12px 13px);
  border: 1px dashed var(--border);
  border-radius: var(--r-xl);
}

.empty-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  color: var(--c-primary);
  background: var(--c-primary-50);
  margin-bottom: 14px;
  font-size: 20px;
}

.empty-title {
  font-size: 15px;
  font-weight: 750;
  color: var(--t-primary);
  margin: 0 0 5px;
}

.empty-desc {
  max-width: 360px;
  font-size: 13px;
  color: var(--t-muted);
  margin: 0 0 18px;
  line-height: 1.7;
}
</style>