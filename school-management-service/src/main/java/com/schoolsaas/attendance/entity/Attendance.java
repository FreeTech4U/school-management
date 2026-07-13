package com.schoolsaas.attendance.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.AttendanceStatus;
import com.schoolsaas.common.enums.Period;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Appel de présence (F-17).
 *
 * CORRECTION — enrollment passe d'une @ManyToOne<StudentEnrollment> à un UUID :
 * l'association violait la règle inter-domaines (attendance → enrollment).
 *
 * RÈGLE SMS : seul le statut ABSENT déclenche une notification au parent.
 * EXCUSED (absence justifiée) et LATE n'en déclenchent pas.
 * Voir AttendanceStatus.triggersParentSms().
 *
 * Une absence ABSENT est régularisable a posteriori : le directeur la passe en
 * EXCUSED et renseigne la justification (un CHECK en base l'exige).
 *
 * UNICITÉ : un seul appel par élève / jour / période
 * → erreur ATTENDANCE_ALREADY_RECORDED.
 */
@Entity
@Table(
        name = "attendance",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_attendance",
                columnNames = {"enrollment_id", "date", "period"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance extends BaseEntity {

    /** INTER-domaine (attendance → enrollment) : UUID. */
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(nullable = false)
    private LocalDate date;

    /** FULL_DAY (défaut) · MORNING · AFTERNOON · EVENING */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Period period = Period.FULL_DAY;

    /** PRESENT · ABSENT · LATE · EXCUSED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    /** Obligatoire si status = EXCUSED (CHECK en base). */
    @Column(columnDefinition = "TEXT")
    private String justification;

    /** Utilisateur ayant fait l'appel. INTER-domaine (→ identity) : UUID. */
    @Column(name = "recorded_by")
    private UUID recordedBy;
}
