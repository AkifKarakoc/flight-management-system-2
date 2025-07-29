import SockJS from 'sockjs-client'
import { Stomp } from '@stomp/stompjs'
import { ElMessage } from 'element-plus'

class WebSocketService {
  constructor() {
    this.client = null
    this.connected = false
    this.subscriptions = new Map()
  }

  connect() {
    return new Promise((resolve, reject) => {
      try {
        const socket = new SockJS('/ws')
        this.client = Stomp.over(socket)

        this.client.connect(
          {},
          () => {
            this.connected = true
            console.log('WebSocket connected')
            resolve()
          },
          error => {
            console.error('WebSocket connection error:', error)
            reject(error)
          }
        )
      } catch (error) {
        reject(error)
      }
    })
  }

  subscribe(topic, callback) {
    if (!this.connected) {
      console.warn('WebSocket not connected')
      return
    }

    const subscription = this.client.subscribe(topic, message => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch (error) {
        console.error('Error parsing WebSocket message:', error)
      }
    })

    this.subscriptions.set(topic, subscription)
    return subscription
  }

  unsubscribe(topic) {
    const subscription = this.subscriptions.get(topic)
    if (subscription) {
      subscription.unsubscribe()
      this.subscriptions.delete(topic)
    }
  }

  disconnect() {
    if (this.client) {
      this.subscriptions.forEach(sub => sub.unsubscribe())
      this.subscriptions.clear()
      this.client.disconnect()
      this.connected = false
    }
  }
}

export const wsService = new WebSocketService()

// Composable for easy usage
export const useWebSocket = () => {
  const isConnected = ref(false)

  const connect = async () => {
    try {
      await wsService.connect()
      isConnected.value = true
    } catch (error) {
      ElMessage.error('WebSocket bağlantısı kurulamadı')
    }
  }

  const subscribe = (topic, callback) => wsService.subscribe(topic, callback)
  const unsubscribe = topic => wsService.unsubscribe(topic)

  onUnmounted(() => {
    wsService.disconnect()
    isConnected.value = false
  })

  return { isConnected, connect, subscribe, unsubscribe }
}
