import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  downloadSecurityLogExport,
  exportSecurityLog,
  getLogActions,
  getSecurityLog,
  getSecurityLogExports,
} from '../api/securitylog'
import Layout from '../components/Layout'

const ACTION_LABELS = {
  USER_CREATED: { label: 'Usuario creado', color: 'bg-blue-100 text-blue-700' },
  USER_DATA_ACCESSED: { label: 'Consulta usuario', color: 'bg-cyan-100 text-cyan-700' },
  USER_LIST_ACCESSED: { label: 'Consulta lista usuarios', color: 'bg-sky-100 text-sky-700' },
  CUSTOMER_DATA_ACCESSED: { label: 'Consulta cliente', color: 'bg-cyan-100 text-cyan-700' },
  CUSTOMER_LIST_ACCESSED: { label: 'Consulta lista clientes', color: 'bg-sky-100 text-sky-700' },
  SECURITY_LOG_EXPORTED: { label: 'Exportacion logs', color: 'bg-fuchsia-100 text-fuchsia-700' },
  PRIVILEGED_USER_ACTIVITY: { label: 'Actividad privilegiada', color: 'bg-orange-100 text-orange-700' },
  LOGIN_OUTSIDE_ALLOWED_HOURS: { label: 'Acceso fuera de horario', color: 'bg-amber-100 text-amber-700' },
  IMPORTANT_FILE_MODIFIED: { label: 'Archivo importante modificado', color: 'bg-red-100 text-red-700' },
  PASSWORD_POLICY_UPDATED: { label: 'Política de contraseñas actualizada', color: 'bg-yellow-100 text-yellow-700' },
  AUTHORIZATION_PARAMETERS_UPDATED: { label: 'Autorización actualizada', color: 'bg-indigo-100 text-indigo-700' },
  USER_UPDATED: { label: 'Usuario actualizado', color: 'bg-blue-100 text-blue-700' },
  USER_DELETED: { label: 'Usuario eliminado', color: 'bg-red-100 text-red-700' },
  ROLE_CREATED: { label: 'Perfil creado', color: 'bg-violet-100 text-violet-700' },
  ROLE_UPDATED: { label: 'Perfil actualizado', color: 'bg-amber-100 text-amber-700' },
  ROLE_DELETED: { label: 'Perfil eliminado', color: 'bg-rose-100 text-rose-700' },
  PASSWORD_CHANGED_BY_USER: { label: 'Cambio por usuario', color: 'bg-green-100 text-green-700' },
  PASSWORD_CHANGED_BY_ADMIN: { label: 'Cambio por admin', color: 'bg-indigo-100 text-indigo-700' },
  PASSWORD_FORCED_CHANGE: { label: 'Cambio forzado', color: 'bg-yellow-100 text-yellow-700' },
  LOGIN_FAILED: { label: 'Login fallido', color: 'bg-red-100 text-red-600' },
  ACCOUNT_LOCKED: { label: 'Cuenta bloqueada', color: 'bg-red-100 text-red-700' },
  ACCOUNT_UNLOCKED: { label: 'Cuenta desbloqueada', color: 'bg-green-100 text-green-700' },
  LOGIN_SUCCESS: { label: 'Login exitoso', color: 'bg-gray-100 text-gray-600' },
}

const EVENT_TYPE_LABELS = {
  SUCCESS: { label: 'Acierto', color: 'bg-emerald-100 text-emerald-700' },
  ERROR: { label: 'Error', color: 'bg-rose-100 text-rose-700' },
}

function renderValue(value) {
  return value ? value : '-'
}

function formatBogotaDateTime(value) {
  return new Date(value).toLocaleString('es-CO', {
    timeZone: 'America/Bogota',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

export default function SecurityLog() {
  const { user } = useAuth()
  const [logs, setLogs] = useState([])
  const [exports, setExports] = useState([])
  const [actions, setActions] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [email, setEmail] = useState('')
  const [userFrom, setUserFrom] = useState('')
  const [userTo, setUserTo] = useState('')
  const [status, setStatus] = useState('')
  const [action, setAction] = useState('')
  const [specificDate, setSpecificDate] = useState('')
  const [createdFrom, setCreatedFrom] = useState('')
  const [createdTo, setCreatedTo] = useState('')
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const navigate = useNavigate()
  const PAGE_SIZE = 15
  const isAdmin = user?.role?.split(',').includes('ADMIN')

  const fetchExports = useCallback(async () => {
    if (!isAdmin) {
      setExports([])
      return
    }
    try {
      const { data } = await getSecurityLogExports()
      setExports(data)
    } catch {
      setExports([])
    }
  }, [isAdmin])

  useEffect(() => {
    getLogActions().then(({ data }) => setActions(data)).catch(() => {})
    fetchExports()
  }, [fetchExports])

  const fetchLogs = useCallback(async () => {
    setLoading(true)
    try {
      const { data } = await getSecurityLog({
        email: email || undefined,
        userFrom: userFrom || undefined,
        userTo: userTo || undefined,
        status: status || undefined,
        action: action || undefined,
        specificDate: specificDate || undefined,
        createdFrom: createdFrom || undefined,
        createdTo: createdTo || undefined,
        page,
        size: PAGE_SIZE,
      })
      setLogs(data.content)
      setTotal(data.totalElements)
    } catch {
      // silencioso
    } finally {
      setLoading(false)
    }
  }, [email, userFrom, userTo, status, action, specificDate, createdFrom, createdTo, page])

  useEffect(() => {
    fetchLogs()
  }, [fetchLogs])

  const handleExport = async () => {
    setExporting(true)
    try {
      await exportSecurityLog({
        email: email || undefined,
        userFrom: userFrom || undefined,
        userTo: userTo || undefined,
        status: status || undefined,
        action: action || undefined,
        specificDate: specificDate || undefined,
        createdFrom: createdFrom || undefined,
        createdTo: createdTo || undefined,
      })
      await fetchLogs()
      await fetchExports()
    } catch (error) {
      window.alert(error.response?.data?.message || 'No fue posible exportar los logs')
    } finally {
      setExporting(false)
    }
  }

  const handleDownload = async (item) => {
    try {
      const { data } = await downloadSecurityLogExport(item.id)
      const url = window.URL.createObjectURL(new Blob([data], { type: 'text/csv;charset=utf-8' }))
      const link = document.createElement('a')
      link.href = url
      link.download = item.fileName
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch (error) {
      window.alert(error.response?.data?.message || 'No fue posible descargar la exportacion')
    }
  }

  const totalPages = Math.ceil(total / PAGE_SIZE)

  return (
    <div className="min-h-screen flex flex-col bg-gray-100">
      <header className="h-14 bg-indigo-700 flex items-center gap-3 px-6 shrink-0">
        <button
          onClick={() => navigate('/dashboard')}
          className="text-xs bg-white/15 hover:bg-white/25 text-white border border-white/40 px-3 py-1.5 rounded-md"
        >
          {'<-'} Volver
        </button>
        <span className="text-white font-semibold">Log de Seguridad</span>
      </header>

      <Layout>
        <div className="p-6 max-w-7xl mx-auto w-full space-y-4">
          <div className="bg-white rounded-lg shadow-sm p-4 flex flex-wrap gap-3 items-end">
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-600">Usuario contiene</label>
              <input
                value={email}
                onChange={e => {
                  setEmail(e.target.value)
                  setPage(0)
                }}
                placeholder="Buscar por usuario/perfil..."
                className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 w-64"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-600">Usuario desde</label>
              <input
                value={userFrom}
                onChange={e => {
                  setUserFrom(e.target.value)
                  setPage(0)
                }}
                placeholder="admin@..."
                className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 w-52"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-600">Usuario hasta</label>
              <input
                value={userTo}
                onChange={e => {
                  setUserTo(e.target.value)
                  setPage(0)
                }}
                placeholder="z@..."
                className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 w-52"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-600">Estado usuario</label>
              <select
                value={status}
                onChange={e => {
                  setStatus(e.target.value)
                  setPage(0)
                }}
                className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 w-40"
              >
                <option value="">Todos</option>
                <option value="ACTIVE">Activo</option>
                <option value="INACTIVE">Inactivo</option>
                <option value="LOCKED">Bloqueado</option>
              </select>
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-600">Accion</label>
              <select
                value={action}
                onChange={e => {
                  setAction(e.target.value)
                  setPage(0)
                }}
                className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 w-56"
              >
                <option value="">Todas las acciones</option>
                {actions.map(item => (
                  <option key={item} value={item}>
                    {ACTION_LABELS[item]?.label || item}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-600">Fecha especifica</label>
              <input
                type="date"
                value={specificDate}
                onChange={e => {
                  setSpecificDate(e.target.value)
                  setPage(0)
                }}
                className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-600">Fecha/hora desde</label>
              <input
                type="datetime-local"
                value={createdFrom}
                onChange={e => {
                  setCreatedFrom(e.target.value)
                  setPage(0)
                }}
                className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-600">Fecha/hora hasta</label>
              <input
                type="datetime-local"
                value={createdTo}
                onChange={e => {
                  setCreatedTo(e.target.value)
                  setPage(0)
                }}
                className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500"
              />
            </div>
            <button
              onClick={() => {
                setEmail('')
                setUserFrom('')
                setUserTo('')
                setStatus('')
                setAction('')
                setSpecificDate('')
                setCreatedFrom('')
                setCreatedTo('')
                setPage(0)
              }}
              className="bg-gray-100 hover:bg-gray-200 text-gray-600 border border-gray-300 px-4 py-2 rounded-md text-sm"
            >
              Limpiar
            </button>
            {isAdmin && (
              <button
                onClick={handleExport}
                disabled={exporting}
                className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-md text-sm font-medium disabled:opacity-60"
              >
                {exporting ? 'Exportando...' : 'Exportar al repositorio'}
              </button>
            )}
          </div>

          <div className="bg-white rounded-lg shadow-sm overflow-x-auto">
            <table className="w-full border-collapse min-w-[1280px]">
              <thead>
                <tr className="bg-gray-50 text-left text-xs text-gray-500 uppercase tracking-wide">
                  {[
                    'Fecha y hora',
                    'Transaccion',
                    'Evento',
                    'Tipo',
                    'Origen / Codigo',
                    'Usuario / Actor',
                    'Equipo / IP',
                    'Valor anterior',
                    'Valor nuevo',
                    'Descripcion',
                  ].map(header => (
                    <th key={header} className="px-4 py-3 border-b border-gray-200">
                      {header}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={10} className="text-center text-gray-400 py-8">
                      Cargando...
                    </td>
                  </tr>
                ) : logs.length === 0 ? (
                  <tr>
                    <td colSpan={10} className="text-center text-gray-400 py-8">
                      Sin registros
                    </td>
                  </tr>
                ) : (
                  logs.map(log => {
                    const actionMeta = ACTION_LABELS[log.action] || { label: log.action, color: 'bg-gray-100 text-gray-600' }
                    const typeMeta = EVENT_TYPE_LABELS[log.eventType] || { label: log.eventType, color: 'bg-gray-100 text-gray-600' }

                    return (
                      <tr key={log.id} className="border-b border-gray-50 hover:bg-gray-50 text-sm text-gray-700 align-top">
                        <td className="px-4 py-3 text-xs text-gray-500 whitespace-nowrap">
                          {formatBogotaDateTime(log.createdAt)}
                        </td>
                        <td className="px-4 py-3 text-xs text-gray-500 whitespace-nowrap">
                          {renderValue(log.transactionId)}
                        </td>
                        <td className="px-4 py-3">
                          <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${actionMeta.color}`}>
                            {actionMeta.label}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${typeMeta.color}`}>
                            {typeMeta.label}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-xs text-gray-600">
                          <div>{renderValue(log.origin)}</div>
                          <div className="text-gray-400">{renderValue(log.eventCode)}</div>
                        </td>
                        <td className="px-4 py-3 text-xs text-gray-600">
                          <div>{renderValue(log.targetEmail)}</div>
                          <div className="text-gray-400">Estado: {renderValue(log.targetStatus)}</div>
                          <div className="text-gray-400">Por: {renderValue(log.performedBy)}</div>
                        </td>
                        <td className="px-4 py-3 text-xs text-gray-600">
                          <div>{renderValue(log.hostName)}</div>
                          <div className="text-gray-400">{renderValue(log.ipAddress)}</div>
                        </td>
                        <td className="px-4 py-3 text-xs text-gray-500 max-w-xs break-words">
                          {renderValue(log.oldValue)}
                        </td>
                        <td className="px-4 py-3 text-xs text-gray-500 max-w-xs break-words">
                          {renderValue(log.newValue)}
                        </td>
                        <td className="px-4 py-3 text-xs text-gray-500 max-w-sm break-words">
                          {renderValue(log.detail)}
                        </td>
                      </tr>
                    )
                  })
                )}
              </tbody>
            </table>
          </div>

          {isAdmin && (
            <div className="bg-white rounded-lg shadow-sm overflow-hidden">
              <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-semibold text-gray-700">Repositorio independiente de exportaciones</h3>
                  <p className="text-xs text-gray-400">Ultimas 20 exportaciones generadas con acceso restringido</p>
                </div>
              </div>
              <table className="w-full border-collapse">
                <thead>
                  <tr className="bg-gray-50 text-left text-xs text-gray-500 uppercase tracking-wide">
                    {['Fecha', 'Archivo', 'Generado por', 'Filtros', 'Registros', 'Acciones'].map(header => (
                      <th key={header} className="px-4 py-3 border-b border-gray-200">
                        {header}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {exports.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="text-center text-gray-400 py-6">
                        Sin exportaciones
                      </td>
                    </tr>
                  ) : (
                    exports.map(item => (
                      <tr key={item.id} className="border-b border-gray-50 hover:bg-gray-50 text-sm text-gray-700">
                        <td className="px-4 py-3 text-xs text-gray-500 whitespace-nowrap">{formatBogotaDateTime(item.createdAt)}</td>
                        <td className="px-4 py-3 text-xs">{item.fileName}</td>
                        <td className="px-4 py-3 text-xs">{item.createdBy}</td>
                        <td className="px-4 py-3 text-xs text-gray-500">
                          usuario={renderValue(item.filterEmail)} | accion={renderValue(item.filterAction)}
                        </td>
                        <td className="px-4 py-3 text-xs">{item.recordCount}</td>
                        <td className="px-4 py-3">
                          <button
                            onClick={() => handleDownload(item)}
                            className="text-xs bg-indigo-50 hover:bg-indigo-100 text-indigo-600 px-2 py-1 rounded"
                          >
                            Descargar
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}

          <div className="flex items-center justify-center gap-4 text-sm text-gray-500">
            <button
              disabled={page === 0}
              onClick={() => setPage(current => current - 1)}
              className="border border-gray-300 bg-white px-3 py-1.5 rounded-md disabled:opacity-40 hover:bg-gray-50"
            >
              ‹ Anterior
            </button>
            <span>
              Pagina {page + 1} de {totalPages || 1} - {total} registros
            </span>
            <button
              disabled={page + 1 >= totalPages}
              onClick={() => setPage(current => current + 1)}
              className="border border-gray-300 bg-white px-3 py-1.5 rounded-md disabled:opacity-40 hover:bg-gray-50"
            >
              Siguiente ›
            </button>
          </div>
        </div>
      </Layout>
    </div>
  )
}
