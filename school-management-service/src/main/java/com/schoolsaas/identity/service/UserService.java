package com.schoolsaas.identity.service;

import com.schoolsaas.common.enums.Role;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.identity.dto.request.CreateUserRequest;
import com.schoolsaas.identity.dto.response.UserResponse;
import com.schoolsaas.identity.entity.Teacher;
import com.schoolsaas.identity.entity.User;
import com.schoolsaas.identity.mapper.UserMapper;
import com.schoolsaas.identity.repository.TeacherRepository;
import com.schoolsaas.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Utilisateur introuvable"));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.conflict("EMAIL_ALREADY_TAKEN", "Cet email est déjà utilisé");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .avatarUrl(request.getAvatarUrl())
                .isActive(true)
                .build();

        user = userRepository.save(user);

        if (Role.TEACHER.equals(request.getRole())) {
            Teacher teacher = Teacher.builder()
                    .user(user)
                    .employeeNumber(request.getEmployeeNumber())
                    .hireDate(request.getHireDate())
                    .specialty(request.getSpecialty())
                    .qualification(request.getQualification())
                    .bio(request.getBio())
                    .build();
            teacherRepository.save(teacher);
        }

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, CreateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Utilisateur introuvable"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        
        // Note: In a real app, email and role changes might need more checks
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.conflict("EMAIL_ALREADY_TAKEN", "Cet email est déjà utilisé");
        }
        user.setEmail(request.getEmail());
        
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Utilisateur introuvable"));
        user.setIsActive(false);
        userRepository.save(user);
    }
}
