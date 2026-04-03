package com.geros.backend.securitylog;

import com.geros.backend.trace.TransactionContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SecurityLogService {

    private static final DateTimeFormatter EXPORT_FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter EXPORT_CELL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SecurityLogRepository repository;
    private final SecurityLogExportRepository exportRepository;
    private final SecurityAlertService securityAlertService;

    @Value("${app.security-log-export-dir:./storage/security-log-exports}")
    private String exportDirectory;

    public SecurityLogService(SecurityLogRepository repository, SecurityLogExportRepository exportRepository,
                              SecurityAlertService securityAlertService) {
        this.repository = repository;
        this.exportRepository = exportRepository;
        this.securityAlertService = securityAlertService;
    }

    public void log(SecurityLog.Action action, String targetEmail,
                    String performedBy, String detail) {
        log(new AuditEntry(action)
                .target(targetEmail)
                .performedBy(performedBy)
                .description(detail));
    }

    public void log(AuditEntry entry) {
        RequestMetadata requestMetadata = resolveRequestMetadata();
        SecurityLog.EventType eventType = entry.eventType != null ? entry.eventType : defaultEventType(entry.action);
        String eventCode = hasText(entry.eventCode) ? entry.eventCode : entry.action.name();
        String origin = hasText(entry.origin) ? entry.origin : requestMetadata.origin();
        String target = hasText(entry.targetEmail) ? entry.targetEmail : "N/A";

        SecurityLog saved = repository.save(new SecurityLog(
                entry.action,
                eventType,
                eventCode,
                origin,
                blankToNull(TransactionContext.getCurrentTransactionId()),
                target,
                blankToNull(entry.performedBy),
                blankToNull(entry.description),
                blankToNull(requestMetadata.ipAddress()),
                blankToNull(requestMetadata.hostName()),
                blankToNull(entry.oldValue),
                blankToNull(entry.newValue)
        ));
        securityAlertService.notifyIfNeeded(saved);
    }

    public Page<SecurityLogDTO.Response> findAll(String email, String userFrom, String userTo,
                                                String status, String action,
                                                LocalDate specificDate, LocalDateTime createdFrom,
                                                LocalDateTime createdTo, Pageable pageable) {
        LocalDateTime effectiveFrom = resolveCreatedFrom(specificDate, createdFrom);
        LocalDateTime effectiveTo = resolveCreatedTo(specificDate, createdTo);
        Page<SecurityLogDTO.Response> page = repository.findWithFilters(
                email  != null && !email.isBlank()  ? email  : null,
                userFrom != null && !userFrom.isBlank() ? userFrom : null,
                userTo != null && !userTo.isBlank() ? userTo : null,
                status != null && !status.isBlank() ? status : null,
                action != null && !action.isBlank() ? action : null,
                effectiveFrom,
                effectiveTo,
                pageable
        ).map(SecurityLogDTO.Response::from);
        logQueryReport(email, userFrom, userTo, status, action, effectiveFrom, effectiveTo, page.getTotalElements());
        return page;
    }

    public SecurityLogDTO.ExportResponse exportLogs(String email, String userFrom, String userTo,
                                                    String status, String action,
                                                    LocalDate specificDate, LocalDateTime createdFrom,
                                                    LocalDateTime createdTo) {
        LocalDateTime effectiveFrom = resolveCreatedFrom(specificDate, createdFrom);
        LocalDateTime effectiveTo = resolveCreatedTo(specificDate, createdTo);
        List<SecurityLog> logs = repository.findAllForExport(
                email != null && !email.isBlank() ? email : null,
                userFrom != null && !userFrom.isBlank() ? userFrom : null,
                userTo != null && !userTo.isBlank() ? userTo : null,
                status != null && !status.isBlank() ? status : null,
                action != null && !action.isBlank() ? action : null,
                effectiveFrom,
                effectiveTo
        );

        try {
            Path exportDir = Paths.get(exportDirectory).toAbsolutePath().normalize();
            Files.createDirectories(exportDir);

            String actor = getCurrentActor();
            String fileName = "security-log-" + java.time.LocalDateTime.now().format(EXPORT_FILE_TIME) + ".csv";
            Path filePath = exportDir.resolve(fileName);

            Files.writeString(filePath, buildCsv(logs), StandardCharsets.UTF_8);

            SecurityLogExport export = exportRepository.save(new SecurityLogExport(
                    fileName,
                    filePath.toString(),
                    actor,
                    blankToNull(email),
                    blankToNull(action),
                    logs.size()
            ));

            log(new AuditEntry(SecurityLog.Action.SECURITY_LOG_EXPORTED)
                    .eventCode("SECURITY_LOG_EXPORT")
                    .origin("Repositorio independiente de logs")
                    .target(fileName)
                    .performedBy(actor)
                    .description("Exportacion de logs a repositorio independiente")
                    .newValue("archivo=" + fileName + ", registros=" + logs.size()));

            return SecurityLogDTO.ExportResponse.from(export);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to export security log repository");
        }
    }

    public List<SecurityLogDTO.ExportResponse> findRecentExports() {
        return exportRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(SecurityLogDTO.ExportResponse::from)
                .toList();
    }

    public Resource downloadExport(Long id) {
        SecurityLogExport export = exportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Export not found with id: " + id));
        Path path = Paths.get(export.getFilePath()).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new RuntimeException("Export file not found");
        }
        return new FileSystemResource(path);
    }

    public SecurityLogExport getExportOrThrow(Long id) {
        return exportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Export not found with id: " + id));
    }

    private String buildCsv(List<SecurityLog> logs) {
        StringBuilder csv = new StringBuilder();
        csv.append("fecha_hora,transaccion,evento,tipo,codigo,origen,usuario_afectado,realizado_por,equipo,ip,valor_anterior,valor_nuevo,descripcion\n");
        for (SecurityLog log : logs) {
            csv.append(csvCell(log.getCreatedAt() != null ? log.getCreatedAt().format(EXPORT_CELL_TIME) : null)).append(',')
               .append(csvCell(log.getTransactionId())).append(',')
               .append(csvCell(log.getAction().name())).append(',')
               .append(csvCell(log.getEventType().name())).append(',')
               .append(csvCell(log.getEventCode())).append(',')
               .append(csvCell(log.getOrigin())).append(',')
               .append(csvCell(SecurityLogMaskingUtils.maskEmail(log.getTargetEmail()))).append(',')
               .append(csvCell(SecurityLogMaskingUtils.maskEmail(log.getPerformedBy()))).append(',')
               .append(csvCell(SecurityLogMaskingUtils.maskHost(log.getHostName()))).append(',')
               .append(csvCell(SecurityLogMaskingUtils.maskIp(log.getIpAddress()))).append(',')
               .append(csvCell(SecurityLogMaskingUtils.maskFreeText(log.getOldValue()))).append(',')
               .append(csvCell(SecurityLogMaskingUtils.maskFreeText(log.getNewValue()))).append(',')
               .append(csvCell(SecurityLogMaskingUtils.maskFreeText(log.getDetail()))).append('\n');
        }
        return csv.toString();
    }

    private String csvCell(String value) {
        String text = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private String getCurrentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private LocalDateTime resolveCreatedFrom(LocalDate specificDate, LocalDateTime createdFrom) {
        if (specificDate != null) {
            return specificDate.atStartOfDay();
        }
        return createdFrom;
    }

    private LocalDateTime resolveCreatedTo(LocalDate specificDate, LocalDateTime createdTo) {
        if (specificDate != null) {
            return specificDate.atTime(23, 59, 59);
        }
        return createdTo;
    }

    private void logQueryReport(String email, String userFrom, String userTo, String status, String action,
                                LocalDateTime createdFrom, LocalDateTime createdTo, long resultCount) {
        String actor = getCurrentActor();
        if ("SYSTEM".equals(actor)) {
            return;
        }

        log(new AuditEntry(SecurityLog.Action.SECURITY_LOG_QUERIED)
                .eventCode("SECURITY_LOG_QUERY")
                .origin("Consulta de logs de produccion")
                .target("SECURITY_LOG")
                .performedBy(actor)
                .description("Consulta de logs en modo solo lectura")
                .oldValue("email=" + normalizeAuditValue(email)
                        + ", userFrom=" + normalizeAuditValue(userFrom)
                        + ", userTo=" + normalizeAuditValue(userTo)
                        + ", status=" + normalizeAuditValue(status)
                        + ", action=" + normalizeAuditValue(action)
                        + ", createdFrom=" + normalizeAuditValue(createdFrom)
                        + ", createdTo=" + normalizeAuditValue(createdTo))
                .newValue("resultados=" + resultCount));
    }

    private String normalizeAuditValue(Object value) {
        if (value == null) {
            return "N/A";
        }
        String text = value.toString().trim();
        return text.isEmpty() ? "N/A" : text;
    }

    private SecurityLog.EventType defaultEventType(SecurityLog.Action action) {
        return switch (action) {
            case LOGIN_FAILED -> SecurityLog.EventType.ERROR;
            default -> SecurityLog.EventType.SUCCESS;
        };
    }

    private RequestMetadata resolveRequestMetadata() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return new RequestMetadata("Proceso interno", null, null);
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        String origin = request.getMethod() + " " + request.getRequestURI();
        String ipAddress = extractIpAddress(request);
        String hostName = extractHostName(request, ipAddress);
        return new RequestMetadata(origin, ipAddress, hostName);
    }

    private String extractIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String extractHostName(HttpServletRequest request, String ipAddress) {
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (hasText(forwardedHost)) {
            return forwardedHost.trim();
        }
        String remoteHost = request.getRemoteHost();
        if (hasText(remoteHost) && !remoteHost.equals(ipAddress)) {
            return remoteHost.trim();
        }
        return request.getRemoteAddr();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    public static class AuditEntry {
        private final SecurityLog.Action action;
        private SecurityLog.EventType eventType;
        private String eventCode;
        private String origin;
        private String targetEmail;
        private String performedBy;
        private String description;
        private String oldValue;
        private String newValue;

        public AuditEntry(SecurityLog.Action action) {
            this.action = action;
        }

        public AuditEntry eventType(SecurityLog.EventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public AuditEntry eventCode(String eventCode) {
            this.eventCode = eventCode;
            return this;
        }

        public AuditEntry origin(String origin) {
            this.origin = origin;
            return this;
        }

        public AuditEntry target(String targetEmail) {
            this.targetEmail = targetEmail;
            return this;
        }

        public AuditEntry performedBy(String performedBy) {
            this.performedBy = performedBy;
            return this;
        }

        public AuditEntry description(String description) {
            this.description = description;
            return this;
        }

        public AuditEntry oldValue(String oldValue) {
            this.oldValue = oldValue;
            return this;
        }

        public AuditEntry newValue(String newValue) {
            this.newValue = newValue;
            return this;
        }
    }

    private record RequestMetadata(String origin, String ipAddress, String hostName) {}
}
