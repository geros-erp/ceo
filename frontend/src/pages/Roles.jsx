import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getRoles, getRolePrivilegeCatalog, getRolePermissions, getRoleUsers, createRole, updateRole, deleteRole } from '../api/roles'
import Layout from '../components/Layout'

const PRIVILEGED_PATHS = ['/security-log']
const AUDIT_PATHS = ['/security-log', '/password-history']
const PROFILE_TYPE_OPTIONS = [
  { value: 'STANDARD', label: 'Estandar' },
  { value: 'READ_ONLY', label: 'Solo consulta' },
  { value: 'AUDIT', label: 'Auditoria' },
]

function buildCatalogRows(items) {
  const byParent = items.reduce((acc, item) => {
    const key = item.parentId ?? 'root'
    acc[key] = acc[key] || []
    acc[key].push(item)
    return acc
  }, {})

  Object.values(byParent).forEach(group => group.sort((a, b) => {
    if (a.sortOrder !== b.sortOrder) return a.sortOrder - b.sortOrder
    return a.menuItemId - b.menuItemId
  }))

  const rows = []
  const visit = (parentId = 'root', depth = 0) => {
    for (const item of byParent[parentId] || []) {
      rows.push({ ...item, depth })
      visit(item.menuItemId, depth + 1)
    }
  }
  visit()
  return rows
}

function mergePermissions(catalogRows, permissions = []) {
  const permissionMap = new Map(permissions.map(permission => [permission.menuItemId, permission]))
  return catalogRows.map(item => {
    const permission = permissionMap.get(item.menuItemId)
    return {
      menuItemId: item.menuItemId,
      menuItemLabel: item.label,
      menuItemPath: item.path,
      parentId: item.parentId,
      active: item.active,
      depth: item.depth,
      canView: permission?.canView ?? false,
      canCreate: permission?.canCreate ?? false,
      canUpdate: permission?.canUpdate ?? false,
      canDelete: permission?.canDelete ?? false,
    }
  })
}

export default function Roles() {
  const [roles, setRoles] = useState([])
  const [catalog, setCatalog] = useState([])
  const [modal, setModal] = useState(null)
  const [selected, setSelected] = useState(null)
  const [form, setForm] = useState({ name: '', description: '', profileType: 'STANDARD', privileged: false, permissions: [] })
  const [queryModal, setQueryModal] = useState(null)
  const [queryRole, setQueryRole] = useState(null)
  const [queryRows, setQueryRows] = useState([])
  const [error, setError] = useState('')
  const [queryLoading, setQueryLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const navigate = useNavigate()

  const catalogRows = useMemo(() => buildCatalogRows(catalog), [catalog])

  const load = () => getRoles().then(({ data }) => setRoles(data)).catch(() => {})

  useEffect(() => {
    load()
    getRolePrivilegeCatalog().then(({ data }) => setCatalog(data)).catch(() => {})
  }, [])

  useEffect(() => {
    if (!modal || catalogRows.length === 0) return
    setForm(current => {
      if (!current.name && !current.description && current.permissions.length === 0) {
        return { ...current, permissions: mergePermissions(catalogRows) }
      }
      return {
        ...current,
        permissions: mergePermissions(catalogRows, current.permissions),
      }
    })
  }, [catalogRows, modal])

  const openCreate = () => {
    setSelected(null)
    setError('')
    setForm({ name: '', description: '', profileType: 'STANDARD', privileged: false, permissions: mergePermissions(catalogRows) })
    setModal('create')
  }

  const openEdit = (role) => {
    setSelected(role)
    setError('')
    setForm({
      name: role.name,
      description: role.description || '',
      profileType: role.profileType || 'STANDARD',
      privileged: role.privileged || false,
      permissions: mergePermissions(catalogRows, role.permissions || []),
    })
    setModal('edit')
  }

  const closeModal = () => {
    setModal(null)
    setSelected(null)
  }

  const openPermissionsQuery = async (role) => {
    setQueryRole(role)
    setQueryModal('permissions')
    setQueryLoading(true)
    try {
      const { data } = await getRolePermissions(role.id)
      setQueryRows(data)
    } catch {
      setQueryRows([])
    } finally {
      setQueryLoading(false)
    }
  }

  const openUsersQuery = async (role) => {
    setQueryRole(role)
    setQueryModal('users')
    setQueryLoading(true)
    try {
      const { data } = await getRoleUsers(role.id)
      setQueryRows(data)
    } catch {
      setQueryRows([])
    } finally {
      setQueryLoading(false)
    }
  }

  const updatePermission = (menuItemId, field, checked) => {
    setForm(current => ({
      ...current,
      permissions: current.permissions.map(permission => {
        if (permission.menuItemId !== menuItemId) return permission

        if (field === 'canView' && !checked) {
          return { ...permission, canView: false, canCreate: false, canUpdate: false, canDelete: false }
        }

        const next = { ...permission, [field]: checked }
        if (form.profileType !== 'STANDARD' && (field === 'canCreate' || field === 'canUpdate' || field === 'canDelete')) {
          next.canCreate = false
          next.canUpdate = false
          next.canDelete = false
          next.canView = checked ? true : next.canView
          return next
        }
        if ((field === 'canCreate' || field === 'canUpdate' || field === 'canDelete') && checked) {
          next.canView = true
        }
        return next
      }),
    }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      const payload = {
        name: form.name,
        description: form.description,
        profileType: form.profileType,
        privileged: form.profileType === 'AUDIT' ? true : form.privileged,
        permissions: form.permissions
          .filter(permission => {
            if (form.profileType === 'AUDIT') return !permission.menuItemPath || AUDIT_PATHS.includes(permission.menuItemPath)
            return form.privileged || !PRIVILEGED_PATHS.includes(permission.menuItemPath)
          })
          .map(permission => ({
            menuItemId: permission.menuItemId,
            canView: permission.canView,
            canCreate: form.profileType === 'STANDARD' ? permission.canCreate : false,
            canUpdate: form.profileType === 'STANDARD' ? permission.canUpdate : false,
            canDelete: form.profileType === 'STANDARD' ? permission.canDelete : false,
          })),
      }
      if (modal === 'create') await createRole(payload)
      else await updateRole(selected.id, payload)
      closeModal()
      load()
    } catch (err) {
      setError(err.response?.data?.message || 'Error al guardar el perfil')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar este perfil?')) return
    try {
      await deleteRole(id)
      load()
    } catch (err) {
      alert(err.response?.data?.message || 'Error')
    }
  }

  const permissionSummary = (role) => {
    const permissions = role.permissions || []
    return {
      modules: permissions.filter(permission => permission.canView).length,
      creates: permissions.filter(permission => permission.canCreate).length,
      updates: permissions.filter(permission => permission.canUpdate).length,
      deletes: permissions.filter(permission => permission.canDelete).length,
    }
  }

  const visiblePermissions = form.permissions.filter(permission => (
    form.profileType === 'AUDIT'
      ? (!permission.menuItemPath || AUDIT_PATHS.includes(permission.menuItemPath))
      : (form.privileged || !PRIVILEGED_PATHS.includes(permission.menuItemPath))
  ))

  const isReadOnlyProfile = form.profileType === 'READ_ONLY' || form.profileType === 'AUDIT'

  const profileTypeLabel = (type) => {
    switch (type) {
      case 'READ_ONLY': return 'Solo consulta'
      case 'AUDIT': return 'Auditoria'
      default: return 'Estandar'
    }
  }

  const inputCls = 'border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 w-full'

  return (
    <div className="min-h-screen flex flex-col bg-gray-100">
      <header className="h-14 bg-indigo-700 flex items-center gap-3 px-6 shrink-0">
        <button onClick={() => navigate('/dashboard')} className="text-xs bg-white/15 hover:bg-white/25 text-white border border-white/40 px-3 py-1.5 rounded-md">← Volver</button>
        <span className="text-white font-semibold">Gestión de Perfiles</span>
      </header>
      <Layout>
        <div className="p-6 max-w-6xl mx-auto w-full">
          <div className="flex justify-between items-center mb-4">
            <div>
              <h2 className="text-lg font-semibold text-gray-800">Perfiles y privilegios</h2>
              <p className="text-sm text-gray-500">Administra el ciclo de vida del perfil y la segregación de funciones por operación.</p>
            </div>
            <button onClick={openCreate} disabled={catalogRows.length === 0} className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white px-4 py-2 rounded-md text-sm font-medium">+ Nuevo perfil</button>
          </div>
          <div className="bg-white rounded-lg shadow-sm overflow-hidden">
            <table className="w-full border-collapse">
              <thead>
                <tr className="bg-gray-50 text-left text-xs text-gray-500 uppercase tracking-wide">
                  {['#', 'Perfil', 'Descripción', 'Funciones', 'Privilegios', 'Acciones'].map(h => <th key={h} className="px-4 py-3 border-b border-gray-200">{h}</th>)}
                </tr>
              </thead>
              <tbody>
                {roles.length === 0
                  ? <tr><td colSpan={6} className="text-center text-gray-400 py-8">Sin perfiles</td></tr>
                  : roles.map(role => {
                    const summary = permissionSummary(role)
                    return (
                      <tr key={role.id} className="border-b border-gray-50 hover:bg-gray-50 text-sm text-gray-700 align-top">
                        <td className="px-4 py-3">{role.id}</td>
                        <td className="px-4 py-3">
                          <div className="flex flex-wrap gap-1">
                            <span className="bg-violet-100 text-violet-700 text-xs px-2 py-0.5 rounded-full font-semibold">{role.name}</span>
                            <span className="bg-slate-100 text-slate-700 text-xs px-2 py-0.5 rounded-full font-semibold">{profileTypeLabel(role.profileType)}</span>
                            {role.privileged && <span className="bg-amber-100 text-amber-700 text-xs px-2 py-0.5 rounded-full font-semibold">Privilegiado</span>}
                          </div>
                        </td>
                        <td className="px-4 py-3 text-gray-500">{role.description || '—'}</td>
                        <td className="px-4 py-3">
                          <span className="text-xs bg-slate-100 text-slate-700 px-2 py-0.5 rounded-full">{summary.modules} funciones con acceso</span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex flex-wrap gap-1">
                            <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full">Crear {summary.creates}</span>
                            <span className="text-xs bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full">Editar {summary.updates}</span>
                            <span className="text-xs bg-rose-100 text-rose-700 px-2 py-0.5 rounded-full">Eliminar {summary.deletes}</span>
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex gap-1">
                            <button onClick={() => openUsersQuery(role)} className="text-xs bg-sky-50 hover:bg-sky-100 text-sky-700 px-2 py-1 rounded">Usuarios</button>
                            <button onClick={() => openPermissionsQuery(role)} className="text-xs bg-emerald-50 hover:bg-emerald-100 text-emerald-700 px-2 py-1 rounded">Permisos</button>
                            <button onClick={() => openEdit(role)} className="text-xs bg-indigo-50 hover:bg-indigo-100 text-indigo-600 px-2 py-1 rounded">Editar</button>
                            <button onClick={() => handleDelete(role.id)} className="text-xs bg-red-50 hover:bg-red-100 text-red-600 px-2 py-1 rounded">Eliminar</button>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
              </tbody>
            </table>
          </div>
        </div>
      </Layout>

      {modal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-6" onClick={closeModal}>
          <div className="bg-white rounded-xl p-7 w-full max-w-6xl shadow-xl max-h-[90vh] overflow-hidden" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-gray-800 mb-1">{modal === 'create' ? 'Nuevo perfil' : 'Editar perfil'}</h3>
            <p className="text-sm text-gray-500 mb-5">Define qué funciones puede ver el perfil y qué operaciones puede ejecutar.</p>

            <form onSubmit={handleSubmit} className="space-y-5">
              <div className="grid grid-cols-2 gap-4">
                <div className="flex flex-col gap-1">
                  <label className="text-xs text-gray-600">Nombre del perfil</label>
                  <input value={form.name} onChange={e => setForm(current => ({ ...current, name: e.target.value }))} required className={inputCls} />
                </div>
                <div className="flex flex-col gap-1">
                  <label className="text-xs text-gray-600">Descripción</label>
                  <input value={form.description} onChange={e => setForm(current => ({ ...current, description: e.target.value }))} className={inputCls} />
                </div>
                <div className="col-span-2 flex flex-col gap-1">
                  <label className="text-xs text-gray-600">Tipo de perfil</label>
                  <select
                    value={form.profileType}
                    onChange={e => setForm(current => ({
                      ...current,
                      profileType: e.target.value,
                      privileged: e.target.value === 'AUDIT' ? true : current.privileged,
                      permissions: current.permissions.map(permission => {
                        const auditHidden = e.target.value === 'AUDIT' && permission.menuItemPath && !AUDIT_PATHS.includes(permission.menuItemPath)
                        const readOnly = e.target.value === 'READ_ONLY' || e.target.value === 'AUDIT'
                        return {
                          ...permission,
                          canView: auditHidden ? false : permission.canView,
                          canCreate: readOnly ? false : permission.canCreate,
                          canUpdate: readOnly ? false : permission.canUpdate,
                          canDelete: readOnly ? false : permission.canDelete,
                        }
                      }),
                    }))}
                    className={inputCls}
                  >
                    {PROFILE_TYPE_OPTIONS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
                  </select>
                </div>
                <label className="col-span-2 flex items-center gap-2 text-sm cursor-pointer">
                  <input
                    type="checkbox"
                    checked={form.profileType === 'AUDIT' ? true : form.privileged}
                    disabled={form.profileType === 'AUDIT'}
                    onChange={e => setForm(current => ({
                      ...current,
                      privileged: e.target.checked,
                      permissions: current.permissions.map(permission => (
                        !e.target.checked && PRIVILEGED_PATHS.includes(permission.menuItemPath)
                          ? { ...permission, canView: false, canCreate: false, canUpdate: false, canDelete: false }
                          : permission
                      )),
                    }))}
                  />
                  Perfil privilegiado para administracion de bitacoras y logs de seguridad
                </label>
              </div>

              <div className="border border-gray-200 rounded-lg overflow-hidden">
                <div className="px-4 py-3 bg-gray-50 border-b border-gray-200 text-sm font-semibold text-gray-700">
                  Matriz de privilegios
                </div>
                <div className="max-h-[48vh] overflow-auto">
                  <table className="w-full border-collapse">
                    <thead className="sticky top-0 bg-white">
                      <tr className="text-left text-xs text-gray-500 uppercase tracking-wide">
                        {['Función', 'Ruta', 'Ver', 'Crear', 'Editar', 'Eliminar'].map(header => (
                          <th key={header} className="px-4 py-3 border-b border-gray-200">{header}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {visiblePermissions.map(permission => (
                        <tr key={permission.menuItemId} className={`border-b border-gray-100 text-sm ${!permission.active ? 'bg-gray-50 text-gray-400' : 'text-gray-700'}`}>
                          <td className="px-4 py-3" style={{ paddingLeft: `${1 + permission.depth * 1.2}rem` }}>
                            {permission.menuItemLabel}
                          </td>
                          <td className="px-4 py-3 text-xs">{permission.menuItemPath || 'Sin ruta directa'}</td>
                          {['canView', 'canCreate', 'canUpdate', 'canDelete'].map(field => (
                            <td key={field} className="px-4 py-3">
                              <input
                                type="checkbox"
                                checked={permission[field]}
                                disabled={isReadOnlyProfile && field !== 'canView'}
                                onChange={e => updatePermission(permission.menuItemId, field, e.target.checked)}
                              />
                            </td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {error && <p className="text-red-600 text-sm">{error}</p>}
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={closeModal} className="bg-gray-100 hover:bg-gray-200 text-gray-700 border border-gray-300 px-4 py-2 rounded-md text-sm">Cancelar</button>
                <button type="submit" disabled={saving} className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white px-4 py-2 rounded-md text-sm">{saving ? 'Guardando...' : 'Guardar perfil'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {queryModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-6" onClick={() => setQueryModal(null)}>
          <div className="bg-white rounded-xl p-7 w-full max-w-4xl shadow-xl max-h-[85vh] overflow-auto" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-gray-800 mb-1">
              {queryModal === 'users' ? 'Usuarios por perfil' : 'Permisos por perfil'}
            </h3>
            <p className="text-sm text-gray-500 mb-5">Perfil: <span className="font-semibold">{queryRole?.name}</span></p>
            {queryLoading ? (
              <p className="text-sm text-gray-500">Cargando consulta...</p>
            ) : queryModal === 'users' ? (
              <table className="w-full border-collapse">
                <thead>
                  <tr className="bg-gray-50 text-left text-xs text-gray-500 uppercase tracking-wide">
                    {['Usuario', 'Nombre', 'Email', 'Estado'].map(header => <th key={header} className="px-4 py-3 border-b border-gray-200">{header}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {queryRows.length === 0
                    ? <tr><td colSpan={4} className="text-center text-gray-400 py-8">Sin usuarios asociados</td></tr>
                    : queryRows.map(row => (
                      <tr key={row.id} className="border-b border-gray-100 text-sm text-gray-700">
                        <td className="px-4 py-3 font-semibold text-indigo-600">@{row.username}</td>
                        <td className="px-4 py-3">{row.fullName}</td>
                        <td className="px-4 py-3">{row.email}</td>
                        <td className="px-4 py-3">{row.status}</td>
                      </tr>
                    ))}
                </tbody>
              </table>
            ) : (
              <table className="w-full border-collapse">
                <thead>
                  <tr className="bg-gray-50 text-left text-xs text-gray-500 uppercase tracking-wide">
                    {['Función', 'Ruta', 'Ver', 'Crear', 'Editar', 'Eliminar'].map(header => <th key={header} className="px-4 py-3 border-b border-gray-200">{header}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {queryRows.length === 0
                    ? <tr><td colSpan={6} className="text-center text-gray-400 py-8">Sin permisos asignados</td></tr>
                    : queryRows.map(row => (
                      <tr key={row.menuItemId} className="border-b border-gray-100 text-sm text-gray-700">
                        <td className="px-4 py-3">{row.menuItemLabel}</td>
                        <td className="px-4 py-3 text-xs text-gray-500">{row.menuItemPath || 'Sin ruta directa'}</td>
                        <td className="px-4 py-3">{row.canView ? 'Sí' : 'No'}</td>
                        <td className="px-4 py-3">{row.canCreate ? 'Sí' : 'No'}</td>
                        <td className="px-4 py-3">{row.canUpdate ? 'Sí' : 'No'}</td>
                        <td className="px-4 py-3">{row.canDelete ? 'Sí' : 'No'}</td>
                      </tr>
                    ))}
                </tbody>
              </table>
            )}
            <div className="flex justify-end pt-5">
              <button type="button" onClick={() => setQueryModal(null)} className="bg-gray-100 hover:bg-gray-200 text-gray-700 border border-gray-300 px-4 py-2 rounded-md text-sm">Cerrar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
