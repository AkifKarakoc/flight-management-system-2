<template>
  <el-container class="layout">
    <el-aside :width="sidebarWidth">
      <AppSidebar :collapsed="sidebarCollapsed" @toggle="toggleSidebar" />
    </el-aside>
    <el-container>
      <el-header height="60px">
        <AppHeader />
      </el-header>
      <el-main>
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'

const sidebarCollapsed = ref(false)

const sidebarWidth = computed(() => {
  return sidebarCollapsed.value ? '64px' : '250px'
})

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<style scoped>
.layout { height: 100vh; }
.el-aside {
  background: #001529;
  transition: width 0.3s ease;
}
.el-header {
  background: #fff;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
}
.el-main { background: #f5f5f5; }
</style>