package com.payprosys.controller;

import com.payprosys.auth.CurrentUser;
import com.payprosys.auth.SessionInfo;
import com.payprosys.dto.ApiResponse;
import com.payprosys.dto.CreateReviewAssignmentRequest;
import com.payprosys.dto.ReviewLevelAssignmentDto;
import com.payprosys.dto.TenantWorkflowConfigDto;
import com.payprosys.dto.WorkflowStepLabelUpdate;
import com.payprosys.exception.ForbiddenException;
import com.payprosys.service.WorkflowConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Phase 3: tenant workflow step labels and user ↔ review level assignments. */
@RestController
@RequestMapping("/api/payroll/workflow/config")
@RequiredArgsConstructor
public class WorkflowConfigController {

    private final WorkflowConfigService workflowConfigService;
    private final CurrentUser currentUser;

    @GetMapping("/corporate")
    public ResponseEntity<ApiResponse<TenantWorkflowConfigDto>> getCorporate(HttpServletRequest request) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN");
        UUID corporateId = requireCorporate(session);
        return ResponseEntity.ok(ApiResponse.success(workflowConfigService.getCorporateConfig(corporateId)));
    }

    @PutMapping(value = "/corporate/steps", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateCorporateSteps(
            HttpServletRequest request,
            @Valid @RequestBody List<WorkflowStepLabelUpdate> body) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN");
        UUID corporateId = requireCorporate(session);
        workflowConfigService.updateCorporateStepLabels(corporateId, body);
        return ResponseEntity.ok(ApiResponse.success("Steps updated", null));
    }

    @PostMapping(value = "/corporate/assignments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ReviewLevelAssignmentDto>> addCorporateAssignment(
            HttpServletRequest request,
            @Valid @RequestBody CreateReviewAssignmentRequest body) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN");
        UUID corporateId = requireCorporate(session);
        ReviewLevelAssignmentDto created = workflowConfigService.addCorporateAssignment(corporateId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Assignment created", created));
    }

    @DeleteMapping("/corporate/assignments/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> removeCorporateAssignment(
            HttpServletRequest request,
            @PathVariable UUID assignmentId) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN");
        UUID corporateId = requireCorporate(session);
        workflowConfigService.removeCorporateAssignment(corporateId, assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Assignment removed", null));
    }

    @GetMapping("/bank")
    public ResponseEntity<ApiResponse<TenantWorkflowConfigDto>> getBank(HttpServletRequest request) {
        SessionInfo session = currentUser.requireRole(request, "BANK_ADMIN");
        UUID bankId = requireBank(session);
        return ResponseEntity.ok(ApiResponse.success(workflowConfigService.getBankConfig(bankId)));
    }

    @PutMapping(value = "/bank/steps", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateBankSteps(
            HttpServletRequest request,
            @Valid @RequestBody List<WorkflowStepLabelUpdate> body) {
        SessionInfo session = currentUser.requireRole(request, "BANK_ADMIN");
        UUID bankId = requireBank(session);
        workflowConfigService.updateBankStepLabels(bankId, body);
        return ResponseEntity.ok(ApiResponse.success("Steps updated", null));
    }

    @PostMapping(value = "/bank/assignments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ReviewLevelAssignmentDto>> addBankAssignment(
            HttpServletRequest request,
            @Valid @RequestBody CreateReviewAssignmentRequest body) {
        SessionInfo session = currentUser.requireRole(request, "BANK_ADMIN");
        UUID bankId = requireBank(session);
        ReviewLevelAssignmentDto created = workflowConfigService.addBankAssignment(bankId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Assignment created", created));
    }

    @DeleteMapping("/bank/assignments/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> removeBankAssignment(
            HttpServletRequest request,
            @PathVariable UUID assignmentId) {
        SessionInfo session = currentUser.requireRole(request, "BANK_ADMIN");
        UUID bankId = requireBank(session);
        workflowConfigService.removeBankAssignment(bankId, assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Assignment removed", null));
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
