package com.schoolsaas.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class EnrollStudentRequest {
    @NotNull(message = "L'élève est obligatoire")
    private UUID studentId;

    @NotNull(message = "La classe est obligatoire")
    private UUID classId;

    @NotNull(message = "L'année scolaire est obligatoire")
    private UUID academicYearId;

    private LocalDate enrollmentDate = LocalDate.now();
    private Boolean isRepeating = false;
}
