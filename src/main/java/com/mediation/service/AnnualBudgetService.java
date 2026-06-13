package com.mediation.service;

import com.mediation.common.BusinessException;
import com.mediation.dto.AnnualBudgetDTO;
import com.mediation.entity.AnnualBudget;
import com.mediation.entity.SubsidyCalculation.AuditStatus;
import com.mediation.repository.AnnualBudgetRepository;
import com.mediation.repository.SubsidyCalculationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AnnualBudgetService {

    private final AnnualBudgetRepository annualBudgetRepository;
    private final SubsidyCalculationRepository subsidyCalculationRepository;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Transactional
    public AnnualBudget createOrUpdate(AnnualBudgetDTO dto) {
        Optional<AnnualBudget> existing = annualBudgetRepository
                .findByBudgetYearAndOrganization(dto.getBudgetYear(), dto.getOrganization());

        AnnualBudget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setTotalAmount(dto.getTotalAmount());
            if (dto.getWarningThreshold() != null) {
                budget.setWarningThreshold(dto.getWarningThreshold());
            }
            if (dto.getCriticalThreshold() != null) {
                budget.setCriticalThreshold(dto.getCriticalThreshold());
            }
        } else {
            budget = AnnualBudget.builder()
                    .budgetYear(dto.getBudgetYear())
                    .organization(dto.getOrganization())
                    .totalAmount(dto.getTotalAmount())
                    .warningThreshold(dto.getWarningThreshold() != null ? dto.getWarningThreshold() : new BigDecimal("80.00"))
                    .criticalThreshold(dto.getCriticalThreshold() != null ? dto.getCriticalThreshold() : new BigDecimal("95.00"))
                    .usedAmount(BigDecimal.ZERO)
                    .build();
        }
        return annualBudgetRepository.save(budget);
    }

    public AnnualBudget getByYearAndOrganization(Integer year, String organization) {
        return annualBudgetRepository.findByBudgetYearAndOrganization(year, organization)
                .orElseThrow(() -> new BusinessException("该年度预算不存在"));
    }

    public AnnualBudget getByYear(Integer year) {
        return annualBudgetRepository.findByBudgetYear(year)
                .orElseThrow(() -> new BusinessException("该年度预算不存在"));
    }

    public Map<String, Object> getBudgetProgress(Integer year) {
        AnnualBudget budget;
        try {
            budget = getByYear(year);
        } catch (BusinessException e) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("budgetYear", year);
            empty.put("totalAmount", BigDecimal.ZERO);
            empty.put("usedAmount", BigDecimal.ZERO);
            empty.put("remainingAmount", BigDecimal.ZERO);
            empty.put("executionRate", BigDecimal.ZERO);
            empty.put("warningLevel", "NONE");
            return empty;
        }

        BigDecimal used = subsidyCalculationRepository
                .sumAmountByAuditStatusAndYear(AuditStatus.审核通过, year);
        budget.setUsedAmount(used);

        BigDecimal remaining = budget.getTotalAmount().subtract(used);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        BigDecimal executionRate = BigDecimal.ZERO;
        if (budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            executionRate = used.multiply(ONE_HUNDRED)
                    .divide(budget.getTotalAmount(), 2, RoundingMode.HALF_UP);
        }

        String warningLevel = "NONE";
        if (executionRate.compareTo(budget.getCriticalThreshold()) >= 0) {
            warningLevel = "CRITICAL";
        } else if (executionRate.compareTo(budget.getWarningThreshold()) >= 0) {
            warningLevel = "WARNING";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("budgetYear", budget.getBudgetYear());
        result.put("organization", budget.getOrganization());
        result.put("totalAmount", budget.getTotalAmount());
        result.put("usedAmount", used);
        result.put("remainingAmount", remaining);
        result.put("executionRate", executionRate);
        result.put("warningThreshold", budget.getWarningThreshold());
        result.put("criticalThreshold", budget.getCriticalThreshold());
        result.put("warningLevel", warningLevel);
        return result;
    }

    @Transactional
    public void checkBudgetAndReserve(BigDecimal amount, boolean requireSpecialApproval) {
        int year = Year.now().getValue();
        AnnualBudget budget;
        try {
            budget = getByYear(year);
        } catch (BusinessException e) {
            throw new BusinessException("当前年度预算未设置，请先设置年度预算");
        }

        BigDecimal used = subsidyCalculationRepository
                .sumAmountByAuditStatusAndYear(AuditStatus.审核通过, year);
        BigDecimal newUsed = used.add(amount);

        BigDecimal executionRate = BigDecimal.ZERO;
        if (budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            executionRate = newUsed.multiply(ONE_HUNDRED)
                    .divide(budget.getTotalAmount(), 4, RoundingMode.HALF_UP);
        }

        if (newUsed.compareTo(budget.getTotalAmount()) > 0) {
            throw new BusinessException("预算不足，超出年度预算总额");
        }

        if (executionRate.compareTo(budget.getCriticalThreshold()) >= 0 && !requireSpecialApproval) {
            throw new BusinessException(428, "预算执行率已超过" + budget.getCriticalThreshold() + "%，需要特批才能继续核算");
        }
    }

    @Transactional
    public void deductBudget(BigDecimal amount) {
        int year = Year.now().getValue();
        AnnualBudget budget;
        try {
            budget = getByYear(year);
        } catch (BusinessException e) {
            return;
        }
        budget.setUsedAmount(budget.getUsedAmount().add(amount));
        annualBudgetRepository.save(budget);
    }

    @Transactional
    public void refundBudget(BigDecimal amount) {
        int year = Year.now().getValue();
        AnnualBudget budget;
        try {
            budget = getByYear(year);
        } catch (BusinessException e) {
            return;
        }
        BigDecimal newUsed = budget.getUsedAmount().subtract(amount);
        if (newUsed.compareTo(BigDecimal.ZERO) < 0) {
            newUsed = BigDecimal.ZERO;
        }
        budget.setUsedAmount(newUsed);
        annualBudgetRepository.save(budget);
    }
}
