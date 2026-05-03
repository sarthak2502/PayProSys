package com.payprosys.service;

import com.payprosys.dto.PayrollBatchDto;
import com.payprosys.dto.PayrollReviewEventDto;
import com.payprosys.dto.WorkflowAppendRemarkRequest;
import com.payprosys.dto.WorkflowApproveRequest;
import com.payprosys.dto.WorkflowRemarksRequest;
import com.payprosys.dto.WorkflowActionHintsDto;
import com.payprosys.dto.WorkflowSendToBankRequest;
import com.payprosys.entity.PayrollBatch;

import java.util.List;
import java.util.UUID;

/** Phase 2: workflow transitions, inbox, history, review timeline (see docs/WORKFLOW_REQUIREMENTS.md). */
public interface PayrollWorkflowService {

    List<PayrollBatchDto> inbox(UUID userId, List<String> roles, UUID corporateId, UUID bankId);

    List<PayrollBatchDto> history(UUID userId, List<String> roles, UUID corporateId, UUID bankId);

    List<PayrollReviewEventDto> listReviewEvents(UUID batchId, UUID userId, List<String> roles, UUID corporateId, UUID bankId);

    WorkflowActionHintsDto getActionHints(UUID batchId, UUID userId, List<String> roles, UUID viewerCorporateId, UUID viewerBankId);

    void appendReviewEvent(UUID batchId, UUID userId, List<String> roles, UUID corporateId, UUID bankId, WorkflowAppendRemarkRequest request);

    void corporateApprove(UUID batchId, UUID corporateId, UUID userId, List<String> roles, WorkflowApproveRequest request);

    void corporateSendBack(UUID batchId, UUID corporateId, UUID userId, List<String> roles, WorkflowRemarksRequest request);

    void corporateReject(UUID batchId, UUID corporateId, UUID userId, List<String> roles, WorkflowRemarksRequest request);

    void corporateSendToBank(UUID batchId, UUID corporateId, UUID userId, List<String> roles, WorkflowSendToBankRequest request);

    void bankApprove(UUID batchId, UUID bankId, UUID userId, List<String> roles, WorkflowRemarksRequest request);

    void bankSendBackToCorporate(UUID batchId, UUID bankId, UUID userId, List<String> roles, WorkflowRemarksRequest request);

    void bankMarkProcessPayment(UUID batchId, UUID bankId, UUID userId, List<String> roles, WorkflowRemarksRequest request);

    /** Used by {@link PayrollService#submitBatch}; legacy one-click when no review events yet. */
    void submitBatchLegacy(UUID batchId, UUID corporateId, UUID actorUserId);

    /** Corporate inbox: user may open batch read-only when their max assigned step is below the current step. */
    boolean isCorporateViewOnlyInboxAccess(PayrollBatch batch, UUID userId, List<String> roles);

    /** Bank inbox: read-only when awaiting another bank step, or after send-back while corporate clarifies. */
    boolean isBankViewOnlyInboxAccess(PayrollBatch batch, UUID userId, List<String> roles);
}
