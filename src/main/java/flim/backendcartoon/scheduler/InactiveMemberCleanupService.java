/*
 * @(#) InactiveMemberCleanupService.java    1.0     10/24/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.scheduler;

import flim.backendcartoon.dto.WsEventDto;
import flim.backendcartoon.entities.WatchRoomMember;
import flim.backendcartoon.services.RoomMessageService;
import flim.backendcartoon.services.WatchRoomMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to automatically cleanup inactive members from watch rooms
 * Members who don't send PING (heartbeat) for more than 60 seconds are removed
 *
 * @author Tran Tan Dat
 * @version 1.0
 * @created 24-October-2025
 */
@Service
public class InactiveMemberCleanupService {

    private static final Logger log = LoggerFactory.getLogger(InactiveMemberCleanupService.class);
    private static final long TIMEOUT_SECONDS = 60; // 60 seconds timeout

    private final WatchRoomMemberService memberService;
    private final RoomMessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    // Track last ping time for each member: { roomId: { userId: lastPingTimestamp } }
    private final Map<String, Map<String, Long>> roomMemberLastPing = new ConcurrentHashMap<>();

    public InactiveMemberCleanupService(
            WatchRoomMemberService memberService,
            RoomMessageService messageService,
            SimpMessagingTemplate messagingTemplate) {
        this.memberService = memberService;
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Update last ping time for a member
     * Called from WatchRoomWsController when receiving PING event
     */
    public void updateMemberPing(String roomId, String userId) {
        roomMemberLastPing
                .computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(userId, System.currentTimeMillis());
    }

    /**
     * Remove member ping tracking when they leave normally
     */
    public void removeMemberPing(String roomId, String userId) {
        Map<String, Long> members = roomMemberLastPing.get(roomId);
        if (members != null) {
            members.remove(userId);
            if (members.isEmpty()) {
                roomMemberLastPing.remove(roomId);
            }
        }
    }

    /**
     * Scheduled task to cleanup inactive members
     * Runs every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void cleanupInactiveMembers() {
        long now = System.currentTimeMillis();
        long timeoutMs = TIMEOUT_SECONDS * 1000;

        for (Map.Entry<String, Map<String, Long>> roomEntry : roomMemberLastPing.entrySet()) {
            String roomId = roomEntry.getKey();
            Map<String, Long> members = roomEntry.getValue();

            // Find inactive members (no ping for > 60 seconds)
            List<String> inactiveUsers = new ArrayList<>();
            for (Map.Entry<String, Long> memberEntry : members.entrySet()) {
                String userId = memberEntry.getKey();
                Long lastPingTime = memberEntry.getValue();

                if (now - lastPingTime > timeoutMs) {
                    inactiveUsers.add(userId);
                }
            }

            // Remove inactive members
            for (String userId : inactiveUsers) {
                try {
                    // Get member info before removing
                    WatchRoomMember member = memberService.getMember(roomId, userId);
                    if (member == null) {
                        members.remove(userId);
                        continue;
                    }

                    String userName = member.getUserName() != null ? member.getUserName() : "User";

                    // Remove from tracking
                    members.remove(userId);

                    // Remove from database
                    memberService.removeMember(roomId, userId);

                    // Save system message
                    messageService.createSystemMessage(roomId, userId, userName,
                            userName + " đã rời phòng (không hoạt động)");

                    // Broadcast LEAVE event to all members
                    WsEventDto leaveEvent = new WsEventDto("LEAVE");
                    leaveEvent.setRoomId(roomId);
                    leaveEvent.setSenderId(userId);
                    leaveEvent.setSenderName(userName);
                    leaveEvent.addPayload("reason", "inactive");
                    leaveEvent.setCreatedAt(Instant.now().toString());

                    messagingTemplate.convertAndSend("/topic/rooms/" + roomId, leaveEvent);

                    // Broadcast updated member list
                    broadcastMemberList(roomId);

                    log.info("Auto-removed inactive member {} ({}) from room {} (no ping for > {}s)",
                            userId, userName, roomId, TIMEOUT_SECONDS);

                } catch (Exception e) {
                    log.error("Error removing inactive member {} from room {}: {}",
                            userId, roomId, e.getMessage());
                }
            }

            // Cleanup empty rooms from tracking
            if (members.isEmpty()) {
                roomMemberLastPing.remove(roomId);
            }
        }
    }

    /**
     * Broadcast updated member list to all members in room
     */
    private void broadcastMemberList(String roomId) {
        try {
            List<WatchRoomMember> members = memberService.getAllMembers(roomId);

            WsEventDto memberListEvent = new WsEventDto("MEMBER_LIST");
            memberListEvent.setRoomId(roomId);
            memberListEvent.addPayload("members", members);
            memberListEvent.setCreatedAt(Instant.now().toString());

            messagingTemplate.convertAndSend("/topic/rooms/" + roomId, memberListEvent);
        } catch (Exception e) {
            log.error("Error broadcasting member list for room {}: {}", roomId, e.getMessage());
        }
    }

    /**
     * Get current tracking info (for debugging)
     */
    public Map<String, Map<String, Long>> getTrackingInfo() {
        return new ConcurrentHashMap<>(roomMemberLastPing);
    }
}

