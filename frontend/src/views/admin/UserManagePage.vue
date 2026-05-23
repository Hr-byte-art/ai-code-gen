<template>
  <div class="user-manage-page">
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">用户管理</h1>
          <span class="page-subtitle">管理平台所有用户</span>
        </div>
        <a-button type="primary" @click="showAddModal" class="add-btn">
          <PlusOutlined />
          添加用户
        </a-button>
      </div>

      <div class="filter-card">
        <a-form layout="inline" :model="filterForm">
          <a-form-item label="用户名">
            <a-input v-model:value="filterForm.userName" placeholder="搜索用户名" allow-clear />
          </a-form-item>
          <a-form-item label="账号">
            <a-input v-model:value="filterForm.userAccount" placeholder="搜索账号" allow-clear />
          </a-form-item>
          <a-form-item label="角色">
            <a-select v-model:value="filterForm.userRole" placeholder="全部角色" allow-clear style="width: 120px">
              <a-select-option value="admin">管理员</a-select-option>
              <a-select-option value="user">普通用户</a-select-option>
            </a-select>
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
        <a-table :columns="columns" :data-source="userList" :pagination="pagination" :loading="loading" @change="handleTableChange" size="middle">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'user'">
              <div class="user-info">
                <a-avatar :size="36" :src="record.userAvatar">
                  <template #icon><UserOutlined /></template>
                </a-avatar>
                <div class="user-details">
                  <div class="username">{{ record.userName }}</div>
                  <div class="email">{{ record.userAccount }}</div>
                </div>
              </div>
            </template>
            <template v-if="column.key === 'userRole'">
              <a-tag :color="record.userRole === 'admin' ? 'red' : 'blue'">
                {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'createTime'">
              {{ formatTime(record.createTime) }}
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-popconfirm title="确定要删除此用户吗？" @confirm="handleDelete(record)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>

      <a-modal v-model:open="addModalVisible" title="添加用户" @ok="handleAddUser" @cancel="addModalVisible = false">
        <a-form :model="addForm" layout="vertical">
          <a-form-item label="用户名" required>
            <a-input v-model:value="addForm.userName" placeholder="请输入用户名" />
          </a-form-item>
          <a-form-item label="账号" required>
            <a-input v-model:value="addForm.userAccount" placeholder="请输入账号" />
          </a-form-item>
          <a-form-item label="角色">
            <a-select v-model:value="addForm.userRole">
              <a-select-option value="user">普通用户</a-select-option>
              <a-select-option value="admin">管理员</a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, UserOutlined } from '@ant-design/icons-vue'
import { getUserList, createUser, deleteUser } from '@/api/user'

const loading = ref(false)
const addModalVisible = ref(false)

const filterForm = reactive({ userName: '', userAccount: '', userRole: undefined })
const addForm = reactive({ userName: '', userAccount: '', userRole: 'user' })

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '用户信息', dataIndex: 'user', key: 'user', width: 200 },
  { title: '角色', dataIndex: 'userRole', key: 'userRole', width: 100 },
  { title: '积分', dataIndex: 'integral', key: 'integral', width: 100 },
  { title: '注册时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 150 }
]

const userList = ref<any[]>([])

const formatTime = (time: string) => { if (!time) return ''; return new Date(time).toLocaleString('zh-CN') }

const fetchUserList = async () => {
  loading.value = true
  try {
    const res: any = await getUserList({
      pageNum: pagination.current, pageSize: pagination.pageSize,
      userName: filterForm.userName || undefined,
      userAccount: filterForm.userAccount || undefined,
      userRole: filterForm.userRole || undefined
    })
    userList.value = res.data?.records || []
    pagination.total = res.data?.totalRow || 0
  } catch (error) { console.error('获取用户列表失败:', error) } finally { loading.value = false }
}

const showAddModal = () => {
  addForm.userName = ''; addForm.userAccount = ''; addForm.userRole = 'user'
  addModalVisible.value = true
}

const handleAddUser = async () => {
  if (!addForm.userName || !addForm.userAccount) { message.warning('请填写完整信息'); return }
  try { await createUser(addForm); message.success('添加成功'); addModalVisible.value = false; fetchUserList() }
  catch (error) { message.error('添加失败') }
}

const handleDelete = async (record: any) => {
  try { await deleteUser(record.id); message.success('删除成功'); fetchUserList() }
  catch (error) { message.error('删除失败') }
}

const handleSearch = () => { pagination.current = 1; fetchUserList() }
const handleReset = () => { filterForm.userName = ''; filterForm.userAccount = ''; filterForm.userRole = undefined; handleSearch() }
const handleTableChange = (pag: any) => { pagination.current = pag.current; pagination.pageSize = pag.pageSize; fetchUserList() }

onMounted(() => { fetchUserList() })
</script>

<style scoped>
.user-manage-page {
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

.add-btn { border-radius: 10px !important; }

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

.user-info { display: flex; align-items: center; gap: 10px; }
.user-details { min-width: 0; }
.username { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.email { font-size: 11px; color: var(--text-muted); }

@media (max-width: 768px) {
  .user-manage-page { padding: 16px; }
  .page-header { flex-direction: column; gap: 12px; }
}
</style>
