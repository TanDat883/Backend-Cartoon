/*
 * @(#) $(NAME).java    1.0     10/18/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 18-October-2025 3:48 PM
 */

import flim.backendcartoon.entities.DTO.request.CreateWatchRoomRequest;
import flim.backendcartoon.entities.DTO.response.VideoStateDTO;
import flim.backendcartoon.entities.DTO.request.JoinRoomRequest;
import flim.backendcartoon.entities.DTO.response.JoinRoomResponse;
import flim.backendcartoon.entities.DTO.response.WatchRoomResponse;
import flim.backendcartoon.entities.WatchRoom;
import flim.backendcartoon.services.WatchRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/watchrooms")
public class WatchRoomController {
    private final WatchRoomService watchRoomService;

    @PostMapping()
    public ResponseEntity<String> createWatchRoom(@Valid @RequestBody CreateWatchRoomRequest request) {
        watchRoomService.createWatchRoom(request);
        return ResponseEntity.ok("Watch room created successfully");
    }

    @GetMapping
    public ResponseEntity<List<WatchRoomResponse>> getAllWatchRooms() {
        List<WatchRoomResponse> watchRooms = watchRoomService.getAllWatchRooms();
        return ResponseEntity.ok(watchRooms);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getWatchRoomById(@PathVariable String roomId) {
        WatchRoom watchRoom = watchRoomService.getWatchRoomById(roomId);

        if (watchRoom == null) {
            return ResponseEntity.notFound().build();
        }

        // Create response with video state
        Map<String, Object> response = new HashMap<>();
        response.put("roomId", watchRoom.getRoomId());
        response.put("userId", watchRoom.getUserId());
        response.put("movieId", watchRoom.getMovieId());
        response.put("roomName", watchRoom.getRoomName());
        response.put("posterUrl", watchRoom.getPosterUrl());
        response.put("videoUrl", watchRoom.getVideoUrl());
        response.put("isPrivate", watchRoom.isPrivateRoom());
        response.put("isAutoStart", watchRoom.isAutoStart());
        response.put("startAt", watchRoom.getStartAt());
        response.put("createdAt", watchRoom.getCreatedAt());
        response.put("status", watchRoom.getStatus());
        response.put("inviteCode", watchRoom.getInviteCode());

        // Add video state
        VideoStateDTO videoState = watchRoomService.getCurrentVideoState(watchRoom);
        response.put("videoState", videoState);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/watch-rooms/join")
    public ResponseEntity<JoinRoomResponse> join(@RequestBody JoinRoomRequest req) {
        var res = watchRoomService.joinRoom(req);
        if (!res.isOk()) return ResponseEntity.status(403).body(res);
        return ResponseEntity.ok(res);
    }

    /**
     * Verify invite code for private room
     * @param roomId Room ID to verify
     * @param request Request body containing inviteCode
     * @return Response with valid status
     */
    @PostMapping("/{roomId}/verify-invite")
    public ResponseEntity<Map<String, Object>> verifyInviteCode(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request) {

        String inviteCode = request.get("inviteCode");

        Map<String, Object> response = new HashMap<>();

        try {
            // Get room by ID
            WatchRoom room = watchRoomService.getWatchRoomById(roomId);

            if (room == null) {
                response.put("valid", false);
                response.put("message", "Phòng không tồn tại");
                return ResponseEntity.status(404).body(response);
            }

            // Check if room is private
            if (!Boolean.TRUE.equals(room.isPrivateRoom())) {
                // Public room - no invite code needed
                response.put("valid", true);
                response.put("message", "Phòng công khai");
                return ResponseEntity.ok(response);
            }

            // Verify invite code for private room
            if (inviteCode == null || inviteCode.trim().isEmpty()) {
                response.put("valid", false);
                response.put("message", "Mã mời không được để trống");
                return ResponseEntity.status(400).body(response);
            }

            if (room.getInviteCode() == null) {
                response.put("valid", false);
                response.put("message", "Phòng chưa có mã mời");
                return ResponseEntity.status(400).body(response);
            }

            // Case-insensitive comparison
            boolean isValid = room.getInviteCode().equalsIgnoreCase(inviteCode.trim());

            if (isValid) {
                response.put("valid", true);
                response.put("message", "Mã mời hợp lệ");
                return ResponseEntity.ok(response);
            } else {
                response.put("valid", false);
                response.put("message", "Mã mời không đúng");
                return ResponseEntity.status(401).body(response);
            }

        } catch (Exception e) {
            response.put("valid", false);
            response.put("message", "Lỗi xác thực: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
