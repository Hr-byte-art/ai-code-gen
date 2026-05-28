<template>
  <div class="admin-page">
    <PageHeader title="应用管理" description="查看平台应用资产，处理优先级和异常记录。" eyebrow="后台" />
    <div class="filter-bar">
      <a-form layout="inline">
        <a-form-item label="应用名称"><a-input v-model:value="filter.appName" placeholder="搜索" allow-clear /></a-form-item>
        <a-form-item label="用户 ID"><a-input v-model:value="filter.userId" placeholder="用户 ID" allow-clear /></a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch" size="small">查询</a-button>
          <a-button style="margin-left:8px" @click="handleReset" size="small">重置</a-button>
        </a-form-item>
      </a-form>
    </div>
    <div class="card">
      <a-table :columns="columns" :data-source="appList" :pagination="pagination" :loading="loading" @change="handleTableChange" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'app'">
            <div class="app-info"><a-avatar :size="28" :src="record.cover"><template #icon><AppstoreOutlined /></template></a-avatar><div><div class="app-name">{{ record.appName }}</div><div class="app-desc">{{ record.initPrompt?.substring(0, 25) }}...</div></div></div>
          </template>
          <template v-if="column.key === 'user'">{{ record.user?.userName || '-' }}</template>
          <template v-if="column.key === 'codeGenType'"><a-tag>{{ record.codeGenType || '默认' }}</a-tag></template>
          <template v-if="column.key === 'createTime'">{{ ft(record.createTime) }}</template>
          <template v-if="column.key === 'action'">
            <a-space><a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button><a-popconfirm title="确定删除？" @confirm="handleDelete(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm></a-space>
          </template>
        </template>
      </a-table>
    </div>
    <a-modal v-model:open="editVisible" title="编辑应用" @ok="handleUpdate">
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="应用名称"><a-input v-model:value="editForm.appName" /></a-form-item>
        <a-form-item label="优先级"><a-input-number v-model:value="editForm.priority" :min="0" :max="100" style="width:100%" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { AppstoreOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { getAdminAppList, adminUpdateApp, adminDeleteApp } from '@/api/app'

const loading = ref(false)
const editVisible = ref(false)
const filter = reactive({ appName: '', userId: '' })
const editForm = reactive({ id: 0, appName: '', priority: 0 })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })
const columns = [
  { title: '应用', dataIndex: 'app', key: 'app', width: 220 },
  { title: '用户', dataIndex: 'user', key: 'user', width: 100 },
  { title: '类型', dataIndex: 'codeGenType', key: 'codeGenType', width: 90 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 130 },
]
const appList = ref<any[]>([])
const ft = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : ''
const fetchAppList = async () => { loading.value = true; try { const r: any = await getAdminAppList({ pageNum: pagination.current, pageSize: pagination.pageSize, appName: filter.appName || undefined, userId: filter.userId || undefined }); appList.value = r.data?.records || []; pagination.total = r.data?.totalRow || 0 } catch (e) {} finally { loading.value = false } }
const handleEdit = (r: any) => { editForm.id = r.id; editForm.appName = r.appName; editForm.priority = r.priority || 0; editVisible.value = true }
const handleUpdate = async () => { try { await adminUpdateApp(editForm); message.success('更新成功'); editVisible.value = false; fetchAppList() } catch (e) { message.error('更新失败') } }
const handleDelete = async (r: any) => { try { await adminDeleteApp(r.id); message.success('删除成功'); fetchAppList() } catch (e) { message.error('删除失败') } }
const handleSearch = () => { pagination.current = 1; fetchAppList() }
const handleReset = () => { filter.appName = ''; filter.userId = ''; handleSearch() }
const handleTableChange = (p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; fetchAppList() }
onMounted(() => fetchAppList())
</script>

<style scoped>
.admin-page { max-width: 1400px; margin: 0 auto; }
.filter-bar { background: var(--bg-card); border-radius: var(--r-lg); padding: 14px 16px; margin-bottom: 16px; border: 1px solid var(--border-light); }
.card { background: var(--bg-card); border-radius: var(--r-lg); padding: 16px; border: 1px solid var(--border-light); }
.app-info { display: flex; align-items: center; gap: 8px; }
.app-name { font-size: 13px; font-weight: 700; color: var(--t-primary); }
.app-desc { font-size: 11px; color: var(--t-light); }
</style>
