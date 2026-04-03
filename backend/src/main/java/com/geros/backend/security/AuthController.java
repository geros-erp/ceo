package com.geros.backend.security;

import com.geros.backend.user.UserDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationValidator authValidator;

    public AuthController(AuthService authService, AuthenticationValidator authValidator) {
        this.authService = authService;
        this.authValidator = authValidator;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTO.LoginResponse> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDTO.LoginResponse> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody UserDTO.ChangePasswordRequest request) {
        String email = authValidator.getAuthenticatedUser(authentication);
        authService.changePassword(email, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody AuthDTO.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message",
                "Si el correo existe, recibirá un enlace para restablecer su contraseña"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody AuthDTO.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Contraseña restablecida correctamente"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        // Permitir logout incluso si la autenticación falló o el token expiró
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            try {
                authService.logout(authentication.getName());
            } catch (Exception e) {
                // Ignorar errores en logout (ej: usuario no encontrado)
                // El cliente ya limpió su sesión local
            }
        }
        return ResponseEntity.noContent().build();
    }
}
