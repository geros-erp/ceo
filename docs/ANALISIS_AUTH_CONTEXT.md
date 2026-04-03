# Análisis de AuthContext.jsx

## Información General

**Archivo**: `frontend/src/context/AuthContext.jsx`  
**Propósito**: Gestión centralizada de autenticación y autorización  
**Tipo**: React Context Provider  
**Líneas de código**: ~160

## Estructura del Componente

### 1. Imports

```javascript
import { createContext, useContext, useEffect, useState } from 'react'
import { getMyMenu } from '../api/menu'
import { logout as logoutApi } from '../api/auth'
```

**Dependencias**:
- React hooks: `createContext`, `useContext`, `useEffect`, `useState`
- API de menú: `getMyMenu` - Obtiene menú del usuario autenticado
- API de autenticación: `logoutApi` - Cierra sesión en backend

### 2. Context Creation

```javascript
const AuthContext = createContext(null)
```

**Propósito**: Crear contexto de React para compartir estado de autenticación en toda la aplicación.

### 3. Función Auxiliar: flattenMenuPaths

```javascript
function flattenMenuPaths(items = []) {
  return items.flatMap(item => [
    ...(item.path ? [item.path] : []),
    ...flattenMenuPaths(item.children || []),
  ])
}
```

**Propósito**: Aplanar estructura jerárquica de menú en array de rutas permitidas.

**Ejemplo**:
```javascript
// Input:
[
  { path: '/dashboard', children: [
    { path: '/users' },
    { path: '/roles' }
  ]}
]

// Output:
['/dashboard', '/users', '/roles']
```

**Análisis**:
- ✅ Recursiva - Maneja menús anidados
- ✅ Funcional - Usa `flatMap` para aplanar
- ✅ Segura - Maneja casos sin `path` o `children`

## Estado del Componente

### 1. Estado `user`

**Inicialización**:
```javascript
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
  
  return token ? { /* objeto user */ } : null
})
```

**Estructura del objeto `user`**:
```javascript
{
  token: string,                      // JWT token
  email: string,                      // Email del usuario
  role: string,                       // Rol (ADMIN, USER)
  allowedPaths: string[],             // Rutas permitidas
  passwordExpiresInDays: number|null, // Días para expiración de contraseña
  currentLoginAt: string,             // Timestamp de login actual
  previousLoginAt: string,            // Timestamp de login anterior
  currentLoginIp: string,             // IP de login actual
  previousLoginIp: string,            // IP de login anterior
  sessionTimeoutSeconds: number       // Timeout de sesión (default: 1800)
}
```

**Análisis**:
- ✅ **Persistencia**: Usa localStorage para mantener sesión entre recargas
- ✅ **Lazy initialization**: Función en useState para evitar lecturas innecesarias
- ✅ **Valores por defecto**: sessionTimeoutSeconds = 1800 (30 minutos)
- ✅ **Parsing seguro**: `JSON.parse(... || '[]')` evita errores
- ⚠️ **Seguridad**: Token en localStorage (vulnerable a XSS, pero aceptable con CSP)

### 2. Estado `menu`

```javascript
const [menu, setMenu] = useState([])
```

**Propósito**: Almacenar estructura de menú del usuario.

**Estructura esperada**:
```javascript
[
  {
    path: '/dashboard',
    label: 'Dashboard',
    icon: 'dashboard',
    children: [
      { path: '/users', label: 'Usuarios' },
      { path: '/roles', label: 'Roles' }
    ]
  }
]
```

### 3. Estado `authzLoading`

```javascript
const [authzLoading, setAuthzLoading] = useState(false)
```

**Propósito**: Indicar si se está cargando información de autorización (menú).

## Funciones Principales

### 1. saveUser(data)

```javascript
const saveUser = (data) => {
  localStorage.setItem('token', data.token)
  localStorage.setItem('email', data.email)
  localStorage.setItem('role',  data.role)
  localStorage.removeItem('allowedPaths')

  if (data.passwordExpiresInDays != null) {
    localStorage.setItem('passwordExpiresInDays', data.passwordExpiresInDays)
  } else {
    localStorage.removeItem('passwordExpiresInDays')
  }

  // ... más campos ...

  setUser({ ...data, allowedPaths: [] })
}
```

**Propósito**: Guardar información del usuario después del login.

**Análisis**:
- ✅ **Persistencia completa**: Guarda todos los campos en localStorage
- ✅ **Limpieza condicional**: Remueve campos si no existen
- ✅ **Inicialización de allowedPaths**: Empieza vacío, se llena con useEffect
- ✅ **Conversión de tipos**: sessionTimeoutSeconds a string para localStorage
- ⚠️ **Redundancia**: Guarda en localStorage Y en estado (necesario para persistencia)

**Campos guardados**:
1. `token` - JWT token (obligatorio)
2. `email` - Email del usuario (obligatorio)
3. `role` - Rol del usuario (obligatorio)
4. `passwordExpiresInDays` - Días para expiración (opcional)
5. `currentLoginAt` - Timestamp de login actual (opcional)
6. `previousLoginAt` - Timestamp de login anterior (opcional)
7. `currentLoginIp` - IP de login actual (opcional)
8. `previousLoginIp` - IP de login anterior (opcional)
9. `sessionTimeoutSeconds` - Timeout de sesión (default: 1800)

### 2. logout(reason = 'manual')

```javascript
const logout = async (reason = 'manual') => {
  try {
    await logoutApi()
  } catch (error) {
    console.log('⚠️ Error en logout del backend (ignorado):', error.response?.status)
  }
  
  // Limpiar historial de navegación
  if (window.history.length > 1) {
    window.history.replaceState(null, '', '/login')
  }
  
  // Limpieza completa de datos locales
  localStorage.clear()
  sessionStorage.clear()
  
  // Limpiar cookies
  document.cookie.split(";").forEach((c) => {
    document.cookie = c
      .replace(/^ +/, "")
      .replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/")
  })
  
  // Limpiar caché del navegador
  if ('caches' in window) {
    caches.keys().then((names) => {
      names.forEach(name => caches.delete(name))
    })
  }
  
  // Limpiar estado de la aplicación
  setMenu([])
  setUser(null)
  
  console.log(`🔒 Logout seguro completado (historial limpiado). Razón: ${reason}`)
}
```

**Propósito**: Cerrar sesión de forma segura y completa.

**Análisis**:
- ✅ **Logout en backend**: Intenta cerrar sesión en servidor
- ✅ **Tolerante a fallos**: Continúa logout local aunque backend falle
- ✅ **Limpieza de historial**: Previene navegación hacia atrás
- ✅ **Limpieza completa**: localStorage, sessionStorage, cookies, caché
- ✅ **Limpieza de estado**: Resetea menu y user
- ✅ **Logging**: Registra razón del logout (manual, timeout, etc.)
- ✅ **Seguridad**: Implementa logout seguro según requisitos

**Razones de logout**:
- `'manual'` - Usuario cierra sesión manualmente
- `'timeout'` - Sesión expirada por inactividad
- `'unauthorized'` - Token inválido o expirado

**Flujo de limpieza**:
```
1. Backend logout (intento)
   ↓
2. Limpiar historial del navegador
   ↓
3. Limpiar localStorage
   ↓
4. Limpiar sessionStorage
   ↓
5. Limpiar cookies
   ↓
6. Limpiar caché
   ↓
7. Resetear estado de React
   ↓
8. Log de confirmación
```

## useEffect - Carga de Menú

```javascript
useEffect(() => {
  if (!user?.token) return

  let active = true
  setAuthzLoading(true)

  getMyMenu()
    .then(({ data }) => {
      if (!active) return
      const allowedPaths = flattenMenuPaths(data)
      localStorage.setItem('allowedPaths', JSON.stringify(allowedPaths))
      setMenu(data)
      setUser(current => current ? { ...current, allowedPaths } : current)
    })
    .catch(() => {
      if (active) logout()
    })
    .finally(() => {
      if (active) setAuthzLoading(false)
    })

  return () => {
    active = false
  }
}, [user?.token])
```

**Propósito**: Cargar menú y rutas permitidas cuando el usuario está autenticado.

**Análisis**:
- ✅ **Condicional**: Solo ejecuta si hay token
- ✅ **Cleanup**: Variable `active` previene actualizaciones después de unmount
- ✅ **Loading state**: Indica cuando está cargando
- ✅ **Persistencia**: Guarda allowedPaths en localStorage
- ✅ **Actualización de estado**: Actualiza menu y user.allowedPaths
- ✅ **Manejo de errores**: Logout automático si falla (token inválido)
- ⚠️ **Dependencia**: Solo depende de `user?.token`, no de función logout

**Flujo**:
```
1. Usuario tiene token
   ↓
2. Activar loading
   ↓
3. Llamar API getMyMenu()
   ↓
4. Aplanar menú a rutas
   ↓
5. Guardar en localStorage
   ↓
6. Actualizar estado (menu + user.allowedPaths)
   ↓
7. Desactivar loading
```

**En caso de error**:
```
API falla (401, 403, etc.)
   ↓
Logout automático
   ↓
Redirigir a login
```

## Valor del Context

```javascript
const allowedPaths = user?.allowedPaths || []
const value = {
  user,
  menu,
  authzLoading,
  saveUser,
  logout,
  hasPathAccess: (path) => !path || allowedPaths.includes(path),
  defaultPath: allowedPaths[0] || '/change-password',
}
```

**Propiedades expuestas**:

1. **`user`** (object|null)
   - Información del usuario autenticado
   - `null` si no hay sesión

2. **`menu`** (array)
   - Estructura de menú del usuario
   - Vacío si no está cargado

3. **`authzLoading`** (boolean)
   - `true` mientras carga menú
   - `false` cuando termina

4. **`saveUser(data)`** (function)
   - Guardar usuario después de login
   - Parámetro: objeto con datos del usuario

5. **`logout(reason)`** (function)
   - Cerrar sesión
   - Parámetro opcional: razón del logout

6. **`hasPathAccess(path)`** (function)
   - Verificar si usuario tiene acceso a ruta
   - Retorna `true` si path es null o está en allowedPaths
   - Uso: `hasPathAccess('/users')` → `true/false`

7. **`defaultPath`** (string)
   - Primera ruta permitida del usuario
   - Fallback: `/change-password`
   - Uso: Redirección después de login

**Análisis**:
- ✅ **API completa**: Todas las operaciones de auth/authz
- ✅ **Funciones auxiliares**: hasPathAccess, defaultPath
- ✅ **Valores por defecto**: Maneja casos sin datos
- ✅ **Inmutabilidad**: No expone setters directos

## Hook Personalizado

```javascript
export const useAuth = () => useContext(AuthContext)
```

**Propósito**: Hook para consumir el contexto de autenticación.

**Uso en componentes**:
```javascript
import { useAuth } from '../context/AuthContext'

function MyComponent() {
  const { user, logout, hasPathAccess } = useAuth()
  
  if (!user) return <Navigate to="/login" />
  
  return (
    <div>
      <p>Bienvenido {user.email}</p>
      {hasPathAccess('/admin') && <AdminPanel />}
      <button onClick={() => logout('manual')}>Cerrar sesión</button>
    </div>
  )
}
```

## Análisis de Seguridad

### ✅ Fortalezas

1. **Logout seguro completo**:
   - Limpia localStorage, sessionStorage, cookies, caché
   - Limpia historial de navegación
   - Previene navegación hacia atrás después de logout

2. **Tolerancia a fallos**:
   - Logout local continúa aunque backend falle
   - Maneja errores de API sin romper la aplicación

3. **Autorización basada en rutas**:
   - `hasPathAccess()` verifica permisos
   - Rutas permitidas cargadas desde backend

4. **Persistencia de sesión**:
   - Mantiene sesión entre recargas de página
   - Recupera estado desde localStorage

5. **Logging de auditoría**:
   - Registra razón de logout
   - Útil para debugging y auditoría

### ⚠️ Consideraciones

1. **Token en localStorage**:
   - **Riesgo**: Vulnerable a XSS
   - **Mitigación**: CSP (Content Security Policy) implementado
   - **Alternativa**: httpOnly cookies (requiere cambios en backend)

2. **Dependencia de useEffect**:
   - No incluye `logout` en dependencias
   - Podría causar warnings de ESLint
   - **Solución**: Usar `useCallback` para logout

3. **Manejo de errores**:
   - Logout automático en error de menú
   - Podría ser agresivo si es error temporal
   - **Mejora**: Reintentos antes de logout

4. **Sincronización de tabs**:
   - No sincroniza logout entre pestañas
   - **Mejora**: Usar `storage` event listener

## Análisis de Rendimiento

### ✅ Optimizaciones

1. **Lazy initialization**:
   - `useState(() => {...})` evita lecturas innecesarias

2. **Cleanup en useEffect**:
   - Variable `active` previene memory leaks

3. **Memoización implícita**:
   - `allowedPaths` calculado una vez por render

### ⚠️ Posibles Mejoras

1. **Memoizar funciones**:
   ```javascript
   const logout = useCallback(async (reason = 'manual') => {
     // ...
   }, [])
   
   const hasPathAccess = useCallback((path) => {
     return !path || allowedPaths.includes(path)
   }, [allowedPaths])
   ```

2. **Memoizar valor del context**:
   ```javascript
   const value = useMemo(() => ({
     user,
     menu,
     authzLoading,
     saveUser,
     logout,
     hasPathAccess,
     defaultPath,
   }), [user, menu, authzLoading, /* ... */])
   ```

## Flujo de Autenticación

### Login Flow

```
1. Usuario ingresa credenciales
   ↓
2. API login() retorna datos
   ↓
3. saveUser(data) guarda en localStorage y estado
   ↓
4. useEffect detecta user.token
   ↓
5. getMyMenu() carga menú y permisos
   ↓
6. allowedPaths actualizado
   ↓
7. Usuario autenticado y autorizado
```

### Logout Flow

```
1. Usuario/Sistema llama logout(reason)
   ↓
2. Intenta logout en backend
   ↓
3. Limpia historial del navegador
   ↓
4. Limpia localStorage, sessionStorage, cookies, caché
   ↓
5. Resetea estado (user = null, menu = [])
   ↓
6. Log de confirmación
   ↓
7. Redirige a /login
```

### Authorization Check Flow

```
1. Componente llama hasPathAccess('/users')
   ↓
2. Verifica si '/users' está en allowedPaths
   ↓
3. Retorna true/false
   ↓
4. Componente renderiza o bloquea acceso
```

## Integración con Otros Componentes

### 1. Login.jsx

```javascript
const { saveUser } = useAuth()

const handleLogin = async (credentials) => {
  const { data } = await login(credentials)
  saveUser(data)
  navigate('/dashboard')
}
```

### 2. PrivateRoute (App.jsx)

```javascript
const { user, authzLoading, hasPathAccess } = useAuth()

if (!user) return <Navigate to="/login" />
if (authzLoading) return <Loading />
if (!hasPathAccess(path)) return <Navigate to="/unauthorized" />

return <Component />
```

### 3. SessionTimeout.jsx

```javascript
const { logout, user } = useAuth()

useEffect(() => {
  const timeout = user?.sessionTimeoutSeconds || 1800
  // Configurar timer de inactividad
  // Al expirar: logout('timeout')
}, [user?.sessionTimeoutSeconds])
```

### 4. Layout.jsx

```javascript
const { user, menu, logout } = useAuth()

return (
  <div>
    <Header user={user} onLogout={() => logout('manual')} />
    <Sidebar menu={menu} />
    <Content />
  </div>
)
```

## Mejoras Sugeridas

### 1. Usar useCallback para funciones

```javascript
const logout = useCallback(async (reason = 'manual') => {
  // ... código actual ...
}, [])

const saveUser = useCallback((data) => {
  // ... código actual ...
}, [])
```

**Beneficio**: Evita re-renders innecesarios en componentes hijos.

### 2. Sincronizar logout entre tabs

```javascript
useEffect(() => {
  const handleStorageChange = (e) => {
    if (e.key === 'token' && !e.newValue) {
      // Token eliminado en otra tab
      setUser(null)
      setMenu([])
    }
  }
  
  window.addEventListener('storage', handleStorageChange)
  return () => window.removeEventListener('storage', handleStorageChange)
}, [])
```

**Beneficio**: Logout en una pestaña cierra sesión en todas.

### 3. Reintentos en carga de menú

```javascript
const loadMenuWithRetry = async (retries = 3) => {
  for (let i = 0; i < retries; i++) {
    try {
      const { data } = await getMyMenu()
      return data
    } catch (error) {
      if (i === retries - 1) throw error
      await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)))
    }
  }
}
```

**Beneficio**: Más resiliente a errores temporales de red.

### 4. Refresh token automático

```javascript
useEffect(() => {
  if (!user?.token) return
  
  const refreshInterval = setInterval(async () => {
    try {
      const { data } = await refreshToken()
      saveUser(data)
    } catch {
      logout('token_expired')
    }
  }, 15 * 60 * 1000) // Cada 15 minutos
  
  return () => clearInterval(refreshInterval)
}, [user?.token])
```

**Beneficio**: Mantiene sesión activa sin requerir re-login.

## Resumen de Calidad del Código

### Puntuación General: 8.5/10

**Fortalezas** (9/10):
- ✅ Código limpio y bien estructurado
- ✅ Manejo robusto de errores
- ✅ Logout seguro implementado
- ✅ Persistencia de sesión
- ✅ Autorización basada en rutas

**Seguridad** (8/10):
- ✅ Logout completo (localStorage, cookies, caché, historial)
- ✅ Tolerante a fallos
- ⚠️ Token en localStorage (mitigado con CSP)
- ⚠️ Sin sincronización entre tabs

**Rendimiento** (8/10):
- ✅ Lazy initialization
- ✅ Cleanup en useEffect
- ⚠️ Funciones no memoizadas
- ⚠️ Valor del context no memoizado

**Mantenibilidad** (9/10):
- ✅ Código legible
- ✅ Funciones bien nombradas
- ✅ Comentarios útiles
- ✅ Estructura clara

## Conclusión

El archivo `AuthContext.jsx` es un componente **bien diseñado y robusto** que maneja correctamente la autenticación y autorización de la aplicación. Implementa características de seguridad importantes como logout seguro completo y limpieza de historial.

**Puntos destacados**:
1. Logout seguro con limpieza completa
2. Persistencia de sesión entre recargas
3. Autorización basada en rutas desde backend
4. Manejo robusto de errores

**Áreas de mejora**:
1. Memoizar funciones con useCallback
2. Sincronizar logout entre pestañas
3. Implementar refresh token automático
4. Agregar reintentos en carga de menú

El código cumple con los requisitos de seguridad establecidos y proporciona una base sólida para la gestión de autenticación en la aplicación.
