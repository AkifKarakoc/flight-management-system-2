<template>
  <div class="data-table">
    <el-table
      :data="data"
      :loading="loading"
      stripe
      border
      style="width: 100%"
    >
      <slot />
    </el-table>

    <el-pagination
      v-if="showPagination"
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="$emit('size-change', $event)"
      @current-change="$emit('current-change', $event)"
      class="pagination"
    />
  </div>
</template>

<script setup>
defineProps({
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  currentPage: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  showPagination: { type: Boolean, default: true }
})

defineEmits(['size-change', 'current-change'])
</script>

<style scoped>
.data-table {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
.pagination {
  padding: 20px;
  justify-content: flex-end;
}
</style>
