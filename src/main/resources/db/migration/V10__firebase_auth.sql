-- Firebase Auth: nullable password for Firebase-only users, stable Firebase UID link.

ALTER TABLE app_user ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE app_user ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

ALTER TABLE app_user ADD COLUMN IF NOT EXISTS firebase_uid TEXT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_firebase_uid
  ON app_user (firebase_uid)
  WHERE firebase_uid IS NOT NULL;
