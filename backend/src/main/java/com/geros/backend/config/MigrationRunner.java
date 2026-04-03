package com.geros.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Component
public class MigrationRunner implements CommandLineRunner {

    private final DataSource dataSource;
    private final ResourceLoader resourceLoader;

    public MigrationRunner(DataSource dataSource, ResourceLoader resourceLoader) {
        this.dataSource = dataSource;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) {
        executeMigrations();
    }

    private void executeMigrations() {
        try (Connection conn = dataSource.getConnection()) {
            for (int i = 1; i <= 30; i++) {
                executeMigration(conn, i);
            }
        } catch (Exception e) {
            log.error("Error ejecutando migraciones", e);
        }
    }

    private void executeMigration(Connection conn, int versionNumber) {
        try {
            String path = String.format("classpath:db/migrate_v%d.sql", versionNumber);
            Resource resource = resourceLoader.getResource(path);

            if (!resource.exists()) {
                return;
            }

            String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                log.info("✓ Migración v{} ejecutada", versionNumber);
            }
        } catch (Exception e) {
            log.warn("⚠ Migración v{} - {}", versionNumber, e.getMessage());
        }
    }
}
