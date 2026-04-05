import { useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { useGoogleReCaptcha } from 'react-google-recaptcha-v3'
import { forgotPassword } from '../../api/auth'

export default function ForgotPassword() {
  const [email, setEmail]     = useState('')
  const [sent, setSent]       = useState(false)
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)
  const { executeRecaptcha }  = useGoogleReCaptcha()

  const handleSubmit = useCallback(async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const recaptchaToken = executeRecaptcha
        ? await executeRecaptcha('forgot_password')
        : null

      await forgotPassword(email, recaptchaToken)
      setSent(true)
    } catch (err) {
      setError(err.response?.data?.message || 'Error al procesar la solicitud')
    } finally {
      setLoading(false)
    }
  }, [email, executeRecaptcha])

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white p-8 rounded-xl shadow-md w-full max-w-sm">
        <h2 className="text-2xl font-semibold text-center text-gray-800 mb-2">
          Recuperar contraseña
        </h2>

        {sent ? (
          <div className="text-center space-y-4">
            <div className="text-5xl">✉️</div>
            <p className="text-gray-600 text-sm">
              Si el correo <strong>{email}</strong> está registrado, recibirás un enlace para restablecer tu contraseña.
            </p>
            <p className="text-xs text-gray-400">El enlace expira en 1 hora.</p>
            <Link to="/login" className="block text-indigo-600 hover:underline text-sm">
              Volver al inicio de sesión
            </Link>
          </div>
        ) : (
          <>
            <p className="text-sm text-gray-500 text-center mb-6">
              Ingresa tu correo y te enviaremos un enlace para restablecer tu contraseña.
            </p>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="flex flex-col gap-1">
                <label className="text-sm text-gray-600">Correo electrónico</label>
                <input type="email" value={email} onChange={e => setEmail(e.target.value)}
                  required autoComplete="email"
                  className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:border-indigo-500" />
              </div>
              {error && <p className="text-red-600 text-sm">{error}</p>}
              <button type="submit" disabled={loading}
                className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white py-2 rounded-md text-sm font-medium transition-colors">
                {loading ? 'Enviando...' : 'Enviar enlace'}
              </button>
              <p className="text-center text-sm">
                <Link to="/login" className="text-indigo-600 hover:underline">
                  Volver al inicio de sesión
                </Link>
              </p>
            </form>
            <p className="text-center text-xs text-gray-400 mt-4">
              Protegido por reCAPTCHA
            </p>
          </>
        )}
      </div>
    </div>
  )
}
