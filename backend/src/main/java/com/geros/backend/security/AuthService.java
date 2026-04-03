package com.geros.backend.security;

import com.geros.backend.mailconfig.MailConfigService;
import com.geros.backend.policy.PasswordPolicy;
import com.geros.backend.policy.PasswordPolicyService;
import com.geros.backend.securitylog.SecurityLog;
import com.geros.backend.securitylog.SecurityLogService;
import com.geros.backend.user.User;
import com.geros.backend.user.UserDTO;
import com.geros.backend.user.UserRepository;
import com.geros.backend.user.UsernameValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordPolicyService policyService;
    private final SecurityLogService securityLogService;
    private final RecaptchaService recaptchaService;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final MailConfigService mailConfigService;
    private final UsernameValidator usernameValidator;
    @org.springframework.beans.factory.annotation.Value("${app.security-alert.allowed-login-start:06:00}")
    private String allowedLoginStart;
    @org.springframework.beans.factory.annotation.Value("${app.security-alert.allowed-login-end:22:00}")
    private String allowedLoginEnd;

    @org.springframework.beans.factory.annotation.Value("${app.auth.max-concurrent-sessions:1}")
    private int maxConcurrentSessions;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, PasswordPolicyService policyService,
                       SecurityLogService securityLogService, RecaptchaService recaptchaService,
                       PasswordResetTokenRepository resetTokenRepository,
                       MailConfigService mailConfigService, UsernameValidator usernameValidator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.policyService = policyService;
        this.securityLogService = securityLogService;
        this.recaptchaService = recaptchaService;
        this.resetTokenRepository = resetTokenRepository;
        this.mailConfigService = mailConfigService;
        this.usernameValidator = usernameValidator;
    }

    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest request) {
        if (request.getRecaptchaToken() != null && !request.getRecaptchaToken().isBlank()) {
            recaptchaService.verify(request.getRecaptchaToken(), "login");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseGet(() -> userRepository.findByEmail(request.getUsername())
                        .orElseThrow(() -> new RuntimeException("Invalid credentials")));

        if (user.isLocked()) {
            PasswordPolicy policy = policyService.getPolicy();
            if (policy.getLockDurationMinutes() > 0) {
                long minutesLocked = ChronoUnit.MINUTES.between(user.getLockedAt(), LocalDateTime.now());
                if (minutesLocked >= policy.getLockDurationMinutes()) {
                    user.setLockedAt(null);
                    user.setFailedAttempts(0);
                    userRepository.save(user);
                    securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.ACCOUNT_UNLOCKED)
                            .eventCode("AUTO_UNLOCK")
                            .origin("Autenticacion")
                            .target(user.getEmail())
                            .performedBy("SYSTEM")
                            .description("Desbloqueo automatico por expiracion del tiempo de bloqueo")
                            .oldValue("estado=BLOQUEADO")
                            .newValue("estado=ACTIVO"));
                } else {
                    long remaining = policy.getLockDurationMinutes() - minutesLocked;
                    throw new RuntimeException("Cuenta bloqueada temporalmente. Intente en " + remaining + " minuto(s) o contacte al administrador");
                }
            } else {
                throw new RuntimeException("Cuenta bloqueada. Contacte al administrador");
            }
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("User is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedAttempt(user);
            throw new RuntimeException("Invalid credentials");
        }

        if (maxConcurrentSessions > 0 && user.getActiveSessions() >= maxConcurrentSessions) {
            throw new RuntimeException("Número máximo de sesiones concurrentes alcanzado. Cierre sesión de otra sesión antes de volver a iniciar sesión.");
        }

        LocalDateTime now = LocalDateTime.now();
        String ip = resolveClientIp();

        user.setPreviousLoginAt(user.getCurrentLoginAt() != null ? user.getCurrentLoginAt() : user.getLastLoginAt());
        user.setPreviousLoginIp(user.getCurrentLoginIp());

        user.setCurrentLoginAt(now);
        user.setCurrentLoginIp(ip);
        user.setLastLoginAt(now);

        user.setFailedAttempts(0);
        user.setActiveSessions(user.getActiveSessions() + 1);
        userRepository.save(user);

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.LOGIN_SUCCESS)
                .eventCode("LOGIN_SUCCESS")
                .origin("Autenticacion")
                .target(user.getEmail())
                .performedBy(user.getEmail())
                .description("Inicio de sesion exitoso"));
        logOutsideAllowedHoursIfNeeded(user);

        PasswordPolicy policy = policyService.getPolicy();
        Integer expiresInDays = null;
        boolean mustChange = user.isMustChangePassword();

        if (policy.isEnabled() && policy.getExpirationDays() > 0 && user.getPasswordChangedAt() != null) {
            long daysSinceChange = ChronoUnit.DAYS.between(user.getPasswordChangedAt(), LocalDateTime.now());
            long remaining = policy.getExpirationDays() - daysSinceChange;
            if (remaining <= 0) {
                mustChange = true;
            } else if (remaining <= policy.getNotifyBeforeExpirationDays()) {
                expiresInDays = (int) remaining;
            }
        }

        String token = jwtUtil.generateToken(user.getEmail());
        String roles = user.getRoles().stream().map(r -> r.getName())
                .collect(java.util.stream.Collectors.joining(","));
        return new AuthDTO.LoginResponse(
                token,
                user.getEmail(),
                roles,
                mustChange,
                expiresInDays,
                user.getCurrentLoginIp(),
                user.getPreviousLoginIp(),
                user.getCurrentLoginAt() != null ? user.getCurrentLoginAt().toString() : null, // currentLoginAt
                user.getPreviousLoginAt() != null ? user.getPreviousLoginAt().toString() : null, // previousLoginAt
                (policy != null && policy.getSessionTimeoutSeconds() != null) ? policy.getSessionTimeoutSeconds() : 1800
        );
    }

    public AuthDTO.LoginResponse register(AuthDTO.RegisterRequest request) {
        usernameValidator.validate(request.getUsername());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use: " + request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("El usuario '" + request.getUsername() + "' ya esta en uso. Cada funcionario debe tener un usuario unico.");
        }

        policyService.validate(request.getPassword());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();
        user.setCurrentLoginAt(now);
        user.setCurrentLoginIp(resolveClientIp());
        user.setLastLoginAt(now);

        userRepository.save(user);

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.USER_CREATED)
                .eventCode("SELF_REGISTER")
                .origin("Autoregistro")
                .target(user.getEmail())
                .performedBy(user.getEmail())
                .description("Usuario '" + user.getUsername() + "' registrado mediante auto-registro")
                .newValue("usuario=" + user.getUsername() + ", email=" + user.getEmail()));

        String token = jwtUtil.generateToken(user.getEmail());
        String roles = user.getRoles().stream().map(r -> r.getName())
                .collect(java.util.stream.Collectors.joining(","));
        return new AuthDTO.LoginResponse(
                token,
                user.getEmail(),
                roles,
                false,
                null,
                user.getCurrentLoginIp(),
                user.getPreviousLoginIp(),
                user.getCurrentLoginAt() != null ? user.getCurrentLoginAt().toString() : null, // currentLoginAt
                user.getPreviousLoginAt() != null ? user.getPreviousLoginAt().toString() : null, // previousLoginAt
                (policyService.getPolicy() != null && policyService.getPolicy().getSessionTimeoutSeconds() != null) ? policyService.getPolicy().getSessionTimeoutSeconds() : 1800
        );
    }

    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        int sessions = user.getActiveSessions();
        if (sessions > 0) {
            user.setActiveSessions(sessions - 1);
            userRepository.save(user);
            securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.LOGOUT)
                    .eventCode("LOGOUT")
                    .origin("Autenticacion")
                    .target(user.getEmail())
                    .performedBy(user.getEmail())
                    .description("Cierre de sesión exitoso"));
        }
    }

    public void changePassword(String email, UserDTO.ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("La contrasena actual es incorrecta");
        }

        policyService.validate(request.getNewPassword());
        policyService.validateHistory(request.getNewPassword(), user);

        boolean forced = user.isMustChangePassword();
        String encoded = passwordEncoder.encode(request.getNewPassword());
        policyService.saveHistory(user, user.getPassword());
        user.setPassword(encoded);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false);
        user.setFailedAttempts(0);
        userRepository.save(user);

        SecurityLog.Action action = forced
                ? SecurityLog.Action.PASSWORD_FORCED_CHANGE
                : SecurityLog.Action.PASSWORD_CHANGED_BY_USER;
        securityLogService.log(new SecurityLogService.AuditEntry(action)
                .eventCode(forced ? "FORCED_PASSWORD_CHANGE" : "USER_PASSWORD_CHANGE")
                .origin("Autogestion de contrasena")
                .target(email)
                .performedBy(email)
                .description(forced ? "Cambio de contrasena forzado completado" : "Cambio de contrasena por el usuario")
                .oldValue("mustChangePassword=" + forced)
                .newValue("mustChangePassword=false"));
    }

    @Transactional
    public void forgotPassword(AuthDTO.ForgotPasswordRequest request) {
        if (request.getRecaptchaToken() != null && !request.getRecaptchaToken().isBlank()) {
            recaptchaService.verify(request.getRecaptchaToken(), "forgot_password");
        }

        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            resetTokenRepository.deleteByEmail(request.getEmail());

            String token = UUID.randomUUID().toString();
            resetTokenRepository.save(new PasswordResetToken(
                    token, request.getEmail(), LocalDateTime.now().plusHours(1)
            ));

            mailConfigService.sendPasswordResetEmail(
                    user.getEmail(), user.getFirstName(), user.getLastName(), token
            );

            securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.PASSWORD_CHANGED_BY_USER)
                    .eventCode("PASSWORD_RESET_REQUEST")
                    .origin("Recuperacion de contrasena")
                    .target(user.getEmail())
                    .performedBy(user.getEmail())
                    .description("Solicitud de recuperacion de contrasena enviada"));
        });
    }

    @Transactional
    public void resetPassword(AuthDTO.ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token invalido o expirado"));

        if (resetToken.isUsed()) {
            throw new RuntimeException("Este enlace ya fue utilizado");
        }

        if (resetToken.isExpired()) {
            throw new RuntimeException("El enlace ha expirado. Solicite uno nuevo");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        policyService.validate(request.getNewPassword());
        policyService.validateHistory(request.getNewPassword(), user);

        String encoded = passwordEncoder.encode(request.getNewPassword());
        policyService.saveHistory(user, user.getPassword());
        user.setPassword(encoded);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false);
        user.setFailedAttempts(0);
        user.setLockedAt(null);
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.PASSWORD_CHANGED_BY_USER)
                .eventCode("PASSWORD_RESET")
                .origin("Recuperacion de contrasena")
                .target(user.getEmail())
                .performedBy(user.getEmail())
                .description("Contrasena restablecida mediante enlace de recuperacion")
                .newValue("mustChangePassword=false"));
    }

    private void handleFailedAttempt(User user) {
        PasswordPolicy policy = policyService.getPolicy();
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);

        boolean locked = policy.isEnabled() && attempts >= policy.getMaxFailedAttempts();
        if (locked) {
            user.setLockedAt(LocalDateTime.now());
        }
        userRepository.save(user);

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.LOGIN_FAILED)
                .eventType(SecurityLog.EventType.ERROR)
                .eventCode("LOGIN_FAILED")
                .origin("Autenticacion")
                .target(user.getEmail())
                .performedBy(user.getEmail())
                .description("Intento fallido " + attempts + " de " + policy.getMaxFailedAttempts())
                .newValue("failedAttempts=" + attempts));

        if (locked) {
            securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.ACCOUNT_LOCKED)
                    .eventType(SecurityLog.EventType.ERROR)
                    .eventCode("ACCOUNT_LOCKED")
                    .origin("Autenticacion")
                    .target(user.getEmail())
                    .performedBy("SYSTEM")
                    .description("Cuenta bloqueada tras " + attempts + " intentos fallidos")
                    .oldValue("estado=ACTIVO")
                    .newValue("estado=BLOQUEADO"));
        }
    }

    private void logOutsideAllowedHoursIfNeeded(User user) {
        LocalTime currentTime = LocalTime.now();
        LocalTime start = parseAllowedHour(allowedLoginStart, LocalTime.of(6, 0));
        LocalTime end = parseAllowedHour(allowedLoginEnd, LocalTime.of(22, 0));
        boolean outsideAllowedWindow = isOutsideAllowedWindow(currentTime, start, end);

        if (!outsideAllowedWindow) {
            return;
        }

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.LOGIN_OUTSIDE_ALLOWED_HOURS)
                .eventType(SecurityLog.EventType.ERROR)
                .eventCode("LOGIN_OUTSIDE_ALLOWED_HOURS")
                .origin("Autenticacion")
                .target(user.getEmail())
                .performedBy(user.getEmail())
                .description("Ingreso exitoso en horario no permitido")
                .oldValue("horarioPermitido=" + start + "-" + end)
                .newValue("horaAcceso=" + currentTime));
    }

    private LocalTime parseAllowedHour(String value, LocalTime fallback) {
        try {
            return LocalTime.parse(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String resolveClientIp() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletRequestAttributes)) {
            return "unknown";
        }

        String forwardedFor = servletRequestAttributes.getRequest().getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = servletRequestAttributes.getRequest().getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return servletRequestAttributes.getRequest().getRemoteAddr();
    }

    private boolean isOutsideAllowedWindow(LocalTime currentTime, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return false;
        }
        if (start.isBefore(end)) {
            return currentTime.isBefore(start) || currentTime.isAfter(end);
        }
        return currentTime.isAfter(end) && currentTime.isBefore(start);
    }
}
