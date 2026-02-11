CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    client_id VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by VARCHAR(36)
);

ALTER TABLE refresh_tokens DROP COLUMN IF EXISTS subject;

DO $$ BEGIN
    ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS user_id UUID;
EXCEPTION
    WHEN duplicate_column THEN null;
END $$;

DO $$ BEGIN
    DELETE FROM refresh_tokens rt
    WHERE rt.user_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM users u WHERE u.id = rt.user_id
    );
END $$;

DO $$ DECLARE
    any_user_id UUID;
BEGIN
    SELECT id INTO any_user_id FROM users LIMIT 1;

    IF any_user_id IS NOT NULL THEN
        UPDATE refresh_tokens SET user_id = any_user_id WHERE user_id IS NULL;
    ELSE
        DELETE FROM refresh_tokens WHERE user_id IS NULL;
    END IF;
END $$;

DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'refresh_tokens' AND column_name = 'user_id' AND is_nullable = 'YES') THEN
        ALTER TABLE refresh_tokens ALTER COLUMN user_id SET NOT NULL;
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_refresh_tokens_user' AND conrelid = 'refresh_tokens'::regclass) THEN
        ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- ============================================================================
-- COLONNES D'AUDIT
-- ============================================================================

DO $$ BEGIN
    ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'created_at' AND is_nullable = 'YES') THEN
        UPDATE users SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
        ALTER TABLE users ALTER COLUMN created_at SET NOT NULL;
    END IF;
EXCEPTION
    WHEN duplicate_column THEN null;
END $$;

DO $$ BEGIN
    ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'updated_at' AND is_nullable = 'YES') THEN
        UPDATE users SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;
        ALTER TABLE users ALTER COLUMN updated_at SET NOT NULL;
    END IF;
EXCEPTION
    WHEN duplicate_column THEN null;
END $$;

ALTER TABLE users ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

-- ============================================================================
-- COLONNES DE SECURITE
-- ============================================================================

DO $$ BEGIN
    ALTER TABLE users ADD COLUMN IF NOT EXISTS locked BOOLEAN;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'locked' AND is_nullable = 'YES') THEN
        UPDATE users SET locked = FALSE WHERE locked IS NULL;
        ALTER TABLE users ALTER COLUMN locked SET NOT NULL;
    END IF;
EXCEPTION
    WHEN duplicate_column THEN null;
END $$;

DO $$ BEGIN
    ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'failed_login_attempts' AND is_nullable = 'YES') THEN
        UPDATE users SET failed_login_attempts = 0 WHERE failed_login_attempts IS NULL;
        ALTER TABLE users ALTER COLUMN failed_login_attempts SET NOT NULL;
    END IF;
EXCEPTION
    WHEN duplicate_column THEN null;
END $$;

ALTER TABLE users ADD COLUMN IF NOT EXISTS last_failed_login TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_users_locked ON users(locked);
CREATE INDEX IF NOT EXISTS idx_users_locked_until ON users(locked_until);

-- ============================================================================
-- TABLE ROLES
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_roles (
    id VARCHAR(36) PRIMARY KEY,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_role UNIQUE (user_id, role)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role);

-- ============================================================================
-- MIGRATION DES DONNEES
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'users' AND column_name = 'roles') THEN

        INSERT INTO user_roles (id, user_id, role)
        SELECT
            gen_random_uuid()::text,
            u.id,
            CASE
                WHEN trim(role_part) = 'USER' THEN 'USER'
                WHEN trim(role_part) = 'ADMIN' THEN 'ADMIN'
                WHEN trim(role_part) = 'MODERATOR' THEN 'MODERATOR'
                WHEN trim(role_part) = 'SUPPORT' THEN 'SUPPORT'
                ELSE 'USER'
            END
        FROM users u
        CROSS JOIN unnest(string_to_array(COALESCE(u.roles, 'USER'), ',')) AS role_part
        WHERE trim(role_part) != '';

        ALTER TABLE users DROP COLUMN roles;
    END IF;
END $$;

INSERT INTO user_roles (id, user_id, role)
SELECT gen_random_uuid()::text, u.id, 'USER'
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);