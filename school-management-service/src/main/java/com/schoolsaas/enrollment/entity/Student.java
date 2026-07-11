package com.schoolsaas.enrollment.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "students")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student extends BaseEntity {

    @Column(name = "student_number", unique = true)
    private String studentNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 1)
    private String gender; // M or F

    @Column(name = "birth_city")
    private String birthCity;

    @Column(name = "birth_country")
    private String birthCountry = "GN";

    @Column(name = "photo_url")
    private String photoUrl;

    private String address;

    @Column(name = "parent_name")
    private String parentName;

    @Column(name = "parent_phone")
    private String parentPhone;

    @Column(name = "medical_notes")
    private String medicalNotes;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
