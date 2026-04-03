import api from './auth'

export const getUsers = (params) => api.get('/users', { params })
export const getUserById        = (id)      => api.get(`/users/${id}`)
export const createUser         = (data)    => api.post('/users', data)
export const updateUser         = (id, data)=> api.put(`/users/${id}`, data)
export const lockUser           = (id)      => api.put(`/users/${id}/lock`)
export const unlockUser         = (id)      => api.put(`/users/${id}/unlock`)
export const adminChangePassword= (id, data)=> api.put(`/users/${id}/password`, data)
export const deleteUser         = (id)      => api.delete(`/users/${id}`)
