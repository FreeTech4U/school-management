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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final RoleCatalogService roleCatalogService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /** F-03 : seuls ces rôles sont assignables à un compte du personnel via cet endpoint. */
    private static final Set<String> ASSIGNABLE_ROLES = Set.of(
            SystemRoleCodes.DIRECTOR, SystemRoleCodes.TEACHER, SystemRoleCodes.ACCOUNTANT);

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
                .avatarUrl(request.getAvatarUrl())
                .isActive(true)
                .build();

        user = userRepository.save(user);

        assignRole(user, request.getRole());

        if (SystemRoleCodes.TEACHER.equals(request.getRole())) {
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

    /**
     * Attribue le rôle demandé au nouvel utilisateur.
     * Sans cet appel, le compte créé n'a aucune ligne dans user_roles : son
     * JWT porterait une liste de rôles vide et aucun @PreAuthorize ne le
     * laisserait rien faire après connexion.
     */
    private void assignRole(User user, String roleCode) {
        if (!ASSIGNABLE_ROLES.contains(roleCode)) {
            throw new BusinessException("INVALID_ROLE",
                    "Le rôle doit être DIRECTOR, TEACHER ou ACCOUNTANT");
        }

        UUID roleId = roleCatalogService.getIdByCode(roleCode);
        userRoleAssignmentRepository.save(
                UserRoleAssignment.builder()
                        .user(user)
                        .roleId(roleId)
                        .build());
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
