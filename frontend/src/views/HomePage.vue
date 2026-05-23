<template>
  <div class="home">
    <section class="hero">
      <div class="hero-grid">
        <div class="hero-copy">
          <div class="hero-kicker">从一句需求到可部署资产</div>
          <h1 class="hero-title">把应用想法整理成可迭代的工程</h1>
          <p class="hero-desc">
            熵擎把需求、生成、预览和部署放在同一条工作流里。先描述你要解决的问题，系统会创建应用资产，后续继续对话迭代。
          </p>
          <div class="hero-notes">
            <span>HTML / 多文件 / Vue 工程</span>
            <span>生成后可继续修改</span>
            <span>部署状态可追踪</span>
          </div>
          <figure class="workflow-preview">
            <img :src="workflowPreview" alt="熵擎 AI 应用生成工作流预览" />
          </figure>
        </div>

        <div class="prompt-panel">
          <div class="prompt-panel-head">
            <div>
              <span class="panel-eyebrow">新应用</span>
              <h2 class="panel-title">描述你要交付的东西</h2>
            </div>
            <span class="panel-shortcut">Ctrl Enter</span>
          </div>
          <a-textarea
            v-model:value="prompt"
            :rows="5"
            placeholder="例如：做一个给摄影师展示作品、预约档期、收集客户需求的网站，首页要有作品分类和联系表单。"
            class="prompt-input"
            @keydown.enter.ctrl="handleGenerate"
          />
          <div class="prompt-footer">
            <div class="prompt-hint">先创建资产，再进入工作台继续细化。</div>
            <a-button type="primary" :loading="generating" @click="handleGenerate">
              <RocketOutlined /> 创建应用
            </a-button>
          </div>
          <div class="templates">
            <button v-for="t in templates" :key="t.name" class="tpl-btn" @click="useTemplate(t.name)">
              <component :is="t.icon" class="tpl-icon" />
              {{ t.label }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <section class="workflow-section">
      <div class="section-inner">
        <div class="section-head compact">
          <span class="section-kicker">生成链路</span>
          <h2 class="section-title">每个页面只负责一件事</h2>
        </div>
        <div class="workflow-list">
          <div v-for="step in workflow" :key="step.title" class="workflow-item">
            <div class="workflow-index">{{ step.index }}</div>
            <div class="workflow-copy">
              <h3 class="workflow-title">{{ step.title }}</h3>
              <p class="workflow-desc">{{ step.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="featured-section">
      <div class="section-inner wide">
        <div class="section-head">
          <div>
            <span class="section-kicker">公开样本</span>
            <h2 class="section-title">看看别人把需求沉淀成了什么</h2>
          </div>
          <a-button @click="$router.push('/app')">进入我的资产</a-button>
        </div>
        <a-spin :spinning="loading">
          <div class="app-grid" v-if="appList.length > 0">
            <AppCard v-for="app in appList" :key="app.id" :app="app" @click="goToApp(app.id)" />
          </div>
          <EmptyState v-if="!loading && appList.length === 0" title="还没有公开样本" description="创建并部署应用后，可以在这里看到可复用的案例资产。" />
        </a-spin>
        <div class="pagination-wrap" v-if="total > pageSize">
          <a-pagination v-model:current="currentPage" :total="total" :page-size="pageSize" @change="handlePageChange" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  RocketOutlined, ShoppingOutlined, ReadOutlined, CheckSquareOutlined, MessageOutlined
} from '@ant-design/icons-vue'
import AppCard from '@/components/AppCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import workflowPreview from '@/assets/workflow-preview.webp'
import { getGoodAppList, createApp } from '@/api/app'

const router = useRouter()
const prompt = ref('')
const generating = ref(false)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(0)
const appList = ref<any[]>([])

const templates = [
  { name: '电商展示与下单', label: '电商站', icon: ShoppingOutlined },
  { name: '内容博客与分类归档', label: '内容站', icon: ReadOutlined },
  { name: '团队任务看板', label: '任务板', icon: CheckSquareOutlined },
  { name: '客服对话页面', label: '对话页', icon: MessageOutlined },
]

const workflow = [
  { index: '01', title: '描述需求', desc: '把目标用户、页面内容和核心动作说清楚。' },
  { index: '02', title: '生成资产', desc: '系统创建应用记录，并保存后续迭代上下文。' },
  { index: '03', title: '对话迭代', desc: '围绕同一个应用持续调整页面、文案和交互。' },
  { index: '04', title: '预览部署', desc: '确认交付结果，并把可访问地址沉淀到资产列表。' },
]

const fetchAppList = async () => {
  loading.value = true
  try {
    const res: any = await getGoodAppList({ pageNum: currentPage.value, pageSize: pageSize.value })
    appList.value = (res.data?.records || []).map((a: any) => ({ ...a, title: a.appName || a.title }))
    total.value = res.data?.totalRow || 0
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const handleGenerate = async () => {
  if (!prompt.value.trim()) { message.warning('先写一句你要做什么'); return }
  generating.value = true
  try {
    const res = await createApp({ initPrompt: prompt.value })
    message.success('应用资产已创建')
    router.push(`/app/edit/${res.data}`)
  } catch (e) { message.error('创建失败，请重试') }
  finally { generating.value = false }
}

const useTemplate = (t: string) => { prompt.value = `请帮我生成一个${t}应用` }
const goToApp = (id: number) => router.push(`/app/chat/${id}`)
const handlePageChange = (page: number) => { currentPage.value = page; fetchAppList() }
onMounted(() => fetchAppList())
</script>

<style scoped>
.home {
  min-height: 100vh;
}

.hero {
  padding: 72px 24px 58px;
  background:
    linear-gradient(135deg, rgba(49, 92, 83, 0.08), transparent 34%),
    var(--bg-card);
  border-bottom: 1px solid var(--border-light);
}

.hero-grid {
  max-width: 1120px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 0.94fr) minmax(420px, 1fr);
  gap: 44px;
  align-items: center;
}

.hero-kicker,
.section-kicker,
.panel-eyebrow {
  display: inline-flex;
  color: var(--c-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.hero-title {
  max-width: 640px;
  font-size: clamp(38px, 5vw, 64px);
  font-weight: 850;
  line-height: 1.02;
  color: var(--t-primary);
  margin: 14px 0 18px;
  letter-spacing: -2px;
}

.hero-desc {
  max-width: 560px;
  font-size: 16px;
  color: var(--t-muted);
  margin: 0;
  line-height: 1.85;
}

.hero-notes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 26px;
}

.hero-notes span {
  padding: 6px 10px;
  border: 1px solid var(--border-light);
  border-radius: var(--r-full);
  background: color-mix(in srgb, var(--bg-soft) 70%, transparent);
  color: var(--t-secondary);
  font-size: 12px;
  font-weight: 650;
}

.workflow-preview {
  margin: 28px 0 0;
  padding: 10px;
  border: 1px solid var(--border-light);
  border-radius: var(--r-2xl);
  background: color-mix(in srgb, var(--bg-card) 76%, transparent);
  box-shadow: var(--shadow-sm);
}

.workflow-preview img {
  display: block;
  width: 100%;
  aspect-ratio: 3 / 2;
  object-fit: cover;
  border-radius: calc(var(--r-2xl) - 8px);
}

.prompt-panel {
  padding: 22px;
  border: 1px solid var(--border);
  border-radius: var(--r-2xl);
  background: var(--bg-card);
  box-shadow: var(--shadow-md);
}

.prompt-panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  margin-bottom: 16px;
}

.panel-title {
  margin: 5px 0 0;
  color: var(--t-primary);
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.4px;
}

.panel-shortcut {
  padding: 4px 8px;
  border: 1px solid var(--border-light);
  border-radius: var(--r-sm);
  color: var(--t-light);
  background: var(--bg-soft);
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.prompt-input {
  border: 1px solid var(--border-light) !important;
  box-shadow: none !important;
  padding: 14px !important;
  font-size: 14px !important;
  resize: none;
  background: var(--bg-input) !important;
}

.prompt-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  padding-top: 14px;
}

.prompt-hint {
  font-size: 12px;
  color: var(--t-light);
}

.templates {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.tpl-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border-radius: var(--r-full);
  background: transparent;
  border: 1px solid var(--border-light);
  cursor: pointer;
  font-size: 12px;
  font-weight: 650;
  color: var(--t-secondary);
  transition: background var(--t-fast), border-color var(--t-fast), color var(--t-fast);
}

.tpl-btn:hover {
  border-color: var(--c-primary-200);
  color: var(--c-primary);
  background: var(--c-primary-50);
}

.tpl-icon {
  font-size: 13px;
  display: flex;
}

.workflow-section {
  padding: 54px 24px;
  background: var(--bg-body);
}

.section-inner {
  max-width: 980px;
  margin: 0 auto;
}

.section-inner.wide {
  max-width: 1120px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 18px;
  margin-bottom: 24px;
}

.section-head.compact {
  display: block;
}

.section-title {
  font-size: 26px;
  font-weight: 830;
  color: var(--t-primary);
  margin: 6px 0 0;
  letter-spacing: -0.7px;
}

.workflow-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border: 1px solid var(--border-light);
  border-radius: var(--r-xl);
  background: var(--bg-card);
  overflow: hidden;
}

.workflow-item {
  min-height: 168px;
  padding: 20px;
  border-right: 1px solid var(--border-light);
}

.workflow-item:last-child {
  border-right: none;
}

.workflow-index {
  color: var(--c-cta);
  font-size: 12px;
  font-weight: 850;
  letter-spacing: 0.08em;
  margin-bottom: 36px;
}

.workflow-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--t-primary);
  margin: 0 0 8px;
}

.workflow-desc {
  color: var(--t-muted);
  font-size: 13px;
  line-height: 1.7;
  margin: 0;
}

.featured-section {
  padding: 56px 24px 72px;
  background: var(--bg-card);
  border-top: 1px solid var(--border-light);
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 920px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }

  .workflow-list {
    grid-template-columns: 1fr 1fr;
  }

  .workflow-item:nth-child(2) {
    border-right: none;
  }
}

@media (max-width: 768px) {
  .hero { padding: 46px 16px 38px; }
  .hero-title { letter-spacing: -1.1px; }
  .prompt-panel { padding: 16px; }
  .prompt-footer { align-items: stretch; flex-direction: column; }
  .prompt-footer :deep(.ant-btn) { width: 100%; }
  .section-head { align-items: flex-start; flex-direction: column; }
  .workflow-list { grid-template-columns: 1fr; }
  .workflow-item { min-height: auto; border-right: none; border-bottom: 1px solid var(--border-light); }
  .workflow-item:last-child { border-bottom: none; }
  .workflow-index { margin-bottom: 18px; }
  .app-grid { grid-template-columns: 1fr; }
}
</style>