-- User alert channel preference (how to notify emergency contacts: email, SMS, or both)
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS alert_channel_preference VARCHAR(10) NULL;
ALTER TABLE app_user ADD CONSTRAINT ck_app_user_alert_channel
  CHECK (alert_channel_preference IS NULL OR alert_channel_preference IN ('EMAIL', 'SMS', 'BOTH'));
