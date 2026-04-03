import api from './auth'

export const getPolicy  = ()       => api.get('/policy')
export const updatePolicy = (data) => api.put('/policy', data)
