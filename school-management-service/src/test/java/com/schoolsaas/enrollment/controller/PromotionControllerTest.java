package com.schoolsaas.enrollment.controller;

import com.schoolsaas.common.enums.PromotionBatchStatus;
import com.schoolsaas.enrollment.dto.request.CreatePromotionBatchRequest;
import com.schoolsaas.enrollment.entity.PromotionBatch;
import com.schoolsaas.enrollment.repository.PromotionBatchRepository;
import com.schoolsaas.enrollment.service.PromotionService;
import com.schoolsaas.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PromotionController.class)
class PromotionControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromotionService promotionService;
    @MockBean
    private PromotionBatchRepository promotionBatchRepository;

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void createPromotionBatch_WithDirectorRole_ShouldCreateBatch() throws Exception {
        CreatePromotionBatchRequest request = CreatePromotionBatchRequest.builder()
                .classId(UUID.randomUUID())
                .academicYearId(UUID.randomUUID())
                .nextAcademicYearId(UUID.randomUUID())
                .notes("Traitement annuel")
                .build();
        PromotionBatch batch = batch("CREATED");

        when(promotionService.createPromotionBatch(
                request.getClassId(), request.getAcademicYearId(), request.getNextAcademicYearId(), request.getNotes()))
                .thenReturn(batch);

        mockMvc.perform(post("/api/v1/school/promotion-batches")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.notes").value("Traitement annuel"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createPromotionBatch_WithTeacherRole_ShouldReturnForbidden() throws Exception {
        CreatePromotionBatchRequest request = CreatePromotionBatchRequest.builder()
                .classId(UUID.randomUUID())
                .academicYearId(UUID.randomUUID())
                .nextAcademicYearId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/v1/school/promotion-batches")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void validatePromotionBatch_WithDirectorRole_ShouldReturnValidatedBatch() throws Exception {
        PromotionBatch batch = batch("VALIDATED");
        when(promotionService.validatePromotionCriteria(batch.getId())).thenReturn(batch);

        mockMvc.perform(put("/api/v1/school/promotion-batches/{id}/validate", batch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VALIDATED"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void executePromotion_WithDirectorRole_ShouldReturnExecutedBatch() throws Exception {
        PromotionBatch batch = batch("EXECUTED");
        batch.setExecutedAt(Instant.now());
        batch.setPromotedCount(12);
        batch.setRepeatedCount(3);
        when(promotionService.executePromotion(batch.getId())).thenReturn(batch);

        mockMvc.perform(put("/api/v1/school/promotion-batches/{id}/execute", batch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXECUTED"))
                .andExpect(jsonPath("$.data.promotedCount").value(12));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void listPromotionBatches_WithAcademicYearFilter_ShouldReturnFilteredBatches() throws Exception {
        UUID academicYearId = UUID.randomUUID();
        PromotionBatch batch = batch("CREATED");
        when(promotionBatchRepository.findByAcademicYearIdAndStatus(academicYearId, "CREATED"))
                .thenReturn(List.of(batch));

        mockMvc.perform(get("/api/v1/school/promotion-batches")
                        .param("academicYearId", academicYearId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("CREATED"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void getPromotionBatch_WithDirectorRole_ShouldReturnBatch() throws Exception {
        PromotionBatch batch = batch("CREATED");
        when(promotionBatchRepository.findById(batch.getId())).thenReturn(Optional.of(batch));

        mockMvc.perform(get("/api/v1/school/promotion-batches/{id}", batch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(batch.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }

    @Test
    void listPromotionBatches_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/school/promotion-batches"))
                .andExpect(status().isUnauthorized());
    }

    private PromotionBatch batch(String status) {
        PromotionBatch batch = PromotionBatch.builder()
                .classId(UUID.randomUUID())
                .academicYearId(UUID.randomUUID())
                .nextAcademicYearId(UUID.randomUUID())
                .status(PromotionBatchStatus.valueOf(status))
                .promotedCount(0)
                .repeatedCount(0)
                .graduatedCount(0)
                .totalProcessed(15)
                .notes("Traitement annuel")
                .build();
        batch.setId(UUID.randomUUID());
        batch.setCreatedAt(Instant.now());
        return batch;
    }
}
