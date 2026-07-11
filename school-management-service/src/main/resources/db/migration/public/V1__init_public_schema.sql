-- ============================================================================
-- V1__init_public_schema.sql
-- Schema PUBLIC — Gestion de la plateforme SaaS
--
-- Contient : les écoles (tenants), les plans d'abonnement,
--            les abonnements actifs et leurs paiements.
--
-- Ce schema est unique et partagé. Les données métier de chaque école
-- vivent dans leur propre schema tenant (voir V1__init_tenant_schema.sql).
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── Fonction updated_at (schema public) ─────────────────────────────────────

CREATE OR REPLACE FUNCTION fn_update_updated_at()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ============================================================================
-- TABLES
-- ============================================================================

-- ── Plans d'abonnement ──────────────────────────────────────────────────────

CREATE TABLE subscription_plans (
    id            UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    code          VARCHAR(50) UNIQUE NOT NULL,
    name          VARCHAR(100)       NOT NULL,
    description   TEXT,
    price_monthly DECIMAL(19, 2)     NOT NULL,
    currency      VARCHAR(3)               DEFAULT 'GNF' NOT NULL,
    billing_cycle VARCHAR(20)              DEFAULT 'MONTHLY' NOT NULL,
    max_students  INTEGER,
    sms_included  INTEGER                  DEFAULT 100 NOT NULL,
    features      JSONB,
    is_active     BOOLEAN                  DEFAULT TRUE NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_plan_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT chk_plan_price         CHECK (price_monthly >= 0)
);

-- ── Écoles (= tenants) ──────────────────────────────────────────────────────

CREATE TABLE schools (
    id           UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    name         VARCHAR(255)        NOT NULL,
    slug         VARCHAR(255) UNIQUE NOT NULL,
    schema_name  VARCHAR(63) UNIQUE  NOT NULL,
    email        VARCHAR(255) UNIQUE NOT NULL,
    phone        VARCHAR(50),
    address      TEXT,
    city         VARCHAR(100),
    country_code VARCHAR(3)               DEFAULT 'GN' NOT NULL,
    logo_url     VARCHAR(255),
    status       VARCHAR(20)              DEFAULT 'trial' NOT NULL,
    timezone     VARCHAR(50)              DEFAULT 'Africa/Conakry' NOT NULL,
    currency     VARCHAR(3)               DEFAULT 'GNF' NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_school_status      CHECK (status IN ('trial', 'active', 'suspended', 'deleted')),
    CONSTRAINT chk_school_schema_name CHECK (schema_name ~ '^[a-z0-9_]+$')
);

-- ── Abonnements des écoles ──────────────────────────────────────────────────

CREATE TABLE school_subscriptions (
    id         UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    school_id  UUID NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    plan_id    UUID NOT NULL REFERENCES subscription_plans (id),
    start_date DATE NOT NULL,
    end_date   DATE NOT NULL,
    status     VARCHAR(20)              DEFAULT 'active' NOT NULL,
    auto_renew BOOLEAN                  DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_subscription_status CHECK (status IN ('active', 'expired', 'cancelled', 'suspended')),
    CONSTRAINT chk_subscription_dates  CHECK (end_date >= start_date)
);

-- ── Paiements des abonnements SaaS ──────────────────────────────────────────

CREATE TABLE subscription_payments (
    id                    UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    subscription_id       UUID NOT NULL REFERENCES school_subscriptions (id) ON DELETE CASCADE,
    amount                DECIMAL(19, 2)     NOT NULL,
    currency              VARCHAR(3)               DEFAULT 'GNF' NOT NULL,
    payment_date          DATE                     DEFAULT CURRENT_DATE NOT NULL,
    payment_method        VARCHAR(50)        NOT NULL,
    transaction_reference VARCHAR(100) UNIQUE,
    status                VARCHAR(20)              DEFAULT 'completed' NOT NULL,
    notes                 TEXT,
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_sub_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_sub_payment_status CHECK (status IN ('pending', 'completed', 'failed', 'refunded')),
    CONSTRAINT chk_sub_payment_method CHECK (payment_method IN
                                             ('cash', 'orange_money', 'mtn_money', 'wave', 'bank_transfer', 'card',
                                              'check'))
);


-- ============================================================================
-- INDEX
-- ============================================================================

CREATE INDEX idx_schools_status ON schools (status);
CREATE INDEX idx_schools_slug ON schools (slug);
CREATE INDEX idx_school_subscriptions_school ON school_subscriptions (school_id);
CREATE INDEX idx_school_subscriptions_status ON school_subscriptions (status);
CREATE INDEX idx_school_subscriptions_end_date ON school_subscriptions (end_date);
CREATE INDEX idx_subscription_payments_sub ON subscription_payments (subscription_id);
CREATE INDEX idx_subscription_payments_date ON subscription_payments (payment_date);


-- ============================================================================
-- TRIGGERS updated_at
-- (CORRECTION : ces triggers étaient absents — updated_at n'était jamais mis à jour)
-- ============================================================================

CREATE TRIGGER trg_updated_at_subscription_plans
    BEFORE UPDATE
    ON subscription_plans
    FOR EACH ROW
EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_schools
    BEFORE UPDATE
    ON schools
    FOR EACH ROW
EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_school_subscriptions
    BEFORE UPDATE
    ON school_subscriptions
    FOR EACH ROW
EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_subscription_payments
    BEFORE UPDATE
    ON subscription_payments
    FOR EACH ROW
EXECUTE FUNCTION fn_update_updated_at();


-- ============================================================================
-- DONNÉES INITIALES — Plans tarifaires (GNF)
-- ============================================================================

INSERT INTO subscription_plans (code, name, description, price_monthly, max_students, sms_included)
VALUES ('starter', 'Starter', 'Pour les petites écoles jusqu''à 200 élèves', 45000.00, 200, 200),
       ('standard', 'Standard', 'Pour les écoles de 200 à 600 élèves', 120000.00, 600, 600),
       ('premium', 'Premium', 'Pour les grandes écoles et groupes scolaires', 280000.00, NULL, 2000);