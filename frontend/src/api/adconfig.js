import api from './auth'

export const getAdConfig    = ()     => api.get('/ad-config')
export const updateAdConfig = (data) => api.put('/ad-config', data)
