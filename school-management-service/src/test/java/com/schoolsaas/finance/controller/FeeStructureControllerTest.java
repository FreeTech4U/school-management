package com.schoolsaas.finance.controller;

import com.schoolsaas.common.enums.FeeStatus;
import com.schoolsaas.common.enums.FeeType;
import com.schoolsaas.enrollment.entity.Student;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.finance.dto.request.FeeStructureRequest;
import com.schoolsaas.finance.dto.request.StudentFeeDiscountRequest;
import com.schoolsaas.finance.entity.FeeStructure;
import com.schoolsaas.finance.entity.StudentFee;
import com.schoolsaas.finance.repository.FeeStructureRepository;
import com.schoolsaas.finance.repository.StudentFeeRepository;
import com.schoolsaas.finance.service.FeeStructureService;
import com.schoolsaas.finance.service.StudentFeeService;
import com.schoolsaas.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeeStructureController.class)
class FeeStructureControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeeStructureService feeStructureService;
    @MockBean
    private FeeStructureRepository feeStructureRepository;
    @MockBean
    private StudentFeeRepository studentFeeRepository;
    @MockBean
    private StudentFeeService studentFeeService;

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void createFeeStructure_WithDirectorRole_ShouldCreateFeeStructure() throws Exception {
        FeeStructureRequest request = validFeeStructureRequest();
        FeeStructure saved = feeStructure(request);
        when(feeStructureRepository.save(any(FeeStructure.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/school/fee-structures")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.label").value("Frais de scolarité"))
                .andExpect(jsonPath("$.data.feeType").value("TUITION"));
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void createFeeStructure_WithAccountantRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/school/fee-structures")
                        .contentType("application/json")
                        .content(asJson(validFeeStructureRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void listFeeStructures_WithAccountantRole_ShouldReturnStructures() throws Exception {
        FeeStructure structure = feeStructure(validFeeStructureRequest());
        when(feeStructureRepository.findAll()).thenReturn(List.of(structure));

        mockMvc.perform(get("/api/v1/school/fee-structures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].label").value("Frais de scolarité"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void updateFeeStructure_WithDirectorRole_ShouldUpdateStructure() throws Exception {
        FeeStructureRequest request = validFeeStructureRequest();
        FeeStructure existing = feeStructure(request);
        when(feeStructureRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(feeStructureRepository.save(any(FeeStructure.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/school/fee-structures/{id}", existing.getId())
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(45000))
                .andExpect(jsonPath("$.message").value("Fee structure updated successfully"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void applyDiscount_WithDirectorRole_ShouldReturnStudentFeeResponse() throws Exception {
        UUID feeId = UUID.randomUUID();
        StudentFeeDiscountRequest request = StudentFeeDiscountRequest.builder()
                .discountAmount(new BigDecimal("5000"))
                .discountReason("Bourse")
                .build();
        StudentFee fee = studentFee(feeId);

        doNothing().when(studentFeeService).applyDiscount(feeId, new BigDecimal("5000"), "Bourse");
        when(studentFeeRepository.findById(feeId)).thenReturn(Optional.of(fee));

        mockMvc.perform(put("/api/v1/school/student-fees/{id}/discount", feeId)
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountAmount").value(5000))
                .andExpect(jsonPath("$.data.feeLabel").value("Frais de scolarité"));
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void listEnrollmentFees_WithAccountantRole_ShouldReturnFees() throws Exception {
        UUID enrollmentId = UUID.randomUUID();
        when(studentFeeRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(studentFee(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/school/enrollments/{enrollmentId}/fees", enrollmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PARTIAL"));
    }

    @Test
    void getFeeStructure_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/school/fee-structures/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void deleteFeeStructure_WithDirectorRole_ShouldDeleteStructure() throws Exception {
        FeeStructure structure = feeStructure(validFeeStructureRequest());
        when(feeStructureRepository.findById(structure.getId())).thenReturn(Optional.of(structure));

        mockMvc.perform(delete("/api/v1/school/fee-structures/{id}", structure.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Fee structure deleted successfully"));
    }

    private FeeStructureRequest validFeeStructureRequest() {
        return FeeStructureRequest.builder()
                .academicYearId(UUID.randomUUID())
                .classId(UUID.randomUUID())
                .feeType(FeeType.TUITION)
                .label("Frais de scolarité")
                .amount(new BigDecimal("45000"))
                .dueDate(LocalDate.of(2026, 3, 5))
                .installmentsAllowed(true)
                .maxInstallments((short) 3)
                .build();
    }

    private FeeStructure feeStructure(FeeStructureRequest request) {
        FeeStructure structure = FeeStructure.builder()
                .academicYearId(request.getAcademicYearId())
                .classId(request.getClassId())
                .feeType(request.getFeeType())
                .label(request.getLabel())
                .amount(request.getAmount())
                .dueDate(request.getDueDate())
                .installmentsAllowed(request.getInstallmentsAllowed())
                .maxInstallments(request.getMaxInstallments())
                .build();
        structure.setId(UUID.randomUUID());
        return structure;
    }

    private StudentFee studentFee(UUID feeId) {
        FeeStructure structure = feeStructure(validFeeStructureRequest());
        structure.setId(UUID.randomUUID());

        StudentEnrollment enrollment = StudentEnrollment.builder()
                .student(new Student())
                .classId(UUID.randomUUID())
                .academicYearId(UUID.randomUUID())
                .build();
        enrollment.setId(UUID.randomUUID());
        enrollment.getStudent().setId(UUID.randomUUID());

        StudentFee fee = StudentFee.builder()
                .enrollmentId(enrollment.getId())
                .feeStructure(structure)
                .amountDue(new BigDecimal("45000"))
                .amountPaid(new BigDecimal("15000"))
                .discountAmount(new BigDecimal("5000"))
                .discountReason("Bourse")
                .dueDate(LocalDate.of(2026, 3, 5))
                .status(FeeStatus.PARTIAL)
                .build();
        fee.setId(feeId);
        return fee;
    }
}
