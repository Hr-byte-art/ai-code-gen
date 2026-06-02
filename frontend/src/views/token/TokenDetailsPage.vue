<template>
  <div class="page">
    <PageHeader title="消耗明细" description="按时间查看每一次模型调用的输入、输出和总消耗。" eyebrow="资源账单">
      <template #actions>
        <a-button @click="router.back()" size="small"><ArrowLeftOutlined /> 返回</a-button>
      </template>
    </PageHeader>

    <section class="filter-bar">
      <a-form layout="inline">
        <a-form-item label="时间范围"><a-range-picker v-model:value="filterForm.dateRange" /></a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch" size="small">查询</a-button>
          <a-button style="margin-left:8px" @click="handleReset" size="small">重置</a-button>
        </a-form-item>
      </a-form>
    </section>

    <section class="mini-summary">
      <div class="mini-item"><span>总 Token</span><strong>{{ fmtNum(summary.totalTokens || 0) }}</strong></div>
      <div class="mini-item"><span>输入</span><strong>{{ fmtNum(summary.totalInputTokens || 0) }}</strong></div>
      <div class="mini-item"><span>输出</span><strong>{{ fmtNum(summary.totalOutputTokens || 0) }}</strong></div>
    </section>

    <section class="card">
      <div class="card-head">
        <div>
          <h3 class="card-title">调用明细</h3>
          <p class="card-desc">这里按每次 AI 调用用途记录，同一个应用可能包含安全检查、路由、代码生成等多条账单，不等同于应用数量。</p>
        </div>
      </div>
      <a-table :columns="columns" :data-source="records" :pagination="pagination" :loading="loading" @change="handleTableChange" size="small">
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { getUserTokenSummary, getUserTokenDetails } from '@/api/token'
import { formatDateTime as formatTime } from '@/utils/time'

const router = useRouter()
const loading = ref(false)
const filterForm = reactive({ dateRange: [] })
const summary = ref<any>({})
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })
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
const records = ref<any[]>([])
const fmtNum = (n: number) => n >= 1e6 ? (n / 1e6).toFixed(1) + 'M' : n >= 1e3 ? (n / 1e3).toFixed(1) + 'K' : String(n)
const fetchSummary = async () => { try { const r = await getUserTokenSummary(); summary.value = r.data || {} } catch (e) {} }
const fetchRecords = async () => { loading.value = true; try { const r: any = await getUserTokenDetails({ page: pagination.current, pageSize: pagination.pageSize }); records.value = r.data || []; pagination.total = r.data?.length || 0 } catch (e) {} finally { loading.value = false } }
const handleSearch = () => { pagination.current = 1; fetchRecords() }
const handleReset = () => { filterForm.dateRange = []; handleSearch() }
const handleTableChange = (p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; fetchRecords() }
onMounted(() => { fetchSummary(); fetchRecords() })
</script>

<style scoped>
.page { padding: 30px 24px 48px; max-width: 1280px; margin: 0 auto; }
.filter-bar { background: var(--bg-card); border-radius: var(--r-lg); padding: 14px 16px; margin-bottom: 16px; border: 1px solid var(--border-light); }
.mini-summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1px; overflow: hidden; margin-bottom: 16px; border: 1px solid var(--border-light); border-radius: var(--r-xl); background: var(--border-light); }
.mini-item { padding: 16px; background: var(--bg-card); }
.mini-item span { display: block; color: var(--t-muted); font-size: 12px; font-weight: 750; margin-bottom: 5px; }
.mini-item strong { color: var(--t-primary); font-size: 22px; font-weight: 850; }
.card { background: var(--bg-card); border-radius: var(--r-xl); padding: 16px; border: 1px solid var(--border-light); }
.card-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px; }
.card-title { margin: 0; font-size: 14px; font-weight: 850; color: var(--t-primary); }
.card-desc { margin: 4px 0 0; color: var(--t-muted); font-size: 12px; line-height: 1.6; }
:deep(.ant-table-thead > tr > th),
:deep(.ant-table-tbody > tr > td) { text-align: center; }
.tv { font-weight: 800; color: var(--c-primary); }
@media (max-width: 768px) { .page { padding: 22px 16px 36px; } .mini-summary { grid-template-columns: 1fr; } }
</style>