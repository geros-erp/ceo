import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

export function useBlockBrowserHistory() {
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    // Agregar entrada al historial para bloquear navegación atrás
    window.history.pushState(null, '', window.location.href)
    
    // Bloquear navegación hacia atrás/adelante
    const handlePopState = () => {
      // Volver a agregar entrada al historial para mantener al usuario en la página actual
      window.history.pushState(null, '', window.location.href)
      
      if (import.meta.env.DEV) {
        console.warn('🚫 Navegación histórica bloqueada durante la sesión')
      }
    }

    // Bloquear teclas de navegación (Alt+Left, Alt+Right, Backspace)
    const handleKeyDown = (e) => {
      // Alt + Flecha Izquierda (navegación atrás)
      if (e.altKey && e.key === 'ArrowLeft') {
        e.preventDefault()
        if (import.meta.env.DEV) {
          console.warn('🚫 Atajo de navegación atrás bloqueado')
        }
      }
      // Alt + Flecha Derecha (navegación adelante)
      if (e.altKey && e.key === 'ArrowRight') {
        e.preventDefault()
        if (import.meta.env.DEV) {
          console.warn('🚫 Atajo de navegación adelante bloqueado')
        }
      }
      // Backspace fuera de inputs (navegación atrás en algunos navegadores)
      if (e.key === 'Backspace' && 
          !['INPUT', 'TEXTAREA'].includes(e.target.tagName) &&
          !e.target.isContentEditable) {
        e.preventDefault()
        if (import.meta.env.DEV) {
          console.warn('🚫 Backspace bloqueado fuera de campos de texto')
        }
      }
    }

    // Bloquear gestos del mouse (botones adelante/atrás)
    const handleMouseButton = (e) => {
      // Botón 3 = atrás, Botón 4 = adelante
      if (e.button === 3 || e.button === 4) {
        e.preventDefault()
        if (import.meta.env.DEV) {
          console.warn('🚫 Botón de navegación del mouse bloqueado')
        }
      }
    }

    window.addEventListener('popstate', handlePopState)
    window.addEventListener('keydown', handleKeyDown, true)
    window.addEventListener('mouseup', handleMouseButton, true)

    return () => {
      window.removeEventListener('popstate', handlePopState)
      window.removeEventListener('keydown', handleKeyDown, true)
      window.removeEventListener('mouseup', handleMouseButton, true)
    }
  }, [location, navigate])
}
