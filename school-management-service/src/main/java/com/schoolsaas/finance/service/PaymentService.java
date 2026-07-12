package com.schoolsaas.finance.service;

import com.schoolsaas.common.enums.FeeStatus;
import com.schoolsaas.common.enums.PaymentMethod;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.finance.dto.request.CreatePaymentRequest;
import com.schoolsaas.finance.dto.response.PaymentResponse;
import com.schoolsaas.finance.entity.Payment;
import com.schoolsaas.finance.entity.PaymentAllocation;
import com.schoolsaas.finance.entity.StudentFee;
import com.schoolsaas.finance.repository.PaymentRepository;
import com.schoolsaas.finance.repository.StudentFeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StudentFeeRepository studentFeeRepository;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        // 1. Verify allocations sum matches total amount
        BigDecimal totalAllocated = request.getAllocations().stream()
                .map(CreatePaymentRequest.AllocationRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalAllocated.compareTo(request.getAmount()) != 0) {
            throw new BusinessException("ALLOCATION_MISMATCH", "La somme des allocations ne correspond pas au montant total");
        }

        // 2. Create Payment entity
        Payment payment = Payment.builder()
                .studentId(request.getStudentId())
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate().atStartOfDay()) // Simplification
                .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()))
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .allocations(new ArrayList<>())
                .build();

        // 3. Process Allocations
        for (CreatePaymentRequest.AllocationRequest allocReq : request.getAllocations()) {
            StudentFee fee = studentFeeRepository.findById(allocReq.getStudentFeeId())
                    .orElseThrow(() -> BusinessException.notFound("FEE_NOT_FOUND", "Frais introuvable: " + allocReq.getStudentFeeId()));

            if (FeeStatus.PAID.equals(fee.getStatus())) {
                throw new BusinessException("FEE_ALREADY_PAID", "Le frais " + fee.getFeeStructure().getLabel() + " est déjà payé");
            }

            BigDecimal remaining = fee.getAmountDue().subtract(fee.getDiscountAmount()).subtract(fee.getAmountPaid());
            if (allocReq.getAmount().compareTo(remaining) > 0) {
                throw new BusinessException("OVER_PAYMENT", "Montant alloué supérieur au restant dû pour " + fee.getFeeStructure().getLabel());
            }

            PaymentAllocation allocation = PaymentAllocation.builder()
                    .payment(payment)
                    .studentFeeId(fee.getId())
                    .amount(allocReq.getAmount())
                    .build();
            
            payment.getAllocations().add(allocation);
            
            // Note: In a real app, fee status recalculation is handled by DB trigger
            // But we might need to update amountPaid for current session if not using trigger results immediately
            fee.setAmountPaid(fee.getAmountPaid().add(allocReq.getAmount()));
            studentFeeRepository.save(fee);
        }

        payment = paymentRepository.save(payment);

        // 4. Send SMS Confirmation (TODO: CommunicationService)

        return mapToResponse(payment);
    }

    public List<PaymentResponse> getPaymentsByStudent(UUID studentId) {
        return paymentRepository.findByStudentId(studentId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelPayment(UUID id, String reason) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("PAYMENT_NOT_FOUND", "Paiement introuvable"));

        if (payment.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new BusinessException("CANCELLATION_WINDOW_EXPIRED", "Impossible d'annuler un paiement vieux de plus de 24h");
        }

        String cancellationNote = String.format("\n[ANNULÉ le %s] %s", LocalDateTime.now(), reason);
        payment.setNotes(payment.getNotes() == null ? cancellationNote : payment.getNotes() + cancellationNote);
        
        // Reverse allocations (In Phase 2, we would recalculate fees status)
        for (PaymentAllocation alloc : payment.getAllocations()) {
            StudentFee fee = studentFeeRepository.findById(alloc.getStudentFeeId()).orElse(null);
            if (fee != null) {
                fee.setAmountPaid(fee.getAmountPaid().subtract(alloc.getAmount()));
                studentFeeRepository.save(fee);
            }
        }
        
        paymentRepository.save(payment);
    }

    private PaymentResponse mapToResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .studentId(p.getStudentId())
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate())
                .paymentMethod(p.getPaymentMethod().name())
                .referenceNumber(p.getReferenceNumber())
                .receiptNumber(p.getReceiptNumber())
                .allocations(p.getAllocations().stream()
                        .map(a -> PaymentResponse.AllocationResponse.builder()
                                .studentFeeId(a.getStudentFeeId())
                                .amountAllocated(a.getAmount())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
