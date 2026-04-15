package com.geros.backend.security;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Centraliza la configuración de seguridad y valida la sintaxis al inicio.
 */
@Component
@ConfigurationProperties(prefix = "app.security")
@Data
@Slf4j
public class SecurityProperties {

    private String cspPolicy = "default-src 'self'";

    @PostConstruct
    public void validate() {
        if (cspPolicy == null || cspPolicy.isBlank()) {
            throw new IllegalStateException("Configuración crítica faltante: app.security.csp-policy no puede estar vacía.");
        }

        // 1. Verificar presencia de default-src (Recomendación OWASP)
        if (!cspPolicy.contains("default-src")) {
            log.warn("⚠️ Seguridad: La política CSP no define 'default-src'. Se recomienda establecer una base restrictiva.");
        }

        // 2. Verificar separadores (Error común: usar comas en lugar de puntos y coma)
        if (cspPolicy.contains(",") && !cspPolicy.contains(";")) {
            throw new IllegalArgumentException("Error en CSP: Las directivas deben separarse por ';' (punto y coma), no por comas.");
        }

        // 3. Verificar comillas en palabras clave comunes
        String[] keywords = {"self", "none", "unsafe-inline", "unsafe-eval", "strict-dynamic"};
        for (String keyword : keywords) {
            // Verifica si la palabra clave existe sin estar rodeada de comillas simples
            // Ejemplo: script-src self vs script-src 'self'
            String patternWithoutQuotes = "(?<!')\\b" + keyword + "\\b(?!')";
            if (cspPolicy.matches(".*" + patternWithoutQuotes + ".*")) {
                log.warn("⚠️ Sintaxis CSP: La palabra clave '{}' debería ir entre comillas simples en la propiedad app.security.csp-policy.", keyword);
            }
        }

        // 4. Validar terminación (opcional, pero ayuda a la legibilidad)
        if (cspPolicy.trim().endsWith(",")) {
            throw new IllegalArgumentException("Error en CSP: La política termina con una coma inválida.");
        }

        log.info("✅ Content-Security-Policy cargada y validada estructuralmente.");
    }
}