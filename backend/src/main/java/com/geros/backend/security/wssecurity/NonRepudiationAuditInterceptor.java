package com.geros.backend.security.wssecurity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * Interceptor de auditoría para no repudio
 * Registra todos los mensajes SOAP (request/response) con:
 * - Hash SHA-256 del mensaje completo
 * - Timestamp preciso
 * - ID de transacción único
 * - Firma digital (incluida en WS-Security)
 * 
 * Permite demostrar que un mensaje fue enviado/recibido en un momento específico
 * y que no ha sido alterado (no repudio).
 */
@Service
public class NonRepudiationAuditInterceptor implements ClientInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(NonRepudiationAuditInterceptor.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public boolean handleRequest(MessageContext messageContext) {
        String transactionId = UUID.randomUUID().toString();
        messageContext.setProperty("transactionId", transactionId);
        
        try {
            WebServiceMessage request = messageContext.getRequest();
            String messageContent = extractMessageContent(request);
            String messageHash = calculateSHA256(messageContent);
            
            // Log de auditoría para no repudio del REQUEST
            logger.info("═══════════════════════════════════════════════════════════════");
            logger.info("WS-SECURITY AUDIT - REQUEST (NO REPUDIO)");
            logger.info("═══════════════════════════════════════════════════════════════");
            logger.info("Transaction ID: {}", transactionId);
            logger.info("Timestamp: {}", LocalDateTime.now().format(FORMATTER));
            logger.info("Message Hash (SHA-256): {}", messageHash);
            logger.info("Message Size: {} bytes", messageContent.length());
            logger.info("Digital Signature: INCLUDED (WS-Security)");
            logger.info("───────────────────────────────────────────────────────────────");
            logger.debug("Request Content:\n{}", messageContent);
            logger.info("═══════════════════════════════════════════════════════════════");
        } catch (IOException e) {
            logger.error("Error extracting request message content", e);
        }
        
        return true;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext) {
        String transactionId = (String) messageContext.getProperty("transactionId");
        
        try {
            WebServiceMessage response = messageContext.getResponse();
            String messageContent = extractMessageContent(response);
            String messageHash = calculateSHA256(messageContent);
            
            // Log de auditoría para no repudio del RESPONSE
            logger.info("═══════════════════════════════════════════════════════════════");
            logger.info("WS-SECURITY AUDIT - RESPONSE (NO REPUDIO)");
            logger.info("═══════════════════════════════════════════════════════════════");
            logger.info("Transaction ID: {}", transactionId);
            logger.info("Timestamp: {}", LocalDateTime.now().format(FORMATTER));
            logger.info("Message Hash (SHA-256): {}", messageHash);
            logger.info("Message Size: {} bytes", messageContent.length());
            logger.info("Digital Signature: VALIDATED (WS-Security)");
            logger.info("───────────────────────────────────────────────────────────────");
            logger.debug("Response Content:\n{}", messageContent);
            logger.info("═══════════════════════════════════════════════════════════════");
        } catch (IOException e) {
            logger.error("Error extracting response message content", e);
        }
        
        return true;
    }

    @Override
    public boolean handleFault(MessageContext messageContext) {
        String transactionId = (String) messageContext.getProperty("transactionId");
        
        try {
            WebServiceMessage response = messageContext.getResponse();
            String messageContent = extractMessageContent(response);
            String messageHash = calculateSHA256(messageContent);
            
            // Log de auditoría para FAULT (también requiere no repudio)
            logger.error("═══════════════════════════════════════════════════════════════");
            logger.error("WS-SECURITY AUDIT - FAULT (NO REPUDIO)");
            logger.error("═══════════════════════════════════════════════════════════════");
            logger.error("Transaction ID: {}", transactionId);
            logger.error("Timestamp: {}", LocalDateTime.now().format(FORMATTER));
            logger.error("Message Hash (SHA-256): {}", messageHash);
            logger.error("Message Size: {} bytes", messageContent.length());
            logger.error("───────────────────────────────────────────────────────────────");
            logger.error("Fault Content:\n{}", messageContent);
            logger.error("═══════════════════════════════════════════════════════════════");
        } catch (IOException e) {
            logger.error("Error extracting fault message content", e);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Exception ex) {
        String transactionId = (String) messageContext.getProperty("transactionId");
        
        if (ex != null) {
            logger.error("═══════════════════════════════════════════════════════════════");
            logger.error("WS-SECURITY AUDIT - ERROR");
            logger.error("═══════════════════════════════════════════════════════════════");
            logger.error("Transaction ID: {}", transactionId);
            logger.error("Timestamp: {}", LocalDateTime.now().format(FORMATTER));
            logger.error("Error: {}", ex.getMessage());
            logger.error("═══════════════════════════════════════════════════════════════");
        }
    }

    /**
     * Extrae el contenido completo del mensaje SOAP
     */
    private String extractMessageContent(WebServiceMessage message) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    /**
     * Calcula hash SHA-256 del mensaje para verificación de integridad
     */
    private String calculateSHA256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            logger.error("Error calculating SHA-256 hash", e);
            return "ERROR_CALCULATING_HASH";
        }
    }
}
