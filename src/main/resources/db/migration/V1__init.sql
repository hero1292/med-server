-- ===========================
-- USERS
-- ===========================
CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255)        NOT NULL,
    full_name     VARCHAR(255)        NOT NULL,
    role          VARCHAR(32)         NOT NULL, -- PATIENT|DOCTOR|ADMIN
    created_at    TIMESTAMPTZ DEFAULT now(),
    updated_at    TIMESTAMPTZ DEFAULT now()
);

-- ===========================
-- DOCTOR PROFILES
-- ===========================
CREATE TABLE doctor_profiles
(
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    specialty VARCHAR(128),
    phone     VARCHAR(64)
);

-- ===========================
-- PATIENT PROFILES
-- ===========================
CREATE TABLE patient_profiles
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    birth_date DATE,
    phone      VARCHAR(64),
    address    VARCHAR(255),
    doctor_id  BIGINT REFERENCES doctor_profiles (id)
);

-- ===========================
-- RECOMMENDATIONS
-- ===========================
CREATE TABLE recommendations
(
    id                BIGSERIAL PRIMARY KEY,
    doctor_id         BIGINT       NOT NULL REFERENCES doctor_profiles (id) ON DELETE CASCADE,
    patient_id        BIGINT       NOT NULL REFERENCES patient_profiles (id) ON DELETE CASCADE,
    title             VARCHAR(255) NOT NULL,
    dose              VARCHAR(128) NOT NULL,
    frequency_per_day INT,
    interval_hours    INT,
    duration_days     INT          NOT NULL,
    start_at          TIMESTAMPTZ  NOT NULL,
    notes             TEXT,
    created_at        TIMESTAMPTZ DEFAULT now(),
    updated_at        TIMESTAMPTZ DEFAULT now()
);

-- ===========================
-- SCHEDULE ITEMS
-- ===========================
CREATE TABLE schedule_items
(
    id                BIGSERIAL PRIMARY KEY,
    recommendation_id BIGINT      NOT NULL REFERENCES recommendations (id) ON DELETE CASCADE,
    patient_id        BIGINT      NOT NULL REFERENCES patient_profiles (id) ON DELETE CASCADE,
    planned_at        TIMESTAMPTZ NOT NULL,
    taken             BOOLEAN DEFAULT FALSE,
    taken_at          TIMESTAMPTZ,
    note              VARCHAR(255)
);

CREATE INDEX idx_schedule_patient_time ON schedule_items (patient_id, planned_at);

-- ===========================
-- TOKENS (для refresh и logout)
-- ===========================
CREATE TABLE tokens
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    refresh_token VARCHAR(512) NOT NULL UNIQUE,
    expires_at    TIMESTAMPTZ  NOT NULL,
    revoked       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_tokens_user_id ON tokens (user_id);
CREATE INDEX idx_tokens_expires_at ON tokens (expires_at);
