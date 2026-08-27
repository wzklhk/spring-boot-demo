<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <img src="/favicon.svg" alt="logo" class="brand-logo" />
        <h1 class="brand-title">Spring Boot <span>Demo</span></h1>
        <p class="brand-sub">Vue 3 · Spring Boot 3 · JWT 鉴权</p>
      </div>

      <el-tabs v-model="mode" class="auth-tabs" stretch>
        <!-- 登录 -->
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" size="large">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码"
                        :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button type="primary" class="submit-btn" size="large" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form>
        </el-tab-pane>

        <!-- 注册 -->
        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top" size="large">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="3-32 位字符" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="registerForm.email" placeholder="请输入邮箱" :prefix-icon="Message" clearable />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="至少 6 位" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码"
                        :prefix-icon="Lock" show-password @keyup.enter="handleRegister" />
            </el-form-item>
            <el-button type="primary" class="submit-btn" size="large" :loading="loading" @click="handleRegister">
              注 册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <p class="tips">注册成功后自动登录 · 登录态由 JWT 维持 24 小时</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { login, register } from '../api/auth'
import { setToken, setUser } from '../utils/auth'

const router = useRouter()
const mode = ref('login')
const loading = ref(false)

// ---- 登录 ----
const loginFormRef = ref()
const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    const data = await login(loginForm)
    setToken(data.token)
    setUser(data.user)
    ElMessage.success(`欢迎回来，${data.user.username}`)
    router.push('/')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

// ---- 注册 ----
const registerFormRef = ref()
const registerForm = reactive({ username: '', email: '', password: '', confirmPassword: '' })
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度需在 3-32 之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需在 6-64 之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        value === registerForm.password ? callback() : callback(new Error('两次输入的密码不一致'))
      },
      trigger: 'blur'
    }
  ]
}

async function handleRegister() {
  await registerFormRef.value.validate()
  loading.value = true
  try {
    const data = await register({
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password
    })
    setToken(data.token)
    setUser(data.user)
    ElMessage.success(`注册成功，欢迎 ${data.user.username}`)
    router.push('/')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 55%, #0f3460 100%);
  padding: 24px;
}

.login-card {
  width: 420px;
  max-width: 100%;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px 28px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.35);
}

.brand {
  text-align: center;
  margin-bottom: 24px;
}

.brand-logo {
  width: 44px;
  height: 44px;
  margin-bottom: 8px;
}

.brand-title {
  font-size: 22px;
  color: #1a1a2e;
  margin: 0 0 4px;
  letter-spacing: 0.5px;
}

.brand-title span {
  color: var(--vue-green);
}

.brand-sub {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.auth-tabs :deep(.el-tabs__item.is-active) {
  color: var(--vue-green);
}

.auth-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--vue-green);
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  background: var(--vue-green);
  border-color: var(--vue-green);
  letter-spacing: 4px;
  font-weight: 600;
}

.submit-btn:hover {
  background: #37a874;
  border-color: #37a874;
}

.tips {
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
  margin: 20px 0 0;
}
</style>
