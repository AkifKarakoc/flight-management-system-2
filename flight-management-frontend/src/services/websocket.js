import SockJS from 'sockjs-client'
import { Stomp } from '@stomp/stompjs'
import { ElMessage } from 'element-plus'
import { ref } from 'vue'

const services = {
  reference: 'http://localhost:8081/ws',
  flight: 'http://localhost:8082/ws',
  archive: 'http://localhost:8083/ws'
}

class WebSocketManager {
  constructor() {
    this.connections = new Map()
    this.isConnected = ref(false)
  }

  async connectAll() {
    const promises = Object.entries(services).map(([name, url]) =>
        this.connect(name, url)
    )
    await Promise.allSettled(promises)
    this.isConnected.value = true
    this.setupSubscriptions()
  }

  connect(serviceName, url) {
    return new Promise((resolve) => {
      const socket = new SockJS(url)
      const client = Stomp.over(socket)

      client.connect({}, () => {
        this.connections.set(serviceName, { client, subscriptions: new Map() })
        console.log(`${serviceName} connected`)
        resolve()
      }, () => resolve())
    })
  }

  setupSubscriptions() {
    // Reference updates
    this.subscribe('reference', '/topic/reference/updates', (data) => {
      ElMessage.success(`${data.entity || 'Reference'} ${data.type}`)
    })

    // Flight updates
    this.subscribe('flight', '/topic/flights', (data) => {
      ElMessage.info(`Flight ${data.flightNumber || ''} ${data.type}`)
    })

    // Archive updates
    this.subscribe('archive', '/topic/archive', (data) => {
      ElMessage.info(`Archive ${data.type}`)
    })
  }

  subscribe(service, topic, callback) {
    const conn = this.connections.get(service)
    if (conn?.client) {
      const sub = conn.client.subscribe(topic, msg => {
        callback(JSON.parse(msg.body))
      })
      conn.subscriptions.set(topic, sub)
    }
  }

  disconnect() {
    this.connections.forEach(conn => {
      conn.subscriptions.forEach(sub => sub.unsubscribe())
      conn.client.disconnect()
    })
    this.connections.clear()
    this.isConnected.value = false
  }
}

export const wsManager = new WebSocketManager()
export const useWebSocket = () => ({
  isConnected: wsManager.isConnected,
  connect: () => wsManager.connectAll(),
  disconnect: () => wsManager.disconnect()
})