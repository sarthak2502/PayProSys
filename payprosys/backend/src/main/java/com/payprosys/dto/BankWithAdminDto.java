package com.payprosys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Bank row for super admin: includes bank admin login credentials. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankWithAdminDto {
    private UUID id;
    private String name;
    private Instant createdAt;
    private String adminEmail;
    private String adminPassword;
    private String adminFirstName;
    private String adminLastName;
}
