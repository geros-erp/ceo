package com.geros.backend.menu;

import com.geros.backend.role.Role;
import com.geros.backend.role.RoleRepository;
import com.geros.backend.securitylog.SecurityLog;
import com.geros.backend.securitylog.SecurityLogService;
import com.geros.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuService {
    private static final String SECURITY_LOG_PATH = "/security-log";


    private final MenuItemRepository           itemRepository;
    private final MenuRolePermissionRepository permissionRepository;
    private final RoleRepository               roleRepository;
    private final UserRepository               userRepository;
    private final SecurityLogService           securityLogService;

    public MenuService(MenuItemRepository itemRepository,
                       MenuRolePermissionRepository permissionRepository,
                       RoleRepository roleRepository,
                       UserRepository userRepository,
                       SecurityLogService securityLogService) {
        this.itemRepository       = itemRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository       = roleRepository;
        this.userRepository       = userRepository;
        this.securityLogService   = securityLogService;
    }

    // ── CRUD ítems ──────────────────────────────────────────────────────────

    public List<MenuDTO.Response> findAll() {
        List<MenuItem> roots = itemRepository.findRootItems();
        return roots.stream().map(m -> {
            List<String> roles = getRolesForItem(m.getId());
            MenuDTO.Response r = MenuDTO.Response.from(m, roles);
            enrichChildren(r, m);
            return r;
        }).collect(Collectors.toList());
    }

    public MenuDTO.Response findById(Long id) {
        MenuItem m = getOrThrow(id);
        List<String> roles = getRolesForItem(id);
        MenuDTO.Response r = MenuDTO.Response.from(m, roles);
        enrichChildren(r, m);
        return r;
    }

    public MenuDTO.Response create(MenuDTO.Request request) {
        MenuItem item = new MenuItem();
        item.setLabel(request.getLabel());
        item.setPath(request.getPath());
        item.setIcon(request.getIcon());
        item.setSortOrder(request.getSortOrder());
        item.setActive(request.isActive());
        if (request.getParentId() != null)
            item.setParent(getOrThrow(request.getParentId()));
        MenuItem saved = itemRepository.save(item);
        logAuthorizationChange("MENU_CREATE", "Creacion de funcion/menu", saved.getPath(), null, buildMenuAuditValue(saved));
        return MenuDTO.Response.from(saved, List.of());
    }

    public MenuDTO.Response update(Long id, MenuDTO.Request request) {
        MenuItem item = getOrThrow(id);
        String oldValue = buildMenuAuditValue(item);
        item.setLabel(request.getLabel());
        item.setPath(request.getPath());
        item.setIcon(request.getIcon());
        item.setSortOrder(request.getSortOrder());
        item.setActive(request.isActive());
        if (request.getParentId() != null)
            item.setParent(getOrThrow(request.getParentId()));
        else
            item.setParent(null);
        MenuItem saved = itemRepository.save(item);
        logAuthorizationChange("MENU_UPDATE", "Actualizacion de funcion/menu", saved.getPath(), oldValue, buildMenuAuditValue(saved));
        return MenuDTO.Response.from(saved, getRolesForItem(id));
    }

    @Transactional
    public void delete(Long id) {
        if (!itemRepository.existsById(id))
            throw new RuntimeException("Menu item not found: " + id);
        MenuItem item = getOrThrow(id);
        String oldValue = buildMenuAuditValue(item);
        itemRepository.deleteById(id);
        logAuthorizationChange("MENU_DELETE", "Eliminacion de funcion/menu", item.getPath(), oldValue, null);
    }

    // ── Permisos ─────────────────────────────────────────────────────────────

    public List<MenuDTO.PermissionResponse> findAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(MenuDTO.PermissionResponse::from)
                .collect(Collectors.toList());
    }

    public List<MenuDTO.PermissionResponse> findPermissionsByItem(Long menuItemId) {
        return permissionRepository.findByMenuItemId(menuItemId).stream()
                .map(MenuDTO.PermissionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MenuDTO.PermissionResponse addPermission(MenuDTO.PermissionRequest request) {
        if (permissionRepository.existsByMenuItemIdAndRoleId(request.getMenuItemId(), request.getRoleId()))
            throw new RuntimeException("Permission already exists");
        MenuItem item = getOrThrow(request.getMenuItemId());
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found"));
        if (SECURITY_LOG_PATH.equals(item.getPath()) && !role.isPrivileged() && !"ADMIN".equals(role.getName())) {
            throw new RuntimeException("Only privileged profiles can receive access to security logs");
        }
        boolean canCreate = request.getCanCreate() != null && request.getCanCreate();
        boolean canUpdate = request.getCanUpdate() != null && request.getCanUpdate();
        boolean canDelete = request.getCanDelete() != null && request.getCanDelete();
        boolean canView = request.getCanView() == null ? true : request.getCanView();
        if (canCreate || canUpdate || canDelete) {
            canView = true;
        }
        if (!canView && !canCreate && !canUpdate && !canDelete) {
            throw new RuntimeException("At least one privilege must be granted");
        }
        MenuRolePermission saved = permissionRepository.save(new MenuRolePermission(
                item,
                role,
                canView,
                canCreate,
                canUpdate,
                canDelete
        ));
        logAuthorizationChange(
                "MENU_PERMISSION_CREATE",
                "Actualizacion de parametros de autorizacion",
                item.getPath(),
                null,
                "rol=" + role.getName() + ", view=" + canView + ", create=" + canCreate + ", update=" + canUpdate + ", delete=" + canDelete
        );
        return MenuDTO.PermissionResponse.from(saved);
    }

    @Transactional
    public void removePermission(Long menuItemId, Long roleId) {
        MenuItem item = getOrThrow(menuItemId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        permissionRepository.deleteByMenuItemIdAndRoleId(menuItemId, roleId);
        logAuthorizationChange(
                "MENU_PERMISSION_DELETE",
                "Eliminacion de parametros de autorizacion",
                item.getPath(),
                "rol=" + role.getName(),
                null
        );
    }

    // ── Menú filtrado por roles del usuario ──────────────────────────────────

    public List<MenuDTO.Response> getMyMenu(String email) {
        Set<String> roleNames = userRepository.findByEmail(email)
                .map(u -> u.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()))
                .orElse(Set.of());
        boolean hasPrivilegedRole = roleNames.contains("ADMIN") || roleRepository.existsByNameInAndPrivilegedTrue(roleNames);

        Set<Long> allowedItemIds = permissionRepository.findByRoleNames(roleNames).stream()
                .filter(MenuRolePermission::isCanView)
                .map(p -> p.getMenuItem().getId())
                .collect(Collectors.toSet());

        List<MenuItem> roots = itemRepository.findActiveRootItems();
        return roots.stream()
                .map(m -> buildFilteredTree(m, allowedItemIds, hasPrivilegedRole))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private MenuDTO.Response buildFilteredTree(MenuItem item, Set<Long> allowedIds, boolean hasPrivilegedRole) {
        if (!item.isActive()) return null;
        if (SECURITY_LOG_PATH.equals(item.getPath()) && !hasPrivilegedRole) return null;

        List<MenuDTO.Response> filteredChildren = item.getChildren().stream()
                .map(c -> buildFilteredTree(c, allowedIds, hasPrivilegedRole))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        boolean hasAccess    = allowedIds.contains(item.getId());
        boolean hasChildren  = !filteredChildren.isEmpty();

        if (!hasAccess && !hasChildren) return null;

        MenuDTO.Response r = new MenuDTO.Response();
        setResponseFields(r, item);
        r.getChildren().addAll(filteredChildren);
        return r;
    }

    private void setResponseFields(MenuDTO.Response r, MenuItem m) {
        try {
            var idField = MenuDTO.Response.class.getDeclaredField("id");         idField.setAccessible(true);         idField.set(r, m.getId());
            var lbField = MenuDTO.Response.class.getDeclaredField("label");      lbField.setAccessible(true);         lbField.set(r, m.getLabel());
            var ptField = MenuDTO.Response.class.getDeclaredField("path");       ptField.setAccessible(true);         ptField.set(r, m.getPath());
            var icField = MenuDTO.Response.class.getDeclaredField("icon");       icField.setAccessible(true);         icField.set(r, m.getIcon());
            var soField = MenuDTO.Response.class.getDeclaredField("sortOrder");  soField.setAccessible(true);         soField.set(r, m.getSortOrder());
            var acField = MenuDTO.Response.class.getDeclaredField("active");     acField.setAccessible(true);         acField.set(r, m.isActive());
            var piField = MenuDTO.Response.class.getDeclaredField("parentId");   piField.setAccessible(true);         piField.set(r, m.getParent() != null ? m.getParent().getId() : null);
            var chField = MenuDTO.Response.class.getDeclaredField("children");   chField.setAccessible(true);         chField.set(r, new java.util.ArrayList<>());
            var rlField = MenuDTO.Response.class.getDeclaredField("roles");      rlField.setAccessible(true);         rlField.set(r, List.of());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void enrichChildren(MenuDTO.Response response, MenuItem item) {
        List<MenuDTO.Response> enriched = item.getChildren().stream().map(c -> {
            List<String> roles = getRolesForItem(c.getId());
            MenuDTO.Response cr = MenuDTO.Response.from(c, roles);
            enrichChildren(cr, c);
            return cr;
        }).collect(Collectors.toList());
        response.getChildren().clear();
        response.getChildren().addAll(enriched);
    }

    private List<String> getRolesForItem(Long itemId) {
        return permissionRepository.findByMenuItemId(itemId).stream()
                .map(p -> p.getRole().getName())
                .collect(Collectors.toList());
    }

    private MenuItem getOrThrow(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + id));
    }

    private void logAuthorizationChange(String eventCode, String description, String target, String oldValue, String newValue) {
        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.AUTHORIZATION_PARAMETERS_UPDATED)
                .eventCode(eventCode)
                .origin("Administracion de autorizaciones")
                .target(target != null && !target.isBlank() ? target : "MENU_CONFIG")
                .performedBy(getCurrentActor())
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue));
    }

    private String buildMenuAuditValue(MenuItem item) {
        return "label=" + item.getLabel()
                + ", path=" + item.getPath()
                + ", parentId=" + (item.getParent() != null ? item.getParent().getId() : "ROOT")
                + ", active=" + item.isActive()
                + ", sortOrder=" + item.getSortOrder();
    }

    private String getCurrentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
