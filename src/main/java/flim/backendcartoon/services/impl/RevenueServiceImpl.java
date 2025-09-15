/*
 * @(#) $(NAME).java    1.0     9/15/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 15-September-2025 4:36 PM
 */

import flim.backendcartoon.entities.DTO.response.RevenueChartResponse;
import flim.backendcartoon.entities.PaymentOrder;
import flim.backendcartoon.repositories.PaymentOrderRepository;
import flim.backendcartoon.services.RevenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RevenueServiceImpl implements RevenueService {

    private final PaymentOrderRepository paymentOrderRepository;

    @Autowired
    public RevenueServiceImpl(PaymentOrderRepository paymentOrderRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
    }

    @Override
    public Double calculateTotalRevenue() {
        return paymentOrderRepository.findAllPaid().stream()
                .mapToDouble(PaymentOrder::getFinalAmount)
                .sum();
    }

    @Override
    public RevenueChartResponse getRevenueByDay(int year, int month) {
        List<PaymentOrder> orders = paymentOrderRepository.findAllPaid().stream()
                .filter(o -> o.getCreatedAt().getYear() == year && o.getCreatedAt().getMonthValue() == month)
                .toList();

        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        Map<Integer, Double> grouped = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getCreatedAt().getDayOfMonth(),
                        Collectors.summingDouble(PaymentOrder::getFinalAmount)
                ));

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        for (int d = 1; d <= daysInMonth; d++) {
            labels.add(String.format("%02d/%02d", d, month));
            data.add(grouped.getOrDefault(d, 0.0));
        }

        return new RevenueChartResponse(labels, data);
    }

    @Override
    public RevenueChartResponse getRevenueByMonth(int year) {
        List<PaymentOrder> orders = paymentOrderRepository.findAllPaid().stream()
                .filter(o -> o.getCreatedAt().getYear() == year)
                .toList();

        Map<Integer, Double> grouped = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getCreatedAt().getMonthValue(),
                        Collectors.summingDouble(PaymentOrder::getFinalAmount)
                ));

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            labels.add("Th" + m);
            data.add(grouped.getOrDefault(m, 0.0));
        }

        return new RevenueChartResponse(labels, data);
    }

    @Override
    public RevenueChartResponse getRevenueByYear(int from, int to) {
        List<PaymentOrder> orders = paymentOrderRepository.findAllPaid();

        Map<Integer, Double> grouped = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getCreatedAt().getYear(),
                        Collectors.summingDouble(PaymentOrder::getFinalAmount)
                ));

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        for (int y = from; y <= to; y++) {
            labels.add(String.valueOf(y));
            data.add(grouped.getOrDefault(y, 0.0));
        }

        return new RevenueChartResponse(labels, data);
    }

}
