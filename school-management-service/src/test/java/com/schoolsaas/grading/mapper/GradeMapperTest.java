package com.schoolsaas.grading.mapper;

import com.schoolsaas.grading.dto.request.GradeRequest;
import com.schoolsaas.grading.dto.response.GradeResponse;
import com.schoolsaas.grading.entity.Grade;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GradeMapperTest {

    private final GradeMapper mapper = Mappers.getMapper(GradeMapper.class);

    @Test
    void toEntity_ShouldMapCorrectly() {
        // Given
        GradeRequest request = new GradeRequest();
        request.setEnrollmentId(UUID.randomUUID());
        request.setClassSubjectId(UUID.randomUUID());
        request.setTermId(UUID.randomUUID());
        request.setValue(BigDecimal.valueOf(15.5));
        request.setEvaluationType("DEVOIR");
        request.setEvaluationLabel("Devoir 1");
        request.setEvaluationDate(LocalDate.now());

        // When
        Grade entity = mapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(request.getEnrollmentId(), entity.getEnrollmentId());
        assertEquals(request.getClassSubjectId(), entity.getClassSubjectId());
        assertEquals(request.getTermId(), entity.getTermId());
        assertEquals(request.getValue(), entity.getValue());
        assertEquals(request.getEvaluationType(), entity.getEvaluationType());
        assertEquals(request.getEvaluationLabel(), entity.getEvaluationLabel());
    }

    @Test
    void toResponse_ShouldMapCorrectly() {
        // Given
        Grade entity = Grade.builder()
                .enrollmentId(UUID.randomUUID())
                .classSubjectId(UUID.randomUUID())
                .termId(UUID.randomUUID())
                .value(BigDecimal.valueOf(18.0))
                .evaluationType("COMPOSITION")
                .evaluationLabel("Composition Trimestre 1")
                .evaluationDate(LocalDate.now())
                .build();
        entity.setId(UUID.randomUUID());

        // When
        GradeResponse response = mapper.toResponse(entity);

        // Then
        assertNotNull(response);
        assertEquals(entity.getId(), response.getId());
        assertEquals(entity.getEnrollmentId(), response.getEnrollmentId());
        assertEquals(entity.getValue(), response.getValue());
        assertEquals(entity.getEvaluationType(), response.getEvaluationType());
    }
}
