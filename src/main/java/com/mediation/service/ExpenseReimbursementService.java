package com.mediation.service;

import com.mediation.common.BusinessException;
import com.mediation.dto.ExpenseReimbursementDTO;
import com.mediation.dto.ReimbursementAuditDTO;
import com.mediation.entity.Dispute;
import com.mediation.entity.ExpenseItem;
import com.mediation.entity.ExpenseItem.ExpenseType;
import com.mediation.entity.ExpenseReimbursement;
import com.mediation.entity.ExpenseReimbursement.ReimbursementStatus;
import com.mediation.entity.Mediator;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.ExpenseItemRepository;
import com.mediation.repository.ExpenseReimbursementRepository;
import com.mediation.repository.MediatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExpenseReimbursementService {

    private final ExpenseReimbursementRepository expenseReimbursementRepository;
    private final ExpenseItemRepository expenseItemRepository;
    private final DisputeRepository disputeRepository;
    private final MediatorRepository mediatorRepository;

    private static final BigDecimal MAX_AMOUNT_PER_CASE = new BigDecimal("200");

    @Transactional
    public ExpenseReimbursement create(ExpenseReimbursementDTO dto) {
        Dispute dispute = disputeRepository.findById(dto.getDisputeId())
                .orElseThrow(() -> new BusinessException("案件不存在"));

        Mediator mediator = mediatorRepository.findById(dto.getMediatorId())
                .orElseThrow(() -> new BusinessException("调解员不存在"));

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ExpenseReimbursementDTO.ExpenseItemDTO itemDTO : dto.getItems()) {
            totalAmount = totalAmount.add(itemDTO.getAmount());
        }

        if (totalAmount.compareTo(MAX_AMOUNT_PER_CASE) > 0) {
            throw new BusinessException("每案报销上限为200元，当前申请金额: " + totalAmount);
        }

        ExpenseReimbursement reimbursement = ExpenseReimbursement.builder()
                .reimbursementNo(generateReimbursementNo())
                .disputeId(dto.getDisputeId())
                .mediatorId(dto.getMediatorId())
                .totalAmount(totalAmount)
                .remark(dto.getRemark())
                .status(ReimbursementStatus.草稿)
                .build();

        reimbursement = expenseReimbursementRepository.save(reimbursement);

        List<ExpenseItem> items = new ArrayList<>();
        for (ExpenseReimbursementDTO.ExpenseItemDTO itemDTO : dto.getItems()) {
            ExpenseType type;
            try {
                type = ExpenseType.valueOf(itemDTO.getExpenseType());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的费用类型: " + itemDTO.getExpenseType());
            }

            ExpenseItem item = ExpenseItem.builder()
                    .reimbursement(reimbursement)
                    .expenseType(type)
                    .amount(itemDTO.getAmount())
                    .expenseDate(itemDTO.getExpenseDate())
                    .description(itemDTO.getDescription())
                    .build();
            items.add(expenseItemRepository.save(item));
        }
        reimbursement.setItems(items);

        return reimbursement;
    }

    @Transactional
    public ExpenseReimbursement submit(Long id) {
        ExpenseReimbursement reimbursement = expenseReimbursementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("报销单不存在"));

        if (reimbursement.getStatus() != ReimbursementStatus.草稿) {
            throw new BusinessException("只有草稿状态的报销单才能提交");
        }

        reimbursement.setStatus(ReimbursementStatus.待审批);
        reimbursement.setSubmittedAt(LocalDateTime.now());
        return expenseReimbursementRepository.save(reimbursement);
    }

    public Page<ExpenseReimbursement> list(int page, int size, String status, Long mediatorId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (status != null && mediatorId != null) {
            return expenseReimbursementRepository.findByMediatorIdAndStatus(
                    mediatorId, ReimbursementStatus.valueOf(status), pageable);
        } else if (status != null) {
            return expenseReimbursementRepository.findByStatus(
                    ReimbursementStatus.valueOf(status), pageable);
        } else if (mediatorId != null) {
            return expenseReimbursementRepository.findByMediatorId(mediatorId, pageable);
        }
        return expenseReimbursementRepository.findAll(pageable);
    }

    public Map<String, Object> getDetail(Long id) {
        ExpenseReimbursement reimbursement = expenseReimbursementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("报销单不存在"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reimbursement", reimbursement);
        result.put("items", expenseItemRepository.findByReimbursementId(id));

        disputeRepository.findById(reimbursement.getDisputeId()).ifPresent(dispute -> {
            result.put("dispute", Map.of(
                    "id", dispute.getId(),
                    "caseNo", dispute.getCaseNo(),
                    "disputeType", dispute.getDisputeType()
            ));
        });

        mediatorRepository.findById(reimbursement.getMediatorId()).ifPresent(mediator -> {
            result.put("mediator", Map.of(
                    "id", mediator.getId(),
                    "name", mediator.getName()
            ));
        });

        return result;
    }

    @Transactional
    public ExpenseReimbursement adminAudit(Long id, ReimbursementAuditDTO dto) {
        ExpenseReimbursement reimbursement = expenseReimbursementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("报销单不存在"));

        if (reimbursement.getStatus() != ReimbursementStatus.待审批) {
            throw new BusinessException("只有待审批状态的报销单才能审核");
        }

        if (Boolean.TRUE.equals(dto.getApproved())) {
            reimbursement.setStatus(ReimbursementStatus.待财务确认);
        } else {
            if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
                throw new BusinessException("驳回原因不能为空");
            }
            reimbursement.setStatus(ReimbursementStatus.管理员驳回);
            reimbursement.setAdminRejectReason(dto.getReason());
        }

        reimbursement.setAdminApprover(dto.getApprover());
        reimbursement.setAdminApprovedAt(LocalDateTime.now());
        return expenseReimbursementRepository.save(reimbursement);
    }

    @Transactional
    public ExpenseReimbursement financeConfirm(Long id, ReimbursementAuditDTO dto) {
        ExpenseReimbursement reimbursement = expenseReimbursementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("报销单不存在"));

        if (reimbursement.getStatus() != ReimbursementStatus.待财务确认) {
            throw new BusinessException("只有待财务确认状态的报销单才能确认");
        }

        if (Boolean.TRUE.equals(dto.getApproved())) {
            reimbursement.setStatus(ReimbursementStatus.已报销);
        } else {
            if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
                throw new BusinessException("驳回原因不能为空");
            }
            reimbursement.setStatus(ReimbursementStatus.财务驳回);
        }

        reimbursement.setFinanceConfirmer(dto.getApprover());
        reimbursement.setFinanceConfirmedAt(LocalDateTime.now());
        return expenseReimbursementRepository.save(reimbursement);
    }

    private String generateReimbursementNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "BX" + date + random;
    }
}
