# monitor-critical-files.ps1
# Script de monitoreo de integridad de archivos criticos para Windows

param(
    [Parameter(Position=0)]
    [ValidateSet('generate','verify','stats','list','help')]
    [string]$Command = 'verify'
)

# Configuracion
$BaselineDir = ".\baselines"
$LogFile = ".\file-integrity-monitor.log"
$AlertFile = ".\integrity-alerts.log"
$ProjectRoot = "."

# Funcion de logging
function Write-Log {
    param([string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logMessage = "[$timestamp] $Message"
    Write-Output $logMessage | Tee-Object -FilePath $LogFile -Append
}

function Write-Alert {
    param([string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $alertMessage = "[$timestamp] ALERT: $Message"
    Write-Output $alertMessage | Tee-Object -FilePath $AlertFile -Append
}

# Crear directorios si no existen
if (-not (Test-Path $BaselineDir)) {
    New-Item -ItemType Directory -Path $BaselineDir | Out-Null
}

# Lista de archivos criticos - NIVEL 1 (Seguridad)
$CriticalLevel1 = @(
    "backend\src\main\java\com\geros\backend\config\SecurityConfig.java",
    "backend\src\main\java\com\geros\backend\security\JwtFilter.java",
    "backend\src\main\java\com\geros\backend\security\JwtService.java",
    "backend\src\main\java\com\geros\backend\security\AuthController.java",
    "backend\src\main\resources\application.properties",
    "backend\pom.xml",
    "frontend\src\context\AuthContext.jsx",
    "frontend\src\api\auth.js"
)

# Lista de archivos criticos - NIVEL 2 (Logica de negocio)
$CriticalLevel2 = @(
    "backend\src\main\java\com\geros\backend\user\UserService.java",
    "backend\src\main\java\com\geros\backend\user\User.java",
    "backend\src\main\java\com\geros\backend\policy\PasswordPolicyService.java"
)

# Funcion para calcular SHA-256
function Get-FileHashSHA256 {
    param([string]$FilePath)
    $hash = Get-FileHash -Path $FilePath -Algorithm SHA256
    return $hash.Hash
}

# Funcion para generar baseline
function New-Baseline {
    param(
        [string]$FilePath,
        [int]$Level
    )
    
    $fullPath = Join-Path $ProjectRoot $FilePath
    $baselineFile = Join-Path $BaselineDir ($FilePath -replace '[\\/]', '_' + ".sha256")
    
    if (Test-Path $fullPath) {
        $hash = Get-FileHashSHA256 -FilePath $fullPath
        "$hash`n$Level" | Out-File -FilePath $baselineFile -Encoding UTF8
        Write-Log "Baseline generado: $FilePath (Nivel $Level)"
    } else {
        Write-Log "WARN: Archivo no encontrado: $FilePath"
    }
}

# Funcion para verificar integridad
function Test-Integrity {
    param(
        [string]$FilePath,
        [int]$Level
    )
    
    $fullPath = Join-Path $ProjectRoot $FilePath
    $baselineFile = Join-Path $BaselineDir ($FilePath -replace '[\\/]', '_' + ".sha256")
    
    if (-not (Test-Path $fullPath)) {
        Write-Alert "Archivo critico NO ENCONTRADO: $FilePath (Nivel $Level)"
        Write-Host "X NO ENCONTRADO: $FilePath (Nivel $Level)" -ForegroundColor Red
        return $false
    }
    
    if (-not (Test-Path $baselineFile)) {
        Write-Log "WARN: No existe baseline para $FilePath - Generando..."
        New-Baseline -FilePath $FilePath -Level $Level
        return $true
    }
    
    $currentHash = Get-FileHashSHA256 -FilePath $fullPath
    $baselineContent = Get-Content $baselineFile
    $baselineHash = $baselineContent[0]
    
    if ($currentHash -ne $baselineHash) {
        Write-Alert "Archivo MODIFICADO: $FilePath (Nivel $Level)"
        Write-Alert "  Hash esperado: $baselineHash"
        Write-Alert "  Hash actual:   $currentHash"
        Write-Host "X MODIFICADO: $FilePath (Nivel $Level)" -ForegroundColor Red
        return $false
    } else {
        Write-Host "OK: $FilePath" -ForegroundColor Green
        return $true
    }
}

# Funcion para generar todos los baselines
function New-AllBaselines {
    Write-Log "=== Generando baselines de archivos criticos ==="
    
    Write-Host "Generando baselines Nivel 1 (Seguridad)..." -ForegroundColor Cyan
    foreach ($file in $CriticalLevel1) {
        New-Baseline -FilePath $file -Level 1
    }
    
    Write-Host "Generando baselines Nivel 2 (Logica de negocio)..." -ForegroundColor Cyan
    foreach ($file in $CriticalLevel2) {
        New-Baseline -FilePath $file -Level 2
    }
    
    Write-Log "Baselines generados exitosamente"
    Write-Host "`nBaselines generados en: $BaselineDir" -ForegroundColor Green
}

# Funcion para verificar todos los archivos
function Test-AllIntegrity {
    Write-Log "=== Iniciando verificacion de integridad ==="
    
    $total = 0
    $ok = 0
    $modified = 0
    $missing = 0
    
    Write-Host "`n=== NIVEL 1: Seguridad y Autenticacion ===" -ForegroundColor Cyan
    foreach ($file in $CriticalLevel1) {
        $total++
        if (Test-Integrity -FilePath $file -Level 1) {
            $ok++
        } else {
            if (Test-Path (Join-Path $ProjectRoot $file)) {
                $modified++
            } else {
                $missing++
            }
        }
    }
    
    Write-Host "`n=== NIVEL 2: Logica de Negocio ===" -ForegroundColor Cyan
    foreach ($file in $CriticalLevel2) {
        $total++
        if (Test-Integrity -FilePath $file -Level 2) {
            $ok++
        } else {
            if (Test-Path (Join-Path $ProjectRoot $file)) {
                $modified++
            } else {
                $missing++
            }
        }
    }
    
    Write-Host "`n=== RESUMEN DE VERIFICACION ===" -ForegroundColor Cyan
    Write-Host "Total de archivos verificados: $total"
    Write-Host "Archivos OK: $ok" -ForegroundColor Green
    
    if ($modified -gt 0) {
        Write-Host "Archivos modificados: $modified" -ForegroundColor Red
    }
    
    if ($missing -gt 0) {
        Write-Host "Archivos faltantes: $missing" -ForegroundColor Red
    }
    
    if ($modified -eq 0 -and $missing -eq 0) {
        Write-Log "Verificacion completada - Sin cambios detectados"
        Write-Host "`nIntegridad verificada - Sistema seguro" -ForegroundColor Green
        return $true
    } else {
        Write-Log "Verificacion completada - $modified modificados, $missing faltantes"
        Write-Host "`nALERTA: Se detectaron cambios en archivos criticos" -ForegroundColor Red
        Write-Host "Revisar: $AlertFile" -ForegroundColor Yellow
        return $false
    }
}

# Funcion para mostrar estadisticas
function Show-Stats {
    Write-Host "=== ESTADISTICAS DE MONITOREO ===" -ForegroundColor Cyan
    Write-Host "Archivos criticos monitoreados:"
    Write-Host "  - Nivel 1 (Seguridad): $($CriticalLevel1.Count)"
    Write-Host "  - Nivel 2 (Logica): $($CriticalLevel2.Count)"
    Write-Host "  - Total: $($CriticalLevel1.Count + $CriticalLevel2.Count)"
    Write-Host ""
    Write-Host "Ubicacion de baselines: $BaselineDir"
    Write-Host "Log de verificaciones: $LogFile"
    Write-Host "Log de alertas: $AlertFile"
}

# Funcion de ayuda
function Show-Help {
    Write-Host "Uso: .\monitor-critical-files.ps1 [comando]" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Comandos:"
    Write-Host "  generate  - Generar baselines de todos los archivos criticos"
    Write-Host "  verify    - Verificar integridad de archivos (por defecto)"
    Write-Host "  stats     - Mostrar estadisticas de monitoreo"
    Write-Host "  list      - Listar archivos criticos monitoreados"
    Write-Host "  help      - Mostrar esta ayuda"
}

# Ejecutar comando
switch ($Command) {
    'generate' { New-AllBaselines }
    'verify' { Test-AllIntegrity }
    'stats' { Show-Stats }
    'help' { Show-Help }
    default { 
        Write-Host "Comando desconocido: $Command" -ForegroundColor Red
        Write-Host "Use '.\monitor-critical-files.ps1 help' para ver comandos disponibles"
    }
}
