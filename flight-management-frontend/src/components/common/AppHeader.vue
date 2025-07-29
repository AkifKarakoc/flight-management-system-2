<template>
  <div class="header">
    <div class="header-left">
      <h3>{{ pageTitle }}</h3>
    </div>

    <div class="header-right">
      <el-badge :value="notifications" class="notification">
        <el-icon size="20"><Bell /></el-icon>
      </el-badge>

      <el-dropdown @command="handleCommand">
        <span class="user-info">
          <el-avatar size="small">{{ auth.user?.username?.charAt(0).toUpperCase() }}</el-avatar>
          <span>{{ auth.user?.username }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">Çıkış Yap</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { Bell, ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const notifications = ref(0)

const pageTitle = computed(() => {
  const titles = {
    '/': 'Dashboard',
    '/flights': 'Uçuş Yönetimi',
    '/airlines': 'Havayolları',
    '/airports': 'Havaalanları',
    '/aircraft': 'Uçaklar',
    '/routes': 'Rotalar',
    '/reports': 'Raporlar'
  }
  return titles[route.path] || 'Uçuş Yönetim Sistemi'
})

const handleCommand = (command) => {
  if (command === 'logout') {
    auth.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.notification {
  cursor: pointer;
}
</style>
