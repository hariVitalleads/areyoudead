-- FCM device token and notification schedule for push notifications (alarm-style)
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS fcm_token TEXT NULL;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS notification_times_json TEXT NULL;

COMMENT ON COLUMN app_user.fcm_token IS 'FCM device token for push notifications';
COMMENT ON COLUMN app_user.notification_times_json IS 'JSON array of daily reminder times in HH:mm (UTC), e.g. ["09:00", "18:00"]';
