-- =============================================================
-- E-Commerce Auth Service - Database Schema (MySQL)
-- =============================================================

CREATE DATABASE IF NOT EXISTS ecommerce_auth
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_auth;

-- ---------------------------------------------------------
-- Users
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    mobile      VARCHAR(10)  NOT NULL,
    password    VARCHAR(100) NOT NULL,           -- BCrypt hash
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email  (email),
    UNIQUE KEY uk_users_mobile (mobile)
);

-- ---------------------------------------------------------
-- Sessions  (active JWT sessions, enable logout / invalidation)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS sessions (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(512) NOT NULL,
    login_time  DATETIME     NOT NULL,
    expiry_time DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sessions_token (token),
    KEY idx_sessions_user (user_id),
    KEY idx_sessions_expiry (expiry_time),
    CONSTRAINT fk_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
);

-- ---------------------------------------------------------
-- Otps  (password-recovery one-time passwords)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS otps (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    identifier  VARCHAR(150) NOT NULL,           -- email of the account
    code        VARCHAR(6)   NOT NULL,
    expiry_time DATETIME     NOT NULL,
    verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_otps_identifier (identifier),
    KEY idx_otps_expiry (expiry_time)
);