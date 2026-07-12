package com.schoolsaas.academic.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.identity.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "class_subjects", uniqueConstraints = {
    @UniqueConstraint(name = "uk_class_subject", columnNames = {"class_id", "subject_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSubject extends BaseEntity {

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false)
    @Min(value = 1, message = "Coefficient minimum est 1")
    @Max(value = 10, message = "Coefficient maximum est 10")
    private Integer coefficient = 1;

    @Column(name = "weekly_hours")
    @Min(value = 1, message = "Heures hebdomadaires minimum est 1")
    @Max(value = 50, message = "Heures hebdomadaires maximum est 50")
    private Integer weeklyHours;

    @PrePersist
    @PreUpdate
    private void validate() {
        if (coefficient == null) {
            coefficient = 1;
        }
        if (coefficient < 1 || coefficient > 10) {
            throw new IllegalArgumentException("Coefficient must be between 1 and 10");
        }
        if (weeklyHours != null && (weeklyHours < 1 || weeklyHours > 50)) {
            throw new IllegalArgumentException("Weekly hours must be between 1 and 50");
        }
        if (classId == null) {
            throw new IllegalArgumentException("Class ID is required");
        }
        if (subject == null) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (teacher == null) {
            throw new IllegalArgumentException("Teacher is required");
        }
    }
}
