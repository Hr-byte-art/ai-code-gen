<template>
  <div class="token-overview-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">Token 管理</h1>
        <span class="page-subtitle">监控和管理你的 Token 使用情况</span>
      </div>

      <!-- 统计卡片 -->
      <a-spin :spinning="loading">
        <a-row :gutter="16" class="stats-row">
          <a-col :xs="12" :sm="12" :md="6">
            <div class="stat-card stat-card-primary">
              <div class="stat-icon">
                <CloudServerOutlined />
              </div>
              <div class="stat-info">
                <div class="stat-label">总 Token 数</div>
                <div class="stat-value">{{ formatNumber(summary.totalTokens || 0) }}</div>
              </div>
            </div>
          </a-col>
          <a-col :xs="12" :sm="12" :md="6">
            <div class="stat-card stat-card-success">
              <div class="stat-icon">
                <ArrowDownOutlined />
              </div>
              <div class="stat-info">
                <div class="stat-label">输入 Token</div>
                <div class="stat-value">{{ formatNumber(summary.totalInputTokens || 0) }}</div>
              </div>
            </div>
          </a-col>
          <a-col :xs="12" :sm="12" :md="6">
            <div class="stat-card stat-card-warning">
              <div class="stat-icon">
                <ArrowUpOutlined />
              </div>
              <div class="stat-info">
                <div class="stat-label">输出 Token</div>
                <div class="stat-value">{{ formatNumber(summary.totalOutputTokens || 0) }}</div>
              </div>
            </div>
          </a-col>
          <a-col :xs="12" :sm="12" :md="6">
            <div class="stat-card stat-card-purple">
              <div class="stat-icon">
                <AppstoreOutlined />
              </div>
              <div class="stat-info">
                <div class="stat-label">应用数量</div>
                <div class="stat-value">{{ summary.appCount || 0 }}</div>
              </div>
            </div>
          </a-col>
        </a-row>
      </a-spin>

      <!-- 快捷操作 -->
      <a-row :gutter="16" class="actions-row">
        <a-col :xs="24" :md="12">
          <div class="action-card" @click="goToDetails">
            <div class="action-icon action-icon-blue">
              <BarChartOutlined />
            </div>
            <div class="action-info">
              <h3 class="action-title">Token 详情</h3>
              <p class="action-desc">查看详细的 Token 使用记录</p>
            </div>
            <RightOutlined class="action-arrow" />
          </div>
        </a-col>
        <a-col :xs="24" :md="12">
          <div class="action-card" @click="goToRanking">
            <div class="action-icon action-icon-amber">
              <TrophyOutlined />
            </div>
            <div class="action-info">
              <h3 class="action-title">排行榜</h3>
              <p class="action-desc">查看用户 Token 消耗排行</p>
            </div>
            <RightOutlined class="action-arrow" />
          </div>
        </a-col>
      </a-row>

      <!-- 最近使用记录 -->
      <div class="records-card">
        <div class="card-header">
          <h3 class="card-title">最近使用记录</h3>
          <a-button type="link" @click="goToDetails" class="view-all-btn">
            查看全部
            <RightOutlined />
          </a-button>
        </div>

        <a-table
          :columns="columns"
          :data-source="records"
          :pagination="false"
          :loading="recordsLoading"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'modelName'">
              <a-tag color="blue">{{ record.modelName }}</a-tag>
            </template>
            <template v-if="column.key === 'totalTokens'">
              <span class="token-value">{{ record.totalTokens }}</span>
            </template>
            <template v-if="column.key === 'createTime'">
              {{ formatTime(record.createTime) }}
            </template>
          </template>
        </a-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  CloudServerOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  AppstoreOutlined,
  BarChartOutlined,
  TrophyOutlined,
  RightOutlined
} from '@ant-design/icons-vue'
import { getUserTokenSummary, getUserTokenDetails } from '@/api/token'

const router = useRouter()

const loading = ref(false)
const recordsLoading = ref(false)
const summary = ref<any>({})
const records = ref<any[]>([])

const columns = [
  { title: '应用名称', dataIndex: 'appName', key: 'appName' },
  { title: '模型', dataIndex: 'modelName', key: 'modelName' },
  { title: '用途', dataIndex: 'aiCallPurpose', key: 'aiCallPurpose' },
  { title: 'Token 数', dataIndex: 'totalTokens', key: 'totalTokens' },
  { title: '时间', dataIndex: 'createTime', key: 'createTime' }
]

const formatNumber = (num: number) => {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const fetchSummary = async () => {
  loading.value = true
  try {
    const res = await getUserTokenSummary()
    summary.value = res.data || {}
  } catch (error) {
    console.error('获取Token统计失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchRecords = async () => {
  recordsLoading.value = true
  try {
    const res: any = await getUserTokenDetails({ page: 1, pageSize: 10 })
    records.value = res.data || []
  } catch (error) {
    console.error('获取Token详情失败:', error)
  } finally {
    recordsLoading.value = false
  }
}

const goToDetails = () => {
  router.push('/token/details')
}

const goToRanking = () => {
  router.push('/token/ranking')
}

onMounted(() => {
  fetchSummary()
  fetchRecords()
})
</script>

<style scoped>
.token-overview-page {
  padding: 24px;
  background: var(--bg-page);
  min-height: calc(100vh - 64px);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.3px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
  transition: all var(--transition-base);
  margin-bottom: 16px;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.stat-card-primary .stat-icon {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
}

.stat-card-success .stat-icon {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
}

.stat-card-warning .stat-icon {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff;
}

.stat-card-purple .stat-icon {
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
  color: #fff;
}

.stat-info {
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 2px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

/* Action Cards */
.actions-row {
  margin-bottom: 20px;
}

.action-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
  cursor: pointer;
  transition: all var(--transition-base);
  margin-bottom: 16px;
}

.action-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: rgba(59, 130, 246, 0.15);
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.action-icon-blue {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
}

.action-icon-amber {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff;
}

.action-info {
  flex: 1;
}

.action-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.action-desc {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--text-muted);
}

.action-arrow {
  font-size: 14px;
  color: #94a3b8;
  transition: transform var(--transition-fast);
}

.action-card:hover .action-arrow {
  transform: translateX(2px);
  color: var(--color-primary);
}

/* Records Card */
.records-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
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

.view-all-btn {
  font-size: 13px !important;
  font-weight: 500 !important;
}

.token-value {
  font-weight: 600;
  color: var(--color-primary);
}

@media (max-width: 768px) {
  .token-overview-page {
    padding: 16px;
  }
}
</style>
