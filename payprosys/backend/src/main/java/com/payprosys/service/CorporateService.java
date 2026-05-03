package com.payprosys.service;

import com.payprosys.dto.CorporateDto;
import com.payprosys.dto.CorporateWithAdminDto;
import com.payprosys.dto.CreateCorporateRequest;
import com.payprosys.dto.CreateCorporateResponse;
import com.payprosys.dto.CreateCorporateWithAdminRequest;
import com.payprosys.dto.UserDto;

import java.util.List;
import java.util.UUID;

public interface CorporateService {

    /** Bank admin: create corporate and its first corporate admin; returns credentials. */
    CreateCorporateResponse createCorporateWithAdmin(UUID bankId, CreateCorporateWithAdminRequest request);

    /** Bank admin/user: list corporates for the bank with each corporate admin's login details. */
    List<CorporateWithAdminDto> getCorporatesByBankWithAdmin(UUID bankId);

    CorporateDto createCorporate(CreateCorporateRequest request);

    List<CorporateDto> getCorporatesByBank(UUID bankId);

    /** Bank admin: list bank users assigned to this corporate (corporate must belong to admin's bank). */
    List<UserDto> getAssignedBankUsers(UUID corporateId, UUID bankId);

    /** Bank admin: assign a bank user (of the same bank) to this corporate. */
    void assignBankUser(UUID corporateId, UUID userId, UUID bankId);

    /** Bank admin: unassign a bank user from this corporate. */
    void unassignBankUser(UUID corporateId, UUID userId, UUID bankId);
}
