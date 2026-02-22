-- Super user flag: when true, user can access /api/admin/* endpoints for audit purposes
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS super_user BOOLEAN NOT NULL DEFAULT false;
