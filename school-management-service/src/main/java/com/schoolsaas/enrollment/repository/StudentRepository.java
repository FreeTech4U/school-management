package com.schoolsaas.enrollment.repository;

import com.schoolsaas.enrollment.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    @Query("SELECT s FROM Student s WHERE LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.studentNumber) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Student> searchStudents(String search, Pageable pageable);
}
