/*
 * @(#) $(NAME).java    1.0     9/15/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.DTO.response.RevenueChartResponse;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 15-September-2025 4:35 PM
 */
public interface RevenueService {
    Double calculateTotalRevenue();
    RevenueChartResponse getRevenueByDay(int year, int month);
    RevenueChartResponse getRevenueByMonth(int year);
    RevenueChartResponse getRevenueByYear(int from, int to);
}

    