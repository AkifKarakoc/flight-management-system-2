// Date formatters
export const formatDate = (date, format = 'DD/MM/YYYY') => {
  if (!date) return ''
  const d = new Date(date)
  const day = String(d.getDate()).padStart(2, '0')
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const year = d.getFullYear()

  return format.replace('DD', day).replace('MM', month).replace('YYYY', year)
}

export const formatTime = (time) => {
  if (!time) return ''

  // Eğer datetime formatında geliyorsa (2025-07-28T10:30:00)
  if (time.includes('T')) {
    return new Date(time).toLocaleTimeString('tr-TR', {
      hour: '2-digit', minute: '2-digit'
    })
  }

  // Eğer sadece time formatında geliyorsa (10:30:00)
  if (time.includes(':')) {
    const [hours, minutes] = time.split(':')
    return `${hours.padStart(2, '0')}:${minutes.padStart(2, '0')}`
  }

  return time
}

// Validation rules
export const rules = {
  required: { required: true, message: 'Bu alan zorunludur' },
  email: { type: 'email', message: 'Geçerli email giriniz' },
  flightNumber: {
    pattern: /^[A-Z]{2}\d{1,4}$/,
    message: 'Uçuş numarası AA9999 formatında olmalı'
  },
  icaoCode: {
    pattern: /^[A-Z]{4}$/,
    message: 'ICAO kodu 4 harf olmalı'
  },
  iataCode: {
    pattern: /^[A-Z]{2,3}$/,
    message: 'IATA kodu 2-3 harf olmalı'
  }
}

// Debounce function
export const debounce = (fn, delay = 300) => {
  let timer
  return (...args) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}

// Loading state manager
export const useLoading = () => {
  const loading = ref(false)

  const withLoading = async (fn) => {
    loading.value = true
    try {
      return await fn()
    } finally {
      loading.value = false
    }
  }

  return { loading, withLoading }
}

// Enhanced Pagination helper with filter support
export const usePagination = (fetchFn, initialFilters = {}) => {
  const data = ref([])
  const loading = ref(false)
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(10)
  const filters = reactive({ ...initialFilters })

  const fetch = async (customFilters = {}) => {
    loading.value = true
    try {
      const params = {
        page: currentPage.value - 1,
        size: pageSize.value,
        ...filters,
        ...customFilters
      }

      // Clean empty values
      Object.keys(params).forEach(key => {
        if (params[key] === '' || params[key] === null || params[key] === undefined) {
          delete params[key]
        }
      })

      const response = await fetchFn(params)

      // Handle different response formats
      if (Array.isArray(response)) {
        data.value = response
        total.value = response.length
      } else {
        data.value = response.content || []
        total.value = response.totalElements || 0
      }
    } catch (error) {
      console.error('Pagination fetch error:', error)
      data.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  const changePage = (page) => {
    currentPage.value = page
    fetch()
  }

  const changeSize = (size) => {
    pageSize.value = size
    currentPage.value = 1
    fetch()
  }

  const applyFilters = (newFilters = {}) => {
    Object.assign(filters, newFilters)
    currentPage.value = 1
    fetch()
  }

  const clearFilters = () => {
    Object.keys(filters).forEach(key => {
      filters[key] = initialFilters[key] || ''
    })
    currentPage.value = 1
    fetch()
  }

  return {
    data,
    loading,
    total,
    currentPage,
    pageSize,
    filters,
    fetch,
    changePage,
    changeSize,
    applyFilters,
    clearFilters
  }
}

// CSV Validation utilities
export const csvValidation = {
  validateFlightNumber: (value) => {
    if (!value) return 'Uçuş numarası gerekli'
    if (!/^[A-Z]{2}\d{1,4}$/.test(value)) return 'Format: AA123'
    return null
  },

  validateDate: (value) => {
    if (!value) return 'Tarih gerekli'
    const date = new Date(value)
    if (isNaN(date.getTime())) return 'Geçersiz tarih'
    return null
  },

  validateTime: (value) => {
    if (!value) return 'Saat gerekli'
    if (!/^\d{2}:\d{2}$/.test(value)) return 'Format: HH:mm'
    return null
  },

  validateRequired: (value, field) => {
    if (!value || value.toString().trim() === '') {
      return `${field} gerekli`
    }
    return null
  }
}

// CSV processing helper
export const processCsvRow = (row, headers) => {
  const processed = {}
  headers.forEach((header, index) => {
    processed[header.trim()] = row[index]?.trim() || ''
  })
  return processed
}
