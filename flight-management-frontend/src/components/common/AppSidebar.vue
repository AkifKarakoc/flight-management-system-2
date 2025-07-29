<template>
  <div class="sidebar">
    <div class="logo">
      <span v-if="!collapsed">UYS</span>
      <span v-else>U</span>
    </div>

    <div class="collapse-btn" @click="$emit('toggle')">
      <el-icon>
        <Expand v-if="collapsed" />
        <Fold v-else />
      </el-icon>
    </div>

    <el-menu
        :default-active="$route.path"
        :collapse="collapsed"
        background-color="#001529"
        text-color="#fff"
        active-text-color="#1890ff"
        router
    >
      <el-menu-item index="/">
        <el-icon><House /></el-icon>
        <span>Dashboard</span>
      </el-menu-item>

      <el-menu-item index="/flights">
        <el-icon><Position /></el-icon>
        <span>Uçuşlar</span>
      </el-menu-item>

      <el-sub-menu index="reference">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>Referans Veriler</span>
        </template>
        <el-menu-item index="/airlines">Havayolları</el-menu-item>
        <el-menu-item index="/airports">Havaalanları</el-menu-item>
        <el-menu-item index="/aircraft">Uçaklar</el-menu-item>
        <el-menu-item index="/routes">Rotalar</el-menu-item>
      </el-sub-menu>

      <el-menu-item index="/reports">
        <el-icon><Document /></el-icon>
        <span>Raporlar</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
import { House, Position, Setting, Document, Expand, Fold } from '@element-plus/icons-vue'

defineProps({
  collapsed: {
    type: Boolean,
    default: false
  }
})

defineEmits(['toggle'])
</script>

<style scoped>
.sidebar { height: 100%; position: relative; }

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  border-bottom: 1px solid #303030;
  transition: all 0.3s ease;
}

.collapse-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  color: #fff;
  cursor: pointer;
  z-index: 10;
  padding: 5px;
  border-radius: 4px;
  transition: background-color 0.3s ease;
}

.collapse-btn:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.el-menu {
  border: none;
  transition: all 0.3s ease;
}

.el-menu--collapse {
  width: 64px;
}
</style>