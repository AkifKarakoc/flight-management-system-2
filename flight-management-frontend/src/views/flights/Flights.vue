<template>
  <AppLayout>
    <PageHeader title="Uçuşlar" description="Uçuş operasyonları yönetimi">
      <template #actions>
        <el-button
          v-if="auth.isAdmin"
          type="primary"
          @click="openModal()"
        >
          <el-icon><Plus /></el-icon>
          Yeni Uçuş
        </el-button>
        <el-button
          v-if="auth.isAdmin"
          type="success"
          @click="showCSVUpload = true"
        >
          <el-icon><Upload /></el-icon>
          CSV Yükle
        </el-button>
      </template>
    </PageHeader>

    <!-- Filters -->
    <el-card style="margin-bottom: 20px;">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-select v-model="filters.status" placeholder="Durum" clearable @change="applyFilters">
            <el-option value="SCHEDULED" label="Planlandı" />
            <el-option value="DEPARTED" label="Kalktı" />
            <el-option value="ARRIVED" label="Vardı" />
            <el-option value="CANCELLED" label="İptal" />
            <el-option value="DELAYED" label="Gecikmeli" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filters.type" placeholder="Tür" clearable @change="applyFilters">
            <el-option value="PASSENGER" label="Yolcu" />
            <el-option value="CARGO" label="Kargo" />
            <el-option value="MIXED" label="Karma" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-input
              v-model="filters.flightNumber"
              placeholder="Uçuş No"
              clearable
              @keyup.enter="applyFilters"
          />
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="applyFilters">Filtrele</el-button>
          <el-button @click="handleClearFilters">Temizle</el-button>
        </el-col>
      </el-row>
    </el-card>

    <DataTable
        :data="flights"
        :loading="loading"
        :total="total"
        :current-page="currentPage"
        :page-size="pageSize"
        @current-change="changePage"
        @size-change="changeSize"
    >
      <el-table-column prop="flightNumber" label="Uçuş No" width="120" />
      <el-table-column prop="airline.name" label="Havayolu" width="150" />
      <el-table-column prop="aircraft.registrationNumber" label="Uçak" width="120" />
      <el-table-column label="Güzergah" width="200">
        <template #default="{ row }">
          {{ row.route?.routePath || `${row.originAirport?.iataCode} → ${row.destinationAirport?.iataCode}` || 'N/A' }}
        </template>
      </el-table-column>
      <el-table-column prop="scheduledDeparture" label="STD" width="120">
        <template #default="{ row }">
          <div>
            <span>{{ formatTime(row.scheduledDeparture) }}</span>
            <span v-if="row.actualDeparture"
                  :style="{ color: isDelayed(row.scheduledDeparture, row.actualDeparture) ? '#f56c6c' : '#67c23a' }">
              / {{ formatTime(row.actualDeparture) }}
            </span>
            <span v-else style="color: #909399"> / -</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="scheduledArrival" label="STA" width="120">
        <template #default="{ row }">
          <div>
            <span>{{ formatTime(row.scheduledArrival) }}</span>
            <span v-if="row.actualArrival"
                  :style="{ color: isDelayed(row.scheduledArrival, row.actualArrival) ? '#f56c6c' : '#67c23a' }">
              / {{ formatTime(row.actualArrival) }}
            </span>
            <span v-else style="color: #909399"> / -</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="Tür" width="140">
        <template #default="{ row }">
          <el-tag :type="getTypeColor(row.type)">
            {{ getTypeWithDetails(row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Gecikme" width="120">
        <template #default="{ row }">
          <span v-if="row.delayMinutes && row.delayMinutes > 0" style="color: #f56c6c">
            {{ row.delayMinutes }}dk
            <el-tooltip v-if="row.delayReason" :content="row.delayReason" placement="top">
              <el-icon style="margin-left: 4px;"><InfoFilled /></el-icon>
            </el-tooltip>
          </span>
          <span v-else style="color: #909399">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="Durum" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusColor(row.status)">
            {{ getStatusLabel(row.status) }}
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
              @click="deleteFlight(row)"
          >
            Sil
          </el-button>
        </template>
      </el-table-column>
    </DataTable>

    <!-- Flight Form Modal -->
    <FormModal
      v-model="modalVisible"
      :title="isEdit ? 'Uçuş Düzenle' : 'Yeni Uçuş'"
      :form="form"
      :rules="formRules"
      :loading="saving"
      @submit="saveFlight"
      @close="closeModal"
      width="800px"
    >
      <!-- Creation Mode Selector - Sadece Create Mode'da göster -->
      <template v-if="!isEdit">
        <el-divider>Uçuş Oluşturma Tipi</el-divider>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="Oluşturma Tipi" prop="creationMode">
              <el-radio-group
                v-model="form.creationMode"
                @change="onCreationModeChange"
                class="creation-mode-selector"
              >
                <el-radio-button value="ROUTE">
                  <el-icon style="margin-right: 5px;"><Location /></el-icon>
                  Mevcut Rota
                </el-radio-button>
                <el-radio-button value="AIRPORTS">
                  <el-icon style="margin-right: 5px;"><Position /></el-icon>
                  Havaalanları
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </template>

      <el-divider>Uçuş Bilgileri</el-divider>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Uçuş No" prop="flightNumber">
            <el-input v-model="form.flightNumber" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Havayolu" prop="airlineId">
            <el-select
              v-model="form.airlineId"
              style="width: 100%"
              filterable
              :loading="airlinesLoading"
              @change="onAirlineChange"
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

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Uçak" prop="aircraftId">
            <el-select
              v-model="form.aircraftId"
              style="width: 100%"
              filterable
              :loading="aircraftLoading"
              :disabled="!form.airlineId"
              placeholder="Önce havayolu seçin"
            >
              <el-option
                v-for="aircraftItem in aircraft"
                :key="aircraftItem.id"
                :label="`${aircraftItem.registrationNumber} (${aircraftItem.aircraftType})`"
                :value="aircraftItem.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <!-- Route Selection (Conditional) -->
          <el-form-item v-if="form.creationMode === 'ROUTE'" label="Rota" prop="routeId">
            <el-select
              v-model="form.routeId"
              style="width: 100%"
              filterable
              :loading="routesLoading"
              :disabled="!form.airlineId"
              placeholder="Önce havayolu seçin"
            >
              <el-option
                v-for="route in routes"
                :key="route.id"
                :label="`${route.routeCode} - ${route.routePath}`"
                :value="route.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- Airport Selection (New) -->
      <el-row :gutter="20" v-if="form.creationMode === 'AIRPORTS'">
        <el-col :span="12">
          <el-form-item label="Kalkış Havaalanı" prop="originAirportId">
            <el-select
              v-model="form.originAirportId"
              style="width: 100%"
              filterable
              :loading="airportsLoading"
              placeholder="Kalkış havaalanını seçin"
            >
              <el-option
                v-for="airport in airports"
                :key="airport.id"
                :label="`${airport.iataCode} - ${airport.name}`"
                :value="airport.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Varış Havaalanı" prop="destinationAirportId">
            <el-select
              v-model="form.destinationAirportId"
              style="width: 100%"
              filterable
              :loading="airportsLoading"
              placeholder="Varış havaalanını seçin"
              :disabled="!form.originAirportId"
            >
              <el-option
                v-for="airport in airports.filter(a => a.id !== form.originAirportId)"
                :key="airport.id"
                :label="`${airport.iataCode} - ${airport.name}`"
                :value="airport.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="Uçuş Tarihi" prop="flightDate">
            <el-input
              v-model="form.flightDate"
              type="date"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="STD" prop="scheduledDeparture">
            <el-input
              v-model="form.scheduledDeparture"
              type="time"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="STA" prop="scheduledArrival">
            <el-input
              v-model="form.scheduledArrival"
              type="time"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="Uçuş Türü" prop="type">
            <el-select v-model="form.type" style="width: 100%">
              <el-option value="PASSENGER" label="Yolcu" />
              <el-option value="CARGO" label="Kargo" />
              <el-option value="MIXED" label="Karma" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="Yolcu Sayısı" prop="passengerCount">
            <el-input-number v-model="form.passengerCount" :min="0" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="Durum" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option value="SCHEDULED" label="Planlandı" />
              <el-option value="DEPARTED" label="Kalktı" />
              <el-option value="ARRIVED" label="Vardı" />
              <el-option value="CANCELLED" label="İptal" />
              <el-option value="DELAYED" label="Gecikmeli" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </FormModal>

    <!-- CSV Upload Modal -->
    <el-dialog v-model="showCSVUpload" title="CSV Dosyası Yükle" width="500px">
      <el-upload
        drag
        :auto-upload="false"
        :on-change="handleCSVSelect"
        :file-list="csvFileList"
        accept=".csv"
      >
        <el-icon class="el-icon--upload"><Upload /></el-icon>
        <div class="el-upload__text">
          CSV dosyasını sürükleyin veya <em>tıklayarak seçin</em>
        </div>
      </el-upload>

      <template #footer>
        <el-button @click="showCSVUpload = false">İptal</el-button>
        <el-button
          type="primary"
          :loading="csvUploading"
          @click="uploadCSV"
        >
          Yükle
        </el-button>
      </template>
    </el-dialog>

    <!-- CSV Preview Modal -->
    <el-dialog v-model="showCSVPreview" title="CSV Önizleme" width="90%" top="5vh">
      <div v-if="csvPreviewData">
        <el-alert
            :title="`${csvPreviewData.validRows} geçerli, ${csvPreviewData.invalidRows} geçersiz satır`"
            :type="csvPreviewData.readyForImport ? 'success' : 'warning'"
            style="margin-bottom: 20px;"
        />

        <el-table :data="csvPreviewData.previewData" style="width: 100%" max-height="400">
          <el-table-column prop="rowNumber" label="Satır" width="60" />
          <el-table-column label="Durum" width="80">
            <template #default="{ row }">
              <el-tag :type="row.valid ? 'success' : 'danger'">
                {{ row.valid ? 'Geçerli' : 'Hata' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="parsedData.flightNumber" label="Uçuş No" width="100" />
          <el-table-column label="Havayolu" width="80">
            <template #default="{ row }">{{ row.parsedData.airlineId }}</template>
          </el-table-column>
          <el-table-column label="Güzergah" width="150">
            <template #default="{ row }">
              {{ row.parsedData.routeInfo || row.parsedData.routeInput }}
            </template>
          </el-table-column>
          <el-table-column prop="parsedData.type" label="Tür" width="100" />
          <el-table-column prop="parsedData.status" label="Durum" width="100" />
          <el-table-column label="Hatalar" min-width="200">
            <template #default="{ row }">
              <div v-if="!row.valid">
                <el-tag v-for="(error, key) in row.fieldErrors" :key="key" type="danger" size="small" style="margin: 2px;">
                  {{ error }}
                </el-tag>
              </div>
              <span v-else style="color: #67c23a;">✓ Geçerli</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="showCSVPreview = false">İptal</el-button>
        <el-button
            v-if="csvPreviewData?.readyForImport"
            type="primary"
            :loading="csvUploading"
            @click="confirmCSVUpload"
        >
          Geçerli Satırları Yükle ({{ csvPreviewData.validRows }})
        </el-button>
        <el-button
            v-else
            type="warning"
            :loading="csvUploading"
            @click="confirmCSVUpload"
        >
          Sadece Geçerli Satırları Yükle ({{ csvPreviewData.validRows }})
        </el-button>
      </template>
    </el-dialog>
  </AppLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Location, Position, InfoFilled } from '@element-plus/icons-vue'
import AppLayout from '@/components/common/AppLayout.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import DataTable from '@/components/tables/DataTable.vue'
import FormModal from '@/components/forms/FormModal.vue'
import { flightAPI, referenceAPI } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { usePagination, useLoading, rules, formatDate, formatTime } from '@/utils'
import { useCsvUpload } from '@/composables/useCsvUpload'

const auth = useAuthStore()
const { loading, withLoading } = useLoading()
const showCSVPreview = ref(false)
const csvPreviewData = ref(null)

const {
  data: flights,
  total,
  currentPage,
  pageSize,
  filters,
  fetch: fetchFlights,
  changePage,
  changeSize,
  applyFilters,
  clearFilters
} = usePagination(flightAPI.getFlights, {
  status: '',
  type: '',
  flightNumber: ''
})

const {
  uploading: csvUploading,
  fileList: csvFileList,
  handleFileSelect: handleCSVSelect,
  uploadCsv
} = useCsvUpload()

const modalVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const showCSVUpload = ref(false)

// Reference data
const airlines = ref([])
const allAircraft = ref([])
const allRoutes = ref([])
const airports = ref([])
const airlinesLoading = ref(false)
const aircraftLoading = ref(false)
const routesLoading = ref(false)
const airportsLoading = ref(false)

// Filtered data based on selected airline
const aircraft = computed(() => {
  if (!form.airlineId) return []
  return allAircraft.value.filter(a => a.airline?.id === form.airlineId)
})

const routes = computed(() => {
  if (!form.airlineId) return []
  return allRoutes.value.filter(r =>
    r.airlineId === form.airlineId || r.visibility === 'SHARED' || r.visibility === 'PUBLIC'
  )
})

const form = reactive({
  id: null,
  flightNumber: '',
  airlineId: null,
  aircraftId: null,
  // YENİ: Creation Mode
  creationMode: 'ROUTE', // Default olarak ROUTE
  // Route-based fields
  routeId: null,
  // Airport-based fields
  originAirportId: null,
  destinationAirportId: null,
  // Timing
  flightDate: '',
  scheduledDeparture: '',
  scheduledArrival: '',
  // Details
  type: 'PASSENGER',
  status: 'SCHEDULED',
  passengerCount: null,
  cargoWeight: null,
  notes: '',
  gateNumber: '',
  active: true
})

const formRules = computed(() => {
  const baseRules = {
    flightNumber: [rules.required],
    airlineId: [rules.required],
    aircraftId: [rules.required],
    creationMode: [rules.required],
    flightDate: [rules.required],
    scheduledDeparture: [rules.required],
    scheduledArrival: [rules.required],
    type: [rules.required]
  }

  // Mode'a göre koşullu validasyon
  if (form.creationMode === 'ROUTE') {
    baseRules.routeId = [rules.required]
  } else if (form.creationMode === 'AIRPORTS') {
    baseRules.originAirportId = [rules.required]
    baseRules.destinationAirportId = [rules.required]
  }

  return baseRules
})

// Load functions - DÜZELTİLDİ
const loadReferenceData = async () => {
  try {
    const [airlinesRes, aircraftRes, routesRes] = await Promise.all([
      referenceAPI.getAirlines({ page: 0, size: 1000 }),
      referenceAPI.getAircraft({ page: 0, size: 1000 }),
      referenceAPI.getRoutes({ page: 0, size: 1000 })
    ])

    airlines.value = airlinesRes.content || []
    allAircraft.value = aircraftRes.content || []
    allRoutes.value = Array.isArray(routesRes) ? routesRes : (routesRes.content || [])
  } catch (error) {
    console.error('Reference data yüklenirken hata:', error)
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

// Havayolu değiştiğinde uçak ve rotayı sıfırla
const onAirlineChange = async () => {
  form.aircraftId = null
  form.routeId = null
}

// Creation mode değiştiğinde çağrılacak
const onCreationModeChange = (mode) => {
  form.creationMode = mode
  // Mode değiştiğinde diğer alanları temizle
  if (mode === 'ROUTE') {
    form.originAirportId = null
    form.destinationAirportId = null
  } else if (mode === 'AIRPORTS') {
    form.routeId = null
  }
}

// DateTime string'den sadece saat kısmını çıkarır (HH:MM)
const extractTimeFromDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return ''

  try {
    // "2025-07-28 08:30" -> "08:30"
    if (typeof dateTimeStr === 'string' && dateTimeStr.includes(' ')) {
      return dateTimeStr.split(' ')[1]?.substring(0, 5) || ''
    }

    // ISO format veya Date object için
    const date = new Date(dateTimeStr)
    if (isNaN(date.getTime())) return ''

    const hours = date.getHours().toString().padStart(2, '0')
    const minutes = date.getMinutes().toString().padStart(2, '0')
    return `${hours}:${minutes}`
  } catch (error) {
    console.warn('Error extracting time:', error)
    return ''
  }
}

// Form'u varsayılan değerlere döndür
const resetForm = () => {
  Object.assign(form, {
    id: null,
    flightNumber: '',
    airlineId: null,
    aircraftId: null,
    creationMode: 'ROUTE',
    routeId: null,
    originAirportId: null,
    destinationAirportId: null,
    flightDate: '',
    scheduledDeparture: '',
    scheduledArrival: '',
    type: 'PASSENGER',
    status: 'SCHEDULED',
    passengerCount: null,
    cargoWeight: null,
    notes: '',
    gateNumber: '',
    active: true
  })
}

const openModal = async (flight = null) => {
  isEdit.value = !!flight

  // Load reference data - DÜZELTİLDİ
  await Promise.all([
    loadReferenceData(),
    loadAirports()
  ])

  if (flight) {
    // Edit mode - mevcut flight'ı form'a map et
    Object.assign(form, {
      id: flight.id,
      flightNumber: flight.flightNumber,
      airlineId: flight.airline?.id,
      aircraftId: flight.aircraft?.id,
      // Creation mode'u tespit et
      creationMode: flight.route ? 'ROUTE' : 'AIRPORTS',
      routeId: flight.route?.id,
      originAirportId: flight.originAirport?.id,
      destinationAirportId: flight.destinationAirport?.id,
      flightDate: flight.flightDate,
      scheduledDeparture: extractTimeFromDateTime(flight.scheduledDeparture),
      scheduledArrival: extractTimeFromDateTime(flight.scheduledArrival),
      type: flight.type,
      status: flight.status,
      passengerCount: flight.passengerCount,
      cargoWeight: flight.cargoWeight,
      notes: flight.notes,
      gateNumber: flight.gateNumber,
      active: flight.active ?? true
    })
  } else {
    // Create mode - form'u temizle
    resetForm()
  }

  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
}

const saveFlight = async () => {
  saving.value = true
  try {
    const payload = { ...form }

    // Mode'a göre gereksiz alanları temizle
    if (form.creationMode === 'ROUTE') {
      delete payload.originAirportId
      delete payload.destinationAirportId
    } else if (form.creationMode === 'AIRPORTS') {
      delete payload.routeId
    }

    if (isEdit.value) {
      await flightAPI.updateFlight(form.id, payload)
      ElMessage.success('Uçuş güncellendi')
    } else {
      await flightAPI.createFlight(payload)
      ElMessage.success('Uçuş oluşturuldu')
    }
    closeModal()
    fetchFlights()
  } catch (error) {
    console.error('Save error:', error)
    ElMessage.error(isEdit.value ? 'Güncelleme başarısız' : 'Oluşturma başarısız')
  } finally {
    saving.value = false
  }
}

// Manual clear filters handler
const handleClearFilters = () => {
  filters.status = ''
  filters.type = ''
  filters.flightNumber = ''
  applyFilters()
}

const deleteFlight = async (flight) => {
  try {
    await ElMessageBox.confirm('Bu uçuşu silmek istediğinizden emin misiniz?', 'Uyarı', {
      type: 'warning'
    })

    await flightAPI.deleteFlight(flight.id)
    ElMessage.success('Uçuş silindi')
    fetchFlights()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Silme işlemi başarısız')
    }
  }
}

const uploadCSV = async () => {
  if (!csvFileList.value.length) {
    ElMessage.error('Lütfen bir CSV dosyası seçin')
    return
  }

  csvUploading.value = true

  try {
    const formData = new FormData()
    formData.append('file', csvFileList.value[0].raw)

    const preview = await flightAPI.previewCSV(formData)

    // Preview data'yı sakla ve modal'ı göster
    csvPreviewData.value = preview
    showCSVUpload.value = false
    showCSVPreview.value = true

  } catch (error) {
    ElMessage.error('CSV önizleme hatası: ' + (error.message || 'Bilinmeyen hata'))
    console.error('CSV Preview Error:', error)
  } finally {
    csvUploading.value = false
  }
}

const confirmCSVUpload = async () => {
  if (!csvPreviewData.value) return

  csvUploading.value = true

  try {
    const validRows = csvPreviewData.value.previewData.filter(row => row.valid)
    const result = await flightAPI.confirmCSVUpload(validRows)

    ElMessage.success(`${result.successCount} uçuş başarıyla yüklendi!`)
    showCSVPreview.value = false
    csvFileList.value = []
    csvPreviewData.value = null
    fetchFlights()

  } catch (error) {
    ElMessage.error('CSV yükleme hatası: ' + (error.message || 'Bilinmeyen hata'))
  } finally {
    csvUploading.value = false
  }
}

const getTypeLabel = (type) => {
  const labels = {
    'PASSENGER': 'Yolcu',
    'CARGO': 'Kargo',
    'MIXED': 'Karma'
  }
  return labels[type] || type
}

const getTypeColor = (type) => {
  const colors = {
    'PASSENGER': 'success',
    'CARGO': 'warning',
    'MIXED': 'info'
  }
  return colors[type] || 'info'
}

const getStatusLabel = (status) => {
  const labels = {
    'SCHEDULED': 'Planlandı',
    'DEPARTED': 'Kalktı',
    'ARRIVED': 'Vardı',
    'CANCELLED': 'İptal',
    'DELAYED': 'Gecikmeli'
  }
  return labels[status] || status
}

const getStatusColor = (status) => {
  const colors = {
    'SCHEDULED': 'info',
    'DEPARTED': 'success',
    'ARRIVED': 'success',
    'CANCELLED': 'danger',
    'DELAYED': 'warning'
  }
  return colors[status] || 'info'
}

onMounted(() => {
  withLoading(fetchFlights)
})

// Helper methods for enhanced table display
const getTypeWithDetails = (row) => {
  const baseLabel = getTypeLabel(row.type)

  if (row.type === 'PASSENGER' && row.passengerCount) {
    return `${baseLabel} (${row.passengerCount})`
  }
  if (row.type === 'CARGO' && row.cargoWeight) {
    return `${baseLabel} (${row.cargoWeight}kg)`
  }
  return baseLabel
}

const isDelayed = (scheduled, actual) => {
  if (!scheduled || !actual) return false

  const scheduledTime = new Date(scheduled).getTime()
  const actualTime = new Date(actual).getTime()

  return actualTime > scheduledTime
}
</script>

<style scoped>
.creation-mode-selector {
  width: 100%;
}

.creation-mode-selector .el-radio-button {
  flex: 1;
}

.creation-mode-selector .el-radio-button__inner {
  width: 100%;
  text-align: center;
  padding: 12px 20px;
}

.el-divider {
  margin: 20px 0 15px 0;
}

.el-divider--horizontal {
  font-weight: 500;
  color: #606266;
}
</style>
