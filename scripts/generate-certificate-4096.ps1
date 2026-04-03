# generate-certificate-4096.ps1
# Script para generar certificado autofirmado X.509 v3 con llave de 4096 bits

# Configuracion
$Alias = "geros"
$KeystoreFile = "geros-keystore-4096.p12"
$KeystorePassword = "geros2024"
$ValidityDays = 365
$KeySize = 4096
$SignatureAlgorithm = "SHA256withRSA"

# Informacion del certificado
$CommonName = "geros.promigas.local"
$OrganizationalUnit = "ITSecurity"
$Organization = "Promigas"
$Locality = "Barranquilla"
$State = "Atlantico"
$Country = "CO"

# Subject Alternative Names
$SanDns1 = "geros.promigas.local"
$SanDns2 = "localhost"
$SanIp = "127.0.0.1"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Generador de Certificado SSL/TLS" -ForegroundColor Cyan
Write-Host "X.509 v3 con llave RSA de 4096 bits" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar si keytool esta disponible
$keytoolPath = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytoolPath) {
    Write-Host "Error: keytool no encontrado" -ForegroundColor Red
    Write-Host "Instalar Java JDK para continuar"
    exit 1
}

# Verificar si el archivo ya existe
if (Test-Path $KeystoreFile) {
    Write-Host "Advertencia: El archivo $KeystoreFile ya existe" -ForegroundColor Yellow
    $response = Read-Host "Desea sobrescribirlo? (s/n)"
    if ($response -ne 's' -and $response -ne 'S') {
        Write-Host "Operacion cancelada"
        exit 0
    }
    Remove-Item $KeystoreFile -Force
}

Write-Host "Generando certificado..." -ForegroundColor Green
Write-Host "  - Tamaño de llave: $KeySize bits"
Write-Host "  - Algoritmo de firma: $SignatureAlgorithm"
Write-Host "  - Validez: $ValidityDays dias"
Write-Host "  - Common Name: $CommonName"
Write-Host ""

# Construir comando keytool
$dname = "CN=$CommonName,OU=$OrganizationalUnit,O=$Organization,L=$Locality,ST=$State,C=$Country"
$san = "SAN=dns:$SanDns1,dns:$SanDns2,ip:$SanIp"

# Generar certificado
$process = Start-Process -FilePath "keytool" -ArgumentList @(
    "-genkeypair",
    "-alias", $Alias,
    "-keyalg", "RSA",
    "-keysize", $KeySize,
    "-sigalg", $SignatureAlgorithm,
    "-validity", $ValidityDays,
    "-keystore", $KeystoreFile,
    "-storetype", "PKCS12",
    "-storepass", $KeystorePassword,
    "-dname", $dname,
    "-ext", $san,
    "-ext", "KeyUsage=digitalSignature,keyEncipherment",
    "-ext", "ExtendedKeyUsage=serverAuth,clientAuth"
) -Wait -PassThru -NoNewWindow

if ($process.ExitCode -eq 0) {
    Write-Host ""
    Write-Host "Certificado generado exitosamente!" -ForegroundColor Green
    Write-Host ""
    
    # Verificar certificado
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "Verificacion del Certificado" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Obtener informacion del certificado
    $certInfo = & keytool -list -v -keystore $KeystoreFile -storepass $KeystorePassword 2>$null
    
    # Verificar tamaño de llave
    if ($certInfo -match "4096") {
        Write-Host "OK Tamaño de llave: 4096 bits" -ForegroundColor Green
    } else {
        Write-Host "X Error: Tamaño de llave incorrecto" -ForegroundColor Red
    }
    
    # Verificar version X.509
    if ($certInfo -match "Version.*3") {
        Write-Host "OK Version: X.509 v3" -ForegroundColor Green
    } else {
        Write-Host "X Error: Version X.509 incorrecta" -ForegroundColor Red
    }
    
    # Verificar algoritmo de firma
    if ($certInfo -match "SHA256withRSA") {
        Write-Host "OK Algoritmo de firma: SHA256withRSA" -ForegroundColor Green
    } else {
        Write-Host "X Error: Algoritmo de firma incorrecto" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "Informacion del Certificado" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    $certInfo | Select-Object -First 30
    
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "Archivos Generados" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "  - Keystore: $KeystoreFile"
    Write-Host "  - Password: $KeystorePassword"
    Write-Host "  - Alias: $Alias"
    Write-Host ""
    
    # Exportar certificado publico
    $CertFile = $KeystoreFile -replace '.p12$', '.pem'
    & keytool -exportcert `
      -alias $Alias `
      -keystore $KeystoreFile `
      -storepass $KeystorePassword `
      -rfc `
      -file $CertFile 2>$null
    
    if (Test-Path $CertFile) {
        Write-Host "  - Certificado publico: $CertFile"
        Write-Host ""
        Write-Host "Para instalar en clientes:" -ForegroundColor Yellow
        Write-Host "  Windows: certutil -addstore -enterprise -f `"Root`" $CertFile"
        Write-Host "  Linux:   sudo cp $CertFile /usr/local/share/ca-certificates/ && sudo update-ca-certificates"
        Write-Host "  macOS:   sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain $CertFile"
    }
    
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "Configuracion Spring Boot" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "Agregar a application.properties:"
    Write-Host ""
    Write-Host "server.ssl.enabled=true"
    Write-Host "server.ssl.key-store=classpath:$KeystoreFile"
    Write-Host "server.ssl.key-store-password=$KeystorePassword"
    Write-Host "server.ssl.key-store-type=PKCS12"
    Write-Host "server.ssl.key-alias=$Alias"
    Write-Host "server.ssl.protocol=TLS"
    Write-Host "server.ssl.enabled-protocols=TLSv1.2,TLSv1.3"
    Write-Host ""
    
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "Siguiente Paso" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "Copiar $KeystoreFile a:" -ForegroundColor Yellow
    Write-Host "  backend\src\main\resources\$KeystoreFile"
    Write-Host ""
    
} else {
    Write-Host ""
    Write-Host "Error al generar certificado" -ForegroundColor Red
    exit 1
}
