package com.geros.backend.securitylog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ImportantFileMonitorTask {

    private final SecurityLogService securityLogService;
    private final Map<String, String> knownChecksums = new ConcurrentHashMap<>();

    @Value("${app.security-alert.monitored-files:src/main/resources/application.properties,src/main/resources/geros-keystore.p12}")
    private String monitoredFiles;

    public ImportantFileMonitorTask(SecurityLogService securityLogService) {
        this.securityLogService = securityLogService;
    }

    @Scheduled(fixedDelayString = "${app.security-alert.file-check-interval-ms:60000}")
    public void scanImportantFiles() {
        monitoredPaths().forEach(this::checkFileIntegrity);
    }

    private void checkFileIntegrity(String configuredPath) {
        Path path = Paths.get(configuredPath).toAbsolutePath().normalize();
        String fileKey = path.toString();
        String currentChecksum = calculateChecksum(path);
        String previousChecksum = knownChecksums.putIfAbsent(fileKey, currentChecksum);

        if (previousChecksum == null) {
            return;
        }

        boolean changed = previousChecksum != null && !previousChecksum.equals(currentChecksum);
        if (!changed) {
            return;
        }

        knownChecksums.put(fileKey, currentChecksum);
        securityLogService.log(new SecurityLogService.AuditEntry(SecurityLog.Action.IMPORTANT_FILE_MODIFIED)
                .eventType(SecurityLog.EventType.ERROR)
                .eventCode("IMPORTANT_FILE_MODIFIED")
                .origin("Monitoreo de integridad de archivos")
                .target(fileKey)
                .performedBy("SYSTEM")
                .description("Cambio detectado en archivo importante de la aplicacion")
                .oldValue("checksum=" + normalize(previousChecksum))
                .newValue("checksum=" + normalize(currentChecksum)));
    }

    private Set<String> monitoredPaths() {
        if (monitoredFiles == null || monitoredFiles.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(monitoredFiles.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private String calculateChecksum(Path path) {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return "MISSING";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(path);
            byte[] hash = digest.digest(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (Exception ex) {
            return "UNREADABLE";
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
