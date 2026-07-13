package com.schoolsaas.grading.controller;

import com.schoolsaas.academic.entity.AcademicYear;
import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.common.enums.ReportCardStatus;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.grading.dto.request.GenerateReportCardsRequest;
import com.schoolsaas.grading.dto.request.ReportCardCommentsRequest;
import com.schoolsaas.grading.entity.ReportCard;
import com.schoolsaas.grading.repository.ReportCardRepository;
import com.schoolsaas.grading.service.ReportCardService;
import com.schoolsaas.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportCardController.class)
class ReportCardControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportCardService reportCardService;
    @MockBean
    private ReportCardRepository reportCardRepository;

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void generateReportCards_WithDirectorRole_ShouldCreateBatch() throws Exception {
        GenerateReportCardsRequest request = GenerateReportCardsRequest.builder()
                .classId(UUID.randomUUID())
                .termId(UUID.randomUUID())
                .build();
        when(reportCardService.generateForClass(request.getClassId(), request.getTermId())).thenReturn(24);

        mockMvc.perform(post("/api/v1/school/report-cards/generate")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Generated 24 report cards successfully"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void generateReportCards_WithTeacherRole_ShouldReturnForbidden() throws Exception {
        GenerateReportCardsRequest request = GenerateReportCardsRequest.builder()
                .classId(UUID.randomUUID())
                .termId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/v1/school/report-cards/generate")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void listReportCards_WithAllowedRole_ShouldReturnCards() throws Exception {
        UUID termId = UUID.randomUUID();
        ReportCard card = reportCard(termId);
        when(reportCardRepository.findAll()).thenReturn(List.of(card));

        mockMvc.perform(get("/api/v1/school/report-cards").param("termId", termId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].termId").value(termId.toString()))
                .andExpect(jsonPath("$.data[0].status").value("DRAFT"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void getReportCard_WithDirectorRole_ShouldReturnCard() throws Exception {
        ReportCard card = reportCard(UUID.randomUUID());
        when(reportCardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        mockMvc.perform(get("/api/v1/school/report-cards/{id}", card.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(card.getId().toString()))
                .andExpect(jsonPath("$.data.studentName").value(card.getEnrollment().getStudentId().toString()));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateComments_WithTeacherRole_ShouldUpdateComments() throws Exception {
        ReportCardCommentsRequest request = ReportCardCommentsRequest.builder()
                .teacherComment("Bon trimestre")
                .directorComment("Continuez")
                .build();
        ReportCard updated = reportCard(UUID.randomUUID());
        updated.setTeacherComment("Bon trimestre");
        updated.setDirectorComment("Continuez");

        when(reportCardService.updateComments(any(), any(), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/school/report-cards/{id}/comments", updated.getId())
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teacherComment").value("Bon trimestre"))
                .andExpect(jsonPath("$.data.directorComment").value("Continuez"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void publishReportCard_WithDirectorRole_ShouldReturnPublishedCard() throws Exception {
        ReportCard published = reportCard(UUID.randomUUID());
        published.setStatus(ReportCardStatus.PUBLISHED);
        published.setPublishedAt(LocalDateTime.now());

        when(reportCardService.publish(published.getId())).thenReturn(published);

        mockMvc.perform(post("/api/v1/school/report-cards/{id}/publish", published.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void publishForClass_WithDirectorRole_ShouldReturnPublishedCount() throws Exception {
        UUID classId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();
        when(reportCardService.publishForClass(classId, termId)).thenReturn(18);

        mockMvc.perform(post("/api/v1/school/report-cards/class/{classId}/term/{termId}/publish-all", classId, termId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Published 18 report cards successfully"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getReportCardPdf_WithAuthentication_ShouldReturnPlaceholder() throws Exception {
        ReportCard card = reportCard(UUID.randomUUID());
        card.setPdfUrl(null);
        when(reportCardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        mockMvc.perform(get("/api/v1/school/report-cards/{id}/pdf", card.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("PDF not yet generated"));
    }

    @Test
    void getEnrollmentReportCard_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/school/report-cards/enrollment/{enrollmentId}/term/{termId}",
                        UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private ReportCard reportCard(UUID termId) {
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID reportCardId = UUID.randomUUID();
        UUID academicYearId = UUID.randomUUID();

        AcademicYear academicYear = AcademicYear.builder()
                .label("2025-2026")
                .build();
        academicYear.setId(academicYearId);

        Term term = Term.builder()
                .academicYear(academicYear)
                .name("Trimestre 1")
                .termNumber(1)
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2025, 12, 20))
                .build();
        term.setId(termId);

        StudentEnrollment enrollment = StudentEnrollment.builder()
                .studentId(studentId)
                .classId(UUID.randomUUID())
                .academicYearId(academicYearId)
                .build();
        enrollment.setId(enrollmentId);

        ReportCard card = ReportCard.builder()
                .enrollment(enrollment)
                .term(term)
                .generalAverage(new BigDecimal("14.25"))
                .rankInClass(3)
                .classSize(28)
                .teacherComment("Bien")
                .directorComment("Continuez")
                .status(ReportCardStatus.DRAFT)
                .pdfUrl("https://cdn/report.pdf")
                .build();
        card.setId(reportCardId);
        card.setCreatedAt(LocalDateTime.now());
        return card;
    }
}
