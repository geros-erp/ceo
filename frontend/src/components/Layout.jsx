import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useBlockBrowserHistory } from '../hooks/useBlockBrowserHistory'
import Sidebar from './Sidebar'

export default function Layout({ children }) {
  const { user, logout }  = useAuth()
  const navigate  = useNavigate()
  const expiresInDays = user?.passwordExpiresInDays

  // Bloquear navegación histórica durante la sesión
  useBlockBrowserHistory()

  const formatDateTime = (dateStr) => {
    if (!dateStr) return 'N/A'
    try {
      const date = new Date(dateStr)
      return date.toLocaleString('es-CO', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
      })
    } catch {
      return dateStr
    }
  }

  return (
    <div className="flex min-h-[calc(100vh-56px)]">
      <Sidebar />
      <div className="flex-1 overflow-y-auto bg-gray-100">
        {/* Header con información de sesión */}
        <div className="bg-white border-b border-gray-200 px-6 py-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-6 text-xs text-gray-600">
              <div className="flex items-center gap-2">
                <span className="font-semibold text-gray-700">👤 {user?.email}</span>
                <span className="text-gray-400">|</span>
                <span className="text-indigo-600 font-medium">{user?.role}</span>
              </div>
              {user?.currentLoginAt && (
                <>
                  <span className="text-gray-400">|</span>
                  <div className="flex items-center gap-1">
                    <span className="text-gray-500">🕒 Sesión actual:</span>
                    <span className="font-medium">{formatDateTime(user.currentLoginAt)}</span>
                    <span className="text-gray-400">desde</span>
                    <span className="font-medium">{user?.currentLoginIp || 'N/A'}</span>
                  </div>
                </>
              )}
              {user?.previousLoginAt && (
                <>
                  <span className="text-gray-400">|</span>
                  <div className="flex items-center gap-1">
                    <span className="text-gray-500">🕙 Última sesión:</span>
                    <span className="font-medium">{formatDateTime(user.previousLoginAt)}</span>
                    <span className="text-gray-400">desde</span>
                    <span className="font-medium">{user?.previousLoginIp || 'N/A'}</span>
                  </div>
                </>
              )}
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => navigate('/change-password')}
                className="text-xs bg-indigo-50 text-indigo-600 px-3 py-1.5 rounded-md border border-indigo-200 hover:bg-indigo-100 transition-colors font-medium"
              >
                🔑 Cambiar contraseña
              </button>
              <button
                onClick={() => navigate('/validate-site')}
                className="text-xs bg-white px-3 py-1.5 rounded-md border border-gray-300 hover:bg-gray-50 transition-colors"
              >
                🔒 Verificar sitio
              </button>
              <button
                onClick={() => logout('manual')}
                className="text-xs bg-red-50 text-red-600 px-3 py-1.5 rounded-md border border-red-200 hover:bg-red-100 transition-colors font-medium"
              >
                🚪 Cerrar sesión
              </button>
            </div>
          </div>
        </div>
        
        {expiresInDays != null && (
          <div className="bg-amber-50 border-b border-amber-300 px-8 py-2 text-sm text-amber-800">
            ⚠️ Tu contraseña expira en <strong>{expiresInDays} día{expiresInDays !== 1 ? 's' : ''}</strong>.{' '}
            <span className="underline cursor-pointer font-medium text-amber-700"
              onClick={() => navigate('/change-password')}>
              Cambiarla ahora
            </span>
          </div>
        )}
        {children}
      </div>
    </div>
  )
}
