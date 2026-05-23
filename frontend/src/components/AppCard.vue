<template>
  <div class="app-card" @click="$emit('click')">
    <div class="app-preview">
      <img
        v-if="app.cover"
        :src="app.cover"
        :alt="app.title"
        class="app-cover"
      />
      <div v-else class="app-placeholder">
        <img
          v-if="app.avatar"
          :src="app.avatar"
          :alt="app.title"
          class="placeholder-logo"
        />
        <AppstoreOutlined v-else />
      </div>
      <div class="app-overlay">
        <a-button type="primary" ghost class="overlay-btn">
          <MessageOutlined />
          打开对话
        </a-button>
      </div>
    </div>
    <div class="app-info">
      <div class="app-info-left">
        <a-avatar :src="app.avatar" :size="36">
          <template #icon><UserOutlined /></template>
        </a-avatar>
      </div>
      <div class="app-info-right">
        <h3 class="app-title">{{ app.title }}</h3>
        <p class="app-author">{{ app.author }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { AppstoreOutlined, UserOutlined, MessageOutlined } from '@ant-design/icons-vue'

interface AppInfo {
  id: number
  title: string
  description: string
  author: string
  avatar: string
  cover: string
}

defineProps<{
  app: AppInfo
}>()

defineEmits<{
  click: []
}>()
</script>

<style scoped>
.app-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
  transition: all var(--transition-slow);
  cursor: pointer;
}

.app-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg);
  border-color: rgba(59, 130, 246, 0.12);
}

.app-preview {
  height: 180px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 50%, #e8eef6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

.app-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.app-card:hover .app-cover {
  transform: scale(1.05);
}

.app-placeholder {
  font-size: 48px;
  color: #d1d5db;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-logo {
  width: 56px;
  height: 56px;
  object-fit: contain;
  opacity: 0.5;
  transition: opacity var(--transition-base);
}

.app-card:hover .placeholder-logo {
  opacity: 0.7;
}

.app-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.85), rgba(139, 92, 246, 0.8));
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--transition-slow);
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.overlay-btn {
  border-color: rgba(255, 255, 255, 0.9) !important;
  color: #fff !important;
  font-weight: 600;
  border-radius: 10px !important;
  padding: 6px 20px !important;
  height: auto !important;
  backdrop-filter: blur(4px);
}

.overlay-btn:hover {
  background: rgba(255, 255, 255, 0.15) !important;
}

.app-info {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-top: 1px solid #f8fafc;
}

.app-info-left {
  flex-shrink: 0;
}

.app-info-right {
  flex: 1;
  min-width: 0;
}

.app-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 2px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-author {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
