-- Flyway migration: initial schema for JWT-authenticated users
-- All TIMESTAMPTZ columns store values in UTC. Application uses hibernate.jdbc.time_zone=UTC
-- and datasource options=-c TimeZone=UTC for consistent UTC handling.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS app_user (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_login_date TIMESTAMPTZ NULL,

  -- Forgot-password flow fields (single active token per user).
  password_reset_token_hash TEXT NULL,
  password_reset_expires_at TIMESTAMPTZ NULL,
  CONSTRAINT ck_app_user_password_reset_pair
    CHECK (
      (password_reset_token_hash IS NULL AND password_reset_expires_at IS NULL)
      OR
      (password_reset_token_hash IS NOT NULL AND password_reset_expires_at IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS audit_event (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NULL REFERENCES app_user(id) ON DELETE SET NULL,
  action TEXT NOT NULL,
  details TEXT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_audit_event_user_id_created_at
  ON audit_event (user_id, created_at DESC);

-- Case-insensitive uniqueness for email
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_email_lower
  ON app_user (lower(email));

-- Look up reset tokens quickly (and ensure no collisions)
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_password_reset_token_hash
  ON app_user (password_reset_token_hash)
  WHERE password_reset_token_hash IS NOT NULL;

CREATE TABLE IF NOT EXISTS registration (
  user_id UUID PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
  registration_type TEXT NOT NULL,
  first_name TEXT NULL,
  middle_name TEXT NULL,
  last_name TEXT NULL,
  country TEXT NULL,
  state TEXT NULL,
  mobile_number TEXT NULL,
  address_line_1 TEXT NULL,
  address_line_2 TEXT NULL,
  has_paid BOOLEAN NOT NULL DEFAULT false,
  paid_at TIMESTAMPTZ NULL,
  CONSTRAINT ck_registration_paid_at
    CHECK ((has_paid = false AND paid_at IS NULL) OR (has_paid = true AND paid_at IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS ix_registration_has_paid
  ON registration (has_paid);

-- Up to 3 emergency contacts per user (enforced by contact_index 1..3)
CREATE TABLE IF NOT EXISTS emergency_contact (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES registration(user_id) ON DELETE CASCADE,
  contact_index SMALLINT NOT NULL,
  mobile_number TEXT NOT NULL,
  email TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_emergency_contact_index CHECK (contact_index BETWEEN 1 AND 3),
  CONSTRAINT ux_emergency_contact_user_contact_index UNIQUE (user_id, contact_index)
);

CREATE INDEX IF NOT EXISTS ix_emergency_contact_user_id
  ON emergency_contact (user_id);

