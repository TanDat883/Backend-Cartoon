/*
 * @(#) $(NAME).java    1.0     10/18/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 18-October-2025 3:46 PM
 */

import flim.backendcartoon.entities.DTO.request.CreateWatchRoomRequest;
import flim.backendcartoon.entities.DTO.response.WatchRoomResponse;
import flim.backendcartoon.entities.WatchRoom;
import flim.backendcartoon.repositories.UserReponsitory;
import flim.backendcartoon.repositories.WatchRoomRepository;
import flim.backendcartoon.services.WatchRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WatchRoomServiceImpl implements WatchRoomService {

    private final WatchRoomRepository watchRoomRepository;
    private final UserReponsitory userReponsitory;

    @Autowired
    public WatchRoomServiceImpl(WatchRoomRepository watchRoomRepository, UserReponsitory userReponsitory) {
        this.watchRoomRepository = watchRoomRepository;
        this.userReponsitory = userReponsitory;
    }

    @Override
    public void createWatchRoom(CreateWatchRoomRequest req) {
        var now = java.time.OffsetDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        var room = new WatchRoom();
        room.setRoomId("room_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        room.setUserId(req.getUserId());
        room.setMovieId(req.getMovieId());
        room.setRoomName(req.getRoomName());
        room.setPosterUrl(req.getPosterUrl());
        room.setPrivateRoom(Boolean.TRUE.equals(req.getIsPrivate()));
        room.setAutoStart(Boolean.TRUE.equals(req.getIsAutoStart()));
        room.setStartAt(req.getStartAt());
        room.setCreatedAt(now.toString());
        room.setInviteCode(req.getInviteCode());

        var status = (req.getStartAt() != null && !req.getStartAt().isBlank())
                ? "SCHEDULED" : "ACTIVE";
        room.setStatus(status);

        watchRoomRepository.saveNew(room);
    }

    @Override
    public WatchRoom getWatchRoomById(String roomId) {
        return watchRoomRepository.get(roomId);
    }

    @Override
    public WatchRoom getRoomById(String roomId) {
        return getWatchRoomById(roomId);
    }

    @Override
    public List<WatchRoomResponse> getAllWatchRooms() {

        return watchRoomRepository.findAll()
                .stream()
                .map(r -> {
                    WatchRoomResponse dto = new WatchRoomResponse();
                    dto.setUserId(r.getUserId());
                    dto.setRoomId(r.getRoomId());
                    dto.setMovieId(r.getMovieId());
                    dto.setRoomName(r.getRoomName());
                    dto.setPosterUrl(r.getPosterUrl());
                    dto.setIsPrivate(Boolean.TRUE.equals(r.isPrivateRoom()));

                    dto.setStartAt(r.getStartAt());

                    // Lấy thông tin user với null-check để tránh NullPointerException
                    var user = userReponsitory.findById(r.getUserId());
                    if (user != null) {
                        dto.setUserName(user.getUserName() != null ? user.getUserName() : "Host");
                        dto.setAvatarUrl(user.getAvatarUrl());
                    } else {
                        // Fallback nếu user không tồn tại (đã bị xóa)
                        dto.setUserName("Host");
                        dto.setAvatarUrl(null);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }
}
