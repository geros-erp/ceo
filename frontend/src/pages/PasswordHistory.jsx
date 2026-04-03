import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getUsers } from '../api/users'
import { getPasswordHistory } from '../api/passwordHistory'
import Layout from '../components/Layout'

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

export default function PasswordHistory() {
  const [users, setUsers]       = useState([])
  const [history, setHistory]   = useState([])
  const [selected, setSelected] = useState(null)
  const [loading, setLoading]   = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    getUsers({ size: 100 }).then(({ data }) => setUsers(data.content)).catch(() => {})
  }, [])

  const handleSelect = async (userId) => {
    const user = users.find(u => u.id === parseInt(userId))
    setSelected(user)
    setLoading(true)
    try {
      const { data } = await getPasswordHistory(userId)
      setHistory(data)
    } catch { setHistory([]) }
    finally { setLoading(false) }
  }

  return (
    <div className="min-h-screen flex flex-col bg-gray-100">
      <header className="h-14 bg-indigo-700 flex items-center gap-3 px-6 shrink-0">
        <button onClick={() => navigate('/dashboard')}
          className="text-xs bg-white/15 hover:bg-white/25 text-white border border-white/40 px-3 py-1.5 rounded-md">
          ← Volver
        </button>
        <span className="text-white font-semibold">Historial de Contraseñas</span>
      </header>

      <Layout>
        <div className="p-6 max-w-3xl mx-auto w-full space-y-5">

          {/* Selector de usuario */}
          <div className="bg-white rounded-lg shadow-sm p-5">
            <label className="text-sm font-medium text-gray-700 block mb-2">Seleccionar usuario</label>
            <select onChange={e => handleSelect(e.target.value)} defaultValue=""
              className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 w-full max-w-sm">
              <option value="" disabled>Seleccionar...</option>
              {users.map(u => (
                <option key={u.id} value={u.id}>{u.firstName} {u.lastName} — {u.email}</option>
              ))}
            </select>
          </div>

          {/* Tabla de historial */}
          {selected && (
            <div className="bg-white rounded-lg shadow-sm overflow-hidden">
              <div className="px-5 py-3 border-b border-gray-100">
                <p className="text-sm font-semibold text-gray-700">
                  Historial de <span className="text-indigo-600">{selected.firstName} {selected.lastName}</span>
                </p>
                <p className="text-xs text-gray-400">{selected.email}</p>
              </div>
              {loading ? (
                <p className="text-center text-gray-400 py-8">Cargando...</p>
              ) : history.length === 0 ? (
                <p className="text-center text-gray-400 py-8">Sin historial registrado</p>
              ) : (
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="bg-gray-50 text-left text-xs text-gray-500 uppercase tracking-wide">
                      <th className="px-5 py-3 border-b border-gray-200">#</th>
                      <th className="px-5 py-3 border-b border-gray-200">Fecha de cambio</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((h, i) => (
                      <tr key={h.id} className="border-b border-gray-50 hover:bg-gray-50 text-sm text-gray-700">
                        <td className="px-5 py-3">{i + 1}</td>
                        <td className="px-5 py-3">{formatBogotaDateTime(h.changedAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}
        </div>
      </Layout>
    </div>
  )
}
