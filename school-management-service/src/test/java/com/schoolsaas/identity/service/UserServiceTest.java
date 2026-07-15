package com.schoolsaas.identity.service;

import com.schoolsaas.common.constants.SystemRoleCodes;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.identity.dto.request.CreateUserRequest;
import com.schoolsaas.identity.dto.response.UserResponse;
import com.schoolsaas.identity.entity.Teacher;
import com.schoolsaas.identity.entity.User;
import com.schoolsaas.identity.entity.UserRoleAssignment;
import com.schoolsaas.identity.mapper.UserMapper;
import com.schoolsaas.identity.repository.TeacherRepository;
import com.schoolsaas.identity.repository.UserRepository;
import com.schoolsaas.identity.repository.UserRoleAssignmentRepository;
import com.schoolsaas.platform.service.RoleCatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Mock
    private RoleCatalogService roleCatalogService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void getAllUsers_ReturnsMappedPage() {
        User user = user("director@test.com");
        UserResponse response = response(user);
        PageRequest pageable = PageRequest.of(0, 10);

        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));
        when(userMapper.toResponse(user)).thenReturn(response);

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("director@test.com", result.getContent().get(0).getEmail());
    }

    @Test
    void getUserById_WhenMissing_ThrowsBusinessException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.getUserById(id));

        assertEquals("USER_NOT_FOUND", ex.getCode());
    }

    @Test
    void createUser_WithTeacherRole_CreatesTeacherAndEncodesPassword() {
        CreateUserRequest request = teacherRequest();
        User savedUser = user(request.getEmail());
        savedUser.setId(UUID.randomUUID());
        UserResponse response = response(savedUser);
        UUID teacherRoleId = UUID.randomUUID();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(roleCatalogService.getIdByCode(SystemRoleCodes.TEACHER)).thenReturn(teacherRoleId);
        when(userMapper.toResponse(savedUser)).thenReturn(response);

        UserResponse result = userService.createUser(request);

        assertEquals(request.getEmail(), result.getEmail());
        verify(userRepository).save(any(User.class));

        ArgumentCaptor<UserRoleAssignment> roleCaptor = ArgumentCaptor.forClass(UserRoleAssignment.class);
        verify(userRoleAssignmentRepository).save(roleCaptor.capture());
        assertSame(savedUser, roleCaptor.getValue().getUser());
        assertEquals(teacherRoleId, roleCaptor.getValue().getRoleId());

        ArgumentCaptor<Teacher> teacherCaptor = ArgumentCaptor.forClass(Teacher.class);
        verify(teacherRepository).save(teacherCaptor.capture());
        Teacher teacher = teacherCaptor.getValue();
        assertSame(savedUser, teacher.getUser());
        assertEquals("EMP-001", teacher.getEmployeeNumber());
        assertEquals("Mathematics", teacher.getSpecialty());
    }

    @Test
    void createUser_WithInvalidRole_ThrowsBusinessExceptionAndSkipsPersistence() {
        CreateUserRequest request = teacherRequest();
        request.setRole("PARENT");
        User savedUser = user(request.getEmail());
        savedUser.setId(UUID.randomUUID());

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.createUser(request));

        assertEquals("INVALID_ROLE", ex.getCode());
        verify(userRoleAssignmentRepository, never()).save(any(UserRoleAssignment.class));
        verify(teacherRepository, never()).save(any(Teacher.class));
    }

    @Test
    void createUser_WithExistingEmail_ThrowsConflict() {
        CreateUserRequest request = teacherRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.createUser(request));

        assertEquals("EMAIL_ALREADY_TAKEN", ex.getCode());
        verify(userRepository, never()).save(any(User.class));
        verify(teacherRepository, never()).save(any(Teacher.class));
    }

    @Test
    void updateUser_WithNewPassword_UpdatesFieldsAndPasswordHash() {
        UUID id = UUID.randomUUID();
        User existing = user("old@test.com");
        existing.setId(id);
        CreateUserRequest request = new CreateUserRequest();
        request.setFirstName("New");
        request.setLastName("Name");
        request.setEmail("new@test.com");
        request.setPhone("+224611111111");
        request.setPassword("new-password");
        request.setAvatarUrl("avatar.png");

        User updated = user(request.getEmail());
        updated.setId(id);
        updated.setFirstName(request.getFirstName());
        updated.setLastName(request.getLastName());
        updated.setPhone(request.getPhone());
        updated.setAvatarUrl(request.getAvatarUrl());
        updated.setPasswordHash("new-hash");

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("new-hash");
        when(userRepository.save(existing)).thenReturn(existing);
        when(userMapper.toResponse(existing)).thenReturn(response(updated));

        UserResponse result = userService.updateUser(id, request);

        assertEquals("new@test.com", result.getEmail());
        assertEquals("new-hash", existing.getPasswordHash());
        assertEquals("New", existing.getFirstName());
    }

    @Test
    void deleteUser_SetsInactiveFlag() {
        UUID id = UUID.randomUUID();
        User existing = user("director@test.com");
        existing.setId(id);
        existing.setIsActive(true);

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        userService.deleteUser(id);

        assertFalse(existing.getIsActive());
        verify(userRepository).save(existing);
    }

    private CreateUserRequest teacherRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFirstName("Awa");
        request.setLastName("Diallo");
        request.setEmail("teacher@test.com");
        request.setPhone("+224622334455");
        request.setPassword("password123");
        request.setRole(SystemRoleCodes.TEACHER);
        request.setEmployeeNumber("EMP-001");
        request.setHireDate(LocalDate.of(2024, 1, 10));
        request.setSpecialty("Mathematics");
        request.setQualification("Master");
        request.setBio("Experienced teacher");
        return request;
    }

    private User user(String email) {
        return User.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .phone("+224600000000")
                .passwordHash("hash")
               // .role(role)
                .isActive(true)
                .build();
    }

    private UserResponse response(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
        //        .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .build();
    }
}
