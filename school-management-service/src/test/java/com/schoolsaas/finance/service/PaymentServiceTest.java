package com.schoolsaas.finance.service;

import com.schoolsaas.common.enums.FeeStatus;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.finance.dto.request.CreatePaymentRequest;
import com.schoolsaas.finance.dto.response.PaymentResponse;
import com.schoolsaas.finance.entity.FeeStructure;
import com.schoolsaas.finance.entity.Payment;
import com.schoolsaas.finance.entity.StudentFee;
import com.schoolsaas.finance.repository.PaymentRepository;
import com.schoolsaas.finance.repository.StudentFeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StudentFeeRepository studentFeeRepository;

    @InjectMocks
    private PaymentService paymentService;

    private UUID studentId;
    private UUID feeId;
    private StudentFee studentFee;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        feeId = UUID.randomUUID();
        
        FeeStructure structure = new FeeStructure();
        structure.setLabel("Tuition");
        
        studentFee = new StudentFee();
        studentFee.setId(feeId);
        studentFee.setAmountDue(new BigDecimal("1000"));
        studentFee.setDiscountAmount(BigDecimal.ZERO);
        studentFee.setAmountPaid(BigDecimal.ZERO);
        studentFee.setStatus(FeeStatus.UNPAID);
        studentFee.setFeeStructure(structure);
    }

    @Test
    void createPayment_Success() {
        // Given
        CreatePaymentRequest.AllocationRequest alloc = new CreatePaymentRequest.AllocationRequest();
        alloc.setStudentFeeId(feeId);
        alloc.setAmount(new BigDecimal("1000"));

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setStudentId(studentId);
        request.setAmount(new BigDecimal("1000"));
        request.setPaymentDate(LocalDate.now());
        request.setPaymentMethod("CASH");
        request.setAllocations(List.of(alloc));

        when(studentFeeRepository.findById(feeId)).thenReturn(Optional.of(studentFee));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        // When
        PaymentResponse response = paymentService.createPayment(request);

        // Then
        assertNotNull(response);
        assertEquals(new BigDecimal("1000"), response.getAmount());
        assertEquals(1, response.getAllocations().size());
        verify(studentFeeRepository).save(studentFee);
        assertEquals(new BigDecimal("1000"), studentFee.getAmountPaid());
    }

    @Test
    void createPayment_MismatchAmount_ThrowsException() {
        // Given
        CreatePaymentRequest.AllocationRequest alloc = new CreatePaymentRequest.AllocationRequest();
        alloc.setStudentFeeId(feeId);
        alloc.setAmount(new BigDecimal("500"));

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(new BigDecimal("1000"));
        request.setAllocations(List.of(alloc));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () -> paymentService.createPayment(request));
        assertEquals("ALLOCATION_MISMATCH", ex.getCode());
    }

    @Test
    void createPayment_FeeAlreadyPaid_ThrowsException() {
        // Given
        studentFee.setStatus(FeeStatus.PAID);
        
        CreatePaymentRequest.AllocationRequest alloc = new CreatePaymentRequest.AllocationRequest();
        alloc.setStudentFeeId(feeId);
        alloc.setAmount(new BigDecimal("1000"));

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setStudentId(studentId);
        request.setAmount(new BigDecimal("1000"));
        request.setPaymentDate(LocalDate.now());
        request.setPaymentMethod("CASH");
        request.setAllocations(List.of(alloc));

        when(studentFeeRepository.findById(feeId)).thenReturn(Optional.of(studentFee));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () -> paymentService.createPayment(request));
        assertEquals("FEE_ALREADY_PAID", ex.getCode());
    }
}
