package com.schoolsaas.academic.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "classes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClass extends BaseEntity {

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id", nullable = false)
    private Level level;

    @Column(nullable = false)
    private String name;

    private String option;
    private Integer capacity;
    
    @Column(name = "room_number")
    private String roomNumber;
}
