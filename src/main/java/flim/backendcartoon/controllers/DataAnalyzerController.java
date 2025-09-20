/*
 * @(#) $(NAME).java    1.0     9/15/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.response.RevenueChartResponse;
import flim.backendcartoon.entities.DTO.response.RevenueSummaryResponse;
import flim.backendcartoon.services.DataAnalyzerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 15-September-2025 4:39 PM
 */
@RestController
@RequestMapping("/data-analyzer")
@RequiredArgsConstructor
public class DataAnalyzerController {

    @Autowired
    private final DataAnalyzerService revenueService;

    // tổng quan doanh thu
    @GetMapping("/revenue/summary")
    public ResponseEntity<RevenueSummaryResponse> getSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        LocalDate now = LocalDate.now();
        int y = (year != null) ? year : now.getYear();
        int m = (month != null) ? month : now.getMonthValue();

        return ResponseEntity.ok(revenueService.getSummary(y, m));
    }

    // Doanh thu theo ngày trong 1 tháng
    @GetMapping("/revenue/day")
    public RevenueChartResponse getRevenueByDay(
            @RequestParam int year,
            @RequestParam int month) {
        return revenueService.getRevenueByDay(year, month);
    }

    // Doanh thu theo 12 tháng trong 1 năm
    @GetMapping("/revenue/month")
    public RevenueChartResponse getRevenueByMonth(@RequestParam int year) {
        return revenueService.getRevenueByMonth(year);
    }

    // Doanh thu theo nhiều năm (from → to)
    @GetMapping("/revenue/year")
    public RevenueChartResponse getRevenueByYear(
            @RequestParam int from,
            @RequestParam int to) {
        return revenueService.getRevenueByYear(from, to);
    }

    // thống kê nhanh
    @GetMapping("/revenue/quick-stats")
    public ResponseEntity<?> getQuickStats() {
        return ResponseEntity.ok(revenueService.getQuickStats());
    }

    // giao dịch gần đây
    @GetMapping("/revenue/recent-transactions")
    public ResponseEntity<?> getRecentTransaction() {
        return ResponseEntity.ok(revenueService.getRecentTransactions(5));
    }
}
