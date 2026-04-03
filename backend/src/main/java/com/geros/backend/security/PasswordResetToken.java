package com.geros.backend.security;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens", schema = "auth")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PasswordResetToken() {}

    public PasswordResetToken(String token, String email, LocalDateTime expiresAt) {
        this.token     = token;
        this.email     = email;
        this.expiresAt = expiresAt;
    }

    public Long getId()              { return id; }
    public String getToken()         { return token; }
    public String getEmail()         { return email; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isUsed()          { return used; }
    public void setUsed(boolean used){ this.used = used; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
