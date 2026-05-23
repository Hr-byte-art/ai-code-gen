<template>
  <div class="user-center-page">
    <div class="container">
      <a-row :gutter="24">
        <!-- 左侧用户信息 -->
        <a-col :xs="24" :md="8">
          <div class="user-card">
            <div class="user-card-bg"></div>
            <div class="user-avatar-section">
              <a-avatar :size="88" :src="userStore.userAvatar" class="user-avatar">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <h2 class="user-name">{{ userStore.username }}</h2>
              <p class="user-account">{{ userStore.userInfo?.userAccount }}</p>
              <a-tag v-if="userStore.isAdmin" color="red" class="role-tag">管理员</a-tag>
              <a-tag v-else color="blue" class="role-tag">普通用户</a-tag>
            </div>

            <div class="user-stats">
              <div class="stat-item">
                <div class="stat-value">{{ userStore.userInfo?.integral || 0 }}</div>
                <div class="stat-label">积分</div>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <div class="stat-value">{{ userStore.userInfo?.vipNumber || 0 }}</div>
                <div class="stat-label">VIP</div>
              </div>
            </div>

            <div class="user-actions">
              <a-button block @click="showEditModal" class="action-btn">
                <EditOutlined />
                编辑资料
              </a-button>
              <a-button block @click="showPasswordModal" class="action-btn">
                <LockOutlined />
                修改密码
              </a-button>
              <a-button block @click="handleSignIn" :loading="signInLoading" class="action-btn sign-in-btn">
                <CalendarOutlined />
                每日签到
              </a-button>
              <a-button block danger @click="handleLogout" class="action-btn">
                <LogoutOutlined />
                退出登录
              </a-button>
            </div>
          </div>
        </a-col>

        <!-- 右侧内容 -->
        <a-col :xs="24" :md="16">
          <div class="content-card">
            <a-tabs v-model:activeKey="activeTab" class="custom-tabs">
              <a-tab-pane key="info" tab="个人信息">
                <a-descriptions :column="{ xs: 1, sm: 2 }" bordered class="info-descriptions">
                  <a-descriptions-item label="用户名">{{ userStore.userInfo?.userName || '-' }}</a-descriptions-item>
                  <a-descriptions-item label="账号">{{ userStore.userInfo?.userAccount || '-' }}</a-descriptions-item>
                  <a-descriptions-item label="角色">
                    <a-tag :color="userStore.isAdmin ? 'red' : 'blue'">
                      {{ userStore.isAdmin ? '管理员' : '普通用户' }}
                    </a-tag>
                  </a-descriptions-item>
                  <a-descriptions-item label="简介" :span="2">{{ userStore.userInfo?.userProfile || '暂无简介' }}</a-descriptions-item>
                  <a-descriptions-item label="注册时间">{{ formatTime(userStore.userInfo?.createTime || '') }}</a-descriptions-item>
                  <a-descriptions-item label="最后签到">{{ formatTime(userStore.userInfo?.recentlySignedIn || '') }}</a-descriptions-item>
                </a-descriptions>
              </a-tab-pane>

              <a-tab-pane key="invited" tab="我的邀请">
                <a-list :data-source="invitedUsers" :loading="invitedLoading" class="invited-list">
                  <template #renderItem="{ item }">
                    <a-list-item>
                      <a-list-item-meta>
                        <template #avatar>
                          <a-avatar :src="item.userAvatar">
                            <template #icon><UserOutlined /></template>
                          </a-avatar>
                        </template>
                        <template #title>{{ item.userName }}</template>
                        <template #description>{{ item.userAccount }}</template>
                      </a-list-item-meta>
                    </a-list-item>
                  </template>
                </a-list>
              </a-tab-pane>
            </a-tabs>
          </div>
        </a-col>
      </a-row>

      <!-- 编辑资料弹窗 -->
      <a-modal
        v-model:open="editModalVisible"
        title="编辑资料"
        @ok="handleUpdateInfo"
        @cancel="editModalVisible = false"
        :ok-button-props="{ class: 'modal-ok-btn' }"
      >
        <a-form :model="editForm" layout="vertical">
          <a-form-item label="用户名">
            <a-input v-model:value="editForm.userName" placeholder="用户名" />
          </a-form-item>
          <a-form-item label="简介">
            <a-textarea v-model:value="editForm.userProfile" :rows="3" placeholder="个人简介" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 修改密码弹窗 -->
      <a-modal
        v-model:open="passwordModalVisible"
        title="修改密码"
        @ok="handleChangePassword"
        @cancel="passwordModalVisible = false"
      >
        <a-form :model="passwordForm" layout="vertical">
          <a-form-item label="旧密码" required>
            <a-input-password v-model:value="passwordForm.oldPassword" placeholder="旧密码" />
          </a-form-item>
          <a-form-item label="新密码" required>
            <a-input-password v-model:value="passwordForm.newPassword" placeholder="新密码" />
          </a-form-item>
          <a-form-item label="确认密码" required>
            <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="确认密码" />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  UserOutlined,
  EditOutlined,
  LockOutlined,
  CalendarOutlined,
  LogoutOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateMyInfo, changePassword, signIn, getMyInvited } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('info')
const editModalVisible = ref(false)
const passwordModalVisible = ref(false)
const signInLoading = ref(false)
const invitedLoading = ref(false)

const editForm = reactive({
  userName: '',
  userProfile: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const invitedUsers = ref<any[]>([])

const formatTime = (time: string) => {
  if (!time) return '暂无'
  return new Date(time).toLocaleString('zh-CN')
}

const showEditModal = () => {
  editForm.userName = userStore.userInfo?.userName || ''
  editForm.userProfile = userStore.userInfo?.userProfile || ''
  editModalVisible.value = true
}

const showPasswordModal = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordModalVisible.value = true
}

const handleUpdateInfo = async () => {
  try {
    await updateMyInfo({
      id: userStore.userId!,
      userName: editForm.userName,
      userProfile: editForm.userProfile
    })
    message.success('更新成功')
    editModalVisible.value = false
    await userStore.fetchUserInfo()
  } catch (error) {
    message.error('更新失败')
  }
}

const handleChangePassword = async () => {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.error('两次密码不一致')
    return
  }
  try {
    await changePassword(passwordForm)
    message.success('密码修改成功')
    passwordModalVisible.value = false
  } catch (error) {
    message.error('修改失败')
  }
}

const handleSignIn = async () => {
  signInLoading.value = true
  try {
    const res = await signIn()
    message.success(`签到成功，获得 ${res.data} 积分`)
    await userStore.fetchUserInfo()
  } catch (error) {
    message.error('签到失败')
  } finally {
    signInLoading.value = false
  }
}

const fetchInvitedUsers = async () => {
  invitedLoading.value = true
  try {
    const res: any = await getMyInvited()
    invitedUsers.value = res.data || []
  } catch (error) {
    console.error('获取邀请列表失败:', error)
  } finally {
    invitedLoading.value = false
  }
}

const handleLogout = async () => {
  await userStore.logout()
  router.push('/user/login')
}

onMounted(() => {
  fetchInvitedUsers()
})
</script>

<style scoped>
.user-center-page {
  padding: 24px;
  background: var(--bg-page);
  min-height: calc(100vh - 64px);
}

.container {
  max-width: 1100px;
  margin: 0 auto;
}

.user-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
  position: relative;
}

.user-card-bg {
  height: 80px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 50%, #6366f1 100%);
  position: relative;
}

.user-card-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.08'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}

.user-avatar-section {
  text-align: center;
  padding: 0 24px 20px;
  margin-top: -44px;
  position: relative;
}

.user-avatar {
  border: 4px solid #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.user-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 12px 0 2px;
}

.user-account {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0 0 8px;
}

.role-tag {
  border-radius: 6px;
  font-size: 12px;
}

.user-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 24px;
  margin: 0 24px;
  background: #f8fafc;
  border-radius: var(--radius-md);
  gap: 0;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: #e2e8f0;
}

.user-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px 24px 24px;
}

.action-btn {
  border-radius: 10px !important;
  height: 40px !important;
  font-weight: 500 !important;
  text-align: left !important;
  display: flex !important;
  align-items: center !important;
  gap: 8px;
}

.sign-in-btn {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.06), rgba(139, 92, 246, 0.06)) !important;
  border-color: rgba(59, 130, 246, 0.2) !important;
  color: var(--color-primary) !important;
}

.content-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid #f1f5f9;
}

.custom-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 20px;
}

.custom-tabs :deep(.ant-tabs-tab) {
  font-weight: 500;
}

.info-descriptions :deep(.ant-descriptions-item-label) {
  width: 100px;
}

.invited-list :deep(.ant-list-item) {
  border-radius: var(--radius-sm);
  padding: 12px 16px;
  margin-bottom: 8px;
  border: 1px solid #f1f5f9;
  transition: all var(--transition-fast);
}

.invited-list :deep(.ant-list-item:hover) {
  border-color: rgba(59, 130, 246, 0.15);
  background: #f8fafc;
}

@media (max-width: 768px) {
  .user-center-page {
    padding: 16px;
  }
  .user-card {
    margin-bottom: 16px;
  }
}
</style>
