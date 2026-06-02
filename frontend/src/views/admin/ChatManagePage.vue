<template>
  <div class="admin-page">
    <PageHeader title="聊天管理" description="检索平台生成对话记录，辅助定位问题链路。" eyebrow="后台" />
    <div class="filter-bar">
      <a-form layout="inline">
        <a-form-item label="消息内容"><a-input v-model:value="filter.message" placeholder="搜索" allow-clear /></a-form-item>
        <a-form-item label="应用 ID"><a-input v-model:value="filter.appId" placeholder="应用 ID" allow-clear /></a-form-item>
        <a-form-item label="用户 ID"><a-input v-model:value="filter.userId" placeholder="用户 ID" allow-clear /></a-form-item>
        <a-form-item><a-button type="primary" @click="handleSearch" size="small">查询</a-button><a-button style="margin-left:8px" @click="handleReset" size="small">重置</a-button></a-form-item>
      </a-form>
    </div>
    <div class="card">
      <a-table :columns="columns" :data-source="chatList" :pagination="pagination" :loading="loading" @change="handleTableChange" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'message'"><div class="msg-text">{{ record.message?.substring(0, 50) }}{{ record.message?.length > 50 ? '...' : '' }}</div></template>
          <template v-if="column.key === 'messageType'"><a-tag :color="record.messageType === 'user' ? 'blue' : 'green'">{{ record.messageType === 'user' ? '用户' : 'AI' }}</a-tag></template>
          <template v-if="column.key === 'createTime'">{{ ft(record.createTime) }}</template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { getAdminChatHistory } from '@/api/chat'
import { formatDateTime as ft } from '@/utils/time'

const loading = ref(false)
const filter = reactive({ message: '', appId: '', userId: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })
const columns = [
  { title: '消息', dataIndex: 'message', key: 'message', width: 280 },
  { title: '类型', dataIndex: 'messageType', key: 'messageType', width: 80 },
  { title: '应用 ID', dataIndex: 'appId', key: 'appId', width: 90 },
  { title: '用户 ID', dataIndex: 'userId', key: 'userId', width: 90 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
]
const chatList = ref<any[]>([])
const fetchChatList = async () => { loading.value = true; try { const r: any = await getAdminChatHistory({ pageNum: pagination.current, pageSize: pagination.pageSize, message: filter.message || undefined, appId: filter.appId || undefined, userId: filter.userId || undefined }); chatList.value = r.data?.records || []; pagination.total = r.data?.totalRow || 0 } catch (e) {} finally { loading.value = false } }
const handleSearch = () => { pagination.current = 1; fetchChatList() }
const handleReset = () => { filter.message = ''; filter.appId = ''; filter.userId = ''; handleSearch() }
const handleTableChange = (p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; fetchChatList() }
onMounted(() => fetchChatList())
</script>

<style scoped>
.admin-page { max-width: 1400px; margin: 0 auto; }
.filter-bar { background: var(--bg-card); border-radius: var(--r-lg); padding: 14px 16px; margin-bottom: 16px; border: 1px solid var(--border-light); }
.card { background: var(--bg-card); border-radius: var(--r-lg); padding: 16px; border: 1px solid var(--border-light); }
.msg-text { max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; color: var(--t-secondary); }
</style>
