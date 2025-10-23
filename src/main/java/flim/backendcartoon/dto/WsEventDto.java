package flim.backendcartoon.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO thống nhất cho tất cả WebSocket events
 *
 * @author Tran Tan Dat
 * @version 1.0
 */
public class WsEventDto {

    private String type;          // JOIN|LEAVE|CHAT|PLAY|PAUSE|SEEK|SYNC_STATE|PING|PONG|SYSTEM
    private String roomId;
    private String senderId;
    private String senderName;
    private String avatarUrl;
    private Map<String, Object> payload;
    private String createdAt;

    public WsEventDto() {
        this.payload = new HashMap<>();
    }

    public WsEventDto(String type) {
        this.type = type;
        this.payload = new HashMap<>();
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // Helper methods
    public void addPayload(String key, Object value) {
        this.payload.put(key, value);
    }

    public Object getPayloadValue(String key) {
        return this.payload.get(key);
    }
}

