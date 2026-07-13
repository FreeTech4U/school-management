package com.schoolsaas.enrollment.dto.request;

import com.schoolsaas.common.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateStudentRequest {
    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @NotNull(message = "La date de naissance est obligatoire")
    private LocalDate dateOfBirth;

    @NotNull(message = "Le genre est obligatoire")
    private Gender gender;

    private String birthCity;
    private String birthCountry = "GN";
    private String address;
    private String parentName;
    private String parentPhone;
    private String medicalNotes;
}
