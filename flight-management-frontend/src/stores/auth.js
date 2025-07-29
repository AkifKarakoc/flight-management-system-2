import { defineStore } from 'pinia'
import { authAPI } from '@/services/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token'),
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),

  getters: {
    isAuthenticated: state => !!state.token,
    isAdmin: state => state.user?.role === 'ADMIN'
  },

  actions: {
    async login(credentials) {
      try {
        const response = await authAPI.login(credentials)
        console.log('API Response:', response)

        // Backend JwtResponse: { token, tokenType, expiresIn }
        this.token = response.token || response.accessToken
        this.user = {
          username: credentials.username,
          role: credentials.username === 'admin' ? 'ADMIN' : 'USER'
        }

        if (!this.token) {
          throw new Error('Token not found in response')
        }

        localStorage.setItem('token', this.token)
        localStorage.setItem('user', JSON.stringify(this.user))

        console.log('Login successful, token:', this.token)
        return response
      } catch (error) {
        console.error('Login error:', error)
        throw error
      }
    },

    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
