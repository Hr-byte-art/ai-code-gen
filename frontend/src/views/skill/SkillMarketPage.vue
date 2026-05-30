<template>
  <div class="skill-market">
    <PageHeader title="Skill 市场" description="浏览社区共享的代码生成技能，一键安装到你的账户。" eyebrow="市场" />

    <div class="skill-filters">
      <a-segmented v-model:value="activeFilter" :options="filterOptions" />
      <a-input-search v-model:value="searchText" placeholder="搜索技能名称..." style="width:240px" allow-clear />
    </div>

    <a-spin :spinning="loading">
      <div class="skill-grid">
        <div v-for="skill in filteredSkills" :key="skill.skillKey" class="skill-card">
          <div class="skill-header">
            <div class="skill-icon">
              <component :is="getIcon(skill.codeGenType)" />
            </div>
            <a-tag v-if="isInstalled(skill.id)" color="green" size="small">已安装</a-tag>
          </div>
          <h3 class="skill-name">{{ skill.name }}</h3>
          <p class="skill-desc">{{ skill.description }}</p>
          <div class="skill-rating" v-if="skill.ratingAvg">
            <span class="stars">{{ '★'.repeat(Math.round(skill.ratingAvg)) }}{{ '☆'.repeat(5 - Math.round(skill.ratingAvg)) }}</span>
            <span class="rating-text">{{ skill.ratingAvg }} ({{ skill.ratingCount }})</span>
          </div>
          <div class="skill-meta">
            <a-tag :color="getStrategyColor(skill.buildStrategy)">{{ skill.buildStrategy }}</a-tag>
            <span class="skill-cost">{{ skill.pointCost }} 积分</span>
            <span class="skill-uses" v-if="skill.useCount">{{ skill.useCount }} 次使用</span>
          </div>
          <div class="skill-actions">
            <a-button v-if="!isInstalled(skill.id)" type="primary" size="small" @click="handleInstall(skill)">
              安装
            </a-button>
            <a-button v-else size="small" @click="handleUninstall(skill)">
              卸载
            </a-button>
            <a-button size="small" @click="openRateModal(skill)">
              评分
            </a-button>
          </div>
        </div>
      </div>
      <a-empty v-if="!loading && filteredSkills.length === 0" description="暂无公开技能" />
    </a-spin>

    <!-- 评分弹窗 -->
    <a-modal v-model:open="rateModalVisible" title="为技能评分" @ok="submitRating" okText="提交">
      <div style="text-align:center;padding:16px 0">
        <a-rate v-model:value="rateScore" :count="5" />
        <div style="margin-top:12px;color:var(--t-muted)">{{ rateScore }} 分</div>
      </div>
      <a-textarea v-model:value="rateComment" placeholder="可选：留下你的评价" :rows="3" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  Html5Outlined, CodeOutlined, CloudOutlined,
  ApiOutlined, RocketOutlined, AppstoreOutlined
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import request from '@/utils/request'
import type { CodeSkill } from '@/types'

const loading = ref(false)
const skills = ref<CodeSkill[]>([])
const installedIds = ref<Set<string>>(new Set())
const activeFilter = ref('all')
const searchText = ref('')

const rateModalVisible = ref(false)
const rateScore = ref(5)
const rateComment = ref('')
const ratingSkillId = ref<string>('')

const filterOptions = [
  { label: '全部', value: 'all' },
  { label: '前端', value: 'frontend' },
  { label: '全栈', value: 'fullstack' },
  { label: '其他', value: 'other' },
]

const filteredSkills = computed(() => {
  let result = skills.value
  if (activeFilter.value === 'frontend') {
    result = result.filter(s => ['none', 'vue', 'react', 'nextjs', 'landing_page'].includes(s.buildStrategy))
  } else if (activeFilter.value === 'fullstack') {
    result = result.filter(s => s.buildStrategy === 'fullstack')
  } else if (activeFilter.value === 'other') {
    result = result.filter(s => !['none', 'vue', 'react', 'nextjs', 'landing_page', 'fullstack'].includes(s.buildStrategy))
  }
  if (searchText.value) {
    const q = searchText.value.toLowerCase()
    result = result.filter(s => s.name.toLowerCase().includes(q) || s.description?.toLowerCase().includes(q))
  }
  return result
})

const isInstalled = (id: string) => installedIds.value.has(id)

const getIcon = (codeGenType: string) => {
  const map: Record<string, any> = {
    html: Html5Outlined, multi_file: CodeOutlined,
    vue_project: CodeOutlined, fullstack: CloudOutlined,
    react_ts: CodeOutlined, nextjs: ApiOutlined,
    landing_page: RocketOutlined, admin_dashboard: AppstoreOutlined,
  }
  return map[codeGenType] || CodeOutlined
}

const getStrategyColor = (strategy: string) => {
  const map: Record<string, string> = {
    none: 'default', vue: 'green', fullstack: 'purple',
    react: 'blue', nextjs: 'cyan', landing_page: 'orange',
  }
  return map[strategy] || 'default'
}

const fetchSkills = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/skill/market/list')
    skills.value = res.data || []
  } catch (e) {} finally { loading.value = false }
}

const fetchInstalled = async () => {
  try {
    const res: any = await request.get('/skill/market/installed')
    const list = res.data || []
    installedIds.value = new Set(list.map((s: CodeSkill) => s.id))
  } catch (e) { /* 未登录时忽略 */ }
}

const handleInstall = async (skill: CodeSkill) => {
  try {
    await request.post('/skill/market/install', null, { params: { skillId: skill.id } })
    installedIds.value.add(skill.id)
    message.success(`已安装「${skill.name}」`)
  } catch (e: any) {
    message.error(e?.response?.data?.message || '安装失败')
  }
}

const handleUninstall = async (skill: CodeSkill) => {
  try {
    await request.post('/skill/market/uninstall', null, { params: { skillId: skill.id } })
    installedIds.value.delete(skill.id)
    message.success(`已卸载「${skill.name}」`)
  } catch (e: any) {
    message.error(e?.response?.data?.message || '卸载失败')
  }
}

const openRateModal = (skill: CodeSkill) => {
  ratingSkillId.value = skill.id
  rateScore.value = 5
  rateComment.value = ''
  rateModalVisible.value = true
}

const submitRating = async () => {
  try {
    await request.post('/skill/market/rate', {
      skillId: ratingSkillId.value,
      score: rateScore.value,
      comment: rateComment.value || null,
    })
    message.success('评分成功')
    rateModalVisible.value = false
    fetchSkills() // 刷新评分
  } catch (e: any) {
    message.error(e?.response?.data?.message || '评分失败')
  }
}

onMounted(() => {
  fetchSkills()
  fetchInstalled()
})
</script>

<style scoped>
.skill-market { padding: 30px 24px 48px; max-width: 1280px; margin: 0 auto; }
.skill-filters { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.skill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.skill-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--r-xl);
  padding: 20px;
  transition: all 0.2s;
}
.skill-card:hover {
  border-color: var(--c-primary);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.skill-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.skill-icon { font-size: 24px; color: var(--c-primary); }
.skill-name { font-size: 16px; font-weight: 700; margin: 0 0 8px; }
.skill-desc { font-size: 13px; color: var(--t-muted); margin: 0 0 8px; line-height: 1.5; min-height: 40px; }
.skill-rating { margin-bottom: 8px; }
.stars { color: #f5a623; font-size: 14px; }
.rating-text { font-size: 12px; color: var(--t-muted); margin-left: 6px; }
.skill-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.skill-cost, .skill-uses { font-size: 12px; color: var(--t-muted); }
.skill-actions { display: flex; gap: 8px; }
</style>
