<template>
  <div class="page">
    <PageHeader title="API 测试" description="测试聊天 API 接口">
      <template #actions>
        <a-button @click="router.back()" size="small"><ArrowLeftOutlined /> 返回</a-button>
      </template>
    </PageHeader>

    <a-row :gutter="20">
      <a-col :xs="24" :lg="10">
        <div class="card">
          <div class="card-head"><h3 class="card-title">请求配置</h3></div>
          <a-form :model="form" layout="vertical">
            <a-form-item label="应用 ID"><a-input v-model:value="form.appId" style="width:100%" placeholder="输入应用 ID" /></a-form-item>
            <a-form-item label="消息内容"><a-textarea v-model:value="form.message" :rows="5" placeholder="输入要发送的消息..." /></a-form-item>
            <a-form-item><a-button type="primary" block size="large" :loading="loading" @click="send" class="send-btn"><SendOutlined /> 发送请求</a-button></a-form-item>
          </a-form>
        </div>
      </a-col>
      <a-col :xs="24" :lg="14">
        <div class="card">
          <div class="card-head"><h3 class="card-title">响应结果</h3><a-button size="small" @click="result = null">清空</a-button></div>
          <div class="result">
            <a-empty v-if="!result" description="发送请求后查看结果" />
            <template v-else>
              <div class="result-head"><a-tag :color="result.status === 'success' ? 'green' : 'red'">{{ result.status === 'success' ? '成功' : '失败' }}</a-tag><span class="result-time">耗时: {{ result.time }}ms</span></div>
              <h4 class="result-label">AI 回复</h4>
              <div class="result-text">{{ result.content }}</div>
            </template>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, SendOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { buildApiUrl } from '@/utils/apiBase'

const router = useRouter()
const loading = ref(false)
const form = reactive({ appId: undefined as string | undefined, message: '' })
const result = ref<any>(null)

const send = async () => {
  if (!form.appId || !form.message.trim()) { message.warning('请填写应用 ID 和消息内容'); return }
  loading.value = true; const t0 = Date.now()
  try {
    result.value = { status: 'success', time: 0, content: '' }
    const es = new EventSource(buildApiUrl(`/app/chat/gen/code?appId=${form.appId}&message=${encodeURIComponent(form.message)}`), { withCredentials: true })
    let full = ''
    es.onmessage = (e) => { if (e.data === '[DONE]') { es.close(); result.value.time = Date.now() - t0; loading.value = false; return }; full += e.data; result.value.content = full }
    es.onerror = () => {
      const errorMessage = '流式连接失败，请确认应用 ID、登录状态和后端服务是否正常'
      es.close()
      result.value.status = 'error'
      result.value.time = Date.now() - t0
      result.value.content = errorMessage
      loading.value = false
      message.error(errorMessage)
    }
  } catch (e) {
    result.value = { status: 'error', time: Date.now() - t0, content: '创建流式连接失败，请稍后重试' }
    loading.value = false
  }
}
</script>

<style scoped>
.page { padding: 28px 24px; max-width: 1400px; margin: 0 auto; }
.card { background: var(--bg-card); border-radius: var(--r-lg); padding: 18px; border: 1px solid var(--border-light); margin-bottom: 16px; }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.card-title { margin: 0; font-size: 14px; font-weight: 700; color: var(--t-primary); }
.send-btn { border-radius: var(--r-lg) !important; font-weight: 700 !important; }
.result { min-height: 300px; }
.result-head { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.result-time { font-size: 13px; color: var(--t-muted); }
.result-label { margin: 0 0 8px; font-size: 13px; font-weight: 700; color: var(--t-secondary); }
.result-text { background: var(--c-primary-50); padding: 14px; border-radius: var(--r-lg); font-size: 13px; line-height: 1.7; color: var(--t-primary); white-space: pre-wrap; max-height: 400px; overflow-y: auto; border: 1px solid var(--c-primary-200); }
@media (max-width: 768px) { .page { padding: 20px 16px; } }
</style>
