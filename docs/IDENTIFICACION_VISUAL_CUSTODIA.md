# Identificación Visual de Usuarios en Custodia - Frontend

## Requisito

**El usuario administrador del sistema se debe diseñar de tal forma que no sea requerido en la operación diaria de manera que se pueda guardar en custodia.**

## Identificación en el Frontend

### 1. Panel de Advertencia Superior

#### Usuario por Defecto ACTIVO (Alerta Crítica)

```
┌─────────────────────────────────────────────────────────────────┐
│ 🚨 ADVERTENCIA DE SEGURIDAD: Usuario privilegiado por defecto  │
│    ACTIVO                                                       │
├─────────────────────────────────────────────────────────────────┤
│ Se ha detectado que el usuario administrador por defecto está  │
│ activo. Este usuario debe permanecer DESHABILITADO y guardado  │
│ en custodia física para cumplir con los requisitos de          │
│ seguridad.                                                      │
│                                                                 │
│ • Acción requerida: Deshabilitar el usuario y guardar          │
│   credenciales en sobre sellado                                │
│ • Ubicación: Caja fuerte o custodia bancaria                   │
│ • Uso: Solo en casos de emergencia                             │
└─────────────────────────────────────────────────────────────────┘

Color: Rojo (bg-red-50, border-red-500)
Icono: 🚨
Cuándo aparece: Cuando existe un usuario con defaultUser=true y isActive=true
```

#### Usuario por Defecto DESHABILITADO (Estado Correcto)

```
┌─────────────────────────────────────────────────────────────────┐
│ 🔐 Usuario administrador en custodia                            │
├─────────────────────────────────────────────────────────────────┤
│ El usuario privilegiado por defecto está correctamente          │
│ deshabilitado y en custodia física. Solo debe reactivarse en   │
│ casos de emergencia (recuperación de acceso).                   │
└─────────────────────────────────────────────────────────────────┘

Color: Púrpura (bg-purple-50, border-purple-500)
Icono: 🔐
Cuándo aparece: Cuando existe un usuario con defaultUser=true y isActive=false
```

### 2. Tabla de Usuarios - Columna "Usuario"

#### Usuario por Defecto Activo

```
┌─────────────────────────────────────────────────────────────┐
│ @admin                                                      │
│ ⚠️ Por defecto                                              │
│ 🚨 ACTIVO (Revisar)  ← Badge rojo parpadeante             │
└─────────────────────────────────────────────────────────────┘

Badges:
- "⚠️ Por defecto": Naranja (bg-orange-100, text-orange-700)
- "🚨 ACTIVO (Revisar)": Rojo con animación pulse (bg-red-100, text-red-700, animate-pulse)
```

#### Usuario por Defecto en Custodia (Deshabilitado)

```
┌─────────────────────────────────────────────────────────────┐
│ @admin                                                      │
│ ⚠️ Por defecto                                              │
│ 🔐 EN CUSTODIA  ← Badge púrpura                            │
└─────────────────────────────────────────────────────────────┘

Badges:
- "⚠️ Por defecto": Naranja (bg-orange-100, text-orange-700)
- "🔐 EN CUSTODIA": Púrpura (bg-purple-100, text-purple-700)
Tooltip: "Usuario en custodia - Solo para emergencias"
```

#### Usuario Operacional Normal

```
┌─────────────────────────────────────────────────────────────┐
│ @jperez                                                     │
│ (sin badges especiales)                                     │
└─────────────────────────────────────────────────────────────┘

Sin badges de custodia - Usuario normal para operación diaria
```

### 3. Tabla de Usuarios - Columna "Estado"

#### Usuario por Defecto en Custodia

```
┌─────────────────────────────────────────────────────────────┐
│ ○ Inactivo                                                  │
│ 📦 Custodia física  ← Indicador adicional                  │
└─────────────────────────────────────────────────────────────┘

Estados:
- "○ Inactivo": Gris (bg-gray-100, text-gray-600)
- "📦 Custodia física": Púrpura (text-purple-600)
Tooltip: "Usuario guardado en custodia física"
```

#### Usuario por Defecto Activo (Incorrecto)

```
┌─────────────────────────────────────────────────────────────┐
│ ✓ Activo  ← Verde pero incorrecto para usuario por defecto │
└─────────────────────────────────────────────────────────────┘

Estado: "✓ Activo" (bg-green-100, text-green-700)
Nota: Este estado es incorrecto para usuarios por defecto
```

#### Usuario Operacional Activo (Correcto)

```
┌─────────────────────────────────────────────────────────────┐
│ ✓ Activo  ← Estado normal para usuarios operacionales      │
└─────────────────────────────────────────────────────────────┘

Estado: "✓ Activo" (bg-green-100, text-green-700)
```

### 4. Modal de Edición

#### Editando Usuario en Custodia

```
┌─────────────────────────────────────────────────────────────┐
│ Editar usuario                                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🔐 Usuario en custodia                                  │ │
│ │                                                         │ │
│ │ Este usuario debe permanecer DESHABILITADO y guardado  │ │
│ │ en custodia física. Solo activar en casos de           │ │
│ │ emergencia.                                             │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ Usuario: @admin (deshabilitado)                             │
│ Nombre: Admin                                               │
│ Apellido: System                                            │
│ Email: admin@geros.com (deshabilitado)                      │
│                                                             │
│ Roles:                                                      │
│ ☑ ADMIN  ☐ USER                                            │
│                                                             │
│ ☐ Usuario activo  ⚠️ No recomendado para usuarios en      │
│                      custodia                               │
│                                                             │
│                                    [Cancelar]  [Guardar]    │
└─────────────────────────────────────────────────────────────┘

Panel de advertencia: Púrpura (bg-purple-50, border-purple-200)
Advertencia en checkbox: Roja (text-red-600)
```

#### Editando Usuario Operacional

```
┌─────────────────────────────────────────────────────────────┐
│ Editar usuario                                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Usuario: @jperez                                            │
│ Nombre: Juan                                                │
│ Apellido: Pérez                                             │
│ Email: juan.perez@empresa.com                               │
│                                                             │
│ Roles:                                                      │
│ ☑ ADMIN  ☐ USER                                            │
│                                                             │
│ ☑ Usuario activo  ← Sin advertencias                       │
│                                                             │
│                                    [Cancelar]  [Guardar]    │
└─────────────────────────────────────────────────────────────┘

Sin advertencias especiales - Usuario normal
```

### 5. Modal de Cambio de Contraseña

```
┌─────────────────────────────────────────────────────────────┐
│ Rotación de Credenciales                                    │
│ Usuario: @admin                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Nueva contraseña: [**********]                              │
│                                                             │
│ ☑ Forzar cambio al ingresar (Recomendado para custodia)    │
│                                                             │
│                                    [Cancelar]  [Cambiar]    │
└─────────────────────────────────────────────────────────────┘

Botón en tabla: "🔑 Clave" (bg-amber-50, text-amber-700)
Tooltip: "Rotar contraseña para custodia"
```

### 6. Confirmación de Eliminación

#### Usuario por Defecto

```
┌─────────────────────────────────────────────────────────────┐
│ ⚠️ ADVERTENCIA: Estás a punto de eliminar un usuario       │
│ privilegiado instalado por defecto.                         │
│                                                             │
│ ¿Estás seguro de que deseas continuar?                      │
│                                                             │
│                                    [Cancelar]  [Eliminar]   │
└─────────────────────────────────────────────────────────────┘
```

#### Usuario Operacional

```
┌─────────────────────────────────────────────────────────────┐
│ ¿Eliminar este usuario?                                     │
│                                                             │
│                                    [Cancelar]  [Eliminar]   │
└─────────────────────────────────────────────────────────────┘
```

## Resumen de Identificadores Visuales

### Iconos

| Icono | Significado | Contexto |
|-------|-------------|----------|
| 🚨 | Alerta crítica | Usuario por defecto ACTIVO (incorrecto) |
| 🔐 | En custodia | Usuario por defecto DESHABILITADO (correcto) |
| ⚠️ | Por defecto | Usuario instalado por defecto |
| 📦 | Custodia física | Guardado en ubicación segura |
| 🔑 | Rotación de clave | Cambiar contraseña para custodia |
| ✓ | Activo | Usuario habilitado |
| ○ | Inactivo | Usuario deshabilitado |
| 🔒 | Bloqueado | Usuario bloqueado por intentos fallidos |

### Colores

| Color | Uso | Significado |
|-------|-----|-------------|
| Rojo | bg-red-50, text-red-700 | Alerta crítica - Acción requerida |
| Púrpura | bg-purple-50, text-purple-700 | Estado de custodia - Correcto |
| Naranja | bg-orange-100, text-orange-700 | Usuario por defecto - Identificación |
| Verde | bg-green-100, text-green-700 | Usuario activo - Normal |
| Gris | bg-gray-100, text-gray-600 | Usuario inactivo - Normal |
| Amarillo | bg-yellow-100, text-yellow-700 | Advertencia - Cambio requerido |

### Animaciones

| Animación | Uso | Significado |
|-----------|-----|-------------|
| animate-pulse | Badge "🚨 ACTIVO (Revisar)" | Alerta crítica que requiere atención inmediata |

## Flujo Visual de Estados

### Estado 1: Instalación Inicial

```
Dashboard:
┌─────────────────────────────────────────────────────────────┐
│ 🚨 ADVERTENCIA DE SEGURIDAD: Usuario privilegiado por      │
│    defecto ACTIVO                                           │
└─────────────────────────────────────────────────────────────┘

Tabla:
Usuario: @admin  ⚠️ Por defecto  🚨 ACTIVO (Revisar)
Estado: ✓ Activo
```

**Acción requerida**: Crear usuarios operacionales y deshabilitar admin

### Estado 2: Configuración Completada

```
Dashboard:
┌─────────────────────────────────────────────────────────────┐
│ 🔐 Usuario administrador en custodia                        │
└─────────────────────────────────────────────────────────────┘

Tabla:
Usuario: @admin  ⚠️ Por defecto  🔐 EN CUSTODIA
Estado: ○ Inactivo  📦 Custodia física

Usuario: @jperez
Estado: ✓ Activo
```

**Estado correcto**: Usuario por defecto en custodia, operación con usuarios personalizados

### Estado 3: Emergencia (Temporal)

```
Dashboard:
┌─────────────────────────────────────────────────────────────┐
│ 🚨 ADVERTENCIA DE SEGURIDAD: Usuario privilegiado por      │
│    defecto ACTIVO                                           │
└─────────────────────────────────────────────────────────────┘

Tabla:
Usuario: @admin  ⚠️ Por defecto  🚨 ACTIVO (Revisar)
Estado: ✓ Activo
```

**Acción requerida**: Resolver emergencia y volver a deshabilitar

## Casos de Uso

### Caso 1: Auditor Verifica Cumplimiento

**Pregunta**: ¿El usuario administrador por defecto está en custodia?

**Verificación visual**:
1. Abrir Dashboard
2. Buscar panel púrpura "🔐 Usuario administrador en custodia"
3. Verificar en tabla: Badge "🔐 EN CUSTODIA" junto a @admin
4. Verificar estado: "○ Inactivo" + "📦 Custodia física"

**Resultado esperado**: ✅ Usuario en custodia correctamente

### Caso 2: Administrador Detecta Problema

**Escenario**: Usuario por defecto está activo

**Identificación visual**:
1. Panel rojo parpadeante en parte superior
2. Badge "🚨 ACTIVO (Revisar)" con animación pulse
3. Advertencia clara de acción requerida

**Acción**: Editar usuario y desmarcar "Usuario activo"

### Caso 3: Nuevo Administrador Aprende el Sistema

**Pregunta**: ¿Qué usuarios puedo usar para operación diaria?

**Identificación visual**:
- Usuarios SIN badge "⚠️ Por defecto" → Usar para operación diaria
- Usuarios CON badge "🔐 EN CUSTODIA" → NO usar (solo emergencias)

### Caso 4: Emergencia - Recuperación de Acceso

**Escenario**: Necesito reactivar usuario en custodia

**Proceso visual**:
1. Localizar usuario con badge "🔐 EN CUSTODIA"
2. Click "Editar"
3. Ver advertencia púrpura: "Usuario en custodia"
4. Marcar "Usuario activo" (aparece advertencia roja)
5. Guardar (sistema muestra alerta crítica en panel superior)
6. Usar para resolver emergencia
7. Volver a deshabilitar

## Implementación Técnica

### Componente: Dashboard.jsx

#### Lógica de Detección

```javascript
// Detectar usuarios por defecto activos
const hasActiveDefaultUser = users.some(u => u.defaultUser && u.isActive)

// Detectar usuarios por defecto en custodia
const hasInactiveDefaultUser = users.some(u => u.defaultUser && !u.isActive)
```

#### Renderizado Condicional

```javascript
// Panel de alerta crítica
{hasActiveDefaultUser && (
  <div className="bg-red-50 border-l-4 border-red-500">
    🚨 ADVERTENCIA DE SEGURIDAD
  </div>
)}

// Panel de estado correcto
{hasInactiveDefaultUser && (
  <div className="bg-purple-50 border-l-4 border-purple-500">
    🔐 Usuario administrador en custodia
  </div>
)}
```

#### Badges en Tabla

```javascript
// Badge "Por defecto"
{u.defaultUser && (
  <span className="bg-orange-100 text-orange-700">
    ⚠️ Por defecto
  </span>
)}

// Badge "EN CUSTODIA" (deshabilitado)
{u.defaultUser && !u.isActive && (
  <span className="bg-purple-100 text-purple-700">
    🔐 EN CUSTODIA
  </span>
)}

// Badge "ACTIVO (Revisar)" (activo - incorrecto)
{u.defaultUser && u.isActive && (
  <span className="bg-red-100 text-red-700 animate-pulse">
    🚨 ACTIVO (Revisar)
  </span>
)}
```

## Mejores Prácticas

### ✅ Identificación Clara

1. **Múltiples indicadores**: Panel superior + badges en tabla + estado
2. **Colores consistentes**: Rojo = problema, Púrpura = custodia, Naranja = por defecto
3. **Iconos descriptivos**: 🚨 alerta, 🔐 custodia, 📦 física
4. **Animaciones**: Pulse para alertas críticas

### ✅ Prevención de Errores

1. **Advertencias en modal de edición**: Antes de activar usuario en custodia
2. **Confirmación especial**: Al eliminar usuarios por defecto
3. **Tooltips informativos**: Explicación en hover
4. **Mensajes claros**: Acción requerida específica

### ✅ Auditoría Visual

1. **Estado visible inmediatamente**: Panel superior en Dashboard
2. **Identificación rápida**: Badges de colores en tabla
3. **Historial implícito**: Estado "Custodia física" indica cumplimiento

## Cumplimiento del Requisito

✅ **Identificación visual clara**: Múltiples indicadores (paneles, badges, estados)

✅ **Distinción de usuarios**: Por defecto vs operacionales claramente diferenciados

✅ **Estado de custodia visible**: Badge "🔐 EN CUSTODIA" + "📦 Custodia física"

✅ **Alertas de incumplimiento**: Panel rojo parpadeante si usuario por defecto está activo

✅ **Prevención de errores**: Advertencias al intentar activar usuario en custodia

✅ **Auditoría facilitada**: Estado visible sin necesidad de consultas SQL

## Resumen

El frontend identifica usuarios en custodia mediante:

1. **Panel superior**: Alerta roja si activo, confirmación púrpura si en custodia
2. **Badge "🔐 EN CUSTODIA"**: Junto al username en tabla
3. **Estado "📦 Custodia física"**: En columna de estado
4. **Advertencias en modales**: Al intentar activar usuario en custodia
5. **Animación pulse**: En badge de alerta crítica
6. **Colores consistentes**: Rojo = problema, Púrpura = custodia correcta

**Resultado**: Identificación visual inmediata del estado de custodia sin necesidad de conocimientos técnicos.
