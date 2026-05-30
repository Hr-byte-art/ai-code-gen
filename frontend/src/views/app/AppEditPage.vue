<template>
  <div class="delivery-page">
    <PageHeader :title="appForm.appName || '应用交付'" :description="phaseLabel" eyebrow="交付工作台">
      <template #actions>
        <a-button @click="goBack" size="small"><ArrowLeftOutlined /> 返回</a-button>
        <a-button @click="goToChat" size="small"><MessageOutlined /> 继续迭代</a-button>
        <a-button @click="handlePreview" size="small"><EyeOutlined /> 预览</a-button>
        <a-button type="primary" @click="handleDeploy" :loading="deploying" size="small">
          <CloudUploadOutlined /> {{ appForm.deployedTime ? '重新部署' : '部署' }}
        </a-button>
      </template>
    </PageHeader>

    <section class="delivery-status">
      <div v-for="step in phaseSteps" :key="step.key" :class="['phase-node', step.state]">
        <span class="phase-index">{{ step.index }}</span>
        <div>
          <div class="phase-title">{{ step.title }}</div>
          <div class="phase-desc">{{ step.desc }}</div>
        </div>
      </div>
    </section>

    <section class="delivery-grid">
      <aside class="config-panel">
        <div class="panel-head">
          <span class="panel-kicker">应用信息</span>
          <a-button type="link" size="small" @click="handleSave" :loading="saving">
            <SaveOutlined /> 保存名称
          </a-button>
        </div>

        <a-form :model="appForm" layout="vertical" class="config-form">
          <a-form-item label="应用名称">
            <a-input v-model:value="appForm.appName" placeholder="应用名称" />
          </a-form-item>
          <div class="info-row">
            <span>应用 ID</span>
            <strong>{{ appId }}</strong>
          </div>
          <div class="info-row">
            <span>生成类型</span>
            <a-tag color="blue">{{ codeGenTypeLabel }}</a-tag>
          </div>
          <div class="info-row">
            <span>部署状态</span>
            <a-tag :color="appForm.deployedTime ? 'green' : 'orange'">
              {{ appForm.deployedTime ? '已上线' : '未上线' }}
            </a-tag>
          </div>
          <a-form-item label="初始需求">
            <a-textarea v-model:value="appForm.initPrompt" :rows="6" disabled />
          </a-form-item>
        </a-form>

        <div v-if="appForm.deployKey" class="deploy-box">
          <span class="deploy-label">访问地址</span>
          <a :href="deployUrl" target="_blank" class="deploy-link">{{ deployUrl }}</a>
        </div>
      </aside>

      <main class="preview-panel">
        <div class="preview-head">
          <div>
            <span class="panel-kicker">预览确认</span>
            <h2 class="preview-title">{{ appForm.deployedTime ? '线上版本' : '等待部署' }}</h2>
          </div>
          <a-button v-if="appForm.deployKey" type="primary" size="small" @click="openDeploy">
            <LinkOutlined /> 打开应用
          </a-button>
        </div>

        <div v-if="appForm.deployKey" class="preview-frame">
          <iframe :src="deployUrl" frameborder="0" class="iframe"></iframe>
        </div>
        <EmptyState v-else title="还没有可预览地址" description="完成部署后，这里会展示应用页面。你也可以先回到生成工作台继续调整需求。">
          <template #action>
            <div class="empty-actions">
              <a-button @click="goToChat">继续迭代</a-button>
              <a-button type="primary" @click="handleDeploy" :loading="deploying">立即部署</a-button>
            </div>
          </template>
        </EmptyState>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined, EyeOutlined, SaveOutlined,
  CloudUploadOutlined, LinkOutlined, MessageOutlined
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getAppById, updateApp, deployApp, getAppBuildStatus } from '@/api/app'

const route = useRoute()
const router = useRouter()
const appId = route.params.id as string
const saving = ref(false)
const deploying = ref(false)

const appForm = reactive<any>({
  appName: '', initPrompt: '', codeGenType: '', deployKey: '', deployedTime: '', priority: 0
})
const buildStatus = ref<Record<string, any>>({})

const codeGenTypeLabel = computed(() => {
  const labels: Record<string, string> = { html: 'HTML', multi_file: '多文件', vue_project: 'Vue', fullstack: '全栈' }
  return labels[appForm.codeGenType] || appForm.codeGenType || '默认'
})
const expressDeployUrl = ref<string>('')
const deployUrl = computed(() => expressDeployUrl.value || (appForm.deployKey ? `/api/code_deploy/${appForm.deployKey}/index.html` : ''))

const currentPhase = computed(() => {
  if (appForm.deployedTime) return 4
  if (appForm.deployKey || buildStatus.value?.distExists || buildStatus.value?.projectExists) return 3
  return 2
})

const phaseLabel = computed(() => {
  const labels: Record<number, string> = {
    2: '等待生成代码，请先进入生成工作台',
    3: '代码已生成，可以预览或部署',
    4: '已部署上线，可以继续迭代后重新部署',
  }
  return labels[currentPhase.value] || ''
})

const getPhaseState = (phase: number) => {
  if (currentPhase.value > phase) return 'done'
  if (currentPhase.value === phase) return 'active'
  return ''
}

const phaseSteps = computed(() => [
  { key: 'created', index: '01', title: '需求建档', desc: '应用记录已创建', state: getPhaseState(1) },
  { key: 'generated', index: '02', title: '生成代码', desc: '等待生成流开始', state: getPhaseState(2) },
  { key: 'preview', index: '03', title: '预览确认', desc: '确认页面和交互', state: getPhaseState(3) },
  { key: 'deploy', index: '04', title: '部署交付', desc: '获得可访问地址', state: getPhaseState(4) },
])

const fetchAppInfo = async () => {
  try {
    const res = await getAppById(appId)
    Object.assign(appForm, res.data)
    const statusRes = await getAppBuildStatus(appId)
    buildStatus.value = statusRes.data || {}
  } catch (e) {
    message.error('应用不存在或已被删除')
    router.push('/app')
  }
}

const goBack = () => router.back()
const goToChat = () => router.push(`/app/chat/${appId}`)
const handlePreview = async () => {
  if (!appForm.deployKey) { message.info('请先部署应用'); return }
  // 全栈项目需要通过 deployApp 获取 Express URL
  if (appForm.codeGenType === 'fullstack' && !expressDeployUrl.value) {
    const url = await deployApp(appId)
    if (url) expressDeployUrl.value = url
  }
  window.open(deployUrl.value, '_blank')
}
const openDeploy = () => window.open(deployUrl.value, '_blank')
const handleSave = async () => {
  saving.value = true
  try { await updateApp({ id: appId, appName: appForm.appName }); message.success('保存成功') }
  catch (e) { message.error('保存失败') }
  finally { saving.value = false }
}
const handleDeploy = async () => {
  deploying.value = true
  try {
    const url = await deployApp(appId)
    if (url) expressDeployUrl.value = url
    message.success('部署成功')
    subscribeBuildEvents()
  }
  catch (e) { message.error('部署失败') }
  finally { deploying.value = false }
}

const buildEventSource = ref<EventSource | null>(null)

const subscribeBuildEvents = () => {
  if (buildEventSource.value) { buildEventSource.value.close() }
  const es = new EventSource(`/api/app/build/events/${appId}`, { withCredentials: true })
  es.onmessage = (e) => {
    try {
      const event = JSON.parse(e.data)
      if (event.type === 'build_success') {
        message.success('构建完成')
        fetchAppInfo()
        es.close()
      } else if (event.type === 'build_fail') {
        message.error('构建失败: ' + (event.message || '未知错误'))
        es.close()
      } else if (event.type === 'deploy_success') {
        message.success('部署成功')
        fetchAppInfo()
        es.close()
      }
    } catch {}
  }
  es.onerror = () => { es.close() }
  buildEventSource.value = es
}

onMounted(() => fetchAppInfo())
</script>

<style scoped>
.delivery-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 30px 24px 48px;
}

.delivery-status {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  overflow: hidden;
  margin-bottom: 18px;
  border: 1px solid var(--border-light);
  border-radius: var(--r-xl);
  background: var(--border-light);
}

.phase-node {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 11px;
  min-height: 92px;
  padding: 16px;
  background: var(--bg-card);
}

.phase-node.active {
  background: var(--c-primary-50);
}

.phase-index {
  color: var(--t-light);
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.08em;
}

.phase-node.done .phase-index,
.phase-node.active .phase-index {
  color: var(--c-primary);
}

.phase-title {
  color: var(--t-primary);
  font-size: 14px;
  font-weight: 850;
  margin-bottom: 4px;
}

.phase-desc {
  color: var(--t-muted);
  font-size: 12px;
  line-height: 1.6;
}

.delivery-grid {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.config-panel,
.preview-panel {
  border: 1px solid var(--border-light);
  border-radius: var(--r-xl);
  background: var(--bg-card);
}

.config-panel {
  padding: 18px;
}

.panel-head,
.preview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-light);
}

.panel-kicker {
  color: var(--t-light);
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.08em;
}

.config-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-light);
  color: var(--t-muted);
  font-size: 13px;
}

.info-row strong {
  color: var(--t-primary);
}

.deploy-box {
  margin-top: 2px;
  padding: 12px;
  border-radius: var(--r-md);
  background: var(--bg-soft);
  border: 1px solid var(--border-light);
}

.deploy-label {
  display: block;
  margin-bottom: 6px;
  color: var(--t-light);
  font-size: 11px;
  font-weight: 850;
}

.deploy-link {
  color: var(--c-primary);
  font-weight: 700;
  word-break: break-all;
}

.preview-panel {
  min-height: 620px;
  padding: 18px;
}

.preview-title {
  margin: 4px 0 0;
  color: var(--t-primary);
  font-size: 20px;
  font-weight: 850;
  letter-spacing: -0.4px;
}

.preview-frame {
  height: 560px;
  border-radius: var(--r-lg);
  overflow: hidden;
  border: 1px solid var(--border);
  background: var(--bg-soft);
}

.iframe {
  width: 100%;
  height: 100%;
  background: #fff;
}

.empty-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}

@media (max-width: 980px) {
  .delivery-status {
    grid-template-columns: repeat(2, 1fr);
  }

  .delivery-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .delivery-page { padding: 22px 16px 36px; }
  .delivery-status { grid-template-columns: 1fr; }
  .preview-frame { height: 460px; }
}
</style>