import api from './auth'

export const getPasswordHistory = (userId) => api.get(`/users/${userId}/password-history`)
