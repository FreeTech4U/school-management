package com.schoolsaas.academic.service;

import com.schoolsaas.academic.entity.ClassSubject;
import com.schoolsaas.academic.entity.Level;
import com.schoolsaas.academic.entity.SchoolClass;
import com.schoolsaas.academic.repository.ClassSubjectRepository;
import com.schoolsaas.academic.repository.LevelRepository;
import com.schoolsaas.academic.repository.SchoolClassRepository;
import com.schoolsaas.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final SchoolClassRepository classRepository;
    private final LevelRepository levelRepository;
    private final ClassSubjectRepository classSubjectRepository;

    public List<Level> getAllLevels() {
        return levelRepository.findAll();
    }

    public Level createLevel(Level level) {
        return levelRepository.save(level);
    }

    public List<SchoolClass> getAllClasses() {
        return classRepository.findAll();
    }

    public List<SchoolClass> getClassesByYear(UUID yearId) {
        return classRepository.findByAcademicYearId(yearId);
    }

    public SchoolClass getClassById(UUID id) {
        return classRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("CLASS_NOT_FOUND", "Classe introuvable"));
    }

    @Transactional
    public SchoolClass createClass(SchoolClass schoolClass) {
        return classRepository.save(schoolClass);
    }

    @Transactional
    public void deleteClass(UUID id) {
        classRepository.deleteById(id);
    }

    // Class Subjects
    public List<ClassSubject> getClassSubjects(UUID classId) {
        return classSubjectRepository.findBySchoolClassId(classId);
    }

    @Transactional
    public ClassSubject assignSubjectToClass(UUID classId, ClassSubject classSubject) {
       // classSubject.setClassId(classId);
        return classSubjectRepository.save(classSubject);
    }

    @Transactional
    public void removeSubjectFromClass(UUID assignmentId) {
        classSubjectRepository.deleteById(assignmentId);
    }
}
