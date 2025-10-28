/*
 * @(#) RoomExpirySweeper.java    1.0     10/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.scheduler;

import flim.backendcartoon.entities.WatchRoom;
import flim.backendcartoon.repositories.WatchRoomRepository;
import flim.backendcartoon.services.RoomMessageService;
import flim.backendcartoon.services.WatchRoomMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * @description Scheduled task to expire rooms after 24 hours
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-October-2025
 */
@Component
public class RoomExpirySweeper {

    private static final Logger log = LoggerFactory.getLogger(RoomExpirySweeper.class);

    @Autowired
    private WatchRoomRepository watchRoomRepository;

    @Autowired
    private RoomMessageService roomMessageService;

    @Autowired
    private WatchRoomMemberService watchRoomMemberService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Run every 5 minutes to check for expired rooms
     * Find ACTIVE rooms that are older than 24h and mark them as EXPIRED
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes = 300,000ms
    public void sweepExpiredRooms() {
        log.info("🧹 Starting room expiry sweep...");

        try {
            List<WatchRoom> allRooms = watchRoomRepository.findAll();
            long now = System.currentTimeMillis() / 1000; // Current epoch seconds
            int expiredCount = 0;

            for (WatchRoom room : allRooms) {
                // Only process ACTIVE or SCHEDULED rooms
                String status = room.getStatus();
                if (!"ACTIVE".equals(status) && !"SCHEDULED".equals(status)) {
                    continue;
                }

                // Check if TTL is expired
                Long ttl = room.getTtl();
                if (ttl != null && ttl <= now) {
                    // Mark as EXPIRED
                    room.setStatus("EXPIRED");
                    room.setExpiredAt(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString());

                    watchRoomRepository.upsert(room);
                    expiredCount++;

                    log.info("⏰ Room expired: roomId={}, createdAt={}",
                             room.getRoomId(), room.getCreatedAt());

                    // ✅ BROADCAST WebSocket event TRƯỚC KHI xóa members
                    try {
                        broadcastRoomDeleted(room.getRoomId(), "EXPIRED", "SYSTEM");
                        log.info("📢 Broadcasted ROOM_DELETED event for expired room: {}", room.getRoomId());
                    } catch (Exception e) {
                        log.error("⚠️ Failed to broadcast ROOM_DELETED event for room {}", room.getRoomId(), e);
                    }

                    // Delete all messages in the room
                    try {
                        int deletedMessages = roomMessageService.deleteAllByRoomId(room.getRoomId());
                        log.info("🗑️ Deleted {} messages from expired room {}", deletedMessages, room.getRoomId());
                    } catch (Exception e) {
                        log.error("❌ Failed to delete messages for room {}", room.getRoomId(), e);
                    }

                    // Delete all members in the room
                    try {
                        int deletedMembers = watchRoomMemberService.deleteAllByRoomId(room.getRoomId());
                        log.info("🗑️ Deleted {} members from expired room {}", deletedMembers, room.getRoomId());
                    } catch (Exception e) {
                        log.error("❌ Failed to delete members for room {}", room.getRoomId(), e);
                    }
                }
            }

            if (expiredCount > 0) {
                log.info("✅ Expired {} rooms", expiredCount);
            } else {
                log.debug("✅ No rooms to expire");
            }

        } catch (Exception e) {
            log.error("❌ Error during room expiry sweep", e);
        }
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

