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

import flim.backendcartoon.entities.DTO.request.CreatePromotionRequest;
import flim.backendcartoon.entities.DTO.request.CreateWatchRoomRequest;
import flim.backendcartoon.entities.DTO.response.WatchRoomResponse;
import flim.backendcartoon.entities.WatchRoom;
import flim.backendcartoon.services.WatchRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<WatchRoom> getWatchRoomById(@PathVariable String roomId) {
        WatchRoom watchRoom = watchRoomService.getWatchRoomById(roomId);
        return ResponseEntity.ok(watchRoom);
    }
}
