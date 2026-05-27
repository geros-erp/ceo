import api from './auth'

export const getUnitsOfMeasure = async () => {
    const response = await api.get('/common/units')
    return response.data
}

export const getUnitOfMeasure = async (id) => {
    const response = await api.get(`/common/units/${id}`)
    return response.data
}

export const createUnitOfMeasure = async (unitData) => {
    const response = await api.post('/common/units', unitData)
    return response.data
}

export const updateUnitOfMeasure = async (id, unitData) => {
    const response = await api.put(`/common/units/${id}`, unitData)
    return response.data
}

export const deleteUnitOfMeasure = async (id) => {
    const response = await api.delete(`/common/units/${id}`)
    return response.data
}
