package com.payprosys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationProfileDto {

    /** BANK, CORPORATE, or null when not applicable (e.g. super admin). */
    private String organizationType;
    private UUID organizationId;
    private String organizationName;
    private String logoUrl;
}
