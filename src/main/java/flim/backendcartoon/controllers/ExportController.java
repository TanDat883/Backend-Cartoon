/*
 * @(#) $(NAME).java    1.0     9/20/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.response.GroupByDataAnalzerResponse;
import flim.backendcartoon.services.ExportDashboardService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 20-September-2025 2:49 PM
 */
@RestController
@RequestMapping("/export")
public class ExportController {

    private final ExportDashboardService exportDashboardService;

    public ExportController(ExportDashboardService exportDashboardService) {
        this.exportDashboardService = exportDashboardService;
    }

    @GetMapping("/dashboard")
    public void exportDashboard(HttpServletResponse response,
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


//    @GetMapping("/promotions-range.xlsx")
//    public void exportPromotions(HttpServletResponse response,
//                                 @RequestParam String startDate,
//                                 @RequestParam String endDate,
//                                 @RequestParam(defaultValue = "10") int topVoucherLimit) throws IOException {
//        var s = LocalDate.parse(startDate);
//        var e = LocalDate.parse(endDate);
//        exportDashboardService.exportPromotionReportRange(response, s, e, topVoucherLimit,
//                null, null);
//    }

}
