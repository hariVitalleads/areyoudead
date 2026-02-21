-- User can snooze/dismiss alerts for a period (e.g. vacation)
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS alerts_snoozed_until TIMESTAMPTZ NULL;
