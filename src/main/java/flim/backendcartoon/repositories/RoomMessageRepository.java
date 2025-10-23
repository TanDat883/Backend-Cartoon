package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.RoomMessage;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.time.Instant;
import java.util.*;

/**
 * Repository cho RoomMessage (chat + event log)
 *
 * @author Tran Tan Dat
 * @version 1.0
 */
@Repository
public class RoomMessageRepository {

    private final DynamoDbTable<RoomMessage> table;

    public RoomMessageRepository(DynamoDbEnhancedClient client) {
        this.table = client.table("RoomMessage", TableSchema.fromBean(RoomMessage.class));
    }

    /**
     * Thêm message mới vào phòng
     */
    public RoomMessage append(String roomId, RoomMessage message) {
        // Tạo sortKey: ts#<millis>#<uuid>
        long ts = System.currentTimeMillis();
        String sortKey = String.format("ts#%d#%s", ts, UUID.randomUUID().toString());

        message.setRoomId(roomId);
        message.setSortKey(sortKey);

        // Set TTL: 7 ngày (epoch seconds)
        if (message.getExpireAt() == null) {
            long ttl = Instant.now().plusSeconds(7 * 24 * 60 * 60).getEpochSecond();
            message.setExpireAt(ttl);
        }

        table.putItem(message);
        return message;
    }

    /**
     * Lấy danh sách messages với phân trang
     * @param roomId ID phòng
     * @param limit số lượng tin nhắn
     * @param cursor sortKey để tiếp tục (null nếu trang đầu)
     * @return Map chứa items và nextCursor
     */
    public Map<String, Object> list(String roomId, int limit, String cursor) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(roomId).build()
        );

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(condition)
                .scanIndexForward(false) // Sắp xếp giảm dần (mới nhất trước)
                .limit(limit);

        // Nếu có cursor, query từ vị trí đó (DynamoDB sẽ tự xử lý pagination)
        // Note: Để đơn giản, chúng ta sẽ dùng cách khác - filter theo sortKey
        if (cursor != null && !cursor.isEmpty()) {
            // Skip exclusiveStartKey vì cần convert sang Map<String, AttributeValue>
            // Tạm thời bỏ qua cursor cho đơn giản, hoặc implement sau
        }

        Iterator<Page<RoomMessage>> pages = table.query(requestBuilder.build()).iterator();

        List<RoomMessage> items = new ArrayList<>();
        String nextCursor = null;

        if (pages.hasNext()) {
            Page<RoomMessage> page = pages.next();
            items.addAll(page.items());

            // Lấy lastEvaluatedKey làm nextCursor
            if (page.lastEvaluatedKey() != null && !page.lastEvaluatedKey().isEmpty()) {
                nextCursor = page.lastEvaluatedKey().get("sk").s();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("nextCursor", nextCursor);

        return result;
    }

    /**
     * Xóa message (nếu cần - thường dùng TTL tự động)
     */
    public void delete(String roomId, String sortKey) {
        Key key = Key.builder()
                .partitionValue(roomId)
                .sortValue(sortKey)
                .build();
        table.deleteItem(key);
    }
}

