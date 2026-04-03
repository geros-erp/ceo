#!/bin/bash
# generate-csr-promigas.sh
# Script para generar CSR (Certificate Signing Request) para CA de Promigas

# Configuracion
PRIVATE_KEY_FILE="geros-private-4096.key"
CSR_FILE="geros-promigas.csr"
KEY_SIZE=4096

# Informacion del certificado
COMMON_NAME="geros.promigas.local"
ORGANIZATIONAL_UNIT="IT Security"
ORGANIZATION="Promigas S.A. E.S.P."
LOCALITY="Barranquilla"
STATE="Atlantico"
COUNTRY="CO"
EMAIL="admin@promigas.com"

# Subject Alternative Names
SAN_DNS="geros.promigas.local,localhost,geros-app.promigas.local"
SAN_IP="127.0.0.1,10.0.0.1"

echo "=========================================="
echo "Generador de CSR para CA de Promigas"
echo "Llave RSA de 4096 bits"
echo "=========================================="
echo ""

# Verificar si openssl esta disponible
if ! command -v openssl &> /dev/null; then
    echo "Error: openssl no encontrado"
    echo "Instalar OpenSSL para continuar"
    exit 1
fi

# Verificar si los archivos ya existen
if [ -f "$PRIVATE_KEY_FILE" ]; then
    echo "Advertencia: La llave privada $PRIVATE_KEY_FILE ya existe"
    read -p "Desea sobrescribirla? (s/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
        echo "Usando llave existente"
    else
        rm -f "$PRIVATE_KEY_FILE"
    fi
fi

# Generar llave privada si no existe
if [ ! -f "$PRIVATE_KEY_FILE" ]; then
    echo "Generando llave privada de $KEY_SIZE bits..."
    openssl genrsa -out "$PRIVATE_KEY_FILE" $KEY_SIZE
    
    if [ $? -eq 0 ]; then
        echo "✓ Llave privada generada: $PRIVATE_KEY_FILE"
        chmod 600 "$PRIVATE_KEY_FILE"
    else
        echo "✗ Error al generar llave privada"
        exit 1
    fi
fi

echo ""
echo "Generando CSR..."
echo "  - Common Name: $COMMON_NAME"
echo "  - Organization: $ORGANIZATION"
echo "  - Organizational Unit: $ORGANIZATIONAL_UNIT"
echo ""

# Crear archivo de configuracion temporal para extensiones
CONFIG_FILE=$(mktemp)
cat > "$CONFIG_FILE" << EOF
[req]
default_bits = $KEY_SIZE
prompt = no
default_md = sha256
req_extensions = req_ext
distinguished_name = dn

[dn]
C = $COUNTRY
ST = $STATE
L = $LOCALITY
O = $ORGANIZATION
OU = $ORGANIZATIONAL_UNIT
CN = $COMMON_NAME
emailAddress = $EMAIL

[req_ext]
subjectAltName = @alt_names
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth, clientAuth

[alt_names]
DNS.1 = geros.promigas.local
DNS.2 = localhost
DNS.3 = geros-app.promigas.local
IP.1 = 127.0.0.1
IP.2 = 10.0.0.1
EOF

# Generar CSR
openssl req -new \
  -key "$PRIVATE_KEY_FILE" \
  -out "$CSR_FILE" \
  -config "$CONFIG_FILE"

# Limpiar archivo temporal
rm -f "$CONFIG_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ CSR generado exitosamente!"
    echo ""
    
    # Verificar CSR
    echo "=========================================="
    echo "Verificacion del CSR"
    echo "=========================================="
    echo ""
    
    # Verificar tamaño de llave
    KEY_LENGTH=$(openssl req -in "$CSR_FILE" -noout -text | grep "Public-Key" | grep -o "[0-9]*" | head -1)
    if [ "$KEY_LENGTH" == "4096" ]; then
        echo "✓ Tamaño de llave: 4096 bits"
    else
        echo "✗ Error: Tamaño de llave incorrecto ($KEY_LENGTH bits)"
    fi
    
    # Verificar algoritmo de firma
    SIG_ALG=$(openssl req -in "$CSR_FILE" -noout -text | grep "Signature Algorithm" | head -1)
    if [[ $SIG_ALG == *"sha256"* ]]; then
        echo "✓ Algoritmo de firma: SHA-256"
    else
        echo "✗ Error: Algoritmo de firma incorrecto"
    fi
    
    # Verificar Subject
    echo ""
    echo "Subject:"
    openssl req -in "$CSR_FILE" -noout -subject
    
    # Verificar extensiones
    echo ""
    echo "Extensiones solicitadas:"
    openssl req -in "$CSR_FILE" -noout -text | grep -A 10 "Requested Extensions"
    
    echo ""
    echo "=========================================="
    echo "Archivos Generados"
    echo "=========================================="
    echo "  - Llave privada: $PRIVATE_KEY_FILE (MANTENER SEGURA)"
    echo "  - CSR: $CSR_FILE (ENVIAR A CA DE PROMIGAS)"
    echo ""
    
    echo "=========================================="
    echo "Contenido del CSR"
    echo "=========================================="
    cat "$CSR_FILE"
    echo ""
    
    echo "=========================================="
    echo "Siguiente Paso"
    echo "=========================================="
    echo "1. Enviar $CSR_FILE a la CA de Promigas"
    echo "2. Proporcionar la siguiente informacion:"
    echo ""
    echo "   Common Name (CN): $COMMON_NAME"
    echo "   Organization (O): $ORGANIZATION"
    echo "   Organizational Unit (OU): $ORGANIZATIONAL_UNIT"
    echo "   Locality (L): $LOCALITY"
    echo "   State (ST): $STATE"
    echo "   Country (C): $COUNTRY"
    echo "   Email: $EMAIL"
    echo "   Key Size: $KEY_SIZE bits"
    echo "   Signature Algorithm: SHA-256 with RSA"
    echo "   Validity: 1 año"
    echo ""
    echo "3. Esperar certificado firmado de CA de Promigas"
    echo "4. Recibir archivos:"
    echo "   - geros-signed.crt (certificado firmado)"
    echo "   - promigas-ca.crt (certificado intermedio)"
    echo "   - promigas-root-ca.crt (certificado raiz)"
    echo ""
    echo "5. Crear keystore con:"
    echo "   ./create-keystore-from-ca.sh"
    echo ""
    
    # Guardar informacion en archivo
    INFO_FILE="${CSR_FILE%.csr}-info.txt"
    cat > "$INFO_FILE" << EOF
CSR Information for Promigas CA
================================

Generated: $(date)

Files:
- Private Key: $PRIVATE_KEY_FILE (KEEP SECURE - DO NOT SHARE)
- CSR: $CSR_FILE (SEND TO PROMIGAS CA)

Certificate Details:
- Common Name (CN): $COMMON_NAME
- Organization (O): $ORGANIZATION
- Organizational Unit (OU): $ORGANIZATIONAL_UNIT
- Locality (L): $LOCALITY
- State (ST): $STATE
- Country (C): $COUNTRY
- Email: $EMAIL
- Key Size: $KEY_SIZE bits
- Signature Algorithm: SHA-256 with RSA
- Requested Validity: 1 year

Subject Alternative Names:
- DNS: $SAN_DNS
- IP: $SAN_IP

Next Steps:
1. Send $CSR_FILE to Promigas CA
2. Wait for signed certificate
3. Receive: geros-signed.crt, promigas-ca.crt, promigas-root-ca.crt
4. Create keystore with create-keystore-from-ca.sh

IMPORTANT: Keep $PRIVATE_KEY_FILE secure and backed up!
EOF
    
    echo "Informacion guardada en: $INFO_FILE"
    echo ""
    
else
    echo ""
    echo "✗ Error al generar CSR"
    exit 1
fi
