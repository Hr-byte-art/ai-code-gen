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
          <template v-if="column.key === 'featured'">
            <a-tag :color="isFeatured(record) ? 'green' : 'default'">{{ isFeatured(record) ? '展示中' : '未展示' }}</a-tag>
          </template>
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
        <a-form-item label="首页公开样本">
          <a-switch
            v-model:checked="editForm.featured"
            checked-children="展示"
            un-checked-children="隐藏"
            :disabled="!canFeature(editForm) && !editForm.featured"
          />
          <div class="form-help">
            {{ canFeature(editForm) ? '开启后会出现在首页“公开样本”区域。' : '应用必须已上线并具备可访问地址后，才能设为首页公开样本。' }}
          </div>
        </a-form-item>
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
import { formatDateTime as ft } from '@/utils/time'

const loading = ref(false)
const editVisible = ref(false)
const GOOD_APP_PRIORITY = 99
const filter = reactive({ appName: '', userId: '' })
const editForm = reactive({ id: '', appName: '', featured: false, deployedTime: '', deployKey: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })
const columns = [
  { title: '应用', dataIndex: 'app', key: 'app', width: 220 },
  { title: '用户', dataIndex: 'user', key: 'user', width: 100 },
  { title: '类型', dataIndex: 'codeGenType', key: 'codeGenType', width: 90 },
  { title: '首页展示', dataIndex: 'featured', key: 'featured', width: 100 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 130 },
]
const appList = ref<any[]>([])
const isFeatured = (app: any) => Number(app?.priority || 0) === GOOD_APP_PRIORITY
const canFeature = (app: any) => Boolean(app?.deployedTime && app?.deployKey)
const fetchAppList = async () => { loading.value = true; try { const r: any = await getAdminAppList({ pageNum: pagination.current, pageSize: pagination.pageSize, appName: filter.appName || undefined, userId: filter.userId || undefined }); appList.value = r.data?.records || []; pagination.total = r.data?.totalRow || 0 } catch (e) {} finally { loading.value = false } }
const handleEdit = (r: any) => { editForm.id = String(r.id); editForm.appName = r.appName; editForm.featured = isFeatured(r); editForm.deployedTime = r.deployedTime || ''; editForm.deployKey = r.deployKey || ''; editVisible.value = true }
const handleUpdate = async () => { if (editForm.featured && !canFeature(editForm)) { message.warning('未上线或不可预览的应用不能设为首页公开样本'); return } try { await adminUpdateApp({ id: editForm.id, appName: editForm.appName, priority: editForm.featured ? GOOD_APP_PRIORITY : 0 }); message.success('更新成功'); editVisible.value = false; fetchAppList() } catch (e) {} }
const handleDelete = async (r: any) => { try { await adminDeleteApp(r.id); message.success('删除成功'); fetchAppList() } catch (e) {} }
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
