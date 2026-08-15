CREATE TABLE personal_access_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    abilities JSONB NOT NULL DEFAULT '[]'::jsonb,
    last_used_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_personal_access_tokens_user_id ON personal_access_tokens(user_id);
CREATE INDEX idx_personal_access_tokens_expires_at ON personal_access_tokens(expires_at);
CREATE INDEX idx_personal_access_tokens_active_user ON personal_access_tokens(user_id)
    WHERE revoked_at IS NULL;

CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_hash VARCHAR(64) NOT NULL UNIQUE,
    ip_address VARCHAR(64),
    user_agent TEXT,
    last_used_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_sessions_user_id ON auth_sessions(user_id);
CREATE INDEX idx_auth_sessions_expires_at ON auth_sessions(expires_at);
CREATE INDEX idx_auth_sessions_active_user ON auth_sessions(user_id)
    WHERE revoked_at IS NULL;
