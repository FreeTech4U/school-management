package com.schoolsaas.enrollment.mapper;

import com.schoolsaas.enrollment.dto.response.StudentResponse;
import com.schoolsaas.enrollment.entity.Student;
import org.mapstruct.Mapper;

@Mapper
public interface EnrollmentMapper {
    StudentResponse toStudentResponse(Student student);
}
