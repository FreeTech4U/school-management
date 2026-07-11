package com.schoolsaas.attendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class AttendanceRequest {
    @NotNull(message = "L'ID de l'inscription est obligatoire")
    private UUID enrollmentId;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    @NotBlank(message = "La période est obligatoire")
    private String period; // FULL_DAY, MORNING, AFTERNOON

    @NotBlank(message = "Le statut est obligatoire")
    private String status; // PRESENT, ABSENT, LATE, EXCUSED

    private String justification;
}
