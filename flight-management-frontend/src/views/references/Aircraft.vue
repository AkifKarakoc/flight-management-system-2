<template>
  <AppLayout>
    <PageHeader title="Uçaklar" description="Uçak filosu yönetimi">
      <template #actions>
        <el-button
          v-if="auth.isAdmin"
          type="primary"
          @click="openModal()"
        >
          <el-icon><Plus /></el-icon>
          Yeni Uçak
        </el-button>
      </template>
    </PageHeader>

    <DataTable
      :data="aircraft"
      :loading="loading"
      :total="total"
      :current-page="currentPage"
      :page-size="pageSize"
      @current-change="changePage"
      @size-change="changeSize"
    >
      <el-table-column prop="registrationNumber" label="Kayıt No" width="120" />
      <el-table-column prop="aircraftType" label="Uçak Tipi" width="120" />
      <el-table-column prop="manufacturer" label="Üretici" width="120" />
      <el-table-column prop="model" label="Model" width="120" />
      <el-table-column prop="airline.name" label="Havayolu" />
      <el-table-column prop="seatCapacity" label="Koltuk" width="80" />
      <el-table-column prop="cargoCapacity" label="Kargo" width="80" />
      <el-table-column prop="maxRange" label="Menzil (km)" width="100" />
      <el-table-column prop="status" label="Durum" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
            {{ row.status === 'ACTIVE' ? 'Aktif' : 'Pasif' }}
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
            @click="deleteAircraft(row)"
          >
            Sil
          </el-button>
        </template>
      </el-table-column>
    </DataTable>

    <FormModal
      v-model="modalVisible"
      :title="isEdit ? 'Uçak Düzenle' : 'Yeni Uçak'"
      :form="form"
      :rules="formRules"
      :loading="saving"
      @submit="saveAircraft"
      @close="closeModal"
      width="700px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Kayıt No" prop="registrationNumber">
            <el-input v-model="form.registrationNumber" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Uçak Tipi" prop="aircraftType">
            <el-input v-model="form.aircraftType" placeholder="B777, A320, etc." />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Üretici" prop="manufacturer">
            <el-select v-model="form.manufacturer" style="width: 100%" filterable>
              <el-option label="Boeing" value="Boeing" />
              <el-option label="Airbus" value="Airbus" />
              <el-option label="Embraer" value="Embraer" />
              <el-option label="Bombardier" value="Bombardier" />
              <el-option label="ATR" value="ATR" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Model" prop="model">
            <el-input v-model="form.model" placeholder="777-300ER, A320-200, etc." />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Havayolu" prop="airlineId">
            <el-select
              v-model="form.airlineId"
              style="width: 100%"
              filterable
              :loading="airlinesLoading"
            >
              <el-option
                v-for="airline in airlines"
                :key="airline.id"
                :label="airline.name"
                :value="airline.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Koltuk Kapasitesi" prop="seatCapacity">
            <el-input-number v-model="form.seatCapacity" :min="0" :max="1000" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Kargo Kapasitesi (kg)" prop="cargoCapacity">
            <el-input-number v-model="form.cargoCapacity" :min="0" :max="200000" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Maksimum Menzil (km)" prop="maxRange">
            <el-input-number v-model="form.maxRange" :min="0" :max="20000" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Üretim Tarihi" prop="manufactureDate">
            <el-input
              v-model="form.manufactureDate"
              type="date"
              style="width: 100%"
              placeholder="YYYY-MM-DD"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Son Bakım Tarihi" prop="lastMaintenance">
            <el-input
              v-model="form.lastMaintenance"
              type="date"
              style="width: 100%"
              placeholder="YYYY-MM-DD"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Durum" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="Aktif" value="ACTIVE" />
              <el-option label="Bakımda" value="MAINTENANCE" />
              <el-option label="Pasif" value="INACTIVE" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
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
  data: aircraft,
  total,
  currentPage,
  pageSize,
  fetch: fetchAircraft,
  changePage
} = usePagination(referenceAPI.getAircraft)

const modalVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const airlines = ref([])
const airlinesLoading = ref(false)

const form = reactive({
  id: null,
  registrationNumber: '',
  aircraftType: '',
  manufacturer: '',
  model: '',
  airlineId: null,
  seatCapacity: 0,
  cargoCapacity: 0,
  maxRange: 0,
  manufactureDate: null,
  lastMaintenance: null,
  status: 'ACTIVE'
})

const formRules = {
  registrationNumber: [rules.required],
  aircraftType: [rules.required],
  manufacturer: [rules.required],
  model: [rules.required],
  airlineId: [rules.required],
  status: [rules.required]
}

const changeSize = (size) => {
  pageSize.value = size
  fetchAircraft()
}

const loadAirlines = async () => {
  airlinesLoading.value = true
  try {
    const response = await referenceAPI.getAirlines({ page: 0, size: 1000 })
    airlines.value = response.content || []
  } catch (error) {
    console.error('Havayolları yüklenirken hata:', error)
  } finally {
    airlinesLoading.value = false
  }
}

const openModal = async (aircraftItem = null) => {
  isEdit.value = !!aircraftItem
  if (aircraftItem) {
    Object.assign(form, {
      ...aircraftItem,
      airlineId: aircraftItem.airline?.id
    })
  } else {
    Object.assign(form, {
      id: null,
      registrationNumber: '',
      aircraftType: '',
      manufacturer: '',
      model: '',
      airlineId: null,
      seatCapacity: 0,
      cargoCapacity: 0,
      maxRange: 0,
      manufactureDate: null,
      lastMaintenance: null,
      status: 'ACTIVE'
    })
  }

  await loadAirlines()
  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
}

const saveAircraft = async () => {
  saving.value = true
  try {
    const payload = { ...form }
    delete payload.airline // Backend'e airline object'i değil airlineId gönderiyoruz

    if (isEdit.value) {
      await referenceAPI.updateAircraft(form.id, payload)
      ElMessage.success('Uçak güncellendi')
    } else {
      await referenceAPI.createAircraft(payload)
      ElMessage.success('Uçak oluşturuldu')
    }
    closeModal()
    fetchAircraft()
  } catch (error) {
    ElMessage.error(isEdit.value ? 'Güncelleme başarısız' : 'Oluşturma başarısız')
  } finally {
    saving.value = false
  }
}

const deleteAircraft = async (aircraftItem) => {
  try {
    await ElMessageBox.confirm('Bu uçağı silmek istediğinizden emin misiniz?', 'Uyarı', {
      type: 'warning'
    })

    await referenceAPI.deleteAircraft(aircraftItem.id)
    ElMessage.success('Uçak silindi')
    fetchAircraft()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Silme işlemi başarısız')
    }
  }
}

onMounted(() => {
  withLoading(fetchAircraft)
})
</script>
