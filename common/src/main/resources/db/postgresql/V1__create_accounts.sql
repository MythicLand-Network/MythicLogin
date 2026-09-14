CREATE TABLE IF NOT EXISTS "${table}" (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(191) NOT NULL UNIQUE,
    realname VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    ip VARCHAR(40),
    lastlogin BIGINT NOT NULL DEFAULT 0,
    regdate BIGINT NOT NULL DEFAULT 0,
    regip VARCHAR(40),
    is_logged BOOLEAN NOT NULL DEFAULT FALSE,
    has_session BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS "idx_${table}_realname_lower" ON "${table}" (LOWER(realname));
