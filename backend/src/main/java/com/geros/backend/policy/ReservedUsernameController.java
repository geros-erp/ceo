package com.geros.backend.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reserved-usernames")
@RequiredArgsConstructor
@PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/reserved-usernames')")
public class ReservedUsernameController {

    private final ReservedUsernameService service;

    @GetMapping
    public List<ReservedUsernameDTO.Response> findAll() {
        return service.findAll();
    }

    @PostMapping
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/reserved-usernames', 'CREATE')")
    public ResponseEntity<ReservedUsernameDTO.Response> create(@RequestBody ReservedUsernameDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/reserved-usernames', 'DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
