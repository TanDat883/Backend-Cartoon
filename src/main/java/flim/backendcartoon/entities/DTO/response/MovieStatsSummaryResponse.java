package flim.backendcartoon.entities.DTO.response;

public class MovieStatsSummaryResponse {
    private long totalMovies;
    private long totalSingle;
    private long totalSeries;
    private long completedCount;
    private long upcomingCount;

    private long totalSeasons;
    private long totalEpisodes;

    private long addedToday;
    private long addedThisWeek;
    private long addedThisMonth;

    private double avgRatingAll;   // bình quân có trọng số theo ratingCount
    private long totalRatings;

    private String topGenre;       // genre xuất hiện nhiều nhất
    private String topCountry;     // country xuất hiện nhiều nhất

    private TopMovieDTOResponse topByViews;
    private TopMovieDTOResponse topByRating;

    public long getTotalMovies() { return totalMovies; }
    public void setTotalMovies(long totalMovies) { this.totalMovies = totalMovies; }
    public long getTotalSingle() { return totalSingle; }
    public void setTotalSingle(long totalSingle) { this.totalSingle = totalSingle; }
    public long getTotalSeries() { return totalSeries; }
    public void setTotalSeries(long totalSeries) { this.totalSeries = totalSeries; }
    public long getCompletedCount() { return completedCount; }
    public void setCompletedCount(long completedCount) { this.completedCount = completedCount; }
    public long getUpcomingCount() { return upcomingCount; }
    public void setUpcomingCount(long upcomingCount) { this.upcomingCount = upcomingCount; }
    public long getTotalSeasons() { return totalSeasons; }
    public void setTotalSeasons(long totalSeasons) { this.totalSeasons = totalSeasons; }
    public long getTotalEpisodes() { return totalEpisodes; }
    public void setTotalEpisodes(long totalEpisodes) { this.totalEpisodes = totalEpisodes; }
    public long getAddedToday() { return addedToday; }
    public void setAddedToday(long addedToday) { this.addedToday = addedToday; }
    public long getAddedThisWeek() { return addedThisWeek; }
    public void setAddedThisWeek(long addedThisWeek) { this.addedThisWeek = addedThisWeek; }
    public long getAddedThisMonth() { return addedThisMonth; }
    public void setAddedThisMonth(long addedThisMonth) { this.addedThisMonth = addedThisMonth; }
    public double getAvgRatingAll() { return avgRatingAll; }
    public void setAvgRatingAll(double avgRatingAll) { this.avgRatingAll = avgRatingAll; }
    public long getTotalRatings() { return totalRatings; }
    public void setTotalRatings(long totalRatings) { this.totalRatings = totalRatings; }
    public String getTopGenre() { return topGenre; }
    public void setTopGenre(String topGenre) { this.topGenre = topGenre; }
    public String getTopCountry() { return topCountry; }
    public void setTopCountry(String topCountry) { this.topCountry = topCountry; }
    public TopMovieDTOResponse getTopByViews() { return topByViews; }
    public void setTopByViews(TopMovieDTOResponse topByViews) { this.topByViews = topByViews; }
    public TopMovieDTOResponse getTopByRating() { return topByRating; }
    public void setTopByRating(TopMovieDTOResponse topByRating) { this.topByRating = topByRating; }
}