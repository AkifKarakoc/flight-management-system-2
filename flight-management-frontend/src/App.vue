<template>
  <router-view />
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

// WebSocket geçici olarak devre dışı
// const { connect } = useWebSocket()
// watch(() => auth.isAuthenticated, (isAuth) => {
//   if (isAuth) connect()
// }, { immediate: true })
</script>
