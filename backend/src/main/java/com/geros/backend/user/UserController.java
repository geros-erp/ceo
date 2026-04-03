package com.geros.backend.user;

import com.geros.backend.policy.PasswordHistory;
import com.geros.backend.policy.PasswordHistoryRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/dashboard')")
public class UserController {

    private final UserService             userService;
    private final PasswordHistoryRepository historyRepository;

    public UserController(UserService userService, PasswordHistoryRepository historyRepository) {
        this.userService       = userService;
        this.historyRepository = historyRepository;
    }

    @GetMapping
    public ResponseEntity<Page<UserDTO.Response>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        return ResponseEntity.ok(userService.findAll(search, isActive, roleId, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO.Response> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/dashboard', 'CREATE')")
    public ResponseEntity<UserDTO.Response> create(@Valid @RequestBody UserDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/dashboard', 'UPDATE')")
    public ResponseEntity<UserDTO.Response> update(@PathVariable Long id,
                                                   @RequestBody UserDTO.UpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PutMapping("/{id}/lock")
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/dashboard', 'UPDATE')")
    public ResponseEntity<UserDTO.Response> lock(@PathVariable Long id) {
        return ResponseEntity.ok(userService.lock(id));
    }

    @PutMapping("/{id}/unlock")
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/dashboard', 'UPDATE')")
    public ResponseEntity<UserDTO.Response> unlock(@PathVariable Long id) {
        return ResponseEntity.ok(userService.unlock(id));
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/dashboard', 'UPDATE')")
    public ResponseEntity<UserDTO.Response> adminChangePassword(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO.AdminChangePasswordRequest request) {
        return ResponseEntity.ok(userService.adminChangePassword(id, request));
    }

    @GetMapping("/{id}/password-history")
    public ResponseEntity<List<Map<String, String>>> getPasswordHistory(@PathVariable Long id) {
        User user = userService.getOrThrow(id);
        List<Map<String, String>> history = historyRepository
                .findByUserOrderByChangedAtDesc(user)
                .stream()
                .map(h -> Map.of(
                    "id",        h.getId().toString(),
                    "changedAt", h.getChangedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accessControlService.hasPrivilege(authentication, '/dashboard', 'DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
