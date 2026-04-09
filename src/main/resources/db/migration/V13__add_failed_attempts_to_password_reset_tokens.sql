ALTER TABLE password_reset_tokens ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;
