package com.schoolsaas.identity.controller;

import com.schoolsaas.common.enums.Role;
import com.schoolsaas.identity.dto.request.CreateUserRequest;
import com.schoolsaas.identity.dto.response.UserResponse;
import com.schoolsaas.identity.service.UserService;
import com.schoolsaas.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void getAllUsers_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/school/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void getAllUsers_WithDirectorRole_ShouldReturnPagedUsers() throws Exception {
        UserResponse response = userResponse();
        PageRequest pageable = PageRequest.of(0, 20);
        when(userService.getAllUsers(org.mockito.ArgumentMatchers.any())).thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        mockMvc.perform(get("/api/v1/school/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("director@test.com"))
                .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void getUserById_WithDirectorRole_ShouldReturnUser() throws Exception {
        UserResponse response = userResponse();
        when(userService.getUserById(response.getId())).thenReturn(response);

        mockMvc.perform(get("/api/v1/school/users/{id}", response.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.data.role").value("DIRECTOR"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void createUser_WithDirectorRole_ShouldCreateUser() throws Exception {
        CreateUserRequest request = userRequest();
        UserResponse response = userResponse();
        when(userService.createUser(org.mockito.ArgumentMatchers.any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/school/users")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("director@test.com"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void createUser_WithInvalidPayload_ShouldReturnBadRequest() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setFirstName("Awa");

        mockMvc.perform(post("/api/v1/school/users")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void updateUser_WithDirectorRole_ShouldUpdateUser() throws Exception {
        UUID id = UUID.randomUUID();
        CreateUserRequest request = userRequest();
        UserResponse response = userResponse();
        response.setId(id);
        when(userService.updateUser(org.mockito.ArgumentMatchers.eq(id), org.mockito.ArgumentMatchers.any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/school/users/{id}", id)
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void deleteUser_WithDirectorRole_ShouldDeleteUser() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(userService).deleteUser(id);

        mockMvc.perform(delete("/api/v1/school/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utilisateur désactivé avec succès"));
    }

    private CreateUserRequest userRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("director@test.com");
        request.setPhone("+224611111111");
        request.setPassword("password123");
        request.setRole(Role.DIRECTOR);
        return request;
    }

    private UserResponse userResponse() {
        return UserResponse.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("director@test.com")
                .phone("+224611111111")
                .role("DIRECTOR")
                .isActive(true)
                .build();
    }
}
