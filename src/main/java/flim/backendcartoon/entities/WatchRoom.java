/*
 * @(#) $(NAME).java    1.0     10/18/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 18-October-2025 3:37 PM
 */

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class WatchRoom {
    private String roomId;            // PK
    private String userId;            // GSI1 PK (DEPRECATED: use hostUserId)
    private String hostUserId;        // Host/creator của phòng
    private String movieId;           // GSI2 PK
    private String roomName;
    private String posterUrl;
    private String videoUrl;          // URL to video file
    private boolean privateRoom;      // rename
    private boolean autoStart;        // rename
    private String startAt;           // ISO-8601 string "2025-10-18T16:30:00+07:00"
    private String createdAt;         // ISO date or datetime
    private String status;            // ACTIVE, SCHEDULED, DELETED, EXPIRED
    private String inviteCode;        // optional
    private Long ttl;                 // DynamoDB TTL (epoch seconds)
    private String deletedAt;         // ISO timestamp khi soft-delete
    private String deletedBy;         // userId của người xóa
    private String expiredAt;         // ISO timestamp khi auto-expire

    // Video state fields for persistence
    private Boolean videoPlaying;
    private Long videoPositionMs;
    private Double videoPlaybackRate;
    private Long videoLastUpdateMs;
    private String videoUpdatedBy;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("roomId")
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    @DynamoDbAttribute("userId")
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @DynamoDbAttribute("hostUserId")
    public String getHostUserId() { return hostUserId; }
    public void setHostUserId(String hostUserId) { this.hostUserId = hostUserId; }

    @DynamoDbAttribute("movieId")
    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    @DynamoDbAttribute("roomName")
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    @DynamoDbAttribute("posterUrl")
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    @DynamoDbAttribute("videoUrl")
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    @DynamoDbAttribute("isPrivate")
    public boolean isPrivateRoom() { return privateRoom; }
    public void setPrivateRoom(boolean privateRoom) { this.privateRoom = privateRoom; }

    @DynamoDbAttribute("isAutoStart")
    public boolean isAutoStart() { return autoStart; }
    public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }

    @DynamoDbAttribute("startAt")
    public String getStartAt() { return startAt; }
    public void setStartAt(String startAt) { this.startAt = startAt; }

    @DynamoDbAttribute("createdAt")
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @DynamoDbAttribute("status")
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @DynamoDbAttribute("inviteCode")
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    @DynamoDbAttribute("videoPlaying")
    public Boolean getVideoPlaying() { return videoPlaying; }
    public void setVideoPlaying(Boolean videoPlaying) { this.videoPlaying = videoPlaying; }

    @DynamoDbAttribute("videoPositionMs")
    public Long getVideoPositionMs() { return videoPositionMs; }
    public void setVideoPositionMs(Long videoPositionMs) { this.videoPositionMs = videoPositionMs; }

    @DynamoDbAttribute("videoPlaybackRate")
    public Double getVideoPlaybackRate() { return videoPlaybackRate; }
    public void setVideoPlaybackRate(Double videoPlaybackRate) { this.videoPlaybackRate = videoPlaybackRate; }

    @DynamoDbAttribute("videoLastUpdateMs")
    public Long getVideoLastUpdateMs() { return videoLastUpdateMs; }
    public void setVideoLastUpdateMs(Long videoLastUpdateMs) { this.videoLastUpdateMs = videoLastUpdateMs; }

    @DynamoDbAttribute("videoUpdatedBy")
    public String getVideoUpdatedBy() { return videoUpdatedBy; }
    public void setVideoUpdatedBy(String videoUpdatedBy) { this.videoUpdatedBy = videoUpdatedBy; }

    @DynamoDbAttribute("ttl")
    public Long getTtl() { return ttl; }
    public void setTtl(Long ttl) { this.ttl = ttl; }

    @DynamoDbAttribute("deletedAt")
    public String getDeletedAt() { return deletedAt; }
    public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }

    @DynamoDbAttribute("deletedBy")
    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

    @DynamoDbAttribute("expiredAt")
    public String getExpiredAt() { return expiredAt; }
    public void setExpiredAt(String expiredAt) { this.expiredAt = expiredAt; }
}

