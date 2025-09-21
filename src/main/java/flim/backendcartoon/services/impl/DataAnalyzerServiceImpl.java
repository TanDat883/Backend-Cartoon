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

import flim.backendcartoon.entities.*;
import flim.backendcartoon.entities.DTO.response.*;
import flim.backendcartoon.repositories.*;

import flim.backendcartoon.entities.DTO.response.QuickStatsResponse;
import flim.backendcartoon.entities.DTO.response.RecentTransactionResponse;
import flim.backendcartoon.entities.DTO.response.RevenueChartResponse;
import flim.backendcartoon.entities.DTO.response.RevenueSummaryResponse;
import flim.backendcartoon.entities.Order;
import flim.backendcartoon.entities.PaymentOrder;
import flim.backendcartoon.entities.User;
import flim.backendcartoon.repositories.OrderRepository;
import flim.backendcartoon.repositories.PaymentOrderRepository;
import flim.backendcartoon.repositories.UserReponsitory;

import flim.backendcartoon.services.DataAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataAnalyzerServiceImpl implements DataAnalyzerService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final OrderRepository orderRepository;
    private final UserReponsitory userRepository;

    //thống kê cho phim...
    private final MovieRepository movieRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;

    @Autowired
    public DataAnalyzerServiceImpl(PaymentOrderRepository paymentOrderRepository
            , OrderRepository orderRepository, MovieRepository movieRepository, SeasonRepository seasonRepository, EpisodeRepository episodeRepository,
                                   UserReponsitory userRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.orderRepository = orderRepository;
        this.movieRepository = movieRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
        this.userRepository = userRepository;
    }


        @Override
        public RevenueChartResponse getRevenueByDay ( int year, int month){
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
        public RevenueChartResponse getRevenueByMonth ( int year){
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
        public RevenueChartResponse getRevenueByYear ( int from, int to){
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
        public RevenueSummaryResponse getSummary ( int year, int month){
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
        public QuickStatsResponse getQuickStats () {
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

        @Override
        public List<RecentTransactionResponse> getRecentTransactions ( int limit){
            return paymentOrderRepository.findTop5ByOrderByCreatedAtDesc().stream()
                    .map(po -> {
                        Order order = orderRepository.findByOrderId(po.getOrderId());
                        String userName = "Ẩn danh";
                        String packageId = "N/A";

                        if (order != null) {
                            packageId = order.getPackageId();

                            User user = userRepository.findById(order.getUserId());
                            if (user != null) {
                                userName = user.getUserName();
                            }
                        }

                        return new RecentTransactionResponse(
                                po.getOrderId(),
                                userName,
                                packageId,
                                po.getFinalAmount(),
                                po.getCreatedAt(),
                                po.getStatus()
                        );
                    })
                    .limit(limit)
                    .toList();
        }

    // ===========================
    // ===== THỐNG KÊ PHIM ======
    // ===========================

    @Override
    public MovieStatsSummaryResponse getMovieSummary(int year, int month) {
        List<Movie> movies = movieRepository.findAllMovies();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        long totalMovies = movies.size();
        long totalSingle = movies.stream().filter(m -> m.getMovieType() == MovieType.SINGLE).count();
        long totalSeries = movies.stream().filter(m -> m.getMovieType() == MovieType.SERIES).count();
        long completed = movies.stream().filter(m -> m.getStatus() == MovieStatus.COMPLETED).count();
        long upcoming  = movies.stream().filter(m -> m.getStatus() == MovieStatus.UPCOMING).count();

        // Tổng seasons & episodes (scan theo movie -> seasons -> count episodes)
        long totalSeasons = 0;
        long totalEpisodes = 0;
        for (Movie m : movies) {
            List<Season> ss = seasonRepository.findByMovieId(m.getMovieId());
            totalSeasons += ss.size();
            for (Season s : ss) {
                totalEpisodes += episodeRepository.countBySeasonId(s.getSeasonId());
            }
        }

        // New movies: today/week/month (dựa trên createdAt)
        LocalDate firstDayOfWeek = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        long addedToday = movies.stream().filter(m -> isSameDate(m.getCreatedAt(), today, zone)).count();
        long addedThisWeek = movies.stream().filter(m -> isBetweenWeek(m.getCreatedAt(), firstDayOfWeek, today, zone)).count();
        long addedThisMonth = movies.stream().filter(m -> isSameYearMonth(m.getCreatedAt(), year, month, zone)).count();

        // Avg rating toàn hệ thống (có trọng số)
        long totalRatings = movies.stream().mapToLong(m -> nullToZero(m.getRatingCount())).sum();
        double weightedSum = movies.stream().mapToDouble(m -> nullToZero(m.getAvgRating()) * nullToZero(m.getRatingCount())).sum();
        double avgRatingAll = totalRatings == 0 ? 0.0 : round1(weightedSum / totalRatings);

        // Phổ biến nhất: genre & country
        String topGenre = topKey(movies.stream()
                .filter(m -> m.getGenres() != null)
                .flatMap(m -> m.getGenres().stream())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting())));
        String topCountry = topKey(movies.stream()
                .map(Movie::getCountry)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting())));

        // Top by views
        Optional<Movie> topViews = movies.stream()
                .max(Comparator.comparingLong(m -> nullToZero(m.getViewCount())));
        // Top by rating (lọc minRatings=5 để tránh nhiễu)
        int minRatings = 5;
        Optional<Movie> topRating = movies.stream()
                .filter(m -> nullToZero(m.getRatingCount()) >= minRatings)
                .max(Comparator.comparingDouble(m -> nullToZero(m.getAvgRating())));

        MovieStatsSummaryResponse out = new MovieStatsSummaryResponse();
        out.setTotalMovies(totalMovies);
        out.setTotalSingle(totalSingle);
        out.setTotalSeries(totalSeries);
        out.setCompletedCount(completed);
        out.setUpcomingCount(upcoming);
        out.setTotalSeasons(totalSeasons);
        out.setTotalEpisodes(totalEpisodes);
        out.setAddedToday(addedToday);
        out.setAddedThisWeek(addedThisWeek);
        out.setAddedThisMonth(addedThisMonth);
        out.setAvgRatingAll(avgRatingAll);
        out.setTotalRatings(totalRatings);
        out.setTopGenre(topGenre == null ? "N/A" : topGenre);
        out.setTopCountry(topCountry == null ? "N/A" : topCountry);
        topViews.ifPresent(m -> out.setTopByViews(mapToTopMovieDTO(m)));
        topRating.ifPresent(m -> out.setTopByRating(mapToTopMovieDTO(m)));
        return out;
    }

    @Override
    public CountChartResponse getNewMoviesByDay(int year, int month) {
        List<Movie> movies = movieRepository.findAllMovies();
        ZoneId zone = ZoneId.systemDefault();
        int days = YearMonth.of(year, month).lengthOfMonth();
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (int d = 1; d <= days; d++) {
            labels.add(String.format("%02d/%02d", d, month));
            final int dd = d;
            long c = movies.stream().filter(m -> {
                if (m.getCreatedAt() == null) return false;
                LocalDate lm = LocalDateTime.ofInstant(m.getCreatedAt(), zone).toLocalDate();
                return lm.getYear() == year && lm.getMonthValue() == month && lm.getDayOfMonth() == dd;
            }).count();
            data.add(c);
        }
        return new CountChartResponse(labels, data);
    }

    @Override
    public CountChartResponse getNewMoviesByMonth(int year) {
        List<Movie> movies = movieRepository.findAllMovies();
        ZoneId zone = ZoneId.systemDefault();
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            labels.add("Th" + m);
            final int mm = m;
            long c = movies.stream().filter(movie -> isSameYearMonth(movie.getCreatedAt(), year, mm, zone)).count();
            data.add(c);
        }
        return new CountChartResponse(labels, data);
    }

    @Override
    public List<CategoryCountItemResponse> getCountByGenre(int top) {
        List<Movie> movies = movieRepository.findAllMovies();
        Map<String, Long> map = movies.stream()
                .filter(m -> m.getGenres() != null)
                .flatMap(m -> m.getGenres().stream())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(top <= 0 ? map.size() : top)
                .map(e -> new CategoryCountItemResponse(e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    public List<CategoryCountItemResponse> getCountByCountry(int top) {
        List<Movie> movies = movieRepository.findAllMovies();
        Map<String, Long> map = movies.stream()
                .map(Movie::getCountry)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(top <= 0 ? map.size() : top)
                .map(e -> new CategoryCountItemResponse(e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    public List<CategoryCountItemResponse> getStatusBreakdown() {
        List<Movie> movies = movieRepository.findAllMovies();
        long completed = movies.stream().filter(m -> m.getStatus() == MovieStatus.COMPLETED).count();
        long upcoming  = movies.stream().filter(m -> m.getStatus() == MovieStatus.UPCOMING).count();
        return List.of(
                new CategoryCountItemResponse("COMPLETED", completed),
                new CategoryCountItemResponse("UPCOMING", upcoming)
        );
    }

    @Override
    public List<CategoryCountItemResponse> getTypeBreakdown() {
        List<Movie> movies = movieRepository.findAllMovies();
        long single = movies.stream().filter(m -> m.getMovieType() == MovieType.SINGLE).count();
        long series = movies.stream().filter(m -> m.getMovieType() == MovieType.SERIES).count();
        return List.of(
                new CategoryCountItemResponse("SINGLE", single),
                new CategoryCountItemResponse("SERIES", series)
        );
    }

    @Override
    public CountChartResponse getReleaseYearDistribution(int from, int to) {
        List<Movie> movies = movieRepository.findAllMovies();
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (int y = from; y <= to; y++) {
            final int yy = y;
            labels.add(String.valueOf(yy));
            long c = movies.stream()
                    .filter(m -> m.getReleaseYear() != null && m.getReleaseYear() == yy)
                    .count();
            data.add(c);
        }
        return new CountChartResponse(labels, data);
    }

    @Override
    public CountChartResponse getEpisodesPerSeason(String movieId) {
        List<Season> seasons = seasonRepository.findByMovieId(movieId);
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (Season s : seasons) {
            labels.add("Season " + s.getSeasonNumber());
            data.add((long) episodeRepository.countBySeasonId(s.getSeasonId()));
        }
        return new CountChartResponse(labels, data);
    }

    @Override
    public List<TopMovieDTOResponse> getTopMoviesByViews(int limit) {
        return movieRepository.topNMoviesByViewCount(limit <= 0 ? 10 : limit)
                .stream().map(this::mapToTopMovieDTO).toList();
    }

    @Override
    public List<TopMovieDTOResponse> getTopMoviesByRating(int limit, int minRatings) {
        List<Movie> all = movieRepository.findAllMovies();
        return all.stream()
                .filter(m -> nullToZero(m.getRatingCount()) >= Math.max(minRatings, 1))
                .sorted(Comparator.comparingDouble((Movie m) -> nullToZero(m.getAvgRating())).reversed())
                .limit(limit <= 0 ? 10 : limit)
                .map(this::mapToTopMovieDTO)
                .toList();
    }

    // ===== helpers =====
    private boolean isSameDate(Instant i, LocalDate d, ZoneId zone) {
        if (i == null) return false;
        LocalDate ld = LocalDateTime.ofInstant(i, zone).toLocalDate();
        return ld.isEqual(d);
    }
    private boolean isBetweenWeek(Instant i, LocalDate from, LocalDate to, ZoneId zone) {
        if (i == null) return false;
        LocalDate ld = LocalDateTime.ofInstant(i, zone).toLocalDate();
        return !ld.isBefore(from) && !ld.isAfter(to);
    }
    private boolean isSameYearMonth(Instant i, int year, int month, ZoneId zone) {
        if (i == null) return false;
        LocalDate ld = LocalDateTime.ofInstant(i, zone).toLocalDate();
        return ld.getYear() == year && ld.getMonthValue() == month;
    }
    private long nullToZero(Long v) { return v == null ? 0L : v; }
    private double nullToZero(Double v) { return v == null ? 0.0 : v; }
    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    private String topKey(Map<String, Long> m) {
        if (m == null || m.isEmpty()) return null;
        return m.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }
    private TopMovieDTOResponse mapToTopMovieDTO(Movie m) {
        return new TopMovieDTOResponse(
                m.getMovieId(),
                m.getTitle(),
                m.getThumbnailUrl(),
                m.getViewCount(),
                m.getAvgRating(),
                m.getRatingCount(),
                m.getReleaseYear(),
                m.getCountry(),
                m.getGenres()
        );
    }
}


