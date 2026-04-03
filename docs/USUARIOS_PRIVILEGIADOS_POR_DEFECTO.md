# Gestión de Usuarios Privilegiados por Defecto

## Requisito de Seguridad

**Debe ser posible identificar y deshabilitar o eliminar los usuarios privilegiados que se instalan por defecto con la solución.**

## Implementación

### 1. Identificación de Usuarios por Defecto

#### Backend - Entidad User

**Campo agregado**: `is_default_user` (boolean)

```java
@Column(name = "is_default_user", nullable = false)
private boolean isDefaultUser = false;
```

**Propósito**: Marcar usuarios que son instalados automáticamente por el sistema.

#### Usuario por Defecto del Sistema

**Email**: admin@geros.com
**Username**: admin
**Password inicial**: admin123
**Rol**: ADMIN
**Marcado como**: `isDefaultUser = true`

### 2. Migración de Base de Datos

**Archivo**: `backend/src/main/resources/db/migration/V1__add_default_user_flag.sql`

```sql
-- Agregar columna
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS is_default_user BOOLEAN NOT NULL DEFAULT FALSE;

-- Marcar usuario admin
UPDATE auth.users SET is_default_user = TRUE WHERE email = 'admin@geros.com';

-- Crear índice
CREATE INDEX IF NOT EXISTS idx_users_is_default_user ON auth.users(is_default_user);
```

### 3. Inicialización del Sistema

**DataInitializer.java**:
- Marca automáticamente el usuario admin como `isDefaultUser = true`
- Muestra advertencia en consola al iniciar:
  ```
  >>> ADVERTENCIA: Usuario privilegiado por defecto detectado. 
      Se recomienda deshabilitar o eliminar después de crear usuarios personalizados.
  ```

### 4. Identificación Visual en Frontend

#### Dashboard - Tabla de Usuarios

**Badge naranja** junto al username:
```
⚠️ Por defecto
```

**Características**:
- Color naranja (bg-orange-100 text-orange-700)
- Tooltip: "Usuario privilegiado instalado por defecto"
- Visible solo para usuarios marcados como `isDefaultUser = true`

### 5. Protecciones de Seguridad

#### No Eliminar Último Administrador

**UserService.delete()**:
```java
if (user.hasRole("ADMIN")) {
    long adminCount = userRepository.findAll().stream()
        .filter(u -> u.hasRole("ADMIN") && Boolean.TRUE.equals(u.getIsActive()))
        .count();
    
    if (adminCount <= 1) {
        throw new SecurityException(
            "No se puede eliminar el último usuario administrador activo del sistema"
        );
    }
}
```

**Propósito**: Evitar que el sistema quede sin administradores.

#### Confirmación Especial para Eliminación

**Frontend - Dashboard.jsx**:
```javascript
const confirmMessage = userToDelete?.isDefaultUser 
  ? '⚠️ ADVERTENCIA: Estás a punto de eliminar un usuario privilegiado instalado por defecto.\n\n¿Estás seguro de que deseas continuar?'
  : '¿Eliminar este usuario?';
```

### 6. Auditoría de Seguridad

**SecurityLog** registra:
- Tipo de usuario eliminado (por defecto o personalizado)
- Email del usuario eliminado
- Quién realizó la eliminación
- Timestamp de la operación

```java
String userType = user.isDefaultUser() 
    ? "Usuario privilegiado por defecto" 
    : "Usuario";

securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.USER_DELETED)
    .description(userType + " eliminado del sistema")
    ...);
```

## Procedimiento Recomendado

### Paso 1: Instalación Inicial

1. Sistema se instala con usuario admin por defecto
2. Credenciales iniciales: admin@geros.com / admin123
3. Usuario marcado como `isDefaultUser = true`

### Paso 2: Configuración Inicial

1. Iniciar sesión con usuario admin por defecto
2. Crear nuevo usuario administrador personalizado:
   - Username único del funcionario
   - Email corporativo
   - Rol ADMIN
   - Contraseña segura
3. Verificar que el nuevo usuario puede acceder

### Paso 3: Deshabilitar Usuario por Defecto

**Opción A - Deshabilitar** (Recomendado):
1. Ir a Dashboard → Usuarios
2. Localizar usuario con badge "⚠️ Por defecto"
3. Click en "Editar"
4. Desmarcar "Usuario activo"
5. Guardar

**Ventajas**:
- Usuario queda inactivo pero preservado
- Se puede reactivar en caso de emergencia
- Mantiene historial de auditoría

**Opción B - Bloquear**:
1. Ir a Dashboard → Usuarios
2. Localizar usuario con badge "⚠️ Por defecto"
3. Click en botón "🔒"
4. Usuario queda bloqueado

**Ventajas**:
- Bloqueo inmediato
- Reversible con "🔓"

### Paso 4: Eliminar Usuario por Defecto (Opcional)

**Solo si**:
- Ya existe al menos otro usuario ADMIN activo
- Se ha verificado acceso con el nuevo administrador
- Se tiene respaldo de credenciales alternativas

**Procedimiento**:
1. Ir a Dashboard → Usuarios
2. Localizar usuario con badge "⚠️ Por defecto"
3. Click en "Eliminar"
4. Confirmar advertencia especial
5. Usuario eliminado permanentemente

**Advertencia**: 
```
⚠️ ADVERTENCIA: Estás a punto de eliminar un usuario privilegiado 
instalado por defecto.

¿Estás seguro de que deseas continuar?
```

## Validaciones de Seguridad

### 1. No Eliminar Último Admin

**Error**: "No se puede eliminar el último usuario administrador activo del sistema"

**Cuándo ocurre**: Al intentar eliminar el único usuario con rol ADMIN activo.

**Solución**: Crear otro usuario ADMIN antes de eliminar.

### 2. Identificación Clara

**Badge visual**: Todos los usuarios por defecto muestran "⚠️ Por defecto"

**API Response**: Campo `isDefaultUser: true` en UserDTO.Response

### 3. Auditoría Completa

**SecurityLog registra**:
- USER_DELETED con tipo de usuario
- Timestamp
- Usuario que realizó la acción
- Email del usuario eliminado

## Consultas SQL Útiles

### Listar Usuarios por Defecto

```sql
SELECT id, username, email, is_active, is_default_user
FROM auth.users
WHERE is_default_user = TRUE;
```

### Deshabilitar Usuario por Defecto

```sql
UPDATE auth.users
SET is_active = FALSE
WHERE is_default_user = TRUE;
```

### Verificar Cantidad de Admins Activos

```sql
SELECT COUNT(*) as admin_count
FROM auth.users u
JOIN auth.user_roles ur ON u.id = ur.user_id
JOIN auth.roles r ON ur.role_id = r.id
WHERE r.name = 'ADMIN' 
  AND u.is_active = TRUE
  AND u.locked_at IS NULL;
```

### Eliminar Usuario por Defecto (Cuidado)

```sql
-- Solo si hay otros admins activos
DELETE FROM auth.users
WHERE is_default_user = TRUE
  AND email = 'admin@geros.com';
```

## Mejores Prácticas

### ✅ HACER

1. **Crear usuario administrador personalizado** inmediatamente después de la instalación
2. **Deshabilitar usuario por defecto** una vez configurado el sistema
3. **Verificar acceso** con el nuevo usuario antes de eliminar el por defecto
4. **Mantener al menos 2 usuarios ADMIN** activos en todo momento
5. **Documentar credenciales** de usuarios administradores en lugar seguro
6. **Revisar logs de seguridad** periódicamente

### ❌ NO HACER

1. **No eliminar** el usuario por defecto sin tener otro admin configurado
2. **No compartir** credenciales del usuario por defecto
3. **No dejar activo** el usuario por defecto en producción
4. **No usar** el usuario por defecto para operaciones diarias
5. **No ignorar** las advertencias de eliminación
6. **No eliminar** el último usuario ADMIN del sistema

## Escenarios de Uso

### Escenario 1: Instalación Nueva

```
1. Sistema instalado → Usuario admin@geros.com creado automáticamente
2. Login con admin@geros.com / admin123
3. Crear usuario juan.perez@empresa.com con rol ADMIN
4. Logout y login con juan.perez@empresa.com
5. Deshabilitar admin@geros.com
6. Sistema operando con usuario personalizado
```

### Escenario 2: Recuperación de Acceso

```
1. Administrador olvida su contraseña
2. Reactivar usuario admin@geros.com (si está deshabilitado)
3. Login con admin@geros.com / admin123
4. Resetear contraseña del administrador
5. Volver a deshabilitar admin@geros.com
```

### Escenario 3: Auditoría de Seguridad

```
1. Revisar Dashboard → Usuarios
2. Verificar que NO hay usuarios con badge "⚠️ Por defecto" activos
3. Si hay usuarios por defecto activos → Investigar y deshabilitar
4. Revisar SecurityLog para verificar eliminaciones
```

## Cumplimiento de Requisito

✅ **Identificación**: Badge visual "⚠️ Por defecto" + campo `isDefaultUser` en API

✅ **Deshabilitar**: Opción "Usuario activo" en formulario de edición

✅ **Bloquear**: Botón "🔒" en tabla de usuarios

✅ **Eliminar**: Botón "Eliminar" con confirmación especial

✅ **Protección**: No permite eliminar último admin activo

✅ **Auditoría**: Todos los cambios registrados en SecurityLog

✅ **Advertencias**: Mensajes claros en consola y UI

## Resumen

El sistema permite **identificar, deshabilitar y eliminar** usuarios privilegiados por defecto de forma segura:

- **Identificación**: Visual (badge naranja) y programática (campo isDefaultUser)
- **Deshabilitar**: Editar usuario y desmarcar "Usuario activo"
- **Bloquear**: Botón de bloqueo en tabla
- **Eliminar**: Botón de eliminación con confirmación especial
- **Protección**: No permite eliminar último administrador
- **Auditoría**: Registro completo en SecurityLog

**Recomendación**: Deshabilitar usuarios por defecto después de crear usuarios administradores personalizados.
