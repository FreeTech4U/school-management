package com.schoolsaas.timetable.repository;

import com.schoolsaas.timetable.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, UUID> {
    
    @Query("SELECT e FROM TimetableEntry e JOIN ClassSubject cs ON e.classSubjectId = cs.id " +
           "WHERE cs.classId = :classId AND e.academicYearId = :yearId AND e.isActive = true")
    List<TimetableEntry> findByClassAndYear(UUID classId, UUID yearId);

    @Query("SELECT e FROM TimetableEntry e JOIN ClassSubject cs ON e.classSubjectId = cs.id " +
           "WHERE cs.teacherId = :teacherId AND e.academicYearId = :yearId AND e.isActive = true")
    List<TimetableEntry> findByTeacherAndYear(UUID teacherId, UUID yearId);
    
    @Query("SELECT COUNT(e) > 0 FROM TimetableEntry e JOIN ClassSubject cs ON e.classSubjectId = cs.id " +
           "WHERE cs.classId = :classId AND e.timeSlotId = :timeSlotId AND e.academicYearId = :yearId AND e.isActive = true")
    boolean existsConflictForClass(UUID classId, UUID timeSlotId, UUID yearId);

    @Query("SELECT COUNT(e) > 0 FROM TimetableEntry e JOIN ClassSubject cs ON e.classSubjectId = cs.id " +
           "WHERE cs.teacherId = :teacherId AND e.timeSlotId = :timeSlotId AND e.academicYearId = :yearId AND e.isActive = true")
    boolean existsConflictForTeacher(UUID teacherId, UUID timeSlotId, UUID yearId);
}
