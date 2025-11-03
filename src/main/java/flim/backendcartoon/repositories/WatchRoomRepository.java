/*
 * @(#) $(NAME).java    1.0     10/18/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 18-October-2025 3:42 PM
 */

import flim.backendcartoon.entities.Promotion;
import flim.backendcartoon.entities.WatchRoom;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;

import java.util.List;
@Repository
public class WatchRoomRepository {
    private final DynamoDbTable<WatchRoom> table;

    public WatchRoomRepository(DynamoDbEnhancedClient client) {
        this.table = client.table("WatchRoom", TableSchema.fromBean(WatchRoom.class));
    }

    public void saveNew(WatchRoom room) {
        table.putItem(r -> r.item(room)
                .conditionExpression(Expression.builder()
                        .expression("attribute_not_exists(roomId)")
                        .build()));
    }

    public void upsert(WatchRoom room) {
        table.putItem(room);
    }

    public boolean existsByInviteCode(String inviteCode) {
        var expression = Expression.builder()
                .expression("inviteCode = :inviteCode")
                .putExpressionValue(":inviteCode", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(inviteCode).build())
                .build();

        var result = table.scan(r -> r.filterExpression(expression)).items().stream().findFirst();
        return result.isPresent();
    }

    public WatchRoom findByInviteCode(String inviteCode) {
        var expression = Expression.builder()
                .expression("inviteCode = :inviteCode")
                .putExpressionValue(":inviteCode", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(inviteCode).build())
                .build();

        var result = table.scan(r -> r.filterExpression(expression)).items().stream().findFirst();
        return result.orElse(null);
    }

    public WatchRoom get(String roomId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(roomId)));
    }

    // CẢNH BÁO: scan chỉ nên dùng tạm để test
    public List<WatchRoom> findAll() {
        return table.scan().items().stream().toList();
    }

    //xóa phòng
    public void deleteRoom (String roomId){
        table.deleteItem(Key.builder().partitionValue(roomId).build());
    }
}
