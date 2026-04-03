import { useEffect, useRef, useCallback, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'
import SessionWarningModal from './SessionWarningModal'

export default function SessionTimeout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const timeoutRef = useRef(null)
  const warningTimeoutRef = useRef(null)
  const [showWarning, setShowWarning] = useState(false)
  const [warningSeconds, setWarningSeconds] = useState(60)

  const handleLogout = useCallback(async () => {
    console.log('🔴 SessionTimeout: Cerrando sesión por inactividad')
    setShowWarning(false)
    
    // Limpiar historial antes de logout
    window.history.replaceState(null, '', '/login')
    
    await logout('timeout')
    navigate('/login', { 
      replace: true,
      state: { message: 'Tu sesión ha expirado por inactividad. Por favor, inicia sesión nuevamente.' }
    })
  }, [logout, navigate])

  const resetTimer = useCallback(() => {
    if (!user?.sessionTimeoutSeconds) {
      console.log('⚠️ SessionTimeout: No hay sessionTimeoutSeconds configurado')
      return
    }

    console.log(`🔄 SessionTimeout: Reiniciando timer (${user.sessionTimeoutSeconds}s)`)

    // Ocultar advertencia si está visible
    setShowWarning(false)

    // Limpiar timers existentes
    if (timeoutRef.current) clearTimeout(timeoutRef.current)
    if (warningTimeoutRef.current) clearTimeout(warningTimeoutRef.current)

    const timeoutMs = user.sessionTimeoutSeconds * 1000
    const warningTime = 60 // Advertir 60 segundos antes
    const warningMs = Math.max(timeoutMs - (warningTime * 1000), timeoutMs * 0.8)

    console.log(`⏱️ SessionTimeout: Modal aparecerá en ${warningMs / 1000}s, logout en ${timeoutMs / 1000}s`)

    // Timer de advertencia
    warningTimeoutRef.current = setTimeout(() => {
      console.log('⚠️ SessionTimeout: Mostrando modal de advertencia')
      setWarningSeconds(warningTime)
      setShowWarning(true)
    }, warningMs)

    // Timer de cierre de sesión
    timeoutRef.current = setTimeout(() => {
      console.log('🔴 SessionTimeout: Tiempo agotado, cerrando sesión')
      handleLogout()
    }, timeoutMs)
  }, [user?.sessionTimeoutSeconds, handleLogout])

  const handleExtendSession = useCallback(() => {
    console.log('✅ SessionTimeout: Usuario extendió la sesión')
    setShowWarning(false)
    resetTimer()
  }, [resetTimer])

  useEffect(() => {
    if (!user?.token) return

    // Eventos que indican actividad del usuario
    const events = [
      'mousedown',
      'mousemove',
      'keypress',
      'scroll',
      'touchstart',
      'click',
    ]

    // Iniciar el timer
    resetTimer()

    // Agregar listeners para detectar actividad
    events.forEach(event => {
      document.addEventListener(event, resetTimer, true)
    })

    // Cleanup
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current)
      if (warningTimeoutRef.current) clearTimeout(warningTimeoutRef.current)
      events.forEach(event => {
        document.removeEventListener(event, resetTimer, true)
      })
    }
  }, [user?.token, resetTimer])

  return (
    <SessionWarningModal
      show={showWarning}
      remainingSeconds={warningSeconds}
      onExtend={handleExtendSession}
      onLogout={handleLogout}
    />
  )
}
