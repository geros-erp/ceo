# Guía de Uso - Servicios de Seguridad

## Para Desarrolladores

Esta guía explica cómo usar los servicios de seguridad implementados en el sistema.

---

## 1. InputValidationService

### Validar Email

```java
@Autowired
private InputValidationService inputValidation;

public void processEmail(String email) {
    // Validar y sanitizar email
    String safeEmail = inputValidation.validateAndSanitizeEmail(email);
    // Usar safeEmail...
}
```

### Validar Username

```java
public void processUsername(String username) {
    // Validar y sanitizar username
    String safeUsername = inputValidation.validateAndSanitizeUsername(username);
    // Usar safeUsername...
}
```

### Validar Texto General

```java
public void processText(String text) {
    // Validar entrada contra inyecciones
    inputValidation.validateInput(text, "descripción");
    
    // Validar, sanitizar y verificar longitud
    String safeText = inputValidation.validateAndSanitizeText(text, "descripción", 500);
    // Usar safeText...
}
```

### Detectar Ataques

```java
public boolean isSafe(String input) {
    // Detectar SQL injection
    if (inputValidation.containsSqlInjection(input)) {
        throw new SecurityException("SQL injection detectado");
    }
    
    // Detectar XSS
    if (inputValidation.containsXss(input)) {
        throw new SecurityException("XSS detectado");
    }
    
    return true;
}
```

### Sanitizar Entrada

```java
public String cleanInput(String input) {
    // Sanitizar removiendo caracteres peligrosos
    return inputValidation.sanitize(input);
}
```

---

## 2. OutputSanitizationService

### Enmascarar Datos Sensibles

```java
@Autowired
private OutputSanitizationService outputSanitization;

public void logUserAction(String email, String ip) {
    // Enmascarar email para logs
    String maskedEmail = outputSanitization.maskEmail(email);
    
    // Enmascarar IP parcialmente
    String maskedIp = outputSanitization.maskIpPartial(ip);
    
    log.info("Usuario {} desde IP {}", maskedEmail, maskedIp);
}
```

### Sanitizar Errores

```java
public ResponseEntity<String> handleError(Exception ex) {
    // Sanitizar mensaje de error
    String safeMessage = outputSanitization.sanitizeErrorMessage(ex.getMessage());
    
    return ResponseEntity.status(500).body(safeMessage);
}
```

### Sanitizar Logs

```java
public void logRequest(String requestData) {
    // Sanitizar datos para logs (enmascara emails, tokens, contraseñas)
    String safeLog = outputSanitization.sanitizeForLog(requestData);
    
    log.info("Request: {}", safeLog);
}
```

### Escapar HTML

```java
public String prepareHtmlOutput(String userInput) {
    // Escapar HTML para prevenir XSS
    return outputSanitization.escapeHtml(userInput);
}
```

---

## 3. DataIntegrityService

### Firmar Datos Críticos

```java
@Autowired
private DataIntegrityService dataIntegrity;

public void saveImportantData(String data) {
    // Firmar datos
    String signature = dataIntegrity.signData(data);
    
    // Guardar data y signature juntos
    repository.save(data, signature);
}
```

### Verificar Integridad

```java
public void processData(String data, String signature) {
    // Validar integridad
    dataIntegrity.validateDataIntegrity(data, signature);
    
    // Si llega aquí, los datos son íntegros
    // Procesar data...
}
```

### Generar Checksum

```java
public String generateChecksum(User user) {
    // Generar checksum de múltiples campos
    return dataIntegrity.generateChecksum(
        user.getId(),
        user.getEmail(),
        user.getRole(),
        user.getLastModified()
    );
}
```

### Verificar Checksum

```java
public boolean verifyUserIntegrity(User user, String expectedChecksum) {
    // Verificar checksum
    return dataIntegrity.verifyChecksum(
        expectedChecksum,
        user.getId(),
        user.getEmail(),
        user.getRole(),
        user.getLastModified()
    );
}
```

### Detectar Modificación

```java
public void checkTampering(String originalHash, String currentData) {
    // Verificar si los datos fueron modificados
    if (dataIntegrity.isDataTampered(originalHash, currentData)) {
        throw new SecurityException("Datos modificados detectados");
    }
}
```

---

## 4. AuthenticationValidator

### Validar Autenticación en Controladores

```java
@Autowired
private AuthenticationValidator authValidator;

@PostMapping("/sensitive-operation")
public ResponseEntity<?> sensitiveOperation(Authentication authentication) {
    // Validar autenticación (lanza SecurityException si falla)
    authValidator.requireAuthentication(authentication);
    
    // Procesar operación...
    return ResponseEntity.ok().build();
}
```

### Obtener Usuario Autenticado

```java
@GetMapping("/profile")
public ResponseEntity<UserProfile> getProfile(Authentication authentication) {
    // Obtener email del usuario autenticado
    String email = authValidator.getAuthenticatedUser(authentication);
    
    // Buscar perfil...
    UserProfile profile = userService.getProfile(email);
    return ResponseEntity.ok(profile);
}
```

---

## 5. Ejemplo Completo: Crear Usuario

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private InputValidationService inputValidation;
    
    @Autowired
    private OutputSanitizationService outputSanitization;
    
    @Autowired
    private DataIntegrityService dataIntegrity;
    
    @Autowired
    private AuthenticationValidator authValidator;
    
    @Autowired
    private UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> createUser(
            Authentication authentication,
            @RequestBody CreateUserRequest request) {
        
        // 1. VALIDAR AUTENTICACIÓN
        String adminEmail = authValidator.getAuthenticatedUser(authentication);
        
        // 2. VALIDAR Y SANITIZAR ENTRADA
        String safeEmail = inputValidation.validateAndSanitizeEmail(request.getEmail());
        String safeUsername = inputValidation.validateAndSanitizeUsername(request.getUsername());
        String safeName = inputValidation.validateAndSanitizeText(request.getName(), "nombre", 100);
        
        // 3. PROCESAR (crear usuario)
        User user = userService.create(safeEmail, safeUsername, safeName);
        
        // 4. GENERAR CHECKSUM PARA INTEGRIDAD
        String checksum = dataIntegrity.generateChecksum(
            user.getId(),
            user.getEmail(),
            user.getUsername()
        );
        user.setChecksum(checksum);
        userService.update(user);
        
        // 5. PREPARAR RESPUESTA SEGURA
        UserDTO response = UserDTO.from(user);
        
        // 6. LOG SANITIZADO
        String logMessage = String.format(
            "Usuario creado: %s por admin: %s",
            safeEmail,
            adminEmail
        );
        log.info(outputSanitization.sanitizeForLog(logMessage));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

---

## 6. Ejemplo Completo: Actualizar Configuración Crítica

```java
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private InputValidationService inputValidation;
    
    @Autowired
    private DataIntegrityService dataIntegrity;
    
    @Autowired
    private AuthenticationValidator authValidator;
    
    @Autowired
    private ConfigService configService;

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfigDTO> updateConfig(
            Authentication authentication,
            @RequestBody UpdateConfigRequest request) {
        
        // 1. VALIDAR AUTENTICACIÓN
        authValidator.requireAuthentication(authentication);
        
        // 2. VALIDAR FIRMA DE INTEGRIDAD
        dataIntegrity.validateDataIntegrity(
            request.getData(),
            request.getSignature()
        );
        
        // 3. VALIDAR Y SANITIZAR ENTRADA
        inputValidation.validateInput(request.getData(), "configuración");
        String safeData = inputValidation.sanitize(request.getData());
        
        // 4. PROCESAR
        Config config = configService.update(safeData);
        
        // 5. FIRMAR NUEVA CONFIGURACIÓN
        String newSignature = dataIntegrity.signData(config.getData());
        config.setSignature(newSignature);
        configService.save(config);
        
        // 6. RESPUESTA
        ConfigDTO response = ConfigDTO.from(config);
        return ResponseEntity.ok(response);
    }
}
```

---

## 7. Manejo de Errores Seguro

```java
@RestControllerAdvice
public class CustomExceptionHandler {

    @Autowired
    private OutputSanitizationService outputSanitization;

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException ex) {
        // Sanitizar mensaje de error
        String safeMessage = outputSanitization.sanitizeErrorMessage(ex.getMessage());
        
        // No exponer detalles técnicos
        ErrorResponse error = new ErrorResponse(
            "SECURITY_VIOLATION",
            safeMessage,
            HttpStatus.FORBIDDEN.value()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        // Log del error real (servidor)
        log.error("Error no manejado", ex);
        
        // Respuesta genérica al cliente (no exponer detalles)
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "Error interno del servidor",
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

## 8. Logging Seguro

```java
@Service
public class UserService {

    @Autowired
    private OutputSanitizationService outputSanitization;

    public void loginUser(String email, String ip) {
        // ❌ MAL - Expone datos sensibles
        log.info("Login: {} desde {}", email, ip);
        
        // ✅ BIEN - Enmascara datos sensibles
        String maskedEmail = outputSanitization.maskEmail(email);
        String maskedIp = outputSanitization.maskIpPartial(ip);
        log.info("Login: {} desde {}", maskedEmail, maskedIp);
    }
    
    public void processRequest(String requestData) {
        // ❌ MAL - Puede contener contraseñas, tokens
        log.debug("Request: {}", requestData);
        
        // ✅ BIEN - Sanitiza automáticamente
        String safeLog = outputSanitization.sanitizeForLog(requestData);
        log.debug("Request: {}", safeLog);
    }
}
```

---

## 9. Validación en DTOs

```java
public class CreateUserRequest {
    
    @NotBlank(message = "Email requerido")
    private String email;
    
    @NotBlank(message = "Username requerido")
    private String username;
    
    @NotBlank(message = "Nombre requerido")
    @Size(min = 1, max = 100, message = "Nombre debe tener entre 1 y 100 caracteres")
    private String name;
    
    // Validación adicional en servicio
    public void validate(InputValidationService validator) {
        validator.validateAndSanitizeEmail(this.email);
        validator.validateAndSanitizeUsername(this.username);
        validator.validateInput(this.name, "nombre");
    }
}
```

---

## 10. Checklist de Seguridad por Endpoint

Para cada endpoint nuevo, verificar:

### Entrada
- [ ] Validar autenticación con `authValidator`
- [ ] Validar autorización con `@PreAuthorize`
- [ ] Validar formato con `inputValidation`
- [ ] Detectar inyecciones con `containsSqlInjection()` / `containsXss()`
- [ ] Sanitizar entrada con `sanitize()`
- [ ] Validar longitud con `isValidLength()`

### Procesamiento
- [ ] Usar prepared statements (JPA automático)
- [ ] Verificar integridad con `dataIntegrity` si es crítico
- [ ] No exponer información sensible en logs
- [ ] Usar transacciones para operaciones críticas

### Salida
- [ ] Sanitizar errores con `sanitizeErrorMessage()`
- [ ] Enmascarar datos sensibles con `mask*()`
- [ ] Escapar HTML con `escapeHtml()` si es necesario
- [ ] No exponer stack traces
- [ ] Agregar transaction ID para trazabilidad

---

## 11. Buenas Prácticas

### ✅ HACER
- Validar TODA entrada del usuario
- Sanitizar TODA salida
- Enmascarar datos sensibles en logs
- Usar servicios de seguridad en TODOS los endpoints
- Validar integridad de datos críticos
- Manejar errores de forma segura
- Usar @PreAuthorize para autorización
- Loguear operaciones críticas

### ❌ NO HACER
- Confiar en entrada del usuario
- Exponer stack traces al cliente
- Loguear contraseñas o tokens
- Hardcodear secretos
- Ignorar excepciones de seguridad
- Usar concatenación de strings para SQL
- Exponer rutas de archivos en errores
- Retornar información técnica en errores

---

## 12. Testing de Seguridad

```java
@SpringBootTest
public class SecurityTest {

    @Autowired
    private InputValidationService inputValidation;

    @Test
    public void testSqlInjectionDetection() {
        String maliciousInput = "'; DROP TABLE users; --";
        
        assertThrows(SecurityException.class, () -> {
            inputValidation.validateInput(maliciousInput, "test");
        });
    }
    
    @Test
    public void testXssDetection() {
        String maliciousInput = "<script>alert('XSS')</script>";
        
        assertThrows(SecurityException.class, () -> {
            inputValidation.validateInput(maliciousInput, "test");
        });
    }
    
    @Test
    public void testEmailMasking() {
        String email = "usuario@ejemplo.com";
        String masked = outputSanitization.maskEmail(email);
        
        assertFalse(masked.contains("usuario"));
        assertTrue(masked.contains("@"));
    }
}
```

---

## Resumen

Los servicios de seguridad proporcionan:

1. **InputValidationService**: Validación y sanitización de entrada
2. **OutputSanitizationService**: Enmascaramiento y sanitización de salida
3. **DataIntegrityService**: Verificación de integridad de datos
4. **AuthenticationValidator**: Validación centralizada de autenticación
5. **SecurityHeadersFilter**: Headers de seguridad HTTP automáticos

Usar estos servicios en TODOS los endpoints para garantizar seguridad completa.
