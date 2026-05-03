package com.payprosys.util;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * One-off utility to generate sample payroll Excel. Run from IDE or:
 * mvn exec:java -Dexec.mainClass="com.payprosys.util.SamplePayrollExcelGenerator"
 */
public final class SamplePayrollExcelGenerator {

    public static void main(String[] args) throws IOException {
        String outPath = args.length > 0 ? args[0] : "sample-payroll-employees.xlsx";
        generate(outPath);
        System.out.println("Created: " + outPath);
    }

    public static void generate(String outPath) throws IOException {
        List<Object[]> rows = List.of(
            new Object[]{"Employee Name", "Account Number", "Joining Date", "CPR ID", "Amount", "Payment for"},
            new Object[]{"Alice Johnson", "ACC001234", LocalDate.of(2022, 1, 15), "CPR-100001", new BigDecimal("4500.00"), "Salary"},
            new Object[]{"Bob Smith", "ACC001235", LocalDate.of(2021, 6, 1), "CPR-100002", new BigDecimal("5200.50"), "Salary"},
            new Object[]{"Carol Williams", "ACC001236", LocalDate.of(2023, 3, 10), "CPR-100003", new BigDecimal("3800.00"), "Salary"},
            new Object[]{"David Brown", "ACC001237", LocalDate.of(2020, 11, 20), "CPR-100004", new BigDecimal("6100.00"), "Salary"},
            new Object[]{"Eva Davis", "ACC001238", LocalDate.of(2022, 8, 5), "CPR-100005", new BigDecimal("2950.75"), "Reimbursement"},
            new Object[]{"Frank Miller", "ACC001239", LocalDate.of(2021, 2, 28), "CPR-100006", new BigDecimal("4700.00"), "Salary"},
            new Object[]{"Grace Lee", "ACC001240", LocalDate.of(2023, 9, 12), "CPR-100007", new BigDecimal("3300.00"), "Bonus"},
            new Object[]{"Henry Wilson", "ACC001241", LocalDate.of(2019, 4, 1), "CPR-100008", new BigDecimal("5500.25"), "Salary"},
            new Object[]{"Ivy Taylor", "ACC001242", LocalDate.of(2022, 11, 15), "CPR-100009", new BigDecimal("4100.00"), "Salary"},
            new Object[]{"Jack Martinez", "ACC001243", LocalDate.of(2024, 1, 8), "CPR-100010", new BigDecimal("3600.00"), "Salary"}
        );

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("Payroll");
            for (int i = 0; i < rows.size(); i++) {
                var row = sheet.createRow(i);
                Object[] cells = rows.get(i);
                for (int j = 0; j < cells.length; j++) {
                    var cell = row.createCell(j);
                    Object v = cells[j];
                    if (v instanceof String s) cell.setCellValue(s);
                    else if (v instanceof Number n) cell.setCellValue(n.doubleValue());
                    else if (v instanceof LocalDate ld) cell.setCellValue(java.sql.Date.valueOf(ld));
                }
            }
            for (int j = 0; j < 6; j++) sheet.autoSizeColumn(j);
            try (var out = new FileOutputStream(outPath)) {
                wb.write(out);
            }
        }
    }
}
