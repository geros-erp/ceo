# Configuración de Certificados TLS/SSL X.509 v3

## Requisito de Seguridad

**La conexión entre el browser del cliente y el servidor web debe ser cifrada utilizando el protocolo TLS con certificados digitales X.509 versión 3.0 con llaves de 4096 bits.**

### Escenarios de Uso

#### 1. Uso Interno (Intranet)
- **Certificados**: Generados directamente por Promigas o autofirmados
- **Propósito**: Aplicaciones internas sin exposición a internet
- **Validación**: CA interna de Promigas

#### 2. Uso en Internet (Público)
- **Certificados**: Entidad reconocida y avalada por Promigas
- **Tipo**: Extended Validation (EV) - Certificados verdes
- **Validación**: CA pública reconocida (DigiCert, GlobalSign, etc.)

## Especificaciones Técnicas

### Certificado Digital

| Característica | Valor Requerido |
|----------------|-----------------|
| Estándar | X.509 versión 3.0 |
| Tamaño de llave | 4096 bits RSA |
| Algoritmo de firma | SHA-256 with RSA |
| Protocolo | TLS 1.2 o superior |
| Formato | PKCS#12 (.p12) o PEM |

### Configuración TLS

| Parámetro | Valor |
|-----------|-------|
| Protocolo mínimo | TLS 1.2 |
| Protocolo recomendado | TLS 1.3 |
| Cipher Suites | Solo strong ciphers |
| Perfect Forward Secrecy | Habilitado |

## Implementación Actual

### Backend (Spring Boot)

**Archivo**: `backend/src/main/resources/application.properties`

```properties
# HTTPS Configuration
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:geros-keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD:geros2024}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=geros

# TLS Protocol Configuration
server.ssl.protocol=TLS
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3
```

**Estado actual**: Certificado autofirmado de desarrollo (2048 bits)

## Generación de Certificados

### Opción 1: Uso Interno (Autofirmado 4096 bits)

#### Paso 1: Generar Certificado Autofirmado

```bash
# Generar certificado X.509 v3 con llave RSA de 4096 bits
keytool -genkeypair \
  -alias geros \
  -keyalg RSA \
  -keysize 4096 \
  -sigalg SHA256withRSA \
  -validity 365 \
  -keystore geros-keystore.p12 \
  -storetype PKCS12 \
  -storepass geros2024 \
  -dname "CN=geros.promigas.local, OU=IT Security, O=Promigas, L=Barranquilla, ST=Atlantico, C=CO" \
  -ext "SAN=dns:geros.promigas.local,dns:localhost,ip:127.0.0.1" \
  -ext "KeyUsage=digitalSignature,keyEncipherment" \
  -ext "ExtendedKeyUsage=serverAuth,clientAuth"
```

**Parámetros importantes**:
- `-keysize 4096`: Llave de 4096 bits (requisito)
- `-sigalg SHA256withRSA`: Algoritmo de firma seguro
- `-validity 365`: Válido por 1 año
- `-ext "SAN=..."`: Subject Alternative Names
- `-ext "KeyUsage=..."`: Uso de la llave
- `-ext "ExtendedKeyUsage=..."`: Uso extendido

#### Paso 2: Verificar Certificado

```bash
# Ver detalles del certificado
keytool -list -v -keystore geros-keystore.p12 -storepass geros2024

# Verificar tamaño de llave (debe mostrar 4096)
keytool -list -v -keystore geros-keystore.p12 -storepass geros2024 | grep "Key length"

# Verificar versión X.509 (debe mostrar v3)
keytool -list -v -keystore geros-keystore.p12 -storepass geros2024 | grep "Version"
```

#### Paso 3: Exportar Certificado Público

```bash
# Exportar certificado en formato PEM
keytool -exportcert \
  -alias geros \
  -keystore geros-keystore.p12 \
  -storepass geros2024 \
  -rfc \
  -file geros-certificate.pem
```

#### Paso 4: Instalar en Clientes

**Windows**:
```powershell
# Importar certificado en Trusted Root Certification Authorities
certutil -addstore -enterprise -f "Root" geros-certificate.pem
```

**Linux**:
```bash
# Copiar certificado
sudo cp geros-certificate.pem /usr/local/share/ca-certificates/geros.crt

# Actualizar certificados
sudo update-ca-certificates
```

**macOS**:
```bash
# Importar en Keychain
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain geros-certificate.pem
```

### Opción 2: CA Interna de Promigas

#### Paso 1: Generar CSR (Certificate Signing Request)

```bash
# Generar llave privada de 4096 bits
openssl genrsa -out geros-private.key 4096

# Generar CSR
openssl req -new \
  -key geros-private.key \
  -out geros.csr \
  -subj "/C=CO/ST=Atlantico/L=Barranquilla/O=Promigas/OU=IT Security/CN=geros.promigas.local" \
  -addext "subjectAltName=DNS:geros.promigas.local,DNS:localhost,IP:127.0.0.1" \
  -addext "keyUsage=digitalSignature,keyEncipherment" \
  -addext "extendedKeyUsage=serverAuth,clientAuth"
```

#### Paso 2: Enviar CSR a CA de Promigas

**Archivo a enviar**: `geros.csr`

**Información requerida**:
- Common Name (CN): geros.promigas.local
- Organization (O): Promigas
- Organizational Unit (OU): IT Security
- Locality (L): Barranquilla
- State (ST): Atlántico
- Country (C): CO
- Key Size: 4096 bits
- Signature Algorithm: SHA-256 with RSA
- Validity: 1 año (renovable)

#### Paso 3: Recibir Certificado Firmado

**Archivos recibidos de CA**:
- `geros-signed.crt` - Certificado firmado
- `promigas-ca.crt` - Certificado de CA intermedia
- `promigas-root-ca.crt` - Certificado raíz de Promigas

#### Paso 4: Crear Keystore PKCS#12

```bash
# Crear cadena de certificados
cat geros-signed.crt promigas-ca.crt promigas-root-ca.crt > geros-chain.crt

# Crear PKCS#12 con cadena completa
openssl pkcs12 -export \
  -in geros-chain.crt \
  -inkey geros-private.key \
  -out geros-keystore.p12 \
  -name geros \
  -passout pass:geros2024
```

### Opción 3: Uso en Internet (Extended Validation)

#### Paso 1: Seleccionar CA Reconocida

**CAs recomendadas** (avaladas por Promigas):
- DigiCert (EV SSL Certificate)
- GlobalSign (Extended Validation SSL)
- Sectigo (EV SSL Certificate)
- Entrust (EV SSL Certificate)

#### Paso 2: Generar CSR para EV

```bash
# Generar llave privada de 4096 bits
openssl genrsa -out geros-ev-private.key 4096

# Generar CSR para EV
openssl req -new \
  -key geros-ev-private.key \
  -out geros-ev.csr \
  -subj "/C=CO/ST=Atlantico/L=Barranquilla/O=Promigas S.A. E.S.P./OU=IT Security/CN=geros.promigas.com" \
  -addext "subjectAltName=DNS:geros.promigas.com,DNS:www.geros.promigas.com" \
  -addext "keyUsage=digitalSignature,keyEncipherment" \
  -addext "extendedKeyUsage=serverAuth"
```

#### Paso 3: Proceso de Validación Extendida

**Documentación requerida por CA**:

1. **Validación de Organización**:
   - Certificado de existencia y representación legal
   - RUT (Registro Único Tributario)
   - Cámara de Comercio
   - Estatutos de la empresa

2. **Validación de Dominio**:
   - Propiedad del dominio geros.promigas.com
   - Acceso a DNS o email administrativo

3. **Validación de Contacto**:
   - Verificación telefónica con representante legal
   - Confirmación de dirección física
   - Verificación de email corporativo

4. **Información Adicional**:
   - Número de identificación fiscal
   - Dirección física verificable
   - Número de teléfono público

#### Paso 4: Recibir Certificado EV

**Archivos recibidos**:
- `geros-ev.crt` - Certificado EV firmado
- `intermediate-ca.crt` - Certificado intermedio
- `root-ca.crt` - Certificado raíz de CA

#### Paso 5: Crear Keystore para Producción

```bash
# Crear cadena de certificados
cat geros-ev.crt intermediate-ca.crt root-ca.crt > geros-ev-chain.crt

# Crear PKCS#12
openssl pkcs12 -export \
  -in geros-ev-chain.crt \
  -inkey geros-ev-private.key \
  -out geros-production-keystore.p12 \
  -name geros \
  -passout pass:${STRONG_PASSWORD}
```

## Configuración en Spring Boot

### Desarrollo (Certificado Autofirmado)

**application-dev.properties**:
```properties
# HTTPS - Desarrollo
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:geros-keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD:geros2024}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=geros

# TLS Configuration
server.ssl.protocol=TLS
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3
server.ssl.ciphers=TLS_AES_256_GCM_SHA384,TLS_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
```

### Producción Interna (CA Promigas)

**application-prod-internal.properties**:
```properties
# HTTPS - Producción Interna
server.port=443
server.ssl.enabled=true
server.ssl.key-store=file:/opt/geros/config/geros-keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=geros

# TLS Configuration
server.ssl.protocol=TLS
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3
server.ssl.ciphers=TLS_AES_256_GCM_SHA384,TLS_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256

# Security Headers
server.ssl.client-auth=none
```

### Producción Internet (Certificado EV)

**application-prod-public.properties**:
```properties
# HTTPS - Producción Pública (Internet)
server.port=443
server.ssl.enabled=true
server.ssl.key-store=file:/opt/geros/config/geros-production-keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=geros

# TLS Configuration - Strict
server.ssl.protocol=TLS
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3
server.ssl.ciphers=TLS_AES_256_GCM_SHA384,TLS_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256

# HSTS (HTTP Strict Transport Security)
server.ssl.client-auth=none

# Additional Security
security.require-ssl=true
```

## Configuración de Nginx (Reverse Proxy)

### Uso Interno

```nginx
server {
    listen 443 ssl http2;
    server_name geros.promigas.local;

    # Certificado CA Promigas
    ssl_certificate /etc/nginx/ssl/geros-chain.crt;
    ssl_certificate_key /etc/nginx/ssl/geros-private.key;

    # TLS Configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'TLS_AES_256_GCM_SHA384:TLS_AES_128_GCM_SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256';
    ssl_prefer_server_ciphers on;

    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    ssl_trusted_certificate /etc/nginx/ssl/promigas-root-ca.crt;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;

    location / {
        proxy_pass https://localhost:8443;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Uso en Internet (EV)

```nginx
server {
    listen 443 ssl http2;
    server_name geros.promigas.com www.geros.promigas.com;

    # Certificado EV
    ssl_certificate /etc/nginx/ssl/geros-ev-chain.crt;
    ssl_certificate_key /etc/nginx/ssl/geros-ev-private.key;

    # TLS Configuration - Strict
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'TLS_AES_256_GCM_SHA384:TLS_AES_128_GCM_SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256';
    ssl_prefer_server_ciphers on;

    # Perfect Forward Secrecy
    ssl_dhparam /etc/nginx/ssl/dhparam-4096.pem;

    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    resolver 8.8.8.8 8.8.4.4 valid=300s;
    resolver_timeout 5s;

    # HSTS (2 years)
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;
    
    # Security Headers
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    location / {
        proxy_pass https://localhost:8443;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_ssl_verify on;
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name geros.promigas.com www.geros.promigas.com;
    return 301 https://$server_name$request_uri;
}
```

## Verificación de Certificados

### Verificar Tamaño de Llave

```bash
# Usando keytool
keytool -list -v -keystore geros-keystore.p12 -storepass geros2024 | grep "Key length"

# Usando openssl
openssl x509 -in geros-certificate.pem -text -noout | grep "Public-Key"

# Debe mostrar: Public-Key: (4096 bit)
```

### Verificar Versión X.509

```bash
# Usando keytool
keytool -list -v -keystore geros-keystore.p12 -storepass geros2024 | grep "Version"

# Usando openssl
openssl x509 -in geros-certificate.pem -text -noout | grep "Version"

# Debe mostrar: Version: 3 (0x2)
```

### Verificar Algoritmo de Firma

```bash
# Usando openssl
openssl x509 -in geros-certificate.pem -text -noout | grep "Signature Algorithm"

# Debe mostrar: Signature Algorithm: sha256WithRSAEncryption
```

### Verificar Extensiones X.509 v3

```bash
# Ver todas las extensiones
openssl x509 -in geros-certificate.pem -text -noout | grep -A 20 "X509v3 extensions"

# Debe incluir:
# - X509v3 Subject Alternative Name
# - X509v3 Key Usage
# - X509v3 Extended Key Usage
```

### Verificar Conexión TLS

```bash
# Verificar protocolo TLS
openssl s_client -connect localhost:8443 -tls1_2

# Verificar cipher suite
openssl s_client -connect localhost:8443 -cipher 'ECDHE-RSA-AES256-GCM-SHA384'

# Verificar certificado completo
openssl s_client -connect localhost:8443 -showcerts
```

### Verificar con Navegador

**Chrome/Edge**:
1. Abrir https://geros.promigas.com
2. Click en candado 🔒
3. Ver "Certificado"
4. Verificar:
   - Emisor: CA reconocida
   - Tipo: Extended Validation (barra verde)
   - Tamaño de llave: 4096 bits
   - Algoritmo: SHA-256 with RSA

**Firefox**:
1. Abrir https://geros.promigas.com
2. Click en candado 🔒
3. "Conexión segura" → "Más información"
4. "Ver certificado"
5. Verificar detalles técnicos

## Renovación de Certificados

### Certificados Internos (Anual)

```bash
# 1. Generar nuevo certificado (30 días antes de expiración)
keytool -genkeypair \
  -alias geros-new \
  -keyalg RSA \
  -keysize 4096 \
  -sigalg SHA256withRSA \
  -validity 365 \
  -keystore geros-keystore-new.p12 \
  -storetype PKCS12 \
  -storepass geros2024 \
  -dname "CN=geros.promigas.local, OU=IT Security, O=Promigas, L=Barranquilla, ST=Atlantico, C=CO"

# 2. Probar en ambiente de staging
# 3. Programar ventana de mantenimiento
# 4. Reemplazar keystore en producción
# 5. Reiniciar aplicación
# 6. Verificar funcionamiento
```

### Certificados EV (Anual)

**Timeline de renovación**:
- **90 días antes**: Iniciar proceso con CA
- **60 días antes**: Enviar CSR y documentación
- **30 días antes**: Recibir certificado renovado
- **15 días antes**: Probar en staging
- **7 días antes**: Programar ventana de mantenimiento
- **Día de expiración**: Desplegar nuevo certificado

## Monitoreo de Certificados

### Script de Verificación

```bash
#!/bin/bash
# check-certificate-expiry.sh

KEYSTORE="/opt/geros/config/geros-keystore.p12"
PASSWORD="geros2024"
ALERT_DAYS=30

# Obtener fecha de expiración
EXPIRY_DATE=$(keytool -list -v -keystore $KEYSTORE -storepass $PASSWORD | grep "Valid until" | awk '{print $4}')

# Calcular días restantes
EXPIRY_EPOCH=$(date -d "$EXPIRY_DATE" +%s)
TODAY_EPOCH=$(date +%s)
DAYS_LEFT=$(( ($EXPIRY_EPOCH - $TODAY_EPOCH) / 86400 ))

echo "Certificado expira en: $DAYS_LEFT días"

if [ $DAYS_LEFT -lt $ALERT_DAYS ]; then
    echo "⚠️  ALERTA: Certificado expira pronto"
    # Enviar alerta
    mail -s "Certificado SSL expira en $DAYS_LEFT días" admin@promigas.com
fi
```

### Cron Job

```bash
# Verificar diariamente a las 8 AM
0 8 * * * /opt/geros/scripts/check-certificate-expiry.sh
```

## Troubleshooting

### Error: "Certificate key length is not 4096 bits"

```bash
# Verificar tamaño actual
openssl x509 -in certificate.pem -text -noout | grep "Public-Key"

# Regenerar con 4096 bits
keytool -genkeypair -keyalg RSA -keysize 4096 ...
```

### Error: "Certificate is not X.509 v3"

```bash
# Verificar versión
openssl x509 -in certificate.pem -text -noout | grep "Version"

# Regenerar con extensiones v3
keytool -genkeypair ... -ext "KeyUsage=..." -ext "ExtendedKeyUsage=..."
```

### Error: "TLS handshake failed"

```bash
# Verificar protocolos habilitados
openssl s_client -connect localhost:8443 -tls1_2

# Verificar cipher suites
openssl s_client -connect localhost:8443 -cipher 'HIGH:!aNULL:!MD5'
```

### Error: "Certificate chain incomplete"

```bash
# Verificar cadena
openssl s_client -connect localhost:8443 -showcerts

# Reconstruir cadena
cat server.crt intermediate.crt root.crt > chain.crt
```

## Checklist de Implementación

### ☑️ Desarrollo

- [ ] Generar certificado autofirmado 4096 bits
- [ ] Configurar application.properties con TLS
- [ ] Verificar versión X.509 v3
- [ ] Probar conexión HTTPS en localhost
- [ ] Instalar certificado en navegadores de desarrollo

### ☑️ Producción Interna

- [ ] Generar CSR con llave de 4096 bits
- [ ] Enviar CSR a CA de Promigas
- [ ] Recibir certificado firmado
- [ ] Crear keystore PKCS#12 con cadena completa
- [ ] Configurar application-prod-internal.properties
- [ ] Instalar certificado raíz de Promigas en clientes
- [ ] Configurar Nginx como reverse proxy
- [ ] Verificar TLS 1.2/1.3
- [ ] Probar conexión desde clientes internos
- [ ] Configurar monitoreo de expiración

### ☑️ Producción Internet

- [ ] Seleccionar CA reconocida (DigiCert, GlobalSign, etc.)
- [ ] Generar CSR con llave de 4096 bits
- [ ] Preparar documentación para validación EV
- [ ] Completar proceso de validación extendida
- [ ] Recibir certificado EV
- [ ] Crear keystore PKCS#12 con cadena completa
- [ ] Configurar application-prod-public.properties
- [ ] Configurar Nginx con HSTS y security headers
- [ ] Verificar barra verde en navegadores
- [ ] Probar desde internet público
- [ ] Configurar renovación automática
- [ ] Configurar monitoreo de expiración

## Cumplimiento del Requisito

✅ **Protocolo TLS**: Configurado con TLS 1.2 y 1.3

✅ **Certificados X.509 v3**: Generación con extensiones v3

✅ **Llaves de 4096 bits**: Procedimientos documentados

✅ **Uso Interno**: Certificados CA Promigas o autofirmados

✅ **Uso Internet**: Certificados EV de CA reconocida

✅ **Configuración Spring Boot**: application.properties configurado

✅ **Configuración Nginx**: Reverse proxy con TLS

✅ **Verificación**: Scripts y comandos de validación

✅ **Monitoreo**: Scripts de verificación de expiración

✅ **Renovación**: Procedimientos documentados

## Resumen

**Configuración actual**: Certificado autofirmado de desarrollo (2048 bits)

**Acción requerida**: Regenerar con 4096 bits para cumplir requisito

**Para producción interna**: Solicitar certificado a CA de Promigas

**Para producción internet**: Solicitar certificado EV a CA reconocida

**Documentación completa**: Procedimientos, configuración y verificación incluidos
