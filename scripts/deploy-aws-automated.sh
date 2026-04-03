#!/bin/bash
################################################################################
# Script de Despliegue Automatizado GEROS en AWS EC2
# Amazon Linux 2023 - Instancia específica
# AMI: al2023-ami-2023.10.20260330.0-kernel-6.1-x86_64
# Volumen: vol-012cb82d4e61d059f (8 GiB - expandir a 30 GiB antes de ejecutar)
################################################################################

set -e  # Detener en caso de error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para imprimir mensajes
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar que se ejecuta como ec2-user
if [ "$USER" != "ec2-user" ]; then
    print_error "Este script debe ejecutarse como usuario ec2-user"
    exit 1
fi

print_info "=== Iniciando Despliegue de GEROS en AWS EC2 ==="
print_info "Amazon Linux 2023 - $(date)"

# Verificar espacio en disco
DISK_SPACE=$(df -h / | awk 'NR==2 {print $4}' | sed 's/G//')
if (( $(echo "$DISK_SPACE < 20" | bc -l) )); then
    print_error "Espacio insuficiente en disco: ${DISK_SPACE}G disponible"
    print_error "Se requiere mínimo 20 GB libres. Expandir volumen EBS primero."
    exit 1
fi
print_success "Espacio en disco verificado: ${DISK_SPACE}G disponible"

# Paso 1: Actualizar sistema
print_info "Paso 1/10: Actualizando sistema..."
sudo dnf update -y > /dev/null 2>&1
sudo dnf install -y git wget curl nano htop bc > /dev/null 2>&1
print_success "Sistema actualizado"

# Paso 2: Instalar Java 21
print_info "Paso 2/10: Instalando Java 21 (Amazon Corretto)..."
if ! command -v java &> /dev/null; then
    sudo dnf install -y java-21-amazon-corretto-devel > /dev/null 2>&1
    echo 'export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto' >> ~/.bashrc
    echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
    source ~/.bashrc
fi
JAVA_VERSION=$(java -version 2>&1 | head -n 1)
print_success "Java instalado: $JAVA_VERSION"

# Paso 3: Instalar PostgreSQL 15
print_info "Paso 3/10: Instalando PostgreSQL 15..."
if ! command -v psql &> /dev/null; then
    sudo dnf install -y postgresql15-server postgresql15-contrib > /dev/null 2>&1
    sudo postgresql-setup --initdb > /dev/null 2>&1
    sudo systemctl start postgresql
    sudo systemctl enable postgresql > /dev/null 2>&1
fi
print_success "PostgreSQL 15 instalado y en ejecución"

# Paso 4: Instalar Nginx
print_info "Paso 4/10: Instalando Nginx..."
if ! command -v nginx &> /dev/null; then
    sudo dnf install -y nginx > /dev/null 2>&1
    sudo systemctl start nginx
    sudo systemctl enable nginx > /dev/null 2>&1
fi
print_success "Nginx instalado y en ejecución"

# Paso 5: Instalar Node.js y Maven
print_info "Paso 5/10: Instalando Node.js y Maven..."
if ! command -v node &> /dev/null; then
    sudo dnf install -y nodejs npm > /dev/null 2>&1
fi
if ! command -v mvn &> /dev/null; then
    sudo dnf install -y maven > /dev/null 2>&1
fi
NODE_VERSION=$(node -v)
MAVEN_VERSION=$(mvn -version | head -n 1)
print_success "Node.js $NODE_VERSION y Maven instalados"

# Paso 6: Configurar PostgreSQL
print_info "Paso 6/10: Configurando PostgreSQL..."

# Solicitar contraseña para base de datos
read -sp "Ingrese contraseña para usuario de base de datos 'geros_user': " DB_PASSWORD
echo
read -sp "Confirme la contraseña: " DB_PASSWORD_CONFIRM
echo

if [ "$DB_PASSWORD" != "$DB_PASSWORD_CONFIRM" ]; then
    print_error "Las contraseñas no coinciden"
    exit 1
fi

# Crear base de datos y usuario
sudo -u postgres psql << EOF > /dev/null 2>&1
CREATE DATABASE geros;
CREATE USER geros_user WITH ENCRYPTED PASSWORD '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON DATABASE geros TO geros_user;
\c geros
GRANT ALL ON SCHEMA public TO geros_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO geros_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO geros_user;
EOF

# Configurar pg_hba.conf
sudo bash -c 'cat > /var/lib/pgsql/data/pg_hba.conf << EOF
# TYPE  DATABASE        USER            ADDRESS                 METHOD
local   all             all                                     peer
host    all             all             127.0.0.1/32            scram-sha-256
host    all             all             ::1/128                 scram-sha-256
EOF'

# Configurar postgresql.conf
sudo sed -i "s/#listen_addresses = 'localhost'/listen_addresses = 'localhost'/" /var/lib/pgsql/data/postgresql.conf

sudo systemctl restart postgresql
print_success "PostgreSQL configurado con base de datos 'geros'"

# Paso 7: Crear estructura de directorios
print_info "Paso 7/10: Creando estructura de directorios..."
sudo mkdir -p /opt/geros/{backend,frontend,config,logs,storage/security-log-exports,backups,scripts}
sudo useradd -r -s /bin/false geros 2>/dev/null || true
sudo chown -R geros:geros /opt/geros
print_success "Estructura de directorios creada"

# Paso 8: Generar secretos
print_info "Paso 8/10: Generando secretos de seguridad..."

# Generar JWT Secret
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')

# Generar SSL Keystore Password
SSL_KEYSTORE_PASSWORD=$(openssl rand -base64 32 | tr -d '\n')

# Solicitar reCAPTCHA secret
read -p "Ingrese reCAPTCHA Secret Key (dejar vacío para usar valor de prueba): " RECAPTCHA_SECRET
if [ -z "$RECAPTCHA_SECRET" ]; then
    RECAPTCHA_SECRET="6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe"
    print_warning "Usando reCAPTCHA Secret de prueba (cambiar en producción)"
fi

# Crear archivo de variables de entorno
sudo bash -c "cat > /opt/geros/config/geros.env << EOF
# Database
DB_PASSWORD=$DB_PASSWORD

# JWT Secret
JWT_SECRET=$JWT_SECRET

# SSL Keystore
SSL_KEYSTORE_PASSWORD=$SSL_KEYSTORE_PASSWORD

# reCaptcha
RECAPTCHA_SECRET=$RECAPTCHA_SECRET

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
EOF"

sudo chmod 600 /opt/geros/config/geros.env
sudo chown geros:geros /opt/geros/config/geros.env
print_success "Secretos generados y almacenados"

# Paso 9: Generar certificado SSL autofirmado
print_info "Paso 9/10: Generando certificado SSL autofirmado (4096 bits)..."

# Solicitar información del certificado
read -p "Ingrese nombre de dominio o IP pública: " DOMAIN_NAME
if [ -z "$DOMAIN_NAME" ]; then
    DOMAIN_NAME="localhost"
fi

# Generar certificado
sudo openssl req -x509 -newkey rsa:4096 -sha256 -days 365 \
    -nodes -keyout /tmp/geros-private.key \
    -out /tmp/geros-certificate.crt \
    -subj "/C=CO/ST=Atlantico/L=Barranquilla/O=Promigas/OU=IT/CN=$DOMAIN_NAME" \
    -addext "subjectAltName=DNS:$DOMAIN_NAME,DNS:localhost,IP:127.0.0.1" \
    > /dev/null 2>&1

# Crear keystore PKCS12
sudo openssl pkcs12 -export \
    -in /tmp/geros-certificate.crt \
    -inkey /tmp/geros-private.key \
    -out /opt/geros/config/geros-keystore.p12 \
    -name geros \
    -passout pass:$SSL_KEYSTORE_PASSWORD \
    > /dev/null 2>&1

# Copiar certificados para Nginx
sudo cp /tmp/geros-certificate.crt /opt/geros/config/
sudo cp /tmp/geros-private.key /opt/geros/config/
sudo rm /tmp/geros-*.{crt,key}

sudo chmod 600 /opt/geros/config/geros-keystore.p12
sudo chmod 600 /opt/geros/config/geros-private.key
sudo chown geros:geros /opt/geros/config/geros-*
print_success "Certificado SSL generado para: $DOMAIN_NAME"

# Paso 10: Crear configuración de aplicación
print_info "Paso 10/10: Creando configuración de aplicación..."

sudo bash -c "cat > /opt/geros/config/application-prod.properties << 'EOF'
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/geros
spring.datasource.username=geros_user
spring.datasource.password=\${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.jdbc.time_zone=America/Bogota

# JWT
jwt.secret=\${JWT_SECRET}
jwt.expiration=86400000

# reCaptcha
recaptcha.secret=\${RECAPTCHA_SECRET}
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
app.security-alert.webhook-urls=
app.security-alert.actions=LOGIN_FAILED,ACCOUNT_LOCKED,PRIVILEGED_USER_ACTIVITY
app.security-alert.allowed-login-start=06:00
app.security-alert.allowed-login-end=22:00

# Auth
app.auth.max-concurrent-sessions=5

# HTTPS
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=file:/opt/geros/config/geros-keystore.p12
server.ssl.key-store-password=\${SSL_KEYSTORE_PASSWORD}
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
EOF"

sudo chown geros:geros /opt/geros/config/application-prod.properties
print_success "Configuración de aplicación creada"

# Resumen
print_success "=== Instalación Base Completada ==="
echo ""
print_info "Próximos pasos:"
echo "1. Transferir código fuente a /opt/geros/"
echo "2. Compilar backend: cd /opt/geros/backend && mvn clean package"
echo "3. Compilar frontend: cd /opt/geros/frontend && npm install && npm run build"
echo "4. Crear servicio systemd para backend"
echo "5. Configurar Nginx"
echo "6. Configurar Security Group de AWS"
echo ""
print_info "Archivos de configuración:"
echo "  - Variables de entorno: /opt/geros/config/geros.env"
echo "  - Configuración app: /opt/geros/config/application-prod.properties"
echo "  - Certificado SSL: /opt/geros/config/geros-keystore.p12"
echo ""
print_info "Credenciales generadas:"
echo "  - DB Password: [guardado en geros.env]"
echo "  - JWT Secret: [guardado en geros.env]"
echo "  - SSL Keystore Password: [guardado en geros.env]"
echo ""
print_warning "IMPORTANTE: Guardar archivo /opt/geros/config/geros.env en lugar seguro"
print_warning "Contiene credenciales sensibles del sistema"
