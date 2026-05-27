import React, { useEffect, useState } from 'react'
import { toast } from 'react-hot-toast'
import { getUnitsOfMeasure, createUnitOfMeasure, updateUnitOfMeasure, deleteUnitOfMeasure } from '../../api/unitOfMeasure'
import Layout from '../../components/Layout'
import { PageHeader, StatusBadge } from '../../components/common'

const UnitList = () => {
    const [units, setUnits] = useState([])
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)

    const [modal, setModal] = useState(null)
    const [selected, setSelected] = useState(null)
    const [form, setForm] = useState({ description: '', abbreviation: '', isActive: true, allowsDecimal: false })

    const loadData = async () => {
        try {
            setLoading(true)
            const data = await getUnitsOfMeasure()
            setUnits(data)
        } catch (error) {
            toast.error('Error al cargar unidades')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => { loadData() }, [])

    const openCreate = () => {
        setSelected(null)
        setForm({ description: '', abbreviation: '', isActive: true, allowsDecimal: false })
        setModal('create')
    }

    const openEdit = (unit) => {
        setSelected(unit)
        setForm({
            description: unit.description,
            abbreviation: unit.abbreviation,
            isActive: unit.isActive,
            allowsDecimal: unit.allowsDecimal
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
            if (modal === 'create') await createUnitOfMeasure(form)
            else await updateUnitOfMeasure(selected.id, form)
            toast.success(modal === 'create' ? 'Unidad creada' : 'Unidad actualizada')
            closeModal()
            loadData()
        } catch (error) {
            toast.error(error.response?.data?.message || 'Error al procesar')
        } finally {
            setSaving(false)
        }
    }

    const handleDelete = async (id) => {
        if (!window.confirm('¿Eliminar esta unidad?')) return
        try {
            await deleteUnitOfMeasure(id)
            toast.success('Unidad eliminada')
            loadData()
        } catch (error) {
            toast.error(error.response?.data?.message || 'No se pudo eliminar')
        }
    }

    const inputCls = "border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 w-full"

    return (
        <Layout>
            <div className="min-h-screen flex flex-col bg-gray-100">
                <PageHeader title="Configuración de Sistema" />
                <div className="p-6 max-w-5xl mx-auto w-full">
                    <div className="flex justify-between items-center mb-4">
                        <div>
                            <h2 className="text-lg font-semibold text-gray-800">Unidades de Medida</h2>
                            <p className="text-sm text-gray-500">Administra las magnitudes físicas para el control de inventario.</p>
                        </div>
                        <button
                            onClick={openCreate}
                            className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors"
                        >
                            + Nueva unidad
                        </button>
                    </div>

                    <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
                        <table className="w-full border-collapse">
                            <thead>
                                <tr className="bg-gray-50 text-left text-xs text-gray-500 uppercase tracking-wide">
                                    <th className="px-4 py-3 border-b border-gray-200 w-24"># ID</th>
                                    <th className="px-4 py-3 border-b border-gray-200">Descripción</th>
                                    <th className="px-4 py-3 border-b border-gray-200">Abreviatura</th>
                                    <th className="px-4 py-3 border-b border-gray-200 text-center">Decimales</th>
                                    <th className="px-4 py-3 border-b border-gray-200 text-center">Estado</th>
                                    <th className="px-4 py-3 border-b border-gray-200 text-right">Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                {loading ? (
                                    <tr><td colSpan={6} className="text-center py-8 text-gray-400">Cargando...</td></tr>
                                ) : units.length === 0 ? (
                                    <tr><td colSpan={6} className="text-center py-8 text-gray-400">Sin registros</td></tr>
                                ) : units.map(unit => (
                                    <tr key={unit.id} className="border-b border-gray-50 hover:bg-gray-50 text-sm text-gray-700">
                                        <td className="px-4 py-3 text-gray-400">#{unit.id}</td>
                                        <td className="px-4 py-3 font-semibold">{unit.description}</td>
                                        <td className="px-4 py-3">
                                            <StatusBadge variant="default" label={unit.abbreviation} />
                                        </td>
                                        <td className="px-4 py-3 text-center">
                                            {unit.allowsDecimal ?
                                                <StatusBadge variant="info" label="SÍ" /> :
                                                <StatusBadge variant="default" label="NO" />
                                            }
                                        </td>
                                        <td className="px-4 py-3 text-center">
                                            {unit.isActive ?
                                                <StatusBadge variant="success" label="ACTIVO" /> :
                                                <StatusBadge variant="default" label="INACTIVO" />
                                            }
                                        </td>
                                        <td className="px-4 py-3 text-right">
                                            <div className="flex gap-1 justify-end">
                                                <button onClick={() => openEdit(unit)} className="text-xs bg-indigo-50 hover:bg-indigo-100 text-indigo-600 px-2 py-1 rounded">Editar</button>
                                                <button onClick={() => handleDelete(unit.id)} className="text-xs bg-red-50 hover:bg-red-100 text-red-600 px-2 py-1 rounded">Eliminar</button>
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
                    <div className="bg-white rounded-xl p-7 w-full max-w-lg shadow-xl max-h-[90vh] overflow-hidden" onClick={e => e.stopPropagation()}>
                        <h3 className="text-lg font-semibold text-gray-800 mb-1">
                            {modal === 'create' ? 'Nueva unidad de medida' : 'Editar unidad de medida'}
                        </h3>
                        <p className="text-sm text-gray-500 mb-5">Define el nombre y la abreviatura para la conversión y reportes.</p>

                        <form onSubmit={handleSubmit} className="space-y-5">
                            <div className="flex flex-col gap-4">
                                <div className="flex flex-col gap-1">
                                    <label className="text-xs text-gray-600">Descripción de la unidad</label>
                                    <input type="text" required value={form.description} onChange={e => setForm({...form, description: e.target.value})} className={inputCls} placeholder="Ej: Metro" />
                                </div>
                                <div className="flex flex-col gap-1">
                                    <label className="text-xs text-gray-600">Abreviatura</label>
                                    <input type="text" required maxLength="10" value={form.abbreviation} onChange={e => setForm({...form, abbreviation: e.target.value})} className={inputCls} placeholder="Ej: MT" />
                                </div>

                                <div className="flex flex-col gap-2 pt-1 border-t border-gray-100">
                                    <label className="flex items-center gap-2 text-sm cursor-pointer py-1">
                                        <input
                                            type="checkbox"
                                            checked={form.allowsDecimal}
                                            onChange={e => setForm({...form, allowsDecimal: e.target.checked})}
                                        />
                                        Permite el uso de decimales (Cantidades fraccionadas)
                                    </label>
                                    <label className="flex items-center gap-2 text-sm cursor-pointer py-1">
                                        <input
                                            type="checkbox"
                                            checked={form.isActive}
                                            onChange={e => setForm({...form, isActive: e.target.checked})}
                                        />
                                        La unidad está activa para su uso
                                    </label>
                                </div>
                            </div>

                            <div className="flex justify-end gap-2 pt-2">
                                <button type="button" onClick={closeModal} className="bg-gray-100 hover:bg-gray-200 text-gray-700 border border-gray-300 px-4 py-2 rounded-md text-sm font-medium">Cancelar</button>
                                <button type="submit" disabled={saving} className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white px-4 py-2 rounded-md text-sm font-medium">
                                    {saving ? 'Guardando...' : 'Guardar unidad'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </Layout>
    )
}

export default UnitList
