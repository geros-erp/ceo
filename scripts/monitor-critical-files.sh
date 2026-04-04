#!/bin/bash
# monitor-critical-files.sh
# Script de monitoreo de integridad de archivos críticos

# Configuración
BASELINE_DIR="./baselines"
LOG_FILE="./file-integrity-monitor.log"
ALERT_FILE="./integrity-alerts.log"
PROJECT_ROOT="."

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Función de logging
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

alert() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ALERT: $1" | tee -a "$ALERT_FILE"
}

# Crear directorios si no existen
mkdir -p "$BASELINE_DIR"

# Lista de archivos críticos - NIVEL 1 (Seguridad)
CRITICAL_LEVEL_1=(
    "backend/src/main/java/com/geros/backend/config/SecurityConfig.java"
    "backend/src/main/java/com/geros/backend/security/JwtFilter.java"
    "backend/src/main/java/com/geros/backend/security/JwtService.java"
    "backend/src/main/java/com/geros/backend/security/AuthController.java"
    "backend/src/main/java/com/geros/backend/security/AuthenticationValidator.java"
    "backend/src/main/java/com/geros/backend/security/SecurityHeadersFilter.java"
    "backend/src/main/java/com/geros/backend/security/InputValidationService.java"
    "backend/src/main/java/com/geros/backend/security/OutputSanitizationService.java"
    "backend/src/main/java/com/geros/backend/security/DataIntegrityService.java"
    "backend/src/main/resources/application.properties"
    "backend/pom.xml"
    "frontend/src/context/AuthContext.jsx"
    "frontend/src/api/auth.js"
    "frontend/src/App.jsx"
)

# Lista de archivos críticos - NIVEL 2 (Lógica de negocio)
CRITICAL_LEVEL_2=(
    "backend/src/main/java/com/geros/backend/user/UserService.java"
    "backend/src/main/java/com/geros/backend/user/User.java"
    "backend/src/main/java/com/geros/backend/role/RoleService.java"
    "backend/src/main/java/com/geros/backend/role/Role.java"
    "backend/src/main/java/com/geros/backend/policy/PasswordPolicyService.java"
    "backend/src/main/java/com/geros/backend/policy/PasswordPolicy.java"
    "backend/src/main/java/com/geros/backend/security/SecurityLogService.java"
    "backend/src/main/java/com/geros/backend/security/SecurityLog.java"
)

# Lista de archivos críticos - NIVEL 3 (Base de datos)
CRITICAL_LEVEL_3=(
    "backend/src/main/resources/db/init.sql"
    "backend/src/main/resources/db/migration/V1__add_default_user_flag.sql"
    "backend/src/main/java/com/geros/backend/user/UserRepository.java"
    "backend/src/main/java/com/geros/backend/role/RoleRepository.java"
)

# Función para generar baseline
generate_baseline() {
    local file=$1
    local level=$2
    local baseline_file="$BASELINE_DIR/$(echo $file | tr '/' '_').sha256"
    
    if [ -f "$PROJECT_ROOT/$file" ]; then
        sha256sum "$PROJECT_ROOT/$file" > "$baseline_file"
        echo "$level" >> "$baseline_file"
        log "Baseline generado: $file (Nivel $level)"
    else
        log "WARN: Archivo no encontrado: $file"
    fi
}

# Función para verificar integridad
check_integrity() {
    local file=$1
    local level=$2
    local baseline_file="$BASELINE_DIR/$(echo $file | tr '/' '_').sha256"
    
    if [ ! -f "$PROJECT_ROOT/$file" ]; then
        alert "Archivo crítico NO ENCONTRADO: $file (Nivel $level)"
        return 1
    fi
    
    if [ ! -f "$baseline_file" ]; then
        log "WARN: No existe baseline para $file - Generando..."
        generate_baseline "$file" "$level"
        return 0
    fi
    
    current_hash=$(sha256sum "$PROJECT_ROOT/$file" | awk '{print $1}')
    baseline_hash=$(head -n 1 "$baseline_file" | awk '{print $1}')
    
    if [ "$current_hash" != "$baseline_hash" ]; then
        alert "Archivo MODIFICADO: $file (Nivel $level)"
        alert "  Hash esperado: $baseline_hash"
        alert "  Hash actual:   $current_hash"
        echo -e "${RED}✗ MODIFICADO${NC}: $file (Nivel $level)"
        return 1
    else
        echo -e "${GREEN}✓ OK${NC}: $file"
        return 0
    fi
}

# Función para generar todos los baselines
generate_all_baselines() {
    log "=== Generando baselines de archivos críticos ==="
    
    echo "Generando baselines Nivel 1 (Seguridad)..."
    for file in "${CRITICAL_LEVEL_1[@]}"; do
        generate_baseline "$file" "1"
    done
    
    echo "Generando baselines Nivel 2 (Lógica de negocio)..."
    for file in "${CRITICAL_LEVEL_2[@]}"; do
        generate_baseline "$file" "2"
    done
    
    echo "Generando baselines Nivel 3 (Base de datos)..."
    for file in "${CRITICAL_LEVEL_3[@]}"; do
        generate_baseline "$file" "3"
    done
    
    log "Baselines generados exitosamente"
    echo -e "${GREEN}✓ Baselines generados en: $BASELINE_DIR${NC}"
}

# Función para verificar todos los archivos
verify_all() {
    log "=== Iniciando verificación de integridad ==="
    
    local total=0
    local modified=0
    local missing=0
    
    echo ""
    echo "=== NIVEL 1: Seguridad y Autenticación ==="
    for file in "${CRITICAL_LEVEL_1[@]}"; do
        total=$((total + 1))
        check_integrity "$file" "1"
        result=$?
        if [ $result -ne 0 ]; then
            if [ -f "$PROJECT_ROOT/$file" ]; then
                modified=$((modified + 1))
            else
                missing=$((missing + 1))
            fi
        fi
    done
    
    echo ""
    echo "=== NIVEL 2: Lógica de Negocio ==="
    for file in "${CRITICAL_LEVEL_2[@]}"; do
        total=$((total + 1))
        check_integrity "$file" "2"
        result=$?
        if [ $result -ne 0 ]; then
            if [ -f "$PROJECT_ROOT/$file" ]; then
                modified=$((modified + 1))
            else
                missing=$((missing + 1))
            fi
        fi
    done
    
    echo ""
    echo "=== NIVEL 3: Base de Datos ==="
    for file in "${CRITICAL_LEVEL_3[@]}"; do
        total=$((total + 1))
        check_integrity "$file" "3"
        result=$?
        if [ $result -ne 0 ]; then
            if [ -f "$PROJECT_ROOT/$file" ]; then
                modified=$((modified + 1))
            else
                missing=$((missing + 1))
            fi
        fi
    done
    
    echo ""
    echo "=== RESUMEN DE VERIFICACIÓN ==="
    echo "Total de archivos verificados: $total"
    echo -e "Archivos OK: ${GREEN}$((total - modified - missing))${NC}"
    
    if [ $modified -gt 0 ]; then
        echo -e "Archivos modificados: ${RED}$modified${NC}"
    fi
    
    if [ $missing -gt 0 ]; then
        echo -e "Archivos faltantes: ${RED}$missing${NC}"
    fi
    
    if [ $modified -eq 0 ] && [ $missing -eq 0 ]; then
        log "✓ Verificación completada - Sin cambios detectados"
        echo -e "${GREEN}✓ Integridad verificada - Sistema seguro${NC}"
        return 0
    else
        log "✗ Verificación completada - $modified modificados, $missing faltantes"
        echo -e "${RED}✗ ALERTA: Se detectaron cambios en archivos críticos${NC}"
        echo -e "${YELLOW}Revisar: $ALERT_FILE${NC}"
        return 1
    fi
}

# Función para mostrar estadísticas
show_stats() {
    echo "=== ESTADÍSTICAS DE MONITOREO ==="
    echo "Archivos críticos monitoreados:"
    echo "  - Nivel 1 (Seguridad): ${#CRITICAL_LEVEL_1[@]}"
    echo "  - Nivel 2 (Lógica): ${#CRITICAL_LEVEL_2[@]}"
    echo "  - Nivel 3 (Base de datos): ${#CRITICAL_LEVEL_3[@]}"
    echo "  - Total: $((${#CRITICAL_LEVEL_1[@]} + ${#CRITICAL_LEVEL_2[@]} + ${#CRITICAL_LEVEL_3[@]}))"
    echo ""
    echo "Ubicación de baselines: $BASELINE_DIR"
    echo "Log de verificaciones: $LOG_FILE"
    echo "Log de alertas: $ALERT_FILE"
}

# Función para listar archivos críticos
list_files() {
    echo "=== ARCHIVOS CRÍTICOS MONITOREADOS ==="
    echo ""
    echo "NIVEL 1 - Seguridad y Autenticación:"
    for file in "${CRITICAL_LEVEL_1[@]}"; do
        echo "  - $file"
    done
    echo ""
    echo "NIVEL 2 - Lógica de Negocio:"
    for file in "${CRITICAL_LEVEL_2[@]}"; do
        echo "  - $file"
    done
    echo ""
    echo "NIVEL 3 - Base de Datos:"
    for file in "${CRITICAL_LEVEL_3[@]}"; do
        echo "  - $file"
    done
}

# Función para actualizar baseline de un archivo específico
update_baseline() {
    local file=$1
    
    if [ -z "$file" ]; then
        echo "Error: Debe especificar un archivo"
        echo "Uso: $0 update <archivo>"
        return 1
    fi
    
    if [ ! -f "$PROJECT_ROOT/$file" ]; then
        echo "Error: Archivo no encontrado: $file"
        return 1
    fi
    
    local baseline_file="$BASELINE_DIR/$(echo $file | tr '/' '_').sha256"
    sha256sum "$PROJECT_ROOT/$file" > "$baseline_file"
    
    log "Baseline actualizado: $file"
    echo -e "${GREEN}✓ Baseline actualizado: $file${NC}"
}

# Menú principal
case "${1:-verify}" in
    generate)
        generate_all_baselines
        ;;
    verify)
        verify_all
        ;;
    stats)
        show_stats
        ;;
    list)
        list_files
        ;;
    update)
        update_baseline "$2"
        ;;
    help)
        echo "Uso: $0 [comando]"
        echo ""
        echo "Comandos:"
        echo "  generate  - Generar baselines de todos los archivos críticos"
        echo "  verify    - Verificar integridad de archivos (por defecto)"
        echo "  stats     - Mostrar estadísticas de monitoreo"
        echo "  list      - Listar archivos críticos monitoreados"
        echo "  update    - Actualizar baseline de un archivo específico"
        echo "  help      - Mostrar esta ayuda"
        echo ""
        echo "Ejemplos:"
        echo "  $0 generate"
        echo "  $0 verify"
        echo "  $0 update backend/src/main/resources/application.properties"
        ;;
    *)
        echo "Comando desconocido: $1"
        echo "Use '$0 help' para ver comandos disponibles"
        exit 1
        ;;
esac

exit $?
