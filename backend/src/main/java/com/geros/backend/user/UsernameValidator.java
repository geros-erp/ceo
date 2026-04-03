package com.geros.backend.user;

import com.geros.backend.policy.ReservedUsernameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Validador de usernames para garantizar que no sean genéricos o compartidos.
 * Cumple con la política de usuarios únicos por funcionario.
 * Los usernames reservados se cargan desde la configuración de política de contraseñas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsernameValidator {

    private final ReservedUsernameRepository reservedUsernameRepository;

    // Fallback en caso de que la política no esté configurada
    private static final Set<String> DEFAULT_RESERVED_USERNAMES = Set.of(
        "admin", "administrator", "root", "superuser",
        "guest", "test", "demo", "example",
        "user", "users", "account", "accounts",
        "system", "app", "application",
        "support", "help", "info", "contact",
        "shared", "group", "team", "common",
        "default", "generic", "temporal", "temp",
        "service", "bot", "api", "anonymous",
        "unknown", "null", "undefined",
        "password", "username", "credentials",
        "111", "222", "333", "444", "555", "666", "777", "888", "999",
        "0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999"
    );

    /**
     * Valida que el username no sea genérico ni compartido.
     * Obtiene la lista de usernames reservados desde la política de contraseñas.
     * @param username Username a validar
     * @throws IllegalArgumentException si no es válido
     */
    public void validate(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El usuario no puede estar vacío");
        }

        String normalized = username.toLowerCase().trim();

        // Obtener usernames reservados desde la política
        Set<String> reservedUsernames = getReservedUsernames();

        // Validar que no sea reservado/genérico
        if (reservedUsernames.contains(normalized)) {
            throw new IllegalArgumentException(
                "El usuario '" + username + "' no está permitido. " +
                "Debe usar un nombre personalizado único para el funcionario (ej: jperez, mgarcia123)");
        }

        // Validar que no sea solo números
        if (normalized.matches("^\\d+$")) {
            throw new IllegalArgumentException(
                "El usuario no puede contener solo números. " +
                "Debe incluir al menos una letra (ej: emp001 no es válido, aber001 sí)");
        }

        // Validar longitud mínima
        if (normalized.length() < 4) {
            throw new IllegalArgumentException(
                "El usuario debe tener al menos 4 caracteres");
        }

        // Validar caracteres válidos (solo letras, números y guiones/guiones bajos)
        if (!normalized.matches("^[a-z0-9_-]+$")) {
            throw new IllegalArgumentException(
                "El usuario solo puede contener letras, números, guiones (-) y guiones bajos (_)");
        }
    }

    /**
     * Obtiene la lista de usernames reservados desde la política de contraseñas.
     * Si la política no tiene configurados, usa los valores por defecto.
     * @return Set de usernames reservados (minúsculas)
     */
    private Set<String> getReservedUsernames() {
        try {
            Set<String> dbReserved = reservedUsernameRepository.findAllUsernamesLower();
            if (dbReserved.isEmpty()) {
                return DEFAULT_RESERVED_USERNAMES;
            }
            Set<String> allReserved = new HashSet<>(DEFAULT_RESERVED_USERNAMES);
            allReserved.addAll(dbReserved);
            return allReserved;
        } catch (Exception e) {
            log.warn("Error obteniendo usernames reservados de la política, usando valores por defecto", e);
        }
        return DEFAULT_RESERVED_USERNAMES;
    }
}
