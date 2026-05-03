package com.payprosys.service;

import com.payprosys.dto.CreateReviewAssignmentRequest;
import com.payprosys.dto.ReviewLevelAssignmentDto;
import com.payprosys.dto.TenantWorkflowConfigDto;
import com.payprosys.dto.WorkflowStepLabelUpdate;

import java.util.List;
import java.util.UUID;

public interface WorkflowConfigService {

    TenantWorkflowConfigDto getCorporateConfig(UUID corporateId);

    void updateCorporateStepLabels(UUID corporateId, List<WorkflowStepLabelUpdate> updates);

    ReviewLevelAssignmentDto addCorporateAssignment(UUID corporateId, CreateReviewAssignmentRequest request);

    void removeCorporateAssignment(UUID corporateId, UUID assignmentId);

    TenantWorkflowConfigDto getBankConfig(UUID bankId);

    void updateBankStepLabels(UUID bankId, List<WorkflowStepLabelUpdate> updates);

    ReviewLevelAssignmentDto addBankAssignment(UUID bankId, CreateReviewAssignmentRequest request);

    void removeBankAssignment(UUID bankId, UUID assignmentId);
}
