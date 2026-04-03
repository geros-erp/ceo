import { createContext, useContext, useEffect, useState, useCallback, useMemo } from 'react'
import { getMyMenu } from '../api/menu'
import { logout as logoutApi } from '../api/auth'

const AuthContext = createContext(null)

function flattenMenuPaths(items = []) {
  return items.flatMap(item => [
    ...(item.path ? [item.path] : []),
    ...flattenMenuPaths(item.children || []),
  ])
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const token = localStorage.getItem('token')
    const email = localStorage.getItem('email')
    const role = localStorage.getItem('role')
    const passwordExpiresInDays = localStorage.getItem('passwordExpiresInDays')
    const currentLoginAt = localStorage.getItem('currentLoginAt')
    const previousLoginAt = localStorage.getItem('previousLoginAt')
    const currentLoginIp = localStorage.getItem('currentLoginIp')
    const previousLoginIp = localStorage.getItem('previousLoginIp')
    const sessionTimeoutSeconds = localStorage.getItem('sessionTimeoutSeconds')
    const allowedPaths = JSON.parse(localStorage.getItem('allowedPaths') || '[]')
    return token
      ? {
          token,
          email,
          role,
          allowedPaths,
          passwordExpiresInDays: passwordExpiresInDays ? parseInt(passwordExpiresInDays) : null,
          currentLoginAt,
          previousLoginAt,
          currentLoginIp,
          previousLoginIp,
          sessionTimeoutSeconds: sessionTimeoutSeconds ? parseInt(sessionTimeoutSeconds) : 1800,
        }
      : null
  })
  const [menu, setMenu] = useState([])
  const [authzLoading, setAuthzLoading] = useState(false)

  const saveUser = useCallback((data) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('email', data.email)
    localStorage.setItem('role',  data.role)
    localStorage.removeItem('allowedPaths')

    if (data.passwordExpiresInDays != null) {
      localStorage.setItem('passwordExpiresInDays', data.passwordExpiresInDays)
    } else {
      localStorage.removeItem('passwordExpiresInDays')
    }

    if (data.currentLoginAt) {
      localStorage.setItem('currentLoginAt', data.currentLoginAt)
    } else {
      localStorage.removeItem('currentLoginAt')
    }
    if (data.previousLoginAt) {
      localStorage.setItem('previousLoginAt', data.previousLoginAt)
    } else {
      localStorage.removeItem('previousLoginAt')
    }
    if (data.currentLoginIp) {
      localStorage.setItem('currentLoginIp', data.currentLoginIp)
    } else {
      localStorage.removeItem('currentLoginIp')
    }
    if (data.previousLoginIp) {
      localStorage.setItem('previousLoginIp', data.previousLoginIp)
    } else {
      localStorage.removeItem('previousLoginIp')
    }
    if (data.sessionTimeoutSeconds) {
      localStorage.setItem('sessionTimeoutSeconds', data.sessionTimeoutSeconds.toString())
    } else {
      localStorage.setItem('sessionTimeoutSeconds', '1800')
    }

    setUser({ ...data, allowedPaths: [] })
  }, [])

  const logout = useCallback(async (reason = 'manual') => {
    try {
      // Intentar logout en el backend, pero no fallar si hay error
      await logoutApi()
    } catch (error) {
      // Ignorar errores de logout (ej: token expirado, sesión inválida)
      // El logout local debe continuar aun si el backend no responde
      console.log('⚠️ Error en logout del backend (ignorado):', error.response?.status)
    }
    
    // Limpiar historial de navegación
    if (window.history.length > 1) {
      // Reemplazar todas las entradas del historial con la página de login
      window.history.replaceState(null, '', '/login')
    }
    
    // Limpieza completa de datos locales
    localStorage.clear()
    sessionStorage.clear()
    
    // Limpiar cookies si existen
    document.cookie.split(";").forEach((c) => {
      document.cookie = c
        .replace(/^ +/, "")
        .replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/")
    })
    
    // Limpiar caché del navegador si es posible
    if ('caches' in window) {
      caches.keys().then((names) => {
        names.forEach(name => caches.delete(name))
      })
    }
    
    // Limpiar estado de la aplicación
    setMenu([])
    setUser(null)
    
    console.log(`🔒 Logout seguro completado (historial limpiado). Razón: ${reason}`)
  }, [])

  // Función para cargar menú con reintentos
  const loadMenuWithRetry = useCallback(async (retries = 3) => {
    for (let i = 0; i < retries; i++) {
      try {
        const { data } = await getMyMenu()
        return data
      } catch (error) {
        console.log(`⚠️ Error cargando menú (intento ${i + 1}/${retries}):`, error.response?.status)
        if (i === retries - 1) throw error
        // Espera exponencial: 1s, 2s, 4s
        await new Promise(resolve => setTimeout(resolve, 1000 * Math.pow(2, i)))
      }
    }
  }, [])

  useEffect(() => {
    if (!user?.token) return

    let active = true
    setAuthzLoading(true)

    loadMenuWithRetry()
      .then((data) => {
        if (!active) return
        const allowedPaths = flattenMenuPaths(data)
        localStorage.setItem('allowedPaths', JSON.stringify(allowedPaths))
        setMenu(data)
        setUser(current => current ? { ...current, allowedPaths } : current)
      })
      .catch(() => {
        if (active) logout('unauthorized')
      })
      .finally(() => {
        if (active) setAuthzLoading(false)
      })

    return () => {
      active = false
    }
  }, [user?.token, loadMenuWithRetry, logout])

  // Sincronizar logout entre pestañas
  useEffect(() => {
    const handleStorageChange = (e) => {
      // Si el token fue eliminado en otra pestaña, cerrar sesión en esta
      if (e.key === 'token' && !e.newValue && user) {
        console.log('🔄 Sesión cerrada en otra pestaña, sincronizando...')
        setUser(null)
        setMenu([])
      }
      // Si se agregó un token en otra pestaña, recargar usuario
      if (e.key === 'token' && e.newValue && !user) {
        console.log('🔄 Sesión iniciada en otra pestaña, sincronizando...')
        window.location.reload()
      }
    }
    
    window.addEventListener('storage', handleStorageChange)
    return () => window.removeEventListener('storage', handleStorageChange)
  }, [user])

  const allowedPaths = user?.allowedPaths || []
  
  const hasPathAccess = useCallback((path) => {
    return !path || allowedPaths.includes(path)
  }, [allowedPaths])
  
  const defaultPath = useMemo(() => {
    return '/dashboard'
  }, [])
  
  const value = useMemo(() => ({
    user,
    menu,
    authzLoading,
    saveUser,
    logout,
    hasPathAccess,
    defaultPath,
  }), [user, menu, authzLoading, saveUser, logout, hasPathAccess, defaultPath])

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
