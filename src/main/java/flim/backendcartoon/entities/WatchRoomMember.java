package flim.backendcartoon.entities;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class WatchRoomMember {

    private String roomId;     // PK
    private String userId;     // SK
    private String userName;   // Tên hiển thị
    private String avatarUrl;  // Avatar URL
    private String joinedAt;   // ISO-8601
    private String lastSeenAt; // ISO-8601 (heartbeat)
    private String role;       // OWNER | CO_HOST | MEMBER
    private Long   expireAt;   // TTL (epoch seconds), optional

    @DynamoDbPartitionKey
    @DynamoDbAttribute("roomId")
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    @DynamoDbSortKey
    @DynamoDbAttribute("userId")
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // GSI để truy vấn tất cả phòng mà 1 user đang tham gia
    @DynamoDbSecondaryPartitionKey(indexNames = "GSI_UserId")
    @DynamoDbAttribute("gsiUserId")
    public String getGsiUserId() { return userId; } // reuse userId làm key GSI
    @DynamoDbAttribute("userName")
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    @DynamoDbAttribute("avatarUrl")
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public void setGsiUserId(String ignored) { /* no-op */ }

    @DynamoDbAttribute("joinedAt")
    public String getJoinedAt() { return joinedAt; }
    public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }

    @DynamoDbAttribute("lastSeenAt")
    public String getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(String lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    @DynamoDbAttribute("role")
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @DynamoDbAttribute("expireAt")
    public Long getExpireAt() { return expireAt; }
    public void setExpireAt(Long expireAt) { this.expireAt = expireAt; }
}
