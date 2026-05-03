package com.payprosys.service;

import com.payprosys.dto.PayrollBatchDto;
import com.payprosys.dto.PayrollRecordDto;
import com.payprosys.dto.PayrollUploadResponse;
import com.payprosys.entity.BankFlowState;
import com.payprosys.entity.Corporate;
import com.payprosys.entity.CorporateFlowState;
import com.payprosys.entity.PaymentBatchKind;
import com.payprosys.entity.PayrollBatch;
import com.payprosys.entity.PayrollBatchStatus;
import com.payprosys.entity.PayrollRecord;
import com.payprosys.entity.User;
import com.payprosys.exception.BadRequestException;
import com.payprosys.exception.ForbiddenException;
import com.payprosys.exception.ResourceNotFoundException;
import com.payprosys.mapper.PayrollBatchMapper;
import com.payprosys.repository.CorporateRepository;
import com.payprosys.repository.PayrollBatchRepository;
import com.payprosys.repository.PayrollRecordRepository;
import com.payprosys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private static final String EMPLOYEE_NAME = "Employee Name";
    private static final String ACCOUNT_NUMBER = "Account Number";
    private static final String JOINING_DATE = "Joining Date";
    private static final String CPR_ID = "CPR ID";
    private static final String AMOUNT = "Amount";
    private static final String PAYMENT_FOR = "Payment for";

    private final PayrollBatchRepository payrollBatchRepository;
    private final PayrollRecordRepository payrollRecordRepository;
    private final CorporateRepository corporateRepository;
    private final UserRepository userRepository;
    private final PayrollBatchMapper payrollBatchMapper;
    private final PayrollWorkflowService payrollWorkflowService;

    @Override
    @Transactional
    public PayrollUploadResponse uploadPayrollExcel(
            UUID corporateId, UUID uploadedById, String fileName, byte[] content, Integer yearMonth, PaymentBatchKind paymentBatchKind) {
        Corporate corporate = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate", corporateId));
        User uploadedBy = userRepository.findById(uploadedById)
                .orElseThrow(() -> new ResourceNotFoundException("User", uploadedById));

        ParsedPayroll parsed = parseExcel(content);

        PaymentBatchKind kind = paymentBatchKind != null ? paymentBatchKind : PaymentBatchKind.PAYROLL;
        PayrollBatch batch = PayrollBatch.builder()
                .corporate(corporate)
                .uploadedBy(uploadedBy)
                .totalRecords(parsed.getRecords().size())
                .totalAmount(parsed.getTotalAmount())
                .fileName(fileName)
                .yearMonth(yearMonth)
                .batchStatus(PayrollBatchStatus.PENDING)
                .corporateFlowState(CorporateFlowState.CORP_NEW)
                .currentCorporateReviewLevel(null)
                .paymentBatchKind(kind)
                .build();

        for (ParsedRecord pr : parsed.getRecords()) {
            PayrollRecord record = PayrollRecord.builder()
                    .payrollBatch(batch)
                    .employeeName(pr.getEmployeeName())
                    .accountNumber(pr.getAccountNumber())
                    .joiningDate(pr.getJoiningDate())
                    .cprId(pr.getCprId())
                    .amount(pr.getAmount())
                    .paymentFor(pr.getPaymentFor() != null ? pr.getPaymentFor() : "")
                    .build();
            batch.getPayrollRecords().add(record);
        }

        batch = payrollBatchRepository.save(batch);

        return PayrollUploadResponse.builder()
                .batchId(batch.getId())
                .totalRecords(batch.getTotalRecords())
                .totalAmount(batch.getTotalAmount())
                .fileName(batch.getFileName())
                .yearMonth(batch.getYearMonth())
                .batchStatus(PayrollBatchStatus.PENDING.name())
                .paymentBatchKind(kind.name())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollBatchDto getBatchById(UUID batchId, UUID actorCorporateId, UUID actorBankId, UUID actorUserId, List<String> actorRoles) {
        PayrollBatch batch = payrollBatchRepository.findByIdWithCorporateAndUploadedBy(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll batch", batchId));
        if (actorCorporateId != null) {
            if (!batch.getCorporate().getId().equals(actorCorporateId)) {
                throw new ForbiddenException("Cannot view this batch");
            }
            PayrollBatchDto dto = payrollBatchMapper.toDto(batch);
            if (actorUserId != null && actorRoles != null) {
                dto.setViewOnly(payrollWorkflowService.isCorporateViewOnlyInboxAccess(batch, actorUserId, actorRoles));
            }
            return dto;
        }
        if (actorBankId != null) {
            if (batch.getCorporate().getBank() == null || !batch.getCorporate().getBank().getId().equals(actorBankId)) {
                throw new ForbiddenException("Batch is not under your bank");
            }
            boolean visible = (batch.getBatchStatus() == PayrollBatchStatus.SUBMITTED
                    || batch.getBatchStatus() == PayrollBatchStatus.COMPLETED)
                    && batch.getCorporateFlowState() == CorporateFlowState.CORP_SENT_TO_BANK;
            boolean terminal = batch.getBankFlowState() == BankFlowState.BANK_PROCESS_PAYMENT;
            boolean awaitingCorporate = batch.getBatchStatus() == PayrollBatchStatus.PENDING
                    && batch.getCorporateFlowState() == CorporateFlowState.CORP_CLARIFICATION
                    && batch.getBankFlowState() == BankFlowState.BANK_SENT_TO_CORPORATE;
            if (!visible && !terminal && !awaitingCorporate) {
                throw new ForbiddenException("Batch is not visible to the bank yet");
            }
            PayrollBatchDto dto = payrollBatchMapper.toDto(batch);
            if (actorUserId != null && actorRoles != null) {
                dto.setViewOnly(payrollWorkflowService.isBankViewOnlyInboxAccess(batch, actorUserId, actorRoles));
            }
            return dto;
        }
        throw new ForbiddenException("Not authorized");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollBatchDto> getBatchesByCorporate(UUID corporateId, PayrollBatchStatus status, PaymentBatchKind paymentBatchKind) {
        List<PayrollBatch> batches;
        if (status == null) {
            batches = payrollBatchRepository.findByCorporateIdOrderByCreatedAtDesc(corporateId);
        } else if (status == PayrollBatchStatus.SUBMITTED) {
            batches = payrollBatchRepository.findByCorporateIdAndBatchStatusInOrderByCreatedAtDesc(
                    corporateId, EnumSet.of(PayrollBatchStatus.SUBMITTED, PayrollBatchStatus.COMPLETED));
        } else {
            batches = payrollBatchRepository.findByCorporateIdAndBatchStatusOrderByCreatedAtDesc(corporateId, status);
        }
        return batches.stream()
                .filter(b -> paymentBatchKind == null || b.getPaymentBatchKind() == paymentBatchKind)
                .map(payrollBatchMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollBatchDto> getSubmittedBatchesForBank(UUID bankId) {
        return payrollBatchRepository.findByBankIdAndBatchStatusOrderByCreatedAtDesc(bankId, PayrollBatchStatus.SUBMITTED).stream()
                .filter(b -> b.getBankFlowState() != BankFlowState.BANK_PROCESS_PAYMENT)
                .map(payrollBatchMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollBatchDto> getCompletedBatchesForBank(UUID bankId) {
        return payrollBatchRepository.findByBankIdAndBatchStatusOrderByCreatedAtDesc(bankId, PayrollBatchStatus.COMPLETED).stream()
                .map(payrollBatchMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void submitBatch(UUID batchId, UUID corporateId, UUID actorUserId) {
        payrollWorkflowService.submitBatchLegacy(batchId, corporateId, actorUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCorporateUnderBank(UUID corporateId, UUID bankId) {
        Corporate c = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate", corporateId));
        if (c.getBank() == null || !c.getBank().getId().equals(bankId)) {
            throw new ForbiddenException("Corporate is not under your bank");
        }
    }

    @Override
    @Transactional
    public void deleteBatch(UUID batchId, UUID corporateId) {
        PayrollBatch batch = payrollBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll batch", batchId));
        if (!batch.getCorporate().getId().equals(corporateId)) {
            throw new ForbiddenException("Batch does not belong to your corporate");
        }
        if (batch.getBatchStatus() != PayrollBatchStatus.PENDING) {
            throw new BadRequestException("Submitted batches cannot be deleted");
        }
        if (batch.getCorporateFlowState() != CorporateFlowState.CORP_NEW
                || batch.getCurrentCorporateReviewLevel() != null) {
            throw new BadRequestException("Only draft batches (not yet submitted for review) can be deleted");
        }
        payrollBatchRepository.delete(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollRecordDto> getRecordsByCorporate(UUID corporateId, Integer yearMonth) {
        var rollupStatuses = EnumSet.of(PayrollBatchStatus.SUBMITTED, PayrollBatchStatus.COMPLETED);
        List<PayrollRecord> records = yearMonth != null
                ? payrollRecordRepository.findByCorporateIdAndYearMonthAndBatchStatusesOrder(corporateId, yearMonth, rollupStatuses)
                : payrollRecordRepository.findByCorporateIdAndBatchStatusesOrderByMonthAndName(corporateId, rollupStatuses);
        return records.stream().map(this::toRecordDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollRecordDto> getRecordsByBatchId(UUID batchId, UUID actorCorporateId, UUID actorBankId) {
        PayrollBatch batch = payrollBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll batch", batchId));
        if (actorCorporateId != null) {
            if (!batch.getCorporate().getId().equals(actorCorporateId)) {
                throw new ForbiddenException("Cannot view records for another corporate");
            }
        } else if (actorBankId != null) {
            if (!batch.getCorporate().getBank().getId().equals(actorBankId)) {
                throw new ForbiddenException("Batch is not under your bank");
            }
            boolean okSubmitted = batch.getBatchStatus() == PayrollBatchStatus.SUBMITTED
                    && batch.getCorporateFlowState() == CorporateFlowState.CORP_SENT_TO_BANK;
            boolean okCompleted = batch.getBatchStatus() == PayrollBatchStatus.COMPLETED
                    && batch.getCorporateFlowState() == CorporateFlowState.CORP_SENT_TO_BANK
                    && batch.getBankFlowState() == BankFlowState.BANK_PROCESS_PAYMENT;
            boolean okRecall = batch.getBatchStatus() == PayrollBatchStatus.PENDING
                    && batch.getCorporateFlowState() == CorporateFlowState.CORP_CLARIFICATION
                    && batch.getBankFlowState() == BankFlowState.BANK_SENT_TO_CORPORATE;
            if (!okSubmitted && !okCompleted && !okRecall) {
                throw new ForbiddenException("Records are not visible to the bank for this batch state");
            }
        } else {
            throw new ForbiddenException("Not authorized");
        }
        return payrollRecordRepository.findByBatchIdWithFetch(batchId).stream()
                .map(this::toRecordDto).toList();
    }

    private PayrollRecordDto toRecordDto(PayrollRecord r) {
        PayrollBatch b = r.getPayrollBatch();
        return PayrollRecordDto.builder()
                .id(r.getId())
                .payrollBatchId(b.getId())
                .yearMonth(b.getYearMonth())
                .batchCreatedAt(b.getCreatedAt())
                .fileName(b.getFileName())
                .employeeName(r.getEmployeeName())
                .accountNumber(r.getAccountNumber())
                .joiningDate(r.getJoiningDate())
                .cprId(r.getCprId())
                .amount(r.getAmount())
                .paymentFor(r.getPaymentFor())
                .build();
    }

    private ParsedPayroll parseExcel(byte[] content) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BadRequestException("Excel file has no sheet");
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BadRequestException("Excel file has no header row");
            }
            int empNameCol = findColumnIndex(headerRow, EMPLOYEE_NAME);
            int accNumCol = findColumnIndex(headerRow, ACCOUNT_NUMBER);
            int joinDateCol = findColumnIndex(headerRow, JOINING_DATE);
            int cprCol = findColumnIndex(headerRow, CPR_ID);
            int amountCol = findColumnIndex(headerRow, AMOUNT);
            int paymentForCol = findColumnIndex(headerRow, PAYMENT_FOR);

            if (empNameCol < 0 || accNumCol < 0 || joinDateCol < 0 || cprCol < 0 || amountCol < 0) {
                throw new BadRequestException("Required columns missing. Expected: Employee Name, Account Number, Joining Date, CPR ID, Amount. Optional: Payment for");
            }

            List<ParsedRecord> records = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String empName = getCellString(row.getCell(empNameCol));
                String accNum = getCellString(row.getCell(accNumCol));
                LocalDate joinDate = getCellDate(row.getCell(joinDateCol));
                String cprId = getCellString(row.getCell(cprCol));
                BigDecimal amount = getCellBigDecimal(row.getCell(amountCol));
                String paymentFor = paymentForCol >= 0 ? getCellString(row.getCell(paymentForCol)) : null;
                if (empName == null || empName.isBlank()) continue;
                if (accNum == null || accNum.isBlank() || joinDate == null || cprId == null || cprId.isBlank() || amount == null) {
                    throw new BadRequestException("Row " + (i + 1) + ": All required fields must be present and valid");
                }
                records.add(new ParsedRecord(empName, accNum, joinDate, cprId, amount, paymentFor != null ? paymentFor.trim() : ""));
                totalAmount = totalAmount.add(amount);
            }

            return new ParsedPayroll(records, totalAmount);
        } catch (IOException e) {
            throw new BadRequestException("Invalid Excel file: " + e.getMessage());
        }
    }

    private static int findColumnIndex(Row headerRow, String name) {
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null && name.equalsIgnoreCase(getCellString(cell).trim())) {
                return i;
            }
        }
        return -1;
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return cell.toString();
    }

    private static LocalDate getCellDate(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                Date d = cell.getDateCellValue();
                return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            // Excel stores dates as serial numbers; convert even when cell has no date format
            try {
                Date d = DateUtil.getJavaDate(cell.getNumericCellValue());
                return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (Exception ignored) {
            }
        }
        String s = getCellString(cell);
        if (s != null && !s.isBlank()) {
            try {
                return LocalDate.parse(s.trim());
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static BigDecimal getCellBigDecimal(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String s = getCellString(cell);
        if (s != null && !s.isBlank()) {
            try {
                return new BigDecimal(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static class ParsedPayroll {
        private final List<ParsedRecord> records;
        private final BigDecimal totalAmount;

        ParsedPayroll(List<ParsedRecord> records, BigDecimal totalAmount) {
            this.records = records;
            this.totalAmount = totalAmount;
        }

        List<ParsedRecord> getRecords() { return records; }
        BigDecimal getTotalAmount() { return totalAmount; }
    }

    private static class ParsedRecord {
        private final String employeeName;
        private final String accountNumber;
        private final LocalDate joiningDate;
        private final String cprId;
        private final BigDecimal amount;
        private final String paymentFor;

        ParsedRecord(String employeeName, String accountNumber, LocalDate joiningDate, String cprId, BigDecimal amount, String paymentFor) {
            this.employeeName = employeeName;
            this.accountNumber = accountNumber;
            this.joiningDate = joiningDate;
            this.cprId = cprId;
            this.amount = amount;
            this.paymentFor = paymentFor;
        }

        String getEmployeeName() { return employeeName; }
        String getAccountNumber() { return accountNumber; }
        LocalDate getJoiningDate() { return joiningDate; }
        String getCprId() { return cprId; }
        BigDecimal getAmount() { return amount; }
        String getPaymentFor() { return paymentFor; }
    }
}
