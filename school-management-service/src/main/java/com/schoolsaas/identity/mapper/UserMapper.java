package com.schoolsaas.identity.mapper;

import com.schoolsaas.identity.dto.response.UserResponse;
import com.schoolsaas.identity.entity.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {
    UserResponse toResponse(User user);
}
