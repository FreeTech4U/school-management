package com.schoolsaas.academic.service;

import com.schoolsaas.academic.entity.Subject;
import com.schoolsaas.academic.repository.SubjectRepository;
import com.schoolsaas.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    @Transactional
    public Subject createSubject(Subject subject) {
        return subjectRepository.save(subject);
    }

    @Transactional
    public Subject updateSubject(UUID id, Subject subjectDetails) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("SUBJECT_NOT_FOUND", "Matière introuvable"));
        subject.setName(subjectDetails.getName());
        subject.setCode(subjectDetails.getCode());
        subject.setColor(subjectDetails.getColor());
        return subjectRepository.save(subject);
    }

    @Transactional
    public void deleteSubject(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("SUBJECT_NOT_FOUND", "Matière introuvable"));
        subject.setIsActive(false);
        subjectRepository.save(subject);
    }
}
