package com.geros.backend.role;

import com.geros.backend.menu.MenuItem;
import com.geros.backend.menu.MenuItemRepository;
import com.geros.backend.menu.MenuRolePermission;
import com.geros.backend.menu.MenuRolePermissionRepository;
import com.geros.backend.securitylog.SecurityLog;
import com.geros.backend.securitylog.SecurityLogService;
import com.geros.backend.user.UserDTO;
import com.geros.backend.user.UserRepository;
import com.geros.backend.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {
    private static final String SECURITY_LOG_PATH = "/security-log";
    private static final Set<String> AUDIT_PATHS = Set.of("/security-log", "/password-history");


    private final RoleRepository repository;
    private final MenuRolePermissionRepository permissionRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SecurityLogService securityLogService;

    public RoleService(RoleRepository repository,
                       MenuRolePermissionRepository permissionRepository,
                       MenuItemRepository menuItemRepository,
                       UserRepository userRepository,
                       UserService userService,
                       SecurityLogService securityLogService) {
        this.repository = repository;
        this.permissionRepository = permissionRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.securityLogService = securityLogService;
    }

    public List<RoleDTO.Response> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public RoleDTO.Response findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public RoleDTO.Response create(RoleDTO.Request request) {
        String normalizedName = request.getName().trim().toUpperCase();
        if (repository.existsByName(normalizedName))
            throw new RuntimeException("Role already exists: " + request.getName());

        Role role = new Role(normalizedName, request.getDescription());
        RoleProfileType profileType = resolveProfileType(request.getProfileType());
        role.setProfileType(profileType);
        role.setPrivileged("ADMIN".equals(normalizedName) || profileType == RoleProfileType.AUDIT || request.isPrivileged());
        Role saved = repository.save(role);
        syncPermissions(saved, request.getPermissions());
        String newValue = buildRoleAuditDetail(saved);
        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.ROLE_CREATED)
                .eventCode("ROLE_CREATE")
                .origin("Administracion de perfiles")
                .target(saved.getName())
                .performedBy(getCurrentActor())
                .description("Creacion de perfil/rol")
                .newValue(newValue));
        return toResponse(saved);
    }

    @Transactional
    public RoleDTO.Response update(Long id, RoleDTO.Request request) {
        Role role = getOrThrow(id);
        String oldValue = buildRoleAuditDetail(role);
        String normalizedName = request.getName().trim().toUpperCase();
        if (!role.getName().equals(normalizedName) && repository.existsByName(normalizedName))
            throw new RuntimeException("Role already exists: " + normalizedName);
        role.setName(normalizedName);
        role.setDescription(request.getDescription());
        RoleProfileType profileType = resolveProfileType(request.getProfileType());
        role.setProfileType(profileType);
        role.setPrivileged("ADMIN".equals(role.getName()) || "ADMIN".equals(normalizedName) || profileType == RoleProfileType.AUDIT || request.isPrivileged());
        Role saved = repository.save(role);
        syncPermissions(saved, request.getPermissions());
        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.ROLE_UPDATED)
                .eventCode("ROLE_UPDATE")
                .origin("Administracion de perfiles")
                .target(saved.getName())
                .performedBy(getCurrentActor())
                .description("Actualizacion de perfil/rol")
                .oldValue(oldValue)
                .newValue(buildRoleAuditDetail(saved)));
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id))
            throw new RuntimeException("Role not found with id: " + id);
        Role role = getOrThrow(id);
        if ("ADMIN".equals(role.getName()))
            throw new RuntimeException("The ADMIN role cannot be deleted");
        if (userRepository.existsByRoles_Id(id))
            throw new RuntimeException("The role is assigned to one or more users and cannot be deleted");
        String roleName = role.getName();
        String oldValue = buildRoleAuditDetail(role);
        permissionRepository.deleteByRoleId(id);
        repository.deleteById(id);
        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.ROLE_DELETED)
                .eventCode("ROLE_DELETE")
                .origin("Administracion de perfiles")
                .target(roleName)
                .performedBy(getCurrentActor())
                .description("Eliminacion de perfil/rol")
                .oldValue(oldValue));
    }

    public Role getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
    }

    public Role getByName(String name) {
        return repository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Role not found: " + name));
    }

    public List<RoleDTO.PrivilegeCatalogItem> getPrivilegeCatalog() {
        return menuItemRepository.findAll(org.springframework.data.domain.Sort.by("sortOrder", "id")).stream()
                .map(RoleDTO.PrivilegeCatalogItem::from)
                .toList();
    }

    public List<RoleDTO.PermissionResponse> findPermissionsByRole(Long id) {
        Role role = getOrThrow(id);
        return permissionRepository.findByRoleId(role.getId()).stream()
                .map(RoleDTO.PermissionResponse::from)
                .toList();
    }

    public List<UserDTO.SummaryResponse> findUsersByRole(Long id) {
        getOrThrow(id);
        return userService.findByRoleId(id);
    }

    private RoleDTO.Response toResponse(Role role) {
        List<RoleDTO.PermissionResponse> permissions = permissionRepository.findByRoleId(role.getId()).stream()
                .map(RoleDTO.PermissionResponse::from)
                .toList();
        return RoleDTO.Response.from(role, permissions);
    }

    private String getCurrentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private String buildRoleAuditDetail(Role role) {
        long permissionCount = permissionRepository.findByRoleId(role.getId()).stream()
                .filter(permission -> permission.isCanView() || permission.isCanCreate() || permission.isCanUpdate() || permission.isCanDelete())
                .count();
        return "Perfil=" + role.getName()
                + ", tipo=" + role.getProfileType().name()
                + ", privilegiado=" + role.isPrivileged()
                + ", permisos=" + permissionCount;
    }

    private RoleProfileType resolveProfileType(String profileType) {
        if (profileType == null || profileType.isBlank()) {
            return RoleProfileType.STANDARD;
        }
        try {
            return RoleProfileType.valueOf(profileType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid profile type: " + profileType);
        }
    }

    private void syncPermissions(Role role, List<RoleDTO.PermissionRequest> permissionRequests) {
        permissionRepository.deleteByRoleId(role.getId());
        if (permissionRequests == null || permissionRequests.isEmpty()) {
            return;
        }

        Map<Long, MenuItem> menuItems = menuItemRepository.findAllById(
                permissionRequests.stream()
                        .map(RoleDTO.PermissionRequest::getMenuItemId)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(MenuItem::getId, item -> item, (a, b) -> a, HashMap::new));

        Set<Long> duplicatedIds = permissionRequests.stream()
                .map(RoleDTO.PermissionRequest::getMenuItemId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!duplicatedIds.isEmpty()) {
            throw new RuntimeException("Duplicated privilege definitions detected");
        }

        List<MenuRolePermission> permissions = permissionRequests.stream()
                .filter(request -> request.getMenuItemId() != null)
                .filter(request -> request.isCanView() || request.isCanCreate() || request.isCanUpdate() || request.isCanDelete())
                .map(request -> {
                    MenuItem item = menuItems.get(request.getMenuItemId());
                    if (item == null) {
                        throw new RuntimeException("Menu item not found: " + request.getMenuItemId());
                    }
                    if (role.getProfileType() == RoleProfileType.AUDIT && item.getPath() != null && !AUDIT_PATHS.contains(item.getPath())) {
                        throw new RuntimeException("Audit profiles can only access audit and log modules");
                    }
                    if (role.getProfileType() != RoleProfileType.STANDARD &&
                            (request.isCanCreate() || request.isCanUpdate() || request.isCanDelete())) {
                        throw new RuntimeException("Read-only and audit profiles cannot grant create, update or delete privileges");
                    }
                    if (SECURITY_LOG_PATH.equals(item.getPath()) && !role.isPrivileged() && !"ADMIN".equals(role.getName())) {
                        throw new RuntimeException("Only privileged profiles can access security logs");
                    }
                    boolean canView = request.isCanView() || request.isCanCreate() || request.isCanUpdate() || request.isCanDelete();
                    return new MenuRolePermission(
                            item,
                            role,
                            canView,
                            role.getProfileType() == RoleProfileType.STANDARD && request.isCanCreate(),
                            role.getProfileType() == RoleProfileType.STANDARD && request.isCanUpdate(),
                            role.getProfileType() == RoleProfileType.STANDARD && request.isCanDelete()
                    );
                })
                .toList();
        permissionRepository.saveAll(permissions);
    }
}
