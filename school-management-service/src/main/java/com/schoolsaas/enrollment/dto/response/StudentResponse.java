package com.schoolsaas.enrollment.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class StudentResponse {
    private UUID id;
    private String studentNumber;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String photoUrl;
    private String parentName;
    private String parentPhone;
    private Boolean isActive;
}
