package flim.backendcartoon.entities;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.List;

/**
 * Item (Movie) embedding for semantic similarity
 *
 * Generated using OpenAI text-embedding-3-small (384 dimensions)
 * Used for:
 * - Cosine similarity with user vectors
 * - Semantic search
 * - Hybrid ranking
 *
 * @author CartoonToo ML Team
 * @version 1.0 - Layer 1 Implementation
 */
@DynamoDbBean
public class ItemEmbedding {

    private String movieId;           // PK
    private List<Float> vector;       // 384-dim embedding
    private List<String> genres;      // Denormalized for quick access
    private List<String> tags;        // ["shounen", "supernatural", "comedy"]
    private Integer releaseYear;
    private String embeddingVersion;  // "v1" for versioning
    private Long lastUpdated;
    private Long ttl;                 // Optional: 1 year

    @DynamoDbPartitionKey
    @DynamoDbAttribute("movieId")
    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    @DynamoDbAttribute("vector")
    public List<Float> getVector() {
        return vector;
    }

    public void setVector(List<Float> vector) {
        this.vector = vector;
    }

    @DynamoDbAttribute("genres")
    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    @DynamoDbAttribute("tags")
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @DynamoDbAttribute("releaseYear")
    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    @DynamoDbAttribute("embeddingVersion")
    public String getEmbeddingVersion() {
        return embeddingVersion != null ? embeddingVersion : "v1";
    }

    public void setEmbeddingVersion(String embeddingVersion) {
        this.embeddingVersion = embeddingVersion;
    }

    @DynamoDbAttribute("lastUpdated")
    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @DynamoDbAttribute("ttl")
    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}

