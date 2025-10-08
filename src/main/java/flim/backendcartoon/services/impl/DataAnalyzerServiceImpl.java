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
import flim.backendcartoon.entities.Payment;
import flim.backendcartoon.entities.PaymentDetail;
import flim.backendcartoon.entities.User;
import flim.backendcartoon.repositories.PaymentRepository;
import flim.backendcartoon.repositories.PaymentDetailRepository;
import flim.backendcartoon.repositories.UserReponsitory;

import flim.backendcartoon.services.DataAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataAnalyzerServiceImpl implements DataAnalyzerService {

    private final PaymentDetailRepository paymentDetailRepository;
    private final PaymentRepository paymentRepository;
    private final UserReponsitory userRepository;
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh"); // đồng bộ với PaymentServiceImpl
    private LocalDate parseIsoToLocalDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return OffsetDateTime.parse(iso).atZoneSameInstant(VN).toLocalDate(); } catch (Exception ignore) {}
        try { return ZonedDateTime.parse(iso).withZoneSameInstant(VN).toLocalDate(); } catch (Exception ignore) {}
        try { return LocalDateTime.parse(iso).atZone(VN).toLocalDate(); } catch (Exception ignore) {}
        try {
            long ms = Long.parseLong(iso.trim());
            return Instant.ofEpochMilli(ms).atZone(VN).toLocalDate();
        } catch (Exception ignore) {}
        return null;
    }


    private LocalDate paymentLocalDate(Payment p) {
        LocalDate d = parseIsoToLocalDate(p.getPaidAt());
        if (d == null) d = parseIsoToLocalDate(p.getCreatedAt());
        return d;
    }
    private boolean inRange(LocalDate d, LocalDate s, LocalDate e) {
        if (d == null) return false;
        if (s != null && d.isBefore(s)) return false;
        if (e != null && d.isAfter(e)) return false;
        return true;
    }
    private double toDouble(Long v) { return v == null ? 0.0 : v.doubleValue(); }


    //thống kê cho phim...
    private final MovieRepository movieRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;

    private final PromotionRepository promotionRepository;
    private final PromotionLineRepository promotionLineRepository;
    private final PromotionDetailRepository promotionDetailRepository;

    @Autowired
    public DataAnalyzerServiceImpl(PaymentDetailRepository paymentDetailRepository,
                                   PaymentRepository paymentRepository,
                                   MovieRepository movieRepository,
                                   SeasonRepository seasonRepository,
                                   EpisodeRepository episodeRepository,
                                   UserReponsitory userRepository,
                                   PromotionRepository promotionRepository,
                                   PromotionLineRepository promotionLineRepository,
                                   PromotionDetailRepository promotionDetailRepository) {
        this.paymentDetailRepository = paymentDetailRepository;
        this.paymentRepository = paymentRepository;
        this.movieRepository = movieRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
        this.userRepository = userRepository;
        this.promotionRepository = promotionRepository;
        this.promotionLineRepository = promotionLineRepository;
        this.promotionDetailRepository = promotionDetailRepository;
    }


    // === Doanh thu theo ngày trong 1 tháng ===
    @Override
    public RevenueChartResponse getRevenueByDay(int year, int month) {
        List<Payment> paid = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .toList();

        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        List<String> labels = new ArrayList<>(daysInMonth);
        List<Double> data = new ArrayList<>(daysInMonth);

        double[] bucket = new double[daysInMonth + 1]; // 1..days

        for (Payment p : paid) {
            LocalDate d = paymentLocalDate(p);
            if (d == null || d.getYear() != year || d.getMonthValue() != month) continue;
            int day = d.getDayOfMonth();
            bucket[day] += toDouble(p.getFinalAmount());
        }

        for (int d = 1; d <= daysInMonth; d++) {
            labels.add(String.format("%02d/%02d", d, month));
            data.add(bucket[d]);
        }
        return new RevenueChartResponse(labels, data);
    }

    // === Doanh thu theo 12 tháng của 1 năm ===
    @Override
    public RevenueChartResponse getRevenueByMonth(int year) {
        List<Payment> paid = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .toList();

        double[] bucket = new double[12]; // 0..11
        for (Payment p : paid) {
            LocalDate d = paymentLocalDate(p);
            if (d == null || d.getYear() != year) continue;
            bucket[d.getMonthValue() - 1] += toDouble(p.getFinalAmount());
        }

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            labels.add("Th" + m);
            data.add(bucket[m - 1]);
        }
        return new RevenueChartResponse(labels, data);
    }

    // === Doanh thu theo nhiều năm (from..to) ===
    @Override
    public RevenueChartResponse getRevenueByYear(int from, int to) {
        if (to < from) { int t = from; from = to; to = t; }

        List<Payment> paid = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .toList();

        Map<Integer, Double> bucket = new LinkedHashMap<>();
        for (int y = from; y <= to; y++) bucket.put(y, 0.0);

        for (Payment p : paid) {
            LocalDate d = paymentLocalDate(p);
            if (d == null || d.getYear() < from || d.getYear() > to) continue;
            bucket.compute(d.getYear(), (k, v) -> v + toDouble(p.getFinalAmount()));
        }

        List<String> labels = bucket.keySet().stream().map(String::valueOf).toList();
        List<Double> data = labels.stream().map(y -> bucket.get(Integer.parseInt(y))).toList();
        return new RevenueChartResponse(labels, data);
    }

    // === Summary theo tháng (tổng/ tháng hiện tại / tổng GD / GD tháng) ===
    @Override
    public RevenueSummaryResponse getSummary(int year, int month) {
        List<Payment> paid = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .toList();

        double totalRevenue = paid.stream().mapToDouble(p -> toDouble(p.getFinalAmount())).sum();

        double monthlyRevenue = paid.stream()
                .filter(p -> {
                    LocalDate d = paymentLocalDate(p);
                    return d != null && d.getYear() == year && d.getMonthValue() == month;
                })
                .mapToDouble(p -> toDouble(p.getFinalAmount())).sum();

        long totalTransactions = paid.size();

        long monthlyTransactions = paid.stream()
                .filter(p -> {
                    LocalDate d = paymentLocalDate(p);
                    return d != null && d.getYear() == year && d.getMonthValue() == month;
                }).count();

        return new RevenueSummaryResponse(totalRevenue, monthlyRevenue, totalTransactions, monthlyTransactions);
    }

    // === Quick stats (hôm nay, tuần này, tăng trưởng so tuần trước, gói phổ biến) ===
    @Override
    public QuickStatsResponse getQuickStats() {
        List<Payment> paid = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .toList();

        LocalDate today = LocalDate.now(VN);
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);

        double todayRevenue = paid.stream()
                .filter(p -> today.equals(paymentLocalDate(p)))
                .mapToDouble(p -> toDouble(p.getFinalAmount())).sum();

        double weekRevenue = paid.stream()
                .filter(p -> {
                    LocalDate d = paymentLocalDate(p);
                    return d != null && !d.isBefore(startOfWeek) && !d.isAfter(today);
                }).mapToDouble(p -> toDouble(p.getFinalAmount())).sum();

        LocalDate startPrevWeek = startOfWeek.minusWeeks(1);
        LocalDate endPrevWeek = startOfWeek.minusDays(1);
        double prevWeekRevenue = paid.stream()
                .filter(p -> {
                    LocalDate d = paymentLocalDate(p);
                    return d != null && !d.isBefore(startPrevWeek) && !d.isAfter(endPrevWeek);
                }).mapToDouble(p -> toDouble(p.getFinalAmount())).sum();

        double growthPercent = (prevWeekRevenue == 0) ? 100.0 : ((weekRevenue - prevWeekRevenue) / prevWeekRevenue) * 100.0;

        // Gói phổ biến theo số giao dịch
        Map<String, Long> pkgCount = paid.stream()
                .collect(Collectors.groupingBy(Payment::getPackageId, Collectors.counting()));
        String popularPackage = pkgCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");

        return new QuickStatsResponse(todayRevenue, weekRevenue, growthPercent, popularPackage);
    }

    // === Giao dịch gần đây (đơn giản) ===
    @Override
    public List<RecentTransactionResponse> getRecentTransactions(int limit) {
        List<Payment> paidSorted = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .sorted(Comparator.comparing(this::paymentLocalDate).reversed())
                .limit(Math.max(1, limit))
                .toList();

        return paidSorted.stream().map(p -> {
            String userName = "Ẩn danh";
            var user = userRepository.findById(p.getUserId());
            if (user != null && user.getUserName() != null) userName = user.getUserName();
            LocalDate created = paymentLocalDate(p);
            // nếu muốn kèm finalAmount dạng Double
            return new RecentTransactionResponse(
                    String.valueOf(p.getPaymentCode()),
                    userName,
                    p.getPackageId(),
                    toDouble(p.getFinalAmount()),
                    created,
                    p.getStatus()
            );
        }).toList();
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

    // ====== Revenue (range) ======
    @Override
    public RevenueChartResponse getRevenueByRange(LocalDate start, LocalDate end, GroupByDataAnalzerResponse groupBy) {
        if (end.isBefore(start)) { var t = start; start = end; end = t; }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > 366 && groupBy == GroupByDataAnalzerResponse.DAY) groupBy = GroupByDataAnalzerResponse.WEEK;

        Map<String, Double> bucket = new LinkedHashMap<>();
        DateTimeFormatter dFmt = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter wFmt = DateTimeFormatter.ofPattern("dd/MM");
        switch (groupBy) {
            case DAY -> {
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) bucket.put(d.format(dFmt), 0.0);
            }
            case WEEK -> {
                LocalDate ptr = start.with(DayOfWeek.MONDAY);
                while (!ptr.isAfter(end)) {
                    LocalDate mon = ptr, sun = mon.plusDays(6);
                    bucket.put(mon.format(wFmt) + "–" + sun.format(wFmt), 0.0);
                    ptr = ptr.plusWeeks(1);
                }
            }
            case MONTH -> {
                YearMonth ym = YearMonth.from(start), yEnd = YearMonth.from(end);
                while (!ym.isAfter(yEnd)) { bucket.put(ym.toString(), 0.0); ym = ym.plusMonths(1); }
            }
        }

        List<Payment> paid = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .toList();

        for (Payment p : paid) {
            LocalDate d = paymentLocalDate(p);
            if (!inRange(d, start, end)) continue;
            String key = switch (groupBy) {
                case DAY   -> d.format(dFmt);
                case WEEK  -> d.with(DayOfWeek.MONDAY).format(wFmt) + "–" + d.with(DayOfWeek.MONDAY).plusDays(6).format(wFmt);
                case MONTH -> YearMonth.from(d).toString();
            };
            bucket.computeIfPresent(key, (k, v) -> v + toDouble(p.getFinalAmount()));
        }

        var labels = new ArrayList<>(bucket.keySet());
        var values = labels.stream().map(bucket::get).toList();
        return new RevenueChartResponse(labels, values);
    }

    @Override
    public RevenueSummaryResponse getRevenueSummaryByRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) { var t = start; start = end; end = t; }
        final LocalDate S = start;
        final LocalDate E = end;

        long days = ChronoUnit.DAYS.between(S, E) + 1;
        List<Payment> paid = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .toList();

        double totalAll = paid.stream().mapToDouble(p -> toDouble(p.getFinalAmount())).sum();
        long txAll = paid.size();

        double totalRange = paid.stream()
                .filter(p -> inRange(paymentLocalDate(p), S, E))
                .mapToDouble(p -> toDouble(p.getFinalAmount())).sum();
        long txRange = paid.stream().filter(p -> inRange(paymentLocalDate(p), S, E)).count();

        return new RevenueSummaryResponse(totalAll, totalRange, txAll, txRange);
    }

    @Override
    public PagedResponse<RecentTransactionResponse> getRecentTransactionsPaged(int page, int size, LocalDate start, LocalDate end) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;

        var filtered = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .filter(p -> {
                    if (start == null || end == null) return true;
                    LocalDate d = paymentLocalDate(p);
                    return inRange(d, start, end);
                })
                .sorted(Comparator.comparing(this::paymentLocalDate).reversed())
                .toList();

        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(filtered.size(), from + size);
        var slice = from >= filtered.size() ? List.<Payment>of() : filtered.subList(from, to);

        var items = slice.stream().map(p -> {
            String userName = "Ẩn danh";
            var user = userRepository.findById(p.getUserId());
            if (user != null && user.getUserName() != null) userName = user.getUserName();
            return new RecentTransactionResponse(
                    String.valueOf(p.getPaymentCode()),
                    userName,
                    p.getPackageId(),
                    toDouble(p.getFinalAmount()),
                    paymentLocalDate(p),
                    p.getStatus()
            );
        }).toList();

        return new PagedResponse<>(page, size, filtered.size(), items);
    }


    // ====== Movies (range) ======
    @Override
    public CountChartResponse getNewMoviesByRange(LocalDate start, LocalDate end, GroupByDataAnalzerResponse groupBy) {
        if (end.isBefore(start)) { var t = start; start = end; end = t; }

        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > 366 && groupBy == GroupByDataAnalzerResponse.DAY) groupBy = GroupByDataAnalzerResponse.WEEK;

        var movies = movieRepository.findAllMovies();
        Map<String, Long> bucket = new LinkedHashMap<>();

        switch (groupBy) {
            case WEEK -> {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
                LocalDate ptr = start.with(DayOfWeek.MONDAY);
                while (!ptr.isAfter(end)) {
                    LocalDate mon = ptr;
                    LocalDate sun = mon.plusDays(6);
                    String key = mon.format(fmt) + "–" + sun.format(fmt);
                    bucket.put(key, 0L);
                    ptr = ptr.plusWeeks(1);
                }
                for (var m : movies) {
                    if (m.getCreatedAt() == null) continue;
                    LocalDate d = LocalDateTime.ofInstant(m.getCreatedAt(), VN).toLocalDate();
                    if (d.isBefore(start) || d.isAfter(end)) continue;
                    LocalDate mon = d.with(DayOfWeek.MONDAY);
                    LocalDate sun = mon.plusDays(6);
                    String k = mon.format(fmt) + "–" + sun.format(fmt);   // <-- KHỚP format
                    bucket.computeIfPresent(k, (kk, v) -> v + 1);
                }
            }
            case MONTH -> {
                YearMonth ym = YearMonth.from(start);
                YearMonth ymEnd = YearMonth.from(end);
                while (!ym.isAfter(ymEnd)) {
                    bucket.put(ym.toString(), 0L);
                    ym = ym.plusMonths(1);
                }
                for (var m : movies) {
                    if (m.getCreatedAt() == null) continue;
                    LocalDate d = LocalDateTime.ofInstant(m.getCreatedAt(), VN).toLocalDate();
                    if (d.isBefore(start) || d.isAfter(end)) continue;
                    YearMonth ymo = YearMonth.from(d);
                    bucket.computeIfPresent(ymo.toString(), (kk, v) -> v + 1);
                }
            }
            case DAY -> {
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) bucket.put(d.toString(), 0L);
                for (var m : movies) {
                    if (m.getCreatedAt() == null) continue;
                    LocalDate d = LocalDateTime.ofInstant(m.getCreatedAt(), VN).toLocalDate();
                    if (d.isBefore(start) || d.isAfter(end)) continue;
                    bucket.computeIfPresent(d.toString(), (kk, v) -> v + 1);
                }
            }
        }

        var labels = new ArrayList<>(bucket.keySet());
        var counts = labels.stream().map(bucket::get).toList();
        return new CountChartResponse(labels, counts);
    }

    @Override
    public MovieStatsSummaryResponse getMovieSummaryByRange(LocalDate start, LocalDate end) {
        var base = getMovieSummary(LocalDate.now().getYear(), LocalDate.now().getMonthValue());

        var all = movieRepository.findAllMovies();
        long addedRange = all.stream().filter(m -> {
            if (m.getCreatedAt() == null) return false;
            LocalDate d = LocalDateTime.ofInstant(m.getCreatedAt(), VN).toLocalDate();
            return !d.isBefore(start) && !d.isAfter(end);
        }).count();

        // reuse field: addedThisMonth = addedInRange
        base.setAddedThisMonth(addedRange);
        return base;
    }
    // ======= Helpers (tiền kiểu Long) =======
    private long L(Long v) { return v == null ? 0L : v; }

    // Lấy tên & type line nếu cần (nếu repo chưa có hàm, cứ để null an toàn)
    private String getPromotionLineName(String lineId) {
        if (lineId == null) return null;
        try {
            var line = promotionLineRepository.findByPromotionLineId(lineId);
            return line != null ? line.getPromotionLineName() : null;
        } catch (Throwable ignore) { return null; }
    }
    private String getPromotionLineType(String lineId) {
        if (lineId == null) return null;
        try {
            var line = promotionLineRepository.findByPromotionLineId(lineId);
            return (line != null && line.getPromotionLineType() != null) ? line.getPromotionLineType().name() : null;
        } catch (Throwable ignore) { return null; }
    }

    // =======================
// == PROMO: SUMMARY =====
// =======================
    @Override
    public PromoStatsSummaryResponse getPromotionSummary(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) { var t = start; start = end; end = t; }
        final LocalDate S = start, E = end;

        // Lọc paid trong range
        var paid = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .filter(p -> inRange(paymentLocalDate(p), S, E))
                .toList();

        // Chỉ xét các giao dịch có voucher (đúng nghĩa "redemption" của DTO này)
        List<Payment> withAnyPromotion = paid.stream()
                           .filter(p -> {
                               var pd = getPD(p);
                          if (pd == null) return false;
                          boolean hasVoucher = pd.getVoucherCode() != null && !pd.getVoucherCode().isBlank();
                        boolean hasPackagePromo = pd.getPromotionId() != null; // áp dụng khuyến mãi theo gói
                        return hasVoucher || hasPackagePromo;
                      })
                          .toList();

        long totalRedemptions = withAnyPromotion.size();
        long uniqueUsers = withAnyPromotion.stream().map(Payment::getUserId).filter(Objects::nonNull).distinct().count();
        long totalDiscountGranted = withAnyPromotion.stream()
                .map(payment -> paymentDetailRepository.findByPaymentId(payment.getPaymentId()))
                .filter(Objects::nonNull)
                .mapToLong(pd -> L(pd.getDiscountAmount())).sum();

        long totalOriginalAmount = withAnyPromotion.stream()
        .map(payment -> paymentDetailRepository.findByPaymentId(payment.getPaymentId()))
                .filter(Objects::nonNull)
                .mapToLong(pd -> L(pd.getOriginalAmount())).sum();

        long totalFinalAmount = withAnyPromotion.stream()
            .map(payment -> paymentDetailRepository.findByPaymentId(payment.getPaymentId()))
                .filter(Objects::nonNull)
                .mapToLong(pd -> L(pd.getFinalAmount())).sum();

        // first/last
        var dates = withAnyPromotion.stream().map(this::paymentLocalDate)
                .filter(Objects::nonNull).sorted().toList();
        LocalDate first = dates.isEmpty() ? null : dates.get(0);
        LocalDate last  = dates.isEmpty() ? null : dates.get(dates.size() - 1);

        // top voucher
        Map<String, List<Payment>> byVoucher = withAnyPromotion.stream()
        .collect(Collectors.groupingBy(p -> {
            var pd = getPD(p);
            return pd != null ? pd.getVoucherCode() : null;
        }));

        VoucherUsageItemResponse topVoucher = byVoucher.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .map(e -> {
                    String voucher = e.getKey();
                    List<Payment> list = e.getValue();
                    long uses = list.size();
                    long uu = list.stream().map(Payment::getUserId).filter(Objects::nonNull).distinct().count();

                    long sumDisc = 0, sumOri = 0, sumFin = 0;
                    LocalDate f = null, l = null;
                    for (var p : list) {
                        var pd = getPD(p);
                        if (pd == null) continue;
                        sumDisc += L(pd.getDiscountAmount());
                        sumOri  += L(pd.getOriginalAmount());
                        sumFin  += L(pd.getFinalAmount());
                        LocalDate d = paymentLocalDate(p);
                        if (d != null) {
                            if (f == null || d.isBefore(f)) f = d;
                            if (l == null || d.isAfter(l))  l = d;
                        }
                    }
                    // enrich từ PromotionDetail nếu có
                    PromotionDetail det = promotionDetailRepository.findByVoucherCode(voucher);
                    Integer maxUsage = det != null ? det.getMaxUsage() : null;
                    Integer usedCount = det != null ? det.getUsedCount() : null;

                    return VoucherUsageItemResponse.builder()
                            .promotionId(det != null ? det.getPromotionId() : null)
                            .promotionLineId(det != null ? det.getPromotionLineId() : null)
                            .voucherCode(voucher)
                            .uses(uses)
                            .uniqueUsers(uu)
                            .totalDiscount(sumDisc)
                            .totalOriginal(sumOri)
                            .totalFinal(sumFin)
                            .maxUsage(maxUsage)
                            .usedCount(usedCount)
                            .firstUse(f)
                            .lastUse(l)
                            .build();
                })
                .sorted(Comparator.comparingLong(VoucherUsageItemResponse::getUses).reversed())
                .findFirst()
                .orElse(null);

        return new PromoStatsSummaryResponse(
                totalRedemptions,
                uniqueUsers,
                totalDiscountGranted,
                totalOriginalAmount,
                totalFinalAmount,
                first,
                last,
                topVoucher
        );
    }

    // ==============================
// == PROMO: VOUCHER LEADERBOARD
// ==============================
    @Override
    public List<VoucherUsageItemResponse> getVoucherLeaderboard(LocalDate start, LocalDate end, int limit) {
        if (start != null && end != null && end.isBefore(start)) { var t = start; start = end; end = t; }
        final LocalDate S = start, E = end;

        var paid = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .filter(p -> inRange(paymentLocalDate(p), S, E))
                .toList();

        Map<String, List<Payment>> byVoucher = paid.stream()
                .map(p -> Map.entry(p, getPD(p)))
                .filter(e -> e.getValue()!=null
                        && e.getValue().getVoucherCode()!=null
                        && !e.getValue().getVoucherCode().isBlank())
                .collect(Collectors.groupingBy(e -> e.getValue().getVoucherCode(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        return byVoucher.entrySet().stream().map(e -> {
                    String voucher = e.getKey();
                    List<Payment> list = e.getValue();

                    long uses = list.size();
                    long uu = list.stream().map(Payment::getUserId).filter(Objects::nonNull).distinct().count();

                    long sumDisc = 0, sumOri = 0, sumFin = 0;
                    LocalDate f = null, l = null;
                    for (var p : list) {
                        var pd = getPD(p);
                        if (pd == null) continue;
                        sumDisc += L(pd.getDiscountAmount());
                        sumOri  += L(pd.getOriginalAmount());
                        sumFin  += L(pd.getFinalAmount());
                        LocalDate d = paymentLocalDate(p);
                        if (d != null) {
                            if (f == null || d.isBefore(f)) f = d;
                            if (l == null || d.isAfter(l))  l = d;
                        }
                    }
                    PromotionDetail det = promotionDetailRepository.findByVoucherCode(voucher);

                    return VoucherUsageItemResponse.builder()
                            .promotionId(det != null ? det.getPromotionId() : null)
                            .promotionLineId(det != null ? det.getPromotionLineId() : null)
                            .voucherCode(voucher)
                            .uses(uses)
                            .uniqueUsers(uu)
                            .totalDiscount(sumDisc)
                            .totalOriginal(sumOri)
                            .totalFinal(sumFin)
                            .maxUsage(det != null ? det.getMaxUsage() : null)
                            .usedCount(det != null ? det.getUsedCount() : null)
                            .firstUse(f)
                            .lastUse(l)
                            .build();
                }).sorted(Comparator.comparingLong(VoucherUsageItemResponse::getUses).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    // ====================================
// == PROMO: STATS THEO PROMOTION LINE
// ====================================
    @Override
    public List<PromotionLineStatsResponse> getPromotionLineStats(LocalDate start, LocalDate end, String promotionId) {
        if (start != null && end != null && end.isBefore(start)) { var t = start; start = end; end = t; }
        final LocalDate S = start, E = end;

        var paid = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .filter(p -> inRange(paymentLocalDate(p), S, E))
                .toList();

        Map<String, long[]> sums = new HashMap<>(); // lineId -> [redemptions, totalDiscount, totalOriginal, totalFinal]
        Map<String, String> lineToPromo = new HashMap<>();
        Map<String, String> lineTypeMap = new HashMap<>();

        for (var p : paid) {
            var pd = getPD(p);
            if (pd == null) continue;
            // nếu truyền promotionId thì lọc
            if (promotionId != null && !promotionId.isBlank()) {
                String pid = pd.getPromotionId();
                if (pid == null && pd.getVoucherCode() != null) {
                    var det = promotionDetailRepository.findByVoucherCode(pd.getVoucherCode());
                    pid = det != null ? det.getPromotionId() : null;
                }
                if (!promotionId.equals(pid)) continue;
            }

            // resolve line (voucher or package)
            String lineId = null, pid = null, type = null;
            if (pd.getVoucherCode() != null && !pd.getVoucherCode().isBlank()) {
                var det = promotionDetailRepository.findByVoucherCode(pd.getVoucherCode());
                if (det != null) {
                    lineId = det.getPromotionLineId();
                    pid = det.getPromotionId();
                    type = "VOUCHER";
                }
            } else if (pd.getPromotionId() != null && p.getPackageId() != null) {
                var opt = promotionDetailRepository.getPackage(pd.getPromotionId(), List.of(p.getPackageId()));
                if (opt != null && opt.isPresent()) {
                    var det = opt.get();
                    lineId = det.getPromotionLineId();
                    pid = det.getPromotionId();
                    type = "PACKAGE";
                }
            }
            if (lineId == null) continue;

            sums.computeIfAbsent(lineId, k -> new long[4]);
            var arr = sums.get(lineId);
            arr[0] += 1;
            arr[1] += L(pd.getDiscountAmount());
            arr[2] += L(pd.getOriginalAmount());
            arr[3] += L(pd.getFinalAmount());
            lineToPromo.put(lineId, pid);
            lineTypeMap.put(lineId, type);
        }

        return sums.entrySet().stream().map(e -> {
                    String lineId = e.getKey();
                    long[] v = e.getValue();
                    String name = getPromotionLineName(lineId);
                    String type = lineTypeMap.get(lineId);
                    return PromotionLineStatsResponse.builder()
                            .promotionId(lineToPromo.get(lineId))
                            .promotionLineId(lineId)
                            .promotionLineName(name)
                            .type(type != null ? type : getPromotionLineType(lineId))
                            .redemptions(v[0])
                            .totalDiscount(v[1])
                            .totalOriginal(v[2])
                            .totalFinal(v[3])
                            .build();
                }).sorted(Comparator.comparingLong(PromotionLineStatsResponse::getRedemptions).reversed())
                .toList();
    }

    // ======================================
// == PROMO: USAGE CHART THEO KHOẢNG TG ==
// ======================================
    @Override
    public PromotionRangeChartResponse getPromotionUsageByRange(LocalDate start, LocalDate end, GroupByDataAnalzerResponse groupBy) {
        if (start != null && end != null && end.isBefore(start)) { var t = start; start = end; end = t; }
        final LocalDate S = start, E = end;

        Map<String, Long> usage = new LinkedHashMap<>();
        Map<String, Long> discount = new LinkedHashMap<>();

        DateTimeFormatter dFmt = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter wFmt = DateTimeFormatter.ofPattern("dd/MM");

        switch (groupBy) {
            case DAY -> {
                for (LocalDate d = S; !d.isAfter(E); d = d.plusDays(1)) {
                    String k = d.format(dFmt);
                    usage.put(k, 0L); discount.put(k, 0L);
                }
            }
            case WEEK -> {
                LocalDate ptr = S.with(DayOfWeek.MONDAY);
                while (!ptr.isAfter(E)) {
                    LocalDate mon = ptr, sun = mon.plusDays(6);
                    String k = mon.format(wFmt) + "–" + sun.format(wFmt);
                    usage.put(k, 0L); discount.put(k, 0L);
                    ptr = ptr.plusWeeks(1);
                }
            }
            case MONTH -> {
                YearMonth ym = YearMonth.from(S), yEnd = YearMonth.from(E);
                while (!ym.isAfter(yEnd)) {
                    String k = ym.toString();
                    usage.put(k, 0L); discount.put(k, 0L);
                    ym = ym.plusMonths(1);
                }
            }
        }

        var paid = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .filter(p -> inRange(paymentLocalDate(p), S, E))
                .toList();

        for (var p : paid) {
            var pd = getPD(p);
            if (pd == null) continue;
            // tính cho mọi giao dịch có áp dụng promotion (voucher hoặc package)
            boolean hasPromo = (pd.getVoucherCode() != null && !pd.getVoucherCode().isBlank())
                    || (pd.getPromotionId() != null);
            if (!hasPromo) continue;

            LocalDate d = paymentLocalDate(p);
            String key = switch (groupBy) {
                case DAY   -> d.format(dFmt);
                case WEEK  -> d.with(DayOfWeek.MONDAY).format(wFmt) + "–" + d.with(DayOfWeek.MONDAY).plusDays(6).format(wFmt);
                case MONTH -> YearMonth.from(d).toString();
            };
            usage.computeIfPresent(key, (k, v) -> v + 1);
            discount.computeIfPresent(key, (k, v) -> v + L(pd.getDiscountAmount()));
        }

        var labels = new ArrayList<>(usage.keySet());
        var uses = labels.stream().map(usage::get).toList();
        var disc = labels.stream().map(discount::get).toList();
        return new PromotionRangeChartResponse(labels, uses, disc);
    }

    // ==================================================
// == PROMO: CHI TIẾT VOUCHER THEO MỘT PROMOTION ID ==
// ==================================================
    @Override
    public List<VoucherUsageItemResponse> getVoucherDetailByPromotion(String promotionId, LocalDate start, LocalDate end) {
        if (promotionId == null || promotionId.isBlank()) return List.of();
        if (start != null && end != null && end.isBefore(start)) { var t = start; start = end; end = t; }
        final LocalDate S = start, E = end;

        var paid = paymentRepository.findAll().stream()
                .filter(this::isPaid)
                .filter(p -> inRange(paymentLocalDate(p), S, E))
                .toList();

        Map<String, List<Payment>> byVoucher = new HashMap<>();
        for (var p : paid) {
            var pd = getPD(p);
            if (pd == null || pd.getVoucherCode() == null || pd.getVoucherCode().isBlank()) continue;
            var det = promotionDetailRepository.findByVoucherCode(pd.getVoucherCode());
            if (det == null || !promotionId.equals(det.getPromotionId())) continue;
            byVoucher.computeIfAbsent(det.getVoucherCode(), k -> new ArrayList<>()).add(p);
        }

        return byVoucher.entrySet().stream().map(e -> {
            String voucher = e.getKey();
            List<Payment> list = e.getValue();
            var det = promotionDetailRepository.findByVoucherCode(voucher);

            long uses = list.size();
            long uu = list.stream().map(Payment::getUserId).filter(Objects::nonNull).distinct().count();

            long sumDisc = 0, sumOri = 0, sumFin = 0;
            LocalDate f = null, l = null;
            for (var p : list) {
                var pd = getPD(p);
                if (pd == null) continue;
                sumDisc += L(pd.getDiscountAmount());
                sumOri  += L(pd.getOriginalAmount());
                sumFin  += L(pd.getFinalAmount());
                LocalDate d = paymentLocalDate(p);
                if (d != null) {
                    if (f == null || d.isBefore(f)) f = d;
                    if (l == null || d.isAfter(l))  l = d;
                }
            }

            return VoucherUsageItemResponse.builder()
                    .promotionId(det != null ? det.getPromotionId() : promotionId)
                    .promotionLineId(det != null ? det.getPromotionLineId() : null)
                    .voucherCode(voucher)
                    .uses(uses)
                    .uniqueUsers(uu)
                    .totalDiscount(sumDisc)
                    .totalOriginal(sumOri)
                    .totalFinal(sumFin)
                    .maxUsage(det != null ? det.getMaxUsage() : null)
                    .usedCount(det != null ? det.getUsedCount() : null)
                    .firstUse(f)
                    .lastUse(l)
                    .build();
        }).sorted(Comparator.comparingLong(VoucherUsageItemResponse::getUses).reversed()).toList();
    }


    // DataAnalyzerServiceImpl
    private PaymentDetail getPD(Payment p) {
        PaymentDetail pd = paymentDetailRepository.findByPaymentId(p.getPaymentId());
        if (pd == null && p.getPaymentCode() != null) {
            // fallback nếu bảng đang khóa theo paymentCode
            try { pd = paymentDetailRepository.findByPaymentCode(p.getPaymentCode()); } catch (Throwable ignore) {}
        }
        return pd;
    }

    private boolean isPaid(Payment p) {
        String s = p.getStatus();
        return s != null && (
                s.equalsIgnoreCase("PAID") || s.equalsIgnoreCase("COMPLETED")
        );
    }



}


