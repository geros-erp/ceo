# Guía Rápida - Monitoreo de Archivos Críticos

## Inicio Rápido

### 1. Generar Baselines (Primera vez)

**Linux/macOS:**
```bash
cd /path/to/geros
chmod +x scripts/monitor-critical-files.sh
./scripts/monitor-critical-files.sh generate
```

**Windows:**
```powershell
cd d:\Harold\DesarrollosWeb\springboot\geros
.\scripts\monitor-critical-files.ps1 generate
```

### 2. Verificar Integridad

**Linux/macOS:**
```bash
./scripts/monitor-critical-files.sh verify
```

**Windows:**
```powershell
.\scripts\monitor-critical-files.ps1 verify
```

### 3. Ver Estadísticas

**Linux/macOS:**
```bash
./scripts/monitor-critical-files.sh stats
```

**Windows:**
```powershell
.\scripts\monitor-critical-files.ps1 stats
```

## Comandos Disponibles

| Comando | Descripción | Ejemplo |
|---------|-------------|---------|
| `generate` | Generar baselines iniciales | `./monitor-critical-files.sh generate` |
| `verify` | Verificar integridad (por defecto) | `./monitor-critical-files.sh verify` |
| `stats` | Mostrar estadísticas | `./monitor-critical-files.sh stats` |
| `list` | Listar archivos monitoreados | `./monitor-critical-files.sh list` |
| `update` | Actualizar baseline de un archivo | `./monitor-critical-files.sh update <archivo>` |
| `help` | Mostrar ayuda | `./monitor-critical-files.sh help` |

## Archivos Monitoreados

### Nivel 1 - Seguridad (14 archivos)
- SecurityConfig.java
- JwtFilter.java, JwtService.java
- AuthController.java, AuthenticationValidator.java
- SecurityHeadersFilter.java
- InputValidationService.java
- OutputSanitizationService.java
- DataIntegrityService.java
- application.properties
- pom.xml
- AuthContext.jsx, auth.js, App.jsx

### Nivel 2 - Lógica de Negocio (8 archivos)
- UserService.java, User.java
- RoleService.java, Role.java
- PasswordPolicyService.java, PasswordPolicy.java
- SecurityLogService.java, SecurityLog.java

### Nivel 3 - Base de Datos (4 archivos)
- init.sql
- V1__add_default_user_flag.sql
- UserRepository.java, RoleRepository.java

**Total: 26 archivos críticos**

## Interpretación de Resultados

### ✅ Resultado OK
```
✓ OK: backend/src/main/java/com/geros/backend/config/SecurityConfig.java
✓ OK: backend/src/main/java/com/geros/backend/security/JwtFilter.java
...
✓ Integridad verificada - Sistema seguro
```
**Acción**: Ninguna - Sistema seguro

### ❌ Archivo Modificado
```
✗ MODIFICADO: backend/src/main/resources/application.properties (Nivel 1)
  Hash esperado: abc123...
  Hash actual:   def456...
```
**Acción**: 
1. Verificar si el cambio fue autorizado
2. Si es legítimo: `./monitor-critical-files.sh update <archivo>`
3. Si no es legítimo: Investigar y restaurar desde backup

### ❌ Archivo Faltante
```
✗ NO ENCONTRADO: backend/pom.xml (Nivel 1)
```
**Acción**: 
1. Restaurar archivo desde backup
2. Investigar causa de eliminación

## Automatización

### Cron Job (Linux/macOS)

**Verificación cada hora:**
```bash
crontab -e
# Agregar:
0 * * * * cd /path/to/geros && ./scripts/monitor-critical-files.sh verify >> /var/log/file-integrity.log 2>&1
```

**Verificación cada 15 minutos:**
```bash
*/15 * * * * cd /path/to/geros && ./scripts/monitor-critical-files.sh verify >> /var/log/file-integrity.log 2>&1
```

### Task Scheduler (Windows)

**PowerShell:**
```powershell
# Crear tarea programada para ejecutar cada hora
$action = New-ScheduledTaskAction -Execute "PowerShell.exe" -Argument "-File d:\Harold\DesarrollosWeb\springboot\geros\scripts\monitor-critical-files.ps1 verify"
$trigger = New-ScheduledTaskTrigger -Once -At (Get-Date) -RepetitionInterval (New-TimeSpan -Hours 1)
Register-ScheduledTask -TaskName "FileIntegrityMonitor" -Action $action -Trigger $trigger
```

## Logs

### Ubicación de Logs

| Log | Ubicación | Contenido |
|-----|-----------|-----------|
| Verificaciones | `./file-integrity-monitor.log` | Todas las verificaciones |
| Alertas | `./integrity-alerts.log` | Solo cambios detectados |
| Baselines | `./baselines/` | Hashes SHA-256 de archivos |

### Ver Logs

**Linux/macOS:**
```bash
# Ver últimas verificaciones
tail -f file-integrity-monitor.log

# Ver alertas
cat integrity-alerts.log

# Ver alertas recientes
tail -20 integrity-alerts.log
```

**Windows:**
```powershell
# Ver últimas verificaciones
Get-Content file-integrity-monitor.log -Tail 20

# Ver alertas
Get-Content integrity-alerts.log

# Monitorear en tiempo real
Get-Content file-integrity-monitor.log -Wait
```

## Casos de Uso

### Caso 1: Después de Actualizar Código

```bash
# 1. Actualizar código desde Git
git pull origin main

# 2. Verificar integridad
./scripts/monitor-critical-files.sh verify

# 3. Si hay cambios legítimos, actualizar baselines
./scripts/monitor-critical-files.sh generate
```

### Caso 2: Antes de Despliegue

```bash
# 1. Verificar integridad antes de desplegar
./scripts/monitor-critical-files.sh verify

# 2. Si todo OK, proceder con despliegue
# 3. Después del despliegue, generar nuevos baselines
./scripts/monitor-critical-files.sh generate
```

### Caso 3: Auditoría de Seguridad

```bash
# 1. Verificar integridad
./scripts/monitor-critical-files.sh verify

# 2. Ver estadísticas
./scripts/monitor-critical-files.sh stats

# 3. Revisar logs de alertas
cat integrity-alerts.log

# 4. Generar reporte
./scripts/monitor-critical-files.sh verify > audit-report-$(date +%Y%m%d).txt
```

### Caso 4: Actualizar Archivo Específico

```bash
# Después de modificar application.properties de forma autorizada
./scripts/monitor-critical-files.sh update backend/src/main/resources/application.properties
```

## Integración con CI/CD

### GitHub Actions

```yaml
# .github/workflows/file-integrity.yml
name: File Integrity Check

on: [push, pull_request]

jobs:
  integrity:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Verify File Integrity
        run: |
          chmod +x scripts/monitor-critical-files.sh
          ./scripts/monitor-critical-files.sh verify
```

### GitLab CI

```yaml
# .gitlab-ci.yml
file-integrity:
  stage: test
  script:
    - chmod +x scripts/monitor-critical-files.sh
    - ./scripts/monitor-critical-files.sh verify
```

## Mejores Prácticas

### ✅ HACER

1. **Generar baselines después de cada release**
2. **Verificar integridad antes de cada despliegue**
3. **Automatizar verificaciones periódicas** (cada hora)
4. **Revisar logs de alertas diariamente**
5. **Actualizar baselines solo después de cambios autorizados**
6. **Mantener backups de baselines** en ubicación segura
7. **Documentar todos los cambios** en archivos críticos

### ❌ NO HACER

1. **No ignorar alertas** de archivos modificados
2. **No actualizar baselines** sin verificar cambios
3. **No almacenar baselines** en el repositorio Git
4. **No deshabilitar monitoreo** en producción
5. **No modificar archivos críticos** sin revisión de seguridad

## Troubleshooting

### Problema: "Archivo no encontrado"

**Causa**: Ruta incorrecta o archivo eliminado

**Solución**:
```bash
# Verificar que estás en el directorio raíz del proyecto
pwd
# Debe mostrar: /path/to/geros

# Verificar que el archivo existe
ls -la backend/src/main/resources/application.properties
```

### Problema: "No existe baseline"

**Causa**: Primera ejecución o baselines eliminados

**Solución**:
```bash
# Generar baselines
./scripts/monitor-critical-files.sh generate
```

### Problema: "Muchos archivos modificados"

**Causa**: Actualización de código o cambios masivos

**Solución**:
```bash
# 1. Verificar cambios en Git
git status
git log -5

# 2. Si los cambios son legítimos, regenerar baselines
./scripts/monitor-critical-files.sh generate

# 3. Si no son legítimos, investigar
git diff HEAD~1
```

## Resumen

**Flujo básico:**
1. `generate` - Generar baselines (primera vez)
2. `verify` - Verificar integridad (periódicamente)
3. `update` - Actualizar baseline (después de cambios autorizados)

**Archivos monitoreados:** 26 archivos críticos en 3 niveles

**Automatización:** Cron job cada hora o Task Scheduler

**Logs:** `file-integrity-monitor.log` y `integrity-alerts.log`

**Documentación completa:** `docs/ARCHIVOS_CRITICOS_MONITOREO.md`
