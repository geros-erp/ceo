import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../../context/AuthContext'
import { useNavigate } from 'react-router-dom'
import { getUsers, createUser, updateUser, deleteUser,
         lockUser, unlockUser, adminChangePassword } from '../../api/users'
import { getRoles } from '../../api/roles'
import Layout from '../../components/Layout'

const EMPTY_FORM = { username: '', firstName: '', lastName: '', email: '', isActive: true, roleIds: [] }

export default function Dashboard() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const [users, setUsers]       = useState([])
  const [roles, setRoles]       = useState([])
  const [total, setTotal]       = useState(0)
  const [page, setPage]         = useState(0)
  const [search, setSearch]     = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [loading, setLoading]   = useState(false)
  const [modal, setModal]       = useState(null)
  const [selected, setSelected] = useState(null)
  const [form, setForm]         = useState(EMPTY_FORM)
  const [pwdForm, setPwdForm]   = useState({ newPassword: '', mustChangePassword: false })
  const [formErr, setFormErr]   = useState('')
  const [saving, setSaving]     = useState(false)
  const PAGE_SIZE = 8

  const fetchUsers = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await getUsers({
        search: search || undefined,
        roleId: roleFilter || undefined,
        status: statusFilter || undefined,
        page,
        size: PAGE_SIZE,
      })
      setUsers(data.content); setTotal(data.totalElements)
    } catch { /* silencioso */ }
    finally { setLoading(false) }
  }, [search, roleFilter, statusFilter, page])

  useEffect(() => { fetchUsers() }, [fetchUsers])
  useEffect(() => { getRoles().then(({ data }) => setRoles(data)).catch(() => {}) }, [])

  const openCreate   = () => { setForm(EMPTY_FORM); setFormErr(''); setModal('create') }
  const openEdit     = (u) => { setSelected(u); setForm({ username: u.username, firstName: u.firstName, lastName: u.lastName, email: u.email, isActive: u.isActive, roleIds: [] }); setFormErr(''); setModal('edit') }
  const openPassword = (u) => { setSelected(u); setPwdForm({ newPassword: '', mustChangePassword: false }); setFormErr(''); setModal('password') }
  const closeModal   = () => { setModal(null); setSelected(null) }

  const handleChange    = (e) => { const { name, value, type, checked } = e.target; setForm(f => ({ ...f, [name]: type === 'checkbox' ? checked : value })) }
  const handlePwdChange = (e) => { const { name, value, type, checked } = e.target; setPwdForm(f => ({ ...f, [name]: type === 'checkbox' ? checked : value })) }

  const handleSubmit = async (e) => {
    e.preventDefault(); setSaving(true); setFormErr('')
    try {
      if (modal === 'create') await createUser(form)
      else await updateUser(selected.id, { firstName: form.firstName, lastName: form.lastName, isActive: form.isActive, roleIds: form.roleIds })
      closeModal(); fetchUsers()
    } catch (err) { setFormErr(err.response?.data?.message || 'Error al guardar') }
    finally { setSaving(false) }
  }

  const handlePasswordSubmit = async (e) => {
    e.preventDefault(); setSaving(true); setFormErr('')
    try { await adminChangePassword(selected.id, pwdForm); closeModal(); fetchUsers() }
    catch (err) { setFormErr(err.response?.data?.message || 'Error') }
    finally { setSaving(false) }
  }

  const handleLock   = async (u) => { if (!window.confirm(`¿Bloquear a ${u.firstName}?`)) return; try { await lockUser(u.id); fetchUsers() } catch (err) { alert(err.response?.data?.message) } }
  const handleUnlock = async (u) => { try { await unlockUser(u.id); fetchUsers() } catch (err) { alert(err.response?.data?.message) } }
  const handleDelete = async (id) => { 
    const userToDelete = users.find(u => u.id === id);
    const confirmMessage = userToDelete?.defaultUser 
      ? '⚠️ ADVERTENCIA: Estás a punto de eliminar un usuario privilegiado instalado por defecto.\n\n¿Estás seguro de que deseas continuar?'
      : '¿Eliminar este usuario?';
    
    if (!window.confirm(confirmMessage)) return; 
    
    try { 
      await deleteUser(id); 
      fetchUsers() 
    } catch (err) { 
      alert(err.response?.data?.message || 'Error al eliminar usuario') 
    } 
  }

  const totalPages = Math.ceil(total / PAGE_SIZE)

  const inputCls = "border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 w-full"
  const btnPrimary = "bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors disabled:opacity-60"
  const btnCancel  = "bg-gray-100 hover:bg-gray-200 text-gray-700 border border-gray-300 px-4 py-2 rounded-md text-sm transition-colors"

  return (
    <div className="min-h-screen flex flex-col bg-gray-100">
      {/* Header */}
      <header className="h-14 bg-indigo-700 flex items-center justify-between px-6 shrink-0">
        <span className="text-white font-semibold">Geros</span>
        <div className="flex items-center gap-3">
          <div className="text-indigo-200 text-xs text-left">
            <div>{user?.email}</div>
            <div>Ingreso actual: {user?.currentLoginAt || '-'} desde {user?.currentLoginIp || '-'}</div>
            <div>Ingreso anterior: {user?.previousLoginAt || '-'} desde {user?.previousLoginIp || '-'}</div>
          </div>
          <button onClick={() => navigate('/change-password')}
            className="text-xs bg-white/15 hover:bg-white/25 text-white border border-white/40 px-3 py-1.5 rounded-md transition-colors">
            🔑 Mi contraseña
          </button>
          <button onClick={logout}
            className="text-xs bg-white/15 hover:bg-white/25 text-white border border-white/40 px-3 py-1.5 rounded-md transition-colors">
            Cerrar sesión
          </button>
        </div>
      </header>

      <Layout>
        <div className="p-6">
          {/* Panel de advertencia para usuarios por defecto */}
          {users.some(u => u.defaultUser && u.isActive) && (
            <div className="mb-4 bg-red-50 border-l-4 border-red-500 p-4 rounded-md">
              <div className="flex items-start gap-3">
                <span className="text-2xl">🚨</span>
                <div className="flex-1">
                  <h4 className="text-sm font-semibold text-red-800 mb-1">
                    ADVERTENCIA DE SEGURIDAD: Usuario privilegiado por defecto ACTIVO
                  </h4>
                  <p className="text-xs text-red-700 mb-2">
                    Se ha detectado que el usuario administrador por defecto está activo. Este usuario debe permanecer DESHABILITADO 
                    y guardado en custodia física para cumplir con los requisitos de seguridad.
                  </p>
                  <div className="text-xs text-red-600 space-y-1">
                    <p>• <strong>Acción requerida:</strong> Deshabilitar el usuario y guardar credenciales en sobre sellado</p>
                    <p>• <strong>Ubicación:</strong> Caja fuerte o custodia bancaria</p>
                    <p>• <strong>Uso:</strong> Solo en casos de emergencia</p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Panel informativo para usuarios en custodia */}
          {users.some(u => u.defaultUser && !u.isActive) && (
            <div className="mb-4 bg-purple-50 border-l-4 border-purple-500 p-4 rounded-md">
              <div className="flex items-start gap-3">
                <span className="text-2xl">🔐</span>
                <div className="flex-1">
                  <h4 className="text-sm font-semibold text-purple-800 mb-1">
                    Usuario administrador en custodia
                  </h4>
                  <p className="text-xs text-purple-700">
                    El usuario privilegiado por defecto está correctamente deshabilitado y en custodia física. 
                    Solo debe reactivarse en casos de emergencia (recuperación de acceso).
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Toolbar */}
          <div className="flex gap-3 mb-4">
            <input className="flex-1 max-w-sm border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500"
              placeholder="Buscar por nombre o email..."
              value={search} onChange={e => { setSearch(e.target.value); setPage(0) }} />
            <select
              value={roleFilter}
              onChange={e => { setRoleFilter(e.target.value); setPage(0) }}
              className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 bg-white"
            >
              <option value="">Todos los perfiles</option>
              {roles.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
            </select>
            <select
              value={statusFilter}
              onChange={e => { setStatusFilter(e.target.value); setPage(0) }}
              className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 bg-white"
            >
              <option value="">Todos los estados</option>
              <option value="ACTIVE">Activos</option>
              <option value="INACTIVE">Inactivos</option>
              <option value="LOCKED">Bloqueados</option>
            </select>
            <button onClick={openCreate} className={btnPrimary}>+ Nuevo usuario</button>
          </div>

          {loading ? (
            <p className="text-center text-gray-500 py-8">Cargando...</p>
          ) : (
            <>
              <div className="bg-white rounded-lg shadow-sm overflow-hidden">
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="bg-gray-50 text-left text-xs text-gray-500 uppercase tracking-wide">
                      {['#','Usuario','Nombre','Email','Roles','Estado','Intentos','Acciones'].map(h => (
                        <th key={h} className="px-4 py-3 border-b border-gray-200">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {users.length === 0
                      ? <tr><td colSpan={8} className="text-center text-gray-400 py-8">Sin resultados</td></tr>
                      : users.map(u => (
                        <tr key={u.id} className="border-b border-gray-50 hover:bg-gray-50 text-sm text-gray-700">
                          <td className="px-4 py-3">{u.id}</td>
                          <td className="px-4 py-3">
                            <div className="flex flex-col gap-1">
                              <div className="flex items-center gap-2">
                                <span className="font-semibold text-indigo-600">@{u.username}</span>
                                {u.defaultUser && (
                                  <span className="text-xs bg-orange-100 text-orange-700 px-2 py-0.5 rounded-full font-medium" title="Usuario privilegiado instalado por defecto">
                                    ⚠️ Por defecto
                                  </span>
                                )}
                              </div>
                              {u.defaultUser && !u.isActive && (
                                <span className="text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded-full font-medium inline-flex items-center gap-1 w-fit" title="Usuario en custodia - Solo para emergencias">
                                  🔐 EN CUSTODIA
                                </span>
                              )}
                              {u.defaultUser && u.isActive && (
                                <span className="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded-full font-medium inline-flex items-center gap-1 w-fit animate-pulse" title="ADVERTENCIA: Usuario por defecto activo - Debe estar en custodia">
                                  🚨 ACTIVO (Revisar)
                                </span>
                              )}
                            </div>
                          </td>
                          <td className="px-4 py-3">{u.firstName} {u.lastName}</td>
                          <td className="px-4 py-3">{u.email}</td>
                          <td className="px-4 py-3">
                            {u.roles?.map(r => (
                              <span key={r} className={`inline-block text-xs px-2 py-0.5 rounded-full mr-1 ${r === 'ADMIN' ? 'bg-violet-100 text-violet-700' : 'bg-sky-100 text-sky-700'}`}>{r}</span>
                            ))}
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex flex-col gap-1">
                              {u.locked
                                ? <span className="text-xs bg-red-100 text-red-600 px-2 py-0.5 rounded-full w-fit">🔒 Bloqueado</span>
                                : u.isActive
                                  ? <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full w-fit">✓ Activo</span>
                                  : <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full w-fit">○ Inactivo</span>}
                              {u.mustChangePassword && <span className="text-xs bg-yellow-100 text-yellow-700 px-2 py-0.5 rounded-full w-fit" title="Debe cambiar contraseña">⚠ Cambio requerido</span>}
                              {u.defaultUser && !u.isActive && (
                                <span className="text-xs text-purple-600 font-medium" title="Usuario guardado en custodia física">
                                  📦 Custodia física
                                </span>
                              )}
                            </div>
                          </td>
                          <td className="px-4 py-3">{u.failedAttempts}</td>
                          <td className="px-4 py-3">
                            <div className="flex gap-1">
                              <button onClick={() => openEdit(u)} className="text-xs bg-indigo-50 hover:bg-indigo-100 text-indigo-600 px-2 py-1 rounded">Editar</button>
                              <button 
                                onClick={() => openPassword(u)} 
                                className="text-xs bg-amber-50 hover:bg-amber-100 text-amber-700 px-2 py-1 rounded transition-colors"
                                title="Rotar contraseña para custodia"
                              >
                                🔑 Clave
                              </button>
                              {u.locked
                                ? <button onClick={() => handleUnlock(u)} className="text-xs bg-green-50 hover:bg-green-100 text-green-600 px-2 py-1 rounded">🔓</button>
                                : <button onClick={() => handleLock(u)} className="text-xs bg-red-50 hover:bg-red-100 text-red-600 px-2 py-1 rounded">🔒</button>}
                              <button onClick={() => handleDelete(u.id)} className="text-xs bg-red-50 hover:bg-red-100 text-red-600 px-2 py-1 rounded">Eliminar</button>
                            </div>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>

              {/* Paginación */}
              <div className="flex items-center justify-center gap-4 mt-4 text-sm text-gray-500">
                <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
                  className="border border-gray-300 bg-white px-3 py-1.5 rounded-md disabled:opacity-40 hover:bg-gray-50">‹ Anterior</button>
                <span>Página {page + 1} de {totalPages || 1} — {total} registros</span>
                <button disabled={page + 1 >= totalPages} onClick={() => setPage(p => p + 1)}
                  className="border border-gray-300 bg-white px-3 py-1.5 rounded-md disabled:opacity-40 hover:bg-gray-50">Siguiente ›</button>
              </div>
            </>
          )}
        </div>
      </Layout>

      {/* Modales */}
      {(modal === 'create' || modal === 'edit') && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={closeModal}>
          <div className="bg-white rounded-xl p-7 w-full max-w-md shadow-xl" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-gray-800 mb-5">{modal === 'create' ? 'Nuevo usuario' : 'Editar usuario'}</h3>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="col-span-2 flex flex-col gap-1"><label className="text-xs text-gray-600">Usuario (único, números y letras)</label><input name="username" value={form.username} onChange={handleChange} required disabled={modal === 'edit'} placeholder="ej: juan123" className={inputCls + ' disabled:bg-gray-50 disabled:text-gray-400'} /></div>
                <div className="flex flex-col gap-1"><label className="text-xs text-gray-600">Nombre</label><input name="firstName" value={form.firstName} onChange={handleChange} required className={inputCls} /></div>
                <div className="flex flex-col gap-1"><label className="text-xs text-gray-600">Apellido</label><input name="lastName" value={form.lastName} onChange={handleChange} required className={inputCls} /></div>
                <div className="col-span-2 flex flex-col gap-1"><label className="text-xs text-gray-600">Email</label><input name="email" type="email" value={form.email} onChange={handleChange} required disabled={modal === 'edit'} className={inputCls + ' disabled:bg-gray-50 disabled:text-gray-400'} /></div>
                <div className="col-span-2">
                  <label className="text-xs text-gray-600 block mb-1">Roles</label>
                  <div className="flex flex-wrap gap-2">
                    {roles.map(r => (
                      <label key={r.id} className="flex items-center gap-1.5 text-sm cursor-pointer">
                        <input type="checkbox" checked={form.roleIds.includes(r.id)}
                          onChange={e => setForm(f => ({ ...f, roleIds: e.target.checked ? [...f.roleIds, r.id] : f.roleIds.filter(id => id !== r.id) }))} />
                        {r.name}
                      </label>
                    ))}
                  </div>
                </div>
                {modal === 'edit' && (
                  <>
                    {selected?.defaultUser && (
                      <div className="col-span-2 bg-purple-50 border border-purple-200 rounded-md p-3">
                        <div className="flex items-start gap-2">
                          <span className="text-lg">🔐</span>
                          <div className="flex-1">
                            <p className="text-xs font-semibold text-purple-800 mb-1">Usuario en custodia</p>
                            <p className="text-xs text-purple-700">
                              Este usuario debe permanecer DESHABILITADO y guardado en custodia física. 
                              Solo activar en casos de emergencia.
                            </p>
                          </div>
                        </div>
                      </div>
                    )}
                    <label className="col-span-2 flex items-center gap-2 text-sm cursor-pointer">
                      <input name="isActive" type="checkbox" checked={form.isActive} onChange={handleChange} />
                      Usuario activo
                      {selected?.defaultUser && form.isActive && (
                        <span className="text-xs text-red-600 font-medium ml-2">⚠️ No recomendado para usuarios en custodia</span>
                      )}
                    </label>
                  </>
                )}
              </div>
              {formErr && <p className="text-red-600 text-sm">{formErr}</p>}
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={closeModal} className={btnCancel}>Cancelar</button>
                <button type="submit" disabled={saving} className={btnPrimary}>{saving ? 'Guardando...' : 'Guardar'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {modal === 'password' && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={closeModal}>
          <div className="bg-white rounded-xl p-7 w-full max-w-md shadow-xl" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-gray-800 mb-1">Rotación de Credenciales</h3>
            <p className="text-xs text-gray-500 mb-5">Usuario: <span className="font-mono text-indigo-600 font-bold">@{selected?.username}</span></p>
            <form onSubmit={handlePasswordSubmit} className="space-y-4">
              <div className="flex flex-col gap-1"><label className="text-xs text-gray-600">Nueva contraseña</label><input name="newPassword" type="password" value={pwdForm.newPassword} onChange={handlePwdChange} required className={inputCls} /></div>
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input name="mustChangePassword" type="checkbox" checked={pwdForm.mustChangePassword} onChange={handlePwdChange} />
                Forzar cambio al ingresar (Recomendado para custodia)
              </label>
              {formErr && <p className="text-red-600 text-sm">{formErr}</p>}
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={closeModal} className={btnCancel}>Cancelar</button>
                <button type="submit" disabled={saving} className={btnPrimary}>{saving ? 'Guardando...' : 'Cambiar'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
