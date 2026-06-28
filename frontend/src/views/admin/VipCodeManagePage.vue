<template>
  <div class="admin-page">
    <PageHeader title="会员码管理" description="生成和维护 VIP 兑换码，用于人工发放会员权益。" eyebrow="后台">
      <template #actions>
        <a-button type="primary" size="small" @click="showAdd">
          <PlusOutlined /> 添加会员码
        </a-button>
      </template>
    </PageHeader>

    <div class="card">
      <a-table :columns="columns" :data-source="vipCodeList" :loading="loading" row-key="id" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'vipCode'">
            <div class="code-cell">
              <code>{{ record.vipCode }}</code>
              <a-button type="link" size="small" @click="copyCode(record.vipCode)">复制</a-button>
            </div>
          </template>
          <template v-if="column.key === 'usage'">
            <a-progress
              :percent="calcUsePercent(record)"
              size="small"
              :format="() => `${record.useNum || 0}/${record.maxUseNum || 0}`"
            />
          </template>
          <template v-if="column.key === 'expDate'">
            {{ ft(record.expDate) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="showEdit(record)">编辑</a-button>
            <a-popconfirm title="确定删除该会员码？" @confirm="handleDelete(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑会员码' : '添加会员码'" @ok="handleSave">
      <a-form :model="form" layout="vertical">
        <a-form-item label="会员码">
          <a-input v-model:value="form.vipCode" :disabled="!!editingId" placeholder="留空时后端自动生成 16 位会员码" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="VIP 有效天数" required>
              <a-input-number v-model:value="form.effectiveDay" :min="1" :max="365" style="width:100%" :disabled="!!editingId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="最大使用人数" required>
              <a-input-number v-model:value="form.maxUseNum" :min="1" :max="1000" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="兑换码过期时间" required>
          <a-date-picker
            v-model:value="form.expDate"
            show-time
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width:100%"
            placeholder="请选择过期时间"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { createVipCode, deleteVipCode, getVipCodeList, updateVipCode } from '@/api/user'
import { formatDateTime as ft } from '@/utils/time'
import type { VipCodeVO } from '@/types'

const loading = ref(false)
const modalVisible = ref(false)
const editingId = ref<string | null>(null)
const vipCodeList = ref<VipCodeVO[]>([])
const form = reactive({
  vipCode: '',
  effectiveDay: 365,
  expDate: '',
  maxUseNum: 1,
})

const columns = [
  { title: '会员码', dataIndex: 'vipCode', key: 'vipCode', width: 260 },
  { title: '有效天数', dataIndex: 'effectiveDay', key: 'effectiveDay', width: 100 },
  { title: '使用情况', key: 'usage', width: 180 },
  { title: '过期时间', dataIndex: 'expDate', key: 'expDate', width: 180 },
  { title: '操作', key: 'action', width: 130 },
]

const fetchVipCodeList = async () => {
  loading.value = true
  try {
    const res: any = await getVipCodeList()
    vipCodeList.value = res.data || []
  } catch {
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.vipCode = ''
  form.effectiveDay = 365
  form.expDate = ''
  form.maxUseNum = 1
}

const showAdd = () => {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

const showEdit = (record: VipCodeVO) => {
  editingId.value = record.id
  form.vipCode = record.vipCode
  form.effectiveDay = record.effectiveDay
  form.expDate = record.expDate?.replace(' ', 'T') || ''
  form.maxUseNum = record.maxUseNum
  modalVisible.value = true
}

const handleSave = async () => {
  if (!form.expDate || !form.maxUseNum) {
    message.warning('请填写完整')
    return
  }
  try {
    if (editingId.value) {
      await updateVipCode({ id: editingId.value, expDate: form.expDate, maxUseNum: form.maxUseNum })
      message.success('更新成功')
    } else {
      await createVipCode({
        vipCode: form.vipCode || undefined,
        effectiveDay: form.effectiveDay,
        expDate: form.expDate,
        maxUseNum: form.maxUseNum,
      })
      message.success('创建成功')
    }
    modalVisible.value = false
    await fetchVipCodeList()
  } catch {
  }
}

const handleDelete = async (record: VipCodeVO) => {
  try {
    await deleteVipCode(record.id)
    message.success('删除成功')
    await fetchVipCodeList()
  } catch {
  }
}

const calcUsePercent = (record: VipCodeVO) => {
  if (!record.maxUseNum) return 0
  return Math.min(100, Math.round(((record.useNum || 0) / record.maxUseNum) * 100))
}

const copyCode = async (vipCode: string) => {
  try {
    await navigator.clipboard.writeText(vipCode)
    message.success('已复制会员码')
  } catch {
    message.warning('复制失败，请手动复制')
  }
}

onMounted(() => fetchVipCodeList())
</script>

<style scoped>
.admin-page { max-width: 1200px; margin: 0 auto; }
.card { background: var(--bg-card); border-radius: var(--r-lg); padding: 16px; border: 1px solid var(--border-light); }
.code-cell { display: flex; align-items: center; gap: 8px; }
.code-cell code { padding: 3px 7px; border-radius: var(--r-sm); background: var(--bg-soft); color: var(--t-primary); font-size: 12px; }
</style>