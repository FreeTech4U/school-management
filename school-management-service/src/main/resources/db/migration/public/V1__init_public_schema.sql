CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'GNF',
    billing_cycle VARCHAR(20) DEFAULT 'MONTHLY',
    max_students INTEGER,
    features JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE schools (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    schema_name VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    country_code VARCHAR(3) DEFAULT 'GN',
    logo_url VARCHAR(255),
    status VARCHAR(20) DEFAULT 'trial', -- trial, active, suspended, deleted
    timezone VARCHAR(50) DEFAULT 'Africa/Conakry',
    currency VARCHAR(3) DEFAULT 'GNF',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE school_subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'active', -- active, expired, cancelled
    auto_renew BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subscription_payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subscription_id UUID NOT NULL REFERENCES school_subscriptions(id),
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'GNF',
    payment_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(50),
    transaction_reference VARCHAR(100) UNIQUE,
    status VARCHAR(20) DEFAULT 'completed',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed data for plans
INSERT INTO subscription_plans (code, name, price, max_students) VALUES 
('starter', 'Starter Plan', 0.00, 100),
('standard', 'Standard Plan', 500000.00, 500),
('premium', 'Premium Plan', 1000000.00, 2000);
