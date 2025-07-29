<template>
  <AppLayout>
    <PageHeader title="Havayolları" description="Havayolu şirketleri yönetimi">
      <template #actions>
        <el-button
          v-if="auth.isAdmin"
          type="primary"
          @click="openModal()"
        >
          <el-icon><Plus /></el-icon>
          Yeni Havayolu
        </el-button>
      </template>
    </PageHeader>

    <DataTable
      :data="airlines"
      :loading="loading"
      :total="total"
      :current-page="currentPage"
      :page-size="pageSize"
      @current-change="changePage"
      @size-change="changeSize"
    >
      <el-table-column prop="iataCode" label="IATA" width="80" />
      <el-table-column prop="icaoCode" label="ICAO" width="80" />
      <el-table-column prop="name" label="Havayolu Adı" />
      <el-table-column prop="country" label="Ülke" width="120" />
      <el-table-column prop="type" label="Tür" width="120">
        <template #default="{ row }">
          <el-tag :type="row.type === 'FULL_SERVICE' ? 'success' : 'info'">
            {{ row.type === 'FULL_SERVICE' ? 'Full Service' : 'Low Cost' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="active" label="Durum" width="100">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'danger'">
            {{ row.active ? 'Aktif' : 'Pasif' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="İşlemler" width="180">
        <template #default="{ row }">
          <el-button
            v-if="auth.isAdmin"
            size="small"
            @click="openModal(row)"
          >
            Düzenle
          </el-button>
          <el-button
            v-if="auth.isAdmin"
            size="small"
            type="danger"
            @click="deleteAirline(row)"
          >
            Sil
          </el-button>
        </template>
      </el-table-column>
    </DataTable>

    <FormModal
      v-model="modalVisible"
      :title="isEdit ? 'Havayolu Düzenle' : 'Yeni Havayolu'"
      :form="form"
      :rules="formRules"
      :loading="saving"
      @submit="saveAirline"
      @close="closeModal"
    >
      <el-form-item label="IATA Kodu" prop="iataCode">
        <el-input v-model="form.iataCode" maxlength="3" />
      </el-form-item>
      <el-form-item label="ICAO Kodu" prop="icaoCode">
        <el-input v-model="form.icaoCode" maxlength="4" />
      </el-form-item>
      <el-form-item label="Havayolu Adı" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="Ülke" prop="country">
        <el-input v-model="form.country" />
      </el-form-item>
      <el-form-item label="Tür" prop="type">
        <el-select v-model="form.type" style="width: 100%">
          <el-option label="Full Service" value="FULL_SERVICE" />
          <el-option label="Low Cost" value="LOW_COST" />
        </el-select>
      </el-form-item>
      <el-form-item label="Durum">
        <el-switch v-model="form.active" />
      </el-form-item>
    </FormModal>
  </AppLayout>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
import AppLayout from '@/components/common/AppLayout.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import DataTable from '@/components/tables/DataTable.vue'
import FormModal from '@/components/forms/FormModal.vue'
import { referenceAPI } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { usePagination, useLoading, rules } from '@/utils'

const auth = useAuthStore()
const { loading, withLoading } = useLoading()

const {
  data: airlines,
  total,
  currentPage,
  pageSize,
  fetch: fetchAirlines,
  changePage
} = usePagination(referenceAPI.getAirlines)

const modalVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const form = reactive({
  id: null,
  iataCode: '',
  icaoCode: '',
  name: '',
  country: '',
  type: 'FULL_SERVICE',
  active: true
})

const formRules = {
  iataCode: [rules.required, rules.iataCode],
  icaoCode: [rules.required, { pattern: /^[A-Z]{3,4}$/, message: 'ICAO kodu 3-4 harf olmalı' }],
  name: [rules.required],
  country: [rules.required],
  type: [rules.required]
}

const changeSize = (size) => {
  pageSize.value = size
  fetchAirlines()
}

const openModal = (airline = null) => {
  isEdit.value = !!airline
  if (airline) {
    Object.assign(form, airline)
  } else {
    Object.assign(form, {
      id: null,
      iataCode: '',
      icaoCode: '',
      name: '',
      country: '',
      type: 'FULL_SERVICE',
      active: true
    })
  }
  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
}

const saveAirline = async () => {
  saving.value = true
  try {
    if (isEdit.value) {
      await referenceAPI.updateAirline(form.id, form)
      ElMessage.success('Havayolu güncellendi')
    } else {
      await referenceAPI.createAirline(form)
      ElMessage.success('Havayolu oluşturuldu')
    }
    closeModal()
    fetchAirlines()
  } catch (error) {
    ElMessage.error(isEdit.value ? 'Güncelleme başarısız' : 'Oluşturma başarısız')
  } finally {
    saving.value = false
  }
}

const deleteAirline = async (airline) => {
  try {
    await ElMessageBox.confirm('Bu havayolunu silmek istediğinizden emin misiniz?', 'Uyarı', {
      type: 'warning'
    })

    await referenceAPI.deleteAirline(airline.id)
    ElMessage.success('Havayolu silindi')
    fetchAirlines()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Silme işlemi başarısız')
    }
  }
}

onMounted(() => {
  withLoading(fetchAirlines)
})
</script>
