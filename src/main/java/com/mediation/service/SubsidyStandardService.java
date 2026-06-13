package com.mediation.service;

import com.mediation.common.BusinessException;
import com.mediation.dto.SubsidyStandardDTO;
import com.mediation.entity.Dispute;
import com.mediation.entity.MediationRecord;
import com.mediation.entity.SubsidyStandard;
import com.mediation.entity.SubsidyStandard.CaseLevel;
import com.mediation.repository.MediationRecordRepository;
import com.mediation.repository.SubsidyStandardRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubsidyStandardService {

    private final SubsidyStandardRepository subsidyStandardRepository;
    private final MediationRecordRepository mediationRecordRepository;

    @PostConstruct
    @Transactional
    public void initDefaultStandards() {
        if (!subsidyStandardRepository.existsByCaseLevel(CaseLevel.简易纠纷)) {
            subsidyStandardRepository.save(SubsidyStandard.builder()
                    .caseLevel(CaseLevel.简易纠纷)
                    .amount(new BigDecimal("200"))
                    .description("调解1次即结案")
                    .minMediationCount(1)
                    .maxMediationCount(1)
                    .build());
        }
        if (!subsidyStandardRepository.existsByCaseLevel(CaseLevel.一般纠纷)) {
            subsidyStandardRepository.save(SubsidyStandard.builder()
                    .caseLevel(CaseLevel.一般纠纷)
                    .amount(new BigDecimal("400"))
                    .description("调解2-3次结案")
                    .minMediationCount(2)
                    .maxMediationCount(3)
                    .build());
        }
        if (!subsidyStandardRepository.existsByCaseLevel(CaseLevel.复杂纠纷)) {
            subsidyStandardRepository.save(SubsidyStandard.builder()
                    .caseLevel(CaseLevel.复杂纠纷)
                    .amount(new BigDecimal("600"))
                    .description("调解4次以上或涉及金额超10万")
                    .minMediationCount(4)
                    .minAmount(new BigDecimal("100000"))
                    .build());
        }
        if (!subsidyStandardRepository.existsByCaseLevel(CaseLevel.集体纠纷)) {
            subsidyStandardRepository.save(SubsidyStandard.builder()
                    .caseLevel(CaseLevel.集体纠纷)
                    .amount(new BigDecimal("800"))
                    .description("涉及人数大于10人")
                    .minInvolvedPeople(11)
                    .build());
        }
    }

    public List<SubsidyStandard> listAll() {
        return subsidyStandardRepository.findAll();
    }

    public SubsidyStandard getById(Long id) {
        return subsidyStandardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("补贴标准不存在"));
    }

    public SubsidyStandard getByCaseLevel(CaseLevel caseLevel) {
        return subsidyStandardRepository.findByCaseLevel(caseLevel)
                .orElseThrow(() -> new BusinessException("补贴标准不存在"));
    }

    @Transactional
    public SubsidyStandard update(Long id, SubsidyStandardDTO dto) {
        SubsidyStandard standard = getById(id);
        if (dto.getAmount() != null) {
            standard.setAmount(dto.getAmount());
        }
        if (dto.getDescription() != null) {
            standard.setDescription(dto.getDescription());
        }
        standard.setMinMediationCount(dto.getMinMediationCount());
        standard.setMaxMediationCount(dto.getMaxMediationCount());
        standard.setMinInvolvedPeople(dto.getMinInvolvedPeople());
        standard.setMinAmount(dto.getMinAmount());
        return subsidyStandardRepository.save(standard);
    }

    public CaseLevel determineCaseLevel(Dispute dispute) {
        List<MediationRecord> records = mediationRecordRepository.findByDisputeId(dispute.getId());
        int mediationCount = records.size();
        int involvedPeople = dispute.getInvolvedPeopleCount() != null ? dispute.getInvolvedPeopleCount() : 0;
        BigDecimal amount = dispute.getAmount() != null ? dispute.getAmount() : BigDecimal.ZERO;

        SubsidyStandard collectiveStandard = getByCaseLevel(CaseLevel.集体纠纷);
        if (collectiveStandard.getMinInvolvedPeople() != null
                && involvedPeople >= collectiveStandard.getMinInvolvedPeople()) {
            return CaseLevel.集体纠纷;
        }

        SubsidyStandard complexStandard = getByCaseLevel(CaseLevel.复杂纠纷);
        if ((complexStandard.getMinMediationCount() != null && mediationCount >= complexStandard.getMinMediationCount())
                || (complexStandard.getMinAmount() != null && amount.compareTo(complexStandard.getMinAmount()) >= 0)) {
            return CaseLevel.复杂纠纷;
        }

        SubsidyStandard generalStandard = getByCaseLevel(CaseLevel.一般纠纷);
        if (generalStandard.getMinMediationCount() != null && generalStandard.getMaxMediationCount() != null
                && mediationCount >= generalStandard.getMinMediationCount()
                && mediationCount <= generalStandard.getMaxMediationCount()) {
            return CaseLevel.一般纠纷;
        }

        return CaseLevel.简易纠纷;
    }
}
