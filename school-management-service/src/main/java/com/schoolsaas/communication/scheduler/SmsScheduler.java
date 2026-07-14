package com.schoolsaas.communication.scheduler;

import com.schoolsaas.common.enums.SchoolStatus;
import com.schoolsaas.communication.service.OverdueFeeService;
import com.schoolsaas.config.multitenancy.TenantContext;
import com.schoolsaas.communication.service.FeeReminderService;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsScheduler {

    private final SchoolRepository     schoolRepository;
    private final FeeReminderService   feeReminderService;
    private final OverdueFeeService overdueFeeService;

    @Scheduled(
            cron = "${app.scheduler.fee-reminder.cron}",
            zone = "Africa/Conakry" // À revoir : voir note en bas de fichier
    )
    public void sendFeeReminders() {
        List<School> schools = schoolRepository.findAllByStatusIn(
                List.of(SchoolStatus.TRIAL, SchoolStatus.ACTIVE));
        log.info("Rappels de frais — {} école(s)", schools.size());

        for (School school : schools) {
            try {
                TenantContext.set(school.getSchemaName());

                // Appel EXTERNE → passe par le proxy Spring → @Transactional actif
                int sent = feeReminderService.processSchoolReminders();

                log.info("{} rappel(s) envoyé(s) pour {}", sent, school.getName());

            } catch (Exception e) {
                log.error("Échec des rappels pour {} (schema={}): {}",
                        school.getName(), school.getSchemaName(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    /**
     * CORRECTION — @Transactional retiré de CETTE méthode.
     *
     * Avant : @Transactional posé directement sur la boucle. Hibernate ouvre
     * la connexion JDBC (et interroge le TenantIdentifierResolver) à l'entrée
     * dans la transaction, donc AVANT la première itération — avant même que
     * TenantContext.set() n'ait été appelé pour la première école. Résultat :
     * la connexion se fixait sur le schema 'public' pour TOUTE la méthode,
     * quel que soit le nombre d'appels à TenantContext.set() ensuite.
     *
     * Conséquence observée en logs :
     *   1. tenant_ste_marie : "relation student_fees does not exist"
     *      (la requête cherchait dans public, pas dans le schema de l'école)
     *   2. school_hadjamballou : "current transaction is aborted"
     *      (PostgreSQL rejette toute commande une fois qu'une transaction a
     *      échoué — même une commande destinée à un schema différent)
     *   3. UnexpectedRollbackException au niveau Spring
     *      (la transaction unique, marquée rollback-only par l'erreur #1,
     *      ne peut plus être committée)
     *
     * Après : cette méthode n'ouvre plus aucune transaction. Chaque itération
     * positionne TenantContext PUIS appelle overdueFeeService, un bean
     * DIFFÉRENT — l'appel passe donc par le proxy Spring, qui ouvre une
     * transaction NEUVE (et donc une connexion neuve, dont le search_path est
     * résolu à cet instant précis, une fois TenantContext déjà positionné).
     *
     * Une école en échec n'affecte plus les suivantes : chaque itération a sa
     * propre transaction, isolée par le try/catch.
     */
    @Scheduled(cron = "${app.scheduler.overdue-fees.cron}", zone = "Africa/Conakry")
    public void markOverdueFees() {
        List<School> schools = schoolRepository.findAllByStatusIn(
                List.of(SchoolStatus.TRIAL, SchoolStatus.ACTIVE));
        log.info("Bascule des frais en retard — {} école(s)", schools.size());

        for (School school : schools) {
            try {
                TenantContext.set(school.getSchemaName());

                // Appel EXTERNE → passe par le proxy Spring → transaction
                // neuve, ouverte APRÈS le TenantContext.set() ci-dessus.
                int updated = overdueFeeService.markOverdueFeesForCurrentTenant();

                log.info("{} frais passés en OVERDUE pour {}", updated, school.getName());

            } catch (Exception e) {
                log.error("Échec markOverdueFees pour {} (schema={}): {}",
                        school.getName(), school.getSchemaName(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    // NOTE — zone = "Africa/Conakry" en dur :
    // Cette valeur ne convient que tant que toutes les écoles sont en Guinée.
    // Pour une plateforme multi-pays, il faudrait remplacer ce cron unique par
    // un tick horaire en UTC ("0 0 * * * *", zone = "UTC") qui filtre, à chaque
    // passage, les écoles pour lesquelles il est actuellement l'heure locale
    // voulue — en utilisant le champ School.timezone. C'est le pattern déjà
    // retenu pour les rappels SMS dans les échanges précédents ; à appliquer
    // ici de la même façon quand une deuxième zone horaire entrera en jeu.
}