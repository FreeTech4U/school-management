package com.schoolsaas.communication.service;

import com.schoolsaas.finance.entity.StudentFee;
import com.schoolsaas.finance.repository.StudentFeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeReminderService {

    private final StudentFeeRepository studentFeeRepository;
    private final SmsService           smsService;

    /**
     * Traite les rappels du tenant courant (positionné par le scheduler).
     *
     * @Transactional fonctionne ici car l'appel vient du scheduler
     * (un bean différent) et passe donc par le proxy Spring.
     */
    @Transactional
    public int processSchoolReminders() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);

        // Le filtre "7 jours" est dans la requête, pas en Java
        List<StudentFee> fees = studentFeeRepository.findFeesNeedingReminder(cutoff);

        int sent = 0;
        for (StudentFee fee : fees) {
            try {
                //smsService.sendTemplatedSms(fee);
                fee.setLastReminderSentAt(Instant.now());
                studentFeeRepository.save(fee);
                sent++;

            } catch (Exception e) {
                // Un numéro invalide ne doit pas bloquer les autres parents
                log.warn("SMS échoué pour le frais {} : {}", fee.getId(), e.getMessage());
            }
        }
        return sent;
    }
}
