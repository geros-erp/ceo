#!/bin/bash

# ═══════════════════════════════════════════════════════════════════════════════
# Script para generar certificados PKI de desarrollo
# ═══════════════════════════════════════════════════════════════════════════════
# 
# ADVERTENCIA: Estos certificados son SOLO para desarrollo y pruebas.
# En producción, usar certificados firmados por una CA reconocida.
#
# Genera:
# 1. Keystore del cliente (certificado privado + llave privada)
# 2. Truststore del cliente (certificados públicos de servidores)
# 3. Keystore del servidor (para simular el servicio externo)
# 4. Exporta certificados públicos para intercambio
#
# ═══════════════════════════════════════════════════════════════════════════════

set -e

CERT_DIR="./certificates"
KEYSTORE_PASSWORD="changeit"
KEY_PASSWORD="changeit"
VALIDITY_DAYS=365

echo "═══════════════════════════════════════════════════════════════"
echo "Generando certificados PKI para WS-Security (Desarrollo)"
echo "═══════════════════════════════════════════════════════════════"

# Crear directorio de certificados
mkdir -p "$CERT_DIR"
cd "$CERT_DIR"

# ───────────────────────────────────────────────────────────────────────────────
# 1. GENERAR KEYSTORE DEL CLIENTE (GEROS)
# ───────────────────────────────────────────────────────────────────────────────
echo ""
echo "1. Generando keystore del cliente (GEROS)..."
keytool -genkeypair \
    -alias geros-client \
    -keyalg RSA \
    -keysize 4096 \
    -validity $VALIDITY_DAYS \
    -keystore client-keystore.p12 \
    -storetype PKCS12 \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=GEROS Client, OU=IT Department, O=GEROS ERP, L=Barranquilla, ST=Atlantico, C=CO" \
    -ext "SAN=dns:localhost,ip:127.0.0.1"

echo "   ✓ Keystore del cliente creado: client-keystore.p12"

# ───────────────────────────────────────────────────────────────────────────────
# 2. EXPORTAR CERTIFICADO PÚBLICO DEL CLIENTE
# ───────────────────────────────────────────────────────────────────────────────
echo ""
echo "2. Exportando certificado público del cliente..."
keytool -exportcert \
    -alias geros-client \
    -keystore client-keystore.p12 \
    -storetype PKCS12 \
    -storepass "$KEYSTORE_PASSWORD" \
    -file geros-client.cer

echo "   ✓ Certificado público exportado: geros-client.cer"

# ───────────────────────────────────────────────────────────────────────────────
# 3. GENERAR KEYSTORE DEL SERVIDOR (SIMULACIÓN)
# ───────────────────────────────────────────────────────────────────────────────
echo ""
echo "3. Generando keystore del servidor (simulación)..."
keytool -genkeypair \
    -alias server \
    -keyalg RSA \
    -keysize 4096 \
    -validity $VALIDITY_DAYS \
    -keystore server-keystore.p12 \
    -storetype PKCS12 \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=External Service, OU=Services, O=Promigas, L=Barranquilla, ST=Atlantico, C=CO" \
    -ext "SAN=dns:servicios.promigas.com,dns:localhost"

echo "   ✓ Keystore del servidor creado: server-keystore.p12"

# ───────────────────────────────────────────────────────────────────────────────
# 4. EXPORTAR CERTIFICADO PÚBLICO DEL SERVIDOR
# ───────────────────────────────────────────────────────────────────────────────
echo ""
echo "4. Exportando certificado público del servidor..."
keytool -exportcert \
    -alias server \
    -keystore server-keystore.p12 \
    -storetype PKCS12 \
    -storepass "$KEYSTORE_PASSWORD" \
    -file server.cer

echo "   ✓ Certificado público exportado: server.cer"

# ───────────────────────────────────────────────────────────────────────────────
# 5. CREAR TRUSTSTORE DEL CLIENTE
# ───────────────────────────────────────────────────────────────────────────────
echo ""
echo "5. Creando truststore del cliente..."
keytool -importcert \
    -alias server-cert \
    -file server.cer \
    -keystore client-truststore.p12 \
    -storetype PKCS12 \
    -storepass "$KEYSTORE_PASSWORD" \
    -noprompt

echo "   ✓ Truststore del cliente creado: client-truststore.p12"

# ───────────────────────────────────────────────────────────────────────────────
# 6. CREAR TRUSTSTORE DEL SERVIDOR (para mTLS)
# ───────────────────────────────────────────────────────────────────────────────
echo ""
echo "6. Creando truststore del servidor..."
keytool -importcert \
    -alias geros-client-cert \
    -file geros-client.cer \
    -keystore server-truststore.p12 \
    -storetype PKCS12 \
    -storepass "$KEYSTORE_PASSWORD" \
    -noprompt

echo "   ✓ Truststore del servidor creado: server-truststore.p12"

# ───────────────────────────────────────────────────────────────────────────────
# 7. LISTAR CERTIFICADOS GENERADOS
# ───────────────────────────────────────────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "Certificados generados exitosamente"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "Archivos generados:"
echo "  • client-keystore.p12      - Keystore del cliente (privado)"
echo "  • client-truststore.p12    - Truststore del cliente (públicos)"
echo "  • server-keystore.p12      - Keystore del servidor (simulación)"
echo "  • server-truststore.p12    - Truststore del servidor (simulación)"
echo "  • geros-client.cer         - Certificado público del cliente"
echo "  • server.cer               - Certificado público del servidor"
echo ""
echo "Contraseñas (CAMBIAR EN PRODUCCIÓN):"
echo "  • Keystore password: $KEYSTORE_PASSWORD"
echo "  • Key password: $KEY_PASSWORD"
echo ""
echo "Próximos pasos:"
echo "  1. Copiar client-keystore.p12 y client-truststore.p12 a:"
echo "     backend/src/main/resources/certificates/"
echo ""
echo "  2. Configurar application.properties con las rutas correctas"
echo ""
echo "  3. En producción, reemplazar con certificados firmados por CA"
echo ""
echo "═══════════════════════════════════════════════════════════════"

# Mostrar información de los certificados
echo ""
echo "Información del certificado del cliente:"
keytool -list -v -keystore client-keystore.p12 -storetype PKCS12 -storepass "$KEYSTORE_PASSWORD" -alias geros-client | grep -A 5 "Owner:"

echo ""
echo "Información del certificado del servidor:"
keytool -list -v -keystore server-keystore.p12 -storetype PKCS12 -storepass "$KEYSTORE_PASSWORD" -alias server | grep -A 5 "Owner:"
