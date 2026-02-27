-- ============================================================================
-- 1. USERS TABLE: AUDIT & SECURITY COLUMNS
-- ============================================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_failed_login TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;

-- Force NOT NULL for audit columns (safe if we provided defaults above)
UPDATE users SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE users SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;
UPDATE users SET locked = FALSE WHERE locked IS NULL;
UPDATE users SET failed_login_attempts = 0 WHERE failed_login_attempts IS NULL;

-- ============================================================================
-- 2. REFRESH TOKENS TABLE: CLEANUP & STRUCTURE
-- ============================================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id UUID NOT NULL,
    client_id VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by VARCHAR(36),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- CLEANUP: Remove legacy column if it exists (for existing envs)
ALTER TABLE refresh_tokens DROP COLUMN IF EXISTS subject;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- ============================================================================
-- 3. USER ROLES TABLE & DATA MIGRATION
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_roles (
    id VARCHAR(36) PRIMARY KEY,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_role UNIQUE (user_id, role)
);

-- Portable Data Migration: USER role
INSERT INTO user_roles (id, user_id, role)
SELECT CONCAT(CAST(id AS VARCHAR(36)), '-U'), id, 'USER'
FROM users
WHERE roles LIKE '%USER%'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = users.id AND ur.role = 'USER');

-- Portable Data Migration: ADMIN role
INSERT INTO user_roles (id, user_id, role)
SELECT CONCAT(CAST(id AS VARCHAR(36)), '-A'), id, 'ADMIN'
FROM users
WHERE roles LIKE '%ADMIN%'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = users.id AND ur.role = 'ADMIN');

-- Fallback: Ensure every user has at least USER role if they had no known roles
INSERT INTO user_roles (id, user_id, role)
SELECT CONCAT(CAST(id AS VARCHAR(36)), '-F'), id, 'USER'
FROM users
WHERE NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = users.id);

-- ============================================================================
-- 4. INDEXES
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_locked ON users(locked);
CREATE INDEX IF NOT EXISTS idx_users_locked_until ON users(locked_until);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role);