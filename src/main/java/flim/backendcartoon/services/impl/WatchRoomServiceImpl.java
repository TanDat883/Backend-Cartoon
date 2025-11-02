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
import flim.backendcartoon.entities.DTO.response.VideoStateDTO;
import flim.backendcartoon.entities.DTO.request.JoinRoomRequest;
import flim.backendcartoon.entities.DTO.response.JoinRoomResponse;
import flim.backendcartoon.entities.DTO.response.WatchRoomResponse;
import flim.backendcartoon.entities.WatchRoom;
import flim.backendcartoon.repositories.MovieRepository;
import flim.backendcartoon.repositories.UserReponsitory;
import flim.backendcartoon.repositories.WatchRoomRepository;
import flim.backendcartoon.services.WatchRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WatchRoomServiceImpl implements WatchRoomService {

    private final WatchRoomRepository watchRoomRepository;
    private final UserReponsitory userReponsitory;
    private final MovieRepository movieRepository;
    private final flim.backendcartoon.services.RoomMessageService roomMessageService;
    private final flim.backendcartoon.services.WatchRoomMemberService watchRoomMemberService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WatchRoomServiceImpl(WatchRoomRepository watchRoomRepository,
                                UserReponsitory userReponsitory,
                                MovieRepository movieRepository,
                                flim.backendcartoon.services.RoomMessageService roomMessageService,
                                flim.backendcartoon.services.WatchRoomMemberService watchRoomMemberService,
                                SimpMessagingTemplate messagingTemplate) {
        this.watchRoomRepository = watchRoomRepository;
        this.userReponsitory = userReponsitory;
        this.movieRepository = movieRepository;
        this.roomMessageService = roomMessageService;
        this.watchRoomMemberService = watchRoomMemberService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void createWatchRoom(CreateWatchRoomRequest req) {
        var now = OffsetDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

        var room = new WatchRoom();

        room.setRoomId(req.getRoomId()); // ✅ Use frontend's roomId
        room.setUserId(req.getUserId()); // For backward compatibility
        room.setHostUserId(req.getUserId()); // New field
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

        // Set TTL: 24h from creation
        long ttlEpochSeconds = now.toEpochSecond() + (24 * 60 * 60);
        room.setTtl(ttlEpochSeconds);

        if (Boolean.TRUE.equals(room.isPrivateRoom())) {
            room.setInviteCode(generateUniqueInviteCode());
        }

        watchRoomRepository.saveNew(room);
    }

    @Override
    public WatchRoom getWatchRoomById(String roomId) {
        WatchRoom room = watchRoomRepository.get(roomId);

        // Check if room exists and is not deleted/expired
        if (room == null || "DELETED".equals(room.getStatus()) || "EXPIRED".equals(room.getStatus())) {
            throw new flim.backendcartoon.exception.RoomGoneException("Room not found or has been deleted");
        }

        // Check TTL
        Long ttl = room.getTtl();
        if (ttl != null && ttl <= System.currentTimeMillis() / 1000) {
            throw new flim.backendcartoon.exception.RoomGoneException("Room has expired");
        }

        return room;
    }

    @Override
    public WatchRoom getRoomById(String roomId) {
        return getWatchRoomById(roomId);
    }

    @Override
    public List<WatchRoomResponse> getAllWatchRooms() {

        return watchRoomRepository.findAll()
                .stream()
                .filter(r -> {
                    // Only show ACTIVE and SCHEDULED rooms
                    String status = r.getStatus();
                    if ("DELETED".equals(status) || "EXPIRED".equals(status)) {
                        return false;
                    }
                    // Check TTL not expired
                    Long ttl = r.getTtl();
                    if (ttl != null && ttl <= System.currentTimeMillis() / 1000) {
                        return false;
                    }
                    return true;
                })
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
                    String hostId = r.getHostUserId() != null ? r.getHostUserId() : r.getUserId();
                    var user = userReponsitory.findById(hostId);
                    if (user != null) {
                        dto.setUserName(user.getUserName() != null ? user.getUserName() : "Host");
                        dto.setAvatarUrl(user.getAvatarUrl());
                    } else {
                        // Fallback nếu user không tồn tại (đã bị xóa)
                        dto.setUserName("Host");
                        dto.setAvatarUrl(null);
                    }

                    // Add video state
                    dto.setVideoState(getCurrentVideoState(r));
                    var movie = movieRepository.findById(r.getMovieId());
                    dto.setMovieTitle(movie != null ? movie.getTitle() : null);

                    // ✅ Add viewer count (count online members)
                    try {
                        java.util.List<flim.backendcartoon.entities.WatchRoomMember> onlineMembers =
                            watchRoomMemberService.getOnlineMembers(r.getRoomId());
                        dto.setViewerCount(onlineMembers != null ? onlineMembers.size() : 0);
                    } catch (Exception e) {
                        // If error, set to 0
                        dto.setViewerCount(0);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }


    /**
     * Calculate current video state from persisted data
     */
    public VideoStateDTO getCurrentVideoState(WatchRoom room) {
        if (room == null) {
            return null;
        }

        VideoStateDTO state = new VideoStateDTO();

        Boolean playing = room.getVideoPlaying();
        Long positionMs = room.getVideoPositionMs();
        Double playbackRate = room.getVideoPlaybackRate();
        Long lastUpdateMs = room.getVideoLastUpdateMs();

        // Set defaults if null
        playing = (playing != null) ? playing : false;
        positionMs = (positionMs != null) ? positionMs : 0L;
        playbackRate = (playbackRate != null) ? playbackRate : 1.0;

        long currentPositionMs = positionMs;

        // If video is playing, calculate drift since last update
        if (playing && lastUpdateMs != null) {
            long now = System.currentTimeMillis();
            long elapsedMs = now - lastUpdateMs;

            // Add elapsed time (adjusted by playback rate)
            currentPositionMs += (long) (elapsedMs * playbackRate);
        }

        state.setPlaying(playing);
        state.setPositionMs(currentPositionMs);
        state.setPlaybackRate(playbackRate);
        state.setServerTimeMs(System.currentTimeMillis());
        state.setUpdatedBy(room.getVideoUpdatedBy());

        return state;
    }

    /**
     * Update video state in database
     */
    public void updateVideoState(String roomId, Boolean playing, Long positionMs,
                                 Double playbackRate, String userId) {
        WatchRoom room = watchRoomRepository.get(roomId);
        if (room != null) {
            if (playing != null) {
                room.setVideoPlaying(playing);
            }
            if (positionMs != null) {
                room.setVideoPositionMs(positionMs);
            }
            if (playbackRate != null) {
                room.setVideoPlaybackRate(playbackRate);
            }
            room.setVideoLastUpdateMs(System.currentTimeMillis());
            room.setVideoUpdatedBy(userId);

            watchRoomRepository.upsert(room);
        }
    }

        @Override
        public JoinRoomResponse joinRoom (JoinRoomRequest req){
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

        private String generateInviteCode () {
            // Loại bỏ kí tự gây nhầm lẫn
            final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
            var rnd = new java.security.SecureRandom();
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
            return sb.toString();
        }


        private String generateUniqueInviteCode () {
            // Tối đa 10 lần thử để tránh trùng
            for (int i = 0; i < 10; i++) {
                String code = generateInviteCode();
                if (!watchRoomRepository.existsByInviteCode(code)) {
                    return code;
                }
            }
            throw new IllegalStateException("Không thể sinh inviteCode duy nhất, thử lại sau.");
        }

    @Override
    public void deleteRoom(String roomId, String actorId, boolean force) {
        // 1. Check authentication
        if (actorId == null || actorId.isBlank()) {
            throw new flim.backendcartoon.exception.UnauthorizedException("User not authenticated");
        }

        // 2. Get room
        WatchRoom room = watchRoomRepository.get(roomId);
        if (room == null || "DELETED".equals(room.getStatus()) || "EXPIRED".equals(room.getStatus())) {
            throw new flim.backendcartoon.exception.RoomGoneException("Room not found or already deleted");
        }

        // 3. Get user role
        flim.backendcartoon.entities.User user = userReponsitory.findById(actorId);
        if (user == null) {
            throw new flim.backendcartoon.exception.UnauthorizedException("User not found");
        }

        // 4. Check permission: must be ADMIN or host
        String hostUserId = room.getHostUserId() != null ? room.getHostUserId() : room.getUserId();
        boolean isAdmin = user.getRole() == flim.backendcartoon.entities.Role.ADMIN;
        boolean isHost = actorId.equals(hostUserId);

        if (!isAdmin && !isHost) {
            throw new flim.backendcartoon.exception.ForbiddenException("You don't have permission to delete this room");
        }

        // 5. Check if room has viewers (if not force)
        if (!force) {
            java.util.List<flim.backendcartoon.entities.WatchRoomMember> onlineMembers = watchRoomMemberService.getOnlineMembers(roomId);

            // ✅ FIX: Exclude the actor (person deleting) from viewer count
            // Only count OTHER online viewers, not the host themselves
            long otherViewersCount = onlineMembers.stream()
                .filter(member -> !actorId.equals(member.getUserId()))
                .count();

            if (otherViewersCount > 0) {
                throw new flim.backendcartoon.exception.RoomHasViewersException(
                    "Cannot delete room with " + otherViewersCount + " other active viewers. Use force=true to override"
                );
            }
        }

        // 6. Soft delete room
        room.setStatus("DELETED");
        room.setDeletedAt(java.time.OffsetDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toString());
        room.setDeletedBy(actorId);
        watchRoomRepository.upsert(room);

        // 7. ✅ BROADCAST WebSocket event TRƯỚC KHI xóa members (để connections còn alive)
        try {
            broadcastRoomDeleted(roomId, "DELETED", actorId);
            System.out.println("📢 Broadcasted ROOM_DELETED event to room: " + roomId);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to broadcast ROOM_DELETED event: " + e.getMessage());
        }

        // 8. Delete all messages in the room
        int deletedMessages = roomMessageService.deleteAllByRoomId(roomId);
        System.out.println("🗑️ Deleted " + deletedMessages + " messages from room " + roomId);

        // 9. Delete all members in the room
        int deletedMembers = watchRoomMemberService.deleteAllByRoomId(roomId);
        System.out.println("🗑️ Deleted " + deletedMembers + " members from room " + roomId);

        System.out.println("✅ Room deleted: roomId=" + roomId + ", deletedBy=" + actorId);
    }

    /**
     * Broadcast ROOM_DELETED event to all users in the room
     */
    private void broadcastRoomDeleted(String roomId, String reason, String deletedBy) {
        flim.backendcartoon.dto.WsEventDto event = new flim.backendcartoon.dto.WsEventDto();
        event.setType("ROOM_DELETED");
        event.setRoomId(roomId);
        event.setSenderId(null);
        event.setSenderName(null);
        event.setAvatarUrl(null);
        event.setCreatedAt(java.time.Instant.now().toString());

        // Payload
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("reason", reason);
        payload.put("deletedBy", deletedBy);
        payload.put("timestamp", System.currentTimeMillis());
        event.setPayload(payload);

        // Broadcast to room topic
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, event);
    }
    }
