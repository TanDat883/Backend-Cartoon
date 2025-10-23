package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.WatchRoomMember;
import flim.backendcartoon.services.RoomMessageService;
import flim.backendcartoon.services.WatchRoomMemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    private final WatchRoomMemberService memberService;
    private final RoomMessageService messageService;

    public WatchRoomRestController(
            WatchRoomMemberService memberService,
            RoomMessageService messageService) {
        this.memberService = memberService;
        this.messageService = messageService;
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
}

