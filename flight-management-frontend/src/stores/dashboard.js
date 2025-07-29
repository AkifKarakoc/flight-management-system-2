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
            console.log('WebSocket mesaj:', data) // Debug için

            // Reference service format: { entityType: 'AIRLINE', eventType: 'AIRLINE_CREATED', payload: {...} }
            if (data.entityType === 'AIRLINE') {
                if (data.eventType === 'AIRLINE_CREATED') {
                    airlines.value.push(data.payload)
                } else if (data.eventType === 'AIRLINE_UPDATED') {
                    const index = airlines.value.findIndex(a => a.id === data.payload.id)
                    if (index !== -1) airlines.value[index] = data.payload
                } else if (data.eventType === 'AIRLINE_DELETED') {
                    airlines.value = airlines.value.filter(a => a.id !== data.entityId)
                }
            }
        })

        // Flight updates
        wsManager.subscribe('flight', '/topic/flights', (data) => {
            console.log('Flight WebSocket mesaj:', data) // Debug için

            // Flight service format: { type: 'CREATE', entity: 'FLIGHT', data: {...} }
            if (data.entity === 'FLIGHT') {
                if (data.type === 'CREATE') {
                    flights.value.push(data.data)
                } else if (data.type === 'UPDATE') {
                    const index = flights.value.findIndex(f => f.id === data.entityId)
                    if (index !== -1) flights.value[index] = data.data
                } else if (data.type === 'DELETE') {
                    flights.value = flights.value.filter(f => f.id !== data.entityId)
                }
            }
        })
    }

    const updateStats = () => {
        stats.value.totalAirlines = airlines.value.length
        stats.value.totalFlights = flights.value.length
    }

    return {
        airlines, flights, stats,
        initWebSocketListeners, updateStats
    }
})