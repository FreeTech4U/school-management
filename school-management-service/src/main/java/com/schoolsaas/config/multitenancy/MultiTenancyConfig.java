package com.schoolsaas.config.multitenancy;

import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Active réellement le multitenant Hibernate.
 *
 * CORRECTION D'UN BUG DE PRODUCTION — déclarer SchemaMultiTenantConnectionProvider
 * et TenantIdentifierResolver en @Component ne suffit PAS à les faire utiliser
 * par Hibernate 6. Rien ne les reliait à la session : Hibernate tournait en
 * mode mono-tenant classique, connecté au search_path par défaut ('public'),
 * sans jamais appeler resolveCurrentTenantIdentifier() ni getConnection(String).
 *
 * Les requêtes sur des entités schema-qualifiées (School, Person,
 * SchoolMembership — @Table(schema = "public")) fonctionnaient par coïncidence,
 * indépendamment de toute résolution de tenant. User, non qualifiée, dépendait
 * du search_path resté sur 'public' → "relation users does not exist".
 *
 * Ce HibernatePropertiesCustomizer transmet les INSTANCES SPRING (déjà
 * correctement injectées avec leur DataSource) aux propriétés Hibernate —
 * à ne pas confondre avec le fait de mettre un NOM DE CLASSE en chaîne dans
 * le YAML, qui pousserait Hibernate à instancier lui-même la classe, hors du
 * contexte Spring, avec un DataSource null.
 */
@Configuration
@RequiredArgsConstructor
public class MultiTenancyConfig {

    private final MultiTenantConnectionProvider<String>   multiTenantConnectionProvider;
    private final CurrentTenantIdentifierResolver<String> currentTenantIdentifierResolver;

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            hibernateProperties.put(
                    MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER,
                    multiTenantConnectionProvider);
            hibernateProperties.put(
                    MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                    currentTenantIdentifierResolver);
        };
    }
}
