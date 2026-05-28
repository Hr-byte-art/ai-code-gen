<template>
  <div class="template-card" @click="$emit('select', template.templateKey)">
    <div class="template-preview">
      <img class="template-image" :src="previewImage" :alt="template.templateName" @error="handlePreviewError" />
    </div>
    <div class="template-info">
      <h4 class="template-name">{{ template.templateName }}</h4>
      <p class="template-desc">{{ template.description }}</p>
      <div class="template-meta">
        <a-tag size="small">{{ template.codeGenType }}</a-tag>
        <span class="template-uses">{{ template.useCount }} 次使用</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { CodeTemplate } from '@/types'
import templatePlaceholder from '@/assets/template-placeholder.webp'

const props = defineProps<{ template: CodeTemplate }>()
defineEmits<{ select: [key: string] }>()

const previewImage = ref(props.template.previewUrl || templatePlaceholder)

watch(() => props.template.previewUrl, (previewUrl) => {
  previewImage.value = previewUrl || templatePlaceholder
})

const handlePreviewError = () => {
  previewImage.value = templatePlaceholder
}
</script>

<style scoped>
.template-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--r-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--t-fast);
}
.template-card:hover {
  border-color: var(--c-primary-200);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}
.template-preview {
  height: 120px;
  background: var(--bg-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.template-preview img,
.template-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.template-info {
  padding: 12px;
}
.template-name {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 800;
  color: var(--t-primary);
}
.template-desc {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--t-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.template-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.template-uses {
  font-size: 11px;
  color: var(--t-light);
}
</style>
