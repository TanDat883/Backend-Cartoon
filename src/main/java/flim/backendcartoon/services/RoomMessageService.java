package flim.backendcartoon.services;

import flim.backendcartoon.entities.RoomMessage;
import flim.backendcartoon.repositories.RoomMessageRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service quản lý chat và event log trong phòng xem chung
 *
 * @author Tran Tan Dat
 * @version 1.0
 */
@Service
public class RoomMessageService {

    private final RoomMessageRepository messageRepository;

    public RoomMessageService(RoomMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Tạo chat message
     */
    public RoomMessage createChatMessage(String roomId, String senderId, String senderName,
                                         String avatarUrl, String text) {
        RoomMessage message = new RoomMessage();
        message.setType("CHAT");
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setAvatarUrl(avatarUrl);
        message.setContent(text);
        message.setCreatedAt(Instant.now().toString());

        return messageRepository.append(roomId, message);
    }

    /**
     * Tạo system message (JOIN, LEAVE)
     */
    public RoomMessage createSystemMessage(String roomId, String senderId, String senderName,
                                           String content) {
        RoomMessage message = new RoomMessage();
        message.setType("SYSTEM");
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setContent(content);
        message.setCreatedAt(Instant.now().toString());

        return messageRepository.append(roomId, message);
    }

    /**
     * Tạo event message (PLAY, PAUSE, SEEK)
     */
    public RoomMessage createEventMessage(String roomId, String senderId, String eventType,
                                          Map<String, String> meta) {
        RoomMessage message = new RoomMessage();
        message.setType("EVENT");
        message.setSenderId(senderId);
        message.setContent(eventType);
        message.setMeta(meta);
        message.setCreatedAt(Instant.now().toString());

        return messageRepository.append(roomId, message);
    }

    /**
     * Lấy danh sách messages với phân trang
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMessages(String roomId, int limit, String cursor) {
        return messageRepository.list(roomId, limit, cursor);
    }

    /**
     * Lấy messages mới nhất
     */
    @SuppressWarnings("unchecked")
    public List<RoomMessage> getRecentMessages(String roomId, int limit) {
        Map<String, Object> result = messageRepository.list(roomId, limit, null);
        return (List<RoomMessage>) result.get("items");
    }
}

