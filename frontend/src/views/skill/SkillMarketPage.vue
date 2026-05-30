<template>
  <div class="skill-market">
    <PageHeader title="Skill 市场" description="浏览可用的代码生成技能，选择最适合的技术栈。" eyebrow="市场" />

    <div class="skill-filters">
      <a-segmented v-model:value="activeFilter" :options="filterOptions" />
    </div>

    <a-spin :spinning="loading">
      <div class="skill-grid">
        <div v-for="skill in filteredSkills" :key="skill.skillKey" class="skill-card" @click="useSkill(skill)">
          <div class="skill-icon">
            <component :is="getIcon(skill.codeGenType)" />
          </div>
          <h3 class="skill-name">{{ skill.name }}</h3>
          <p class="skill-desc">{{ skill.description }}</p>
          <div class="skill-meta">
            <a-tag :color="getStrategyColor(skill.buildStrategy)">{{ skill.buildStrategy }}</a-tag>
            <span class="skill-cost">{{ skill.pointCost }} 积分</span>
          </div>
          <div class="skill-badges">
            <a-tag v-if="skill.customTools" color="cyan" size="small">自定义工具</a-tag>
            <a-tag v-if="skill.hooks" color="orange" size="small">钩子</a-tag>
          </div>
        </div>
      </div>
      <a-empty v-if="!loading && filteredSkills.length === 0" description="暂无可用技能" />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Html5Outlined, CodeOutlined, CloudOutlined,
  ApiOutlined, RocketOutlined, AppstoreOutlined
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { listSkills } from '@/api/skill'
import type { CodeSkill } from '@/types'

const router = useRouter()
const loading = ref(false)
const skills = ref<CodeSkill[]>([])
const activeFilter = ref('all')

const filterOptions = [
  { label: '全部', value: 'all' },
  { label: '前端', value: 'frontend' },
  { label: '全栈', value: 'fullstack' },
  { label: '其他', value: 'other' },
]

const filteredSkills = computed(() => {
  if (activeFilter.value === 'all') return skills.value
  if (activeFilter.value === 'frontend') {
    return skills.value.filter(s => ['none', 'vue', 'react', 'nextjs', 'landing_page'].includes(s.buildStrategy))
  }
  if (activeFilter.value === 'fullstack') {
    return skills.value.filter(s => s.buildStrategy === 'fullstack')
  }
  return skills.value.filter(s => !['none', 'vue', 'react', 'nextjs', 'landing_page', 'fullstack'].includes(s.buildStrategy))
})

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

const useSkill = (skill: CodeSkill) => {
  router.push({ path: '/', query: { skill: skill.skillKey } })
}

onMounted(async () => {
  loading.value = true
  try {
    const r: any = await listSkills()
    skills.value = (r.data || []).filter((s: CodeSkill) => s.isActive)
  } catch (e) {} finally { loading.value = false }
})
</script>

<style scoped>
.skill-market { padding: 30px 24px 48px; max-width: 1280px; margin: 0 auto; }
.skill-filters { margin-bottom: 24px; }
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
  cursor: pointer;
  transition: all 0.2s;
}
.skill-card:hover {
  border-color: var(--c-primary);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}
.skill-icon { font-size: 28px; color: var(--c-primary); margin-bottom: 12px; }
.skill-name { font-size: 16px; font-weight: 700; margin: 0 0 8px; }
.skill-desc { font-size: 13px; color: var(--t-muted); margin: 0 0 12px; line-height: 1.5; min-height: 40px; }
.skill-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.skill-cost { font-size: 12px; color: var(--t-muted); }
.skill-badges { display: flex; gap: 4px; }
</style>
