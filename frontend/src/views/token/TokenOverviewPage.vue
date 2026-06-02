<template>
  <div class="page">
    <PageHeader title="资源账单" description="把每次生成和迭代消耗沉淀成可追踪的账单。" eyebrow="Token" />

    <a-spin :spinning="loading">
      <section class="ledger-summary">
        <div class="summary-primary">
          <span class="summary-label">累计消耗</span>
          <strong>{{ fmtNum(summary.totalTokens || 0) }}</strong>
          <span class="summary-note">Token</span>
        </div>
        <div class="summary-split">
          <div class="split-item">
            <span>输入</span>
            <strong>{{ fmtNum(summary.totalInputTokens || 0) }}</strong>
          </div>
          <div class="split-item">
            <span>输出</span>
            <strong>{{ fmtNum(summary.totalOutputTokens || 0) }}</strong>
          </div>
          <div class="split-item">
            <span>关联应用</span>
            <strong>{{ summary.appCount || 0 }}</strong>
          </div>
        </div>
      </section>
    </a-spin>

    <section class="quota-section" v-if="quota">
      <div class="quota-card">
        <div class="quota-header">
          <span class="quota-label">今日额度</span>
          <a-tag :color="quota.dailyGenUsed >= quota.dailyGenLimit ? 'red' : 'green'" size="small">
            {{ quota.dailyGenUsed >= quota.dailyGenLimit ? '已用完' : '正常' }}
          </a-tag>
        </div>
        <div class="quota-grid">
          <div class="quota-item">
            <span class="quota-item-label">生成次数</span>
            <div class="quota-bar">
              <div class="quota-bar-fill" :style="{ width: Math.min(100, (quota.dailyGenUsed / quota.dailyGenLimit) * 100) + '%' }"></div>
            </div>
            <span class="quota-item-value">{{ quota.dailyGenUsed }} / {{ quota.dailyGenLimit }}</span>
          </div>
          <div class="quota-item">
            <span class="quota-item-label">Token 消耗</span>
            <div class="quota-bar">
              <div class="quota-bar-fill" :style="{ width: Math.min(100, (quota.dailyTokenUsed / quota.dailyTokenLimit) * 100) + '%' }"></div>
            </div>
            <span class="quota-item-value">{{ fmtNum(quota.dailyTokenUsed) }} / {{ fmtNum(quota.dailyTokenLimit) }}</span>
          </div>
        </div>
      </div>
      <div class="quota-card">
        <div class="quota-header">
          <span class="quota-label">本月额度</span>
          <a-tag :color="quota.monthlyGenUsed >= quota.monthlyGenLimit ? 'red' : 'blue'" size="small">
            {{ quota.monthlyGenUsed >= quota.monthlyGenLimit ? '已用完' : '正常' }}
          </a-tag>
        </div>
        <div class="quota-grid">
          <div class="quota-item">
            <span class="quota-item-label">生成次数</span>
            <div class="quota-bar">
              <div class="quota-bar-fill" :style="{ width: Math.min(100, (quota.monthlyGenUsed / quota.monthlyGenLimit) * 100) + '%' }"></div>
            </div>
            <span class="quota-item-value">{{ quota.monthlyGenUsed }} / {{ quota.monthlyGenLimit }}</span>
          </div>
          <div class="quota-item">
            <span class="quota-item-label">Token 消耗</span>
            <div class="quota-bar">
              <div class="quota-bar-fill" :style="{ width: Math.min(100, (quota.monthlyTokenUsed / quota.monthlyTokenLimit) * 100) + '%' }"></div>
            </div>
            <span class="quota-item-value">{{ fmtNum(quota.monthlyTokenUsed) }} / {{ fmtNum(quota.monthlyTokenLimit) }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="quick-links">
      <button class="action-card" @click="router.push('/token/details')">
        <div class="action-icon"><BarChartOutlined /></div>
        <div class="action-info"><h3>消耗明细</h3><p>按应用、模型和用途查看调用记录</p></div>
        <RightOutlined class="action-arrow" />
      </button>
      <button class="action-card" @click="router.push('/token/model')">
        <div class="action-icon"><CloudServerOutlined /></div>
        <div class="action-info"><h3>模型分布</h3><p>看清楚哪些模型带来主要成本</p></div>
        <RightOutlined class="action-arrow" />
      </button>
      <button class="action-card" @click="router.push('/token/ranking')">
        <div class="action-icon"><TrophyOutlined /></div>
        <div class="action-info"><h3>使用排行</h3><p>比较用户维度的资源使用情况</p></div>
        <RightOutlined class="action-arrow" />
      </button>
    </section>

    <section class="gen-stats" v-if="genStats">
      <div class="gen-stats-grid">
        <div class="gen-stat-card">
          <span class="gen-stat-label">总生成次数</span>
          <strong class="gen-stat-value">{{ genStats.totalGenerations }}</strong>
        </div>
        <div class="gen-stat-card">
          <span class="gen-stat-label">已部署</span>
          <strong class="gen-stat-value">{{ genStats.deployCount }}</strong>
        </div>
        <div class="gen-stat-card">
          <span class="gen-stat-label">HTML</span>
          <strong class="gen-stat-value">{{ genStats.htmlCount }}</strong>
        </div>
        <div class="gen-stat-card">
          <span class="gen-stat-label">多文件</span>
          <strong class="gen-stat-value">{{ genStats.multiFileCount }}</strong>
        </div>
        <div class="gen-stat-card">
          <span class="gen-stat-label">Vue 项目</span>
          <strong class="gen-stat-value">{{ genStats.vueCount }}</strong>
        </div>
      </div>
      <div class="recent-gen" v-if="genStats.recentRecords?.length">
        <h4 class="recent-gen-title">最近生成</h4>
        <div class="recent-gen-list">
          <div class="recent-gen-item" v-for="r in genStats.recentRecords" :key="r.appId">
            <span class="recent-gen-name">{{ r.appName }}</span>
            <a-tag size="small">{{ r.codeGenType }}</a-tag>
            <span class="recent-gen-tokens">{{ fmtNum(r.tokenUsed) }} Token</span>
          </div>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="card-head">
        <h3 class="card-title">最近账单记录</h3>
        <a-button type="link" size="small" @click="router.push('/token/details')">查看全部 <RightOutlined /></a-button>
      </div>
      <a-table :columns="columns" :data-source="records" :pagination="false" :loading="recordsLoading" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'modelName'"><a-tag color="blue">{{ record.modelName }}</a-tag></template>
          <template v-if="column.key === 'purposeDescription'">{{ record.purposeDescription || record.aiCallPurpose }}</template>
          <template v-if="column.key === 'totalTokens'"><span class="tv">{{ record.totalTokens }}</span></template>
          <template v-if="column.key === 'createTime'">{{ formatTime(record.createTime) }}</template>
        </template>
      </a-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  CloudServerOutlined, BarChartOutlined, TrophyOutlined, RightOutlined
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { getUserTokenSummary, getUserTokenDetails, getUserGenerationStats } from '@/api/token'
import { getMyQuota } from '@/api/quota'
import { formatDateTime as formatTime } from '@/utils/time'
import type { UserQuotaVO, GenerationStatsDTO } from '@/types'

const router = useRouter()
const loading = ref(false)
const recordsLoading = ref(false)
const summary = ref<any>({})
const records = ref<any[]>([])
const quota = ref<UserQuotaVO | null>(null)
const genStats = ref<GenerationStatsDTO | null>(null)

const fmtNum = (n: number) => n >= 1e6 ? (n / 1e6).toFixed(1) + 'M' : n >= 1e3 ? (n / 1e3).toFixed(1) + 'K' : String(n)

const columns = [
  { title: '应用', dataIndex: 'appName', key: 'appName' },
  { title: '应用 ID', dataIndex: 'appId', key: 'appId' },
  { title: '模型', dataIndex: 'modelName', key: 'modelName' },
  { title: '用途', dataIndex: 'purposeDescription', key: 'purposeDescription' },
  { title: '输入', dataIndex: 'inputTokens', key: 'inputTokens' },
  { title: '输出', dataIndex: 'outputTokens', key: 'outputTokens' },
  { title: '总 Token', dataIndex: 'totalTokens', key: 'totalTokens' },
  { title: '时间', dataIndex: 'createTime', key: 'createTime' },
]

const fetchSummary = async () => {
  loading.value = true
  try { const res = await getUserTokenSummary(); summary.value = res.data || {} }
  catch (e) {} finally { loading.value = false }
}

const fetchRecords = async () => {
  recordsLoading.value = true
  try { const res: any = await getUserTokenDetails({ page: 1, pageSize: 10 }); records.value = res.data || [] }
  catch (e) {} finally { recordsLoading.value = false }
}

const fetchQuota = async () => {
  try { const res: any = await getMyQuota(); quota.value = res.data || null }
  catch (e) {}
}

const fetchGenStats = async () => {
  try { const res: any = await getUserGenerationStats(); genStats.value = res.data || null }
  catch (e) {}
}

onMounted(() => { fetchSummary(); fetchRecords(); fetchQuota(); fetchGenStats() })
</script>

<style scoped>
.page { padding: 30px 24px 48px; max-width: 1280px; margin: 0 auto; }
.ledger-summary { display: grid; grid-template-columns: 1fr 1.35fr; gap: 1px; overflow: hidden; margin-bottom: 16px; border: 1px solid var(--border-light); border-radius: var(--r-xl); background: var(--border-light); }
.summary-primary, .summary-split { background: var(--bg-card); }
.summary-primary { padding: 22px; }
.summary-label, .summary-note { display: block; color: var(--t-muted); font-size: 12px; font-weight: 750; }
.summary-primary strong { display: block; margin: 6px 0 3px; color: var(--t-primary); font-size: 42px; font-weight: 900; letter-spacing: -1.4px; line-height: 1; }
.summary-split { display: grid; grid-template-columns: repeat(3, 1fr); }
.split-item { padding: 22px; border-right: 1px solid var(--border-light); }
.split-item:last-child { border-right: none; }
.split-item span { display: block; color: var(--t-muted); font-size: 12px; font-weight: 750; margin-bottom: 8px; }
.split-item strong { color: var(--t-primary); font-size: 24px; font-weight: 850; }
.quick-links { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.action-card { display: flex; align-items: center; gap: 13px; text-align: left; padding: 15px; background: var(--bg-card); border-radius: var(--r-lg); border: 1px solid var(--border-light); cursor: pointer; transition: border-color var(--t-fast), background var(--t-fast); }
.action-card:hover { border-color: var(--c-primary-200); background: var(--c-primary-50); }
.action-icon { width: 38px; height: 38px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: var(--c-primary); background: var(--bg-soft); font-size: 17px; flex-shrink: 0; }
.action-info { flex: 1; min-width: 0; }
.action-info h3 { margin: 0; font-size: 14px; font-weight: 800; color: var(--t-primary); }
.action-info p { margin: 3px 0 0; font-size: 12px; color: var(--t-muted); }
.action-arrow { color: var(--t-light); }
.card { background: var(--bg-card); border-radius: var(--r-xl); padding: 18px; border: 1px solid var(--border-light); }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.card-title { margin: 0; font-size: 14px; font-weight: 850; color: var(--t-primary); }
.tv { font-weight: 800; color: var(--c-primary); }
.quota-section { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
.quota-card { background: var(--bg-card); border-radius: var(--r-xl); padding: 18px; border: 1px solid var(--border-light); }
.quota-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.quota-label { font-size: 14px; font-weight: 800; color: var(--t-primary); }
.quota-grid { display: flex; flex-direction: column; gap: 12px; }
.quota-item { display: flex; align-items: center; gap: 10px; }
.quota-item-label { font-size: 12px; color: var(--t-muted); font-weight: 750; min-width: 70px; }
.quota-bar { flex: 1; height: 6px; background: var(--bg-soft); border-radius: 3px; overflow: hidden; }
.quota-bar-fill { height: 100%; background: var(--c-primary); border-radius: 3px; transition: width 0.3s ease; }
.quota-item-value { font-size: 12px; font-weight: 700; color: var(--t-primary); min-width: 100px; text-align: right; }
.gen-stats { background: var(--bg-card); border-radius: var(--r-xl); padding: 18px; border: 1px solid var(--border-light); margin-bottom: 16px; }
.gen-stats-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 16px; }
.gen-stat-card { text-align: center; padding: 12px; background: var(--bg-soft); border-radius: var(--r-lg); }
.gen-stat-label { display: block; font-size: 12px; color: var(--t-muted); font-weight: 750; margin-bottom: 4px; }
.gen-stat-value { font-size: 22px; font-weight: 900; color: var(--c-primary); }
.recent-gen-title { margin: 0 0 10px; font-size: 14px; font-weight: 800; color: var(--t-primary); }
.recent-gen-list { display: flex; flex-direction: column; gap: 8px; }
.recent-gen-item { display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: var(--bg-soft); border-radius: var(--r-md); }
.recent-gen-name { font-weight: 700; color: var(--t-primary); font-size: 13px; flex: 1; }
.recent-gen-tokens { font-size: 12px; color: var(--t-muted); font-weight: 700; }
@media (max-width: 900px) { .ledger-summary, .quick-links, .quota-section { grid-template-columns: 1fr; } .summary-split { grid-template-columns: 1fr; } .split-item { border-right: none; border-bottom: 1px solid var(--border-light); } .gen-stats-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) { .page { padding: 22px 16px 36px; } .gen-stats-grid { grid-template-columns: repeat(2, 1fr); } }
</style>