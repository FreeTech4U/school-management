package com.schoolsaas.academic.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true)
    private String code;

    private String color;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
