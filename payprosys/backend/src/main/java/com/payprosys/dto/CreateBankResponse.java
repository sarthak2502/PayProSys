package com.payprosys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response after creating a bank: bank details + admin login for handing to bank admin. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBankResponse {
    private BankDto bank;
    private String adminEmail;
    private String adminPassword;
    private String adminFirstName;
    private String adminLastName;
}
