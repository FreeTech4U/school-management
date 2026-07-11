package com.schoolsaas.platform.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OnboardingResponse {
    private String tenantSlug;
    private String schemaName;
    private String schoolName;
    private String message;
}
