-- Refresh tokens for JWT refresh flow
CREATE TABLE IF NOT EXISTS refresh_token (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  token_hash TEXT NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_refresh_token_user_id ON refresh_token (user_id);
CREATE INDEX IF NOT EXISTS ix_refresh_token_expires_at ON refresh_token (expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS ux_refresh_token_token_hash ON refresh_token (token_hash);

-- Emergency contact: email verification and opt-out
ALTER TABLE emergency_contact ADD COLUMN IF NOT EXISTS verification_token TEXT NULL;
ALTER TABLE emergency_contact ADD COLUMN IF NOT EXISTS verification_token_expires_at TIMESTAMPTZ NULL;
ALTER TABLE emergency_contact ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ NULL;
ALTER TABLE emergency_contact ADD COLUMN IF NOT EXISTS opted_out_at TIMESTAMPTZ NULL;

-- User: manual check-in ("all clear") to dismiss/snooze alerts
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_manual_check_in_at TIMESTAMPTZ NULL;
