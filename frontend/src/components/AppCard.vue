<template>
  <div class="app-card" @click="$emit('click')">
    <div class="cover-wrap">
      <img class="cover-image" :src="coverImage" :alt="app.title" @error="handleCoverError" />
    </div>

    <div class="card-head">
      <div class="card-identity">
        <div class="asset-mark">{{ initials }}</div>
        <div class="card-title-group">
          <h3 class="card-title">{{ app.title }}</h3>
          <div class="card-subline">
            <span v-if="app.codeGenType">{{ codeGenTypeLabel }}</span>
            <span>{{ timeAgo }}</span>
          </div>
        </div>
      </div>
      <span :class="['status-pill', statusClass]">{{ statusLabel }}</span>
    </div>

    <p class="card-desc">{{ app.description || app.initPrompt || '还没有补充应用说明' }}</p>

    <div class="next-step">
      <span class="next-label">下一步</span>
      <span class="next-text">{{ nextStep }}</span>
    </div>

    <div class="card-actions">
      <a-button size="small" type="primary" class="primary-action" @click.stop="$emit('chat')">
        <MessageOutlined /> 继续迭代
      </a-button>
      <a-button size="small" @click.stop="$emit('preview')" v-if="app.deployedTime">
        <EyeOutlined /> 预览
      </a-button>
      <a-button size="small" @click.stop="$emit('deploy')" v-else>
        <CloudUploadOutlined /> 部署
      </a-button>
      <a-dropdown trigger="click">
        <a-button size="small" class="more-action" @click.stop>
          <MoreOutlined /> 更多
        </a-button>
        <template #overlay>
          <a-menu @click.stop>
            <a-menu-item key="edit" @click="$emit('edit')">
              <EditOutlined /> 交付页
            </a-menu-item>
            <a-menu-item key="delete" danger>
              <a-popconfirm title="确定删除此应用？删除后不可恢复。" @confirm.stop="$emit('delete')">
                <span class="delete-menu-item" @click.stop>
                  <DeleteOutlined /> 删除应用
                </span>
              </a-popconfirm>
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  MessageOutlined, EditOutlined, EyeOutlined,
  CloudUploadOutlined, DeleteOutlined, MoreOutlined
} from '@ant-design/icons-vue'
import defaultCover from '@/assets/default-cover.webp'
import coverCreated from '@/assets/cover-created.webp'
import coverGenerated from '@/assets/cover-generated.webp'
import coverFailed from '@/assets/cover-failed.webp'
import coverCancelled from '@/assets/cover-cancelled.webp'
import coverDeployed from '@/assets/cover-deployed.webp'
import coverRestricted from '@/assets/cover-restricted.webp'

interface AppInfo {
  id: string
  title: string
  description?: string
  initPrompt?: string
  author?: string
  avatar?: string
  cover?: string
  codeGenType?: string
  deployedTime?: string
  createTime?: string
  updateTime?: string
  deployKey?: string
}

const props = defineProps<{ app: AppInfo }>()
defineEmits<{ click: []; chat: []; edit: []; preview: []; deploy: []; delete: [] }>()

const STATUS_COVER_MARKERS = {
  generated: 'defaultAppCover.jpg',
  failed: 'createFailed.png',
  cancelled: 'userCancel.png',
  restricted: 'noPermission.png',
}

const isRealScreenshotCover = (cover: string) => cover.includes('/screenshots/')

const resolveCoverImage = (cover?: string) => {
  if (cover) {
    if (cover.includes(STATUS_COVER_MARKERS.failed)) return coverFailed
    if (cover.includes(STATUS_COVER_MARKERS.cancelled)) return coverCancelled
    if (cover.includes(STATUS_COVER_MARKERS.restricted)) return coverRestricted
    if (isRealScreenshotCover(cover)) return cover
    if (cover.includes(STATUS_COVER_MARKERS.generated)) {
      return props.app.deployedTime ? coverDeployed : coverGenerated
    }
    return cover
  }

  if (props.app.deployedTime) return coverDeployed
  if (props.app.deployKey) return coverGenerated
  return coverCreated
}

const coverImage = ref(resolveCoverImage(props.app.cover))

watch(() => [props.app.cover, props.app.deployedTime, props.app.deployKey], () => {
  coverImage.value = resolveCoverImage(props.app.cover)
})

const handleCoverError = () => {
  coverImage.value = props.app.deployedTime ? coverDeployed : defaultCover
}

const codeGenTypeLabel = computed(() => {
  const labels: Record<string, string> = { html: 'HTML', multi_file: '多文件', vue_project: 'Vue', fullstack: '全栈' }
  return labels[props.app.codeGenType || ''] || props.app.codeGenType
})
const statusLabel = computed(() => props.app.deployedTime ? '已上线' : props.app.deployKey ? '可部署' : '草稿')
const statusClass = computed(() => props.app.deployedTime ? 'online' : props.app.deployKey ? 'ready' : 'draft')
const nextStep = computed(() => props.app.deployedTime ? '查看线上效果或继续迭代' : props.app.deployKey ? '部署并确认访问地址' : '继续对话完善需求')
const initials = computed(() => (props.app.title || '应用').slice(0, 2))

const timeAgo = computed(() => {
  const time = props.app.updateTime || props.app.createTime
  if (!time) return '时间未知'
  const diff = Date.now() - new Date(time).getTime()
  if (diff < 60000) return '刚刚更新'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 2592000000) return `${Math.floor(diff / 86400000)} 天前`
  return new Date(time).toLocaleDateString('zh-CN')
})
</script>

<style scoped>
.app-card {
  background: var(--bg-card);
  border-radius: var(--r-lg);
  border: 1px solid var(--border-light);
  padding: 16px;
  cursor: pointer;
  transition: border-color var(--t-fast), background var(--t-fast), transform var(--t-fast), box-shadow var(--t-fast);
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.app-card:hover {
  border-color: var(--border-strong);
  background: color-mix(in srgb, var(--bg-card) 86%, var(--c-primary-50));
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.cover-wrap {
  height: 132px;
  border-radius: calc(var(--r-lg) - 4px);
  overflow: hidden;
  background: var(--bg-soft);
  border: 1px solid var(--border-light);
}

.cover-image {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.card-identity {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
}

.asset-mark {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--bg-soft);
  border: 1px solid var(--border-light);
  color: var(--c-primary);
  font-size: 12px;
  font-weight: 850;
}

.card-title-group {
  min-width: 0;
}

.card-title {
  font-size: 15px;
  font-weight: 780;
  color: var(--t-primary);
  margin: 0 0 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-subline {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--t-light);
  font-size: 11px;
  font-weight: 650;
}

.card-subline span + span::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 4px;
  margin-right: 8px;
  border-radius: 50%;
  background: var(--border-strong);
  vertical-align: middle;
}

.status-pill {
  padding: 3px 8px;
  border-radius: var(--r-full);
  font-size: 11px;
  font-weight: 780;
  white-space: nowrap;
  border: 1px solid transparent;
}

.status-pill.online {
  color: var(--c-success);
  background: #eaf4ee;
  border-color: #cfe5d8;
}

.status-pill.ready {
  color: var(--c-warning);
  background: #f6ede1;
  border-color: #e8d4bb;
}

.status-pill.draft {
  color: var(--t-muted);
  background: var(--bg-soft);
  border-color: var(--border-light);
}

.card-desc {
  min-height: 40px;
  font-size: 13px;
  color: var(--t-muted);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.6;
}

.next-step {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: var(--r-md);
  background: var(--bg-soft);
  border: 1px solid var(--border-light);
}

.next-label {
  color: var(--t-light);
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
}

.next-text {
  color: var(--t-secondary);
  font-size: 12px;
  font-weight: 650;
}

.card-actions {
  display: flex;
  gap: 7px;
  padding-top: 1px;
  align-items: center;
}

.card-actions :deep(.ant-btn) {
  font-size: 12px !important;
  height: 30px !important;
  padding: 0 11px !important;
}

.primary-action {
  flex: 1;
}

.more-action {
  color: var(--t-muted);
}

.delete-menu-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
</style>