package com.payprosys.controller;

import com.payprosys.auth.CurrentUser;
import com.payprosys.auth.SessionInfo;
import com.payprosys.dto.ApiResponse;
import com.payprosys.dto.PayrollBatchDto;
import com.payprosys.dto.PayrollRecordDto;
import com.payprosys.dto.PayrollUploadResponse;
import com.payprosys.entity.PaymentBatchKind;
import com.payprosys.entity.PayrollBatchStatus;
import com.payprosys.exception.BadRequestException;
import com.payprosys.exception.ForbiddenException;
import com.payprosys.service.PayrollService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final CurrentUser currentUser;

    /** Corporate users: upload Excel for their corporate. Month format YYYYMM (e.g. 202503). Batch starts as PENDING. */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PayrollUploadResponse>> uploadPayroll(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam("month") String month,
            @RequestParam(value = "paymentBatchKind", required = false, defaultValue = "PAYROLL") String paymentBatchKind) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER");
        if (!session.getRoles().contains("CORP_USER")) {
            throw new ForbiddenException("Only corporate payroll users may upload files");
        }
        if (session.getCorporateId() == null) {
            throw new ForbiddenException("User must be linked to a corporate");
        }
        Integer yearMonth = parseYearMonth(month);
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is required"));
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xlsx")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only .xlsx files are allowed"));
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (java.io.IOException e) {
            throw new BadRequestException("Failed to read file: " + e.getMessage());
        }
        PaymentBatchKind kind = parsePaymentBatchKind(paymentBatchKind);
        PayrollUploadResponse response = payrollService.uploadPayrollExcel(
                session.getCorporateId(), session.getUserId(), filename, content, yearMonth, kind);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Payroll uploaded", response));
    }

    private static Integer parseYearMonth(String month) {
        if (month == null || month.isBlank()) {
            throw new BadRequestException("Month is required (format: YYYY-MM or YYYYMM, e.g. 2025-03 or 202503)");
        }
        String normalized = month.replace("-", "").trim();
        if (normalized.length() != 6) {
            throw new BadRequestException("Month must be YYYY-MM or YYYYMM");
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid month format");
        }
    }

    /** Corporate: all or filtered by status (PENDING / SUBMITTED includes completed / COMPLETED). Bank: supply corporateId only for their corporates. */
    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<List<PayrollBatchDto>>> getBatches(HttpServletRequest request,
            @RequestParam UUID corporateId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentBatchKind) {
        var session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER", "BANK_ADMIN", "BANK_USER");
        PayrollBatchStatus parsed = parseOptionalStatus(status);
        PaymentBatchKind kindFilter = parseOptionalPaymentBatchKind(paymentBatchKind);
        if (session.getCorporateId() != null) {
            if (!session.getCorporateId().equals(corporateId)) {
                throw new ForbiddenException("Cannot view another corporate's batches");
            }
            return ResponseEntity.ok(ApiResponse.success(payrollService.getBatchesByCorporate(corporateId, parsed, kindFilter)));
        }
        // bank user: caller must only request corporates belonging to their bank (light check via service would need corporate repo)
        if (session.getBankId() == null) {
            throw new ForbiddenException("Not authorized");
        }
        // For bank, only allow listing per corporate they know; optional: verify corporate belongs to bank via repository
        payrollService.assertCorporateUnderBank(corporateId, session.getBankId());
        return ResponseEntity.ok(ApiResponse.success(payrollService.getBatchesByCorporate(corporateId, parsed, kindFilter)));
    }

    /** Bank: all SUBMITTED batches across corporates of this bank (no corporateId). */
    @GetMapping("/batches/submitted-for-bank")
    public ResponseEntity<ApiResponse<List<PayrollBatchDto>>> getSubmittedForBank(HttpServletRequest request) {
        var session = currentUser.requireRole(request, "BANK_ADMIN", "BANK_USER");
        if (session.getBankId() == null) {
            throw new ForbiddenException("User must be linked to a bank");
        }
        List<PayrollBatchDto> batches = payrollService.getSubmittedBatchesForBank(session.getBankId());
        return ResponseEntity.ok(ApiResponse.success(batches));
    }

    /** Bank: completed (payment processed) batches across corporates of this bank. */
    @GetMapping("/batches/completed-for-bank")
    public ResponseEntity<ApiResponse<List<PayrollBatchDto>>> getCompletedForBank(HttpServletRequest request) {
        var session = currentUser.requireRole(request, "BANK_ADMIN", "BANK_USER");
        if (session.getBankId() == null) {
            throw new ForbiddenException("User must be linked to a bank");
        }
        List<PayrollBatchDto> batches = payrollService.getCompletedBatchesForBank(session.getBankId());
        return ResponseEntity.ok(ApiResponse.success(batches));
    }

    /** Corporate: own batch. Bank: only when submitted to bank or terminal process-payment. */
    @GetMapping("/batches/{id}")
    public ResponseEntity<ApiResponse<PayrollBatchDto>> getBatch(HttpServletRequest request, @PathVariable UUID id) {
        var session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER", "BANK_ADMIN", "BANK_USER");
        UUID corp = session.getCorporateId();
        UUID bank = corp == null ? session.getBankId() : null;
        PayrollBatchDto dto = payrollService.getBatchById(id, corp, bank, session.getUserId(), session.getRoles());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    private static PayrollBatchStatus parseOptionalStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PayrollBatchStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("status must be PENDING, SUBMITTED, or COMPLETED");
        }
    }

    private static PaymentBatchKind parsePaymentBatchKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return PaymentBatchKind.PAYROLL;
        }
        try {
            return PaymentBatchKind.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("paymentBatchKind must be PAYROLL or VENDOR");
        }
    }

    private static PaymentBatchKind parseOptionalPaymentBatchKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parsePaymentBatchKind(raw);
    }

    @PostMapping("/batches/{id}/submit")
    public ResponseEntity<ApiResponse<Void>> submitBatch(HttpServletRequest request, @PathVariable UUID id) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER");
        if (session.getCorporateId() == null) {
            throw new ForbiddenException("User must be linked to a corporate");
        }
        payrollService.submitBatch(id, session.getCorporateId(), session.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Batch submitted", null));
    }

    @DeleteMapping("/batches/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(HttpServletRequest request, @PathVariable UUID id) {
        SessionInfo session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER");
        if (session.getCorporateId() == null) {
            throw new ForbiddenException("User must be linked to a corporate");
        }
        payrollService.deleteBatch(id, session.getCorporateId());
        return ResponseEntity.ok(ApiResponse.success("Batch deleted", null));
    }

    @GetMapping("/records")
    public ResponseEntity<ApiResponse<List<PayrollRecordDto>>> getRecords(HttpServletRequest request,
            @RequestParam UUID corporateId,
            @RequestParam(required = false) Integer month) {
        var session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER", "BANK_ADMIN", "BANK_USER");
        if (session.getCorporateId() != null && !session.getCorporateId().equals(corporateId)) {
            throw new ForbiddenException("Cannot view another corporate's payroll records");
        }
        if (session.getBankId() != null && session.getCorporateId() == null) {
            payrollService.assertCorporateUnderBank(corporateId, session.getBankId());
        }
        List<PayrollRecordDto> records = payrollService.getRecordsByCorporate(corporateId, month);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/records/by-batch")
    public ResponseEntity<ApiResponse<List<PayrollRecordDto>>> getRecordsByBatch(HttpServletRequest request,
            @RequestParam UUID batchId) {
        var session = currentUser.requireRole(request, "CORP_ADMIN", "CORP_USER", "BANK_ADMIN", "BANK_USER");
        UUID corp = session.getCorporateId();
        UUID bank = corp == null ? session.getBankId() : null;
        List<PayrollRecordDto> records = payrollService.getRecordsByBatchId(batchId, corp, bank);
        return ResponseEntity.ok(ApiResponse.success(records));
    }
}
