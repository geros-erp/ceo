# Guía Rápida - Despliegue en AWS

## Requisitos Previos

- ✅ Servidor AWS EC2 (Ubuntu 22.04 o Amazon Linux 2023)
- ✅ Tipo: t3.medium o superior (2 vCPU, 4 GB RAM)
- ✅ Almacenamiento: 30 GB mínimo
- ✅ Security Group con puertos 80, 443 abiertos

## Opción 1: Despliegue Automatizado (Recomendado)

### Paso 1: Conectar al Servidor

```bash
ssh -i "tu-clave.pem" ubuntu@tu-servidor-aws.compute.amazonaws.com
```

### Paso 2: Instalar Requisitos

```bash
# Actualizar sistema
sudo apt update && sudo apt upgrade -y

# Instalar Java 21
sudo add-apt-repository ppa:openjdk-r/ppa -y
sudo apt update
sudo apt install openjdk-21-jdk -y

# Instalar PostgreSQL 15
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
sudo apt update
sudo apt install postgresql-15 postgresql-contrib-15 -y

# Instalar Nginx
sudo apt install nginx -y

# Instalar Node.js
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc
nvm install 18
```

### Paso 3: Transferir Archivos

**Desde tu máquina local:**

```bash
# Comprimir proyecto
tar -czf geros.tar.gz backend frontend scripts docs

# Transferir a servidor
scp -i "tu-clave.pem" geros.tar.gz ubuntu@tu-servidor:/tmp/
```

**En el servidor:**

```bash
# Extraer archivos
cd /tmp
tar -xzf geros.tar.gz
```

### Paso 4: Ejecutar Script de Despliegue

```bash
cd /tmp
sudo chmod +x scripts/deploy-aws.sh
sudo ./scripts/deploy-aws.sh
```

El script te pedirá:
- Contraseña para base de datos
- Dominio de la aplicación
- Confirmación para iniciar servicios

### Paso 5: Copiar Certificados SSL

```bash
# Copiar certificado
sudo cp geros-keystore-4096.p12 /opt/geros/config/geros-keystore.p12

# O generar certificado con Let's Encrypt
sudo certbot --nginx -d tu-dominio.promigas.com
```

### Paso 6: Verificar

```bash
# Ver estado de servicios
sudo systemctl status geros-backend
sudo systemctl status nginx

# Ver logs
sudo journalctl -u geros-backend -f
```

Abrir navegador: `https://tu-dominio.promigas.com`

## Opción 2: Despliegue Manual

### Paso 1: Compilar Aplicación

**Backend:**
```bash
cd backend
./mvnw clean package -DskipTests
```

**Frontend:**
```bash
cd frontend
npm install
npm run build
```

### Paso 2: Crear Estructura

```bash
sudo mkdir -p /opt/geros/{backend,frontend,config,logs,storage}
sudo useradd -r -s /bin/false geros
sudo chown -R geros:geros /opt/geros
```

### Paso 3: Copiar Archivos

```bash
# Backend
sudo cp backend/target/backend-0.0.1-SNAPSHOT.jar /opt/geros/backend/

# Frontend
sudo cp -r frontend/dist/* /opt/geros/frontend/

# Configuración
sudo cp backend/src/main/resources/application.properties /opt/geros/config/application-prod.properties
```

### Paso 4: Configurar Base de Datos

```bash
sudo -u postgres psql << EOF
CREATE DATABASE geros;
CREATE USER geros_user WITH ENCRYPTED PASSWORD 'TU_PASSWORD';
GRANT ALL PRIVILEGES ON DATABASE geros TO geros_user;
\c geros
GRANT ALL ON SCHEMA public TO geros_user;
EOF
```

### Paso 5: Crear Servicio

```bash
sudo nano /etc/systemd/system/geros-backend.service
```

Copiar contenido del servicio (ver documentación completa).

```bash
sudo systemctl daemon-reload
sudo systemctl enable geros-backend
sudo systemctl start geros-backend
```

### Paso 6: Configurar Nginx

```bash
sudo nano /etc/nginx/sites-available/geros
```

Copiar configuración de Nginx (ver documentación completa).

```bash
sudo ln -s /etc/nginx/sites-available/geros /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

## Configuración de Security Group

### Reglas de Entrada

| Puerto | Protocolo | Origen | Descripción |
|--------|-----------|--------|-------------|
| 22 | TCP | Tu IP | SSH |
| 80 | TCP | 0.0.0.0/0 | HTTP |
| 443 | TCP | 0.0.0.0/0 | HTTPS |

## Comandos Útiles

### Ver Logs

```bash
# Backend
sudo journalctl -u geros-backend -f

# Nginx
sudo tail -f /var/log/nginx/geros-error.log

# PostgreSQL
sudo tail -f /var/log/postgresql/postgresql-15-main.log
```

### Reiniciar Servicios

```bash
sudo systemctl restart geros-backend
sudo systemctl restart nginx
sudo systemctl restart postgresql
```

### Backup Manual

```bash
sudo -u postgres pg_dump geros > /opt/geros/backups/backup_$(date +%Y%m%d).sql
```

## Verificación

### 1. Backend

```bash
# Verificar servicio
sudo systemctl status geros-backend

# Probar API
curl -k https://localhost:8443/api/auth/login
```

### 2. Frontend

```bash
# Verificar archivos
ls -la /opt/geros/frontend/

# Probar Nginx
curl http://localhost
```

### 3. Base de Datos

```bash
# Conectar
sudo -u postgres psql -d geros

# Verificar tablas
\dt auth.*

# Verificar usuario admin
SELECT * FROM auth.users WHERE email = 'admin@geros.com';
```

### 4. Navegador

1. Abrir: `https://tu-dominio.promigas.com`
2. Login: `admin` / `admin123`
3. Verificar funcionalidad

## Troubleshooting

### Backend no inicia

```bash
# Ver logs detallados
sudo journalctl -u geros-backend -n 200

# Verificar Java
java -version

# Verificar puerto
sudo netstat -tlnp | grep 8443
```

### Error de conexión a BD

```bash
# Verificar PostgreSQL
sudo systemctl status postgresql

# Probar conexión
sudo -u postgres psql -d geros -c "SELECT 1;"
```

### Nginx error 502

```bash
# Verificar backend
sudo systemctl status geros-backend

# Ver logs de Nginx
sudo tail -f /var/log/nginx/geros-error.log
```

## Actualización

```bash
# Detener backend
sudo systemctl stop geros-backend

# Backup de BD
sudo -u postgres pg_dump geros > /opt/geros/backups/backup_pre_update.sql

# Actualizar código
cd /opt/geros
sudo git pull origin main

# Compilar
cd backend && ./mvnw clean package -DskipTests
cd ../frontend && npm install && npm run build

# Copiar archivos
sudo cp backend/target/*.jar /opt/geros/backend/
sudo cp -r frontend/dist/* /opt/geros/frontend/

# Reiniciar
sudo systemctl start geros-backend
sudo systemctl restart nginx
```

## Backup Automático

```bash
# Editar crontab
sudo crontab -e

# Agregar backup diario a las 2 AM
0 2 * * * /opt/geros/scripts/backup-db.sh
```

## Monitoreo

### CloudWatch (Opcional)

```bash
# Instalar agente de CloudWatch
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i amazon-cloudwatch-agent.deb
```

### Logs Centralizados

```bash
# Configurar rsyslog para enviar logs
sudo nano /etc/rsyslog.d/50-geros.conf

# Agregar:
if $programname == 'geros-backend' then /var/log/geros/backend.log
& stop
```

## Checklist Post-Despliegue

- [ ] Backend iniciado correctamente
- [ ] Frontend accesible
- [ ] Base de datos funcionando
- [ ] Certificado SSL válido
- [ ] Login funciona
- [ ] Cambiar contraseña de admin
- [ ] Configurar backup automático
- [ ] Configurar monitoreo
- [ ] Documentar credenciales
- [ ] Probar desde internet

## Información de Contacto

**Archivos importantes:**
- Configuración: `/opt/geros/config/`
- Logs: `/opt/geros/logs/`
- Backups: `/opt/geros/backups/`

**Documentación completa:**
- `docs/DESPLIEGUE_AWS.md`
- `docs/CERTIFICADOS_TLS_SSL.md`

**Credenciales por defecto:**
- Usuario: `admin`
- Contraseña: `admin123`
- **⚠️ CAMBIAR INMEDIATAMENTE DESPUÉS DEL PRIMER LOGIN**

## Resumen de Comandos

```bash
# Estado de servicios
sudo systemctl status geros-backend nginx postgresql

# Logs en tiempo real
sudo journalctl -u geros-backend -f

# Reiniciar todo
sudo systemctl restart geros-backend nginx

# Backup manual
sudo -u postgres pg_dump geros > backup.sql

# Ver usuarios conectados
sudo -u postgres psql -d geros -c "SELECT * FROM auth.users;"
```

## Soporte

Para más información, consultar:
- Documentación completa: `docs/DESPLIEGUE_AWS.md`
- Guía de certificados: `docs/CERTIFICADOS_TLS_SSL.md`
- Monitoreo de archivos: `docs/ARCHIVOS_CRITICOS_MONITOREO.md`
