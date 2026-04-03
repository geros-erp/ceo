package com.geros.backend.user;

import com.geros.backend.role.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad de Usuario del Sistema.
 * 
 * POLÍTICA DE USUARIOS ÚNICOS:
 * - Cada funcionario DEBE tener un usuario único e inmutable
 * - El username NO se puede modificar después de la creación
 * - NO se permiten usuarios compartidos entre funcionarios
 * - NO se permiten usuarios genéricos (admin, guest, test, etc)
 * - El username garantiza la identificación única del funcionario
 * 
 * El username y email son identificadores únicos que no pueden cambiar.
 * Esto cumple con estándares de auditoría y no-repudio.
 */
@Entity
@Table(name = "users", schema = "auth")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Email @NotBlank
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles", schema = "auth",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt = LocalDateTime.now();

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "current_login_at")
    private LocalDateTime currentLoginAt;

    @Column(name = "previous_login_at")
    private LocalDateTime previousLoginAt;

    @Column(name = "current_login_ip", length = 100)
    private String currentLoginIp;

    @Column(name = "previous_login_ip", length = 100)
    private String previousLoginIp;

    @Column(name = "active_sessions", nullable = false)
    private int activeSessions = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "is_default_user", nullable = false)
    private boolean isDefaultUser = false;

    public User() {}

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }

    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getCurrentLoginAt() { return currentLoginAt; }
    public void setCurrentLoginAt(LocalDateTime currentLoginAt) { this.currentLoginAt = currentLoginAt; }

    public LocalDateTime getPreviousLoginAt() { return previousLoginAt; }
    public void setPreviousLoginAt(LocalDateTime previousLoginAt) { this.previousLoginAt = previousLoginAt; }

    public String getCurrentLoginIp() { return currentLoginIp; }
    public void setCurrentLoginIp(String currentLoginIp) { this.currentLoginIp = currentLoginIp; }

    public String getPreviousLoginIp() { return previousLoginIp; }
    public void setPreviousLoginIp(String previousLoginIp) { this.previousLoginIp = previousLoginIp; }

    public int getActiveSessions() { return activeSessions; }
    public void setActiveSessions(int activeSessions) { this.activeSessions = activeSessions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public boolean isDefaultUser() { return isDefaultUser; }
    public void setDefaultUser(boolean defaultUser) { isDefaultUser = defaultUser; }

    public boolean isLocked() { return lockedAt != null; }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equals(roleName));
    }

    @PreUpdate
    public void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
