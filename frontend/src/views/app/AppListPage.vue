<template>
  <div class="app-list-page">
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">我的应用</h1>
          <span class="page-subtitle">管理你创建的所有应用</span>
        </div>
        <a-button type="primary" @click="handleCreate" class="create-btn">
          <PlusOutlined />
          创建应用
        </a-button>
      </div>

      <a-spin :spinning="loading">
        <div class="app-grid">
          <AppCard
            v-for="app in myAppList"
            :key="app.id"
            :app="app"
            @click="goToChat(app.id)"
          />
        </div>
        <a-empty v-if="!loading && myAppList.length === 0" description="暂无应用，点击上方按钮创建">
          <a-button type="primary" @click="handleCreate">
            <PlusOutlined />
            创建第一个应用
          </a-button>
        </a-empty>
      </a-spin>

      <div class="pagination-wrapper" v-if="total > pageSize">
        <a-pagination
          v-model:current="currentPage"
          :total="total"
          :page-size="pageSize"
          @change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import AppCard from '@/components/AppCard.vue'
import { getMyAppList } from '@/api/app'

const router = useRouter()

const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const myAppList = ref<any[]>([])

const fetchMyApps = async () => {
  loading.value = true
  try {
    const res: any = await getMyAppList({
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    myAppList.value = res.data?.records || []
    total.value = res.data?.totalRow || 0
  } catch (error) {
    console.error('获取应用列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  router.push('/')
}

const goToChat = (id: number) => {
  router.push(`/app/chat/${id}`)
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchMyApps()
}

onMounted(() => {
  fetchMyApps()
})
</script>

<style scoped>
.app-list-page {
  padding: 24px;
  background: var(--bg-page);
  min-height: calc(100vh - 64px);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
}

.create-btn {
  border-radius: 10px !important;
  font-weight: 600 !important;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 768px) {
  .app-list-page {
    padding: 16px;
  }
  .page-title {
    font-size: 20px;
  }
  .page-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  .app-grid {
    grid-template-columns: 1fr;
  }
}
</style>
