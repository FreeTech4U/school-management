package com.schoolsaas.finance.service;

import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.finance.entity.FeeStructure;
import com.schoolsaas.finance.repository.FeeStructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeeStructureService {

    private final FeeStructureRepository feeStructureRepository;

    public List<FeeStructure> getAllFeeStructures() {
        return feeStructureRepository.findAll();
    }

    public List<FeeStructure> getByYear(UUID yearId) {
        return feeStructureRepository.findByAcademicYearId(yearId);
    }

    @Transactional
    public FeeStructure createFeeStructure(FeeStructure feeStructure) {
        return feeStructureRepository.save(feeStructure);
    }

    @Transactional
    public FeeStructure updateFeeStructure(UUID id, FeeStructure details) {
        FeeStructure fs = feeStructureRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("FEE_STRUCTURE_NOT_FOUND", "Structure de frais introuvable"));
        
        fs.setLabel(details.getLabel());
        fs.setAmount(details.getAmount());
        fs.setDueDate(details.getDueDate());
        fs.setInstallmentsAllowed(details.getInstallmentsAllowed());
        fs.setMaxInstallments(details.getMaxInstallments());
        
        return feeStructureRepository.save(fs);
    }

    @Transactional
    public void deleteFeeStructure(UUID id) {
        feeStructureRepository.deleteById(id);
    }
}
