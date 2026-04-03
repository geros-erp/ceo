# Guía Rápida - Certificados TLS/SSL

## Requisito

**Conexión cifrada con TLS usando certificados X.509 v3 con llaves de 4096 bits**

## Escenarios

### 1. Desarrollo Local
**Certificado**: Autofirmado 4096 bits  
**Uso**: Desarrollo y pruebas

### 2. Producción Interna
**Certificado**: CA de Promigas 4096 bits  
**Uso**: Aplicaciones internas (intranet)

### 3. Producción Internet
**Certificado**: CA reconocida EV 4096 bits  
**Uso**: Aplicaciones públicas (internet)

## Comandos Rápidos

### Generar Certificado Autofirmado (Desarrollo)

**Windows:**
```powershell
.\scripts\generate-certificate-4096.ps1
```

**Linux/macOS:**
```bash
chmod +x scripts/generate-certificate-4096.sh
./scripts/generate-certificate-4096.sh
```

**Resultado**: `geros-keystore-4096.p12` (4096 bits, X.509 v3)

### Generar CSR para CA de Promigas

**Linux/macOS:**
```bash
chmod +x scripts/generate-csr-promigas.sh
./scripts/generate-csr-promigas.sh
```

**Resultado**: 
- `geros-private-4096.key` (llave privada - GUARDAR SEGURA)
- `geros-promigas.csr` (enviar a CA de Promigas)

### Verificar Certificado

**Tamaño de llave:**
```bash
keytool -list -v -keystore geros-keystore-4096.p12 -storepass geros2024 | grep "4096"
```

**Versión X.509:**
```bash
keytool -list -v -keystore geros-keystore-4096.p12 -storepass geros2024 | grep "Version: 3"
```

**Algoritmo de firma:**
```bash
keytool -list -v -keystore geros-keystore-4096.p12 -storepass geros2024 | grep "SHA256withRSA"
```

## Configuración Spring Boot

### application.properties

```properties
# HTTPS Configuration
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:geros-keystore-4096.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD:geros2024}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=geros

# TLS Protocol
server.ssl.protocol=TLS
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3
```

## Instalación en Clientes

### Windows
```powershell
certutil -addstore -enterprise -f "Root" geros-keystore-4096.pem
```

### Linux
```bash
sudo cp geros-keystore-4096.pem /usr/local/share/ca-certificates/geros.crt
sudo update-ca-certificates
```

### macOS
```bash
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain geros-keystore-4096.pem
```

## Proceso para Producción Interna

1. **Generar CSR**:
   ```bash
   ./scripts/generate-csr-promigas.sh
   ```

2. **Enviar a CA de Promigas**:
   - Archivo: `geros-promigas.csr`
   - Información: Ver `geros-promigas-info.txt`

3. **Recibir certificados**:
   - `geros-signed.crt`
   - `promigas-ca.crt`
   - `promigas-root-ca.crt`

4. **Crear keystore**:
   ```bash
   cat geros-signed.crt promigas-ca.crt promigas-root-ca.crt > geros-chain.crt
   
   openssl pkcs12 -export \
     -in geros-chain.crt \
     -inkey geros-private-4096.key \
     -out geros-promigas-keystore.p12 \
     -name geros \
     -passout pass:STRONG_PASSWORD
   ```

5. **Configurar aplicación**:
   ```properties
   server.ssl.key-store=file:/opt/geros/config/geros-promigas-keystore.p12
   server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
   ```

## Proceso para Producción Internet (EV)

1. **Seleccionar CA**: DigiCert, GlobalSign, Sectigo, Entrust

2. **Generar CSR**:
   ```bash
   openssl genrsa -out geros-ev-private.key 4096
   
   openssl req -new \
     -key geros-ev-private.key \
     -out geros-ev.csr \
     -subj "/C=CO/ST=Atlantico/L=Barranquilla/O=Promigas S.A. E.S.P./OU=IT Security/CN=geros.promigas.com"
   ```

3. **Enviar CSR a CA** con documentación:
   - Certificado de existencia
   - RUT
   - Cámara de Comercio
   - Estatutos

4. **Validación Extendida**: CA verifica organización (2-4 semanas)

5. **Recibir certificado EV**:
   - `geros-ev.crt`
   - `intermediate-ca.crt`
   - `root-ca.crt`

6. **Crear keystore**:
   ```bash
   cat geros-ev.crt intermediate-ca.crt root-ca.crt > geros-ev-chain.crt
   
   openssl pkcs12 -export \
     -in geros-ev-chain.crt \
     -inkey geros-ev-private.key \
     -out geros-production-keystore.p12 \
     -name geros
   ```

## Verificación

### Verificar Conexión TLS

```bash
# Verificar protocolo
openssl s_client -connect localhost:8443 -tls1_2

# Verificar certificado
openssl s_client -connect localhost:8443 -showcerts

# Verificar cipher suite
openssl s_client -connect localhost:8443 -cipher 'ECDHE-RSA-AES256-GCM-SHA384'
```

### Verificar en Navegador

1. Abrir https://localhost:8443
2. Click en candado 🔒
3. Ver certificado
4. Verificar:
   - Tamaño de llave: 4096 bits
   - Versión: X.509 v3
   - Algoritmo: SHA-256 with RSA

## Renovación

### Certificados Internos (Anual)

**90 días antes de expiración**:
```bash
# Generar nuevo certificado
./scripts/generate-certificate-4096.sh

# O solicitar renovación a CA de Promigas
./scripts/generate-csr-promigas.sh
```

### Certificados EV (Anual)

**Timeline**:
- 90 días antes: Iniciar proceso con CA
- 60 días antes: Enviar CSR y documentación
- 30 días antes: Recibir certificado renovado
- 15 días antes: Probar en staging
- 7 días antes: Programar mantenimiento

## Monitoreo

### Script de Verificación

```bash
# Verificar expiración
keytool -list -v -keystore geros-keystore.p12 -storepass PASSWORD | grep "Valid until"

# Días restantes
openssl x509 -in certificate.pem -noout -enddate
```

### Cron Job

```bash
# Verificar diariamente
0 8 * * * /opt/geros/scripts/check-certificate-expiry.sh
```

## Troubleshooting

### Error: "Key length is not 4096 bits"

**Solución**: Regenerar con `-keysize 4096`

### Error: "Certificate is not X.509 v3"

**Solución**: Agregar extensiones `-ext "KeyUsage=..." -ext "ExtendedKeyUsage=..."`

### Error: "TLS handshake failed"

**Solución**: Verificar protocolos habilitados en application.properties

### Error: "Certificate chain incomplete"

**Solución**: Incluir certificados intermedios en cadena

## Checklist

### ☑️ Desarrollo
- [ ] Generar certificado autofirmado 4096 bits
- [ ] Verificar X.509 v3
- [ ] Configurar application.properties
- [ ] Probar conexión HTTPS

### ☑️ Producción Interna
- [ ] Generar CSR 4096 bits
- [ ] Enviar a CA de Promigas
- [ ] Recibir certificado firmado
- [ ] Crear keystore con cadena completa
- [ ] Instalar en servidor
- [ ] Configurar monitoreo

### ☑️ Producción Internet
- [ ] Seleccionar CA reconocida
- [ ] Generar CSR 4096 bits
- [ ] Preparar documentación EV
- [ ] Completar validación extendida
- [ ] Recibir certificado EV
- [ ] Crear keystore
- [ ] Verificar barra verde en navegadores
- [ ] Configurar renovación automática

## Archivos Importantes

| Archivo | Descripción | Seguridad |
|---------|-------------|-----------|
| `*.key` | Llave privada | 🔴 CRÍTICO - Nunca compartir |
| `*.csr` | Certificate Signing Request | 🟢 Público - Enviar a CA |
| `*.crt` | Certificado firmado | 🟢 Público |
| `*.p12` | Keystore PKCS#12 | 🔴 CRÍTICO - Proteger con password |
| `*.pem` | Certificado en formato PEM | 🟢 Público |

## Documentación Completa

**Ver**: `docs/CERTIFICADOS_TLS_SSL.md`

## Resumen

**Requisito**: X.509 v3 con llaves de 4096 bits

**Desarrollo**: Certificado autofirmado

**Producción Interna**: CA de Promigas

**Producción Internet**: CA reconocida con EV (certificados verdes)

**Scripts disponibles**:
- `generate-certificate-4096.sh` / `.ps1`
- `generate-csr-promigas.sh`
