import { useState, useCallback, useEffect } from 'react'
import { useNavigate, Link, useLocation } from 'react-router-dom'
import { useGoogleReCaptcha } from 'react-google-recaptcha-v3'
import { login } from '../../api/auth'
import { useAuth } from '../../context/AuthContext'
import { usePreventBackNavigation } from '../../hooks/usePreventBackNavigation'

export default function Login() {
  usePreventBackNavigation() // Prevenir volver atrás después de logout
  const location = useLocation()
  const [form, setForm]       = useState({ username: '', password: '' })
  const [error, setError]     = useState('')
  const [info, setInfo]       = useState(location.state?.message || '')
  const [loading, setLoading] = useState(false)
  const { saveUser }          = useAuth()
  const navigate              = useNavigate()
  const { executeRecaptcha }  = useGoogleReCaptcha()

  useEffect(() => {
    if (location.state?.message) {
      setInfo(location.state.message)
      // Limpiar el mensaje del state para que no persista en el historial
      window.history.replaceState({}, document.title)
    }
  }, [location])

  const handleChange = (e) =>
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = useCallback(async (e) => {
    e.preventDefault()
    setError('')
    setInfo('')
    setLoading(true)
    try {
      const recaptchaToken = executeRecaptcha
        ? await executeRecaptcha('login')
        : null

      const { data } = await login(form.username, form.password, recaptchaToken)
      saveUser(data)
      if (data.mustChangePassword)
        navigate('/change-password', { state: { forced: true } })
      else
        navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.message || 'Credenciales inválidas')
    } finally {
      setLoading(false)
    }
  }, [form, executeRecaptcha, saveUser, navigate])

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white p-8 rounded-xl shadow-md w-full max-w-sm">
        <h2 className="text-2xl font-semibold text-center text-gray-800 mb-6">Iniciar sesión</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="flex flex-col gap-1">
            <label className="text-sm text-gray-600" htmlFor="username">Usuario o Correo</label>
            <input id="username" name="username" type="text" value={form.username}
              onChange={handleChange} required autoComplete="username"
              placeholder="nombre_usuario o correo@ejemplo.com"
              className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500" />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-sm text-gray-600" htmlFor="password">Contraseña</label>
            <input id="password" name="password" type="password" value={form.password}
              onChange={handleChange} required autoComplete="current-password"
              className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500" />
          </div>
          {info && <p className="text-blue-600 text-sm bg-blue-50 p-3 rounded-md">{info}</p>}
          {error && <p className="text-red-600 text-sm">{error}</p>}
          <button type="submit" disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white py-2 rounded-md text-sm font-medium transition-colors">
            {loading ? 'Ingresando...' : 'Ingresar'}
          </button>
          <p className="text-center text-sm text-gray-500">
            <Link to="/forgot-password" className="text-indigo-600 hover:underline">
              ¿Olvidaste tu contraseña?
            </Link>
          </p>
        </form>
        <p className="text-center text-xs text-gray-400 mt-4">
          Protegido por reCAPTCHA
        </p>
      </div>
    </div>
  )
}
