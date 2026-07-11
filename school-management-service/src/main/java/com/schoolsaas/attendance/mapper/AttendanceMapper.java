package com.schoolsaas.attendance.mapper;

import com.schoolsaas.attendance.dto.request.AttendanceRequest;
import com.schoolsaas.attendance.dto.response.AttendanceResponse;
import com.schoolsaas.attendance.entity.Attendance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {
    Attendance toEntity(AttendanceRequest request);

    AttendanceResponse toResponse(Attendance attendance);
}
