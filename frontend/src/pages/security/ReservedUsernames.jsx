import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Layout from '../../components/Layout'
import { PageHeader, LoadingState } from '../../components/common'
import { getReservedUsernames, createReservedUsername, deleteReservedUsername } from '../../api/reservedUsernames'

export default function ReservedUsernames() {
  const [reserved, setReserved] = useState([])
  const [newUsername, setNewUsername] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const navigate = useNavigate()

  const fetchReserved = async () => {
    try {
      setLoading(true)
      setError(null)
      const { data } = await getReservedUsernames()
      setReserved(data)
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudo cargar la lista de reservados.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchReserved()
  }, [])

  const handleAdd = async (e) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)

    if (newUsername.trim().length < 3) {
      setError('El username debe tener al menos 3 caracteres.')
      return
    }

    try {
      await createReservedUsername({ username: newUsername.toLowerCase().trim() })
      setSuccess(`"${newUsername}" añadido a reservados.`)
      setNewUsername('')
      fetchReserved()
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudo guardar el username')
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('¿Liberar este username de la lista de reservados?')) return

    try {
      await deleteReservedUsername(id)
      setSuccess('Username eliminado de reservados.')
      fetchReserved()
    } catch (err) {
      setError(err.response?.data?.message || 'Error al eliminar restricción')
    }
  }

  const inputCls = 'border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500 w-full'
  const btnPrimary = 'bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-2 rounded-md text-sm font-medium transition-colors disabled:opacity-60 shadow-sm'

  return (
    <div className="min-h-screen flex flex-col bg-gray-100">
      <PageHeader title="Control de Identidades" />

      <Layout>
        <div className="p-6 max-w-2xl mx-auto w-full space-y-6">
          <div className="mb-4">
            <h1 className="text-xl font-bold text-gray-800">Control de Identidades</h1>
            <p className="text-sm text-gray-500">Gestione nombres prohibidos para evitar cuentas genéricas o compartidas.</p>
          </div>

          <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
            <h2 className="text-sm font-semibold mb-4 text-gray-700 uppercase tracking-wider">Reservar Nuevo Username</h2>
            <form onSubmit={handleAdd} className="flex gap-3">
              <input
                type="text"
                className={inputCls}
                placeholder="Ej: admin, root, api, shared..."
                value={newUsername}
                onChange={(e) => setNewUsername(e.target.value)}
                required
              />
              <button className={btnPrimary}>Añadir</button>
            </form>
            {error && <div className="mt-3 text-red-600 text-xs font-medium bg-red-50 p-2 rounded border border-red-100">{error}</div>}
            {success && <div className="mt-3 text-green-600 text-xs font-medium bg-green-50 p-2 rounded border border-green-100">{success}</div>}
          </div>

          <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
            <div className="px-6 py-3 bg-gray-50 border-b font-semibold text-gray-600 uppercase text-xs tracking-wider">Lista de Restricciones</div>
            <div className="divide-y divide-gray-100">
              {loading ? (
                <LoadingState message="Cargando lista..." />
              ) : reserved.length === 0 ? (
                <div className="p-8 text-center text-gray-400 text-sm">No hay usernames reservados actualmente.</div>
              ) : reserved.map((item) => (
                <div key={item.id} className="px-6 py-3 flex justify-between items-center hover:bg-gray-50 transition">
                  <span className="font-mono text-indigo-600 font-semibold bg-indigo-50 px-2 py-1 rounded text-sm">@{item.username}</span>
                  <button onClick={() => handleDelete(item.id)} className="text-red-500 hover:text-red-700 text-xs font-bold uppercase tracking-tight">Eliminar</button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </Layout>
    </div>
  )
}
