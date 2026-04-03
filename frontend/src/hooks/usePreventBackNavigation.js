import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

export function usePreventBackNavigation() {
  const location = useLocation()

  useEffect(() => {
    window.history.pushState(null, '', window.location.href)
    
    const handlePopState = () => {
      window.history.pushState(null, '', window.location.href)
      if (import.meta.env.DEV) {
        console.warn('⚠️ Navegación hacia atrás bloqueada')
      }
    }

    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [location])
}
