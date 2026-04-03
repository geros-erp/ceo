package com.geros.backend.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Servicio de sanitización de salida basado en OWASP
 * Implementa controles de salida para prevenir XSS y exposición de datos sensibles
 */
@Component
public class OutputSanitizationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"
    );
    
    private static final Pattern IP_PATTERN = Pattern.compile(
        "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"
    );

    /**
     * Enmascara emails para logs y respuestas
     * ejemplo@dominio.com -> e*****o@d****o.com
     */
    public String maskEmail(String email) {
        if (email == null || email.isBlank()) return email;
        
        return EMAIL_PATTERN.matcher(email).replaceAll(match -> {
            String local = match.group(1);
            String domain = match.group(2);
            
            String maskedLocal = local.length() > 2 
                ? local.charAt(0) + "*****" + local.charAt(local.length() - 1)
                : "***";
            
            String[] domainParts = domain.split("\\.");
            String maskedDomain = domainParts[0].charAt(0) + "****" + 
                (domainParts.length > 1 ? "." + domainParts[domainParts.length - 1] : "");
            
            return maskedLocal + "@" + maskedDomain;
        });
    }

    /**
     * Enmascara números de teléfono
     * 1234567890 -> ***-***-7890
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return phone;
        return PHONE_PATTERN.matcher(phone).replaceAll("***-***-$3");
    }

    /**
     * Enmascara direcciones IP parcialmente
     * 192.168.1.100 -> 192.168.***.***
     */
    public String maskIpPartial(String ip) {
        if (ip == null || ip.isBlank()) return ip;
        return IP_PATTERN.matcher(ip).replaceAll(match -> {
            String[] parts = match.group().split("\\.");
            return parts[0] + "." + parts[1] + ".***.***";
        });
    }

    /**
     * Enmascara tokens JWT (muestra solo primeros y últimos caracteres)
     */
    public String maskToken(String token) {
        if (token == null || token.isBlank()) return token;
        if (token.length() <= 20) return "***";
        return token.substring(0, 10) + "..." + token.substring(token.length() - 10);
    }

    /**
     * Enmascara contraseñas completamente
     */
    public String maskPassword(String password) {
        if (password == null || password.isBlank()) return "";
        return "********";
    }

    /**
     * Escapa HTML para prevenir XSS en salida
     */
    public String escapeHtml(String input) {
        if (input == null) return null;
        
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
    }

    /**
     * Escapa JSON para prevenir inyección en respuestas JSON
     */
    public String escapeJson(String input) {
        if (input == null) return null;
        
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\f", "\\f");
    }

    /**
     * Sanitiza mensaje de error para no exponer información sensible
     */
    public String sanitizeErrorMessage(String errorMessage) {
        if (errorMessage == null) return "Error interno del servidor";
        
        // Remover stack traces
        if (errorMessage.contains("at ") && errorMessage.contains(".java:")) {
            return "Error interno del servidor";
        }
        
        // Remover rutas de archivos
        errorMessage = errorMessage.replaceAll("[A-Za-z]:\\\\[^\\s]+", "[PATH]");
        errorMessage = errorMessage.replaceAll("/[^\\s]+\\.java", "[FILE]");
        
        // Remover información de base de datos
        errorMessage = errorMessage.replaceAll("SQL.*?;", "[SQL]");
        errorMessage = errorMessage.replaceAll("database.*?\\s", "[DB] ");
        
        // Limitar longitud
        if (errorMessage.length() > 200) {
            errorMessage = errorMessage.substring(0, 200) + "...";
        }
        
        return errorMessage;
    }

    /**
     * Sanitiza texto para logs (enmascara datos sensibles)
     */
    public String sanitizeForLog(String text) {
        if (text == null) return null;
        
        String sanitized = text;
        
        // Enmascarar emails
        sanitized = maskEmail(sanitized);
        
        // Enmascarar teléfonos
        sanitized = maskPhone(sanitized);
        
        // Enmascarar IPs
        sanitized = maskIpPartial(sanitized);
        
        // Enmascarar posibles tokens
        if (sanitized.contains("Bearer ")) {
            sanitized = sanitized.replaceAll("Bearer [A-Za-z0-9._-]+", "Bearer [TOKEN]");
        }
        
        // Enmascarar posibles contraseñas en JSON
        sanitized = sanitized.replaceAll("\"password\"\\s*:\\s*\"[^\"]+\"", "\"password\":\"***\"");
        sanitized = sanitized.replaceAll("\"newPassword\"\\s*:\\s*\"[^\"]+\"", "\"newPassword\":\"***\"");
        sanitized = sanitized.replaceAll("\"oldPassword\"\\s*:\\s*\"[^\"]+\"", "\"oldPassword\":\"***\"");
        
        return sanitized;
    }

    /**
     * Valida que la salida no contenga información sensible
     */
    public void validateOutput(String output) {
        if (output == null) return;
        
        // Verificar que no contenga stack traces
        if (output.contains(".java:") && output.contains("at ")) {
            throw new SecurityException("La salida contiene información sensible (stack trace)");
        }
        
        // Verificar que no contenga rutas de archivos del sistema
        if (output.matches(".*[A-Za-z]:\\\\.*") || output.matches(".*/home/.*") || output.matches(".*/usr/.*")) {
            throw new SecurityException("La salida contiene rutas del sistema");
        }
    }

    /**
     * Prepara respuesta segura removiendo campos sensibles
     */
    public String prepareSecureResponse(String response) {
        if (response == null) return null;
        
        // Escapar HTML
        String secure = escapeHtml(response);
        
        // Validar salida
        validateOutput(secure);
        
        return secure;
    }
}
