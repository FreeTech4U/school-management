package com.schoolsaas.academic.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.grading.entity.Grade;
import com.schoolsaas.grading.entity.ReportCard;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "terms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Term extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(nullable = false)
    private String name;

    @Column(name = "term_number", nullable = false)
    private Integer termNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current")
    private Boolean isCurrent = false;

    @Column(name = "grades_entry_open")
    private Boolean gradesEntryOpen = false;

    @OneToMany(mappedBy = "term", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Grade> grades;

    @OneToMany(mappedBy = "term", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportCard> reportCards;
}
