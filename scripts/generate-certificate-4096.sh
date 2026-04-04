#!/bin/bash
# generate-certificate-4096.sh
# Script para generar certificado autofirmado X.509 v3 con llave de 4096 bits

# Configuracion
ALIAS="geros"
KEYSTORE_FILE="geros-keystore-4096.p12"
KEYSTORE_PASSWORD="geros2024"
VALIDITY_DAYS=365
KEY_SIZE=4096
SIGNATURE_ALGORITHM="SHA256withRSA"

# Informacion del certificado
COMMON_NAME="geros.promigas.local"
ORGANIZATIONAL_UNIT="IT Security"
ORGANIZATION="Promigas"
LOCALITY="Barranquilla"
STATE="Atlantico"
COUNTRY="CO"

# Subject Alternative Names
SAN_DNS="geros.promigas.local,localhost"
SAN_IP="127.0.0.1"

echo "=========================================="
echo "Generador de Certificado SSL/TLS"
echo "X.509 v3 con llave RSA de 4096 bits"
echo "=========================================="
echo ""

# Verificar si keytool esta disponible
if ! command -v keytool &> /dev/null; then
    echo "Error: keytool no encontrado"
    echo "Instalar Java JDK para continuar"
    exit 1
fi

# Verificar si el archivo ya existe
if [ -f "$KEYSTORE_FILE" ]; then
    echo "Advertencia: El archivo $KEYSTORE_FILE ya existe"
    read -p "Desea sobrescribirlo? (s/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
        echo "Operacion cancelada"
        exit 0
    fi
    rm -f "$KEYSTORE_FILE"
fi

echo "Generando certificado..."
echo "  - Tamaño de llave: $KEY_SIZE bits"
echo "  - Algoritmo de firma: $SIGNATURE_ALGORITHM"
echo "  - Validez: $VALIDITY_DAYS dias"
echo "  - Common Name: $COMMON_NAME"
echo ""

# Generar certificado
keytool -genkeypair \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize $KEY_SIZE \
  -sigalg $SIGNATURE_ALGORITHM \
  -validity $VALIDITY_DAYS \
  -keystore "$KEYSTORE_FILE" \
  -storetype PKCS12 \
  -storepass "$KEYSTORE_PASSWORD" \
  -dname "CN=$COMMON_NAME, OU=$ORGANIZATIONAL_UNIT, O=$ORGANIZATION, L=$LOCALITY, ST=$STATE, C=$COUNTRY" \
  -ext "SAN=dns:$SAN_DNS,ip:$SAN_IP" \
  -ext "KeyUsage=digitalSignature,keyEncipherment" \
  -ext "ExtendedKeyUsage=serverAuth,clientAuth"

if [ $? -eq 0 ]; then
    echo ""
    echo "Certificado generado exitosamente!"
    echo ""
    
    # Verificar certificado
    echo "=========================================="
    echo "Verificacion del Certificado"
    echo "=========================================="
    echo ""
    
    # Verificar tamaño de llave
    KEY_LENGTH=$(keytool -list -v -keystore "$KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD" 2>/dev/null | grep -o "4096" | head -1)
    if [ "$KEY_LENGTH" == "4096" ]; then
        echo "✓ Tamaño de llave: 4096 bits"
    else
        echo "✗ Error: Tamaño de llave incorrecto"
    fi
    
    # Verificar version X.509
    VERSION=$(keytool -list -v -keystore "$KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD" 2>/dev/null | grep "Version" | head -1)
    if [[ $VERSION == *"3"* ]]; then
        echo "✓ Version: X.509 v3"
    else
        echo "✗ Error: Version X.509 incorrecta"
    fi
    
    # Verificar algoritmo de firma
    SIG_ALG=$(keytool -list -v -keystore "$KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD" 2>/dev/null | grep "Signature algorithm name" | head -1)
    if [[ $SIG_ALG == *"SHA256withRSA"* ]]; then
        echo "✓ Algoritmo de firma: SHA256withRSA"
    else
        echo "✗ Error: Algoritmo de firma incorrecto"
    fi
    
    echo ""
    echo "=========================================="
    echo "Informacion del Certificado"
    echo "=========================================="
    keytool -list -v -keystore "$KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD" 2>/dev/null | head -30
    
    echo ""
    echo "=========================================="
    echo "Archivos Generados"
    echo "=========================================="
    echo "  - Keystore: $KEYSTORE_FILE"
    echo "  - Password: $KEYSTORE_PASSWORD"
    echo "  - Alias: $ALIAS"
    echo ""
    
    # Exportar certificado publico
    CERT_FILE="${KEYSTORE_FILE%.p12}.pem"
    keytool -exportcert \
      -alias "$ALIAS" \
      -keystore "$KEYSTORE_FILE" \
      -storepass "$KEYSTORE_PASSWORD" \
      -rfc \
      -file "$CERT_FILE" 2>/dev/null
    
    if [ -f "$CERT_FILE" ]; then
        echo "  - Certificado publico: $CERT_FILE"
        echo ""
        echo "Para instalar en clientes:"
        echo "  Windows: certutil -addstore -enterprise -f \"Root\" $CERT_FILE"
        echo "  Linux:   sudo cp $CERT_FILE /usr/local/share/ca-certificates/ && sudo update-ca-certificates"
        echo "  macOS:   sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain $CERT_FILE"
    fi
    
    echo ""
    echo "=========================================="
    echo "Configuracion Spring Boot"
    echo "=========================================="
    echo "Agregar a application.properties:"
    echo ""
    echo "server.ssl.enabled=true"
    echo "server.ssl.key-store=classpath:$KEYSTORE_FILE"
    echo "server.ssl.key-store-password=$KEYSTORE_PASSWORD"
    echo "server.ssl.key-store-type=PKCS12"
    echo "server.ssl.key-alias=$ALIAS"
    echo "server.ssl.protocol=TLS"
    echo "server.ssl.enabled-protocols=TLSv1.2,TLSv1.3"
    echo ""
    
else
    echo ""
    echo "Error al generar certificado"
    exit 1
fi
