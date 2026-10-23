CREATE TABLE identity.app_user (
    id                  BIGSERIAL PRIMARY KEY,
    phone               VARCHAR(13)  NOT NULL UNIQUE,
    status              VARCHAR(20)  NOT NULL,
    pin_hash            VARCHAR(200) NOT NULL,
    failed_pin_attempts INTEGER      NOT NULL DEFAULT 0,
    pin_locked_until    TIMESTAMPTZ,
    pin_lock_count      INTEGER      NOT NULL DEFAULT 0,
    created_date        TIMESTAMPTZ  NOT NULL,
    updated_date        TIMESTAMPTZ,
    version             BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE identity.device (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT       NOT NULL REFERENCES identity.app_user (id),
    public_key           TEXT         NOT NULL,
    biometric_public_key TEXT,
    name                 VARCHAR(100) NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    bound_at             TIMESTAMPTZ  NOT NULL,
    revoked_at           TIMESTAMPTZ,
    created_date         TIMESTAMPTZ  NOT NULL,
    updated_date         TIMESTAMPTZ,
    version              BIGINT       NOT NULL DEFAULT 0
);

-- Tek aktif cihaz kuralı veritabanında da korunur
CREATE UNIQUE INDEX ux_device_one_active_per_user ON identity.device (user_id) WHERE status = 'ACTIVE';

CREATE TABLE identity.refresh_token (
    id            BIGSERIAL PRIMARY KEY,
    family_id     UUID        NOT NULL,
    token_hash    VARCHAR(64) NOT NULL UNIQUE,
    user_id       BIGINT      NOT NULL REFERENCES identity.app_user (id),
    device_id     BIGINT      NOT NULL REFERENCES identity.device (id),
    parent_id     BIGINT REFERENCES identity.refresh_token (id),
    expires_at    TIMESTAMPTZ NOT NULL,
    used_at       TIMESTAMPTZ,
    revoked_at    TIMESTAMPTZ,
    revoke_reason VARCHAR(30),
    created_date  TIMESTAMPTZ NOT NULL,
    updated_date  TIMESTAMPTZ,
    version       BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX ix_refresh_token_family ON identity.refresh_token (family_id);
CREATE INDEX ix_refresh_token_device ON identity.refresh_token (device_id) WHERE revoked_at IS NULL;

CREATE TABLE identity.otp_challenge (
    id                      BIGSERIAL PRIMARY KEY,
    challenge_id            UUID        NOT NULL UNIQUE,
    phone                   VARCHAR(13) NOT NULL,
    ip                      VARCHAR(45),
    code_hash               VARCHAR(64) NOT NULL,
    attempts                INTEGER     NOT NULL DEFAULT 0,
    status                  VARCHAR(20) NOT NULL,
    expires_at              TIMESTAMPTZ NOT NULL,
    verified_at             TIMESTAMPTZ,
    verification_hash       VARCHAR(64) UNIQUE,
    verification_expires_at TIMESTAMPTZ,
    consumed_at             TIMESTAMPTZ,
    created_date            TIMESTAMPTZ NOT NULL,
    updated_date            TIMESTAMPTZ,
    version                 BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX ix_otp_challenge_phone_created ON identity.otp_challenge (phone, created_date);
CREATE INDEX ix_otp_challenge_ip_created ON identity.otp_challenge (ip, created_date);

CREATE TABLE identity.auth_event (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    device_id   BIGINT,
    phone       VARCHAR(13),
    type        VARCHAR(40)  NOT NULL,
    detail      VARCHAR(300),
    ip          VARCHAR(45),
    occurred_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_auth_event_user ON identity.auth_event (user_id, id);

CREATE FUNCTION identity.prevent_auth_event_change() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'auth_event kayıtları değiştirilemez';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auth_event_immutable
    BEFORE UPDATE OR DELETE ON identity.auth_event
    FOR EACH ROW EXECUTE FUNCTION identity.prevent_auth_event_change();
