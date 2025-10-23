package flim.backendcartoon.services;

import flim.backendcartoon.entities.WatchRoomMember;
import flim.backendcartoon.repositories.WatchRoomMemberRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service quản lý thành viên phòng xem chung
 *
 * @author Tran Tan Dat
 * @version 1.0
 */
@Service
public class WatchRoomMemberService {

    private final WatchRoomMemberRepository memberRepository;

    public WatchRoomMemberService(WatchRoomMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * Thêm member vào phòng
     */
    public WatchRoomMember addMember(String roomId, String userId, String role) {
        return addMember(roomId, userId, role, null, null);
    }

    /**
     * Thêm member vào phòng với thông tin đầy đủ
     */
    public WatchRoomMember addMember(String roomId, String userId, String role, String userName, String avatarUrl) {
        WatchRoomMember member = new WatchRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setRole(role);
        member.setUserName(userName != null ? userName : "User");
        member.setAvatarUrl(avatarUrl);
        member.setJoinedAt(Instant.now().toString());
        member.setLastSeenAt(Instant.now().toString());

        // TTL: 24 giờ sau khi join (có thể điều chỉnh)
        long ttl = Instant.now().plusSeconds(24 * 60 * 60).getEpochSecond();
        member.setExpireAt(ttl);

        memberRepository.upsert(member);
        return member;
    }

    /**
     * Cập nhật heartbeat (lastSeenAt)
     */
    public void updateHeartbeat(String roomId, String userId) {
        WatchRoomMember member = memberRepository.get(roomId, userId);
        if (member != null) {
            member.setLastSeenAt(Instant.now().toString());
            memberRepository.upsert(member);
        }
    }

    /**
     * Xóa member khỏi phòng
     */
    public void removeMember(String roomId, String userId) {
        memberRepository.remove(roomId, userId);
    }

    /**
     * Lấy thông tin member
     */
    public WatchRoomMember getMember(String roomId, String userId) {
        return memberRepository.get(roomId, userId);
    }

    /**
     * Kiểm tra user có phải là owner không
     */
    public boolean isOwner(String roomId, String userId) {
        WatchRoomMember member = getMember(roomId, userId);
        return member != null && "OWNER".equals(member.getRole());
    }

    /**
     * Kiểm tra user có trong phòng không
     */
    public boolean isMember(String roomId, String userId) {
        return getMember(roomId, userId) != null;
    }

    /**
     * Lấy tất cả members trong phòng
     */
    public List<WatchRoomMember> getAllMembers(String roomId) {
        return memberRepository.listByRoom(roomId);
    }

    /**
     * Lấy members đang online (lastSeenAt trong 45 giây)
     */
    public List<WatchRoomMember> getOnlineMembers(String roomId) {
        return memberRepository.listOnlineMembers(roomId, 45);
    }

    /**
     * Lấy tất cả phòng mà user đang tham gia
     */
    public List<WatchRoomMember> getRoomsByUser(String userId) {
        return memberRepository.listRoomsByUser(userId);
    }

    /**
     * Chuyển quyền owner cho user khác
     */
    public void transferOwnership(String roomId, String fromUserId, String toUserId) {
        WatchRoomMember currentOwner = getMember(roomId, fromUserId);
        WatchRoomMember newOwner = getMember(roomId, toUserId);

        if (currentOwner != null && "OWNER".equals(currentOwner.getRole())) {
            currentOwner.setRole("MEMBER");
            memberRepository.upsert(currentOwner);
        }

        if (newOwner != null) {
            newOwner.setRole("OWNER");
            memberRepository.upsert(newOwner);
        }
    }
}

