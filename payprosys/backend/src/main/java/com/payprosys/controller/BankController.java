package com.payprosys.controller;

import com.payprosys.auth.CurrentUser;
import com.payprosys.dto.ApiResponse;
import com.payprosys.dto.BankWithAdminDto;
import com.payprosys.dto.CreateBankResponse;
import com.payprosys.dto.CreateBankWithAdminRequest;
import com.payprosys.service.BankService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final CurrentUser currentUser;

    /** Super admin only: create bank and its first bank admin; returns credentials to hand to bank admin. */
    @PostMapping
    public ResponseEntity<ApiResponse<CreateBankResponse>> createBank(HttpServletRequest request, @Valid @RequestBody CreateBankWithAdminRequest body) {
        currentUser.requireRole(request, "SUPER_ADMIN");
        CreateBankResponse created = bankService.createBankWithAdmin(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Bank created", created));
    }

    /** Super admin only: list all banks with bank name and each bank admin's login (email/password). */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BankWithAdminDto>>> getAllBanks(HttpServletRequest request) {
        currentUser.requireRole(request, "SUPER_ADMIN");
        List<BankWithAdminDto> banks = bankService.getAllBanksWithAdmin();
        return ResponseEntity.ok(ApiResponse.success(banks));
    }
}
