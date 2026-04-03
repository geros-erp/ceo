package com.geros.backend.adconfig;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ad-config")
@PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/ad-config')")
public class AdConfigController {

    private final AdConfigService service;

    public AdConfigController(AdConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<AdConfigDTO.Response> get() {
        return ResponseEntity.ok(service.get());
    }

    @PutMapping
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/ad-config', 'UPDATE')")
    public ResponseEntity<AdConfigDTO.Response> update(@RequestBody AdConfigDTO.Request request) {
        return ResponseEntity.ok(service.update(request));
    }
}
