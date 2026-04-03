package com.geros.backend.security;

import com.geros.backend.user.User;
import com.geros.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuthenticationSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();

        userRepository.findByEmail(email).ifPresent(user -> {
            // Actualizar la fecha de último acceso
            user.setLastLoginAt(LocalDateTime.now());
            // También aprovechamos para resetear intentos fallidos si los hubiera
            user.setFailedAttempts(0);
            user.setLockedAt(null);
            userRepository.save(user);
        });
    }
}