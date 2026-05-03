package com.payprosys.controller;

import com.payprosys.auth.CurrentUser;
import com.payprosys.auth.SessionInfo;
import com.payprosys.dto.ApiResponse;
import com.payprosys.dto.PayrollBatchDto;
import com.payprosys.dto.PayrollReviewEventDto;
import com.payprosys.dto.WorkflowActionHintsDto;
import com.payprosys.dto.WorkflowAppendRemarkRequest;
import com.payprosys.dto.WorkflowApproveRequest;
import com.payprosys.dto.WorkflowRemarksRequest;
import com.payprosys.dto.WorkflowSendToBankRequest;
import com.payprosys.exception.ForbiddenException;
import com.payprosys.service.PayrollWorkflowService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Phase 2: workflow inbox, history, transitions, review timeline (see docs/WORKFLOW_REQUIREMENTS.md). */
@RestController
@RequestMapping("/api/payroll/workflow")
@RequiredArgsConstructor
public class PayrollWorkflowController {

    private final PayrollWorkflowService payrollWorkflowService;
    private final CurrentUser currentUser;

    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse<List<PayrollBatchDto>>> inbox(HttpServletRequest request) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER", "BANK_ADMIN", "BANK_USER");
        List<PayrollBatchDto> list = payrollWorkflowService.inbox(
                session.getUserId(),
                session.getRoles(),
                session.getCorporateId(),
                session.getBankId());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PayrollBatchDto>>> history(HttpServletRequest request) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER", "BANK_ADMIN", "BANK_USER");
        List<PayrollBatchDto> list = payrollWorkflowService.history(
                session.getUserId(),
                session.getRoles(),
                session.getCorporateId(),
                session.getBankId());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/batches/{id}/review-events")
    public ResponseEntity<ApiResponse<List<PayrollReviewEventDto>>> reviewEvents(HttpServletRequest request, @PathVariable UUID id) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER", "BANK_ADMIN", "BANK_USER");
        List<PayrollReviewEventDto> list = payrollWorkflowService.listReviewEvents(
                id,
                session.getUserId(),
                session.getRoles(),
                session.getCorporateId(),
                session.getBankId());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/batches/{id}/action-hints")
    public ResponseEntity<ApiResponse<WorkflowActionHintsDto>> actionHints(HttpServletRequest request, @PathVariable UUID id) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER", "BANK_ADMIN", "BANK_USER");
        UUID corp = session.getCorporateId();
        UUID bank = corp != null ? null : session.getBankId();
        WorkflowActionHintsDto hints = payrollWorkflowService.getActionHints(
                id,
                session.getUserId(),
                session.getRoles(),
                corp,
                bank);
        return ResponseEntity.ok(ApiResponse.success(hints));
    }

    @PostMapping(value = "/batches/{id}/review-events", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> appendReviewEvent(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody WorkflowAppendRemarkRequest body) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER", "BANK_ADMIN", "BANK_USER");
        payrollWorkflowService.appendReviewEvent(
                id,
                session.getUserId(),
                session.getRoles(),
                session.getCorporateId(),
                session.getBankId(),
                body != null ? body : new WorkflowAppendRemarkRequest());
        return ResponseEntity.ok(ApiResponse.success("Remark recorded", null));
    }

    @PostMapping(value = "/batches/{id}/corporate/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> corporateApprove(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody(required = false) WorkflowApproveRequest body) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER");
        UUID corporateId = requireCorporate(session);
        payrollWorkflowService.corporateApprove(
                id,
                corporateId,
                session.getUserId(),
                session.getRoles(),
                body != null ? body : WorkflowApproveRequest.builder().build());
        return ResponseEntity.ok(ApiResponse.success("Approved", null));
    }

    @PostMapping(value = "/batches/{id}/corporate/send-back", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> corporateSendBack(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody(required = false) WorkflowRemarksRequest body) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER");
        UUID corporateId = requireCorporate(session);
        payrollWorkflowService.corporateSendBack(
                id,
                corporateId,
                session.getUserId(),
                session.getRoles(),
                body != null ? body : WorkflowRemarksRequest.builder().build());
        return ResponseEntity.ok(ApiResponse.success("Sent back", null));
    }

    @PostMapping(value = "/batches/{id}/corporate/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> corporateReject(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody(required = false) WorkflowRemarksRequest body) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER");
        UUID corporateId = requireCorporate(session);
        payrollWorkflowService.corporateReject(
                id,
                corporateId,
                session.getUserId(),
                session.getRoles(),
                body != null ? body : WorkflowRemarksRequest.builder().build());
        return ResponseEntity.ok(ApiResponse.success("Rejected", null));
    }

    @PostMapping(value = "/batches/{id}/corporate/send-to-bank", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> corporateSendToBank(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody(required = false) WorkflowSendToBankRequest body) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER");
        UUID corporateId = requireCorporate(session);
        payrollWorkflowService.corporateSendToBank(
                id,
                corporateId,
                session.getUserId(),
                session.getRoles(),
                body != null ? body : WorkflowSendToBankRequest.builder().build());
        return ResponseEntity.ok(ApiResponse.success("Sent to bank", null));
    }

    @PostMapping(value = "/batches/{id}/bank/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> bankApprove(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody(required = false) WorkflowRemarksRequest body) {
        SessionInfo session = currentUser.requireRole(request, "BANK_ADMIN", "BANK_USER");
        UUID bankId = requireBank(session);
        payrollWorkflowService.bankApprove(
                id,
                bankId,
                session.getUserId(),
                session.getRoles(),
                body != null ? body : WorkflowRemarksRequest.builder().build());
        return ResponseEntity.ok(ApiResponse.success("Approved", null));
    }

    @PostMapping(value = "/batches/{id}/bank/send-back-to-corporate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> bankSendBackToCorporate(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody(required = false) WorkflowRemarksRequest body) {
        SessionInfo session = currentUser.requireRole(request, "BANK_ADMIN", "BANK_USER");
        UUID bankId = requireBank(session);
        payrollWorkflowService.bankSendBackToCorporate(
                id,
                bankId,
                session.getUserId(),
                session.getRoles(),
                body != null ? body : WorkflowRemarksRequest.builder().build());
        return ResponseEntity.ok(ApiResponse.success("Sent back to corporate", null));
    }

    @PostMapping(value = "/batches/{id}/bank/mark-process-payment", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> bankMarkProcessPayment(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody(required = false) WorkflowRemarksRequest body) {
        SessionInfo session = currentUser.requireRole(request, "BANK_ADMIN", "BANK_USER");
        UUID bankId = requireBank(session);
        payrollWorkflowService.bankMarkProcessPayment(
                id,
                bankId,
                session.getUserId(),
                session.getRoles(),
                body != null ? body : WorkflowRemarksRequest.builder().build());
        return ResponseEntity.ok(ApiResponse.success("Marked process payment", null));
    }

    private static UUID requireCorporate(SessionInfo session) {
        if (session.getCorporateId() == null) {
            throw new ForbiddenException("User must be linked to a corporate");
        }
        return session.getCorporateId();
    }

    private static UUID requireBank(SessionInfo session) {
        if (session.getBankId() == null) {
            throw new ForbiddenException("User must be linked to a bank");
        }
        return session.getBankId();
    }
}
