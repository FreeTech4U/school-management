package com.schoolsaas.timetable.repository;

import com.schoolsaas.timetable.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, UUID> {
    
    @Query("SELECT e FROM TimetableEntry e JOIN ClassSubject cs ON e.classSubjectId = cs.id " +
           "WHERE cs.schoolClass.id = :classId AND e.academicYearId = :yearId AND e.isActive = true")
    List<TimetableEntry> findByClassAndYear(@Param("classId") UUID classId, @Param("yearId") UUID yearId);

    @Query("SELECT e FROM TimetableEntry e JOIN ClassSubject cs ON e.classSubjectId = cs.id " +
           "WHERE cs.teacherId = :teacherId AND e.academicYearId = :yearId AND e.isActive = true")
    List<TimetableEntry> findByTeacherAndYear(@Param("teacherId") UUID teacherId, @Param("yearId") UUID yearId);
    
    @Query("SELECT COUNT(e) > 0 FROM TimetableEntry e JOIN ClassSubject cs ON e.classSubjectId = cs.id " +
           "WHERE cs.schoolClass.id = :classId AND e.timeSlot.id = :timeSlotId AND e.academicYearId = :yearId AND e.isActive = true")
    boolean existsConflictForClass(@Param("classId") UUID classId, @Param("timeSlotId") UUID timeSlotId, @Param("yearId") UUID yearId);

    @Query("SELECT COUNT(e) > 0 FROM TimetableEntry e JOIN ClassSubject cs ON e.classSubjectId = cs.id " +
           "WHERE cs.teacherId = :teacherId AND e.timeSlot.id = :timeSlotId AND e.academicYearId = :yearId AND e.isActive = true")
    boolean existsConflictForTeacher(@Param("teacherId") UUID teacherId, @Param("timeSlotId") UUID timeSlotId, @Param("yearId") UUID yearId);
}
