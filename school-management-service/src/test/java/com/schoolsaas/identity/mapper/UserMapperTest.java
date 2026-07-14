package com.schoolsaas.identity.mapper;

import com.schoolsaas.identity.dto.response.UserResponse;
import com.schoolsaas.identity.entity.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toResponse_ShouldMapCorrectly() {
        // Given
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .role(Role.DIRECTOR)
                .isActive(true)
                .build();
        user.setId(UUID.randomUUID());

        // When
        UserResponse response = mapper.toResponse(user);

        // Then
        assertNotNull(response);
        assertEquals(user.getId(), response.getId());
        assertEquals(user.getFirstName(), response.getFirstName());
        assertEquals(user.getLastName(), response.getLastName());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(user.getRole().name(), response.getRole());
        assertEquals(user.getIsActive(), response.getIsActive());
    }
}
