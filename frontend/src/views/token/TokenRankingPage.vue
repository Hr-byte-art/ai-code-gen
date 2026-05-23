<template>
  <div class="ranking-page">
    <div class="container">
      <div class="page-header">
        <a-button @click="goBack" class="back-btn" size="small">
          <ArrowLeftOutlined />
          返回
        </a-button>
        <h1 class="page-title">排行榜</h1>
      </div>

      <!-- 时间范围筛选 -->
      <div class="filter-card">
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

      <!-- 排行榜内容 -->
      <a-row :gutter="24">
        <!-- 左侧排行榜 -->
        <a-col :xs="24" :lg="16">
          <div class="ranking-card">
            <div class="card-header">
              <h3 class="card-title">用户 Token 消耗排行</h3>
              <a-tag color="blue">{{ selectedPeriodLabel }}</a-tag>
            </div>

            <a-spin :spinning="loading">
              <div class="ranking-list">
                <div v-for="(item, index) in rankings" :key="item.userId" class="ranking-item">
                  <div class="rank-badge" :class="{ 'rank-gold': index === 0, 'rank-silver': index === 1, 'rank-bronze': index === 2 }">
                    {{ index + 1 }}
                  </div>

                  <a-avatar :size="44" :src="item.userAvatar" class="rank-avatar">
                    <template #icon><UserOutlined /></template>
                  </a-avatar>

                  <div class="rank-info">
                    <div class="rank-name">{{ item.userName }}</div>
                    <div class="rank-desc">应用数: {{ item.appCount }}</div>
                  </div>

                  <div class="rank-stats">
                    <div class="rank-token-value">{{ formatNumber(item.totalTokens) }}</div>
                    <div class="rank-token-label">Token</div>
                  </div>
                </div>
                <a-empty v-if="!loading && rankings.length === 0" description="暂无数据" />
              </div>
            </a-spin>
          </div>
        </a-col>

        <!-- 右侧统计 -->
        <a-col :xs="24" :lg="8">
          <div class="my-rank-card">
            <div class="card-header">
              <h3 class="card-title">我的排名</h3>
            </div>
            <div class="my-rank-content">
              <div class="my-rank-number">
                <span class="rank-value">{{ myRanking.ranking || '-' }}</span>
                <span class="rank-total">/ {{ totalUsers }}</span>
              </div>
              <div class="my-rank-stats">
                <div class="my-stat-item">
                  <div class="my-stat-value">{{ formatNumber(myRanking.totalTokens || 0) }}</div>
                  <div class="my-stat-label">总 Token</div>
                </div>
                <div class="my-stat-divider"></div>
                <div class="my-stat-item">
                  <div class="my-stat-value">{{ myRanking.appCount || 0 }}</div>
                  <div class="my-stat-label">应用数</div>
                </div>
              </div>
            </div>
          </div>

          <div class="info-card">
            <div class="card-header">
              <h3 class="card-title">排行榜说明</h3>
            </div>
            <div class="info-content">
              <p>排行榜根据用户的 Token 使用量进行排名。</p>
              <ul>
                <li>每日凌晨更新数据</li>
                <li>仅统计成功请求</li>
                <li>相同使用量按时间先后排序</li>
              </ul>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined, UserOutlined } from '@ant-design/icons-vue'
import { getUserTokenRanking, getUserTokenSummary } from '@/api/token'

const router = useRouter()

const loading = ref(false)
const selectedPeriod = ref('all')

const timePeriods = [
  { value: 'today', label: '今日' },
  { value: 'week', label: '本周' },
  { value: 'month', label: '本月' },
  { value: 'all', label: '全部' }
]

const selectedPeriodLabel = computed(() => {
  const period = timePeriods.find(p => p.value === selectedPeriod.value)
  return period?.label || ''
})

const rankings = ref<any[]>([])
const totalUsers = ref(0)
const myRanking = ref<any>({})

const formatNumber = (num: number) => {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

const fetchRanking = async () => {
  loading.value = true
  try {
    const res: any = await getUserTokenRanking({ page: 1, pageSize: 50 })
    rankings.value = res.data?.rankings || []
    totalUsers.value = res.data?.totalUsers || 0
  } catch (error) {
    console.error('获取排行榜失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchMyRanking = async () => {
  try {
    const res = await getUserTokenSummary()
    myRanking.value = res.data || {}
  } catch (error) {
    console.error('获取我的排名失败:', error)
  }
}

const goBack = () => { router.back() }

onMounted(() => { fetchRanking(); fetchMyRanking() })
</script>

<style scoped>
.ranking-page {
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

.filter-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
}

.period-tabs {
  display: flex;
  gap: 4px;
}

.period-btn {
  padding: 6px 16px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.period-btn:hover { background: #f1f5f9; color: var(--text-secondary); }
.period-btn.active { background: var(--color-primary); color: #fff; }

.ranking-card, .my-rank-card, .info-card {
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
}

.card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: #f8fafc;
  border-radius: 12px;
  transition: all var(--transition-fast);
}

.ranking-item:hover { background: #f1f5f9; }

.rank-badge {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  flex-shrink: 0;
}

.rank-gold { background: linear-gradient(135deg, #f59e0b, #d97706); color: #fff; }
.rank-silver { background: linear-gradient(135deg, #94a3b8, #64748b); color: #fff; }
.rank-bronze { background: linear-gradient(135deg, #d97706, #b45309); color: #fff; }

.rank-avatar { flex-shrink: 0; border: 2px solid #e2e8f0 !important; }

.rank-info { flex: 1; min-width: 0; }

.rank-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.rank-desc { font-size: 12px; color: var(--text-muted); margin-top: 1px; }

.rank-stats { text-align: right; flex-shrink: 0; }
.rank-token-value { font-size: 16px; font-weight: 700; color: var(--color-primary); }
.rank-token-label { font-size: 11px; color: var(--text-muted); }

.my-rank-content { text-align: center; }

.my-rank-number { margin-bottom: 20px; }
.my-rank-number .rank-value { font-size: 48px; font-weight: 800; color: var(--color-primary); line-height: 1; }
.my-rank-number .rank-total { font-size: 16px; color: #94a3b8; font-weight: 500; }

.my-rank-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
}

.my-stat-item { flex: 1; text-align: center; }
.my-stat-value { font-size: 18px; font-weight: 700; color: var(--text-primary); }
.my-stat-label { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.my-stat-divider { width: 1px; height: 32px; background: #e2e8f0; }

.info-content { font-size: 13px; line-height: 1.7; color: var(--text-secondary); }
.info-content p { margin: 0 0 8px; }
.info-content ul { padding-left: 18px; margin: 8px 0 0; }
.info-content li { margin-bottom: 4px; }

@media (max-width: 768px) {
  .ranking-page { padding: 16px; }
  .page-header { flex-direction: column; align-items: flex-start; }
}
</style>
