package flim.backendcartoon.controllers;

import flim.backendcartoon.dto.WsEventDto;
import flim.backendcartoon.entities.WatchRoom;
import flim.backendcartoon.entities.WatchRoomMember;
import flim.backendcartoon.services.*;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket Controller cho tính năng xem phim chung
 *
 * @author Tran Tan Dat
 * @version 1.0
 */
@Controller
public class WatchRoomWsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WatchRoomService watchRoomService;
    private final WatchRoomMemberService memberService;
    private final RoomMessageService messageService;
    private final RoomPlaybackStateManager playbackStateManager;

    public WatchRoomWsController(
            SimpMessagingTemplate messagingTemplate,
            WatchRoomService watchRoomService,
            WatchRoomMemberService memberService,
            RoomMessageService messageService,
            RoomPlaybackStateManager playbackStateManager) {
        this.messagingTemplate = messagingTemplate;
        this.watchRoomService = watchRoomService;
        this.memberService = memberService;
        this.messageService = messageService;
        this.playbackStateManager = playbackStateManager;
    }

    /**
     * Xử lý user join vào phòng
     * /app/rooms/{roomId}/join
     */
    @MessageMapping("/rooms/{roomId}/join")
    public void handleJoin(@DestinationVariable String roomId,
                          @Payload WsEventDto event) {
        try {
            String userId = event.getSenderId();
            String userName = event.getSenderName();
            String avatarUrl = event.getAvatarUrl();

            // Kiểm tra phòng tồn tại và active
            WatchRoom room = watchRoomService.getWatchRoomById(roomId);
            if (room == null || !"ACTIVE".equals(room.getStatus())) {
                sendErrorToUser(userId, "Room not found or inactive");
                return;
            }

            // Kiểm tra private room
            if (room.isPrivateRoom()) {
                String inviteCode = (String) event.getPayloadValue("inviteCode");
                if (inviteCode == null || !room.getInviteCode().equals(inviteCode)) {
                    sendErrorToUser(userId, "Invalid invite code");
                    return;
                }
            }

            // Kiểm tra xem đã là member chưa
            WatchRoomMember existingMember = memberService.getMember(roomId, userId);
            String role = "MEMBER";

            if (existingMember == null) {
                // Nếu là owner của room thì set role = OWNER
                if (room.getUserId().equals(userId)) {
                    role = "OWNER";
                }
                // Lưu member với userName và avatarUrl từ Frontend
                memberService.addMember(roomId, userId, role, userName, avatarUrl);
            } else {
                role = existingMember.getRole();
                memberService.updateHeartbeat(roomId, userId);
            }

            // Lưu system message
            messageService.createSystemMessage(roomId, userId, userName,
                    userName + " đã tham gia phòng");

            // Broadcast JOIN event to ALL members (including sender)
            WsEventDto joinEvent = new WsEventDto("JOIN");
            joinEvent.setRoomId(roomId);
            joinEvent.setSenderId(userId);
            joinEvent.setSenderName(userName);
            joinEvent.setAvatarUrl(avatarUrl);
            joinEvent.addPayload("role", role);
            joinEvent.setCreatedAt(Instant.now().toString());

            messagingTemplate.convertAndSend("/topic/rooms/" + roomId, joinEvent);

            // Broadcast MEMBER_LIST to ALL members in room (CRITICAL FIX)
            broadcastMemberListToAll(roomId);

            // Gửi SYNC_STATE cho user vừa join only
            sendSyncState(roomId, userId);

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorToUser(event.getSenderId(), "Failed to join room: " + e.getMessage());
        }
    }

    /**
     * Xử lý user leave phòng
     * /app/rooms/{roomId}/leave
     */
    @MessageMapping("/rooms/{roomId}/leave")
    public void handleLeave(@DestinationVariable String roomId,
                           @Payload WsEventDto event) {
        try {
            String userId = event.getSenderId();
            String userName = event.getSenderName();

            // Xóa member
            memberService.removeMember(roomId, userId);

            // Lưu system message
            messageService.createSystemMessage(roomId, userId, userName,
                    userName + " đã rời phòng");

            // Broadcast LEAVE event to ALL members
            WsEventDto leaveEvent = new WsEventDto("LEAVE");
            leaveEvent.setRoomId(roomId);
            leaveEvent.setSenderId(userId);
            leaveEvent.setSenderName(userName);
            leaveEvent.setCreatedAt(Instant.now().toString());

            messagingTemplate.convertAndSend("/topic/rooms/" + roomId, leaveEvent);

            // Broadcast updated MEMBER_LIST to ALL remaining members (CRITICAL FIX)
            broadcastMemberListToAll(roomId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Xử lý chat message
     * /app/rooms/{roomId}/chat
     */
    @MessageMapping("/rooms/{roomId}/chat")
    public void handleChat(@DestinationVariable String roomId,
                          @Payload WsEventDto event) {
        try {
            String userId = event.getSenderId();
            String userName = event.getSenderName();
            String avatarUrl = event.getAvatarUrl();
            String text = (String) event.getPayloadValue("text");

            // Kiểm tra member
            if (!memberService.isMember(roomId, userId)) {
                sendErrorToUser(userId, "You are not a member of this room");
                return;
            }

            // Lưu chat message
            messageService.createChatMessage(roomId, userId, userName, avatarUrl, text);

            // Broadcast CHAT event
            WsEventDto chatEvent = new WsEventDto("CHAT");
            chatEvent.setRoomId(roomId);
            chatEvent.setSenderId(userId);
            chatEvent.setSenderName(userName);
            chatEvent.setAvatarUrl(avatarUrl);
            chatEvent.addPayload("text", text);
            chatEvent.setCreatedAt(Instant.now().toString());

            messagingTemplate.convertAndSend("/topic/rooms/" + roomId, chatEvent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Xử lý control (PLAY, PAUSE, SEEK)
     * /app/rooms/{roomId}/control
     */
    @MessageMapping("/rooms/{roomId}/control")
    public void handleControl(@DestinationVariable String roomId,
                             @Payload WsEventDto event) {
        try {
            String userId = event.getSenderId();
            String controlType = (String) event.getPayloadValue("controlType");

            // Kiểm tra member
            if (!memberService.isMember(roomId, userId)) {
                sendErrorToUser(userId, "You are not a member of this room");
                return;
            }

            // Kiểm tra quyền control (chỉ OWNER được control - có thể config)
            boolean onlyHostControls = true; // TODO: lấy từ room config

            if (onlyHostControls && !memberService.isOwner(roomId, userId)) {
                sendErrorToUser(userId, "Only host can control playback");
                return;
            }

            long positionMs = getLongFromPayload(event, "positionMs", 0L);
            double playbackRate = getDoubleFromPayload(event, "playbackRate", 1.0);
            long atHostTimeMs = System.currentTimeMillis();

            // Cập nhật state và broadcast
            WsEventDto controlEvent = new WsEventDto(controlType);
            controlEvent.setRoomId(roomId);
            controlEvent.setSenderId(userId);
            controlEvent.addPayload("positionMs", positionMs);
            controlEvent.addPayload("playbackRate", playbackRate);
            controlEvent.addPayload("atHostTimeMs", atHostTimeMs);
            controlEvent.setCreatedAt(Instant.now().toString());

            switch (controlType) {
                case "PLAY":
                    playbackStateManager.handlePlay(roomId, positionMs, playbackRate);
                    break;
                case "PAUSE":
                    playbackStateManager.handlePause(roomId, positionMs);
                    break;
                case "SEEK":
                    playbackStateManager.handleSeek(roomId, positionMs);
                    break;
                default:
                    return;
            }

            // Lưu event message (optional)
            Map<String, String> meta = new HashMap<>();
            meta.put("positionMs", String.valueOf(positionMs));
            meta.put("playbackRate", String.valueOf(playbackRate));
            messageService.createEventMessage(roomId, userId, controlType, meta);

            // Broadcast control event
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId, controlEvent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Xử lý ping (heartbeat)
     * /app/rooms/{roomId}/ping
     */
    @MessageMapping("/rooms/{roomId}/ping")
    public void handlePing(@DestinationVariable String roomId,
                          @Payload WsEventDto event) {
        try {
            String userId = event.getSenderId();

            // Cập nhật lastSeenAt
            memberService.updateHeartbeat(roomId, userId);

            // Trả về PONG
            WsEventDto pongEvent = new WsEventDto("PONG");
            pongEvent.setRoomId(roomId);
            pongEvent.addPayload("serverTimeMs", System.currentTimeMillis());
            pongEvent.setCreatedAt(Instant.now().toString());

            messagingTemplate.convertAndSendToUser(userId, "/queue/reply", pongEvent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Broadcast MEMBER_LIST to ALL members in room
     * CRITICAL FIX: Ensures all members see the same member list
     * - Creator sees themselves (1 member initially)
     * - When someone joins, BOTH creator and new joiner see updated list (2 members)
     */
    private void broadcastMemberListToAll(String roomId) {
        try {
            // Lấy tất cả members trong phòng (bao gồm cả members đã có trước đó)
            List<WatchRoomMember> members = memberService.getAllMembers(roomId);

            WsEventDto memberListEvent = new WsEventDto("MEMBER_LIST");
            memberListEvent.setRoomId(roomId);
            memberListEvent.addPayload("members", members);
            memberListEvent.setCreatedAt(Instant.now().toString());

            // ✅ FIXED: Broadcast to /topic (ALL subscribers) instead of /user/queue (single user)
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId, memberListEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gửi SYNC_STATE cho user mới join
     */
    private void sendSyncState(String roomId, String userId) {
        RoomPlaybackStateManager.SyncStateDto syncState = playbackStateManager.getSyncState(roomId);

        WsEventDto syncEvent = new WsEventDto("SYNC_STATE");
        syncEvent.setRoomId(roomId);
        syncEvent.addPayload("playing", syncState.isPlaying());
        syncEvent.addPayload("positionMs", syncState.getPositionMs());
        syncEvent.addPayload("playbackRate", syncState.getPlaybackRate());
        syncEvent.addPayload("serverTimeMs", syncState.getServerTimeMs());
        syncEvent.setCreatedAt(Instant.now().toString());

        messagingTemplate.convertAndSendToUser(userId, "/queue/reply", syncEvent);
    }

    /**
     * Gửi error message cho user
     */
    private void sendErrorToUser(String userId, String errorMessage) {
        WsEventDto errorEvent = new WsEventDto("ERROR");
        errorEvent.addPayload("message", errorMessage);
        errorEvent.setCreatedAt(Instant.now().toString());

        messagingTemplate.convertAndSendToUser(userId, "/queue/reply", errorEvent);
    }

    /**
     * Helper: lấy long từ payload
     */
    private long getLongFromPayload(WsEventDto event, String key, long defaultValue) {
        Object value = event.getPayloadValue(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    /**
     * Helper: lấy double từ payload
     */
    private double getDoubleFromPayload(WsEventDto event, String key, double defaultValue) {
        Object value = event.getPayloadValue(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
}

