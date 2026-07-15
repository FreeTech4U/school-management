package com.schoolsaas.grading.mapper;

import com.schoolsaas.grading.dto.request.GradeRequest;
import com.schoolsaas.grading.dto.response.GradeResponse;
import com.schoolsaas.grading.entity.Grade;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GradeMapper {
    Grade toEntity(GradeRequest request);

    GradeResponse toResponse(Grade grade);
}
