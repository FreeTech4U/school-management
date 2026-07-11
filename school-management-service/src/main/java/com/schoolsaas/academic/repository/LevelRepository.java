package com.schoolsaas.academic.repository;

import com.schoolsaas.academic.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LevelRepository extends JpaRepository<Level, UUID> {
}
