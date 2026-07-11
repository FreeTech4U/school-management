package com.schoolsaas.enrollment.mapper;

import com.schoolsaas.enrollment.dto.response.StudentResponse;
import com.schoolsaas.enrollment.entity.Student;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EnrollmentMapperTest {

    private final EnrollmentMapper mapper = Mappers.getMapper(EnrollmentMapper.class);

    @Test
    void toStudentResponse_ShouldMapCorrectly() {
        // Given
        Student student = Student.builder()
                .studentNumber("ST123")
                .firstName("Alice")
                .lastName("Smith")
                .isActive(true)
                .build();
        student.setId(UUID.randomUUID());

        // When
        StudentResponse response = mapper.toStudentResponse(student);

        // Then
        assertNotNull(response);
        assertEquals(student.getId(), response.getId());
        assertEquals(student.getStudentNumber(), response.getStudentNumber());
        assertEquals(student.getFirstName(), response.getFirstName());
        assertEquals(student.getLastName(), response.getLastName());
        assertEquals(student.getIsActive(), response.getIsActive());
    }
}
