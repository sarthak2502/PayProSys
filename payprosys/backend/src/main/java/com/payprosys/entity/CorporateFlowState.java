package com.payprosys.entity;

/** Corporate-side batch lifecycle (see docs/WORKFLOW_REQUIREMENTS.md). */
public enum CorporateFlowState {
    CORP_NEW,
    CORP_IN_REVIEW,
    CORP_CLARIFICATION,
    CORP_APPROVED_HOLD,
    CORP_REJECTED,
    CORP_SENT_TO_BANK
}
