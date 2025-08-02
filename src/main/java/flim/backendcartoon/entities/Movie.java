package flim.backendcartoon.entities;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@DynamoDbBean
public class Movie {
    private String movieId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String createdAt;
    private List<String> genres;
    private Long viewCount = 0L;
    private VipLevel accessVipLevel;
    private String duration;
    private String country;
    private String topic;
    private MovieType movieType;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("movieId")
    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    @DynamoDbAttribute("title")
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @DynamoDbAttribute("description")
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @DynamoDbAttribute("thumbnailUrl")
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }


    @DynamoDbAttribute("createdAt")
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @DynamoDbAttribute("genres")
    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres;}

    @DynamoDbAttribute("viewCount")
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount;}

    @DynamoDbAttribute("accessVipLevel")
    public VipLevel getAccessVipLevel() { return accessVipLevel; }
    public void setAccessVipLevel(VipLevel accessVipLevel) { this.accessVipLevel = accessVipLevel;}

    @DynamoDbAttribute("duration")
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    @DynamoDbAttribute("country")
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    @DynamoDbAttribute("topic")
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    @DynamoDbAttribute("movieType")
    public MovieType getMovieType() { return movieType; }
    public void setMovieType(MovieType movieType) { this.movieType = movieType;}

}
