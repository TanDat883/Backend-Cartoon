package flim.backendcartoon.entities;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
public class PlaybackIssue {
    private String targetKey;        // PK
    private String issueId;          // SK

    private String movieId;          // để hiển thị
    private String seasonId;         // optional
    private Integer episodeNumber;   // optional

    private String reportedBy;       // userId
    private String userTypeKey;      // "U#<userId>#T#<type>" (chống trùng mềm)
    private PlaybackIssueType type;  // VIDEO/AUDIO/SUBTITLE/OTHER
    private String detail;           // mô tả FE

    private PlaybackIssueStatus status; // OPEN/IN_PROGRESS/RESOLVED/INVALID
    private Integer reportCount;        // cộng dồn khi trùng
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastReportedAt;

    // (tùy chọn) TTL dọn sau N ngày khi closed
    private Long ttl;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("targetKey")
    public String getTargetKey() { return targetKey; }
    public void setTargetKey(String targetKey) { this.targetKey = targetKey; }

    @DynamoDbSortKey
    @DynamoDbAttribute("issueId")
    public String getIssueId() { return issueId; }
    public void setIssueId(String issueId) { this.issueId = issueId; }

    @DynamoDbAttribute("movieId") public String getMovieId(){return movieId;}
    public void setMovieId(String movieId){this.movieId = movieId;}

    @DynamoDbAttribute("seasonId") public String getSeasonId(){return seasonId;}
    public void setSeasonId(String seasonId){this.seasonId = seasonId;}

    @DynamoDbAttribute("episodeNumber") public Integer getEpisodeNumber(){return episodeNumber;}
    public void setEpisodeNumber(Integer episodeNumber){this.episodeNumber = episodeNumber;}

    @DynamoDbAttribute("reportedBy") public String getReportedBy(){return reportedBy;}
    public void setReportedBy(String reportedBy){this.reportedBy = reportedBy;}

    @DynamoDbAttribute("userTypeKey") public String getUserTypeKey(){return userTypeKey;}
    public void setUserTypeKey(String userTypeKey){this.userTypeKey = userTypeKey;}

    @DynamoDbAttribute("type") public PlaybackIssueType getType(){return type;}
    public void setType(PlaybackIssueType type){this.type = type;}

    @DynamoDbAttribute("detail") public String getDetail(){return detail;}
    public void setDetail(String detail){this.detail = detail;}

    @DynamoDbAttribute("status") public PlaybackIssueStatus getStatus(){return status;}
    public void setStatus(PlaybackIssueStatus status){this.status = status;}

    @DynamoDbAttribute("reportCount") public Integer getReportCount(){return reportCount;}
    public void setReportCount(Integer reportCount){this.reportCount = reportCount;}

    @DynamoDbAttribute("createdAt") public Instant getCreatedAt(){return createdAt;}
    public void setCreatedAt(Instant createdAt){this.createdAt = createdAt;}

    @DynamoDbAttribute("updatedAt") public Instant getUpdatedAt(){return updatedAt;}
    public void setUpdatedAt(Instant updatedAt){this.updatedAt = updatedAt;}

    @DynamoDbAttribute("lastReportedAt") public Instant getLastReportedAt(){return lastReportedAt;}
    public void setLastReportedAt(Instant lastReportedAt){this.lastReportedAt = lastReportedAt;}

    @DynamoDbAttribute("ttl") public Long getTtl(){return ttl;}
    public void setTtl(Long ttl){this.ttl = ttl;}
}