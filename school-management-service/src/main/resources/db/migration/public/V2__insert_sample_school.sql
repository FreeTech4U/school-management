-- Script de peuplement initial pour créer une école active (Guinée)
-- Cela permet de déclencher la migration automatique du schéma tenant au démarrage

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
) VALUES (
    'Lycée Sainte Marie de Dixinn', 
    'ste-marie-dixinn', 
    'tenant_ste_marie', 
    'admin@stemarie.gn', 
    '+224620000000', 
    'Dixinn, Conakry', 
    'Conakry', 
    'GN', 
    'active', 
    'Africa/Conakry', 
    'GNF'
);

-- Associer un abonnement à l'école
INSERT INTO school_subscriptions (
    school_id, 
    plan_id, 
    start_date, 
    end_date, 
    status
) VALUES (
    (SELECT id FROM schools WHERE slug = 'ste-marie-dixinn'),
    (SELECT id FROM subscription_plans WHERE code = 'premium'),
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '1 year',
    'active'
);
