<template>
  <div class="model-statistics-page">
    <div class="container">
      <div class="page-header">
        <a-button @click="goBack" class="back-btn" size="small">
          <ArrowLeftOutlined />
          返回
        </a-button>
        <h1 class="page-title">模型统计</h1>
      </div>

      <div class="ranking-card">
        <div class="card-header">
          <h3 class="card-title">模型 Token 消耗排行</h3>
          <div class="period-tabs">
            <button
              v-for="period in timePeriods"
              :key="period.value"
              :class="['period-btn', { active: selectedPeriod === period.value }]"
              @click="selectedPeriod = period.value; fetchRanking()"
            >
              {{ period.label }}
            </button>
          </div>
        </div>

        <a-spin :spinning="loading">
          <a-table :columns="columns" :data-source="rankings" :pagination="false" size="middle">
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'rank'">
                <div class="rank-badge" :class="{ 'rank-gold': index === 0, 'rank-silver': index === 1, 'rank-bronze': index === 2 }">
                  {{ index + 1 }}
                </div>
              </template>
              <template v-if="column.key === 'modelName'">
                <a-tag color="blue">{{ record.modelDisplayName || record.modelName }}</a-tag>
              </template>
              <template v-if="column.key === 'totalTokens'">
                <span class="token-value">{{ record.totalTokens }}</span>
              </template>
              <template v-if="column.key === 'percentage'">
                <a-progress :percent="record.percentage" size="small" :stroke-color="{ '0%': '#3b82f6', '100%': '#8b5cf6' }" :show-info="false" />
                <span class="percentage-text">{{ record.percentage }}%</span>
              </template>
              <template v-if="column.key === 'estimatedCost'">
                <span class="cost-value">&yen;{{ record.estimatedCost?.toFixed(2) || '0.00' }}</span>
              </template>
            </template>
          </a-table>
        </a-spin>
      </div>

      <a-row :gutter="24">
        <a-col :xs="24" :md="12">
          <div class="stat-card">
            <div class="card-header">
              <h3 class="card-title">统计概览</h3>
            </div>
            <a-descriptions :column="1" bordered size="small">
              <a-descriptions-item label="总模型数">{{ rankingData.totalModels || 0 }}</a-descriptions-item>
              <a-descriptions-item label="总 Token 数">{{ rankingData.totalTokens || 0 }}</a-descriptions-item>
              <a-descriptions-item label="总调用次数">{{ rankingData.totalCalls || 0 }}</a-descriptions-item>
              <a-descriptions-item label="最活跃模型">{{ rankingData.mostActiveModel || '-' }}</a-descriptions-item>
              <a-descriptions-item label="最高效模型">{{ rankingData.mostEfficientModel || '-' }}</a-descriptions-item>
            </a-descriptions>
          </div>
        </a-col>
        <a-col :xs="24" :md="12">
          <div class="stat-card">
            <div class="card-header">
              <h3 class="card-title">时间范围</h3>
            </div>
            <a-descriptions :column="1" bordered size="small">
              <a-descriptions-item label="开始时间">{{ formatTime(rankingData.statisticsStartTime) }}</a-descriptions-item>
              <a-descriptions-item label="结束时间">{{ formatTime(rankingData.statisticsEndTime) }}</a-descriptions-item>
              <a-descriptions-item label="排行类型">{{ rankingData.rankingType || '-' }}</a-descriptions-item>
            </a-descriptions>
          </div>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { getModelTokenRanking } from '@/api/token'

const router = useRouter()

const loading = ref(false)
const selectedPeriod = ref('all')

const timePeriods = [
  { value: 'today', label: '今日' },
  { value: 'week', label: '本周' },
  { value: 'month', label: '本月' },
  { value: 'all', label: '全部' }
]

const rankings = ref<any[]>([])
const rankingData = ref<any>({})

const columns = [
  { title: '排名', dataIndex: 'rank', key: 'rank', width: 70 },
  { title: '模型', dataIndex: 'modelName', key: 'modelName', width: 150 },
  { title: '总 Token', dataIndex: 'totalTokens', key: 'totalTokens', width: 120 },
  { title: '调用次数', dataIndex: 'callCount', key: 'callCount', width: 100 },
  { title: '用户数', dataIndex: 'userCount', key: 'userCount', width: 100 },
  { title: '占比', dataIndex: 'percentage', key: 'percentage', width: 160 },
  { title: '预估费用', dataIndex: 'estimatedCost', key: 'estimatedCost', width: 100 }
]

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const fetchRanking = async () => {
  loading.value = true
  try {
    const res: any = await getModelTokenRanking({ limit: 20 })
    rankings.value = res.data?.rankings || []
    rankingData.value = res.data || {}
  } catch (error) {
    console.error('获取模型排行失败:', error)
  } finally {
    loading.value = false
  }
}

const goBack = () => { router.back() }

onMounted(() => { fetchRanking() })
</script>

<style scoped>
.model-statistics-page {
  padding: 24px;
  background: var(--bg-page);
  min-height: calc(100vh - 64px);
}

.container { max-width: 1200px; margin: 0 auto; }

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.back-btn { border-radius: 8px !important; }

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.ranking-card, .stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.period-tabs {
  display: flex;
  gap: 4px;
}

.period-btn {
  padding: 5px 14px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.period-btn:hover { background: #f1f5f9; color: var(--text-secondary); }
.period-btn.active { background: var(--color-primary); color: #fff; }

.rank-badge {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  background: #e2e8f0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.rank-gold { background: linear-gradient(135deg, #f59e0b, #d97706); color: #fff; }
.rank-silver { background: linear-gradient(135deg, #94a3b8, #64748b); color: #fff; }
.rank-bronze { background: linear-gradient(135deg, #d97706, #b45309); color: #fff; }

.token-value { font-weight: 600; color: var(--color-primary); }

.percentage-text { font-size: 12px; color: var(--text-muted); margin-left: 8px; }

.cost-value { font-weight: 600; color: var(--text-secondary); }

@media (max-width: 768px) {
  .model-statistics-page { padding: 16px; }
  .page-header { flex-direction: column; align-items: flex-start; }
}
</style>
