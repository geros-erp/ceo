package com.geros.backend.policy;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policy")
@PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/policy')")
public class PasswordPolicyController {

    private final PasswordPolicyService service;

    public PasswordPolicyController(PasswordPolicyService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PasswordPolicyDTO.Response> get() {
        return ResponseEntity.ok(service.get());
    }

    @PutMapping
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/policy', 'UPDATE')")
    public ResponseEntity<PasswordPolicyDTO.Response> update(@RequestBody PasswordPolicyDTO.Request request) {
        return ResponseEntity.ok(service.update(request));
    }
}
