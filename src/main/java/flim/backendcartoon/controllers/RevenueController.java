/*
 * @(#) $(NAME).java    1.0     9/15/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.response.RevenueChartResponse;
import flim.backendcartoon.services.RevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 15-September-2025 4:39 PM
 */
@RestController
@RequestMapping("/revenue")
@RequiredArgsConstructor
public class RevenueController {

    @Autowired
    private final RevenueService revenueService;

    // Doanh thu theo ngày trong 1 tháng
    @GetMapping("/day")
    public RevenueChartResponse getRevenueByDay(
            @RequestParam int year,
            @RequestParam int month) {
        return revenueService.getRevenueByDay(year, month);
    }

    // Doanh thu theo 12 tháng trong 1 năm
    @GetMapping("/month")
    public RevenueChartResponse getRevenueByMonth(@RequestParam int year) {
        return revenueService.getRevenueByMonth(year);
    }

    // Doanh thu theo nhiều năm (from → to)
    @GetMapping("/year")
    public RevenueChartResponse getRevenueByYear(
            @RequestParam int from,
            @RequestParam int to) {
        return revenueService.getRevenueByYear(from, to);
    }
}
