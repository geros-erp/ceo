package com.geros.backend.mailconfig;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mail-config")
@PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/mail-config')")
public class MailConfigController {

    private final MailConfigService service;

    public MailConfigController(MailConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<MailConfigDTO.Response> get() {
        return ResponseEntity.ok(service.get());
    }

    @PutMapping
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/mail-config', 'UPDATE')")
    public ResponseEntity<MailConfigDTO.Response> update(@RequestBody MailConfigDTO.Request request) {
        return ResponseEntity.ok(service.update(request));
    }
}
