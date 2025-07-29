<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>Uçuş Yönetim Sistemi</h2>
        <p>Sisteme giriş yapmak için bilgilerinizi giriniz</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="Kullanıcı Adı"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="Şifre"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            style="width: 100%"
          >
            Giriş Yap
          </el-button>
        </el-form-item>
      </el-form>

      <div class="demo-accounts">
        <p>Demo Hesaplar:</p>
        <div class="accounts">
          <el-tag @click="setAccount('admin')" style="cursor: pointer; margin-right: 8px;">
            Admin: admin / admin123
          </el-tag>
          <el-tag @click="setAccount('user')" style="cursor: pointer;">
            User: user / user123
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useLoading } from '@/utils'
import { rules } from '@/utils'

const auth = useAuthStore()
const router = useRouter()
const { loading, withLoading } = useLoading()

const formRef = ref()
const form = reactive({
  username: '',
  password: ''
})

const formRules = {
  username: [rules.required],
  password: [rules.required]
}

const handleLogin = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return

    await withLoading(async () => {
      try {
        await auth.login(form)
        console.log('Auth state after login:', auth.isAuthenticated, auth.user)
        ElMessage.success('Giriş başarılı')
        await nextTick()
        console.log('Redirecting to dashboard...')
        await router.push('/')
        console.log('Redirect completed')
      } catch (error) {
        console.error('Login failed:', error)
        ElMessage.error('Giriş başarısız')
      }
    })
  })
}

const setAccount = (type) => {
  if (type === 'admin') {
    form.username = 'admin'
    form.password = 'admin123'
  } else {
    form.username = 'user'
    form.password = 'user123'
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
}

.login-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.demo-accounts {
  margin-top: 20px;
  text-align: center;
}

.demo-accounts p {
  margin: 0 0 8px 0;
  color: #909399;
  font-size: 12px;
}

.accounts {
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>
