package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "schools", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class School extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(name = "schema_name", unique = true, nullable = false)
    private String schemaName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;
    private String address;
    private String city;

    @Column(name = "country_code", length = 3)
    private String countryCode = "GN";

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(nullable = false)
    private String status = "trial"; // trial, active, suspended, deleted

    private String timezone = "Africa/Conakry";
    private String currency = "GNF";
}
