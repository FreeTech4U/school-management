package com.schoolsaas.finance.controller;

import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.finance.dto.request.CreatePaymentRequest;
import com.schoolsaas.finance.dto.response.PaymentResponse;
import com.schoolsaas.finance.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ACCOUNTANT')")
    public ApiResponse<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return ApiResponse.ok(paymentService.createPayment(request));
    }

    @GetMapping("/payments/student/{studentId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ACCOUNTANT')")
    public ApiResponse<List<PaymentResponse>> getPaymentsByStudent(@PathVariable UUID studentId) {
        return ApiResponse.ok(paymentService.getPaymentsByStudent(studentId));
    }

    @DeleteMapping("/payments/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Void> cancelPayment(@PathVariable UUID id, @RequestParam String reason) {
        paymentService.cancelPayment(id, reason);
        return ApiResponse.ok(null, "Paiement annulé");
    }
}
