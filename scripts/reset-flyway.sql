-- Reset Flyway and all application tables. Run manually with psql.
-- Usage: psql -h localhost -U areyoudead -d areyoudead -f scripts/reset-flyway.sql

-- Drop application tables (order matters due to foreign keys)
DROP TABLE IF EXISTS emergency_contact CASCADE;
DROP TABLE IF EXISTS registration CASCADE;
DROP TABLE IF EXISTS audit_event CASCADE;
DROP TABLE IF EXISTS app_user CASCADE;

-- Drop Flyway schema history (clears all migration records)
DROP TABLE IF EXISTS flyway_schema_history CASCADE;
