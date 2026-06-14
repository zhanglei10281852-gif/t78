package com.mediation.service;

import com.mediation.entity.ExpenseReimbursement.ReimbursementStatus;
import com.mediation.entity.Mediator;
import com.mediation.entity.SubsidyCalculation;
import com.mediation.entity.SubsidyCalculation.AuditStatus;
import com.mediation.entity.SubsidyStandard.CaseLevel;
import com.mediation.repository.ExpenseReimbursementRepository;
import com.mediation.repository.MediatorRepository;
import com.mediation.repository.SubsidyCalculationRepository;
import com.mediation.repository.SubsidyPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuditStatsService {

    private final SubsidyCalculationRepository subsidyCalculationRepository;
    private final SubsidyPaymentRepository subsidyPaymentRepository;
    private final ExpenseReimbursementRepository expenseReimbursementRepository;
    private final MediatorRepository mediatorRepository;
    private final AnnualBudgetService annualBudgetService;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public Map<String, Object> getMonthlyReconciliation(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        BigDecimal calculationTotal = subsidyCalculationRepository
                .sumAmountByAuditStatusAndDateBetween(AuditStatus.审核通过, startDate, endDate);
        BigDecimal paymentTotal = subsidyPaymentRepository
                .sumAmountByDateBetween(startDate, endDate);
        BigDecimal reimbursementTotal = expenseReimbursementRepository
                .sumAmountByStatusAndDateBetween(ReimbursementStatus.已报销, startDate, endDate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("calculationTotal", calculationTotal);
        result.put("paymentTotal", paymentTotal);
        result.put("reimbursementTotal", reimbursementTotal);
        result.put("grandTotal", calculationTotal.add(paymentTotal).add(reimbursementTotal));
        return result;
    }

    public Map<String, Object> getAnnualAudit(int year) {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> byOrganization = new LinkedHashMap<>();
        Map<String, BigDecimal> orgSubsidyMap = new LinkedHashMap<>();
        Map<String, Integer> orgCaseCountMap = new LinkedHashMap<>();

        Map<Long, Integer> mediatorCaseCountMap = new LinkedHashMap<>();
        List<Object[]> caseCountData = subsidyCalculationRepository
                .monthlyCaseCountByMediator(AuditStatus.审核通过, year);
        for (Object[] row : caseCountData) {
            Long mediatorId = (Long) row[0];
            long count = (Long) row[2];
            mediatorCaseCountMap.merge(mediatorId, (int) count, Integer::sum);
        }

        List<Object[]> mediatorYearStats = subsidyCalculationRepository
                .sumAmountByMediatorAndYear(AuditStatus.审核通过, year);

        for (Object[] row : mediatorYearStats) {
            Long mediatorId = (Long) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            int caseCount = mediatorCaseCountMap.getOrDefault(mediatorId, 0);
            mediatorRepository.findById(mediatorId).ifPresent(m -> {
                String org = m.getOrganization();
                orgSubsidyMap.merge(org, amount, BigDecimal::add);
                orgCaseCountMap.merge(org, caseCount, Integer::sum);
            });
        }

        for (Map.Entry<String, BigDecimal> entry : orgSubsidyMap.entrySet()) {
            Map<String, Object> orgData = new LinkedHashMap<>();
            orgData.put("totalSubsidy", entry.getValue());
            orgData.put("caseCount", orgCaseCountMap.getOrDefault(entry.getKey(), 0));
            byOrganization.put(entry.getKey(), orgData);
        }
        result.put("byOrganization", byOrganization);

        List<Map<String, Object>> mediatorRanking = new ArrayList<>();
        for (Object[] row : mediatorYearStats) {
            Long mediatorId = (Long) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("mediatorId", mediatorId);
            m.put("totalSubsidy", amount);
            mediatorRepository.findById(mediatorId).ifPresent(mediator -> {
                m.put("mediatorName", mediator.getName());
                m.put("organization", mediator.getOrganization());
            });
            mediatorRanking.add(m);
        }
        result.put("mediatorRanking", mediatorRanking);

        result.put("abnormalMediators", detectAbnormalMediators(year));

        return result;
    }

    public List<Map<String, Object>> detectAbnormalMediators(int year) {
        List<Object[]> rawData = subsidyCalculationRepository
                .monthlyCaseCountByMediator(AuditStatus.审核通过, year);

        Map<Long, List<Integer>> mediatorMonthlyCounts = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            Long mediatorId = (Long) row[0];
            int month = (Integer) row[1];
            long count = (Long) row[2];
            mediatorMonthlyCounts.computeIfAbsent(mediatorId, k -> new ArrayList<>(Collections.nCopies(12, 0)))
                    .set(month - 1, (int) count);
        }

        List<Map<String, Object>> abnormal = new ArrayList<>();
        for (Map.Entry<Long, List<Integer>> entry : mediatorMonthlyCounts.entrySet()) {
            Long mediatorId = entry.getKey();
            List<Integer> counts = entry.getValue();

            int totalCases = counts.stream().mapToInt(Integer::intValue).sum();
            long nonZeroMonths = counts.stream().filter(c -> c > 0).count();

            if (totalCases < 3 || nonZeroMonths < 3) {
                continue;
            }

            double mean = counts.stream().mapToInt(Integer::intValue).average().orElse(0);
            double variance = counts.stream()
                    .mapToDouble(c -> Math.pow(c - mean, 2))
                    .average().orElse(0);
            double stdDev = Math.sqrt(variance);
            double threshold = mean + 3 * stdDev;

            for (int i = 0; i < counts.size(); i++) {
                if (stdDev > 0 && counts.get(i) > threshold && counts.get(i) >= 2) {
                    Map<String, Object> ab = new LinkedHashMap<>();
                    ab.put("mediatorId", mediatorId);
                    mediatorRepository.findById(mediatorId).ifPresent(m -> {
                        ab.put("mediatorName", m.getName());
                        ab.put("organization", m.getOrganization());
                    });
                    ab.put("month", i + 1);
                    ab.put("caseCount", counts.get(i));
                    ab.put("mean", round(mean, 2));
                    ab.put("stdDev", round(stdDev, 2));
                    ab.put("threshold", round(threshold, 2));
                    ab.put("totalCases", totalCases);
                    ab.put("nonZeroMonths", nonZeroMonths);
                    abnormal.add(ab);
                }
            }
        }
        return abnormal;
    }

    public Map<String, Object> getMonthlySubsidyTrend(int year) {
        List<Object[]> data = subsidyCalculationRepository.monthlyTrendByYear(AuditStatus.审核通过, year);
        Map<String, Object> result = new LinkedHashMap<>();
        BigDecimal[] monthly = new BigDecimal[12];
        Arrays.fill(monthly, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;

        for (Object[] row : data) {
            int month = (Integer) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            monthly[month - 1] = amount;
            total = total.add(amount);
        }

        result.put("year", year);
        result.put("total", total);
        result.put("monthlyData", monthly);
        return result;
    }

    public Map<String, Object> getCaseLevelDistribution(int year) {
        List<Object[]> data = subsidyCalculationRepository.statsByCaseLevelAndYear(AuditStatus.审核通过, year);
        Map<String, Object> result = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        Map<String, Map<String, Object>> levelData = new LinkedHashMap<>();

        for (CaseLevel level : CaseLevel.values()) {
            levelData.put(level.name(), null);
        }

        for (Object[] row : data) {
            CaseLevel level = (CaseLevel) row[0];
            long count = (Long) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            total = total.add(amount);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("caseLevel", level);
            item.put("count", count);
            item.put("amount", amount);
            levelData.put(level.name(), item);
        }

        for (Map.Entry<String, Map<String, Object>> entry : levelData.entrySet()) {
            if (entry.getValue() != null && total.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal amount = (BigDecimal) entry.getValue().get("amount");
                BigDecimal ratio = amount.multiply(ONE_HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
                entry.getValue().put("ratio", ratio);
            } else if (entry.getValue() == null) {
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("caseLevel", CaseLevel.valueOf(entry.getKey()));
                empty.put("count", 0);
                empty.put("amount", BigDecimal.ZERO);
                empty.put("ratio", BigDecimal.ZERO);
                levelData.put(entry.getKey(), empty);
            }
        }

        result.put("year", year);
        result.put("totalAmount", total);
        result.put("byLevel", levelData);
        return result;
    }

    public Map<String, Object> getMediatorAnnualRanking(int year, int topN) {
        List<Object[]> data = subsidyPaymentRepository.sumAmountByMediatorAndYear(year);
        List<Map<String, Object>> ranking = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int rank = 1;

        for (Object[] row : data) {
            if (ranking.size() >= topN) break;
            Long mediatorId = (Long) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            total = total.add(amount);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("mediatorId", mediatorId);
            item.put("amount", amount);
            mediatorRepository.findById(mediatorId).ifPresent(m -> {
                item.put("mediatorName", m.getName());
                item.put("organization", m.getOrganization());
            });
            ranking.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("topN", topN);
        result.put("totalSubsidy", total);
        result.put("ranking", ranking);
        return result;
    }

    public Map<String, Object> getBudgetExecutionCurve(int year) {
        Map<String, Object> budgetProgress = annualBudgetService.getBudgetProgress(year);
        List<Object[]> paymentTrend = subsidyPaymentRepository.monthlyTrendByYear(year);

        BigDecimal totalBudget = (BigDecimal) budgetProgress.getOrDefault("totalAmount", BigDecimal.ZERO);
        BigDecimal[] cumulative = new BigDecimal[12];
        BigDecimal runningTotal = BigDecimal.ZERO;
        Map<Integer, BigDecimal> monthMap = new LinkedHashMap<>();

        for (Object[] row : paymentTrend) {
            int month = (Integer) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            monthMap.put(month, amount);
        }

        for (int i = 0; i < 12; i++) {
            runningTotal = runningTotal.add(monthMap.getOrDefault(i + 1, BigDecimal.ZERO));
            cumulative[i] = runningTotal;
        }

        BigDecimal[] executionRates = new BigDecimal[12];
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            for (int i = 0; i < 12; i++) {
                executionRates[i] = cumulative[i].multiply(ONE_HUNDRED)
                        .divide(totalBudget, 2, RoundingMode.HALF_UP);
            }
        } else {
            Arrays.fill(executionRates, BigDecimal.ZERO);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("totalBudget", totalBudget);
        result.put("monthlyCumulative", cumulative);
        result.put("monthlyExecutionRate", executionRates);
        result.put("warningThreshold", budgetProgress.get("warningThreshold"));
        result.put("criticalThreshold", budgetProgress.get("criticalThreshold"));
        return result;
    }

    public Map<String, Object> getAverageSubsidyPerMediator(int year) {
        List<Object[]> mediatorStats = subsidyPaymentRepository.sumAmountByMediatorAndYear(year);
        long mediatorCount = mediatorRepository.count();
        BigDecimal totalSubsidy = subsidyPaymentRepository.sumAmountByYear(year);

        BigDecimal avgPerMediator = BigDecimal.ZERO;
        BigDecimal avgPerActiveMediator = BigDecimal.ZERO;

        if (mediatorCount > 0) {
            avgPerMediator = totalSubsidy.divide(BigDecimal.valueOf(mediatorCount), 2, RoundingMode.HALF_UP);
        }
        if (!mediatorStats.isEmpty()) {
            avgPerActiveMediator = totalSubsidy.divide(BigDecimal.valueOf(mediatorStats.size()), 2, RoundingMode.HALF_UP);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("totalMediators", mediatorCount);
        result.put("activeMediators", mediatorStats.size());
        result.put("totalSubsidy", totalSubsidy);
        result.put("avgPerMediator", avgPerMediator);
        result.put("avgPerActiveMediator", avgPerActiveMediator);
        return result;
    }

    private double round(double value, int places) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
