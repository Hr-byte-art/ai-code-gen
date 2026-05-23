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

    <section class="card">
      <div class="card-head">
        <h3 class="card-title">最近账单记录</h3>
        <a-button type="link" size="small" @click="router.push('/token/details')">查看全部 <RightOutlined /></a-button>
      </div>
      <a-table :columns="columns" :data-source="records" :pagination="false" :loading="recordsLoading" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'modelName'"><a-tag color="blue">{{ record.modelName }}</a-tag></template>
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
import { getUserTokenSummary, getUserTokenDetails } from '@/api/token'

const router = useRouter()
const loading = ref(false)
const recordsLoading = ref(false)
const summary = ref<any>({})
const records = ref<any[]>([])

const fmtNum = (n: number) => n >= 1e6 ? (n / 1e6).toFixed(1) + 'M' : n >= 1e3 ? (n / 1e3).toFixed(1) + 'K' : String(n)
const formatTime = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : ''

const columns = [
  { title: '应用', dataIndex: 'appName', key: 'appName' },
  { title: '模型', dataIndex: 'modelName', key: 'modelName' },
  { title: '用途', dataIndex: 'aiCallPurpose', key: 'aiCallPurpose' },
  { title: 'Token', dataIndex: 'totalTokens', key: 'totalTokens' },
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

onMounted(() => { fetchSummary(); fetchRecords() })
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
@media (max-width: 900px) { .ledger-summary, .quick-links { grid-template-columns: 1fr; } .summary-split { grid-template-columns: 1fr; } .split-item { border-right: none; border-bottom: 1px solid var(--border-light); } }
@media (max-width: 768px) { .page { padding: 22px 16px 36px; } }
</style>