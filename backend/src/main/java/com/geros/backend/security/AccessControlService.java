package com.geros.backend.security;

import com.geros.backend.menu.MenuRolePermissionRepository;
import com.geros.backend.role.RoleRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component("accessControlService")
public class AccessControlService {
    private static final Set<String> PRIVILEGED_PATHS = Set.of("/security-log");

    private final MenuRolePermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public AccessControlService(MenuRolePermissionRepository permissionRepository, RoleRepository roleRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    public boolean hasMenuAccess(Authentication authentication, String menuPath) {
        return hasPrivilege(authentication, menuPath, "READ");
    }

    public boolean hasPrivilege(Authentication authentication, String menuPath, String privilege) {
        if (authentication == null || !authentication.isAuthenticated() || menuPath == null || menuPath.isBlank()) {
            return false;
        }

        Set<String> roleNames = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .collect(Collectors.toSet());

        if (roleNames.contains("ADMIN")) {
            return true;
        }

        if (roleNames.isEmpty()) {
            return false;
        }

        if (PRIVILEGED_PATHS.contains(menuPath) && !roleRepository.existsByNameInAndPrivilegedTrue(roleNames)) {
            return false;
        }

        return permissionRepository.findActivePermissionsForPath(menuPath, roleNames).stream()
                .anyMatch(permission -> permission.grants(privilege));
    }
}
