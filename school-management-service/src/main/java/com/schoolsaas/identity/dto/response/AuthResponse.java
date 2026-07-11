package com.schoolsaas.identity.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserData user;

    @Data
    @Builder
    public static class UserData {
        private UUID id;
        private String fullName;
        private String email;
        private List<String> roles;
        private UUID tenantId;
        private String schoolName;
    }
}
