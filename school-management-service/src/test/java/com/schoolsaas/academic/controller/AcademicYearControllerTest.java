package com.schoolsaas.academic.controller;

import com.schoolsaas.academic.entity.AcademicYear;
import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.common.enums.YearStatus;
import com.schoolsaas.academic.service.AcademicYearService;
import com.schoolsaas.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AcademicYearController.class)
class AcademicYearControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AcademicYearService academicYearService;

    @Test
    void getAllYears_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/school/academic-years"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void getAllYears_WithAuthentication_ShouldReturnYears() throws Exception {
        AcademicYear year = academicYear("2025-2026");
        when(academicYearService.getAllYears()).thenReturn(List.of(year));

        mockMvc.perform(get("/api/v1/school/academic-years"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].label").value("2025-2026"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void createYear_WithDirectorRole_ShouldCreateYear() throws Exception {
        AcademicYear year = academicYear("2025-2026");
        when(academicYearService.createYear(any(AcademicYear.class))).thenReturn(year);

        mockMvc.perform(post("/api/v1/school/academic-years")
                        .contentType("application/json")
                        .content(asJson(year)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.label").value("2025-2026"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createYear_WithTeacherRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/school/academic-years")
                        .contentType("application/json")
                        .content(asJson(academicYear("2025-2026"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void createTerm_WithDirectorRole_ShouldCreateTerm() throws Exception {
        UUID yearId = UUID.randomUUID();
        Term term = term("Trimestre 1");
        when(academicYearService.createTerm(eq(yearId), any(Term.class))).thenReturn(term);

        mockMvc.perform(post("/api/v1/school/academic-years/{yearId}/terms", yearId)
                        .contentType("application/json")
                        .content(asJson(term)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Trimestre 1"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void openGradesEntry_WithDirectorRole_ShouldInvokeService() throws Exception {
        UUID termId = UUID.randomUUID();
        doNothing().when(academicYearService).setGradesEntryStatus(termId, true);

        mockMvc.perform(post("/api/v1/school/terms/{id}/open-grades-entry", termId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Saisie des notes ouverte"));

        verify(academicYearService).setGradesEntryStatus(termId, true);
    }

    private AcademicYear academicYear(String label) {
        AcademicYear year = AcademicYear.builder()
                .label(label)
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2026, 7, 31))
                .isCurrent(true)
                .status(YearStatus.ACTIVE)
                .build();
        year.setId(UUID.randomUUID());
        return year;
    }

    private Term term(String name) {
        Term term = Term.builder()
                .name(name)
                .termNumber((short) 1)
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2025, 12, 20))
                .isCurrent(true)
                .gradesEntryOpen(false)
                .build();
        term.setId(UUID.randomUUID());
        return term;
    }
}
