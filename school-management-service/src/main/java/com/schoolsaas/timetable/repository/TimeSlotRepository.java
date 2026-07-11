package com.schoolsaas.timetable.repository;

import com.schoolsaas.timetable.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, UUID> {
}
