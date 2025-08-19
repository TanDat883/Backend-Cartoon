package flim.backendcartoon.entities;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
public class Episode {
    // ===== KEYS =====
    // PK: seasonId  -> tất cả tập thuộc 1 season
    // SK: episodeNumber (1..N) -> query ra đúng thứ tự tập
    private  String seasonId;       // ID của phần chứa episode này
    private Integer episodeNumber;
    // ===== ATTRIBUTES =====
    private String episodeId;
    private String movieId;
    private String title;
    private String videoUrl;

    private Instant releaseDate;      // (khuyến nghị) ngày phát hành tập
    private Instant createdAt;        // chuẩn hóa Instant
    private Instant updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("seasonId")
    public String getSeasonId() { return seasonId; }
    public void setSeasonId(String seasonId) { this.seasonId = seasonId; }

    @DynamoDbSortKey
    @DynamoDbAttribute("episodeNumber")
    public Integer getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }

    @DynamoDbAttribute("episodeId")
    public String getEpisodeId() { return episodeId; }
    public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }

    @DynamoDbAttribute("movieId")
    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    @DynamoDbAttribute("title")
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @DynamoDbAttribute("videoUrl")
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    @DynamoDbAttribute("releaseDate")
    public Instant getReleaseDate() { return releaseDate; }
    public void setReleaseDate(Instant releaseDate) { this.releaseDate = releaseDate; }

    @DynamoDbAttribute("createdAt")
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @DynamoDbAttribute("updatedAt")
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}