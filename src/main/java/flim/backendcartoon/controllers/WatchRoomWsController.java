package flim.backendcartoon.controllers;

import flim.backendcartoon.dto.WsEventDto;
import flim.backendcartoon.entities.WatchRoom;
import flim.backendcartoon.entities.WatchRoomMember;
import flim.backendcartoon.scheduler.InactiveMemberCleanupService;
import flim.backendcartoon.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
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

    private static final Logger log = LoggerFactory.getLogger(WatchRoomWsController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final WatchRoomService watchRoomService;
    private final WatchRoomMemberService memberService;
    private final RoomMessageService messageService;
    private final RoomPlaybackStateManager playbackStateManager;
    private final InactiveMemberCleanupService cleanupService;

    public WatchRoomWsController(
            SimpMessagingTemplate messagingTemplate,
            WatchRoomService watchRoomService,
            WatchRoomMemberService memberService,
            RoomMessageService messageService,
            RoomPlaybackStateManager playbackStateManager,
            InactiveMemberCleanupService cleanupService) {
        this.messagingTemplate = messagingTemplate;
        this.watchRoomService = watchRoomService;
        this.memberService = memberService;
        this.messageService = messageService;
        this.playbackStateManager = playbackStateManager;
        this.cleanupService = cleanupService;
    }

    /**
     * Xử lý user join vào phòng
     * /app/rooms/{roomId}/join
     */
    @MessageMapping("/rooms/{roomId}/join")
    public void handleJoin(@DestinationVariable String roomId,
                          @Payload WsEventDto event,
                          @Header("simpSessionId") String sessionId) {
        log.info("🔵 JOIN received: roomId={}, userId={}, userName={}, sessionId={}",
                roomId, event.getSenderId(), event.getSenderName(), sessionId);

        try {
            String userId = event.getSenderId();
            String userName = event.getSenderName();
            String avatarUrl = event.getAvatarUrl();

            // Validate input
            if (userId == null || userName == null) {
                log.error("❌ Invalid JOIN event: missing required fields (userId or userName)");
                sendErrorToUser(userId, "Missing required fields");
                return;
            }

            // Kiểm tra phòng tồn tại và active
            log.debug("🔍 Checking if room exists: {}", roomId);
            WatchRoom room;
            try {
                room = watchRoomService.getWatchRoomById(roomId);
            } catch (flim.backendcartoon.exception.RoomGoneException e) {
                log.warn("⚠️ Room is deleted/expired: roomId={}, userId={}", roomId, userId);
                sendErrorToUser(userId, "This room has been deleted or expired");
                return;
            }

            if (room == null || !"ACTIVE".equals(room.getStatus())) {
                log.error("❌ Room not found or inactive: roomId={}, status={}",
                        roomId, room != null ? room.getStatus() : "NULL");
                sendErrorToUser(userId, "Room not found or inactive");
                return;
            }
            log.info("✅ Room found: roomId={}, status={}, owner={}",
                    roomId, room.getStatus(), room.getUserId());

            // Kiểm tra private room
            if (room.isPrivateRoom()) {
                log.debug("🔒 Private room - checking invite code");
                String inviteCode = (String) event.getPayloadValue("inviteCode");
                if (inviteCode == null || !room.getInviteCode().equals(inviteCode)) {
                    log.error("❌ Invalid invite code for private room: roomId={}", roomId);
                    sendErrorToUser(userId, "Invalid invite code");
                    return;
                }
                log.info("✅ Invite code validated");
            }

            // Kiểm tra xem đã là member chưa
            log.debug("🔍 Checking existing member: roomId={}, userId={}", roomId, userId);
            WatchRoomMember existingMember = memberService.getMember(roomId, userId);
            String role = "MEMBER";

            if (existingMember == null) {
                // Nếu là owner của room thì set role = OWNER
                if (room.getUserId().equals(userId)) {
                    role = "OWNER";
                    log.info("👑 User is room owner: userId={}", userId);
                }
                // Lưu member với userName và avatarUrl từ Frontend
                log.info("💾 Saving new member to DB: roomId={}, userId={}, role={}, userName={}",
                        roomId, userId, role, userName);
                try {
                    WatchRoomMember savedMember = memberService.addMember(roomId, userId, role, userName, avatarUrl);
                    log.info("✅ Member saved successfully: {}", savedMember);
                } catch (Exception e) {
                    log.error("❌ Failed to save member to DB: ", e);
                    // Continue anyway to broadcast event
                }
            } else {
                role = existingMember.getRole();
                log.info("♻️ Member already exists, updating heartbeat: userId={}, role={}", userId, role);
                memberService.updateHeartbeat(roomId, userId);
            }

            // Initialize ping tracking for this member
            cleanupService.updateMemberPing(roomId, userId);
            log.debug("⏱️ Ping tracking initialized for member: userId={}", userId);

            // Lưu system message
            try {
                messageService.createSystemMessage(roomId, userId, userName,
                        userName + " đã tham gia phòng");
                log.debug("💬 System message saved: {} joined room", userName);
            } catch (Exception e) {
                log.error("❌ Failed to save system message: ", e);
            }

            // Broadcast JOIN event to ALL members (including sender)
            WsEventDto joinEvent = new WsEventDto("JOIN");
            joinEvent.setRoomId(roomId);
            joinEvent.setSenderId(userId);
            joinEvent.setSenderName(userName);
            joinEvent.setAvatarUrl(avatarUrl);
            joinEvent.addPayload("role", role);
            joinEvent.setCreatedAt(Instant.now().toString());

            String destination = "/topic/rooms/" + roomId;
            log.info("📢 Broadcasting JOIN event to destination: {}", destination);
            try {
                messagingTemplate.convertAndSend(destination, joinEvent);
                log.info("✅ JOIN event broadcasted successfully to {}", destination);
            } catch (Exception e) {
                log.error("❌ Failed to broadcast JOIN event: ", e);
            }

            // Broadcast MEMBER_LIST to ALL members in room (CRITICAL FIX)
            log.info("📋 Broadcasting MEMBER_LIST to all members in room: {}", roomId);
            broadcastMemberListToAll(roomId);

            // Gửi SYNC_STATE cho user vừa join only
            log.info("🔄 Sending SYNC_STATE to user: {}", userId);
            sendSyncState(roomId, userId);

            // Gửi UNREAD_COUNT cho user vừa join
            log.info("📬 Sending UNREAD_COUNT to user: {}", userId);
            sendUnreadCount(roomId, userId);

            log.info("🎉 JOIN handling completed successfully for userId={} in roomId={}", userId, roomId);

        } catch (Exception e) {
            log.error("❌ Unexpected error handling JOIN: roomId={}, event={}", roomId, event, e);
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

            // Remove ping tracking
            cleanupService.removeMemberPing(roomId, userId);

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
     * Xử lý read receipt - Mark messages as read
     * /app/rooms/{roomId}/read
     */
    @MessageMapping("/rooms/{roomId}/read")
    public void handleReadReceipt(@DestinationVariable String roomId,
                                  @Payload WsEventDto event,
                                  @Header("simpSessionId") String sessionId) {
        log.info("👁️ READ_RECEIPT received: roomId={}, userId={}, sessionId={}",
                roomId, event.getSenderId(), sessionId);

        try {
            String userId = event.getSenderId();
            String lastReadMessageSortKey = (String) event.getPayloadValue("lastReadMessageSortKey");

            if (lastReadMessageSortKey == null) {
                log.warn("⚠️ READ_RECEIPT missing lastReadMessageSortKey");
                return;
            }

            log.debug("📖 Marking messages as read up to: {}", lastReadMessageSortKey);

            // Update read receipt in DB
            memberService.updateReadReceipt(roomId, userId, lastReadMessageSortKey);
            log.info("✅ Read receipt updated successfully for user: {}", userId);

            // Optional: Broadcast to others (for "seen by" feature) - Not needed for current requirement
            // Could be added later if needed

        } catch (Exception e) {
            log.error("❌ Unexpected error handling READ_RECEIPT: roomId={}, event={}", roomId, event, e);
        }
    }

    /**
     * Xử lý chat message
     * /app/rooms/{roomId}/chat
     */
    @MessageMapping("/rooms/{roomId}/chat")
    public void handleChat(@DestinationVariable String roomId,
                          @Payload WsEventDto event,
                          @Header("simpSessionId") String sessionId) {
        log.info("💬 CHAT received: roomId={}, userId={}, userName={}, sessionId={}",
                roomId, event.getSenderId(), event.getSenderName(), sessionId);

        try {
            String userId = event.getSenderId();
            String userName = event.getSenderName();
            String avatarUrl = event.getAvatarUrl();
            String text = (String) event.getPayloadValue("text");

            log.debug("💬 Chat message content: '{}'", text);

            // Kiểm tra member
            log.debug("🔍 Checking if user is member: roomId={}, userId={}", roomId, userId);
            if (!memberService.isMember(roomId, userId)) {
                log.error("❌ User is not a member of room: userId={}, roomId={}", userId, roomId);
                sendErrorToUser(userId, "You are not a member of this room");
                return;
            }
            log.info("✅ User is valid member");

            // Lưu chat message
            log.debug("💾 Saving chat message to DB");
            try {
                messageService.createChatMessage(roomId, userId, userName, avatarUrl, text);
                log.info("✅ Chat message saved to DB");
            } catch (Exception e) {
                log.error("❌ Failed to save chat message: ", e);
            }

            // Broadcast CHAT event
            WsEventDto chatEvent = new WsEventDto("CHAT");
            chatEvent.setRoomId(roomId);
            chatEvent.setSenderId(userId);
            chatEvent.setSenderName(userName);
            chatEvent.setAvatarUrl(avatarUrl);
            chatEvent.addPayload("text", text);
            chatEvent.setCreatedAt(Instant.now().toString());

            String destination = "/topic/rooms/" + roomId;
            log.info("📢 Broadcasting CHAT event to destination: {}", destination);
            try {
                messagingTemplate.convertAndSend(destination, chatEvent);
                log.info("✅ CHAT event broadcasted successfully");
            } catch (Exception e) {
                log.error("❌ Failed to broadcast CHAT event: ", e);
            }

        } catch (Exception e) {
            log.error("❌ Unexpected error handling CHAT: roomId={}, event={}", roomId, event, e);
        }
    }

    /**
     * Xử lý control (PLAY, PAUSE, SEEK)
     * /app/rooms/{roomId}/control
     */
    @MessageMapping("/rooms/{roomId}/control")
    public void handleControl(@DestinationVariable String roomId,
                             @Payload WsEventDto event,
                             @Header("simpSessionId") String sessionId) {
        String controlType = (String) event.getPayloadValue("controlType");
        log.info("🎮 CONTROL received: roomId={}, userId={}, controlType={}, sessionId={}",
                roomId, event.getSenderId(), controlType, sessionId);

        try {
            String userId = event.getSenderId();

            // Kiểm tra member
            log.debug("🔍 Checking if user is member: roomId={}, userId={}", roomId, userId);
            if (!memberService.isMember(roomId, userId)) {
                log.error("❌ User is not a member of room: userId={}, roomId={}", userId, roomId);
                sendErrorToUser(userId, "You are not a member of this room");
                return;
            }

            // Kiểm tra quyền control (chỉ OWNER được control - có thể config)
            boolean onlyHostControls = true; // TODO: lấy từ room config

            if (onlyHostControls && !memberService.isOwner(roomId, userId)) {
                log.warn("⚠️ User tried to control but is not owner: userId={}", userId);
                sendErrorToUser(userId, "Only host can control playback");
                return;
            }
            log.info("✅ User has control permission");

            long positionMs = getLongFromPayload(event, "positionMs", 0L);
            double playbackRate = getDoubleFromPayload(event, "playbackRate", 1.0);
            long atHostTimeMs = System.currentTimeMillis();

            log.debug("🎬 Control details: type={}, positionMs={}, playbackRate={}",
                    controlType, positionMs, playbackRate);

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
                    log.debug("▶️ Processing PLAY command");
                    playbackStateManager.handlePlay(roomId, positionMs, playbackRate);
                    // Persist to database
                    watchRoomService.updateVideoState(roomId, true, positionMs, playbackRate, userId);
                    log.info("✅ PLAY state updated and persisted");
                    break;
                case "PAUSE":
                    log.debug("⏸️ Processing PAUSE command");
                    playbackStateManager.handlePause(roomId, positionMs);
                    // Persist to database
                    watchRoomService.updateVideoState(roomId, false, positionMs, null, userId);
                    log.info("✅ PAUSE state updated and persisted");
                    break;
                case "SEEK":
                    log.debug("⏩ Processing SEEK command");
                    playbackStateManager.handleSeek(roomId, positionMs);
                    // Persist to database
                    watchRoomService.updateVideoState(roomId, null, positionMs, null, userId);
                    log.info("✅ SEEK state updated and persisted");
                    break;
                default:
                    log.warn("⚠️ Unknown control type: {}", controlType);
                    return;
            }

            // Lưu event message (optional)
            try {
                Map<String, String> meta = new HashMap<>();
                meta.put("positionMs", String.valueOf(positionMs));
                meta.put("playbackRate", String.valueOf(playbackRate));
                messageService.createEventMessage(roomId, userId, controlType, meta);
                log.debug("💾 Event message saved");
            } catch (Exception e) {
                log.error("❌ Failed to save event message: ", e);
            }

            // Broadcast control event
            String destination = "/topic/rooms/" + roomId;
            log.info("📢 Broadcasting CONTROL event to destination: {}", destination);
            try {
                messagingTemplate.convertAndSend(destination, controlEvent);
                log.info("✅ CONTROL event broadcasted successfully");
            } catch (Exception e) {
                log.error("❌ Failed to broadcast CONTROL event: ", e);
            }

        } catch (Exception e) {
            log.error("❌ Unexpected error handling CONTROL: roomId={}, controlType={}, event={}",
                    roomId, controlType, event, e);
        }
    }

    /**
     * Xử lý ping (heartbeat)
     * /app/rooms/{roomId}/ping
     */
    @MessageMapping("/rooms/{roomId}/ping")
    public void handlePing(@DestinationVariable String roomId,
                          @Payload WsEventDto event,
                          @Header("simpSessionId") String sessionId) {
        log.debug("💓 PING received: roomId={}, userId={}, sessionId={}",
                roomId, event.getSenderId(), sessionId);

        try {
            String userId = event.getSenderId();

            // Update ping tracking for auto-cleanup
            cleanupService.updateMemberPing(roomId, userId);

            // Cập nhật lastSeenAt
            memberService.updateHeartbeat(roomId, userId);
            log.debug("✅ Heartbeat updated for user: {}", userId);

            // Trả về PONG
            WsEventDto pongEvent = new WsEventDto("PONG");
            pongEvent.setRoomId(roomId);
            pongEvent.addPayload("serverTimeMs", System.currentTimeMillis());
            pongEvent.setCreatedAt(Instant.now().toString());

            String destination = "/user/" + userId + "/queue/reply";
            log.debug("📤 Sending PONG to user: {}, destination: {}", userId, destination);
            try {
                messagingTemplate.convertAndSendToUser(userId, "/queue/reply", pongEvent);
                log.debug("✅ PONG sent successfully");
            } catch (Exception e) {
                log.error("❌ Failed to send PONG: ", e);
            }

        } catch (Exception e) {
            log.error("❌ Unexpected error handling PING: roomId={}, event={}", roomId, event, e);
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

            String destination = "/topic/rooms/" + roomId;

            // Broadcast to /topic (ALL subscribers)
            messagingTemplate.convertAndSend(destination, memberListEvent);

        } catch (Exception e) {
            log.error("❌ Failed to broadcast MEMBER_LIST: roomId={}", roomId, e);
        }
    }

    /**
     * Gửi SYNC_STATE cho user mới join
     */
    private void sendSyncState(String roomId, String userId) {
        log.debug("🔄 Preparing SYNC_STATE: roomId={}, userId={}", roomId, userId);
        try {
            RoomPlaybackStateManager.SyncStateDto syncState = playbackStateManager.getSyncState(roomId);

            WsEventDto syncEvent = new WsEventDto("SYNC_STATE");
            syncEvent.setRoomId(roomId);
            syncEvent.addPayload("playing", syncState.isPlaying());
            syncEvent.addPayload("positionMs", syncState.getPositionMs());
            syncEvent.addPayload("playbackRate", syncState.getPlaybackRate());
            syncEvent.addPayload("serverTimeMs", syncState.getServerTimeMs());
            syncEvent.setCreatedAt(Instant.now().toString());

            String destination = "/user/" + userId + "/queue/reply";
            log.info("📤 Sending SYNC_STATE to user: {}, destination: {}, playing={}, positionMs={}",
                    userId, destination, syncState.isPlaying(), syncState.getPositionMs());

            messagingTemplate.convertAndSendToUser(userId, "/queue/reply", syncEvent);
            log.info("✅ SYNC_STATE sent successfully");

        } catch (Exception e) {
            log.error("❌ Failed to send SYNC_STATE: roomId={}, userId={}", roomId, userId, e);
        }
    }

    /**
     * Gửi UNREAD_COUNT cho user mới join hoặc khi có update
     */
    private void sendUnreadCount(String roomId, String userId) {
        log.debug("📬 Preparing UNREAD_COUNT: roomId={}, userId={}", roomId, userId);
        try {
            // Get last read message sort key from member data
            String lastReadMessageSortKey = memberService.getLastReadMessageSortKey(roomId, userId);

            // Calculate unread count
            int unreadCount = messageService.getUnreadCount(roomId, lastReadMessageSortKey);

            log.info("📊 Unread count calculated: roomId={}, userId={}, count={}, lastReadSortKey={}",
                    roomId, userId, unreadCount, lastReadMessageSortKey);

            // Send UNREAD_COUNT event
            WsEventDto unreadEvent = new WsEventDto("UNREAD_COUNT");
            unreadEvent.setRoomId(roomId);
            unreadEvent.addPayload("unreadCount", unreadCount);
            unreadEvent.addPayload("lastReadMessageSortKey", lastReadMessageSortKey);
            unreadEvent.setCreatedAt(Instant.now().toString());

            String destination = "/user/" + userId + "/queue/reply";
            log.info("📤 Sending UNREAD_COUNT to user: {}, destination: {}, count={}",
                    userId, destination, unreadCount);

            messagingTemplate.convertAndSendToUser(userId, "/queue/reply", unreadEvent);
            log.info("✅ UNREAD_COUNT sent successfully");

        } catch (Exception e) {
            log.error("❌ Failed to send UNREAD_COUNT: roomId={}, userId={}", roomId, userId, e);
        }
    }

    /**
     * Gửi error message cho user
     */
    private void sendErrorToUser(String userId, String errorMessage) {
        log.warn("⚠️ Sending ERROR to user: userId={}, message={}", userId, errorMessage);
        try {
            WsEventDto errorEvent = new WsEventDto("ERROR");
            errorEvent.addPayload("message", errorMessage);
            errorEvent.setCreatedAt(Instant.now().toString());

            messagingTemplate.convertAndSendToUser(userId, "/queue/reply", errorEvent);
            log.debug("✅ ERROR sent successfully to user: {}", userId);
        } catch (Exception e) {
            log.error("❌ Failed to send ERROR to user: {}", userId, e);
        }
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

