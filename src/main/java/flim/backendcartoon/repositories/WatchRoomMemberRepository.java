package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.WatchRoomMember;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository cho WatchRoomMember
 *
 * @author Tran Tan Dat
 * @version 1.0
 */
@Repository
public class WatchRoomMemberRepository {

    private final DynamoDbTable<WatchRoomMember> table;

    public WatchRoomMemberRepository(DynamoDbEnhancedClient client) {
        this.table = client.table("WatchRoomMember", TableSchema.fromBean(WatchRoomMember.class));
    }

    /**
     * Thêm hoặc cập nhật member
     */
    public void upsert(WatchRoomMember member) {
        table.putItem(member);
    }

    /**
     * Xóa member khỏi phòng
     */
    public void remove(String roomId, String userId) {
        Key key = Key.builder()
                .partitionValue(roomId)
                .sortValue(userId)
                .build();
        table.deleteItem(key);
    }

    /**
     * Lấy thông tin member cụ thể
     */
    public WatchRoomMember get(String roomId, String userId) {
        Key key = Key.builder()
                .partitionValue(roomId)
                .sortValue(userId)
                .build();
        return table.getItem(key);
    }

    /**
     * Lấy tất cả members trong 1 phòng
     */
    public List<WatchRoomMember> listByRoom(String roomId) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(roomId).build()
        );

        return table.query(condition)
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả phòng mà user đang tham gia (qua GSI)
     */
    public List<WatchRoomMember> listRoomsByUser(String userId) {
        DynamoDbIndex<WatchRoomMember> index = table.index("GSI_UserId");

        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(userId).build()
        );

        return index.query(condition)
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    /**
     * Lấy members đang online (lastSeenAt trong 45 giây gần đây)
     */
    public List<WatchRoomMember> listOnlineMembers(String roomId, long thresholdSeconds) {
        long now = System.currentTimeMillis();
        long threshold = now - (thresholdSeconds * 1000);

        return listByRoom(roomId).stream()
                .filter(m -> {
                    if (m.getLastSeenAt() == null) return false;
                    try {
                        long lastSeen = java.time.Instant.parse(m.getLastSeenAt()).toEpochMilli();
                        return lastSeen >= threshold;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }
}

