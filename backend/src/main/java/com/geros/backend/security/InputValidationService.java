package com.geros.backend.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Servicio de validación y sanitización de entrada basado en OWASP
 * Implementa controles de entrada para prevenir inyecciones y ataques
 */
@Component
public class InputValidationService {

    // Patrones de validación seguros
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,50}$");
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s]+$");
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('.*(--|;|/\\*|\\*/|xp_|sp_|exec|execute|select|insert|update|delete|drop|create|alter|union|script|javascript|onerror|onload).*')|" +
        "(\\b(select|insert|update|delete|drop|create|alter|exec|execute|union|script)\\b)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "<script|javascript:|onerror=|onload=|<iframe|eval\\(|expression\\(|vbscript:",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Valida formato de email
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Valida formato de username
     */
    public boolean isValidUsername(String username) {
        if (username == null || username.isBlank()) return false;
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    /**
     * Valida que el texto sea alfanumérico
     */
    public boolean isAlphanumeric(String text) {
        if (text == null || text.isBlank()) return false;
        return ALPHANUMERIC_PATTERN.matcher(text.trim()).matches();
    }

    /**
     * Detecta intentos de inyección SQL
     */
    public boolean containsSqlInjection(String input) {
        if (input == null || input.isBlank()) return false;
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Detecta intentos de XSS
     */
    public boolean containsXss(String input) {
        if (input == null || input.isBlank()) return false;
        return XSS_PATTERN.matcher(input).find();
    }

    /**
     * Sanitiza entrada removiendo caracteres peligrosos
     */
    public String sanitize(String input) {
        if (input == null) return null;
        
        // Remover caracteres de control
        String sanitized = input.replaceAll("[\\x00-\\x1F\\x7F]", "");
        
        // Escapar caracteres HTML
        sanitized = sanitized
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
        
        return sanitized.trim();
    }

    /**
     * Valida longitud de entrada
     */
    public boolean isValidLength(String input, int minLength, int maxLength) {
        if (input == null) return false;
        int length = input.trim().length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Valida que la entrada no contenga patrones maliciosos
     */
    public void validateInput(String input, String fieldName) {
        if (input == null || input.isBlank()) {
            throw new SecurityException(fieldName + " no puede estar vacío");
        }

        if (containsSqlInjection(input)) {
            throw new SecurityException("Entrada inválida detectada en " + fieldName + ": posible inyección SQL");
        }

        if (containsXss(input)) {
            throw new SecurityException("Entrada inválida detectada en " + fieldName + ": posible ataque XSS");
        }
    }

    /**
     * Valida entrada de texto general
     */
    public String validateAndSanitizeText(String input, String fieldName, int maxLength) {
        validateInput(input, fieldName);
        
        if (!isValidLength(input, 1, maxLength)) {
            throw new SecurityException(fieldName + " debe tener entre 1 y " + maxLength + " caracteres");
        }
        
        return sanitize(input);
    }

    /**
     * Valida y sanitiza email
     */
    public String validateAndSanitizeEmail(String email) {
        if (!isValidEmail(email)) {
            throw new SecurityException("Formato de email inválido");
        }
        return sanitize(email.toLowerCase().trim());
    }

    /**
     * Valida y sanitiza username
     */
    public String validateAndSanitizeUsername(String username) {
        if (!isValidUsername(username)) {
            throw new SecurityException("Username inválido. Solo se permiten letras, números, puntos, guiones y guiones bajos (3-50 caracteres)");
        }
        return sanitize(username.toLowerCase().trim());
    }
}
