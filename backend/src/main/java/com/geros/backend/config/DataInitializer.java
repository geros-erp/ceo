package com.geros.backend.config;

import com.geros.backend.menu.MenuItem;
import com.geros.backend.menu.MenuItemRepository;
import com.geros.backend.menu.MenuRolePermission;
import com.geros.backend.menu.MenuRolePermissionRepository;
import com.geros.backend.policy.PasswordPolicy;
import com.geros.backend.policy.PasswordPolicyRepository;
import com.geros.backend.role.Role;
import com.geros.backend.role.RoleProfileType;
import com.geros.backend.role.RoleRepository;
import com.geros.backend.user.User;
import com.geros.backend.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyRepository policyRepository;
    private final RoleRepository roleRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuRolePermissionRepository menuPermissionRepository;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           PasswordPolicyRepository policyRepository, RoleRepository roleRepository,
                           MenuItemRepository menuItemRepository,
                           MenuRolePermissionRepository menuPermissionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.policyRepository = policyRepository;
        this.roleRepository = roleRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuPermissionRepository = menuPermissionRepository;
    }

    @Override
    public void run(String... args) {
        initPolicy();
        initRoles();
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        initAdmin(adminRole);
        initMenu(adminRole);
        resetActiveSessions();
    }

    private void resetActiveSessions() {
        // Resetear sesiones activas al iniciar la aplicación
        userRepository.findAll().forEach(user -> {
            if (user.getActiveSessions() > 0) {
                user.setActiveSessions(0);
                userRepository.save(user);
            }
        });
        System.out.println(">>> Sesiones activas reseteadas");
    }

    private void initPolicy() {
        if (policyRepository.count() == 0) {
            policyRepository.save(new PasswordPolicy());
            System.out.println(">>> Politica de contrasenas creada con valores por defecto");
        }
    }

    private void initRoles() {
        if (!roleRepository.existsByName("ADMIN")) {
            Role admin = new Role("ADMIN", "Administrador del sistema");
            admin.setPrivileged(true);
            admin.setProfileType(RoleProfileType.STANDARD);
            roleRepository.save(admin);
        }
        if (!roleRepository.existsByName("USER")) {
            Role user = new Role("USER", "Usuario interno");
            user.setProfileType(RoleProfileType.STANDARD);
            roleRepository.save(user);
        }
        if (!roleRepository.existsByName("CUSTOMER")) {
            Role customer = new Role("CUSTOMER", "Cliente externo (Directorio Activo)");
            customer.setProfileType(RoleProfileType.STANDARD);
            roleRepository.save(customer);
        }
    }

    private void initAdmin(Role adminRole) {
        if (userRepository.existsByEmail("admin@geros.com")) {
            userRepository.findByEmail("admin@geros.com").ifPresent(user -> {
                user.setPassword(passwordEncoder.encode("admin123"));
                user.setRoles(Set.of(adminRole));
                user.setDefaultUser(true); // Marcar como usuario por defecto
                userRepository.save(user);
            });
            return;
        }

        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("Geros");
        admin.setEmail("admin@geros.com");
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setPasswordChangedAt(LocalDateTime.now());
        admin.setRoles(Set.of(adminRole));
        admin.setDefaultUser(true); // Marcar como usuario por defecto
        userRepository.save(admin);
        System.out.println(">>> Usuario admin creado: admin@geros.com / admin123");
        System.out.println(">>> ADVERTENCIA: Usuario privilegiado por defecto detectado. Se recomienda deshabilitar o eliminar despues de crear usuarios personalizados.");
    }

    private void initMenu(Role adminRole) {
        if (menuItemRepository.count() > 0) return;

        MenuItem inicio = saveItem("Inicio", "/dashboard", "🏠", 0, null);
        grantAdmin(inicio, adminRole);

        MenuItem usuarios = saveItem("Usuarios", "/users", "👥", 1, null);
        grantAdmin(usuarios, adminRole);

        MenuItem seguridad = saveItem("Seguridad", null, "🔒", 2, null);
        grantAdmin(seguridad, adminRole);

        MenuItem roles = saveItem("Roles", "/roles", "🏷️", 1, seguridad);
        MenuItem politica = saveItem("Politica de Contrasenas", "/policy", "🔑", 2, seguridad);
        MenuItem historial = saveItem("Historial de Contrasenas", "/password-history", "📋", 3, seguridad);
        MenuItem reservados = saveItem("Control de Identidades", "/reserved-usernames", "🚫", 4, seguridad);
        grantAdmin(roles, adminRole);
        grantAdmin(politica, adminRole);
        grantAdmin(historial, adminRole);
        grantAdmin(reservados, adminRole);

        MenuItem config = saveItem("Configuracion", null, "⚙️", 3, null);
        grantAdmin(config, adminRole);

        MenuItem correo = saveItem("Correo SMTP", "/mail-config", "✉️", 1, config);
        MenuItem ad = saveItem("Directorio Activo", "/ad-config", "🖥️", 2, config);
        MenuItem menu = saveItem("Menu", "/menu-config", "📋", 3, config);
        MenuItem secLog = saveItem("Log de Seguridad", "/security-log", "🛡️", 4, config);
        grantAdmin(correo, adminRole);
        grantAdmin(ad, adminRole);
        grantAdmin(menu, adminRole);
        grantAdmin(secLog, adminRole);

        System.out.println(">>> Menu inicial creado");
    }

    private MenuItem saveItem(String label, String path, String icon, int order, MenuItem parent) {
        MenuItem item = new MenuItem();
        item.setLabel(label);
        item.setPath(path);
        item.setIcon(icon);
        item.setSortOrder(order);
        item.setActive(true);
        item.setParent(parent);
        return menuItemRepository.save(item);
    }

    private void grantAdmin(MenuItem item, Role adminRole) {
        if (!menuPermissionRepository.existsByMenuItemIdAndRoleId(item.getId(), adminRole.getId())) {
            menuPermissionRepository.save(new MenuRolePermission(item, adminRole, true, true, true, true));
        }
    }
}
