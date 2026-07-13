package com.schoolsaas.grading.mapper;

import com.schoolsaas.academic.entity.ClassSubject;
import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.common.enums.EvaluationType;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
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
        assertEquals(request.getValue(), entity.getValue());
        assertEquals("DEVOIR", entity.getEvaluationType().toString());
        assertEquals(request.getEvaluationLabel(), entity.getEvaluationLabel());
    }

    @Test
    void toResponse_ShouldMapCorrectly() {
        // Given
        UUID enrollmentId = UUID.randomUUID();
        UUID classSubjectId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();

        StudentEnrollment enrollment = StudentEnrollment.builder().build();
        enrollment.setId(enrollmentId);

        ClassSubject classSubject = ClassSubject.builder().build();
        classSubject.setId(classSubjectId);

        Term term = Term.builder().build();
        term.setId(termId);

        Grade entity = Grade.builder()
                .enrollmentId(enrollmentId)
                .classSubjectId(classSubjectId)
                .termId(termId)
                .value(BigDecimal.valueOf(18.0))
                .evaluationType(EvaluationType.COMPOSITION)
                .evaluationLabel("Composition Trimestre 1")
                .evaluationDate(LocalDate.now())
                .build();
        entity.setId(gradeId);

        // When
        GradeResponse response = mapper.toResponse(entity);

        // Then
        assertNotNull(response);
        assertEquals(gradeId, response.getId());
        // Note: MapStruct mapper doesn't extract nested IDs. Controller uses manual mapToResponse()
        assertEquals(entity.getValue(), response.getValue());
        assertEquals("COMPOSITION", response.getEvaluationType());
    }
}
