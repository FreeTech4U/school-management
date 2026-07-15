package com.schoolsaas.identity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    private String avatarUrl;
    private Boolean isActive;
    private Instant lastLoginAt;
    private Instant createdAt;
}
