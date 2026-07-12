package com.schoolsaas.academic.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "class_subjects")
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
    private Integer coefficient = 1;

    @Column(name = "weekly_hours")
    private Integer weeklyHours;
}
