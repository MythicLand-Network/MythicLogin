ALTER TABLE "${table}" ADD COLUMN email VARCHAR(320);

CREATE UNIQUE INDEX "idx_${table}_email_lower" ON "${table}" (LOWER(email)) WHERE email IS NOT NULL;
