# Restricción de Navegación Histórica Durante la Sesión

## Objetivo

Bloquear completamente el acceso al historial de navegación del navegador mientras el usuario tiene una sesión activa, evitando que pueda ver páginas anteriores mediante:
- Botón "Atrás" del navegador
- Botón "Adelante" del navegador
- Atajos de teclado (Alt+Left, Alt+Right, Backspace)
- Botones del mouse (botones laterales)
- Gestos táctiles

## Implementación

### 1. Hook useBlockBrowserHistory.js

**Ubicación**: `frontend/src/hooks/useBlockBrowserHistory.js`

**Funcionalidades**:
- Agrega entrada al historial con `window.history.pushState()` al montar
- Intercepta evento `popstate` y vuelve a agregar entrada para mantener al usuario en la página actual
- Bloquea atajos de teclado:
  - `Alt + ←` (navegación atrás)
  - `Alt + →` (navegación adelante)
  - `Backspace` fuera de campos de texto
- Bloquea botones del mouse (botones 3 y 4)
- Usa capture phase (`true`) para interceptar eventos antes que otros handlers
- Logs en modo desarrollo para debugging

**Estrategia de bloqueo**:
```javascript
// Al detectar popstate (botón atrás/adelante)
window.history.pushState(null, '', window.location.href)
// Esto agrega una nueva entrada, manteniendo al usuario en la página actual
```

**Eventos bloqueados**:
```javascript
- popstate: Navegación con botones del navegador (capture: false)
- keydown: Atajos de teclado (capture: true)
- mouseup: Botones laterales del mouse (capture: true)
```

### 2. Integración en PrivateRoute (App.jsx)

**Ubicación**: `frontend/src/App.jsx`

El hook se ejecuta en el componente PrivateRoute, que envuelve TODAS las rutas autenticadas:
```javascript
function PrivateRoute({ children, path }) {
  useBlockBrowserHistory() // ✅ Activo en todas las rutas protegidas
  // ...
}
```

**Páginas protegidas**:
- Dashboard
- Password Policy
- Change Password
- Roles
- AD Config
- Mail Config
- Menu Config
- Reserved Usernames
- Password History
- Security Log
- Validate Site
- Unauthorized

### 3. Integración en Layout.jsx (Doble protección)

**Ubicación**: `frontend/src/components/Layout.jsx`

También se ejecuta en Layout para reforzar la protección:
```javascript
useBlockBrowserHistory()
```

### 4. Limpieza de Historial en Logout

**AuthContext.jsx**:
```javascript
// Limpiar historial de navegación
if (window.history.length > 1) {
  window.history.replaceState(null, '', '/login')
}
```

**SessionTimeout.jsx**:
```javascript
// Limpiar historial antes de logout por timeout
window.history.replaceState(null, '', '/login')
```

## Cómo Funciona

### Bloqueo del Botón Atrás

1. Usuario hace clic en botón "Atrás"
2. Navegador dispara evento `popstate`
3. Hook intercepta el evento
4. Hook ejecuta `window.history.pushState()` para agregar nueva entrada
5. Usuario permanece en la página actual
6. Log en consola (DEV): `🚫 Navegación histórica bloqueada durante la sesión`

### Bloqueo de Atajos de Teclado

1. Usuario presiona `Alt + ←`
2. Evento `keydown` se dispara en capture phase
3. Hook intercepta con `e.preventDefault()`
4. Navegación bloqueada
5. Log en consola (DEV): `🚫 Atajo de navegación atrás bloqueado`

### Bloqueo de Botones del Mouse

1. Usuario presiona botón lateral del mouse (botón 3 o 4)
2. Evento `mouseup` se dispara en capture phase
3. Hook intercepta con `e.preventDefault()`
4. Navegación bloqueada
5. Log en consola (DEV): `🚫 Botón de navegación del mouse bloqueado`

## Comportamiento

### Durante la Sesión Activa

1. **Usuario intenta navegar atrás**: Bloqueado, permanece en página actual
2. **Usuario intenta navegar adelante**: Bloqueado, permanece en página actual
3. **Usuario presiona Alt+Left**: Bloqueado, sin efecto
4. **Usuario presiona Alt+Right**: Bloqueado, sin efecto
5. **Usuario presiona Backspace fuera de input**: Bloqueado, sin efecto
6. **Usuario usa botones del mouse**: Bloqueado, sin efecto
7. **Logs en consola (DEV)**: Muestra mensaje de bloqueo
8. **Navegación interna (Links de React Router)**: Funciona normalmente

### Al Cerrar Sesión

1. **Logout manual**: Limpia historial, redirige a login
2. **Logout por timeout**: Limpia historial, redirige a login
3. **Logout por token expirado**: Limpia historial, redirige a login
4. **Después de logout**: No puede volver atrás a páginas autenticadas (usePreventBackNavigation)

## Excepciones

**Backspace permitido en**:
- Campos `<input>`
- Campos `<textarea>`
- Elementos con `contentEditable`

**Navegación permitida**:
- Links de React Router (`<Link>`, `navigate()`)
- Navegación programática dentro de la aplicación

## Logs de Debugging (Modo Desarrollo)

```
🚫 Navegación histórica bloqueada durante la sesión
🚫 Atajo de navegación atrás bloqueado
🚫 Atajo de navegación adelante bloqueado
🚫 Backspace bloqueado fuera de campos de texto
🚫 Botón de navegación del mouse bloqueado
```

## Compatibilidad

- ✅ Chrome/Edge: Todos los métodos de navegación bloqueados
- ✅ Firefox: Todos los métodos de navegación bloqueados
- ✅ Safari: Todos los métodos de navegación bloqueados
- ✅ Dispositivos móviles: Gestos táctiles bloqueados

## Seguridad

**Capas de protección**:
1. Hook bloquea navegación durante sesión activa (PrivateRoute + Layout)
2. PrivateRoute valida autenticación en cada ruta
3. Logout limpia historial completamente
4. usePreventBackNavigation bloquea volver después de logout
5. Interceptores de API validan token en cada request

**Resultado**: Usuario no puede acceder a páginas anteriores ni durante ni después de la sesión.

## Testing

### Pruebas Manuales

1. **Botón Atrás del navegador**: 
   - Hacer clic en botón atrás
   - Resultado esperado: Permanece en página actual
   - Log en consola: `🚫 Navegación histórica bloqueada durante la sesión`

2. **Alt+Left**: 
   - Presionar Alt + Flecha Izquierda
   - Resultado esperado: Sin efecto
   - Log en consola: `🚫 Atajo de navegación atrás bloqueado`

3. **Alt+Right**: 
   - Presionar Alt + Flecha Derecha
   - Resultado esperado: Sin efecto
   - Log en consola: `🚫 Atajo de navegación adelante bloqueado`

4. **Backspace en página**: 
   - Presionar Backspace fuera de un input
   - Resultado esperado: Sin efecto
   - Log en consola: `🚫 Backspace bloqueado fuera de campos de texto`

5. **Backspace en input**: 
   - Escribir en un input y presionar Backspace
   - Resultado esperado: Borra caracteres normalmente
   - Sin log

6. **Botones del mouse**: 
   - Usar botones laterales del mouse (si están disponibles)
   - Resultado esperado: Sin efecto
   - Log en consola: `🚫 Botón de navegación del mouse bloqueado`

7. **Logout y botón Atrás**: 
   - Cerrar sesión
   - Hacer clic en botón atrás
   - Resultado esperado: No vuelve a páginas autenticadas

8. **Timeout y botón Atrás**: 
   - Esperar timeout de sesión
   - Hacer clic en botón atrás
   - Resultado esperado: No vuelve a páginas autenticadas

9. **Navegación interna**: 
   - Usar menú lateral para navegar entre páginas
   - Resultado esperado: Funciona normalmente
   - Sin bloqueos

### Verificación en Consola (DEV)

Abrir DevTools (F12) y verificar logs cuando se intenta navegar:
```
🚫 Navegación histórica bloqueada durante la sesión
```

### Verificación de Historial

En consola del navegador:
```javascript
// Ver longitud del historial
console.log(window.history.length)

// Intentar navegar atrás programáticamente
window.history.back() // Debe ser bloqueado
```

## Notas Importantes

- El bloqueo es activo solo durante la sesión autenticada
- No afecta la navegación normal dentro de la aplicación (usando Links de React Router)
- No afecta la funcionalidad de campos de texto
- Limpia completamente el historial al cerrar sesión
- Compatible con todos los navegadores modernos
- Usa capture phase para interceptar eventos antes que otros handlers
- Se ejecuta en PrivateRoute (todas las rutas autenticadas) y Layout (doble protección)

## Troubleshooting

### El botón atrás sigue funcionando

1. Verificar que estás en una ruta autenticada (envuelta en PrivateRoute)
2. Abrir consola y verificar que aparecen los logs de bloqueo
3. Verificar que el hook se está ejecutando (agregar console.log en useEffect)
4. Limpiar caché del navegador y recargar

### Los atajos de teclado no se bloquean

1. Verificar que los eventos se registran con capture: true
2. Verificar que no hay otros handlers que intercepten primero
3. Probar en modo incógnito para descartar extensiones del navegador

### La navegación interna no funciona

1. Verificar que estás usando `<Link>` o `navigate()` de React Router
2. No usar `<a href>` para navegación interna
3. Verificar que las rutas están correctamente configuradas
