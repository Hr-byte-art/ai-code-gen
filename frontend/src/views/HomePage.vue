<template>
  <div class="home-page">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-gradient"></div>
      <div class="bg-grid"></div>
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
      <div class="bg-orb bg-orb-3"></div>
    </div>

    <div class="container">
      <!-- Hero 区域 -->
      <section class="hero-section">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          AI 驱动 · 一键生成
        </div>
        <h1 class="hero-title">熵擎 AI 应用生成平台</h1>
        <p class="hero-slogan">用自然语言描述你的想法，AI 为你构建完整应用</p>
        <p class="hero-description">
          支持 HTML、多文件项目、Vue 工程等多种生成模式
        </p>
      </section>

      <!-- 输入区域 -->
      <section class="input-section">
        <div class="input-card">
          <a-textarea
            v-model:value="prompt"
            :rows="4"
            placeholder="描述你想要的应用，例如：一个在线商城、一个博客系统、一个任务管理工具..."
            class="prompt-input"
            @keydown.enter.ctrl="handleGenerate"
          />
          <div class="input-footer">
            <a-tooltip title="按 Ctrl+Enter 生成">
              <span class="shortcut-hint">
                <InfoCircleOutlined />
                Ctrl + Enter
              </span>
            </a-tooltip>
            <a-button
              type="primary"
              :loading="generating"
              @click="handleGenerate"
              class="generate-btn"
            >
              <RocketOutlined />
              生成应用
            </a-button>
          </div>
        </div>
      </section>

      <!-- 快捷操作 -->
      <section class="quick-actions">
        <button
          v-for="tpl in templates"
          :key="tpl.name"
          class="template-btn"
          @click="useTemplate(tpl.name)"
        >
          <span class="template-icon">{{ tpl.icon }}</span>
          <span class="template-label">{{ tpl.label }}</span>
        </button>
      </section>

      <!-- 精选应用 -->
      <section class="section">
        <div class="section-header">
          <h2 class="section-title">精选应用</h2>
          <span class="section-subtitle">探索社区创建的优秀应用</span>
        </div>
        <a-spin :spinning="loading">
          <div class="featured-grid">
            <AppCard
              v-for="app in appList"
              :key="app.id"
              :app="app"
              @click="goToApp(app.id)"
            />
          </div>
          <a-empty v-if="!loading && appList.length === 0" description="暂无精选应用" />
        </a-spin>
        <div class="pagination-wrapper" v-if="total > pageSize">
          <a-pagination
            v-model:current="currentPage"
            :total="total"
            :page-size="pageSize"
            @change="handlePageChange"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  RocketOutlined,
  InfoCircleOutlined,
  ShoppingOutlined,
  ReadOutlined,
  CheckSquareOutlined,
  MessageOutlined
} from '@ant-design/icons-vue'
import AppCard from '@/components/AppCard.vue'
import { getGoodAppList, createApp } from '@/api/app'

const router = useRouter()

// 输入状态
const prompt = ref('')
const generating = ref(false)
const loading = ref(false)

// 分页状态
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 应用列表
const appList = ref<any[]>([])

// 模板
const templates = [
  { name: '商城', label: '商城应用', icon: '🛒' },
  { name: '博客', label: '博客系统', icon: '📝' },
  { name: '任务', label: '任务管理', icon: '✅' },
  { name: '聊天', label: '聊天应用', icon: '💬' }
]

// 获取精选应用列表
const fetchAppList = async () => {
  loading.value = true
  try {
    const res: any = await getGoodAppList({
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    appList.value = res.data?.records || []
    total.value = res.data?.totalRow || 0
  } catch (error) {
    console.error('获取应用列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 生成应用
const handleGenerate = async () => {
  if (!prompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  generating.value = true
  try {
    const res = await createApp({ initPrompt: prompt.value })
    message.success('应用创建成功！')
    router.push(`/app/edit/${res.data}`)
  } catch (error) {
    message.error('创建失败，请重试')
  } finally {
    generating.value = false
  }
}

// 使用模板
const useTemplate = (template: string) => {
  prompt.value = `请帮我生成一个${template}应用`
}

// 跳转应用
const goToApp = (id: number) => {
  router.push(`/app/chat/${id}`)
}

// 分页变化
const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchAppList()
}

onMounted(() => {
  fetchAppList()
})
</script>

<style scoped>
.home-page {
  position: relative;
  min-height: 100vh;
}

.bg-decoration {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.bg-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100vh;
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.04) 0%, rgba(139, 92, 246, 0.02) 50%, transparent 100%);
}

.bg-grid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    linear-gradient(rgba(59, 130, 246, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.025) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 80% 60% at 50% 20%, black 30%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse 80% 60% at 50% 20%, black 30%, transparent 70%);
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}

.bg-orb-1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.15), transparent 70%);
  top: -10%;
  right: -5%;
  animation: float1 20s ease-in-out infinite;
}

.bg-orb-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.12), transparent 70%);
  top: 15%;
  left: -8%;
  animation: float2 25s ease-in-out infinite;
}

.bg-orb-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.1), transparent 70%);
  bottom: 20%;
  right: 10%;
  animation: float3 18s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-30px, 20px) scale(1.05); }
  66% { transform: translate(20px, -15px) scale(0.95); }
}

@keyframes float2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(25px, -20px) scale(1.08); }
  66% { transform: translate(-15px, 25px) scale(0.92); }
}

@keyframes float3 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-20px, -30px); }
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
  position: relative;
  z-index: 2;
  width: 100%;
  box-sizing: border-box;
}

/* Hero Section */
.hero-section {
  text-align: center;
  padding: 72px 0 48px;
  position: relative;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #3b82f6;
  margin-bottom: 24px;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3b82f6;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.hero-title {
  font-size: 52px;
  font-weight: 800;
  margin: 0 0 16px;
  line-height: 1.15;
  background: linear-gradient(135deg, #1e293b 0%, #3b82f6 50%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -1.5px;
  animation: fadeInUp 0.8s ease-out;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.hero-slogan {
  font-size: 20px;
  margin: 0 0 8px;
  color: #334155;
  font-weight: 500;
  animation: fadeInUp 0.8s ease-out 0.1s both;
}

.hero-description {
  font-size: 15px;
  margin: 0;
  color: #64748b;
  animation: fadeInUp 0.8s ease-out 0.2s both;
}

/* Input Section */
.input-section {
  max-width: 720px;
  margin: 0 auto 32px;
  animation: fadeInUp 0.8s ease-out 0.3s both;
}

.input-card {
  background: rgba(255, 255, 255, 0.9);
  -webkit-backdrop-filter: blur(20px);
  backdrop-filter: blur(20px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  transition: all var(--transition-base);
}

.input-card:focus-within {
  border-color: rgba(59, 130, 246, 0.3);
  box-shadow: var(--shadow-lg), 0 0 0 4px rgba(59, 130, 246, 0.06);
}

.prompt-input {
  border: none !important;
  box-shadow: none !important;
  border-radius: 0 !important;
  font-size: 15px;
  padding: 20px 24px 8px;
  background: transparent !important;
  resize: none;
}

.prompt-input:focus {
  box-shadow: none !important;
  border: none !important;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px 12px;
}

.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
  padding: 4px 10px;
  border-radius: 6px;
  background: #f8fafc;
}

.generate-btn {
  border-radius: 10px !important;
  padding: 6px 24px !important;
  height: auto !important;
  font-weight: 600 !important;
}

/* Quick Actions */
.quick-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-bottom: 64px;
  flex-wrap: wrap;
  animation: fadeInUp 0.8s ease-out 0.4s both;
}

.template-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.85);
  -webkit-backdrop-filter: blur(12px);
  backdrop-filter: blur(12px);
  border: 1px solid #e2e8f0;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  transition: all var(--transition-base);
}

.template-btn:hover {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-primary);
}

.template-icon {
  font-size: 16px;
  line-height: 1;
}

.template-btn:hover .template-icon {
  filter: brightness(0) invert(1);
}

/* Section */
.section {
  margin-bottom: 64px;
}

.section-header {
  margin-bottom: 32px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.3px;
}

.section-subtitle {
  font-size: 14px;
  color: var(--text-muted);
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-section {
    padding: 48px 0 32px;
  }
  .hero-title {
    font-size: 32px;
    letter-spacing: -0.5px;
  }
  .hero-slogan {
    font-size: 16px;
  }
  .hero-description {
    font-size: 14px;
  }
  .featured-grid {
    grid-template-columns: 1fr;
  }
  .quick-actions {
    gap: 8px;
  }
  .template-btn {
    padding: 8px 14px;
    font-size: 13px;
  }
}
</style>
