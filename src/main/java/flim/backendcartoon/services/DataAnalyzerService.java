/*
 * @(#) $(NAME).java    1.0     9/15/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;



import flim.backendcartoon.entities.DTO.response.*;

import java.time.LocalDate;
import java.util.List;

import flim.backendcartoon.entities.DTO.response.QuickStatsResponse;
import flim.backendcartoon.entities.DTO.response.RecentTransactionResponse;
import flim.backendcartoon.entities.DTO.response.RevenueChartResponse;
import flim.backendcartoon.entities.DTO.response.RevenueSummaryResponse;


import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 15-September-2025 4:35 PM
 */
public interface DataAnalyzerService {
    RevenueChartResponse getRevenueByDay(int year, int month);
    RevenueChartResponse getRevenueByMonth(int year);
    RevenueChartResponse getRevenueByYear(int from, int to);
    RevenueSummaryResponse getSummary(int year, int month);
    QuickStatsResponse getQuickStats();
    List<RecentTransactionResponse> getRecentTransactions(int limit);

    // ======= THỐNG KÊ PHIM =======
    MovieStatsSummaryResponse getMovieSummary(int year, int month);
    CountChartResponse getNewMoviesByDay(int year, int month);
    CountChartResponse getNewMoviesByMonth(int year);
    List<CategoryCountItemResponse> getCountByGenre(int top);
    List<CategoryCountItemResponse> getCountByCountry(int top);
    List<CategoryCountItemResponse> getStatusBreakdown(); // COMPLETED/UPCOMING
    List<CategoryCountItemResponse> getTypeBreakdown();   // SINGLE/SERIES
    CountChartResponse getReleaseYearDistribution(int from, int to);
    CountChartResponse getEpisodesPerSeason(String movieId);
    List<TopMovieDTOResponse> getTopMoviesByViews(int limit);
    List<TopMovieDTOResponse> getTopMoviesByRating(int limit, int minRatings);
    // interface
    List<TopMovieDTOResponse> getTopMoviesByViewsInRange(LocalDate start, LocalDate end, int limit);
    List<TopMovieDTOResponse> getTopMoviesByRatingInRange(LocalDate start, LocalDate end, int limit, int minRatings);


    // ===== MỚI (range) =====
    RevenueChartResponse getRevenueByRange(LocalDate start, LocalDate end, GroupByDataAnalzerResponse groupBy);
    RevenueSummaryResponse getRevenueSummaryByRange(LocalDate start, LocalDate end);
    PagedResponse<RecentTransactionResponse> getRecentTransactionsPaged(int page, int size, LocalDate start, LocalDate end);

    CountChartResponse getNewMoviesByRange(LocalDate start, LocalDate end, GroupByDataAnalzerResponse groupBy);
    MovieStatsSummaryResponse getMovieSummaryByRange(LocalDate start, LocalDate end);

    // ======= THỐNG KÊ KHUYẾN MÃI =======
    PromoStatsSummaryResponse getPromotionSummary(LocalDate start, LocalDate end);

    List<VoucherUsageItemResponse> getVoucherLeaderboard(LocalDate start, LocalDate end, int limit);

    List<PromotionLineStatsResponse> getPromotionLineStats(LocalDate start, LocalDate end, String promotionId);

    PromotionRangeChartResponse getPromotionUsageByRange(LocalDate start, LocalDate end, GroupByDataAnalzerResponse groupBy);

    List<VoucherUsageItemResponse> getVoucherDetailByPromotion(String promotionId, LocalDate start, LocalDate end);

    //thống kê doanh thu khách hàng
    CustomerSalesReportResponse getCustomerSalesByRange(LocalDate start, LocalDate end);



}

    