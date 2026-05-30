<template>
  <div class="admin-page">
    <PageHeader title="MCP 服务管理" description="管理外部 MCP (Model Context Protocol) 服务器连接。" eyebrow="后台">
      <template #actions>
        <a-button type="primary" @click="showAdd" size="small"><PlusOutlined /> 添加服务器</a-button>
      </template>
    </PageHeader>

    <div class="card">
      <a-table :columns="columns" :data-source="serverList" :loading="loading" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div>
              <div style="font-weight:700">{{ record.name }}</div>
              <div style="font-size:12px;color:var(--t-muted)">{{ record.url }}</div>
            </div>
          </template>
          <template v-if="column.key === 'isActive'">
            <a-tag :color="record.isActive ? 'green' : 'red'">{{ record.isActive ? '启用' : '禁用' }}</a-tag>
          </template>
          <template v-if="column.key === 'toolCount'">
            <a-tag color="blue">{{ record.toolCount || 0 }} 个工具</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="handleRefresh(record)">刷新工具</a-button>
            <a-button type="link" size="small" @click="viewTools(record)">查看工具</a-button>
            <a-popconfirm title="确定删除？" @confirm="handleDelete(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 添加服务器弹窗 -->
    <a-modal v-model:open="modalVisible" title="添加 MCP 服务器" @ok="handleSave" okText="添加">
      <a-form :model="form" layout="vertical">
        <a-form-item label="服务器名称" required>
          <a-input v-model:value="form.name" placeholder="如: chrome-devtools" />
        </a-form-item>
        <a-form-item label="服务器 URL" required>
          <a-input v-model:value="form.url" placeholder="如: http://localhost:3000" />
        </a-form-item>
        <a-form-item label="描述">
          <a-input v-model:value="form.description" placeholder="服务器用途说明" />
        </a-form-item>
        <a-form-item label="传输类型">
          <a-select v-model:value="form.transport">
            <a-select-option value="sse">SSE (HTTP)</a-select-option>
            <a-select-option value="stdio" disabled>Stdio (暂不支持)</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 工具列表弹窗 -->
    <a-modal v-model:open="toolsModalVisible" :title="toolsTitle" :footer="null" width="700px">
      <a-table :columns="toolColumns" :data-source="toolList" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'description'">
            <span style="font-size:12px">{{ record.description }}</span>
          </template>
          <template v-if="column.key === 'inputSchema'">
            <span style="font-size:11px;font-family:monospace">{{ formatSchema(record.inputSchema) }}</span>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import request from '@/utils/request'

interface McpServer {
  id: string
  name: string
  url: string
  description: string
  transport: string
  isActive: number
  toolCount: number
}

interface McpTool {
  name: string
  description: string
  inputSchema: any
  serverName: string
}

const loading = ref(false)
const serverList = ref<McpServer[]>([])
const modalVisible = ref(false)
const toolsModalVisible = ref(false)
const toolsTitle = ref('')
const toolList = ref<McpTool[]>([])

const form = reactive({
  name: '', url: '', description: '', transport: 'sse'
})

const columns = [
  { title: '服务器', dataIndex: 'name', key: 'name', width: 250 },
  { title: '状态', dataIndex: 'isActive', key: 'isActive', width: 80 },
  { title: '工具数', dataIndex: 'toolCount', key: 'toolCount', width: 100 },
  { title: '传输', dataIndex: 'transport', key: 'transport', width: 80 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作', key: 'action', width: 200 },
]

const toolColumns = [
  { title: '工具名', dataIndex: 'name', key: 'name', width: 150 },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '参数', dataIndex: 'inputSchema', key: 'inputSchema', width: 200 },
]

const formatSchema = (schema: any) => {
  if (!schema) return '-'
  const props = schema.properties
  if (!props) return '-'
  return Object.keys(props).map(k => `${k}: ${props[k]?.type || '?'}`).join(', ')
}

const fetchList = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/mcp/admin/list')
    serverList.value = res.data || []
  } catch (e) {} finally { loading.value = false }
}

const showAdd = () => {
  Object.assign(form, { name: '', url: '', description: '', transport: 'sse' })
  modalVisible.value = true
}

const handleSave = async () => {
  if (!form.name || !form.url) {
    message.warning('请填写名称和 URL')
    return
  }
  try {
    await request.post('/mcp/admin/add', form)
    message.success('添加成功')
    modalVisible.value = false
    fetchList()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '添加失败')
  }
}

const handleDelete = async (record: McpServer) => {
  try {
    await request.post('/mcp/admin/delete', null, { params: { id: record.id } })
    message.success('删除成功')
    fetchList()
  } catch (e) { message.error('删除失败') }
}

const handleRefresh = async (record: McpServer) => {
  try {
    const res: any = await request.post('/mcp/admin/refresh', null, { params: { id: record.id } })
    message.success(`刷新成功，发现 ${res.data} 个工具`)
    fetchList()
  } catch (e) { message.error('刷新失败') }
}

const viewTools = async (record: McpServer) => {
  toolsTitle.value = `${record.name} - 工具列表`
  try {
    const res: any = await request.get('/mcp/admin/tools')
    toolList.value = (res.data || []).filter((t: McpTool) => t.serverName === record.name)
    toolsModalVisible.value = true
  } catch (e) { message.error('获取工具列表失败') }
}

onMounted(() => fetchList())
</script>

<style scoped>
.admin-page { padding: 30px 24px 48px; max-width: 1280px; margin: 0 auto; }
.card { background: var(--bg-card); border-radius: var(--r-xl); padding: 18px; border: 1px solid var(--border-light); }
</style>
