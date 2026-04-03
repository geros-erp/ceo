# Guía de Prueba - Validaciones de Username Único

## Validaciones Implementadas

### 1. Username No Puede Ser Genérico

**Intenta crear un usuario con**:
- Username: `admin`
- Email: `admin@empresa.com`
- Nombre: Juan
- Apellido: Pérez

**Resultado**: ❌ **RECHAZADO**
```
El usuario 'admin' no está permitido. Debe usar un nombre personalizado único 
para el funcionario (ej: jperez, mgarcia123)
```

---

### 2. Username Debe Tener al Menos 4 Caracteres

**Intenta crear un usuario con**:
- Username: `jpe`
- Email: `jperez@empresa.com`

**Resultado**: ❌ **RECHAZADO**
```
El usuario debe tener al menos 4 caracteres
```

---

### 3. Username No Puede Ser Solo Números

**Intenta crear un usuario con**:
- Username: `123456`
- Email: `emp123@empresa.com`

**Resultado**: ❌ **RECHAZADO**
```
El usuario no puede contener solo números. Debe incluir al menos una letra 
(ej: emp001 no es válido, aber001 sí)
```

---

### 4. Username Debe Ser Único

**Primer usuario creado**:
- Username: `jperez`
- Email: `juan@empresa.com`

**Segundo intento con el mismo username**:
- Username: `jperez`
- Email: `juan2@empresa.com`

**Resultado**: ❌ **RECHAZADO** (para el segundo)
```
El usuario 'jperez' ya está en uso. Cada funcionario debe tener un usuario 
único y personalizado.
```

---

### 5. Username No Se Puede Modificar

**Usuario creado** con username: `mgarcia`

**Intenta editar el usuario** para cambiar username a: `mgarcia2024`

**Resultado**: ❌ **NO PERMITIDO**
```
El campo username está deshabilitado en edición.
No se permite modificar el username de un usuario existente.
```

---

## Usernames Válidos - Ejemplos

✅ **Válidos**:
- `jperez` - Inicial + Apellido
- `mgarcia123` - Nombre + Números
- `j-garcia` - Con guión
- `j_garcia` - Con guión bajo
- `apolo_dev` - Con contexto
- `peredosanchez` - Nombre completo
- `m2024` - Inicial + Año
- `carter_admin` - Con rol

---

## Usernames Inválidos - Ejemplos

❌ **Inválidos**:
- `admin` - Reservado/genérico
- `root` - Reservado
- `test` - Genérico
- `guest` - Genérico
- `123456` - Solo números
- `jpe` - Muy corto (< 4 caracteres)
- `shared` - Compartido
- `team` - Grupal

---

## Restricciones Reservadas

Estas palabras NO se pueden usar como username:

### Administrativas
admin, administrator, root, superuser

### Genéricas
guest, test, demo, example, user, users, account, accounts

### De Sistema
system, app, application, service, bot, api

### Compartidas
shared, group, team, common, generic

### Temporales
temporal, temp, default

### Números [Solo números]
000, 111, 222, 333, 444, 555, 666, 777, 888, 999
0000, 1111, etc.

---

## Flujo de Creación de Usuario

```
1. ADMIN abre formulario "Nuevo Usuario"
   ↓
2. Ingresa Username: "jperez" 
   ↓
3. Sistema valida:
   ✓ No es vacío
   ✓ Tiene >= 4 caracteres
   ✓ Contiene al menos 1 letra
   ✓ Solo tiene caracteres válidos (letras, números, -, _)
   ✓ No es reservado/genérico
   ✓ No existe en BD (unique)
   ↓
4. Si todas las validaciones pasan → CREAR
   Si alguna falla → RECHAZAR y mostrar mensaje específico
   ↓
5. Usuario creado - USERNAME INMUTABLE
```

---

## Pruebas Recomendadas

### Test 1: Crear usuario válido
```
- Username: jperez
- Email: juan.perez@empresa.com
- Nombre: Juan
- Apellido: Pérez
- Resultado esperado: ✅ ÉXITO
```

### Test 2: Intentar genérico
```
- Username: admin
- Email: admin2@empresa.com
- Resultado esperado: ❌ RECHAZADO
```

### Test 3: Intentar duplicado
```
- Username: jperez (ya existe)
- Email: otro@empresa.com
- Resultado esperado: ❌ RECHAZADO - Ya en uso
```

### Test 4: Intentar modificar username
```
- Editar usuario con username: jperez
- Intentar cambiar a: jperez2024
- Resultado esperado: ❌ Campo deshabilitado
```

---

## Impacto en Auditoría

Cada acción queda registrada con el username:

```
EVENTO: USER_CREATED
- Usuario: jperez (INMUTABLE)
- Email: juan@empresa.com
- Timestamp: 2026-03-31 10:05:00
- Acción: Usuario creado por administrador
- Detalles: Contraseña temporal asignada y enviada

EVENTO: LOGIN_SUCCESS
- Usuario: jperez
- Timestamp: 2026-03-31 10:05:30
- Acción: Inicio de sesión exitoso

EVENTO: PASSWORD_CHANGED_BY_USER
- Usuario: jperez
- Timestamp: 2026-03-31 10:06:00
- Acción: Cambio de contraseña
```

---

## Beneficios

✅ **No-repudio**: Nadie puede negar sus acciones (username inmutable)
✅ **Auditoría**: Trazabilidad completa por funcionario
✅ **Seguridad**: Evita usuarios compartidos y genéricos
✅ **Compliance**: Cumple regulaciones de auditoría
✅ **Integridad**: Cada persona es identificada únicamente

---

**Fecha**: 31 de Marzo de 2026
**Sistema**: Geros v1.0+
**Estado**: ✅ En Producción
