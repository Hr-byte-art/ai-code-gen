<template>
  <div class="token-details-page">
    <div class="container">
      <div class="page-header">
        <a-button @click="goBack" class="back-btn" size="small">
          <ArrowLeftOutlined />
          返回
        </a-button>
        <h1 class="page-title">Token 详情</h1>
      </div>

      <!-- 筛选条件 -->
      <div class="filter-card">
        <a-form layout="inline" :model="filterForm">
          <a-form-item label="时间范围">
            <a-range-picker v-model:value="filterForm.dateRange" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="handleSearch" size="small">
              <SearchOutlined />
              查询
            </a-button>
            <a-button style="margin-left: 8px" @click="handleReset" size="small">
              重置
            </a-button>
          </a-form-item>
        </a-form>
      </div>

      <!-- 统计信息 -->
      <a-row :gutter="16" class="stats-row">
        <a-col :xs="24" :sm="8">
          <div class="stat-card stat-primary">
            <div class="stat-label">总 Token 数</div>
            <div class="stat-value">{{ formatNumber(summary.totalTokens || 0) }}</div>
          </div>
        </a-col>
        <a-col :xs="24" :sm="8">
          <div class="stat-card stat-success">
            <div class="stat-label">输入 Token</div>
            <div class="stat-value">{{ formatNumber(summary.totalInputTokens || 0) }}</div>
          </div>
        </a-col>
        <a-col :xs="24" :sm="8">
          <div class="stat-card stat-warning">
            <div class="stat-label">输出 Token</div>
            <div class="stat-value">{{ formatNumber(summary.totalOutputTokens || 0) }}</div>
          </div>
        </a-col>
      </a-row>

      <!-- 数据表格 -->
      <div class="table-card">
        <a-table
          :columns="columns"
          :data-source="records"
          :pagination="pagination"
          :loading="loading"
          @change="handleTableChange"
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { getUserTokenSummary, getUserTokenDetails } from '@/api/token'

const router = useRouter()

const loading = ref(false)

const filterForm = reactive({
  dateRange: []
})

const summary = ref<any>({})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '应用名称', dataIndex: 'appName', key: 'appName' },
  { title: '模型', dataIndex: 'modelName', key: 'modelName' },
  { title: '用途', dataIndex: 'aiCallPurpose', key: 'aiCallPurpose' },
  { title: '输入 Token', dataIndex: 'inputTokens', key: 'inputTokens' },
  { title: '输出 Token', dataIndex: 'outputTokens', key: 'outputTokens' },
  { title: '总 Token', dataIndex: 'totalTokens', key: 'totalTokens' },
  { title: '时间', dataIndex: 'createTime', key: 'createTime' }
]

const records = ref<any[]>([])

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
  try {
    const res = await getUserTokenSummary()
    summary.value = res.data || {}
  } catch (error) {
    console.error('获取统计失败:', error)
  }
}

const fetchRecords = async () => {
  loading.value = true
  try {
    const res: any = await getUserTokenDetails({
      page: pagination.current,
      pageSize: pagination.pageSize
    })
    records.value = res.data || []
    pagination.total = res.data?.length || 0
  } catch (error) {
    console.error('获取记录失败:', error)
  } finally {
    loading.value = false
  }
}

const goBack = () => { router.back() }
const handleSearch = () => { pagination.current = 1; fetchRecords() }
const handleReset = () => { filterForm.dateRange = []; handleSearch() }
const handleTableChange = (pag: any) => { pagination.current = pag.current; pagination.pageSize = pag.pageSize; fetchRecords() }

onMounted(() => { fetchSummary(); fetchRecords() })
</script>

<style scoped>
.token-details-page {
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
  padding: 16px 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
}

.stats-row { margin-bottom: 20px; }

.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
  margin-bottom: 16px;
  border-left: 3px solid transparent;
}

.stat-primary { border-left-color: #3b82f6; }
.stat-success { border-left-color: #10b981; }
.stat-warning { border-left-color: #f59e0b; }

.stat-label { font-size: 12px; color: var(--text-muted); margin-bottom: 4px; }

.stat-value { font-size: 22px; font-weight: 700; color: var(--text-primary); letter-spacing: -0.5px; }

.table-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
}

.token-value { font-weight: 600; color: var(--color-primary); }

@media (max-width: 768px) {
  .token-details-page { padding: 16px; }
  .page-header { flex-direction: column; align-items: flex-start; }
}
</style>
