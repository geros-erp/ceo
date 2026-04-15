package com.geros.backend.config;

import com.geros.backend.security.JwtFilter;
import com.geros.backend.security.PrivilegedActivityAuditFilter;
import com.geros.backend.security.SecurityHeadersFilter;
import com.geros.backend.trace.TransactionContext;
import com.geros.backend.trace.TransactionTraceFilter;
import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final TransactionTraceFilter transactionTraceFilter;
    private final PrivilegedActivityAuditFilter privilegedActivityAuditFilter;
    private final SecurityHeadersFilter securityHeadersFilter;

    public SecurityConfig(JwtFilter jwtFilter, TransactionTraceFilter transactionTraceFilter,
                          PrivilegedActivityAuditFilter privilegedActivityAuditFilter,
                          SecurityHeadersFilter securityHeadersFilter) {
        this.jwtFilter = jwtFilter;
        this.transactionTraceFilter = transactionTraceFilter;
        this.privilegedActivityAuditFilter = privilegedActivityAuditFilter;
        this.securityHeadersFilter = securityHeadersFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(transactionTraceFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(securityHeadersFilter, TransactionTraceFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(privilegedActivityAuditFilter, JwtFilter.class);

        return http.build();
    }

    // Redirigir HTTP (8080) → HTTPS (8443)
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.addAdditionalTomcatConnectors(httpRedirectConnector());
        return tomcat;
    }

    private Connector httpRedirectConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        return connector;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://localhost:5173", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(TransactionContext.HEADER_NAME));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
