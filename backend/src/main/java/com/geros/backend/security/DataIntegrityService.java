package com.geros.backend.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Servicio de integridad de datos basado en OWASP
 * Implementa controles para garantizar la integridad de datos críticos
 */
@Component
public class DataIntegrityService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String SECRET_KEY = "geros-integrity-key-2024"; // En producción usar variable de entorno

    /**
     * Genera hash SHA-256 de datos
     */
    public String generateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException("Error al generar hash: " + e.getMessage());
        }
    }

    /**
     * Genera HMAC para verificar integridad
     */
    public String generateHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8), 
                HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (Exception e) {
            throw new SecurityException("Error al generar HMAC: " + e.getMessage());
        }
    }

    /**
     * Verifica HMAC de datos
     */
    public boolean verifyHmac(String data, String expectedHmac) {
        String actualHmac = generateHmac(data);
        return MessageDigest.isEqual(
            actualHmac.getBytes(StandardCharsets.UTF_8),
            expectedHmac.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Genera checksum para verificación de integridad
     */
    public String generateChecksum(Object... fields) {
        StringBuilder data = new StringBuilder();
        for (Object field : fields) {
            if (field != null) {
                data.append(field.toString()).append("|");
            }
        }
        return generateHash(data.toString());
    }

    /**
     * Verifica checksum de datos
     */
    public boolean verifyChecksum(String expectedChecksum, Object... fields) {
        String actualChecksum = generateChecksum(fields);
        return MessageDigest.isEqual(
            actualChecksum.getBytes(StandardCharsets.UTF_8),
            expectedChecksum.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Valida integridad de datos críticos antes de procesamiento
     */
    public void validateDataIntegrity(String data, String signature) {
        if (data == null || data.isBlank()) {
            throw new SecurityException("Datos vacíos no pueden ser validados");
        }

        if (signature == null || signature.isBlank()) {
            throw new SecurityException("Firma de integridad requerida");
        }

        if (!verifyHmac(data, signature)) {
            throw new SecurityException("Integridad de datos comprometida: firma inválida");
        }
    }

    /**
     * Genera firma para datos críticos
     */
    public String signData(String data) {
        if (data == null || data.isBlank()) {
            throw new SecurityException("No se pueden firmar datos vacíos");
        }
        return generateHmac(data);
    }

    /**
     * Verifica que los datos no hayan sido modificados
     */
    public boolean isDataTampered(String originalHash, String currentData) {
        String currentHash = generateHash(currentData);
        return !MessageDigest.isEqual(
            originalHash.getBytes(StandardCharsets.UTF_8),
            currentHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}
