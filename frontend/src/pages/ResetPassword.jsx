import { useState } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { resetPassword } from '../api/auth'

export default function ResetPassword() {
  const [form, setForm]         = useState({ newPassword: '', confirm: '' })
  const [error, setError]       = useState('')
  const [success, setSuccess]   = useState(false)
  const [loading, setLoading]   = useState(false)
  const [searchParams]          = useSearchParams()
  const navigate                = useNavigate()
  const token                   = searchParams.get('token')

  const handleChange = (e) =>
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (form.newPassword !== form.confirm) {
      setError('Las contraseñas no coinciden')
      return
    }
    setError('')
    setLoading(true)
    try {
      await resetPassword(token, form.newPassword)
      setSuccess(true)
      setTimeout(() => navigate('/login'), 3000)
    } catch (err) {
      setError(err.response?.data?.message || 'Error al restablecer la contraseña')
    } finally {
      setLoading(false)
    }
  }

  if (!token) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="bg-white p-8 rounded-xl shadow-md w-full max-w-sm text-center space-y-4">
          <p className="text-red-600 font-medium">Enlace inválido o expirado.</p>
          <Link to="/forgot-password" className="text-indigo-600 hover:underline text-sm">
            Solicitar nuevo enlace
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white p-8 rounded-xl shadow-md w-full max-w-sm">
        <h2 className="text-2xl font-semibold text-center text-gray-800 mb-6">
          Nueva contraseña
        </h2>

        {success ? (
          <div className="text-center space-y-3">
            <div className="text-5xl">✅</div>
            <p className="text-green-600 font-medium">Contraseña restablecida correctamente.</p>
            <p className="text-sm text-gray-500">Redirigiendo al login...</p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            {[
              { name: 'newPassword', label: 'Nueva contraseña' },
              { name: 'confirm',     label: 'Confirmar contraseña' },
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
              {loading ? 'Guardando...' : 'Restablecer contraseña'}
            </button>
            <p className="text-center text-sm">
              <Link to="/login" className="text-indigo-600 hover:underline">
                Volver al inicio de sesión
              </Link>
            </p>
          </form>
        )}
      </div>
    </div>
  )
}
