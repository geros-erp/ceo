import React, { useEffect, useState } from 'react'
import { toast } from 'react-hot-toast'
import { FiPackage, FiSearch, FiSave, FiX } from 'react-icons/fi'
import { getProducts, createProduct, updateProduct, deleteProduct } from '../../api/product'
import { getUnitsOfMeasure } from '../../api/unitOfMeasure'
import Layout from '../../components/Layout'
import { PageHeader, StatusBadge } from '../../components/common'

const ProductList = () => {
    const [products, setProducts] = useState([])
    const [units, setUnits] = useState([])
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [searchTerm, setSearchTerm] = useState('')

    const [modal, setModal] = useState(null)
    const [selected, setSelected] = useState(null)
    const [form, setForm] = useState({ code: '', description: '', unitOfMeasureId: '', requiresSerial: false, isActive: true })

    const loadData = async () => {
        try {
            setLoading(true)
            const [productsData, unitsData] = await Promise.all([getProducts(), getUnitsOfMeasure()])
            setProducts(productsData)
            setUnits(unitsData)
        } catch (error) {
            toast.error('Error al cargar datos')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => { loadData() }, [])

    const openCreate = () => {
        setSelected(null)
        setForm({ code: '', description: '', unitOfMeasureId: '', requiresSerial: false, isActive: true })
        setModal('create')
    }

    const openEdit = (product) => {
        setSelected(product)
        setForm({
            code: product.code,
            description: product.description,
            unitOfMeasureId: product.unitOfMeasureId,
            requiresSerial: product.requiresSerial,
            isActive: product.isActive
        })
        setModal('edit')
    }

    const closeModal = () => {
        setModal(null)
        setSelected(null)
    }

    const handleSubmit = async (e) => {
        e.preventDefault()
        setSaving(true)
        try {
            if (modal === 'create') await createProduct(form)
            else await updateProduct(selected.id, form)
            toast.success(modal === 'create' ? 'Producto creado' : 'Producto actualizado')
            closeModal()
            loadData()
        } catch (error) {
            toast.error(error.response?.data?.message || 'Error al procesar')
        } finally {
            setSaving(false)
        }
    }

    const handleDelete = async (id) => {
        if (!window.confirm('¿Eliminar este producto?')) return
        try {
            await deleteProduct(id)
            toast.success('Producto eliminado')
            loadData()
        } catch (error) {
            toast.error('No se pudo eliminar')
        }
    }

    const filtered = products.filter(p =>
        p.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
        p.description.toLowerCase().includes(searchTerm.toLowerCase())
    )

    const inputCls = "border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 w-full"

    return (
        <Layout>
            <div className="min-h-screen flex flex-col bg-gray-100">
                <PageHeader title="Gestión de Inventario" />
                <div className="p-6 max-w-6xl mx-auto w-full">
                    <div className="flex justify-between items-center mb-4">
                        <div>
                            <h2 className="text-lg font-semibold text-gray-800">Catálogo de Productos</h2>
                            <p className="text-sm text-gray-500">Administra el ciclo de vida de los productos y su trazabilidad.</p>
                        </div>
                        <button
                            onClick={openCreate}
                            className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors"
                        >
                            + Nuevo producto
                        </button>
                    </div>

                    {/* Filtros */}
                    <div className="mb-4 relative">
                        <FiSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                        <input
                            type="text"
                            placeholder="Buscar código o descripción..."
                            className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-md text-sm focus:ring-1 focus:ring-indigo-500 outline-none"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>

                    <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
                        <table className="w-full border-collapse">
                            <thead>
                                <tr className="bg-gray-50 text-left text-xs text-gray-500 uppercase tracking-wide">
                                    <th className="px-4 py-3 border-b border-gray-200">Código</th>
                                    <th className="px-4 py-3 border-b border-gray-200">Descripción</th>
                                    <th className="px-4 py-3 border-b border-gray-200">U. Medida</th>
                                    <th className="px-4 py-3 border-b border-gray-200 text-center">Estado</th>
                                    <th className="px-4 py-3 border-b border-gray-200">Serie</th>
                                    <th className="px-4 py-3 border-b border-gray-200 text-right">Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                {loading ? (
                                    <tr><td colSpan={5} className="text-center py-8 text-gray-400">Cargando...</td></tr>
                                ) : filtered.length === 0 ? (
                                    <tr><td colSpan={5} className="text-center py-8 text-gray-400">Sin registros</td></tr>
                                ) : filtered.map(product => (
                                    <tr key={product.id} className="border-b border-gray-50 hover:bg-gray-50 text-sm text-gray-700">
                                        <td className="px-4 py-3">
                                            <span className="bg-slate-100 text-slate-700 text-xs px-2 py-0.5 rounded-full font-semibold">{product.code}</span>
                                        </td>
                                        <td className="px-4 py-3 text-gray-600">{product.description}</td>
                                        <td className="px-4 py-3">
                                            <StatusBadge variant="info" label={product.unitOfMeasureName} />
                                        </td>
                                        <td className="px-4 py-3 text-center">
                                            {product.isActive ?
                                                <StatusBadge variant="success" label="ACTIVO" /> :
                                                <StatusBadge variant="default" label="INACTIVO" />
                                            }
                                        </td>
                                        <td className="px-4 py-3">
                                            {product.requiresSerial && <StatusBadge variant="warning" label="SÍ" />}
                                        </td>
                                        <td className="px-4 py-3 text-right">
                                            <div className="flex gap-1 justify-end">
                                                <button onClick={() => openEdit(product)} className="text-xs bg-indigo-50 hover:bg-indigo-100 text-indigo-600 px-2 py-1 rounded">Editar</button>
                                                <button onClick={() => handleDelete(product.id)} className="text-xs bg-red-50 hover:bg-red-100 text-red-600 px-2 py-1 rounded font-medium">Eliminar</button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            {/* Modal */}
            {modal && (
                <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-6" onClick={closeModal}>
                    <div className="bg-white rounded-xl p-7 w-full max-w-2xl shadow-xl max-h-[90vh] overflow-hidden" onClick={e => e.stopPropagation()}>
                        <h3 className="text-lg font-semibold text-gray-800 mb-1">
                            {modal === 'create' ? 'Nuevo producto' : 'Editar producto'}
                        </h3>
                        <p className="text-sm text-gray-500 mb-5">Define las características técnicas y la unidad de medida del producto.</p>

                        <form onSubmit={handleSubmit} className="space-y-5">
                            <div className="grid grid-cols-2 gap-4">
                                <div className="flex flex-col gap-1">
                                    <label className="text-xs text-gray-600">Código</label>
                                    <input type="text" required value={form.code} onChange={e => setForm({...form, code: e.target.value})} className={inputCls} />
                                </div>
                                <div className="flex flex-col gap-1">
                                    <label className="text-xs text-gray-600">Unidad de Medida</label>
                                    <select required value={form.unitOfMeasureId} onChange={e => setForm({...form, unitOfMeasureId: e.target.value})} className={inputCls}>
                                        <option value="">Seleccione...</option>
                                        {units.map(u => <option key={u.id} value={u.id}>{u.description} ({u.abbreviation})</option>)}
                                    </select>
                                </div>
                                <div className="col-span-2 flex flex-col gap-1">
                                    <label className="text-xs text-gray-600">Descripción</label>
                                    <textarea rows="3" required value={form.description} onChange={e => setForm({...form, description: e.target.value})} className={inputCls} />
                                </div>
                                <label className="col-span-2 flex items-center gap-2 text-sm cursor-pointer">
                                    <input
                                        type="checkbox"
                                        checked={form.requiresSerial}
                                        onChange={e => setForm({...form, requiresSerial: e.target.checked})}
                                    />
                                    El producto requiere control de número de serie
                                </label>
                                <label className="col-span-2 flex items-center gap-2 text-sm cursor-pointer">
                                    <input
                                        type="checkbox"
                                        checked={form.isActive}
                                        onChange={e => setForm({...form, isActive: e.target.checked})}
                                    />
                                    El producto está activo para su uso en el sistema
                                </label>
                            </div>

                            <div className="flex justify-end gap-2 pt-2">
                                <button type="button" onClick={closeModal} className="bg-gray-100 hover:bg-gray-200 text-gray-700 border border-gray-300 px-4 py-2 rounded-md text-sm font-medium">Cancelar</button>
                                <button type="submit" disabled={saving} className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white px-4 py-2 rounded-md text-sm font-medium">
                                    {saving ? 'Guardando...' : 'Guardar producto'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </Layout>
    )
}

export default ProductList
