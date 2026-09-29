ALTER TABLE users ADD COLUMN auth_version INTEGER NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS password_recovery_events (
  recovery_id TEXT PRIMARY KEY,
  used_at TEXT NOT NULL
);
