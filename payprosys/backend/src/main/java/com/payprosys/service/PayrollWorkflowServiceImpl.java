package com.payprosys.service;

import com.payprosys.dto.PayrollBatchDto;
import com.payprosys.dto.PayrollReviewEventDto;
import com.payprosys.dto.WorkflowActionHintsDto;
import com.payprosys.dto.WorkflowAppendRemarkRequest;
import com.payprosys.dto.WorkflowApproveRequest;
import com.payprosys.dto.WorkflowRemarksRequest;
import com.payprosys.dto.WorkflowSendToBankRequest;
import com.payprosys.entity.BankFlowState;
import com.payprosys.entity.BankUserReviewLevel;
import com.payprosys.entity.CorporateFlowState;
import com.payprosys.entity.CorporateUserReviewLevel;
import com.payprosys.entity.PayrollBatch;
import com.payprosys.entity.PayrollBatchReviewEvent;
import com.payprosys.entity.PayrollBatchStatus;
import com.payprosys.entity.User;
import com.payprosys.exception.BadRequestException;
import com.payprosys.exception.ForbiddenException;
import com.payprosys.exception.ResourceNotFoundException;
import com.payprosys.mapper.PayrollBatchMapper;
import com.payprosys.repository.BankUserReviewLevelRepository;
import com.payprosys.repository.BankWorkflowStepRepository;
import com.payprosys.repository.CorporateUserReviewLevelRepository;
import com.payprosys.repository.CorporateWorkflowStepRepository;
import com.payprosys.repository.PayrollBatchRepository;
import com.payprosys.repository.PayrollBatchReviewEventRepository;
import com.payprosys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollWorkflowServiceImpl implements PayrollWorkflowService {

    private static final String EVT_APPROVE = "CORP_APPROVE";
    private static final String EVT_SEND_BACK = "CORP_SEND_BACK";
    private static final String EVT_REJECT = "CORP_REJECT";
    private static final String EVT_SEND_TO_BANK = "SEND_TO_BANK";
    private static final String EVT_BANK_APPROVE = "BANK_APPROVE";
    private static final String EVT_BANK_SEND_BACK = "BANK_SEND_BACK_TO_CORPORATE";
    private static final String EVT_BANK_PROCESS = "BANK_PROCESS_PAYMENT";

    private final PayrollBatchRepository payrollBatchRepository;
    private final PayrollBatchReviewEventRepository reviewEventRepository;
    private final CorporateWorkflowStepRepository corporateWorkflowStepRepository;
    private final BankWorkflowStepRepository bankWorkflowStepRepository;
    private final CorporateUserReviewLevelRepository corporateUserReviewLevelRepository;
    private final BankUserReviewLevelRepository bankUserReviewLevelRepository;
    private final UserRepository userRepository;
    private final PayrollBatchMapper payrollBatchMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PayrollBatchDto> inbox(UUID userId, List<String> roles, UUID corporateId, UUID bankId) {
        if (corporateId != null && roleCorp(roles)) {
            return payrollBatchRepository.findByCorporateIdOrderByCreatedAtDesc(corporateId).stream()
                    .filter(b -> corporateInboxMatch(b, userId, roles)
                            || corporateInboxViewOnlyMatch(b, userId, roles)
                            || corporateInboxUpChainViewOnly(b, userId, roles)
                            || corporateInboxSentToBankViewOnly(b, userId, roles))
                    .sorted(Comparator.comparing(PayrollBatch::getCreatedAt).reversed())
                    .map(b -> {
                        PayrollBatchDto dto = payrollBatchMapper.toDto(b);
                        boolean action = corporateInboxMatch(b, userId, roles);
                        boolean viewOnly = !action && (corporateInboxViewOnlyMatch(b, userId, roles)
                                || corporateInboxUpChainViewOnly(b, userId, roles)
                                || corporateInboxSentToBankViewOnly(b, userId, roles));
                        dto.setViewOnly(viewOnly);
                        if (viewOnly && dto.getWorkflowListSummary() != null) {
                            dto.setWorkflowListSummary(dto.getWorkflowListSummary() + " · Read-only for you");
                        }
                        return dto;
                    })
                    .toList();
        }
        if (bankId != null && roleBank(roles)) {
            return payrollBatchRepository.findForBankInbox(
                            bankId,
                            PayrollBatchStatus.SUBMITTED,
                            CorporateFlowState.CORP_SENT_TO_BANK,
                            PayrollBatchStatus.PENDING,
                            CorporateFlowState.CORP_CLARIFICATION,
                            BankFlowState.BANK_SENT_TO_CORPORATE)
                    .stream()
                    .filter(b -> bankInboxMatch(b, userId, roles)
                            || bankInboxViewOnlyMatch(b, userId, roles)
                            || bankInboxRecallViewOnly(b, roles)
                            || bankInboxApprovedAwaitingProcessViewOnly(b, userId, roles))
                    .sorted(Comparator.comparing(PayrollBatch::getCreatedAt).reversed())
                    .map(b -> {
                        PayrollBatchDto dto = payrollBatchMapper.toDto(b);
                        boolean action = bankInboxMatch(b, userId, roles);
                        boolean viewOnly = !action && (bankInboxViewOnlyMatch(b, userId, roles)
                                || bankInboxRecallViewOnly(b, roles)
                                || bankInboxApprovedAwaitingProcessViewOnly(b, userId, roles));
                        dto.setViewOnly(viewOnly);
                        if (viewOnly && dto.getWorkflowListSummary() != null) {
                            dto.setWorkflowListSummary(dto.getWorkflowListSummary() + " · Read-only for you");
                        }
                        return dto;
                    })
                    .toList();
        }
        throw new ForbiddenException("Not authorized for inbox");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollBatchDto> history(UUID userId, List<String> roles, UUID corporateId, UUID bankId) {
        if (corporateId != null && roleCorp(roles)) {
            return payrollBatchRepository.findByCorporateIdOrderByCreatedAtDesc(corporateId).stream()
                    .filter(this::corporateHistoryMatch)
                    .sorted(Comparator.comparing(PayrollBatch::getCreatedAt).reversed())
                    .map(payrollBatchMapper::toDto)
                    .toList();
        }
        if (bankId != null && roleBank(roles)) {
            return payrollBatchRepository.findByBankIdAndBankFlowStateOrderByCreatedAtDesc(bankId, BankFlowState.BANK_PROCESS_PAYMENT).stream()
                    .sorted(Comparator.comparing(PayrollBatch::getCreatedAt).reversed())
                    .map(payrollBatchMapper::toDto)
                    .toList();
        }
        throw new ForbiddenException("Not authorized for history");
    }

    private boolean corporateHistoryMatch(PayrollBatch b) {
        if (b.getCorporateFlowState() == CorporateFlowState.CORP_REJECTED) {
            return true;
        }
        return b.getBankFlowState() == BankFlowState.BANK_PROCESS_PAYMENT;
    }

    private boolean corporateInboxMatch(PayrollBatch b, UUID userId, List<String> roles) {
        CorporateFlowState s = b.getCorporateFlowState();
        if (s == CorporateFlowState.CORP_REJECTED || s == CorporateFlowState.CORP_SENT_TO_BANK) {
            return false;
        }
        if (s == CorporateFlowState.CORP_APPROVED_HOLD) {
            return canUserActSendToBank(b.getCorporate().getId(), userId, roles);
        }
        Integer lvl = b.getCurrentCorporateReviewLevel();
        if (lvl == null) {
            return false;
        }
        return canUserActAtCorporateLevel(b.getCorporate().getId(), userId, roles, lvl);
    }

    /**
     * Past reviewers (e.g. L1) keep the batch in inbox read-only while a higher step (e.g. L2) owns the action.
     */
    private boolean corporateInboxViewOnlyMatch(PayrollBatch b, UUID userId, List<String> roles) {
        CorporateFlowState s = b.getCorporateFlowState();
        if (s == CorporateFlowState.CORP_REJECTED || s == CorporateFlowState.CORP_SENT_TO_BANK) {
            return false;
        }
        if (s == CorporateFlowState.CORP_APPROVED_HOLD) {
            return false;
        }
        if (roles.contains("CORP_ADMIN")) {
            return false;
        }
        Integer cur = b.getCurrentCorporateReviewLevel();
        if (cur == null) {
            return false;
        }
        UUID corpId = b.getCorporate().getId();
        if (corporateUserReviewLevelRepository.countByCorporate_Id(corpId) == 0) {
            return false;
        }
        OptionalInt maxOpt = corporateUserReviewLevelRepository.findByCorporate_IdAndUser_Id(corpId, userId).stream()
                .mapToInt(a -> a.getReviewLevel())
                .max();
        return maxOpt.isPresent() && maxOpt.getAsInt() < cur;
    }

    /**
     * After an internal send-back, users above the current step (e.g. L2 while batch sits at L1) keep the row
     * in inbox read-only so the thread stays visible to everyone who had already advanced it.
     */
    private boolean corporateInboxUpChainViewOnly(PayrollBatch b, UUID userId, List<String> roles) {
        CorporateFlowState s = b.getCorporateFlowState();
        if (s == CorporateFlowState.CORP_REJECTED || s == CorporateFlowState.CORP_SENT_TO_BANK) {
            return false;
        }
        if (s == CorporateFlowState.CORP_APPROVED_HOLD) {
            return false;
        }
        if (roles.contains("CORP_ADMIN")) {
            return false;
        }
        Integer cur = b.getCurrentCorporateReviewLevel();
        if (cur == null) {
            return false;
        }
        UUID corpId = b.getCorporate().getId();
        if (corporateUserReviewLevelRepository.countByCorporate_Id(corpId) == 0) {
            return false;
        }
        OptionalInt maxOpt = corporateUserReviewLevelRepository.findByCorporate_IdAndUser_Id(corpId, userId).stream()
                .mapToInt(CorporateUserReviewLevel::getReviewLevel)
                .max();
        return maxOpt.isPresent() && maxOpt.getAsInt() > cur;
    }

    /** After send-to-bank, all corporate users keep a read-only inbox row until the bank finishes (not after payment processed). */
    private boolean corporateInboxSentToBankViewOnly(PayrollBatch b, UUID userId, List<String> roles) {
        if (!roleCorp(roles)) {
            return false;
        }
        return b.getCorporateFlowState() == CorporateFlowState.CORP_SENT_TO_BANK
                && b.getBankFlowState() != BankFlowState.BANK_PROCESS_PAYMENT;
    }

    @Override
    public boolean isCorporateViewOnlyInboxAccess(PayrollBatch batch, UUID userId, List<String> roles) {
        return corporateInboxViewOnlyMatch(batch, userId, roles)
                || corporateInboxUpChainViewOnly(batch, userId, roles)
                || corporateInboxSentToBankViewOnly(batch, userId, roles);
    }

    @Override
    public boolean isBankViewOnlyInboxAccess(PayrollBatch batch, UUID userId, List<String> roles) {
        return bankInboxViewOnlyMatch(batch, userId, roles)
                || bankInboxRecallViewOnly(batch, roles)
                || bankInboxApprovedAwaitingProcessViewOnly(batch, userId, roles);
    }

    private boolean bankInboxMatch(PayrollBatch b, UUID userId, List<String> roles) {
        if (b.getCorporateFlowState() != CorporateFlowState.CORP_SENT_TO_BANK) {
            return false;
        }
        BankFlowState bs = b.getBankFlowState();
        if (bs == null) {
            return false;
        }
        if (bs == BankFlowState.BANK_SENT_TO_CORPORATE) {
            return false;
        }
        if (bs == BankFlowState.BANK_PROCESS_PAYMENT) {
            return false;
        }
        if (bs == BankFlowState.BANK_APPROVED) {
            return canUserMarkBankPayment(b.getCorporate().getBank().getId(), userId, roles);
        }
        Integer lvl = b.getCurrentBankReviewLevel();
        if (lvl == null) {
            return false;
        }
        return canUserActAtBankLevel(b.getCorporate().getBank().getId(), userId, roles, lvl);
    }

    /** Past bank reviewers keep the batch read-only while a higher bank step owns the action. */
    private boolean bankInboxViewOnlyMatch(PayrollBatch b, UUID userId, List<String> roles) {
        if (!roleBank(roles)) {
            return false;
        }
        if (b.getBatchStatus() != PayrollBatchStatus.SUBMITTED || b.getCorporateFlowState() != CorporateFlowState.CORP_SENT_TO_BANK) {
            return false;
        }
        BankFlowState bs = b.getBankFlowState();
        if (bs == null || bs == BankFlowState.BANK_SENT_TO_CORPORATE || bs == BankFlowState.BANK_PROCESS_PAYMENT) {
            return false;
        }
        if (roles.contains("BANK_ADMIN")) {
            return false;
        }
        Integer cur = b.getCurrentBankReviewLevel();
        if (cur == null) {
            return false;
        }
        UUID bankEntityId = b.getCorporate().getBank().getId();
        if (bankUserReviewLevelRepository.countByBank_Id(bankEntityId) == 0) {
            return false;
        }
        OptionalInt maxOpt = bankUserReviewLevelRepository.findByBank_IdAndUser_Id(bankEntityId, userId).stream()
                .mapToInt(BankUserReviewLevel::getReviewLevel)
                .max();
        return maxOpt.isPresent() && maxOpt.getAsInt() < cur;
    }

    /** After bank send-back, the batch stays in the bank inbox read-only until corporate responds. */
    private boolean bankInboxRecallViewOnly(PayrollBatch b, List<String> roles) {
        if (!roleBank(roles)) {
            return false;
        }
        return b.getBatchStatus() == PayrollBatchStatus.PENDING
                && b.getCorporateFlowState() == CorporateFlowState.CORP_CLARIFICATION
                && b.getBankFlowState() == BankFlowState.BANK_SENT_TO_CORPORATE;
    }

    /**
     * After the final bank approval step, {@link BankFlowState#BANK_APPROVED} clears {@code currentBankReviewLevel};
     * reviewers who are not eligible to mark process payment keep the row read-only in the bank inbox.
     */
    private boolean bankInboxApprovedAwaitingProcessViewOnly(PayrollBatch b, UUID userId, List<String> roles) {
        if (!roleBank(roles)) {
            return false;
        }
        if (b.getBatchStatus() != PayrollBatchStatus.SUBMITTED || b.getCorporateFlowState() != CorporateFlowState.CORP_SENT_TO_BANK) {
            return false;
        }
        if (b.getBankFlowState() != BankFlowState.BANK_APPROVED) {
            return false;
        }
        return !canUserMarkBankPayment(b.getCorporate().getBank().getId(), userId, roles);
    }

    private boolean canUserMarkBankPayment(UUID bankId, UUID userId, List<String> roles) {
        if (!roleBank(roles)) {
            return false;
        }
        if (roles.contains("BANK_ADMIN")) {
            return true;
        }
        if (bankUserReviewLevelRepository.countByBank_Id(bankId) == 0) {
            return true;
        }
        int max = maxBankStep(bankId);
        return bankUserReviewLevelRepository.findByBank_IdAndUser_Id(bankId, userId).stream()
                .anyMatch(a -> a.getReviewLevel() == max);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollReviewEventDto> listReviewEvents(UUID batchId, UUID userId, List<String> roles, UUID corporateId, UUID bankId) {
        PayrollBatch batch = loadBatch(batchId);
        assertCanViewTimeline(batch, userId, roles, corporateId, bankId);
        java.util.UUID batchCorporateId = batch.getCorporate().getId();
        boolean useCorporateFilter = corporateId != null
                && roleCorp(roles)
                && batchCorporateId.equals(corporateId);
        boolean useBankFilter = bankId != null
                && roleBank(roles)
                && batch.getCorporate().getBank() != null
                && batch.getCorporate().getBank().getId().equals(bankId);
        if (useCorporateFilter) {
            return reviewEventRepository.findByPayrollBatch_IdWithActorOrderByCreatedAtAsc(batchId).stream()
                    .filter(ev -> isReviewEventVisibleToCorporate(ev, batchCorporateId))
                    .map(ev -> toEventDto(ev, batch, true))
                    .toList();
        }
        if (useBankFilter) {
            return reviewEventRepository.findByPayrollBatch_IdWithActorOrderByCreatedAtAsc(batchId).stream()
                    .filter(this::isReviewEventVisibleToBank)
                    .map(ev -> toEventDto(ev, batch, false))
                    .toList();
        }
        throw new ForbiddenException("Cannot view this batch timeline");
    }

    /** Corporate: same-org thread + cross-party rows marked bank-visible (final approve, send-to-bank, bank send-back). */
    private boolean isReviewEventVisibleToCorporate(PayrollBatchReviewEvent ev, java.util.UUID batchCorporateId) {
        if (ev.isBankVisible()) {
            return true;
        }
        User actor = ev.getActor();
        if (actor.getCorporate() != null && batchCorporateId.equals(actor.getCorporate().getId())) {
            return true;
        }
        return false;
    }

    /** Bank: bank-side thread + corporate rows explicitly marked bank-visible. */
    private boolean isReviewEventVisibleToBank(PayrollBatchReviewEvent ev) {
        if (ev.isBankVisible()) {
            return true;
        }
        User actor = ev.getActor();
        return actor.getBank() != null;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowActionHintsDto getActionHints(UUID batchId, UUID userId, List<String> roles, UUID viewerCorporateId, UUID viewerBankId) {
        PayrollBatch batch = loadBatch(batchId);
        assertActorCanViewBatch(batch, viewerCorporateId, viewerBankId);
        UUID corpEntityId = batch.getCorporate().getId();
        int maxCorp = maxCorporateStep(corpEntityId);
        int maxBank = batch.getCorporate().getBank() != null ? maxBankStep(batch.getCorporate().getBank().getId()) : 1;

        WorkflowActionHintsDto.WorkflowActionHintsDtoBuilder b = WorkflowActionHintsDto.builder()
                .maxCorporateReviewLevel(maxCorp)
                .maxBankReviewLevel(maxBank)
                .showCorporateApprove(false)
                .showCorporateReject(false)
                .showCorporateSendBack(false)
                .showCorporateSendToBank(false)
                .showBankApprove(false)
                .showBankReject(false)
                .showBankSendBack(false)
                .showBankProcessPayment(false);

        if (viewerCorporateId != null && batch.getCorporate().getId().equals(viewerCorporateId) && roleCorp(roles)) {
            boolean atCorpStep = hintsCorporateAtCurrentReviewStep(batch, corpEntityId, userId, roles);
            b.showCorporateApprove(atCorpStep)
                    .showCorporateReject(atCorpStep)
                    .showCorporateSendBack(atCorpStep)
                    .showCorporateSendToBank(hintsCorporateSendToBank(batch, corpEntityId, userId, roles));
        }

        if (viewerBankId != null && batch.getCorporate().getBank() != null
                && batch.getCorporate().getBank().getId().equals(viewerBankId) && roleBank(roles)) {
            UUID bankEntityId = batch.getCorporate().getBank().getId();
            b.showBankApprove(hintsBankApproveOrSendBack(batch, bankEntityId, userId, roles))
                    .showBankSendBack(hintsBankApproveOrSendBack(batch, bankEntityId, userId, roles))
                    .showBankProcessPayment(hintsBankProcessPayment(batch, bankEntityId, userId, roles));
        }

        return b.build();
    }

    @Override
    @Transactional
    public void appendReviewEvent(UUID batchId, UUID userId, List<String> roles, UUID corporateId, UUID bankId, WorkflowAppendRemarkRequest request) {
        PayrollBatch batch = loadBatch(batchId);
        assertCanViewTimeline(batch, userId, roles, corporateId, bankId);
        User actor = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        boolean bankActor = corporateId == null && bankId != null;
        boolean bankVisible = request.isBankVisible() && !bankActor;
        PayrollBatchReviewEvent ev = PayrollBatchReviewEvent.builder()
                .payrollBatch(batch)
                .actor(actor)
                .eventType(request.getEventType() != null ? request.getEventType() : "REMARK")
                .body(request.getBody())
                .bankVisible(bankVisible)
                .build();
        reviewEventRepository.save(ev);
    }

    @Override
    @Transactional
    public void corporateApprove(UUID batchId, UUID corporateId, UUID userId, List<String> roles, WorkflowApproveRequest request) {
        PayrollBatch batch = loadBatch(batchId);
        assertCorporate(batch, corporateId);
        CorporateFlowState s = batch.getCorporateFlowState();
        if (s != CorporateFlowState.CORP_NEW && s != CorporateFlowState.CORP_IN_REVIEW && s != CorporateFlowState.CORP_CLARIFICATION) {
            throw new BadRequestException("Batch is not awaiting a corporate approval step");
        }
        if (batch.getCurrentCorporateReviewLevel() == null) {
            throw new BadRequestException("Batch has not been submitted for corporate review yet");
        }
        int lvl = batch.getCurrentCorporateReviewLevel();
        if (!canUserActAtCorporateLevel(corporateId, userId, roles, lvl)) {
            throw new ForbiddenException("Not authorized to approve at this step");
        }
        int max = maxCorporateStep(corporateId);
        if (request.isSendToBankAfter()) {
            if (lvl != max) {
                throw new BadRequestException("Send to bank is only allowed after the final corporate review level");
            }
            if (request.getRemarksForBank() == null || request.getRemarksForBank().isBlank()) {
                throw new BadRequestException("remarksForBank is required when sendToBankAfter is true");
            }
        }

        appendCorpEvent(batch, userId, EVT_APPROVE, request.getRemarks(), false);

        if (request.isSendToBankAfter()) {
            applySendToBank(batch, request.getRemarksForBank(), userId);
            return;
        }

        if (lvl < max) {
            batch.setCorporateFlowState(CorporateFlowState.CORP_IN_REVIEW);
            batch.setCurrentCorporateReviewLevel(lvl + 1);
        } else {
            batch.setCorporateFlowState(CorporateFlowState.CORP_APPROVED_HOLD);
            batch.setCurrentCorporateReviewLevel(null);
        }
        payrollBatchRepository.save(batch);
    }

    @Override
    @Transactional
    public void corporateSendBack(UUID batchId, UUID corporateId, UUID userId, List<String> roles, WorkflowRemarksRequest request) {
        PayrollBatch batch = loadBatch(batchId);
        assertCorporate(batch, corporateId);
        CorporateFlowState s = batch.getCorporateFlowState();
        if (s != CorporateFlowState.CORP_NEW && s != CorporateFlowState.CORP_IN_REVIEW && s != CorporateFlowState.CORP_CLARIFICATION) {
            throw new BadRequestException("Send back is not allowed in the current corporate state");
        }
        if (batch.getCurrentCorporateReviewLevel() == null) {
            throw new BadRequestException("Batch has not been submitted for corporate review yet");
        }
        int lvl = batch.getCurrentCorporateReviewLevel();
        if (!canUserActAtCorporateLevel(corporateId, userId, roles, lvl)) {
            throw new ForbiddenException("Not authorized to send back at this step");
        }
        int prev = Math.max(1, lvl - 1);
        batch.setCorporateFlowState(CorporateFlowState.CORP_CLARIFICATION);
        batch.setCurrentCorporateReviewLevel(prev);
        appendCorpEvent(batch, userId, EVT_SEND_BACK, request.getRemarks(), false);
        payrollBatchRepository.save(batch);
    }

    @Override
    @Transactional
    public void corporateReject(UUID batchId, UUID corporateId, UUID userId, List<String> roles, WorkflowRemarksRequest request) {
        PayrollBatch batch = loadBatch(batchId);
        assertCorporate(batch, corporateId);
        CorporateFlowState s = batch.getCorporateFlowState();
        if (s != CorporateFlowState.CORP_NEW && s != CorporateFlowState.CORP_IN_REVIEW && s != CorporateFlowState.CORP_CLARIFICATION) {
            throw new BadRequestException("Reject is not allowed in the current corporate state");
        }
        if (batch.getCurrentCorporateReviewLevel() == null) {
            throw new BadRequestException("Batch has not been submitted for corporate review yet");
        }
        int lvl = batch.getCurrentCorporateReviewLevel();
        if (!canUserActAtCorporateLevel(corporateId, userId, roles, lvl)) {
            throw new ForbiddenException("Not authorized to reject at this step");
        }
        batch.setCorporateFlowState(CorporateFlowState.CORP_REJECTED);
        batch.setCurrentCorporateReviewLevel(null);
        batch.setBankFlowState(null);
        batch.setCurrentBankReviewLevel(null);
        appendCorpEvent(batch, userId, EVT_REJECT, request.getRemarks(), false);
        payrollBatchRepository.save(batch);
    }

    @Override
    @Transactional
    public void corporateSendToBank(UUID batchId, UUID corporateId, UUID userId, List<String> roles, WorkflowSendToBankRequest request) {
        PayrollBatch batch = loadBatch(batchId);
        assertCorporate(batch, corporateId);
        if (batch.getCorporateFlowState() != CorporateFlowState.CORP_APPROVED_HOLD) {
            throw new BadRequestException("Batch must be in corporate approved (hold) before sending to bank");
        }
        if (!canUserActSendToBank(corporateId, userId, roles)) {
            throw new ForbiddenException("Not authorized to send this batch to the bank");
        }
        if (request.getRemarksForBank() == null || request.getRemarksForBank().isBlank()) {
            throw new BadRequestException("remarksForBank is required to send this batch to the bank");
        }
        applySendToBank(batch, request.getRemarksForBank().trim(), userId);
    }

    @Override
    @Transactional
    public void bankApprove(UUID batchId, UUID bankId, UUID userId, List<String> roles, WorkflowRemarksRequest request) {
        PayrollBatch batch = loadBatch(batchId);
        assertBank(batch, bankId);
        if (batch.getBatchStatus() != PayrollBatchStatus.SUBMITTED || batch.getCorporateFlowState() != CorporateFlowState.CORP_SENT_TO_BANK) {
            throw new BadRequestException("Batch is not with the bank for approval");
        }
        BankFlowState bs = batch.getBankFlowState();
        if (bs != BankFlowState.BANK_NEW && bs != BankFlowState.BANK_IN_REVIEW && bs != BankFlowState.BANK_CLARIFICATION) {
            throw new BadRequestException("Bank approval is not allowed in the current bank state");
        }
        int lvl = batch.getCurrentBankReviewLevel() != null ? batch.getCurrentBankReviewLevel() : 1;
        if (!canUserActAtBankLevel(bankId, userId, roles, lvl)) {
            throw new ForbiddenException("Not authorized to approve at this bank step");
        }
        int max = maxBankStep(bankId);
        appendBankEvent(batch, userId, EVT_BANK_APPROVE, request.getRemarks(), false);
        if (lvl < max) {
            batch.setBankFlowState(BankFlowState.BANK_IN_REVIEW);
            batch.setCurrentBankReviewLevel(lvl + 1);
        } else {
            batch.setBankFlowState(BankFlowState.BANK_APPROVED);
            batch.setCurrentBankReviewLevel(null);
        }
        payrollBatchRepository.save(batch);
    }

    @Override
    @Transactional
    public void bankSendBackToCorporate(UUID batchId, UUID bankId, UUID userId, List<String> roles, WorkflowRemarksRequest request) {
        PayrollBatch batch = loadBatch(batchId);
        assertBank(batch, bankId);
        if (batch.getBatchStatus() != PayrollBatchStatus.SUBMITTED || batch.getCorporateFlowState() != CorporateFlowState.CORP_SENT_TO_BANK) {
            throw new BadRequestException("Batch is not with the bank");
        }
        BankFlowState bs = batch.getBankFlowState();
        if (bs != BankFlowState.BANK_NEW && bs != BankFlowState.BANK_IN_REVIEW && bs != BankFlowState.BANK_CLARIFICATION) {
            throw new BadRequestException("Send back to corporate is not allowed in the current bank state");
        }
        int lvl = batch.getCurrentBankReviewLevel() != null ? batch.getCurrentBankReviewLevel() : 1;
        if (!canUserActAtBankLevel(bankId, userId, roles, lvl)) {
            throw new ForbiddenException("Not authorized at this bank step");
        }
        int maxCorp = maxCorporateStep(batch.getCorporate().getId());
        batch.setBatchStatus(PayrollBatchStatus.PENDING);
        batch.setCorporateFlowState(CorporateFlowState.CORP_CLARIFICATION);
        batch.setCurrentCorporateReviewLevel(maxCorp);
        batch.setBankFlowState(BankFlowState.BANK_SENT_TO_CORPORATE);
        batch.setCurrentBankReviewLevel(null);
        appendBankEvent(batch, userId, EVT_BANK_SEND_BACK, request.getRemarks(), true);
        payrollBatchRepository.save(batch);
    }

    @Override
    @Transactional
    public void bankMarkProcessPayment(UUID batchId, UUID bankId, UUID userId, List<String> roles, WorkflowRemarksRequest request) {
        PayrollBatch batch = loadBatch(batchId);
        assertBank(batch, bankId);
        if (batch.getBankFlowState() != BankFlowState.BANK_APPROVED) {
            throw new BadRequestException("Batch must be bank-approved before marking process payment");
        }
        if (!canUserMarkBankPayment(bankId, userId, roles)) {
            throw new ForbiddenException("Not authorized to mark process payment");
        }
        batch.setBankFlowState(BankFlowState.BANK_PROCESS_PAYMENT);
        batch.setBatchStatus(PayrollBatchStatus.COMPLETED);
        appendBankEvent(batch, userId, EVT_BANK_PROCESS, request.getRemarks(), true);
        payrollBatchRepository.save(batch);
    }

    @Override
    @Transactional
    public void submitBatchLegacy(UUID batchId, UUID corporateId, UUID actorUserId) {
        PayrollBatch batch = loadBatch(batchId);
        assertCorporate(batch, corporateId);
        if (batch.getBatchStatus() != PayrollBatchStatus.PENDING) {
            throw new BadRequestException("Only pending batches can be submitted");
        }
        long eventCount = reviewEventRepository.countByPayrollBatch_Id(batchId);

        if (batch.getCorporateFlowState() == CorporateFlowState.CORP_NEW
                && batch.getCurrentCorporateReviewLevel() == null) {
            batch.setCurrentCorporateReviewLevel(1);
            payrollBatchRepository.save(batch);
            return;
        }

        if (eventCount == 0 && batch.getCorporateFlowState() == CorporateFlowState.CORP_NEW
                && batch.getCurrentCorporateReviewLevel() != null) {
            return;
        }

        if (batch.getCorporateFlowState() != CorporateFlowState.CORP_APPROVED_HOLD) {
            throw new BadRequestException("Complete corporate approval in the workflow inbox first, or use Send to bank when the batch is on hold.");
        }
        applySendToBank(batch, batch.getRemarksForBank() != null ? batch.getRemarksForBank() : "", actorUserId);
    }

    private void applySendToBank(PayrollBatch batch, String remarksForBank, UUID actorUserId) {
        batch.setRemarksForBank(remarksForBank != null ? remarksForBank : "");
        batch.setCorporateFlowState(CorporateFlowState.CORP_SENT_TO_BANK);
        batch.setCurrentCorporateReviewLevel(null);
        batch.setBatchStatus(PayrollBatchStatus.SUBMITTED);
        batch.setBankFlowState(BankFlowState.BANK_NEW);
        batch.setCurrentBankReviewLevel(1);
        appendCorpEvent(batch, actorUserId, EVT_SEND_TO_BANK, remarksForBank, true);
        payrollBatchRepository.saveAndFlush(batch);
    }

    private void appendCorpEvent(PayrollBatch batch, UUID actorUserId, String type, String body, boolean bankVisible) {
        User actor = userRepository.findById(actorUserId).orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));
        reviewEventRepository.save(PayrollBatchReviewEvent.builder()
                .payrollBatch(batch)
                .actor(actor)
                .eventType(type)
                .body(body)
                .bankVisible(bankVisible)
                .build());
    }

    private void appendBankEvent(PayrollBatch batch, UUID actorUserId, String type, String body, boolean bankVisible) {
        User actor = userRepository.findById(actorUserId).orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));
        reviewEventRepository.save(PayrollBatchReviewEvent.builder()
                .payrollBatch(batch)
                .actor(actor)
                .eventType(type)
                .body(body)
                .bankVisible(bankVisible)
                .build());
    }

    private PayrollReviewEventDto toEventDto(PayrollBatchReviewEvent e, PayrollBatch batch, boolean viewingFromCorporateTimeline) {
        User actor = e.getActor();
        boolean crossParty = e.isBankVisible();
        boolean corpActor = actor.getCorporate() != null;
        boolean bankActor = actor.getBank() != null;
        boolean visibleToCorporate = crossParty || corpActor;
        boolean visibleToBank = crossParty || bankActor;
        return PayrollReviewEventDto.builder()
                .id(e.getId())
                .actorUserId(actor.getId())
                .actorStepChip(resolveActorStepChipForViewer(actor, batch, viewingFromCorporateTimeline))
                .actorName(actorDisplayName(actor))
                .actorEmail(actor.getEmail())
                .eventType(e.getEventType())
                .body(e.getBody())
                .bankVisible(e.isBankVisible())
                .visibleToBank(visibleToBank)
                .visibleToCorporate(visibleToCorporate)
                .createdAt(e.getCreatedAt())
                .build();
    }

    /**
     * Bank-side timeline: show this batch's corporate name for remarks authored by corporate users (instead of L3…).
     * Corporate-side timeline: show the bank's name for remarks authored by bank users (instead of L1…).
     */
    private String resolveActorStepChipForViewer(User actor, PayrollBatch batch, boolean viewingFromCorporateTimeline) {
        if (viewingFromCorporateTimeline) {
            if (actor.getBank() != null) {
                if (batch.getCorporate().getBank() != null) {
                    String bankName = batch.getCorporate().getBank().getName();
                    if (bankName != null && !bankName.isBlank()) {
                        return bankName;
                    }
                }
                return "Bank";
            }
        } else {
            if (actor.getCorporate() != null) {
                String corpName = batch.getCorporate().getName();
                if (corpName != null && !corpName.isBlank()) {
                    return corpName;
                }
                return "Corporate";
            }
        }
        return buildActorStepChip(actor);
    }

    private String actorDisplayName(User actor) {
        String fn = actor.getFirstName() != null ? actor.getFirstName().trim() : "";
        String ln = actor.getLastName() != null ? actor.getLastName().trim() : "";
        String combined = (fn + " " + ln).trim();
        return combined.isEmpty() ? null : combined;
    }

    private String buildActorStepChip(User actor) {
        if (actor.getCorporate() != null) {
            UUID cid = actor.getCorporate().getId();
            if (corporateUserReviewLevelRepository.countByCorporate_Id(cid) == 0) {
                return "Corporate";
            }
            List<Integer> levels = corporateUserReviewLevelRepository.findByCorporate_IdAndUser_Id(cid, actor.getId()).stream()
                    .map(CorporateUserReviewLevel::getReviewLevel)
                    .sorted()
                    .toList();
            if (levels.isEmpty()) {
                return actor.getEmail() != null ? actor.getEmail() : "Corporate";
            }
            return levels.stream()
                    .map(lvl -> corporateWorkflowStepRepository.findByCorporate_IdAndStepLevel(cid, lvl)
                            .map(s -> "L" + lvl + " (" + s.getLabel() + ")")
                            .orElse("L" + lvl))
                    .collect(Collectors.joining(", "));
        }
        if (actor.getBank() != null) {
            UUID bid = actor.getBank().getId();
            if (bankUserReviewLevelRepository.countByBank_Id(bid) == 0) {
                return "Bank";
            }
            List<Integer> levels = bankUserReviewLevelRepository.findByBank_IdAndUser_Id(bid, actor.getId()).stream()
                    .map(BankUserReviewLevel::getReviewLevel)
                    .sorted()
                    .toList();
            if (levels.isEmpty()) {
                return actor.getEmail() != null ? actor.getEmail() : "Bank";
            }
            return levels.stream()
                    .map(lvl -> bankWorkflowStepRepository.findByBank_IdAndStepLevel(bid, lvl)
                            .map(s -> "L" + lvl + " (" + s.getLabel() + ")")
                            .orElse("L" + lvl))
                    .collect(Collectors.joining(", "));
        }
        return "System";
    }

    private PayrollBatch loadBatch(UUID id) {
        return payrollBatchRepository.findByIdWithCorporateAndUploadedBy(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll batch", id));
    }

    private void assertCorporate(PayrollBatch batch, UUID corporateId) {
        if (!batch.getCorporate().getId().equals(corporateId)) {
            throw new ForbiddenException("Batch does not belong to your corporate");
        }
    }

    private void assertBank(PayrollBatch batch, UUID bankId) {
        if (batch.getCorporate().getBank() == null || !batch.getCorporate().getBank().getId().equals(bankId)) {
            throw new ForbiddenException("Batch is not under your bank");
        }
    }

    private void assertActorCanViewBatch(PayrollBatch batch, UUID viewerCorporateId, UUID viewerBankId) {
        if (viewerCorporateId != null) {
            if (!batch.getCorporate().getId().equals(viewerCorporateId)) {
                throw new ForbiddenException("Cannot view this batch");
            }
            return;
        }
        if (viewerBankId != null) {
            if (batch.getCorporate().getBank() == null || !batch.getCorporate().getBank().getId().equals(viewerBankId)) {
                throw new ForbiddenException("Batch is not under your bank");
            }
            if (!isBankBatchOpenedForWorkflow(batch)) {
                throw new ForbiddenException("Batch is not visible to the bank yet");
            }
            return;
        }
        throw new ForbiddenException("Not authorized");
    }

    private boolean hintsCorporateAtCurrentReviewStep(PayrollBatch batch, UUID corporateId, UUID userId, List<String> roles) {
        CorporateFlowState s = batch.getCorporateFlowState();
        if (s != CorporateFlowState.CORP_NEW && s != CorporateFlowState.CORP_IN_REVIEW && s != CorporateFlowState.CORP_CLARIFICATION) {
            return false;
        }
        Integer lvlBox = batch.getCurrentCorporateReviewLevel();
        if (lvlBox == null) {
            return false;
        }
        return canUserActAtCorporateLevel(corporateId, userId, roles, lvlBox);
    }

    private boolean hintsCorporateSendToBank(PayrollBatch batch, UUID corporateId, UUID userId, List<String> roles) {
        if (batch.getCorporateFlowState() != CorporateFlowState.CORP_APPROVED_HOLD) {
            return false;
        }
        return canUserActSendToBank(corporateId, userId, roles);
    }

    private boolean hintsBankApproveOrSendBack(PayrollBatch batch, UUID bankId, UUID userId, List<String> roles) {
        if (batch.getBatchStatus() != PayrollBatchStatus.SUBMITTED || batch.getCorporateFlowState() != CorporateFlowState.CORP_SENT_TO_BANK) {
            return false;
        }
        BankFlowState bs = batch.getBankFlowState();
        if (bs != BankFlowState.BANK_NEW && bs != BankFlowState.BANK_IN_REVIEW && bs != BankFlowState.BANK_CLARIFICATION) {
            return false;
        }
        int lvl = batch.getCurrentBankReviewLevel() != null ? batch.getCurrentBankReviewLevel() : 1;
        return canUserActAtBankLevel(bankId, userId, roles, lvl);
    }

    private boolean hintsBankProcessPayment(PayrollBatch batch, UUID bankId, UUID userId, List<String> roles) {
        if (batch.getBankFlowState() != BankFlowState.BANK_APPROVED) {
            return false;
        }
        return canUserMarkBankPayment(bankId, userId, roles);
    }

    private void assertCanViewTimeline(PayrollBatch batch, UUID userId, List<String> roles, UUID corporateId, UUID bankId) {
        if (corporateId != null && batch.getCorporate().getId().equals(corporateId) && roleCorp(roles)) {
            return;
        }
        if (bankId != null && batch.getCorporate().getBank() != null && batch.getCorporate().getBank().getId().equals(bankId) && roleBank(roles)) {
            if (isBankBatchOpenedForWorkflow(batch)) {
                return;
            }
        }
        throw new ForbiddenException("Cannot view this batch timeline");
    }

    private boolean isBankBatchOpenedForWorkflow(PayrollBatch batch) {
        if (batch.getBankFlowState() == BankFlowState.BANK_PROCESS_PAYMENT) {
            return true;
        }
        if (batch.getBatchStatus() == PayrollBatchStatus.SUBMITTED
                && batch.getCorporateFlowState() == CorporateFlowState.CORP_SENT_TO_BANK) {
            return true;
        }
        return batch.getBatchStatus() == PayrollBatchStatus.PENDING
                && batch.getCorporateFlowState() == CorporateFlowState.CORP_CLARIFICATION
                && batch.getBankFlowState() == BankFlowState.BANK_SENT_TO_CORPORATE;
    }

    private boolean roleCorp(List<String> roles) {
        return roles.contains("CORP_ADMIN") || roles.contains("CORP_USER");
    }

    private boolean roleBank(List<String> roles) {
        return roles.contains("BANK_ADMIN") || roles.contains("BANK_USER");
    }

    private int maxCorporateStep(UUID corporateId) {
        return corporateWorkflowStepRepository.findByCorporate_IdOrderByStepLevelAsc(corporateId).stream()
                .mapToInt(s -> s.getStepLevel())
                .max()
                .orElse(1);
    }

    private int maxBankStep(UUID bankId) {
        return bankWorkflowStepRepository.findByBank_IdOrderByStepLevelAsc(bankId).stream()
                .mapToInt(s -> s.getStepLevel())
                .max()
                .orElse(1);
    }

    private boolean canUserActAtCorporateLevel(UUID corporateId, UUID userId, List<String> roles, int level) {
        if (!roleCorp(roles)) {
            return false;
        }
        if (roles.contains("CORP_ADMIN")) {
            return true;
        }
        if (corporateUserReviewLevelRepository.countByCorporate_Id(corporateId) == 0) {
            return true;
        }
        return corporateUserReviewLevelRepository.findByCorporate_IdAndUser_Id(corporateId, userId).stream()
                .anyMatch(a -> a.getReviewLevel() == level);
    }

    private boolean canUserActSendToBank(UUID corporateId, UUID userId, List<String> roles) {
        if (!roleCorp(roles)) {
            return false;
        }
        int max = maxCorporateStep(corporateId);
        return canUserActAtCorporateLevel(corporateId, userId, roles, max);
    }

    private boolean canUserActAtBankLevel(UUID bankId, UUID userId, List<String> roles, int level) {
        if (!roleBank(roles)) {
            return false;
        }
        if (roles.contains("BANK_ADMIN")) {
            return true;
        }
        if (bankUserReviewLevelRepository.countByBank_Id(bankId) == 0) {
            return true;
        }
        return bankUserReviewLevelRepository.findByBank_IdAndUser_Id(bankId, userId).stream()
                .anyMatch(a -> a.getReviewLevel() == level);
    }
}
