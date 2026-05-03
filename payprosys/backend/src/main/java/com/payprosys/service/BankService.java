package com.payprosys.service;

import com.payprosys.dto.BankDto;
import com.payprosys.dto.BankWithAdminDto;
import com.payprosys.dto.CreateBankRequest;
import com.payprosys.dto.CreateBankResponse;
import com.payprosys.dto.CreateBankWithAdminRequest;

import java.util.List;

public interface BankService {

    /** Super admin: create bank and its first bank admin; returns bank + credentials. */
    CreateBankResponse createBankWithAdmin(CreateBankWithAdminRequest request);

    /** Super admin: list all banks with each bank's admin login details. */
    List<BankWithAdminDto> getAllBanksWithAdmin();

    @Deprecated
    BankDto createBank(CreateBankRequest request);

    @Deprecated
    List<BankDto> getAllBanks();
}
