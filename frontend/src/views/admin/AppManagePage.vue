<template>
  <div class="app-manage-page">
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">应用管理</h1>
          <span class="page-subtitle">管理平台所有应用</span>
        </div>
      </div>

      <div class="filter-card">
        <a-form layout="inline" :model="filterForm">
          <a-form-item label="应用名称">
            <a-input v-model:value="filterForm.appName" placeholder="搜索应用名称" allow-clear />
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
        <a-table :columns="columns" :data-source="appList" :pagination="pagination" :loading="loading" @change="handleTableChange" size="middle">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'app'">
              <div class="app-info">
                <a-avatar :size="36" :src="record.cover">
                  <template #icon><AppstoreOutlined /></template>
                </a-avatar>
                <div class="app-details">
                  <div class="app-name">{{ record.appName }}</div>
                  <div class="app-desc">{{ record.initPrompt?.substring(0, 30) }}...</div>
                </div>
              </div>
            </template>
            <template v-if="column.key === 'user'">
              {{ record.user?.userName || '-' }}
            </template>
            <template v-if="column.key === 'codeGenType'">
              <a-tag>{{ record.codeGenType || '默认' }}</a-tag>
            </template>
            <template v-if="column.key === 'createTime'">
              {{ formatTime(record.createTime) }}
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
                <a-popconfirm title="确定要删除此应用吗？" @confirm="handleDelete(record)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>

      <a-modal v-model:open="editModalVisible" title="编辑应用" @ok="handleUpdateApp" @cancel="editModalVisible = false">
        <a-form :model="editForm" layout="vertical">
          <a-form-item label="应用名称">
            <a-input v-model:value="editForm.appName" placeholder="应用名称" />
          </a-form-item>
          <a-form-item label="优先级">
            <a-input-number v-model:value="editForm.priority" :min="0" :max="100" style="width: 100%" />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, AppstoreOutlined } from '@ant-design/icons-vue'
import { getAdminAppList, adminUpdateApp, adminDeleteApp } from '@/api/app'

const loading = ref(false)
const editModalVisible = ref(false)

const filterForm = reactive({ appName: '', userId: '' })
const editForm = reactive({ id: 0, appName: '', priority: 0 })

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '应用信息', dataIndex: 'app', key: 'app', width: 250 },
  { title: '用户', dataIndex: 'user', key: 'user', width: 120 },
  { title: '类型', dataIndex: 'codeGenType', key: 'codeGenType', width: 100 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 150 }
]

const appList = ref<any[]>([])

const formatTime = (time: string) => { if (!time) return ''; return new Date(time).toLocaleString('zh-CN') }

const fetchAppList = async () => {
  loading.value = true
  try {
    const res: any = await getAdminAppList({
      pageNum: pagination.current, pageSize: pagination.pageSize,
      appName: filterForm.appName || undefined,
      userId: filterForm.userId ? Number(filterForm.userId) : undefined
    })
    appList.value = res.data?.records || []
    pagination.total = res.data?.totalRow || 0
  } catch (error) { console.error('获取应用列表失败:', error) } finally { loading.value = false }
}

const handleEdit = (record: any) => {
  editForm.id = record.id; editForm.appName = record.appName; editForm.priority = record.priority || 0
  editModalVisible.value = true
}

const handleUpdateApp = async () => {
  try { await adminUpdateApp(editForm); message.success('更新成功'); editModalVisible.value = false; fetchAppList() }
  catch (error) { message.error('更新失败') }
}

const handleDelete = async (record: any) => {
  try { await adminDeleteApp(record.id); message.success('删除成功'); fetchAppList() }
  catch (error) { message.error('删除失败') }
}

const handleSearch = () => { pagination.current = 1; fetchAppList() }
const handleReset = () => { filterForm.appName = ''; filterForm.userId = ''; handleSearch() }
const handleTableChange = (pag: any) => { pagination.current = pag.current; pagination.pageSize = pag.pageSize; fetchAppList() }

onMounted(() => { fetchAppList() })
</script>

<style scoped>
.app-manage-page {
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

.app-info { display: flex; align-items: center; gap: 10px; }
.app-details { min-width: 0; }
.app-name { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.app-desc { font-size: 11px; color: var(--text-muted); }

@media (max-width: 768px) {
  .app-manage-page { padding: 16px; }
}
</style>
