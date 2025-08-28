package flim.backendcartoon.entities;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;
import java.util.List;

@DynamoDbBean
public class Movie {
    // ================== KEYS ==================
    private String movieId;            // PK

    // ================== CORE INFO ==================
    private String slug;               // SEO-friendly, unique trong hệ thống
    private String title;
    private String originalTitle;      // tên gốc (nếu có)
    private String description;
    private String thumbnailUrl;
    private String bannerUrl;          // ảnh cover ngang
    private String trailerUrl;
    private String duration;
    private List<String> genres;       // danh sách thể loại

    private Integer releaseYear;
    private String country;
    private String topic;              // chủ đề (theo FE của bạn)
    private MovieType movieType;       // SINGLE | SERIES

    // ================== ACCESS / VISIBILITY ==================
    private PackageType minPackageType;      // yêu cầu tối thiểu (thay cho accessVipLevel)     // gắn banner nổi bật
    private MovieStatus status;        // ONGOING | COMPLETED | UPCOMING

    // ================== COUNTERS / DENORMALIZED ==================
    private Long viewCount = 0L;
    private Double avgRating = 0.0;
    private Long ratingCount = 0L;

    private Integer seasonsCount = 0;  // số phần (season/part) – phục vụ hiển thị nhanh
    private Integer episodesCount = 0; // tổng số tập (gộp các season)

    private List<String> authorIds;    // danh sách tác giả (đạo diễn/diễn viên...)

    // ================== TIMESTAMPS ==================
    private Instant createdAt;         // chuẩn hóa Instant

    // ================== "INDEX KEYS" phục vụ GSI ==================
    // Pattern: 1 partition key cố định + 1 sort key
    // GSI_Latest_ByCreated:  PK = "LATEST"  | SK = createdAt
    // GSI_Popular_ByView:    PK = "POPULAR" | SK = viewCount
    private String latestKey = "LATEST";
    private String popularKey = "POPULAR";

    // ================== KEYS ==================
    @DynamoDbPartitionKey
    @DynamoDbAttribute("movieId")
    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    // ================== CORE INFO ==================
    @DynamoDbAttribute("slug")
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    @DynamoDbAttribute("title")
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @DynamoDbAttribute("originalTitle")
    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }

    @DynamoDbAttribute("description")
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @DynamoDbAttribute("thumbnailUrl")
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    @DynamoDbAttribute("bannerUrl")
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    @DynamoDbAttribute("trailerUrl")
    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }

    @DynamoDbAttribute("genres")
    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    @DynamoDbAttribute("releaseYear")
    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }

    @DynamoDbAttribute("country")
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    @DynamoDbAttribute("topic")
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    @DynamoDbAttribute("movieType")
    public MovieType getMovieType() { return movieType; }
    public void setMovieType(MovieType movieType) { this.movieType = movieType; }

    // ================== ACCESS / VISIBILITY ==================
    @DynamoDbAttribute("minPackageType")
    public PackageType getMinVipLevel() { return minPackageType; }
    public void setMinVipLevel(PackageType minPackageType) { this.minPackageType = minPackageType; }

    @DynamoDbAttribute("status")
    public MovieStatus getStatus() { return status; }
    public void setStatus(MovieStatus status) { this.status = status; }

    @DynamoDbAttribute("duration")
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    // ================== COUNTERS / DENORMALIZED ==================
    @DynamoDbAttribute("viewCount")
    // Sort key cho GSI_Popular_ByView
    @DynamoDbSecondarySortKey(indexNames = { "GSI_Popular_ByView" })
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    @DynamoDbAttribute("avgRating")
    public Double getAvgRating() { return avgRating; }
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }

    @DynamoDbAttribute("ratingCount")
    public Long getRatingCount() { return ratingCount; }
    public void setRatingCount(Long ratingCount) { this.ratingCount = ratingCount; }

    @DynamoDbAttribute("seasonsCount")
    public Integer getSeasonsCount() { return seasonsCount; }
    public void setSeasonsCount(Integer seasonsCount) { this.seasonsCount = seasonsCount; }

    @DynamoDbAttribute("episodesCount")
    public Integer getEpisodesCount() { return episodesCount; }
    public void setEpisodesCount(Integer episodesCount) { this.episodesCount = episodesCount; }

    @DynamoDbAttribute("authorIds")
    public List<String> getAuthorIds() { return authorIds; }
    public void setAuthorIds(List<String> authorIds) { this.authorIds = authorIds; }

    // ================== TIMESTAMPS ==================
    @DynamoDbAttribute("createdAt")
    // Sort key cho GSI_Latest_ByCreated
    @DynamoDbSecondarySortKey(indexNames = { "GSI_Latest_ByCreated" })
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // ================== INDEX PARTITION KEYS (GSI) ==================
    @DynamoDbAttribute("latestKey")
    @DynamoDbSecondaryPartitionKey(indexNames = { "GSI_Latest_ByCreated" })
    public String getLatestKey() { return latestKey; }
    public void setLatestKey(String latestKey) { this.latestKey = latestKey; }

    @DynamoDbAttribute("popularKey")
    @DynamoDbSecondaryPartitionKey(indexNames = { "GSI_Popular_ByView" })
    public String getPopularKey() { return popularKey; }
    public void setPopularKey(String popularKey) { this.popularKey = popularKey; }


}