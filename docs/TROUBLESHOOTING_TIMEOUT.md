# 🔧 Troubleshooting - Timeout de Sesión

## Problema: La sesión no se cierra automáticamente

### ✅ Cambios Aplicados

He corregido dos problemas:

1. **Modal no cerraba sesión al llegar a 0**: Ahora llama automáticamente a `onLogout()`
2. **Logs de depuración agregados**: Puedes ver exactamente qué está pasando

---

## 🧪 Cómo Probar Ahora

### Paso 1: Recargar el Frontend

El frontend se recarga automáticamente con Vite. Si no:
```bash
# En el navegador
Ctrl + Shift + R (recarga forzada)
```

### Paso 2: Abrir la Consola del Navegador

1. Presiona **F12**
2. Ve a la pestaña **Console**
3. Limpia la consola (ícono 🚫)

### Paso 3: Configurar Timeout Corto

1. Login: `admin` / `admin123`
2. Ir a **Política de Contraseñas**
3. Cambiar **"Timeout de sesión"** a **120** (2 minutos)
4. Click en **"Guardar cambios"**

Verás en la consola:
```
🔄 SessionTimeout: Reiniciando timer (120s)
⏱️ SessionTimeout: Modal aparecerá en 60s, logout en 120s
```

### Paso 4: NO TOCAR NADA

**IMPORTANTE:** No muevas el mouse, no hagas scroll, no toques el teclado.

Cada vez que haces algo, verás:
```
🔄 SessionTimeout: Reiniciando timer (120s)
```

Esto es CORRECTO - el timer se reinicia con actividad.

### Paso 5: Esperar 1 Minuto

Después de 60 segundos SIN ACTIVIDAD, verás:
```
⚠️ SessionTimeout: Mostrando modal de advertencia
⏱️ SessionWarningModal: Iniciando contador desde 60s
⏱️ SessionWarningModal: 59s restantes
⏱️ SessionWarningModal: 58s restantes
...
```

Y aparecerá el modal.

### Paso 6: Esperar 1 Minuto Más

Si NO haces click en ningún botón, verás:
```
⏱️ SessionWarningModal: 3s restantes
⏱️ SessionWarningModal: 2s restantes
⏱️ SessionWarningModal: 1s restantes
🔴 SessionWarningModal: Contador llegó a 0, llamando a onLogout
🔴 SessionTimeout: Cerrando sesión por inactividad
```

Y serás redirigido a `/login` con el mensaje de sesión expirada.

---

## 🐛 Problemas Comunes

### Problema 1: "El timer se reinicia constantemente"

**Causa:** Estás moviendo el mouse o haciendo scroll.

**Solución:** 
- Deja las manos quietas
- No toques el mouse
- No hagas scroll
- No presiones teclas

**Verificación:**
Si ves muchos logs de:
```
🔄 SessionTimeout: Reiniciando timer (120s)
```
Es porque estás generando actividad.

---

### Problema 2: "El modal no aparece"

**Posibles causas:**

#### A. sessionTimeoutSeconds no está configurado

Verifica en la consola:
```javascript
localStorage.getItem('sessionTimeoutSeconds')
```

Debería devolver: `"120"` (o el valor que configuraste)

Si devuelve `null`:
1. Cierra sesión
2. Inicia sesión nuevamente
3. El valor se carga del backend al hacer login

#### B. El componente no está montado

Verifica en React DevTools:
```
<AuthProvider>
  <BrowserRouter>
    <SessionTimeout>  ← Debe estar aquí
```

Si no está, revisa que `App.jsx` tenga:
```jsx
<SessionTimeout />
```

#### C. No hay usuario autenticado

Verifica:
```javascript
localStorage.getItem('token')
```

Debe devolver un token JWT largo.

---

### Problema 3: "El modal aparece pero no cierra la sesión"

**Causa:** Bug corregido en esta actualización.

**Solución:**
1. Recarga la página (Ctrl + Shift + R)
2. Verifica que veas los nuevos logs en consola
3. Espera a que el contador llegue a 0

**Verificación:**
Deberías ver:
```
🔴 SessionWarningModal: Contador llegó a 0, llamando a onLogout
🔴 SessionTimeout: Cerrando sesión por inactividad
```

---

### Problema 4: "Veo el mensaje pero no me redirige"

**Causa:** Posible error en la navegación.

**Verificación:**
Abre la consola y busca errores en rojo.

**Solución temporal:**
Haz click en el botón "Cerrar sesión" del modal.

---

## 🔍 Debugging Avanzado

### Ver Estado Completo

Abre la consola y ejecuta:
```javascript
// Ver configuración de timeout
console.log('Timeout:', localStorage.getItem('sessionTimeoutSeconds'))

// Ver si hay token
console.log('Token:', localStorage.getItem('token') ? 'Existe' : 'No existe')

// Ver usuario completo
console.log('User:', JSON.parse(localStorage.getItem('email')))
```

### Forzar Cierre de Sesión

Si quieres probar el cierre inmediato:
```javascript
// En la consola del navegador
window.dispatchEvent(new Event('beforeunload'))
```

### Ver Timers Activos

```javascript
// Esto no funciona directamente, pero puedes ver los logs
// Los timers se muestran en los logs de SessionTimeout
```

---

## 📊 Logs Esperados (Flujo Completo)

### 1. Al Iniciar Sesión
```
🔄 SessionTimeout: Reiniciando timer (120s)
⏱️ SessionTimeout: Modal aparecerá en 60s, logout en 120s
```

### 2. Al Mover el Mouse (durante los primeros 60s)
```
🔄 SessionTimeout: Reiniciando timer (120s)
⏱️ SessionTimeout: Modal aparecerá en 60s, logout en 120s
```

### 3. Después de 60s de Inactividad
```
⚠️ SessionTimeout: Mostrando modal de advertencia
⏱️ SessionWarningModal: Iniciando contador desde 60s
⏱️ SessionWarningModal: 59s restantes
⏱️ SessionWarningModal: 58s restantes
...
```

### 4. Si Haces Click en "Continuar Sesión"
```
✅ SessionTimeout: Usuario extendió la sesión
🔄 SessionTimeout: Reiniciando timer (120s)
⏱️ SessionTimeout: Modal aparecerá en 60s, logout en 120s
🧹 SessionWarningModal: Limpiando interval
```

### 5. Si NO Haces Nada (contador llega a 0)
```
⏱️ SessionWarningModal: 3s restantes
⏱️ SessionWarningModal: 2s restantes
⏱️ SessionWarningModal: 1s restantes
🔴 SessionWarningModal: Contador llegó a 0, llamando a onLogout
🔴 SessionTimeout: Cerrando sesión por inactividad
```

### 6. En la Página de Login
Deberías ver el mensaje:
```
ℹ️ Tu sesión ha expirado por inactividad. 
   Por favor, inicia sesión nuevamente.
```

---

## ✅ Checklist de Verificación

Antes de reportar un problema, verifica:

- [ ] Frontend está corriendo (http://localhost:5173)
- [ ] Backend está corriendo (https://localhost:8443)
- [ ] Estás autenticado (tienes token)
- [ ] sessionTimeoutSeconds está configurado
- [ ] Consola del navegador está abierta
- [ ] NO estás moviendo el mouse durante la prueba
- [ ] NO estás haciendo scroll durante la prueba
- [ ] NO estás presionando teclas durante la prueba
- [ ] Esperaste el tiempo completo (2 minutos en el ejemplo)
- [ ] Ves los logs en la consola

---

## 🎯 Prueba Definitiva

### Configuración Extrema (30 segundos)

Para probar rápidamente:

1. Ir a **Política de Contraseñas**
2. Cambiar timeout a **30** segundos
3. Guardar
4. **NO TOCAR NADA**
5. Esperar 15 segundos → Modal aparece
6. Esperar 15 segundos más → Sesión se cierra

**Logs esperados:**
```
🔄 SessionTimeout: Reiniciando timer (30s)
⏱️ SessionTimeout: Modal aparecerá en 15s, logout en 30s

[Después de 15s]
⚠️ SessionTimeout: Mostrando modal de advertencia
⏱️ SessionWarningModal: Iniciando contador desde 15s
⏱️ SessionWarningModal: 14s restantes
...
⏱️ SessionWarningModal: 1s restantes
🔴 SessionWarningModal: Contador llegó a 0, llamando a onLogout
🔴 SessionTimeout: Cerrando sesión por inactividad
```

---

## 🆘 Si Aún No Funciona

### Opción 1: Limpiar Todo

```javascript
// En la consola del navegador
localStorage.clear()
// Recargar página
location.reload()
// Iniciar sesión nuevamente
```

### Opción 2: Verificar Archivos

Asegúrate de que estos archivos tengan el código actualizado:

1. `frontend/src/components/SessionTimeout.jsx`
2. `frontend/src/components/SessionWarningModal.jsx`

### Opción 3: Reiniciar Frontend

```bash
# Detener Vite (Ctrl + C en la terminal)
# Iniciar nuevamente
cd frontend
npm run dev
```

### Opción 4: Verificar React DevTools

1. Instalar React DevTools (extensión de Chrome)
2. Abrir DevTools → Components
3. Buscar `SessionTimeout`
4. Ver props y state en tiempo real

---

## 📝 Reportar Problema

Si después de todo esto aún no funciona, proporciona:

1. **Logs completos de la consola** (copiar todo)
2. **Valor de sessionTimeoutSeconds** (`localStorage.getItem('sessionTimeoutSeconds')`)
3. **¿Aparece el modal?** Sí/No
4. **¿Qué pasa cuando llega a 0?** Descripción
5. **Errores en consola** (si hay)
6. **Navegador y versión** (Chrome 120, Firefox 121, etc.)

---

**Última actualización:** 2026-04-03  
**Versión:** 2.0 (con correcciones)
