import api from './auth'

export const getReservedUsernames = () => api.get('/reserved-usernames')
export const createReservedUsername = (data) => api.post('/reserved-usernames', data)
export const deleteReservedUsername = (id) => api.delete(`/reserved-usernames/${id}`)
