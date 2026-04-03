# Identificación de Usuarios en Custodia - Guía Rápida

## ¿Cómo identificar en el frontend?

### 1. Panel Superior del Dashboard

#### ✅ Estado CORRECTO (Usuario en custodia)
```
┌────────────────────────────────────────────────────────┐
│ 🔐 Usuario administrador en custodia                   │
│                                                        │
│ El usuario privilegiado por defecto está              │
│ correctamente deshabilitado y en custodia física.     │
└────────────────────────────────────────────────────────┘
```
- **Color**: Púrpura (bg-purple-50)
- **Significado**: Sistema configurado correctamente

#### ❌ Estado INCORRECTO (Usuario activo)
```
┌────────────────────────────────────────────────────────┐
│ 🚨 ADVERTENCIA DE SEGURIDAD: Usuario privilegiado     │
│    por defecto ACTIVO                                  │
│                                                        │
│ Acción requerida: Deshabilitar el usuario y guardar   │
│ credenciales en sobre sellado                          │
└────────────────────────────────────────────────────────┘
```
- **Color**: Rojo (bg-red-50)
- **Significado**: Requiere acción inmediata

### 2. Tabla de Usuarios

#### Usuario en Custodia (Correcto)
```
Usuario: @admin  ⚠️ Por defecto  🔐 EN CUSTODIA
Estado:  ○ Inactivo  📦 Custodia física
```

#### Usuario Activo (Incorrecto)
```
Usuario: @admin  ⚠️ Por defecto  🚨 ACTIVO (Revisar) [parpadeante]
Estado:  ✓ Activo
```

#### Usuario Operacional (Normal)
```
Usuario: @jperez
Estado:  ✓ Activo
```

### 3. Badges y Colores

| Badge | Color | Significado |
|-------|-------|-------------|
| 🔐 EN CUSTODIA | Púrpura | Usuario deshabilitado correctamente |
| 🚨 ACTIVO (Revisar) | Rojo parpadeante | Requiere deshabilitar |
| ⚠️ Por defecto | Naranja | Usuario instalado por defecto |
| 📦 Custodia física | Púrpura | Guardado en ubicación segura |

### 4. Modal de Edición

Al editar usuario en custodia:
```
┌────────────────────────────────────────────────────────┐
│ 🔐 Usuario en custodia                                 │
│                                                        │
│ Este usuario debe permanecer DESHABILITADO y guardado │
│ en custodia física. Solo activar en casos de          │
│ emergencia.                                            │
└────────────────────────────────────────────────────────┘

☐ Usuario activo  ⚠️ No recomendado para usuarios en custodia
```

## Verificación Rápida

### ¿El sistema está configurado correctamente?

✅ **SÍ** si ves:
- Panel púrpura "🔐 Usuario administrador en custodia"
- Badge "🔐 EN CUSTODIA" en usuario @admin
- Estado "○ Inactivo" + "📦 Custodia física"

❌ **NO** si ves:
- Panel rojo "🚨 ADVERTENCIA DE SEGURIDAD"
- Badge "🚨 ACTIVO (Revisar)" parpadeante
- Estado "✓ Activo" en usuario por defecto

## Acciones

### Si el usuario está ACTIVO (incorrecto):
1. Click en "Editar" en usuario @admin
2. Desmarcar "Usuario activo"
3. Guardar
4. Verificar que aparece panel púrpura

### Si el usuario está EN CUSTODIA (correcto):
- No hacer nada
- Sistema configurado correctamente
- Usuario guardado en custodia física

## Archivos de Documentación

- **Guía completa**: `docs/IDENTIFICACION_VISUAL_CUSTODIA.md`
- **Procedimiento de custodia**: `docs/USUARIO_ADMIN_CUSTODIA.md`
- **Gestión de usuarios por defecto**: `docs/USUARIOS_PRIVILEGIADOS_POR_DEFECTO.md`
