package com.geros.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("🚀 Iniciando migración híbrida (UOM -> public, Productos -> inventory)...");
        try {
            // 1. Asegurar esquema inventory
            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS inventory");

            // 2. LIMPIEZA DRÁSTICA: Eliminar la tabla del esquema equivocado (inventory)
            if (checkTableExists("inventory", "units_of_measure")) {
                log.info("🧹 Eliminando tabla huérfana inventory.units_of_measure...");
                try {
                    // Recuperar datos antes de borrar si la tabla en public no los tiene
                    jdbcTemplate.execute("INSERT INTO public.units_of_measure (description, abbreviation, is_active, allows_decimal) " +
                                       "SELECT description, abbreviation, is_active, allows_decimal FROM inventory.units_of_measure " +
                                       "ON CONFLICT DO NOTHING");
                } catch (Exception e) { log.warn("Aviso: No se pudieron migrar algunos datos de inventory a public (posible diferencia de columnas)"); }

                jdbcTemplate.execute("DROP TABLE inventory.units_of_measure CASCADE");
                log.info("✅ Tabla inventory.units_of_measure eliminada definitivamente.");
            }

            // --- SECCIÓN: UNIDADES DE MEDIDA (public) ---
            log.info("🔹 Validando Unidades de Medida en esquema PUBLIC...");

            // Renombrar 'name' a 'description' si existe
            if (checkColumnExists("public", "units_of_measure", "name")) {
                jdbcTemplate.execute("ALTER TABLE public.units_of_measure RENAME COLUMN name TO description");
                log.info("✅ public.units_of_measure: 'name' -> 'description'");
            }

            // Asegurar columnas necesarias
            ensureColumnExists("public", "units_of_measure", "is_active", "BOOLEAN DEFAULT TRUE");
            ensureColumnExists("public", "units_of_measure", "allows_decimal", "BOOLEAN DEFAULT FALSE");
            ensureColumnExists("public", "units_of_measure", "created_by", "VARCHAR(100)");
            ensureColumnExists("public", "units_of_measure", "updated_by", "VARCHAR(100)");
            ensureColumnExists("public", "units_of_measure", "created_at", "TIMESTAMP DEFAULT NOW()");
            ensureColumnExists("public", "units_of_measure", "updated_at", "TIMESTAMP DEFAULT NOW()");

            // --- SECCIÓN: PRODUCTOS (inventory) ---
            log.info("🔹 Validando Productos en esquema INVENTORY...");

            // Mover de public a inventory si existe en public
            if (checkTableExists("public", "products")) {
                jdbcTemplate.execute("ALTER TABLE public.products SET SCHEMA inventory");
                log.info("✅ Tabla products movida de public a INVENTORY");
            }

            // Asegurar columna is_active en productos
            ensureColumnExists("inventory", "products", "is_active", "BOOLEAN DEFAULT TRUE");
            ensureColumnExists("inventory", "products", "created_by", "VARCHAR(100)");
            ensureColumnExists("inventory", "products", "updated_by", "VARCHAR(100)");
            ensureColumnExists("inventory", "products", "created_at", "TIMESTAMP DEFAULT NOW()");
            ensureColumnExists("inventory", "products", "updated_at", "TIMESTAMP DEFAULT NOW()");

            // --- SECCIÓN: MENÚ (menu) ---
            log.info("🔹 Validando Menú en esquema MENU...");
            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS menu");
            ensureColumnExists("menu", "menu_items", "icon", "VARCHAR(100)");
            updateMenuIcons();

            // --- SECCIÓN: AUDITORÍA (security_log) ---
            setupAuditSystem();

            log.info("✨ Migración híbrida finalizada con éxito.");

        } catch (Exception e) {
            log.error("❌ Error en migración: {}", e.getMessage());
        }
    }

    /**
     * Sincroniza iconos lógicos basados en el label del menú para asegurar
     * compatibilidad con react-icons/fi en el frontend.
     */
    private void updateMenuIcons() {
        if (!checkTableExists("menu", "menu_items")) return;
        log.info("🖼️ Sincronizando iconografía del menú...");
        String sql = """
            UPDATE menu.menu_items SET icon = CASE
                WHEN label ILIKE '%dashboard%' OR label ILIKE '%inicio%' OR label ILIKE '%principal%' THEN 'home'
                WHEN label ILIKE '%usuario%' THEN 'users'
                WHEN label ILIKE '%rol%' OR label ILIKE '%perfil%' OR label ILIKE '%permiso%' THEN 'shield'
                WHEN label ILIKE '%seguridad%' THEN 'shield'
                WHEN label ILIKE '%producto%' OR label ILIKE '%inventario%' THEN 'package'
                WHEN label ILIKE '%unidad%' OR label ILIKE '%medida%' THEN 'layers'
                WHEN label ILIKE '%contrato%' THEN 'briefcase'
                WHEN label ILIKE '%proyecto%' THEN 'folder'
                WHEN label ILIKE '%log%' OR label ILIKE '%actividad%' OR label ILIKE '%auditoria%' THEN 'activity'
                WHEN label ILIKE '%correo%' OR label ILIKE '%email%' OR label ILIKE '%smtp%' THEN 'mail'
                WHEN label ILIKE '%directorio%' OR label ILIKE '%servidor%' OR label ILIKE '% ad %' OR label ILIKE 'ad %' OR label = 'AD' THEN 'server'
                WHEN label ILIKE '%configuracion%' OR label ILIKE '%ajuste%' OR label ILIKE '%sistema%' THEN 'settings'
                WHEN label ILIKE '%clave%' OR label ILIKE '%password%' OR label ILIKE '%politica%' THEN 'lock'
                WHEN label ILIKE '%historial%' OR label ILIKE '%menu%' OR label ILIKE '%lista%' THEN 'list'
                WHEN label ILIKE '%reservado%' OR label ILIKE '%identidad%' THEN 'user-x'
                ELSE COALESCE(NULLIF(icon, 'FiCircle'), 'circle')
            END
            WHERE icon IS NULL OR icon = '' OR icon = 'FiCircle' OR icon = 'circle' OR icon ~ '[^[:ascii:]]'
               OR (icon NOT IN ('home', 'users', 'shield', 'package', 'layers', 'briefcase', 'folder', 'activity',
                               'mail', 'server', 'settings', 'lock', 'list', 'user-x', 'shield-check'))
        """;
        try {
            int rows = jdbcTemplate.update(sql);
            if (rows > 0) log.info("✅ Se actualizaron {} iconos de menú con nombres lógicos.", rows);
        } catch (Exception e) { log.warn("No se pudo actualizar la iconografía del menú: {}", e.getMessage()); }
    }

    private boolean checkTableExists(String schema, String table) {
        try {
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name=?)",
                Boolean.class, schema, table));
        } catch (Exception e) { return false; }
    }

    private boolean checkColumnExists(String schema, String table, String column) {
        try {
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=? AND table_name=? AND column_name=?)",
                Boolean.class, schema, table, column));
        } catch (Exception e) { return false; }
    }

    private void ensureColumnExists(String schema, String table, String column, String typeDef) {
        if (checkTableExists(schema, table) && !checkColumnExists(schema, table, column)) {
            try {
                jdbcTemplate.execute(String.format("ALTER TABLE %s.%s ADD COLUMN %s %s", schema, table, column, typeDef));
                log.info("➕ Columna '{}' añadida a {}.{}", column, schema, table);
            } catch (Exception e) { log.warn("No se pudo agregar columna {}: {}", column, e.getMessage()); }
        }
    }

    /**
     * Configura la tabla de logs y los triggers necesarios para la trazabilidad.
     */
    private void setupAuditSystem() {
        log.info("🛡️ Configurando sistema de auditoría en DB...");

        // 1. Crear tabla de logs si no existe
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS public.security_log (
                id SERIAL PRIMARY KEY,
                event_type VARCHAR(50) NOT NULL,
                table_name VARCHAR(100) NOT NULL,
                record_id VARCHAR(100),
                action_user VARCHAR(100),
                details JSONB,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);

        // 2. Crear función de trigger para auditoría
        jdbcTemplate.execute("""
            CREATE OR REPLACE FUNCTION public.fn_log_entity_changes()
            RETURNS TRIGGER AS $$
            BEGIN
                IF (TG_OP = 'INSERT') THEN
                    INSERT INTO public.security_log (event_type, table_name, record_id, action_user, details)
                    VALUES ('CREATE', TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME, CAST(NEW.id AS TEXT), NEW.created_by, row_to_json(NEW)::jsonb);
                ELSIF (TG_OP = 'UPDATE') THEN
                    INSERT INTO public.security_log (event_type, table_name, record_id, action_user, details)
                    VALUES ('UPDATE', TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME, CAST(NEW.id AS TEXT), NEW.updated_by,
                            jsonb_build_object('old', row_to_json(OLD)::jsonb, 'new', row_to_json(NEW)::jsonb));
                ELSIF (TG_OP = 'DELETE') THEN
                    INSERT INTO public.security_log (event_type, table_name, record_id, action_user, details)
                    VALUES ('DELETE', TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME, CAST(OLD.id AS TEXT), 'system', row_to_json(OLD)::jsonb);
                END IF;
                RETURN NULL;
            END;
            $$ LANGUAGE plpgsql;
        """);

        // 3. Aplicar triggers a las tablas
        applyAuditTrigger("public", "units_of_measure");
        applyAuditTrigger("inventory", "products");
    }

    private void applyAuditTrigger(String schema, String table) {
        if (checkTableExists(schema, table)) {
            String triggerName = "trg_" + table + "_audit";
            jdbcTemplate.execute(String.format("DROP TRIGGER IF EXISTS %s ON %s.%s", triggerName, schema, table));
            jdbcTemplate.execute(String.format("""
                CREATE TRIGGER %s
                AFTER INSERT OR UPDATE OR DELETE ON %s.%s
                FOR EACH ROW EXECUTE FUNCTION public.fn_log_entity_changes()
            """, triggerName, schema, table));
            log.info("✅ Trigger de auditoría aplicado a {}.{}", schema, table);
        }
    }
}
