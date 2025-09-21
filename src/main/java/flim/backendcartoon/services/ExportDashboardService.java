/*
 * @(#) $(NAME).java    1.0     9/20/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 20-September-2025 2:46 PM
 */

import flim.backendcartoon.entities.DTO.response.QuickStatsResponse;
import flim.backendcartoon.entities.DTO.response.RecentTransactionResponse;
import flim.backendcartoon.entities.DTO.response.RevenueChartResponse;
import flim.backendcartoon.entities.DTO.response.RevenueSummaryResponse;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportDashboardService {
    private final DataAnalyzerService dataAnalyzerService;

    public ExportDashboardService(DataAnalyzerService dataAnalyzerService) {
        this.dataAnalyzerService = dataAnalyzerService;
    }

    public void exportDashboard(HttpServletResponse response, int year, int month) throws IOException {
        // Lấy dữ liệu
        RevenueSummaryResponse summary = dataAnalyzerService.getSummary(year, month);
        QuickStatsResponse quickStats = dataAnalyzerService.getQuickStats();
        RevenueChartResponse chart = dataAnalyzerService.getRevenueByMonth(year);
        List<RecentTransactionResponse> transactions = dataAnalyzerService.getRecentTransactions(20); // có thể lấy nhiều hơn

        Workbook workbook = new XSSFWorkbook();

        // ===== SHEET 1: Summary =====
        Sheet sheet1 = workbook.createSheet("Summary");
        int rowIdx = 0;
        sheet1.createRow(rowIdx++).createCell(0).setCellValue("Tổng doanh thu");
        sheet1.getRow(rowIdx-1).createCell(1).setCellValue(summary.getTotalRevenue());
        sheet1.createRow(rowIdx++).createCell(0).setCellValue("Doanh thu tháng");
        sheet1.getRow(rowIdx-1).createCell(1).setCellValue(summary.getMonthlyRevenue());
        sheet1.createRow(rowIdx++).createCell(0).setCellValue("Tổng giao dịch");
        sheet1.getRow(rowIdx-1).createCell(1).setCellValue(summary.getTotalTransactions());
        sheet1.createRow(rowIdx++).createCell(0).setCellValue("GD tháng này");
        sheet1.getRow(rowIdx-1).createCell(1).setCellValue(summary.getMonthlyTransactions());

        // ===== SHEET 2: Quick Stats =====
        Sheet sheet2 = workbook.createSheet("Quick Stats");
        rowIdx = 0;
        sheet2.createRow(rowIdx++).createCell(0).setCellValue("Doanh thu hôm nay");
        sheet2.getRow(rowIdx-1).createCell(1).setCellValue(quickStats.getTodayRevenue());
        sheet2.createRow(rowIdx++).createCell(0).setCellValue("Doanh thu tuần");
        sheet2.getRow(rowIdx-1).createCell(1).setCellValue(quickStats.getWeekRevenue());
        sheet2.createRow(rowIdx++).createCell(0).setCellValue("Tăng trưởng (%)");
        sheet2.getRow(rowIdx-1).createCell(1).setCellValue(quickStats.getGrowthPercent());
        sheet2.createRow(rowIdx++).createCell(0).setCellValue("Gói phổ biến nhất");
        sheet2.getRow(rowIdx-1).createCell(1).setCellValue(quickStats.getPopularPackage());

        // ===== SHEET 3: Chart (Doanh thu theo tháng) =====
        Sheet sheet3 = workbook.createSheet("Revenue Chart");
        Row headerRow = sheet3.createRow(0);
        headerRow.createCell(0).setCellValue("Tháng");
        headerRow.createCell(1).setCellValue("Doanh thu");
        for (int i = 0; i < chart.getLabels().size(); i++) {
            Row row = sheet3.createRow(i + 1);
            row.createCell(0).setCellValue(chart.getLabels().get(i));
            row.createCell(1).setCellValue(chart.getData().get(i));
        }

        // ===== SHEET 4: Recent Transactions =====
        Sheet sheet4 = workbook.createSheet("Transactions");
        String[] headers = {"Order ID", "User", "Package", "Amount (VND)", "Date", "Status"};
        Row head = sheet4.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            head.createCell(i).setCellValue(headers[i]);
        }

        int txRow = 1;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (RecentTransactionResponse tx : transactions) {
            Row row = sheet4.createRow(txRow++);
            row.createCell(0).setCellValue(tx.getOrderId());
            row.createCell(1).setCellValue(tx.getUserName());
            row.createCell(2).setCellValue(tx.getPackageId());
            row.createCell(3).setCellValue(tx.getFinalAmount());
            row.createCell(4).setCellValue(tx.getCreatedAt().format(fmt));
            row.createCell(5).setCellValue(tx.getStatus());
        }

        // Auto size columns
        for (Sheet sheet : List.of(sheet1, sheet2, sheet3, sheet4)) {
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
        }

        // Xuất file
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=dashboard.xlsx");
        ServletOutputStream out = response.getOutputStream();
        workbook.write(out);
        workbook.close();
        out.close();
    }
}
