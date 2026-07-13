package com.schoolsaas.academic.controller;

import com.schoolsaas.academic.entity.ClassSubject;
import com.schoolsaas.academic.entity.Level;
import com.schoolsaas.academic.entity.SchoolClass;
import com.schoolsaas.academic.entity.Subject;
import com.schoolsaas.academic.service.ClassService;
import com.schoolsaas.identity.entity.User;
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

@WebMvcTest(ClassController.class)
class ClassControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClassService classService;

    @Test
    @WithMockUser(roles = "TEACHER")
    void getLevels_WithAuthentication_ShouldReturnLevels() throws Exception {
        Level level = level("College", 1);
        when(classService.getAllLevels()).thenReturn(List.of(level));

        mockMvc.perform(get("/api/v1/school/levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("College"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void createLevel_WithDirectorRole_ShouldCreateLevel() throws Exception {
        Level level = level("Lycee", 2);
        when(classService.createLevel(org.mockito.ArgumentMatchers.any(Level.class))).thenReturn(level);

        mockMvc.perform(post("/api/v1/school/levels")
                        .contentType("application/json")
                        .content(asJson(level)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Lycee"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createLevel_WithTeacherRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/school/levels")
                        .contentType("application/json")
                        .content(asJson(level("Lycee", 2))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getAllClasses_WithAuthentication_ShouldReturnClasses() throws Exception {
        SchoolClass schoolClass = schoolClass("7e A");
        when(classService.getAllClasses()).thenReturn(List.of(schoolClass));

        mockMvc.perform(get("/api/v1/school/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("7e A"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void createClass_WithDirectorRole_ShouldCreateClass() throws Exception {
        SchoolClass schoolClass = schoolClass("8e B");
        when(classService.createClass(org.mockito.ArgumentMatchers.any(SchoolClass.class))).thenReturn(schoolClass);

        mockMvc.perform(post("/api/v1/school/classes")
                        .contentType("application/json")
                        .content(asJson(schoolClass)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("8e B"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void updateClass_WithDirectorRole_ShouldUpdateClass() throws Exception {
        SchoolClass schoolClass = schoolClass("9e C");
        when(classService.createClass(org.mockito.ArgumentMatchers.any(SchoolClass.class))).thenReturn(schoolClass);

        mockMvc.perform(put("/api/v1/school/classes/{id}", schoolClass.getId())
                        .contentType("application/json")
                        .content(asJson(schoolClass)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("9e C"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void deleteClass_WithDirectorRole_ShouldDeleteClass() throws Exception {
        UUID classId = UUID.randomUUID();
        doNothing().when(classService).deleteClass(classId);

        mockMvc.perform(delete("/api/v1/school/classes/{id}", classId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Classe supprimée"));

        verify(classService).deleteClass(classId);
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void assignSubject_WithDirectorRole_ShouldAssignSubject() throws Exception {
        UUID classId = UUID.randomUUID();
        ClassSubject classSubject = classSubject(classId);
        when(classService.assignSubjectToClass(org.mockito.ArgumentMatchers.eq(classId), org.mockito.ArgumentMatchers.any(ClassSubject.class)))
                .thenReturn(classSubject);

        mockMvc.perform(post("/api/v1/school/classes/{classId}/subjects", classId)
                        .contentType("application/json")
                        .content(asJson(classSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classId").value(classId.toString()));
    }

    private Level level(String name, int orderIndex) {
        Level level = Level.builder().name(name).orderIndex(orderIndex).build();
        level.setId(UUID.randomUUID());
        return level;
    }

    private SchoolClass schoolClass(String name) {
        SchoolClass schoolClass = SchoolClass.builder()
                .academicYearId(UUID.randomUUID())
                .level(level("College", 1))
                .name(name)
                .option("Sciences")
                .capacity(40)
                .roomNumber("A12")
                .build();
        schoolClass.setId(UUID.randomUUID());
        return schoolClass;
    }

    private ClassSubject classSubject(UUID classId) {
        User teacher = User.builder().firstName("Mamadou").lastName("Diallo").email("t@test.com").passwordHash("x").role("TEACHER").build();
        teacher.setId(UUID.randomUUID());
        Subject subject = Subject.builder().name("Maths").code("MATH").color("#fff").isActive(true).build();
        subject.setId(UUID.randomUUID());
        ClassSubject classSubject = ClassSubject.builder()
                .classId(classId)
                .subject(subject)
                .teacher(teacher)
                .coefficient(2)
                .weeklyHours(4)
                .build();
        classSubject.setId(UUID.randomUUID());
        return classSubject;
    }
}
