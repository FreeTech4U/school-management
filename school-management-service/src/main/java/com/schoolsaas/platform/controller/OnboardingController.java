package com.schoolsaas.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.platform.dto.request.OnboardingRequest;
import com.schoolsaas.platform.dto.response.OnboardingResponse;
import com.schoolsaas.platform.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/platform/onboard")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "Endpoints pour l'inscription des écoles sur la plateforme")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscription d'une nouvelle école", description = "Crée un nouveau tenant, un schéma de base de données dédié et l'utilisateur administrateur (directeur)")
    public ApiResponse<OnboardingResponse> onboard(@Valid @RequestBody OnboardingRequest request) {
        return ApiResponse.ok(onboardingService.onboard(request));
    }
}
