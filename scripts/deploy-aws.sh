#!/bin/bash
# deploy-aws.sh
# Script automatizado de despliegue en AWS

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuración
APP_NAME="geros"
APP_DIR="/opt/$APP_NAME"
BACKEND_DIR="$APP_DIR/backend"
FRONTEND_DIR="$APP_DIR/frontend"
CONFIG_DIR="$APP_DIR/config"
LOGS_DIR="$APP_DIR/logs"
STORAGE_DIR="$APP_DIR/storage"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  GEROS - Despliegue Automatizado AWS  ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Función para imprimir mensajes
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar si se ejecuta como root
if [ "$EUID" -ne 0 ]; then 
    log_error "Este script debe ejecutarse como root (sudo)"
    exit 1
fi

# Paso 1: Verificar requisitos
log_info "Verificando requisitos del sistema..."

# Verificar Java
if ! command -v java &> /dev/null; then
    log_error "Java no está instalado"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    log_error "Se requiere Java 21 o superior (actual: $JAVA_VERSION)"
    exit 1
fi
log_info "Java $JAVA_VERSION detectado ✓"

# Verificar PostgreSQL
if ! command -v psql &> /dev/null; then
    log_error "PostgreSQL no está instalado"
    exit 1
fi
log_info "PostgreSQL detectado ✓"

# Verificar Nginx
if ! command -v nginx &> /dev/null; then
    log_error "Nginx no está instalado"
    exit 1
fi
log_info "Nginx detectado ✓"

# Verificar Node.js
if ! command -v node &> /dev/null; then
    log_error "Node.js no está instalado"
    exit 1
fi
log_info "Node.js $(node -v) detectado ✓"

echo ""

# Paso 2: Crear estructura de directorios
log_info "Creando estructura de directorios..."

mkdir -p $APP_DIR
mkdir -p $BACKEND_DIR
mkdir -p $FRONTEND_DIR
mkdir -p $CONFIG_DIR
mkdir -p $LOGS_DIR
mkdir -p $STORAGE_DIR/security-log-exports
mkdir -p $APP_DIR/backups
mkdir -p $APP_DIR/scripts

log_info "Directorios creados ✓"
echo ""

# Paso 3: Crear usuario de aplicación
log_info "Configurando usuario de aplicación..."

if ! id -u $APP_NAME &> /dev/null; then
    useradd -r -s /bin/false $APP_NAME
    log_info "Usuario $APP_NAME creado ✓"
else
    log_info "Usuario $APP_NAME ya existe ✓"
fi

# Dar permisos
chown -R $APP_NAME:$APP_NAME $APP_DIR
log_info "Permisos configurados ✓"
echo ""

# Paso 4: Configurar base de datos
log_info "Configurando base de datos..."

read -p "¿Desea configurar la base de datos PostgreSQL? (s/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Ss]$ ]]; then
    read -p "Ingrese la contraseña para el usuario geros_user: " DB_PASSWORD
    
    sudo -u postgres psql << EOF
CREATE DATABASE geros;
CREATE USER geros_user WITH ENCRYPTED PASSWORD '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON DATABASE geros TO geros_user;
\c geros
GRANT ALL ON SCHEMA public TO geros_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO geros_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO geros_user;
EOF
    
    log_info "Base de datos configurada ✓"
else
    log_warn "Configuración de base de datos omitida"
    DB_PASSWORD="CHANGE_ME"
fi
echo ""

# Paso 5: Generar secretos
log_info "Generando secretos..."

JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
SSL_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')

log_info "Secretos generados ✓"
echo ""

# Paso 6: Crear archivo de variables de entorno
log_info "Creando archivo de configuración..."

cat > $CONFIG_DIR/geros.env << EOF
# Database
DB_PASSWORD=$DB_PASSWORD

# JWT Secret
JWT_SECRET=$JWT_SECRET

# SSL Keystore
SSL_KEYSTORE_PASSWORD=$SSL_PASSWORD

# reCaptcha (cambiar en producción)
RECAPTCHA_SECRET=6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
EOF

chmod 600 $CONFIG_DIR/geros.env
chown $APP_NAME:$APP_NAME $CONFIG_DIR/geros.env

log_info "Archivo de configuración creado ✓"
echo ""

# Paso 7: Crear application-prod.properties
log_info "Creando application-prod.properties..."

cat > $CONFIG_DIR/application-prod.properties << 'EOF'
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/geros
spring.datasource.username=geros_user
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.jdbc.time_zone=America/Bogota

# JWT
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000

# reCaptcha
recaptcha.secret=${RECAPTCHA_SECRET}
recaptcha.min-score=0.5

# Timezone
app.timezone=America/Bogota

# Security Logs
app.security-log-export-dir=/opt/geros/storage/security-log-exports
app.security-log.read-only=true
app.security-log.online-retention-days=365

# Security Alerts
app.security-alert.enabled=true
app.security-alert.email-recipients=admin@promigas.com
app.security-alert.actions=LOGIN_FAILED,ACCOUNT_LOCKED,PRIVILEGED_USER_ACTIVITY

# Auth
app.auth.max-concurrent-sessions=5

# HTTPS
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=file:/opt/geros/config/geros-keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=geros
server.ssl.protocol=TLS
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3

# Logging
logging.level.root=INFO
logging.level.com.geros.backend=INFO
logging.file.name=/opt/geros/logs/application.log
logging.file.max-size=10MB
logging.file.max-history=30

# Database initialization
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:db/init.sql
EOF

chown $APP_NAME:$APP_NAME $CONFIG_DIR/application-prod.properties

log_info "application-prod.properties creado ✓"
echo ""

# Paso 8: Copiar archivos de aplicación
log_info "Copiando archivos de aplicación..."

if [ -f "backend/target/backend-0.0.1-SNAPSHOT.jar" ]; then
    cp backend/target/backend-0.0.1-SNAPSHOT.jar $BACKEND_DIR/
    chown $APP_NAME:$APP_NAME $BACKEND_DIR/backend-0.0.1-SNAPSHOT.jar
    log_info "Backend JAR copiado ✓"
else
    log_warn "Backend JAR no encontrado. Compilar con: ./mvnw clean package"
fi

if [ -d "frontend/dist" ]; then
    cp -r frontend/dist/* $FRONTEND_DIR/
    chown -R $APP_NAME:$APP_NAME $FRONTEND_DIR
    log_info "Frontend copiado ✓"
else
    log_warn "Frontend build no encontrado. Compilar con: npm run build"
fi

echo ""

# Paso 9: Crear servicio systemd
log_info "Creando servicio systemd..."

cat > /etc/systemd/system/geros-backend.service << EOF
[Unit]
Description=GEROS Backend Application
After=postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=$APP_NAME
Group=$APP_NAME
WorkingDirectory=$BACKEND_DIR

EnvironmentFile=$CONFIG_DIR/geros.env

ExecStart=/usr/bin/java \\
    -Xms512m \\
    -Xmx2048m \\
    -Dspring.profiles.active=prod \\
    -Dspring.config.location=file:$CONFIG_DIR/application-prod.properties \\
    -jar $BACKEND_DIR/backend-0.0.1-SNAPSHOT.jar

Restart=always
RestartSec=10

StandardOutput=journal
StandardError=journal
SyslogIdentifier=geros-backend

NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable geros-backend

log_info "Servicio systemd creado ✓"
echo ""

# Paso 10: Configurar Nginx
log_info "Configurando Nginx..."

read -p "Ingrese el dominio (ej: geros.promigas.com): " DOMAIN

cat > /etc/nginx/sites-available/geros << EOF
server {
    listen 80;
    server_name $DOMAIN;
    return 301 https://\$server_name\$request_uri;
}

server {
    listen 443 ssl http2;
    server_name $DOMAIN;

    ssl_certificate $CONFIG_DIR/geros-certificate.crt;
    ssl_certificate_key $CONFIG_DIR/geros-private.key;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'TLS_AES_256_GCM_SHA384:TLS_AES_128_GCM_SHA256:ECDHE-RSA-AES256-GCM-SHA384';
    ssl_prefer_server_ciphers on;

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;

    root $FRONTEND_DIR;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass https://localhost:8443;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_ssl_verify off;
    }

    access_log /var/log/nginx/geros-access.log;
    error_log /var/log/nginx/geros-error.log;
}
EOF

ln -sf /etc/nginx/sites-available/geros /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

nginx -t && systemctl restart nginx

log_info "Nginx configurado ✓"
echo ""

# Paso 11: Crear scripts de mantenimiento
log_info "Creando scripts de mantenimiento..."

# Script de backup
cat > $APP_DIR/scripts/backup-db.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/geros/backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/geros_backup_$DATE.sql"

mkdir -p $BACKUP_DIR
sudo -u postgres pg_dump geros > $BACKUP_FILE
gzip $BACKUP_FILE
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "Backup completado: $BACKUP_FILE.gz"
EOF

chmod +x $APP_DIR/scripts/backup-db.sh

log_info "Scripts de mantenimiento creados ✓"
echo ""

# Paso 12: Iniciar servicios
log_info "Iniciando servicios..."

read -p "¿Desea iniciar el backend ahora? (s/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Ss]$ ]]; then
    systemctl start geros-backend
    sleep 5
    
    if systemctl is-active --quiet geros-backend; then
        log_info "Backend iniciado correctamente ✓"
    else
        log_error "Error al iniciar backend. Ver logs: journalctl -u geros-backend -n 50"
    fi
fi

echo ""

# Resumen
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Despliegue Completado  ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${GREEN}Información importante:${NC}"
echo ""
echo "Directorios:"
echo "  - Aplicación: $APP_DIR"
echo "  - Configuración: $CONFIG_DIR"
echo "  - Logs: $LOGS_DIR"
echo ""
echo "Servicios:"
echo "  - Backend: sudo systemctl status geros-backend"
echo "  - Nginx: sudo systemctl status nginx"
echo "  - PostgreSQL: sudo systemctl status postgresql"
echo ""
echo "Logs:"
echo "  - Backend: sudo journalctl -u geros-backend -f"
echo "  - Nginx: sudo tail -f /var/log/nginx/geros-error.log"
echo ""
echo "Acceso:"
echo "  - URL: https://$DOMAIN"
echo "  - Usuario: admin"
echo "  - Contraseña: admin123"
echo ""
echo -e "${YELLOW}IMPORTANTE:${NC}"
echo "1. Copiar certificado SSL a: $CONFIG_DIR/geros-certificate.crt"
echo "2. Copiar llave privada a: $CONFIG_DIR/geros-private.key"
echo "3. Copiar keystore a: $CONFIG_DIR/geros-keystore.p12"
echo "4. Cambiar contraseña del usuario admin después del primer login"
echo "5. Configurar backup automático: sudo crontab -e"
echo "   Agregar: 0 2 * * * $APP_DIR/scripts/backup-db.sh"
echo ""
echo -e "${GREEN}¡Despliegue completado exitosamente!${NC}"
