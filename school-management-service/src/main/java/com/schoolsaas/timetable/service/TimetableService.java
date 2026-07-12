package com.schoolsaas.timetable.service;

import com.schoolsaas.academic.entity.ClassSubject;
import com.schoolsaas.academic.repository.ClassSubjectRepository;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.timetable.entity.TimetableEntry;
import com.schoolsaas.timetable.repository.TimetableEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableEntryRepository timetableEntryRepository;
    private final ClassSubjectRepository classSubjectRepository;

    public List<TimetableEntry> getByClass(UUID classId, UUID yearId) {
        return timetableEntryRepository.findByClassAndYear(classId, yearId);
    }

    @Transactional
    public TimetableEntry createEntry(TimetableEntry entry) {
        ClassSubject cs = classSubjectRepository.findById(entry.getClassSubjectId())
                .orElseThrow(() -> BusinessException.notFound("CLASS_SUBJECT_NOT_FOUND", "Matière de classe introuvable"));

        // 1. Check Class conflict
        if (timetableEntryRepository.existsConflictForClass(cs.getClassId(), entry.getTimeSlotId(), entry.getAcademicYearId())) {
            throw BusinessException.conflict("CLASS_TIMESLOT_CONFLICT", "La classe est déjà occupée sur ce créneau");
        }

        // 2. Check Teacher conflict
        if (cs.getTeacher() != null && timetableEntryRepository.existsConflictForTeacher(cs.getTeacher().getId(), entry.getTimeSlotId(), entry.getAcademicYearId())) {
            throw BusinessException.conflict("TEACHER_TIMESLOT_CONFLICT", "L'enseignant est déjà occupé sur ce créneau");
        }
        
        return timetableEntryRepository.save(entry);
    }
}
