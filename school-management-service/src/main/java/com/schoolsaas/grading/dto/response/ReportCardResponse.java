package com.schoolsaas.grading.dto.response;

import com.schoolsaas.common.enums.ReportCardStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCardResponse {

    private UUID id;
    private UUID enrollmentId;
    private String studentName;
    private UUID termId;
    private String termName;
    private BigDecimal generalAverage;
    private Short rankInClass;
    private Short classSize;
    private String teacherComment;
    private String directorComment;
    private ReportCardStatus status;
    private String pdfUrl;
    private Instant publishedAt;
    private Instant createdAt;
}
