ALTER TABLE "${table}" ADD COLUMN email TEXT;

CREATE UNIQUE INDEX "idx_${table}_email_lower" ON "${table}" (LOWER(email));
