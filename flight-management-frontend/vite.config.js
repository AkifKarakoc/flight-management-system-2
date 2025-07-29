import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia']
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  resolve: {
    alias: { '@': '/src' }
  },
  server: {
    port: 3000,
    proxy: {
      '/api/v1/auth': 'http://localhost:8081',
      '/api/v1/airlines': 'http://localhost:8081',
      '/api/v1/airports': 'http://localhost:8081',
      '/api/v1/aircrafts': 'http://localhost:8081',
      '/api/v1/routes': 'http://localhost:8081',
      '/api/v1/flights': 'http://localhost:8082'
    }
  },
  define: {
    global: 'globalThis'
  }
})
