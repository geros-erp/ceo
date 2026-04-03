# Protección de Transacciones sin Autenticación

## Implementación Completa

### Backend

#### 1. SecurityConfig.java
**Endpoints públicos restringidos**:
- `/api/auth/login` - Permitido
- `/api/auth/forgot-password` - Permitido
- `/api/auth/reset-password` - Permitido
- **Todos los demás endpoints requieren autenticación JWT válida**

#### 2. JwtFilter.java
**Validación estricta de tokens**:
- Rechaza requests con token inválido o expirado con HTTP 401
- Retorna JSON: `{"error":"Token inválido o expirado"}`
- No permite continuar sin autenticación válida

#### 3. AuthenticationValidator.java (Nuevo)
**Componente centralizado de validación**:
- `requireAuthentication()`: Valida que existe autenticación
- `getAuthenticatedUser()`: Obtiene email del usuario autenticado
- Lanza `ResponseStatusException` con HTTP 401 si no hay autenticación

#### 4. Controladores Protegidos
**AuthController**:
- `/api/auth/change-password`: Requiere autenticación, usa AuthenticationValidator
- `/api/auth/logout`: Permite logout sin autenticación (limpieza de sesión)

**MenuController**:
- `/api/menu/my-menu`: Requiere autenticación, usa AuthenticationValidator
- Todos los endpoints CRUD: Protegidos con @PreAuthorize

**Otros Controladores**:
- UserController: @PreAuthorize a nivel de clase y métodos
- RoleController: @PreAuthorize a nivel de clase y métodos
- PasswordPolicyController: @PreAuthorize a nivel de clase y métodos
- SecurityLogController: @PreAuthorize a nivel de clase y métodos

### Frontend

#### auth.js - Request Interceptor
**Validación antes de cada request**:
- Verifica existencia de token en localStorage
- Excepciones: login, forgot-password, reset-password
- Si no hay token: bloquea request, redirige a /login, log de error
- Agrega token JWT en header Authorization

#### auth.js - Response Interceptor
**Manejo de errores de autenticación**:
- Detecta HTTP 401 (token inválido/expirado)
- Limpia localStorage y sessionStorage
- Redirige automáticamente a /login
- Log de error en consola

#### App.jsx - PrivateRoute
**Protección de rutas frontend**:
- Valida existencia de usuario en contexto
- Valida permisos de acceso por ruta
- Redirige a /login si no hay usuario
- Redirige a ruta autorizada si no tiene permisos

## Flujo de Protección

### Caso 1: Request sin token
1. Frontend interceptor detecta ausencia de token
2. Bloquea request antes de enviarlo
3. Redirige a /login
4. Log: `❌ Intento de transacción sin autenticación: [url]`

### Caso 2: Request con token expirado
1. Frontend envía request con token
2. Backend JwtFilter valida token
3. Token expirado → Backend retorna 401
4. Frontend interceptor detecta 401
5. Limpia sesión y redirige a /login
6. Log: `❌ Token inválido o expirado, redirigiendo a login`

### Caso 3: Request con token inválido
1. Frontend envía request con token
2. Backend JwtFilter valida token
3. Token inválido → Backend retorna 401 con JSON error
4. Frontend interceptor detecta 401
5. Limpia sesión y redirige a /login

### Caso 4: Endpoint que requiere autenticación sin token
1. Frontend bloquea en interceptor (Caso 1)
2. Si pasa frontend, backend SecurityConfig rechaza (no tiene token)
3. Backend retorna 401 o 403

### Caso 5: Endpoint protegido con @PreAuthorize
1. Token válido pero sin permisos
2. Backend AccessControlService valida permisos
3. Si no tiene permisos → Backend retorna 403 Forbidden
4. Frontend muestra mensaje de acceso denegado

## Capas de Seguridad

1. **Frontend - Interceptor Request**: Bloquea transacciones sin token
2. **Frontend - PrivateRoute**: Protege rutas de navegación
3. **Backend - SecurityConfig**: Define endpoints públicos/protegidos
4. **Backend - JwtFilter**: Valida tokens JWT
5. **Backend - @PreAuthorize**: Valida permisos específicos
6. **Backend - AuthenticationValidator**: Valida autenticación en métodos
7. **Frontend - Interceptor Response**: Maneja errores 401 y limpia sesión

## Endpoints Públicos (sin autenticación)

- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

## Endpoints Protegidos (requieren autenticación)

- `POST /api/auth/change-password` - Requiere token JWT válido
- `POST /api/auth/logout` - Permite sin token (limpieza)
- `GET /api/menu/my-menu` - Requiere token JWT válido
- Todos los endpoints de `/api/users/**`
- Todos los endpoints de `/api/roles/**`
- Todos los endpoints de `/api/policy/**`
- Todos los endpoints de `/api/security-log/**`
- Todos los endpoints de `/api/menu/**` (excepto my-menu)
- Todos los endpoints de `/api/ad-config/**`
- Todos los endpoints de `/api/mail-config/**`

## Logs de Seguridad

**Frontend**:
- `❌ Intento de transacción sin autenticación: [url]`
- `❌ Token inválido o expirado, redirigiendo a login`
- `API Request: [METHOD] [URL]`
- `API Response: [STATUS] [URL]`
- `API Error: [STATUS] [DATA]`

**Backend**:
- JwtFilter retorna: `{"error":"Token inválido o expirado"}`
- AuthenticationValidator lanza: `ResponseStatusException(401, "Usuario no autenticado")`
- SecurityLog registra intentos de acceso no autorizado

## Pruebas de Seguridad

1. **Sin token**: Intentar acceder a endpoint protegido → Bloqueado en frontend
2. **Token expirado**: Esperar expiración y hacer request → 401 y redirect a login
3. **Token inválido**: Modificar token y hacer request → 401 y redirect a login
4. **Sin permisos**: Acceder a ruta sin permisos → 403 o redirect a ruta autorizada
5. **Manipular localStorage**: Borrar token y navegar → Redirect a login
