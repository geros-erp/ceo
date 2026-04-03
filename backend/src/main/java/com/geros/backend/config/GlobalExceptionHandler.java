package com.geros.backend.config;

import com.geros.backend.security.OutputSanitizationService;
import com.geros.backend.trace.TransactionContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final OutputSanitizationService outputSanitizationService;

    public GlobalExceptionHandler(OutputSanitizationService outputSanitizationService) {
        this.outputSanitizationService = outputSanitizationService;
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Unexpected error";
        
        // Sanitizar mensaje de error para no exponer información sensible
        msg = outputSanitizationService.sanitizeErrorMessage(msg);
        
        String transactionId = currentTransactionId();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (msg.contains("not found"))      status = HttpStatus.NOT_FOUND;
        if (msg.contains("Invalid credentials") || msg.contains("inactive")) status = HttpStatus.UNAUTHORIZED;
        if (msg.contains("already in use"))  status = HttpStatus.CONFLICT;
        
        return ResponseEntity.status(status).body(Map.of(
                "message", msg,
                "transactionId", transactionId
        ));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurity(SecurityException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Security violation";
        msg = outputSanitizationService.sanitizeErrorMessage(msg);
        String transactionId = currentTransactionId();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "message", msg,
                "transactionId", transactionId
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst().orElse("Validation error");
        
        // Sanitizar mensaje de validación
        message = outputSanitizationService.sanitizeErrorMessage(message);
        
        String transactionId = currentTransactionId();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                             .body(Map.of(
                                     "message", message,
                                     "transactionId", transactionId
                             ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        // No exponer detalles de excepciones genéricas
        String msg = "Error interno del servidor";
        String transactionId = currentTransactionId();
        
        // Log del error real (no expuesto al cliente)
        System.err.println("Error no manejado [" + transactionId + "]: " + ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", msg,
                "transactionId", transactionId
        ));
    }

    private String currentTransactionId() {
        String transactionId = TransactionContext.getCurrentTransactionId();
        return transactionId != null && !transactionId.isBlank() ? transactionId : "N/A";
    }
}
