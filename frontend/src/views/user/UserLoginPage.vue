<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
      <div class="bg-grid"></div>
    </div>

    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <router-link to="/" class="login-logo">
            <img src="@/assets/logo.svg" alt="Logo" />
          </router-link>
          <h1 class="login-title">欢迎回来</h1>
          <p class="login-subtitle">登录你的账号继续使用</p>
        </div>

        <a-form
          :model="formState"
          @finish="handleLogin"
          layout="vertical"
          class="login-form"
        >
          <a-form-item
            name="userAccount"
            :rules="[{ required: true, message: '请输入用户名' }]"
          >
            <a-input
              v-model:value="formState.userAccount"
              size="large"
              placeholder="用户名"
              class="form-input"
            >
              <template #prefix><UserOutlined /></template>
            </a-input>
          </a-form-item>

          <a-form-item
            name="userPassword"
            :rules="[{ required: true, message: '请输入密码' }]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              size="large"
              placeholder="密码"
              class="form-input"
            >
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>

          <a-form-item>
            <div class="form-options">
              <a-checkbox v-model:checked="formState.remember">
                记住我
              </a-checkbox>
              <a class="forgot-link" @click="handleForgotPassword">
                忘记密码？
              </a>
            </div>
          </a-form-item>

          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              block
              :loading="loading"
              class="submit-btn"
            >
              登录
            </a-button>
          </a-form-item>

          <div class="login-footer">
            <span>还没有账号？</span>
            <router-link to="/user/register" class="register-link">
              立即注册
            </router-link>
          </div>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)

const formState = reactive({
  userAccount: '',
  userPassword: '',
  remember: false
})

const handleLogin = async () => {
  loading.value = true
  try {
    const success = await userStore.login({
      userAccount: formState.userAccount,
      userPassword: formState.userPassword
    })

    if (success) {
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    }
  } catch (error) {
    message.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

const handleForgotPassword = () => {
  message.info('请联系管理员重置密码')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8eeff 50%, #f5f0ff 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(59, 130, 246, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
}

.bg-orb-1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.15), transparent 70%);
  top: -15%;
  right: -10%;
}

.bg-orb-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.12), transparent 70%);
  bottom: -10%;
  left: -8%;
}

.login-container {
  width: 100%;
  max-width: 420px;
  position: relative;
  z-index: 1;
}

.login-card {
  background: rgba(255, 255, 255, 0.92);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  backdrop-filter: blur(24px) saturate(180%);
  border-radius: 20px;
  padding: 40px 36px;
  box-shadow:
    0 20px 60px rgba(15, 23, 42, 0.08),
    0 4px 16px rgba(15, 23, 42, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(226, 232, 240, 0.6);
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  display: inline-block;
  margin-bottom: 16px;
}

.login-logo img {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.login-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 6px;
  letter-spacing: -0.5px;
}

.login-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.login-form {
  width: 100%;
}

.form-input {
  border-radius: 10px !important;
}

.submit-btn {
  height: 44px !important;
  border-radius: 10px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.forgot-link {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: color var(--transition-fast);
}

.forgot-link:hover {
  color: var(--color-primary-dark);
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--text-muted);
}

.register-link {
  color: var(--color-primary);
  font-weight: 600;
  margin-left: 4px;
  transition: color var(--transition-fast);
}

.register-link:hover {
  color: var(--color-primary-dark);
}
</style>
