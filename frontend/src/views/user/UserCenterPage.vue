<template>
  <div class="page">
    <a-row :gutter="20">
      <a-col :xs="24" :md="8">
        <div class="profile-card">
          <div class="profile-banner"></div>
          <div class="profile-body">
            <a-avatar :size="64" :src="userStore.userAvatar" class="profile-avatar"><template #icon><UserOutlined /></template></a-avatar>
            <h2 class="profile-name">{{ userStore.username }}</h2>
            <p class="profile-account">{{ userStore.userInfo?.userAccount }}</p>
            <a-tag :color="userStore.isAdmin ? 'red' : 'blue'">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</a-tag>
            <div class="profile-stats">
              <div class="stat"><span class="stat-num">{{ userStore.userInfo?.integral || 0 }}</span><span class="stat-lbl">积分</span></div>
              <div class="stat-div"></div>
              <div class="stat"><span class="stat-num">{{ userStore.userInfo?.vipNumber || 0 }}</span><span class="stat-lbl">VIP</span></div>
            </div>
            <div class="profile-actions">
              <a-button block @click="showEditModal"><EditOutlined /> 编辑资料</a-button>
              <a-button block @click="showPasswordModal"><LockOutlined /> 修改密码</a-button>
              <a-button block @click="handleSignIn" :loading="signInLoading" class="sign-btn"><CalendarOutlined /> 每日签到</a-button>
              <a-button block danger @click="handleLogout"><LogoutOutlined /> 退出登录</a-button>
            </div>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :md="16">
        <div class="content-card">
          <a-tabs v-model:activeKey="activeTab">
            <a-tab-pane key="info" tab="个人信息">
              <a-descriptions :column="{ xs: 1, sm: 2 }" bordered size="small">
                <a-descriptions-item label="用户名">{{ userStore.userInfo?.userName || '-' }}</a-descriptions-item>
                <a-descriptions-item label="账号">{{ userStore.userInfo?.userAccount || '-' }}</a-descriptions-item>
                <a-descriptions-item label="角色"><a-tag :color="userStore.isAdmin ? 'red' : 'blue'">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</a-tag></a-descriptions-item>
                <a-descriptions-item label="简介" :span="2">{{ userStore.userInfo?.userProfile || '暂无简介' }}</a-descriptions-item>
                <a-descriptions-item label="注册时间">{{ formatDateTime(userStore.userInfo?.createTime || '', '暂无') }}</a-descriptions-item>
                <a-descriptions-item label="最后签到">{{ formatDateTime(userStore.userInfo?.recentlySignedIn || '', '暂无') }}</a-descriptions-item>
              </a-descriptions>
            </a-tab-pane>
            <a-tab-pane key="invited" tab="我的邀请">
              <a-list :data-source="invitedUsers" :loading="invitedLoading">
                <template #renderItem="{ item }">
                  <a-list-item><a-list-item-meta><template #avatar><a-avatar :src="item.userAvatar"><template #icon><UserOutlined /></template></a-avatar></template><template #title>{{ item.userName }}</template><template #description>{{ item.userAccount }}</template></a-list-item-meta></a-list-item>
                </template>
              </a-list>
            </a-tab-pane>
          </a-tabs>
        </div>
      </a-col>
    </a-row>

    <a-modal v-model:open="editModalVisible" title="编辑资料" @ok="handleUpdateInfo">
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="用户名"><a-input v-model:value="editForm.userName" /></a-form-item>
        <a-form-item label="简介"><a-textarea v-model:value="editForm.userProfile" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>
    <a-modal v-model:open="passwordModalVisible" title="修改密码" @ok="handleChangePassword">
      <a-form :model="passwordForm" layout="vertical">
        <a-form-item label="旧密码" required><a-input-password v-model:value="passwordForm.oldPassword" /></a-form-item>
        <a-form-item label="新密码" required><a-input-password v-model:value="passwordForm.newPassword" /></a-form-item>
        <a-form-item label="确认密码" required><a-input-password v-model:value="passwordForm.confirmPassword" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, EditOutlined, LockOutlined, CalendarOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateMyInfo, changePassword, signIn, getMyInvited } from '@/api/user'
import { formatDateTime } from '@/utils/time'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('info')
const editModalVisible = ref(false)
const passwordModalVisible = ref(false)
const signInLoading = ref(false)
const invitedLoading = ref(false)
const editForm = reactive({ userName: '', userProfile: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const invitedUsers = ref<any[]>([])
const showEditModal = () => { editForm.userName = userStore.userInfo?.userName || ''; editForm.userProfile = userStore.userInfo?.userProfile || ''; editModalVisible.value = true }
const showPasswordModal = () => { passwordForm.oldPassword = ''; passwordForm.newPassword = ''; passwordForm.confirmPassword = ''; passwordModalVisible.value = true }
const handleUpdateInfo = async () => { try { await updateMyInfo({ id: userStore.userId!, userName: editForm.userName, userProfile: editForm.userProfile }); message.success('更新成功'); editModalVisible.value = false; await userStore.fetchUserInfo() } catch (e) {} }
const handleChangePassword = async () => { if (passwordForm.newPassword !== passwordForm.confirmPassword) { message.error('两次密码不一致'); return }; try { await changePassword(passwordForm); message.success('密码修改成功'); passwordModalVisible.value = false } catch (e) {} }
const handleSignIn = async () => { signInLoading.value = true; try { const res = await signIn(); message.success(`签到成功，获得 ${res.data} 积分`); await userStore.fetchUserInfo() } catch (e) {} finally { signInLoading.value = false } }
const fetchInvitedUsers = async () => { invitedLoading.value = true; try { const res: any = await getMyInvited(); invitedUsers.value = res.data || [] } catch (e) {} finally { invitedLoading.value = false } }
const handleLogout = async () => { await userStore.logout(); router.push('/user/login') }
onMounted(() => {
  userStore.fetchUserInfo()
  fetchInvitedUsers()
})
</script>

<style scoped>
.page { padding: 28px 24px; max-width: 1100px; margin: 0 auto; }
.profile-card { background: var(--bg-card); border-radius: var(--r-xl); overflow: hidden; border: 1px solid var(--border-light); }
.profile-banner { height: 64px; background: var(--c-primary); }
.profile-body { padding: 0 20px 24px; text-align: center; }
.profile-avatar { margin-top: -32px; border: 4px solid var(--bg-card) !important; box-shadow: var(--shadow-md); }
.profile-name { font-size: 17px; font-weight: 800; color: var(--t-primary); margin: 10px 0 2px; }
.profile-account { font-size: 13px; color: var(--t-muted); margin: 0 0 8px; }
.profile-stats { display: flex; align-items: center; justify-content: center; margin: 14px 0; padding: 12px; background: var(--c-primary-50); border-radius: var(--r-md); }
.stat { flex: 1; text-align: center; }
.stat-num { display: block; font-size: 18px; font-weight: 800; color: var(--c-primary); }
.stat-lbl { font-size: 12px; color: var(--t-muted); }
.stat-div { width: 1px; height: 24px; background: var(--c-primary-200); }
.profile-actions { display: flex; flex-direction: column; gap: 8px; }
.sign-btn { background: var(--c-primary-50) !important; border-color: var(--c-primary-200) !important; color: var(--c-primary) !important; }
.content-card { background: var(--bg-card); border-radius: var(--r-xl); padding: 20px; border: 1px solid var(--border-light); }
@media (max-width: 768px) { .page { padding: 20px 16px; } .profile-card { margin-bottom: 16px; } }
</style>
