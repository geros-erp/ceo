package com.geros.backend.policy;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "password_policy", schema = "auth")
public class PasswordPolicy {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "min_length", nullable = false)
    private int minLength = 8;

    @Column(name = "max_length", nullable = false)
    private int maxLength = 64;

    @Column(name = "require_uppercase", nullable = false)
    private boolean requireUppercase = true;

    @Column(name = "require_lowercase", nullable = false)
    private boolean requireLowercase = true;

    @Column(name = "require_numbers", nullable = false)
    private boolean requireNumbers = true;

    @Column(name = "require_special_chars", nullable = false)
    private boolean requireSpecialChars = true;

    @Column(name = "max_failed_attempts", nullable = false)
    private int maxFailedAttempts = 5;

    @Column(name = "expiration_days", nullable = false)
    private int expirationDays = 90;

    @Column(name = "password_history_count", nullable = false)
    private int passwordHistoryCount = 5;

    @Column(name = "lock_duration_minutes", nullable = false)
    private int lockDurationMinutes = 30;

    @Column(name = "max_sequence_length", nullable = false)
    private int maxSequenceLength = 3;

    @Column(name = "notify_before_expiration_days", nullable = false)
    private int notifyBeforeExpirationDays = 7;

    @Column(name = "min_password_age_days", nullable = false)
    private int minPasswordAgeDays = 1;

    @Column(name = "inactivity_lock_days", nullable = false)
    private int inactivityLockDays = 90;

    @Column(name = "max_inactivity_days", nullable = false)
    private int maxInactivityDays = 0;

    @Column(name = "reserved_usernames", columnDefinition = "jsonb default '[]'")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> reservedUsernames = new ArrayList<>();

    @Column(name = "session_timeout_seconds", nullable = false)
    private Integer sessionTimeoutSeconds = 1800;

    public Long getId() { return id; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getMinLength() { return minLength; }
    public void setMinLength(int minLength) { this.minLength = minLength; }

    public int getMaxLength() { return maxLength; }
    public void setMaxLength(int maxLength) { this.maxLength = maxLength; }

    public boolean isRequireUppercase() { return requireUppercase; }
    public void setRequireUppercase(boolean requireUppercase) { this.requireUppercase = requireUppercase; }

    public boolean isRequireLowercase() { return requireLowercase; }
    public void setRequireLowercase(boolean requireLowercase) { this.requireLowercase = requireLowercase; }

    public boolean isRequireNumbers() { return requireNumbers; }
    public void setRequireNumbers(boolean requireNumbers) { this.requireNumbers = requireNumbers; }

    public boolean isRequireSpecialChars() { return requireSpecialChars; }
    public void setRequireSpecialChars(boolean requireSpecialChars) { this.requireSpecialChars = requireSpecialChars; }

    public int getMaxFailedAttempts() { return maxFailedAttempts; }
    public void setMaxFailedAttempts(int maxFailedAttempts) { this.maxFailedAttempts = maxFailedAttempts; }

    public int getExpirationDays() { return expirationDays; }
    public void setExpirationDays(int expirationDays) { this.expirationDays = expirationDays; }

    public int getPasswordHistoryCount() { return passwordHistoryCount; }
    public void setPasswordHistoryCount(int passwordHistoryCount) { this.passwordHistoryCount = passwordHistoryCount; }

    public int getLockDurationMinutes() { return lockDurationMinutes; }
    public void setLockDurationMinutes(int lockDurationMinutes) { this.lockDurationMinutes = lockDurationMinutes; }

    public int getMaxSequenceLength() { return maxSequenceLength; }
    public void setMaxSequenceLength(int maxSequenceLength) { this.maxSequenceLength = maxSequenceLength; }

    public int getNotifyBeforeExpirationDays() { return notifyBeforeExpirationDays; }
    public void setNotifyBeforeExpirationDays(int notifyBeforeExpirationDays) { this.notifyBeforeExpirationDays = notifyBeforeExpirationDays; }

    public int getMinPasswordAgeDays() { return minPasswordAgeDays; }
    public void setMinPasswordAgeDays(int minPasswordAgeDays) { this.minPasswordAgeDays = minPasswordAgeDays; }

    public int getInactivityLockDays() { return inactivityLockDays; }
    public void setInactivityLockDays(int inactivityLockDays) { this.inactivityLockDays = inactivityLockDays; }

    public int getMaxInactivityDays() { return maxInactivityDays; }
    public void setMaxInactivityDays(int maxInactivityDays) { this.maxInactivityDays = maxInactivityDays; }

    public List<String> getReservedUsernames() { return reservedUsernames; }
    public void setReservedUsernames(List<String> reservedUsernames) { this.reservedUsernames = reservedUsernames; }

    public Integer getSessionTimeoutSeconds() { return sessionTimeoutSeconds; }
    public void setSessionTimeoutSeconds(Integer sessionTimeoutSeconds) { this.sessionTimeoutSeconds = sessionTimeoutSeconds; }
}
