-- #5 User Check-In Flow: custom inactivity threshold, escalation tracking, contact label

-- Per-user inactivity threshold (days). NULL = use global default.
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS inactivity_threshold_days SMALLINT NULL;
ALTER TABLE app_user ADD CONSTRAINT ck_app_user_inactivity_threshold
  CHECK (inactivity_threshold_days IS NULL OR (inactivity_threshold_days >= 1 AND inactivity_threshold_days <= 90));

-- Escalation tracking: when we first alerted, how many contacts notified so far
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS first_alert_sent_at TIMESTAMPTZ NULL;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS contacts_alerted_count SMALLINT NULL;
ALTER TABLE app_user ADD CONSTRAINT ck_app_user_contacts_alerted
  CHECK (contacts_alerted_count IS NULL OR (contacts_alerted_count >= 1 AND contacts_alerted_count <= 20));

-- Optional label for contact (e.g. "Primary", "Mom", "Spouse")
ALTER TABLE emergency_contact ADD COLUMN IF NOT EXISTS label TEXT NULL;
