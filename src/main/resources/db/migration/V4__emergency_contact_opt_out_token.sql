-- Token for contact to opt out (included in alert emails)
ALTER TABLE emergency_contact ADD COLUMN IF NOT EXISTS opt_out_token UUID NULL;
UPDATE emergency_contact SET opt_out_token = gen_random_uuid() WHERE opt_out_token IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_emergency_contact_opt_out_token ON emergency_contact (opt_out_token) WHERE opt_out_token IS NOT NULL;
