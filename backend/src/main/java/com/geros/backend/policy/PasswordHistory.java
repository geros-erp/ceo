package com.geros.backend.policy;

import com.geros.backend.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_history", schema = "auth")
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    public PasswordHistory() {}

    public PasswordHistory(User user, String passwordHash) {
        this.user         = user;
        this.passwordHash = passwordHash;
        this.changedAt    = LocalDateTime.now();
    }

    public Long getId()              { return id; }
    public User getUser()            { return user; }
    public String getPasswordHash()  { return passwordHash; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
