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
import flim.backendcartoon.entities.DTO.request.JoinRoomRequest;
import flim.backendcartoon.entities.DTO.response.JoinRoomResponse;
import flim.backendcartoon.entities.DTO.response.WatchRoomResponse;
import flim.backendcartoon.entities.WatchRoom;
import flim.backendcartoon.repositories.MovieRepository;
import flim.backendcartoon.repositories.UserReponsitory;
import flim.backendcartoon.repositories.WatchRoomRepository;
import flim.backendcartoon.services.WatchRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WatchRoomServiceImpl implements WatchRoomService {

    private final WatchRoomRepository watchRoomRepository;
    private final UserReponsitory userReponsitory;
    private final MovieRepository movieRepository;

    @Autowired
    public WatchRoomServiceImpl(WatchRoomRepository watchRoomRepository, UserReponsitory userReponsitory, MovieRepository movieRepository) {
        this.watchRoomRepository = watchRoomRepository;
        this.userReponsitory = userReponsitory;
        this.movieRepository = movieRepository;
    }

    @Override
    public void createWatchRoom(CreateWatchRoomRequest req) {
        var now = OffsetDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

        var room = new WatchRoom();
        room.setRoomId("room_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        room.setUserId(req.getUserId());
        room.setMovieId(req.getMovieId());
        room.setRoomName(req.getRoomName());
        room.setPosterUrl(req.getPosterUrl());
        room.setVideoUrl(req.getVideoUrl());
        room.setPrivateRoom(Boolean.TRUE.equals(req.getIsPrivate()));
        room.setAutoStart(Boolean.TRUE.equals(req.getIsAutoStart()));
        room.setStartAt(req.getStartAt());
        room.setCreatedAt(now.toString());
        room.setInviteCode(null); // default

        var status = (req.getStartAt() != null && !req.getStartAt().isBlank()) ? "SCHEDULED" : "ACTIVE";
        room.setStatus(status);

        if (Boolean.TRUE.equals(room.isPrivateRoom())) {
            room.setInviteCode(generateUniqueInviteCode());
        }

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
                    dto.setVideoUrl(r.getVideoUrl());
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

                    var movie = movieRepository.findById(r.getMovieId());
                    dto.setMovieTitle(movie != null ? movie.getTitle() : null);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public JoinRoomResponse joinRoom(JoinRoomRequest req) {
        var room = watchRoomRepository.get(req.getRoomId());
        if (room == null) {
            return new JoinRoomResponse(false, "Phòng không tồn tại");
        }
        if (Boolean.TRUE.equals(room.isPrivateRoom())) {
            if (room.getInviteCode() == null || req.getInviteCode() == null
                    || !room.getInviteCode().equalsIgnoreCase(req.getInviteCode().trim())) {
                return new JoinRoomResponse(false, "Mã mời không hợp lệ");
            }
        }
        return new JoinRoomResponse(true, "Tham gia thành công");
    }

    private String generateInviteCode() {
        // Loại bỏ kí tự gây nhầm lẫn
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        var rnd = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        return sb.toString();
    }

    private String generateUniqueInviteCode() {
        // Tối đa 10 lần thử để tránh trùng
        for (int i = 0; i < 10; i++) {
            String code = generateInviteCode();
            if (!watchRoomRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không thể sinh inviteCode duy nhất, thử lại sau.");
    }
}
