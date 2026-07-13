package com.schoolsaas.academic.controller;

import com.schoolsaas.academic.entity.Subject;
import com.schoolsaas.academic.service.SubjectService;
import com.schoolsaas.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubjectController.class)
class SubjectControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubjectService subjectService;

    @Test
    @WithMockUser(roles = "TEACHER")
    void getAllSubjects_WithAuthentication_ShouldReturnSubjects() throws Exception {
        Subject subject = subject("Maths", "MATH");
        when(subjectService.getAllSubjects()).thenReturn(List.of(subject));

        mockMvc.perform(get("/api/v1/school/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Maths"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void createSubject_WithDirectorRole_ShouldCreateSubject() throws Exception {
        Subject subject = subject("Physique", "PHY");
        when(subjectService.createSubject(org.mockito.ArgumentMatchers.any(Subject.class))).thenReturn(subject);

        mockMvc.perform(post("/api/v1/school/subjects")
                        .contentType("application/json")
                        .content(asJson(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("PHY"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createSubject_WithTeacherRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/school/subjects")
                        .contentType("application/json")
                        .content(asJson(subject("Physique", "PHY"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void updateSubject_WithDirectorRole_ShouldUpdateSubject() throws Exception {
        Subject subject = subject("SVT", "SVT");
        when(subjectService.updateSubject(org.mockito.ArgumentMatchers.eq(subject.getId()), org.mockito.ArgumentMatchers.any(Subject.class)))
                .thenReturn(subject);

        mockMvc.perform(put("/api/v1/school/subjects/{id}", subject.getId())
                        .contentType("application/json")
                        .content(asJson(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("SVT"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void deleteSubject_WithDirectorRole_ShouldDeleteSubject() throws Exception {
        UUID subjectId = UUID.randomUUID();
        doNothing().when(subjectService).deleteSubject(subjectId);

        mockMvc.perform(delete("/api/v1/school/subjects/{id}", subjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Matière désactivée"));

        verify(subjectService).deleteSubject(subjectId);
    }

    private Subject subject(String name, String code) {
        Subject subject = Subject.builder().name(name).code(code).color("#123456").isActive(true).build();
        subject.setId(UUID.randomUUID());
        return subject;
    }
}
