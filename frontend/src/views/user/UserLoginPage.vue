<template>
  <div class="auth-page">
    <div class="auth-left" :style="{ '--auth-bg-image': `url(${authBg})` }">
      <div class="auth-left-content">
        <div class="auth-brand">
          <img src="@/assets/logo.svg" alt="Logo" class="brand-logo" />
          <span class="brand-name">{{ $t('brand.name') }}</span>
        </div>
        <h1 class="auth-headline">AI 驱动的智能应用生成平台</h1>
        <p class="auth-sub">输入想法，AI 为你构建完整应用</p>
        <div class="auth-features">
          <div class="feature"><RocketOutlined class="f-icon" /> 一键生成 Web 应用</div>
          <div class="feature"><ThunderboltOutlined class="f-icon" /> 支持多种生成模式</div>
          <div class="feature"><BgColorsOutlined class="f-icon" /> AI 自动设计界面</div>
        </div>
      </div>
    </div>

    <div class="auth-right">
      <div class="auth-form-wrap">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-subtitle">登录你的账号继续使用</p>

        <a-form :model="formState" @finish="handleLogin" layout="vertical" class="auth-form">
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入用户名' }]">
            <a-input v-model:value="formState.userAccount" size="large" placeholder="用户名">
              <template #prefix><UserOutlined /></template>
            </a-input>
          </a-form-item>

          <a-form-item name="userPassword" :rules="[{ required: true, message: '请输入密码' }]">
            <a-input-password v-model:value="formState.userPassword" size="large" placeholder="密码">
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>

          <a-form-item>
            <div class="form-row">
              <a-checkbox v-model:checked="formState.remember">记住我</a-checkbox>
              <a class="forgot-link" @click="handleForgotPassword">忘记密码？</a>
            </div>
          </a-form-item>

          <a-form-item>
            <a-button type="primary" html-type="submit" size="large" block :loading="loading">
              登录
            </a-button>
          </a-form-item>

          <div class="form-footer">
            还没有账号？<router-link to="/user/register" class="link">立即注册</router-link>
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
import { UserOutlined, LockOutlined, RocketOutlined, ThunderboltOutlined, BgColorsOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import authBg from '@/assets/auth-bg.webp'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const formState = reactive({ userAccount: '', userPassword: '', remember: false })

const handleLogin = async () => {
  loading.value = true
  try {
    const success = await userStore.login({ userAccount: formState.userAccount, userPassword: formState.userPassword })
    if (success) router.push((route.query.redirect as string) || '/app')
  } catch (e) {}
  finally { loading.value = false }
}

const handleForgotPassword = () => message.info('请联系管理员重置密码')
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
}

.auth-left {
  flex: 1;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.94), rgba(22, 32, 46, 0.78)),
    var(--auth-bg-image) center / cover no-repeat;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  position: relative;
  overflow: hidden;
}

.auth-left::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 30% 70%, rgba(78, 116, 96, 0.18), transparent 52%),
    radial-gradient(circle at 70% 30%, rgba(201, 166, 107, 0.12), transparent 48%);
}

.auth-left-content {
  position: relative;
  z-index: 1;
  max-width: 420px;
  color: #fff;
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 48px;
}

.brand-logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
}

.auth-headline {
  font-size: 36px;
  font-weight: 800;
  line-height: 1.2;
  margin: 0 0 16px;
  letter-spacing: -0.5px;
}

.auth-sub {
  font-size: 16px;
  opacity: 0.7;
  margin: 0 0 40px;
  line-height: 1.6;
}

.auth-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  font-weight: 500;
  opacity: 0.85;
}

.f-icon {
  font-size: 16px;
  opacity: 0.7;
}

.auth-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #fff;
}

.auth-form-wrap {
  width: 100%;
  max-width: 360px;
}

.form-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--t-primary);
  margin: 0 0 6px;
  letter-spacing: -0.3px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--t-muted);
  margin: 0 0 28px;
}

.auth-form {
  width: 100%;
}

.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.forgot-link {
  color: var(--c-primary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.form-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: var(--t-muted);
}

.link {
  color: var(--c-primary);
  font-weight: 600;
  margin-left: 4px;
}

@media (max-width: 900px) {
  .auth-page { flex-direction: column; }
  .auth-left { padding: 40px 24px; min-height: auto; }
  .auth-headline { font-size: 24px; }
  .auth-right { padding: 32px 24px; }
}
</style>
