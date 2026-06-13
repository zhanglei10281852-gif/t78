package com.mediation.service;

import com.mediation.common.BusinessException;
import com.mediation.dto.BatchPaymentDTO;
import com.mediation.entity.SubsidyCalculation;
import com.mediation.entity.SubsidyCalculation.AuditStatus;
import com.mediation.entity.SubsidyPayment;
import com.mediation.entity.SubsidyPayment.PaymentMethod;
import com.mediation.repository.SubsidyCalculationRepository;
import com.mediation.repository.SubsidyPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SubsidyPaymentService {

    private final SubsidyPaymentRepository subsidyPaymentRepository;
    private final SubsidyCalculationRepository subsidyCalculationRepository;

    @Transactional
    public List<SubsidyPayment> batchPay(BatchPaymentDTO dto) {
        if (dto.getCalculationIds() == null || dto.getCalculationIds().isEmpty()) {
            throw new BusinessException("请选择要发放的核算单");
        }

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(dto.getPaymentMethod());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的发放方式");
        }

        List<SubsidyCalculation> calculations = subsidyCalculationRepository
                .findByIdsAndAuditStatus(dto.getCalculationIds(), AuditStatus.审核通过);

        if (calculations.isEmpty()) {
            throw new BusinessException("没有可发放的核算单（需审核通过且未发放）");
        }

        List<Long> paidIds = new ArrayList<>();
        for (SubsidyCalculation calc : calculations) {
            if (subsidyPaymentRepository.existsByCalculationId(calc.getId())) {
                paidIds.add(calc.getId());
            }
        }
        if (!paidIds.isEmpty()) {
            throw new BusinessException("以下核算单已发放: " + paidIds);
        }

        List<SubsidyPayment> payments = new ArrayList<>();
        for (SubsidyCalculation calc : calculations) {
            SubsidyPayment payment = SubsidyPayment.builder()
                    .paymentNo(generatePaymentNo())
                    .calculationId(calc.getId())
                    .mediatorId(calc.getMediatorId())
                    .paymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDate.now())
                    .paymentMethod(paymentMethod)
                    .amount(calc.getAmount())
                    .createdBy(dto.getCreatedBy())
                    .remark(dto.getRemark())
                    .build();
            payments.add(subsidyPaymentRepository.save(payment));
        }

        return payments;
    }

    public Page<SubsidyPayment> list(int page, int size, Long mediatorId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (mediatorId != null) {
            return subsidyPaymentRepository.findByMediatorId(mediatorId, pageable);
        }
        return subsidyPaymentRepository.findAll(pageable);
    }

    public Map<String, Object> getMediatorIncomeSummary(Long mediatorId) {
        Map<String, Object> result = new LinkedHashMap<>();

        BigDecimal totalIncome = subsidyPaymentRepository.sumAmountByMediatorId(mediatorId);
        result.put("totalIncome", totalIncome);

        int currentYear = LocalDate.now().getYear();
        BigDecimal yearIncome = subsidyPaymentRepository.sumAmountByYear(currentYear);
        result.put("yearIncome", yearIncome != null ? yearIncome : BigDecimal.ZERO);

        List<SubsidyPayment> paymentList = subsidyPaymentRepository.findByMediatorId(mediatorId);
        result.put("paymentCount", paymentList.size());
        result.put("paymentDetails", paymentList);

        return result;
    }

    public List<Map<String, Object>> getMediatorIncomeDetails(Long mediatorId) {
        List<SubsidyPayment> payments = subsidyPaymentRepository.findByMediatorId(mediatorId);
        List<Map<String, Object>> details = new ArrayList<>();

        for (SubsidyPayment payment : payments) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("paymentNo", payment.getPaymentNo());
            item.put("calculationId", payment.getCalculationId());
            item.put("amount", payment.getAmount());
            item.put("paymentDate", payment.getPaymentDate());
            item.put("paymentMethod", payment.getPaymentMethod());
            item.put("bankTransferNo", payment.getBankTransferNo());
            item.put("receiptNo", payment.getReceiptNo());
            item.put("remark", payment.getRemark());

            subsidyCalculationRepository.findById(payment.getCalculationId())
                    .ifPresent(calc -> {
                        item.put("caseLevel", calc.getCaseLevel());
                        item.put("disputeId", calc.getDisputeId());
                    });

            details.add(item);
        }

        details.sort((a, b) -> {
            LocalDate da = (LocalDate) a.get("paymentDate");
            LocalDate db = (LocalDate) b.get("paymentDate");
            return db.compareTo(da);
        });

        return details;
    }

    private String generatePaymentNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "FF" + date + random;
    }
}
