package com.geros.backend.user;

import com.geros.backend.mailconfig.MailConfigService;
import com.geros.backend.policy.PasswordGenerator;
import com.geros.backend.policy.PasswordPolicyService;
import com.geros.backend.role.Role;
import com.geros.backend.role.RoleRepository;
import com.geros.backend.securitylog.SecurityLog;
import com.geros.backend.securitylog.SecurityLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService policyService;
    private final RoleRepository roleRepository;
    private final PasswordGenerator passwordGenerator;
    private final MailConfigService mailConfigService;
    private final SecurityLogService securityLogService;
    private final UsernameValidator usernameValidator;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       PasswordPolicyService policyService, RoleRepository roleRepository,
                       PasswordGenerator passwordGenerator, MailConfigService mailConfigService,
                       SecurityLogService securityLogService, UsernameValidator usernameValidator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.policyService = policyService;
        this.roleRepository = roleRepository;
        this.passwordGenerator = passwordGenerator;
        this.mailConfigService = mailConfigService;
        this.securityLogService = securityLogService;
        this.usernameValidator = usernameValidator;
    }

    public Page<UserDTO.Response> findAll(String search, Boolean isActive, Long roleId, String status, Pageable pageable) {
        Page<User> page = userRepository.findAllWithFilters(search, isActive, roleId, status, pageable);
        logUserListAccessIfNeeded(page, search, roleId, status);
        return page.map(UserDTO.Response::from);
    }

    public List<UserDTO.SummaryResponse> findByRoleId(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));

        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream().anyMatch(userRole -> userRole.getId().equals(role.getId())))
                .map(UserDTO.SummaryResponse::from)
                .toList();
    }

    public UserDTO.Response findById(Long id) {
        User user = getOrThrow(id);
        logUserDetailAccessIfNeeded(user);
        return UserDTO.Response.from(user);
    }

    public UserDTO.Response create(UserDTO.Request request) {
        usernameValidator.validate(request.getUsername());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use: " + request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("El usuario '" + request.getUsername() + "' ya esta en uso. Cada funcionario debe tener un usuario unico y personalizado.");
        }

        String rawPassword = passwordGenerator.generate();
        String encoded = passwordEncoder.encode(rawPassword);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(encoded);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(true);
        user.setRoles(resolveRoles(request.getRoleIds()));

        User saved = userRepository.save(user);
        policyService.saveHistory(saved, encoded);
        mailConfigService.sendWelcomeEmail(saved.getEmail(), saved.getFirstName(), saved.getLastName(), rawPassword);

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.USER_CREATED)
                .eventCode("USER_CREATE")
                .origin("Administracion de usuarios")
                .target(saved.getEmail())
                .performedBy(getCurrentActor())
                .description("Usuario '" + user.getUsername() + "' creado por administrador. Contrasena temporal asignada y enviada por correo")
                .newValue(buildUserAuditValue(saved)));

        return UserDTO.Response.from(saved);
    }

    public UserDTO.Response update(Long id, UserDTO.UpdateRequest request) {
        User user = getOrThrow(id);
        String oldValue = buildUserAuditValue(user);

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        if (request.getRoleIds() != null) {
            user.setRoles(resolveRoles(request.getRoleIds()));
        }
        User saved = userRepository.save(user);
        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.USER_UPDATED)
                .eventCode("USER_UPDATE")
                .origin("Administracion de usuarios")
                .target(saved.getEmail())
                .performedBy(getCurrentActor())
                .description("Actualizacion de cuenta de usuario")
                .oldValue(oldValue)
                .newValue(buildUserAuditValue(saved)));
        return UserDTO.Response.from(saved);
    }

    public UserDTO.Response lock(Long id) {
        User user = getOrThrow(id);
        user.setLockedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.ACCOUNT_LOCKED)
                .eventCode("USER_LOCK")
                .origin("Administracion de usuarios")
                .target(user.getEmail())
                .performedBy(getCurrentActor())
                .description("Cuenta bloqueada manualmente por administrador")
                .oldValue("estado=ACTIVO")
                .newValue("estado=BLOQUEADO"));
        return UserDTO.Response.from(saved);
    }

    public UserDTO.Response unlock(Long id) {
        User user = getOrThrow(id);
        user.setLockedAt(null);
        user.setFailedAttempts(0);
        User saved = userRepository.save(user);
        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.ACCOUNT_UNLOCKED)
                .eventCode("USER_UNLOCK")
                .origin("Administracion de usuarios")
                .target(user.getEmail())
                .performedBy(getCurrentActor())
                .description("Cuenta desbloqueada por administrador")
                .oldValue("estado=BLOQUEADO")
                .newValue("estado=ACTIVO"));
        return UserDTO.Response.from(saved);
    }

    public UserDTO.Response adminChangePassword(Long id, UserDTO.AdminChangePasswordRequest request) {
        User user = getOrThrow(id);
        policyService.validate(request.getNewPassword());
        policyService.validateHistory(request.getNewPassword(), user);
        String encoded = passwordEncoder.encode(request.getNewPassword());
        policyService.saveHistory(user, user.getPassword());
        user.setPassword(encoded);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(request.isMustChangePassword());
        user.setFailedAttempts(0);
        user.setLockedAt(null);
        User saved = userRepository.save(user);

        String detail = request.isMustChangePassword()
                ? "Contrasena cambiada por administrador. Se requiere cambio en proximo login"
                : "Contrasena cambiada por administrador";
        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.PASSWORD_CHANGED_BY_ADMIN)
                .eventCode("ADMIN_PASSWORD_CHANGE")
                .origin("Administracion de usuarios")
                .target(user.getEmail())
                .performedBy(getCurrentActor())
                .description(detail)
                .oldValue("mustChangePassword=" + !request.isMustChangePassword())
                .newValue("mustChangePassword=" + request.isMustChangePassword()));

        return UserDTO.Response.from(saved);
    }

    public void delete(Long id) {
        User user = getOrThrow(id);
        
        // Validar que no se elimine el último usuario con rol ADMIN
        if (user.hasRole("ADMIN")) {
            long adminCount = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ADMIN") && Boolean.TRUE.equals(u.getIsActive()))
                .count();
            
            if (adminCount <= 1) {
                throw new SecurityException("No se puede eliminar el último usuario administrador activo del sistema");
            }
        }
        
        String oldValue = buildUserAuditValue(user);
        String userType = user.isDefaultUser() ? "Usuario privilegiado por defecto" : "Usuario";
        
        userRepository.deleteById(id);
        
        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.USER_DELETED)
                .eventCode("USER_DELETE")
                .origin("Administracion de usuarios")
                .target(user.getEmail())
                .performedBy(getCurrentActor())
                .description(userType + " eliminado del sistema")
                .oldValue(oldValue));
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Role> roles = new HashSet<>();
        for (Long roleId : roleIds) {
            roleRepository.findById(roleId).ifPresent(roles::add);
        }
        return roles;
    }

    public User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    private String getCurrentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private User getCurrentActorUser() {
        String actorEmail = getCurrentActor();
        if ("SYSTEM".equals(actorEmail)) {
            return null;
        }
        return userRepository.findByEmail(actorEmail).orElse(null);
    }

    private boolean isInternalEmployee(User actor) {
        return actor != null && !actor.hasRole("CUSTOMER");
    }

    private void logUserDetailAccessIfNeeded(User targetUser) {
        User actor = getCurrentActorUser();
        if (!isInternalEmployee(actor)) {
            return;
        }

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.USER_DATA_ACCESSED)
                .eventCode("USER_CONFIDENTIAL_DETAIL_QUERY")
                .origin("Consulta confidencial de usuarios")
                .target(targetUser.getEmail())
                .performedBy(actor.getEmail())
                .description("Consulta de informacion confidencial del usuario")
                .newValue("usuario=" + targetUser.getUsername() + ", email=" + targetUser.getEmail() + ", roles=" + buildRoleSummary(targetUser)));
    }

    private void logUserListAccessIfNeeded(Page<User> page, String search, Long roleId, String status) {
        User actor = getCurrentActorUser();
        if (!isInternalEmployee(actor)) {
            return;
        }

        java.util.List<User> consultedUsers = page.getContent();
        if (consultedUsers.isEmpty()) {
            return;
        }

        String consultedUsersSummary = consultedUsers.stream()
                .map(user -> user.getEmail() + "[" + buildRoleSummary(user) + "]")
                .limit(10)
                .reduce((left, right) -> left + "|" + right)
                .orElse("SIN_USUARIOS");

        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.USER_LIST_ACCESSED)
                .eventCode("USER_CONFIDENTIAL_LIST_QUERY")
                .origin("Consulta confidencial de usuarios")
                .target("USER_LIST")
                .performedBy(actor.getEmail())
                .description("Consulta de listado con informacion confidencial de usuarios")
                .oldValue("search=" + normalizeAuditValue(search) + ", roleId=" + normalizeAuditValue(roleId) + ", status=" + normalizeAuditValue(status))
                .newValue("usuarios=" + consultedUsersSummary + ", cantidad=" + consultedUsers.size()));
    }

    private String normalizeAuditValue(Object value) {
        if (value == null) {
            return "N/A";
        }
        String text = value.toString().trim();
        return text.isEmpty() ? "N/A" : text;
    }

    private String buildUserAuditValue(User user) {
        String roles = buildRoleSummary(user);
        String status = user.isLocked()
                ? "BLOQUEADO"
                : (Boolean.TRUE.equals(user.getIsActive()) ? "ACTIVO" : "INACTIVO");
        return "usuario=" + user.getUsername()
                + ", email=" + user.getEmail()
                + ", estado=" + status
                + ", roles=" + roles;
    }

    private String buildRoleSummary(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .reduce((left, right) -> left + "|" + right)
                .orElse("SIN_ROL");
    }
}
