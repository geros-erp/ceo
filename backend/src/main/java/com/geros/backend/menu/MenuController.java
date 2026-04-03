package com.geros.backend.menu;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService service;
    private final com.geros.backend.security.AuthenticationValidator authValidator;

    public MenuController(MenuService service, com.geros.backend.security.AuthenticationValidator authValidator) {
        this.service = service;
        this.authValidator = authValidator;
    }

    // ── Menú del usuario autenticado ─────────────────────────────────────────
    @GetMapping("/my-menu")
    public ResponseEntity<List<MenuDTO.Response>> myMenu(Authentication authentication) {
        String email = authValidator.getAuthenticatedUser(authentication);
        return ResponseEntity.ok(service.getMyMenu(email));
    }

    // ── CRUD ítems ───────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/menu-config')")
    public ResponseEntity<List<MenuDTO.Response>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/menu-config')")
    public ResponseEntity<MenuDTO.Response> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/menu-config', 'CREATE')")
    public ResponseEntity<MenuDTO.Response> create(@RequestBody MenuDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/menu-config', 'UPDATE')")
    public ResponseEntity<MenuDTO.Response> update(@PathVariable Long id,
                                                    @RequestBody MenuDTO.Request request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/menu-config', 'DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Permisos ─────────────────────────────────────────────────────────────
    @GetMapping("/permissions")
    @PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/menu-config')")
    public ResponseEntity<List<MenuDTO.PermissionResponse>> findAllPermissions() {
        return ResponseEntity.ok(service.findAllPermissions());
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/menu-config')")
    public ResponseEntity<List<MenuDTO.PermissionResponse>> findPermissionsByItem(@PathVariable Long id) {
        return ResponseEntity.ok(service.findPermissionsByItem(id));
    }

    @PostMapping("/permissions")
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/menu-config', 'UPDATE')")
    public ResponseEntity<MenuDTO.PermissionResponse> addPermission(@RequestBody MenuDTO.PermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addPermission(request));
    }

    @DeleteMapping("/permissions/{menuItemId}/{roleId}")
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/menu-config', 'DELETE')")
    public ResponseEntity<Void> removePermission(@PathVariable Long menuItemId,
                                                  @PathVariable Long roleId) {
        service.removePermission(menuItemId, roleId);
        return ResponseEntity.noContent().build();
    }
}
