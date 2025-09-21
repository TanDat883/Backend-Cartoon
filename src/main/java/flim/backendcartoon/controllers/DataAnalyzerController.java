/*
 * @(#) $(NAME).java    1.0     9/15/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.response.CountChartResponse;
import flim.backendcartoon.entities.DTO.response.MovieStatsSummaryResponse;
import flim.backendcartoon.entities.DTO.response.RevenueChartResponse;
import flim.backendcartoon.entities.DTO.response.RevenueSummaryResponse;
import flim.backendcartoon.services.DataAnalyzerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    //giao dịch gần đây
    @GetMapping("/revenue/recent-transactions")
    public ResponseEntity<?> getRecentTransaction() {
        return ResponseEntity.ok(revenueService.getRecentTransactions(5));
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
            @RequestParam(defaultValue = "5") int minRatings) {
        return ResponseEntity.ok(revenueService.getTopMoviesByRating(limit, minRatings));
    }
}



