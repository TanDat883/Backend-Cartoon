package flim.backendcartoon.entities;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.HashMap;
import java.util.Map;

/**
 * User behavior signal for ML personalization
 *
 * Captures user interactions:
 * - click_movie: User clicked on a movie card
 * - view_start: Started watching
 * - view_engaged: Watched > 30s
 * - view_end: Finished episode
 * - add_wishlist: Added to watchlist
 * - click_like: Liked/rated movie
 * - search_query: Submitted chat query
 *
 * @author CartoonToo ML Team
 * @version 1.0 - Layer 1 Implementation
 */
@DynamoDbBean
public class UserSignal {

    private String userId;              // PK
    private Long timestamp;             // SK (sortKey)
    private String eventType;           // "click_movie" | "view_engaged" | etc.
    private String movieId;             // Which movie (nullable for search_query)
    private String movieTitle;          // Denormalized for easy display
    private Map<String, String> metadata; // Flexible: {source: "chatbot", watchTime: "35", query: "anime hay"}
    private Long ttl;                   // DynamoDB TTL: 90 days

    @DynamoDbPartitionKey
    @DynamoDbAttribute("userId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("timestamp")
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @DynamoDbAttribute("eventType")
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @DynamoDbAttribute("movieId")
    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    @DynamoDbAttribute("movieTitle")
    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    @DynamoDbAttribute("metadata")
    public Map<String, String> getMetadata() {
        return metadata != null ? metadata : new HashMap<>();
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @DynamoDbAttribute("ttl")
    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    // Helper: Extract dwell time from metadata
    public Integer getDwellTime() {
        Object dwellTime = getMetadata().get("dwellTime");
        if (dwellTime instanceof Integer) {
            return (Integer) dwellTime;
        }
        return 0;
    }
}

