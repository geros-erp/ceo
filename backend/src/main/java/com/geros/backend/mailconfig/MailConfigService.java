package com.geros.backend.mailconfig;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class MailConfigService {

    private final MailConfigRepository repository;

    public MailConfigService(MailConfigRepository repository) {
        this.repository = repository;
    }

    public MailConfigDTO.Response get() {
        return MailConfigDTO.Response.from(getOrCreate());
    }

    public MailConfigDTO.Response update(MailConfigDTO.Request request) {
        MailConfig config = getOrCreate();
        config.setEnabled(request.isEnabled());
        config.setHost(request.getHost());
        config.setPort(request.getPort() > 0 ? request.getPort() : 587);
        config.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isBlank())
            config.setPassword(request.getPassword());
        config.setUseTls(request.isUseTls());
        config.setUseSsl(request.isUseSsl());
        config.setFromAddress(request.getFromAddress());
        config.setFromName(request.getFromName());
        return MailConfigDTO.Response.from(repository.save(config));
    }

    public void sendWelcomeEmail(String toEmail, String firstName, String lastName, String password) {
        MailConfig config = getOrCreate();
        if (!config.isEnabled()) return;

        try {
            JavaMailSenderImpl sender = buildSender(config);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromName    = config.getFromName()    != null ? config.getFromName()    : "Sistema Geros";
            String fromAddress = config.getFromAddress() != null ? config.getFromAddress() : config.getUsername();

            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(toEmail);
            helper.setSubject("Creación de Credenciales");
            helper.setText(buildBody(firstName, lastName, toEmail, password), true);

            sender.send(message);
        } catch (Exception e) {
            System.err.println(">>> Error al enviar correo a " + toEmail + ": " + e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String toEmail, String firstName, String lastName, String token) {
        MailConfig config = getOrCreate();
        if (!config.isEnabled()) return;

        try {
            JavaMailSenderImpl sender = buildSender(config);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromName    = config.getFromName()    != null ? config.getFromName()    : "Sistema Geros";
            String fromAddress = config.getFromAddress() != null ? config.getFromAddress() : config.getUsername();
            String resetLink   = "https://localhost:5173/reset-password?token=" + token;

            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(toEmail);
            helper.setSubject("Recuperación de Contraseña");
            helper.setText(buildResetBody(firstName, lastName, resetLink), true);

            sender.send(message);
        } catch (Exception e) {
            System.err.println(">>> Error al enviar correo de recuperación a " + toEmail + ": " + e.getMessage());
        }
    }

    public void sendSecurityAlertEmail(String toEmail, String subject, String htmlBody) {
        MailConfig config = getOrCreate();
        if (!config.isEnabled()) return;

        try {
            JavaMailSenderImpl sender = buildSender(config);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromName = config.getFromName() != null ? config.getFromName() : "Sistema Geros";
            String fromAddress = config.getFromAddress() != null ? config.getFromAddress() : config.getUsername();

            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            sender.send(message);
        } catch (Exception e) {
            System.err.println(">>> Error al enviar alerta de seguridad a " + toEmail + ": " + e.getMessage());
        }
    }

    private String buildResetBody(String firstName, String lastName, String resetLink) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                  <div style="background: #4f46e5; padding: 24px; border-radius: 8px 8px 0 0;">
                    <h2 style="color: #fff; margin: 0;">Recuperación de Contraseña</h2>
                  </div>
                  <div style="background: #f9fafb; padding: 24px; border-radius: 0 0 8px 8px; border: 1px solid #e5e7eb;">
                    <p>Estimado/a <strong>%s %s</strong>,</p>
                    <p>Recibimos una solicitud para restablecer su contraseña. Haga clic en el siguiente enlace:</p>
                    <div style="text-align: center; margin: 24px 0;">
                      <a href="%s" style="background: #4f46e5; color: #fff; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: bold;">
                        Restablecer contraseña
                      </a>
                    </div>
                    <p style="color: #dc2626;"><strong>⚠ Este enlace expira en 1 hora.</strong></p>
                    <p style="color: #6b7280; font-size: 0.85rem;">Si no solicitó este cambio, ignore este correo.</p>
                  </div>
                </body>
                </html>
                """.formatted(firstName, lastName, resetLink);
    }

    private JavaMailSenderImpl buildSender(MailConfig config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());
        sender.setUsername(config.getUsername());
        sender.setPassword(config.getPassword());
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isUseTls()));
        props.put("mail.smtp.ssl.enable", String.valueOf(config.isUseSsl()));
        props.put("mail.debug", "false");

        return sender;
    }

    private String buildBody(String firstName, String lastName, String email, String password) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                  <div style="background: #4f46e5; padding: 24px; border-radius: 8px 8px 0 0;">
                    <h2 style="color: #fff; margin: 0;">Creación de Credenciales</h2>
                  </div>
                  <div style="background: #f9fafb; padding: 24px; border-radius: 0 0 8px 8px; border: 1px solid #e5e7eb;">
                    <p>Estimado/a <strong>%s %s</strong>,</p>
                    <p>Se ha creado una cuenta de acceso al sistema con las siguientes credenciales:</p>
                    <table style="background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; padding: 16px; width: 100%%;">
                      <tr><td style="padding: 8px; color: #6b7280;">Usuario:</td><td style="padding: 8px;"><strong>%s</strong></td></tr>
                      <tr><td style="padding: 8px; color: #6b7280;">Contraseña temporal:</td><td style="padding: 8px;"><strong>%s</strong></td></tr>
                    </table>
                    <p style="margin-top: 16px; color: #dc2626;">
                      <strong>⚠ Importante:</strong> Por seguridad, deberá cambiar su contraseña en el primer inicio de sesión.
                    </p>
                    <p style="color: #6b7280; font-size: 0.85rem; margin-top: 24px;">
                      Este es un mensaje automático, por favor no responda a este correo.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(firstName, lastName, email, password);
    }

    private MailConfig getOrCreate() {
        return repository.findById(1L).orElseGet(() -> repository.save(new MailConfig()));
    }
}
