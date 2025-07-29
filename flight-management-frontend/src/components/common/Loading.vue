<template>
  <div v-if="loading" class="loading-container">
    <el-loading
      :text="text"
      background="rgba(0, 0, 0, 0.7)"
    />
  </div>
</template>

<script setup>
defineProps({
  loading: { type: Boolean, default: false },
  text: { type: String, default: 'Yükleniyor...' }
})
</script>

<style scoped>
.loading-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 9999;
}
</style>
