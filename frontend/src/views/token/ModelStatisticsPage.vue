<template>
  <div class="page">
    <PageHeader title="模型分布" description="看清楚不同模型在资源账单里的占比和调用强度。" eyebrow="资源账单">
      <template #actions>
        <a-button @click="router.back()" size="small"><ArrowLeftOutlined /> 返回</a-button>
      </template>
    </PageHeader>

    <section class="card">
      <div class="card-head">
        <div>
          <h3 class="card-title">模型消耗排行</h3>
          <p class="card-desc">按当前范围统计模型维度的 Token 和费用。</p>
        </div>
        <div class="period-tabs">
          <button v-for="p in periods" :key="p.value" :class="['p-btn', { active: selected === p.value }]" @click="selected = p.value; fetchRanking()">{{ p.label }}</button>
        </div>
      </div>
      <a-spin :spinning="loading">
        <a-table :columns="columns" :data-source="rankings" :pagination="false" size="small">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'rank'"><div class="rank-cell">{{ index + 1 }}</div></template>
            <template v-if="column.key === 'modelName'"><a-tag color="blue">{{ record.modelDisplayName || record.modelName }}</a-tag></template>
            <template v-if="column.key === 'totalTokens'"><span class="tv">{{ record.totalTokens }}</span></template>
            <template v-if="column.key === 'percentage'">
              <div class="usage-bar"><span :style="{ width: `${record.percentage || 0}%` }"></span></div>
              <span class="pct">{{ record.percentage }}%</span>
            </template>
            <template v-if="column.key === 'estimatedCost'">¥{{ record.estimatedCost?.toFixed(2) || '0.00' }}</template>
          </template>
        </a-table>
      </a-spin>
    </section>

    <section class="info-grid">
      <div class="card compact"><div class="card-head"><h3 class="card-title">统计概览</h3></div>
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="总模型数">{{ rankingData.totalModels || 0 }}</a-descriptions-item>
          <a-descriptions-item label="总 Token 数">{{ rankingData.totalTokens || 0 }}</a-descriptions-item>
          <a-descriptions-item label="总调用次数">{{ rankingData.totalCalls || 0 }}</a-descriptions-item>
          <a-descriptions-item label="最活跃模型">{{ rankingData.mostActiveModel || '-' }}</a-descriptions-item>
        </a-descriptions>
      </div>
      <div class="card compact"><div class="card-head"><h3 class="card-title">时间范围</h3></div>
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="开始时间">{{ ft(rankingData.statisticsStartTime) }}</a-descriptions-item>
          <a-descriptions-item label="结束时间">{{ ft(rankingData.statisticsEndTime) }}</a-descriptions-item>
          <a-descriptions-item label="排行类型">{{ rankingData.rankingType || '-' }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { getModelTokenRanking } from '@/api/token'

const router = useRouter()
const loading = ref(false)
const selected = ref('all')
const periods = [{ value: 'today', label: '今日' }, { value: 'week', label: '本周' }, { value: 'month', label: '本月' }, { value: 'all', label: '全部' }]
const rankings = ref<any[]>([])
const rankingData = ref<any>({})
const columns = [
  { title: '排名', dataIndex: 'rank', key: 'rank', width: 60 },
  { title: '模型', dataIndex: 'modelName', key: 'modelName', width: 140 },
  { title: '总 Token', dataIndex: 'totalTokens', key: 'totalTokens', width: 110 },
  { title: '调用次数', dataIndex: 'callCount', key: 'callCount', width: 90 },
  { title: '用户数', dataIndex: 'userCount', key: 'userCount', width: 90 },
  { title: '占比', dataIndex: 'percentage', key: 'percentage', width: 170 },
  { title: '预估费用', dataIndex: 'estimatedCost', key: 'estimatedCost', width: 90 },
]
const ft = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-'
const fetchRanking = async () => { loading.value = true; try { const r: any = await getModelTokenRanking({ limit: 20 }); rankings.value = r.data?.rankings || []; rankingData.value = r.data || {} } catch (e) {} finally { loading.value = false } }
onMounted(() => fetchRanking())
</script>

<style scoped>
.page { padding: 30px 24px 48px; max-width: 1200px; margin: 0 auto; }
.card { background: var(--bg-card); border-radius: var(--r-xl); padding: 18px; border: 1px solid var(--border-light); margin-bottom: 16px; }
.card.compact { margin-bottom: 0; }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; flex-wrap: wrap; gap: 10px; }
.card-title { margin: 0; font-size: 14px; font-weight: 850; color: var(--t-primary); }
.card-desc { margin: 4px 0 0; color: var(--t-muted); font-size: 12px; }
.period-tabs { display: flex; gap: 4px; padding: 3px; border: 1px solid var(--border-light); border-radius: var(--r-md); background: var(--bg-soft); }
.p-btn { padding: 5px 12px; border-radius: var(--r-sm); border: none; background: transparent; color: var(--t-muted); font-size: 12px; font-weight: 700; cursor: pointer; transition: background var(--t-fast), color var(--t-fast); }
.p-btn:hover { background: var(--bg-hover); }
.p-btn.active { background: var(--bg-card); color: var(--c-primary); }
.rank-cell { width: 24px; height: 24px; border-radius: 8px; background: var(--bg-soft); border: 1px solid var(--border-light); display: inline-flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 850; color: var(--t-muted); }
.tv { font-weight: 800; color: var(--c-primary); }
.usage-bar { display: inline-flex; width: 92px; height: 7px; overflow: hidden; border-radius: 999px; background: var(--bg-soft); vertical-align: middle; }
.usage-bar span { display: block; height: 100%; border-radius: inherit; background: var(--c-primary); }
.pct { font-size: 12px; color: var(--t-muted); margin-left: 8px; }
.info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
@media (max-width: 900px) { .info-grid { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .page { padding: 22px 16px 36px; } }
</style>