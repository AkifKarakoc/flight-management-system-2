import { defineStore } from 'pinia'
import { ref } from 'vue'
import { wsManager } from '@/services/websocket'

export const useDashboardStore = defineStore('dashboard', () => {
    const airlines = ref([])
    const flights = ref([])
    const stats = ref({})

    const initWebSocketListeners = () => {
        // Reference data updates
        wsManager.subscribe('reference', '/topic/reference/updates', (data) => {
            if (data.entity === 'AIRLINE') {
                if (data.type === 'CREATE') airlines.value.push(data.data)
                else if (data.type === 'UPDATE') {
                    const index = airlines.value.findIndex(a => a.id === data.entityId)
                    if (index !== -1) airlines.value[index] = data.data
                }
                else if (data.type === 'DELETE') {
                    airlines.value = airlines.value.filter(a => a.id !== data.entityId)
                }
            }
        })

        // Flight updates
        wsManager.subscribe('flight', '/topic/flights', (data) => {
            if (data.type === 'CREATE') flights.value.push(data.data)
            else if (data.type === 'UPDATE') {
                const index = flights.value.findIndex(f => f.id === data.entityId)
                if (index !== -1) flights.value[index] = data.data
            }
            else if (data.type === 'DELETE') {
                flights.value = flights.value.filter(f => f.id !== data.entityId)
            }
        })
    }

    return {
        airlines, flights, stats,
        initWebSocketListeners
    }
})