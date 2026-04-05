import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import api from '../../api/auth'
import { useAuth } from '../../context/AuthContext'

export default function ChangePassword() {
  const [form, setForm]       = useState({ currentPassword: '', newPassword: '', confirm: '' })
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)
  const { logout } = useAuth()
  const navigate   = useNavigate()
  const location   = useLocation()
  const forced     = location.state?.forced === true

  const handleChange = (e) =>
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (form.newPassword !== form.confirm) { setError('Las contraseñas no coinciden'); return }
    setError(''); setLoading(true)
    try {
      await api.post('/auth/change-password', {
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
      })
      if (forced) { logout(); navigate('/login') }
      else navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.message || 'Error al cambiar contraseña')
    } finally { setLoading(false) }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white p-8 rounded-xl shadow-md w-full max-w-sm">
        <h2 className="text-2xl font-semibold text-center text-gray-800 mb-4">Cambiar contraseña</h2>
        {forced && (
          <p className="text-red-600 text-sm mb-4 text-center">Debes cambiar tu contraseña para continuar.</p>
        )}
        <form onSubmit={handleSubmit} className="space-y-4">
          {[
            { name: 'currentPassword', label: 'Contraseña actual' },
            { name: 'newPassword',     label: 'Nueva contraseña' },
            { name: 'confirm',         label: 'Confirmar nueva contraseña' },
          ].map(f => (
            <div key={f.name} className="flex flex-col gap-1">
              <label className="text-sm text-gray-600">{f.label}</label>
              <input name={f.name} type="password" value={form[f.name]}
                onChange={handleChange} required
                className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500" />
            </div>
          ))}
          {error && <p className="text-red-600 text-sm">{error}</p>}
          <button type="submit" disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white py-2 rounded-md text-sm font-medium transition-colors">
            {loading ? 'Guardando...' : 'Cambiar contraseña'}
          </button>
          {!forced && (
            <button type="button" onClick={() => navigate('/dashboard')}
              className="w-full bg-gray-100 hover:bg-gray-200 text-gray-700 border border-gray-300 py-2 rounded-md text-sm transition-colors">
              Cancelar
            </button>
          )}
        </form>
      </div>
    </div>
  )
}
