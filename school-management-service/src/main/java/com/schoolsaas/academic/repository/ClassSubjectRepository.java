package com.schoolsaas.academic.repository;

import com.schoolsaas.academic.entity.ClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassSubjectRepository extends JpaRepository<ClassSubject, UUID> {
    List<ClassSubject> findBySchoolClassId(UUID schoolClassId);
    List<ClassSubject> findByTeacherId(UUID teacherId);
}
