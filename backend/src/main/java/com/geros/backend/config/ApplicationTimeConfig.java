package com.geros.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class ApplicationTimeConfig {

    @Value("${app.timezone:America/Bogota}")
    private String applicationTimeZone;

    @PostConstruct
    public void configureTimeZone() {
        TimeZone timeZone = TimeZone.getTimeZone(ZoneId.of(applicationTimeZone));
        TimeZone.setDefault(timeZone);
    }
}
