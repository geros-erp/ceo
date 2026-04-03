package com.geros.backend.securitylog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/security-log")
@PreAuthorize("@accessControlService.hasMenuAccess(authentication, '/security-log')")
public class SecurityLogController {

    private final SecurityLogService service;

    public SecurityLogController(SecurityLogService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<SecurityLogDTO.Response>> findAll(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String userFrom,
            @RequestParam(required = false) String userTo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate specificDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.findAll(
                email, userFrom, userTo, status, action, specificDate, createdFrom, createdTo, pageable
        ));
    }

    @GetMapping("/actions")
    public ResponseEntity<List<String>> getActions() {
        return ResponseEntity.ok(
            Arrays.stream(SecurityLog.Action.values()).map(Enum::name).toList()
        );
    }

    @PostMapping("/exports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SecurityLogDTO.ExportResponse> exportLogs(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String userFrom,
            @RequestParam(required = false) String userTo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate specificDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {
        return ResponseEntity.ok(service.exportLogs(
                email, userFrom, userTo, status, action, specificDate, createdFrom, createdTo
        ));
    }

    @GetMapping("/exports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SecurityLogDTO.ExportResponse>> findExports() {
        return ResponseEntity.ok(service.findRecentExports());
    }

    @GetMapping("/exports/{id}/download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadExport(@PathVariable Long id) {
        SecurityLogExport export = service.getExportOrThrow(id);
        Resource resource = service.downloadExport(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.getFileName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}
