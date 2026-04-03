# 🔐 Seguridad del Frontend - Portal Web

## Requisitos de Seguridad

### 1. Salida Segura (Secure Logout)
- Invalidar sesión en servidor
- Limpiar todos los datos locales
- Prevenir uso del botón "Atrás" del navegador
- Limpiar caché del navegador

### 2. Protección del Código Fuente
- Ofuscación de JavaScript
- Minificación de código
- Eliminación de source maps en producción
- Protección contra inspección de código

---

## 📋 PARTE 1: SALIDA SEGURA

### Implementación Actual vs Requerida

#### ✅ Ya Implementado

1. **Invalidación en Servidor**
   ```javascript
   // frontend/src/context/AuthContext.jsx
   const logout = async () => {
     await logoutApi() // ✅ Llama al backend
     localStorage.clear() // ✅ Limpia datos locales
     setUser(null) // ✅ Limpia estado
   }
   ```

2. **Limpieza de localStorage**
   - Token JWT eliminado
   - Email eliminado
   - Role eliminado
   - Todos los datos de sesión eliminados

#### ⚠️ Pendiente de Implementar

1. **Prevenir Botón "Atrás"**
2. **Limpiar Caché del Navegador**
3. **Invalidar Historial de Navegación**
4. **Limpiar sessionStorage**
5. **Revocar tokens en servidor**

---

## 🔧 Implementación: Salida Segura Completa

### Paso 1: Mejorar el Logout en AuthContext

**Archivo:** `frontend/src/context/AuthContext.jsx`

```javascript
const logout = async (reason = 'manual') => {
  try {
    // 1. Notificar al backend
    await logoutApi()
  } catch (error) {
    console.log('⚠️ Error en logout del backend (ignorado):', error.response?.status)
  }

  // 2. Limpiar TODOS los storages
  localStorage.clear()
  sessionStorage.clear()
  
  // 3. Limpiar cookies (si las hay)
  document.cookie.split(";").forEach((c) => {
    document.cookie = c
      .replace(/^ +/, "")
      .replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/")
  })

  // 4. Limpiar estado de la aplicación
  setMenu([])
  setUser(null)

  // 5. Limpiar caché del navegador (si es posible)
  if ('caches' in window) {
    caches.keys().then((names) => {
      names.forEach(name => caches.delete(name))
    })
  }

  // 6. Registrar razón del logout
  console.log(`🔒 Logout seguro completado. Razón: ${reason}`)
}
```

### Paso 2: Prevenir Botón "Atrás" del Navegador

**Crear:** `frontend/src/hooks/usePreventBackNavigation.js`

```javascript
import { useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'

export function usePreventBackNavigation() {
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    // Agregar entrada al historial para prevenir "atrás"
    window.history.pushState(null, '', window.location.href)
    
    const handlePopState = (event) => {
      // Prevenir navegación hacia atrás
      window.history.pushState(null, '', window.location.href)
      
      // Opcional: Mostrar advertencia
      console.warn('⚠️ Navegación hacia atrás bloqueada por seguridad')
    }

    window.addEventListener('popstate', handlePopState)

    return () => {
      window.removeEventListener('popstate', handlePopState)
    }
  }, [location])
}
```

**Usar en Login:**

```javascript
// frontend/src/pages/Login.jsx
import { usePreventBackNavigation } from '../hooks/usePreventBackNavigation'

export default function Login() {
  usePreventBackNavigation() // ✅ Prevenir volver atrás después de logout
  
  // ... resto del código
}
```

### Paso 3: Limpiar Historial al Hacer Logout

**Modificar:** `frontend/src/context/AuthContext.jsx`

```javascript
const logout = async (reason = 'manual') => {
  // ... código anterior ...

  // 7. Limpiar historial de navegación
  // Reemplazar la entrada actual del historial
  window.history.replaceState(null, '', '/login')
  
  // Navegar a login sin agregar al historial
  navigate('/login', { 
    replace: true, // ✅ No agregar al historial
    state: { 
      message: reason === 'timeout' 
        ? 'Tu sesión ha expirado por inactividad. Por favor, inicia sesión nuevamente.'
        : 'Has cerrado sesión correctamente.'
    }
  })
}
```

### Paso 4: Detectar Intento de Acceso Sin Autenticación

**Crear:** `frontend/src/components/SecureRoute.jsx`

```javascript
import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function SecureRoute({ children }) {
  const { user } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    // Si no hay usuario, redirigir inmediatamente
    if (!user) {
      // Limpiar cualquier dato residual
      localStorage.clear()
      sessionStorage.clear()
      
      // Redirigir a login
      navigate('/login', { 
        replace: true,
        state: { message: 'Debes iniciar sesión para acceder.' }
      })
    }
  }, [user, navigate])

  // Si no hay usuario, no renderizar nada
  if (!user) {
    return null
  }

  return children
}
```

### Paso 5: Invalidar Sesión al Cerrar Pestaña/Navegador

**Agregar en:** `frontend/src/App.jsx`

```javascript
import { useEffect } from 'react'
import { useAuth } from './context/AuthContext'

function App() {
  const { logout } = useAuth()

  useEffect(() => {
    // Detectar cierre de pestaña/navegador
    const handleBeforeUnload = (event) => {
      // Opcional: Hacer logout al cerrar
      // logout('browser_close')
      
      // O simplemente limpiar datos sensibles
      sessionStorage.clear()
    }

    // Detectar cuando la pestaña pierde visibilidad
    const handleVisibilityChange = () => {
      if (document.hidden) {
        // Opcional: Iniciar timer de inactividad más agresivo
        console.log('⚠️ Pestaña oculta - sesión en riesgo')
      }
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    document.addEventListener('visibilitychange', handleVisibilityChange)

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload)
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [logout])

  return (
    // ... resto del código
  )
}
```

---

## 📋 PARTE 2: PROTECCIÓN DEL CÓDIGO FUENTE

### Estrategia de Ofuscación y Protección

#### 1. Configuración de Build para Producción

**Modificar:** `frontend/vite.config.js`

```javascript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => ({
  plugins: [react(), tailwindcss()],
  
  server: {
    proxy: {
      '/api': {
        target: 'https://localhost:8443',
        secure: false,
        changeOrigin: true,
      },
    },
  },

  build: {
    // ✅ Minificación agresiva
    minify: 'terser',
    
    terserOptions: {
      compress: {
        // Eliminar console.log en producción
        drop_console: mode === 'production',
        drop_debugger: true,
        pure_funcs: mode === 'production' ? ['console.log', 'console.info', 'console.debug'] : [],
      },
      mangle: {
        // Ofuscar nombres de variables y funciones
        toplevel: true,
        safari10: true,
      },
      format: {
        // Eliminar comentarios
        comments: false,
      },
    },

    // ✅ NO generar source maps en producción
    sourcemap: mode !== 'production',

    // ✅ Dividir código en chunks
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'axios-vendor': ['axios'],
        },
        // Ofuscar nombres de archivos
        entryFileNames: mode === 'production' 
          ? 'assets/[name].[hash].js'
          : 'assets/[name].js',
        chunkFileNames: mode === 'production'
          ? 'assets/[name].[hash].js'
          : 'assets/[name].js',
        assetFileNames: mode === 'production'
          ? 'assets/[name].[hash].[ext]'
          : 'assets/[name].[ext]',
      },
    },

    // ✅ Optimizaciones adicionales
    cssCodeSplit: true,
    assetsInlineLimit: 4096,
    chunkSizeWarningLimit: 500,
  },

  // ✅ Optimización de dependencias
  optimizeDeps: {
    include: ['react', 'react-dom', 'react-router-dom', 'axios'],
  },
}))
```

#### 2. Instalar Dependencias de Ofuscación

**Ejecutar:**
```bash
cd frontend
npm install --save-dev terser vite-plugin-compression
```

#### 3. Agregar Plugin de Compresión

**Modificar:** `frontend/vite.config.js`

```javascript
import viteCompression from 'vite-plugin-compression'

export default defineConfig(({ mode }) => ({
  plugins: [
    react(), 
    tailwindcss(),
    // ✅ Comprimir archivos en producción
    mode === 'production' && viteCompression({
      algorithm: 'gzip',
      ext: '.gz',
      threshold: 10240, // Solo comprimir archivos > 10KB
      deleteOriginFile: false,
    }),
    mode === 'production' && viteCompression({
      algorithm: 'brotliCompress',
      ext: '.br',
      threshold: 10240,
      deleteOriginFile: false,
    }),
  ].filter(Boolean),
  // ... resto de la configuración
}))
```

#### 4. Protección Adicional en HTML

**Modificar:** `frontend/index.html`

```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  
  <!-- ✅ Prevenir inspección de código (parcial) -->
  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
  
  <!-- ✅ Content Security Policy -->
  <meta http-equiv="Content-Security-Policy" 
        content="default-src 'self'; 
                 script-src 'self' 'unsafe-inline' 'unsafe-eval' https://www.gstatic.com https://www.google.com; 
                 style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
                 font-src 'self' https://fonts.gstatic.com;
                 img-src 'self' data: https:;
                 connect-src 'self' https://localhost:8443;">
  
  <!-- ✅ Prevenir clickjacking -->
  <meta http-equiv="X-Frame-Options" content="DENY" />
  
  <!-- ✅ Prevenir MIME sniffing -->
  <meta http-equiv="X-Content-Type-Options" content="nosniff" />
  
  <title>GEROS - Sistema de Gestión</title>
</head>
<body>
  <div id="root"></div>
  
  <!-- ✅ Script para detectar DevTools (opcional) -->
  <script>
    // Detectar si DevTools está abierto
    (function() {
      const devtools = /./;
      devtools.toString = function() {
        this.opened = true;
      }
      
      const checkDevTools = () => {
        console.log('%c', devtools);
        if (devtools.opened) {
          console.warn('⚠️ Herramientas de desarrollo detectadas');
          // Opcional: Registrar evento en backend
        }
        devtools.opened = false;
      }
      
      // Solo en producción
      if (import.meta.env.PROD) {
        setInterval(checkDevTools, 1000);
      }
    })();
  </script>
  
  <script type="module" src="/src/main.jsx"></script>
</body>
</html>
```

#### 5. Deshabilitar Click Derecho y Atajos (Opcional)

**Crear:** `frontend/src/utils/disableDevTools.js`

```javascript
export function disableDevTools() {
  // Solo en producción
  if (import.meta.env.PROD) {
    
    // Deshabilitar click derecho
    document.addEventListener('contextmenu', (e) => {
      e.preventDefault()
      console.warn('⚠️ Click derecho deshabilitado')
      return false
    })

    // Deshabilitar F12, Ctrl+Shift+I, Ctrl+Shift+J, Ctrl+U
    document.addEventListener('keydown', (e) => {
      // F12
      if (e.keyCode === 123) {
        e.preventDefault()
        return false
      }
      
      // Ctrl+Shift+I (DevTools)
      if (e.ctrlKey && e.shiftKey && e.keyCode === 73) {
        e.preventDefault()
        return false
      }
      
      // Ctrl+Shift+J (Console)
      if (e.ctrlKey && e.shiftKey && e.keyCode === 74) {
        e.preventDefault()
        return false
      }
      
      // Ctrl+U (View Source)
      if (e.ctrlKey && e.keyCode === 85) {
        e.preventDefault()
        return false
      }
      
      // Ctrl+S (Save)
      if (e.ctrlKey && e.keyCode === 83) {
        e.preventDefault()
        return false
      }
    })

    // Detectar resize de ventana (posible apertura de DevTools)
    let devtoolsOpen = false
    const threshold = 160
    
    const detectDevTools = () => {
      const widthThreshold = window.outerWidth - window.innerWidth > threshold
      const heightThreshold = window.outerHeight - window.innerHeight > threshold
      
      if (widthThreshold || heightThreshold) {
        if (!devtoolsOpen) {
          devtoolsOpen = true
          console.warn('⚠️ DevTools detectadas')
          // Opcional: Registrar en backend o cerrar sesión
        }
      } else {
        devtoolsOpen = false
      }
    }
    
    setInterval(detectDevTools, 500)
  }
}
```

**Usar en:** `frontend/src/main.jsx`

```javascript
import { disableDevTools } from './utils/disableDevTools'

// Aplicar protecciones
disableDevTools()

// ... resto del código
```

#### 6. Scripts de Build

**Modificar:** `frontend/package.json`

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "build:prod": "vite build --mode production",
    "preview": "vite preview",
    "lint": "eslint .",
    "analyze": "vite build --mode production && vite-bundle-visualizer"
  }
}
```

---

## 🔐 PARTE 3: PROTECCIÓN EN EL BACKEND

### Headers de Seguridad

**Ya implementado en:** `backend/src/main/java/com/geros/backend/config/SecurityConfig.java`

```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("default-src 'self'; " +
            "script-src 'self' https://www.gstatic.com https://www.google.com 'unsafe-inline'; " +
            "frame-src https://www.google.com https://www.recaptcha.net; " +
            "connect-src 'self' https://www.google.com;"))
    .frameOptions(frame -> frame.deny())
    .xssProtection(xss -> xss.disable())
    .contentTypeOptions(Customizer.withDefaults())
);
```

### Agregar Headers Adicionales

```java
// Agregar en SecurityConfig.java
http.headers(headers -> headers
    // ... headers existentes ...
    
    // ✅ Prevenir caching de páginas sensibles
    .cacheControl(cache -> cache.disable())
    
    // ✅ Strict Transport Security
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000))
    
    // ✅ Referrer Policy
    .referrerPolicy(referrer -> referrer
        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
    
    // ✅ Permissions Policy
    .addHeaderWriter(new StaticHeadersWriter(
        "Permissions-Policy", 
        "geolocation=(), microphone=(), camera=()"
    ))
);
```

---

## 📊 Comparación: Antes vs Después

| Aspecto | Antes ❌ | Después ✅ |
|---------|---------|-----------|
| **Salida Segura** |
| Invalidación en servidor | ✅ | ✅ |
| Limpieza de localStorage | ✅ | ✅ |
| Limpieza de sessionStorage | ❌ | ✅ |
| Limpieza de cookies | ❌ | ✅ |
| Limpieza de caché | ❌ | ✅ |
| Prevenir botón "Atrás" | ❌ | ✅ |
| Limpiar historial | ❌ | ✅ |
| **Protección de Código** |
| Minificación | ✅ (básica) | ✅ (agresiva) |
| Ofuscación | ❌ | ✅ |
| Source maps en producción | ✅ (expuesto) | ❌ (eliminado) |
| Eliminación de console.log | ❌ | ✅ |
| Compresión gzip/brotli | ❌ | ✅ |
| Nombres de archivos ofuscados | ❌ | ✅ |
| CSP headers | ✅ (básico) | ✅ (completo) |
| Deshabilitar DevTools | ❌ | ✅ (opcional) |
| Deshabilitar click derecho | ❌ | ✅ (opcional) |
| Detección de DevTools | ❌ | ✅ (opcional) |

---

## ⚠️ ADVERTENCIAS IMPORTANTES

### 1. Ofuscación NO es Cifrado

- El código ofuscado puede ser revertido con tiempo y esfuerzo
- Es una capa de protección, no una solución definitiva
- La seguridad real debe estar en el backend

### 2. Deshabilitar DevTools Puede Afectar UX

- Usuarios avanzados pueden frustrarse
- Puede dificultar el soporte técnico
- Considerar solo para aplicaciones de alta seguridad

### 3. Balance entre Seguridad y Usabilidad

- No todas las medidas son apropiadas para todos los casos
- Evaluar el nivel de seguridad requerido
- Considerar el perfil de usuarios

---

## 🎯 Plan de Implementación

### Fase 1: Salida Segura (1-2 días)
- [ ] Mejorar función logout
- [ ] Implementar limpieza completa de storages
- [ ] Agregar prevención de botón "Atrás"
- [ ] Probar flujo completo de logout

### Fase 2: Ofuscación Básica (1 día)
- [ ] Configurar Terser en vite.config.js
- [ ] Deshabilitar source maps en producción
- [ ] Eliminar console.log en producción
- [ ] Probar build de producción

### Fase 3: Protección Avanzada (2-3 días)
- [ ] Instalar plugins de compresión
- [ ] Configurar CSP headers completos
- [ ] Implementar detección de DevTools (opcional)
- [ ] Deshabilitar click derecho (opcional)
- [ ] Probar en diferentes navegadores

### Fase 4: Testing y Validación (1-2 días)
- [ ] Probar logout en todos los escenarios
- [ ] Verificar ofuscación del código
- [ ] Validar headers de seguridad
- [ ] Pruebas de penetración básicas

---

## ✅ Checklist de Seguridad

### Salida Segura
- [ ] Logout invalida sesión en servidor
- [ ] localStorage limpiado completamente
- [ ] sessionStorage limpiado completamente
- [ ] Cookies eliminadas
- [ ] Caché del navegador limpiado
- [ ] Botón "Atrás" no permite volver
- [ ] Historial de navegación limpiado
- [ ] Mensaje de confirmación mostrado

### Protección de Código
- [ ] Código minificado en producción
- [ ] Código ofuscado en producción
- [ ] Source maps NO incluidos en producción
- [ ] console.log eliminados en producción
- [ ] Archivos comprimidos (gzip/brotli)
- [ ] Nombres de archivos ofuscados
- [ ] CSP headers configurados
- [ ] X-Frame-Options configurado
- [ ] X-Content-Type-Options configurado

### Opcional (Alta Seguridad)
- [ ] Click derecho deshabilitado
- [ ] Atajos de DevTools deshabilitados
- [ ] Detección de DevTools activa
- [ ] Registro de intentos de inspección

---

**Última actualización:** 2026-04-03  
**Estado:** 📝 Documentado - Pendiente de implementación  
**Prioridad:** ALTA
