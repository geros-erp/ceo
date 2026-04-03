package com.geros.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class RecaptchaService {

    @Value("${recaptcha.secret}")
    private String secret;

    @Value("${recaptcha.min-score}")
    private double minScore;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://www.google.com/recaptcha/api")
            .build();

    @SuppressWarnings("unchecked")
    public void verify(String token, String action) {
        try {
            Map<String, Object> response = webClient.post()
                    .uri("/siteverify")
                    .body(BodyInserters.fromFormData("secret", secret)
                            .with("response", token))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !Boolean.TRUE.equals(response.get("success")))
                throw new RuntimeException("Verificación reCaptcha fallida");

            Object scoreObj = response.get("score");
            if (scoreObj instanceof Number score && score.doubleValue() < minScore)
                throw new RuntimeException("Puntuación reCaptcha insuficiente. Intente nuevamente");

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Si falla la conexión con Google en desarrollo, permitir continuar
            System.err.println(">>> reCaptcha verification error: " + e.getMessage());
        }
    }
}
