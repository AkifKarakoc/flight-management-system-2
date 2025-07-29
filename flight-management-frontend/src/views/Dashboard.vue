<template>
  <AppLayout>
    <PageHeader title="Dashboard" description="Uçuş yönetim sistemi genel görünümü" />

    <div class="dashboard-grid">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ stats.totalFlights }}</div>
              <div class="stat-label">Toplam Uçuş</div>
            </div>
            <el-icon class="stat-icon flight"><Position /></el-icon>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ stats.activeAirlines }}</div>
              <div class="stat-label">Aktif Havayolu</div>
            </div>
            <el-icon class="stat-icon airline"><OfficeBuilding /></el-icon>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ stats.totalAirports }}</div>
              <div class="stat-label">Havaalanı</div>
            </div>
            <el-icon class="stat-icon airport"><Location /></el-icon>
          </el-card>
        </el-col>

        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ stats.totalAircraft }}</div>
              <div class="stat-label">Uçak</div>
            </div>
            <el-icon class="stat-icon aircraft"><Ship /></el-icon>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
          <el-card title="Son Uçuşlar">
            <template #header>
              <span>Son Uçuşlar</span>
            </template>
            <el-table :data="recentFlights" :loading="loading" style="width: 100%">
              <el-table-column prop="flightNumber" label="Uçuş No" width="120" />
              <el-table-column prop="airline.name" label="Havayolu" width="120" />
              <el-table-column label="Rota">
                <template #default="{ row }">
                  {{ row.route?.segments?.[0]?.originAirport?.iataCode }} → {{ row.route?.segments?.[row.route.segments.length-1]?.destinationAirport?.iataCode }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="Durum" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card>
            <template #header>
              <span>Hızlı İşlemler</span>
            </template>
            <div class="quick-actions">
              <el-button type="primary" @click="$router.push('/flights')">
                <el-icon><Plus /></el-icon>
                Yeni Uçuş
              </el-button>
              <el-button @click="$router.push('/airlines')">
                <el-icon><Setting /></el-icon>
                Havayolu Yönetimi
              </el-button>
              <el-button @click="$router.push('/reports')">
                <el-icon><Document /></el-icon>
                Raporlar
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </AppLayout>
</template>

<script setup>
import { Position, OfficeBuilding, Location, Ship, Plus, Setting, Document } from '@element-plus/icons-vue'
import AppLayout from '@/components/common/AppLayout.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { referenceAPI, flightAPI } from '@/services/api'
import { useLoading } from '@/utils'

const { loading, withLoading } = useLoading()

const stats = reactive({
  totalFlights: 0,
  activeAirlines: 0,
  totalAirports: 0,
  totalAircraft: 0
})

const recentFlights = ref([])

const loadStats = async () => {
  try {
    const [flights, airlines, airports, aircraft] = await Promise.allSettled([
      flightAPI.getFlights({ page: 0, size: 1 }),
      referenceAPI.getAirlines({ page: 0, size: 1 }),
      referenceAPI.getAirports({ page: 0, size: 1 }),
      referenceAPI.getAircraft({ page: 0, size: 1 })
    ])

    stats.totalFlights = flights.status === 'fulfilled' ? flights.value.totalElements : 0
    stats.activeAirlines = airlines.status === 'fulfilled' ? airlines.value.totalElements : 0
    stats.totalAirports = airports.status === 'fulfilled' ? airports.value.totalElements : 0
    stats.totalAircraft = aircraft.status === 'fulfilled' ? aircraft.value.totalElements : 0
  } catch (error) {
    console.error('Stats yüklenirken hata:', error)
  }
}

const loadRecentFlights = async () => {
  try {
    const response = await flightAPI.getFlights({ page: 0, size: 4 })
    recentFlights.value = response.content || []
  } catch (error) {
    console.error('Son uçuşlar yüklenirken hata:', error)
    recentFlights.value = []
  }
}

const getStatusType = (status) => {
  const types = {
    'SCHEDULED': 'info',
    'DEPARTED': 'success',
    'ARRIVED': 'success',
    'CANCELLED': 'danger',
    'DELAYED': 'warning'
  }
  return types[status] || 'info'
}

onMounted(() => {
  withLoading(async () => {
    await Promise.all([loadStats(), loadRecentFlights()])
  })
})
</script>

<style scoped>
.dashboard-grid {
  padding: 0 20px;
}

.stat-card {
  position: relative;
  overflow: hidden;
}

.stat-content {
  position: relative;
  z-index: 2;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.stat-icon {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 48px;
  opacity: 0.1;
}

.stat-icon.flight { color: #409eff; }
.stat-icon.airline { color: #67c23a; }
.stat-icon.airport { color: #e6a23c; }
.stat-icon.aircraft { color: #f56c6c; }

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-actions .el-button {
  justify-content: flex-start;
  width: 100%;
}
</style>
