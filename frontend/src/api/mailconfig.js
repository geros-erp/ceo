import api from './auth'

export const getMailConfig    = ()     => api.get('/mail-config')
export const updateMailConfig = (data) => api.put('/mail-config', data)
