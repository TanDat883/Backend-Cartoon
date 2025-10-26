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
}
