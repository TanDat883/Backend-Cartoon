/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.Payment;
import flim.backendcartoon.entities.PaymentDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Objects;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-July-2025 2:03 PM
 */
@Repository
public class PaymentDetailRepository {
    private final DynamoDbTable<PaymentDetail> table;

    @Autowired
    public PaymentDetailRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("PaymentDetail", TableSchema.fromBean(PaymentDetail.class));
    }

    public void save(PaymentDetail paymentDetail) {
        System.out.println("Saving payment order to DynamoDB: " + paymentDetail);
        table.putItem(paymentDetail);
    }

    public void update(PaymentDetail paymentDetail) {
        table.updateItem(paymentDetail);
    }


    public PaymentDetail findByPaymentCode(Long paymentCode) {
        return table.scan().items().stream()
                .filter(p -> p.getPaymentCode().equals(paymentCode))
                .findFirst()
                .orElse(null);
    }

   public PaymentDetail findById(String id) {
        return table.getItem(r -> r.key(k -> k.partitionValue(id)));
    }

}
