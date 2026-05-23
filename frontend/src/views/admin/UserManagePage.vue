<template>
  <div class="admin-page">
    <PageHeader title="用户管理" description="维护账号、角色和积分信息。" eyebrow="后台">
      <template #actions>
        <a-button type="primary" @click="showAdd" size="small"><PlusOutlined /> 添加用户</a-button>
      </template>
    </PageHeader>
    <div class="filter-bar">
      <a-form layout="inline">
        <a-form-item label="用户名"><a-input v-model:value="filter.userName" placeholder="搜索" allow-clear /></a-form-item>
        <a-form-item label="账号"><a-input v-model:value="filter.userAccount" placeholder="搜索" allow-clear /></a-form-item>
        <a-form-item label="角色"><a-select v-model:value="filter.userRole" placeholder="全部" allow-clear style="width:110px"><a-select-option value="admin">管理员</a-select-option><a-select-option value="user">普通用户</a-select-option></a-select></a-form-item>
        <a-form-item><a-button type="primary" @click="handleSearch" size="small">查询</a-button><a-button style="margin-left:8px" @click="handleReset" size="small">重置</a-button></a-form-item>
      </a-form>
    </div>
    <div class="card">
      <a-table :columns="columns" :data-source="userList" :pagination="pagination" :loading="loading" @change="handleTableChange" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'user'"><div class="u-info"><a-avatar :size="28" :src="record.userAvatar"><template #icon><UserOutlined /></template></a-avatar><div><div class="u-name">{{ record.userName }}</div><div class="u-acct">{{ record.userAccount }}</div></div></div></template>
          <template v-if="column.key === 'userRole'"><a-tag :color="record.userRole === 'admin' ? 'red' : 'blue'">{{ record.userRole === 'admin' ? '管理员' : '普通用户' }}</a-tag></template>
          <template v-if="column.key === 'createTime'">{{ ft(record.createTime) }}</template>
          <template v-if="column.key === 'action'"><a-popconfirm title="确定删除？" @confirm="handleDelete(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm></template>
        </template>
      </a-table>
    </div>
    <a-modal v-model:open="addVisible" title="添加用户" @ok="handleAdd">
      <a-form :model="addForm" layout="vertical">
        <a-form-item label="用户名" required><a-input v-model:value="addForm.userName" /></a-form-item>
        <a-form-item label="账号" required><a-input v-model:value="addForm.userAccount" /></a-form-item>
        <a-form-item label="角色"><a-select v-model:value="addForm.userRole"><a-select-option value="user">普通用户</a-select-option><a-select-option value="admin">管理员</a-select-option></a-select></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, UserOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { getUserList, createUser, deleteUser } from '@/api/user'

const loading = ref(false)
const addVisible = ref(false)
const filter = reactive({ userName: '', userAccount: '', userRole: undefined })
const addForm = reactive({ userName: '', userAccount: '', userRole: 'user' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })
const columns = [
  { title: '用户', dataIndex: 'user', key: 'user', width: 180 },
  { title: '角色', dataIndex: 'userRole', key: 'userRole', width: 90 },
  { title: '积分', dataIndex: 'integral', key: 'integral', width: 80 },
  { title: '注册时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 100 },
]
const userList = ref<any[]>([])
const ft = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : ''
const fetchUserList = async () => { loading.value = true; try { const r: any = await getUserList({ pageNum: pagination.current, pageSize: pagination.pageSize, userName: filter.userName || undefined, userAccount: filter.userAccount || undefined, userRole: filter.userRole || undefined }); userList.value = r.data?.records || []; pagination.total = r.data?.totalRow || 0 } catch (e) {} finally { loading.value = false } }
const showAdd = () => { addForm.userName = ''; addForm.userAccount = ''; addForm.userRole = 'user'; addVisible.value = true }
const handleAdd = async () => { if (!addForm.userName || !addForm.userAccount) { message.warning('请填写完整'); return }; try { await createUser(addForm); message.success('添加成功'); addVisible.value = false; fetchUserList() } catch (e) { message.error('添加失败') } }
const handleDelete = async (r: any) => { try { await deleteUser(r.id); message.success('删除成功'); fetchUserList() } catch (e) { message.error('删除失败') } }
const handleSearch = () => { pagination.current = 1; fetchUserList() }
const handleReset = () => { filter.userName = ''; filter.userAccount = ''; filter.userRole = undefined; handleSearch() }
const handleTableChange = (p: any) => { pagination.current = p.current; pagination.pageSize = p.pageSize; fetchUserList() }
onMounted(() => fetchUserList())
</script>

<style scoped>
.admin-page { max-width: 1400px; margin: 0 auto; }
.filter-bar { background: var(--bg-card); border-radius: var(--r-lg); padding: 14px 16px; margin-bottom: 16px; border: 1px solid var(--border-light); }
.card { background: var(--bg-card); border-radius: var(--r-lg); padding: 16px; border: 1px solid var(--border-light); }
.u-info { display: flex; align-items: center; gap: 8px; }
.u-name { font-size: 13px; font-weight: 700; color: var(--t-primary); }
.u-acct { font-size: 11px; color: var(--t-light); }
</style>
