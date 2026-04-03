import api from './auth'

export const getRoles    = ()       => api.get('/roles')
export const getRolePrivilegeCatalog = () => api.get('/roles/privilege-catalog')
export const getRolePermissions = (id) => api.get(`/roles/${id}/permissions`)
export const getRoleUsers = (id) => api.get(`/roles/${id}/users`)
export const createRole  = (data)   => api.post('/roles', data)
export const updateRole  = (id, data) => api.put(`/roles/${id}`, data)
export const deleteRole  = (id)     => api.delete(`/roles/${id}`)
