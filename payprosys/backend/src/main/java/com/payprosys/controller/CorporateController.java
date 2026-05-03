package com.payprosys.controller;

import com.payprosys.auth.CurrentUser;
import com.payprosys.auth.SessionInfo;
import com.payprosys.dto.ApiResponse;
import com.payprosys.dto.CorporateWithAdminDto;
import com.payprosys.dto.CreateCorporateResponse;
import com.payprosys.dto.CreateCorporateWithAdminRequest;
import com.payprosys.exception.ForbiddenException;
import com.payprosys.service.CorporateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/corporates")
@RequiredArgsConstructor
public class CorporateController {

    private final CorporateService corporateService;
    private final CurrentUser currentUser;

    /** Bank admin only: create corporate and its first corporate admin; use current user's bank. */
    @PostMapping
    public ResponseEntity<ApiResponse<CreateCorporateResponse>> createCorporate(HttpServletRequest request, @Valid @RequestBody CreateCorporateWithAdminRequest body) {
        SessionInfo session = currentUser.requireRole(request, "BANK_ADMIN");
        if (session.getBankId() == null) {
            throw new ForbiddenException("Bank admin must be linked to a bank");
        }
        CreateCorporateResponse created = corporateService.createCorporateWithAdmin(session.getBankId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Corporate created", created));
    }

    /** Bank admin/user: list corporates for their bank only, with each corporate admin's login details. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CorporateWithAdminDto>>> getCorporates(HttpServletRequest request) {
        SessionInfo session = currentUser.requireRole(request, "BANK_ADMIN", "BANK_USER");
        if (session.getBankId() == null) {
            throw new ForbiddenException("User must be linked to a bank");
        }
        List<CorporateWithAdminDto> corporates = corporateService.getCorporatesByBankWithAdmin(session.getBankId());
        return ResponseEntity.ok(ApiResponse.success(corporates));
    }

    /** Bank admin: list bank users assigned to this corporate. */
    @GetMapping("/{id}/bank-users")
    public ResponseEntity<ApiResponse<List<com.payprosys.dto.UserDto>>> getAssignedBankUsers(HttpServletRequest request, @PathVariable UUID id) {
        SessionInfo session = currentUser.requireRole(request, "BANK_ADMIN");
        if (session.getBankId() == null) {
            throw new ForbiddenException("User must be linked to a bank");
        }
        List<com.payprosys.dto.UserDto> users = corporateService.getAssignedBankUsers(id, session.getBankId());
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /** Bank admin: assign a bank user to this corporate. */
    @PostMapping("/{id}/bank-users")
    public ResponseEntity<ApiResponse<Void>> assignBankUser(HttpServletRequest request, @PathVariable UUID id, @RequestBody Map<String, String> body) {
        SessionInfo session = currentUser.requireRole(request, "BANK_ADMIN");
        if (session.getBankId() == null) {
            throw new ForbiddenException("User must be linked to a bank");
        }
        String userIdStr = body.get("userId");
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new com.payprosys.exception.BadRequestException("userId is required");
        }
        corporateService.assignBankUser(id, UUID.fromString(userIdStr), session.getBankId());
        return ResponseEntity.ok(ApiResponse.success("Bank user assigned", null));
    }

    /** Bank admin: unassign a bank user from this corporate. */
    @DeleteMapping("/{id}/bank-users/{userId}")
    public ResponseEntity<ApiResponse<Void>> unassignBankUser(HttpServletRequest request, @PathVariable UUID id, @PathVariable UUID userId) {
        SessionInfo session = currentUser.requireRole(request, "BANK_ADMIN");
        if (session.getBankId() == null) {
            throw new ForbiddenException("User must be linked to a bank");
        }
        corporateService.unassignBankUser(id, userId, session.getBankId());
        return ResponseEntity.ok(ApiResponse.success("Bank user unassigned", null));
    }
}
