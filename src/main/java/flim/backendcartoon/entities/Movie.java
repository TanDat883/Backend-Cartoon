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
    private String userId;
    private List<String> genres;
    private Long viewCount;
    private String rating;
    private VipLevel accessVipLevel;

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

    @DynamoDbAttribute("userId")
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @DynamoDbAttribute("genres")
    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres;}

    @DynamoDbAttribute("viewCount")
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount;}

    @DynamoDbAttribute("rating")
    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating;}

    @DynamoDbAttribute("accessVipLevel")
    public VipLevel getAccessVipLevel() { return accessVipLevel; }
    public void setAccessVipLevel(VipLevel accessVipLevel) { this.accessVipLevel = accessVipLevel;}

}
