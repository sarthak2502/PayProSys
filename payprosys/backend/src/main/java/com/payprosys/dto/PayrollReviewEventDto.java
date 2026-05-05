package com.payprosys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollReviewEventDto {

    private UUID id;
    private UUID actorUserId;
    /** Display name for timeline (e.g. "L1 (Label)"). */
    private String actorStepChip;
    private String actorName;
    private String actorEmail;
    private String eventType;
    private String body;
    private boolean bankVisible;
    /** Shown on corporate-authored rows: visible to the bank side (cross-party or internal corp thread). */
    private boolean visibleToBank;
    /** Shown on bank-authored rows: visible to the corporate side (cross-party or internal bank thread). */
    private boolean visibleToCorporate;
    private Instant createdAt;
}
