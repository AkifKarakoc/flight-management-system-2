<template>
  <AppLayout>
    <PageHeader title="Havaalanları" description="Havaalanları yönetimi">
      <template #actions>
        <el-button
          v-if="auth.isAdmin"
          type="primary"
          @click="openModal()"
        >
          <el-icon><Plus /></el-icon>
          Yeni Havaalanı
        </el-button>
      </template>
    </PageHeader>

    <DataTable
      :data="airports"
      :loading="loading"
      :total="total"
      :current-page="currentPage"
      :page-size="pageSize"
      @current-change="changePage"
      @size-change="changeSize"
    >
      <el-table-column prop="iataCode" label="IATA" width="80" />
      <el-table-column prop="icaoCode" label="ICAO" width="80" />
      <el-table-column prop="name" label="Havaalanı Adı" />
      <el-table-column prop="city" label="Şehir" width="120" />
      <el-table-column prop="country" label="Ülke" width="120" />
      <el-table-column prop="type" label="Tip" width="120">
        <template #default="{ row }">
          <el-tag :type="getTypeColor(row.type)">{{ getTypeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="timezone" label="Zaman Dilimi" width="120" />
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
            @click="deleteAirport(row)"
          >
            Sil
          </el-button>
        </template>
      </el-table-column>
    </DataTable>

    <FormModal
      v-model="modalVisible"
      :title="isEdit ? 'Havaalanı Düzenle' : 'Yeni Havaalanı'"
      :form="form"
      :rules="formRules"
      :loading="saving"
      @submit="saveAirport"
      @close="closeModal"
    >
      <el-form-item label="IATA Kodu" prop="iataCode">
        <el-input v-model="form.iataCode" maxlength="3" />
      </el-form-item>
      <el-form-item label="ICAO Kodu" prop="icaoCode">
        <el-input v-model="form.icaoCode" maxlength="4" />
      </el-form-item>
      <el-form-item label="Havaalanı Adı" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="Şehir" prop="city">
        <el-input v-model="form.city" />
      </el-form-item>
      <el-form-item label="Ülke" prop="country">
        <el-input v-model="form.country" />
      </el-form-item>
      <el-form-item label="Tip" prop="type">
        <el-select v-model="form.type" style="width: 100%">
          <el-option label="Uluslararası" value="INTERNATIONAL" />
          <el-option label="İç Hat" value="DOMESTIC" />
          <el-option label="Askeri" value="MILITARY" />
          <el-option label="Özel" value="PRIVATE" />
        </el-select>
      </el-form-item>
      <el-form-item label="Zaman Dilimi" prop="timezone">
        <el-select v-model="form.timezone" style="width: 100%" filterable>
          <el-option label="UTC+3 (Turkey)" value="Europe/Istanbul" />
          <el-option label="UTC+0 (GMT)" value="GMT" />
          <el-option label="UTC+1 (CET)" value="CET" />
          <el-option label="UTC+2 (EET)" value="EET" />
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
  data: airports,
  total,
  currentPage,
  pageSize,
  fetch: fetchAirports,
  changePage
} = usePagination(referenceAPI.getAirports)

const modalVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const form = reactive({
  id: null,
  iataCode: '',
  icaoCode: '',
  name: '',
  city: '',
  country: '',
  type: 'INTERNATIONAL',
  timezone: 'Europe/Istanbul',
  active: true
})

const formRules = {
  iataCode: [rules.required, rules.iataCode],
  icaoCode: [rules.required, rules.icaoCode],
  name: [rules.required],
  city: [rules.required],
  country: [rules.required],
  type: [rules.required],
  timezone: [rules.required]
}

const changeSize = (size) => {
  pageSize.value = size
  fetchAirports()
}

const openModal = (airport = null) => {
  isEdit.value = !!airport
  if (airport) {
    Object.assign(form, airport)
  } else {
    Object.assign(form, {
      id: null,
      iataCode: '',
      icaoCode: '',
      name: '',
      city: '',
      country: '',
      type: 'INTERNATIONAL',
      timezone: 'Europe/Istanbul',
      active: true
    })
  }
  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
}

const saveAirport = async () => {
  saving.value = true
  try {
    if (isEdit.value) {
      await referenceAPI.updateAirport(form.id, form)
      ElMessage.success('Havaalanı güncellendi')
    } else {
      await referenceAPI.createAirport(form)
      ElMessage.success('Havaalanı oluşturuldu')
    }
    closeModal()
    fetchAirports()
  } catch (error) {
    ElMessage.error(isEdit.value ? 'Güncelleme başarısız' : 'Oluşturma başarısız')
  } finally {
    saving.value = false
  }
}

const deleteAirport = async (airport) => {
  try {
    await ElMessageBox.confirm('Bu havaalanını silmek istediğinizden emin misiniz?', 'Uyarı', {
      type: 'warning'
    })

    await referenceAPI.deleteAirport(airport.id)
    ElMessage.success('Havaalanı silindi')
    fetchAirports()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Silme işlemi başarısız')
    }
  }
}

const getTypeLabel = (type) => {
  const labels = {
    'INTERNATIONAL': 'Uluslararası',
    'DOMESTIC': 'İç Hat',
    'MILITARY': 'Askeri',
    'PRIVATE': 'Özel'
  }
  return labels[type] || type
}

const getTypeColor = (type) => {
  const colors = {
    'INTERNATIONAL': 'success',
    'DOMESTIC': 'info',
    'MILITARY': 'warning',
    'PRIVATE': 'danger'
  }
  return colors[type] || 'info'
}

onMounted(() => {
  withLoading(fetchAirports)
})
</script>
