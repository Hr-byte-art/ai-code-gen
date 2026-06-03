<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import request from '@/utils/request'
import { formatDateTime as formatTime } from '@/utils/time'

interface AgentTrace {
  id: string
  traceId: string
  agentName: string
  appId: number
  userId: number | null
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

interface AgentTraceSummary {
  traceId: string
  appId: number
  userId: number | null
  status: string
  finalReviewResult: string | null
  finalReviewScore: number | null
  stepCount: number
  totalDurationMs: number | null
  startTime: string | null
  endTime: string | null
  createTime: string
  hasError: boolean
}

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const summaryList = ref<AgentTraceSummary[]>([])
const detailList = ref<AgentTrace[]>([])
const activeTraceId = ref('')

const filters = reactive({
  appId: '',
  traceId: '',
  status: undefined as string | undefined,
  reviewResult: undefined as string | undefined,
})

const columns = [
  { title: '追踪ID', dataIndex: 'traceId', key: 'traceId', width: 160 },
  { title: '应用ID', dataIndex: 'appId', key: 'appId', width: 180 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '最终结果', dataIndex: 'finalReviewResult', key: 'finalReviewResult', width: 100 },
  { title: '最终评分', dataIndex: 'finalReviewScore', key: 'finalReviewScore', width: 90 },
  { title: '步骤', dataIndex: 'stepCount', key: 'stepCount', width: 70 },
  { title: '总耗时', dataIndex: 'totalDurationMs', key: 'totalDurationMs', width: 90 },
  { title: '更新时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 90 },
]

const detailTitle = computed(() => activeTraceId.value ? `链路详情：${activeTraceId.value}` : '链路详情')

const requestParams = computed(() => ({
  appId: filters.appId ? Number(filters.appId) : undefined,
  traceId: filters.traceId || undefined,
  status: filters.status,
  reviewResult: filters.reviewResult,
  limit: 300,
}))

const fetchSummaries = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/agent/trace/admin/summary', { params: requestParams.value })
    summaryList.value = res.data || []
  } catch (e) {
    message.error('获取 Agent 追踪链路失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (record: AgentTraceSummary) => {
  activeTraceId.value = record.traceId
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res: any = await request.get(`/agent/trace/admin/trace/${record.traceId}`)
    detailList.value = res.data || []
  } catch (e) {
    message.error('获取链路详情失败')
  } finally {
    detailLoading.value = false
  }
}

const resetFilters = () => {
  Object.assign(filters, {
    appId: '',
    traceId: '',
    status: undefined,
    reviewResult: undefined,
  })
  fetchSummaries()
}

const formatDuration = (durationMs?: number | null) => {
  if (!durationMs) return '-'
  return `${(durationMs / 1000).toFixed(1)}s`
}

const getStatusColor = (status?: string | null) => {
  if (status === 'success') return 'green'
  if (status === 'error' || status === 'timeout') return 'red'
  return 'default'
}

const getReviewColor = (reviewResult?: string | null) => {
  if (reviewResult === 'passed') return 'green'
  if (reviewResult === 'failed') return 'red'
  return 'default'
}

const getAgentColor = (agentName?: string | null) => {
  if (agentName === 'reviewer' || agentName === 'review') return 'blue'
  if (agentName === 'optimizer') return 'orange'
  return 'purple'
}

const getScoreColor = (score?: number | null) => {
  if (score == null) return 'var(--t-muted)'
  return score >= 80 ? 'var(--c-success)' : 'var(--c-error)'
}

const parseIssues = (issues?: string | null) => {
  if (!issues) return []
  try {
    const parsed = JSON.parse(issues)
    return Array.isArray(parsed) ? parsed : [String(parsed)]
  } catch (e) {
    return [issues]
  }
}

onMounted(() => fetchSummaries())
</script>

<template>
  <div class="admin-page">
    <PageHeader title="Agent 追踪" description="按一次生成链路查看代码审查和优化 Agent 的执行过程。" eyebrow="后台" />

    <div class="card filter-card">
      <a-form layout="inline" :model="filters" class="filter-form">
        <a-form-item label="应用ID">
          <a-input v-model:value="filters.appId" allow-clear placeholder="输入 appId" class="filter-input" />
        </a-form-item>
        <a-form-item label="追踪ID">
          <a-input v-model:value="filters.traceId" allow-clear placeholder="输入 traceId" class="trace-input" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="filters.status" allow-clear placeholder="全部" class="filter-select">
            <a-select-option value="success">success</a-select-option>
            <a-select-option value="error">error</a-select-option>
            <a-select-option value="timeout">timeout</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="审查结果">
          <a-select v-model:value="filters.reviewResult" allow-clear placeholder="全部" class="filter-select">
            <a-select-option value="passed">通过</a-select-option>
            <a-select-option value="failed">未通过</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" :loading="loading" @click="fetchSummaries">查询</a-button>
            <a-button @click="resetFilters">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>

    <div class="card">
      <a-table
        row-key="traceId"
        :columns="columns"
        :data-source="summaryList"
        :loading="loading"
        size="small"
        :pagination="{ pageSize: 20 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'traceId'">
            <span class="mono">{{ record.traceId }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-if="column.key === 'finalReviewResult'">
            <a-tag v-if="record.finalReviewResult" :color="getReviewColor(record.finalReviewResult)">
              {{ record.finalReviewResult === 'passed' ? '通过' : '未通过' }}
            </a-tag>
            <span v-else class="muted">-</span>
          </template>
          <template v-if="column.key === 'finalReviewScore'">
            <span :style="{ color: getScoreColor(record.finalReviewScore) }">
              {{ record.finalReviewScore ?? '-' }}
            </span>
          </template>
          <template v-if="column.key === 'stepCount'">
            <a-tag color="blue">{{ record.stepCount }} 步</a-tag>
          </template>
          <template v-if="column.key === 'totalDurationMs'">
            <span>{{ formatDuration(record.totalDurationMs) }}</span>
          </template>
          <template v-if="column.key === 'createTime'">
            <span>{{ formatTime(record.createTime) }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record)">查看详情</a-button>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal v-model:open="detailVisible" :title="detailTitle" width="820px" :footer="null">
      <a-spin :spinning="detailLoading">
        <a-timeline class="trace-timeline">
          <a-timeline-item
            v-for="(trace, index) in detailList"
            :key="trace.id"
            :color="trace.status === 'success' ? 'green' : 'red'"
          >
            <div class="step-card">
              <div class="step-head">
                <a-space>
                  <a-tag color="geekblue">Step {{ index + 1 }}</a-tag>
                  <a-tag :color="getAgentColor(trace.agentName)">{{ trace.agentName }}</a-tag>
                  <a-tag :color="getStatusColor(trace.status)">{{ trace.status }}</a-tag>
                  <a-tag v-if="trace.reviewResult" :color="getReviewColor(trace.reviewResult)">
                    {{ trace.reviewResult === 'passed' ? '通过' : '未通过' }}
                  </a-tag>
                </a-space>
                <span class="muted">{{ formatDuration(trace.durationMs) }}</span>
              </div>

              <div class="step-meta">
                <span>模型：{{ trace.modelName || '-' }}</span>
                <span>评分：<b :style="{ color: getScoreColor(trace.reviewScore) }">{{ trace.reviewScore ?? '-' }}</b></span>
                <span>时间：{{ formatTime(trace.createTime) }}</span>
              </div>

              <div v-if="parseIssues(trace.issues).length" class="issues-block">
                <div class="issues-title">问题列表</div>
                <ul class="issues-list">
                  <li v-for="issue in parseIssues(trace.issues)" :key="issue">{{ issue }}</li>
                </ul>
              </div>

              <a-alert
                v-if="trace.errorMessage"
                type="error"
                show-icon
                :message="trace.errorMessage"
                class="error-alert"
              />
            </div>
          </a-timeline-item>
        </a-timeline>
        <a-empty v-if="!detailLoading && detailList.length === 0" description="暂无链路详情" />
      </a-spin>
    </a-modal>
  </div>
</template>

<style scoped>
.admin-page { padding: 30px 24px 48px; max-width: 1400px; margin: 0 auto; }
.card { background: var(--bg-card); border-radius: var(--r-xl); padding: 18px; border: 1px solid var(--border-light); }
.filter-card { margin-bottom: 16px; }
.filter-form { row-gap: 12px; }
.filter-input { width: 160px; }
.trace-input { width: 210px; }
.filter-select { width: 130px; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; }
.muted { color: var(--t-muted); }
.trace-timeline { margin-top: 8px; padding-top: 8px; }
.step-card { border: 1px solid var(--border-light); border-radius: var(--r-lg); padding: 14px; background: var(--bg-card); }
.step-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.step-meta { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 10px; color: var(--t-secondary); font-size: 13px; }
.issues-block { margin-top: 12px; padding: 12px; border-radius: var(--r-md); background: rgba(250, 173, 20, 0.08); }
.issues-title { font-weight: 700; margin-bottom: 8px; color: var(--t-primary); }
.issues-list { margin: 0; padding-left: 18px; color: var(--t-secondary); }
.error-alert { margin-top: 12px; }
</style>
