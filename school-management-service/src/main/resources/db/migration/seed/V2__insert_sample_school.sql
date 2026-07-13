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
           'ste-marie-dixinn',          -- respecte ^[a-z0-9-]+$
           'tenant_ste_marie',          -- respecte ^[a-z0-9_]+$ (63 car. max)
           'admin@stemarie.gn',
           '+224620000000',
           'Dixinn, Conakry',
           'Conakry',
           'GN',
           'ACTIVE',                    -- CORRECTION : était 'active' (minuscules)
           -- → violait CHECK (status IN ('TRIAL','ACTIVE',...))
           'Africa/Conakry',            -- pilote les schedulers : les SMS de relance
           -- partiront à 8h heure de Conakry
           'GNF'
       )
    ON CONFLICT (slug) DO NOTHING;


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
    'YEARLY',                              -- CORRECTION : colonne absente du
    -- script d'origine (NOT NULL)
    CURRENT_DATE,
    (CURRENT_DATE + INTERVAL '1 year')::DATE,  -- CORRECTION : l'expression
                                               -- produisait un TIMESTAMP, la
                                               -- colonne attend une DATE
    'ACTIVE',                              -- CORRECTION : était 'active'
    TRUE
FROM       schools            s
               CROSS JOIN subscription_plans p
WHERE  s.slug = 'ste-marie-dixinn'
  AND  p.code = 'premium'
  -- Ne rien insérer si l'école a déjà un abonnement actif.
  AND NOT EXISTS (
    SELECT 1
    FROM   school_subscriptions ss
    WHERE  ss.school_id = s.id
      AND  ss.status    = 'ACTIVE'
);
