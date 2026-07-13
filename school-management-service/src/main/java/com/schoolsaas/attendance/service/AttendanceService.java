package com.schoolsaas.attendance.service;

import com.schoolsaas.attendance.entity.Attendance;
import com.schoolsaas.attendance.repository.AttendanceRepository;
import com.schoolsaas.common.enums.AttendanceStatus;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.communication.service.SmsService;
import com.schoolsaas.enrollment.entity.Student;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.enrollment.repository.StudentEnrollmentRepository;
import com.schoolsaas.enrollment.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final SmsService smsService;

    @Transactional
    public Attendance recordAttendance(Attendance attendance) {
        if (attendanceRepository.existsByEnrollmentIdAndDateAndPeriod(
                attendance.getEnrollmentId(), attendance.getDate(), attendance.getPeriod())) {
            throw BusinessException.conflict("ATTENDANCE_ALREADY_RECORDED", "La présence est déjà enregistrée pour cet élève ce jour");
        }

        Attendance saved = attendanceRepository.save(attendance);

        if (AttendanceStatus.ABSENT.equals(attendance.getStatus())) {
            sendAbsenceNotification(attendance);
        }

        return saved;
    }

    private void sendAbsenceNotification(Attendance attendance) {
        StudentEnrollment enrollment = enrollmentRepository.findById(attendance.getEnrollmentId()).orElse(null);
        if (enrollment == null) return;

        Student student = studentRepository.findById(enrollment.getStudent().getId()).orElse(null);
        if (student == null || student.getParentPhone() == null) return;

        Map<String, String> variables = new HashMap<>();
        variables.put("parent_name", student.getParentName() != null ? student.getParentName() : "Parent");
        variables.put("student_name", student.getFirstName() + " " + student.getLastName());
        variables.put("date", attendance.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        smsService.sendTemplatedSms(student.getId(), student.getParentPhone(), "absence_notification", variables);
    }
}
