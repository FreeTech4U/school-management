package com.schoolsaas.identity.controller;

import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.identity.dto.request.CreateUserRequest;
import com.schoolsaas.identity.dto.response.UserResponse;
import com.schoolsaas.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Page<UserResponse>> getAllUsers(Pageable pageable) {
        Page<UserResponse> page = userService.getAllUsers(pageable);
        return ApiResponse.paged(page, ApiResponse.PageMeta.of(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<UserResponse> getUserById(@PathVariable UUID id) {
        return ApiResponse.ok(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userService.createUser(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<UserResponse> updateUser(@PathVariable UUID id, @Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ApiResponse.ok(null, "Utilisateur désactivé avec succès");
    }
}
