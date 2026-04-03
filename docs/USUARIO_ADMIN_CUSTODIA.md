# Usuario Administrador en Custodia

## Requisito de Seguridad

**El usuario administrador del sistema se debe diseñar de tal forma que no sea requerido en la operación diaria de manera que se pueda guardar en custodia.**

## Concepto de Custodia

El usuario administrador por defecto (admin@geros.com) está diseñado como **usuario de emergencia** que:

- ✅ NO debe usarse en operaciones diarias
- ✅ Debe permanecer deshabilitado/bloqueado en producción
- ✅ Solo se activa en casos de emergencia
- ✅ Permite recuperación de acceso al sistema
- ✅ Se guarda en custodia física (sobre sellado, caja fuerte)

## Diseño del Sistema

### 1. Separación de Roles

#### Usuario por Defecto (Custodia)
```
Email: admin@geros.com
Username: admin
Password: admin123 (cambiar en primera instalación)
Rol: ADMIN
Estado: DESHABILITADO (después de configuración inicial)
Propósito: Recuperación de emergencia
```

#### Usuarios Operacionales (Diarios)
```
Email: juan.perez@empresa.com
Username: jperez
Password: [contraseña segura personalizada]
Rol: ADMIN
Estado: ACTIVO
Propósito: Operación diaria del sistema
```

### 2. Flujo de Implementación

```
┌─────────────────────────────────────────────────────────────┐
│ FASE 1: INSTALACIÓN INICIAL                                 │
├─────────────────────────────────────────────────────────────┤
│ 1. Sistema instalado                                        │
│ 2. Usuario admin@geros.com creado automáticamente           │
│ 3. Estado: ACTIVO (temporal)                                │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ FASE 2: CONFIGURACIÓN INICIAL                               │
├─────────────────────────────────────────────────────────────┤
│ 1. Login con admin@geros.com                                │
│ 2. Cambiar contraseña por defecto                           │
│ 3. Crear usuarios administradores operacionales:            │
│    - juan.perez@empresa.com (ADMIN)                         │
│    - maria.gomez@empresa.com (ADMIN)                        │
│ 4. Verificar acceso con usuarios operacionales              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ FASE 3: PONER EN CUSTODIA                                   │
├─────────────────────────────────────────────────────────────┤
│ 1. Deshabilitar admin@geros.com                             │
│ 2. Documentar credenciales en sobre sellado                 │
│ 3. Guardar en caja fuerte / custodia física                 │
│ 4. Registrar custodios autorizados                          │
│ 5. Estado: DESHABILITADO                                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ FASE 4: OPERACIÓN DIARIA                                    │
├─────────────────────────────────────────────────────────────┤
│ - Usuarios operacionales realizan todas las tareas          │
│ - admin@geros.com permanece DESHABILITADO                   │
│ - Sistema funciona sin usuario por defecto                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ FASE 5: EMERGENCIA (Solo si es necesario)                   │
├─────────────────────────────────────────────────────────────┤
│ 1. Recuperar sobre de custodia                              │
│ 2. Reactivar admin@geros.com (SQL o interfaz)               │
│ 3. Login con credenciales de custodia                       │
│ 4. Resolver emergencia (resetear contraseñas, etc.)         │
│ 5. Volver a deshabilitar admin@geros.com                    │
│ 6. Devolver a custodia                                      │
│ 7. Registrar uso en log de auditoría                        │
└─────────────────────────────────────────────────────────────┘
```

## Procedimiento de Custodia

### Paso 1: Preparar Credenciales

**Documento de Custodia** (imprimir y sellar):

```
═══════════════════════════════════════════════════════════
    CREDENCIALES DE USUARIO ADMINISTRADOR EN CUSTODIA
═══════════════════════════════════════════════════════════

Sistema: GEROS - Sistema de Gestión de Seguridad
Fecha de custodia: [DD/MM/YYYY]
Responsable: [Nombre del responsable de seguridad]

CREDENCIALES:
-------------
URL: https://localhost:8443
Email: admin@geros.com
Username: admin
Password: [contraseña cambiada en instalación]

INSTRUCCIONES DE USO:
---------------------
1. Solo abrir en caso de emergencia
2. Reactivar usuario antes de usar
3. Deshabilitar después de resolver emergencia
4. Registrar uso en log de auditoría
5. Volver a sellar y guardar en custodia

CUSTODIOS AUTORIZADOS:
----------------------
1. [Nombre] - [Cargo] - [Firma]
2. [Nombre] - [Cargo] - [Firma]

REGISTRO DE ACCESOS:
--------------------
Fecha       | Motivo              | Autorizado por | Firma
------------|---------------------|----------------|-------
            |                     |                |
            |                     |                |

═══════════════════════════════════════════════════════════
```

### Paso 2: Deshabilitar Usuario

**Opción A - Desde la Interfaz**:
```
1. Login con usuario operacional
2. Ir a Dashboard → Usuarios
3. Localizar admin@geros.com (badge "⚠️ Por defecto")
4. Click "Editar"
5. Desmarcar "Usuario activo"
6. Guardar
```

**Opción B - Desde SQL**:
```sql
UPDATE auth.users
SET is_active = FALSE
WHERE email = 'admin@geros.com';
```

### Paso 3: Guardar en Custodia Física

1. **Imprimir** documento de credenciales
2. **Sellar** en sobre opaco
3. **Etiquetar**: "USUARIO ADMINISTRADOR - SOLO EMERGENCIAS"
4. **Guardar** en:
   - Caja fuerte
   - Bóveda de seguridad
   - Custodia bancaria
5. **Registrar** ubicación y custodios autorizados

### Paso 4: Documentar Procedimiento

**Crear documento interno** con:
- Ubicación física de credenciales
- Lista de custodios autorizados
- Procedimiento de recuperación
- Contactos de emergencia
- Política de uso

## Escenarios de Emergencia

### Escenario 1: Administrador Olvida Contraseña

**Problema**: Usuario operacional no puede acceder.

**Solución**:
```
1. Recuperar sobre de custodia
2. Reactivar admin@geros.com:
   UPDATE auth.users SET is_active = TRUE WHERE email = 'admin@geros.com';
3. Login con admin@geros.com
4. Ir a Dashboard → Usuarios
5. Seleccionar usuario operacional
6. Click "Cambiar contraseña"
7. Establecer nueva contraseña temporal
8. Deshabilitar admin@geros.com nuevamente
9. Devolver a custodia
10. Registrar uso en log
```

### Escenario 2: Todos los Administradores Bloqueados

**Problema**: Múltiples intentos fallidos bloquearon a todos los admins.

**Solución**:
```
1. Recuperar sobre de custodia
2. Reactivar admin@geros.com
3. Login con admin@geros.com
4. Desbloquear usuarios operacionales:
   UPDATE auth.users SET locked_at = NULL, failed_attempts = 0 
   WHERE email IN ('juan.perez@empresa.com', 'maria.gomez@empresa.com');
5. Deshabilitar admin@geros.com
6. Devolver a custodia
```

### Escenario 3: Auditoría de Seguridad

**Problema**: Auditor requiere verificar usuario por defecto está deshabilitado.

**Verificación**:
```sql
SELECT 
    username,
    email,
    is_active,
    is_default_user,
    locked_at,
    last_login_at
FROM auth.users
WHERE is_default_user = TRUE;
```

**Resultado esperado**:
```
username | email              | is_active | is_default_user | locked_at | last_login_at
---------|--------------------|-----------|-----------------|-----------|--------------
admin    | admin@geros.com    | FALSE     | TRUE            | NULL      | [fecha antigua]
```

## Validaciones de Seguridad

### ✅ Sistema Permite Operación sin Usuario por Defecto

**Verificación**:
- Deshabilitar admin@geros.com
- Realizar operaciones con usuarios operacionales:
  - Crear usuarios
  - Modificar políticas
  - Revisar logs
  - Gestionar roles
- **Resultado**: Sistema funciona completamente

### ✅ Usuario por Defecto No es Requerido Diariamente

**Diseño**:
- Usuarios operacionales tienen rol ADMIN completo
- Todas las funciones disponibles sin admin@geros.com
- Sistema mantiene al menos 2 admins operacionales activos

### ✅ Recuperación de Emergencia Funcional

**Prueba**:
1. Deshabilitar todos los usuarios operacionales
2. Reactivar admin@geros.com desde SQL
3. Login exitoso
4. Restaurar acceso a usuarios operacionales
5. Deshabilitar admin@geros.com nuevamente

## Mejores Prácticas

### Durante Instalación

1. ✅ Cambiar contraseña de admin@geros.com inmediatamente
2. ✅ Crear mínimo 2 usuarios administradores operacionales
3. ✅ Verificar acceso con usuarios operacionales
4. ✅ Deshabilitar admin@geros.com antes de producción
5. ✅ Documentar y guardar credenciales en custodia

### Durante Operación

1. ✅ Usar solo usuarios operacionales para tareas diarias
2. ✅ Mantener admin@geros.com DESHABILITADO
3. ✅ Revisar periódicamente estado de custodia
4. ✅ Actualizar lista de custodios autorizados
5. ✅ Auditar que admin@geros.com no tenga logins recientes

### Durante Emergencia

1. ✅ Documentar motivo de uso de credenciales de custodia
2. ✅ Registrar quién autorizó el acceso
3. ✅ Resolver emergencia rápidamente
4. ✅ Deshabilitar admin@geros.com inmediatamente después
5. ✅ Devolver credenciales a custodia
6. ✅ Registrar uso en log de auditoría

## Consultas SQL Útiles

### Verificar Estado de Usuario en Custodia

```sql
SELECT 
    username,
    email,
    is_active,
    is_default_user,
    locked_at,
    last_login_at,
    CASE 
        WHEN is_active = FALSE THEN '✅ EN CUSTODIA'
        WHEN is_active = TRUE THEN '⚠️ ACTIVO (revisar)'
    END as estado_custodia
FROM auth.users
WHERE is_default_user = TRUE;
```

### Reactivar Usuario de Custodia (Emergencia)

```sql
-- Solo en emergencia
UPDATE auth.users
SET is_active = TRUE,
    locked_at = NULL,
    failed_attempts = 0
WHERE email = 'admin@geros.com';
```

### Devolver a Custodia (Después de Emergencia)

```sql
UPDATE auth.users
SET is_active = FALSE
WHERE email = 'admin@geros.com';
```

### Verificar Administradores Operacionales

```sql
SELECT 
    u.username,
    u.email,
    u.is_active,
    u.is_default_user,
    u.last_login_at
FROM auth.users u
JOIN auth.user_roles ur ON u.id = ur.user_id
JOIN auth.roles r ON ur.role_id = r.id
WHERE r.name = 'ADMIN'
  AND u.is_default_user = FALSE
  AND u.is_active = TRUE
ORDER BY u.last_login_at DESC;
```

### Auditar Uso de Usuario en Custodia

```sql
SELECT 
    action,
    description,
    username,
    ip_address,
    created_at
FROM auth.security_logs
WHERE username = 'admin'
ORDER BY created_at DESC
LIMIT 20;
```

## Checklist de Implementación

### ☑️ Instalación

- [ ] Sistema instalado con admin@geros.com
- [ ] Contraseña por defecto cambiada
- [ ] Usuario admin@geros.com funcional

### ☑️ Configuración

- [ ] Creados mínimo 2 usuarios administradores operacionales
- [ ] Usuarios operacionales tienen rol ADMIN
- [ ] Verificado acceso con usuarios operacionales
- [ ] Todas las funciones disponibles sin admin@geros.com

### ☑️ Custodia

- [ ] Usuario admin@geros.com deshabilitado
- [ ] Credenciales documentadas en formato impreso
- [ ] Sobre sellado y etiquetado
- [ ] Guardado en ubicación segura (caja fuerte)
- [ ] Custodios autorizados registrados
- [ ] Procedimiento de emergencia documentado

### ☑️ Operación

- [ ] Sistema operando solo con usuarios operacionales
- [ ] admin@geros.com permanece deshabilitado
- [ ] Sin logins recientes de admin@geros.com
- [ ] Auditorías periódicas del estado de custodia

### ☑️ Auditoría

- [ ] Verificado que admin@geros.com está deshabilitado
- [ ] Verificado que existen admins operacionales activos
- [ ] Verificado que sistema funciona sin usuario por defecto
- [ ] Documentación de custodia actualizada

## Cumplimiento del Requisito

✅ **Usuario administrador NO requerido en operación diaria**:
- Sistema funciona completamente con usuarios operacionales
- admin@geros.com permanece deshabilitado
- Todas las funciones administrativas disponibles sin usuario por defecto

✅ **Usuario administrador puede guardarse en custodia**:
- Diseñado para permanecer deshabilitado
- Credenciales en sobre sellado
- Guardado en ubicación física segura
- Solo se usa en emergencias

✅ **Recuperación de emergencia funcional**:
- Reactivación rápida desde SQL o interfaz
- Acceso completo para resolver emergencias
- Procedimiento documentado y probado

## Resumen

El usuario administrador por defecto (admin@geros.com) está diseñado como **usuario de emergencia en custodia**:

1. **No es requerido diariamente**: Sistema opera con usuarios administradores operacionales
2. **Permanece deshabilitado**: Estado normal es is_active = FALSE
3. **Guardado en custodia física**: Credenciales en sobre sellado en caja fuerte
4. **Solo para emergencias**: Reactivación temporal para recuperación de acceso
5. **Auditable**: Todos los usos registrados en SecurityLog

**Recomendación**: Mantener admin@geros.com deshabilitado en producción y usar solo usuarios administradores operacionales para todas las tareas diarias.
