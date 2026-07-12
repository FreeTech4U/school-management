package com.schoolsaas.grading.entity;

import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.ReportCardStatus;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_cards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCard extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private StudentEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "general_average")
    private BigDecimal generalAverage;

    @Column(name = "rank_in_class")
    private Integer rankInClass;

    @Column(name = "class_size")
    private Integer classSize;

    @Column(name = "teacher_comment")
    private String teacherComment;

    @Column(name = "director_comment")
    private String directorComment;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportCardStatus status = ReportCardStatus.DRAFT;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
