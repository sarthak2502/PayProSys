package com.payprosys.dto;

import com.payprosys.entity.CorporateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Corporate row for bank admin: includes corporate admin login credentials. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateWithAdminDto {
    private UUID id;
    private String name;
    private CorporateStatus status;
    private Instant createdAt;
    private UUID bankId;
    private String adminEmail;
    private String adminPassword;
    private String adminFirstName;
    private String adminLastName;
}
