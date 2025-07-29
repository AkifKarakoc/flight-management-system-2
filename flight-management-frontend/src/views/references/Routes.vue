<template>
  <AppLayout>
    <PageHeader title="Rotalar" description="Uçuş rotaları yönetimi">
      <template #actions>
        <el-button
          type="primary"
          @click="openModal()"
        >
          <el-icon><Plus /></el-icon>
          Yeni Rota
        </el-button>
      </template>
    </PageHeader>

    <DataTable
      :data="routes"
      :loading="loading"
      :total="total"
      :current-page="currentPage"
      :page-size="pageSize"
      @current-change="changePage"
      @size-change="changeSize"
    >
      <el-table-column prop="routeCode" label="Rota Kodu" width="120" />
      <el-table-column prop="routeName" label="Rota Adı" />
      <el-table-column prop="routePath" label="Güzergah" />
      <el-table-column prop="totalDistance" label="Mesafe (km)" width="120" />
      <el-table-column prop="totalEstimatedTime" label="Süre (dk)" width="120" />
      <el-table-column prop="routeType" label="Tip" width="100">
        <template #default="{ row }">
          <el-tag :type="row.routeType === 'DOMESTIC' ? 'success' : 'info'">
            {{ row.routeType === 'DOMESTIC' ? 'İç Hat' : 'Dış Hat' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="visibility" label="Görünürlük" width="100">
        <template #default="{ row }">
          <el-tag :type="row.visibility === 'SHARED' ? 'success' : 'warning'">
            {{ row.visibility === 'SHARED' ? 'Paylaşılan' : 'Özel' }}
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
            size="small"
            @click="openModal(row)"
          >
            Düzenle
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="deleteRoute(row)"
          >
            Sil
          </el-button>
        </template>
      </el-table-column>
    </DataTable>

    <FormModal
      v-model="modalVisible"
      :title="isEdit ? 'Rota Düzenle' : 'Yeni Rota'"
      :form="form"
      :rules="formRules"
      :loading="saving"
      @submit="saveRoute"
      @close="closeModal"
      width="800px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Rota Kodu" prop="routeCode">
            <el-input v-model="form.routeCode" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Rota Adı" prop="routeName">
            <el-input v-model="form.routeName" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="Rota Tipi" prop="routeType">
            <el-select v-model="form.routeType" style="width: 100%">
              <el-option label="İç Hat" value="DOMESTIC" />
              <el-option label="Dış Hat" value="INTERNATIONAL" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="Görünürlük" prop="visibility">
            <el-select v-model="form.visibility" style="width: 100%">
              <el-option label="Paylaşılan" value="SHARED" />
              <el-option label="Özel" value="PRIVATE" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
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
      </el-row>

      <el-divider>Segment Bilgileri</el-divider>

      <div v-for="(segment, index) in form.segments" :key="index" class="segment-row">
        <el-row :gutter="20" align="middle">
          <el-col :span="2">
            <span>{{ index + 1 }}.</span>
          </el-col>
          <el-col :span="5">
            <el-select
              v-model="segment.originAirportId"
              placeholder="Kalkış"
              filterable
              :loading="airportsLoading"
              @change="onOriginAirportChange(index)"
            >
              <el-option
                v-for="airport in getAvailableOriginAirports(index)"
                :key="airport.id"
                :label="`${airport.iataCode} - ${airport.name}`"
                :value="airport.id"
              />
            </el-select>
          </el-col>
          <el-col :span="5">
            <el-select
              v-model="segment.destinationAirportId"
              placeholder="Varış"
              filterable
              :loading="airportsLoading"
              @change="onDestinationAirportChange(index)"
            >
              <el-option
                v-for="airport in getAvailableDestinationAirports(index)"
                :key="airport.id"
                :label="`${airport.iataCode} - ${airport.name}`"
                :value="airport.id"
              />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-input-number
              v-model="segment.distance"
              placeholder="Mesafe (km)"
              :min="1"
              style="width: 100%"
            />
          </el-col>
          <el-col :span="4">
            <el-input-number
              v-model="segment.estimatedFlightTime"
              placeholder="Süre (dk)"
              :min="1"
              style="width: 100%"
            />
          </el-col>
          <el-col :span="2">
            <el-switch v-model="segment.active" />
          </el-col>
          <el-col :span="2">
            <el-button
              v-if="form.segments.length > 1"
              size="small"
              type="danger"
              @click="removeSegment(index)"
            >
              Sil
            </el-button>
          </el-col>
        </el-row>
      </div>

      <el-button @click="addSegment" style="margin-top: 10px;">
        <el-icon><Plus /></el-icon>
        Segment Ekle
      </el-button>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
          <el-form-item label="Durum">
            <el-switch v-model="form.active" />
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

const routes = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const fetchRoutes = async () => {
  try {
    const response = await referenceAPI.getRoutes({
      page: currentPage.value - 1,
      size: pageSize.value
    })

    // Backend direkt array dönüyor, pagination wrapper yok
    if (Array.isArray(response)) {
      routes.value = response
      total.value = response.length
    } else {
      // Eğer pagination wrapper varsa
      routes.value = response.content || []
      total.value = response.totalElements || 0
    }
  } catch (error) {
    console.error('Routes yüklenirken hata:', error)
    routes.value = []
    total.value = 0
  }
}

const changePage = (page) => {
  currentPage.value = page
  fetchRoutes()
}

const modalVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const airlines = ref([])
const airports = ref([])
const airlinesLoading = ref(false)
const airportsLoading = ref(false)

const form = reactive({
  id: null,
  routeCode: '',
  routeName: '',
  routeType: 'DOMESTIC',
  visibility: 'SHARED',
  airlineId: null,
  active: true,
  segments: [
    {
      segmentOrder: 1,
      originAirportId: null,
      destinationAirportId: null,
      distance: null,
      estimatedFlightTime: null,
      active: true
    }
  ]
})

const formRules = {
  routeCode: [rules.required],
  routeName: [rules.required],
  routeType: [rules.required],
  visibility: [rules.required],
  airlineId: [rules.required]
}

const changeSize = (size) => {
  pageSize.value = size
  fetchRoutes()
}

const loadAirlines = async () => {
  airlinesLoading.value = true
  try {
    const response = await referenceAPI.getAirlines({ page: 0, size: 1000 })
    airlines.value = response.content || []
  } finally {
    airlinesLoading.value = false
  }
}

const loadAirports = async () => {
  airportsLoading.value = true
  try {
    const response = await referenceAPI.getAirports({ page: 0, size: 1000 })
    airports.value = response.content || []
  } finally {
    airportsLoading.value = false
  }
}

const addSegment = () => {
  const lastSegment = form.segments[form.segments.length - 1]
  form.segments.push({
    segmentOrder: form.segments.length + 1,
    originAirportId: lastSegment?.destinationAirportId || null, // Son segment'in varışı yeni segment'in kalkışı
    destinationAirportId: null,
    distance: null,
    estimatedFlightTime: null,
    active: true
  })
}

// Kullanılan havaalanlarını filtrele
const getAvailableOriginAirports = (segmentIndex) => {
  if (segmentIndex === 0) return airports.value // İlk segment için tüm havaalanları

  // Diğer segmentler için önceki segment'in varışı otomatik seçili olmalı
  const prevSegment = form.segments[segmentIndex - 1]
  return airports.value.filter(airport => airport.id === prevSegment?.destinationAirportId)
}

const getAvailableDestinationAirports = (segmentIndex) => {
  const usedAirportIds = new Set()

  // Bu segment'e kadar kullanılan tüm havaalanlarını topla
  for (let i = 0; i <= segmentIndex; i++) {
    const segment = form.segments[i]
    if (segment.originAirportId) usedAirportIds.add(segment.originAirportId)
    if (i < segmentIndex && segment.destinationAirportId) {
      usedAirportIds.add(segment.destinationAirportId) // Önceki segment'lerin varışları
    }
  }

  // Kullanılmamış havaalanlarını döndür
  return airports.value.filter(airport => !usedAirportIds.has(airport.id))
}

// Segment origin değiştiğinde diğer segmentleri güncelle
const onOriginAirportChange = (segmentIndex) => {
  // Bu segment'ten sonraki segmentlerin kalkış noktalarını güncelle
  for (let i = segmentIndex + 1; i < form.segments.length; i++) {
    const prevSegment = form.segments[i - 1]
    form.segments[i].originAirportId = prevSegment.destinationAirportId
  }
}

// Segment destination değiştiğinde sonraki segmentleri güncelle
const onDestinationAirportChange = (segmentIndex) => {
  // Bu segment'ten sonraki segmentlerin kalkış noktalarını güncelle
  for (let i = segmentIndex + 1; i < form.segments.length; i++) {
    const prevSegment = form.segments[i - 1]
    form.segments[i].originAirportId = prevSegment.destinationAirportId
  }
}

const removeSegment = (index) => {
  form.segments.splice(index, 1)
  // Segment order'ları yeniden düzenle
  form.segments.forEach((segment, idx) => {
    segment.segmentOrder = idx + 1
  })
}

const openModal = async (route = null) => {
  isEdit.value = !!route
  if (route) {
    Object.assign(form, {
      ...route,
      segments: route.segments?.map(segment => ({
        segmentOrder: segment.segmentOrder,
        originAirportId: segment.originAirport?.id,
        destinationAirportId: segment.destinationAirport?.id,
        distance: segment.distance,
        estimatedFlightTime: segment.estimatedFlightTime,
        active: segment.active
      })) || [
        {
          segmentOrder: 1,
          originAirportId: null,
          destinationAirportId: null,
          distance: null,
          estimatedFlightTime: null,
          active: true
        }
      ]
    })
  } else {
    Object.assign(form, {
      id: null,
      routeCode: '',
      routeName: '',
      routeType: 'DOMESTIC',
      visibility: 'SHARED',
      airlineId: null,
      active: true,
      segments: [
        {
          segmentOrder: 1,
          originAirportId: null,
          destinationAirportId: null,
          distance: null,
          estimatedFlightTime: null,
          active: true
        }
      ]
    })
  }

  await Promise.all([loadAirlines(), loadAirports()])
  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
}

const saveRoute = async () => {
  saving.value = true
  try {
    if (isEdit.value) {
      await referenceAPI.updateRoute(form.id, form)
      ElMessage.success('Rota güncellendi')
    } else {
      await referenceAPI.createRoute(form)
      ElMessage.success('Rota oluşturuldu')
    }
    closeModal()
    fetchRoutes()
  } catch (error) {
    ElMessage.error(isEdit.value ? 'Güncelleme başarısız' : 'Oluşturma başarısız')
  } finally {
    saving.value = false
  }
}

const deleteRoute = async (route) => {
  try {
    await ElMessageBox.confirm('Bu rotayı silmek istediğinizden emin misiniz?', 'Uyarı', {
      type: 'warning'
    })

    await referenceAPI.deleteRoute(route.id)
    ElMessage.success('Rota silindi')
    fetchRoutes()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Silme işlemi başarısız')
    }
  }
}

onMounted(() => {
  withLoading(fetchRoutes)
})
</script>

<style scoped>
.segment-row {
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #f9f9f9;
}
</style>
