package com.payprosys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Which workflow actions the current user may attempt on a batch (aligned with server-side checks). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowActionHintsDto {

    private int maxCorporateReviewLevel;
    private int maxBankReviewLevel;

    @Builder.Default
    private boolean showCorporateApprove = false;

    @Builder.Default
    private boolean showCorporateReject = false;

    @Builder.Default
    private boolean showCorporateSendBack = false;

    @Builder.Default
    private boolean showCorporateSendToBank = false;

    @Builder.Default
    private boolean showBankApprove = false;

    /** No bank reject API in this POC; kept for a uniform UI when enabled later. */
    @Builder.Default
    private boolean showBankReject = false;

    @Builder.Default
    private boolean showBankSendBack = false;

    @Builder.Default
    private boolean showBankProcessPayment = false;
}
