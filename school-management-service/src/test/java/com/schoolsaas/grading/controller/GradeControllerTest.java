package com.schoolsaas.grading.controller;

import com.schoolsaas.academic.entity.ClassSubject;
import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.academic.repository.ClassSubjectRepository;
import com.schoolsaas.academic.repository.TermRepository;
import com.schoolsaas.common.enums.EvaluationType;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.enrollment.repository.StudentEnrollmentRepository;
import com.schoolsaas.grading.dto.request.GradeRequest;
import com.schoolsaas.grading.entity.Grade;
import com.schoolsaas.grading.repository.GradeRepository;
import com.schoolsaas.grading.service.GradeService;
import com.schoolsaas.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GradeController.class)
class GradeControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GradeService gradeService;
    @MockBean
    private GradeRepository gradeRepository;
    @MockBean
    private StudentEnrollmentRepository enrollmentRepository;
    @MockBean
    private ClassSubjectRepository classSubjectRepository;
    @MockBean
    private TermRepository termRepository;

    @Test
    @WithMockUser(roles = "TEACHER")
    void recordGrade_WithTeacherRole_ShouldCreateGrade() throws Exception {
        GradeRequest request = validGradeRequest();
        StudentEnrollment enrollment = enrollment(request.getEnrollmentId());
        ClassSubject classSubject = classSubject(request.getClassSubjectId());
        Term term = term(request.getTermId());
        Grade grade = grade(request.getEnrollmentId(), request.getClassSubjectId(), request.getTermId());

        when(enrollmentRepository.findById(request.getEnrollmentId())).thenReturn(Optional.of(enrollment));
        when(classSubjectRepository.findById(request.getClassSubjectId())).thenReturn(Optional.of(classSubject));
        when(termRepository.findById(request.getTermId())).thenReturn(Optional.of(term));
        when(gradeService.enterGrade(any(Grade.class))).thenReturn(grade);

        mockMvc.perform(post("/api/v1/school/grades")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enrollmentId").value(request.getEnrollmentId().toString()))
                .andExpect(jsonPath("$.data.evaluationType").value("DEVOIR"));
    }

    @Test
    void recordGrade_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/school/grades")
                        .contentType("application/json")
                        .content(asJson(validGradeRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void recordGrade_WithUnauthorizedRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/school/grades")
                        .contentType("application/json")
                        .content(asJson(validGradeRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void recordGrade_WithInvalidPayload_ShouldReturnBadRequest() throws Exception {
        GradeRequest request = new GradeRequest();
        request.setEnrollmentId(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/school/grades")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void listGrades_WithTeacherRole_ShouldReturnGrades() throws Exception {
        Grade grade = grade(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(gradeRepository.findAll()).thenReturn(List.of(grade));

        mockMvc.perform(get("/api/v1/school/grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].value").value(15.5));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void deleteGrade_WithDirectorRole_ShouldDeleteGrade() throws Exception {
        Grade grade = grade(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(gradeRepository.findById(grade.getId())).thenReturn(Optional.of(grade));

        mockMvc.perform(delete("/api/v1/school/grades/{id}", grade.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Grade deleted successfully"));

        verify(gradeRepository).delete(grade);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void deleteGrade_WithTeacherRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/school/grades/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateGrade_WithTeacherRole_ShouldUpdateGrade() throws Exception {
        GradeRequest request = validGradeRequest();
        Grade grade = grade(request.getEnrollmentId(), request.getClassSubjectId(), request.getTermId());
        when(gradeRepository.findById(grade.getId())).thenReturn(Optional.of(grade));
        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/school/grades/{id}", grade.getId())
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value(15.5));
    }

    private GradeRequest validGradeRequest() {
        GradeRequest request = new GradeRequest();
        request.setEnrollmentId(UUID.randomUUID());
        request.setClassSubjectId(UUID.randomUUID());
        request.setTermId(UUID.randomUUID());
        request.setValue(new BigDecimal("15.5"));
        request.setEvaluationType("DEVOIR");
        request.setEvaluationLabel("Devoir 1");
        request.setEvaluationDate(LocalDate.of(2026, 2, 10));
        request.setComment("Bon travail");
        return request;
    }

    private Grade grade(UUID enrollmentId, UUID classSubjectId, UUID termId) {
        Grade grade = Grade.builder()
                .enrollment(enrollment(enrollmentId))
                .classSubject(classSubject(classSubjectId))
                .term(term(termId))
                .value(new BigDecimal("15.5"))
                .evaluationType(EvaluationType.DEVOIR)
                .evaluationLabel("Devoir 1")
                .evaluationDate(LocalDate.of(2026, 2, 10))
                .comment("Bon travail")
                .build();
        grade.setId(UUID.randomUUID());
        return grade;
    }

    private StudentEnrollment enrollment(UUID id) {
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .studentId(UUID.randomUUID())
                .classId(UUID.randomUUID())
                .academicYearId(UUID.randomUUID())
                .build();
        enrollment.setId(id);
        return enrollment;
    }

    private ClassSubject classSubject(UUID id) {
        ClassSubject classSubject = ClassSubject.builder()
                .classId(UUID.randomUUID())
                .coefficient(2)
                .weeklyHours(4)
                .build();
        classSubject.setId(id);
        return classSubject;
    }

    private Term term(UUID id) {
        Term term = Term.builder()
                .name("Trimestre 2")
                .termNumber(2)
                .startDate(LocalDate.of(2026, 1, 5))
                .endDate(LocalDate.of(2026, 3, 31))
                .build();
        term.setId(id);
        return term;
    }
}
