<template>
  <router-view />
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/services/websocket'
import { watch } from 'vue'

const auth = useAuthStore()
const { connect } = useWebSocket()

watch(() => auth.isAuthenticated, (isAuth) => {
  if (isAuth) connect()
}, { immediate: true })
</script>