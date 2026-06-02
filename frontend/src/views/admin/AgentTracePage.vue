<template>
  <div class="admin-page">
    <PageHeader title="Agent 追踪" description="查看代码审查和优化 Agent 的执行记录。" eyebrow="后台" />

    <div class="card">
      <a-table :columns="columns" :data-source="traceList" :loading="loading" size="small" :pagination="{ pageSize: 20 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'agentName'">
            <a-tag :color="record.agentName === 'review' ? 'blue' : 'orange'">{{ record.agentName }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'success' ? 'green' : 'red'">{{ record.status }}</a-tag>
          </template>
          <template v-if="column.key === 'reviewResult'">
            <a-tag v-if="record.reviewResult === 'passed'" color="green">通过</a-tag>
            <a-tag v-else-if="record.reviewResult === 'failed'" color="red">未通过</a-tag>
            <span v-else style="color:var(--t-muted)">-</span>
          </template>
          <template v-if="column.key === 'reviewScore'">
            <span v-if="record.reviewScore != null" :style="{ color: record.reviewScore >= 80 ? 'var(--c-success)' : 'var(--c-error)' }">
              {{ record.reviewScore }}
            </span>
            <span v-else style="color:var(--t-muted)">-</span>
          </template>
          <template v-if="column.key === 'durationMs'">
            <span>{{ record.durationMs ? (record.durationMs / 1000).toFixed(1) + 's' : '-' }}</span>
          </template>
          <template v-if="column.key === 'createTime'">
            <span>{{ formatTime(record.createTime) }}</span>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/common/PageHeader.vue'
import request from '@/utils/request'
import { formatDateTime as formatTime } from '@/utils/time'

interface AgentTrace {
  id: string
  traceId: string
  agentName: string
  appId: number
  userId: number
  modelName: string
  inputTokens: number
  outputTokens: number
  status: string
  reviewResult: string | null
  reviewScore: number | null
  issues: string | null
  startTime: string
  endTime: string
  durationMs: number | null
  errorMessage: string | null
  createTime: string
}

const loading = ref(false)
const traceList = ref<AgentTrace[]>([])

const columns = [
  { title: 'Agent', dataIndex: 'agentName', key: 'agentName', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '审查结果', dataIndex: 'reviewResult', key: 'reviewResult', width: 90 },
  { title: '评分', dataIndex: 'reviewScore', key: 'reviewScore', width: 70 },
  { title: '耗时', dataIndex: 'durationMs', key: 'durationMs', width: 80 },
  { title: '模型', dataIndex: 'modelName', key: 'modelName', width: 120 },
  { title: '应用ID', dataIndex: 'appId', key: 'appId', width: 180 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 150 },
  { title: '错误', dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true },
]

const fetchList = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/agent/trace/admin/list', { params: { limit: 200 } })
    traceList.value = res.data || []
  } catch (e) {} finally { loading.value = false }
}

onMounted(() => fetchList())
</script>

<style scoped>
.admin-page { padding: 30px 24px 48px; max-width: 1400px; margin: 0 auto; }
.card { background: var(--bg-card); border-radius: var(--r-xl); padding: 18px; border: 1px solid var(--border-light); }
</style>
