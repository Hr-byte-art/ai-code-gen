<template>
  <div class="api-test-page">
    <div class="container">
      <div class="page-header">
        <a-button @click="goBack" class="back-btn" size="small">
          <ArrowLeftOutlined />
          返回
        </a-button>
        <h1 class="page-title">API 测试</h1>
      </div>

      <a-row :gutter="24">
        <!-- 左侧配置 -->
        <a-col :xs="24" :lg="10">
          <div class="config-card">
            <div class="card-header">
              <h3 class="card-title">请求配置</h3>
            </div>

            <a-form :model="requestForm" layout="vertical">
              <a-form-item label="应用 ID">
                <a-input-number v-model:value="requestForm.appId" :min="1" style="width: 100%" placeholder="输入应用 ID" />
              </a-form-item>

              <a-form-item label="消息内容">
                <a-textarea
                  v-model:value="requestForm.message"
                  :rows="5"
                  placeholder="输入要发送的消息..."
                />
              </a-form-item>

              <a-form-item>
                <a-button
                  type="primary"
                  block
                  size="large"
                  :loading="loading"
                  @click="sendRequest"
                  class="send-btn"
                >
                  <SendOutlined />
                  发送请求
                </a-button>
              </a-form-item>
            </a-form>
          </div>
        </a-col>

        <!-- 右侧结果 -->
        <a-col :xs="24" :lg="14">
          <div class="result-card">
            <div class="card-header">
              <h3 class="card-title">响应结果</h3>
              <a-button size="small" @click="clearResult" class="clear-btn">
                清空
              </a-button>
            </div>

            <div class="result-content">
              <a-empty v-if="!result" description="发送请求后查看结果" />
              <template v-else>
                <div class="result-header">
                  <a-tag :color="result.status === 'success' ? 'green' : 'red'">
                    {{ result.status === 'success' ? '成功' : '失败' }}
                  </a-tag>
                  <span class="result-time">耗时: {{ result.time }}ms</span>
                </div>

                <div class="result-section">
                  <h4 class="section-title">AI 回复</h4>
                  <div class="result-text">{{ result.content }}</div>
                </div>
              </template>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, SendOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const loading = ref(false)

const requestForm = reactive({
  appId: undefined as number | undefined,
  message: ''
})

const result = ref<any>(null)

const sendRequest = async () => {
  if (!requestForm.appId || !requestForm.message.trim()) {
    message.warning('请填写应用 ID 和消息内容')
    return
  }

  loading.value = true
  const startTime = Date.now()

  try {
    result.value = { status: 'success', time: 0, content: '' }

    const eventSource = new EventSource(
      `${import.meta.env.VITE_API_BASE_URL}/app/chat/gen/code?appId=${requestForm.appId}&message=${encodeURIComponent(requestForm.message)}`,
      { withCredentials: true }
    )

    let fullResponse = ''

    eventSource.onmessage = (event) => {
      const data = event.data
      if (data === '[DONE]') {
        eventSource.close()
        result.value.time = Date.now() - startTime
        loading.value = false
        return
      }
      fullResponse += data
      result.value.content = fullResponse
    }

    eventSource.onerror = (error) => {
      console.error('SSE错误:', error)
      eventSource.close()
      result.value.status = 'error'
      result.value.time = Date.now() - startTime
      loading.value = false
      message.error('请求失败')
    }
  } catch (error) {
    result.value = {
      status: 'error',
      time: Date.now() - startTime,
      content: '请求失败，请检查应用 ID 是否正确'
    }
    loading.value = false
    message.error('请求失败')
  }
}

const clearResult = () => { result.value = null }
const goBack = () => { router.back() }
</script>

<style scoped>
.api-test-page {
  padding: 24px;
  background: var(--bg-page);
  min-height: calc(100vh - 64px);
}

.container { max-width: 1400px; margin: 0 auto; }

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

.config-card, .result-card {
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

.clear-btn { border-radius: 8px !important; }

.send-btn { border-radius: 10px !important; font-weight: 600 !important; }

.result-content { min-height: 300px; }

.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.result-time { font-size: 13px; color: var(--text-muted); }

.result-section { margin-bottom: 16px; }

.section-title {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.result-text {
  background: #f8fafc;
  padding: 16px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-primary);
  white-space: pre-wrap;
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #f1f5f9;
}

@media (max-width: 768px) {
  .api-test-page { padding: 16px; }
  .page-header { flex-direction: column; align-items: flex-start; }
}
</style>
