package com.schoolsaas.finance.service;

import com.schoolsaas.common.enums.FeeStatus;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.finance.entity.FeeStructure;
import com.schoolsaas.finance.entity.StudentFee;
import com.schoolsaas.finance.repository.FeeStructureRepository;
import com.schoolsaas.finance.repository.StudentFeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentFeeService {

    private final StudentFeeRepository studentFeeRepository;
    private final FeeStructureRepository feeStructureRepository;

    @Transactional
    public void generateFeesForEnrollment(StudentEnrollment enrollment) {
        log.info("Generating fees for enrollment: {}", enrollment.getId());
        
        List<FeeStructure> structures = feeStructureRepository.findByAcademicYearId(enrollment.getAcademicYearId());
        
        List<StudentFee> studentFees = structures.stream()
                .filter(fs -> fs.getClassId() == null || fs.getClassId().equals(enrollment.getClassId()))
                .map(fs -> StudentFee.builder()
                        .enrollmentId(enrollment.getId())
                        .feeStructure(fs)
                        .amountDue(fs.getAmount())
                        .amountPaid(BigDecimal.ZERO)
                        .discountAmount(BigDecimal.ZERO)
                        .dueDate(fs.getDueDate())
                        .status(FeeStatus.UNPAID)
                        .build())
                .collect(Collectors.toList());
        
        studentFeeRepository.saveAll(studentFees);
    }

    public List<StudentFee> getFeesByEnrollment(UUID enrollmentId) {
        return studentFeeRepository.findByEnrollmentId(enrollmentId);
    }

    @Transactional
    public void applyDiscount(UUID feeId, BigDecimal discountAmount, String reason) {
        StudentFee fee = studentFeeRepository.findById(feeId)
                .orElseThrow(() -> BusinessException.notFound("FEE_NOT_FOUND", "Frais introuvable"));
        
        if (FeeStatus.PAID.equals(fee.getStatus())) {
            throw new BusinessException("FEE_ALREADY_PAID", "Impossible d'appliquer une remise sur un frais déjà payé");
        }
        
        fee.setDiscountAmount(discountAmount);
        fee.setDiscountReason(reason);
        
        // Status will be recalculated by trigger or we can do it here too
        BigDecimal remaining = fee.getAmountDue().subtract(discountAmount).subtract(fee.getAmountPaid());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            fee.setStatus(FeeStatus.PAID);
        }
        
        studentFeeRepository.save(fee);
    }
}
