import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

// Request interceptor
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor
api.interceptors.response.use(
  response => response.data,
  error => {
    const message = error.response?.data?.message || 'Bir hata oluştu'
    ElMessage.error(message)

    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }

    return Promise.reject(error)
  }
)

// API Methods
export const authAPI = {
  login: data => api.post('/auth/login', data),
  logout: () => api.post('/auth/logout')
}

export const referenceAPI = {
  // Airlines
  getAirlines: params => api.get('/airlines', { params }),
  createAirline: data => api.post('/airlines', data),
  updateAirline: (id, data) => api.put(`/airlines/${id}`, data),
  deleteAirline: id => api.delete(`/airlines/${id}`),

  // Airports
  getAirports: params => api.get('/airports', { params }),
  createAirport: data => api.post('/airports', data),
  updateAirport: (id, data) => api.put(`/airports/${id}`, data),
  deleteAirport: id => api.delete(`/airports/${id}`),

  // Aircraft - DÜZELTME: /aircraft -> /aircrafts
  getAircraft: params => api.get('/aircrafts', { params }),
  createAircraft: data => api.post('/aircrafts', data),
  updateAircraft: (id, data) => api.put(`/aircrafts/${id}`, data),
  deleteAircraft: id => api.delete(`/aircrafts/${id}`),

  // Routes
  getRoutes: params => api.get('/routes', { params }),
  createRoute: data => api.post('/routes', data),
  updateRoute: (id, data) => api.put(`/routes/${id}`, data),
  deleteRoute: id => api.delete(`/routes/${id}`)
}

export const flightAPI = {
  getFlights: params => api.get('/flights', { params }),
  createFlight: data => api.post('/flights', data),
  updateFlight: (id, data) => api.put(`/flights/${id}`, data),
  deleteFlight: id => api.delete(`/flights/${id}`),
  previewCSV: (formData) => api.post('/flights/upload/preview', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),

  confirmCSVUpload: (validRows) => api.post('/flights/upload/confirm', validRows),

  downloadCSVTemplate: () => api.get('/flights/csv-template', {
    responseType: 'blob'
  })
}

export default api
