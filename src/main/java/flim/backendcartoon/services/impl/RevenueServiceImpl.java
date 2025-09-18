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

import flim.backendcartoon.entities.DTO.response.QuickStatsResponse;
import flim.backendcartoon.entities.DTO.response.RevenueChartResponse;
import flim.backendcartoon.entities.DTO.response.RevenueSummaryResponse;
import flim.backendcartoon.entities.Order;
import flim.backendcartoon.entities.PaymentOrder;
import flim.backendcartoon.repositories.OrderRepository;
import flim.backendcartoon.repositories.PaymentOrderRepository;
import flim.backendcartoon.services.RevenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RevenueServiceImpl implements RevenueService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final OrderRepository orderRepository;

    @Autowired
    public RevenueServiceImpl(PaymentOrderRepository paymentOrderRepository, OrderRepository orderRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.orderRepository = orderRepository;
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

    @Override
    public RevenueSummaryResponse getSummary(int year, int month) {
        List<PaymentOrder> allPaid = paymentOrderRepository.findAllPaid();

        // Tổng doanh thu
        Double totalRevenue = allPaid.stream()
                .mapToDouble(PaymentOrder::getFinalAmount)
                .sum();

        // Doanh thu tháng hiện tại
        Double monthlyRevenue = allPaid.stream()
                .filter(o -> o.getCreatedAt().getYear() == year
                        && o.getCreatedAt().getMonthValue() == month)
                .mapToDouble(PaymentOrder::getFinalAmount)
                .sum();

        // Tổng số giao dịch
        Long totalTransactions = (long) allPaid.size();

        // Số giao dịch trong tháng hiện tại
        Long monthlyTransactions = allPaid.stream()
                .filter(o -> o.getCreatedAt().getYear() == year
                        && o.getCreatedAt().getMonthValue() == month)
                .count();

        return new RevenueSummaryResponse(totalRevenue, monthlyRevenue,
                totalTransactions, monthlyTransactions);
    }

    @Override
    public QuickStatsResponse getQuickStats() {
        List<PaymentOrder> allPaid = paymentOrderRepository.findAllPaid();
        LocalDate today = LocalDate.now();

        // Doanh thu hôm nay
        Double todayRevenue = allPaid.stream()
                .filter(o -> o.getCreatedAt().isEqual(today))
                .mapToDouble(PaymentOrder::getFinalAmount)
                .sum();

        // Doanh thu tuần (tính từ thứ 2 tới CN hiện tại)
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        Double weekRevenue = allPaid.stream()
                .filter(o -> {
                    LocalDate d = o.getCreatedAt();
                    return !d.isBefore(startOfWeek) && !d.isAfter(today);
                })
                .mapToDouble(PaymentOrder::getFinalAmount)
                .sum();

        // Tăng trưởng = (tuần này - tuần trước) / tuần trước * 100
        LocalDate startPrevWeek = startOfWeek.minusWeeks(1);
        LocalDate endPrevWeek = startOfWeek.minusDays(1);
        Double prevWeekRevenue = allPaid.stream()
                .filter(o -> {
                    LocalDate d = o.getCreatedAt();
                    return !d.isBefore(startPrevWeek) && !d.isAfter(endPrevWeek);
                })
                .mapToDouble(PaymentOrder::getFinalAmount)
                .sum();

        double growthPercent = (prevWeekRevenue == 0) ? 100
                : ((weekRevenue - prevWeekRevenue) / prevWeekRevenue) * 100;

        Map<String, Long> packageCount = allPaid.stream()
                .map(po -> orderRepository.findByOrderId(po.getOrderId()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Order::getPackageId, Collectors.counting()));

        String popularPackage = packageCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return new QuickStatsResponse(todayRevenue, weekRevenue, growthPercent, popularPackage);
    }

}
