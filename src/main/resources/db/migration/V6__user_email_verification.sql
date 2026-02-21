-- User email verification for registration
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS email_verification_token TEXT NULL;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS email_verification_token_expires_at TIMESTAMPTZ NULL;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ NULL;

-- Treat existing users as verified (backward compatibility)
UPDATE app_user SET email_verified_at = created_at WHERE email_verified_at IS NULL;
