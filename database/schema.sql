-- =============================================================
-- GymPabon - database schema (MySQL 8)
-- Run this file first, then seed.sql.
-- =============================================================

DROP DATABASE IF EXISTS gympabon;
CREATE DATABASE gympabon CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci;
USE gympabon;

-- -------------------------------------------------------------
-- Plans: one row per plan type. The behaviour of each plan
-- (access hours, enrollment fee, trainer) lives in the Java
-- subclasses BasicPlan, StandardPlan and PremiumPlan.
-- -------------------------------------------------------------
CREATE TABLE plan (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    plan_type      ENUM('BASIC', 'STANDARD', 'PREMIUM') NOT NULL UNIQUE,
    name           VARCHAR(40)   NOT NULL,
    monthly_price  DECIMAL(10,2) NOT NULL CHECK (monthly_price > 0),
    description    VARCHAR(255)  NOT NULL
);

-- -------------------------------------------------------------
-- People: Client, Trainer and User inherit from the abstract
-- class Person, so each table repeats the Person columns
-- (one table per concrete class).
-- -------------------------------------------------------------
CREATE TABLE client (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    document_number    VARCHAR(20)  NOT NULL UNIQUE,
    first_name         VARCHAR(60)  NOT NULL,
    last_name          VARCHAR(60)  NOT NULL,
    phone              VARCHAR(20),
    email              VARCHAR(100),
    birth_date         DATE,
    registration_date  DATE         NOT NULL,
    active             BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE trainer (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    document_number  VARCHAR(20)  NOT NULL UNIQUE,
    first_name       VARCHAR(60)  NOT NULL,
    last_name        VARCHAR(60)  NOT NULL,
    phone            VARCHAR(20),
    email            VARCHAR(100),
    specialty        VARCHAR(60)  NOT NULL,
    work_schedule    VARCHAR(60)  NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE
);

-- "user" is a reserved word in MySQL, so the table is app_user.
CREATE TABLE app_user (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    document_number  VARCHAR(20)  NOT NULL UNIQUE,
    first_name       VARCHAR(60)  NOT NULL,
    last_name        VARCHAR(60)  NOT NULL,
    phone            VARCHAR(20),
    email            VARCHAR(100),
    username         VARCHAR(30)  NOT NULL UNIQUE,
    password_hash    VARCHAR(200) NOT NULL,
    role             ENUM('ADMIN', 'RECEPTIONIST') NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login       DATETIME
);

-- -------------------------------------------------------------
-- Memberships: a client buys a plan for a duration.
-- trainer_id is only filled for Premium memberships.
-- -------------------------------------------------------------
CREATE TABLE membership (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    client_id            INT           NOT NULL,
    plan_id              INT           NOT NULL,
    trainer_id           INT           NULL,
    duration             ENUM('ONE_MONTH', 'THREE_MONTHS', 'SIX_MONTHS', 'TWELVE_MONTHS') NOT NULL,
    start_date           DATE          NOT NULL,
    end_date             DATE          NOT NULL,
    enrollment_fee       DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_amount         DECIMAL(10,2) NOT NULL,
    status               ENUM('PENDING_PAYMENT', 'SCHEDULED', 'ACTIVE', 'EXPIRED', 'CANCELLED') NOT NULL,
    cancellation_reason  VARCHAR(255),
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_membership_client  FOREIGN KEY (client_id)  REFERENCES client(id),
    CONSTRAINT fk_membership_plan    FOREIGN KEY (plan_id)    REFERENCES plan(id),
    CONSTRAINT fk_membership_trainer FOREIGN KEY (trainer_id) REFERENCES trainer(id),
    CONSTRAINT chk_membership_dates  CHECK (end_date >= start_date)
);

CREATE INDEX idx_membership_client_status ON membership (client_id, status);
CREATE INDEX idx_membership_trainer_status ON membership (trainer_id, status);

-- -------------------------------------------------------------
-- Payments: one full payment per membership. Payments are never
-- deleted; an administrator can void one with a reason.
-- -------------------------------------------------------------
CREATE TABLE payment (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    membership_id  INT           NOT NULL,
    amount         DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    method         ENUM('CASH', 'TRANSFER', 'CARD') NOT NULL,
    payment_date   DATE          NOT NULL,
    registered_by  INT           NOT NULL,
    voided         BOOLEAN       NOT NULL DEFAULT FALSE,
    void_reason    VARCHAR(255),
    CONSTRAINT fk_payment_membership FOREIGN KEY (membership_id) REFERENCES membership(id),
    CONSTRAINT fk_payment_user       FOREIGN KEY (registered_by) REFERENCES app_user(id)
);

CREATE INDEX idx_payment_date ON payment (payment_date);

-- -------------------------------------------------------------
-- Attendance: every entry attempt, allowed or rejected.
-- -------------------------------------------------------------
CREATE TABLE attendance (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    client_id         INT          NOT NULL,
    date_time         DATETIME     NOT NULL,
    allowed           BOOLEAN      NOT NULL,
    rejection_reason  VARCHAR(255),
    registered_by     INT          NOT NULL,
    CONSTRAINT fk_attendance_client FOREIGN KEY (client_id)     REFERENCES client(id),
    CONSTRAINT fk_attendance_user   FOREIGN KEY (registered_by) REFERENCES app_user(id)
);

CREATE INDEX idx_attendance_date ON attendance (date_time);
