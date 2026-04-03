# Plan de Implementación: Prevención de Robo de Sesión

## 🎯 Objetivo
Implementar controles avanzados para prevenir el robo de sesión (session hijacking) y garantizar que solo el usuario legítimo pueda usar su sesión.

## 📋 Estrategias a Implementar

### 1. Validación de IP del Cliente ⭐ PRIORIDAD ALTA

#### Descripción
Validar que la dirección IP del cliente no cambie durante la sesión activa.

#### Implementación

**Paso 1: Modificar JwtUtil para incluir IP**
```java
// backend/src/main/java/com/geros/backend/security/JwtUtil.java

public String generateToken(String email, String clientIp) {
    return Jwts.builder()
            .setSubject(email)
            .claim("ip", clientIp)
            .claim("iat", new Date())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
}

public String extractIp(String token) {
    return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .get("ip", String.class);
}
```

**Paso 2: Actualizar AuthService**
```java
// backend/src/main/java/com/geros/backend/security/AuthService.java

// En el método login(), cambiar:
String token = jwtUtil.generateToken(user.getEmail(), ip);
```

**Paso 3: Validar IP en JwtFilter**
```java
// backend/src/main/java/com/geros/backend/security/JwtFilter.java

@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }

    String token = authHeader.substring(7);

    if (!jwtUtil.isTokenValid(token)) {
        filterChain.doFilter(request, response);
        return;
    }

    // NUEVA VALIDACIÓN: Verificar IP
    String tokenIp = jwtUtil.extractIp(token);
    String requestIp = resolveClientIp(request);
    
    if (!tokenIp.equals(requestIp)) {
        // Registrar intento de robo de sesión
        String email = jwtUtil.extractEmail(token);
        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.SESSION_HIJACKING_ATTEMPT)
                .eventCode("SESSION_IP_MISMATCH")
                .origin("Seguridad de sesión")
                .target(email)
                .performedBy(email)
                .description("Intento de uso de sesión desde IP diferente")
                .oldValue("tokenIp=" + tokenIp)
                .newValue("requestIp=" + requestIp));
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\":\"Session hijacking detected\"}");
        return;
    }

    // Continuar con autenticación normal...
    String email = jwtUtil.extractEmail(token);
    // ... resto del código
}

private String resolveClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
        return forwardedFor.split(",")[0].trim();
    }
    
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
        return realIp.trim();
    }
    
    return request.getRemoteAddr();
}
```

**Paso 4: Agregar nueva acción al enum**
```java
// backend/src/main/java/com/geros/backend/securitylog/SecurityLog.java

public enum Action {
    // ... acciones existentes
    SESSION_HIJACKING_ATTEMPT,
    // ...
}
```

#### Consideraciones
- ⚠️ Puede causar problemas con usuarios que cambian de red (WiFi → 4G)
- 💡 Solución: Permitir cambio de IP pero requerir re-autenticación
- 💡 Alternativa: Validar solo cambios drásticos de geolocalización

---

### 2. Sistema de Refresh Tokens ⭐ PRIORIDAD ALTA

#### Descripción
Implementar tokens de corta duración (access tokens) y tokens de larga duración (refresh tokens) para renovar sesiones sin re-autenticación completa.

#### Implementación

**Paso 1: Crear entidad RefreshToken**
```java
// backend/src/main/java/com/geros/backend/security/RefreshToken.java

@Entity
@Table(name = "refresh_tokens", schema = "auth")
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    // Getters y setters
}
```

**Paso 2: Crear tabla en base de datos**
```sql
-- backend/src/main/resources/db/migration/V2__create_refresh_tokens.sql

CREATE TABLE IF NOT EXISTS auth.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_email VARCHAR(150) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address VARCHAR(100),
    user_agent TEXT,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_email) 
        REFERENCES auth.users(email) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_email ON auth.refresh_tokens(user_email);
CREATE INDEX idx_refresh_tokens_token ON auth.refresh_tokens(token);
```

**Paso 3: Crear servicio de Refresh Tokens**
```java
// backend/src/main/java/com/geros/backend/security/RefreshTokenService.java

@Service
public class RefreshTokenService {
    
    private final RefreshTokenRepository repository;
    private final JwtUtil jwtUtil;
    
    @Value("${jwt.refresh.expiration:604800000}") // 7 días
    private long refreshExpiration;
    
    public RefreshToken createRefreshToken(String email, String ip, String userAgent) {
        // Revocar tokens anteriores del mismo usuario
        repository.findByUserEmailAndRevokedFalse(email)
                .forEach(token -> {
                    token.setRevoked(true);
                    repository.save(token);
                });
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUserEmail(email);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000));
        refreshToken.setIpAddress(ip);
        refreshToken.setUserAgent(userAgent);
        
        return repository.save(refreshToken);
    }
    
    public Optional<RefreshToken> findByToken(String token) {
        return repository.findByTokenAndRevokedFalse(token)
                .filter(rt -> rt.getExpiresAt().isAfter(LocalDateTime.now()));
    }
    
    public void revokeToken(String token) {
        repository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            repository.save(rt);
        });
    }
}
```

**Paso 4: Actualizar AuthService**
```java
// Modificar método login() para devolver refresh token

public AuthDTO.LoginResponse login(AuthDTO.LoginRequest request) {
    // ... código existente ...
    
    String accessToken = jwtUtil.generateToken(user.getEmail(), ip);
    RefreshToken refreshToken = refreshTokenService.createRefreshToken(
        user.getEmail(), 
        ip, 
        request.getUserAgent()
    );
    
    return new AuthDTO.LoginResponse(
        accessToken,
        refreshToken.getToken(), // NUEVO
        user.getEmail(),
        roles,
        mustChange,
        // ... resto de campos
    );
}
```

**Paso 5: Crear endpoint de refresh**
```java
// backend/src/main/java/com/geros/backend/security/AuthController.java

@PostMapping("/refresh")
public ResponseEntity<AuthDTO.RefreshResponse> refresh(
        @RequestBody AuthDTO.RefreshRequest request,
        HttpServletRequest httpRequest) {
    
    RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    
    // Validar IP (opcional pero recomendado)
    String currentIp = resolveClientIp(httpRequest);
    if (!refreshToken.getIpAddress().equals(currentIp)) {
        securityLogService.log(/* ... log de intento sospechoso ... */);
        throw new RuntimeException("IP mismatch");
    }
    
    // Generar nuevo access token
    String newAccessToken = jwtUtil.generateToken(refreshToken.getUserEmail(), currentIp);
    
    return ResponseEntity.ok(new AuthDTO.RefreshResponse(newAccessToken));
}
```

**Paso 6: Frontend - Interceptor para renovar token**
```javascript
// frontend/src/api/auth.js

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers['Authorization'] = 'Bearer ' + token;
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem('refreshToken');
      
      if (!refreshToken) {
        return Promise.reject(error);
      }

      try {
        const { data } = await api.post('/auth/refresh', { refreshToken });
        localStorage.setItem('token', data.accessToken);
        api.defaults.headers.common['Authorization'] = 'Bearer ' + data.accessToken;
        processQueue(null, data.accessToken);
        return api(originalRequest);
      } catch (err) {
        processQueue(err, null);
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);
```

#### Configuración Recomendada
- Access Token: 15 minutos
- Refresh Token: 7 días
- Revocar refresh tokens al logout
- Permitir solo 1 refresh token activo por usuario

---

### 3. Validación de User-Agent 🔸 PRIORIDAD MEDIA

#### Descripción
Validar que el navegador/dispositivo no cambie durante la sesión.

#### Implementación
Similar a validación de IP, pero con User-Agent:
```java
.claim("userAgent", userAgent)
```

#### Consideraciones
- Menos crítico que IP
- User-Agent puede cambiar con actualizaciones del navegador
- Útil como capa adicional de seguridad

---

### 4. Fingerprinting del Navegador 🔸 PRIORIDAD MEDIA

#### Descripción
Crear una huella digital única del navegador basada en características del dispositivo.

#### Implementación Frontend
```javascript
// frontend/src/utils/fingerprint.js

export async function generateFingerprint() {
  const components = {
    canvas: await getCanvasFingerprint(),
    webgl: await getWebGLFingerprint(),
    audio: await getAudioFingerprint(),
    fonts: await getFontsFingerprint(),
    screen: getScreenFingerprint(),
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    language: navigator.language,
    platform: navigator.platform,
    hardwareConcurrency: navigator.hardwareConcurrency,
    deviceMemory: navigator.deviceMemory,
    colorDepth: screen.colorDepth,
    pixelRatio: window.devicePixelRatio
  };
  
  // Generar hash del fingerprint
  const fingerprintString = JSON.stringify(components);
  const hashBuffer = await crypto.subtle.digest('SHA-256', 
    new TextEncoder().encode(fingerprintString));
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

// Enviar en cada petición
api.interceptors.request.use(async (config) => {
  const fingerprint = await generateFingerprint();
  config.headers['X-Device-Fingerprint'] = fingerprint;
  return config;
});
```

#### Implementación Backend
```java
// Validar fingerprint en JwtFilter
String tokenFingerprint = jwtUtil.extractFingerprint(token);
String requestFingerprint = request.getHeader("X-Device-Fingerprint");

if (!tokenFingerprint.equals(requestFingerprint)) {
    // Log y rechazar
}
```

---

## 🔄 Flujo Completo de Seguridad de Sesión

```
1. Usuario hace login
   ↓
2. Backend genera:
   - Access Token (15 min) con: email, IP, userAgent, fingerprint
   - Refresh Token (7 días) almacenado en BD
   ↓
3. Frontend almacena ambos tokens
   ↓
4. Cada petición incluye:
   - Authorization: Bearer <accessToken>
   - X-Device-Fingerprint: <fingerprint>
   ↓
5. Backend valida en cada petición:
   - Token válido y no expirado
   - IP coincide con token
   - User-Agent coincide con token
   - Fingerprint coincide con token
   ↓
6. Si access token expira:
   - Frontend usa refresh token automáticamente
   - Backend valida refresh token
   - Genera nuevo access token
   ↓
7. Si refresh token expira o es inválido:
   - Logout automático
   - Redirigir a login
```

---

## 📊 Matriz de Riesgos vs Controles

| Riesgo | Control | Prioridad | Estado |
|--------|---------|-----------|--------|
| Token robado por XSS | HttpOnly cookies | ALTA | ⏳ Pendiente |
| Token robado por MITM | HTTPS obligatorio | ALTA | ✅ Implementado |
| Replay attack | Validación de IP | ALTA | ⏳ Pendiente |
| Session fixation | Regenerar token en login | MEDIA | ✅ Implementado |
| Brute force | Rate limiting | ALTA | ⏳ Pendiente |
| Token de larga duración | Refresh tokens | ALTA | ⏳ Pendiente |
| Cambio de dispositivo | Fingerprinting | MEDIA | ⏳ Pendiente |

---

## 🧪 Plan de Pruebas

### Prueba 1: Validación de IP
1. Login desde IP A
2. Intentar usar token desde IP B
3. Verificar rechazo y log de seguridad

### Prueba 2: Refresh Token
1. Login y obtener tokens
2. Esperar expiración de access token
3. Verificar renovación automática
4. Verificar que refresh token se revoca al logout

### Prueba 3: Fingerprinting
1. Login desde navegador A
2. Copiar token a navegador B
3. Verificar rechazo por fingerprint diferente

---

## 📝 Checklist de Implementación

### Fase 1: Validación de IP (1 semana)
- [ ] Modificar JwtUtil para incluir IP
- [ ] Actualizar AuthService
- [ ] Implementar validación en JwtFilter
- [ ] Agregar logs de seguridad
- [ ] Pruebas unitarias
- [ ] Pruebas de integración
- [ ] Documentación

### Fase 2: Refresh Tokens (1-2 semanas)
- [ ] Crear entidad RefreshToken
- [ ] Crear tabla en BD
- [ ] Implementar RefreshTokenService
- [ ] Crear endpoint /auth/refresh
- [ ] Actualizar frontend con interceptor
- [ ] Pruebas de renovación automática
- [ ] Pruebas de revocación
- [ ] Documentación

### Fase 3: Fingerprinting (1 semana)
- [ ] Implementar generación de fingerprint en frontend
- [ ] Incluir fingerprint en tokens
- [ ] Validar fingerprint en backend
- [ ] Pruebas cross-browser
- [ ] Documentación

---

## 🚀 Despliegue

### Configuración Recomendada para Producción
```properties
# application.properties

# Access token: 15 minutos
jwt.expiration=900000

# Refresh token: 7 días
jwt.refresh.expiration=604800000

# Validaciones estrictas
app.security.validate-ip=true
app.security.validate-user-agent=true
app.security.validate-fingerprint=true

# Rate limiting
app.security.rate-limit.enabled=true
app.security.rate-limit.requests-per-minute=60
```

---

**Última actualización:** 2026-04-03
**Autor:** Sistema GEROS
**Versión:** 1.0
