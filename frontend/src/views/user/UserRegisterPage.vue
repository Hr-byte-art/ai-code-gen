<template>
  <div class="auth-page">
    <div class="auth-left" :style="{ '--auth-bg-image': `url(${authBg})` }">
      <div class="auth-left-content">
        <div class="auth-brand">
          <img src="@/assets/logo.svg" alt="Logo" class="brand-logo" />
          <span class="brand-name">{{ $t('brand.name') }}</span>
        </div>
        <h1 class="auth-headline">开始创建你的应用</h1>
        <p class="auth-sub">无需编程经验，用自然语言描述需求即可</p>
        <div class="auth-features">
          <div class="feature"><AimOutlined class="f-icon" /> 自然语言描述即可</div>
          <div class="feature"><CodeOutlined class="f-icon" /> 多种代码生成模式</div>
          <div class="feature"><CloudUploadOutlined class="f-icon" /> 一键部署上线</div>
        </div>
      </div>
    </div>

    <div class="auth-right">
      <div class="auth-form-wrap">
        <h2 class="form-title">创建账号</h2>
        <p class="form-subtitle">填写信息完成注册</p>

        <a-form :model="formState" @finish="handleRegister" layout="vertical" class="auth-form">
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入用户名' }, { min: 3, message: '用户名至少3个字符' }]">
            <a-input v-model:value="formState.userAccount" size="large" placeholder="用户名">
              <template #prefix><UserOutlined /></template>
            </a-input>
          </a-form-item>

          <a-form-item name="userPassword" :rules="[{ required: true, message: '请输入密码' }, { min: 6, message: '密码至少6个字符' }]">
            <a-input-password v-model:value="formState.userPassword" size="large" placeholder="密码">
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>

          <a-form-item name="checkPassword" :rules="[{ required: true, message: '请确认密码' }, { validator: validateConfirmPassword }]">
            <a-input-password v-model:value="formState.checkPassword" size="large" placeholder="确认密码">
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>

          <a-form-item name="shareCode">
            <a-input v-model:value="formState.shareCode" size="large" placeholder="邀请码（选填）">
              <template #prefix><GiftOutlined /></template>
            </a-input>
          </a-form-item>

          <a-form-item name="agreement" :rules="[{ validator: validateAgreement }]">
            <a-checkbox v-model:checked="formState.agreement">
              我已阅读并同意 <a class="link">用户协议</a> 和 <a class="link">隐私政策</a>
            </a-checkbox>
          </a-form-item>

          <a-form-item>
            <a-button type="primary" html-type="submit" size="large" block :loading="loading">
              注册
            </a-button>
          </a-form-item>

          <div class="form-footer">
            已有账号？<router-link to="/user/login" class="link">立即登录</router-link>
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
import { UserOutlined, LockOutlined, GiftOutlined, AimOutlined, CodeOutlined, CloudUploadOutlined } from '@ant-design/icons-vue'
import { register } from '@/api/user'
import authBg from '@/assets/auth-bg.webp'

const router = useRouter()
const loading = ref(false)
const formState = reactive({ userAccount: '', userPassword: '', checkPassword: '', shareCode: '', agreement: false })

const validateConfirmPassword = async (_: any, value: string) => {
  if (value && value !== formState.userPassword) throw new Error('两次输入的密码不一致')
}
const validateAgreement = async (_: any, value: boolean) => {
  if (!value) throw new Error('请同意用户协议和隐私政策')
}

const handleRegister = async () => {
  loading.value = true
  try {
    await register({ userAccount: formState.userAccount, userPassword: formState.userPassword, checkPassword: formState.checkPassword, shareCode: formState.shareCode || undefined })
    message.success('注册成功！请登录')
    router.push('/user/login')
  } catch (e) { message.error('注册失败，请重试') }
  finally { loading.value = false }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; display: flex; }

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

.auth-left-content { position: relative; z-index: 1; max-width: 420px; color: #fff; }

.auth-brand { display: flex; align-items: center; gap: 10px; margin-bottom: 48px; }
.brand-logo { width: 32px; height: 32px; border-radius: 8px; }
.brand-name { font-size: 18px; font-weight: 700; }

.auth-headline { font-size: 36px; font-weight: 800; line-height: 1.2; margin: 0 0 16px; letter-spacing: -0.5px; }
.auth-sub { font-size: 16px; opacity: 0.7; margin: 0 0 40px; line-height: 1.6; }

.auth-features { display: flex; flex-direction: column; gap: 16px; }
.feature { display: flex; align-items: center; gap: 12px; font-size: 14px; font-weight: 500; opacity: 0.85; }
.f-icon { font-size: 16px; opacity: 0.7; }

.auth-right { flex: 1; display: flex; align-items: center; justify-content: center; padding: 40px; background: #fff; }
.auth-form-wrap { width: 100%; max-width: 360px; }

.form-title { font-size: 24px; font-weight: 700; color: var(--t-primary); margin: 0 0 6px; letter-spacing: -0.3px; }
.form-subtitle { font-size: 14px; color: var(--t-muted); margin: 0 0 28px; }
.auth-form { width: 100%; }

.link { color: var(--c-primary); font-weight: 600; }
.form-footer { text-align: center; margin-top: 16px; font-size: 13px; color: var(--t-muted); }

@media (max-width: 900px) {
  .auth-page { flex-direction: column; }
  .auth-left { padding: 40px 24px; min-height: auto; }
  .auth-headline { font-size: 24px; }
  .auth-right { padding: 32px 24px; }
}
</style>
