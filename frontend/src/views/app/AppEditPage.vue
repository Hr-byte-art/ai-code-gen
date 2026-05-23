<template>
  <div class="edit-page">
    <div class="edit-container">
      <!-- 顶部工具栏 -->
      <div class="edit-toolbar">
        <div class="toolbar-left">
          <a-button @click="goBack" class="back-btn">
            <ArrowLeftOutlined />
            返回
          </a-button>
          <h2 class="page-title">编辑应用</h2>
        </div>
        <div class="toolbar-right">
          <a-button @click="handlePreview" class="toolbar-action">
            <EyeOutlined />
            预览
          </a-button>
          <a-button type="primary" @click="handleSave" :loading="saving">
            <SaveOutlined />
            保存
          </a-button>
          <a-button @click="handleDeploy" :loading="deploying" class="toolbar-action deploy-btn">
            <CloudUploadOutlined />
            部署
          </a-button>
        </div>
      </div>

      <!-- 编辑区域 -->
      <a-row :gutter="20">
        <!-- 左侧配置 -->
        <a-col :xs="24" :md="8">
          <div class="config-card">
            <div class="card-label">应用配置</div>
            <a-form :model="appForm" layout="vertical" class="config-form">
              <a-form-item label="应用名称">
                <a-input v-model:value="appForm.appName" placeholder="应用名称" />
              </a-form-item>

              <a-form-item label="应用 ID">
                <a-input :value="String(appId)" disabled class="disabled-input" />
              </a-form-item>

              <a-form-item label="初始提示词">
                <a-textarea
                  v-model:value="appForm.initPrompt"
                  :rows="5"
                  placeholder="描述你想要的应用..."
                  disabled
                  class="disabled-input"
                />
              </a-form-item>

              <a-form-item label="代码生成类型">
                <a-tag color="blue">{{ appForm.codeGenType || '默认' }}</a-tag>
              </a-form-item>

              <a-form-item label="部署状态">
                <div class="deploy-status">
                  <span class="status-dot" :class="appForm.deployedTime ? 'status-active' : 'status-pending'"></span>
                  <a-tag :color="appForm.deployedTime ? 'green' : 'orange'">
                    {{ appForm.deployedTime ? '已部署' : '未部署' }}
                  </a-tag>
                </div>
              </a-form-item>

              <a-form-item v-if="appForm.deployKey" label="部署地址">
                <a :href="deployUrl" target="_blank" class="deploy-link">{{ deployUrl }}</a>
              </a-form-item>
            </a-form>
          </div>
        </a-col>

        <!-- 右侧预览 -->
        <a-col :xs="24" :md="16">
          <div class="preview-card">
            <div class="preview-header">
              <h3 class="preview-title">
                <EyeOutlined />
                应用预览
              </h3>
              <a-button v-if="appForm.deployedTime" type="primary" size="small" @click="openDeploy">
                <LinkOutlined />
                打开应用
              </a-button>
            </div>
            <div class="preview-content">
              <div v-if="appForm.deployedTime" class="deploy-preview">
                <iframe :src="deployUrl" frameborder="0" class="preview-iframe"></iframe>
              </div>
              <div v-else class="no-deploy">
                <div class="no-deploy-icon">
                  <CloudUploadOutlined />
                </div>
                <p class="no-deploy-text">应用尚未部署</p>
                <p class="no-deploy-desc">部署后可在此处预览应用效果</p>
                <a-button type="primary" @click="handleDeploy" :loading="deploying">
                  立即部署
                </a-button>
              </div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  EyeOutlined,
  SaveOutlined,
  CloudUploadOutlined,
  LinkOutlined
} from '@ant-design/icons-vue'
import { getAppById, updateApp, deployApp } from '@/api/app'

const route = useRoute()
const router = useRouter()

const appId = Number(route.params.id)

const saving = ref(false)
const deploying = ref(false)

const appForm = reactive<any>({
  appName: '',
  initPrompt: '',
  codeGenType: '',
  deployKey: '',
  deployedTime: '',
  priority: 0
})

const deployUrl = computed(() => {
  if (appForm.deployKey) {
    return `/api/static/${appForm.deployKey}/`
  }
  return ''
})

const fetchAppInfo = async () => {
  try {
    const res = await getAppById(appId)
    Object.assign(appForm, res.data)
  } catch (error) {
    console.error('获取应用信息失败:', error)
    message.error('获取应用信息失败')
  }
}

const goBack = () => {
  router.back()
}

const handlePreview = () => {
  if (appForm.deployedTime) {
    window.open(deployUrl.value, '_blank')
  } else {
    message.info('请先部署应用')
  }
}

const openDeploy = () => {
  window.open(deployUrl.value, '_blank')
}

const handleSave = async () => {
  saving.value = true
  try {
    await updateApp({
      id: appId,
      appName: appForm.appName
    })
    message.success('保存成功')
  } catch (error) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleDeploy = async () => {
  deploying.value = true
  try {
    await deployApp(appId)
    message.success('部署请求已提交，请稍后刷新查看状态')
    setTimeout(() => {
      fetchAppInfo()
    }, 3000)
  } catch (error) {
    message.error('部署失败')
  } finally {
    deploying.value = false
  }
}

onMounted(() => {
  fetchAppInfo()
})
</script>

<style scoped>
.edit-page {
  padding: 24px;
  background: var(--bg-page);
  min-height: calc(100vh - 64px);
}

.edit-container {
  max-width: 1400px;
  margin: 0 auto;
}

.edit-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 20px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  border-radius: 8px !important;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.toolbar-action {
  border-radius: 8px !important;
}

.deploy-btn {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.08), rgba(16, 185, 129, 0.04)) !important;
  border-color: rgba(16, 185, 129, 0.3) !important;
  color: #059669 !important;
}

.config-card,
.preview-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
}

.card-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 16px;
}

.config-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: var(--text-secondary);
  font-size: 13px;
}

.disabled-input {
  background: #f8fafc !important;
  color: var(--text-muted) !important;
}

.deploy-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-active {
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.15);
}

.status-pending {
  background: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.15);
}

.deploy-link {
  color: var(--color-primary);
  font-weight: 500;
  word-break: break-all;
}

.preview-header {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-content {
  min-height: 400px;
}

.deploy-preview {
  width: 100%;
  height: 500px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.preview-iframe {
  width: 100%;
  height: 100%;
}

.no-deploy {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  text-align: center;
}

.no-deploy-icon {
  font-size: 48px;
  color: #d1d5db;
  margin-bottom: 16px;
}

.no-deploy-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-secondary);
  margin: 0 0 4px;
}

.no-deploy-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0 0 20px;
}

@media (max-width: 768px) {
  .edit-page {
    padding: 16px;
  }
  .edit-toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  .toolbar-right {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
