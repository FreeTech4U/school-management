package com.schoolsaas.communication.controller;

import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.communication.entity.SmsLog;
import com.schoolsaas.communication.entity.SmsTemplate;
import com.schoolsaas.communication.repository.SmsLogRepository;
import com.schoolsaas.communication.repository.SmsTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsTemplateRepository templateRepository;
    private final SmsLogRepository smsLogRepository;

    @GetMapping("/templates")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<List<SmsTemplate>> getAllTemplates() {
        return ApiResponse.ok(templateRepository.findAll());
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<SmsTemplate> createTemplate(@RequestBody SmsTemplate template) {
        return ApiResponse.ok(templateRepository.save(template));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ACCOUNTANT')")
    public ApiResponse<List<SmsLog>> getAllLogs(@RequestParam(required = false) UUID studentId) {
        if (studentId != null) {
            return ApiResponse.ok(smsLogRepository.findByStudentId(studentId));
        }
        return ApiResponse.ok(smsLogRepository.findAll());
    }
}
