package com.geros.backend.policy;

import com.geros.backend.user.User;
import com.geros.backend.securitylog.SecurityLog;
import com.geros.backend.securitylog.SecurityLogService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PasswordPolicyService {

    // Secuencias de teclado QWERTY (horizontal y diagonal)
    private static final String[] KEYBOARD_SEQUENCES = {
        "qwertyuiop", "asdfghjkl", "zxcvbnm",
        "1234567890", "qwerty", "asdf", "zxcv"
    };

    private final PasswordPolicyRepository  repository;
    private final PasswordHistoryRepository historyRepository;
    private final PasswordEncoder           passwordEncoder;
    private final SecurityLogService securityLogService;

    public PasswordPolicyService(PasswordPolicyRepository repository,
                                 PasswordHistoryRepository historyRepository,
                                 PasswordEncoder passwordEncoder,
                                 SecurityLogService securityLogService) {
        this.repository        = repository;
        this.historyRepository = historyRepository;
        this.passwordEncoder   = passwordEncoder;
        this.securityLogService = securityLogService;
    }

    public PasswordPolicy getPolicy() {
        return repository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Password policy not found"));
    }

    public PasswordPolicyDTO.Response get() {
        return PasswordPolicyDTO.Response.from(getPolicy());
    }

    public PasswordPolicyDTO.Response update(PasswordPolicyDTO.Request request) {
        PasswordPolicy policy = getPolicy();
        String oldValue = buildPolicyAuditValue(policy);
        policy.setEnabled(request.isEnabled());
        policy.setMinLength(request.getMinLength());
        policy.setMaxLength(request.getMaxLength());
        policy.setRequireUppercase(request.isRequireUppercase());
        policy.setRequireLowercase(request.isRequireLowercase());
        policy.setRequireNumbers(request.isRequireNumbers());
        policy.setRequireSpecialChars(request.isRequireSpecialChars());
        policy.setMaxFailedAttempts(request.getMaxFailedAttempts());
        policy.setLockDurationMinutes(request.getLockDurationMinutes());
        policy.setExpirationDays(request.getExpirationDays());
        policy.setNotifyBeforeExpirationDays(request.getNotifyBeforeExpirationDays());
        policy.setMinPasswordAgeDays(request.getMinPasswordAgeDays());
        policy.setPasswordHistoryCount(request.getPasswordHistoryCount());
        policy.setMaxSequenceLength(request.getMaxSequenceLength());
        if (request.getSessionTimeoutSeconds() != null)
            policy.setSessionTimeoutSeconds(request.getSessionTimeoutSeconds());
        if (request.getReservedUsernames() != null)
            policy.setReservedUsernames(request.getReservedUsernames());
        PasswordPolicy saved = repository.save(policy);
        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.PASSWORD_POLICY_UPDATED)
                .eventCode("PASSWORD_POLICY_UPDATE")
                .origin("Politica de contrasenas")
                .target("PASSWORD_POLICY")
                .performedBy(getCurrentActor())
                .description("Actualizacion de parametros de politica de contrasenas")
                .oldValue(oldValue)
                .newValue(buildPolicyAuditValue(saved)));
        return PasswordPolicyDTO.Response.from(saved);
    }

    public void validate(String password) {
        PasswordPolicy policy = getPolicy();
        if (!policy.isEnabled()) return;

        if (password.length() < policy.getMinLength())
            throw new RuntimeException("La contraseña debe tener al menos " + policy.getMinLength() + " caracteres");

        if (password.length() > policy.getMaxLength())
            throw new RuntimeException("La contraseña no puede superar " + policy.getMaxLength() + " caracteres");

        if (policy.isRequireUppercase() && !password.matches(".*[A-Z].*"))
            throw new RuntimeException("La contraseña debe contener al menos una letra mayúscula");

        if (policy.isRequireLowercase() && !password.matches(".*[a-z].*"))
            throw new RuntimeException("La contraseña debe contener al menos una letra minúscula");

        if (policy.isRequireNumbers() && !password.matches(".*[0-9].*"))
            throw new RuntimeException("La contraseña debe contener al menos un número");

        if (policy.isRequireSpecialChars() && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"))
            throw new RuntimeException("La contraseña debe contener al menos un carácter especial");

        if (policy.getMaxSequenceLength() > 0)
            validateSequences(password, policy.getMaxSequenceLength());
    }

    public void validateMinAge(User user) {
        PasswordPolicy policy = getPolicy();
        if (!policy.isEnabled() || policy.getMinPasswordAgeDays() <= 0) return;
        if (user.getPasswordChangedAt() == null) return;

        long daysSinceChange = java.time.temporal.ChronoUnit.DAYS
                .between(user.getPasswordChangedAt(), java.time.LocalDateTime.now());

        if (daysSinceChange < policy.getMinPasswordAgeDays())
            throw new RuntimeException(
                "No puede cambiar la contraseña hasta que hayan pasado "
                + policy.getMinPasswordAgeDays() + " día(s) desde el último cambio. "
                + "Días restantes: " + (policy.getMinPasswordAgeDays() - daysSinceChange));
    }

    public void validateHistory(String newPassword, User user) {
        PasswordPolicy policy = getPolicy();
        if (!policy.isEnabled() || policy.getPasswordHistoryCount() <= 0) return;

        List<PasswordHistory> history = historyRepository
                .findByUserOrderByChangedAtDesc(user)
                .stream()
                .limit(policy.getPasswordHistoryCount())
                .toList();

        for (PasswordHistory h : history) {
            if (passwordEncoder.matches(newPassword, h.getPasswordHash()))
                throw new RuntimeException("La contraseña ya fue utilizada recientemente. " +
                        "Debe usar una de las últimas " + policy.getPasswordHistoryCount() + " contraseñas diferentes");
        }
    }

    public void saveHistory(User user, String encodedPassword) {
        PasswordPolicy policy = getPolicy();
        if (!policy.isEnabled() || policy.getPasswordHistoryCount() <= 0) return;

        historyRepository.save(new PasswordHistory(user, encodedPassword));

        List<PasswordHistory> all = historyRepository.findByUserOrderByChangedAtDesc(user);
        if (all.size() > policy.getPasswordHistoryCount()) {
            List<PasswordHistory> toDelete = all.subList(policy.getPasswordHistoryCount(), all.size());
            historyRepository.deleteAll(toDelete);
        }
    }

    // ── Validación de secuencias ─────────────────────────────────────────────

    private void validateSequences(String password, int maxLen) {
        String lower = password.toLowerCase();

        // Secuencias numéricas ascendentes y descendentes
        if (hasNumericSequence(lower, maxLen))
            throw new RuntimeException("La contraseña no debe contener una secuencia numérica de " + maxLen + " o más caracteres consecutivos (ej: 123, 987)");

        // Secuencias alfabéticas ascendentes y descendentes
        if (hasAlphaSequence(lower, maxLen))
            throw new RuntimeException("La contraseña no debe contener una secuencia alfabética de " + maxLen + " o más caracteres consecutivos (ej: abc, xyz)");

        // Secuencias de teclado QWERTY
        if (hasKeyboardSequence(lower, maxLen))
            throw new RuntimeException("La contraseña no debe contener una secuencia de teclado de " + maxLen + " o más caracteres consecutivos (ej: qwerty, asdf)");
    }

    private boolean hasNumericSequence(String pwd, int maxLen) {
        int count = 1;
        for (int i = 1; i < pwd.length(); i++) {
            char prev = pwd.charAt(i - 1);
            char curr = pwd.charAt(i);
            if (Character.isDigit(prev) && Character.isDigit(curr)) {
                int diff = curr - prev;
                if (diff == 1 || diff == -1) {
                    count++;
                    if (count >= maxLen) return true;
                } else {
                    count = 1;
                }
            } else {
                count = 1;
            }
        }
        return false;
    }

    private boolean hasAlphaSequence(String pwd, int maxLen) {
        int count = 1;
        for (int i = 1; i < pwd.length(); i++) {
            char prev = pwd.charAt(i - 1);
            char curr = pwd.charAt(i);
            if (Character.isLetter(prev) && Character.isLetter(curr)) {
                int diff = curr - prev;
                if (diff == 1 || diff == -1) {
                    count++;
                    if (count >= maxLen) return true;
                } else {
                    count = 1;
                }
            } else {
                count = 1;
            }
        }
        return false;
    }

    private boolean hasKeyboardSequence(String pwd, int maxLen) {
        for (String seq : KEYBOARD_SEQUENCES) {
            String reversed = new StringBuilder(seq).reverse().toString();
            for (String s : new String[]{seq, reversed}) {
                for (int i = 0; i <= s.length() - maxLen; i++) {
                    String sub = s.substring(i, i + maxLen);
                    if (pwd.contains(sub)) return true;
                }
            }
        }
        return false;
    }

    private String getCurrentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private String buildPolicyAuditValue(PasswordPolicy policy) {
        return "enabled=" + policy.isEnabled()
                + ", minLength=" + policy.getMinLength()
                + ", maxLength=" + policy.getMaxLength()
                + ", requireUppercase=" + policy.isRequireUppercase()
                + ", requireLowercase=" + policy.isRequireLowercase()
                + ", requireNumbers=" + policy.isRequireNumbers()
                + ", requireSpecialChars=" + policy.isRequireSpecialChars()
                + ", maxFailedAttempts=" + policy.getMaxFailedAttempts()
                + ", lockDurationMinutes=" + policy.getLockDurationMinutes()
                + ", expirationDays=" + policy.getExpirationDays()
                + ", notifyBeforeExpirationDays=" + policy.getNotifyBeforeExpirationDays()
                + ", minPasswordAgeDays=" + policy.getMinPasswordAgeDays()
                + ", passwordHistoryCount=" + policy.getPasswordHistoryCount()
                + ", maxSequenceLength=" + policy.getMaxSequenceLength()
                + ", sessionTimeoutSeconds=" + policy.getSessionTimeoutSeconds()
                + ", maxInactivityDays=" + policy.getMaxInactivityDays();
    }
}
