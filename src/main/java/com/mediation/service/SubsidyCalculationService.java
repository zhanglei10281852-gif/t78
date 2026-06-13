package com.mediation.service;

import com.mediation.common.BusinessException;
import com.mediation.dto.SubsidyCalculationAuditDTO;
import com.mediation.entity.*;
import com.mediation.entity.Dispute.DisputeStatus;
import com.mediation.entity.SubsidyCalculation.AuditStatus;
import com.mediation.entity.SubsidyStandard.CaseLevel;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.MediationRecordRepository;
import com.mediation.repository.MediatorRepository;
import com.mediation.repository.SubsidyCalculationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SubsidyCalculationService {

    private final SubsidyCalculationRepository subsidyCalculationRepository;
    private final DisputeRepository disputeRepository;
    private final MediatorRepository mediatorRepository;
    private final MediationRecordRepository mediationRecordRepository;
    private final SubsidyStandardService subsidyStandardService;
    private final AnnualBudgetService annualBudgetService;

    @Transactional
    public SubsidyCalculation calculate(Long disputeId) {
        if (subsidyCalculationRepository.existsByDisputeId(disputeId)) {
            throw new BusinessException("该案件已生成补贴核算单");
        }

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new BusinessException("案件不存在"));

        if (dispute.getStatus() != DisputeStatus.调解成功 && dispute.getStatus() != DisputeStatus.调解失败) {
            throw new BusinessException("只有已结案的案件才能核算补贴");
        }

        if (dispute.getMediatorId() == null) {
            throw new BusinessException("该案件未分配调解员");
        }

        Mediator mediator = mediatorRepository.findById(dispute.getMediatorId())
                .orElseThrow(() -> new BusinessException("调解员不存在"));

        CaseLevel caseLevel = subsidyStandardService.determineCaseLevel(dispute);
        SubsidyStandard standard = subsidyStandardService.getByCaseLevel(caseLevel);

        long mediationCount = mediationRecordRepository.findByDisputeId(disputeId).size();

        SubsidyCalculation calculation = SubsidyCalculation.builder()
                .calculationNo(generateCalculationNo())
                .disputeId(disputeId)
                .mediatorId(dispute.getMediatorId())
                .caseLevel(caseLevel)
                .amount(standard.getAmount())
                .mediationCount((int) mediationCount)
                .calculationDate(LocalDate.now())
                .auditStatus(AuditStatus.待审核)
                .build();

        return subsidyCalculationRepository.save(calculation);
    }

    public Page<SubsidyCalculation> list(int page, int size, String auditStatus, Long mediatorId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (auditStatus != null) {
            return subsidyCalculationRepository.findByAuditStatus(AuditStatus.valueOf(auditStatus), pageable);
        } else if (mediatorId != null) {
            return subsidyCalculationRepository.findByMediatorId(mediatorId, pageable);
        }
        return subsidyCalculationRepository.findAll(pageable);
    }

    public Map<String, Object> getDetail(Long id) {
        SubsidyCalculation calc = subsidyCalculationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("核算单不存在"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("calculation", calc);

        disputeRepository.findById(calc.getDisputeId()).ifPresent(dispute -> {
            result.put("dispute", Map.of(
                    "id", dispute.getId(),
                    "caseNo", dispute.getCaseNo(),
                    "disputeType", dispute.getDisputeType(),
                    "applicantName", dispute.getApplicantName(),
                    "respondentName", dispute.getRespondentName(),
                    "amount", dispute.getAmount(),
                    "involvedPeopleCount", dispute.getInvolvedPeopleCount(),
                    "status", dispute.getStatus()
            ));
        });

        mediatorRepository.findById(calc.getMediatorId()).ifPresent(mediator -> {
            result.put("mediator", Map.of(
                    "id", mediator.getId(),
                    "name", mediator.getName(),
                    "organization", mediator.getOrganization()
            ));
        });

        return result;
    }

    @Transactional
    public SubsidyCalculation audit(Long id, SubsidyCalculationAuditDTO dto) {
        SubsidyCalculation calc = subsidyCalculationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("核算单不存在"));

        if (calc.getAuditStatus() != AuditStatus.待审核) {
            throw new BusinessException("该核算单已审核，无法重复审核");
        }

        if (Boolean.TRUE.equals(dto.getApproved())) {
            annualBudgetService.checkBudgetAndReserve(calc.getAmount(), false);
            calc.setAuditStatus(AuditStatus.审核通过);
            annualBudgetService.deductBudget(calc.getAmount());
        } else {
            if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
                throw new BusinessException("驳回原因不能为空");
            }
            calc.setAuditStatus(AuditStatus.审核驳回);
            calc.setAuditReason(dto.getReason());
        }

        calc.setAuditedBy(dto.getAuditedBy());
        calc.setAuditedAt(LocalDateTime.now());

        return subsidyCalculationRepository.save(calc);
    }

    @Transactional
    public SubsidyCalculation specialApproveAudit(Long id, SubsidyCalculationAuditDTO dto) {
        SubsidyCalculation calc = subsidyCalculationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("核算单不存在"));

        if (calc.getAuditStatus() != AuditStatus.待审核) {
            throw new BusinessException("该核算单已审核，无法重复审核");
        }

        if (Boolean.TRUE.equals(dto.getApproved())) {
            annualBudgetService.checkBudgetAndReserve(calc.getAmount(), true);
            calc.setAuditStatus(AuditStatus.审核通过);
            annualBudgetService.deductBudget(calc.getAmount());
        } else {
            if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
                throw new BusinessException("驳回原因不能为空");
            }
            calc.setAuditStatus(AuditStatus.审核驳回);
            calc.setAuditReason(dto.getReason());
        }

        calc.setAuditedBy(dto.getAuditedBy());
        calc.setAuditedAt(LocalDateTime.now());

        return subsidyCalculationRepository.save(calc);
    }

    public List<SubsidyCalculation> getMediatorApprovedCalculations(Long mediatorId) {
        return subsidyCalculationRepository.findByMediatorIdAndAuditStatus(mediatorId, AuditStatus.审核通过);
    }

    private String generateCalculationNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "BT" + date + random;
    }
}
