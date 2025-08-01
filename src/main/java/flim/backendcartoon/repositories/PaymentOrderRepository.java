/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.PaymentOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-July-2025 2:03 PM
 */
@Repository
public class PaymentOrderRepository {
    private final DynamoDbTable<PaymentOrder> table;

    @Autowired
    public PaymentOrderRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("PaymentOrder", TableSchema.fromBean(PaymentOrder.class));
    }

    public void save(PaymentOrder paymentOrder) {
        System.out.println("Saving payment order to DynamoDB: " + paymentOrder);
        table.putItem(paymentOrder);
    }

    public void update(PaymentOrder paymentOrder) {
        table.updateItem(paymentOrder);
    }

    public PaymentOrder findByOrderCode(Long code) {
        return table.getItem(r -> r.key(k -> k.partitionValue(code)));
    }

    public PaymentOrder findByOrderId(String orderId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(orderId)));
    }
}
