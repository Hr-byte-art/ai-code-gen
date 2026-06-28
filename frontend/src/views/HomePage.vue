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
                <button v-for="t in scenarioTemplates" :key="t.key" class="choice-btn scenario" @click="useTemplate(t.key)">
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
                <template v-if="homepageSkills.length > 0">
                  <button
                    v-for="s in homepageSkills"
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
                    v-for="m in selectableFallbackModes"
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

            <div class="choice-group">
              <div class="choice-head">
                <span>设计风格</span>
                <small>可选，模仿知名网站的视觉风格</small>
              </div>
              <div class="choice-list">
                <button class="choice-btn mode" :class="{ active: !selectedDesignKey }" @click="selectedDesignKey = null">
                  <span>默认风格</span>
                  <small>AI 自由发挥</small>
                </button>
                <button
                  v-for="d in designTemplates.filter(item => normalizeDesignKey(item) !== 'default').slice(0, 8)"
                  :key="d.key"
                  class="choice-btn mode"
                  :class="{ active: selectedDesignKey === d.key }"
                  @click="selectedDesignKey = d.key"
                >
                  <span>{{ formatDesignName(d) }}</span>
                  <small>{{ formatDesignDesc(d) }}</small>
                </button>
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
          <button class="routing-btn primary" :disabled="!userStore.canUsePremiumGeneration" @click="confirmFullstack">
            <span class="routing-badge">推荐方案</span>
            <strong>生成全栈应用</strong>
            <span v-if="userStore.canUsePremiumGeneration">包含后端 API、数据能力和前端页面 · {{ routingRecommendation?.pointCost }} 积分</span>
            <span v-else>需要 VIP 权限，升级后可使用全栈生成</span>
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
            <AppCard
              v-for="app in appList"
              :key="app.id"
              :app="app"
              action-mode="previewOnly"
              @click="goToPreview(app)"
              @preview="goToPreview(app)"
            />
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
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  RocketOutlined, ShoppingOutlined, ReadOutlined, CheckSquareOutlined, MessageOutlined
} from '@ant-design/icons-vue'
import AppCard from '@/components/AppCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import workflowPreview from '@/assets/workflow-preview.webp'
import { getGoodAppList, createApp, getRoutingRecommendation, getDeployedAppUrl } from '@/api/app'
import { listTemplates } from '@/api/template'
import { listSkills } from '@/api/skill'
import { getDesignTemplateList } from '@/api/design'
import { useUserStore } from '@/stores/user'
import type { CodeTemplate, CodeSkill, RoutingRecommendation, DesignTemplateInfo } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const prompt = ref('')
const generating = ref(false)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(0)
const appList = ref<any[]>([])
const apiTemplates = ref<CodeTemplate[]>([])
const skills = ref<CodeSkill[]>([])
const designTemplates = ref<DesignTemplateInfo[]>([])
const selectedTemplateKey = ref<string | null>(null)
const selectedCodeGenType = ref<string | null>(null)
const selectedDesignKey = ref<string | null>(null)
const routingRecommendation = ref<RoutingRecommendation | null>(null)
const showRoutingModal = ref(false)
const routingLoading = ref(false)

const HOMEPAGE_SKILL_TYPES = ['html', 'vue_project', 'fullstack']
const requiresPremiumGeneration = (skill?: Pick<CodeSkill, 'buildStrategy'> | null) =>
  Boolean(skill && skill.buildStrategy !== 'none')
const homepageSkills = computed(() => {
  const skillMap = new Map(skills.value.map(skill => [skill.codeGenType, skill]))
  return HOMEPAGE_SKILL_TYPES
    .map(codeGenType => skillMap.get(codeGenType))
    .filter((skill): skill is CodeSkill => Boolean(skill))
    .filter(skill => !requiresPremiumGeneration(skill) || userStore.canUsePremiumGeneration)
})
const selectableFallbackModes = computed(() => fallbackModes
  .filter(mode => !['vue_project', 'fullstack'].includes(mode.codeGenType) || userStore.canUsePremiumGeneration))

const DESIGN_STYLE_COPY: Record<string, { name: string; desc: string }> = {
  default: { name: '默认风格', desc: 'AI 自由发挥' },
  airbnb: { name: '民宿生活风', desc: '温暖卡片、图片友好' },
  airtable: { name: '协作工具风', desc: '清爽表格、效率产品' },
  apple: { name: '苹果极简风', desc: '大留白、高级产品感' },
  binance: { name: '金融科技风', desc: '高对比、数据平台感' },
  bmw: { name: '豪华汽车风', desc: '强视觉、黑白高级感' },
  'bmw-m': { name: '性能运动风', desc: '速度感、强冲击力' },
  bugatti: { name: '超跑奢华风', desc: '强对比、顶级质感' },
  cal: { name: '日程预约风', desc: '简洁排期、效率工具' },
  claude: { name: 'AI 助手风', desc: '克制排版、对话友好' },
  clay: { name: '增长工具风', desc: '数据卡片、销售线索' },
  clickhouse: { name: '数据分析风', desc: '高性能、技术产品感' },
  cohere: { name: 'AI 平台风', desc: '科技渐变、模型服务' },
  coinbase: { name: '加密金融风', desc: '蓝白清爽、可信交易' },
  composio: { name: '集成平台风', desc: '工具连接、自动化感' },
  cursor: { name: '开发工具风', desc: '深色界面、工程质感' },
  elevenlabs: { name: '语音 AI 风', desc: '音频波形、未来感' },
  expo: { name: '开发平台风', desc: '文档清晰、产品工具' },
  ferrari: { name: '豪华性能风', desc: '红黑冲击、速度感' },
  figma: { name: '设计协作风', desc: '彩色模块、创作工具' },
  framer: { name: '动效建站风', desc: '现代视觉、作品展示' },
  hashicorp: { name: '基础设施风', desc: '企业级、云原生工具' },
  ibm: { name: '企业科技风', desc: '理性网格、专业可信' },
  intercom: { name: '客服产品风', desc: '消息沟通、服务台感' },
  kraken: { name: '交易平台风', desc: '深色金融、数据密集' },
  lamborghini: { name: '超跑运动风', desc: '锐利线条、强冲击' },
  linear: { name: '项目管理风', desc: '极简深色、效率产品' },
  notion: { name: '知识库风', desc: '文档卡片、轻量协作' },
  shopify: { name: '电商平台风', desc: '电商展示、转化友好' },
  stripe: { name: '支付科技风', desc: '渐变科技、商业转化' },
  vercel: { name: '云部署风', desc: '黑白极简、开发者产品' },
}

const normalizeDesignKey = (template: DesignTemplateInfo) => {
  const raw = `${template.key || template.name || ''}`.toLowerCase()
  const compact = raw.replace(/[^a-z0-9]/g, '')
  if (raw.includes('bmw-m') || raw.includes('bmw m')) return 'bmw-m'
  if (raw.includes('airbnb')) return 'airbnb'
  if (raw.includes('airtable')) return 'airtable'
  if (raw.includes('apple')) return 'apple'
  if (raw.includes('binance')) return 'binance'
  if (raw === 'bmw' || raw.includes('bmw version')) return 'bmw'
  if (raw.includes('bugatti')) return 'bugatti'
  if (compact.includes('calcom') || raw === 'cal' || raw.includes('cal ')) return 'cal'
  if (raw.includes('linear')) return 'linear'
  if (raw.includes('default')) return 'default'
  return raw.replace(/[^a-z0-9-]/g, '')
}

const cleanDesignName = (name: string) => {
  const raw = (name || '').replace(/version\s*:\s*\w+/gi, '').replace(/name\s*:/gi, '').trim()
  return raw || '自定义风格'
}

const formatDesignName = (template: DesignTemplateInfo) => {
  const copy = DESIGN_STYLE_COPY[normalizeDesignKey(template)]
  return copy?.name || cleanDesignName(template.name)
}

const formatDesignDesc = (template: DesignTemplateInfo) => {
  const copy = DESIGN_STYLE_COPY[normalizeDesignKey(template)]
  if (copy?.desc) return copy.desc
  const cleanDesc = (template.description || '')
    .replace(/version\s*:\s*\w+/gi, '')
    .replace(/name\s*:/gi, '')
    .replace(/\s+/g, ' ')
    .trim()
  return cleanDesc ? cleanDesc.slice(0, 14) : '视觉参考风格'
}

const scenarioPrompts: Record<string, string> = {
  ecommerce: '请帮我生成一个电商商品展示与下单页面应用。面向需要展示商品和引导购买的商家，页面需要包含首页横幅、商品分类、商品卡片、价格与库存信息、商品详情展示、购物车入口、下单流程引导和售后/联系方式区域。整体风格要清爽、有商业质感，重点突出商品卖点、购买路径和转化效率。',
  blog: '请帮我生成一个内容博客与分类归档页面应用。面向个人创作者或内容团队，页面需要包含文章列表、分类筛选、标签、精选文章、文章详情、作者信息、阅读量/发布时间展示和搜索入口。整体风格要适合长期内容沉淀，阅读体验清晰，结构层级明确。',
  taskBoard: '请帮我生成一个团队任务看板页面应用。面向项目协作团队，页面需要包含待办、进行中、已完成等任务分组，任务卡片需要展示标题、优先级、负责人、截止时间和进度状态，并提供新增任务、筛选、搜索和状态切换的交互。整体风格要偏效率工具，信息密度适中，操作路径清楚。',
  customerService: '请帮我生成一个客服对话页面应用。面向在线客服和用户沟通场景，页面需要包含会话列表、当前聊天窗口、消息气泡、用户资料卡、快捷回复、问题分类、工单状态、输入框和发送按钮。需要区分客服与用户消息，支持展示未读提醒、在线状态和常见问题入口。整体风格要专业、稳定、适合客服工作台使用。',
}

const scenarioTemplates = [
  { key: 'ecommerce', label: '电商展示', icon: ShoppingOutlined },
  { key: 'blog', label: '内容博客', icon: ReadOutlined },
  { key: 'taskBoard', label: '任务看板', icon: CheckSquareOutlined },
  { key: 'customerService', label: '客服对话', icon: MessageOutlined },
]

const resolveScenarioPrompt = (templateName = '', templateKey = '') => {
  const raw = `${templateKey} ${templateName}`.toLowerCase()
  if (raw.includes('ecommerce') || raw.includes('shop') || raw.includes('电商')) return scenarioPrompts.ecommerce
  if (raw.includes('blog') || raw.includes('content') || raw.includes('博客') || raw.includes('内容')) return scenarioPrompts.blog
  if (raw.includes('task') || raw.includes('todo') || raw.includes('board') || raw.includes('任务') || raw.includes('看板')) return scenarioPrompts.taskBoard
  if (raw.includes('chat') || raw.includes('customer') || raw.includes('service') || raw.includes('客服') || raw.includes('对话')) return scenarioPrompts.customerService
  return `请帮我生成一个${templateName || '业务'}页面应用。请补充清晰的信息架构、核心模块、主要交互、视觉风格和适用场景，让页面具备真实可用的产品完成度。`
}

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
      // 推荐前端，普通用户不能使用 Vue 工程时降级为多文件项目
      const recommendedType = isPremiumCodeGenType(rec.recommendedType) && !userStore.canUsePremiumGeneration
        ? 'multi_file'
        : rec.recommendedType
      await doCreateApp(recommendedType)
    }
  } catch (e) {
    // 路由失败，降级为直接创建
    await doCreateApp()
  } finally { routingLoading.value = false }
}

const isPremiumCodeGenType = (codeGenType?: string | null) => ['vue_project', 'fullstack'].includes(codeGenType || '')

const doCreateApp = async (codeGenType?: string) => {
  if (isPremiumCodeGenType(codeGenType) && !userStore.canUsePremiumGeneration) {
    message.warning('Vue 项目和全栈应用生成需要 VIP 权限')
    return
  }
  generating.value = true
  try {
    const params: any = { initPrompt: prompt.value }
    if (selectedTemplateKey.value) params.templateKey = selectedTemplateKey.value
    if (codeGenType) params.codeGenType = codeGenType
    if (selectedDesignKey.value) params.designKey = selectedDesignKey.value
    const res = await createApp(params)
    message.success('应用资产已创建')
    router.push(`/app/chat/${res.data}?autoGenerate=1`)
  } catch (e: any) { message.error(e?.message || '创建失败，请重试') }
  finally { generating.value = false; showRoutingModal.value = false }
}

const confirmFullstack = () => {
  if (!userStore.canUsePremiumGeneration) {
    message.warning('全栈应用生成需要 VIP 权限')
    return
  }
  showRoutingModal.value = false
  doCreateApp(routingRecommendation.value?.recommendedType)
}

const chooseFrontend = () => {
  showRoutingModal.value = false
  const alternativeType = routingRecommendation.value?.alternativeType
  doCreateApp(userStore.canUsePremiumGeneration ? (alternativeType || 'vue_project') : 'multi_file')
}

const useAutoMode = () => {
  selectedCodeGenType.value = null
  selectedTemplateKey.value = null
}
const useTemplate = (key: string) => { prompt.value = resolveScenarioPrompt('', key); selectedTemplateKey.value = null }
const useApiTemplate = (key: string) => {
  selectedTemplateKey.value = key
  selectedCodeGenType.value = null
  const tpl = apiTemplates.value.find(t => t.templateKey === key)
  if (tpl) { prompt.value = resolveScenarioPrompt(tpl.templateName, tpl.templateKey) }
}
const useSkill = (skill: CodeSkill) => {
  selectedCodeGenType.value = skill.codeGenType
  selectedTemplateKey.value = null
}
const useFallbackMode = (codeGenType: string) => {
  selectedCodeGenType.value = codeGenType
  selectedTemplateKey.value = null
}
const formatSkillCost = (skill: CodeSkill) => skill.pointCost ? `${skill.pointCost} 积分` : '生成模式'
const goToPreview = async (app: any) => {
  if (!app?.deployedTime || !app?.deployKey) {
    message.warning('该应用还没有可访问的线上地址')
    return
  }
  const url = await getDeployedAppUrl(app.id)
  if (url) {
    window.open(url, '_blank')
  }
}
const handlePageChange = (page: number) => { currentPage.value = page; fetchAppList() }

const fetchTemplates = async () => {
  try { const res: any = await listTemplates(); apiTemplates.value = res.data || [] }
  catch (e) {}
}

const fetchSkills = async () => {
  try { const res: any = await listSkills(); skills.value = res.data || [] }
  catch (e) {}
}

const fetchDesignTemplates = async () => {
  try { const res: any = await getDesignTemplateList(); designTemplates.value = res.data || [] }
  catch (e) {}
}

onMounted(() => { fetchAppList(); fetchTemplates(); fetchSkills(); fetchDesignTemplates() })
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
.routing-btn:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}
.routing-btn:disabled:hover {
  transform: none;
  background: linear-gradient(135deg, var(--c-primary-50), var(--bg-card));
}
.routing-btn.secondary:hover { border-color: var(--c-primary-200); background: var(--c-primary-50); transform: translateY(-1px); }
</style>