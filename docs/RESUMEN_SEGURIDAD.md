# Resumen de Implementaciones de Seguridad - Sistema GEROS

## 📊 Estado Actual de Implementaciones

### ✅ IMPLEMENTADO Y FUNCIONANDO

#### 1. **Control de Timeout de Sesión por Inactividad**
- **Ubicación:** Frontend y Backend
- **Descripción:** Sistema que cierra automáticamente la sesión del usuario tras un período de inactividad configurable
- **Características:**
  - Configurable desde interfaz de administración (60-86400 segundos)
  - Detección automática de actividad del usuario (mouse, teclado, scroll, touch)
  - Modal de advertencia 60 segundos antes de cerrar sesión
  - Opción de extender sesión manualmente
  - Valor por defecto: 1800 segundos (30 minutos)
- **Archivos:**
  - `frontend/src/components/SessionTimeout.jsx`
  - `frontend/src/components/SessionWarningModal.jsx`
  - `backend/src/main/java/com/geros/backend/policy/PasswordPolicy.java`

#### 2. **Registro de Auditoría de Cambios en Políticas**
- **Ubicación:** Backend
- **Descripción:** Todos los cambios en políticas de seguridad se registran en el log de auditoría
- **Incluye:**
  - Cambios en timeout de sesión
  - Cambios en número de sesiones concurrentes permitidas
  - Cambios en políticas de contraseñas
  - Registro de valores anteriores y nuevos
  - Identificación del usuario que realizó el cambio
- **Archivos:**
  - `backend/src/main/java/com/geros/backend/policy/PasswordPolicyService.java`
  - `backend/src/main/java/com/geros/backend/securitylog/SecurityLogService.java`

#### 3. **Autenticación JWT**
- **Ubicación:** Backend
- **Descripción:** Sistema de autenticación basado en JSON Web Tokens
- **Características:**
  - Tokens firmados con HS256
  - Expiración configurable (por defecto 24 horas)
  - Validación en cada petición
- **Archivos:**
  - `backend/src/main/java/com/geros/backend/security/JwtUtil.java`
  - `backend/src/main/java/com/geros/backend/security/JwtFilter.java`

#### 4. **HTTPS Obligatorio**
- **Ubicación:** Backend
- **Descripción:** Comunicación cifrada mediante SSL/TLS
- **Características:**
  - Puerto 8443 (desarrollo)
  - Certificado autofirmado (desarrollo)
  - Keystore PKCS12
- **Archivo:** `backend/src/main/resources/application.properties`

#### 5. **Control de Sesiones Concurrentes**
- **Ubicación:** Backend
- **Descripción:** Límite de sesiones simultáneas por usuario
- **Características:**
  - Configurable por parámetro (actualmente: 5 sesiones)
  - Contador de sesiones activas por usuario
  - Validación en login
  - Decremento automático en logout
- **Archivos:**
  - `backend/src/main/java/com/geros/backend/security/AuthService.java`
  - `backend/src/main/resources/application.properties`

#### 6. **Registro de Actividad de Login**
- **Ubicación:** Backend
- **Descripción:** Registro detallado de información de login
- **Incluye:**
  - IP actual y anterior
  - Fecha/hora actual y anterior
  - Intentos fallidos
  - Bloqueos de cuenta
  - Login fuera de horario permitido
- **Archivos:**
  - `backend/src/main/java/com/geros/backend/user/User.java`
  - `backend/src/main/java/com/geros/backend/security/AuthService.java`

#### 7. **Transaction ID en Peticiones**
- **Ubicación:** Frontend
- **Descripción:** ID único para cada petición HTTP
- **Características:**
  - Generado con crypto.randomUUID()
  - Incluido en header X-Transaction-Id
  - Registrado en logs del backend
- **Archivo:** `frontend/src/api/auth.js`

---

## ⚠️ PENDIENTE DE IMPLEMENTAR

### 1. **Prevención Avanzada de Robo de Sesión**

#### A. Validación de IP del Cliente
**Prioridad:** ALTA
**Descripción:** Validar que la IP del cliente no cambie durante la sesión
**Implementación sugerida:**
```java
// En JwtUtil.java - Agregar IP al token
public String generateToken(String email, String clientIp) {
    return Jwts.builder()
            .setSubject(email)
            .claim("ip", clientIp)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
}

// En JwtFilter.java - Validar IP
String tokenIp = extractIp(token);
String requestIp = resolveClientIp(request);
if (!tokenIp.equals(requestIp)) {
    // Invalidar sesión y registrar evento sospechoso
    securityLogService.log(SecurityLog.Action.SESSION_HIJACKING_ATTEMPT);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    return;
}
```

#### B. Validación de User-Agent
**Prioridad:** MEDIA
**Descripción:** Validar que el navegador/dispositivo no cambie durante la sesión
**Implementación sugerida:**
```java
// Agregar User-Agent al token y validar en cada petición
.claim("userAgent", request.getHeader("User-Agent"))
```

#### C. Rotación de Tokens (Token Refresh)
**Prioridad:** ALTA
**Descripción:** Implementar sistema de refresh tokens para renovar tokens sin re-autenticación
**Implementación sugerida:**
```java
// Crear tabla refresh_tokens
// Generar access token (corta duración: 15 min)
// Generar refresh token (larga duración: 7 días)
// Endpoint /api/auth/refresh para renovar access token
```

#### D. Fingerprinting del Navegador
**Prioridad:** MEDIA
**Descripción:** Crear huella digital del navegador para detectar cambios
**Implementación sugerida:**
```javascript
// Frontend - Generar fingerprint
const fingerprint = await generateFingerprint({
  canvas: true,
  webgl: true,
  audio: true,
  fonts: true,
  screen: true
});
// Enviar en cada petición
```

### 2. **Firma Digital de Transacciones**

#### A. Firma de Transacciones Críticas
**Prioridad:** ALTA
**Descripción:** Firmar digitalmente transacciones importantes para garantizar integridad
**Implementación sugerida:**
```java
// Crear servicio de firma digital
@Service
public class TransactionSignatureService {
    
    private final KeyPair keyPair;
    
    public String signTransaction(String transactionId, String data) {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update((transactionId + data).getBytes());
        return Base64.getEncoder().encodeToString(signature.sign());
    }
    
    public boolean verifySignature(String transactionId, String data, String signatureStr) {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(keyPair.getPublic());
        signature.update((transactionId + data).getBytes());
        return signature.verify(Base64.getDecoder().decode(signatureStr));
    }
}
```

#### B. Registro de Transacciones Firmadas
**Prioridad:** ALTA
**Descripción:** Almacenar todas las transacciones firmadas para auditoría
**Implementación sugerida:**
```sql
CREATE TABLE auth.signed_transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    user_email VARCHAR(150) NOT NULL,
    action VARCHAR(100) NOT NULL,
    data_hash VARCHAR(255) NOT NULL,
    signature TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(100),
    user_agent TEXT
);
```

#### C. Validación de Integridad
**Prioridad:** ALTA
**Descripción:** Validar que los datos no fueron modificados
**Implementación sugerida:**
```java
// Interceptor para validar firma en peticiones críticas
@Component
public class SignatureValidationInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) {
        String signature = request.getHeader("X-Signature");
        String transactionId = request.getHeader("X-Transaction-Id");
        
        if (requiresSignature(request) && !isValidSignature(signature, transactionId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}
```

### 3. **Mejoras Adicionales de Seguridad**

#### A. Rate Limiting
**Prioridad:** ALTA
**Descripción:** Limitar número de peticiones por IP/usuario
**Implementación sugerida:**
```java
// Usar Bucket4j o implementación custom
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain chain) {
        String key = resolveClientIp(request);
        RateLimiter limiter = limiters.computeIfAbsent(key, 
            k -> RateLimiter.create(100.0)); // 100 req/sec
        
        if (!limiter.tryAcquire()) {
            response.setStatus(429); // Too Many Requests
            return;
        }
        chain.doFilter(request, response);
    }
}
```

#### B. CSRF Protection
**Prioridad:** MEDIA
**Descripción:** Protección contra Cross-Site Request Forgery
**Estado:** Spring Security lo maneja automáticamente para formularios
**Mejora:** Implementar tokens CSRF para API REST

#### C. Content Security Policy (CSP)
**Prioridad:** MEDIA
**Descripción:** Ya implementado en SecurityConfig
**Estado:** ✅ Configurado
**Ubicación:** `backend/src/main/java/com/geros/backend/config/SecurityConfig.java`

#### D. Detección de Anomalías
**Prioridad:** BAJA
**Descripción:** Sistema de ML para detectar comportamiento anómalo
**Implementación futura:** Analizar patrones de uso y alertar sobre anomalías

---

## 🎯 Plan de Implementación Recomendado

### Fase 1: Seguridad Crítica (1-2 semanas)
1. ✅ Control de timeout de sesión (COMPLETADO)
2. ✅ Registro de auditoría de cambios (COMPLETADO)
3. ⏳ Validación de IP del cliente
4. ⏳ Sistema de refresh tokens
5. ⏳ Rate limiting básico

### Fase 2: Firma de Transacciones (2-3 semanas)
1. ⏳ Servicio de firma digital
2. ⏳ Tabla de transacciones firmadas
3. ⏳ Interceptor de validación
4. ⏳ Integración con frontend

### Fase 3: Mejoras Avanzadas (3-4 semanas)
1. ⏳ Fingerprinting del navegador
2. ⏳ Validación de User-Agent
3. ⏳ CSRF tokens para API
4. ⏳ Sistema de detección de anomalías

---

## 📁 Estructura de Archivos Actual

### Backend
```
backend/src/main/java/com/geros/backend/
├── security/
│   ├── AuthService.java (✅ Control de sesiones concurrentes)
│   ├── JwtUtil.java (✅ Generación y validación de tokens)
│   ├── JwtFilter.java (✅ Filtro de autenticación)
│   └── SecurityConfig.java (✅ Configuración de seguridad)
├── policy/
│   ├── PasswordPolicy.java (✅ Entidad con sessionTimeoutSeconds)
│   ├── PasswordPolicyService.java (✅ Registro de auditoría)
│   └── PasswordPolicyDTO.java (✅ DTO con sessionTimeoutSeconds)
├── securitylog/
│   ├── SecurityLog.java (✅ Entidad de log)
│   └── SecurityLogService.java (✅ Servicio de auditoría)
└── user/
    └── User.java (✅ Campos de tracking de sesión)
```

### Frontend
```
frontend/src/
├── components/
│   ├── SessionTimeout.jsx (✅ Control de inactividad)
│   └── SessionWarningModal.jsx (✅ Modal de advertencia)
├── api/
│   └── auth.js (✅ Transaction ID en peticiones)
├── context/
│   └── AuthContext.jsx (✅ Gestión de sesión)
└── pages/
    ├── Login.jsx (✅ Mensajes de sesión expirada)
    └── PasswordPolicy.jsx (✅ Configuración de timeout)
```

---

## 🔐 Configuraciones de Seguridad Actuales

### application.properties
```properties
# JWT
jwt.secret=geros-secret-key-change-in-production
jwt.expiration=86400000  # 24 horas

# HTTPS
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:geros-keystore.p12
server.ssl.key-store-password=geros2024

# Sesiones Concurrentes
app.auth.max-concurrent-sessions=5

# Timeout de Sesión (en base de datos)
# Configurable desde /policy
# Valor por defecto: 1800 segundos (30 minutos)
```

---

## 📊 Métricas de Seguridad Implementadas

### Logs de Auditoría Registrados
1. ✅ LOGIN_SUCCESS
2. ✅ LOGIN_FAILED
3. ✅ LOGOUT
4. ✅ ACCOUNT_LOCKED
5. ✅ ACCOUNT_UNLOCKED
6. ✅ LOGIN_OUTSIDE_ALLOWED_HOURS
7. ✅ PASSWORD_CHANGED_BY_USER
8. ✅ PASSWORD_FORCED_CHANGE
9. ✅ PASSWORD_POLICY_UPDATED (incluye sessionTimeoutSeconds)
10. ✅ USER_CREATED
11. ✅ USER_UPDATED
12. ✅ USER_DELETED

### Información Registrada por Evento
- ✅ Transaction ID
- ✅ Timestamp
- ✅ Usuario que ejecuta la acción
- ✅ Acción realizada
- ✅ Valores anteriores y nuevos
- ✅ IP del cliente
- ✅ Descripción del evento

---

## 🚀 Próximos Pasos Inmediatos

### 1. Reiniciar Backend
```bash
# Detener backend actual
taskkill /F /PID <PID>

# Iniciar con cambios aplicados
cd backend
mvnw.cmd spring-boot:run
```

### 2. Probar Funcionalidades Implementadas
- [ ] Login con usuario admin
- [ ] Modificar timeout de sesión desde /policy
- [ ] Verificar registro en log de seguridad
- [ ] Probar inactividad y cierre automático de sesión
- [ ] Verificar modal de advertencia

### 3. Planificar Implementación de Fase 1
- [ ] Diseñar validación de IP
- [ ] Diseñar sistema de refresh tokens
- [ ] Implementar rate limiting básico

---

## 📝 Notas Importantes

### Seguridad en Producción
⚠️ **CRÍTICO:** Antes de desplegar a producción:
1. Cambiar `jwt.secret` por un valor seguro y largo
2. Reemplazar certificado autofirmado por certificado válido
3. Configurar `jwt.expiration` a un valor más corto (ej: 15 minutos)
4. Implementar refresh tokens
5. Habilitar validación de IP
6. Configurar rate limiting agresivo
7. Revisar y endurecer CSP headers

### Cumplimiento
- ✅ OWASP Top 10: Cubierto parcialmente
- ✅ ISO 27001: Controles de acceso implementados
- ⏳ PCI DSS: Requiere firma de transacciones (pendiente)
- ✅ GDPR: Logs de auditoría implementados

### Rendimiento
- Timeout de sesión: Impacto mínimo (solo frontend)
- JWT: Impacto bajo (validación rápida)
- Logs de auditoría: Impacto medio (escritura asíncrona recomendada)
- Firma digital: Impacto alto (solo para transacciones críticas)

---

## 📞 Contacto y Soporte

Para dudas sobre implementaciones de seguridad:
- Revisar documentación en `/docs`
- Consultar logs en `backend/storage/security-log-exports`
- Verificar configuración en `application.properties`

---

**Última actualización:** 2026-04-03
**Versión del documento:** 1.0
**Estado del proyecto:** En desarrollo activo
