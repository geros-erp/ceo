import api from './auth'

export const getMyMenu          = ()           => api.get('/menu/my-menu')
export const getAllMenuItems     = ()           => api.get('/menu')
export const createMenuItem     = (data)       => api.post('/menu', data)
export const updateMenuItem     = (id, data)   => api.put(`/menu/${id}`, data)
export const deleteMenuItem     = (id)         => api.delete(`/menu/${id}`)
export const getAllPermissions   = ()           => api.get('/menu/permissions')
export const getItemPermissions  = (id)        => api.get(`/menu/${id}/permissions`)
export const addPermission       = (data)      => api.post('/menu/permissions', data)
export const removePermission    = (itemId, roleId) => api.delete(`/menu/permissions/${itemId}/${roleId}`)
