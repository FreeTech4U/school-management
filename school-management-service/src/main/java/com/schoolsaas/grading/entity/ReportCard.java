package com.schoolsaas.grading.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_cards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCard extends BaseEntity {

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "term_id", nullable = false)
    private UUID termId;

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
    private String status = "DRAFT"; // DRAFT, PUBLISHED, SENT_TO_PARENT

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
