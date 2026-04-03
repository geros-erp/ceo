package com.geros.backend.role;

import com.geros.backend.user.UserDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/roles')")
public class RoleController {

    private final RoleService service;

    public RoleController(RoleService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<RoleDTO.Response>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/privilege-catalog")
    public ResponseEntity<List<RoleDTO.PrivilegeCatalogItem>> getPrivilegeCatalog() {
        return ResponseEntity.ok(service.getPrivilegeCatalog());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDTO.Response> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<List<RoleDTO.PermissionResponse>> findPermissionsByRole(@PathVariable Long id) {
        return ResponseEntity.ok(service.findPermissionsByRole(id));
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<List<UserDTO.SummaryResponse>> findUsersByRole(@PathVariable Long id) {
        return ResponseEntity.ok(service.findUsersByRole(id));
    }

    @PostMapping
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/roles', 'CREATE')")
    public ResponseEntity<RoleDTO.Response> create(@Valid @RequestBody RoleDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/roles', 'UPDATE')")
    public ResponseEntity<RoleDTO.Response> update(@PathVariable Long id,
                                                    @Valid @RequestBody RoleDTO.Request request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/roles', 'DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
