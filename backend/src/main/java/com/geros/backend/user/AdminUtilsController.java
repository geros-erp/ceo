package com.geros.backend.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminUtilsController {

    private final UserRepository userRepository;

    public AdminUtilsController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/reset-sessions")
    public ResponseEntity<Map<String, String>> resetAllSessions() {
        userRepository.findAll().forEach(user -> {
            user.setActiveSessions(0);
            userRepository.save(user);
        });
        
        return ResponseEntity.ok(Map.of(
            "message", "Todas las sesiones han sido reseteadas",
            "count", String.valueOf(userRepository.count())
        ));
    }
}
