<template>
  <div class="login-page">
    <div class="login-panel">
      <h1 class="login-title">AI Agent Station</h1>
      <p class="login-subtitle">统一身份登录</p>

      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" label-position="top">
        <el-form-item label="租户ID（可选）">
          <el-input v-model="form.tenantId" class="gemini-input" placeholder="默认使用后端默认租户" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            class="gemini-input"
            placeholder="请输入用户名"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            class="gemini-input"
            type="password"
            show-password
            placeholder="请输入密码"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-button type="primary" class="gemini-btn-primary login-btn" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  tenantId: '',
  username: '',
  password: ''
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await authStore.login({
      tenantId: form.tenantId || undefined,
      username: form.username,
      password: form.password
    })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (error: any) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at top left, rgba(138, 180, 248, 0.15), transparent 36%),
    radial-gradient(circle at bottom right, rgba(129, 201, 149, 0.12), transparent 36%),
    var(--gemini-bg-primary);
}

.login-panel {
  width: 420px;
  padding: 28px 26px;
  border-radius: 18px;
  border: 1px solid var(--gemini-border);
  background: rgba(30, 31, 32, 0.92);
  backdrop-filter: blur(10px);
}

.login-title {
  margin: 0;
  font-size: 24px;
  color: var(--gemini-text-primary);
}

.login-subtitle {
  margin: 6px 0 20px;
  color: var(--gemini-text-secondary);
  font-size: 13px;
}

.login-form {
  margin-top: 8px;
}

.login-btn {
  width: 100%;
  margin-top: 10px;
}
</style>
