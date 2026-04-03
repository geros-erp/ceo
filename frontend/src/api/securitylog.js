import api from './auth'

export const getSecurityLog = (params) => api.get('/security-log', { params })
export const getLogActions  = ()       => api.get('/security-log/actions')
export const exportSecurityLog = (params) => api.post('/security-log/exports', null, { params })
export const getSecurityLogExports = () => api.get('/security-log/exports')
export const downloadSecurityLogExport = (id) =>
  api.get(`/security-log/exports/${id}/download`, { responseType: 'blob' })
