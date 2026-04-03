package com.geros.backend.securitylog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityLogRetentionTask {

    private static final int MINIMUM_ONLINE_RETENTION_DAYS = 365;

    private final SecurityLogRepository securityLogRepository;

    @Value("${app.security-log.online-retention-days:365}")
    private int configuredRetentionDays;

    @Scheduled(cron = "0 30 1 * * ?")
    @Transactional
    public void enforceOnlineRetention() {
        int effectiveRetentionDays = Math.max(configuredRetentionDays, MINIMUM_ONLINE_RETENTION_DAYS);
        if (configuredRetentionDays < MINIMUM_ONLINE_RETENTION_DAYS) {
            log.warn("La retencion configurada para logs ({}) es menor al minimo requerido. Se aplica {} dias en linea.",
                    configuredRetentionDays, effectiveRetentionDays);
        }

        LocalDateTime threshold = LocalDateTime.now().minusDays(effectiveRetentionDays);
        long recordsToDelete = securityLogRepository.countByCreatedAtBefore(threshold);
        if (recordsToDelete == 0) {
            log.debug("No hay logs de seguridad/ciberseguridad fuera de la ventana minima de {} dias.", effectiveRetentionDays);
            return;
        }

        long deleted = securityLogRepository.deleteByCreatedAtBefore(threshold);
        log.info("Politica de retencion aplicada sobre security_log. Conservacion en linea minima: {} dias. Registros depurados: {}.",
                effectiveRetentionDays, deleted);
    }
}
