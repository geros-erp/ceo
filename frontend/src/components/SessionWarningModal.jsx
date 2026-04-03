import { useEffect, useState } from 'react'

export default function SessionWarningModal({ show, remainingSeconds, onExtend, onLogout }) {
  const [seconds, setSeconds] = useState(remainingSeconds)

  useEffect(() => {
    if (!show) return
    
    console.log(`⏱️ SessionWarningModal: Iniciando contador desde ${remainingSeconds}s`)
    setSeconds(remainingSeconds)
    const interval = setInterval(() => {
      setSeconds(prev => {
        const newValue = prev - 1
        console.log(`⏱️ SessionWarningModal: ${newValue}s restantes`)
        if (prev <= 1) {
          clearInterval(interval)
          console.log('🔴 SessionWarningModal: Contador llegó a 0, llamando a onLogout')
          // Llamar a onLogout cuando llegue a 0
          setTimeout(() => onLogout(), 100)
          return 0
        }
        return newValue
      })
    }, 1000)

    return () => {
      console.log('🧹 SessionWarningModal: Limpiando interval')
      clearInterval(interval)
    }
  }, [show, remainingSeconds, onLogout])

  if (!show) return null

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl p-6 max-w-md w-full mx-4">
        <div className="flex items-center gap-3 mb-4">
          <div className="flex-shrink-0 w-10 h-10 bg-yellow-100 rounded-full flex items-center justify-center">
            <svg className="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-gray-900">Sesión por expirar</h3>
        </div>
        
        <p className="text-sm text-gray-600 mb-4">
          Tu sesión está a punto de expirar por inactividad. ¿Deseas continuar?
        </p>
        
        <div className="bg-gray-50 rounded-lg p-3 mb-4">
          <p className="text-center text-2xl font-bold text-gray-900">{seconds}s</p>
          <p className="text-center text-xs text-gray-500">Tiempo restante</p>
        </div>

        <div className="flex gap-3">
          <button
            onClick={onExtend}
            className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white py-2 px-4 rounded-md text-sm font-medium transition-colors"
          >
            Continuar sesión
          </button>
          <button
            onClick={onLogout}
            className="flex-1 bg-gray-200 hover:bg-gray-300 text-gray-700 py-2 px-4 rounded-md text-sm font-medium transition-colors"
          >
            Cerrar sesión
          </button>
        </div>
      </div>
    </div>
  )
}
