-- Relax contact_index constraint to support configurable max (app.emergency-contacts.max-count, max 20)
ALTER TABLE emergency_contact DROP CONSTRAINT IF EXISTS ck_emergency_contact_index;
ALTER TABLE emergency_contact ADD CONSTRAINT ck_emergency_contact_index CHECK (contact_index BETWEEN 1 AND 20);
