<template>
  <div class="page-header">
    <div class="page-header-left">
      <div v-if="eyebrow" class="page-header-eyebrow">{{ eyebrow }}</div>
      <h1 class="page-header-title">{{ title }}</h1>
      <p v-if="description" class="page-header-desc">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-header-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
  eyebrow?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  margin-bottom: 22px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}

.page-header-left {
  min-width: 0;
}

.page-header-eyebrow {
  width: fit-content;
  margin-bottom: 8px;
  padding: 3px 8px;
  border: 1px solid var(--border);
  border-radius: var(--r-full);
  color: var(--t-muted);
  background: var(--bg-soft);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.page-header-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--t-primary);
  margin: 0;
  letter-spacing: -0.5px;
  line-height: 1.2;
}

.page-header-desc {
  max-width: 680px;
  font-size: 14px;
  color: var(--t-muted);
  margin: 7px 0 0;
  line-height: 1.7;
}

.page-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .page-header-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>