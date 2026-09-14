CREATE TABLE IF NOT EXISTS "${table}" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    realname TEXT NOT NULL,
    password TEXT NOT NULL,
    ip TEXT,
    lastlogin INTEGER NOT NULL DEFAULT 0,
    regdate INTEGER NOT NULL DEFAULT 0,
    regip TEXT,
    is_logged INTEGER NOT NULL DEFAULT 0,
    has_session INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS "idx_${table}_realname_lower" ON "${table}" (LOWER(realname));
