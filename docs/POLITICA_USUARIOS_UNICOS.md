# Política de Usuarios Únicos - Geros

## Objetivo
Garantizar que cada funcionario tiene un usuario único, personalizado e inmutable en el sistema, cumpliendo con estándares de auditoría, no-repudio y seguridad.

## Principios Fundamentales

### 1. Usuario Único por Funcionario
- **Un usuario = Un funcionario**
- Cada persona debe tener su propia cuenta personalizada
- NO se permiten usuarios compartidos entre varios funcionarios
- NO se permiten usuarios grupales o de equipo

### 2. Username Inmutable
- El `username` NO se puede modificar una vez creado
- Es la identificación permanente del funcionario
- Se garantiza la trazabilidad de acciones a una persona específica

### 3. Username Personalizado
- Debe ser único en todo el sistema
- Debe incluir al menos una letra (no solo números)
- Longitud mínima: 4 caracteres
- Caracteres válidos: letras, números, guiones (-) y guiones bajos (_)
- Ejemplos válidos: `jperez`, `mgarcia123`, `j-garcia`, `j_garcia`

### 4. Usernames Prohibidos (Genéricos/Compartidos)
El sistema rechaza automáticamente usernames genéricos o compartidos:

- **Administrativos**: `admin`, `root`, `superuser`, `administrator`
- **Genéricos**: `user`, `users`, `account`, `accounts`, `test`, `demo`, `guest`
- **De Sistema**: `system`, `app`, `service`, `bot`, `api`
- **Solo números**: `111`, `000`, `123`, etc.
- **Otros**: `shared`, `group`, `team`, `common`, `generic`

### 5. Validaciones del Sistema

#### En Backend (Java)
```java
// El validador UsernameValidator garantiza:
- No sea genérico o compartido
- Contiene al menos una letra
- Mínimo 4 caracteres
- Solo caracteres válidos (letras, números, -, _)
- Lanza IllegalArgumentException si no cumple
```

#### En Base de Datos
```sql
-- Restricción UNIQUE en username
ALTER TABLE auth.users ADD CONSTRAINT uk_username UNIQUE (username);

-- Índice para búsquedas rápidas
CREATE INDEX idx_users_username_active ON auth.users(username, is_active);
```

#### En Frontend (React)
- Campo deshabilitado después de crear usuario
- No se permite editar username
- Validación en tiempo real de formato

### 6. Auditoría y Trazabilidad

Cada acción registra:
- **username**: Quién realizó la acción
- **email**: Correo corporativo del usuario
- **timestamp**: Cuándo se realizó
- **acción**: Qué se realizó (login, crear usuario, cambiar rol, etc)
- **detalles**: Información adicional relevante

Esto permite:
- ✅ No-repudio: Nadie puede negar haber realizado una acción
- ✅ Auditoría: Completa trazabilidad de actividades
- ✅ Seguridad: Identificación precisa de responsables

### 7. Ciclo de Vida del Usuario

```
CREACIÓN
  ↓
username asignado (personalizado) + validado
  ↓
NO SE PUEDE CAMBIAR (inmutable)
  ↓
MODIFICACIONES PERMITIDAS
  - Nombre/Apellido
  - Email
  - Roles
  - Estado (activo/inactivo)
  ↓
ELIMINACIÓN
  - Se puede eliminar (se pierde historial de BD)
  - Pero el username permanece en logs de auditoría
```

### 8. Casos de Uso Permitidos

✅ **Crear usuario**: `jperez` para "Juan Pérez"
✅ **Crear usuario**: `m-garcia-2024` para "María García"
✅ **Crear usuario**: `apolo_admin` para "Admin Apollo"
✅ **Editar usuario**: Cambiar nombre, rol, departamento
✅ **Bloquear/Desbloquear**: Cambiar estado de cuenta
✅ **Cambiar contraseña**: Actualización de credenciales

### 9. Casos de Uso NO Permitidos

❌ **Crear usuario**: `admin` (genérico)
❌ **Crear usuario**: `123456` (solo números)
❌ **Crear usuario**: `shared_account` (compartido)
❌ **Modificar username**: Una vez creado, NO se puede cambiar
❌ **Compartir usuario**: Cada persona debe tener el suyo
❌ **Usuario grupal**: Un usuario por persona, no por grupo

### 10. Mensajes de Error y Soluciones

#### ❌ "El usuario no puede estar vacío"
- **Causa**: Username vacío
- **Solución**: Proporcione un username válido

#### ❌ "El usuario debe tener al menos 4 caracteres"
- **Causa**: Username muy corto
- **Solución**: Use `jpe` → `jpere` o similar (mínimo 4)

#### ❌ "El usuario no puede contener solo números"
- **Causa**: Username como `123456`
- **Solución**: Use `emp123` o `j2024`

#### ❌ "El usuario 'admin' no está permitido"
- **Causa**: Username genérico/reservado
- **Solución**: Use algo personalizado como `admin_jperez`

#### ❌ "El usuario 'jperez' ya está en uso"
- **Causa**: Username duplicado
- **Solución**: Elija otro username único (ej: `jperez_2024`)

### 11. Buenas Prácticas

1. **Usar iniciales + apellido**: `jgarcia`, `mlopez`, `apeor`
2. **Agregar contexto si es necesario**: `jgarcia_dev`, `mlopez_admin`
3. **Evitar caracteres especiales innecesarios**: Prefiera `j_garcia` sobre `j-garcía`
4. **Consistencia organizacional**: Si su empresa usa patrón, manténgalo
5. **Evitar datos personales sensibles**: No use fechas de nacimiento o números de ID

### 12. Responsabilidades

**Administrador de Sistema**:
- ✅ Crear usuarios con usernames únicos y personalizado
- ✅ Documentar la política de usuarios
- ✅ Validar que no haya usuarios compartidos
- ✅ Monitorear intentos de crear usuarios genéricos

**Usuarios**:
- ✅ No compartir credenciales
- ✅ Avisar si otro usuario conoce su contraseña
- ✅ Usar solo su propio usuario, nunca uno ajeno

**Seguridad**:
- ✅ Revisar logs regularmente
- ✅ Alertar sobre patrones sospechosos
- ✅ Garantizar integridad de la autenticación

---

**Última actualización**: 31 de Marzo de 2026
**Aplicable a**: Sistema Geros v1.0+
**Cumplimiento**: Este sistema implementa esta política de forma automática y obligatoria.
