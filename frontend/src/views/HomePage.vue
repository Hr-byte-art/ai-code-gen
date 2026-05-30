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
          </div>
          <a-textarea
            v-model:value="prompt"
            :rows="5"
            placeholder="例如：做一个给摄影师展示作品、预约档期、收集客户需求的网站，首页要有作品分类和联系表单。"
            class="prompt-input"
            @keydown.enter.ctrl="handleGenerate"
          />
          <div class="prompt-footer">
            <div class="prompt-hint">Enter 换行，Ctrl + Enter 开始生成</div>
            <a-button type="primary" size="large" :loading="generating || routingLoading" @click="handleGenerate">
              <RocketOutlined /> {{ routingLoading ? 'AI 分析中...' : '开始生成' }}
            </a-button>
          </div>

          <div class="choice-area">
            <div class="choice-group">
              <div class="choice-head">
                <span>应用场景</span>
                <small>可选，帮你快速写入需求</small>
              </div>
              <div class="choice-list" v-if="apiTemplates.length > 0">
                <button
                  v-for="t in apiTemplates"
                  :key="t.templateKey"
                  class="choice-btn scenario"
                  :class="{ active: selectedTemplateKey === t.templateKey }"
                  @click="useApiTemplate(t.templateKey)"
                >
                  {{ t.templateName }}
                </button>
              </div>
              <div class="choice-list" v-else>
                <button v-for="t in scenarioTemplates" :key="t.name" class="choice-btn scenario" @click="useTemplate(t.name)">
                  <component :is="t.icon" class="tpl-icon" />
                  {{ t.label }}
                </button>
              </div>
            </div>

            <div class="choice-group">
              <div class="choice-head">
                <span>生成模式</span>
                <small>默认自动判断，不懂技术也能直接开始</small>
              </div>
              <div class="choice-list">
                <button class="choice-btn mode" :class="{ active: !selectedCodeGenType }" @click="useAutoMode">
                  <span>自动推荐</span>
                  <small>AI 判断</small>
                </button>
                <template v-if="skills.length > 0">
                  <button
                    v-for="s in skills"
                    :key="s.skillKey"
                    class="choice-btn mode"
                    :class="{ active: selectedCodeGenType === s.codeGenType }"
                    @click="useSkill(s)"
                  >
                    <span>{{ s.name }}</span>
                    <small>{{ formatSkillCost(s) }}</small>
                  </button>
                </template>
                <template v-else>
                  <button
                    v-for="m in fallbackModes"
                    :key="m.codeGenType"
                    class="choice-btn mode"
                    :class="{ active: selectedCodeGenType === m.codeGenType }"
                    @click="useFallbackMode(m.codeGenType)"
                  >
                    <span>{{ m.label }}</span>
                    <small>{{ m.desc }}</small>
                  </button>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 全栈/前端确认卡片 -->
    <a-modal v-model:open="showRoutingModal" :footer="null" :closable="false" :maskClosable="false" width="420px" centered>
      <div class="routing-confirm">
        <div class="routing-icon">🤖</div>
        <h3 class="routing-title">AI 推荐：{{ routingRecommendation?.recommendedName }}</h3>
        <p class="routing-reason">{{ routingRecommendation?.reason }}</p>
        <div class="routing-options">
          <button class="routing-btn primary" @click="confirmFullstack">
            <span class="routing-badge">推荐方案</span>
            <strong>生成全栈应用</strong>
            <span>包含后端 API、数据能力和前端页面 · {{ routingRecommendation?.pointCost }} 积分</span>
          </button>
          <button class="routing-btn secondary" @click="chooseFrontend">
            <span class="routing-badge muted">轻量方案</span>
            <strong>只生成前端页面</strong>
            <span>适合展示型页面，不包含后端数据能力 · {{ routingRecommendation?.alternativePointCost }} 积分</span>
          </button>
        </div>
      </div>
    </a-modal>

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
          <a-button @click="$router.push('/app')">查看我的应用</a-button>
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
import { getGoodAppList, createApp, getRoutingRecommendation } from '@/api/app'
import { listTemplates } from '@/api/template'
import { listSkills } from '@/api/skill'
import type { CodeTemplate, CodeSkill, RoutingRecommendation } from '@/types'

const router = useRouter()
const prompt = ref('')
const generating = ref(false)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(0)
const appList = ref<any[]>([])
const apiTemplates = ref<CodeTemplate[]>([])
const skills = ref<CodeSkill[]>([])
const selectedTemplateKey = ref<string | null>(null)
const selectedCodeGenType = ref<string | null>(null)
const routingRecommendation = ref<RoutingRecommendation | null>(null)
const showRoutingModal = ref(false)
const routingLoading = ref(false)

const scenarioTemplates = [
  { name: '电商展示与下单', label: '电商展示', icon: ShoppingOutlined },
  { name: '内容博客与分类归档', label: '内容博客', icon: ReadOutlined },
  { name: '团队任务看板', label: '任务看板', icon: CheckSquareOutlined },
  { name: '客服对话页面', label: '客服对话', icon: MessageOutlined },
]

const fallbackModes = [
  { codeGenType: 'html', label: '单页 HTML', desc: '轻量页面' },
  { codeGenType: 'multi_file', label: '多文件项目', desc: '结构更清楚' },
  { codeGenType: 'vue_project', label: 'Vue 工程', desc: '适合迭代' },
  { codeGenType: 'fullstack', label: '全栈应用', desc: '含后端能力' },
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

  // 如果用户已明确选择了技能或模板，直接创建
  if (selectedCodeGenType.value || selectedTemplateKey.value) {
    await doCreateApp(selectedCodeGenType.value || undefined)
    return
  }

  // 否则，先获取 AI 路由推荐
  routingLoading.value = true
  try {
    const res: any = await getRoutingRecommendation({ initPrompt: prompt.value })
    const rec = res.data as RoutingRecommendation
    if (rec.fullstack) {
      // 推荐全栈，显示确认卡片
      routingRecommendation.value = rec
      showRoutingModal.value = true
    } else {
      // 推荐前端，直接创建
      await doCreateApp(rec.recommendedType)
    }
  } catch (e) {
    // 路由失败，降级为直接创建
    await doCreateApp()
  } finally { routingLoading.value = false }
}

const doCreateApp = async (codeGenType?: string) => {
  generating.value = true
  try {
    const params: any = { initPrompt: prompt.value }
    if (selectedTemplateKey.value) params.templateKey = selectedTemplateKey.value
    if (codeGenType) params.codeGenType = codeGenType
    const res = await createApp(params)
    message.success('应用资产已创建')
    router.push(`/app/chat/${res.data}?autoGenerate=1`)
  } catch (e) { message.error('创建失败，请重试') }
  finally { generating.value = false; showRoutingModal.value = false }
}

const confirmFullstack = () => {
  showRoutingModal.value = false
  doCreateApp(routingRecommendation.value?.recommendedType)
}

const chooseFrontend = () => {
  showRoutingModal.value = false
  doCreateApp(routingRecommendation.value?.alternativeType || 'vue_project')
}

const useAutoMode = () => {
  selectedCodeGenType.value = null
  selectedTemplateKey.value = null
}
const useTemplate = (t: string) => { prompt.value = `请帮我生成一个${t}应用`; selectedTemplateKey.value = null }
const useApiTemplate = (key: string) => {
  selectedTemplateKey.value = key
  selectedCodeGenType.value = null
  const tpl = apiTemplates.value.find(t => t.templateKey === key)
  if (tpl) { prompt.value = `请帮我生成一个${tpl.templateName}` }
}
const useSkill = (skill: CodeSkill) => {
  prompt.value = prompt.value.trim() || `请帮我生成一个${skill.name}`
  selectedCodeGenType.value = skill.codeGenType
  selectedTemplateKey.value = null
}
const useFallbackMode = (codeGenType: string) => {
  selectedCodeGenType.value = codeGenType
  selectedTemplateKey.value = null
}
const formatSkillCost = (skill: CodeSkill) => skill.pointCost ? `${skill.pointCost} 积分` : '生成模式'
const goToApp = (id: string) => router.push(`/app/chat/${id}`)
const handlePageChange = (page: number) => { currentPage.value = page; fetchAppList() }

const fetchTemplates = async () => {
  try { const res: any = await listTemplates(); apiTemplates.value = res.data || [] }
  catch (e) {}
}

const fetchSkills = async () => {
  try { const res: any = await listSkills(); skills.value = res.data || [] }
  catch (e) {}
}

onMounted(() => { fetchAppList(); fetchTemplates(); fetchSkills() })
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

.choice-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--border-light);
}

.choice-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.choice-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}

.choice-head span {
  color: var(--t-primary);
  font-size: 13px;
  font-weight: 820;
}

.choice-head small {
  color: var(--t-light);
  font-size: 11px;
  font-weight: 650;
}

.choice-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.choice-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: background var(--t-fast), border-color var(--t-fast), color var(--t-fast), box-shadow var(--t-fast), transform var(--t-fast);
}

.choice-btn:hover {
  border-color: var(--c-primary-200);
  color: var(--c-primary);
  background: var(--c-primary-50);
  transform: translateY(-1px);
}

.choice-btn.active {
  border-color: var(--c-primary);
  color: var(--c-primary);
  background: var(--c-primary-50);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--c-primary) 18%, transparent);
}

.choice-btn.scenario {
  padding: 7px 12px;
  border-radius: var(--r-full);
  background: transparent;
  font-size: 12px;
  font-weight: 680;
  color: var(--t-secondary);
}

.choice-btn.scenario.active {
  color: var(--c-primary);
  background: var(--c-primary-50);
}

.choice-btn.mode {
  min-width: 112px;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  padding: 10px 12px;
  border-radius: var(--r-lg);
  background: var(--bg-soft);
  color: var(--t-secondary);
}

.choice-btn.mode span {
  font-size: 12px;
  font-weight: 820;
}

.choice-btn.mode small {
  color: var(--t-light);
  font-size: 11px;
  font-weight: 650;
}

.choice-btn.mode.active small {
  color: color-mix(in srgb, var(--c-primary) 72%, var(--t-muted));
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
  .choice-head { align-items: flex-start; flex-direction: column; gap: 3px; }
  .choice-btn.mode { flex: 1 1 calc(50% - 4px); min-width: 0; }
  .section-head { align-items: flex-start; flex-direction: column; }
  .workflow-list { grid-template-columns: 1fr; }
  .workflow-item { min-height: auto; border-right: none; border-bottom: 1px solid var(--border-light); }
  .workflow-item:last-child { border-bottom: none; }
  .workflow-index { margin-bottom: 18px; }
  .app-grid { grid-template-columns: 1fr; }
}

.routing-confirm { text-align: center; padding: 12px 0; }
.routing-icon { font-size: 40px; margin-bottom: 12px; }
.routing-title { margin: 0 0 8px; font-size: 18px; font-weight: 800; color: var(--t-primary); }
.routing-reason { margin: 0 0 20px; font-size: 13px; color: var(--t-muted); line-height: 1.6; }
.routing-options { display: flex; flex-direction: column; gap: 10px; }
.routing-btn {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
  padding: 16px;
  border-radius: var(--r-lg);
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: all var(--t-fast);
  background: var(--bg-card);
  text-align: left;
}
.routing-badge {
  display: inline-flex;
  padding: 3px 8px;
  border-radius: var(--r-full);
  background: var(--c-primary);
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}
.routing-badge.muted {
  background: var(--bg-soft);
  color: var(--t-muted);
}
.routing-btn strong { font-size: 15px; font-weight: 850; color: var(--t-primary); }
.routing-btn span:last-child { font-size: 12px; color: var(--t-muted); line-height: 1.55; }
.routing-btn.primary {
  border-color: var(--c-primary);
  background: linear-gradient(135deg, var(--c-primary-50), var(--bg-card));
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--c-primary) 14%, transparent);
}
.routing-btn.primary strong { color: var(--c-primary); }
.routing-btn.primary:hover { background: var(--c-primary-100); transform: translateY(-1px); }
.routing-btn.secondary:hover { border-color: var(--c-primary-200); background: var(--c-primary-50); transform: translateY(-1px); }
</style>