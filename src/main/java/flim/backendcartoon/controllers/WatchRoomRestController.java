package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.response.DeleteRoomResponse;
import flim.backendcartoon.entities.WatchRoomMember;
import flim.backendcartoon.services.RoomMessageService;
import flim.backendcartoon.services.WatchRoomMemberService;
import flim.backendcartoon.services.WatchRoomService;
import flim.backendcartoon.utils.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller cho các endpoint phụ trợ của Watch Room
 *
 * @author Tran Tan Dat
 * @version 1.0
 */
@RestController
@RequestMapping({"/api/watchrooms", "/watchrooms"})  // Support both paths
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:*"})
public class WatchRoomRestController {

    private static final Logger log = LoggerFactory.getLogger(WatchRoomRestController.class);

    private final WatchRoomMemberService memberService;
    private final RoomMessageService messageService;
    private final WatchRoomService watchRoomService;

    public WatchRoomRestController(
            WatchRoomMemberService memberService,
            RoomMessageService messageService,
            WatchRoomService watchRoomService) {
        this.memberService = memberService;
        this.messageService = messageService;
        this.watchRoomService = watchRoomService;
    }

    /**
     * GET /api/watchrooms/{roomId}/members
     * Lấy danh sách thành viên đang online trong phòng
     */
    @GetMapping("/{roomId}/members")
    public ResponseEntity<?> getMembers(@PathVariable String roomId) {
        try {
            List<WatchRoomMember> members = memberService.getOnlineMembers(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("roomId", roomId);
            response.put("members", members);
            response.put("count", members.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get members",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/watchrooms/{roomId}/messages/search
     * Lấy lịch sử chat với phân trang
     */
    @GetMapping("/{roomId}/messages/search")
    public ResponseEntity<?> searchMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String cursor) {
        try {
            Map<String, Object> result = messageService.getMessages(roomId, limit, cursor);

            Map<String, Object> response = new HashMap<>();
            response.put("roomId", roomId);
            response.put("items", result.get("items"));
            response.put("nextCursor", result.get("nextCursor"));
            response.put("hasMore", result.get("nextCursor") != null);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to search messages",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/watchrooms/{roomId}/transfer-host
     * Chuyển quyền host cho user khác
     */
    @PostMapping("/{roomId}/transfer-host")
    public ResponseEntity<?> transferHost(
            @PathVariable String roomId,
            @RequestParam String fromUserId,
            @RequestParam String toUserId) {
        try {
            // Kiểm tra fromUser có phải owner không
            if (!memberService.isOwner(roomId, fromUserId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Only owner can transfer host"
                ));
            }

            // Kiểm tra toUser có trong phòng không
            if (!memberService.isMember(roomId, toUserId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Target user is not a member of this room"
                ));
            }

            memberService.transferOwnership(roomId, fromUserId, toUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Host transferred successfully",
                    "newOwnerId", toUserId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to transfer host",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/watchrooms/{roomId}/kick
     * Kick member khỏi phòng (chỉ owner)
     */
    @PostMapping("/{roomId}/kick")
    public ResponseEntity<?> kickMember(
            @PathVariable String roomId,
            @RequestParam String ownerId,
            @RequestParam String userId) {
        try {
            // Kiểm tra quyền owner
            if (!memberService.isOwner(roomId, ownerId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Only owner can kick members"
                ));
            }

            // Không cho kick chính mình
            if (ownerId.equals(userId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Cannot kick yourself"
                ));
            }

            memberService.removeMember(roomId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Member kicked successfully",
                    "kickedUserId", userId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to kick member",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/watchrooms/user/{userId}
     * Lấy tất cả phòng mà user đang tham gia
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserRooms(@PathVariable String userId) {
        try {
            List<WatchRoomMember> rooms = memberService.getRoomsByUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("rooms", rooms);
            response.put("count", rooms.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get user rooms",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/watchrooms/{roomId}/unread
     * Lấy số lượng messages chưa đọc
     */
    @GetMapping("/{roomId}/unread")
    public ResponseEntity<?> getUnreadCount(
            @PathVariable String roomId,
            @RequestParam String userId) {
        log.info("📬 REST API: GET /watchrooms/{}/unread?userId={}", roomId, userId);
        try {
            // Get last read message sort key
            String lastReadMessageSortKey = memberService.getLastReadMessageSortKey(roomId, userId);

            // Calculate unread count
            int unreadCount = messageService.getUnreadCount(roomId, lastReadMessageSortKey);

            log.info("✅ Unread count retrieved: roomId={}, userId={}, count={}", roomId, userId, unreadCount);

            Map<String, Object> response = new HashMap<>();
            response.put("roomId", roomId);
            response.put("userId", userId);
            response.put("unreadCount", unreadCount);
            response.put("lastReadMessageSortKey", lastReadMessageSortKey);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Failed to get unread count: roomId={}, userId={}", roomId, userId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to get unread count",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/watchrooms/{roomId}/read
     * Mark messages as read (REST API fallback for WebSocket)
     */
    @PostMapping("/{roomId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable String roomId,
            @RequestBody MarkReadRequest request) {
        log.info("📖 REST API: POST /watchrooms/{}/read, userId={}", roomId, request.getUserId());
        try {
            memberService.updateReadReceipt(roomId, request.getUserId(), request.getLastReadMessageSortKey());

            log.info("✅ Read receipt updated via REST API: roomId={}, userId={}", roomId, request.getUserId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Messages marked as read",
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("❌ Failed to mark as read: roomId={}, userId={}", roomId, request.getUserId(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to mark as read",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/watchrooms/{roomId}
     * Xóa phòng (soft delete) - chỉ ADMIN hoặc host
     * @param roomId Room ID to delete
     * @param force If true, delete even if room has viewers
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<DeleteRoomResponse> deleteRoom(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "false") boolean force) {

        // Get actorId from RequestContext (set by AuthInterceptor)
        String actorId = RequestContext.getActorId();

        // Call service to delete room
        watchRoomService.deleteRoom(roomId, actorId, force);

        return ResponseEntity.ok(new DeleteRoomResponse(true));
    }

    /**
     * DTO for mark as read request
     */
    public static class MarkReadRequest {
        private String userId;
        private String lastReadMessageSortKey;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getLastReadMessageSortKey() { return lastReadMessageSortKey; }
        public void setLastReadMessageSortKey(String lastReadMessageSortKey) {
            this.lastReadMessageSortKey = lastReadMessageSortKey;
        }
    }
}
