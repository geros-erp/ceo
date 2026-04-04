package com.geros.backend.project;

import com.geros.backend.security.AuthenticationValidator;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;
    private final AuthenticationValidator authValidator;

    public ProjectController(ProjectService service, AuthenticationValidator authValidator) {
        this.service = service;
        this.authValidator = authValidator;
    }

    @GetMapping("/contract/{contractId}")
    @PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/contracts')")
    public ResponseEntity<Page<ProjectDTO.Response>> findByContract(
            @PathVariable Long contractId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.findByContract(contractId, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/contracts')")
    public ResponseEntity<ProjectDTO.Response> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/contracts')")
    public ResponseEntity<ProjectDTO.Response> create(
            @Valid @RequestBody ProjectDTO.Request request,
            Authentication authentication) {
        String email = authValidator.getAuthenticatedUser(authentication);
        return ResponseEntity.ok(service.create(request, email));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/contracts')")
    public ResponseEntity<ProjectDTO.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectDTO.Request request,
            Authentication authentication) {
        String email = authValidator.getAuthenticatedUser(authentication);
        return ResponseEntity.ok(service.update(id, request, email));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/contracts')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        String email = authValidator.getAuthenticatedUser(authentication);
        service.delete(id, email);
        return ResponseEntity.noContent().build();
    }
}
