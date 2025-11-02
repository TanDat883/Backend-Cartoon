package flim.backendcartoon.entities;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User behavior profile for personalization
 *
 * Lưu trữ embedding vector và preferences của user để:
 * - Personalized recommendations (cosine similarity)
 * - Conversational persona (tone, style)
 * - Proactive suggestions (intent prediction)
 *
 * @author CartoonToo ML Team
 * @version 1.0 - Layer 1 Implementation
 */
@DynamoDbBean
public class UserProfile {

    private String userId;                    // PK
    private List<Float> userVector;           // 384-dim embedding (EMA of clicked items)
    private List<String> topGenres;           // Top 5 genres user watches most
    private String priceTier;                 // "FREE" | "BASIC" | "PREMIUM"
    private List<String> lastIntents;         // Last 5 intents: ["rec", "promo", "filter:action"]
    private Map<String, Integer> genreCount;  // {"Action": 15, "Romance": 8}
    private String preferredTone;             // "energetic" | "calm" | "professional"
    private Long totalInteractions;           // Total signals captured
    private Long lastUpdated;                 // Timestamp
    private Long ttl;                         // DynamoDB TTL (90 days)
    private Boolean personalizationEnabled;   // User consent for personalization

    @DynamoDbPartitionKey
    @DynamoDbAttribute("userId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbAttribute("userVector")
    public List<Float> getUserVector() {
        return userVector;
    }

    public void setUserVector(List<Float> userVector) {
        this.userVector = userVector;
    }

    @DynamoDbAttribute("topGenres")
    public List<String> getTopGenres() {
        return topGenres != null ? topGenres : new ArrayList<>();
    }

    public void setTopGenres(List<String> topGenres) {
        this.topGenres = topGenres;
    }

    @DynamoDbAttribute("priceTier")
    public String getPriceTier() {
        return priceTier != null ? priceTier : "FREE";
    }

    public void setPriceTier(String priceTier) {
        this.priceTier = priceTier;
    }

    @DynamoDbAttribute("lastIntents")
    public List<String> getLastIntents() {
        return lastIntents != null ? lastIntents : new ArrayList<>();
    }

    public void setLastIntents(List<String> lastIntents) {
        this.lastIntents = lastIntents;
    }

    @DynamoDbAttribute("genreCount")
    public Map<String, Integer> getGenreCount() {
        return genreCount != null ? genreCount : new HashMap<>();
    }

    public void setGenreCount(Map<String, Integer> genreCount) {
        this.genreCount = genreCount;
    }

    @DynamoDbAttribute("preferredTone")
    public String getPreferredTone() {
        return preferredTone != null ? preferredTone : "friendly";
    }

    public void setPreferredTone(String preferredTone) {
        this.preferredTone = preferredTone;
    }

    @DynamoDbAttribute("totalInteractions")
    public Long getTotalInteractions() {
        return totalInteractions != null ? totalInteractions : 0L;
    }

    public void setTotalInteractions(Long totalInteractions) {
        this.totalInteractions = totalInteractions;
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

    @DynamoDbAttribute("personalizationEnabled")
    public Boolean getPersonalizationEnabled() {
        return personalizationEnabled != null ? personalizationEnabled : true;
    }

    public void setPersonalizationEnabled(Boolean personalizationEnabled) {
        this.personalizationEnabled = personalizationEnabled;
    }
}

