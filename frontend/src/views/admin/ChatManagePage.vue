<template>
  <div class="chat-manage-page">
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">聊天管理</h1>
          <span class="page-subtitle">查看平台所有聊天记录</span>
        </div>
      </div>

      <div class="filter-card">
        <a-form layout="inline" :model="filterForm">
          <a-form-item label="消息内容">
            <a-input v-model:value="filterForm.message" placeholder="搜索消息内容" allow-clear />
          </a-form-item>
          <a-form-item label="应用 ID">
            <a-input v-model:value="filterForm.appId" placeholder="应用 ID" allow-clear />
          </a-form-item>
          <a-form-item label="用户 ID">
            <a-input v-model:value="filterForm.userId" placeholder="用户 ID" allow-clear />
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

      <div class="table-card">
        <a-table :columns="columns" :data-source="chatList" :pagination="pagination" :loading="loading" @change="handleTableChange" size="middle">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'message'">
              <div class="message-content">
                {{ record.message?.substring(0, 50) }}{{ record.message?.length > 50 ? '...' : '' }}
              </div>
            </template>
            <template v-if="column.key === 'messageType'">
              <a-tag :color="record.messageType === 'user' ? 'blue' : 'green'">
                {{ record.messageType === 'user' ? '用户' : 'AI' }}
              </a-tag>
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
import { SearchOutlined } from '@ant-design/icons-vue'
import { getAdminChatHistory } from '@/api/chat'

const loading = ref(false)

const filterForm = reactive({ message: '', appId: '', userId: '' })

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '消息内容', dataIndex: 'message', key: 'message', width: 300 },
  { title: '类型', dataIndex: 'messageType', key: 'messageType', width: 100 },
  { title: '应用 ID', dataIndex: 'appId', key: 'appId', width: 100 },
  { title: '用户 ID', dataIndex: 'userId', key: 'userId', width: 100 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 180 }
]

const chatList = ref<any[]>([])

const formatTime = (time: string) => { if (!time) return ''; return new Date(time).toLocaleString('zh-CN') }

const fetchChatList = async () => {
  loading.value = true
  try {
    const res: any = await getAdminChatHistory({
      pageNum: pagination.current, pageSize: pagination.pageSize,
      message: filterForm.message || undefined,
      appId: filterForm.appId ? Number(filterForm.appId) : undefined,
      userId: filterForm.userId ? Number(filterForm.userId) : undefined
    })
    chatList.value = res.data?.records || []
    pagination.total = res.data?.totalRow || 0
  } catch (error) { console.error('获取聊天列表失败:', error) } finally { loading.value = false }
}

const handleSearch = () => { pagination.current = 1; fetchChatList() }
const handleReset = () => { filterForm.message = ''; filterForm.appId = ''; filterForm.userId = ''; handleSearch() }
const handleTableChange = (pag: any) => { pagination.current = pag.current; pagination.pageSize = pag.pageSize; fetchChatList() }

onMounted(() => { fetchChatList() })
</script>

<style scoped>
.chat-manage-page {
  padding: 24px;
  background: var(--bg-page);
  min-height: calc(100vh - 64px);
}

.container { max-width: 1400px; margin: 0 auto; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-left { display: flex; flex-direction: column; gap: 2px; }

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.page-subtitle { font-size: 14px; color: var(--text-muted); }

.filter-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
}

.table-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
}

.message-content {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .chat-manage-page { padding: 16px; }
}
</style>
