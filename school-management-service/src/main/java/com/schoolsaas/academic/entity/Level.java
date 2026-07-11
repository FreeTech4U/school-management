package com.schoolsaas.academic.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "levels")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Level extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
