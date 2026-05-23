<template>
  <div class="page">
    <PageHeader title="使用排行" description="查看用户维度的资源使用情况，辅助发现异常消耗。" eyebrow="资源账单">
      <template #actions>
        <a-button @click="router.back()" size="small"><ArrowLeftOutlined /> 返回</a-button>
      </template>
    </PageHeader>

    <section class="filter-bar">
      <div class="period-tabs">
        <button v-for="p in periods" :key="p.value" :class="['p-btn', { active: selected === p.value }]" @click="selected = p.value; fetchRanking()">{{ p.label }}</button>
      </div>
    </section>

    <section class="rank-layout">
      <div class="card main-card">
        <div class="card-head"><h3 class="card-title">用户 Token 消耗排行</h3><a-tag color="blue">{{ periodLabel }}</a-tag></div>
        <a-spin :spinning="loading">
          <div class="rank-list">
            <div v-for="(item, i) in rankings" :key="item.userId" class="rank-item">
              <div class="rank-badge">{{ i + 1 }}</div>
              <a-avatar :size="36" :src="item.userAvatar"><template #icon><UserOutlined /></template></a-avatar>
              <div class="rank-info"><div class="rank-name">{{ item.userName }}</div><div class="rank-desc">应用: {{ item.appCount }}</div></div>
              <div class="rank-stats"><div class="rank-val">{{ fmtNum(item.totalTokens) }}</div><div class="rank-lbl">Token</div></div>
            </div>
            <a-empty v-if="!loading && rankings.length === 0" />
          </div>
        </a-spin>
      </div>
      <aside class="card side-card">
        <div class="card-head"><h3 class="card-title">我的位置</h3></div>
        <div class="my-rank"><span class="my-rank-num">{{ myRanking.ranking || '-' }}</span><span class="my-rank-total">/ {{ totalUsers }}</span></div>
        <div class="my-stats">
          <div class="my-stat"><div class="my-stat-val">{{ fmtNum(myRanking.totalTokens || 0) }}</div><div class="my-stat-lbl">总 Token</div></div>
          <div class="my-stat-div"></div>
          <div class="my-stat"><div class="my-stat-val">{{ myRanking.appCount || 0 }}</div><div class="my-stat-lbl">应用数</div></div>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined, UserOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { getUserTokenRanking, getUserTokenSummary } from '@/api/token'

const router = useRouter()
const loading = ref(false)
const selected = ref('all')
const periods = [{ value: 'today', label: '今日' }, { value: 'week', label: '本周' }, { value: 'month', label: '本月' }, { value: 'all', label: '全部' }]
const periodLabel = computed(() => periods.find(p => p.value === selected.value)?.label || '')
const rankings = ref<any[]>([])
const totalUsers = ref(0)
const myRanking = ref<any>({})
const fmtNum = (n: number) => n >= 1e6 ? (n / 1e6).toFixed(1) + 'M' : n >= 1e3 ? (n / 1e3).toFixed(1) + 'K' : String(n)
const fetchRanking = async () => { loading.value = true; try { const r: any = await getUserTokenRanking({ page: 1, pageSize: 50 }); rankings.value = r.data?.rankings || []; totalUsers.value = r.data?.totalUsers || 0 } catch (e) {} finally { loading.value = false } }
const fetchMyRanking = async () => { try { const r = await getUserTokenSummary(); myRanking.value = r.data || {} } catch (e) {} }
onMounted(() => { fetchRanking(); fetchMyRanking() })
</script>

<style scoped>
.page { padding: 30px 24px 48px; max-width: 1200px; margin: 0 auto; }
.filter-bar { background: var(--bg-card); border-radius: var(--r-lg); padding: 10px 12px; margin-bottom: 16px; border: 1px solid var(--border-light); }
.period-tabs { display: flex; gap: 4px; }
.p-btn { padding: 6px 14px; border-radius: var(--r-md); border: none; background: transparent; color: var(--t-muted); font-size: 13px; font-weight: 700; cursor: pointer; transition: background var(--t-fast), color var(--t-fast); }
.p-btn:hover { background: var(--bg-hover); }
.p-btn.active { background: var(--c-primary-50); color: var(--c-primary); }
.rank-layout { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 16px; align-items: start; }
.card { background: var(--bg-card); border-radius: var(--r-xl); padding: 18px; border: 1px solid var(--border-light); }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.card-title { margin: 0; font-size: 14px; font-weight: 850; color: var(--t-primary); }
.rank-list { display: flex; flex-direction: column; gap: 6px; }
.rank-item { display: flex; align-items: center; gap: 10px; padding: 10px 12px; background: var(--bg-soft); border: 1px solid var(--border-light); border-radius: var(--r-md); }
.rank-badge { width: 26px; height: 26px; border-radius: var(--r-sm); background: var(--bg-card); border: 1px solid var(--border-light); display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 850; color: var(--t-muted); flex-shrink: 0; }
.rank-info { flex: 1; min-width: 0; }
.rank-name { font-size: 13px; font-weight: 800; color: var(--t-primary); }
.rank-desc { font-size: 11px; color: var(--t-muted); }
.rank-stats { text-align: right; flex-shrink: 0; }
.rank-val { font-size: 14px; font-weight: 850; color: var(--c-primary); }
.rank-lbl { font-size: 11px; color: var(--t-light); }
.my-rank { text-align: center; margin: 10px 0 18px; }
.my-rank-num { font-size: 44px; font-weight: 900; color: var(--c-primary); line-height: 1; }
.my-rank-total { font-size: 14px; color: var(--t-light); font-weight: 700; }
.my-stats { display: flex; align-items: center; background: var(--bg-soft); border: 1px solid var(--border-light); border-radius: var(--r-md); padding: 12px; }
.my-stat { flex: 1; text-align: center; }
.my-stat-val { font-size: 16px; font-weight: 850; color: var(--t-primary); }
.my-stat-lbl { font-size: 12px; color: var(--t-muted); }
.my-stat-div { width: 1px; height: 24px; background: var(--border); }
@media (max-width: 920px) { .rank-layout { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .page { padding: 22px 16px 36px; } }
</style>