/*
 * @(#) $(NAME).java    1.0     7/31/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 31-July-2025 8:31 PM
 */
@Repository
public class OrderRepository {
    private final DynamoDbTable<Order> table;

    @Autowired
    public OrderRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Order", TableSchema.fromBean(Order.class));
    }

    public void save(Order order) {
        System.out.println("Saving order to DynamoDB: " + order);
        table.putItem(order);
    }

    public void update(Order order) {
        table.updateItem(order);
    }

    public Order findByOrderId(String orderId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(orderId)));
    }

//    public List<Order> findByUserId(String userId) {
//        return table.scan(r -> r.filterExpression("userId = :userId")
//                .expressionAttributeValues(Map.of(":userId", userId)))
//                .items()
//                .stream()
//                .collect(Collectors.toList());
//    }
}
