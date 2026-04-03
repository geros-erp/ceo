package com.geros.backend.securitylog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geros.backend.mailconfig.MailConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class SecurityAlertService {

    private final MailConfigService mailConfigService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.security-alert.enabled:false}")
    private boolean enabled;

    @Value("${app.security-alert.email-recipients:}")
    private String emailRecipients;

    @Value("${app.security-alert.webhook-urls:}")
    private String webhookUrls;

    @Value("${app.security-alert.actions:LOGIN_FAILED,ACCOUNT_LOCKED,LOGIN_OUTSIDE_ALLOWED_HOURS,PRIVILEGED_USER_ACTIVITY,IMPORTANT_FILE_MODIFIED,PASSWORD_POLICY_UPDATED,USER_CREATED,USER_UPDATED,USER_DELETED,ROLE_CREATED,ROLE_UPDATED,ROLE_DELETED,AUTHORIZATION_PARAMETERS_UPDATED,SECURITY_LOG_EXPORTED}")
    private String alertActions;

    public SecurityAlertService(MailConfigService mailConfigService, ObjectMapper objectMapper) {
        this.mailConfigService = mailConfigService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void notifyIfNeeded(SecurityLog securityLog) {
        if (!enabled || securityLog == null || !shouldAlert(securityLog.getAction())) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            sendEmailAlerts(securityLog);
            sendWebhookAlerts(securityLog);
        });
    }

    private boolean shouldAlert(SecurityLog.Action action) {
        return configuredActions().contains(action.name());
    }

    private void sendEmailAlerts(SecurityLog securityLog) {
        String subject = "[GEROS] Alerta de seguridad: " + securityLog.getAction().name();
        String body = buildHtmlBody(securityLog);
        configuredEmails().forEach(email -> mailConfigService.sendSecurityAlertEmail(email, subject, body));
    }

    private void sendWebhookAlerts(SecurityLog securityLog) {
        configuredWebhooks().forEach(url -> {
            try {
                String payload = objectMapper.writeValueAsString(new AlertPayload(securityLog));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {
                // No interrumpir la operación por fallas del canal de alerta.
            }
        });
    }

    private Set<String> configuredActions() {
        return splitCsv(alertActions);
    }

    private Set<String> configuredEmails() {
        return splitCsv(emailRecipients);
    }

    private Set<String> configuredWebhooks() {
        return splitCsv(webhookUrls);
    }

    private Set<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private String buildHtmlBody(SecurityLog log) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 700px; margin: 0 auto;">
                  <div style="background: #b91c1c; padding: 20px; border-radius: 8px 8px 0 0;">
                    <h2 style="color: #fff; margin: 0;">Alerta de Seguridad</h2>
                  </div>
                  <div style="border: 1px solid #e5e7eb; border-top: 0; padding: 20px; background: #fff;">
                    <p>Se registró un evento que requiere atención:</p>
                    <table style="width: 100%%; border-collapse: collapse; font-size: 14px;">
                      <tr><td style="padding: 8px; color: #6b7280;">Evento</td><td style="padding: 8px;"><strong>%s</strong></td></tr>
                      <tr><td style="padding: 8px; color: #6b7280;">Tipo</td><td style="padding: 8px;">%s</td></tr>
                      <tr><td style="padding: 8px; color: #6b7280;">Transacción</td><td style="padding: 8px;">%s</td></tr>
                      <tr><td style="padding: 8px; color: #6b7280;">Origen</td><td style="padding: 8px;">%s</td></tr>
                      <tr><td style="padding: 8px; color: #6b7280;">Actor</td><td style="padding: 8px;">%s</td></tr>
                      <tr><td style="padding: 8px; color: #6b7280;">Afectado</td><td style="padding: 8px;">%s</td></tr>
                      <tr><td style="padding: 8px; color: #6b7280;">Detalle</td><td style="padding: 8px;">%s</td></tr>
                    </table>
                  </div>
                </body>
                </html>
                """.formatted(
                log.getAction().name(),
                log.getEventType().name(),
                defaultValue(log.getTransactionId()),
                defaultValue(log.getOrigin()),
                SecurityLogMaskingUtils.maskEmail(log.getPerformedBy()),
                SecurityLogMaskingUtils.maskEmail(log.getTargetEmail()),
                SecurityLogMaskingUtils.maskFreeText(log.getDetail())
        );
    }

    private String defaultValue(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private record AlertPayload(
            String action,
            String eventType,
            String eventCode,
            String transactionId,
            String origin,
            String target,
            String performedBy,
            String detail,
            String createdAt
    ) {
        private AlertPayload(SecurityLog log) {
            this(
                    log.getAction().name(),
                    log.getEventType().name(),
                    log.getEventCode(),
                    log.getTransactionId(),
                    log.getOrigin(),
                    SecurityLogMaskingUtils.maskEmail(log.getTargetEmail()),
                    SecurityLogMaskingUtils.maskEmail(log.getPerformedBy()),
                    SecurityLogMaskingUtils.maskFreeText(log.getDetail()),
                    log.getCreatedAt() != null ? log.getCreatedAt().toString() : null
            );
        }
    }
}
