package com.schoolsaas.communication.service;

import com.schoolsaas.finance.repository.StudentFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bascule les frais échus en statut OVERDUE, pour le tenant COURANT
 * (celui positionné dans TenantContext par l'appelant).
 *
 * Cette méthode doit TOUJOURS être appelée depuis un bean différent
 * (SmsScheduler), jamais depuis une méthode du même bean : @Transactional ne
 * s'applique que sur les appels externes, qui passent par le proxy Spring.
 * Un appel interne (this.markOverdueFeesForCurrentTenant()) court-circuiterait
 * le proxy et l'annotation serait silencieusement ignorée.
 *
 * L'ORDRE ENTRE LE TenantContext.set() DE L'APPELANT ET L'OUVERTURE DE LA
 * TRANSACTION ICI EST CE QUI FAIT FONCTIONNER LE MULTITENANT : Hibernate
 * n'interroge le TenantIdentifierResolver qu'à l'entrée dans la transaction —
 * c'est-à-dire à l'entrée dans CETTE méthode, une fois que l'appelant a déjà
 * positionné le bon schema.
 */
@Service
@RequiredArgsConstructor
public class OverdueFeeService {

    private final StudentFeeRepository studentFeeRepository;

    @Transactional
    public int markOverdueFeesForCurrentTenant() {
        return studentFeeRepository.markOverdueFees();
    }
}
