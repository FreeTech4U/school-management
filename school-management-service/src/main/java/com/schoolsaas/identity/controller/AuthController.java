package com.schoolsaas.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.identity.dto.request.LoginRequest;
import com.schoolsaas.identity.dto.response.AuthResponse;
import com.schoolsaas.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints pour l'authentification et la gestion des jetons")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authentification utilisateur", description = "Permet à un utilisateur de se connecter et de recevoir un jeton JWT")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le jeton", description = "Permet d'obtenir un nouveau jeton d'accès à partir d'un jeton de rafraîchissement")
    public ApiResponse<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return ApiResponse.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion", description = "Invalide la session actuelle (principalement géré côté client)")
    public ApiResponse<Void> logout() {
        // En phase 1, le logout est géré côté client (suppression du token)
        return ApiResponse.ok(null, "Déconnexion réussie");
    }
}
