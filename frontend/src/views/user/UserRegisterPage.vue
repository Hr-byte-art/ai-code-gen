<template>
  <div class="register-page">
    <div class="register-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
      <div class="bg-grid"></div>
    </div>

    <div class="register-container">
      <div class="register-card">
        <div class="register-header">
          <router-link to="/" class="register-logo">
            <img src="@/assets/logo.svg" alt="Logo" />
          </router-link>
          <h1 class="register-title">创建账号</h1>
          <p class="register-subtitle">注册成为平台用户，开始生成应用</p>
        </div>

        <a-form
          :model="formState"
          @finish="handleRegister"
          layout="vertical"
          class="register-form"
        >
          <a-form-item
            name="userAccount"
            :rules="[
              { required: true, message: '请输入用户名' },
              { min: 3, message: '用户名至少3个字符' }
            ]"
          >
            <a-input
              v-model:value="formState.userAccount"
              size="large"
              placeholder="用户名"
            >
              <template #prefix><UserOutlined /></template>
            </a-input>
          </a-form-item>

          <a-form-item
            name="userPassword"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 6, message: '密码至少6个字符' }
            ]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              size="large"
              placeholder="密码"
            >
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>

          <a-form-item
            name="checkPassword"
            :rules="[
              { required: true, message: '请确认密码' },
              { validator: validateConfirmPassword }
            ]"
          >
            <a-input-password
              v-model:value="formState.checkPassword"
              size="large"
              placeholder="确认密码"
            >
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>

          <a-form-item
            name="shareCode"
          >
            <a-input
              v-model:value="formState.shareCode"
              size="large"
              placeholder="邀请码（选填）"
            >
              <template #prefix><GiftOutlined /></template>
            </a-input>
          </a-form-item>

          <a-form-item
            name="agreement"
            :rules="[
              { validator: validateAgreement }
            ]"
          >
            <a-checkbox v-model:checked="formState.agreement">
              我已阅读并同意
              <a class="agreement-link">用户协议</a>
              和
              <a class="agreement-link">隐私政策</a>
            </a-checkbox>
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
              注册
            </a-button>
          </a-form-item>

          <div class="register-footer">
            <span>已有账号？</span>
            <router-link to="/user/login" class="login-link">
              立即登录
            </router-link>
          </div>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, GiftOutlined } from '@ant-design/icons-vue'
import { register } from '@/api/user'

const router = useRouter()

const loading = ref(false)

const formState = reactive({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
  shareCode: '',
  agreement: false
})

const validateConfirmPassword = async (_rule: any, value: string) => {
  if (value && value !== formState.userPassword) {
    throw new Error('两次输入的密码不一致')
  }
}

const validateAgreement = async (_rule: any, value: boolean) => {
  if (!value) {
    throw new Error('请同意用户协议和隐私政策')
  }
}

const handleRegister = async () => {
  loading.value = true
  try {
    await register({
      userAccount: formState.userAccount,
      userPassword: formState.userPassword,
      checkPassword: formState.checkPassword,
      shareCode: formState.shareCode || undefined
    })

    message.success('注册成功！请登录')
    router.push('/user/login')
  } catch (error) {
    message.error('注册失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8eeff 50%, #f5f0ff 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.register-bg {
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
  background: radial-gradient(circle, rgba(139, 92, 246, 0.15), transparent 70%);
  top: -15%;
  left: -10%;
}

.bg-orb-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.12), transparent 70%);
  bottom: -10%;
  right: -8%;
}

.register-container {
  width: 100%;
  max-width: 420px;
  position: relative;
  z-index: 1;
}

.register-card {
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

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-logo {
  display: inline-block;
  margin-bottom: 16px;
}

.register-logo img {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.register-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 6px;
  letter-spacing: -0.5px;
}

.register-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.register-form {
  width: 100%;
}

.submit-btn {
  height: 44px !important;
  border-radius: 10px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
}

.agreement-link {
  color: var(--color-primary);
  font-weight: 500;
}

.agreement-link:hover {
  color: var(--color-primary-dark);
}

.register-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--text-muted);
}

.login-link {
  color: var(--color-primary);
  font-weight: 600;
  margin-left: 4px;
  transition: color var(--transition-fast);
}

.login-link:hover {
  color: var(--color-primary-dark);
}
</style>
