<template>
  <div class="admin-page">
    <PageHeader title="技能管理" description="管理代码生成技能的配置，包括 prompt、工具集和构建策略。" eyebrow="后台">
      <template #actions>
        <a-button type="primary" @click="showAdd" size="small"><PlusOutlined /> 添加技能</a-button>
      </template>
    </PageHeader>
    <div class="card">
      <a-table :columns="columns" :data-source="skillList" :loading="loading" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div>
              <div style="font-weight:700">{{ record.name }}</div>
              <div style="font-size:12px;color:var(--t-muted)">{{ record.skillKey }}</div>
            </div>
          </template>
          <template v-if="column.key === 'buildStrategy'">
            <a-tag :color="record.buildStrategy === 'none' ? 'default' : record.buildStrategy === 'vue' ? 'green' : 'purple'">
              {{ record.buildStrategy }}
            </a-tag>
          </template>
          <template v-if="column.key === 'modelStrategy'">
            <a-tag :color="record.modelStrategy === 'reasoning' ? 'blue' : 'default'">{{ record.modelStrategy }}</a-tag>
          </template>
          <template v-if="column.key === 'isActive'">
            <a-tag :color="record.isActive ? 'green' : 'red'">{{ record.isActive ? '启用' : '禁用' }}</a-tag>
          </template>
          <template v-if="column.key === 'extensions'">
            <a-tag v-if="record.customTools" color="cyan">自定义工具</a-tag>
            <a-tag v-if="record.hooks" color="orange">钩子</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="showEdit(record)">编辑</a-button>
            <a-popconfirm title="确定删除？" @confirm="handleDelete(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑技能' : '添加技能'" width="700px" @ok="handleSave">
      <a-form :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="技能名称" required><a-input v-model:value="form.name" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="唯一标识" required><a-input v-model:value="form.skillKey" :disabled="!!editingId" /></a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述"><a-input v-model:value="form.description" /></a-form-item>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="代码生成类型" required><a-input v-model:value="form.codeGenType" /></a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="积分消耗" required><a-input-number v-model:value="form.pointCost" :min="1" style="width:100%" /></a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="排序"><a-input-number v-model:value="form.sortOrder" :min="0" style="width:100%" /></a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="构建策略">
              <a-select v-model:value="form.buildStrategy">
                <a-select-option value="none">none</a-select-option>
                <a-select-option value="vue">vue</a-select-option>
                <a-select-option value="fullstack">fullstack</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="模型策略">
              <a-select v-model:value="form.modelStrategy">
                <a-select-option value="standard">standard</a-select-option>
                <a-select-option value="reasoning">reasoning</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="状态">
              <a-select v-model:value="form.isActive">
                <a-select-option :value="1">启用</a-select-option>
                <a-select-option :value="0">禁用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="工具列表（逗号分隔，留空表示全部）">
          <a-input v-model:value="form.toolNames" placeholder="aiImageSearchTool,webSearchTool" />
        </a-form-item>

        <a-collapse :bordered="false" style="background:transparent;margin-bottom:16px">
          <a-collapse-panel key="customTools" header="自定义工具（JSON）">
            <a-typography-paragraph type="secondary" :style="{fontSize:'12px',marginBottom:'8px'}">
              定义 HTTP 工具，格式：[{'{'}"name":"qr","description":"生成二维码","endpoint":"https://...","method":"GET","parameters":[{'{'}"name":"text","type":"string","required":true{'}'}]{'}'}]
            </a-typography-paragraph>
            <a-textarea v-model:value="form.customTools" :rows="4" placeholder='[{"name":"qr","description":"生成二维码","endpoint":"https://api.qrserver.com/v1/create-qr-code/?size=200x200&data={text}","method":"GET","parameters":[{"name":"text","type":"string","required":true}]}]' />
          </a-collapse-panel>
          <a-collapse-panel key="hooks" header="生命周期钩子（JSON）">
            <a-typography-paragraph type="secondary" :style="{fontSize:'12px',marginBottom:'8px'}">
              定义 beforeGenerate / afterGenerate 钩子，格式：{'{'}"beforeGenerate":[{'{'}"type":"http","url":"...","method":"POST"{'}'}]{'}'}
            </a-typography-paragraph>
            <a-textarea v-model:value="form.hooks" :rows="3" placeholder='{"beforeGenerate":[{"type":"http","url":"https://api.example.com/before","method":"POST"}]}' />
          </a-collapse-panel>
        </a-collapse>

        <a-form-item label="系统提示词" required>
          <a-textarea v-model:value="form.systemPrompt" :rows="10" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { adminListSkills, addSkill, updateSkill, deleteSkill } from '@/api/skill'
import type { CodeSkill } from '@/types'

const loading = ref(false)
const modalVisible = ref(false)
const editingId = ref<string | null>(null)
const skillList = ref<CodeSkill[]>([])

const form = reactive<Partial<CodeSkill>>({
  name: '', skillKey: '', description: '', systemPrompt: '',
  codeGenType: '', pointCost: 10, toolNames: null,
  buildStrategy: 'none', modelStrategy: 'standard', isActive: 1, sortOrder: 0,
  customTools: null, hooks: null
})

const columns = [
  { title: '技能', dataIndex: 'name', key: 'name', width: 160 },
  { title: '类型', dataIndex: 'codeGenType', key: 'codeGenType', width: 110 },
  { title: '积分', dataIndex: 'pointCost', key: 'pointCost', width: 70 },
  { title: '构建', dataIndex: 'buildStrategy', key: 'buildStrategy', width: 100 },
  { title: '模型', dataIndex: 'modelStrategy', key: 'modelStrategy', width: 100 },
  { title: '扩展', key: 'extensions', width: 130 },
  { title: '状态', dataIndex: 'isActive', key: 'isActive', width: 80 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 70 },
  { title: '操作', key: 'action', width: 130 },
]

const fetchList = async () => {
  loading.value = true
  try { const r: any = await adminListSkills(); skillList.value = r.data || [] }
  catch (e) {} finally { loading.value = false }
}

const resetForm = () => {
  Object.assign(form, {
    name: '', skillKey: '', description: '', systemPrompt: '',
    codeGenType: '', pointCost: 10, toolNames: null,
    buildStrategy: 'none', modelStrategy: 'standard', isActive: 1, sortOrder: 0,
    customTools: null, hooks: null
  })
}

const showAdd = () => { resetForm(); editingId.value = null; modalVisible.value = true }

const showEdit = (record: CodeSkill) => {
  editingId.value = record.id
  Object.assign(form, { ...record })
  modalVisible.value = true
}

const handleSave = async () => {
  if (!form.name || !form.skillKey || !form.systemPrompt || !form.codeGenType) {
    message.warning('请填写必填字段'); return
  }
  try {
    if (editingId.value) {
      await updateSkill({ ...form, id: editingId.value })
      message.success('更新成功')
    } else {
      await addSkill(form)
      message.success('添加成功')
    }
    modalVisible.value = false
    fetchList()
  } catch (e) { message.error('操作失败') }
}

const handleDelete = async (record: CodeSkill) => {
  try { await deleteSkill(record.id); message.success('删除成功'); fetchList() }
  catch (e) { message.error('删除失败') }
}

onMounted(() => fetchList())
</script>

<style scoped>
.admin-page { padding: 30px 24px 48px; max-width: 1280px; margin: 0 auto; }
.card { background: var(--bg-card); border-radius: var(--r-xl); padding: 18px; border: 1px solid var(--border-light); }
</style>
