package com.geros.backend.contract;

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
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService service;
    private final AuthenticationValidator authValidator;

    public ContractController(ContractService service, AuthenticationValidator authValidator) {
        this.service = service;
        this.authValidator = authValidator;
    }

    @GetMapping
    @PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/contracts')")
    public ResponseEntity<Page<ContractDTO.Response>> findAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.findAll(search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/contracts')")
    public ResponseEntity<ContractDTO.Response> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/contracts')")
    public ResponseEntity<ContractDTO.Response> create(
            @Valid @RequestBody ContractDTO.Request request,
            Authentication authentication) {
        String email = authValidator.getAuthenticatedUser(authentication);
        return ResponseEntity.ok(service.create(request, email));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/contracts')")
    public ResponseEntity<ContractDTO.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody ContractDTO.Request request,
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
