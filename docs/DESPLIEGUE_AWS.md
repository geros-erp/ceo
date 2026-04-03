# Guía de Despliegue en AWS EC2

## Información de la Instancia EC2

### Especificaciones Técnicas
- **AMI**: Amazon Linux 2023 (al2023-ami-2023.10.20260330.0-kernel-6.1-x86_64)
- **AMI ID**: ami-01b14b7ad41e17ba4
- **Plataforma**: Linux/UNIX
- **Kernel**: 6.1
- **Arquitectura**: x86_64
- **Par de claves**: llave-ceo
- **Modo de arranque**: UEFI
- **Fecha de lanzamiento**: 02/04/2026 18:56:14 GMT-5

### Almacenamiento
- **Dispositivo raíz**: /dev/xvda
- **Tipo**: EBS
- **Volumen ID**: vol-012cb82d4e61d059f
- **Tamaño**: 8 GiB (⚠️ **RECOMENDACIÓN**: Expandir a mínimo 30 GiB)
- **Estado**: En uso
- **Optimización EBS**: Habilitada

### Configuración
- **Especificación de crédito**: Unlimited
- **Monitoreo**: Desactivado (⚠️ **RECOMENDACIÓN**: Activar CloudWatch)
- **Protección de terminación**: Desactivada (⚠️ **RECOMENDACIÓN**: Activar en producción)

## ⚠️ ACCIONES PREVIAS REQUERIDAS

### 1. Expandir Volumen EBS (CRÍTICO)

El volumen actual de 8 GiB es insuficiente. Se requiere mínimo 30 GiB.

**Pasos en AWS Console:**
1. EC2 → Volumes → Seleccionar `vol-012cb82d4e61d059f`
2. Actions → Modify Volume
3. Cambiar Size a `30` GiB
4. Modify

**Expandir sistema de archivos en la instancia:**
```bash
# Conectar a la instancia
ssh -i "llave-ceo.pem" ec2-user@tu-ip-publica

# Verificar particiones
lsblk

# Expandir partición (Amazon Linux 2023)
sudo growpart /dev/xvda 1

# Expandir sistema de archivos
sudo xfs_growfs -d /

# Verificar espacio
df -h
```

### 2. Activar Monitoreo CloudWatch

**En AWS Console:**
1. EC2 → Instances → Seleccionar instancia
2. Actions → Monitor and troubleshoot → Manage detailed monitoring
3. Enable → Confirm

### 3. Activar Protección de Terminación

**En AWS Console:**
1. EC2 → Instances → Seleccionar instancia
2. Actions → Instance settings → Change termination protection
3. Enable → Save

## Arquitectura de Despliegue

```
Internet
    ↓
[AWS Security Group]
    ↓
[Nginx :80/:443] → Reverse Proxy
    ↓
[Spring Boot :8443] → Backend (HTTPS)
    ↓
[PostgreSQL :5432] → Base de datos
```

## Paso 1: Conectar y Preparar Servidor Amazon Linux 2023

### 1.1 Conectar al Servidor

```bash
# Desde tu máquina local (Windows PowerShell o WSL)
ssh -i "llave-ceo.pem" ec2-user@tu-ip-publica-ec2

# Si hay error de permisos en Windows:
# icacls "llave-ceo.pem" /inheritance:r
# icacls "llave-ceo.pem" /grant:r "%username%:R"
```

### 1.2 Actualizar Sistema

```bash
# Amazon Linux 2023 usa dnf (sucesor de yum)
sudo dnf update -y

# Instalar herramientas básicas
sudo dnf install -y git wget curl nano htop
```

### 1.3 Instalar Java 21 (Amazon Corretto)

```bash
# Amazon Corretto 21 (distribución de OpenJDK optimizada para AWS)
sudo dnf install -y java-21-amazon-corretto-devel

# Verificar instalación
java -version
# Debe mostrar: openjdk version "21.x.x" Amazon Corretto

# Configurar JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### 1.4 Instalar PostgreSQL 15

```bash
# Instalar PostgreSQL 15
sudo dnf install -y postgresql15-server postgresql15-contrib

# Inicializar base de datos
sudo postgresql-setup --initdb

# Iniciar y habilitar servicio
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Verificar estado
sudo systemctl status postgresql
```

### 1.5 Instalar Nginx

```bash
# Instalar Nginx
sudo dnf install -y nginx

# Iniciar y habilitar servicio
sudo systemctl start nginx
sudo systemctl enable nginx

# Verificar estado
sudo systemctl status nginx
```

### 1.6 Instalar Node.js 18 (para build de frontend)

```bash
# Instalar Node.js 18 desde repositorio oficial
sudo dnf install -y nodejs npm

# Verificar versiones
node -v
npm -v

# Si la versión no es 18+, usar nvm:
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc
nvm install 18
nvm use 18
```

### 1.7 Instalar Maven (para compilar backend)

```bash
# Instalar Maven
sudo dnf install -y maven

# Verificar
mvn -version
```

## Paso 2: Configurar Base de Datos

### 2.1 Crear Usuario y Base de Datos

```bash
# Cambiar a usuario postgres
sudo -u postgres psql

# Dentro de psql:
CREATE DATABASE geros;
CREATE USER geros_user WITH ENCRYPTED PASSWORD 'STRONG_PASSWORD_HERE';
GRANT ALL PRIVILEGES ON DATABASE geros TO geros_user;

# Dar permisos en el esquema
\c geros
GRANT ALL ON SCHEMA public TO geros_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO geros_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO geros_user;

# Salir
\q
```

### 2.2 Configurar Acceso Local (Amazon Linux 2023)

```bash
# Editar postgresql.conf
sudo nano /var/lib/pgsql/data/postgresql.conf

# Cambiar (solo acceso local - RECOMENDADO):
listen_addresses = 'localhost'

# Editar pg_hba.conf
sudo nano /var/lib/pgsql/data/pg_hba.conf

# Reemplazar contenido con:
# TYPE  DATABASE        USER            ADDRESS                 METHOD
local   all             all                                     peer
host    all             all             127.0.0.1/32            scram-sha-256
host    all             all             ::1/128                 scram-sha-256

# Reiniciar PostgreSQL
sudo systemctl restart postgresql

# Verificar estado
sudo systemctl status postgresql
```

## Paso 3: Preparar Aplicación

### 3.1 Crear Estructura de Directorios

```bash
# Crear directorios
sudo mkdir -p /opt/geros
sudo mkdir -p /opt/geros/backend
sudo mkdir -p /opt/geros/frontend
sudo mkdir -p /opt/geros/config
sudo mkdir -p /opt/geros/logs
sudo mkdir -p /opt/geros/storage/security-log-exports

# Crear usuario para la aplicación
sudo useradd -r -s /bin/false geros

# Dar permisos
sudo chown -R geros:geros /opt/geros
```

### 3.2 Transferir Archivos desde Local

**Opción A: Usando SCP**

```bash
# Desde tu máquina local (Windows)
# Backend JAR
scp -i "tu-clave.pem" backend/target/backend-0.0.1-SNAPSHOT.jar ubuntu@tu-servidor:/tmp/

# Frontend build
scp -i "tu-clave.pem" -r frontend/dist ubuntu@tu-servidor:/tmp/

# Configuración
scp -i "tu-clave.pem" backend/src/main/resources/application.properties ubuntu@tu-servidor:/tmp/
```

**Opción B: Usando Git**

```bash
# En el servidor AWS
cd /opt/geros
sudo git clone https://github.com/tu-usuario/geros.git .
sudo chown -R geros:geros /opt/geros
```

### 3.3 Compilar Backend en Servidor

```bash
cd /opt/geros/backend

# Compilar con Maven
./mvnw clean package -DskipTests

# Verificar JAR generado
ls -lh target/*.jar
```

### 3.4 Compilar Frontend en Servidor

```bash
cd /opt/geros/frontend

# Instalar dependencias
npm install

# Build para producción
npm run build

# Verificar build
ls -lh dist/
```

## Paso 4: Configurar Backend

### 4.1 Crear application-prod.properties

```bash
sudo nano /opt/geros/config/application-prod.properties
```

**Contenido**:
```properties
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

# reCaptcha (usar claves reales de producción)
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
```

### 4.2 Crear Variables de Entorno

```bash
sudo nano /opt/geros/config/geros.env
```

**Contenido**:
```bash
# Database
DB_PASSWORD=STRONG_PASSWORD_HERE

# JWT Secret (generar uno fuerte)
JWT_SECRET=GENERATE_STRONG_SECRET_HERE_MIN_256_BITS

# SSL Keystore
SSL_KEYSTORE_PASSWORD=KEYSTORE_PASSWORD_HERE

# reCaptcha (obtener de Google)
RECAPTCHA_SECRET=YOUR_RECAPTCHA_SECRET_KEY

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
```

**Generar JWT Secret fuerte**:
```bash
openssl rand -base64 64
```

### 4.3 Copiar Certificado SSL

```bash
# Copiar keystore generado
sudo cp /tmp/geros-keystore-4096.p12 /opt/geros/config/geros-keystore.p12
sudo chown geros:geros /opt/geros/config/geros-keystore.p12
sudo chmod 600 /opt/geros/config/geros-keystore.p12
```

### 4.4 Crear Servicio Systemd

```bash
sudo nano /etc/systemd/system/geros-backend.service
```

**Contenido**:
```ini
[Unit]
Description=GEROS Backend Application
After=postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=geros
Group=geros
WorkingDirectory=/opt/geros/backend

# Cargar variables de entorno
EnvironmentFile=/opt/geros/config/geros.env

# Comando de ejecución
ExecStart=/usr/bin/java \
    -Xms512m \
    -Xmx2048m \
    -Dspring.profiles.active=prod \
    -Dspring.config.location=file:/opt/geros/config/application-prod.properties \
    -jar /opt/geros/backend/target/backend-0.0.1-SNAPSHOT.jar

# Restart policy
Restart=always
RestartSec=10

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=geros-backend

# Security
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

### 4.5 Iniciar Backend

```bash
# Recargar systemd
sudo systemctl daemon-reload

# Habilitar servicio
sudo systemctl enable geros-backend

# Iniciar servicio
sudo systemctl start geros-backend

# Verificar estado
sudo systemctl status geros-backend

# Ver logs
sudo journalctl -u geros-backend -f
```

## Paso 5: Configurar Nginx

### 5.1 Crear Configuración de Nginx

```bash
sudo nano /etc/nginx/sites-available/geros
```

**Contenido**:
```nginx
# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name tu-dominio.promigas.com;
    
    # Redirect all HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

# HTTPS Server
server {
    listen 443 ssl http2;
    server_name tu-dominio.promigas.com;

    # SSL Certificate (usar certificado real en producción)
    ssl_certificate /opt/geros/config/geros-certificate.crt;
    ssl_certificate_key /opt/geros/config/geros-private.key;

    # SSL Configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'TLS_AES_256_GCM_SHA384:TLS_AES_128_GCM_SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256';
    ssl_prefer_server_ciphers on;
    
    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # Security Headers
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Root directory for frontend
    root /opt/geros/frontend/dist;
    index index.html;

    # Frontend - SPA routing
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Backend API - Proxy to Spring Boot
    location /api/ {
        proxy_pass https://localhost:8443;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # SSL verification
        proxy_ssl_verify off;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Logs
    access_log /var/log/nginx/geros-access.log;
    error_log /var/log/nginx/geros-error.log;
}
```

### 5.2 Habilitar Sitio

```bash
# Crear symlink
sudo ln -s /etc/nginx/sites-available/geros /etc/nginx/sites-enabled/

# Eliminar sitio default
sudo rm /etc/nginx/sites-enabled/default

# Verificar configuración
sudo nginx -t

# Reiniciar Nginx
sudo systemctl restart nginx
```

## Paso 6: Configurar Security Group de AWS

### 6.1 Reglas de Entrada (Inbound Rules)

| Tipo | Protocolo | Puerto | Origen | Descripción |
|------|-----------|--------|--------|-------------|
| HTTP | TCP | 80 | 0.0.0.0/0 | Redirect a HTTPS |
| HTTPS | TCP | 443 | 0.0.0.0/0 | Acceso público |
| SSH | TCP | 22 | Tu IP | Administración |
| Custom TCP | TCP | 8443 | 127.0.0.1/32 | Backend (solo local) |
| PostgreSQL | TCP | 5432 | 127.0.0.1/32 | Base de datos (solo local) |

### 6.2 Configurar en AWS Console

1. Ir a EC2 → Security Groups
2. Seleccionar security group de tu instancia
3. Edit inbound rules
4. Agregar reglas según tabla anterior
5. Save rules

## Paso 7: Configurar Dominio (Opcional)

### 7.1 Configurar Route 53

1. Ir a Route 53 → Hosted zones
2. Seleccionar tu dominio
3. Create record:
   - Record name: `geros`
   - Record type: `A`
   - Value: IP pública de tu EC2
   - TTL: `300`
4. Save

### 7.2 Obtener Certificado SSL con Let's Encrypt

```bash
# Instalar Certbot
sudo apt install certbot python3-certbot-nginx -y

# Obtener certificado
sudo certbot --nginx -d geros.promigas.com

# Renovación automática
sudo certbot renew --dry-run
```

## Paso 8: Verificación

### 8.1 Verificar Backend

```bash
# Verificar servicio
sudo systemctl status geros-backend

# Verificar logs
sudo journalctl -u geros-backend -n 50

# Verificar puerto
sudo netstat -tlnp | grep 8443

# Probar API
curl -k https://localhost:8443/api/auth/login
```

### 8.2 Verificar Frontend

```bash
# Verificar archivos
ls -la /opt/geros/frontend/dist/

# Verificar Nginx
sudo nginx -t
sudo systemctl status nginx

# Probar acceso
curl http://localhost
```

### 8.3 Verificar Base de Datos

```bash
# Conectar a PostgreSQL
sudo -u postgres psql -d geros

# Verificar tablas
\dt auth.*

# Verificar usuario admin
SELECT * FROM auth.users WHERE email = 'admin@geros.com';

# Salir
\q
```

### 8.4 Verificar desde Navegador

1. Abrir: `https://tu-dominio.promigas.com`
2. Verificar certificado SSL
3. Login con: `admin` / `admin123`
4. Verificar funcionalidad

## Paso 9: Monitoreo y Logs

### 9.1 Ver Logs de Backend

```bash
# Logs en tiempo real
sudo journalctl -u geros-backend -f

# Últimas 100 líneas
sudo journalctl -u geros-backend -n 100

# Logs de aplicación
sudo tail -f /opt/geros/logs/application.log
```

### 9.2 Ver Logs de Nginx

```bash
# Access logs
sudo tail -f /var/log/nginx/geros-access.log

# Error logs
sudo tail -f /var/log/nginx/geros-error.log
```

### 9.3 Ver Logs de PostgreSQL

```bash
# Logs de PostgreSQL
sudo tail -f /var/log/postgresql/postgresql-15-main.log
```

## Paso 10: Backup y Mantenimiento

### 10.1 Backup de Base de Datos

```bash
# Crear script de backup
sudo nano /opt/geros/scripts/backup-db.sh
```

**Contenido**:
```bash
#!/bin/bash
BACKUP_DIR="/opt/geros/backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/geros_backup_$DATE.sql"

mkdir -p $BACKUP_DIR

# Backup
sudo -u postgres pg_dump geros > $BACKUP_FILE

# Comprimir
gzip $BACKUP_FILE

# Eliminar backups antiguos (más de 7 días)
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "Backup completado: $BACKUP_FILE.gz"
```

```bash
# Dar permisos
sudo chmod +x /opt/geros/scripts/backup-db.sh

# Programar backup diario (cron)
sudo crontab -e

# Agregar:
0 2 * * * /opt/geros/scripts/backup-db.sh
```

### 10.2 Actualizar Aplicación

```bash
# Script de actualización
sudo nano /opt/geros/scripts/update-app.sh
```

**Contenido**:
```bash
#!/bin/bash
set -e

echo "Actualizando GEROS..."

# Detener servicios
sudo systemctl stop geros-backend

# Backup de base de datos
/opt/geros/scripts/backup-db.sh

# Actualizar código
cd /opt/geros
sudo git pull origin main

# Compilar backend
cd /opt/geros/backend
./mvnw clean package -DskipTests

# Compilar frontend
cd /opt/geros/frontend
npm install
npm run build

# Reiniciar servicios
sudo systemctl start geros-backend
sudo systemctl restart nginx

echo "Actualización completada"
```

## Troubleshooting

### Backend no inicia

```bash
# Ver logs detallados
sudo journalctl -u geros-backend -n 200

# Verificar Java
java -version

# Verificar permisos
ls -la /opt/geros/backend/target/*.jar

# Verificar variables de entorno
sudo cat /opt/geros/config/geros.env
```

### Error de conexión a base de datos

```bash
# Verificar PostgreSQL
sudo systemctl status postgresql

# Verificar conexión
sudo -u postgres psql -d geros -c "SELECT 1;"

# Verificar credenciales en application-prod.properties
```

### Nginx no sirve frontend

```bash
# Verificar archivos
ls -la /opt/geros/frontend/dist/

# Verificar configuración
sudo nginx -t

# Ver logs de error
sudo tail -f /var/log/nginx/geros-error.log
```

### Certificado SSL inválido

```bash
# Verificar certificado
openssl x509 -in /opt/geros/config/geros-certificate.crt -text -noout

# Verificar configuración Nginx
sudo nginx -t

# Renovar con Let's Encrypt
sudo certbot renew
```

## Checklist de Despliegue

### ☑️ Preparación
- [ ] Servidor AWS con Ubuntu/Amazon Linux
- [ ] Security Group configurado
- [ ] Dominio configurado (opcional)
- [ ] Certificado SSL preparado

### ☑️ Instalación
- [ ] Java 21 instalado
- [ ] PostgreSQL 15 instalado
- [ ] Nginx instalado
- [ ] Node.js instalado

### ☑️ Base de Datos
- [ ] Base de datos `geros` creada
- [ ] Usuario `geros_user` creado
- [ ] Permisos otorgados
- [ ] Conexión verificada

### ☑️ Backend
- [ ] Código compilado
- [ ] application-prod.properties configurado
- [ ] Variables de entorno configuradas
- [ ] Certificado SSL copiado
- [ ] Servicio systemd creado
- [ ] Servicio iniciado y habilitado

### ☑️ Frontend
- [ ] Código compilado (npm run build)
- [ ] Archivos copiados a /opt/geros/frontend/dist
- [ ] Nginx configurado
- [ ] Nginx reiniciado

### ☑️ Seguridad
- [ ] Security Group configurado
- [ ] Certificado SSL instalado
- [ ] HTTPS funcionando
- [ ] Firewall configurado

### ☑️ Verificación
- [ ] Backend responde en :8443
- [ ] Frontend accesible en :443
- [ ] Login funciona
- [ ] Base de datos accesible
- [ ] Logs funcionando

### ☑️ Mantenimiento
- [ ] Backup automático configurado
- [ ] Monitoreo configurado
- [ ] Script de actualización creado

## Resumen de Comandos Rápidos

```bash
# Ver estado de servicios
sudo systemctl status geros-backend
sudo systemctl status nginx
sudo systemctl status postgresql

# Reiniciar servicios
sudo systemctl restart geros-backend
sudo systemctl restart nginx

# Ver logs
sudo journalctl -u geros-backend -f
sudo tail -f /var/log/nginx/geros-error.log

# Backup manual
/opt/geros/scripts/backup-db.sh

# Actualizar aplicación
/opt/geros/scripts/update-app.sh
```

## Contacto y Soporte

**Documentación adicional**:
- `docs/CERTIFICADOS_TLS_SSL.md` - Configuración de certificados
- `docs/ARCHIVOS_CRITICOS_MONITOREO.md` - Monitoreo de archivos
- `docs/USUARIO_ADMIN_CUSTODIA.md` - Gestión de usuarios

**Logs importantes**:
- Backend: `/opt/geros/logs/application.log`
- Nginx: `/var/log/nginx/geros-*.log`
- PostgreSQL: `/var/log/postgresql/postgresql-15-main.log`
