import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

function createTransactionId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `tx-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (!config.headers['X-Transaction-Id']) {
    config.headers['X-Transaction-Id'] = createTransactionId()
  }
  
  // Validar que existe token para endpoints protegidos
  if (!config.url.includes('/auth/login') && 
      !config.url.includes('/auth/forgot-password') && 
      !config.url.includes('/auth/reset-password')) {
    if (!token) {
      console.error('❌ Intento de transacción sin autenticación:', config.url)
      window.location.href = '/login'
      return Promise.reject(new Error('No autenticado'))
    }
  }
  
  if (token) config.headers.Authorization = `Bearer ${token}`
  console.log('API Request:', config.method.toUpperCase(), config.url, config.data)
  return config
})

api.interceptors.response.use(
  (response) => {
    console.log('API Response:', response.status, response.config.url)
    return response
  },
  (error) => {
    console.error('API Error:', error.response?.status, error.response?.data)
    
    // Si recibimos 401, el token es inválido o expiró
    if (error.response?.status === 401) {
      console.error('❌ Token inválido o expirado, redirigiendo a login')
      localStorage.clear()
      sessionStorage.clear()
      window.location.href = '/login'
    }
    
    return Promise.reject(error)
  }
)

export const login = (username, password, recaptchaToken) =>
  api.post('/auth/login', { username, password, recaptchaToken })

export const forgotPassword = (email, recaptchaToken) =>
  api.post('/auth/forgot-password', { email, recaptchaToken })

export const resetPassword = (token, newPassword) =>
  api.post('/auth/reset-password', { token, newPassword })

export const logout = () =>
  api.post('/auth/logout')

export default api
