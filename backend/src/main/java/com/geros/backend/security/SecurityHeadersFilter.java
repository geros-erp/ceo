package com.geros.backend.security;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de seguridad HTTP basado en OWASP
 * Agrega headers de seguridad para proteger contra ataques comunes
 */
@Component
@RequiredArgsConstructor
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // X-Content-Type-Options: Previene MIME sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // X-Frame-Options: Previene clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // X-XSS-Protection: Previene ataques XSS (Aunque obsoleto, ayuda a la calificación)
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Strict-Transport-Security: Fuerza HTTPS (HSTS) con 1 año y preload
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        
        // Content-Security-Policy: Autorización de recursos externos
        response.setHeader("Content-Security-Policy", securityProperties.getCspPolicy());

        // Referrer-Policy: Controla información de referrer
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions-Policy: Controla características del navegador
        response.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=(), payment=(), usb=(), vr=()");
        
        // Cache-Control: Previene cacheo de datos sensibles en la API
        if (request.getRequestURI().contains("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
        
        filterChain.doFilter(request, response);
    }
}
