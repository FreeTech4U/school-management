package com.schoolsaas.config.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.get();
        log.info("[DIAG] resolveCurrentTenantIdentifier() appelé, retourne = {}", tenantId);
        return (tenantId != null) ? tenantId : "public";
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
