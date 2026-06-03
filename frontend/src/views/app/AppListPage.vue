<template>
  <div class="page">
    <PageHeader title="应用资产" description="这里保存你创建过的应用、当前状态和下一步动作。">
      <template #actions>
        <a-button type="primary" @click="handleCreate"><PlusOutlined /> 新建应用</a-button>
      </template>
    </PageHeader>

    <section class="asset-overview">
      <div class="overview-item">
        <span class="overview-label">全部</span>
        <strong>{{ total }}</strong>
      </div>
      <div class="overview-item">
        <span class="overview-label">已上线</span>
        <strong>{{ deployedCount }}</strong>
      </div>
      <div class="overview-item">
        <span class="overview-label">草稿</span>
        <strong>{{ draftCount }}</strong>
      </div>
      <div class="overview-item muted">
        <span class="overview-label">近 7 天活跃</span>
        <strong>{{ recentCount }}</strong>
      </div>
    </section>

    <section class="asset-toolbar">
      <div class="toolbar-copy">
        <span class="toolbar-title">资产列表</span>
        <span class="toolbar-desc">优先处理草稿和可部署应用。</span>
      </div>
      <div class="toolbar-controls">
        <a-input
          v-model:value="searchText"
          placeholder="搜索应用名称"
          allow-clear
          class="search-input"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select v-model:value="statusFilter" placeholder="状态" style="width: 128px">
          <a-select-option value="all">全部</a-select-option>
          <a-select-option value="deployed">已上线</a-select-option>
          <a-select-option value="draft">草稿</a-select-option>
        </a-select>
      </div>
    </section>

    <a-spin :spinning="loading">
      <div class="app-grid" v-if="filteredApps.length > 0">
        <AppCard
          v-for="app in filteredApps"
          :key="app.id"
          :app="app"
          @click="goToChat(app.id)"
          @chat="goToChat(app.id)"
          @edit="goToEdit(app.id)"
          @preview="goToPreview(app)"
          @deploy="handleDeploy(app.id)"
          @delete="handleDelete(app.id)"
        />
      </div>
      <EmptyState
        v-else-if="!loading"
        title="还没有应用资产"
        description="从一句需求开始创建应用，生成后会沉淀在这里，方便继续迭代和部署。"
      >
        <template #action>
          <a-button type="primary" @click="handleCreate"><PlusOutlined /> 新建应用</a-button>
        </template>
      </EmptyState>
    </a-spin>

    <div class="pagination-wrap" v-if="total > pageSize">
      <a-pagination v-model:current="currentPage" :total="total" :page-size="pageSize" @change="handlePageChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, SearchOutlined
} from '@ant-design/icons-vue'
import AppCard from '@/components/AppCard.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getMyAppList, deployApp, deleteApp } from '@/api/app'

const router = useRouter()
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const myAppList = ref<any[]>([])
const searchText = ref('')
const statusFilter = ref('all')

const deployedCount = computed(() => myAppList.value.filter(a => a.deployedTime).length)
const draftCount = computed(() => myAppList.value.filter(a => !a.deployedTime).length)
const recentCount = computed(() => {
  const day = 86400000
  return myAppList.value.filter(a => a.createTime && Date.now() - new Date(a.createTime).getTime() < 7 * day).length
})

const filteredApps = computed(() => {
  let apps = myAppList.value
  if (searchText.value) {
    const q = searchText.value.toLowerCase()
    apps = apps.filter(a => a.title?.toLowerCase().includes(q) || a.appName?.toLowerCase().includes(q))
  }
  if (statusFilter.value === 'deployed') apps = apps.filter(a => a.deployedTime)
  if (statusFilter.value === 'draft') apps = apps.filter(a => !a.deployedTime)
  return apps
})

const fetchMyApps = async () => {
  loading.value = true
  try {
    const res: any = await getMyAppList({ pageNum: currentPage.value, pageSize: pageSize.value })
    myAppList.value = (res.data?.records || []).map((a: any) => ({
      ...a,
      title: a.appName || a.title,
    }))
    total.value = res.data?.totalRow || 0
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const handleCreate = () => router.push('/')
const goToChat = (id: string) => router.push(`/app/chat/${id}`)
const goToEdit = (id: string) => router.push(`/app/edit/${id}`)
const goToPreview = async (app: any) => {
  if (!app.deployKey) return
  // 全栈项目需要通过 deployApp 获取 Express URL
  if (app.codeGenType === 'fullstack') {
    const url = await deployApp(app.id)
    if (url) { window.open(url, '_blank'); return }
  }
  window.open(`/api/code_deploy/${app.deployKey}/index.html`, '_blank')
}
const handleDeploy = async (id: string) => {
  try {
    const url = await deployApp(id)
    await fetchMyApps()
    if (url) window.open(url, '_blank')
  } catch (e) {}
}
const handleDelete = async (id: string) => {
  try {
    await deleteApp(id)
    message.success('删除成功')
    fetchMyApps()
  } catch (e) {}
}
const handlePageChange = (page: number) => { currentPage.value = page; fetchMyApps() }
onMounted(() => fetchMyApps())
</script>

<style scoped>
.page {
  padding: 30px 24px 48px;
  max-width: 1280px;
  margin: 0 auto;
}

.asset-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  margin-bottom: 18px;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: var(--r-xl);
  background: var(--border-light);
}

.overview-item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  background: var(--bg-card);
}

.overview-item.muted {
  background: var(--bg-soft);
}

.overview-label {
  color: var(--t-muted);
  font-size: 12px;
  font-weight: 700;
}

.overview-item strong {
  color: var(--t-primary);
  font-size: 22px;
  font-weight: 850;
  letter-spacing: -0.6px;
}

.asset-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  padding: 14px 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--r-lg);
  background: var(--bg-card);
}

.toolbar-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toolbar-title {
  color: var(--t-primary);
  font-size: 14px;
  font-weight: 800;
}

.toolbar-desc {
  color: var(--t-muted);
  font-size: 12px;
}

.toolbar-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 260px;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 900px) {
  .asset-overview {
    grid-template-columns: repeat(2, 1fr);
  }

  .asset-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-controls {
    flex-wrap: wrap;
  }

  .search-input {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .page { padding: 22px 16px 36px; }
  .app-grid { grid-template-columns: 1fr; }
}
</style>