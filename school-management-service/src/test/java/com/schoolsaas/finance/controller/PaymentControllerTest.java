package com.schoolsaas.finance.controller;

import com.schoolsaas.finance.dto.request.CreatePaymentRequest;
import com.schoolsaas.finance.dto.response.PaymentResponse;
import com.schoolsaas.finance.service.PaymentService;
import com.schoolsaas.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void createPayment_WithAccountantRole_ShouldCreatePayment() throws Exception {
        CreatePaymentRequest request = validPaymentRequest();
        PaymentResponse response = paymentResponse(request.getStudentId());
        when(paymentService.createPayment(any(CreatePaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/school/payments")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(request.getStudentId().toString()))
                .andExpect(jsonPath("$.data.amount").value(30000));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createPayment_WithTeacherRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/school/payments")
                        .contentType("application/json")
                        .content(asJson(validPaymentRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void getPaymentsByStudent_WithAccountantRole_ShouldReturnPayments() throws Exception {
        UUID studentId = UUID.randomUUID();
        when(paymentService.getPaymentsByStudent(studentId)).thenReturn(List.of(paymentResponse(studentId)));

        mockMvc.perform(get("/api/v1/school/payments/student/{studentId}", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].studentId").value(studentId.toString()));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void cancelPayment_WithDirectorRole_ShouldInvokeService() throws Exception {
        UUID paymentId = UUID.randomUUID();
        doNothing().when(paymentService).cancelPayment(paymentId, "Erreur de saisie");

        mockMvc.perform(delete("/api/v1/school/payments/{id}", paymentId)
                        .param("reason", "Erreur de saisie"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Paiement annulé"));

        verify(paymentService).cancelPayment(paymentId, "Erreur de saisie");
    }

    private CreatePaymentRequest validPaymentRequest() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setStudentId(UUID.randomUUID());
        request.setAmount(new BigDecimal("30000"));
        request.setPaymentMethod("CASH");
        request.setReferenceNumber("PAY-001");
        request.setPaymentDate(LocalDate.of(2026, 2, 15));

        CreatePaymentRequest.AllocationRequest allocation = new CreatePaymentRequest.AllocationRequest();
        allocation.setStudentFeeId(UUID.randomUUID());
        allocation.setAmount(new BigDecimal("30000"));
        request.setAllocations(List.of(allocation));
        return request;
    }

    private PaymentResponse paymentResponse(UUID studentId) {
        return PaymentResponse.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .studentName("Awa Diallo")
                .amount(new BigDecimal("30000"))
                .paymentDate(LocalDateTime.of(2026, 2, 15, 10, 0))
                .paymentMethod("CASH")
                .referenceNumber("PAY-001")
                .receiptNumber("RCPT-001")
                .build();
    }
}
