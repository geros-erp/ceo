import api from './auth'

/**
 * Obtiene la lista completa de productos del inventario.
 * @returns {Promise<Array>} Lista de productos (ProductDTO.Response)
 */
export const getProducts = async () => {
    const response = await api.get('/inventory/products')
    return response.data
}

/**
 * Obtiene un producto específico por su ID.
 * @param {number|string} id
 * @returns {Promise<Object>} Detalle del producto
 */
export const getProduct = async (id) => {
    const response = await api.get(`/inventory/products/${id}`)
    return response.data
}

/**
 * Crea un nuevo producto.
 * @param {Object} productData - Datos del producto (ProductDTO.Request)
 * @returns {Promise<Object>} Producto creado
 */
export const createProduct = async (productData) => {
    const response = await api.post('/inventory/products', productData)
    return response.data
}

/**
 * Actualiza un producto existente.
 * @param {number|string} id
 * @param {Object} productData
 * @returns {Promise<Object>} Producto actualizado
 */
export const updateProduct = async (id, productData) => {
    const response = await api.put(`/inventory/products/${id}`, productData)
    return response.data
}

/**
 * Elimina un producto del sistema.
 * @param {number|string} id
 */
export const deleteProduct = async (id) => {
    const response = await api.delete(`/inventory/products/${id}`)
    return response.data
}
