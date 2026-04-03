# Control de Timeout de Sesión por Inactividad

## Descripción
Sistema que controla automáticamente el tiempo de inactividad de una sesión de usuario y cierra la sesión cuando se excede el límite configurado.

## Características Implementadas

### 1. **Configuración Paramétrica**
- El timeout se configura en segundos desde la interfaz de administración
- Ruta: `/policy` → Sección "Sesión y autenticación" → "Timeout de sesión (segundos)"
- Rango permitido: 60 segundos (1 minuto) a 86400 segundos (24 horas)
- Valor por defecto: 1800 segundos (30 minutos)

### 2. **Detección de Actividad del Usuario**
El sistema detecta actividad del usuario mediante los siguientes eventos:
- `mousedown` - Click del mouse
- `mousemove` - Movimiento del mouse
- `keypress` - Pulsación de teclas
- `scroll` - Desplazamiento de página
- `touchstart` - Toque en pantalla táctil
- `click` - Click en elementos

### 3. **Advertencia Antes de Expirar**
- 60 segundos antes de que expire la sesión, se muestra un modal de advertencia
- El modal muestra un contador regresivo en tiempo real
- El usuario puede:
  - **Continuar sesión**: Reinicia el contador de inactividad
  - **Cerrar sesión**: Cierra la sesión inmediatamente

### 4. **Cierre Automático de Sesión**
- Si el usuario no interactúa durante el tiempo configurado, la sesión se cierra automáticamente
- El usuario es redirigido a la página de login
- Se muestra un mensaje informativo: "Tu sesión ha expirado por inactividad. Por favor, inicia sesión nuevamente."

## Archivos Modificados/Creados

### Frontend
1. **`frontend/src/components/SessionTimeout.jsx`** (NUEVO)
   - Componente principal que controla el timeout
   - Detecta actividad del usuario
   - Gestiona los timers de advertencia y cierre

2. **`frontend/src/components/SessionWarningModal.jsx`** (NUEVO)
   - Modal de advertencia visual
   - Contador regresivo
   - Botones de acción (Continuar/Cerrar)

3. **`frontend/src/App.jsx`** (MODIFICADO)
   - Integración del componente SessionTimeout
   - Activo en todas las rutas protegidas

4. **`frontend/src/pages/Login.jsx`** (MODIFICADO)
   - Soporte para mostrar mensajes informativos
   - Muestra el mensaje de sesión expirada

5. **`frontend/src/pages/PasswordPolicy.jsx`** (MODIFICADO)
   - Campo de configuración para sessionTimeoutSeconds
   - Validación de rango (60-86400 segundos)

6. **`frontend/src/context/AuthContext.jsx`** (YA EXISTÍA)
   - Ya almacenaba sessionTimeoutSeconds del backend
   - No requirió modificaciones adicionales

### Backend
7. **`backend/src/main/resources/application.properties`** (MODIFICADO)
   - Aumentado límite de sesiones concurrentes de 1 a 5
   - `app.auth.max-concurrent-sessions=5`

## Flujo de Funcionamiento

```
1. Usuario inicia sesión
   ↓
2. Backend envía sessionTimeoutSeconds (ej: 1800)
   ↓
3. Frontend inicia timer de inactividad
   ↓
4. Usuario realiza actividad → Timer se reinicia
   ↓
5. Usuario inactivo por (timeout - 60) segundos
   ↓
6. Se muestra modal de advertencia con contador
   ↓
7a. Usuario hace click en "Continuar" → Timer se reinicia
   ↓
7b. Usuario no interactúa por 60 segundos más
   ↓
8. Sesión se cierra automáticamente
   ↓
9. Redirección a /login con mensaje informativo
```

## Configuración Recomendada

### Entornos de Desarrollo
- **Timeout:** 3600 segundos (1 hora)
- Permite trabajar sin interrupciones frecuentes

### Entornos de Producción
- **Timeout:** 1800 segundos (30 minutos)
- Balance entre seguridad y usabilidad

### Entornos de Alta Seguridad
- **Timeout:** 300-600 segundos (5-10 minutos)
- Máxima seguridad para datos sensibles

## Pruebas

### Prueba 1: Configuración del Timeout
1. Iniciar sesión como administrador
2. Ir a `/policy`
3. Modificar "Timeout de sesión (segundos)" a 120 (2 minutos)
4. Guardar cambios
5. Cerrar sesión e iniciar sesión nuevamente
6. Esperar 1 minuto sin interactuar
7. Verificar que aparece el modal de advertencia
8. Esperar 1 minuto más
9. Verificar que la sesión se cierra automáticamente

### Prueba 2: Reinicio del Timer por Actividad
1. Configurar timeout a 120 segundos
2. Iniciar sesión
3. Esperar 1 minuto
4. Mover el mouse o hacer click
5. Esperar 1 minuto más
6. Verificar que NO aparece el modal (el timer se reinició)

### Prueba 3: Extensión Manual de Sesión
1. Configurar timeout a 120 segundos
2. Iniciar sesión
3. Esperar 1 minuto (aparece modal)
4. Hacer click en "Continuar sesión"
5. Verificar que el modal se cierra
6. Verificar que el timer se reinicia

## Notas Técnicas

### Persistencia del Timeout
- El valor de `sessionTimeoutSeconds` se almacena en localStorage
- Se carga automáticamente al refrescar la página
- Se actualiza al iniciar sesión

### Limpieza de Recursos
- Los timers se limpian correctamente al cerrar sesión
- Los event listeners se remueven al desmontar el componente
- No hay memory leaks

### Compatibilidad
- Compatible con todos los navegadores modernos
- Funciona en dispositivos móviles (eventos touch)
- Responsive design del modal de advertencia

## Mejoras Futuras (Opcionales)

1. **Notificación de Escritorio**
   - Usar Notification API para alertar al usuario

2. **Sonido de Alerta**
   - Reproducir sonido cuando aparece el modal

3. **Configuración por Rol**
   - Diferentes timeouts según el rol del usuario

4. **Registro de Eventos**
   - Log de sesiones cerradas por inactividad en SecurityLog

5. **Modo "Recordarme"**
   - Opción para extender el timeout automáticamente
