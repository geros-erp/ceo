import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAllMenuItems, createMenuItem, updateMenuItem, deleteMenuItem, getAllPermissions, addPermission, removePermission } from '../api/menu'
import { getRoles } from '../api/roles'
import Layout from '../components/Layout'

const EMPTY = { label: '', path: '', icon: '', sortOrder: 0, active: true, parentId: null }
const EMPTY_PERMISSION = { menuItemId: '', roleId: '', canView: true, canCreate: false, canUpdate: false, canDelete: false }

export default function MenuConfig() {
  const [items, setItems] = useState([])
  const [roles, setRoles] = useState([])
  const [perms, setPerms] = useState([])
  const [modal, setModal] = useState(null)
  const [selected, setSelected] = useState(null)
  const [form, setForm] = useState(EMPTY)
  const [permForm, setPermForm] = useState(EMPTY_PERMISSION)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const navigate = useNavigate()

  const flattenTree = (nodes, depth = 0) => {
    const result = []
    for (const node of nodes) {
      result.push({ ...node, depth })
      if (node.children?.length) result.push(...flattenTree(node.children, depth + 1))
    }
    return result
  }

  const load = () => {
    getAllMenuItems().then(({ data }) => setItems(flattenTree(data))).catch(() => {})
    getAllPermissions().then(({ data }) => setPerms(data)).catch(() => {})
  }

  useEffect(() => {
    load()
    getRoles().then(({ data }) => setRoles(data)).catch(() => {})
  }, [])

  const openCreate = () => {
    setForm(EMPTY)
    setError('')
    setSelected(null)
    setModal('item')
  }

  const openEdit = (item) => {
    setSelected(item)
    setForm({ label: item.label, path: item.path || '', icon: item.icon || '', sortOrder: item.sortOrder, active: item.active, parentId: item.parentId || null })
    setError('')
    setModal('item')
  }

  const closeModal = () => {
    setModal(null)
    setSelected(null)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      const payload = { ...form, parentId: form.parentId || null }
      if (selected) await updateMenuItem(selected.id, payload)
      else await createMenuItem(payload)
      closeModal()
      load()
    } catch (err) {
      setError(err.response?.data?.message || 'Error al guardar')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar este ítem y sus hijos?')) return
    try {
      await deleteMenuItem(id)
      load()
    } catch (err) {
      alert(err.response?.data?.message)
    }
  }

  const handleAddPerm = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      await addPermission({
        menuItemId: parseInt(permForm.menuItemId),
        roleId: parseInt(permForm.roleId),
        canView: permForm.canView,
        canCreate: permForm.canCreate,
        canUpdate: permForm.canUpdate,
        canDelete: permForm.canDelete,
      })
      setPermForm(EMPTY_PERMISSION)
      load()
    } catch (err) {
      setError(err.response?.data?.message || 'Error al agregar permiso')
    } finally {
      setSaving(false)
    }
  }

  const inputCls = 'border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 w-full'
  const selectCls = 'border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500'

  return (
    <div className="min-h-screen flex flex-col bg-gray-100">
      <header className="h-14 bg-indigo-700 flex items-center gap-3 px-6 shrink-0">
        <button onClick={() => navigate('/dashboard')} className="text-xs bg-white/15 hover:bg-white/25 text-white border border-white/40 px-3 py-1.5 rounded-md">← Volver</button>
        <span className="text-white font-semibold">Configuración de Menú</span>
      </header>
      <Layout>
        <div className="p-6 max-w-6xl mx-auto w-full space-y-6">
          <div>
            <div className="flex justify-between items-center mb-3">
              <h3 className="text-sm font-semibold text-gray-700">Ítems del menú</h3>
              <button onClick={openCreate} className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-md text-sm font-medium">+ Nuevo ítem</button>
            </div>
            <div className="bg-white rounded-lg shadow-sm overflow-hidden">
              <table className="w-full border-collapse">
                <thead>
                  <tr className="bg-gray-50 text-left text-xs text-gray-500 uppercase tracking-wide">
                    {['Ítem', 'Ruta', 'Ícono', 'Orden', 'Estado', 'Perfiles y privilegios', 'Acciones'].map(header => (
                      <th key={header} className="px-4 py-3 border-b border-gray-200">{header}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {items.length === 0
                    ? <tr><td colSpan={7} className="text-center text-gray-400 py-8">Sin ítems</td></tr>
                    : items.map(item => (
                      <tr key={item.id} className="border-b border-gray-50 hover:bg-gray-50 text-sm text-gray-700">
                        <td className="px-4 py-3" style={{ paddingLeft: `${1 + item.depth * 1.25}rem` }}>{item.icon} {item.label}</td>
                        <td className="px-4 py-3 text-gray-400 text-xs">{item.path || '—'}</td>
                        <td className="px-4 py-3">{item.icon || '—'}</td>
                        <td className="px-4 py-3">{item.sortOrder}</td>
                        <td className="px-4 py-3">
                          <span className={`text-xs px-2 py-0.5 rounded-full ${item.active ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-600'}`}>
                            {item.active ? 'Activo' : 'Inactivo'}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex flex-wrap gap-1">
                            {perms.filter(permission => permission.menuItemId === item.id).map(permission => (
                              <span
                                key={permission.id}
                                onClick={() => removePermission(item.id, permission.roleId).then(load)}
                                className="text-xs bg-violet-100 text-violet-700 px-2 py-0.5 rounded-full cursor-pointer hover:bg-red-100 hover:text-red-600 transition-colors"
                                title="Click para quitar"
                              >
                                {permission.roleName} [{[
                                  permission.canView && 'V',
                                  permission.canCreate && 'C',
                                  permission.canUpdate && 'E',
                                  permission.canDelete && 'D',
                                ].filter(Boolean).join('/') || '—'}] ×
                              </span>
                            ))}
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex gap-1">
                            <button onClick={() => openEdit(item)} className="text-xs bg-indigo-50 hover:bg-indigo-100 text-indigo-600 px-2 py-1 rounded">Editar</button>
                            <button onClick={() => handleDelete(item.id)} className="text-xs bg-red-50 hover:bg-red-100 text-red-600 px-2 py-1 rounded">Eliminar</button>
                          </div>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow-sm p-5">
            <h3 className="text-sm font-semibold text-gray-700 mb-4">Asignar privilegios por perfil</h3>
            <form onSubmit={handleAddPerm} className="flex flex-wrap gap-3 items-end">
              <div className="flex flex-col gap-1">
                <label className="text-xs text-gray-600">Ítem de menú</label>
                <select value={permForm.menuItemId} onChange={e => setPermForm(current => ({ ...current, menuItemId: e.target.value }))} required className={selectCls + ' min-w-[220px]'}>
                  <option value="">Seleccionar...</option>
                  {items.map(item => <option key={item.id} value={item.id}>{'—'.repeat(item.depth)} {item.label}</option>)}
                </select>
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-xs text-gray-600">Perfil</label>
                <select value={permForm.roleId} onChange={e => setPermForm(current => ({ ...current, roleId: e.target.value }))} required className={selectCls + ' min-w-[180px]'}>
                  <option value="">Seleccionar...</option>
                  {roles.map(role => <option key={role.id} value={role.id}>{role.name}</option>)}
                </select>
              </div>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={permForm.canView} onChange={e => setPermForm(current => ({ ...current, canView: e.target.checked, ...(e.target.checked ? {} : { canCreate: false, canUpdate: false, canDelete: false }) }))} />
                Ver
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={permForm.canCreate} onChange={e => setPermForm(current => ({ ...current, canCreate: e.target.checked, canView: e.target.checked ? true : current.canView }))} />
                Crear
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={permForm.canUpdate} onChange={e => setPermForm(current => ({ ...current, canUpdate: e.target.checked, canView: e.target.checked ? true : current.canView }))} />
                Editar
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={permForm.canDelete} onChange={e => setPermForm(current => ({ ...current, canDelete: e.target.checked, canView: e.target.checked ? true : current.canView }))} />
                Eliminar
              </label>
              <button type="submit" disabled={saving} className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white px-4 py-2 rounded-md text-sm font-medium">
                {saving ? 'Guardando...' : 'Asignar'}
              </button>
            </form>
            {error && <p className="text-red-600 text-sm mt-2">{error}</p>}
          </div>
        </div>
      </Layout>

      {modal === 'item' && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={closeModal}>
          <div className="bg-white rounded-xl p-7 w-full max-w-md shadow-xl" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-gray-800 mb-5">{selected ? 'Editar ítem' : 'Nuevo ítem'}</h3>
            <form onSubmit={handleSubmit} className="space-y-3">
              <div className="flex flex-col gap-1"><label className="text-xs text-gray-600">Etiqueta</label><input value={form.label} onChange={e => setForm(current => ({ ...current, label: e.target.value }))} required className={inputCls} /></div>
              <div className="flex flex-col gap-1"><label className="text-xs text-gray-600">Ruta</label><input value={form.path} onChange={e => setForm(current => ({ ...current, path: e.target.value }))} placeholder="/ruta" className={inputCls} /></div>
              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1"><label className="text-xs text-gray-600">Ícono</label><input value={form.icon} onChange={e => setForm(current => ({ ...current, icon: e.target.value }))} placeholder="👥" className={inputCls} /></div>
                <div className="flex flex-col gap-1"><label className="text-xs text-gray-600">Orden</label><input type="number" value={form.sortOrder} onChange={e => setForm(current => ({ ...current, sortOrder: parseInt(e.target.value) }))} className={inputCls} /></div>
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-xs text-gray-600">Ítem padre</label>
                <select value={form.parentId || ''} onChange={e => setForm(current => ({ ...current, parentId: e.target.value ? parseInt(e.target.value) : null }))} className={inputCls}>
                  <option value="">Sin padre (raíz)</option>
                  {items.filter(item => item.id !== selected?.id).map(item => <option key={item.id} value={item.id}>{'—'.repeat(item.depth)} {item.label}</option>)}
                </select>
              </div>
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input type="checkbox" checked={form.active} onChange={e => setForm(current => ({ ...current, active: e.target.checked }))} />
                Activo
              </label>
              {error && <p className="text-red-600 text-sm">{error}</p>}
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={closeModal} className="bg-gray-100 hover:bg-gray-200 text-gray-700 border border-gray-300 px-4 py-2 rounded-md text-sm">Cancelar</button>
                <button type="submit" disabled={saving} className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white px-4 py-2 rounded-md text-sm">{saving ? 'Guardando...' : 'Guardar'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
