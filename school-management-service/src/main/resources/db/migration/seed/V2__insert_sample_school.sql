-- =============================================================================
-- V2__insert_sample_school.sql
-- Donnée de démonstration : une école d'exemple
--
-- Rôle : déclencher la création automatique du schema tenant au démarrage.
--        TenantInitializer parcourt les écoles TRIAL/ACTIVE et applique
--        V1__init_tenant_schema.sql sur chacune.
-- =============================================================================

-- ── L'école ──────────────────────────────────────────────────────────────────
-- ON CONFLICT DO NOTHING rend le script idempotent : si Flyway rejoue cette
-- migration (par exemple après un repair()), l'INSERT est ignoré au lieu de
-- violer la contrainte UNIQUE sur slug.

INSERT INTO schools (
    name,
    slug,
    schema_name,
    email,
    phone,
    address,
    city,
    country_code,
    status,
    timezone,
    currency
)
VALUES (
    'Lycée Sainte Marie de Dixinn',
    'ste-marie-dixinn', -- respecte ^[a-z0-9-]+$
    'tenant_ste_marie', -- respecte ^[a-z0-9_]+$ (63 car. max)
    'admin@stemarie.gn',
    '+224620000000',
    'Dixinn, Conakry',
    'Conakry',
    'GN',
    'ACTIVE', -- CORRECTION : était 'active' (minuscules)
    -- → violait CHECK (status IN ('TRIAL','ACTIVE',...))
    'Africa/Conakry', -- pilote les schedulers : les SMS de relance
    -- partiront à 8h heure de Conakry
    'GNF'
)
ON CONFLICT (slug) DO NOTHING;

-- =============================================================================
-- 7. DONNÉES INITIALES — Catalogue tarifaire (GNF)
--
-- Référence de change : 1 EUR ≈ 9 500 GNF
--
--   Starter  :  45 000 GNF/mois  (≈  4,7 €)  →   450 000 GNF/an  (2 mois offerts)
--   Standard : 120 000 GNF/mois  (≈ 12,6 €)  → 1 200 000 GNF/an  (2 mois offerts)
--   Premium  : 280 000 GNF/mois  (≈ 29,5 €)  → 2 800 000 GNF/an  (2 mois offerts)
--
-- La remise annuelle (2 mois offerts) est l'incitation à l'engagement : elle
-- sécurise la trésorerie et réduit mécaniquement le taux de résiliation.
-- =============================================================================

INSERT INTO subscription_plans (
    code,
    name,
    description,
    price_monthly,
    price_yearly,
    max_students,
    sms_included,
    features
)
VALUES
    (
        'starter',
        'Starter',
        'Pour les petites écoles jusqu''à 200 élèves',
        45000.00,
        450000.00, -- 10 mois payés au lieu de 12
        200,
        200,
        '{"timetable": false, "advanced_reports": false, "parent_app": false}'::jsonb
    ),
    (
        'standard',
        'Standard',
        'Pour les écoles de 200 à 600 élèves',
        120000.00,
        1200000.00,
        600,
        600,
        '{"timetable": true, "advanced_reports": true, "parent_app": false}'::jsonb
    ),
    (
        'premium',
        'Premium',
        'Pour les grandes écoles et groupes scolaires — élèves illimités',
        280000.00,
        2800000.00,
        NULL, -- illimité
        2000,
        '{"timetable": true, "advanced_reports": true, "parent_app": true}'::jsonb
    );

-- ── L'abonnement ─────────────────────────────────────────────────────────────
-- L'index UNIQUE partiel uq_school_active_subscription interdit deux
-- abonnements ACTIVE pour la même école : le ON CONFLICT le prend en compte.

INSERT INTO school_subscriptions (
    school_id,
    plan_id,
    billing_cycle,
    start_date,
    end_date,
    status,
    auto_renew
)
SELECT
    s.id,
    p.id,
    'YEARLY', -- CORRECTION : colonne absente du
    -- script d'origine (NOT NULL)
    CURRENT_DATE,
    (CURRENT_DATE + INTERVAL '1 year')::DATE, -- CORRECTION : l'expression
    -- produisait un TIMESTAMP, la
    -- colonne attend une DATE
    'ACTIVE', -- CORRECTION : était 'active'
    TRUE
FROM schools s
    CROSS JOIN subscription_plans p
WHERE s.slug = 'ste-marie-dixinn'
    AND p.code = 'premium'
    -- Ne rien insérer si l'école a déjà un abonnement actif.
    AND NOT EXISTS (
        SELECT 1
        FROM school_subscriptions ss
        WHERE ss.school_id = s.id
            AND ss.status = 'ACTIVE'
    );

-- ── Rôles ────────────────────────────────────────────────────────────────────
-- Reprend exactement les 4 valeurs de l'ancien enum Java Role, désormais
-- supprimé. D'autres rôles pourront être ajoutés plus tard via l'API
-- d'administration, sans toucher à cette migration ni à aucun schema tenant.
INSERT INTO roles (code, label)
VALUES
    ('DIRECTOR', 'Directeur'),
    ('TEACHER', 'Enseignant'),
    ('ACCOUNTANT', 'Comptable'),
    ('PARENT', 'Parent');
