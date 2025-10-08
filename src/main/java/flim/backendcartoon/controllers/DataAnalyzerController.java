/*
 * @(#) $(NAME).java    1.0     9/15/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.response.*;
import flim.backendcartoon.services.DataAnalyzerService;
import flim.backendcartoon.services.ExportDashboardService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

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
    @Autowired
    private final ExportDashboardService exportDashboardService;

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
    public RevenueChartResponse getRevenueByDay(@RequestParam int year, @RequestParam int month) {
        return revenueService.getRevenueByDay(year, month);
    }

    @GetMapping("/revenue/month")
    public RevenueChartResponse getRevenueByMonth(@RequestParam int year) {
        return revenueService.getRevenueByMonth(year);
    }

    @GetMapping("/revenue/year")
    public RevenueChartResponse getRevenueByYear(@RequestParam int from, @RequestParam int to) {
        return revenueService.getRevenueByYear(from, to);
    }

    // thống kê nhanh
    @GetMapping("/revenue/quick-stats")
    public ResponseEntity<QuickStatsResponse> getQuickStats() {
        return ResponseEntity.ok(revenueService.getQuickStats());
    }
    //giao dịch gần đây
    @GetMapping("/revenue/recent-transactions")
    public ResponseEntity<?> getRecentTransaction() {
        return ResponseEntity.ok(revenueService.getRecentTransactions(5));
    }

    // ===== Revenue (date range + groupBy) =====
    @GetMapping("/revenue/range")
    public RevenueChartResponse getRevenueByRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "DAY") GroupByDataAnalzerResponse groupBy) {
        return revenueService.getRevenueByRange(LocalDate.parse(startDate), LocalDate.parse(endDate), groupBy);
    }

    @GetMapping("/revenue/range/summary")
    public RevenueSummaryResponse getRevenueSummaryByRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return revenueService.getRevenueSummaryByRange(LocalDate.parse(startDate), LocalDate.parse(endDate));
    }

    // Recent transactions có phân trang + (optional) range filter
    @GetMapping("/revenue/recent-transactions/paged")
    public PagedResponse<RecentTransactionResponse> getRecentTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate s = (startDate == null || startDate.isBlank()) ? null : LocalDate.parse(startDate);
        LocalDate e = (endDate == null || endDate.isBlank()) ? null : LocalDate.parse(endDate);
        return revenueService.getRecentTransactionsPaged(page, size, s, e);
    }




    // ======= MOVIE ANALYTICS (mới) =======

    @GetMapping("/movies/summary")
    public ResponseEntity<MovieStatsSummaryResponse> getMovieSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        var now = LocalDate.now();
        int y = (year != null) ? year : now.getYear();
        int m = (month != null) ? month : now.getMonthValue();
        return ResponseEntity.ok(revenueService.getMovieSummary(y, m));
    }

    @GetMapping("/movies/new/day")
    public CountChartResponse getNewMoviesByDay(
            @RequestParam int year,
            @RequestParam int month) {
        return revenueService.getNewMoviesByDay(year, month);
    }

    @GetMapping("/movies/new/month")
    public CountChartResponse getNewMoviesByMonth(@RequestParam int year) {
        return revenueService.getNewMoviesByMonth(year);
    }

    @GetMapping("/movies/genre")
    public ResponseEntity<?> getCountByGenre(@RequestParam(defaultValue = "10") int top) {
        return ResponseEntity.ok(revenueService.getCountByGenre(top));
    }

    @GetMapping("/movies/country")
    public ResponseEntity<?> getCountByCountry(@RequestParam(defaultValue = "10") int top) {
        return ResponseEntity.ok(revenueService.getCountByCountry(top));
    }

    @GetMapping("/movies/status")
    public ResponseEntity<?> getStatusBreakdown() {
        return ResponseEntity.ok(revenueService.getStatusBreakdown());
    }

    @GetMapping("/movies/type")
    public ResponseEntity<?> getTypeBreakdown() {
        return ResponseEntity.ok(revenueService.getTypeBreakdown());
    }

    @GetMapping("/movies/release-year")
    public CountChartResponse getReleaseYearDistribution(
            @RequestParam int from,
            @RequestParam int to) {
        return revenueService.getReleaseYearDistribution(from, to);
    }

    @GetMapping("/movies/{movieId}/episodes-per-season")
    public CountChartResponse getEpisodesPerSeason(@PathVariable String movieId) {
        return revenueService.getEpisodesPerSeason(movieId);
    }

    @GetMapping("/movies/top/views")
    public ResponseEntity<?> getTopByViews(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(revenueService.getTopMoviesByViews(limit));
    }

    @GetMapping("/movies/top/rating")
    public ResponseEntity<?> getTopByRating(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "1") int minRatings) {
        return ResponseEntity.ok(revenueService.getTopMoviesByRating(limit, minRatings));
    }


    // ===== Movies (date range + groupBy) =====
    @GetMapping("/movies/new/range")
    public CountChartResponse getNewMoviesByRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "DAY") GroupByDataAnalzerResponse groupBy
    ) {
        return revenueService.getNewMoviesByRange(LocalDate.parse(startDate), LocalDate.parse(endDate), groupBy);
    }

    @GetMapping("/movies/range/summary")
    public MovieStatsSummaryResponse getMovieSummaryByRange(
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        return revenueService.getMovieSummaryByRange(LocalDate.parse(startDate), LocalDate.parse(endDate));
    }

    // ====== EXPORT EXCEL ======
    // Xuất theo year/month (giống file mẫu)
    @GetMapping("/export/dashboard.xlsx")
    public void exportDashboardYM(HttpServletResponse response,
                                  @RequestParam(required = false) Integer year,
                                  @RequestParam(required = false) Integer month,
                                  @RequestParam(defaultValue = "false") boolean includePromotions,
                                  @RequestParam(defaultValue = "10") int topVoucherLimit) throws IOException {
        var now = LocalDate.now();
        int y = (year != null) ? year : now.getYear();
        int m = (month != null) ? month : now.getMonthValue();

        var start = java.time.YearMonth.of(y, m).atDay(1);
        var end   = java.time.YearMonth.of(y, m).atEndOfMonth();

        exportDashboardService.exportDashboardRange(
                response, start, end, GroupByDataAnalzerResponse.DAY,
                null, null,
                includePromotions, topVoucherLimit
        );
    }


    // =========================
    // ===== PROMOTIONS ========
    // =========================

    // 1) Tổng quan khuyến mãi trong khoảng ngày
    @GetMapping("/promotions/summary")
    public PromoStatsSummaryResponse getPromotionSummary(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return revenueService.getPromotionSummary(LocalDate.parse(startDate), LocalDate.parse(endDate));
    }

    // 2) BXH voucher (top N)
    @GetMapping("/promotions/vouchers/leaderboard")
    public List<VoucherUsageItemResponse> getVoucherLeaderboard(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "10") int limit) {
        return revenueService.getVoucherLeaderboard(LocalDate.parse(startDate), LocalDate.parse(endDate), limit);
    }

    // 3) Stats theo promotion line (có thể lọc theo promotionId)
    @GetMapping("/promotions/lines")
    public List<PromotionLineStatsResponse> getPromotionLineStats(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String promotionId) {
        return revenueService.getPromotionLineStats(LocalDate.parse(startDate), LocalDate.parse(endDate), promotionId);
    }

    // 4) Biểu đồ usage theo khoảng thời gian
    @GetMapping("/promotions/usage")
    public PromotionRangeChartResponse getPromotionUsageByRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "DAY") GroupByDataAnalzerResponse groupBy) {
        return revenueService.getPromotionUsageByRange(LocalDate.parse(startDate), LocalDate.parse(endDate), groupBy);
    }

    // 5) Chi tiết voucher theo promotionId
    @GetMapping("/promotions/{promotionId}/vouchers")
    public List<VoucherUsageItemResponse> getVoucherDetailByPromotion(
            @PathVariable String promotionId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return revenueService.getVoucherDetailByPromotion(promotionId, LocalDate.parse(startDate), LocalDate.parse(endDate));
    }

    // Xuất theo khoảng ngày + groupBy (DAY|WEEK|MONTH)
    @GetMapping("/export/dashboard-range.xlsx")
    public void exportDashboardRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "DAY") GroupByDataAnalzerResponse groupBy,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String companyAddress,
            @RequestParam(defaultValue = "false") boolean includePromotions,
            @RequestParam(defaultValue = "10") int topVoucherLimit,
            HttpServletResponse response) throws Exception {

        LocalDate s = LocalDate.parse(startDate); // ISO yyyy-MM-dd
        LocalDate e = LocalDate.parse(endDate);

        exportDashboardService.exportDashboardRange(
                response, s, e, groupBy,
                companyName, companyAddress,
                includePromotions, topVoucherLimit
        );
    }




}



