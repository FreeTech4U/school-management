package com.schoolsaas.grading.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class GradeRequest {
    @NotNull(message = "L'ID de l'inscription est obligatoire")
    private UUID enrollmentId;

    @NotNull(message = "L'ID de la matière est obligatoire")
    private UUID classSubjectId;

    @NotNull(message = "L'ID du trimestre est obligatoire")
    private UUID termId;

    @NotNull(message = "La valeur de la note est obligatoire")
    private BigDecimal value;

    @NotBlank(message = "Le type d'évaluation est obligatoire")
    private String evaluationType; // DEVOIR, COMPOSITION, ORAL, TP

    @NotBlank(message = "Le libellé est obligatoire")
    private String evaluationLabel;

    @NotNull(message = "La date d'évaluation est obligatoire")
    private LocalDate evaluationDate;

    private String comment;
}
